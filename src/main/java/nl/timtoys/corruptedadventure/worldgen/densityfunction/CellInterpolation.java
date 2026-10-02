package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The value an {@code interpolated} marker around {@code argument} has at any position, also away from the block the
 * noise chunk is filling: the argument at the corners of the noise cell (4 × 8 × 4 blocks, the overworld's
 * {@code size_horizontal} 1 / {@code size_vertical} 2; corners on multiples of 4 and 8) and the same trilinear
 * interpolation as {@code NoiseChunk.NoiseInterpolator} (y, then x, then z). A marker itself may only be read for the
 * block being filled; this reads its shape elsewhere.
 * <p>
 * The corner values come from a per-thread cache ({@link ThreadOwned}); it only remembers results, a corner's value
 * is a pure function of its position.
 */
final class CellInterpolation {
    static final int CELL_WIDTH = 4;
    static final int CELL_HEIGHT = 8;
    private static final int CACHE_BITS = 10;

    private final DensityFunction argument;
    private final ThreadOwned<CornerCache> corners = new ThreadOwned<>(CornerCache::new);

    CellInterpolation(DensityFunction argument) {
        this.argument = argument;
    }

    double sample(double x, double y, double z) {
        int cx = Mth.floor(x / CELL_WIDTH);
        int cy = Mth.floor(y / CELL_HEIGHT);
        int cz = Mth.floor(z / CELL_WIDTH);
        double dx = x / CELL_WIDTH - cx;
        double dy = y / CELL_HEIGHT - cy;
        double dz = z / CELL_WIDTH - cz;
        CornerCache cache = corners.get();
        double v000 = cache.get(this, cx, cy, cz);
        double v100 = cache.get(this, cx + 1, cy, cz);
        double v010 = cache.get(this, cx, cy + 1, cz);
        double v110 = cache.get(this, cx + 1, cy + 1, cz);
        double v001 = cache.get(this, cx, cy, cz + 1);
        double v101 = cache.get(this, cx + 1, cy, cz + 1);
        double v011 = cache.get(this, cx, cy + 1, cz + 1);
        double v111 = cache.get(this, cx + 1, cy + 1, cz + 1);
        double xz00 = Mth.lerp(dy, v000, v010);
        double xz10 = Mth.lerp(dy, v100, v110);
        double xz01 = Mth.lerp(dy, v001, v011);
        double xz11 = Mth.lerp(dy, v101, v111);
        double z0 = Mth.lerp(dx, xz00, xz10);
        double z1 = Mth.lerp(dx, xz01, xz11);
        return Mth.lerp(dz, z0, z1);
    }

    private double corner(int cx, int cy, int cz) {
        return argument.compute(new DensityFunction.SinglePointContext(cx * CELL_WIDTH, cy * CELL_HEIGHT, cz * CELL_WIDTH));
    }

    /** Direct-mapped cache of corner values. */
    private static final class CornerCache {
        private static final int SIZE = 1 << CACHE_BITS;
        private final long[] keys = new long[SIZE];
        private final boolean[] filled = new boolean[SIZE];
        private final double[] values = new double[SIZE];

        double get(CellInterpolation owner, int cx, int cy, int cz) {
            long key = ((long) cx & 0x3FFFFFL) << 42 | ((long) cy & 0xFFFFFL) << 22 | (long) cz & 0x3FFFFFL;
            int slot = (int) (SeededHash.mix(key) >>> (64 - CACHE_BITS));
            if (!filled[slot] || keys[slot] != key) {
                filled[slot] = true;
                keys[slot] = key;
                values[slot] = owner.corner(cx, cy, cz);
            }
            return values[slot];
        }
    }
}
