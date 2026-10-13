package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the islands of ring 4 lie and what belongs to each (package 61), in two pure stages:
 * <ol>
 *   <li>{@link #place}: the islands of one grid cell, from the cell alone (hash, ring layout, base terrain): how many,
 *   how big and how high grow towards ring 5 (at the inner edge now and then, at the outer edge archipelagos); sizes
 *   (diameters) clods 5–15, middle 20–60, giants 80–150; each island's bottom 15–40 above the ground on the inside,
 *   up to ~200 on the outside. Each island with its hole and the reach of its lake stays within one cell of its centre
 *   (review 59 #6) and inside ring 4 proper (the zones are 62 and 63). A low hole whose water lies at the sea level
 *   gets an outlet to the nearest open water.</li>
 *   <li>{@link #details}: per island, from the islands of stage 1 around it and the full surface: the lake level of its
 *   hole (above the sea only on high ground, below the lowest rim), its waterfalls (middle 1–2, giants 2–3: a spring in
 *   the top block at the edge, the falling water in the column beside it down to the lake) and, for about one in four
 *   middle and giant islands, a lift column beside the edge from the lake up to above the top.</li>
 * </ol>
 */
final class IslandPlacement {
    private static final long CELL_SALT = 6101;
    private static final int MAX_PER_CELL = 5;
    /** The bottom of an island stays at least this far above the ground under it. */
    static final double MIN_CLEARANCE = 15.0;
    /** The highest top (below the top slide of the stack, 300). */
    static final double MAX_TOP = 290.0;
    /** A spring needs this many blocks of island under its top. */
    private static final int SPRING_DEPTH = 3;
    private static final double LIFT_CHANCE = 0.25;
    /** An outlet looks this far (blocks, from the hole's edge) for open water. */
    private static final double OUTLET_SEARCH = 60.0;
    private static final double OPEN_WATER = 61.0;

    private IslandPlacement() {
    }

    /**
     * The islands of grid cell (i, j): the cell's candidates ({@link #candidates}) without those that grow into a
     * candidate of a neighbouring cell that outranks them (review 61 #4; {@link #OUTRANK}: the bigger kind, then the
     * bigger island, then the centre). Pure: it reads the neighbours' candidates, never their final islands.
     */
    static List<IslandShape> place(IslandField field, int i, int j) {
        List<IslandShape> own = field.candidates(i, j);
        if (own.isEmpty()) {
            return own;
        }
        List<IslandShape> kept = new ArrayList<>();
        for (IslandShape shape : own) {
            boolean beaten = false;
            for (int di = -1; di <= 1 && !beaten; di++) {
                for (int dj = -1; dj <= 1 && !beaten; dj++) {
                    if (di == 0 && dj == 0) {
                        continue;
                    }
                    for (IslandShape other : field.candidates(i + di, j + dj)) {
                        if (!clearOf(shape, List.of(other)) && OUTRANK.compare(other, shape) > 0) {
                            beaten = true;
                            break;
                        }
                    }
                }
            }
            if (!beaten) {
                kept.add(shape);
            }
        }
        return List.copyOf(kept);
    }

    /** Which of two overlapping islands of neighbouring cells stays. */
    static final java.util.Comparator<IslandShape> OUTRANK = java.util.Comparator.comparingInt(IslandShape::kind)
            .thenComparingDouble(IslandShape::a).thenComparingDouble(IslandShape::x).thenComparingDouble(IslandShape::z);

    /** The candidates of grid cell (i, j): placed from the cell alone, clear of each other. Pure. */
    static List<IslandShape> candidates(IslandField field, int i, int j) {
        int cell = field.settings().cellSize();
        RingLayout layout = field.ringLayout();
        List<Integer> radii = layout.settings().ringOuterRadii();
        double inner = radii.get(Ring.RING_3.ordinal());
        double outer = radii.get(Ring.RING_4.ordinal());
        double cx = (i + 0.5) * cell;
        double cz = (j + 0.5) * cell;
        double distance = layout.effectiveDistance(cx, cz);
        if (distance < inner - cell || distance > outer + cell) {
            return List.of();
        }
        double p = Mth.clamp((distance - inner) / (outer - inner), 0.0, 1.0);
        long h = SeededHash.hash(field.hashNoise(), i, j, CELL_SALT);
        double expected = 0.25 + 3.25 * Math.pow(p, 1.5);
        int count = Math.min(MAX_PER_CELL, (int) expected + (SeededHash.unit(h, 0) < expected - Math.floor(expected) ? 1 : 0));
        List<IslandShape> placed = new ArrayList<>();
        for (int k = 0; k < count * 4 && placed.size() < count; k++) {
            IslandShape shape = candidate(field, layout, SeededHash.mix(h + (k + 1) * 0x9E3779B97F4A7C15L), i, j, cell, p);
            if (shape != null && clearOf(shape, placed)) {
                placed.add(shape);
            }
        }
        return List.copyOf(placed);
    }

    @Nullable
    private static IslandShape candidate(IslandField field, RingLayout layout, long h, int i, int j, int cell, double p) {
        double u = SeededHash.unit(h, 1);
        int kind = p > 0.3 && u < 0.015 + 0.06 * p ? IslandShape.GIANT : u < 0.4 + 0.2 * p ? IslandShape.MIDDLE : IslandShape.CLOD;
        double s = SeededHash.unit(h, 2);
        double diameter = switch (kind) {
            case IslandShape.GIANT -> 80.0 + 70.0 * s;
            case IslandShape.MIDDLE -> 20.0 + 40.0 * s * s;
            default -> 5.0 + 10.0 * s;
        };
        double stretch = 1.0 + (kind == IslandShape.GIANT ? 0.15 : 0.4) * SeededHash.unit(h, 3);
        double a = diameter / 2.0 / Math.sqrt(stretch);
        double x = (i + SeededHash.unit(h, 4)) * cell;
        double z = (j + SeededHash.unit(h, 5)) * cell;
        double angle = SeededHash.unit(h, 6) * Math.PI;
        int lobes = 2 + (int) (SeededHash.unit(h, 7) * 3.0);
        double phase = SeededHash.unit(h, 8) * Math.PI * 2.0;
        double share = kind == IslandShape.GIANT ? 0.45 : kind == IslandShape.MIDDLE ? 0.55 : 0.7;
        double thickness = diameter * share / IslandShape.MAX_UNDERSIDE;
        IslandShape probe = new IslandShape(x, z, a, stretch, angle, lobes, phase, 0.0, thickness, 0.0, kind, Double.NaN, Double.NaN);
        if (probe.groundReach() > cell || !insideRing4(layout, x, z, probe.groundReach())) {
            return null;
        }
        // The ground the island was torn from (its centre) and the highest ground under its footprint.
        double groundY = field.baseHeight(x, z);
        double highest = Math.max(groundY, IslandShape.SEA_LEVEL);
        double reach = probe.reach();
        for (int n = 0; n < 8; n++) {
            double t = n * Math.PI / 4.0;
            highest = Math.max(highest, field.baseHeight(x + reach * Math.cos(t), z + reach * Math.sin(t)));
        }
        double v = SeededHash.unit(h, 9);
        double low = MIN_CLEARANCE + 25.0 * v;
        double high = MIN_CLEARANCE + 185.0 * v;
        double clearance = Mth.lerp(Math.pow(p, 1.3), low, high);
        double top = highest + clearance + thickness * IslandShape.MAX_UNDERSIDE + 1.0;
        top = Math.min(top, MAX_TOP);
        if (top - 1.0 - thickness * IslandShape.MAX_UNDERSIDE < highest + MIN_CLEARANCE) {
            return null;
        }
        double[] outlet = outlet(field, probe, x, z, groundY, cell);
        return new IslandShape(x, z, a, stretch, angle, lobes, phase, top, thickness, groundY, kind,
                outlet == null ? Double.NaN : outlet[0], outlet == null ? Double.NaN : outlet[1]);
    }

    /** Whether a disc of {@code reach} around (x, z) lies in ring 4 proper (no transition zone). */
    private static boolean insideRing4(RingLayout layout, double x, double z, double reach) {
        if (!inRing4(layout.sampleOverworld(x, z))) {
            return false;
        }
        for (int n = 0; n < 8; n++) {
            double t = n * Math.PI / 4.0;
            if (!inRing4(layout.sampleOverworld(x + reach * Math.cos(t), z + reach * Math.sin(t)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean inRing4(RingPosition position) {
        return !position.inTransition() && position.ring() == Ring.RING_4;
    }

    /** No two islands of a cell overlap where their heights meet (6 blocks apart at least). */
    private static boolean clearOf(IslandShape shape, List<IslandShape> placed) {
        for (IslandShape other : placed) {
            double reach = shape.reach() + other.reach();
            boolean apart = Math.hypot(shape.x() - other.x(), shape.z() - other.z()) > reach;
            boolean stacked = shape.lowest() > other.highest() + 6 || other.lowest() > shape.highest() + 6;
            if (!apart && !stacked) {
                return false;
            }
        }
        return true;
    }

    /**
     * The far end of a low hole's outlet: where its water stands at the sea level (its floor below it, its ground low),
     * the nearest point of open water within {@link #OUTLET_SEARCH} of its edge along 16 directions, kept inside the
     * cell's reach. Null for none.
     */
    @Nullable
    private static double[] outlet(IslandField field, IslandShape probe, double x, double z, double groundY, int cell) {
        if (groundY < IslandShape.SEA_LEVEL || groundY > 75.0 || groundY - probe.thickness() * 0.6 > IslandShape.SEA_LEVEL - 2.0) {
            return null;
        }
        double edge = probe.holeRadius();
        double limit = cell - edge * 0.2 - IslandShape.OUTLET_HALF_WIDTH - IslandShape.OUTLET_BANK - 2.0;
        double best = Double.MAX_VALUE;
        double[] found = null;
        for (int n = 0; n < 16; n++) {
            double t = n * Math.PI / 8.0;
            for (double r = edge + 6.0; r <= Math.min(edge + OUTLET_SEARCH, limit); r += 6.0) {
                if (r >= best) {
                    break;
                }
                double px = x + r * Math.cos(t);
                double pz = z + r * Math.sin(t);
                if (field.baseHeight(px, pz) < OPEN_WATER) {
                    best = r;
                    found = new double[]{px, pz};
                    break;
                }
            }
        }
        return found;
    }

    // ---------------------------------------------------------------- stage 2

    /**
     * The lake, falls and lift of an island (stage 2). The lake's top (exclusive) lies one block under the lowest
     * surface of the 4-block grid in a band from the hole's edge to 14 blocks past it (with the other islands' holes
     * cut in, review 61 #3) and under the floor of any peat sinkhole near that band: that grid is what the terrain
     * interpolates, so no open space beside the lake's band ({@link IslandShape#LAKE_BAND}) lies below its water. A lake
     * whose water would not rise above the sea's top block (Y 62) needs none of its own: the sea fills it.
     */
    static IslandField.Details details(IslandField field, IslandShape shape) {
        double radius = shape.holeRadius();
        double to = 1.0 + 14.0 / radius;
        double reach = shape.reach() * IslandShape.HOLE_SCALE + 15.0;
        int minX = Mth.floor((shape.x() - reach) / 4.0) * 4;
        int minZ = Mth.floor((shape.z() - reach) / 4.0) * 4;
        double rim = Double.POSITIVE_INFINITY;
        for (int cx = minX; cx <= shape.x() + reach; cx += 4) {
            for (int cz = minZ; cz <= shape.z() + reach; cz += 4) {
                double dh = shape.holeDistance(cx, cz);
                if (dh >= 1.0 && dh <= to) {
                    rim = Math.min(rim, field.surfaceUncached(cx, cz));
                }
            }
        }
        rim = Math.min(rim, field.sinkholeFloorWithin(shape.x(), shape.z(), reach));
        int lakeTop = Mth.floor(rim) - 1;
        if (lakeTop <= IslandShape.SEA_LEVEL) {
            lakeTop = Integer.MIN_VALUE;
        }
        // The highest block of water under the island: the lake's or the sea's (whose top block is Y 62).
        int waterTop = lakeTop == Integer.MIN_VALUE ? (int) IslandShape.SEA_LEVEL - 1 : lakeTop - 1;
        int lakeBottom = Mth.floor(shape.deepestFloor());
        long h = SeededHash.hash(field.hashNoise(), Mth.floor(shape.x()), Mth.floor(shape.z()), CELL_SALT + 1);
        List<IslandField.Fall> falls = new ArrayList<>();
        IslandField.Lift lift = null;
        if (shape.kind() != IslandShape.CLOD) {
            int wanted = shape.kind() == IslandShape.GIANT
                    ? 2 + (SeededHash.unit(h, 0) < 0.5 ? 1 : 0) : 1 + (SeededHash.unit(h, 0) < 0.4 ? 1 : 0);
            double start = SeededHash.unit(h, 1) * Math.PI * 2.0;
            int tries = 12;
            for (int k = 0; k < tries && falls.size() < wanted; k++) {
                // Spread around the island: golden-angle steps from a seeded start.
                double t = start + k * 2.399963;
                IslandField.Fall fall = fall(field, shape, t, waterTop, lakeTop);
                if (fall != null && falls.stream().noneMatch(f -> Math.abs(f.fallX() - fall.fallX()) + Math.abs(f.fallZ() - fall.fallZ()) < 6)) {
                    falls.add(fall);
                }
            }
            if (SeededHash.unit(h, 2) < LIFT_CHANCE) {
                for (int k = 0; k < 6 && lift == null; k++) {
                    lift = lift(field, shape, start + Math.PI + k * 1.1, waterTop, lakeTop, falls);
                }
            }
        }
        return new IslandField.Details(lakeTop, lakeBottom, List.copyOf(falls), lift);
    }

    /** Whether the column holds the island's water up to {@code waterTop} (inside its lake, or the sea). */
    private static boolean underWater(IslandField field, IslandShape shape, int x, int z, int waterTop, int lakeTop) {
        // Columns are sampled on block positions, like the terrain's cell corners.
        if (lakeTop != Integer.MIN_VALUE && shape.holeDistance(x, z) >= 1.0 + IslandShape.LAKE_BAND / shape.holeRadius()) {
            return false;
        }
        // Another island's hole holds its own water (review 61 #3: a column takes the lake of the hole it lies in).
        for (IslandShape other : field.shapesNear(x, z)) {
            if (!other.equals(shape) && other.holeDistance(x, z) < 1.0) {
                return false;
            }
        }
        // One block of margin for the terrain's interpolation.
        return field.surfaceUncached(x, z) <= waterTop - 1.0;
    }

    /** The cardinal step closest to angle {@code t}. */
    private static int[] step(double t) {
        return Math.abs(Math.cos(t)) >= Math.abs(Math.sin(t))
                ? new int[]{Math.cos(t) > 0 ? 1 : -1, 0} : new int[]{0, Math.sin(t) > 0 ? 1 : -1};
    }

    /**
     * The edge column of the island along angle {@code t}: the outermost block of the island whose neighbour one
     * {@code step} further holds none of it, as {x, z, highest block, lowest block}; null if none.
     */
    @Nullable
    private static int[] edge(IslandShape shape, double t, int[] step) {
        int[] last = null;
        for (double r = 0.0; r <= shape.reach(); r += 0.5) {
            int bx = Mth.floor(shape.x() + r * Math.cos(t));
            int bz = Mth.floor(shape.z() + r * Math.sin(t));
            int[] interval = shape.interval(bx + 0.5, bz + 0.5);
            if (interval != null && shape.interval(bx + step[0] + 0.5, bz + step[1] + 0.5) == null) {
                last = new int[]{bx, bz, interval[1], interval[0]};
            }
        }
        return last;
    }

    /**
     * A waterfall along angle {@code t}: the spring one block under the top of the edge column (island above, below
     * and on its other three sides, so its water only leaves outwards), the fall in the column beside it, free of
     * islands from the spring down to the water, which must stand under it.
     */
    @Nullable
    private static IslandField.Fall fall(IslandField field, IslandShape shape, double t, int waterTop, int lakeTop) {
        int[] step = step(t);
        int[] edge = edge(shape, t, step);
        if (edge == null || edge[2] - edge[3] < SPRING_DEPTH - 1) {
            return null;
        }
        int sx = edge[0];
        int sz = edge[1];
        int springY = edge[2] - 1;
        int[][] walls = {{-step[0], -step[1]}, {step[1], step[0]}, {-step[1], -step[0]}};
        for (int[] wall : walls) {
            int[] interval = shape.interval(sx + wall[0] + 0.5, sz + wall[1] + 0.5);
            if (interval == null || interval[1] < springY || interval[0] > springY) {
                return null;
            }
        }
        int fx = sx + step[0];
        int fz = sz + step[1];
        if (springY <= waterTop + 4 || !underWater(field, shape, fx, fz, waterTop, lakeTop)
                || !clearOfOthers(field, shape, fx, fz, waterTop, springY + 1)) {
            return null;
        }
        return new IslandField.Fall(sx, sz, springY, fx, fz, waterTop + 1);
    }

    /**
     * A lift beside the island's edge along angle {@code t}, two blocks out: open water under it, nothing in the way up
     * to two blocks above the edge, away from the falls.
     */
    @Nullable
    private static IslandField.Lift lift(IslandField field, IslandShape shape, double t, int waterTop, int lakeTop,
                                         List<IslandField.Fall> falls) {
        int[] step = step(t);
        int[] edge = edge(shape, t, step);
        if (edge == null) {
            return null;
        }
        int lx = edge[0] + 2 * step[0];
        int lz = edge[1] + 2 * step[1];
        int top = edge[2] + 2;
        boolean nearFall = falls.stream().anyMatch(f -> Math.abs(f.fallX() - lx) + Math.abs(f.fallZ() - lz) < 6);
        if (nearFall || shape.interval(lx + 0.5, lz + 0.5) != null || !underWater(field, shape, lx, lz, waterTop, lakeTop)
                || !clearOfOthers(field, shape, lx, lz, waterTop, top)) {
            return null;
        }
        return new IslandField.Lift(lx, lz, waterTop, top);
    }

    /** Whether no other island has a block in the column between {@code bottom} and {@code top}. */
    private static boolean clearOfOthers(IslandField field, IslandShape shape, int x, int z, int bottom, int top) {
        for (IslandShape other : field.shapesNear(x, z)) {
            if (other.equals(shape)) {
                continue;
            }
            int[] interval = other.interval(x + 0.5, z + 0.5);
            if (interval != null && interval[1] >= bottom && interval[0] <= top) {
                return false;
            }
        }
        return true;
    }
}
