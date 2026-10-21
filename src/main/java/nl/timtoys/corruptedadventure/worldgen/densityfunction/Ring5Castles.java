package nl.timtoys.corruptedadventure.worldgen.densityfunction;

/**
 * The shape numbers of ring 5's castle pinnacles and their bridges (package 84, V5-A, V6-A): one place for them, like
 * {@code Ring4Band}. The sites themselves (where, how big) are {@link Ring5Field#placeCastles}; this is the pinnacle's
 * ruined rim and walls and the two natural bridges per castle.
 */
final class Ring5Castles {
    /** Where a pinnacle's walls end (V3-A: void below), ragged upwards by up to {@link #WALL_RAG}. */
    static final int PINNACLE_BOTTOM = -40;
    static final double WALL_RAG = 8.0;
    /** The ruined rim round the plateau: 8–16 blocks wide, steps 3 wide and 2 lower each, more broken outwards. */
    static final int RIM_MIN = 8;
    static final int RIM_MAX = 16;
    static final double RIM_STEP_WIDTH = 3.0;
    static final double RIM_STEP_DROP = 2.0;
    static final double RIM_MAX_BROKEN = 0.45;
    /** The rim's underside rises this much per block outwards (a shelf on the pinnacle's wall). */
    static final double RIM_UNDERSIDE_RISE = 3.0;
    /** The {@code castles} value: 1 on the plateau, rim from {@link #RIM_VALUE} down to {@link #BELT_VALUE}, bridges. */
    static final double RIM_VALUE = 0.9;
    static final double BELT_VALUE = 0.6;
    static final double BRIDGE_VALUE = 0.5;

    /** Two bridges per castle (V6-A), 5–9 wide, sagging 2–6 in the middle, 4–8 thick underneath. */
    static final int BRIDGES = 2;
    static final double HALF_MIN = 2.5;
    static final double HALF_MAX = 4.5;
    static final double SAG_MIN = 2.0;
    static final double SAG_MAX = 6.0;
    static final double THICK_MIN = 4.0;
    static final double THICK_MAX = 8.0;
    /** Drips under the deck: this share of its columns hang 2–6 lower. */
    static final double DRIP_CHANCE = 0.08;
    /** The deck falls at most this much per block towards its far end; a steeper drop raises a mound where it lands. */
    static final double MAX_SLOPE = 0.5;
    /** The landing mound's flanks: this much lower per block from the bridge's end. */
    static final double MOUND_SLOPE = 0.8;
    /** The second bridge leaves at one of these angles (degrees) from the inward ray, either side. */
    static final double[] SIDE_ANGLES = {30.0, 40.0, 50.0, 60.0};
    /** How far (blocks past the rim) a bridge looks for ground: first {@link #REACH}, then up to {@link #FAR_REACH}. */
    static final double REACH = 260.0;
    static final double FAR_REACH = 900.0;
    /** A landing needs ground for this many blocks onwards; the bridge ends this far onto it. */
    static final double LANDING_DEPTH = 6.0;
    static final double LANDING_OVERLAP = 3.0;
    /** The size of a floe is counted in floe cells, at most this many. */
    static final int FLOE_COUNT_CAP = 120;

    private Ring5Castles() {
    }
}
