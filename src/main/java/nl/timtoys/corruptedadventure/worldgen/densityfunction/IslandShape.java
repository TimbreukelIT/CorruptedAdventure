package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

/**
 * The full shape of one floating island of ring 4 (package 61) and of its mirror hole: a ragged ellipse (half axes
 * {@code a · stretch} along {@code angle} and {@code a} across) with a slightly domed top, and an underside like a torn
 * clod: a bowl with {@code lobes} hanging lobes ({@link Volcano#underside}). The hole is the same shape mirrored into
 * the ground under it: the same outline a little wider ({@link #HOLE_SCALE}, with its own raggedness), as deep at each
 * point as the clod is thick there, measured down from {@code groundY} (the ground at the centre before it was torn
 * out). An outlet (optional) is a channel from the hole's edge to open water.
 * <p>
 * Pure value; {@link IslandField} reads it per column, {@link IslandPlacement} makes it.
 *
 * @param kind     {@link #CLOD}, {@link #MIDDLE}, {@link #GIANT} or {@link #TREE}
 * @param outletX  the outlet's far end (open water), NaN without one
 */
record IslandShape(double x, double z, double a, double stretch, double angle, int lobes, double phase, double top,
                   double thickness, double groundY, int kind, double outletX, double outletZ) {
    static final int CLOD = 0;
    static final int MIDDLE = 1;
    static final int GIANT = 2;
    /** 74: the half torn-out island a world tree stands on ({@link WorldTrees}); outranks every other kind. */
    static final int TREE = 3;
    /** The overworld's sea level. */
    static final double SEA_LEVEL = 63.0;
    /** The deepest torn edge (blocks). */
    static final double EDGE_DEPTH = 5.0;
    /** A hole's floor never lies below this (the giants' holes would reach far towards the dry bottom at Y −54). */
    static final double HOLE_FLOOR_MIN = 0.0;
    /** The hole's outline is this much wider than the island's. */
    static final double HOLE_SCALE = 1.12;
    /** The deepest the underside hangs, as a share of {@code thickness} ({@link Volcano#underside} stays below it). */
    static final double MAX_UNDERSIDE = 0.9;
    /** How far (blocks) past the hole's edge its lake and the band kept free of caves reach. */
    static final double LAKE_BAND = 8.0;
    static final double CAVE_BAND = 16.0;
    /** An outlet: half width of its bed and the width of its banks (blocks), its bed's Y. */
    static final double OUTLET_HALF_WIDTH = 2.5;
    static final double OUTLET_BANK = 4.0;
    static final double OUTLET_BED = 59.0;
    /** The outline reaches at most this far past {@code a · stretch} ({@link Volcano#ragged}: |h| ≤ 0.16). */
    static final double RAGGED_REACH = 1.0 / (1.0 - 0.16);

    /** Normalised distance from the centre (ragged outline): below 1 inside the island. */
    double distance(double px, double pz) {
        return outline(px, pz, 1.0, phase);
    }

    /** Normalised distance in the hole's outline: below 1 inside the hole. */
    double holeDistance(double px, double pz) {
        return outline(px, pz, HOLE_SCALE, phase + 1.3);
    }

    private double outline(double px, double pz, double scale, double raggedPhase) {
        double dx = px - x;
        double dz = pz - z;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double along = (dx * cos + dz * sin) / (a * stretch * scale);
        double across = (-dx * sin + dz * cos) / (a * scale);
        return Volcano.ragged(Math.sqrt(along * along + across * across), Math.atan2(across, along), raggedPhase);
    }

    /** The angle around the centre in the island's own frame (for the lobes). */
    double theta(double px, double pz) {
        double dx = px - x;
        double dz = pz - z;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return Math.atan2((-dx * sin + dz * cos) / a, (dx * cos + dz * sin) / (a * stretch));
    }

    /** The top (first air above the island) at normalised distance {@code d}: nearly flat, like the ground it was. */
    double topAt(double d) {
        return top + 1.0 * (1.0 - d * d) - Math.pow(d, 8.0);
    }

    /** The underside at normalised distance {@code d} and angle {@code theta}. */
    double bottomAt(double d, double theta) {
        return top - 1.0 - depth(d, theta);
    }

    /**
     * How far the clod hangs below its top at {@code d}: the bowl with lobes, but never less than the torn edge
     * ({@link #edgeDepth}), so the island ends in a steep side of soil and rock, not a sharp wedge.
     */
    double depth(double d, double theta) {
        return Math.max(edgeDepth(), thickness * Volcano.underside(d, theta, lobes, phase));
    }

    /** The torn edge: a quarter of the thickness, at most {@link #EDGE_DEPTH} blocks. */
    double edgeDepth() {
        return Math.min(EDGE_DEPTH, 0.25 * thickness);
    }

    /** The solid blocks of the island in a column: {lowest, highest} (inclusive), or null if it holds none. */
    int[] interval(double px, double pz) {
        double d = distance(px, pz);
        if (d >= 1.0) {
            return null;
        }
        int lo = Mth.ceil(bottomAt(d, theta(px, pz)));
        int hi = Mth.floor(topAt(d) - 1.0);
        return hi < lo ? null : new int[]{lo, hi};
    }

    /** The floor of the hole in a column, or NaN outside it. */
    double holeFloor(double px, double pz) {
        double dh = holeDistance(px, pz);
        if (dh >= 1.0) {
            return Double.NaN;
        }
        return Math.max(HOLE_FLOOR_MIN, groundY - depth(dh, theta(px, pz)));
    }

    /** The deepest the hole's floor can lie. */
    double deepestFloor() {
        return Math.max(HOLE_FLOOR_MIN, groundY - thickness * MAX_UNDERSIDE);
    }

    /** The floor of the outlet's channel in a column (its banks rising to the ground), or +infinity off it. */
    double outletFloor(double px, double pz) {
        if (Double.isNaN(outletX)) {
            return Double.POSITIVE_INFINITY;
        }
        double vx = outletX - x;
        double vz = outletZ - z;
        double t = Mth.clamp(((px - x) * vx + (pz - z) * vz) / (vx * vx + vz * vz), 0.0, 1.0);
        double dx = px - (x + t * vx);
        double dz = pz - (z + t * vz);
        double off = Math.sqrt(dx * dx + dz * dz) - OUTLET_HALF_WIDTH;
        if (off >= OUTLET_BANK) {
            return Double.POSITIVE_INFINITY;
        }
        // A bed, then banks rising 3 blocks per block (the ground caps them).
        return OUTLET_BED + 3.0 * Math.max(0.0, off);
    }

    /** How far (blocks) the island reaches from its centre. */
    double reach() {
        return a * stretch * RAGGED_REACH + 1.0;
    }

    /** How far (blocks) the hole, its lake band, the caves' band and the outlet reach from the centre. */
    double groundReach() {
        double reach = reach() * HOLE_SCALE + CAVE_BAND + 2.0;
        if (!Double.isNaN(outletX)) {
            reach = Math.max(reach, Math.hypot(outletX - x, outletZ - z) + OUTLET_HALF_WIDTH + OUTLET_BANK + 1.0);
        }
        return reach;
    }

    /** The hole's mean radius (blocks): to turn the normalised hole distance into blocks. */
    double holeRadius() {
        return a * Math.sqrt(stretch) * HOLE_SCALE;
    }

    /** The lowest block the underside can reach. */
    int lowest() {
        return Mth.floor(top - 1.0 - thickness * MAX_UNDERSIDE);
    }

    /** The highest block of the island. */
    int highest() {
        return Mth.floor(topAt(0.0) - 1.0);
    }

    /** The public view of the island ({@link IslandField.Island}). */
    IslandField.Island view() {
        return new IslandField.Island(x, z, reach(), lowest(), highest());
    }
}
