package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.world.level.ChunkPos;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Table of per-column data for one chunk plus a rim of {@link #MARGIN} columns around it, filled lazily: for
 * density functions that run per block outside the interpolation. The noise chunk fills a cell per block in the order
 * y → x → z, so the column changes with every block and {@code cache_2d} (one remembered column) does not help; this
 * table gives one computation per column. The rim serves lookups of neighbouring columns from the chunk's edge.
 * <p>
 * Only a cache, per thread ({@link ThreadOwned}): a column's value is a pure function of x and z, so it does not
 * matter which thread or chunk asks first. Asking for a column outside the current window moves the window to that
 * column's chunk.
 */
final class ColumnTable<T> {
    static final int MARGIN = 8;
    private static final int SIDE = 16 + 2 * MARGIN;

    private final Supplier<T> factory;
    private final ThreadOwned<Page<T>> pages = new ThreadOwned<>(Page::new);

    ColumnTable(Supplier<T> factory) {
        this.factory = factory;
    }

    /** The column at x, z, filled by {@code filler} the first time it is asked for in this window. */
    T get(int x, int z, Filler<T> filler) {
        Page<T> page = pages.get();
        int lx = x - page.originX;
        int lz = z - page.originZ;
        if (page.key == ChunkPos.INVALID_CHUNK_POS || lx < 0 || lz < 0 || lx >= SIDE || lz >= SIDE) {
            page.moveTo(x >> 4, z >> 4);
            lx = x - page.originX;
            lz = z - page.originZ;
        }
        int index = lx + lz * SIDE;
        T column = page.columns[index];
        if (column == null) {
            column = factory.get();
            page.columns[index] = column;
        }
        if (!page.filled[index]) {
            filler.fill(column, x, z);
            page.filled[index] = true;
        }
        return column;
    }

    @FunctionalInterface
    interface Filler<T> {
        void fill(T column, int x, int z);
    }

    private static final class Page<T> {
        private long key = ChunkPos.INVALID_CHUNK_POS;
        private int originX;
        private int originZ;
        private final boolean[] filled = new boolean[SIDE * SIDE];
        @SuppressWarnings("unchecked")
        private final T[] columns = (T[]) new Object[SIDE * SIDE];

        void moveTo(int chunkX, int chunkZ) {
            key = ChunkPos.asLong(chunkX, chunkZ);
            originX = (chunkX << 4) - MARGIN;
            originZ = (chunkZ << 4) - MARGIN;
            Arrays.fill(filled, false);
        }
    }
}
