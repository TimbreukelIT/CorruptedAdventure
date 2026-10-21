package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * The 2D shape of the fracture zone 4→5 (package 85, V7-A, V8-A): one place for its numbers and the per-column
 * decisions, read by {@link ZoneFracture} (the per-block cut after the ring blend) and {@link ZoneFractureValue} (the
 * column value {@code zone_4_5/overworld/fracture}). Pure: a function of the layout, the per-world salt and x, z.
 * <p>
 * The ground breaks along the edges of a jittered grid stretched along the ring ({@link #CELL_ALONG} ×
 * {@link #CELL_RADIAL}, bent by {@link #WARP}): a network of long cracks along and slantwise across the ring, 2–8 wide,
 * whose floor lies 30–60 under the ground. Along every crack a crossing every {@link #CROSSING_MIN}–{@link #CROSSING_MAX}
 * blocks where it stays closed (a collapsed slab level with the ground). Some cells are pushed up as floes (8–30, tilted
 * up to 30°); loose chunks float above the cracks, mostly in the outer half; every ~650 blocks a raised flat floe is a
 * landmark (the split rock, W-biomes 86c). The zone's first and last {@link #CALM} blocks stay whole, so outside the zone
 * and on its edges the terrain is exactly the ring blend.
 * <p>
 * Positions along the ring are the arc at the zone's middle radius, wrapped round the loop (no seam at ±π).
 */
final class ZoneFractureShape {
    /** No fracture in the zone's first and last blocks (both edges), growing to full over {@link #RAMP} blocks. */
    static final double CALM = 16.0;
    static final double RAMP = 32.0;
    /** The grid: cells this long along the ring and this deep across it. */
    static final double CELL_ALONG = 48.0;
    static final double CELL_RADIAL = 26.0;
    /** How far (blocks) the grid is bent, and its scale (blocks per wiggle, about). */
    static final double WARP = 7.0;
    static final double WARP_SCALE = 1.0 / 56.0;
    /** A crack's half width at full strength (blocks): 1–4, so 2–8 wide. */
    static final double MIN_HALF = 1.0;
    static final double MAX_HALF = 4.0;
    /** A crack's floor this far under the ground (blocks), rough by up to {@link #FLOOR_ROUGH}. */
    static final int MIN_DEPTH = 30;
    static final int MAX_DEPTH = 60;
    static final double FLOOR_ROUGH = 2.0;
    /** A crossing every 24–34 blocks along each crack, closed over 2 × {@link #CROSSING_HALF} blocks. */
    static final double CROSSING_MIN = 24.0;
    static final double CROSSING_MAX = 34.0;
    static final double CROSSING_HALF = 2.0;
    /** Raised floes: this share of the cells at full strength, 8–30 up, tilted up to 30°. */
    static final double FLOE_CHANCE = 0.2;
    static final double FLOE_MIN_LIFT = 8.0;
    static final double FLOE_MAX_LIFT = 30.0;
    static final double FLOE_MAX_TILT = 30.0;
    /** How deep (blocks under the ground) a raised floe's extra rock reaches, so it stands on the ground. */
    static final double FLOE_ROOT = 12.0;
    /** Loose chunks above the cracks: one per cell at most, Ø 4–16, 4–16 above the ground, ≥ 3 thick. */
    static final int CHUNK_CELL = 40;
    static final double CHUNK_CHANCE_OUTER = 0.45;
    static final double CHUNK_CHANCE_INNER = 0.1;
    static final double CHUNK_MIN_RADIUS = 2.0;
    static final double CHUNK_MAX_RADIUS = 8.0;
    static final double CHUNK_MIN_ABOVE = 4.0;
    static final double CHUNK_MAX_ABOVE = 16.0;
    /** Landmarks: a raised flat floe every ~650 blocks (± 100) along the zone, radius 9–13 (Ø ≥ 16), 10–16 up. */
    static final double LANDMARK_CELL = 650.0;
    static final double LANDMARK_JITTER = 100.0;
    static final double LANDMARK_SHARE = 0.55;
    static final int LANDMARK_MIN_RADIUS = 9;
    static final int LANDMARK_MAX_RADIUS = 13;
    static final double LANDMARK_MIN_LIFT = 10.0;
    static final double LANDMARK_MAX_LIFT = 16.0;
    /** No crack within this many blocks of a landmark's edge. */
    static final double LANDMARK_CLEAR = 4.0;

    static final int NONE = 0;
    static final int CRACK = 1;

    private static final int POINT_SALT = 8501;
    private static final int EDGE_SALT = 8502;
    private static final int FLOE_SALT = 8503;
    private static final int CHUNK_SALT = 8504;
    private static final int LANDMARK_SALT = 8505;

    private final RingLayout layout;
    private final long salt;
    private final SimplexNoise warp;
    private final double start;
    private final double end;
    private final double middle;
    private final double period;
    private final int countAlong;
    private final double cellAlong;

    ZoneFractureShape(RingLayout layout, long salt) {
        this.layout = layout;
        this.salt = salt;
        this.warp = new SimplexNoise(new XoroshiroRandomSource(salt ^ 0x5A4E3F2D1C0B0A09L));
        this.start = Ring4Sundered.zoneStart(layout);
        this.end = Ring4Sundered.zoneEnd(layout);
        this.middle = 0.5 * (start + end);
        this.period = RingArc.perimeter(middle);
        this.countAlong = RingArc.cellCount(period, CELL_ALONG);
        this.cellAlong = period / countAlong;
    }

    RingLayout layout() {
        return layout;
    }

    long salt() {
        return salt;
    }

    /** The zone's inner and outer edge (effective distance). */
    double start() {
        return start;
    }

    double end() {
        return end;
    }

    /**
     * How strongly the zone is fractured at an effective distance, 0..1: 0 outside the zone 4→5 and in its first and last
     * {@link #CALM} blocks, full from {@link #RAMP} further on.
     */
    static double strength(RingLayout layout, double effective) {
        double from = effective - Ring4Sundered.zoneStart(layout);
        double to = Ring4Sundered.zoneEnd(layout) - effective;
        return Ring4Relief.smooth(from, CALM, CALM + RAMP) * Ring4Relief.smooth(to, CALM, CALM + RAMP);
    }

    /** Whether an effective distance lies where the fracture may cut (strength above 0). */
    boolean active(double effective) {
        return effective > start + CALM && effective < end - CALM;
    }

    /** The 2D decisions of one column. */
    static final class Info {
        /** {@link #NONE} or {@link #CRACK}. */
        int kind;
        double strength;
        /** A crack's floor (blocks under the ground) and half width (blocks). */
        double depth;
        double half;
        /** A raised floe on the column: its lift (blocks above the ground) here, else NaN. */
        double floeLift = Double.NaN;
        /** A loose chunk over the column: its height above the ground, thickness here; NaN if none. */
        double chunkAbove = Double.NaN;
        double chunkThickness;
        /** The landmark the column lies on, else −1. */
        int landmark = -1;

        void reset() {
            kind = NONE;
            strength = 0.0;
            depth = 0.0;
            half = 0.0;
            floeLift = Double.NaN;
            chunkAbove = Double.NaN;
            chunkThickness = 0.0;
            landmark = -1;
        }
    }

    /** The decisions of column x, z into {@code info} (reset first). */
    void fill(Info info, int x, int z, double[] landmarkCentre) {
        info.reset();
        double effective = layout.effectiveDistance(x, z);
        double strength = strength(layout, effective);
        info.strength = strength;
        if (strength <= 0.0) {
            return;
        }
        double px = x + 0.5;
        double pz = z + 0.5;
        int landmark = landmarkNear(px, pz, landmarkCentre);
        if (landmark >= 0) {
            double d = Math.hypot(px - landmarkCentre[0], pz - landmarkCentre[1]);
            if (d <= landmarkCentre[2]) {
                info.landmark = landmark;
                return;
            }
            if (d <= landmarkCentre[2] + LANDMARK_CLEAR) {
                return;
            }
        }
        Cell cell = cell(px, pz);
        double half = strength * cell.half;
        if (cell.edge < half && !cell.crossing) {
            info.kind = CRACK;
            info.half = half;
            info.depth = cell.depth;
        } else if (cell.floe) {
            info.floeLift = cell.lift;
        }
        chunkOver(info, px, pz, strength, effective);
    }

    /** The nearest landmark (index) whose clearing may hold the point, its centre and radius into {@code centre}; else −1. */
    int landmarkNear(double x, double z, double[] centre) {
        int count = RingArc.cellCount(period, LANDMARK_CELL);
        double size = period / count;
        double along = RingArc.wrap(Math.atan2(z, x) * middle, period);
        int k = (int) Math.floor(along / size);
        for (int dk = -1; dk <= 1; dk++) {
            int index = Math.floorMod(k + dk, count);
            double[] c = landmark(index);
            if (Math.hypot(x - c[0], z - c[1]) <= c[2] + LANDMARK_CLEAR) {
                centre[0] = c[0];
                centre[1] = c[1];
                centre[2] = c[2];
                centre[3] = c[3];
                return index;
            }
        }
        return -1;
    }

    /** The number of landmarks round the zone. */
    int landmarkCount() {
        return RingArc.cellCount(period, LANDMARK_CELL);
    }

    /** Landmark {@code k}: {x, z, radius, lift} (centre at {@link #LANDMARK_SHARE} of the zone, on its ray). */
    double[] landmark(int k) {
        int count = landmarkCount();
        double size = period / count;
        long h = CellShapes.cell(salt, k, 0, LANDMARK_SALT);
        double along = (k + 0.5) * size + (CellShapes.unit(h, 0) - 0.5) * 2.0 * LANDMARK_JITTER;
        double angle = along / middle;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double distance = Ring5Field.alongRay(layout, cos, sin, start + LANDMARK_SHARE * (end - start));
        int radius = LANDMARK_MIN_RADIUS + (int) Math.floor((LANDMARK_MAX_RADIUS - LANDMARK_MIN_RADIUS + 1) * CellShapes.unit(h, 1));
        double lift = LANDMARK_MIN_LIFT + (LANDMARK_MAX_LIFT - LANDMARK_MIN_LIFT) * CellShapes.unit(h, 2);
        return new double[]{Math.round(distance * cos) + 0.5, Math.round(distance * sin) + 0.5, radius, lift};
    }

    /** The cell a point lies in on the bent grid and its nearest edge. */
    record Cell(int i, int j, double edge, double half, double depth, boolean crossing, boolean floe, double lift) {
    }

    /** Along-the-ring and radial coordinates of a point, bent: {a (wrapped), r}. */
    double[] warped(double x, double z) {
        double a = RingArc.wrap(Math.atan2(z, x) * middle, period);
        double r = Math.hypot(x, z);
        a += WARP * warp.getValue(x * WARP_SCALE, z * WARP_SCALE);
        r += WARP * warp.getValue(z * WARP_SCALE + 711.3, x * WARP_SCALE - 311.7);
        return new double[]{RingArc.wrap(a, period), r};
    }

    /** The jittered point of grid cell (i, j), {a, r}; i wrapped round the loop. */
    private double[] point(int i, int j) {
        long h = CellShapes.cell(salt, Math.floorMod(i, countAlong), j, POINT_SALT);
        return new double[]{(i + 0.15 + 0.7 * CellShapes.unit(h, 0)) * cellAlong, (j + 0.15 + 0.7 * CellShapes.unit(h, 1)) * CELL_RADIAL};
    }

    Cell cell(double x, double z) {
        double[] w = warped(x, z);
        int ci = (int) Math.floor(w[0] / cellAlong);
        int cj = (int) Math.floor(w[1] / CELL_RADIAL);
        int bi = 0;
        int bj = 0;
        double best = Double.MAX_VALUE;
        double[] bestPoint = null;
        double[][] points = new double[15][];
        int n = 0;
        for (int di = -1; di <= 1; di++) {
            for (int dj = -2; dj <= 2; dj++) {
                double[] p = point(ci + di, cj + dj);
                // Along the loop: the shorter way round (the seam at ±π).
                p[0] = w[0] + RingArc.loopDelta(p[0], w[0], period);
                points[n++] = p;
                double d = (w[0] - p[0]) * (w[0] - p[0]) + (w[1] - p[1]) * (w[1] - p[1]);
                if (d < best) {
                    best = d;
                    bi = ci + di;
                    bj = cj + dj;
                    bestPoint = p;
                }
            }
        }
        double edge = Double.MAX_VALUE;
        int oi = bi;
        int oj = bj;
        double[] other = null;
        for (int k = 0; k < n; k++) {
            double[] p = points[k];
            if (p == bestPoint) {
                continue;
            }
            double ex = p[0] - bestPoint[0];
            double ez = p[1] - bestPoint[1];
            double length = Math.hypot(ex, ez);
            double dk = (w[0] - p[0]) * (w[0] - p[0]) + (w[1] - p[1]) * (w[1] - p[1]);
            double e = (dk - best) / (2.0 * length);
            if (e < edge) {
                edge = e;
                oi = ci + (k / 5) - 1;
                oj = cj + (k % 5) - 2;
                other = p;
            }
        }
        long pair = pairHash(bi, bj, oi, oj);
        double half = MIN_HALF + (MAX_HALF - MIN_HALF) * CellShapes.unit(pair, 0);
        double depth = MIN_DEPTH + (MAX_DEPTH - MIN_DEPTH) * CellShapes.unit(pair, 1);
        // The crossings: along the edge (its direction across the two points), one every 24–34 blocks.
        double ex = other[0] - bestPoint[0];
        double ez = other[1] - bestPoint[1];
        double length = Math.hypot(ex, ez);
        double along = ((w[0] - 0.5 * (bestPoint[0] + other[0])) * -ez + (w[1] - 0.5 * (bestPoint[1] + other[1])) * ex) / length;
        boolean crossing = crossingAt(pair, along);
        double[] floe = floeOf(CellShapes.cell(salt, Math.floorMod(bi, countAlong), bj, FLOE_SALT));
        double lift = Double.NaN;
        if (floe != null) {
            double slope = Math.tan(Math.toRadians(floe[1])) * ((w[0] - bestPoint[0]) * Math.cos(floe[2]) + (w[1] - bestPoint[1]) * Math.sin(floe[2]));
            lift = floe[0] + slope;
        }
        return new Cell(Math.floorMod(bi, countAlong), bj, edge, half, depth, crossing, floe != null, lift);
    }

    /**
     * Whether a position {@code along} an edge (blocks from its middle) is one of its crossings: one every 24–34 blocks
     * (by the edge's hash), each {@link #CROSSING_HALF} either side.
     */
    static boolean crossingAt(long pair, double along) {
        double spacing = CROSSING_MIN + (CROSSING_MAX - CROSSING_MIN) * CellShapes.unit(pair, 2);
        double phase = spacing * CellShapes.unit(pair, 3);
        return Math.abs(RingArc.loopDelta(along, phase, spacing)) < CROSSING_HALF;
    }

    /** A cell's raised floe by its hash: {lift at its point (8–30), tilt (degrees, ≤ 30), direction (radians)}, or null. */
    @javax.annotation.Nullable
    static double[] floeOf(long floeHash) {
        if (CellShapes.unit(floeHash, 0) >= FLOE_CHANCE) {
            return null;
        }
        return new double[]{FLOE_MIN_LIFT + (FLOE_MAX_LIFT - FLOE_MIN_LIFT) * CellShapes.unit(floeHash, 1),
                FLOE_MAX_TILT * CellShapes.unit(floeHash, 2), 2.0 * Math.PI * CellShapes.unit(floeHash, 3)};
    }

    /** The number of grid cells round the loop. */
    int countAlong() {
        return countAlong;
    }

    /** A hash of an edge, the same from either side. */
    private long pairHash(int i1, int j1, int i2, int j2) {
        long a = (((long) Math.floorMod(i1, countAlong)) << 32) ^ (j1 & 0xFFFFFFFFL);
        long b = (((long) Math.floorMod(i2, countAlong)) << 32) ^ (j2 & 0xFFFFFFFFL);
        return CellShapes.cell(salt, Math.min(a, b), Math.max(a, b), EDGE_SALT);
    }

    /** A loose chunk over the point (the nearest chunk cell whose chunk covers it), into {@code info}. */
    private void chunkOver(Info info, double x, double z, double strength, double effective) {
        int ci = Math.floorDiv(Mth.floor(x), CHUNK_CELL);
        int cj = Math.floorDiv(Mth.floor(z), CHUNK_CELL);
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                double[] chunk = chunk(ci + di, cj + dj);
                if (chunk == null) {
                    continue;
                }
                double d = Math.hypot(x - chunk[0], z - chunk[1]);
                if (d <= chunk[2]) {
                    info.chunkAbove = chunk[3];
                    info.chunkThickness = 3.0 + (chunk[4] - 3.0) * (1.0 - d / chunk[2]);
                    return;
                }
            }
        }
    }

    /** The loose chunk of chunk cell (i, j): {x, z, radius, height above the ground, thickness}, or null. */
    double[] chunk(int i, int j) {
        long h = CellShapes.cell(salt, i, j, CHUNK_SALT);
        double x = (i + 0.2 + 0.6 * CellShapes.unit(h, 0)) * CHUNK_CELL;
        double z = (j + 0.2 + 0.6 * CellShapes.unit(h, 1)) * CHUNK_CELL;
        double effective = layout.effectiveDistance(x, z);
        double strength = strength(layout, effective);
        if (strength <= 0.0) {
            return null;
        }
        double share = (effective - start) / (end - start);
        double chance = (share >= 0.5 ? CHUNK_CHANCE_OUTER : CHUNK_CHANCE_INNER) * strength;
        if (CellShapes.unit(h, 2) >= chance) {
            return null;
        }
        // Over a crack only (its centre column's, as the column sees it): the chunk that broke out of it.
        double cx = Mth.floor(x) + 0.5;
        double cz = Mth.floor(z) + 0.5;
        Cell cell = cell(cx, cz);
        if (cell.edge >= strength(layout, layout.effectiveDistance(Mth.floor(x), Mth.floor(z))) * cell.half || cell.crossing
                || landmarkNear(cx, cz, new double[4]) >= 0) {
            return null;
        }
        double radius = CHUNK_MIN_RADIUS + (CHUNK_MAX_RADIUS - CHUNK_MIN_RADIUS) * CellShapes.unit(h, 3);
        double above = CHUNK_MIN_ABOVE + (CHUNK_MAX_ABOVE - CHUNK_MIN_ABOVE) * CellShapes.unit(h, 4);
        double thickness = Math.max(3.0, radius * (0.6 + 0.6 * CellShapes.unit(h, 5)));
        return new double[]{x, z, radius, above, thickness};
    }
}
