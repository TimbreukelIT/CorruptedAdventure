package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Server → every player of a dimension: the full ring weather of that dimension (at most 5 rings). Sent on login,
 * dimension change and respawn, on every weather change and when {@code doWeatherCycle} flips. Empty {@code states} =
 * no ring weather here (the client forgets what it had). {@code gameTime} is the level's game time when the states were
 * taken: while {@code cycleRunning} the client subtracts the game time passed since then (not its own tick count, which
 * runs ahead of a lagging server).
 */
public record RingWeatherPayload(ResourceKey<Level> dimension, long gameTime, boolean cycleRunning,
                                 Map<Ring, RingWeatherState> states)
        implements CustomPacketPayload {
    public static final Type<RingWeatherPayload> TYPE = new Type<>(CorruptedAdventure.id("ring_weather"));

    private static final StreamCodec<ByteBuf, Ring> RING_CODEC = ByteBufCodecs.idMapper(Ring::byIndex, Ring::ordinal);

    public static final StreamCodec<ByteBuf, RingWeatherPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), RingWeatherPayload::dimension,
            ByteBufCodecs.VAR_LONG, RingWeatherPayload::gameTime,
            ByteBufCodecs.BOOL, RingWeatherPayload::cycleRunning,
            ByteBufCodecs.<ByteBuf, Ring, RingWeatherState, Map<Ring, RingWeatherState>>map(
                    size -> new EnumMap<>(Ring.class), RING_CODEC, RingWeatherState.STREAM_CODEC, Ring.values().length),
            RingWeatherPayload::states,
            RingWeatherPayload::new);

    public RingWeatherPayload {
        states = states.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(states));
    }

    @Override
    public Type<RingWeatherPayload> type() {
        return TYPE;
    }
}
