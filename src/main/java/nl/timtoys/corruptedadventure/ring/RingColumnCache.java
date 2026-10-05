package nl.timtoys.corruptedadventure.ring;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Arrays;

/**
 * Per-thread cache of {@link RingLayout} results per block column, for worldgen. Terrain and fluids ask for the ring
 * of every block, but the answer only depends on x and z, so this turns one noise lookup per block into roughly one
 * per column.
 * <p>
 * Direct-mapped on the low 5 bits of x and z: a chunk plus the cell corners just past its edge fit without evicting
 * each other. Entries are keyed by overworld coordinates, so a Nether column and the overworld column it maps onto
 * share an entry; their results are identical anyway.
 */
public final class RingColumnCache {
    private static final int BITS = 5;
    private static final int MASK = (1 << BITS) - 1;
    private static final ThreadLocal<RingColumnCache> PER_THREAD = ThreadLocal.withInitial(RingColumnCache::new);

    private final long[] keys = new long[1 << (2 * BITS)];
    private final RingPosition[] positions = new RingPosition[keys.length];
    private final double[] effectiveDistance = new double[keys.length];
    private final double[] voidEdgeProgress = new double[keys.length];
    private RingLayout layout;

    RingColumnCache() {
    }

    /** Ring at a block column of the running server's world. */
    public static RingPosition ringAt(ResourceKey<Level> dimension, int x, int z) {
        return PER_THREAD.get().position(RingLayout.current(), RingLayout.scale(dimension), x, z);
    }

    /**
     * {@link RingLayout#reaches}: whether {@code ring} can have weight within {@code margin} blocks of the dimension
     * ({@link RingLayout#reachMargin}) of a block column of the running server's world.
     */
    public static boolean reaches(ResourceKey<Level> dimension, Ring ring, int x, int z, double margin) {
        RingLayout layout = RingLayout.current();
        int scale = RingLayout.scale(dimension);
        return layout.reaches(ring, PER_THREAD.get().effectiveDistance(layout, scale, x, z), layout.reachMargin(margin, scale));
    }

    /**
     * {@link RingLayout#boundaryDepth} at a block column of the running server's world, in blocks of {@code dimension}:
     * how far the column lies inside {@code ring}, to its nearest boundary; negative outside.
     */
    public static double boundaryDepth(ResourceKey<Level> dimension, Ring ring, int x, int z) {
        RingLayout layout = RingLayout.current();
        int scale = RingLayout.scale(dimension);
        return layout.boundaryDepth(ring, PER_THREAD.get().effectiveDistance(layout, scale, x, z)) / scale;
    }

    /** {@link RingLayout#voidEdgeProgress} at a block column of the running server's world. */
    public static double voidEdgeProgress(ResourceKey<Level> dimension, int x, int z) {
        return PER_THREAD.get().voidEdgeProgress(RingLayout.current(), RingLayout.scale(dimension), x, z);
    }

    RingPosition position(RingLayout layout, int scale, int x, int z) {
        return positions[fill(layout, scale, x, z)];
    }

    double effectiveDistance(RingLayout layout, int scale, int x, int z) {
        return effectiveDistance[fill(layout, scale, x, z)];
    }

    double voidEdgeProgress(RingLayout layout, int scale, int x, int z) {
        return voidEdgeProgress[fill(layout, scale, x, z)];
    }

    private int fill(RingLayout layout, int scale, int x, int z) {
        if (this.layout != layout) {
            // A new world (singleplayer) has another seed or other radii.
            this.layout = layout;
            Arrays.fill(positions, null);
        }
        int index = (x & MASK) | (z & MASK) << BITS;
        long overworldX = (long) x * scale;
        long overworldZ = (long) z * scale;
        long key = overworldX << 32 | overworldZ & 0xFFFFFFFFL;
        if (positions[index] == null || keys[index] != key) {
            keys[index] = key;
            effectiveDistance[index] = layout.effectiveDistance(overworldX, overworldZ);
            positions[index] = layout.positionAt(effectiveDistance[index]);
            voidEdgeProgress[index] = layout.voidEdgeProgress(overworldX, overworldZ);
        }
        return index;
    }
}
