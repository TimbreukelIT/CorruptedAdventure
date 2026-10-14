package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * {@code sundered_reach} (package 63): across the zone 4→5 the ground breaks up. The zone's inner edge is calm; from
 * there on the ground is cut into chunks by a jittered grid of {@link #CHUNK_CELL} blocks, the cracks between them
 * widen outwards ({@link #crack}), and more and more chunks come loose and float above an abyss whose floor lies at
 * {@link #ABYSS_FLOOR}, under a still sea up to Y {@link #SEA_TOP}. Narrow land bridges ({@link #bridge}) stay whole
 * across the zone. Pure: functions of the layout, the salt and x, z.
 */
final class Ring4Sundered {
    /** The abyss's floor, and the top block of its still sea. */
    static final double ABYSS_FLOOR = -20.0;
    static final int SEA_TOP = 0;
    /**
     * The ground of the zone (and of a band {@link #RIM_BEFORE} blocks before it in ring 4) never lies below this (5a):
     * the zone's sea stands at {@link #SEA_TOP}, so ground there under Y 63 would be dry pits beside the cracks, and ring
     * 4's sea (Y 62) stops against this rim instead of standing beside the zone's air.
     */
    static final double RIM_TOP = 64.0;
    static final double RIM_BEFORE = 12.0;
    /** From this share of the zone on the ground is fully broken. */
    static final double FULL_AT = 0.55;
    /**
     * How far (blocks) the grid is bent before the chunks are found (5a): the cracks run along curved edges, not
     * axis-aligned ones.
     */
    static final double WARP = 10.0;
    /** The scale of the bending (×3 the warp noise's: ~40 blocks per wiggle). */
    static final double WARP_SCALE = 3.0;
    /**
     * A loose chunk's outline lies this far (blocks) inside its cell's edge, more or less ({@link #RAG}): ragged and
     * rounded instead of the cell's straight edges; between them the abyss.
     */
    static final double LOOSE_MARGIN = 2.5;
    static final double RAG = 2.5;
    /** Over this many blocks from its outline a loose chunk grows to its full thickness (a rounded rim). */
    static final double LOOSE_TAPER = 8.0;
    /** The ground breaks into chunks around the points of a jittered grid of this size. */
    static final int CHUNK_CELL = 36;
    /** A land bridge every ~{@link #BRIDGE_CELL} blocks along the zone (jittered by {@link #BRIDGE_JITTER}). */
    static final int BRIDGE_CELL = 650;
    static final double BRIDGE_JITTER = 100.0;
    static final double BRIDGE_HALF_MIN = 4.0;
    static final double BRIDGE_HALF_MAX = 6.0;
    private static final int CHUNK_SALT = 6301;
    private static final int LOOSE_SALT = 6303;
    private static final int BRIDGE_SALT = 6302;

    private Ring4Sundered() {
    }

    /** The inner edge of the zone 4→5 (effective distance). */
    static double zoneStart(RingLayout layout) {
        double r4 = layout.settings().ringOuterRadii().get(Ring.RING_4.ordinal());
        return r4 - layout.settings().transitionWidth(Ring.RING_4.ordinal()) / 2.0;
    }

    /** Whether an effective distance lies in the rim and the zone, where the ground is at least {@link #RIM_TOP}. */
    static boolean inRim(RingLayout layout, double effective) {
        return effective >= zoneStart(layout) - RIM_BEFORE && effective <= zoneEnd(layout);
    }

    /** The outer edge of the zone 4→5 (effective distance). */
    static double zoneEnd(RingLayout layout) {
        double r4 = layout.settings().ringOuterRadii().get(Ring.RING_4.ordinal());
        return r4 + layout.settings().transitionWidth(Ring.RING_4.ordinal()) / 2.0;
    }

    /** 0..1: how far the ground has broken up at an effective distance (0 at the zone's inner edge). */
    static double strength(RingLayout layout, double effective) {
        double start = zoneStart(layout);
        double t = (effective - start) / (zoneEnd(layout) - start);
        return Ring4Relief.smooth(t, 0.0, FULL_AT);
    }

    /** The point of chunk cell (i, j): {x, z}. */
    static double[] point(long salt, int i, int j) {
        long h = CellShapes.cell(salt, i, j, CHUNK_SALT);
        return new double[]{(i + 0.15 + 0.7 * CellShapes.unit(h, 0)) * CHUNK_CELL,
                (j + 0.15 + 0.7 * CellShapes.unit(h, 1)) * CHUNK_CELL};
    }

    /** The chunk a column belongs to: {i, j, half the gap to the nearest other chunk's point (the edge's distance)}. */
    static double[] chunk(long salt, double x, double z) {
        int ci = Math.floorDiv(Mth.floor(x), CHUNK_CELL);
        int cj = Math.floorDiv(Mth.floor(z), CHUNK_CELL);
        double best = Double.MAX_VALUE;
        double second = Double.MAX_VALUE;
        int bi = ci;
        int bj = cj;
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                double[] p = point(salt, ci + di, cj + dj);
                double d = Math.hypot(x - p[0], z - p[1]);
                if (d < best) {
                    second = best;
                    best = d;
                    bi = ci + di;
                    bj = cj + dj;
                } else if (d < second) {
                    second = d;
                }
            }
        }
        return new double[]{bi, bj, (second - best) / 2.0};
    }

    /** The extra half width of a crack at full strength (blocks, on top of 0.5). */
    static final double CRACK_EXTRA = 2.5;
    /** The share of chunks that come loose at full strength. */
    static final double LOOSE_CHANCE = 0.25;

    /** The half width (blocks) of the cracks at a strength: none at 0, 0.5 + {@link #CRACK_EXTRA} at 1. */
    static double crack(double strength) {
        return strength <= 0.0 ? 0.0 : 0.5 + CRACK_EXTRA * strength;
    }

    /**
     * A loose chunk at a strength: {offset of its top above the ground, thickness}, or null while it holds. A chunk comes
     * loose once the strength passes its own threshold (0.3–0.9); loose chunks float higher or lower, more so outwards.
     */
    static double[] loose(long salt, int i, int j, double strength) {
        long h = CellShapes.cell(salt, i, j, LOOSE_SALT);
        // Only about one chunk in four comes loose at all (the rest stay ground, so the abyss keeps to its share).
        if (CellShapes.unit(h, 3) >= LOOSE_CHANCE) {
            return null;
        }
        double threshold = 0.3 + 0.6 * CellShapes.unit(h, 0);
        if (strength < threshold) {
            return null;
        }
        double reach = 15.0 + 70.0 * strength;
        double offset = (CellShapes.unit(h, 1) * 2.0 - 0.6) * reach;
        double thickness = 8.0 + 22.0 * CellShapes.unit(h, 2);
        return new double[]{offset, thickness};
    }

    /** The loop round the zone (blocks): the seam of the angle lies on it, and the bridge cells wrap round it. */
    static double perimeter(RingLayout layout) {
        return 2.0 * Math.PI * (zoneStart(layout) + zoneEnd(layout)) / 2.0;
    }

    /** The position along the zone (blocks), in [0, perimeter): continuous across the seam of the angle. */
    static double along(RingLayout layout, double x, double z) {
        return Ring4Cliff.wrap(Math.atan2(z, x) * (zoneStart(layout) + zoneEnd(layout)) / 2.0, perimeter(layout));
    }

    /** The land bridge of bridge cell {@code k} (taken modulo the count round the loop): {position (blocks), half width}. */
    static double[] bridge(long salt, int k, double period) {
        int count = Ring4Cliff.cellCount(period, BRIDGE_CELL);
        double cellSize = period / count;
        int kk = Math.floorMod(k, count);
        long h = CellShapes.cell(salt, kk, 0, BRIDGE_SALT);
        double along = (kk + 0.5) * cellSize + (CellShapes.unit(h, 0) - 0.5) * 2.0 * BRIDGE_JITTER;
        double half = BRIDGE_HALF_MIN + (BRIDGE_HALF_MAX - BRIDGE_HALF_MIN) * CellShapes.unit(h, 1);
        return new double[]{along, half};
    }

    /** Whether a position along the zone (blocks) lies on a land bridge, which crosses the zone whole (round the seam too). */
    static boolean onBridge(long salt, double along, double period) {
        int count = Ring4Cliff.cellCount(period, BRIDGE_CELL);
        int k = (int) Math.floor(along / (period / count));
        for (int dk = -1; dk <= 1; dk++) {
            double[] bridge = bridge(salt, k + dk, period);
            if (Ring4Cliff.loopDistance(along, bridge[0], period) <= bridge[1]) {
                return true;
            }
        }
        return false;
    }
}
