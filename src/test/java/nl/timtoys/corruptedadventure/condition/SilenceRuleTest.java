package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.condition.SilenceRule.Input;
import nl.timtoys.corruptedadventure.condition.SilenceRule.Rates;
import nl.timtoys.corruptedadventure.condition.SilenceRule.Result;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The silence rule (package 97, ring 5 contract §11, answers 2026-10-23): 0 → 100 in ~5 min, open space ×1.5, another
 * player close by halves the rise (the two multiply), a sound heard a moment ago makes it fall whatever else holds,
 * outside the zone it falls; the zone is Nether ring 5, switching at 0.5.
 */
class SilenceRuleTest {
    private static final Rates DEFAULTS = new Rates(
            ServerConfig.SILENCE.rise.getDefault().floatValue(),
            ServerConfig.SILENCE.openMultiplier.getDefault().floatValue(),
            ServerConfig.SILENCE.companionMultiplier.getDefault().floatValue(),
            ServerConfig.SILENCE.soundFall.getDefault().floatValue(),
            ServerConfig.SILENCE.outsideFall.getDefault().floatValue());

    private static Result zone(boolean open, boolean together) {
        return SilenceRule.evaluate(new Input(true, false, open, together), DEFAULTS);
    }

    @Test
    void theHallsFillInAboutFiveMinutes() {
        Result result = zone(false, false);
        assertSame(SilenceExposure.ZONE, result.exposure());
        assertSeconds(300, result);
    }

    @Test
    void openSpaceIsOneAndAHalfTimesFaster() {
        Result result = zone(true, false);
        assertSame(SilenceExposure.OPEN, result.exposure());
        assertEquals(DEFAULTS.rise() * 1.5f, result.rate(), 1e-6f);
        assertSeconds(200, result);
    }

    @Test
    void anotherPlayerHalvesTheRiseAlsoInOpenSpace() {
        Result together = zone(false, true);
        assertSame(SilenceExposure.TOGETHER, together.exposure());
        assertSeconds(600, together);
        Result openTogether = zone(true, true);
        assertSame(SilenceExposure.TOGETHER, openTogether.exposure());
        assertEquals(DEFAULTS.rise() * 1.5f * 0.5f, openTogether.rate(), 1e-6f);
        assertSeconds(400, openTogether);
    }

    @Test
    void aSoundMakesItFallWhateverElseHolds() {
        for (boolean open : new boolean[]{false, true}) {
            for (boolean together : new boolean[]{false, true}) {
                Result result = SilenceRule.evaluate(new Input(true, true, open, together), DEFAULTS);
                assertSame(SilenceExposure.SOUND, result.exposure());
                assertEquals(-2.0f, result.rate());
            }
        }
        // One bell ring: 10 s of relief at 2.0/s.
        assertEquals(-20.0f, SilenceRule.evaluate(Input.HEARD, DEFAULTS).rate() * 10);
    }

    @Test
    void outsideTheZoneFalls() {
        assertEquals(-1.0f, SilenceRule.evaluate(Input.OUTSIDE, DEFAULTS).rate());
        Result result = SilenceRule.evaluate(new Input(false, true, true, true), DEFAULTS);
        assertSame(SilenceExposure.OUTSIDE, result.exposure());
        assertEquals(-1.0f, result.rate());
    }

    @Test
    void aFallRateOfZeroIsPositiveZero() {
        Rates noFall = new Rates(0.33f, 1.5f, 0.5f, 0.0f, 0.0f);
        assertEquals(Float.floatToIntBits(0.0f), Float.floatToIntBits(SilenceRule.evaluate(Input.OUTSIDE, noFall).rate()));
        assertEquals(Float.floatToIntBits(0.0f), Float.floatToIntBits(SilenceRule.evaluate(Input.HEARD, noFall).rate()));
    }

    @Test
    void theZoneIsNetherRingFiveSwitchingAtHalf() {
        assertTrue(SilenceRule.inZone(Level.NETHER, RingPosition.within(Ring.RING_5)));
        assertTrue(SilenceRule.inZone(Level.NETHER, new RingPosition(Ring.RING_4, Ring.RING_5, 0.51)));
        assertFalse(SilenceRule.inZone(Level.NETHER, new RingPosition(Ring.RING_4, Ring.RING_5, 0.49)));
        assertTrue(SilenceRule.inZone(Level.NETHER, new RingPosition(Ring.RING_5, Ring.VOID, 0.49)));
        assertFalse(SilenceRule.inZone(Level.NETHER, new RingPosition(Ring.RING_5, Ring.VOID, 0.51)));
        assertFalse(SilenceRule.inZone(Level.NETHER, RingPosition.within(Ring.RING_4)));
        assertFalse(SilenceRule.inZone(Level.NETHER, RingPosition.within(Ring.VOID)));
        assertFalse(SilenceRule.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_5)));
        assertFalse(SilenceRule.inZone(Level.END, RingPosition.within(Ring.RING_5)));
    }

    private static void assertSeconds(float expected, Result result) {
        float seconds = 100 / result.rate();
        assertTrue(seconds > expected * 0.9f && seconds < expected * 1.1f, "0 -> 100 in " + seconds + " s");
    }
}
