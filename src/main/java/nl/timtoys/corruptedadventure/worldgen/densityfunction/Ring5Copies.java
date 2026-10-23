package nl.timtoys.corruptedadventure.worldgen.densityfunction;

/**
 * The mirrored doppelgangers of ring 5's inner band (package 93, V93-6/7 = A; the copy field of contract §4): at most
 * one pair per cell of {@link #CELL} × {@link #CELL} with chance {@link #CHANCE} (up to {@link #ATTEMPTS} places tried
 * per cell), a source square of {@link #SIZE} ×
 * {@link #SIZE} and, {@link #MIN_GAP}–{@link #MAX_GAP} further along x or z (centre to centre 64–160), its target: the
 * same ground mirrored about the plane halfway between them (the axis square to the line source → target), so from the
 * source one sees its mirror image. The target's outer {@link #MIN_EDGE}–{@link #MAX_EDGE} blocks blend from the own
 * ground into the copy; inside that the target is the source, block for block (shape, cracks, glitch shapes and the
 * caves of its own; not vanilla's caves, not the features). Only the shape; the field ({@link Ring5Field#copyPair})
 * keeps the pairs that lie wholly in quiet inner band. Pure: a function of the per-world salt and the cell.
 */
final class Ring5Copies {
    static final int CELL = 600;
    static final double CHANCE = 0.9;
    static final int SIZE = 48;
    /** Between the squares (blocks): centre to centre SIZE + gap, 64–160. */
    static final int MIN_GAP = 16;
    static final int MAX_GAP = 112;
    /** The target's blending edge (blocks, Chebyshev from its border). */
    static final int MIN_EDGE = 8;
    static final int MAX_EDGE = 12;
    /** Both squares keep this far inside the cell. */
    static final int MARGIN = 16;
    /** Places tried per cell: the inner band is only ~650 wide, so one random place often misses it. */
    static final int ATTEMPTS = 2;
    private static final int PAIR_SALT = 9310;

    private final long salt;

    Ring5Copies(long salt) {
        this.salt = salt;
    }

    static int cellOf(int block) {
        return Math.floorDiv(block, CELL);
    }

    /** The cell's draw: whether it has a pair at all. */
    boolean has(int i, int j) {
        return CellShapes.unit(CellShapes.cell(salt, i, j, PAIR_SALT), 0) < CHANCE;
    }

    /**
     * Try {@code attempt} (0 … {@link #ATTEMPTS} − 1) at the pair of cell (i, j), before the field's check; the field keeps
     * the first that lies in quiet inner band.
     */
    Pair pair(int i, int j, int attempt) {
        long h = CellShapes.cell(salt, i, j, PAIR_SALT + 1 + attempt);
        int gap = MIN_GAP + (int) Math.floor((MAX_GAP - MIN_GAP + 1) * CellShapes.unit(h, 1));
        int direction = (int) Math.floor(4 * CellShapes.unit(h, 2));
        boolean alongX = direction < 2;
        int sign = direction % 2 == 0 ? 1 : -1;
        int offset = SIZE + gap;
        int edge = MIN_EDGE + (int) Math.floor((MAX_EDGE - MIN_EDGE + 1) * CellShapes.unit(h, 3));
        int alongRoom = CELL - 2 * MARGIN - offset - SIZE;
        int acrossRoom = CELL - 2 * MARGIN - SIZE;
        int along = MARGIN + (int) Math.floor((alongRoom + 1) * CellShapes.unit(h, 4));
        int across = MARGIN + (int) Math.floor((acrossRoom + 1) * CellShapes.unit(h, 5));
        // The square nearer the cell's low side is the source when the copy runs towards +, else the target.
        int low = along;
        int high = along + offset;
        int sourceAlong = sign > 0 ? low : high;
        int targetAlong = sign > 0 ? high : low;
        int sx = (alongX ? sourceAlong : across) + i * CELL;
        int sz = (alongX ? across : sourceAlong) + j * CELL;
        int tx = (alongX ? targetAlong : across) + i * CELL;
        int tz = (alongX ? across : targetAlong) + j * CELL;
        return new Pair(sx, sz, tx, tz, alongX, edge);
    }

    /**
     * A doppelganger: the source square's low corner, the target's, the axis they lie along and the target's blending
     * edge. The mirror maps the target's column at {@code x} (along x) onto {@code sx + tx + SIZE − 1 − x}.
     */
    record Pair(int sx, int sz, int tx, int tz, boolean alongX, int edge) {
        boolean inTarget(int x, int z) {
            return x >= tx && x < tx + SIZE && z >= tz && z < tz + SIZE;
        }

        boolean inSource(int x, int z) {
            return x >= sx && x < sx + SIZE && z >= sz && z < sz + SIZE;
        }

        /** Whether the source or the target square overlaps the box (blocks, inclusive). */
        boolean touches(int minX, int minZ, int maxX, int maxZ) {
            return overlaps(sx, sz, minX, minZ, maxX, maxZ) || overlaps(tx, tz, minX, minZ, maxX, maxZ);
        }

        private static boolean overlaps(int x0, int z0, int minX, int minZ, int maxX, int maxZ) {
            return x0 <= maxX && x0 + SIZE > minX && z0 <= maxZ && z0 + SIZE > minZ;
        }

        /** The source column of a target column (the mirror image). */
        int sourceX(int x, int z) {
            return alongX ? sx + tx + SIZE - 1 - x : sx + (x - tx);
        }

        int sourceZ(int x, int z) {
            return alongX ? sz + (z - tz) : sz + tz + SIZE - 1 - z;
        }

        /** Chebyshev distance (blocks) of a target column from the target's border: 0 on the outermost ring. */
        int depth(int x, int z) {
            return Math.min(Math.min(x - tx, tx + SIZE - 1 - x), Math.min(z - tz, tz + SIZE - 1 - z));
        }

        /** Whether a target column is the copy itself (inside the blending edge). */
        boolean core(int x, int z) {
            return inTarget(x, z) && depth(x, z) >= edge;
        }

        /** The copy's weight at a target column: 0 at the border, 1 from the edge's width inwards (smoothstep). */
        double weight(int x, int z) {
            return Ring4Relief.smooth(depth(x, z) + 0.5, 0.0, edge);
        }
    }
}
