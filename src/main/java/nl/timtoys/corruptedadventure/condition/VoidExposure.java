package nl.timtoys.corruptedadventure.condition;

import net.minecraft.util.StringRepresentable;

/**
 * Why the void rule rises or falls. {@link #INNER}, {@link #OUTER} and {@link #SOURCE} rise (the label names the
 * strongest); {@link #LIGHT} and {@link #OUTSIDE} fall. Night and ring weather are not here: {@link VoidAmplifierModifier}
 * adds them after the rule. The serialized name is the label of {@code /ca condition get}.
 */
public enum VoidExposure implements StringRepresentable {
    /** In ring 5's inner band, no void source nearby. */
    INNER("inner"),
    /** In ring 5's outer band or on the void rim, no stronger void source nearby. */
    OUTER("outer"),
    /** In the zone, a block in {@code #void_sources} within {@code sourceRadius} of feet or head, stronger than the band. */
    SOURCE("source"),
    /** In the zone, block light at eye height of at least {@code lightLevel}: falls, also at a source or in the outer band. */
    LIGHT("light"),
    /** Not in the zone (rings 1–4, the Nether). */
    OUTSIDE("outside");

    private final String name;

    VoidExposure(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
