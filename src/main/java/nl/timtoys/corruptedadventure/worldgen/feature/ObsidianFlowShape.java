package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

/**
 * The obsidian flow of {@code volcanic_peaks} ({@link VolcanicLandmarks.Kind#OBSIDIAN_FLOW}): a solidified tongue of
 * obsidian, 30–60 long and 3–7 wide, that runs down a flank from its source (the site's anchor) along the steepest way
 * down, narrow at the vent and swelling into a thicker lobe at its toe. Its core is obsidian a block above the ground
 * with spots of crying obsidian, its edge a rim of blackstone. Written on the ground of each column (a
 * {@link VolcanicPlan.Cover}), so the tongue lies on the real slope block for block. Pure in the site and the pure
 * ground heights; null where the flank is too flat, too short or breaks off in a cliff.
 */
final class ObsidianFlowShape {
    /** The flow's path, one point per block along it. */
    record Point(double x, double z) {
    }

    private ObsidianFlowShape() {
    }

    @Nullable
    static VolcanicPlan build(VolcanicLandmarks.Site site, IntBinaryOperator ground) {
        RandomSource random = new XoroshiroRandomSource(site.seed());
        int length = 30 + random.nextInt(31);
        double width = 3.0 + random.nextInt(5);
        List<Point> path = path(site, ground, random, length);
        if (path == null) {
            return null;
        }
        VolcanicPlan plan = new VolcanicPlan(site.x(), site.z(), site.kind().reach());
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState blackstone = Blocks.BLACKSTONE.defaultBlockState();
        int n = path.size();
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (Point p : path) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minZ = Math.min(minZ, p.z());
            maxZ = Math.max(maxZ, p.z());
        }
        int pad = Mth.ceil(width * 0.75) + 2;
        for (int z = Mth.floor(minZ) - pad; z <= Mth.floor(maxZ) + pad; z++) {
            for (int x = Mth.floor(minX) - pad; x <= Mth.floor(maxX) + pad; x++) {
                // The nearest point of the path and how far along it lies.
                double best = Double.MAX_VALUE;
                int at = 0;
                for (int i = 0; i < n; i++) {
                    double d = Mth.square(path.get(i).x() - x - 0.5) + Mth.square(path.get(i).z() - z - 0.5);
                    if (d < best) {
                        best = d;
                        at = i;
                    }
                }
                double d = Math.sqrt(best);
                double f = at / (double) (n - 1);
                // Narrow at the vent, full in the middle, a swollen lobe at the toe; ragged edges.
                double half = width / 2.0 * (0.55 + 0.45 * Mth.clamp(f / 0.25, 0.0, 1.0)) * (1.0 + 0.35 * Mth.clamp((f - 0.82) / 0.18, 0.0, 1.0));
                half += (edge(site.seed(), x, z) - 0.5) * 0.9;
                if (d > half + 1.0) {
                    continue;
                }
                double roll = edge(site.seed() ^ 0x5EEDL, x, z);
                if (d > half) {
                    // The rim: blackstone, here and there a lump of levee a block high.
                    plan.cover(x, z, new VolcanicPlan.Cover(1, blackstone, roll < 0.35 ? 1 : 0, blackstone, null));
                } else {
                    boolean thick = d < half - 1.2 || f < 0.05;
                    int raise = (thick ? 1 : 0) + (f > 0.85 && d < half - 1.0 ? 1 : 0);
                    BlockState top = roll < 0.07 || at <= 1 && d < 1.2 ? crying : obsidian;
                    plan.cover(x, z, new VolcanicPlan.Cover(2, raise == 0 ? top : obsidian, raise, top, null));
                }
            }
        }
        return plan;
    }

    /**
     * The path from the source down the flank: each step a block along a heading that turns towards the steepest way
     * down (pure ground, over ±3 blocks), with a little wobble; null where the flank does not carry a flow of 30.
     */
    @Nullable
    static List<Point> path(VolcanicLandmarks.Site site, IntBinaryOperator ground, RandomSource random, int length) {
        double x = site.x() + 0.5;
        double z = site.z() + 0.5;
        double[] down = downhill(ground, x, z, 4);
        if (down == null) {
            return null;
        }
        double hx = down[0];
        double hz = down[1];
        int reach = site.kind().reach() - 6;
        List<Point> path = new ArrayList<>();
        path.add(new Point(x, z));
        int previous = ground.applyAsInt(Mth.floor(x), Mth.floor(z));
        int top = previous;
        int rising = 0;
        for (int step = 1; step < length; step++) {
            double[] local = downhill(ground, x, z, 3);
            if (local != null) {
                hx = hx * 0.7 + local[0] * 0.3;
                hz = hz * 0.7 + local[1] * 0.3;
            }
            double wobble = (random.nextDouble() - 0.5) * 0.16;
            double c = Math.cos(wobble);
            double s = Math.sin(wobble);
            double nx = hx * c - hz * s;
            double nz = hx * s + hz * c;
            double norm = Math.sqrt(nx * nx + nz * nz);
            hx = nx / norm;
            hz = nz / norm;
            double tx = x + hx;
            double tz = z + hz;
            if (Math.abs(tx - site.x()) > reach || Math.abs(tz - site.z()) > reach) {
                break;
            }
            int g = ground.applyAsInt(Mth.floor(tx), Mth.floor(tz));
            if (Math.abs(g - previous) > 2) {
                // A step of three or more within a block: a cliff, not a flank.
                return null;
            }
            rising = g > previous ? rising + 1 : 0;
            if (rising >= 2) {
                break;
            }
            x = tx;
            z = tz;
            previous = g;
            path.add(new Point(x, z));
        }
        int drop = top - ground.applyAsInt(Mth.floor(x), Mth.floor(z));
        if (path.size() < 30 || drop < Math.max(6, path.size() / 5)) {
            return null;
        }
        return path;
    }

    /** The unit vector of the steepest way down at (x, z) over ±{@code d} blocks, or null on level ground. */
    @Nullable
    private static double[] downhill(IntBinaryOperator ground, double x, double z, int d) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        double gx = ground.applyAsInt(bx + d, bz) - ground.applyAsInt(bx - d, bz);
        double gz = ground.applyAsInt(bx, bz + d) - ground.applyAsInt(bx, bz - d);
        double norm = Math.sqrt(gx * gx + gz * gz);
        if (norm < 1.0) {
            return null;
        }
        return new double[]{-gx / norm, -gz / norm};
    }

    /** A pure 0..1 per column: the ragged edge and the spots. */
    private static double edge(long seed, int x, int z) {
        return VolcanicLandmarks.unit(VolcanicLandmarks.mix(seed ^ ((long) x * 0x9E3779B97F4A7C15L) ^ ((long) z * 0xC2B2AE3D27D4EB4FL)), 7);
    }
}
