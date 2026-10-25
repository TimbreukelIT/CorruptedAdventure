package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import javax.annotation.Nullable;
import java.util.function.IntBinaryOperator;

/**
 * The ember giant of {@code ember_woods} ({@link VolcanicLandmarks.Kind#EMBER_GIANT}): an ember tree of 20–28 with a
 * 2 × 2 trunk of charred logs on a foot of charred wood twice as wide, a broad flat crown of ember leaves (radius 7–9)
 * on 4–6 branches, and a glowing hollow in the foot: an opening on one side, smouldering stone at the back and overhead,
 * smouldering ash on its floor and round the foot. The leaves are persistent (no decay, whatever the reach to a log).
 * The ember woods' own trees keep clear of it ({@link VolcanicLandmarkClearanceFilter}). Pure in the site and the pure
 * ground heights; null where the ground is not level.
 */
final class EmberGiantShape {
    /** The wide foot of charred wood round the trunk reaches this high above the ground. */
    private static final int FOOT = 5;
    /** The hollow's height. */
    private static final int HOLLOW = 3;

    private EmberGiantShape() {
    }

    @Nullable
    static VolcanicPlan build(VolcanicLandmarks.Site site, IntBinaryOperator ground) {
        int g = site.ground();
        double cx = site.x() + 1.0;
        double cz = site.z() + 1.0;
        if (!VolcanicLandmarks.level(ground, cx, cz, 6, 3, g)) {
            return null;
        }
        RandomSource random = new XoroshiroRandomSource(site.seed());
        VolcanicPlan plan = new VolcanicPlan(site.x(), site.z(), site.kind().reach());
        BlockState log = ModBlocks.CHARRED_LOG.get().defaultBlockState();
        BlockState wood = ModBlocks.CHARRED_WOOD.get().defaultBlockState();
        BlockState stone = ModBlocks.SMOLDERING_STONE.get().defaultBlockState();
        BlockState ash = ModBlocks.SMOLDERING_ASH.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int height = 20 + random.nextInt(9);
        Direction opening = Direction.Plane.HORIZONTAL.getRandomDirection(random);

        // The hollow first (its blocks win): air inside the trunk's 2 × 2 and in the opening, glowing walls round it.
        for (int dz = 0; dz <= 1; dz++) {
            for (int dx = 0; dx <= 1; dx++) {
                int x = site.x() + dx;
                int z = site.z() + dz;
                plan.put(new BlockPos(x, g - 1, z), ash);
                for (int y = g; y < g + HOLLOW; y++) {
                    plan.put(new BlockPos(x, y, z), air);
                }
                plan.put(new BlockPos(x, g + HOLLOW, z), stone);
            }
        }
        for (int k = 0; k <= 1; k++) {
            BlockPos front = beside(site, opening, k);
            BlockPos back = beside(site, opening.getOpposite(), k);
            for (int y = g; y < g + HOLLOW; y++) {
                plan.put(new BlockPos(front.getX(), y, front.getZ()), y < g + HOLLOW - 1 || k == 0 ? air : wood);
                plan.put(new BlockPos(back.getX(), y, back.getZ()), stone);
            }
            plan.put(new BlockPos(front.getX(), g - 1, front.getZ()), ash);
        }
        for (Direction side : new Direction[]{opening.getClockWise(), opening.getCounterClockWise()}) {
            for (int k = 0; k <= 1; k++) {
                BlockPos wall = beside(site, side, k);
                if (random.nextFloat() < 0.5F) {
                    plan.put(new BlockPos(wall.getX(), g + 1, wall.getZ()), stone);
                }
            }
        }

        // The foot: the ring round the 2 × 2 of charred wood up to FOOT, a block into the ground; then the trunk.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int k = 0; k <= 1; k++) {
                BlockPos ring = beside(site, side, k);
                int top = g + FOOT - (k == 0 ? 0 : random.nextInt(2));
                for (int y = g - 1; y <= top; y++) {
                    plan.put(new BlockPos(ring.getX(), y, ring.getZ()), wood);
                }
                plan.foot(ring.getX(), ring.getZ(), g - 1, 1, wood);
            }
        }
        for (int dz = 0; dz <= 1; dz++) {
            for (int dx = 0; dx <= 1; dx++) {
                int x = site.x() + dx;
                int z = site.z() + dz;
                for (int y = g + HOLLOW + 1; y <= g + height; y++) {
                    plan.put(new BlockPos(x, y, z), y <= g + FOOT ? wood : log);
                }
                plan.foot(x, z, g - 1, 1, wood);
            }
        }

        // The branches, from the upper trunk out into the crown, rising.
        int crown = g + height - 2;
        double radius = 7.0 + random.nextDouble() * 2.0;
        int branches = 4 + random.nextInt(3);
        double angle = random.nextDouble() * Math.PI * 2.0;
        for (int b = 0; b < branches; b++) {
            angle += Math.PI * 2.0 / branches + (random.nextDouble() - 0.5) * 0.5;
            int from = crown - 4 + random.nextInt(3);
            double ux = Math.cos(angle);
            double uz = Math.sin(angle);
            Direction.Axis axis = Math.abs(ux) >= Math.abs(uz) ? Direction.Axis.X : Direction.Axis.Z;
            double length = radius - 2.0 - random.nextDouble() * 1.5;
            for (double s = 1.2; s <= length; s += 0.5) {
                plan.put(new BlockPos(Mth.floor(cx + ux * s), from + Mth.floor(s * 0.45), Mth.floor(cz + uz * s)),
                        log.setValue(RotatedPillarBlock.AXIS, axis));
            }
        }

        // The crown: a broad, flattened, ragged blob of ember leaves round the top.
        BlockState leaves = ModBlocks.EMBER_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        double tall = 3.0 + random.nextDouble();
        int r = Mth.ceil(radius) + 1;
        for (int dy = -3; dy <= 4; dy++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    float jitter = random.nextFloat();
                    double px = Mth.floor(cx) + dx + 0.5 - cx;
                    double pz = Mth.floor(cz) + dz + 0.5 - cz;
                    // The crown sags at its edge: lower outside, higher over the trunk.
                    double h = Math.sqrt(px * px + pz * pz) / radius;
                    double py = dy + 0.5 + 1.5 * h * h;
                    double d = h * h + Mth.square(py / tall);
                    if (d <= 0.85 + jitter * 0.3) {
                        plan.put(new BlockPos(Mth.floor(cx) + dx, crown + dy, Mth.floor(cz) + dz), leaves);
                    }
                }
            }
        }

        // Smouldering ash in the soil round the foot.
        for (int dz = -4; dz <= 5; dz++) {
            for (int dx = -4; dx <= 5; dx++) {
                double d = Math.sqrt(Mth.square(site.x() + dx + 0.5 - cx) + Mth.square(site.z() + dz + 0.5 - cz));
                if (d < 4.5 && random.nextFloat() < 0.55F * (1.0 - d / 4.5) + 0.1F) {
                    plan.cover(site.x() + dx, site.z() + dz, new VolcanicPlan.Cover(1, ash, 0, ash, null));
                }
            }
        }
        return plan;
    }

    /** The column beside the trunk's face {@code side}, the {@code k}-th of the two along it. */
    private static BlockPos beside(VolcanicLandmarks.Site site, Direction side, int k) {
        return switch (side) {
            case NORTH -> new BlockPos(site.x() + k, 0, site.z() - 1);
            case SOUTH -> new BlockPos(site.x() + k, 0, site.z() + 2);
            case WEST -> new BlockPos(site.x() - 1, 0, site.z() + k);
            default -> new BlockPos(site.x() + 2, 0, site.z() + k);
        };
    }
}
