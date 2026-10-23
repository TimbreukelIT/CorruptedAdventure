package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

/**
 * The clean missing chunks of ring 5's inner band (94, V1-A): whole 16 × 16 chunks gone down to below {@code min_y}, on the
 * chunk grid, about one per {@link #CELL_CHUNKS} × {@link #CELL_CHUNKS} chunks; besides the ragged fallen chunks of the
 * corrupt spots (83b, terrain). Pure: one decision per cell from the world's salt ({@link Ring5World#salt}) and the pure terrain.
 * <p>
 * The surface pass makes them (every stone block of the chunk becomes air, before the bedrock rule;
 * {@code corruptedadventure:ring_5_ground} part {@code missing_chunk}), so no feature of any chunk places into them
 * afterwards in an order that matters, and the void rims (95) see them as void.
 */
public final class Ring5MissingChunks {
    /** One missing chunk at most per cell of this many chunks a side, with {@link #CHANCE}. */
    public static final int CELL_CHUNKS = 20;
    static final double CHANCE = 0.9;
    /** The chunk lies this many chunks inside its cell. */
    static final int MARGIN_CHUNKS = 2;
    /** Blocks the chunk keeps from the band line (inside the inner band) and from ring 5's inner boundary zone. */
    static final double BAND_CLEARANCE = 48.0;
    static final double ZONE_CLEARANCE = 176.0;

    private static final long SALT = 0x31_5513_C4L;
    private static final ThreadLocal<Memo> MEMO = ThreadLocal.withInitial(Memo::new);

    private Ring5MissingChunks() {
    }

    /** Whether chunk (cx, cz) of the overworld is a clean missing chunk. */
    public static boolean missing(Ring5World world, int cx, int cz) {
        int i = Math.floorDiv(cx, CELL_CHUNKS);
        int j = Math.floorDiv(cz, CELL_CHUNKS);
        long chosen = MEMO.get().chunk(world, i, j);
        return chosen != Memo.NONE && chosen == pack(cx, cz);
    }

    /** The missing chunk of cell (i, j) as a packed chunk position, or {@link Memo#NONE}. Pure in the world and the cell. */
    static long decide(Ring5World world, int i, int j) {
        RandomSource r = new XoroshiroRandomSource(RandomSupport.mixStafford13(world.salt() ^ SALT),
                RandomSupport.mixStafford13(pack(i, j)));
        int span = CELL_CHUNKS - 2 * MARGIN_CHUNKS;
        int cx = i * CELL_CHUNKS + MARGIN_CHUNKS + r.nextInt(span);
        int cz = j * CELL_CHUNKS + MARGIN_CHUNKS + r.nextInt(span);
        if (r.nextDouble() >= CHANCE) {
            return Memo.NONE;
        }
        int x0 = cx << 4;
        int z0 = cz << 4;
        int mx = x0 + 8;
        int mz = z0 + 8;
        if (world.band().signedDistance(mx, mz) > -BAND_CLEARANCE || world.ringDepth(mx, mz) < ZONE_CLEARANCE) {
            return Memo.NONE;
        }
        // Never over a crack or a corrupt spot's hole: the chunk's corners, edges and middle all stand on ground.
        for (int dx = 0; dx <= 16; dx += 8) {
            for (int dz = 0; dz <= 16; dz += 8) {
                if (!world.field().solidGround(Math.min(x0 + dx, x0 + 15), Math.min(z0 + dz, z0 + 15))) {
                    return Memo.NONE;
                }
            }
        }
        return pack(cx, cz);
    }

    static long pack(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** The last cells asked on this thread (a chunk's surface pass asks its own and its neighbours' a lot). */
    private static final class Memo {
        static final long NONE = Long.MIN_VALUE;
        private static final int SIZE = 8;
        private final long[] cells = new long[SIZE];
        private final long[] chunks = new long[SIZE];
        private final Object[] owners = new Object[SIZE];
        private int next;

        long chunk(Ring5World world, int i, int j) {
            long cell = pack(i, j);
            for (int k = 0; k < SIZE; k++) {
                if (owners[k] == world && cells[k] == cell) {
                    return chunks[k];
                }
            }
            long chosen = decide(world, i, j);
            cells[next] = cell;
            chunks[next] = chosen;
            owners[next] = world;
            next = (next + 1) % SIZE;
            return chosen;
        }
    }
}
