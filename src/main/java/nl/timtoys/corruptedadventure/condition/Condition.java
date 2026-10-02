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
    DEHYDRATION("dehydration", 0xFFD9A441);

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
