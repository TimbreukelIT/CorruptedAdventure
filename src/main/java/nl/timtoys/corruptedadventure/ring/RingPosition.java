package nl.timtoys.corruptedadventure.ring;

/**
 * Where a position lies in the ring layout.
 * Outside a transition zone {@code inner == outer} and {@code blend == 0}.
 *
 * @param inner ring on the inside of the nearest boundary
 * @param outer ring on the outside of the nearest boundary
 * @param blend 0 = fully {@code inner}, 1 = fully {@code outer}; linear across the transition zone
 */
public record RingPosition(Ring inner, Ring outer, double blend) {
    public static RingPosition within(Ring ring) {
        return new RingPosition(ring, ring, 0.0);
    }

    /** The ring this position belongs to: whichever side of the boundary it is on. */
    public Ring ring() {
        return blend < 0.5 ? inner : outer;
    }

    public boolean inTransition() {
        return inner != outer;
    }
}
