package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

/**
 * The shape numbers of ring 5's outer band and outer edge (package 84, V2-A, V3-A, V4-A, V6-A, V12-A): one place for
 * them, like {@code Ring4Band}. {@link Ring5Field} builds the terrain from these; the functions here are pure.
 * <p>
 * The ground of the outer band breaks into floes: a jittered grid of {@link #FLOE_CELL} blocks, each cell either a floe
 * or void, with the void's {@link #share} growing from 0 at the band line to {@link #EDGE_SHARE} at ring 5's edge and to
 * all void a little beyond it. Neighbouring floe cells join, so floes come in all sizes. Floating chunks
 * ({@link #FRAGMENT_CELL}), narrow ridges and strips of stepping stones ({@link #RIDGE_CELL}) lie over the void.
 * <p>
 * The outer progress {@code p}: 0 on the band line, 1 on ring 5's outer boundary (both with their noise), beyond it the
 * edge progress {@code q}: 0 on the boundary, 1 where the edge is empty ({@link #EMPTY_SHARE} of the way to the border).
 */
final class Ring5Outer {
    /** The floes' grid (blocks). */
    static final int FLOE_CELL = 40;
    /** How far (blocks) the floe grid is bent before the cells are found: ragged edges instead of straight ones. */
    static final double FLOE_WARP = 9.0;
    /** And bent again on a small scale (blocks): the edges fray instead of running as smooth curves. */
    static final double FLOE_RAG = 2.5;
    /** No void before this outer progress (the floes join the inner band's ground). */
    static final double VOID_FROM = 0.04;
    /** The void's share of the ground at ring 5's outer boundary (V2-A: ~85 %). */
    static final double EDGE_SHARE = 0.86;
    /** The share's curve between the band line and the edge (below 1: rising fast at first). */
    static final double SHARE_POWER = 0.85;
    /** Beyond the boundary the last floes go within this many blocks. */
    static final double FLOES_GONE = 140.0;
    /** The outer edge is empty from this share of the way between ring 5's boundary and the border (V2-A: ~10,050). */
    static final double EMPTY_SHARE = 0.7;

    /** Floe tops (V4-A: Y ~70–110): at the band line, the rise to the edge, the relief noise's reach, the limits. */
    static final double TOP_LINE = 80.0;
    static final double TOP_RISE = 10.0;
    static final double TOP_RELIEF = 26.0;
    static final double TOP_MIN = 70.0;
    static final double TOP_MAX = 110.0;
    /** Over this many blocks past the band line the floes' top runs from the inner band's surface to their own. */
    static final double LINE_BLEND = 96.0;
    /** Floe undersides (V3-A): Y −40 at the band line; at the edge Y ~16 in the middle of a floe and 20–40 on average with the thinning to its rims. */
    static final double BOTTOM_LINE = -40.0;
    static final double BOTTOM_EDGE = 16.0;
    static final double BOTTOM_RAG = 7.0;
    /** Never lower than this (bedrock stays off the floes: vanilla's floor is Y −64 to −60), never thinner than this. */
    static final double BOTTOM_MIN = -56.0;
    static final double MIN_THICKNESS = 6.0;
    /** Towards its edge (within {@link #EDGE_REACH} blocks) a floe thins to this share less: a rounded underside. */
    static final double EDGE_THIN = 0.5;
    static final double EDGE_REACH = 20.0;

    /** The shapes of {@code void_scars} and {@code inverted_hills} fade in over this much of the region axis (±0.04). */
    static final double REGION_BLEND = 0.04;
    /** {@code void_scars}: cracks along the zero lines of the scar noise, this wide (noise units) at full weight. */
    static final double CRACK_WIDTH = 0.022;
    /** {@code void_scars}: a crater per cell with this chance, radius (blocks) 4–15 (Ø 8–30). */
    static final int CRATER_CELL = 112;
    static final double CRATER_CHANCE = 0.45;
    static final double CRATER_MIN_RADIUS = 4.0;
    static final double CRATER_MAX_RADIUS = 15.0;
    /** How far (blocks) the {@code scars} value runs out from an opening (for voidstone rims). */
    static final double SCAR_RIM = 3.0;
    /** {@code inverted_hills}: an upside-down hill per cell with this chance, radius 12–26, 10–40 long. */
    static final int HILL_CELL = 56;
    static final double HILL_CHANCE = 0.75;
    static final double HILL_MIN_RADIUS = 12.0;
    static final double HILL_MAX_RADIUS = 26.0;
    static final double HILL_MIN_DEPTH = 10.0;
    static final double HILL_MAX_DEPTH = 40.0;

    /** Floating chunks: one cell each, from this outer progress on; Y 40–160, Ø 6–40, tilt up to 20°. */
    static final int FRAGMENT_CELL = 48;
    static final double FRAGMENT_FROM = 0.15;
    static final double FRAGMENT_MIN_CHANCE = 0.2;
    static final double FRAGMENT_MAX_CHANCE = 0.65;
    static final double FRAGMENT_MIN_RADIUS = 3.0;
    static final double FRAGMENT_MAX_RADIUS = 20.0;
    static final int FRAGMENT_MIN_Y = 40;
    static final int FRAGMENT_MAX_Y = 160;
    static final double FRAGMENT_MAX_TILT = 20.0;
    static final double MIN_THICK = 3.0;

    /** A narrow ridge (V6-A) every ~{@link #RIDGE_CELL} blocks along the ring (± {@link #RIDGE_JITTER}), 3–5 wide. */
    static final int RIDGE_CELL = 500;
    static final double RIDGE_JITTER = 100.0;
    static final double RIDGE_HALF_MIN = 1.5;
    static final double RIDGE_HALF_MAX = 2.5;
    static final double RIDGE_MIN_THICK = 4.0;
    static final double RIDGE_MAX_THICK = 6.0;
    /** Ridges and stepping stones run out to this outer progress. */
    static final double STRIP_END = 0.92;
    /** Stepping stones half-way between the ridges: Ø 7, one every 8 blocks (gaps of 1), 3–4 thick. */
    static final double STONE_STEP = 8.0;
    static final double STONE_RADIUS = 3.5;

    private Ring5Outer() {
    }

    /** The outer progress: 0 on the band line, 1 on ring 5's outer boundary; negative in the inner band. */
    static double progress(double effective, double signedDistance, double ring5Radius) {
        double line = effective - signedDistance;
        return signedDistance / Math.max(1.0, ring5Radius - line);
    }

    /** The void's share of the ground at an outer progress and effective distance. */
    static double share(double progress, double effective, double ring5Radius) {
        if (progress < VOID_FROM) {
            return 0.0;
        }
        double t = Math.min(1.0, (progress - VOID_FROM) / (1.0 - VOID_FROM));
        double share = EDGE_SHARE * Math.pow(t, SHARE_POWER);
        if (effective > ring5Radius) {
            share += (1.0 - EDGE_SHARE) * Ring4Relief.smooth(effective - ring5Radius, 0.0, FLOES_GONE);
        }
        return Math.min(1.0, share);
    }

    /** Where the outer edge is empty (effective distance): {@link #EMPTY_SHARE} of the way to the border. */
    static double emptyFrom(double ring5Radius, double border) {
        return ring5Radius + EMPTY_SHARE * (border - ring5Radius);
    }

    /** The floes' top before the line blend: {@link #TOP_LINE} rising outwards, plus relief (noise ≈ ±0.5). */
    static double floeTop(double progress, double relief, double detail) {
        double s = Mth.clamp(progress, 0.0, 1.0);
        double top = TOP_LINE + TOP_RISE * s + TOP_RELIEF * relief * (0.45 + 0.55 * s) + 1.5 * detail;
        return Mth.clamp(top, TOP_MIN, TOP_MAX);
    }

    /** The floes' underside: −40 at the band line rising to ~16 at the edge (in the middle of a floe), ragged. */
    static double floeBottom(double progress, double rag) {
        double s = Mth.clamp(progress, 0.0, 1.0);
        return BOTTOM_LINE + (BOTTOM_EDGE - BOTTOM_LINE) * s + BOTTOM_RAG * rag;
    }

    /** The weight (0..1) of a region's shapes: 0 at the threshold, 1 at {@link #REGION_BLEND} past it. */
    static double regionWeight(double past) {
        return Ring4Relief.smooth(past, 0.0, REGION_BLEND);
    }

    /** The chance of a floating chunk in a cell at an outer progress; beyond the boundary falling to 0 at the empty edge. */
    static double fragmentChance(double progress, double edgeProgress) {
        if (progress < FRAGMENT_FROM || edgeProgress >= 1.0) {
            return 0.0;
        }
        double t = Math.min(1.0, (progress - FRAGMENT_FROM) / (1.0 - FRAGMENT_FROM));
        double chance = FRAGMENT_MIN_CHANCE + (FRAGMENT_MAX_CHANCE - FRAGMENT_MIN_CHANCE) * t;
        return edgeProgress > 0.0 ? chance * (1.0 - edgeProgress) : chance;
    }

    /** The largest radius of a floating chunk: big near the band line, small at the empty edge. */
    static double fragmentMaxRadius(double progress, double edgeProgress) {
        double t = Mth.clamp(0.75 * (progress - FRAGMENT_FROM) / (1.0 - FRAGMENT_FROM) + 0.25 * Math.max(0.0, edgeProgress), 0.0, 1.0);
        return FRAGMENT_MAX_RADIUS - (FRAGMENT_MAX_RADIUS - FRAGMENT_MIN_RADIUS - 2.0) * t;
    }
}
