package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.Ring5WeatherBands;
import nl.timtoys.corruptedadventure.weather.RingWeatherSettings;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** {@code [weather.ring5]} (package 92): exactly the keys of plan 92 §2 with the P2 defaults. */
class Ring5WeatherConfigTest {
    @Test
    void ring5SectionHasThePlanKeys() {
        ServerConfig.RingWeather section = ServerConfig.RING_WEATHER.get(Ring.RING_5);
        assertEquals(List.of("weather", "ring5", "clearMinSeconds"), section.clearMinSeconds.getPath());
        assertEquals(List.of("weather", "ring5", "weatherMaxSeconds"), section.weatherMaxSeconds.getPath());
        assertEquals(List.of("weather", "ring5", "glitchStormWeight"), section.weights.get(RingWeatherType.GLITCH_STORM).getPath());
        assertEquals(List.of("weather", "ring5", "reverseRainWeight"), section.weights.get(RingWeatherType.REVERSE_RAIN).getPath());
        assertEquals(List.of("weather", "ring5", "standstillWeight"), section.weights.get(RingWeatherType.STANDSTILL).getPath());
        assertEquals(3, section.weights.size());

        ServerConfig.Ring5WeatherEffects effects = ServerConfig.RING_5_WEATHER;
        assertSame(section.ring5Effects, effects);
        assertEquals(List.of("weather", "ring5", "outerGlitchStormWeight"), effects.outerGlitchStormWeight.getPath());
        assertEquals(List.of("weather", "ring5", "outerReverseRainWeight"), effects.outerReverseRainWeight.getPath());
        assertEquals(List.of("weather", "ring5", "outerStandstillWeight"), effects.outerStandstillWeight.getPath());
        assertEquals(List.of("weather", "ring5", "standstillMinSeconds"), effects.standstillMinSeconds.getPath());
        assertEquals(List.of("weather", "ring5", "standstillMaxSeconds"), effects.standstillMaxSeconds.getPath());
        assertEquals(List.of("weather", "ring5", "glitchStormVoidMultiplier"), effects.glitchStormVoidMultiplier.getPath());
        assertEquals(List.of("weather", "ring5", "reverseRainVoidMultiplier"), effects.reverseRainVoidMultiplier.getPath());
        assertEquals(List.of("weather", "ring5", "standstillVoidMultiplier"), effects.standstillVoidMultiplier.getPath());
        assertNull(ServerConfig.RING_WEATHER.get(Ring.RING_4).ring5Effects);
        assertNull(ServerConfig.RING_WEATHER.get(Ring.RING_5).ring4Effects);
    }

    /** P2 (2026-10-21): inner 55/15/30, outer 20/70/10, standstill 40–120 s; void factors 2.0/1.5/1.5. */
    @Test
    void ring5Defaults() {
        ServerConfig.RingWeather section = ServerConfig.RING_WEATHER.get(Ring.RING_5);
        assertEquals(55, section.weights.get(RingWeatherType.GLITCH_STORM).getDefault());
        assertEquals(15, section.weights.get(RingWeatherType.REVERSE_RAIN).getDefault());
        assertEquals(30, section.weights.get(RingWeatherType.STANDSTILL).getDefault());
        ServerConfig.Ring5WeatherEffects effects = ServerConfig.RING_5_WEATHER;
        assertEquals(20, effects.outerGlitchStormWeight.getDefault());
        assertEquals(70, effects.outerReverseRainWeight.getDefault());
        assertEquals(10, effects.outerStandstillWeight.getDefault());
        assertEquals(40, effects.standstillMinSeconds.getDefault());
        assertEquals(120, effects.standstillMaxSeconds.getDefault());
        assertEquals(2.0, effects.voidMultiplier(RingWeatherType.GLITCH_STORM, ModConfigSpec.DoubleValue::getDefault));
        assertEquals(1.5, effects.voidMultiplier(RingWeatherType.REVERSE_RAIN, ModConfigSpec.DoubleValue::getDefault));
        assertEquals(1.5, effects.voidMultiplier(RingWeatherType.STANDSTILL, ModConfigSpec.DoubleValue::getDefault));
        assertEquals(1.0, effects.voidMultiplier(RingWeatherType.TOXIC_RAIN, ModConfigSpec.DoubleValue::getDefault));
        assertEquals(1.0, effects.voidMultiplier(RingWeatherType.CLEAR, ModConfigSpec.DoubleValue::getDefault));
    }

    /** Before the synced config arrives (and in tests) the band rule runs on the defaults, in ticks. */
    @Test
    void bandSettingsUseTheDefaultsUntilLoaded() {
        Ring5WeatherBands.Settings settings = ServerConfig.ring5Bands();
        assertEquals(Map.of(RingWeatherType.GLITCH_STORM, 20, RingWeatherType.REVERSE_RAIN, 70, RingWeatherType.STANDSTILL, 10),
                settings.outerWeights());
        assertEquals(new RingWeatherSettings.Range(800, 2400), settings.standstillTicks());
    }

    @Test
    void standstillMinAboveMaxFallsBackToTheDefaults() {
        assertEquals(new RingWeatherSettings.Range(40, 120), ServerConfig.Ring5WeatherEffects.standstillSeconds(200, 100));
        assertEquals(new RingWeatherSettings.Range(30, 60), ServerConfig.Ring5WeatherEffects.standstillSeconds(30, 60));
        assertEquals(new RingWeatherSettings.Range(50, 50), ServerConfig.Ring5WeatherEffects.standstillSeconds(50, 50));
    }
}
