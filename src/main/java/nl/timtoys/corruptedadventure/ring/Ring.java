package nl.timtoys.corruptedadventure.ring;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * The concentric rings, from the centre outwards. {@link #VOID} is everything beyond ring 5.
 */
public enum Ring implements StringRepresentable {
    RING_1,
    RING_2,
    RING_3,
    RING_4,
    RING_5,
    VOID;

    /** {@code ring_1} … {@code ring_5}, {@code void}. */
    public static final Codec<Ring> CODEC = StringRepresentable.fromEnum(Ring::values);

    private static final Ring[] VALUES = values();

    private final String serializedName = name().toLowerCase(Locale.ROOT);

    /** @param index 0 for ring 1 up to 5 for the void */
    public static Ring byIndex(int index) {
        return VALUES[index];
    }

    /** 1 to 5, or 6 for the void. */
    public int number() {
        return ordinal() + 1;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
