package nl.timtoys.corruptedadventure.ring;

/**
 * The concentric rings, from the centre outwards. {@link #VOID} is everything beyond ring 5.
 */
public enum Ring {
    RING_1,
    RING_2,
    RING_3,
    RING_4,
    RING_5,
    VOID;

    private static final Ring[] VALUES = values();

    /** @param index 0 for ring 1 up to 5 for the void */
    public static Ring byIndex(int index) {
        return VALUES[index];
    }

    /** 1 to 5, or 6 for the void. */
    public int number() {
        return ordinal() + 1;
    }
}
