package nl.timtoys.corruptedadventure.weather;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.ring.Ring;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The ring weather types. Each type belongs to exactly one ring of one dimension; {@link #CLEAR} belongs to every ring
 * with weather. Stored by name (renaming a value turns saved weather into {@code clear}), sent by index (both sides
 * run the same mod version, guarded by {@code ModNetwork.PROTOCOL_VERSION}).
 */
public enum RingWeatherType implements StringRepresentable {
    CLEAR(null, null),
    SANDSTORM(Ring.RING_2, Level.OVERWORLD),
    HEAT_WAVE(Ring.RING_2, Level.OVERWORLD),
    ASH_RAIN(Ring.RING_3, Level.OVERWORLD),
    ERUPTION(Ring.RING_3, Level.OVERWORLD),
    DENSE_FOG(Ring.RING_4, Level.OVERWORLD),
    TOXIC_RAIN(Ring.RING_4, Level.OVERWORLD),
    GLITCH_STORM(Ring.RING_5, Level.OVERWORLD),
    REVERSE_RAIN(Ring.RING_5, Level.OVERWORLD);

    /** {@code clear}, {@code sandstorm}, {@code heat_wave}, … */
    public static final Codec<RingWeatherType> CODEC = StringRepresentable.fromEnum(RingWeatherType::values);
    public static final StreamCodec<ByteBuf, RingWeatherType> STREAM_CODEC =
            ByteBufCodecs.idMapper(index -> values()[index], Enum::ordinal);

    private final @Nullable Ring ring;
    private final @Nullable ResourceKey<Level> dimension;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    RingWeatherType(@Nullable Ring ring, @Nullable ResourceKey<Level> dimension) {
        this.ring = ring;
        this.dimension = dimension;
    }

    /** The ring of this type; null for {@link #CLEAR}. */
    public @Nullable Ring ring() {
        return ring;
    }

    /** The dimension of this type; null for {@link #CLEAR}. */
    public @Nullable ResourceKey<Level> dimension() {
        return dimension;
    }

    /** The weather types of one ring (without {@code clear}); empty for a ring without ring weather. */
    public static List<RingWeatherType> typesFor(ResourceKey<Level> dimension, Ring ring) {
        return Arrays.stream(values()).filter(type -> type.ring == ring && dimension.equals(type.dimension)).toList();
    }

    /** The rings of a dimension that have weather types, in ring order. */
    public static List<Ring> ringsWithWeather(ResourceKey<Level> dimension) {
        return Arrays.stream(Ring.values()).filter(ring -> !typesFor(dimension, ring).isEmpty()).toList();
    }

    public static boolean hasWeather(ResourceKey<Level> dimension) {
        return !ringsWithWeather(dimension).isEmpty();
    }

    /** Whether this type may be the weather of the ring: {@code clear}, or one of the ring's own types. */
    public boolean allowedIn(ResourceKey<Level> dimension, Ring ring) {
        return this == CLEAR ? !typesFor(dimension, ring).isEmpty() : this.ring == ring && dimension.equals(this.dimension);
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static @Nullable RingWeatherType byName(String name) {
        for (RingWeatherType type : values()) {
            if (type.serializedName.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
