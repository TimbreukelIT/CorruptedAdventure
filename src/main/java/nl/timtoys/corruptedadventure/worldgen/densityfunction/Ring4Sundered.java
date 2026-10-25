package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * What ring 4 keeps of {@code sundered_reach} (package 63) since the fracture zone (85, V8-A): the zone 4→5's edges and
 * the rim of ground that ring 4's terrain holds at Y {@link #RIM_TOP} in the zone and a band before it (5a), so ring 4's
 * sea (Y 62) stops against rock. The cracks, loose chunks, land bridges and the abyss's sea are gone: the zone's shape is
 * {@link ZoneFractureShape}, cut after the ring blend by {@link ZoneFracture}. Pure.
 */
final class Ring4Sundered {
    /**
     * The ground of the zone (and of a band {@link #RIM_BEFORE} blocks before it in ring 4) never lies below this (5a):
     * ring 4's sea (Y 62) stops against this rim instead of standing beside the zone's dry air.
     */
    static final double RIM_TOP = 64.0;
    static final double RIM_BEFORE = 12.0;

    private Ring4Sundered() {
    }

    /** The inner edge of the zone 4→5 (effective distance). */
    static double zoneStart(RingLayout layout) {
        double r4 = layout.settings().ringOuterRadii().get(Ring.RING_4.ordinal());
        return r4 - layout.settings().transitionWidth(Ring.RING_4.ordinal()) / 2.0;
    }

    /** The outer edge of the zone 4→5 (effective distance). */
    static double zoneEnd(RingLayout layout) {
        double r4 = layout.settings().ringOuterRadii().get(Ring.RING_4.ordinal());
        return r4 + layout.settings().transitionWidth(Ring.RING_4.ordinal()) / 2.0;
    }

    /** Whether an effective distance lies in the rim and the zone, where the ground is at least {@link #RIM_TOP}. */
    static boolean inRim(RingLayout layout, double effective) {
        return effective >= zoneStart(layout) - RIM_BEFORE && effective <= zoneEnd(layout);
    }
}
