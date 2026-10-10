package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lava tubes and magma chambers under ring 3 (package 42). Per cell of {@link #CELL} blocks at most one tube system:
 * <ul>
 *   <li>a <b>main tube</b>: a smooth, dry basalt tunnel 5–9 wide with a flat floor, Y 20–80, following a sinuous path
 *   for 280–550 blocks, at least {@link #ROOF} blocks of rock under the ground, lava lakes and lava rivers. Both ends
 *   (if the ground there allows) rise as ramps to a mouth at the surface, one preferably in a collapsed lava tube trench of
 *   the basalt flows (39d); a few skylights open its roof where it runs shallow;</li>
 *   <li>a lower <b>side branch</b> with a lava gutter (pools stepping down, each held by a lip of crust), descending from the
 *   main tube into the lava lake of a <b>magma chamber</b> in the cell (30–60 wide, 15–30 high, Y −40…10), or, where no
 *   chamber fits, ending blind after ~100 blocks.</li>
 * </ul>
 * The main tube stays dry, a shelter from the heat; the glow of a branch is the warning that lava lies ahead.
 * <p>
 * Everything is traced once per world (pure: a function of the field's noises and the cell), over the ground without
 * rivers ({@link VolcanicField#traceColumn}), and indexed per tile of {@link #TILE} blocks; tracing never depends on
 * which chunk asks first. A column turns the shapes into intervals ({@link #addTo}): the opening (carved, outside the
 * terrain interpolation), a skin and a shell of rock around it (the surface paints them as basalt), lava in the gutter and
 * the chamber's lake, and a guard band that keeps vanilla caves and carvers away from the lava and the chamber.
 */
final class LavaTubes {
    static final int CELL = 400;
    private static final double CHANCE = 0.85;
    static final double STEP = 8.0;
    private static final int MIN_STEPS = 35;
    private static final int MAX_STEPS = 58;
    /** After its length the tube walks on this many steps looking for a place for its mouth, else it ends blind. */
    private static final int MOUTH_SEARCH = 10;
    private static final int MIN_POINTS = 12;
    static final double MIN_FLOOR = 20.0;
    static final double MAX_FLOOR = 80.0;
    /** Under a deep cut a tube may sink this low to keep its roof; a tube that cannot ends there. */
    static final double LOWEST_FLOOR = 12.0;
    /** The floor of a tube lies about this far below the ground. */
    private static final double DEPTH = 26.0;
    static final double MAX_SLOPE = 0.3;
    static final double MOUTH_SLOPE = 0.45;
    /** Rock kept above a tube (except at its mouths and skylights). */
    static final double ROOF = 8.0;
    /**
     * 42d: where a tube breaks through the ground (the trench in front of its mouth) and lies at least this far below
     * it, a roof of one block of ash crust covers it: a piece of thin roof in front of every mouth that falls in under a
     * player. The shallow end of the trench stays open, so the mouth still shows.
     */
    static final int LID_DROP = 3;
    /** The smooth inner wall and the shell of rock around every opening, in blocks. */
    static final double SKIN = 1.3;
    static final double SHELL = 3.0;
    private static final double BRANCH_SLOPE = 0.3;
    private static final double BRANCH_STEP = 6.0;
    private static final double DEAD_END_SLOPE = 0.22;
    private static final double BLIND_MIN = 70.0;
    private static final double BLIND_MAX = 110.0;
    /** The largest lateral meander of a branch. */
    private static final double MAX_WOBBLE = 12.0;
    private static final double GUTTER = 1.1;
    /** The gutter starts this far down the branch, so no lava stands at the junction with the main tube. */
    static final double GUTTER_START = 12.0;
    /** A chamber's centre keeps this far from its cell's edge: chambers of neighbouring cells never meet. */
    static final double CHAMBER_INSET = 70.0;
    private static final int CHAMBER_TRIES = 4;
    /** Spacing of the search for a lava tube trench (39d) to open into: rows of samples, refined where the axis is crossed. */
    private static final double TRENCH_SEARCH = 12.0;
    /** The longest branch down to a chamber. */
    static final double MAX_BRANCH = 420.0;
    /** A mouth opens only where the ground lies at most this high (its ramp stays short). */
    private static final double MOUTH_MAX_GROUND = 125.0;
    static final int TILE = 128;
    private static final int SALT = 4202;

    private LavaTubes() {
    }

    /**
     * One tube: points with the Y of its floor (the first air block, before {@code floor} is floored) and its half width,
     * the distance walked from its start ({@code along}). A branch with lava has a gutter from {@link #GUTTER_START} on.
     */
    record Tube(double[] x, double[] z, double[] floor, double[] half, double[] along, boolean lava, double minX,
                double maxX, double minZ, double maxZ) {
        int points() {
            return x.length;
        }

        /** Half the height of the cross-section's ellipse for a half width. */
        static double halfHeight(double half) {
            return 0.75 * half + 1.0;
        }

        /** How far above its floor a tube's opening reaches. */
        static double clearance(double half) {
            return 1.6 * halfHeight(half);
        }
    }

    /** A skylight: a ragged shaft from a tube's roof ({@code bottom}) up through the ground ({@code top}). */
    record Skylight(double x, double z, double radius, double bottom, double top, double phase) {
    }

    /**
     * A magma chamber: a flattened ellipsoid (half axes {@code a}, {@code b}, turned by {@code angle}, ragged outline),
     * a dome of {@code dome} above {@code centreY} and a bowl of {@code bowl} below it, lava up to and including
     * {@code lake}.
     */
    record Chamber(double x, double z, double a, double b, double angle, double centreY, double dome, double bowl, int lake,
                   double phase) {
        /** Normalised (ragged) distance of a column, the axes grown by {@code grow} blocks. */
        double distance(double px, double pz, double grow) {
            double dx = px - x;
            double dz = pz - z;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double along = (dx * cos + dz * sin) / (a + grow);
            double across = (-dx * sin + dz * cos) / (b + grow);
            return Volcano.ragged(Math.sqrt(along * along + across * across), Math.atan2(across, along), phase);
        }

        /** The radius of the plain ellipse (no raggedness) towards a direction in world axes. */
        double radiusTowards(double dirX, double dirZ) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double along = dirX * cos + dirZ * sin;
            double across = -dirX * sin + dirZ * cos;
            return 1.0 / Math.sqrt(along * along / (a * a) + across * across / (b * b));
        }

        /** How far (blocks) the chamber, its walls and its guard reach from the centre. */
        double reach() {
            return (Math.max(a, b) + SHELL + 3.0) * 1.2;
        }

        double bottom() {
            return centreY - bowl;
        }

        double top() {
            return centreY + dome;
        }
    }

    /** The tube system of one cell: the main tube and its branch, the skylights, the chamber (or none). */
    record TubeSystem(List<Tube> tubes, List<Skylight> skylights, @Nullable Chamber chamber) {
        static final TubeSystem EMPTY = new TubeSystem(List.of(), List.of(), null);
    }

    /** What a tile needs: the tubes that may touch it (with their segments there), the skylights and chambers. */
    record Tile(Tube[] tubes, int[][] segments, Skylight[] skylights, Chamber[] chambers) {
        static final Tile EMPTY = new Tile(new Tube[0], new int[0][], new Skylight[0], new Chamber[0]);
    }

    // ---------------------------------------------------------------- tracing

    /** The tube system of cell (i, j). Pure. */
    static TubeSystem trace(VolcanicField f, int i, int j) {
        long h = CellShapes.cell(f.salt(), i, j, SALT);
        if (CellShapes.unit(h, 0) >= CHANCE) {
            return TubeSystem.EMPTY;
        }
        Probe probe = new Probe(f);
        double[] start = mouthInCell(f, probe, h, i, j);
        if (start == null) {
            return TubeSystem.EMPTY;
        }
        double half = 2.5 + 2.0 * CellShapes.unit(h, 1);
        double heading = CellShapes.unit(h, 2) * Math.PI * 2.0;
        int length = MIN_STEPS + (int) (CellShapes.unit(h, 3) * (MAX_STEPS - MIN_STEPS + 1));
        Path main = walk(f, probe, start[0], start[1], heading, half, length);
        if (main == null) {
            return TubeSystem.EMPTY;
        }
        Tube tube = main.profile(true, main.endMouth);
        List<Skylight> skylights = skylights(f, probe, main, tube, half, h);
        List<Tube> tubes = new ArrayList<>(2);
        tubes.add(tube);
        Chamber chamber = null;
        Branch branch = branch(f, probe, tube, h, i, j);
        if (branch != null) {
            tubes.add(branch.tube);
            chamber = branch.chamber;
        }
        return new TubeSystem(List.copyOf(tubes), List.copyOf(skylights), chamber);
    }

    /** The ground over the tubes, read once per point. */
    private static final class Probe {
        final VolcanicField f;
        final VolcanicField.Column column = new VolcanicField.Column();
        final double[] river = new double[3];
        double ground;
        /** The lowest lava (crater lake or river bed) over the point, or +∞. */
        double lava;

        Probe(VolcanicField f) {
            this.f = f;
        }

        Probe at(double x, double z) {
            VolcanicField.Column c = f.traceColumn(x, z, column);
            ground = c.preHeight;
            lava = c.hasLava() ? c.lavaBottom() : Double.POSITIVE_INFINITY;
            if (f.nearestRiver(x, z, river) && river[0] < 2.5) {
                lava = Math.min(lava, river[1] - 6.0);
            }
            return this;
        }

        /** Whether a tube may open to the surface here: flat ground in the core, far from lava, craters and the abyss. */
        boolean mouth(double x, double z) {
            if (f.regions(x, z).core() < 0.99 || f.nearAbyss(x, z, 30.0) || f.riverWithin(x, z, 30.0)) {
                return false;
            }
            VolcanicField.Column c = f.traceColumn(x, z, column);
            return c.calderaMask <= 0.0 && !c.hasLava() && c.rise < 6.0 && c.preHeight <= MOUTH_MAX_GROUND;
        }
    }

    /**
     * A mouth for the start: in a collapsed lava tube trench of the basalt flows (39d) if the cell has basalt flows (a fine
     * search, as the trenches are narrow), else the first good place on a jittered 5 × 5 grid over the cell.
     */
    @Nullable
    private static double[] mouthInCell(VolcanicField f, Probe probe, long h, int i, int j) {
        double inset = 40.0;
        double span = (CELL - 2.0 * inset) / 5.0;
        double[] any = null;
        boolean flows = false;
        for (int k = 0; k < 25; k++) {
            double x = i * CELL + inset + (k % 5 + CellShapes.unit(h, 10 + 2 * k)) * span;
            double z = j * CELL + inset + (k / 5 + CellShapes.unit(h, 11 + 2 * k)) * span;
            flows |= f.regions(x, z).flowsS() > 0.3;
            if (any == null && probe.mouth(x, z)) {
                any = new double[]{x, z};
            }
        }
        if (flows) {
            // Rows of samples of the tubes' axis value; where it changes sign the axis lies in between (interpolated).
            Ring3Relief.Detail detail = new Ring3Relief.Detail();
            double oz = CellShapes.unit(h, 61) * TRENCH_SEARCH;
            for (double z = j * CELL + inset + oz; z < (j + 1) * CELL - inset; z += TRENCH_SEARCH) {
                double x0 = i * CELL + inset;
                double a = f.relief().lavaTubeAxis(x0, z);
                for (double x = x0 + TRENCH_SEARCH; x < (i + 1) * CELL - inset; x += TRENCH_SEARCH) {
                    double b = f.relief().lavaTubeAxis(x, z);
                    if (a * b <= 0.0 && a != b) {
                        double xc = x - TRENCH_SEARCH * b / (b - a);
                        detail.reset();
                        if (f.regions(xc, z).flowsS() > 0.3 && f.relief().lavaTube(xc, z, detail) > 0.3 && detail.floor >= 0.5
                                && probe.mouth(xc, z)) {
                            return new double[]{xc, z};
                        }
                    }
                    a = b;
                }
            }
        }
        return any;
    }

    /** The points of a walk with the ground and lava over each, before the floor is chosen. */
    private static final class Path {
        final List<double[]> points = new ArrayList<>();
        final double half;
        boolean endMouth;

        Path(double half) {
            this.half = half;
        }

        int size() {
            return points.size();
        }

        /** {x, z, ground, lava}. */
        double[] get(int k) {
            return points.get(k);
        }

        /**
         * The floor along the path: ~{@link #DEPTH} under the ground within Y 20–80, smoothed, under every cap (roof
         * over ground and lava), at most {@link #MAX_SLOPE} per block; a mouth ramp at each end that has one.
         */
        Tube profile(boolean startMouth, boolean endMouth) {
            int n = size();
            double top = Tube.clearance(half);
            double[] target = new double[n];
            double[] cap = new double[n];
            for (int k = 0; k < n; k++) {
                double[] p = get(k);
                target[k] = Mth.clamp(p[2] - DEPTH, MIN_FLOOR, MAX_FLOOR);
                cap[k] = Math.min(p[2], p[3]) - ROOF - top;
            }
            double[] floor = new double[n];
            for (int k = 0; k < n; k++) {
                double sum = 0.0;
                for (int d = -2; d <= 2; d++) {
                    sum += target[Mth.clamp(k + d, 0, n - 1)];
                }
                floor[k] = Math.min(sum / 5.0, cap[k]);
            }
            double rise = MAX_SLOPE * STEP;
            for (int k = 1; k < n; k++) {
                floor[k] = Math.min(floor[k], floor[k - 1] + rise);
            }
            for (int k = n - 2; k >= 0; k--) {
                floor[k] = Math.min(floor[k], floor[k + 1] + rise);
            }
            if (startMouth) {
                ramp(floor, 0, 1, top);
            }
            if (endMouth) {
                ramp(floor, n - 1, -1, top);
            }
            double[] xs = new double[n];
            double[] zs = new double[n];
            double[] halves = new double[n];
            double[] along = new double[n];
            for (int k = 0; k < n; k++) {
                xs[k] = get(k)[0];
                zs[k] = get(k)[1];
                halves[k] = half;
                along[k] = k == 0 ? 0.0 : along[k - 1] + Math.hypot(xs[k] - xs[k - 1], zs[k] - zs[k - 1]);
            }
            return tube(xs, zs, floor, halves, along, false);
        }

        /**
         * A mouth: from the end at {@code from} the floor climbs at {@link #MOUTH_SLOPE} to just under the ground there,
         * so the tube opens as a sloping trench; only where the ramp keeps its roof under any lava.
         */
        private void ramp(double[] floor, int from, int dir, double top) {
            double mouth = get(from)[2] - 2.0;
            int end = from;
            for (int s = 0; end >= 0 && end < floor.length; end += dir, s++) {
                double m = mouth - s * STEP * MOUTH_SLOPE;
                if (m <= floor[end]) {
                    break;
                }
                if (m + top + ROOF > get(end)[3]) {
                    return; // lava over the ramp: no mouth here
                }
            }
            if (end < 0 || end >= floor.length) {
                return; // the ramp would not reach the tube's own level
            }
            for (int k = from, s = 0; k != end; k += dir, s++) {
                floor[k] = mouth - s * STEP * MOUTH_SLOPE;
            }
        }
    }

    private static Tube tube(double[] xs, double[] zs, double[] floor, double[] halves, double[] along, boolean lava) {
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (int k = 0; k < xs.length; k++) {
            minX = Math.min(minX, xs[k]);
            maxX = Math.max(maxX, xs[k]);
            minZ = Math.min(minZ, zs[k]);
            maxZ = Math.max(maxZ, zs[k]);
        }
        return new Tube(xs, zs, floor, halves, along, lava, minX, maxX, minZ, maxZ);
    }

    /**
     * The main tube's walk: a sinuous path whose heading follows the warp noise (so it bends gently and never loops), in
     * the ring 3 core, clear of the abyss, wherever a tube still fits under the ground. After {@code length} steps it looks
     * for a mouth; without one it ends blind. Null if too short.
     */
    @Nullable
    private static Path walk(VolcanicField f, Probe probe, double x, double z, double heading, double half, int length) {
        Path path = new Path(half);
        double top = Tube.clearance(half);
        double px = x;
        double pz = z;
        for (int s = 0; s <= length + MOUTH_SEARCH; s++) {
            if (s > 0) {
                double a = heading + 1.1 * f.warpAt(px * 0.7 + 3131.5, pz * 0.7 - 1717.5);
                px += Math.cos(a) * STEP;
                pz += Math.sin(a) * STEP;
            }
            if (f.regions(px, pz).core() < 0.95 || f.nearAbyss(px, pz, 12.0)) {
                break;
            }
            probe.at(px, pz);
            if (Math.min(probe.ground, probe.lava) - ROOF - top < LOWEST_FLOOR) {
                break;
            }
            path.points.add(new double[]{px, pz, probe.ground, probe.lava});
            if (s >= length && probe.mouth(px, pz)) {
                path.endMouth = true;
                break;
            }
        }
        return path.size() < MIN_POINTS ? null : path;
    }

    /** One to three skylights where the main tube runs shallow (not near its mouths, never under lava or on a volcano). */
    private static List<Skylight> skylights(VolcanicField f, Probe probe, Path path, Tube tube, double half, long h) {
        List<Skylight> found = new ArrayList<>(3);
        int n = path.size();
        double top = Tube.clearance(half);
        int window = 25;
        for (int w = 8; w + 8 < n; w += window) {
            int best = -1;
            double bestRoof = 28.0;
            for (int k = w; k < Math.min(n - 8, w + window); k++) {
                double[] p = path.get(k);
                double roof = p[2] - tube.floor()[k] - top;
                if (p[3] == Double.POSITIVE_INFINITY && roof > 4.0 && roof < bestRoof) {
                    best = k;
                    bestRoof = roof;
                }
            }
            if (best < 0) {
                continue;
            }
            double[] p = path.get(best);
            VolcanicField.Column c = f.traceColumn(p[0], p[1], probe.column);
            if (c.calderaMask > 0.0 || c.rise >= 6.0 || f.riverWithin(p[0], p[1], 12.0)) {
                continue;
            }
            int n0 = found.size();
            found.add(new Skylight(p[0], p[1], 1.4 + 1.0 * CellShapes.unit(h, 80 + n0), tube.floor()[best] + top - 2.0,
                    p[2] + 4.0, CellShapes.unit(h, 90 + n0) * Math.PI * 2.0));
        }
        return found;
    }

    private record Branch(Tube tube, @Nullable Chamber chamber) {
    }

    /**
     * The side branch: into the lake of a magma chamber somewhere in the cell (a few tries), from the point of the main
     * tube with the shortest branch that keeps a gentle slope; else a short blind branch from the main tube's lowest
     * stretch; null if neither fits.
     */
    @Nullable
    private static Branch branch(VolcanicField f, Probe probe, Tube main, long h, int i, int j) {
        double half = 2.0 + 0.8 * CellShapes.unit(h, 30);
        double a = 15.0 + 15.0 * CellShapes.unit(h, 31);
        double b = a * (0.7 + 0.3 * CellShapes.unit(h, 32));
        double dome = 8.0 + 8.0 * CellShapes.unit(h, 33);
        double bowl = 6.0 + 5.0 * CellShapes.unit(h, 34);
        double lowest = -40.0 + bowl;
        double highest = 10.0 - dome;
        double centreY = lowest + CellShapes.unit(h, 35) * (highest - lowest);
        int lake = Mth.floor(centreY - 0.55 * bowl);
        double angle = CellShapes.unit(h, 36) * Math.PI;
        double phase = CellShapes.unit(h, 37) * Math.PI * 2.0;
        int n = main.points();
        double span = CELL - 2.0 * CHAMBER_INSET;
        for (int attempt = 0; attempt < CHAMBER_TRIES; attempt++) {
            double cx = i * CELL + CHAMBER_INSET + CellShapes.unit(h, 41 + 2 * attempt) * span;
            double cz = j * CELL + CHAMBER_INSET + CellShapes.unit(h, 42 + 2 * attempt) * span;
            Chamber chamber = new Chamber(cx, cz, a, b, angle, centreY, dome, bowl, lake, phase);
            if (!chamberFits(f, chamber)) {
                continue;
            }
            // The junction: away from the mouth ramps, the nearest point whose branch keeps its slope.
            int best = -1;
            double bestDistance = MAX_BRANCH;
            for (int k = 0; k < n; k++) {
                double along = main.along()[k];
                if (along < 40.0 || along > main.along()[n - 1] - 40.0) {
                    continue;
                }
                double distance = Math.hypot(cx - main.x()[k], cz - main.z()[k]);
                double shore = distance - 0.85 * chamber.radiusTowards((main.x()[k] - cx) / distance, (main.z()[k] - cz) / distance);
                if (distance < bestDistance && shore >= 20.0 && main.floor()[k] - (lake + 1.0) <= BRANCH_SLOPE * shore) {
                    best = k;
                    bestDistance = distance;
                }
            }
            if (best < 0) {
                continue;
            }
            double px = main.x()[best];
            double pz = main.z()[best];
            double dirX = (cx - px) / bestDistance;
            double dirZ = (cz - pz) / bestDistance;
            double shore = bestDistance - 0.85 * chamber.radiusTowards(-dirX, -dirZ);
            Tube tube = branchTube(f, probe, px, pz, dirX, dirZ, main.floor()[best], shore, bestDistance, lake + 1.0, half, h);
            if (tube != null) {
                return new Branch(tube, chamber);
            }
        }
        // No chamber: a blind branch with a short gutter from the lowest stretch of the main tube, to one side.
        int from = n / 4;
        for (int k = n / 4; k < 3 * n / 4; k++) {
            if (main.floor()[k] < main.floor()[from]) {
                from = k;
            }
        }
        int next = Math.min(n - 1, from + 1);
        int prev = Math.max(0, from - 1);
        double heading = Math.atan2(main.z()[next] - main.z()[prev], main.x()[next] - main.x()[prev])
                + (CellShapes.unit(h, 38) < 0.5 ? 1.0 : -1.0) * Math.PI / 2.0;
        double length = BLIND_MIN + (BLIND_MAX - BLIND_MIN) * CellShapes.unit(h, 39);
        Tube tube = branchTube(f, probe, main.x()[from], main.z()[from], Math.cos(heading), Math.sin(heading), main.floor()[from],
                length, length, main.floor()[from] - DEAD_END_SLOPE * length, half, h);
        return tube == null ? null : new Branch(tube, null);
    }

    /** Whether a chamber lies in the ring 3 core, clear of the abyss. */
    private static boolean chamberFits(VolcanicField f, Chamber chamber) {
        if (f.nearAbyss(chamber.x(), chamber.z(), chamber.reach() + 20.0)) {
            return false;
        }
        for (int k = 0; k <= 4; k++) {
            double r = k == 4 ? 0.0 : chamber.reach();
            if (f.regions(chamber.x() + r * Math.cos(k * Math.PI / 2.0), chamber.z() + r * Math.sin(k * Math.PI / 2.0)).core() < 0.99) {
                return false;
            }
        }
        return true;
    }

    /**
     * A branch from (px, pz) along (dirX, dirZ): the floor falls evenly from {@code floorP} to {@code endFloor} over the
     * first {@code shore} blocks and stays there to {@code length}; a gentle lateral meander that vanishes at both ends.
     * Null where it would come too close to the ground, lava or the abyss.
     */
    @Nullable
    private static Tube branchTube(VolcanicField f, Probe probe, double px, double pz, double dirX, double dirZ, double floorP,
                                   double shore, double length, double endFloor, double half, long h) {
        int n = Math.max(2, (int) Math.ceil(length / BRANCH_STEP) + 1);
        double top = Tube.clearance(half);
        double wobble = MAX_WOBBLE * (0.5 + 0.5 * CellShapes.unit(h, 40));
        double[] xs = new double[n];
        double[] zs = new double[n];
        double[] floor = new double[n];
        double[] halves = new double[n];
        double[] along = new double[n];
        for (int k = 0; k < n; k++) {
            double s = length * k / (n - 1);
            double u = Math.min(1.0, s / shore);
            double off = wobble * Math.sin(Math.PI * u) * Mth.clamp(f.warpAt(px * 0.01 + s * 0.02 + 515.5, pz * 0.01 - 919.5), -1.0, 1.0);
            xs[k] = px + dirX * s - dirZ * off;
            zs[k] = pz + dirZ * s + dirX * off;
            floor[k] = floorP + (endFloor - floorP) * u;
            halves[k] = half;
            along[k] = k == 0 ? 0.0 : along[k - 1] + Math.hypot(xs[k] - xs[k - 1], zs[k] - zs[k - 1]);
            if (k > 0) {
                if (f.regions(xs[k], zs[k]).core() < 0.95 || f.nearAbyss(xs[k], zs[k], 12.0)) {
                    return null;
                }
                probe.at(xs[k], zs[k]);
                if (floor[k] + top > Math.min(probe.ground, probe.lava) - ROOF) {
                    return null;
                }
            }
        }
        return tube(xs, zs, floor, halves, along, true);
    }

    // ---------------------------------------------------------------- tiles

    /** The tubes, skylights and chambers that may touch tile (ti, tj): from the systems of every cell within reach. */
    static Tile tile(VolcanicField f, int ti, int tj) {
        double pad = 4.5 + SHELL + 2.0;
        double x0 = (double) ti * TILE - pad;
        double x1 = (double) (ti + 1) * TILE + pad;
        double z0 = (double) tj * TILE - pad;
        double z1 = (double) (tj + 1) * TILE + pad;
        // Everything of a system lies within this many blocks of its cell: the main tube walks at most
        // (MAX_STEPS + MOUTH_SEARCH) steps from a start inside the cell; a branch to a chamber ends at the chamber, which
        // lies inside the cell, so it stays within the main tube's hull and the cell; a blind branch adds BLIND_MAX; a
        // branch's meander adds at most MAX_WOBBLE.
        double extent = (MAX_STEPS + MOUTH_SEARCH) * STEP + BLIND_MAX + MAX_WOBBLE + pad;
        int reach = (int) Math.floor((extent + TILE / 2.0 + CELL) / CELL);
        int ci = Math.floorDiv((int) ((ti + 0.5) * TILE), CELL);
        int cj = Math.floorDiv((int) ((tj + 0.5) * TILE), CELL);
        List<Tube> tubes = new ArrayList<>();
        List<int[]> segments = new ArrayList<>();
        List<Skylight> skylights = new ArrayList<>();
        List<Chamber> chambers = new ArrayList<>();
        for (int i = ci - reach; i <= ci + reach; i++) {
            for (int j = cj - reach; j <= cj + reach; j++) {
                TubeSystem system = f.tubeSystem(i, j);
                for (Tube t : system.tubes()) {
                    if (t.maxX() < x0 || t.minX() > x1 || t.maxZ() < z0 || t.minZ() > z1) {
                        continue;
                    }
                    List<Integer> mine = new ArrayList<>();
                    for (int k = 0; k + 1 < t.points(); k++) {
                        if (Math.max(t.x()[k], t.x()[k + 1]) < x0 || Math.min(t.x()[k], t.x()[k + 1]) > x1
                                || Math.max(t.z()[k], t.z()[k + 1]) < z0 || Math.min(t.z()[k], t.z()[k + 1]) > z1) {
                            continue;
                        }
                        mine.add(k);
                    }
                    if (!mine.isEmpty()) {
                        tubes.add(t);
                        segments.add(mine.stream().mapToInt(Integer::intValue).toArray());
                    }
                }
                for (Skylight s : system.skylights()) {
                    if (s.x() + 6.0 >= x0 && s.x() - 6.0 <= x1 && s.z() + 6.0 >= z0 && s.z() - 6.0 <= z1) {
                        skylights.add(s);
                    }
                }
                Chamber c = system.chamber();
                if (c != null && c.x() + c.reach() >= x0 && c.x() - c.reach() <= x1 && c.z() + c.reach() >= z0
                        && c.z() - c.reach() <= z1) {
                    chambers.add(c);
                }
            }
        }
        if (tubes.isEmpty() && skylights.isEmpty() && chambers.isEmpty()) {
            return Tile.EMPTY;
        }
        return new Tile(tubes.toArray(new Tube[0]), segments.toArray(new int[0][]), skylights.toArray(new Skylight[0]),
                chambers.toArray(new Chamber[0]));
    }

    // ---------------------------------------------------------------- columns

    /** The openings, walls, lava and guard bands of every tube, skylight and chamber of {@code tile} in column (px, pz). */
    static void addTo(VolcanicField.Column c, Tile tile, double px, double pz) {
        for (int k = 0; k < tile.tubes().length; k++) {
            addTube(c, tile.tubes()[k], tile.segments()[k], px, pz);
        }
        for (Skylight s : tile.skylights()) {
            addSkylight(c, s, px, pz);
        }
        for (Chamber ch : tile.chambers()) {
            addChamber(c, ch, px, pz);
        }
    }

    /** The section of one tube through the column, from its nearest segment there. */
    private static void addTube(VolcanicField.Column c, Tube t, int[] segments, double px, double pz) {
        double best = Double.MAX_VALUE;
        int bestK = -1;
        double bestT = 0.0;
        for (int k : segments) {
            double ax = t.x()[k];
            double az = t.z()[k];
            double dx = t.x()[k + 1] - ax;
            double dz = t.z()[k + 1] - az;
            double len2 = dx * dx + dz * dz;
            double u = len2 == 0.0 ? 0.0 : Mth.clamp(((px - ax) * dx + (pz - az) * dz) / len2, 0.0, 1.0);
            double ex = ax + u * dx - px;
            double ez = az + u * dz - pz;
            double d2 = ex * ex + ez * ez;
            if (d2 < best) {
                best = d2;
                bestK = k;
                bestT = u;
            }
        }
        if (bestK < 0) {
            return;
        }
        double dist = Math.sqrt(best);
        double half = Mth.lerp(bestT, t.half()[bestK], t.half()[bestK + 1]);
        if (dist >= half + SHELL) {
            return;
        }
        int floor = Mth.floor(Mth.lerp(bestT, t.floor()[bestK], t.floor()[bestK + 1]));
        double halfHeight = Tube.halfHeight(half);
        double centre = floor + 0.6 * halfHeight;
        int[] opening = section(dist, half, halfHeight, centre, floor, 0.0);
        int[] skin = section(dist, half, halfHeight, centre, floor, SKIN);
        int[] shell = section(dist, half, halfHeight, centre, floor, SHELL);
        int crust = Integer.MIN_VALUE;
        // 42d: the ground's top block (rock lies below the column's height); a dry tube that would open through it this
        // deep gets a roof of ash crust there instead (a thin solid, so the interpolated terrain cannot leave a hole).
        int roof = Mth.ceil(c.height) - 1;
        if (!t.lava() && opening != null && opening[1] >= roof && roof - floor >= LID_DROP) {
            opening[1] = roof - 1;
            c.addSolid(roof, roof, Landmarks.KIND_CRUST);
        }
        if (t.lava()) {
            double along = Mth.lerp(bestT, t.along()[bestK], t.along()[bestK + 1]);
            boolean gutter = along >= GUTTER_START;
            if (gutter && dist < GUTTER && opening != null) {
                // The gutter: one block deeper than the floor, filled with lava.
                opening[0] = floor - 1;
                c.addSubLava(floor - 1, floor - 1);
            }
            if (gutter && dist < GUTTER + 2.0) {
                crust = floor - 1;
            }
            // Keep caves and carvers away from the gutter.
            c.addDeepGuard(floor - 5, floor + 1);
        }
        c.addCave(opening, skin, shell, crust);
    }

    /**
     * The block range of a tube's cross-section in a column at {@code dist} from its axis, grown by {@code grow} blocks
     * all round: an ellipse of half width {@code half} and half height {@code halfHeight} around {@code centre}, cut flat
     * at its floor (the first air block {@code floor}). Null if the column misses it.
     */
    @Nullable
    private static int[] section(double dist, double half, double halfHeight, double centre, int floor, double grow) {
        double w = half + grow;
        double e = 1.0 - dist * dist / (w * w);
        if (e <= 0.0) {
            return null;
        }
        double span = (halfHeight + grow) * Math.sqrt(e);
        double lo = Math.max(centre - span, floor - grow);
        double hi = centre + span;
        int bottom = (int) Math.ceil(lo - 0.5);
        int top = (int) Math.floor(hi - 0.5);
        return bottom > top ? null : new int[]{bottom, top};
    }

    private static void addSkylight(VolcanicField.Column c, Skylight s, double px, double pz) {
        double dx = px - s.x();
        double dz = pz - s.z();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double radius = s.radius() * (1.0 + 0.25 * Math.cos(3.0 * Math.atan2(dz, dx) + s.phase()));
        if (dist >= radius + SHELL) {
            return;
        }
        int bottom = Mth.floor(s.bottom());
        int top = Mth.floor(s.top());
        int[] opening = dist < radius ? new int[]{bottom, top} : null;
        int[] skin = dist < radius + SKIN ? new int[]{bottom, top} : null;
        int[] shell = new int[]{bottom, top};
        c.addCave(opening, skin, shell, Integer.MIN_VALUE);
    }

    private static void addChamber(VolcanicField.Column c, Chamber ch, double px, double pz) {
        if (Math.abs(px - ch.x()) >= ch.reach() || Math.abs(pz - ch.z()) >= ch.reach()) {
            return;
        }
        double guard = ch.distance(px, pz, SHELL + 2.0);
        if (guard >= 1.0) {
            return;
        }
        c.addDeepGuard(Mth.floor(ch.bottom() - SHELL - 2.0), Mth.ceil(ch.top() + SHELL + 2.0));
        int[] opening = chamberSection(ch, px, pz, 0.0);
        int[] skin = chamberSection(ch, px, pz, SKIN);
        int[] shell = chamberSection(ch, px, pz, SHELL);
        if (opening != null && opening[0] <= ch.lake()) {
            c.addSubLava(opening[0], ch.lake());
        }
        c.addCave(opening, skin, shell, ch.lake() + 1);
    }

    /** The block range of a chamber in a column, grown by {@code grow} blocks all round; null if the column misses it. */
    @Nullable
    private static int[] chamberSection(Chamber ch, double px, double pz, double grow) {
        double d = ch.distance(px, pz, grow);
        if (d >= 1.0) {
            return null;
        }
        double s = Math.sqrt(1.0 - d * d);
        int bottom = (int) Math.ceil(ch.centreY() - (ch.bowl() + grow) * s - 0.5);
        int top = (int) Math.floor(ch.centreY() + (ch.dome() + grow) * s - 0.5);
        return bottom > top ? null : new int[]{bottom, top};
    }
}
