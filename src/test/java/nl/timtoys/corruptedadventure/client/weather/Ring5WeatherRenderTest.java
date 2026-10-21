package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.sounds.SoundSource;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the ring 5 weather rendering (package 92). */
class Ring5WeatherRenderTest {
    /** Vanilla's rain falls (the scroll drops over time); reverse rain rises (the scroll grows). */
    @Test
    void reverseRainScrollsTheOtherWay() {
        int hash = 12345;
        float falling = RainSheet.scroll(101.0F, hash, 3.5F, 1.0F) - RainSheet.scroll(100.0F, hash, 3.5F, 1.0F);
        float rising = RainSheet.scroll(101.0F, hash, 3.5F, ReverseRainRenderer.SPEED)
                - RainSheet.scroll(100.0F, hash, 3.5F, ReverseRainRenderer.SPEED);
        assertTrue(falling < 0.0F, "falling " + falling);
        assertTrue(rising > 0.0F, "rising " + rising);
        assertEquals(-falling * -ReverseRainRenderer.SPEED, rising, 1.0E-4F);
    }

    /** The standstill's streaks hang at one time per standstill, the same on every client, in float range. */
    @Test
    void standstillFreezesTheRainAtTheSeedsTime() {
        for (int seed : new int[]{1, -1, 42, Integer.MIN_VALUE, Integer.MAX_VALUE, 987654321}) {
            float time = StandstillRenderer.frozenTime(seed);
            assertEquals(time, StandstillRenderer.frozenTime(seed));
            assertTrue(time >= 0.0F && time < 4096.0F, "time " + time);
            assertEquals(time, (float) Math.floor(time));
        }
    }

    /** The clouds: vanilla's clock without a standstill, still at full strength, never running backwards. */
    @Test
    void cloudClockSlowsToAHalt() {
        assertEquals(1000.25, StandstillClouds.cloudTime(1000, 0.25F, 0.0F, 0.0), 1.0E-9);
        assertEquals(StandstillClouds.cloudTime(1000, 0.0F, 1.0F, 30.0), StandstillClouds.cloudTime(1000, 0.9F, 1.0F, 30.0), 1.0E-9);
        // Across a tick at half strength: before the tick (lag 10, partial 1) equals after it (lag 10.5, partial 0).
        double before = StandstillClouds.cloudTime(1000, 1.0F, 0.5F, 10.0);
        double after = StandstillClouds.cloudTime(1001, 0.0F, 0.5F, 10.5);
        assertEquals(before, after, 1.0E-6);
        assertTrue(StandstillClouds.cloudTime(1000, 0.5F, 0.5F, 10.0) > StandstillClouds.cloudTime(1000, 0.0F, 0.5F, 10.0));
    }

    /** A full standstill drains to 15 %, nothing at 0; the UI (MASTER) is never touched. */
    @Test
    void standstillVolumeFactor() {
        assertEquals(1.0F, Ring5SoundDamping.standstillFactor(0.0F));
        assertEquals(0.15F, Ring5SoundDamping.standstillFactor(1.0F), 1.0E-6F);
        assertEquals(1.0F - 0.85F * 0.5F, Ring5SoundDamping.standstillFactor(0.5F), 1.0E-6F);
        assertEquals(0.15F, Ring5SoundDamping.standstillFactor(3.0F), 1.0E-6F);
        assertEquals(1.0F, Ring5SoundDamping.volume(SoundSource.MASTER));
        assertEquals(1.0F, Ring5SoundDamping.volume(null));
        assertEquals(1.0F, Ring5SoundDamping.volume(SoundSource.MUSIC));
    }

    @Test
    void glitchNoiseScalesWithTheStorm() {
        assertEquals(0, GlitchStormOverlay.blocks(0.0F));
        assertEquals(GlitchStormOverlay.MAX_BLOCKS / 2, GlitchStormOverlay.blocks(0.5F));
        assertEquals(GlitchStormOverlay.MAX_BLOCKS, GlitchStormOverlay.blocks(2.0F));
        assertTrue(GlitchStormOverlay.MAX_HICCUP_TICKS <= 2);
    }

    /** Ring 5 splits into its two bands' weathers, weights adding up to the ring's; one share if both are the same. */
    @Test
    void ring5SplitsIntoBothBands() {
        RingWeatherState set = RingWeatherState.start(RingWeatherType.GLITCH_STORM, 20000, 0);
        RingWeatherMix.Share whole = new RingWeatherMix.Share(Ring.RING_5, set, 0.8F, set.intensity() * 0.8F);
        List<RingWeatherMix.Share> same = new ArrayList<>();
        RingWeatherClientHooks.addRing5(same, whole, 0.0);
        assertEquals(1, same.size());
        assertSame(whole, same.getFirst());

        RingWeatherState storm = RingWeatherState.start(RingWeatherType.GLITCH_STORM, 20000, seedWithReverseRainOutside());
        RingWeatherState mid = storm.countDown(5000);
        RingWeatherMix.Share ring5 = new RingWeatherMix.Share(Ring.RING_5, mid, 0.8F, mid.intensity() * 0.8F);
        List<RingWeatherMix.Share> split = new ArrayList<>();
        RingWeatherClientHooks.addRing5(split, ring5, 0.0);
        assertEquals(2, split.size());
        assertEquals(RingWeatherType.GLITCH_STORM, split.get(0).state().type());
        assertEquals(RingWeatherType.REVERSE_RAIN, split.get(1).state().type());
        assertEquals(0.4F, split.get(0).weight(), 1.0E-6F);
        assertEquals(0.4F, split.get(1).weight(), 1.0E-6F);
        assertEquals(0.4F, split.get(1).strength(), 1.0E-6F);

        List<RingWeatherMix.Share> deepInside = new ArrayList<>();
        RingWeatherClientHooks.addRing5(deepInside, ring5, -1.0);
        assertEquals(1, deepInside.size());
        assertEquals(RingWeatherType.GLITCH_STORM, deepInside.getFirst().state().type());
        List<RingWeatherMix.Share> farOut = new ArrayList<>();
        RingWeatherClientHooks.addRing5(farOut, ring5, 1.0);
        assertEquals(1, farOut.size());
        assertEquals(RingWeatherType.REVERSE_RAIN, farOut.getFirst().state().type());
    }

    private static int seedWithReverseRainOutside() {
        for (int seed = 1; ; seed++) {
            RingWeatherState state = RingWeatherState.start(RingWeatherType.GLITCH_STORM, 20000, seed);
            List<RingWeatherMix.Share> shares = new ArrayList<>();
            RingWeatherClientHooks.addRing5(shares, new RingWeatherMix.Share(Ring.RING_5, state, 1.0F, 0.0F), 1.0);
            if (shares.getFirst().state().type() == RingWeatherType.REVERSE_RAIN) {
                return seed;
            }
        }
    }
}
