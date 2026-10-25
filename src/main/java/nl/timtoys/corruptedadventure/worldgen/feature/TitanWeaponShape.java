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
 * The titan's weapon of {@code titan_fields} ({@link VolcanicLandmarks.Kind#TITAN_WEAPON}): a giant's sword (blade,
 * crossguard with curled ends, wrapped grip, pommel) stuck point down, or a spear (shaft with bindings, leaf-shaped head)
 * planted butt down, half buried and slanting 20–30 blocks out of the ground at 10–33° from the vertical. Polished
 * deepslate and deepslate tiles, basalt and polished basalt; no valuable blocks. A few chips of cobbled deepslate lie
 * round it. Rasterised from the weapon's own frame (along it, across the blade, through it), thick enough to stay closed
 * at any slant. Pure in the site and the pure ground heights; null where the ground is not level.
 */
final class TitanWeaponShape {
    /** How far the weapon reaches into the ground along its axis, and the deepest block written. */
    private static final double BURIED = 5.0;
    private static final int WRITTEN_BELOW = 3;
    /** The farthest the weapon's top leans out from its foot (blocks). */
    private static final double MAX_LEAN = 12.0;

    private TitanWeaponShape() {
    }

    @Nullable
    static VolcanicPlan build(VolcanicLandmarks.Site site, IntBinaryOperator ground) {
        int g = site.ground();
        double ex = site.x() + 0.5;
        double ez = site.z() + 0.5;
        if (!VolcanicLandmarks.level(ground, ex, ez, 6, 3, g)) {
            return null;
        }
        RandomSource random = new XoroshiroRandomSource(site.seed());
        boolean sword = random.nextFloat() < 0.65F;
        int height = 20 + random.nextInt(11);
        double maxTilt = Math.atan(MAX_LEAN / height);
        double tilt = Math.toRadians(10.0) + random.nextDouble() * (maxTilt - Math.toRadians(10.0));
        double azimuth = random.nextDouble() * Math.PI * 2.0;
        double roll = random.nextDouble() * Math.PI;
        Frame frame = new Frame(tilt, azimuth, roll);
        double length = height / Math.cos(tilt);
        double split = length * (sword ? 0.62 : 0.66);
        double guard = 4.5 + random.nextDouble();

        VolcanicPlan plan = new VolcanicPlan(site.x(), site.z(), site.kind().reach());
        BlockState polished = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState tiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState basalt = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, frame.alongAxis());
        BlockState polishedBasalt = Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, frame.alongAxis());
        BlockState guardState = Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, frame.acrossAxis());
        int reach = Mth.ceil(Math.sin(tilt) * length + guard + 2.0);
        for (int y = g - WRITTEN_BELOW; y <= g + height + 3; y++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int dx = -reach; dx <= reach; dx++) {
                    double px = site.x() + dx + 0.5 - ex;
                    double py = y + 0.5 - g;
                    double pz = site.z() + dz + 0.5 - ez;
                    double u = frame.u(px, py, pz);
                    if (u < -BURIED || u > length + 0.5) {
                        continue;
                    }
                    double v = frame.v(px, py, pz);
                    double w = frame.w(px, py, pz);
                    BlockPos pos = new BlockPos(site.x() + dx, y, site.z() + dz);
                    BlockState state = sword ? sword(u, v, w, split, length, guard, polished, tiles, basalt, guardState)
                            : spear(u, v, w, split, length, polished, tiles, polishedBasalt);
                    if (state == null) {
                        continue;
                    }
                    // Chips out of the blade's edges.
                    double edge = sword ? 1.7 : 2.6;
                    if (Math.abs(v) > edge - 0.6 && u > 0.0 && u < split && chip(site.seed(), pos) < 0.06) {
                        continue;
                    }
                    plan.put(pos, state);
                }
            }
        }

        // Chips of cobbled deepslate round the foot.
        int chips = 4 + random.nextInt(4);
        for (int k = 0; k < chips; k++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double d = 2.5 + random.nextDouble() * 4.0;
            plan.groundDecor(Mth.floor(ex + Math.cos(a) * d), Mth.floor(ez + Math.sin(a) * d), Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        }
        return plan;
    }

    /** The sword's block at (u, v, w) of its frame, or null outside it. */
    @Nullable
    private static BlockState sword(double u, double v, double w, double split, double length, double guard, BlockState polished,
                                    BlockState tiles, BlockState grip, BlockState guardState) {
        double av = Math.abs(v);
        if (u <= split) {
            double half = 1.7;
            return av <= half && Math.abs(w) <= 0.9 + 0.2 * (1.0 - av / half) ? av < 0.55 ? tiles : polished : null;
        }
        // The crossguard, its ends curling down towards the blade.
        double curl = Math.max(0.0, av - (guard - 2.0)) * 0.7;
        if (u <= split + 1.9 - curl && u >= split - curl && av <= guard && Math.abs(w) <= 1.1) {
            return guardState;
        }
        double radial = Math.sqrt(v * v + w * w);
        double pommel = length - 1.4;
        if (u >= pommel - 1.6) {
            return Mth.square(u - pommel) + v * v + w * w <= 1.6 * 1.6 ? u > pommel + 0.6 ? tiles : polished : null;
        }
        if (u > split + 1.9) {
            // The grip, wrapped every three blocks.
            boolean wrap = Math.floorMod(Mth.floor(u - split), 3) == 0;
            return radial <= (wrap ? 1.25 : 0.95) ? wrap ? polished : grip : null;
        }
        return null;
    }

    /** The spear's block at (u, v, w) of its frame, or null outside it. */
    @Nullable
    private static BlockState spear(double u, double v, double w, double split, double length, BlockState polished, BlockState tiles,
                                    BlockState shaft) {
        double radial = Math.sqrt(v * v + w * w);
        if (u <= split) {
            boolean binding = u >= 1.5 && u <= 3.0 || u >= split - 1.8;
            return radial <= (binding ? 1.25 : 0.95) ? binding ? polished : shaft : null;
        }
        // The head: a leaf, widest a third of the way up, to a point.
        double f = (u - split) / (length - split);
        double half = 0.5 + 2.1 * Math.pow(f < 0.3 ? f / 0.3 : (1.0 - f) / 0.7, 0.8);
        double av = Math.abs(v);
        return av <= half && Math.abs(w) <= 0.9 + 0.25 * (1.0 - av / half) ? av < 0.55 ? tiles : polished : null;
    }

    private static double chip(long seed, BlockPos pos) {
        return VolcanicLandmarks.unit(VolcanicLandmarks.mix(seed ^ pos.asLong()), 11);
    }

    /** The weapon's frame: {@code a} along it (up), {@code b} across the blade, {@code c} through it. */
    private static final class Frame {
        private final double ax;
        private final double ay;
        private final double az;
        private final double bx;
        private final double by;
        private final double bz;
        private final double cx;
        private final double cy;
        private final double cz;

        Frame(double tilt, double azimuth, double roll) {
            ax = Math.sin(tilt) * Math.cos(azimuth);
            ay = Math.cos(tilt);
            az = Math.sin(tilt) * Math.sin(azimuth);
            // A horizontal vector square to a, and a × it; the blade's width turned by the roll between the two.
            double hx = -Math.sin(azimuth);
            double hz = Math.cos(azimuth);
            double kx = ay * hz;
            double ky = az * hx - ax * hz;
            double kz = -ay * hx;
            bx = hx * Math.cos(roll) + kx * Math.sin(roll);
            by = ky * Math.sin(roll);
            bz = hz * Math.cos(roll) + kz * Math.sin(roll);
            cx = ay * bz - az * by;
            cy = az * bx - ax * bz;
            cz = ax * by - ay * bx;
        }

        double u(double x, double y, double z) {
            return x * ax + y * ay + z * az;
        }

        double v(double x, double y, double z) {
            return x * bx + y * by + z * bz;
        }

        double w(double x, double y, double z) {
            return x * cx + y * cy + z * cz;
        }

        Direction.Axis alongAxis() {
            return axis(ax, ay, az);
        }

        Direction.Axis acrossAxis() {
            return axis(bx, by, bz);
        }

        private static Direction.Axis axis(double x, double y, double z) {
            if (Math.abs(y) >= Math.abs(x) && Math.abs(y) >= Math.abs(z)) {
                return Direction.Axis.Y;
            }
            return Math.abs(x) >= Math.abs(z) ? Direction.Axis.X : Direction.Axis.Z;
        }
    }
}
