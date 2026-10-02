package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.condition.DehydrationRule.Input;
import nl.timtoys.corruptedadventure.condition.DehydrationRule.Rates;
import nl.timtoys.corruptedadventure.condition.DehydrationRule.Result;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DehydrationRuleTest {
    /** The defaults of {@code [conditions.dehydration]}. */
    private static final Rates DEFAULTS = new Rates(0.4f, 0.5f, 0.5f, 2.0f, 2.0f, 0.5f);
    /** All different, so every label is decided by its own rate. */
    private static final Rates DISTINCT = new Rates(0.4f, 0.3f, 0.6f, 2.0f, 1.5f, 0.7f);

    @Test
    void sunInTheZoneRises() {
        assertResult(0.4f, DehydrationExposure.SUN,
                DehydrationRule.evaluate(new Input(true, true, true, false, false), DEFAULTS));
    }

    @Test
    void eachCoolingSourceHasItsRate() {
        assertResult(-0.3f, DehydrationExposure.SHADE, rated(true, true, false, false, false));
        assertResult(-0.6f, DehydrationExposure.NIGHT, rated(true, false, false, false, false));
        assertResult(-2.0f, DehydrationExposure.WATER, rated(true, true, true, true, false));
        assertResult(-1.5f, DehydrationExposure.OASIS, rated(true, true, true, false, true));
        assertResult(-0.7f, DehydrationExposure.OUTSIDE, rated(false, false, false, false, false));
    }

    @Test
    void theStrongestCoolingWinsAndNothingStacks() {
        // Night + water, oasis + water, shade + oasis.
        assertResult(-2.0f, DehydrationExposure.WATER, rated(true, false, false, true, false));
        assertResult(-2.0f, DehydrationExposure.WATER, rated(true, true, true, true, true));
        assertResult(-1.5f, DehydrationExposure.OASIS, rated(true, true, false, false, true));
        // Water also cools outside the zone, at the stronger of the two.
        assertResult(-2.0f, DehydrationExposure.WATER, rated(false, false, false, true, false));
        // A weak water rate loses to the outside rate.
        Rates weakWater = new Rates(0.4f, 0.5f, 0.5f, 0.1f, 2.0f, 0.5f);
        assertResult(-0.5f, DehydrationExposure.OUTSIDE,
                DehydrationRule.evaluate(new Input(false, false, false, true, false), weakWater));
    }

    @Test
    void equalRatesTakeTheFirstLabel() {
        assertResult(-2.0f, DehydrationExposure.WATER,
                DehydrationRule.evaluate(new Input(true, true, true, true, true), DEFAULTS));
    }

    @Test
    void onlySunIsPositive() {
        // Every combination of the inputs (change 28 doubles exactly the positive rate).
        for (int bits = 0; bits < 32; bits++) {
            Input input = new Input((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0,
                    (bits & 16) != 0);
            for (Rates rates : new Rates[]{DEFAULTS, DISTINCT}) {
                Result result = DehydrationRule.evaluate(input, rates);
                boolean sun = input.inZone() && input.sunUp() && input.directSun() && !input.inWater()
                        && !input.inOasis();
                assertEquals(sun, result.exposure() == DehydrationExposure.SUN, input.toString());
                assertEquals(sun, result.rate() > 0, input.toString());
                if (!input.inZone()) {
                    assertTrue(result.rate() < 0, "never rising outside the zone: " + input);
                }
            }
        }
    }

    @Test
    void zeroFallRateIsPlusZero() {
        Rates noShadeCooling = new Rates(0.4f, 0, 0.5f, 2.0f, 2.0f, 0.5f);
        Result result = DehydrationRule.evaluate(new Input(true, true, false, false, false), noShadeCooling);
        assertEquals(DehydrationExposure.SHADE, result.exposure());
        assertEquals(0, Float.compare(0f, result.rate()), "+0.0, not -0.0");
    }

    @Test
    void zoneIsRing2OfTheOverworldSwitchingAtHalf() {
        assertTrue(DehydrationRule.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_2)));
        // Boundary 1 (ring 1 -> ring 2) and boundary 2 (ring 2 -> ring 3).
        assertFalse(DehydrationRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_1, Ring.RING_2, 0.49)));
        assertTrue(DehydrationRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_1, Ring.RING_2, 0.51)));
        assertTrue(DehydrationRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_2, Ring.RING_3, 0.49)));
        assertFalse(DehydrationRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_2, Ring.RING_3, 0.51)));
        for (Ring ring : new Ring[]{Ring.RING_1, Ring.RING_3, Ring.RING_4, Ring.RING_5, Ring.VOID}) {
            assertFalse(DehydrationRule.inZone(Level.OVERWORLD, RingPosition.within(ring)), ring.name());
        }
        // Nether ring 2 has no condition.
        assertFalse(DehydrationRule.inZone(Level.NETHER, RingPosition.within(Ring.RING_2)));
    }

    @Test
    void submergedMeansAboveTheWaist() {
        assertFalse(ConditionContext.isSubmerged(0), "dry");
        assertFalse(ConditionContext.isSubmerged(8 / 9.0 - 0.001), "standing in one layer of source water");
        assertTrue(ConditionContext.isSubmerged(1.0));
        assertTrue(ConditionContext.isSubmerged(1.8), "two blocks deep");
    }

    @Test
    void labelsAreTheSerializedNames() {
        assertEquals("sun", DehydrationExposure.SUN.getSerializedName());
        assertEquals("shade", DehydrationExposure.SHADE.getSerializedName());
        assertEquals("night", DehydrationExposure.NIGHT.getSerializedName());
        assertEquals("water", DehydrationExposure.WATER.getSerializedName());
        assertEquals("oasis", DehydrationExposure.OASIS.getSerializedName());
        assertEquals("outside", DehydrationExposure.OUTSIDE.getSerializedName());
    }

    private static Result rated(boolean inZone, boolean sunUp, boolean directSun, boolean inWater, boolean inOasis) {
        return DehydrationRule.evaluate(new Input(inZone, sunUp, directSun, inWater, inOasis), DISTINCT);
    }

    private static void assertResult(float rate, DehydrationExposure exposure, Result result) {
        assertEquals(exposure, result.exposure());
        assertEquals(rate, result.rate(), 1e-6);
    }
}
