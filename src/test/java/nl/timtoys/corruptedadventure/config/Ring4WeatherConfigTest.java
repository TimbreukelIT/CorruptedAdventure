package nl.timtoys.corruptedadventure.config;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** {@code [weather.ring4]} (package 70): exactly the contract keys (contract §6) with the decided defaults. */
class Ring4WeatherConfigTest {
    @Test
    void ring4SectionHasTheContractKeys() {
        ServerConfig.RingWeather section = ServerConfig.RING_WEATHER.get(Ring.RING_4);
        assertEquals(List.of("weather", "ring4", "clearMinSeconds"), section.clearMinSeconds.getPath());
        assertEquals(List.of("weather", "ring4", "clearMaxSeconds"), section.clearMaxSeconds.getPath());
        assertEquals(List.of("weather", "ring4", "weatherMinSeconds"), section.weatherMinSeconds.getPath());
        assertEquals(List.of("weather", "ring4", "weatherMaxSeconds"), section.weatherMaxSeconds.getPath());
        assertEquals(List.of("weather", "ring4", "denseFogWeight"), section.weights.get(RingWeatherType.DENSE_FOG).getPath());
        assertEquals(List.of("weather", "ring4", "stormWindWeight"), section.weights.get(RingWeatherType.STORM_WIND).getPath());
        assertEquals(List.of("weather", "ring4", "toxicRainWeight"), section.weights.get(RingWeatherType.TOXIC_RAIN).getPath());
        assertEquals(3, section.weights.size());

        ServerConfig.Ring4WeatherEffects effects = ServerConfig.RING_4_WEATHER;
        assertSame(section.ring4Effects, effects);
        assertEquals(List.of("weather", "ring4", "toxicRainPoisonMultiplier"), effects.toxicRainPoisonMultiplier.getPath());
        assertEquals(List.of("weather", "ring4", "stormPushStrength"), effects.stormPushStrength.getPath());
        assertEquals(List.of("weather", "ring4", "stormPushMinY"), effects.stormPushMinY.getPath());
        assertNull(ServerConfig.RING_WEATHER.get(Ring.RING_3).ring4Effects);
        assertNull(ServerConfig.RING_WEATHER.get(Ring.RING_5).ring4Effects);
    }

    /** Decisions 2026-10-13 (Q1–Q2 A): the vanilla rhythm, equal weights, a light push from Y 120. */
    @Test
    void ring4Defaults() {
        ServerConfig.RingWeather section = ServerConfig.RING_WEATHER.get(Ring.RING_4);
        assertEquals(600, section.clearMinSeconds.getDefault());
        assertEquals(9000, section.clearMaxSeconds.getDefault());
        assertEquals(600, section.weatherMinSeconds.getDefault());
        assertEquals(1200, section.weatherMaxSeconds.getDefault());
        section.weights.values().forEach(weight -> assertEquals(1, weight.getDefault()));
        assertEquals(1.0, ServerConfig.RING_4_WEATHER.toxicRainPoisonMultiplier.getDefault());
        assertEquals(0.05, ServerConfig.RING_4_WEATHER.stormPushStrength.getDefault());
        assertEquals(120, ServerConfig.RING_4_WEATHER.stormPushMinY.getDefault());
    }
}
