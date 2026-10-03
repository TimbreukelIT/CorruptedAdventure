package nl.timtoys.corruptedadventure.worldgen.surface;

/**
 * The pure core of {@code polygon_edge}: a Voronoi pattern of polygons over the block columns, without world access.
 * The sites lie on a triangular lattice of spacing {@code cell} (every other row shifted by half a cell), each moved by
 * up to {@code jitter} · {@code cell} along both axes (a hash per lattice point). The Voronoi cells of a triangular
 * lattice are hexagons, so the pattern reads as slightly irregular hexagons, {@code cell} across.
 * <p>
 * A column is an edge when the bisector between its nearest site and another site passes within {@code width} / 2 of
 * the column centre, measured along the axis that crosses the bisector most steeply. With width 1 every edge is exactly
 * one column wide per row or column it crosses: a closed line of single blocks (8-connected), whatever its direction.
 * <p>
 * A function of (hash, x, z) only; the small site cache does not change results. Not thread-safe (one instance per
 * chunk's surface pass).
 */
final class PolygonGrid {
    /** Lattice points around a column that can hold its nearest site or a bisector within reach (jitter <= 0.45). */
    private static final int REACH = 2;
    private static final double ROW = Math.sqrt(3.0) / 2.0;
    private static final int CACHE = 64;

    /** A seeded hash of a lattice point. */
    @FunctionalInterface
    interface Hash {
        long at(long i, long row);
    }

    /** Two independent uniform values in [0, 1) from a hash. */
    @FunctionalInterface
    interface Units {
        double unit(long hash, int n);
    }

    private final Hash hash;
    private final Units units;
    private final double cell;
    private final double jitter;
    private final double halfWidth;
    private final long[] cacheI = new long[CACHE];
    private final long[] cacheRow = new long[CACHE];
    private final double[] cacheX = new double[CACHE];
    private final double[] cacheZ = new double[CACHE];
    private final boolean[] cached = new boolean[CACHE];
    private double siteX;
    private double siteZ;

    PolygonGrid(Hash hash, Units units, double cell, double jitter, double width) {
        this.hash = hash;
        this.units = units;
        this.cell = cell;
        this.jitter = jitter;
        this.halfWidth = width / 2.0;
    }

    /** Whether the column at block x, z lies on an edge. */
    boolean isEdge(int x, int z) {
        double px = x + 0.5;
        double pz = z + 0.5;
        double rowHeight = cell * ROW;
        long row0 = (long) Math.floor(pz / rowHeight);
        // The nearest site first.
        double nearest = Double.MAX_VALUE;
        double nx = 0;
        double nz = 0;
        for (long row = row0 - REACH; row <= row0 + REACH; row++) {
            long i0 = (long) Math.floor(px / cell - shift(row));
            for (long i = i0 - REACH; i <= i0 + REACH; i++) {
                site(i, row);
                double d = sq(siteX - px) + sq(siteZ - pz);
                if (d < nearest) {
                    nearest = d;
                    nx = siteX;
                    nz = siteZ;
                }
            }
        }
        // Then the bisectors with every other site, along the axis that crosses each most steeply.
        for (long row = row0 - REACH; row <= row0 + REACH; row++) {
            long i0 = (long) Math.floor(px / cell - shift(row));
            for (long i = i0 - REACH; i <= i0 + REACH; i++) {
                site(i, row);
                double axis = Math.max(Math.abs(siteX - nx), Math.abs(siteZ - nz));
                if (axis == 0) {
                    continue;
                }
                double d = sq(siteX - px) + sq(siteZ - pz);
                if ((d - nearest) / (2.0 * axis) < halfWidth) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The lattice point nearest to the column: its polygon (for tests). */
    long owner(int x, int z) {
        double px = x + 0.5;
        double pz = z + 0.5;
        long row0 = (long) Math.floor(pz / (cell * ROW));
        double nearest = Double.MAX_VALUE;
        long owner = 0;
        for (long row = row0 - REACH; row <= row0 + REACH; row++) {
            long i0 = (long) Math.floor(px / cell - shift(row));
            for (long i = i0 - REACH; i <= i0 + REACH; i++) {
                site(i, row);
                double d = sq(siteX - px) + sq(siteZ - pz);
                if (d < nearest) {
                    nearest = d;
                    owner = i * 0x1_0000_0000L + (row & 0xFFFF_FFFFL);
                }
            }
        }
        return owner;
    }

    private static double shift(long row) {
        return (row & 1) == 0 ? 0.0 : 0.5;
    }

    /** Sets {@link #siteX}, {@link #siteZ} to the site of lattice point (i, row). */
    private void site(long i, long row) {
        int slot = (int) ((i & 7) | (row & 7) << 3);
        if (cached[slot] && cacheI[slot] == i && cacheRow[slot] == row) {
            siteX = cacheX[slot];
            siteZ = cacheZ[slot];
            return;
        }
        long h = hash.at(i, row);
        siteX = (i + shift(row)) * cell + (2.0 * units.unit(h, 0) - 1.0) * jitter * cell;
        siteZ = row * cell * ROW + (2.0 * units.unit(h, 1) - 1.0) * jitter * cell;
        cached[slot] = true;
        cacheI[slot] = i;
        cacheRow[slot] = row;
        cacheX[slot] = siteX;
        cacheZ[slot] = siteZ;
    }

    private static double sq(double v) {
        return v * v;
    }
}
