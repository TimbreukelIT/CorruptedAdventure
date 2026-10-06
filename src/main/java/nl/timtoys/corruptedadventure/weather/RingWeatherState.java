package nl.timtoys.corruptedadventure.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The weather of one ring: its type and how long it still lasts. {@link #intensity()} is the same formula on the server
 * (gameplay) and the client (rendering), so only changes are synced, never every tick.
 *
 * @param type           the weather type ({@code clear} between two weathers)
 * @param remainingTicks ticks left, 0 to {@code durationTicks}
 * @param durationTicks  total length of this weather; 0 = {@link #STILL} (a ring without ring weather)
 * @param seed           drawn for every new weather (e.g. the wind direction of change 28); 0 for {@code clear}
 */
public record RingWeatherState(RingWeatherType type, int remainingTicks, int durationTicks, int seed) {
    /** Fade-in and fade-out of a weather: 30 seconds, unless its type has its own ({@link RingWeatherType#fadeTicks()}). */
    public static final int FADE_TICKS = 600;

    /** A ring without ring weather: clear, without end. */
    public static final RingWeatherState STILL = new RingWeatherState(RingWeatherType.CLEAR, 0, 0, 0);

    public static final Codec<RingWeatherState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RingWeatherType.CODEC.fieldOf("type").forGetter(RingWeatherState::type),
            Codec.INT.fieldOf("remaining_ticks").forGetter(RingWeatherState::remainingTicks),
            Codec.INT.fieldOf("duration_ticks").forGetter(RingWeatherState::durationTicks),
            Codec.INT.optionalFieldOf("seed", 0).forGetter(RingWeatherState::seed)
    ).apply(instance, RingWeatherState::new));

    public static final StreamCodec<ByteBuf, RingWeatherState> STREAM_CODEC = StreamCodec.composite(
            RingWeatherType.STREAM_CODEC, RingWeatherState::type,
            ByteBufCodecs.VAR_INT, RingWeatherState::remainingTicks,
            ByteBufCodecs.VAR_INT, RingWeatherState::durationTicks,
            ByteBufCodecs.VAR_INT, RingWeatherState::seed,
            RingWeatherState::new);

    /** Clamps instead of throwing: saved or received values must never crash the game. */
    public RingWeatherState {
        durationTicks = Math.max(0, durationTicks);
        remainingTicks = Math.clamp(remainingTicks, 0, durationTicks);
    }

    /** A fresh weather of the full duration. */
    public static RingWeatherState start(RingWeatherType type, int durationTicks, int seed) {
        return new RingWeatherState(type, durationTicks, durationTicks, type == RingWeatherType.CLEAR ? 0 : seed);
    }

    /** 0 for {@code clear}; otherwise rises from 0 to 1 over the type's first fade ticks and falls back to 0 over the last. */
    public float intensity() {
        if (type == RingWeatherType.CLEAR || durationTicks == 0) {
            return 0.0F;
        }
        int elapsed = durationTicks - remainingTicks;
        return Math.min(1.0F, Math.min(elapsed, remainingTicks) / (float) type.fadeTicks());
    }

    /** This weather {@code ticks} later, at least 0 left (choosing the next weather is {@link RingWeatherCycle}'s job). */
    public RingWeatherState countDown(int ticks) {
        return new RingWeatherState(type, remainingTicks - ticks, durationTicks, seed);
    }

    public boolean isStill() {
        return durationTicks == 0;
    }
}
