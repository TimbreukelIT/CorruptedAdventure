package nl.timtoys.corruptedadventure.client.weather;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure arithmetic of the client weather: fog blending, exposure easing and the debug log line. */
class WeatherClientMathTest {
    @Test
    void fogFarGoesFromVanillaToTheWeather() {
        assertEquals(192.0F, FogBlend.far(192.0F, SandstormRenderer.FOG_FAR, 0.0F));
        assertEquals(24.0F, FogBlend.far(192.0F, SandstormRenderer.FOG_FAR, 1.0F));
        assertEquals(108.0F, FogBlend.far(192.0F, SandstormRenderer.FOG_FAR, 0.5F), 1.0E-4F);
        // Never further than vanilla (a short render distance stays short), and amount is clamped.
        assertEquals(16.0F, FogBlend.far(16.0F, SandstormRenderer.FOG_FAR, 1.0F));
        assertEquals(24.0F, FogBlend.far(192.0F, SandstormRenderer.FOG_FAR, 3.0F));
    }

    @Test
    void fogNearGoesToZeroAndNeverPastFar() {
        assertEquals(172.0F, FogBlend.near(172.0F, 192.0F, 0.0F));
        assertEquals(0.0F, FogBlend.near(172.0F, 24.0F, 1.0F));
        assertEquals(24.0F, FogBlend.near(172.0F, 24.0F, 0.5F));
        assertEquals(0.5F, FogBlend.colour(0.0F, 1.0F, 0.5F));
    }

    @Test
    void exposureEasesOverTwoSeconds() {
        float value = 1.0F;
        int ticks = 0;
        while (value > 0.0F) {
            value = SkyExposure.approach(value, 0.0F, SkyExposure.STEP_PER_TICK);
            ticks++;
        }
        assertTrue(ticks == 40 || ticks == 41, "~2 s: " + ticks); // 41 if float rounding leaves a last crumb
        assertEquals(0.5F, SkyExposure.approach(0.49F, 0.5F, SkyExposure.STEP_PER_TICK));
        assertEquals(0.525F, SkyExposure.approach(0.5F, 1.0F, SkyExposure.STEP_PER_TICK), 1.0E-6F);
    }

    @Test
    void debugLineIsEqualForEqualWeather() {
        RingWeatherState storm = new RingWeatherState(RingWeatherType.SANDSTORM, 3000, 6000, 42);
        List<RingWeatherMix.Share> shares = List.of(new RingWeatherMix.Share(Ring.RING_2, storm, 1.0F, 0.8F));
        String line = RingWeatherDebugLog.describe(shares);
        assertTrue(line.startsWith("ring_2 sandstorm strength 0.75 wind "), line);
        // Small strength differences between two clients land in the same 0.25 step.
        assertEquals(line, RingWeatherDebugLog.describe(List.of(new RingWeatherMix.Share(Ring.RING_2, storm, 1.0F, 0.79F))));
        RingWeatherState clear = RingWeatherState.start(RingWeatherType.CLEAR, 6000, 0);
        assertEquals("none", RingWeatherDebugLog.describe(List.of(new RingWeatherMix.Share(Ring.RING_2, clear, 1.0F, 0.0F))));
    }
}
