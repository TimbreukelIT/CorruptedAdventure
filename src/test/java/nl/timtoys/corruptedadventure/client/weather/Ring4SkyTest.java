package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.biome.BiomeAmbience;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure arithmetic of ring 4's sky and mist (sparring 2026-10-20): dusk window, hue shift, mist height, clouds. */
class Ring4SkyTest {
    /** Vanilla's {@code DimensionType#timeOfDay} for a day time in ticks. */
    private static float timeOfDay(long dayTime) {
        double d0 = Mth.frac(dayTime / 24000.0 - 0.25);
        double d1 = 0.5 - Math.cos(d0 * Math.PI) / 2.0;
        return (float) (d0 * 2.0 + d1) / 3.0F;
    }

    @Test
    void duskOnlyAroundSunriseAndSunset() {
        assertEquals(0.0F, Ring4Dusk.duskness(timeOfDay(6000)));
        assertEquals(0.0F, Ring4Dusk.duskness(timeOfDay(18000)));
        assertTrue(Ring4Dusk.duskness(timeOfDay(12500)) > 0.5F, "sunset");
        assertTrue(Ring4Dusk.duskness(timeOfDay(23200)) > 0.9F, "sunrise");
    }

    @Test
    void theHueShiftKeepsTheBrightness() {
        Vec3 sky = new Vec3(0.2, 0.35, 0.5);
        Vec3 shifted = Ring4Dusk.towardsHue(sky, Vec3.fromRGB24(Ring4Dusk.MYTHIC_SKY), 1.0F);
        assertEquals(BiomeAmbience.luma(sky.x, sky.y, sky.z), BiomeAmbience.luma(shifted.x, shifted.y, shifted.z), 1.0E-6);
        assertTrue(shifted.x > sky.x, "towards pink");
        assertSame(sky, Ring4Dusk.towardsHue(sky, Vec3.ZERO, 1.0F));
    }

    @Test
    void outsideRing4NothingChanges() {
        float[] vanilla = {1.0F, 0.5F, 0.2F, 0.8F};
        assertSame(vanilla, Ring4Dusk.sunriseColor(vanilla, timeOfDay(12500)));
        Vec3 sky = new Vec3(0.3, 0.6, 0.9);
        assertSame(sky, Ring4Dusk.skyColor(sky, timeOfDay(12500)));
        assertEquals(192.0F, Ring4Clouds.cloudHeight(192.0F));
        assertTrue(Float.isNaN(Ring4Clouds.cloudHeight(Float.NaN)));
    }

    @Test
    void lowMistOnlyNearTheSea() {
        assertEquals(1.0F, Ring4Fog.mistHeight(Ring4Fog.SEA_LEVEL + 2.0F));
        assertEquals(1.0F, Ring4Fog.mistHeight(Ring4Fog.SEA_LEVEL + Ring4Fog.MIST_FULL_ABOVE));
        float half = Ring4Fog.mistHeight(Ring4Fog.SEA_LEVEL + (Ring4Fog.MIST_FULL_ABOVE + Ring4Fog.MIST_CLEAR_ABOVE) / 2.0F);
        assertEquals(0.5F, half, 1.0E-4F);
        assertEquals(0.0F, Ring4Fog.mistHeight(Ring4Fog.SEA_LEVEL + Ring4Fog.MIST_CLEAR_ABOVE));
        assertEquals(0.0F, Ring4Fog.mistHeight(200.0F));
    }

    @Test
    void theBlightedHazeThickensTowardsRing5() {
        RingPosition inside = RingPosition.within(Ring.RING_4);
        assertEquals(0.0F, Ring4SkyState.towardsRing5(inside, 0.3));
        assertEquals(0.0F, Ring4SkyState.towardsRing5(inside, Ring4SkyState.BAND_LINE));
        assertEquals(0.5F, Ring4SkyState.towardsRing5(inside, 0.8), 1.0E-6F);
        assertEquals(1.0F, Ring4SkyState.towardsRing5(new RingPosition(Ring.RING_4, Ring.RING_5, 0.7), -1.0));
        assertEquals(0.0F, Ring4SkyState.towardsRing5(new RingPosition(Ring.RING_3, Ring.RING_4, 0.3), -1.0));
    }

    @Test
    void cloudFogMovesOutOnlyAsFarAsTheFogReaches() {
        assertEquals(108.0F, Ring4Clouds.fogExtra(108.0F, Ring4Clouds.REFERENCE_FOG));
        assertEquals(108.0F, Ring4Clouds.fogExtra(108.0F, 512.0F));
        assertEquals(18.0F, Ring4Clouds.fogExtra(108.0F, 32.0F), 1.0E-4F);
        // The distance haze's early start (104 of 160) does not reach the clouds: vanilla's fade, last tenth.
        assertEquals(250.0F - 25.0F, Ring4Clouds.cloudFogStart(104.0F, 160.0F, 250.0F), 1.0E-4F);
        // In a thick fog (mist: 0..56, clouds to 87) the layer 200+ blocks up lies beyond the end anyway.
        assertEquals(87.0F - 8.7F, Ring4Clouds.cloudFogStart(0.0F, 56.0F, 87.0F), 1.0E-4F);
    }
}
