package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.condition.HeatRule.Input;
import nl.timtoys.corruptedadventure.condition.HeatRule.Rates;
import nl.timtoys.corruptedadventure.condition.HeatRule.Result;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatRuleTest {
    /** The defaults of {@code [conditions.heat]} (contract §5). */
    private static final Rates DEFAULTS = new Rates(0.55f, 1.5f, 2.0f, 0.5f, 0.25f, 0.5f, 2.0f, 2.0f, 0.5f);
    /** All fall rates different, so every label is decided by its own rate. */
    private static final Rates DISTINCT = new Rates(0.55f, 1.5f, 2.0f, 0.6f, 0.25f, 0.4f, 2.0f, 1.5f, 0.7f);

    /** In the zone, dry, no refuge. */
    private static Input zone(boolean sunUp, boolean directSun, boolean underRoof, boolean nearSource, boolean inPeak) {
        return new Input(true, sunUp, directSun, underRoof, nearSource, inPeak, false, false);
    }

    @Test
    void sunInTheZoneRisesAtTheBaseRate() {
        assertResult(0.55f, HeatExposure.SUN, HeatRule.evaluate(zone(true, true, false, false, false), DEFAULTS));
    }

    @Test
    void fromZeroToHundredInAboutThreeMinutes() {
        float seconds = 100 / HeatRule.evaluate(zone(true, true, false, false, false), DEFAULTS).rate();
        assertTrue(seconds > 170 && seconds < 190, "0 -> 100 in " + seconds + " s");
    }

    @Test
    void aSourceMultipliesTheRise() {
        assertResult(0.825f, HeatExposure.SOURCE, HeatRule.evaluate(zone(true, true, false, true, false), DEFAULTS));
    }

    @Test
    void aPeakMultipliesTheRise() {
        assertResult(1.1f, HeatExposure.PEAK, HeatRule.evaluate(zone(true, true, false, false, true), DEFAULTS));
    }

    @Test
    void theStrongestFactorWinsAndFactorsDoNotStack() {
        assertResult(1.1f, HeatExposure.PEAK, HeatRule.evaluate(zone(true, true, false, true, true), DEFAULTS));
        Rates strongSource = new Rates(0.55f, 3.0f, 2.0f, 0.5f, 0.25f, 0.5f, 2.0f, 2.0f, 0.5f);
        assertResult(1.65f, HeatExposure.SOURCE, HeatRule.evaluate(zone(true, true, false, true, true), strongSource));
    }

    @Test
    void aSourceHeatsAtNightAndUnderground() {
        assertResult(0.825f, HeatExposure.SOURCE, HeatRule.evaluate(zone(false, false, false, true, false), DEFAULTS));
        assertResult(0.825f, HeatExposure.SOURCE, HeatRule.evaluate(zone(true, false, true, true, false), DEFAULTS));
        assertResult(0.825f, HeatExposure.SOURCE, HeatRule.evaluate(zone(false, false, true, true, false), DEFAULTS));
        // A source on a peak at night: the peak factor is the strongest.
        assertResult(1.1f, HeatExposure.PEAK, HeatRule.evaluate(zone(false, false, false, true, true), DEFAULTS));
    }

    @Test
    void aPeakAloneDoesNotHeatWithoutSunOrSource() {
        assertResult(-0.6f, HeatExposure.NIGHT, HeatRule.evaluate(zone(false, false, false, false, true), DISTINCT));
        assertResult(-0.4f, HeatExposure.ROOF, HeatRule.evaluate(zone(true, false, true, false, true), DISTINCT));
    }

    @Test
    void eachCoolingCaseHasItsRate() {
        assertResult(-0.6f, HeatExposure.NIGHT, HeatRule.evaluate(zone(false, false, false, false, false), DISTINCT));
        assertResult(-0.4f, HeatExposure.ROOF, HeatRule.evaluate(zone(true, false, true, false, false), DISTINCT));
        assertResult(-0.25f, HeatExposure.SHADE, HeatRule.evaluate(zone(true, false, false, false, false), DISTINCT));
        assertResult(-2.0f, HeatExposure.WATER, HeatRule.evaluate(
                new Input(true, false, false, false, false, false, true, false), DISTINCT));
        assertResult(-1.5f, HeatExposure.REFUGE, HeatRule.evaluate(
                new Input(true, false, false, false, false, false, false, true), DISTINCT));
        assertResult(-0.7f, HeatExposure.OUTSIDE, HeatRule.evaluate(
                new Input(false, false, false, false, false, false, false, false), DISTINCT));
    }

    @Test
    void shadeByDayCoolsLessThanARoof() {
        assertTrue(HeatRule.evaluate(zone(true, false, false, false, false), DEFAULTS).rate()
                > HeatRule.evaluate(zone(true, false, true, false, false), DEFAULTS).rate());
    }

    @Test
    void nightUnderARoofTakesTheStrongerFall() {
        assertResult(-0.6f, HeatExposure.NIGHT, HeatRule.evaluate(zone(false, false, true, false, false), DISTINCT));
        Rates deepRoof = new Rates(0.55f, 1.5f, 2.0f, 0.5f, 0.25f, 1.0f, 2.0f, 2.0f, 0.5f);
        assertResult(-1.0f, HeatExposure.ROOF, HeatRule.evaluate(zone(false, false, true, false, false), deepRoof));
        // Equal rates: night comes first in the enum.
        assertResult(-0.5f, HeatExposure.NIGHT, HeatRule.evaluate(zone(false, false, true, false, false), DEFAULTS));
    }

    @Test
    void waterAndTheRefugeBeatSunSourceAndPeak() {
        Input waterInTheSun = new Input(true, true, true, false, true, true, true, false);
        Input refugeInTheSun = new Input(true, true, true, false, true, true, false, true);
        assertResult(-2.0f, HeatExposure.WATER, HeatRule.evaluate(waterInTheSun, DISTINCT));
        assertResult(-1.5f, HeatExposure.REFUGE, HeatRule.evaluate(refugeInTheSun, DISTINCT));
        // Both: the stronger; equal rates take the first label.
        assertResult(-2.0f, HeatExposure.WATER, HeatRule.evaluate(
                new Input(true, true, true, false, true, true, true, true), DEFAULTS));
    }

    @Test
    void outsideTheZoneTheStrongestCoolingWins() {
        assertResult(-2.0f, HeatExposure.WATER, HeatRule.evaluate(
                new Input(false, false, false, false, false, false, true, false), DISTINCT));
        Rates weakWater = new Rates(0.55f, 1.5f, 2.0f, 0.5f, 0.25f, 0.5f, 0.1f, 2.0f, 0.5f);
        assertResult(-0.5f, HeatExposure.OUTSIDE, HeatRule.evaluate(
                new Input(false, false, false, false, false, false, true, false), weakWater));
    }

    @Test
    void onlyExposureInTheZoneIsPositive() {
        for (int bits = 0; bits < 256; bits++) {
            Input input = new Input((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0,
                    (bits & 16) != 0, (bits & 32) != 0, (bits & 64) != 0, (bits & 128) != 0);
            Result result = HeatRule.evaluate(input, DEFAULTS);
            boolean rises = input.inZone() && !input.inWater() && !input.inRefuge()
                    && (input.directSun() || input.nearSource());
            assertEquals(rises, result.rate() > 0, input.toString());
            boolean risingLabel = result.exposure() == HeatExposure.SUN || result.exposure() == HeatExposure.SOURCE
                    || result.exposure() == HeatExposure.PEAK;
            assertEquals(rises, risingLabel, input.toString());
        }
    }

    @Test
    void aZeroFallRateIsPositiveZero() {
        Rates still = new Rates(0.55f, 1.5f, 2.0f, 0f, 0f, 0f, 0f, 0f, 0f);
        assertEquals(Float.floatToIntBits(0.0f),
                Float.floatToIntBits(HeatRule.evaluate(zone(true, false, false, false, false), still).rate()));
    }

    @Test
    void theZoneIsRingThreeOfTheOverworld() {
        assertTrue(HeatRule.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_3)));
        assertFalse(HeatRule.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_2)));
        assertFalse(HeatRule.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_4)));
        assertFalse(HeatRule.inZone(Level.NETHER, RingPosition.within(Ring.RING_3)));
        // Hard at blend 0.5 of a transition.
        assertTrue(HeatRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_2, Ring.RING_3, 0.5)));
        assertFalse(HeatRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_2, Ring.RING_3, 0.49)));
        assertTrue(HeatRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_3, Ring.RING_4, 0.49)));
        assertFalse(HeatRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_3, Ring.RING_4, 0.5)));
    }

    private static void assertResult(float rate, HeatExposure exposure, Result result) {
        assertEquals(rate, result.rate(), 1.0E-6f);
        assertEquals(exposure, result.exposure());
    }
}
