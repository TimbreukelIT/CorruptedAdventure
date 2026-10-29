package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.Optional;

/**
 * Natural canyon walls for The Layered's arrival and its claw marks (fase-4-nightmares-concept.md "Stap 2 voor ring
 * 2–5"). A wall counts as natural when the stone its back would lie against is the same natural rock and sand its rock
 * fall works with (the block tag {@code corruptedadventure:layered_rockfall}), so never a player's build. Walls are found
 * by casting rays out from a point (what you can see from there), only through loaded chunks. Server-side, read only.
 */
public final class LayeredWallSpot {
    /** Blocks between its hitbox and the wall: within the 0.2 that {@link TheLayered#touchesWall} probes. */
    private static final double GAP = 0.05;
    /** Its hitbox lies against the wall two blocks from its block: the block of its feet maps to one place. */
    private static final int WALL_STEPS = 2;
    private static final int RAYS = 24;
    private static final double[] SPOT_HEIGHTS = {3.0, 5.0, 1.5};
    private static final double[] FACE_HEIGHTS = {1.5, 3.0};
    private static final double MIN_DISTANCE = 4.0;
    /** A spot this close above the ground costs as much as this many blocks of distance: it should hang, not stand. */
    private static final int LOW_GAP = 2;
    private static final double LOW_COST = 3.0;
    /** Of the stone behind its back, at least this share must be there (a wall, not a lone boulder). */
    private static final double MIN_COVER = 0.65;

    private LayeredWallSpot() {
    }

    /**
     * Where its feet go on a wall: {@code pos} the block of its feet (what an arrival stores as its place), {@code feet}
     * the exact position, {@code wall} the side of the wall (it faces it).
     */
    public record Spot(BlockPos pos, Vec3 feet, Direction wall) {
    }

    /** A flat piece of wall: {@code at} the middle of the face of {@code block}, which faces {@code facing}. */
    public record Face(Vec3 at, Direction facing, BlockPos block, BlockState state) {
    }

    /**
     * The best spot on a natural wall within {@code radius} of {@code from} (a target's feet), seen from there: the nearest,
     * at least {@link #MIN_DISTANCE} away, preferring one off the ground. Empty without a wall, or where the chunks are not
     * loaded.
     */
    public static Optional<Spot> find(ServerLevel level, EntityType<?> type, Vec3 from, double radius, RandomSource random) {
        if (!loaded(level, from, radius)) {
            return Optional.empty();
        }
        float start = random.nextFloat() * Mth.TWO_PI;
        Spot best = null;
        double bestCost = Double.MAX_VALUE;
        for (int i = 0; i < RAYS; i++) {
            float angle = start + i * Mth.TWO_PI / RAYS;
            for (double height : SPOT_HEIGHTS) {
                Vec3 eye = from.add(0.0, height, 0.0);
                BlockHitResult hit = naturalHit(level, eye, angle, radius);
                if (hit == null) {
                    continue;
                }
                Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
                Vec3 centre = hit.getLocation().add(normal.scale(type.getWidth() / 2.0 + GAP));
                int feetY = Mth.floor(hit.getLocation().y - type.getHeight() / 2.0);
                for (int dy : new int[] {0, 1, -1}) {
                    Optional<Spot> spot = at(level, type, BlockPos.containing(centre.x, feetY + dy, centre.z));
                    if (spot.isEmpty()) {
                        continue;
                    }
                    double distance = Math.sqrt(Mth.square(spot.get().feet().x - from.x) + Mth.square(spot.get().feet().z - from.z));
                    if (distance < MIN_DISTANCE) {
                        continue;
                    }
                    double cost = distance + (gapBelow(level, spot.get().pos()) < LOW_GAP ? LOW_COST : 0.0);
                    if (cost < bestCost) {
                        best = spot.get();
                        bestCost = cost;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * The spot whose feet are in block {@code pos}, against a natural wall {@value #WALL_STEPS} blocks to one side: room
     * for the whole hitbox (dry), the wall right beside it and mostly natural stone. Empty if no side has one.
     */
    public static Optional<Spot> at(ServerLevel level, EntityType<?> type, BlockPos pos) {
        double half = type.getWidth() / 2.0;
        for (Direction wall : Direction.Plane.HORIZONTAL) {
            Vec3 feet = feetAgainst(pos, wall, half);
            AABB box = type.getDimensions().makeBoundingBox(feet);
            if (!level.hasChunksAt(Mth.floor(box.minX), Mth.floor(box.minZ), Mth.floor(box.maxX) + 1, Mth.floor(box.maxZ) + 1)
                    || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.noBlockCollision(null, box) || level.containsAnyLiquid(box)) {
                continue;
            }
            AABB probe = new AABB(box.minX, box.minY + 0.5, box.minZ, box.maxX, box.maxY, box.maxZ)
                    .deflate(0.05, 0.0, 0.05).move(wall.getStepX() * 0.2, 0.0, wall.getStepZ() * 0.2);
            if (!level.noBlockCollision(null, probe) && naturalBehind(level, box, wall)) {
                return Optional.of(new Spot(pos, feet, wall));
            }
        }
        return Optional.empty();
    }

    /**
     * The nearest flat piece of natural wall within {@code radius} of {@code from} (claw marks): its block and the
     * blocks round it on the face are natural stone with open air in front. Empty without one or where not loaded.
     */
    public static Optional<Face> findFace(ServerLevel level, Vec3 from, double radius) {
        if (!loaded(level, from, radius)) {
            return Optional.empty();
        }
        Face best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < RAYS; i++) {
            float angle = i * Mth.TWO_PI / RAYS;
            for (double height : FACE_HEIGHTS) {
                BlockHitResult hit = naturalHit(level, from.add(0.0, height, 0.0), angle, radius);
                if (hit == null || !isFlat(level, hit.getBlockPos(), hit.getDirection())) {
                    continue;
                }
                double distance = hit.getLocation().distanceTo(from);
                if (distance < bestDistance) {
                    BlockPos block = hit.getBlockPos();
                    Vec3 at = Vec3.atCenterOf(block).add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.5));
                    best = new Face(at, hit.getDirection(), block, level.getBlockState(block));
                    bestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** Natural stone for its back, its marks and their dust. */
    public static boolean isNatural(BlockState state) {
        return state.is(ModTags.Blocks.LAYERED_ROCKFALL);
    }

    /** The first block a ray hits, if it hits the side of natural stone (not a top or a bottom, not a build). */
    private static BlockHitResult naturalHit(ServerLevel level, Vec3 eye, float angle, double radius) {
        Vec3 end = eye.add(Mth.cos(angle) * radius, 0.0, Mth.sin(angle) * radius);
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                CollisionContext.empty()));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getDirection().getAxis().isHorizontal()
                || !isNatural(level.getBlockState(hit.getBlockPos()))) {
            return null;
        }
        return hit;
    }

    private static Vec3 feetAgainst(BlockPos pos, Direction wall, double half) {
        BlockPos stone = pos.relative(wall, WALL_STEPS);
        // The face of the wall block toward pos; the hitbox's side lies GAP before it.
        double face = wall.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? stone.get(wall.getAxis()) : stone.get(wall.getAxis()) + 1.0;
        double across = face - wall.getAxisDirection().getStep() * (half + GAP);
        return wall.getAxis() == Direction.Axis.X
                ? new Vec3(across, pos.getY(), pos.getZ() + 0.5)
                : new Vec3(pos.getX() + 0.5, pos.getY(), across);
    }

    /** The stone behind its back: no build, and at least {@link #MIN_COVER} of it there. */
    private static boolean naturalBehind(ServerLevel level, AABB box, Direction wall) {
        int depth = wall.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? Mth.floor((wall.getAxis() == Direction.Axis.X ? box.maxX : box.maxZ) + 0.2)
                : Mth.floor((wall.getAxis() == Direction.Axis.X ? box.minX : box.minZ) - 0.2);
        boolean alongX = wall.getAxis() == Direction.Axis.Z;
        int from = Mth.floor(alongX ? box.minX : box.minZ);
        int to = Mth.floor((alongX ? box.maxX : box.maxZ) - 1.0E-4);
        int solid = 0;
        int all = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = Mth.floor(box.minY + 0.5); y <= Mth.floor(box.maxY - 1.0E-4); y++) {
            for (int a = from; a <= to; a++) {
                pos.set(alongX ? a : depth, y, alongX ? depth : a);
                BlockState state = level.getBlockState(pos);
                all++;
                if (state.getCollisionShape(level, pos).isEmpty()) {
                    continue;
                }
                if (!isNatural(state)) {
                    return false;
                }
                solid++;
            }
        }
        return all > 0 && solid >= all * MIN_COVER;
    }

    /** The face of {@code block} and its eight neighbours on it: natural full blocks with no block in front. */
    private static boolean isFlat(ServerLevel level, BlockPos block, Direction facing) {
        Direction side = facing.getClockWise();
        for (int dy = -1; dy <= 1; dy++) {
            for (int ds = -1; ds <= 1; ds++) {
                BlockPos pos = block.relative(side, ds).above(dy);
                BlockState state = level.getBlockState(pos);
                BlockPos front = pos.relative(facing);
                if (!isNatural(state) || !state.isCollisionShapeFullBlock(level, pos)
                        || !level.getBlockState(front).getCollisionShape(level, front).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Blocks of air under its feet, up to {@link #LOW_GAP}. */
    private static int gapBelow(ServerLevel level, BlockPos feet) {
        for (int i = 1; i <= LOW_GAP; i++) {
            BlockPos below = feet.below(i);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return i - 1;
            }
        }
        return LOW_GAP;
    }

    private static boolean loaded(ServerLevel level, Vec3 from, double radius) {
        return level.hasChunksAt(Mth.floor(from.x - radius), Mth.floor(from.z - radius), Mth.floor(from.x + radius),
                Mth.floor(from.z + radius));
    }
}
