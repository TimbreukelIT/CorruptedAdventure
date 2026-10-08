package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Lava rivers of ring 3 (change 39d): every active volcano sends one or two rivers from its foot downhill, around other
 * volcanoes and into rift valleys, until they end in a lava pool in a low place. The lava level never rises downstream;
 * each step down of one block is a small lava fall.
 * <p>
 * A river is traced once per world (pure: a function of the field's noises and its volcano), over the ground without
 * rivers ({@link VolcanicField#traceGround}); the results are shared by every copy of the field ({@code Decisions}) and
 * indexed per tile of {@link #TILE} blocks. Tracing never depends on which chunk asks first.
 */
final class LavaRivers {
    static final double STEP = 8.0;
    static final int MAX_STEPS = 170;
    /** The lava lies this far below the ground the river follows. */
    static final double DROP = 2.0;
    static final double MIN_HALF = 3.0;
    static final double MAX_HALF = 8.0;
    /** Steps over which a river widens from {@link #MIN_HALF} to {@link #MAX_HALF}. */
    private static final double WIDEN_STEPS = 100.0;
    /** The end pool is this much wider than the river. */
    private static final double POOL = 1.8;
    /** A river that would have to cut deeper than this through a rise ends before it. */
    private static final double MAX_GORGE = 22.0;
    /** A river that has not descended for this many steps has reached a basin. */
    private static final int BASIN_STEPS = 40;
    private static final double[] TURNS = {0.0, -0.35, 0.35, -0.75, 0.75, -1.2, 1.2};
    private static final double TURN_COST = 0.6;
    static final int TILE = 128;
    /** How far beyond a segment's half width a column still wants to know about it (banks, levees, islands). */
    static final double PAD = 4.0 * MAX_HALF * POOL + 8.0;
    private static final int SALT = 41;

    private LavaRivers() {
    }

    /** One traced river: points with their lava level (non-increasing) and half width, and the arches across it. */
    record River(double[] x, double[] z, double[] level, double[] half, double minX, double maxX, double minZ, double maxZ,
                 List<Landmarks.Arch> arches) {
        int points() {
            return x.length;
        }
    }

    /** The segments of all rivers that may touch one tile (parallel arrays), and the arches over them there. */
    record Tile(River[] rivers, int[] segments, Landmarks.Arch[] arches) {
        static final Tile EMPTY = new Tile(new River[0], new int[0], new Landmarks.Arch[0]);
    }

    /** The rivers of volcano cell (i, j): none for an empty cell or a dormant volcano. */
    static List<River> trace(VolcanicField field, int i, int j) {
        Volcano v = field.volcano(i, j);
        if (v == null || v.dormant()) {
            return List.of();
        }
        long h = CellShapes.cell(field.salt(), i, j, SALT);
        int count = CellShapes.unit(h, 0) < 0.5 ? 2 : 1;
        List<River> rivers = new ArrayList<>(count);
        double angle = CellShapes.unit(h, 1) * Math.PI * 2.0;
        for (int k = 0; k < count; k++) {
            double a = k == 0 ? angle : angle + Math.PI * (0.6 + 0.8 * CellShapes.unit(h, 2));
            River river = traceFrom(field, v, a);
            if (river != null) {
                List<Landmarks.Arch> arches = Landmarks.riverArches(river, CellShapes.cell(field.salt(), i, j, SALT + 1 + k));
                rivers.add(new River(river.x(), river.z(), river.level(), river.half(), river.minX(), river.maxX(), river.minZ(),
                        river.maxZ(), arches));
            }
        }
        return List.copyOf(rivers);
    }

    private static River traceFrom(VolcanicField field, Volcano v, double angle) {
        double px = v.x() + Math.cos(angle) * v.radius() * 0.8;
        double pz = v.z() + Math.sin(angle) * v.radius() * 0.8;
        double heading = angle;
        double ground = field.traceGround(px, pz);
        double level = ground - DROP;
        List<double[]> points = new ArrayList<>();
        points.add(new double[]{px, pz, level, MIN_HALF});
        int flat = 0;
        VolcanicField.Column scratch = new VolcanicField.Column();
        for (int s = 1; s <= MAX_STEPS; s++) {
            double bestX = 0.0;
            double bestZ = 0.0;
            double bestScore = Double.MAX_VALUE;
            double bestGround = 0.0;
            double bestHeading = heading;
            // A slow meander from the warp noise keeps straight descents from reading as ruled lines.
            double meander = 0.25 * field.warpAt(px * 2.0, pz * 2.0);
            for (double turn : TURNS) {
                double a = heading + turn + meander;
                double qx = px + Math.cos(a) * STEP;
                double qz = pz + Math.sin(a) * STEP;
                VolcanicField.Column probe = field.traceColumn(qx, qz, scratch);
                if (probe.calderaMask > 0.0) {
                    continue; // never into a crater
                }
                double g = probe.preHeight;
                double score = g + TURN_COST * Math.abs(turn) * STEP;
                if (score < bestScore) {
                    bestScore = score;
                    bestX = qx;
                    bestZ = qz;
                    bestGround = g;
                    bestHeading = a;
                }
            }
            if (bestScore == Double.MAX_VALUE) {
                break; // a crater on every side
            }
            px = bestX;
            pz = bestZ;
            heading = bestHeading;
            if (!field.riverMayFlow(px, pz)) {
                break;
            }
            double free = bestGround - DROP;
            if (free < level) {
                flat = free < level - 0.05 ? 0 : flat + 1;
                level = free;
            } else {
                flat++;
            }
            if (free - level > MAX_GORGE || flat >= BASIN_STEPS) {
                break;
            }
            double half = MIN_HALF + (MAX_HALF - MIN_HALF) * Math.min(1.0, s / WIDEN_STEPS);
            points.add(new double[]{px, pz, level, half});
        }
        if (points.size() < 4) {
            return null;
        }
        double[] last = points.get(points.size() - 1);
        last[3] *= POOL;
        int n = points.size();
        double[] xs = new double[n];
        double[] zs = new double[n];
        double[] levels = new double[n];
        double[] halves = new double[n];
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            double[] p = points.get(k);
            xs[k] = p[0];
            zs[k] = p[1];
            levels[k] = p[2];
            halves[k] = p[3];
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minZ = Math.min(minZ, p[1]);
            maxZ = Math.max(maxZ, p[1]);
        }
        return new River(xs, zs, levels, halves, minX, maxX, minZ, maxZ, List.of());
    }

    /** The segments that may touch tile (ti, tj): from the rivers of every volcano cell within reach. */
    static Tile tile(VolcanicField field, int ti, int tj) {
        double x0 = (double) ti * TILE - PAD;
        double x1 = (double) (ti + 1) * TILE + PAD;
        double z0 = (double) tj * TILE - PAD;
        double z1 = (double) (tj + 1) * TILE + PAD;
        int size = field.settings().cellSize();
        int reach = (int) Math.ceil((MAX_STEPS * STEP + 0.8 * size + PAD) / size);
        int ci = Math.floorDiv((int) ((ti + 0.5) * TILE), size);
        int cj = Math.floorDiv((int) ((tj + 0.5) * TILE), size);
        List<River> rivers = new ArrayList<>();
        List<Integer> segments = new ArrayList<>();
        List<Landmarks.Arch> arches = new ArrayList<>();
        for (int i = ci - reach; i <= ci + reach; i++) {
            for (int j = cj - reach; j <= cj + reach; j++) {
                for (River r : field.riversOf(i, j)) {
                    if (r.maxX() < x0 || r.minX() > x1 || r.maxZ() < z0 || r.minZ() > z1) {
                        continue;
                    }
                    for (Landmarks.Arch arch : r.arches()) {
                        if (arch.x() + arch.reach() >= x0 && arch.x() - arch.reach() <= x1 && arch.z() + arch.reach() >= z0
                                && arch.z() - arch.reach() <= z1) {
                            arches.add(arch);
                        }
                    }
                    for (int k = 0; k + 1 < r.points(); k++) {
                        if (Math.max(r.x()[k], r.x()[k + 1]) < x0 || Math.min(r.x()[k], r.x()[k + 1]) > x1
                                || Math.max(r.z()[k], r.z()[k + 1]) < z0 || Math.min(r.z()[k], r.z()[k + 1]) > z1) {
                            continue;
                        }
                        rivers.add(r);
                        segments.add(k);
                    }
                }
            }
        }
        if (rivers.isEmpty() && arches.isEmpty()) {
            return Tile.EMPTY;
        }
        int[] seg = new int[segments.size()];
        for (int k = 0; k < seg.length; k++) {
            seg[k] = segments.get(k);
        }
        return new Tile(rivers.toArray(new River[0]), seg, arches.toArray(new Landmarks.Arch[0]));
    }

    /**
     * The river nearest to (x, z) relative to its width: writes into {@code out} the distance in half widths
     * ({@code along}), the lava level and the half width there, and returns false if no river is within {@link #PAD}.
     */
    static boolean nearest(Tile tile, double x, double z, double[] out) {
        double bestAlong = Double.MAX_VALUE;
        for (int k = 0; k < tile.segments().length; k++) {
            River r = tile.rivers()[k];
            int s = tile.segments()[k];
            double ax = r.x()[s];
            double az = r.z()[s];
            double dx = r.x()[s + 1] - ax;
            double dz = r.z()[s + 1] - az;
            double len2 = dx * dx + dz * dz;
            double t = len2 == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, ((x - ax) * dx + (z - az) * dz) / len2));
            double ex = ax + t * dx - x;
            double ez = az + t * dz - z;
            double distance = Math.sqrt(ex * ex + ez * ez);
            if (distance > PAD) {
                continue;
            }
            double half = r.half()[s] + t * (r.half()[s + 1] - r.half()[s]);
            double along = distance / half;
            if (along < bestAlong) {
                bestAlong = along;
                out[0] = along;
                out[1] = r.level()[s] + t * (r.level()[s + 1] - r.level()[s]);
                out[2] = half;
            }
        }
        return bestAlong != Double.MAX_VALUE;
    }

}
