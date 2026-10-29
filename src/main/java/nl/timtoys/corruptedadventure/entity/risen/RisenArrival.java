package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.RisenConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareArrival;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How The Risen comes (design doc ring 3, "Komst (het koor zwelt aan)", and "Stap 2 voor ring 2–5"), for the director.
 * Omens: a low choir swelling, from the sky above its target or from the ground under its feet, and smoke and sparks
 * rising from the ash casts near it. Then it depends on the place: under the open sky it comes flying from behind, high,
 * its shadow sweeping over its target ({@link RisenState#PASSING}), and turns down in a dive; under a roof, in a cave or
 * a chasm, the ground in front of its target glows, melts to a pool ({@link RisenMeltPool}, sheltered: no lava where the
 * sky is open) and it rises from it; the pool sets to a lasting scar. No meltable ground: it rises without a pool; no
 * floor at all: it comes from the sky after all. The omens change no block. While it roams it now and then leaves a
 * smouldering patch where it passed ({@link RisenTrace}). Server-side only.
 */
public final class RisenArrival implements NightmareArrival {
    private static final int CHOIR_EVERY = 50;
    /** The last ticks of the omens: wing beats high above, or a rumble in the ground. */
    private static final int LAST_TICKS = 100;
    private static final int LAST_EVERY = 25;
    private static final double CHOIR_ABOVE = 14.0;
    private static final double CHOIR_BELOW = 3.0;
    private static final double WINGS_ABOVE = 30.0;
    private static final int CAST_SCAN_EVERY = 200;
    private static final int CAST_EVERY = 4;
    private static final int MAX_CASTS = 12;
    /** Casts are looked for this far above and below its target's feet (they stand on the ground it walks on). */
    private static final int CAST_DY = 8;
    /** Two cast blocks this close (each way) are one figure: it smokes from its top. */
    private static final int CAST_SPREAD = 2;
    /** A melt pool smaller than this is a poor place: another is looked for first. */
    private static final int MIN_POOL = 12;
    private static final int[] RISE_STEPS = {0, 2, -1, 4};
    private static final int[] RISE_ANGLES = {0, 30, -30, 60, -60, 90, -90, 135, -135, 180};
    private static final int[] FLOOR_DY = {0, -1, 1, -2, 2, -3, -4};
    private static final double[] PASS_SHARES = {1.0, 0.6, 0.3};
    private static final int[] PASS_ANGLES = {180, 150, -150, 120, -120, 90, -90, 0};
    /** Places followed at once; more means something forgot to cancel. */
    private static final int MAX_PLACES = 16;

    /** The ash casts that stir, per place of an arrival under way. */
    private final Map<BlockPos, List<BlockPos>> casts = new HashMap<>();

    @Override
    public int omenTicks() {
        return ServerConfig.RISEN.omenSeconds.get() * 20;
    }

    @Override
    public void omen(ServerLevel level, ServerPlayer target, BlockPos place, int tick) {
        RisenConfig config = ServerConfig.RISEN;
        float progress = Mth.clamp(tick / (float) Math.max(1, omenTicks()), 0.0F, 1.0F);
        boolean open = underOpenSky(level, target);
        if (tick % CHOIR_EVERY == 0) {
            double y = open ? target.getY() + CHOIR_ABOVE : target.getY() - CHOIR_BELOW;
            RisenEffects.omenChoir(level, target.getX(), y, target.getZ(), progress);
        }
        int left = omenTicks() - tick;
        if (left <= LAST_TICKS && left % LAST_EVERY == 0) {
            if (open) {
                RisenEffects.omenWingsAbove(level, target.getX(), target.getY() + WINGS_ABOVE, target.getZ());
            } else {
                RisenEffects.omenRumble(level, target.getX(), target.getY() - 1.0, target.getZ());
            }
        }
        if (tick % CAST_SCAN_EVERY == 0 || !casts.containsKey(place)) {
            if (casts.size() >= MAX_PLACES && !casts.containsKey(place)) {
                casts.clear();
            }
            casts.put(place, castsNear(level, target.blockPosition(), config.castRadius.get()));
        }
        if (tick % CAST_EVERY == 0) {
            for (BlockPos cast : casts.get(place)) {
                if (level.isLoaded(cast)) {
                    RisenEffects.castStirs(level, cast, progress);
                }
            }
        }
    }

    @Override
    @Nullable
    public Nightmare appear(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target, BlockPos place,
                            float strength) {
        if (!level.hasChunkAt(target.blockPosition())) {
            return null;
        }
        RisenConfig config = ServerConfig.RISEN;
        Nightmare risen = null;
        if (!underOpenSky(level, target)) {
            BlockPos floor = riseSpot(level, target, config);
            if (floor != null) {
                risen = rise(level, type, target, floor);
            }
        }
        if (risen == null) {
            risen = pass(level, type, target, config);
        }
        if (risen != null) {
            casts.remove(place);
        }
        return risen;
    }

    @Override
    public void cancelled(ServerLevel level, BlockPos place) {
        casts.remove(place);
    }

    @Override
    public void roamed(ServerLevel level, BlockPos from, BlockPos to) {
        RisenConfig config = ServerConfig.RISEN;
        if (level.hasChunkAt(to) && level.getRandom().nextDouble() < config.traceChance.get()) {
            RisenTrace.leave(level, to, config);
        }
    }

    /** Where the choir sings from and which way it comes: the sky seen from its target's eyes. */
    public static boolean underOpenSky(ServerLevel level, ServerPlayer target) {
        return level.canSeeSky(BlockPos.containing(target.getEyePosition()));
    }

    @Nullable
    private static Nightmare rise(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target,
                                  BlockPos floor) {
        Entity created = type.create(level);
        if (!(created instanceof TheRisen risen)) {
            return null;
        }
        Vec3 feet = Vec3.atBottomCenterOf(floor.above());
        float yaw = yawToward(feet, target.position());
        risen.moveTo(feet.x, feet.y, feet.z, yaw, 0.0F);
        risen.setYHeadRot(yaw);
        risen.setYBodyRot(yaw);
        if (!level.addFreshEntity(risen)) {
            return null;
        }
        // Only once it is in the world: a failed spawn must not leave molten ground behind.
        risen.riseFrom(level, floor);
        risen.setTarget(target);
        return risen;
    }

    @Nullable
    private static Nightmare pass(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target,
                                  RisenConfig config) {
        Entity created = type.create(level);
        if (!(created instanceof TheRisen risen)) {
            return null;
        }
        Vec3 start = passStart(level, target, config);
        Vec3 aim = target.position();
        float yaw = start.subtract(aim).horizontalDistanceSqr() < 1.0E-4 ? target.getYHeadRot() : yawToward(start, aim);
        risen.moveTo(start.x, start.y, start.z, yaw, 0.0F);
        risen.setYHeadRot(yaw);
        risen.setYBodyRot(yaw);
        if (!level.addFreshEntity(risen)) {
            return null;
        }
        risen.passOver(aim, config.passHeight.get());
        risen.setTarget(target);
        return risen;
    }

    /**
     * High up behind its target, where it can tick all the way to its turn {@code passBeyond} past its target:
     * {@code passDistance} back, else nearer and more to the side; last of all right above its target.
     */
    private static Vec3 passStart(ServerLevel level, ServerPlayer target, RisenConfig config) {
        float yaw = target.getYHeadRot();
        double ground = groundY(level, target.getX(), target.getZ());
        for (double share : PASS_SHARES) {
            double distance = config.passDistance.get() * share;
            if (distance < 1.0) {
                continue;
            }
            for (int angle : PASS_ANGLES) {
                float rad = (yaw + angle) * Mth.DEG_TO_RAD;
                // The look vector is (-sin, cos); 180° is behind.
                double x = target.getX() - Mth.sin(rad) * distance;
                double z = target.getZ() + Mth.cos(rad) * distance;
                BlockPos column = BlockPos.containing(x, target.getY(), z);
                // The turn lies straight on through its target.
                BlockPos turn = BlockPos.containing(target.getX() + (target.getX() - x) / distance * config.passBeyond.get(),
                        target.getY(), target.getZ() + (target.getZ() - z) / distance * config.passBeyond.get());
                if (level.isPositionEntityTicking(column) && level.isPositionEntityTicking(turn)
                        && level.getWorldBorder().isWithinBounds(column)) {
                    return new Vec3(x, passY(level, x, z, ground, config), z);
                }
            }
        }
        return new Vec3(target.getX(), passY(level, target.getX(), target.getZ(), ground, config), target.getZ());
    }

    private static double passY(ServerLevel level, double x, double z, double targetGround, RisenConfig config) {
        double y = Math.max(groundY(level, x, z), targetGround) + config.passHeight.get();
        return Math.min(y, level.getMaxBuildHeight() - 4.0);
    }

    /**
     * The floor block to rise from: ~{@code riseDistance} in front of its target (never so near that the pool reaches
     * it), preferably one it can see, under the roof, with a pool of at least {@link #MIN_POOL} blocks; else the best
     * found; null if there is no floor at all.
     */
    @Nullable
    static BlockPos riseSpot(ServerLevel level, ServerPlayer target, RisenConfig config) {
        int radius = config.meltPoolRadius.get();
        boolean melts = config.meltPool.get();
        float yaw = target.getYHeadRot();
        Vec3 eyes = target.getEyePosition();
        BlockPos best = null;
        int bestScore = -1;
        for (int step : RISE_STEPS) {
            double distance = Math.max(radius + 2, config.riseDistance.get() + step);
            for (int angle : RISE_ANGLES) {
                float rad = (yaw + angle) * Mth.DEG_TO_RAD;
                BlockPos floor = floorNear(level, target.getX() - Mth.sin(rad) * distance, target.getBlockY(),
                        target.getZ() + Mth.cos(rad) * distance);
                if (floor == null) {
                    continue;
                }
                boolean seen = sees(level, target, eyes, floor);
                boolean roofed = !level.canSeeSky(floor.above());
                int size = melts ? RisenMeltPool.plan(level, floor, radius, true).size() : 0;
                if (seen && roofed && (!melts || size >= MIN_POOL)) {
                    return floor;
                }
                int score = (seen ? 2000 : 0) + (roofed ? 1000 : 0) + size;
                if (score > bestScore) {
                    best = floor;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    /** A sturdy floor near {@code baseY} in column (x, z) with two free blocks above it, loaded and inside the border. */
    @Nullable
    private static BlockPos floorNear(ServerLevel level, double x, int baseY, double z) {
        for (int dy : FLOOR_DY) {
            BlockPos floor = BlockPos.containing(x, baseY + dy - 1, z);
            if (!level.hasChunkAt(floor) || !level.getWorldBorder().isWithinBounds(floor)) {
                return null;
            }
            if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                    && free(level, floor.above()) && free(level, floor.above(2))) {
                return floor;
            }
        }
        return null;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    private static boolean sees(ServerLevel level, ServerPlayer target, Vec3 eyes, BlockPos floor) {
        Vec3 spot = Vec3.atBottomCenterOf(floor.above()).add(0.0, 0.5, 0.0);
        return level.clip(new ClipContext(eyes, spot, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target))
                .getType() == HitResult.Type.MISS;
    }

    /**
     * The ash casts within {@code radius} of {@code centre}: per figure its top block (ash or smouldering ash standing
     * free on the surface, open on at least three sides; flat ash ground is not a cast). At most {@link #MAX_CASTS}.
     */
    public static List<BlockPos> castsNear(ServerLevel level, BlockPos centre, double radius) {
        List<BlockPos> found = new ArrayList<>();
        int r = Mth.floor(radius);
        for (int dx = -r; dx <= r && found.size() < MAX_CASTS; dx++) {
            for (int dz = -r; dz <= r && found.size() < MAX_CASTS; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                    continue;
                }
                BlockPos top = castTop(level, x, centre.getY(), z);
                if (top != null && found.stream().noneMatch(other -> near(other, top))) {
                    found.add(top);
                }
            }
        }
        return found;
    }

    /** The highest cast block in column (x, z) within {@link #CAST_DY} of {@code y} with nothing solid on it. */
    @Nullable
    private static BlockPos castTop(ServerLevel level, int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y + CAST_DY, z);
        for (int dy = CAST_DY; dy >= -CAST_DY; dy--, pos.move(Direction.DOWN)) {
            if (isCast(level, pos)) {
                BlockPos above = pos.above();
                return level.getBlockState(above).getCollisionShape(level, above).isEmpty() ? pos.immutable() : null;
            }
        }
        return null;
    }

    private static boolean isCast(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.ASH_BLOCK.get()) && !state.is(ModBlocks.SMOLDERING_ASH.get())) {
            return false;
        }
        if (level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
            return false;
        }
        int open = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(side);
            if (level.getBlockState(next).getCollisionShape(level, next).isEmpty()) {
                open++;
            }
        }
        return open >= 3;
    }

    private static boolean near(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= CAST_SPREAD && Math.abs(a.getZ() - b.getZ()) <= CAST_SPREAD;
    }

    private static double groundY(ServerLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(to.z - from.z, to.x - from.x)) - 90.0F;
    }
}
