package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.List;
import java.util.function.IntBinaryOperator;

/**
 * The bone carcass of {@code peat_moors} and {@code reed_marsh} ({@link BlightedLandmarks.Kind#BONE_CARCASS}): the
 * half rotten skeleton of something huge, 16–22 long, lying in the peat or the marsh. A spine of bone blocks along the
 * ground that sinks into it towards the tail, ribs as arches over it (4–6 out, 5–8 high, a gap at the top, some broken
 * off or gone), a hollow skull with eye holes and a jaw at the head. Moss on some bones, blighted glowcaps in the shade
 * under the ribs. Pure in the site and the pure ground heights.
 */
final class BoneCarcassShape {
    private static final int REACH = BlightedLandmarks.Kind.BONE_CARCASS.radius();

    private BoneCarcassShape() {
    }

    static LandmarkPlan build(BlightedLandmarks.Site site, IntBinaryOperator ground) {
        RandomSource random = new XoroshiroRandomSource(site.seed());
        LandmarkPlan plan = new LandmarkPlan();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double ux = Math.cos(angle);
        double uz = Math.sin(angle);
        int length = 18 + random.nextInt(5);
        double half = length / 2.0;
        // The skeleton runs from the tail's end (half + 3 behind) to the jaw's tip (half + 4.5 ahead): centre it.
        double shift = -0.75;
        double cx = site.x() + 0.5 + ux * shift;
        double cz = site.z() + 0.5 + uz * shift;
        Direction.Axis spineAxis = Math.abs(ux) >= Math.abs(uz) ? Direction.Axis.X : Direction.Axis.Z;
        Direction.Axis sideAxis = spineAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        double cageFrom = -0.3 * length;
        double cageTo = 0.35 * length;

        // The spine: on the ground along the ribcage, sinking into it towards the tail; gaps where vertebrae rotted.
        for (double t = -half - 3.0; t <= half; t += 0.5) {
            float rot = random.nextFloat();
            float process = random.nextFloat();
            int x = Mth.floor(cx + ux * t);
            int z = Mth.floor(cz + uz * t);
            int g = ground.applyAsInt(x, z);
            int y = t >= cageFrom ? g : g - 1 - Mth.floor((cageFrom - t) / 4.0);
            if (rot < 0.1F) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            put(plan, site, pos, bone.setValue(RotatedPillarBlock.AXIS, spineAxis));
            plan.feet.add(new LandmarkPlan.Foot(x, z, y, 1, bone));
            if (process < 0.3F && t >= cageFrom - 2.0 && Math.round(t * 2.0) % 3 == 0) {
                put(plan, site, pos.above(), bone);
            }
        }

        // The ribs: arches over the spine, highest in the middle of the cage, leaning back a little.
        for (double t = cageFrom; t <= cageTo; t += 2.0) {
            double f = (t - cageFrom) / (cageTo - cageFrom);
            double w = (5.0 + random.nextInt(2)) * (0.6 + 0.4 * Math.sin(Math.PI * f));
            double h = w + 1.0 + random.nextInt(2);
            int base = ground.applyAsInt(Mth.floor(cx + ux * t), Mth.floor(cz + uz * t));
            double gap = 0.15 + random.nextDouble() * 0.2;
            for (int side = -1; side <= 1; side += 2) {
                float fate = random.nextFloat();
                double cut = 0.45 + random.nextDouble() * 0.35;
                if (fate < 0.1F) {
                    continue;
                }
                double end = (Math.PI / 2.0 - gap) * (fate < 0.3F ? cut : 1.0);
                rib(plan, site, random, bone, sideAxis, cx, cz, ux, uz, t, side, w, h, base, end);
            }
        }

        skull(plan, site, random, bone, ground, cx + ux * (half + 1.5), cz + uz * (half + 1.5), ux, uz);
        dress(plan, random, cx, cz, ux, uz, cageFrom, cageTo);
        return plan;
    }

    private static void rib(LandmarkPlan plan, BlightedLandmarks.Site site, RandomSource random, BlockState bone, Direction.Axis sideAxis,
                            double cx, double cz, double ux, double uz, double t, int side, double w, double h, int base, double end) {
        BlockPos last = null;
        for (double a = 0.0; a <= end; a += 0.04) {
            double lateral = side * w * Math.cos(a);
            double up = h * Math.sin(a);
            double along = t - 0.12 * up;
            int x = Mth.floor(cx + ux * along - uz * lateral);
            int z = Mth.floor(cz + uz * along + ux * lateral);
            BlockPos pos = new BlockPos(x, base + Mth.floor(up), z);
            if (pos.equals(last)) {
                continue;
            }
            boolean steep = Math.abs(h * Math.cos(a)) > Math.abs(w * Math.sin(a));
            if (last == null) {
                plan.feet.add(new LandmarkPlan.Foot(x, z, pos.getY(), 1, bone));
            }
            last = pos;
            if (random.nextFloat() < 0.04F) {
                continue;
            }
            put(plan, site, pos, steep ? bone : bone.setValue(RotatedPillarBlock.AXIS, sideAxis));
        }
    }

    /** A hollow skull (a shell of an ellipsoid on the ground) with two eye holes, and a jaw reaching forward. */
    private static void skull(LandmarkPlan plan, BlightedLandmarks.Site site, RandomSource random, BlockState bone, IntBinaryOperator ground,
                              double sx, double sz, double ux, double uz) {
        int g = ground.applyAsInt(Mth.floor(sx), Mth.floor(sz));
        double cy = g + 1.5;
        for (int dy = -1; dy <= 4; dy++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dx = -4; dx <= 4; dx++) {
                    float rot = random.nextFloat();
                    double px = Mth.floor(sx) + dx + 0.5 - sx;
                    double pz = Mth.floor(sz) + dz + 0.5 - sz;
                    double along = px * ux + pz * uz;
                    double lateral = -px * uz + pz * ux;
                    double py = g + dy + 0.5 - cy;
                    double d = Mth.square(along / 3.2) + Mth.square(lateral / 2.3) + Mth.square(py / 2.2);
                    boolean eye = along > 0.6 && along < 2.4 && Math.abs(Math.abs(lateral) - 1.3) < 0.8 && py > 0.0 && py < 1.6;
                    if (d > 1.0 || d < 0.42 || eye || g + dy < g || rot < 0.05F) {
                        continue;
                    }
                    put(plan, site, new BlockPos(Mth.floor(sx) + dx, g + dy, Mth.floor(sz) + dz), bone);
                }
            }
        }
        Direction.Axis jawAxis = Math.abs(ux) >= Math.abs(uz) ? Direction.Axis.X : Direction.Axis.Z;
        for (int side = -1; side <= 1; side += 2) {
            for (double s = 2.5; s <= 4.5; s += 0.5) {
                int x = Mth.floor(sx + ux * s - uz * side * 1.4);
                int z = Mth.floor(sz + uz * s + ux * side * 1.4);
                if (random.nextFloat() < 0.15F) {
                    continue;
                }
                put(plan, site, new BlockPos(x, ground.applyAsInt(x, z), z), bone.setValue(RotatedPillarBlock.AXIS, jawAxis));
            }
        }
    }

    /** Moss on some bones; blighted glowcaps on the ground in the shade of the ribcage. */
    private static void dress(LandmarkPlan plan, RandomSource random, double cx, double cz, double ux, double uz, double from, double to) {
        for (BlockPos pos : List.copyOf(plan.objects.keySet())) {
            float roll = random.nextFloat();
            if (roll < 0.14F && !plan.solid(pos.above())) {
                plan.decor.add(new LandmarkPlan.Decor(pos.above(), Blocks.MOSS_CARPET.defaultBlockState(), pos));
            }
        }
        BlockState glowcap = ModBlocks.BLIGHTED_GLOWCAP.get().defaultBlockState();
        for (double t = from; t <= to; t += 1.0) {
            for (double lateral = -3.0; lateral <= 3.0; lateral += 1.0) {
                float roll = random.nextFloat();
                int x = Mth.floor(cx + ux * t - uz * lateral);
                int z = Mth.floor(cz + uz * t + ux * lateral);
                if (roll < 0.12F) {
                    plan.groundDecor.put(new BlockPos(x, 0, z), glowcap);
                }
            }
        }
    }

    /** Bone at {@code pos} unless it lies beyond the landmark's reach. */
    private static void put(LandmarkPlan plan, BlightedLandmarks.Site site, BlockPos pos, BlockState state) {
        if (Math.abs(pos.getX() - site.x()) <= REACH && Math.abs(pos.getZ() - site.z()) <= REACH) {
            plan.objects.putIfAbsent(pos, state);
        }
    }
}
