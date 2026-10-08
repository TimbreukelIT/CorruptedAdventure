package nl.timtoys.corruptedadventure.condition;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The environmental conditions, each with its own meter 0–100 per player. Stored by name (so the order may change);
 * synced by position in {@link #values()} (client and server always run the same mod version).
 * Rules and thresholds live in {@link ConditionDefinitions}.
 */
public enum Condition implements StringRepresentable {
    /** Test condition: only rises while {@code /ca condition debug <player> rise} is set. */
    DEBUG("debug", 0xFFB040FF),
    /** Ring 2: rises in direct sunlight by day, falls in shade, water, the oasis, at night and outside ring 2. */
    DEHYDRATION("dehydration", 0xFFD9A441),
    /**
     * Ring 3: rises in direct sun by day and near heat sources (also at night and underground), faster on the peaks;
     * falls at night, under a roof, a little in shade, fast in water and in the dormant caldera, and outside ring 3.
     */
    HEAT("heat", 0xFFE8501E),
    /**
     * Nether ring 3: rises everywhere in the zone, faster on ice and in the exposed biomes; falls near warmth and
     * outside the zone. Leather slows it; at the top the player freezes (package 55).
     */
    COLD("cold", 0xFF7FC8F0);

    public static final Codec<Condition> CODEC = StringRepresentable.fromEnum(Condition::values);

    private final String name;
    private final int hudColor;

    Condition(String name, int hudColor) {
        this.name = name;
        this.hudColor = hudColor;
    }

    /** ARGB colour of the HUD bar (placeholder until the textures change). */
    public int hudColor() {
        return hudColor;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
