package nl.timtoys.corruptedadventure.config;

import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherSettings;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RingWeatherConfigTest {
    @Test
    void defaultsAreTheVanillaRhythmWithEqualWeights() {
        for (Ring ring : RingWeatherType.ringsWithWeather(Level.OVERWORLD)) {
            ServerConfig.RingWeather config = section(ring);
            assertEquals(600, config.clearMinSeconds.getDefault());
            assertEquals(9000, config.clearMaxSeconds.getDefault());
            assertEquals(600, config.weatherMinSeconds.getDefault());
            assertEquals(1200, config.weatherMaxSeconds.getDefault());
            assertEquals(RingWeatherType.typesFor(Level.OVERWORLD, ring), config.weights.keySet().stream().toList());
            config.weights.values().forEach(weight -> assertEquals(1, weight.getDefault()));
        }
    }

    @Test
    void sectionAndKeyNames() {
        assertEquals("ring3", section(Ring.RING_3).clearMinSeconds.getPath().get(1));
        assertEquals("weather", section(Ring.RING_3).clearMinSeconds.getPath().getFirst());
        assertEquals("heatWaveWeight", ServerConfig.RingWeather.weightKey(RingWeatherType.HEAT_WAVE));
        assertEquals("sandstormWeight", ServerConfig.RingWeather.weightKey(RingWeatherType.SANDSTORM));
        assertEquals("glitchStormWeight", ServerConfig.RingWeather.weightKey(RingWeatherType.GLITCH_STORM));
    }

    @Test
    void ring2EffectsLiveInTheRing2Section() {
        ServerConfig.Ring2WeatherEffects effects = ServerConfig.RING_2_WEATHER;
        assertSame(section(Ring.RING_2).ring2Effects, effects);
        assertEquals(java.util.List.of("weather", "ring2", "heatWaveDehydrationMultiplier"),
                effects.heatWaveDehydrationMultiplier.getPath());
        assertEquals(java.util.List.of("weather", "ring2", "sandstormPushStrength"), effects.sandstormPushStrength.getPath());
        assertEquals(java.util.List.of("weather", "ring2", "sandstormGustTicks"), effects.sandstormGustTicks.getPath());
        assertEquals(2.0, effects.heatWaveDehydrationMultiplier.getDefault());
        assertEquals(0.08, effects.sandstormPushStrength.getDefault());
        assertEquals(10, effects.sandstormGustTicks.getDefault());
        assertSame(null, section(Ring.RING_3).ring2Effects);
    }

    @Test
    void ringsWithoutWeatherHaveNoSettings() {
        assertSame(RingWeatherSettings.NONE, ServerConfig.ringWeather(Ring.RING_1));
        assertSame(RingWeatherSettings.NONE, ServerConfig.ringWeather(Ring.VOID));
    }

    @Test
    void secondsBecomeTicks() {
        Map<RingWeatherType, Integer> weights = new EnumMap<>(RingWeatherType.class);
        weights.put(RingWeatherType.SANDSTORM, 3);
        RingWeatherSettings settings = RingWeatherSettings.ofSeconds(600, 9000, 600, 1200, weights);
        assertEquals(12_000, settings.clearMinTicks());
        assertEquals(180_000, settings.clearMaxTicks());
        assertEquals(12_000, settings.weatherMinTicks());
        assertEquals(24_000, settings.weatherMaxTicks());
        assertEquals(Map.of(RingWeatherType.SANDSTORM, 3), settings.weights());
    }

    @Test
    void badSettingsAreAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> RingWeatherSettings.ofSeconds(10, 5, 1, 1, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> RingWeatherSettings.ofSeconds(1, 1, 10, 5, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> RingWeatherSettings.ofSeconds(1, 1, 1, 1, Map.of(RingWeatherType.CLEAR, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> RingWeatherSettings.ofSeconds(1, 1, 1, 1, Map.of(RingWeatherType.SANDSTORM, -1)));
    }

    private static ServerConfig.RingWeather section(Ring ring) {
        return ServerConfig.RING_WEATHER.get(ring);
    }
}
