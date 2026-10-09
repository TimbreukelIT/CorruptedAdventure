package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.condition.ColdRule.Input;
import nl.timtoys.corruptedadventure.condition.ColdRule.Rates;
import nl.timtoys.corruptedadventure.condition.ColdRule.Result;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColdRuleTest {
    /** The defaults of {@code [conditions.cold]} (answers G2-C1..C3). */
    private static final Rates DEFAULTS = new Rates(
            ServerConfig.COLD.riseInZone.getDefault().floatValue(),
            ServerConfig.COLD.iceMultiplier.getDefault().floatValue(),
            ServerConfig.COLD.exposedMultiplier.getDefault().floatValue(),
            ServerConfig.COLD.warmthFall.getDefault().floatValue(),
            ServerConfig.COLD.outsideFall.getDefault().floatValue());

    private static Input zone(boolean onIce, boolean inExposed) {
        return new Input(true, false, onIce, inExposed);
    }

    @Test
    void theZoneRisesFromZeroToHundredInAboutFourMinutes() {
        Result result = ColdRule.evaluate(zone(false, false), DEFAULTS);
        assertSame(ColdExposure.ZONE, result.exposure());
        float seconds = 100 / result.rate();
        assertTrue(seconds > 225 && seconds < 255, "0 -> 100 in " + seconds + " s");
    }

    @Test
    void iceAndTheExposedBiomesRiseOneAndAHalfTimesFaster() {
        float base = ColdRule.evaluate(zone(false, false), DEFAULTS).rate();
        assertResult(base * 1.5f, ColdExposure.ICE, ColdRule.evaluate(zone(true, false), DEFAULTS));
        assertResult(base * 1.5f, ColdExposure.EXPOSED, ColdRule.evaluate(zone(false, true), DEFAULTS));
    }

    @Test
    void theStrongestFactorWinsAndFactorsDoNotStack() {
        float base = DEFAULTS.rise();
        // Equal factors: ice before exposed.
        assertResult(base * 1.5f, ColdExposure.ICE, ColdRule.evaluate(zone(true, true), DEFAULTS));
        Rates strongExposed = new Rates(base, 1.5f, 2.0f, 2.0f, 0.5f);
        assertResult(base * 2.0f, ColdExposure.EXPOSED, ColdRule.evaluate(zone(true, true), strongExposed));
    }

    @Test
    void warmthCoolsEverywhereInTheZone() {
        for (boolean onIce : new boolean[]{false, true}) {
            for (boolean inExposed : new boolean[]{false, true}) {
                assertResult(-2.0f, ColdExposure.WARMTH,
                        ColdRule.evaluate(new Input(true, true, onIce, inExposed), DEFAULTS));
            }
        }
    }

    @Test
    void outsideTheZoneFalls() {
        assertResult(-0.5f, ColdExposure.OUTSIDE, ColdRule.evaluate(Input.OUTSIDE, DEFAULTS));
        assertResult(-0.5f, ColdExposure.OUTSIDE, ColdRule.evaluate(new Input(false, true, true, true), DEFAULTS));
    }

    @Test
    void aFallRateOfZeroIsPositiveZero() {
        Rates noFall = new Rates(0.42f, 1.5f, 1.5f, 0.0f, 0.0f);
        assertEquals(0.0f, ColdRule.evaluate(Input.OUTSIDE, noFall).rate());
        assertEquals(Float.floatToIntBits(0.0f), Float.floatToIntBits(ColdRule.evaluate(Input.OUTSIDE, noFall).rate()));
    }

    @Test
    void onlyAWarmthOrOutsideInputFalls() {
        for (int bits = 0; bits < 16; bits++) {
            Input input = new Input((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0);
            boolean falls = !input.inZone() || input.nearWarmth();
            assertEquals(falls, ColdRule.evaluate(input, DEFAULTS).rate() < 0, input.toString());
        }
    }

    @Test
    void theZoneIsRingThreeOfTheNetherSwitchingAtTheMiddle() {
        assertTrue(ColdRule.inZone(Level.NETHER, RingPosition.within(Ring.RING_3)));
        assertTrue(ColdRule.inZone(Level.NETHER, new RingPosition(Ring.RING_2, Ring.RING_3, 0.51)));
        assertFalse(ColdRule.inZone(Level.NETHER, new RingPosition(Ring.RING_2, Ring.RING_3, 0.49)));
        assertTrue(ColdRule.inZone(Level.NETHER, new RingPosition(Ring.RING_3, Ring.RING_4, 0.49)));
        assertFalse(ColdRule.inZone(Level.NETHER, new RingPosition(Ring.RING_3, Ring.RING_4, 0.51)));
        assertFalse(ColdRule.inZone(Level.NETHER, RingPosition.within(Ring.RING_2)));
        assertFalse(ColdRule.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_3)));
        assertFalse(ColdRule.inZone(Level.END, RingPosition.within(Ring.RING_3)));
    }

    @Test
    void onlyARisingColdRateIsModified() {
        assertTrue(ColdRule.isRisingCold(Condition.COLD, 0.42f));
        assertFalse(ColdRule.isRisingCold(Condition.COLD, -2.0f));
        assertFalse(ColdRule.isRisingCold(Condition.COLD, 0.0f));
        assertFalse(ColdRule.isRisingCold(Condition.HEAT, 0.55f));
    }

    @Test
    void leatherTakesFifteenPercentPerPieceAndNeverMakesImmune() {
        float reduction = ServerConfig.COLD.leatherReductionPerPiece.getDefault().floatValue();
        assertEquals(1.0f, LeatherColdModifier.factor(0, reduction), 1.0E-6f);
        assertEquals(0.85f, LeatherColdModifier.factor(1, reduction), 1.0E-6f);
        assertEquals(0.4f, LeatherColdModifier.factor(4, reduction), 1.0E-6f);
        // The config maximum (0.2) still leaves a fifth with full leather: never immune.
        assertEquals(0.2f, LeatherColdModifier.factor(4, 0.2f), 1.0E-6f);
        // Never below 0, whatever the input.
        assertEquals(0.0f, LeatherColdModifier.factor(5, 0.25f), 1.0E-6f);
    }

    @Test
    void theFreezeKeepsThePlayerFullyFrozenButNeverLowersVanillasCount() {
        assertEquals(140, ColdFreeze.frozenTicks(0, 140));
        assertEquals(140, ColdFreeze.frozenTicks(138, 140));
        assertEquals(141, ColdFreeze.frozenTicks(141, 140));
    }

    private static void assertResult(float rate, ColdExposure exposure, Result result) {
        assertEquals(rate, result.rate(), 1.0E-6f);
        assertSame(exposure, result.exposure());
    }
}
