package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import javax.annotation.Nullable;
import java.util.function.IntBinaryOperator;

/**
 * The charred giant of {@code ashen_wastes} ({@link VolcanicLandmarks.Kind#CHARRED_GIANT}): the dead 2 × 2 trunk of a
 * tree the ash killed, 15–25 high with a jagged broken top, root flares of charred wood, 2–3 broken branches, the
 * snapped-off top lying beside it, its foot half in a dune of ash that trails away with the wind. Pure in the site and
 * the pure ground heights; null where the ground is not level.
 */
final class CharredGiantShape {
    private CharredGiantShape() {
    }

    @Nullable
    static VolcanicPlan build(VolcanicLandmarks.Site site, IntBinaryOperator ground) {
        int g = site.ground();
        // The trunk's middle is the corner (x + 1, z + 1) of its four columns.
        double cx = site.x() + 1.0;
        double cz = site.z() + 1.0;
        if (!VolcanicLandmarks.level(ground, cx, cz, 7, 3, g)) {
            return null;
        }
        RandomSource random = new XoroshiroRandomSource(site.seed());
        VolcanicPlan plan = new VolcanicPlan(site.x(), site.z(), site.kind().reach());
        BlockState log = ModBlocks.CHARRED_LOG.get().defaultBlockState();
        BlockState wood = ModBlocks.CHARRED_WOOD.get().defaultBlockState();
        int height = 15 + random.nextInt(11);

        // The trunk, two blocks into the ground, each column broken off at its own height; a splinter on the highest.
        int highest = Integer.MIN_VALUE;
        BlockPos splinter = null;
        for (int dz = 0; dz <= 1; dz++) {
            for (int dx = 0; dx <= 1; dx++) {
                int x = site.x() + dx;
                int z = site.z() + dz;
                int top = g + height - random.nextInt(4);
                for (int y = g - 2; y <= top; y++) {
                    plan.put(new BlockPos(x, y, z), y < g + 1 ? wood : log);
                }
                plan.foot(x, z, g - 2, 1, wood);
                if (top > highest) {
                    highest = top;
                    splinter = new BlockPos(x, top + 1, z);
                }
            }
        }
        plan.put(splinter, log);
        if (random.nextBoolean()) {
            plan.put(splinter.above(), log);
        }

        // Root flares: a buttress of wood beside each face of the trunk, 1–3 high, some gone.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int k = 0; k <= 1; k++) {
                if (random.nextFloat() < 0.3F) {
                    continue;
                }
                BlockPos foot = flarePos(site, side, k);
                int up = 1 + random.nextInt(3);
                for (int y = g - 2; y < g + up; y++) {
                    plan.put(new BlockPos(foot.getX(), y, foot.getZ()), wood);
                }
                plan.foot(foot.getX(), foot.getZ(), g - 2, 1, wood);
            }
        }

        // Broken branches: 2–3 stubs from the upper half, rising a little, at least a quarter turn apart.
        int branches = 2 + random.nextInt(2);
        double angle = random.nextDouble() * Math.PI * 2.0;
        for (int b = 0; b < branches; b++) {
            angle += Math.PI * 2.0 / branches + (random.nextDouble() - 0.5) * 0.6;
            int from = g + (int) (height * (0.45 + 0.4 * random.nextDouble()));
            int length = 3 + random.nextInt(4);
            double rise = 0.25 + random.nextDouble() * 0.45;
            branch(plan, log, cx, cz, from, angle, length, rise);
        }

        // The snapped-off top on the ground, half in the ash, pointing away from the trunk.
        double fall = random.nextDouble() * Math.PI * 2.0;
        int fallen = 3 + random.nextInt(3);
        double fx = cx + Math.cos(fall) * 3.5;
        double fz = cz + Math.sin(fall) * 3.5;
        Direction.Axis fallAxis = Math.abs(Math.cos(fall)) >= Math.abs(Math.sin(fall)) ? Direction.Axis.X : Direction.Axis.Z;
        for (int k = 0; k < fallen; k++) {
            int x = Mth.floor(fx + Math.cos(fall) * k);
            int z = Mth.floor(fz + Math.sin(fall) * k);
            int y = ground.applyAsInt(x, z) - (k == fallen - 1 ? 1 : 0);
            plan.put(new BlockPos(x, y, z), log.setValue(RotatedPillarBlock.AXIS, fallAxis));
            plan.foot(x, z, y, 1, ModBlocks.ASH_BLOCK.get().defaultBlockState());
        }

        dune(plan, random, cx, cz);
        return plan;
    }

    /** The column beside the trunk's face {@code side}, the {@code k}-th of the two along it. */
    private static BlockPos flarePos(VolcanicLandmarks.Site site, Direction side, int k) {
        return switch (side) {
            case NORTH -> new BlockPos(site.x() + k, 0, site.z() - 1);
            case SOUTH -> new BlockPos(site.x() + k, 0, site.z() + 2);
            case WEST -> new BlockPos(site.x() - 1, 0, site.z() + k);
            default -> new BlockPos(site.x() + 2, 0, site.z() + k);
        };
    }

    private static void branch(VolcanicPlan plan, BlockState log, double cx, double cz, int from, double angle, int length, double rise) {
        double ux = Math.cos(angle);
        double uz = Math.sin(angle);
        Direction.Axis axis = Math.abs(ux) >= Math.abs(uz) ? Direction.Axis.X : Direction.Axis.Z;
        BlockPos last = null;
        // From the trunk's surface (1.2 out of the middle) to the broken end.
        for (double s = 1.2; s <= 1.2 + length; s += 0.5) {
            BlockPos pos = new BlockPos(Mth.floor(cx + ux * s), from + Mth.floor((s - 1.2) * rise), Mth.floor(cz + uz * s));
            if (!pos.equals(last)) {
                plan.put(pos, last != null && pos.getY() != last.getY() ? log : log.setValue(RotatedPillarBlock.AXIS, axis));
                last = pos;
            }
        }
    }

    /** Ash round the foot: highest at the trunk, trailing downwind; layers on top for the fraction. */
    private static void dune(VolcanicPlan plan, RandomSource random, double cx, double cz) {
        double wind = random.nextDouble() * Math.PI * 2.0;
        double height = 2.0 + random.nextDouble() * 1.5;
        double radius = 5.5 + random.nextDouble() * 2.0;
        BlockState ash = ModBlocks.ASH_BLOCK.get().defaultBlockState();
        BlockState layer = ModBlocks.ASH_LAYER.get().defaultBlockState();
        int r = Mth.ceil(radius * 1.5);
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                int x = Mth.floor(cx) + dx;
                int z = Mth.floor(cz) + dz;
                double px = x + 0.5 - cx;
                double pz = z + 0.5 - cz;
                double d = Math.sqrt(px * px + pz * pz);
                double downwind = d < 1.0E-6 ? 0.0 : (px * Math.cos(wind) + pz * Math.sin(wind)) / d;
                double stretched = d / (radius * (1.0 + 0.5 * Math.max(0.0, downwind)));
                float jitter = random.nextFloat();
                if (stretched >= 1.0) {
                    continue;
                }
                double f = height * Math.pow(1.0 - stretched, 1.3) + (jitter - 0.5) * 0.4;
                if (f <= 0.1) {
                    continue;
                }
                int raise = Mth.floor(f);
                int layers = Mth.clamp((int) Math.round((f - raise) * 8.0), 0, 7);
                plan.cover(x, z, new VolcanicPlan.Cover(1, ash, raise, ash, layers == 0 ? null : layer.setValue(SnowLayerBlock.LAYERS, layers)));
            }
        }
    }
}
