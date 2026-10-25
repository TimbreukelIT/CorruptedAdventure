package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import javax.annotation.Nullable;
import java.util.function.IntBinaryOperator;

/**
 * The whale skeleton of {@code dried_seabed} ({@link VolcanicLandmarks.Kind#WHALE_SKELETON}): the bones of something the
 * sea left behind, 25–35 long, half in the sand. The spine runs over the ribcage as the ridge of an arcade of ribs (arches
 * 5–7 out and 6–8 high that rise out of the sand, some broken or gone), sinks towards the tail into the sand and meets a
 * long, flat skull at the head with two jaw bones reaching forward on the ground; flippers beside the chest, loose
 * vertebrae behind the tail and sand drifted against the bones. Bone blocks only. Pure in the site and the pure ground
 * heights; null where the ground under it is not level.
 */
final class WhaleSkeletonShape {
    /** How deep the ribs' feet stand in the sand. */
    private static final int SINK = 2;

    private WhaleSkeletonShape() {
    }

    @Nullable
    static VolcanicPlan build(VolcanicLandmarks.Site site, IntBinaryOperator ground) {
        RandomSource random = new XoroshiroRandomSource(site.seed());
        double angle = random.nextDouble() * Math.PI * 2.0;
        double ux = Math.cos(angle);
        double uz = Math.sin(angle);
        int length = 25 + random.nextInt(11);
        double half = length / 2.0;
        double cx = site.x() + 0.5;
        double cz = site.z() + 0.5;
        int g = site.ground();
        for (double t = -half; t <= half; t += 4.0) {
            for (int side = -1; side <= 1; side++) {
                int x = Mth.floor(cx + ux * t - uz * side * 5.0);
                int z = Mth.floor(cz + uz * t + ux * side * 5.0);
                if (Math.abs(ground.applyAsInt(x, z) - g) > 3) {
                    return null;
                }
            }
        }
        VolcanicPlan plan = new VolcanicPlan(site.x(), site.z(), site.kind().reach());
        Bones bones = new Bones(plan, cx, cz, ux, uz);
        double skull = Math.round(0.27 * length);
        double cageFront = half - skull - 1.0;
        double cageBack = cageFront - Math.round(0.34 * length);
        double ribHeight = 6.0 + random.nextInt(3);
        double ribWidth = 5.0 + random.nextInt(2) + random.nextDouble() * 0.8;

        // The ribs: arches every two blocks, from the sand up to the spine, tallest and widest mid-chest.
        for (double t = cageBack; t <= cageFront; t += 2.0) {
            double f = (t - cageBack) / (cageFront - cageBack);
            double profile = 0.6 + 0.4 * Math.sin(Math.PI * f);
            double h = ribHeight * profile;
            double w = ribWidth * profile;
            for (int side = -1; side <= 1; side += 2) {
                float fate = random.nextFloat();
                if (fate < 0.12F) {
                    continue;
                }
                double end = fate < 0.32F ? (0.35 + random.nextDouble() * 0.35) * Math.PI / 2.0 : Math.PI / 2.0;
                bones.rib(random, t, side, w, h, g, end);
            }
        }

        // The spine: the ridge of the ribcage, down to the skull at the head, sinking into the sand towards the tail.
        double skullTop = 3.0;
        for (double t = -half; t <= half - skull + 0.5; t += 0.5) {
            double up;
            if (t >= cageBack && t <= cageFront) {
                double f = (t - cageBack) / (cageFront - cageBack);
                up = ribHeight * (0.6 + 0.4 * Math.sin(Math.PI * f));
            } else if (t > cageFront) {
                double f = (t - cageFront) / (half - skull - cageFront + 0.5);
                up = Mth.lerp(f, ribHeight * 0.6, skullTop);
            } else {
                double f = (cageBack - t) / (cageBack + half);
                up = Mth.lerp(f, ribHeight * 0.6, -SINK);
            }
            float rot = random.nextFloat();
            if (rot < 0.06F) {
                continue;
            }
            BlockPos pos = bones.along(t, 0.0, g + Mth.floor(up));
            bones.put(pos, bones.spine());
            // A dorsal process every few blocks over the chest and the back.
            if (t <= cageFront && Math.round(t * 2.0) % 5 == 0 && up > 0.0) {
                bones.put(pos.above(), bones.bone());
            }
            if (up <= 0.5) {
                plan.cover(pos.getX(), pos.getZ(), sand(1));
            }
        }

        bones.skull(random, half - skull / 2.0, skull / 2.0, g);
        // The jaw bones: on the ground from the back of the skull past its snout, bowed outwards.
        for (int side = -1; side <= 1; side += 2) {
            for (double t = half - skull; t <= half + 1.0; t += 0.5) {
                double f = (t - (half - skull)) / (skull + 1.0);
                double lateral = side * (2.6 + 1.6 * Math.sin(Math.PI * f) - 1.2 * f);
                if (random.nextFloat() < 0.08F) {
                    continue;
                }
                int x = bones.x(t, lateral);
                int z = bones.z(t, lateral);
                BlockPos pos = new BlockPos(x, ground.applyAsInt(x, z) - (f > 0.8 ? 1 : 0), z);
                bones.put(pos, bones.spine());
                plan.foot(x, z, pos.getY(), 1, bones.bone());
            }
        }
        // Flippers: a fan of three short bones on the ground beside the front of the chest.
        for (int side = -1; side <= 1; side += 2) {
            if (random.nextFloat() < 0.2F) {
                continue;
            }
            for (int finger = 0; finger < 3; finger++) {
                double sweep = 0.35 + finger * 0.3;
                for (double s = 0.0; s <= 3.0 + random.nextInt(2); s += 0.5) {
                    double t = cageFront - 1.0 - s * Math.sin(sweep);
                    double lateral = side * (ribWidth + 0.5 + s * Math.cos(sweep));
                    int x = bones.x(t, lateral);
                    int z = bones.z(t, lateral);
                    bones.put(new BlockPos(x, ground.applyAsInt(x, z), z), bones.side());
                    plan.foot(x, z, ground.applyAsInt(x, z), 1, bones.bone());
                }
            }
        }
        // Loose vertebrae behind the tail.
        for (int k = 0; k < 3 + random.nextInt(3); k++) {
            double t = -half - 1.0 - random.nextDouble() * 2.5;
            double lateral = (random.nextDouble() - 0.5) * 6.0;
            plan.groundDecor(bones.x(t, lateral), bones.z(t, lateral), bones.bone());
        }
        return plan;
    }

    /** Sand drifted a block high against the bones. */
    private static VolcanicPlan.Cover sand(int raise) {
        BlockState sand = Blocks.SAND.defaultBlockState();
        return new VolcanicPlan.Cover(1, sand, raise, sand, null);
    }

    /** The skeleton's frame: along its axis {@code t}, across it {@code lateral}. */
    private static final class Bones {
        private final VolcanicPlan plan;
        private final double cx;
        private final double cz;
        private final double ux;
        private final double uz;
        private final Direction.Axis spineAxis;
        private final Direction.Axis sideAxis;

        Bones(VolcanicPlan plan, double cx, double cz, double ux, double uz) {
            this.plan = plan;
            this.cx = cx;
            this.cz = cz;
            this.ux = ux;
            this.uz = uz;
            this.spineAxis = Math.abs(ux) >= Math.abs(uz) ? Direction.Axis.X : Direction.Axis.Z;
            this.sideAxis = spineAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        }

        int x(double t, double lateral) {
            return Mth.floor(cx + ux * t - uz * lateral);
        }

        int z(double t, double lateral) {
            return Mth.floor(cz + uz * t + ux * lateral);
        }

        BlockPos along(double t, double lateral, int y) {
            return new BlockPos(x(t, lateral), y, z(t, lateral));
        }

        BlockState bone() {
            return Blocks.BONE_BLOCK.defaultBlockState();
        }

        BlockState spine() {
            return bone().setValue(RotatedPillarBlock.AXIS, spineAxis);
        }

        BlockState side() {
            return bone().setValue(RotatedPillarBlock.AXIS, sideAxis);
        }

        void put(BlockPos pos, BlockState state) {
            plan.put(pos, state);
        }

        /** A rib from its foot in the sand ({@code a} = 0) up towards the spine ({@code a} = π/2), cut at {@code end}. */
        void rib(RandomSource random, double t, int side, double w, double h, int g, double end) {
            BlockPos last = null;
            for (double a = 0.0; a <= end; a += 0.04) {
                double lateral = side * w * Math.cos(a);
                double up = (h + SINK) * Math.sin(a) - SINK;
                BlockPos pos = along(t - 0.1 * Math.max(0.0, up), lateral, g + Mth.floor(up));
                if (pos.equals(last)) {
                    continue;
                }
                if (last == null || last.getY() < g && pos.getY() >= g) {
                    // Where the rib comes out of the sand: drifted sand round it.
                    plan.cover(pos.getX(), pos.getZ(), sand(1));
                }
                last = pos;
                if (pos.getY() < g - 1 || random.nextFloat() < 0.03F) {
                    continue;
                }
                boolean steep = Math.abs((h + SINK) * Math.cos(a)) > Math.abs(w * Math.sin(a));
                put(pos, steep ? bone() : side());
            }
        }

        /** A long, flat, hollow skull on the ground, narrowing to the snout, two eye holes at the back. */
        void skull(RandomSource random, double centre, double halfLength, int g) {
            int r = Mth.ceil(halfLength) + 1;
            BlockPos middle = along(centre, 0.0, g);
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        float rot = random.nextFloat();
                        int x = middle.getX() + dx;
                        int z = middle.getZ() + dz;
                        double px = x + 0.5 - cx;
                        double pz = z + 0.5 - cz;
                        double along = px * ux + pz * uz - centre;
                        double lateral = -px * uz + pz * ux;
                        double f = along / halfLength;
                        if (Math.abs(f) > 1.0) {
                            continue;
                        }
                        // Widest at the back (f = -1 .. -0.3), a long taper to the snout.
                        double width = 3.3 * Math.sqrt(Math.max(0.0, 1.0 - Mth.square(Math.max(0.0, f + 0.3) / 1.3)))
                                * Math.sqrt(Math.max(0.0, 1.0 - Mth.square(Math.min(0.0, f + 0.3) / 0.7)));
                        double tall = 2.6 * (1.0 - 0.45 * Math.max(0.0, f));
                        double py = dy + 0.5 + 0.5;
                        if (width < 0.5) {
                            continue;
                        }
                        double d = Mth.square(lateral / width) + Mth.square(py / tall);
                        boolean eye = f < -0.35 && f > -0.75 && Math.abs(Math.abs(lateral) - width * 0.8) < 0.7 && py > 0.8 && py < 2.0;
                        if (d > 1.0 || d < 0.45 && dy > -1 || eye || rot < 0.04F) {
                            continue;
                        }
                        put(new BlockPos(x, g + dy, z), bone());
                    }
                }
            }
        }
    }
}
