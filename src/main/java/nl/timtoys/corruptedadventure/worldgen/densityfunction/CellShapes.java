package nl.timtoys.corruptedadventure.worldgen.densityfunction;

/**
 * Small landforms on hash grids (change 39d): at most one per grid cell, kept inside its cell, so a column only looks at
 * its own cell. Pure: a function of the world salt ({@link VolcanicField#salt()}) and the cell. Heights are relative to
 * the column's ground; thin shapes (pillars) are solid intervals outside the terrain interpolation.
 */
final class CellShapes {
    private static final long K1 = 0x9E3779B97F4A7C15L;
    private static final long K2 = 0xC2B2AE3D27D4EB4FL;
    private static final long K3 = 0x165667B19E3779F9L;

    static final int HOODOO = 1;
    static final int MOUND = 2;
    static final int SINKHOLE = 3;
    static final int SEA_STACK = 4;
    static final int PRISM = 5;
    static final int CINDER = 6;
    static final int TITAN = 7;
    static final int RIB = 8;

    private static final int HOODOO_CELL = 12;
    private static final int MOUND_CELL = 36;
    private static final int SINKHOLE_CELL = 64;
    private static final int STACK_CELL = 48;
    /** Size of the hexagons of the column cliffs (centre to corner). */
    static final double HEX = 3.2;
    private static final double SQRT3 = Math.sqrt(3.0);

    private CellShapes() {
    }

    /** A pure hash of a grid cell and a shape kind. */
    static long cell(long salt, long i, long j, int kind) {
        return SeededHash.mix(salt + SeededHash.mix(i * K1 + j * K2 + kind * K3));
    }

    static double unit(long hash, int n) {
        return SeededHash.unit(hash, n);
    }

    /** Centre of a cell, jittered so that a shape of radius {@code margin} stays inside it. */
    private static double centre(long hash, int n, long index, int size, double margin) {
        return index * size + margin + unit(hash, n) * (size - 2.0 * margin);
    }

    /**
     * A hoodoo (ash pillar with a wider cap, 5–15 high) on a gully floor: writes [bottom, top] relative to the ground into
     * {@code out} and returns true, or false if this column has none.
     */
    static boolean hoodoo(long salt, double x, double z, double[] out) {
        long i = Math.floorDiv((long) Math.floor(x), HOODOO_CELL);
        long j = Math.floorDiv((long) Math.floor(z), HOODOO_CELL);
        long h = cell(salt, i, j, HOODOO);
        if (unit(h, 0) >= 0.55) {
            return false;
        }
        double radius = 1.3 + 1.4 * unit(h, 1);
        double cx = centre(h, 2, i, HOODOO_CELL, radius + 1.5);
        double cz = centre(h, 3, j, HOODOO_CELL, radius + 1.5);
        double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
        double height = 5.0 + 10.0 * unit(h, 4);
        if (d <= radius) {
            out[0] = -3.0;
            out[1] = height;
            return true;
        }
        if (d <= radius + 1.3) {
            out[0] = height - 1.5 - unit(h, 5);
            out[1] = height;
            return true;
        }
        return false;
    }

    /** A low fumarole mound (3–6 high, radius 5–9): its height at (x, z). */
    static double mound(long salt, double x, double z) {
        long i = Math.floorDiv((long) Math.floor(x), MOUND_CELL);
        long j = Math.floorDiv((long) Math.floor(z), MOUND_CELL);
        long h = cell(salt, i, j, MOUND);
        if (unit(h, 0) >= 0.45) {
            return 0.0;
        }
        double radius = 5.0 + 4.0 * unit(h, 1);
        double cx = centre(h, 2, i, MOUND_CELL, radius);
        double cz = centre(h, 3, j, MOUND_CELL, radius);
        double d2 = ((x - cx) * (x - cx) + (z - cz) * (z - cz)) / (radius * radius);
        if (d2 >= 1.0) {
            return 0.0;
        }
        double k = 1.0 - d2;
        return (3.0 + 3.0 * unit(h, 4)) * k * k;
    }

    /** A sinkhole (radius 4–8, 8–16 deep, steep walls): the depth of the cut at (x, z). */
    static double sinkhole(long salt, double x, double z) {
        long i = Math.floorDiv((long) Math.floor(x), SINKHOLE_CELL);
        long j = Math.floorDiv((long) Math.floor(z), SINKHOLE_CELL);
        long h = cell(salt, i, j, SINKHOLE);
        if (unit(h, 0) >= 0.3) {
            return 0.0;
        }
        double radius = 4.0 + 4.0 * unit(h, 1);
        double cx = centre(h, 2, i, SINKHOLE_CELL, radius + 6.0);
        double cz = centre(h, 3, j, SINKHOLE_CELL, radius + 6.0);
        double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)) / radius;
        if (d >= 1.0) {
            return 0.0;
        }
        return (8.0 + 8.0 * unit(h, 4)) * (1.0 - Math.pow(d, 6.0));
    }

    /** A sea stack (15–40 high, radius 3–7, tapering): its top above the ground at (x, z), or 0. */
    static double seaStack(long salt, double x, double z) {
        long i = Math.floorDiv((long) Math.floor(x), STACK_CELL);
        long j = Math.floorDiv((long) Math.floor(z), STACK_CELL);
        long h = cell(salt, i, j, SEA_STACK);
        if (unit(h, 0) >= 0.32) {
            return 0.0;
        }
        double radius = 3.0 + 4.0 * unit(h, 1);
        double cx = centre(h, 2, i, STACK_CELL, radius + 2.0);
        double cz = centre(h, 3, j, STACK_CELL, radius + 2.0);
        double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)) / radius;
        if (d >= 1.0) {
            return 0.0;
        }
        double height = 15.0 + 25.0 * unit(h, 4);
        // Steep sides with a rounded shoulder; the upper part narrows.
        return height * Math.min(1.0, 1.6 * (1.0 - d * d * d));
    }

    /**
     * The centre of the hexagon of the column cliffs that contains (x, z), written into {@code out} (x, z), plus a jitter
     * for its top (−2.5..+0.5) in {@code out[2]}.
     */
    static void hexagon(long salt, double x, double z, double[] out) {
        double q = (SQRT3 / 3.0 * x - z / 3.0) / HEX;
        double r = (2.0 / 3.0 * z) / HEX;
        double s = -q - r;
        double rq = Math.rint(q);
        double rr = Math.rint(r);
        double rs = Math.rint(s);
        double dq = Math.abs(rq - q);
        double dr = Math.abs(rr - r);
        double ds = Math.abs(rs - s);
        if (dq > dr && dq > ds) {
            rq = -rr - rs;
        } else if (dr > ds) {
            rr = -rq - rs;
        }
        out[0] = HEX * SQRT3 * (rq + rr / 2.0);
        out[1] = HEX * 1.5 * rr;
        out[2] = -2.5 + 3.0 * unit(cell(salt, (long) rq, (long) rr, PRISM), 0);
    }
}
