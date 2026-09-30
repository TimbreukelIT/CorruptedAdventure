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
    private final double[] voidEdgeProgress = new double[keys.length];
    private RingLayout layout;

    RingColumnCache() {
    }

    /** Ring at a block column of the running server's world. */
    public static RingPosition ringAt(ResourceKey<Level> dimension, int x, int z) {
        return PER_THREAD.get().position(RingLayout.current(), RingLayout.scale(dimension), x, z);
    }

    /** {@link RingLayout#voidEdgeProgress} at a block column of the running server's world. */
    public static double voidEdgeProgress(ResourceKey<Level> dimension, int x, int z) {
        return PER_THREAD.get().voidEdgeProgress(RingLayout.current(), RingLayout.scale(dimension), x, z);
    }

    RingPosition position(RingLayout layout, int scale, int x, int z) {
        return positions[fill(layout, scale, x, z)];
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
            positions[index] = layout.sampleOverworld(overworldX, overworldZ);
            voidEdgeProgress[index] = layout.voidEdgeProgress(overworldX, overworldZ);
        }
        return index;
    }
}
