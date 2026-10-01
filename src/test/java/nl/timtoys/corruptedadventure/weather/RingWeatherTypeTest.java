package nl.timtoys.corruptedadventure.weather;

import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.ring.Ring;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingWeatherTypeTest {
    @Test
    void serializedNamesAreUniqueAndRoundTrip() {
        assertEquals(RingWeatherType.values().length,
                Arrays.stream(RingWeatherType.values()).map(RingWeatherType::getSerializedName).distinct().count());
        for (RingWeatherType type : RingWeatherType.values()) {
            assertEquals(type, RingWeatherType.byName(type.getSerializedName()));
        }
        assertEquals("heat_wave", RingWeatherType.HEAT_WAVE.getSerializedName());
        assertNull(RingWeatherType.byName("rain"));
    }

    @Test
    void everyTypeButClearBelongsToExactlyOneOverworldRing() {
        for (RingWeatherType type : RingWeatherType.values()) {
            if (type == RingWeatherType.CLEAR) {
                assertNull(type.ring());
                continue;
            }
            assertEquals(Level.OVERWORLD, type.dimension());
            long rings = Arrays.stream(Ring.values())
                    .filter(ring -> RingWeatherType.typesFor(Level.OVERWORLD, ring).contains(type)).count();
            assertEquals(1, rings, type.getSerializedName());
            assertTrue(type.ring().number() >= 2 && type.ring().number() <= 5, type.getSerializedName());
        }
    }

    @Test
    void ringsWithWeather() {
        assertEquals(List.of(Ring.RING_2, Ring.RING_3, Ring.RING_4, Ring.RING_5), RingWeatherType.ringsWithWeather(Level.OVERWORLD));
        assertEquals(List.of(RingWeatherType.SANDSTORM, RingWeatherType.HEAT_WAVE), RingWeatherType.typesFor(Level.OVERWORLD, Ring.RING_2));
        assertTrue(RingWeatherType.typesFor(Level.OVERWORLD, Ring.RING_1).isEmpty());
        assertTrue(RingWeatherType.typesFor(Level.OVERWORLD, Ring.VOID).isEmpty());
        assertTrue(RingWeatherType.hasWeather(Level.OVERWORLD));
    }

    @Test
    void noNetherRingWeather() {
        for (Ring ring : Ring.values()) {
            assertTrue(RingWeatherType.typesFor(Level.NETHER, ring).isEmpty());
        }
        assertFalse(RingWeatherType.hasWeather(Level.NETHER));
        assertFalse(RingWeatherType.hasWeather(Level.END));
    }

    @Test
    void allowedIn() {
        assertTrue(RingWeatherType.SANDSTORM.allowedIn(Level.OVERWORLD, Ring.RING_2));
        assertFalse(RingWeatherType.ERUPTION.allowedIn(Level.OVERWORLD, Ring.RING_2));
        assertTrue(RingWeatherType.CLEAR.allowedIn(Level.OVERWORLD, Ring.RING_5));
        assertFalse(RingWeatherType.CLEAR.allowedIn(Level.OVERWORLD, Ring.RING_1));
        assertFalse(RingWeatherType.SANDSTORM.allowedIn(Level.OVERWORLD, Ring.RING_1));
        assertFalse(RingWeatherType.SANDSTORM.allowedIn(Level.NETHER, Ring.RING_2));
    }
}
