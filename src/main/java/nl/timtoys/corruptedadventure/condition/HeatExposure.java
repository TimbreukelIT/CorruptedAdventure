package nl.timtoys.corruptedadventure.condition;

import net.minecraft.util.StringRepresentable;

/**
 * Why the heat meter rises or falls. {@link #SUN}, {@link #SOURCE} and {@link #PEAK} rise (the label names the
 * strongest factor); every other value cools, and when several apply the strongest wins (they do not stack). The
 * serialized name is the label of {@code /ca condition get}.
 */
public enum HeatExposure implements StringRepresentable {
    /** In the zone, by day, in full sky light, without a stronger factor. */
    SUN("sun"),
    /** In the zone within {@code sourceRadius} of a heat source (also at night and underground). */
    SOURCE("source"),
    /** Exposed (sun or source) in a peak biome, where the peak factor is the strongest. */
    PEAK("peak"),
    /** Submerged above the waist, anywhere. */
    WATER("water"),
    /** In a refuge biome (the dormant caldera), also in full sun or next to a source. */
    REFUGE("refuge"),
    /** In the zone while the sun is down, away from sources. */
    NIGHT("night"),
    /** In the zone under a solid block (a roof, an overhang, underground), away from sources. */
    ROOF("roof"),
    /** In the zone by day without full sky light and without a solid block above (leaves), away from sources. */
    SHADE("shade"),
    /** Not in the zone (other rings, the void, the Nether). */
    OUTSIDE("outside");

    private final String name;

    HeatExposure(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
