package nl.timtoys.corruptedadventure.condition;

import net.minecraft.util.StringRepresentable;

/**
 * Why the dehydration meter rises or falls. Only {@link #SUN} rises (change 28's heat wave doubles exactly that);
 * every other value cools, and when several apply the strongest wins (they do not stack). The serialized name is the
 * label of {@code /ca condition get}.
 */
public enum DehydrationExposure implements StringRepresentable {
    /** In the zone, by day, in full sky light, dry and not in the oasis. */
    SUN("sun"),
    /** Submerged above the waist, anywhere. */
    WATER("water"),
    /** In the oasis biome, also in full sun. */
    OASIS("oasis"),
    /** In the zone while the sun is down. */
    NIGHT("night"),
    /** In the zone by day without full sky light. */
    SHADE("shade"),
    /** Not in the zone (other rings, the void, the Nether). */
    OUTSIDE("outside");

    private final String name;

    DehydrationExposure(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
