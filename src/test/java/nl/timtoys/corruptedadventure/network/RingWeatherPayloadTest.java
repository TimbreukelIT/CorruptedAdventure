package nl.timtoys.corruptedadventure.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingWeatherPayloadTest {
    @Test
    void roundTripFullState() {
        Map<Ring, RingWeatherState> states = new EnumMap<>(Ring.class);
        states.put(Ring.RING_2, new RingWeatherState(RingWeatherType.SANDSTORM, 600, 12_000, 12345));
        states.put(Ring.RING_3, RingWeatherState.start(RingWeatherType.CLEAR, 180_000, 0));
        states.put(Ring.RING_4, new RingWeatherState(RingWeatherType.TOXIC_RAIN, 1, 24_000, -1));
        states.put(Ring.RING_5, new RingWeatherState(RingWeatherType.REVERSE_RAIN, 24_000, 24_000, Integer.MAX_VALUE));
        RingWeatherPayload payload = new RingWeatherPayload(Level.OVERWORLD, 123_456_789_012L, true, states);
        RingWeatherPayload received = roundTrip(payload);
        assertEquals(payload, received);
        assertEquals(List.of(Ring.RING_2, Ring.RING_3, Ring.RING_4, Ring.RING_5), List.copyOf(received.states().keySet()));
    }

    @Test
    void roundTripEmptyNether() {
        RingWeatherPayload payload = new RingWeatherPayload(Level.NETHER, 0L, false, Map.of());
        RingWeatherPayload received = roundTrip(payload);
        assertEquals(payload, received);
        assertTrue(received.states().isEmpty());
    }

    @Test
    void statesAreCopiedAndReadOnly() {
        Map<Ring, RingWeatherState> states = new EnumMap<>(Ring.class);
        states.put(Ring.RING_2, RingWeatherState.STILL);
        RingWeatherPayload payload = new RingWeatherPayload(Level.OVERWORLD, 20L, true, states);
        states.clear();
        assertEquals(1, payload.states().size());
        assertThrows(UnsupportedOperationException.class, () -> payload.states().clear());
    }

    private static RingWeatherPayload roundTrip(RingWeatherPayload payload) {
        ByteBuf buffer = Unpooled.buffer();
        RingWeatherPayload.STREAM_CODEC.encode(buffer, payload);
        RingWeatherPayload received = RingWeatherPayload.STREAM_CODEC.decode(buffer);
        assertEquals(0, buffer.readableBytes());
        return received;
    }
}
