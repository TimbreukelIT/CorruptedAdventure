package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pure parts of ring 5's and Nether ring 5's client atmosphere (package 98, V98-1 … V98-11 = A): the sky's
 * blackness per band, the stars, the fog distances, the glitch fog, the echo footsteps, the silence and the void meter's
 * glitch visuals.
 */
class Ring5AtmosphereTest {
    private static final double EDGE = 9700.0;

    /** V98-1 A: 0 on the band line, rising towards the edge, ~0.85 on it, 1 from 200 blocks over the void. */
    @Test
    void theSkyGoesBlackFromTheBandLineToTheVoid() {
        assertEquals(0.0F, Ring5Sky.outward(-50.0, 8800.0, EDGE), "inner band");
        assertEquals(0.0F, Ring5Sky.blackness(Ring5Sky.outward(0.0, 8850.0, EDGE), 8850.0 - EDGE), "on the line");
        float previous = -1.0F;
        for (double distance = 8850.0; distance <= 10100.0; distance += 25.0) {
            double signed = distance - 8850.0;
            float black = Ring5Sky.blackness(Ring5Sky.outward(signed, distance, EDGE), distance - EDGE);
            assertTrue(black >= previous, "never lighter outward at " + distance);
            assertTrue(black >= 0.0F && black <= 1.0F);
            previous = black;
        }
        assertEquals(Ring5Sky.EDGE_BLACKNESS, Ring5Sky.blackness(Ring5Sky.outward(850.0, EDGE, EDGE), 0.0), 1.0E-6, "the edge");
        assertEquals(1.0F, Ring5Sky.blackness(1.0F, Ring5Sky.VOID_RUN), 1.0E-6, "over the void");
        assertEquals(1.0F, Ring5Sky.blackness(1.0F, 400.0), 1.0E-6);
    }

    /** V98-2 A: stars at least the blackness's (vanilla's midnight 0.5 at full black); sun and moon dimmed to 40 %. */
    @Test
    void starsShineByTheBlacknessAndTheSunDims() {
        assertEquals(0.3F, Ring5Sky.stars(0.3F, 0.0F), "vanilla at night");
        assertEquals(Ring5Sky.FULL_STARS, Ring5Sky.stars(0.0F, 1.0F), "noon in the void");
        assertEquals(0.25F, Ring5Sky.stars(0.0F, 0.5F), 1.0E-6);
        assertEquals(1.0F, Ring5Sky.sunFactor(0.0F));
        assertEquals(Ring5Sky.MIN_SUN, Ring5Sky.sunFactor(1.0F), 1.0E-6);
        Vec3 sky = new Vec3(0.4, 0.35, 0.45);
        assertSame(sky, Ring5Sky.skyColor(sky), "nothing outside ring 5 (no tick has run)");
    }

    /** V98-3 A: far = 0.45 × view, within 48–112. V98-7 A: Nether ring 5 at 0.8 × view (≤ 192), near 0.4 × far. */
    @Test
    void fogDistancesFollowTheView() {
        assertEquals(48.0F, Ring5Fog.innerFar(64.0F), "short view: at least 48");
        assertEquals(0.45F * 160.0F, Ring5Fog.innerFar(160.0F), 1.0E-4);
        assertEquals(112.0F, Ring5Fog.innerFar(512.0F), "long view: at most 112");
        float[] nether = NetherRing5Fog.planes(128.0F);
        assertEquals(0.8F * 128.0F, nether[0], 1.0E-4);
        assertEquals(0.4F * nether[0], nether[1], 1.0E-4);
        assertEquals(0.8F * 192.0F, NetherRing5Fog.planes(512.0F)[0], 1.0E-4, "capped at 192");
    }

    /** V98-4 A: a stutter of 1–3 ticks at 40–70 % every 4–12 s, twice as often at rate 2; Screen Effect 0 = none. */
    @Test
    void theGlitchFogStuttersNowAndThen() {
        for (float rate : new float[] {1.0F, 2.0F}) {
            GlitchFog fog = new GlitchFog();
            RandomSource random = RandomSource.create(20260930L);
            List<Integer> gaps = new ArrayList<>();
            int run = 0;
            int lastStart = -1;
            for (int tick = 0; tick < 20 * 60 * 20; tick++) {
                fog.tick(random, true, rate);
                if (fog.stuttering()) {
                    float factor = fog.farFactor(1.0F);
                    assertTrue(factor >= GlitchFog.MIN_FACTOR && factor <= GlitchFog.MAX_FACTOR, "factor " + factor);
                    assertEquals(1.0F, fog.farFactor(0.0F), "Screen Effect Scale 0");
                    if (run++ == 0) {
                        if (lastStart >= 0) {
                            gaps.add(tick - lastStart);
                        }
                        lastStart = tick;
                    }
                    assertTrue(run <= GlitchFog.MAX_TICKS, "never longer than 3 ticks");
                } else {
                    assertEquals(1.0F, fog.farFactor(1.0F));
                    assertFalse(fog.violet());
                    run = 0;
                }
            }
            for (int gap : gaps) {
                assertTrue(gap >= GlitchFog.MIN_WAIT / rate && gap <= GlitchFog.MAX_WAIT / rate + GlitchFog.MAX_TICKS + 1,
                        "gap " + gap + " at rate " + rate);
            }
            assertTrue(gaps.size() > 50, "plenty of stutters");
        }
        GlitchFog off = new GlitchFog();
        RandomSource random = RandomSource.create(1L);
        for (int tick = 0; tick < 2000; tick++) {
            off.tick(random, false, 1.0F);
            assertFalse(off.stuttering(), "inactive: never");
        }
    }

    /** V98-6 A: a burst after 1–3 min of walking, 4–10 echoes 8–16 ticks late, one extra after a stop. */
    @Test
    void echoesComeInBurstsAndOneExtraAfterAStop() {
        EchoSteps steps = new EchoSteps();
        RandomSource random = RandomSource.create(7L);
        long now = 0;
        while (!steps.inBurst()) {
            assertEquals(EchoSteps.NONE, steps.step(now), "no echo before a burst");
            steps.tick(now++, true, random);
            assertTrue(now <= EchoSteps.MAX_WALK + 1, "a burst within 3 min of walking");
        }
        assertTrue(now >= EchoSteps.MIN_WALK, "not before 1 min");
        assertTrue(steps.behind() >= EchoSteps.MIN_BEHIND && steps.behind() <= EchoSteps.MAX_BEHIND);
        // Two steps, 8 ticks apart, then the player stops.
        long first = steps.step(now);
        assertTrue(first - now >= EchoSteps.MIN_DELAY && first - now <= EchoSteps.MAX_DELAY);
        long second = steps.step(now + 8);
        assertEquals(first - now, second - (now + 8), "the same delay through the burst");
        long extra = EchoSteps.NONE;
        for (long t = now + 9; t < now + 8 + EchoSteps.STOPPED_AFTER + 1 && extra == EchoSteps.NONE; t++) {
            extra = steps.tick(t, false, random);
        }
        assertEquals(second + 8, extra, "one more echo, one step after the last");
        assertFalse(steps.inBurst(), "and that was it");

        // A burst that runs out while walking ends without an extra echo.
        EchoSteps walker = new EchoSteps();
        long t = 0;
        while (!walker.inBurst()) {
            walker.tick(t++, true, random);
        }
        int echoes = 0;
        while (walker.inBurst()) {
            assertTrue(walker.step(t) != EchoSteps.NONE);
            walker.tick(t, true, random);
            echoes++;
            t += 8;
        }
        assertTrue(echoes >= EchoSteps.MIN_STEPS && echoes <= EchoSteps.MAX_STEPS, "echoes " + echoes);
        for (int i = 0; i < 40; i++) {
            assertEquals(EchoSteps.NONE, walker.tick(t + i, false, random), "no extra after a finished burst");
        }
    }

    /** The echo is louder at its source so it is heard at 0.5–0.7 of the step; never above 1. */
    @Test
    void echoVolumeMakesUpForTheDistance() {
        assertEquals(0.15F * 0.6F / (1.0F - 8.0F / 16.0F), EchoSteps.echoVolume(0.15F, 0.6F, 8.0), 1.0E-6);
        assertEquals(1.0F, EchoSteps.echoVolume(0.9F, 0.7F, 12.0), "capped");
    }

    /** V98-9 A: 1 below 30, gliding to 0.35 at 90, then silent; heartbeat 20 → 10 ticks; HUD at 60 with hysteresis. */
    @Test
    void theSilenceDrainsTheSoundAndTheHud() {
        assertEquals(1.0F, SilenceEffects.targetVolume(29, 30, 90));
        assertEquals(1.0F, SilenceEffects.targetVolume(30, 30, 90));
        assertEquals((1.0F + SilenceEffects.LOW_VOLUME) / 2.0F, SilenceEffects.targetVolume(60, 30, 90), 1.0E-6);
        assertEquals(0.0F, SilenceEffects.targetVolume(90, 30, 90));
        assertEquals(SilenceEffects.SLOW_BEAT, SilenceEffects.beatTicks(90, 90));
        assertEquals(SilenceEffects.FAST_BEAT, SilenceEffects.beatTicks(100, 90));
        assertFalse(SilenceEffects.hidden(59, 60, false));
        assertTrue(SilenceEffects.hidden(60, 60, false));
        assertTrue(SilenceEffects.hidden(58, 60, true), "hysteresis");
        assertFalse(SilenceEffects.hidden(57, 60, true));
        assertFalse(SilenceEffects.HIDDEN_LAYERS.contains(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR));
        assertFalse(SilenceEffects.HIDDEN_LAYERS.contains(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR));
        assertFalse(SilenceEffects.HIDDEN_LAYERS.contains(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CHAT));
        assertTrue(SilenceEffects.HIDDEN_LAYERS.contains(net.neoforged.neoforge.client.gui.VanillaGuiLayers.PLAYER_HEALTH));
    }

    /** V98-11 A: 0 below the first threshold, growing to 0.6 of a storm at 100. */
    @Test
    void voidGlitchesGrowWithTheMeter() {
        assertEquals(0.0F, VoidGlitchEffects.strength(29, 30));
        assertEquals(0.0F, VoidGlitchEffects.strength(30, 30));
        assertEquals(VoidGlitchEffects.MAX_SHARE / 2.0F, VoidGlitchEffects.strength(65, 30), 1.0E-6);
        assertEquals(VoidGlitchEffects.MAX_SHARE, VoidGlitchEffects.strength(100, 30), 1.0E-6);
    }

    /** The silence's factor joins the weather's in the shared damping; MASTER (the UI and the heartbeat) never. */
    @Test
    void theSilenceFeedsTheSharedDamping() {
        Ring5SoundDamping.reset();
        Ring5SoundDamping.feedSilence(0.4F);
        Ring5SoundDamping.advance();
        assertEquals(0.4F, Ring5SoundDamping.volume(net.minecraft.sounds.SoundSource.MUSIC), 1.0E-6);
        assertEquals(1.0F, Ring5SoundDamping.volume(net.minecraft.sounds.SoundSource.MASTER));
        Ring5SoundDamping.advance();
        assertEquals(1.0F, Ring5SoundDamping.volume(net.minecraft.sounds.SoundSource.MUSIC), "without a feed back to full");
        Ring5SoundDamping.reset();
    }
}
