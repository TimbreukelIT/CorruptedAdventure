package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import nl.timtoys.corruptedadventure.weather.SandstormWind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the ring 4 weather rendering (package 70). */
class Ring4WeatherRenderTest {
    @Test
    void denseFogClosesToAboutThirtyTwoBlocks() {
        assertEquals(32.0F, FogBlend.far(192.0F, DenseFogRenderer.FOG_FAR, 1.0F));
        assertEquals(112.0F, FogBlend.far(192.0F, DenseFogRenderer.FOG_FAR, 0.5F), 1.0E-4F);
    }

    /** The storm's rain slants along the same wind as the server's push, flatter as the storm grows. */
    @Test
    void stormRainSlantsWithTheWind() {
        RingWeatherState storm = RingWeatherState.start(RingWeatherType.STORM_WIND, 20000, 1234);
        Vec3 wind = SandstormWind.direction(storm);
        Vec3 full = StormWindRenderer.slant(storm, 1.0F);
        assertEquals(StormWindRenderer.SLANT, full.length(), 1.0E-6);
        assertEquals(1.0, full.normalize().dot(wind), 1.0E-9);
        assertEquals(StormWindRenderer.SLANT * 0.5, StormWindRenderer.slant(storm, 0.5F).length(), 1.0E-6);
        assertEquals(0.0, StormWindRenderer.slant(storm, 0.0F).length(), 1.0E-9);
    }

    /** The debug log (test S) shows the storm's wind like the sandstorm's; fog and toxic rain have none. */
    @Test
    void debugLogShowsTheStormWind() {
        RingWeatherState storm = RingWeatherState.start(RingWeatherType.STORM_WIND, 20000, 1234);
        String line = RingWeatherDebugLog.describe(List.of(new RingWeatherMix.Share(Ring.RING_4, storm, 1.0F, 0.8F)));
        assertEquals("ring_4 storm_wind strength 0.75 wind " + SandstormWind.describe(storm), line);
        RingWeatherState fog = RingWeatherState.start(RingWeatherType.DENSE_FOG, 20000, 1234);
        assertEquals("ring_4 dense_fog strength 1.00",
                RingWeatherDebugLog.describe(List.of(new RingWeatherMix.Share(Ring.RING_4, fog, 1.0F, 1.0F))));
        assertTrue(SandstormWind.hasWind(RingWeatherType.STORM_WIND));
        assertTrue(SandstormWind.hasWind(RingWeatherType.SANDSTORM));
        assertFalse(SandstormWind.hasWind(RingWeatherType.TOXIC_RAIN));
        assertFalse(SandstormWind.hasWind(RingWeatherType.DENSE_FOG));
    }
}
