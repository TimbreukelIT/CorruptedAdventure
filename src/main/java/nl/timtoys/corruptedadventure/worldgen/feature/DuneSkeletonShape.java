package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.IntBinaryOperator;

/**
 * The giant skeleton of {@code dune_sea} ({@link Ring2FlatLandmarks.Kind#DUNE_SKELETON}): something huge that died in
 * the dunes, 22–30 long, half sunk into the sand. The spine lies in the sand along the ground and sinks away towards
 * the tail; the ribs rise out of the sand as arches (6–9 out, up to ~11 high, a gap at the top, some broken off or
 * gone); the skull lies half buried at the head with its eye holes and jaw; a few loose bones round it. In the style of
 * ring 4's {@link BoneCarcassShape}, but bigger, dry and without moss or fungi. Pure in the site and the pure ground.
 */
final class DuneSkeletonShape {
    /** The highest first open block over the ground the skeleton lies on: a valley or a lower flank, no crest. */
    static final int MAX_GROUND = 84;

    private DuneSkeletonShape() {
    }

    static LandmarkPlan build(Ring2FlatLandmarks.Site site, IntBinaryOperator ground) {
        RandomSource random = Ring2FlatLandmarks.random(site, 1);
        LandmarkPlan plan = new LandmarkPlan();
        int reach = site.kind().reach();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double ux = Math.cos(angle);
        double uz = Math.sin(angle);
        int length = 22 + random.nextInt(9);
        double half = length / 2.0;
        // Sunk 0 or 1 into the sand all along: at 1 the spine lies flush with the sand and only the ribs rise out.
        int sunk = random.nextInt(2);
        // From the tail's end (half + 4 behind) to the jaw's tip (half + 6 ahead): centred.
        double cx = site.x() + 0.5 - ux;
        double cz = site.z() + 0.5 - uz;
        Direction.Axis spineAxis = Math.abs(ux) >= Math.abs(uz) ? Direction.Axis.X : Direction.Axis.Z;
        Direction.Axis sideAxis = spineAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        double cageFrom = -0.3 * length;
        double cageTo = 0.35 * length;

        // The spine: along the ground, sinking away towards the tail; gaps where vertebrae are gone.
        for (double t = -half - 4.0; t <= half; t += 0.5) {
            float gone = random.nextFloat();
            float knob = random.nextFloat();
            int x = Mth.floor(cx + ux * t);
            int z = Mth.floor(cz + uz * t);
            int g = ground.applyAsInt(x, z);
            int y = g - sunk - (t >= cageFrom ? 0 : 1 + Mth.floor((cageFrom - t) / 3.0));
            if (gone < 0.08F) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            put(plan, site, reach, pos, bone.setValue(RotatedPillarBlock.AXIS, spineAxis));
            plan.feet.add(new LandmarkPlan.Foot(x, z, y, 1, bone));
            if (knob < 0.3F && t >= cageFrom - 2.0 && Math.round(t * 2.0) % 3 == 0) {
                put(plan, site, reach, pos.above(), bone);
            }
        }

        // The ribs: arches over the spine out of the sand, highest in the middle of the cage, leaning back a little.
        for (double t = cageFrom; t <= cageTo; t += 2.0) {
            double f = (t - cageFrom) / (cageTo - cageFrom);
            double w = (6.0 + random.nextInt(4)) * (0.55 + 0.45 * Math.sin(Math.PI * f));
            double h = w + 1.0 + random.nextInt(3);
            int base = ground.applyAsInt(Mth.floor(cx + ux * t), Mth.floor(cz + uz * t)) - 1 - sunk;
            double gap = 0.15 + random.nextDouble() * 0.2;
            for (int side = -1; side <= 1; side += 2) {
                float fate = random.nextFloat();
                double cut = 0.4 + random.nextDouble() * 0.35;
                if (fate < 0.12F) {
                    continue;
                }
                double end = (Math.PI / 2.0 - gap) * (fate < 0.37F ? cut : 1.0);
                rib(plan, site, reach, bone, sideAxis, cx, cz, ux, uz, t, side, w, h, base, end);
            }
        }

        skull(plan, site, reach, random, bone, ground, cx + ux * (half + 1.5), cz + uz * (half + 1.5), ux, uz);

        // Loose bones lying round it in the sand.
        int loose = 4 + random.nextInt(5);
        for (int i = 0; i < loose; i++) {
            double around = random.nextDouble() * Math.PI * 2.0;
            double distance = 4.0 + random.nextDouble() * 8.0;
            int x = Mth.floor(cx + Math.cos(around) * distance);
            int z = Mth.floor(cz + Math.sin(around) * distance);
            Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
            int y = ground.applyAsInt(x, z) - random.nextInt(2);
            put(plan, site, reach, new BlockPos(x, y, z), bone.setValue(RotatedPillarBlock.AXIS, axis));
            plan.feet.add(new LandmarkPlan.Foot(x, z, y, 1, bone));
        }
        return plan;
    }

    private static void rib(LandmarkPlan plan, Ring2FlatLandmarks.Site site, int reach, BlockState bone,
                            Direction.Axis sideAxis, double cx, double cz, double ux, double uz, double t, int side, double w, double h,
                            int base, double end) {
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
            // No gaps along a rib: a missing block would leave the rest of it hanging.
            put(plan, site, reach, pos, steep ? bone : bone.setValue(RotatedPillarBlock.AXIS, sideAxis));
        }
    }

    /** A hollow skull, its lower half in the sand, with two eye holes, and a jaw reaching forward. */
    private static void skull(LandmarkPlan plan, Ring2FlatLandmarks.Site site, int reach, RandomSource random, BlockState bone,
                              IntBinaryOperator ground, double sx, double sz, double ux, double uz) {
        int g = ground.applyAsInt(Mth.floor(sx), Mth.floor(sz));
        double cy = g + 0.5;
        for (int dy = -2; dy <= 3; dy++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dx = -5; dx <= 5; dx++) {
                    float worn = random.nextFloat();
                    double px = Mth.floor(sx) + dx + 0.5 - sx;
                    double pz = Mth.floor(sz) + dz + 0.5 - sz;
                    double along = px * ux + pz * uz;
                    double lateral = -px * uz + pz * ux;
                    double py = g + dy + 0.5 - cy;
                    double d = Mth.square(along / 3.8) + Mth.square(lateral / 2.7) + Mth.square(py / 2.5);
                    boolean eye = along > 0.8 && along < 2.8 && Math.abs(Math.abs(lateral) - 1.5) < 0.8 && py > 0.0 && py < 1.8;
                    if (d > 1.0 || d < 0.45 || eye || worn < 0.05F) {
                        continue;
                    }
                    put(plan, site, reach, new BlockPos(Mth.floor(sx) + dx, g + dy, Mth.floor(sz) + dz), bone);
                }
            }
        }
        Direction.Axis jawAxis = Math.abs(ux) >= Math.abs(uz) ? Direction.Axis.X : Direction.Axis.Z;
        for (int side = -1; side <= 1; side += 2) {
            for (double s = 3.0; s <= 5.5; s += 0.5) {
                int x = Mth.floor(sx + ux * s - uz * side * 1.6);
                int z = Mth.floor(sz + uz * s + ux * side * 1.6);
                if (random.nextFloat() < 0.15F) {
                    continue;
                }
                int y = ground.applyAsInt(x, z) - 1;
                put(plan, site, reach, new BlockPos(x, y, z), bone.setValue(RotatedPillarBlock.AXIS, jawAxis));
            }
        }
    }

    /** Bone at {@code pos} unless it lies beyond the landmark's reach. */
    private static void put(LandmarkPlan plan, Ring2FlatLandmarks.Site site, int reach, BlockPos pos, BlockState state) {
        if (Math.abs(pos.getX() - site.x()) <= reach && Math.abs(pos.getZ() - site.z()) <= reach) {
            plan.objects.putIfAbsent(pos, state);
        }
    }
}
