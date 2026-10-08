package nl.timtoys.corruptedadventure.condition;

import net.minecraft.util.StringRepresentable;

/**
 * Why the cold meter rises or falls. {@link #ZONE}, {@link #ICE} and {@link #EXPOSED} rise (the label names the
 * strongest factor); {@link #WARMTH} and {@link #OUTSIDE} fall. The serialized name is the label of
 * {@code /ca condition get}.
 */
public enum ColdExposure implements StringRepresentable {
    /** In the zone, away from warmth, without a stronger factor (the halls and rifts). */
    ZONE("zone"),
    /** In the zone, standing on ice ({@code #minecraft:ice}). */
    ICE("ice"),
    /** In the zone, in an exposed biome ({@code #corruptedadventure:cold_exposed}, the open plain). */
    EXPOSED("exposed"),
    /** In the zone within {@code warmthRadius} of a lit warmth source (also in lava or fire itself). */
    WARMTH("warmth"),
    /** Not in the zone (other Nether rings, the overworld). */
    OUTSIDE("outside");

    private final String name;

    ColdExposure(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
