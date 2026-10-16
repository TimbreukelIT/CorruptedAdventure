package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the world trees of ring 4 stand (74, landmark; decision 2026-10-10): about one per {@link #ARC_CELL} blocks
 * along the ring (measured on ring 4's inner radius: ~15 round the world), only well inside the mythical band and
 * never in a zone, each with its half torn-out island ({@link IslandShape#TREE}) and its falls along the roots. Pure:
 * a function of the seed (the field's hash noise and relief) and the ring layout, decided once per world.
 * <p>
 * Each arc cell tries up to {@link #TRIES} seeded sites and keeps the first that fits: the band at the trunk and round
 * the tree's whole reach at most {@link #BAND_MAX} (clearly mythical), ring 4 proper (no transition) up to
 * {@link #ZONE_MARGIN} past its reach, ground between {@link #GROUND_MIN} and {@link #GROUND_MAX}, and room for at least
 * {@link #MIN_HEIGHT} blocks of tree under {@link #MAX_CROWN}. A cell without a fitting site has no tree.
 */
final class WorldTrees {
    /** About one world tree per this many blocks along the ring (on its inner radius). */
    static final double ARC_CELL = 2500.0;
    static final int TRIES = 16;
    /** The band value at most (−1 mythical … 1 blighted): clearly inside the mythical band. */
    static final double BAND_MAX = -0.2;
    /** Blocks of ring 4 proper (no transition zone) past the tree's reach. */
    static final double ZONE_MARGIN = 24.0;
    static final double GROUND_MIN = 44.0;
    static final double GROUND_MAX = 96.0;
    static final double MIN_HEIGHT = 80.0;
    static final double MAX_HEIGHT = 140.0;
    /** The highest block a crown may reach (under the stack's top slide at Y 300). */
    static final int MAX_CROWN = 298;
    /** The tree's island: diameter, clearance of its lowest lobe over the ground under it. */
    static final double ISLAND_MIN = 52.0;
    static final double ISLAND_SPREAD = 16.0;
    private static final double CLEARANCE = 15.0;
    private static final double CLEARANCE_SPREAD = 6.0;
    /** Falls per tree at most, and the sideways offset (blocks, past a root's radius) of a fall beside its root. */
    static final int FALLS = 3;
    private static final double FALL_BESIDE = 3.0;
    private static final long SALT = 7401;

    private WorldTrees() {
    }

    /** All world trees of the world, in arc-cell order. */
    static List<WorldTreeShape> place(IslandField field) {
        RingLayout layout = field.ringLayout();
        List<Integer> radii = layout.settings().ringOuterRadii();
        double inner = radii.get(Ring.RING_3.ordinal());
        double outer = radii.get(Ring.RING_4.ordinal());
        int cells = RingArc.cellCount(RingArc.perimeter(inner), ARC_CELL);
        double from = inner + layout.settings().transitionWidth(Ring.RING_3.ordinal()) / 2.0 + 100.0;
        double to = inner + 0.45 * (outer - inner);
        List<WorldTreeShape> trees = new ArrayList<>();
        for (int k = 0; k < cells; k++) {
            for (int attempt = 0; attempt < TRIES; attempt++) {
                long h = SeededHash.hash(field.hashNoise(), k, attempt, SALT);
                double angle = (k + 0.1 + 0.8 * SeededHash.unit(h, 0)) * Math.PI * 2.0 / cells;
                double r = from + (to - from) * SeededHash.unit(h, 1);
                WorldTreeShape tree = site(field, layout, r * Math.cos(angle), r * Math.sin(angle), h);
                if (tree != null) {
                    trees.add(tree);
                    break;
                }
            }
        }
        return List.copyOf(trees);
    }

    /** The tree at (x, z) if the site fits, else null. */
    @Nullable
    private static WorldTreeShape site(IslandField field, RingLayout layout, double x, double z, long h) {
        Ring4Band band = Ring4Band.of(layout);
        if (band.value(x, z) > BAND_MAX || !inRing4(layout, x, z)) {
            return null;
        }
        double groundY = field.baseHeight(x, z);
        if (groundY < GROUND_MIN || groundY > GROUND_MAX) {
            return null;
        }
        IslandShape island = island(field, x, z, groundY, h);
        if (island == null || !watered(field, island, groundY)) {
            return null;
        }
        double height = Math.min(MIN_HEIGHT + (MAX_HEIGHT - MIN_HEIGHT) * SeededHash.unit(h, 9), MAX_CROWN - island.highest());
        if (height < MIN_HEIGHT) {
            return null;
        }
        WorldTreeShape tree = new WorldTreeShape(x, z, island, height, SeededHash.mix(h + 0x5DEECE66DL), field::baseHeight);
        for (int n = 0; n < 16; n++) {
            double t = n * Math.PI / 8.0;
            double bx = x + tree.reach * Math.cos(t);
            double bz = z + tree.reach * Math.sin(t);
            double mx = x + (tree.reach + ZONE_MARGIN) * Math.cos(t);
            double mz = z + (tree.reach + ZONE_MARGIN) * Math.sin(t);
            if (band.value(bx, bz) > BAND_MAX || !inRing4(layout, mx, mz)) {
                return null;
            }
        }
        return tree;
    }

    /** The half torn-out island under the tree, like {@link IslandPlacement}'s giants but smaller and lower. */
    @Nullable
    private static IslandShape island(IslandField field, double x, double z, double groundY, long h) {
        int cell = field.settings().cellSize();
        double diameter = ISLAND_MIN + ISLAND_SPREAD * SeededHash.unit(h, 2);
        double stretch = 1.0 + 0.12 * SeededHash.unit(h, 3);
        double a = diameter / 2.0 / Math.sqrt(stretch);
        double angle = SeededHash.unit(h, 4) * Math.PI;
        int lobes = 3 + (SeededHash.unit(h, 5) < 0.5 ? 0 : 1);
        double phase = SeededHash.unit(h, 6) * Math.PI * 2.0;
        double thickness = diameter * 0.45 / IslandShape.MAX_UNDERSIDE;
        IslandShape probe = new IslandShape(x, z, a, stretch, angle, lobes, phase, 0.0, thickness, 0.0, IslandShape.TREE,
                Double.NaN, Double.NaN);
        if (probe.groundReach() > cell) {
            return null;
        }
        double highest = Math.max(groundY, IslandShape.SEA_LEVEL);
        double reach = probe.reach();
        for (int n = 0; n < 8; n++) {
            double t = n * Math.PI / 4.0;
            highest = Math.max(highest, field.baseHeight(x + reach * Math.cos(t), z + reach * Math.sin(t)));
        }
        double clearance = CLEARANCE + CLEARANCE_SPREAD * SeededHash.unit(h, 7);
        double top = highest + clearance + thickness * IslandShape.MAX_UNDERSIDE + 1.0;
        if (top > IslandPlacement.MAX_TOP) {
            return null;
        }
        double[] outlet = IslandPlacement.outlet(field, probe, x, z, groundY, cell);
        return new IslandShape(x, z, a, stretch, angle, lobes, phase, top, thickness, groundY, IslandShape.TREE,
                outlet == null ? Double.NaN : outlet[0], outlet == null ? Double.NaN : outlet[1]);
    }

    /**
     * Whether the island's hole will hold water up to its edge, so falls can pour from the island into it (an estimate
     * on the base terrain: the islands' details may not be asked while the trees are placed): ground at the sea, so the
     * sea fills the hole; or a rim all round at least {@link #RIM_MIN} (a lake of its own, 61) that the ground at the
     * centre does not tower over.
     */
    private static boolean watered(IslandField field, IslandShape island, double groundY) {
        if (groundY <= IslandShape.SEA_LEVEL + 1.0) {
            return true;
        }
        double rim = Double.POSITIVE_INFINITY;
        double hole = island.holeRadius();
        for (double r : new double[]{hole + 3.0, hole + 10.0}) {
            for (int n = 0; n < 16; n++) {
                double t = n * Math.PI / 8.0;
                rim = Math.min(rim, field.baseHeight(island.x() + r * Math.cos(t), island.z() + r * Math.sin(t)));
            }
        }
        return rim >= RIM_MIN && groundY <= rim + 2.0;
    }

    /** The lowest rim (base terrain) round a tree's hole for a lake of its own. */
    private static final double RIM_MIN = 66.0;

    private static boolean inRing4(RingLayout layout, double x, double z) {
        RingPosition position = layout.sampleOverworld(x, z);
        return !position.inTransition() && position.ring() == Ring.RING_4;
    }

    /**
     * The details of a tree's island: its lake and lift as for any island ({@link IslandPlacement#details}), but its
     * falls beside its roots (the falls along the roots): up to {@link #FALLS}, each tried just to either side of a
     * root, kept where the tree leaves its spring and its whole fall free (a root or the trunk never stands in the
     * water). The lift stays only where the tree leaves it free too.
     */
    static IslandField.Details details(IslandField field, WorldTreeShape tree) {
        IslandShape island = tree.island;
        IslandField.Details plain = IslandPlacement.details(field, island);
        int lakeTop = plain.lakeTop();
        int waterTop = lakeTop == Integer.MIN_VALUE ? (int) IslandShape.SEA_LEVEL - 1 : lakeTop - 1;
        List<IslandField.Fall> falls = new ArrayList<>();
        for (WorldTreeShape.Tube root : tree.roots) {
            if (falls.size() >= FALLS) {
                break;
            }
            double rx = root.px()[0] - tree.x;
            double rz = root.pz()[0] - tree.z;
            double theta = Math.atan2(rz, rx);
            double edge = Math.max(8.0, island.a());
            double offset = (root.pr()[0] + FALL_BESIDE) / edge;
            for (int side = -1; side <= 1 && falls.size() < FALLS; side += 2) {
                IslandField.Fall fall = IslandPlacement.fall(field, island, theta + side * offset, waterTop, lakeTop);
                if (fall != null && free(tree, fall) && falls.stream().noneMatch(f -> Math.abs(f.fallX() - fall.fallX())
                        + Math.abs(f.fallZ() - fall.fallZ()) < 6)) {
                    falls.add(fall);
                    break;
                }
            }
        }
        IslandField.Lift lift = plain.lift();
        if (lift != null && !tree.clear(lift.x(), lift.z(), lift.bottom(), lift.top())) {
            lift = null;
        }
        return new IslandField.Details(lakeTop, plain.lakeBottom(), List.copyOf(falls), lift);
    }

    /** Whether the tree leaves a fall's spring and its falling column free. */
    static boolean free(WorldTreeShape tree, IslandField.Fall fall) {
        return tree.clear(fall.springX(), fall.springZ(), fall.springY(), fall.springY())
                && tree.clear(fall.fallX(), fall.fallZ(), fall.bottom() - 1, fall.springY() + 1);
    }

    /** The tree whose island this is, or null. */
    @Nullable
    static WorldTreeShape of(List<WorldTreeShape> trees, IslandShape island) {
        for (WorldTreeShape tree : trees) {
            if (tree.island.equals(island)) {
                return tree;
            }
        }
        return null;
    }

    /**
     * Whether a candidate island would grow into a tree: into its crown or roots, or with its hole into the tree's
     * island's hole (the roots' anchors lie within that reach).
     */
    static boolean clashes(WorldTreeShape tree, IslandShape candidate) {
        double d = Math.hypot(candidate.x() - tree.x, candidate.z() - tree.z);
        return d < tree.reach + candidate.reach() + 6.0 || d < tree.island.groundReach() + candidate.groundReach();
    }

    /** Whether the grid cell (i, j) holds the tree's island (its centre). */
    static boolean inCell(WorldTreeShape tree, int cellSize, int i, int j) {
        return Math.floorDiv(Mth.floor(tree.x), cellSize) == i && Math.floorDiv(Mth.floor(tree.z), cellSize) == j;
    }
}
