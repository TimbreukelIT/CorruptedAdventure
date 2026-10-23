package nl.timtoys.corruptedadventure.condition;

import net.minecraft.util.StringRepresentable;

/**
 * Why the silence meter rises or falls (package 97). {@link #ZONE}, {@link #OPEN} and {@link #TOGETHER} rise;
 * {@link #SOUND} and {@link #OUTSIDE} fall. The serialized name is the label of {@code /ca condition get}.
 */
public enum SilenceExposure implements StringRepresentable {
    /** In the zone, in the halls, alone. */
    ZONE("zone"),
    /** In the zone, in open space (the hollow cathedral, on a white plateau), alone. */
    OPEN("open"),
    /** In the zone with another player close by (the rise is slowed; open space still counts in the rate). */
    TOGETHER("together"),
    /** A bell, goat horn, note block or jukebox was heard a moment ago: it falls, whatever else holds. */
    SOUND("sound"),
    /** Not in the zone (other Nether rings, the overworld). */
    OUTSIDE("outside");

    private final String name;

    SilenceExposure(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
