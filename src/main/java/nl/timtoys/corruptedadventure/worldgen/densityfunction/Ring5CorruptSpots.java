package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * The corrupt spots of ring 5's inner band (83b, teststop 6a): about one per {@link #CELL_CHUNKS} × {@link #CELL_CHUNKS}
 * chunks, each {@link #MIN_CHUNKS}–{@link #MAX_CHUNKS} touching chunks where the world looks wrongly rendered: whole
 * 16 × 16 columns lifted or sunk {@link #MIN_LIFT}–{@link #MAX_LIFT} with hard walls on the chunk borders, and one
 * chunk fallen away into the void, inside a ragged rim of {@link #MIN_RAG}–{@link #MAX_RAG} blocks. Only the shape; the
 * field ({@link Ring5Field#corruptSpot}) keeps the spots that lie wholly inside the inner band. Pure: a function of the
 * per-world salt, the hills' noise (the rim) and the cell.
 */
final class Ring5CorruptSpots {
    static final int CHUNK = 16;
    /** One spot at most per cell of this many chunks a side (240 blocks), with this chance. */
    static final int CELL_CHUNKS = 15;
    static final double CHANCE = 0.9;
    static final int MIN_CHUNKS = 2;
    static final int MAX_CHUNKS = 5;
    /**
     * The first chunk lies this many chunks inside its cell; the spot grows from it by at most {@code MAX_CHUNKS − 1}, so
     * every chunk of a spot keeps at least one chunk (16 blocks) from its cell's edge: wider than the dam (6) and the
     * layer margin (6), so a column only ever needs the spot of its own cell.
     */
    static final int MARGIN_CHUNKS = MAX_CHUNKS;
    /** How far a shifted chunk lies above or below its ground (whole blocks, either sign). */
    static final int MIN_LIFT = 8;
    static final int MAX_LIFT = 30;
    /** The fallen chunk's rim of solid ground inside it, per column, from the hills' noise at this scale. */
    static final int MIN_RAG = 1;
    static final int MAX_RAG = 3;
    static final double RAG_SCALE = 8.0;
    /** Tries to grow a spot to its size (a neighbour already in the spot is a lost try). */
    private static final int GROW_TRIES = 24;
    private static final int SPOT_SALT = 8310;

    private final long salt;
    private final DensityFunction.NoiseHolder hills;

    Ring5CorruptSpots(long salt, DensityFunction.NoiseHolder hills) {
        this.salt = salt;
        this.hills = hills;
    }

    /** The spot cell of a block coordinate. */
    static int cellOf(int block) {
        return Math.floorDiv(Math.floorDiv(block, CHUNK), CELL_CHUNKS);
    }

    /** The spot of cell (i, j) before the field's check, or null. */
    @Nullable
    Spot spot(int i, int j) {
        long h = CellShapes.cell(salt, i, j, SPOT_SALT);
        if (CellShapes.unit(h, 0) >= CHANCE) {
            return null;
        }
        int size = MIN_CHUNKS + (int) Math.floor((MAX_CHUNKS - MIN_CHUNKS + 1) * CellShapes.unit(h, 1));
        int span = CELL_CHUNKS - 2 * MARGIN_CHUNKS;
        int[] xs = new int[size];
        int[] zs = new int[size];
        xs[0] = i * CELL_CHUNKS + MARGIN_CHUNKS + (int) Math.floor(span * CellShapes.unit(h, 2));
        zs[0] = j * CELL_CHUNKS + MARGIN_CHUNKS + (int) Math.floor(span * CellShapes.unit(h, 3));
        int count = 1;
        for (int t = 0; t < GROW_TRIES && count < size; t++) {
            int from = (int) Math.floor(count * CellShapes.unit(h, 10 + 2 * t));
            int side = (int) Math.floor(4 * CellShapes.unit(h, 11 + 2 * t));
            int cx = xs[from] + (side == 0 ? 1 : side == 1 ? -1 : 0);
            int cz = zs[from] + (side == 2 ? 1 : side == 3 ? -1 : 0);
            if (indexOf(xs, zs, count, cx, cz) < 0) {
                xs[count] = cx;
                zs[count] = cz;
                count++;
            }
        }
        int[] shifts = new int[count];
        int fallen = (int) Math.floor(count * CellShapes.unit(h, 4));
        for (int k = 0; k < count; k++) {
            long g = CellShapes.cell(salt, xs[k], zs[k], SPOT_SALT + 1);
            int amount = MIN_LIFT + (int) Math.floor((MAX_LIFT - MIN_LIFT + 1) * CellShapes.unit(g, 0));
            shifts[k] = k == fallen ? 0 : CellShapes.unit(g, 1) < 0.5 ? amount : -amount;
        }
        return new Spot(Arrays.copyOf(xs, count), Arrays.copyOf(zs, count), shifts, fallen);
    }

    private static int indexOf(int[] xs, int[] zs, int count, int cx, int cz) {
        for (int k = 0; k < count; k++) {
            if (xs[k] == cx && zs[k] == cz) {
                return k;
            }
        }
        return -1;
    }

    /** The fallen chunk's solid rim at a column inside it: {@link #MIN_RAG}–{@link #MAX_RAG} blocks. */
    int rag(int x, int z) {
        double n = 0.5 + 1.5 * hills.getValue(x * RAG_SCALE + 7000.5, 0.0, z * RAG_SCALE - 7000.5);
        return MIN_RAG + (int) Math.floor((MAX_RAG - MIN_RAG + 1) * Mth.clamp(n, 0.0, 0.999));
    }

    /**
     * How far (blocks) a column lies from the void of a spot's fallen chunk: at most 0 exactly on its void columns, never
     * more than the true distance (outside the chunk: the chunk's Chebyshev distance plus the thinnest rim), so a dam
     * measured with it is at least as wide as asked.
     */
    double holeDistance(Spot spot, int x, int z) {
        int lx = x - spot.chunkX[spot.fallen] * CHUNK;
        int lz = z - spot.chunkZ[spot.fallen] * CHUNK;
        int ex = lx < 0 ? -lx : lx >= CHUNK ? lx - CHUNK + 1 : 0;
        int ez = lz < 0 ? -lz : lz >= CHUNK ? lz - CHUNK + 1 : 0;
        if (ex > 0 || ez > 0) {
            return Math.max(ex, ez) + MIN_RAG;
        }
        int inside = Math.min(Math.min(lx, CHUNK - 1 - lx), Math.min(lz, CHUNK - 1 - lz));
        return rag(x, z) - inside;
    }

    /**
     * A corrupt spot: its chunks (chunk coordinates), the shift of each (whole blocks; 0 for the fallen one) and the
     * index of the fallen chunk.
     */
    record Spot(int[] chunkX, int[] chunkZ, int[] shift, int fallen) {
        int size() {
            return chunkX.length;
        }

        /** The index of the spot's chunk holding the column, or −1. */
        int chunkAt(int x, int z) {
            return indexOf(chunkX, chunkZ, chunkX.length, Math.floorDiv(x, CHUNK), Math.floorDiv(z, CHUNK));
        }

        /** Whether one of the spot's chunks overlaps the box (blocks, inclusive). */
        boolean touches(int minX, int minZ, int maxX, int maxZ) {
            for (int k = 0; k < chunkX.length; k++) {
                int x0 = chunkX[k] * CHUNK;
                int z0 = chunkZ[k] * CHUNK;
                if (x0 <= maxX && x0 + CHUNK > minX && z0 <= maxZ && z0 + CHUNK > minZ) {
                    return true;
                }
            }
            return false;
        }

        /** Whether the column lies within {@code margin} blocks (Chebyshev) of one of the spot's chunks. */
        boolean near(int x, int z, int margin) {
            for (int k = 0; k < chunkX.length; k++) {
                int x0 = chunkX[k] * CHUNK;
                int z0 = chunkZ[k] * CHUNK;
                if (x >= x0 - margin && x < x0 + CHUNK + margin && z >= z0 - margin && z < z0 + CHUNK + margin) {
                    return true;
                }
            }
            return false;
        }
    }
}
