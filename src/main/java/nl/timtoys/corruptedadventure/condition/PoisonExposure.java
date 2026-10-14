package nl.timtoys.corruptedadventure.condition;

import net.minecraft.util.StringRepresentable;

/**
 * Why the poison rule rises or falls. {@link #POOL} and {@link #GAS} rise (the label names the stronger source);
 * {@link #CLEAN} and {@link #OUTSIDE} fall. Toxic rain is not here: it is the weather, added by
 * {@link ToxicRainPoisonModifier} after the rule (so {@code /ca condition get} shows {@code clean} with a positive rate
 * in the rain). The serialized name is the label of {@code /ca condition get}.
 */
public enum PoisonExposure implements StringRepresentable {
    /** In the zone, a block in {@code #poison_sources} within {@code poolRadius} of feet or head. */
    POOL("pool"),
    /** In the zone, the block at eye height in {@code #poison_gases}. */
    GAS("gas"),
    /** In the zone, away from every source (water and the blighted glowcaps are no source). */
    CLEAN("clean"),
    /** Not in the zone (other rings, the void, the Nether), also next to a source. */
    OUTSIDE("outside");

    private final String name;

    PoisonExposure(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
