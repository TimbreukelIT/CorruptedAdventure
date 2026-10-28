package nl.timtoys.corruptedadventure.entity.hollow;

/**
 * The life of a {@link HollowRift}. An arrival runs OMEN → OPENING → EYES → EMERGE → CLOSING; a rift step only
 * OPENING → OPEN → CLOSING. The client draws each phase from the synced phase and its length.
 */
public enum RiftPhase {
    /** A hairline crack in the air that slowly lengthens; omens around it. */
    OMEN,
    /** The crack tears open into the full wound. */
    OPENING,
    /** Open, and deep inside two white eyes look out. */
    EYES,
    /** The hand grips the edge and The Hollow pulls itself through. */
    EMERGE,
    /** Open without eyes: a rift step's way in or out. */
    OPEN,
    /** Closes up; an arrival leaves its scar when it is gone. */
    CLOSING;

    private static final RiftPhase[] VALUES = values();

    public static RiftPhase byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : CLOSING;
    }

    /** Fully open: it pulls. */
    public boolean isOpen() {
        return this == EYES || this == EMERGE || this == OPEN;
    }
}
