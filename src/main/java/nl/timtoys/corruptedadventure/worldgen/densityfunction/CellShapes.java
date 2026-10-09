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
    static final int COLLAPSE = 9;
    static final int RUBBLE = 10;

    private static final int HOODOO_CELL = 12;
    private static final int MOUND_CELL = 36;
    private static final int SINKHOLE_CELL = 64;
    private static final int STACK_CELL = 48;
    /** Collapses (39f): one per cell at most, about one per 250–350 blocks on the plains. */
    static final int COLLAPSE_CELL = 224;
    private static final double COLLAPSE_CHANCE = 0.85;
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
        double[] m = moundShape(salt, x, z);
        if (m == null) {
            return 0.0;
        }
        double d2 = ((x - m[0]) * (x - m[0]) + (z - m[1]) * (z - m[1])) / (m[2] * m[2]);
        if (d2 >= 1.0) {
            return 0.0;
        }
        double k = 1.0 - d2;
        return m[3] * k * k;
    }

    /** The mound of the cell of (x, z) as {centre x, centre z, radius, height}, or null if the cell has none. */
    static double[] moundShape(long salt, double x, double z) {
        long i = Math.floorDiv((long) Math.floor(x), MOUND_CELL);
        long j = Math.floorDiv((long) Math.floor(z), MOUND_CELL);
        long h = cell(salt, i, j, MOUND);
        if (unit(h, 0) >= 0.45) {
            return null;
        }
        double radius = 5.0 + 4.0 * unit(h, 1);
        return new double[]{centre(h, 2, i, MOUND_CELL, radius), centre(h, 3, j, MOUND_CELL, radius), radius, 3.0 + 3.0 * unit(h, 4)};
    }

    /** A sinkhole (radius 4–8, 8–16 deep, steep walls): the depth of the cut at (x, z). */
    static double sinkhole(long salt, double x, double z) {
        double[] s = sinkholeShape(salt, x, z);
        if (s == null) {
            return 0.0;
        }
        double d = Math.sqrt((x - s[0]) * (x - s[0]) + (z - s[1]) * (z - s[1])) / s[2];
        if (d >= 1.0) {
            return 0.0;
        }
        return s[3] * (1.0 - Math.pow(d, 6.0));
    }

    /** The sinkhole of the cell of (x, z) as {centre x, centre z, radius, depth}, or null if the cell has none. */
    static double[] sinkholeShape(long salt, double x, double z) {
        long i = Math.floorDiv((long) Math.floor(x), SINKHOLE_CELL);
        long j = Math.floorDiv((long) Math.floor(z), SINKHOLE_CELL);
        long h = cell(salt, i, j, SINKHOLE);
        if (unit(h, 0) >= 0.3) {
            return null;
        }
        double radius = 4.0 + 4.0 * unit(h, 1);
        return new double[]{centre(h, 2, i, SINKHOLE_CELL, radius + 6.0), centre(h, 3, j, SINKHOLE_CELL, radius + 6.0), radius,
                8.0 + 8.0 * unit(h, 4)};
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

    /**
     * A collapse (39f): a ragged sinkhole on the plains where the roof of a cave fell in, the surface's opening to the
     * caves (vanilla cave entrances stay sealed below the ring 3 surface, except under a collapse). The rim follows a few
     * harmonics around the centre; the walls go down in three steep steps to a floor of rubble; on part of the rim the lip
     * overhangs the first step; some have a shaft down towards the caves.
     */
    record Collapse(double x, double z, double radius, double depth, double a2, double p2, double a3, double p3, double a5,
                    double p5, double overhangPhase, boolean shaft, double shaftX, double shaftZ, double shaftRadius) {
        /** The rim's distance from the centre at angle {@code theta}. */
        double outline(double theta) {
            return radius * (1.0 + a2 * Math.cos(2.0 * theta + p2) + a3 * Math.cos(3.0 * theta + p3) + a5 * Math.cos(5.0 * theta + p5));
        }

        /** How far (blocks) the collapse and its overhangs reach from the centre along either axis. */
        double reach() {
            return radius * (1.0 + a2 + a3 + a5) + 8.0;
        }

        /** Normalised distance (0 at the centre, 1 on the rim); {@code warp} (blocks) roughens the outline. */
        double q(double px, double pz, double warp) {
            double dx = px - x;
            double dz = pz - z;
            return (Math.sqrt(dx * dx + dz * dz) + warp) / outline(Math.atan2(dz, dx));
        }

        /** The depth of the cut at normalised distance {@code q}: three steep steps from the rim down to the floor. */
        double cut(double q) {
            if (q >= 1.0) {
                return 0.0;
            }
            return depth * VolcanicMath.stair((1.0 - q) / 0.45 + 0.17, 3, 0.22);
        }

        /** Depth of the first step below the rim (where an overhanging lip ends). */
        double firstStep() {
            return depth / 3.0;
        }

        /** Whether the rim overhangs at the angle of (px, pz). */
        boolean overhangs(double px, double pz) {
            return Math.cos(Math.atan2(pz - z, px - x) + overhangPhase) > 0.15;
        }

        /** Whether (px, pz) lies in the shaft (if the collapse has one). */
        boolean inShaft(double px, double pz) {
            if (!shaft) {
                return false;
            }
            double dx = px - shaftX;
            double dz = pz - shaftZ;
            double d2 = dx * dx + dz * dz;
            double r = shaftRadius * (1.0 + 0.25 * Math.cos(3.0 * Math.atan2(dz, dx) + p3));
            return d2 < r * r;
        }
    }

    /** The collapse shape of cell (i, j), or null (pure; where it may stand is {@link #placeCollapse}). */
    static Collapse collapseShape(long salt, long i, long j) {
        long h = cell(salt, i, j, COLLAPSE);
        if (unit(h, 0) >= COLLAPSE_CHANCE) {
            return null;
        }
        double radius = 7.0 + 6.0 * unit(h, 1);
        double a2 = 0.10 + 0.15 * unit(h, 2);
        double a3 = 0.05 + 0.10 * unit(h, 3);
        double a5 = 0.03 + 0.05 * unit(h, 4);
        double margin = radius * (1.0 + a2 + a3 + a5) + 28.0;
        double cx = centre(h, 5, i, COLLAPSE_CELL, margin);
        double cz = centre(h, 6, j, COLLAPSE_CELL, margin);
        boolean shaft = unit(h, 7) < 0.45;
        double shaftAngle = unit(h, 8) * Math.PI * 2.0;
        double shaftOffset = 0.25 * radius * unit(h, 9);
        return new Collapse(cx, cz, radius, 12.0 + 10.0 * unit(h, 10), a2, unit(h, 11) * 6.3, a3, unit(h, 12) * 6.3, a5,
                unit(h, 13) * 6.3, unit(h, 14) * 6.3, shaft, cx + Math.cos(shaftAngle) * shaftOffset,
                cz + Math.sin(shaftAngle) * shaftOffset, 2.2 + 1.3 * unit(h, 15));
    }

    /**
     * The collapse of cell (i, j) where the terrain has one: on the plains of the ash wastes, ember woods, basalt flows,
     * peak range and sulfur fields, clear of volcanoes, rivers, rifts and abysses; else null.
     */
    static Collapse placeCollapse(VolcanicField field, int i, int j) {
        Collapse c = collapseShape(field.salt(), i, j);
        if (c == null) {
            return null;
        }
        VolcanicField.Regions r = field.regions(c.x(), c.z());
        double plains = r.ashenS() + r.emberS() + r.flowsS() + r.peaksS() + r.sulfurS();
        if (r.core() < 0.9 || r.fade() < 0.9 || r.seabed() > 0.05 || r.titan() > 0.3 || plains < 0.6) {
            return null;
        }
        if (!field.clearOfVolcanoes(c.x(), c.z(), c.reach() + 30.0) || field.nearAbyss(c.x(), c.z(), c.reach())
                || field.riverWithin(c.x(), c.z(), c.reach() + 12.0)) {
            return null;
        }
        // Not where an obsidian needle stands (its foot would hang over the pit).
        int ni = Math.floorDiv((int) Math.floor(c.x()), Landmarks.NEEDLE_CELL);
        int nj = Math.floorDiv((int) Math.floor(c.z()), Landmarks.NEEDLE_CELL);
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                Landmarks.Needle n = field.needle(ni + di, nj + dj);
                if (n != null && Math.hypot(n.x() - c.x(), n.z() - c.z()) < c.reach() + n.radius() + 6.0) {
                    return null;
                }
            }
        }
        return field.riftCut(c.x(), c.z(), new Ring3Relief.Detail()) > 2.0 ? null : c;
    }
}
