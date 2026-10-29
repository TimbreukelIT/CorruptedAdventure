package nl.timtoys.corruptedadventure.entity.risen;

/**
 * What The Risen is doing; decided by the server ({@link TheRisen#customServerAiStep}) and synced as one byte, so the
 * client can pick the body animation.
 */
public enum RisenState {
    /** Out of the molten ground: first only the glow (it is still under it), then it rises. No collision. */
    RISING(false),
    /** Circling its target high up; ash rain and released victims happen here. */
    CIRCLING(false),
    /** Stooping on its target, talons first. */
    DIVING(false),
    /** Coming down beside its target. */
    LANDING(false),
    /** On the ground: beak and wing buffet. */
    LANDED(true),
    /** Going straight up again after landing or a stun. */
    TAKING_OFF(false),
    /** Hanging still (or standing) while the choir draws breath, then the scream. */
    SCREAMING(false),
    /** The horn drowned out the choir: rigid; in the air it falls. */
    STUNNED(true),
    /** Climbing high above its target for the swoop, then hanging there flaring: the warning. */
    SOARING(false),
    /** The swoop: down a straight line it no longer steers, then skimming the ground with a trail of fire behind it. */
    SWOOPING(false),
    /** Hanging over an area while burning embers fall from its wings. */
    EMBER_RAIN(false),
    /** Staggered in the air (the shared stagger): it sags a few blocks, tumbling, then takes off again. */
    STAGGERED(false);

    private final boolean grounded;

    RisenState(boolean grounded) {
        this.grounded = grounded;
    }

    /** Whether it has gravity and collides with blocks; in every other state it flies through them, like the dragon. */
    public boolean grounded() {
        return grounded;
    }

    /**
     * An attack that only lives in the tick (its aim is not saved): loaded in it, it goes back to circling and picks
     * anew.
     */
    boolean restartsWhenLoaded() {
        return this == DIVING || this == LANDING || this == SOARING || this == SWOOPING || this == EMBER_RAIN
                || this == STAGGERED;
    }

    static RisenState byId(int id) {
        RisenState[] values = values();
        return values[Math.floorMod(id, values.length)];
    }
}
