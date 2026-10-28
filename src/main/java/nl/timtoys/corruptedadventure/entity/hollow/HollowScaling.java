package nl.timtoys.corruptedadventure.entity.hollow;

/** The Hollow's max health per number of players in the fight: one player the base, every further one a part more. */
public final class HollowScaling {
    private HollowScaling() {
    }

    /** The factor on top of the base health, for an {@code ADD_MULTIPLIED_BASE} modifier; 0 for zero or one fighter. */
    public static double extraHealthFactor(double perExtraPlayer, int fighters) {
        return perExtraPlayer * Math.max(0, fighters - 1);
    }

    public static double maxHealth(double baseHealth, double perExtraPlayer, int fighters) {
        return baseHealth * (1.0 + extraHealthFactor(perExtraPlayer, fighters));
    }
}
