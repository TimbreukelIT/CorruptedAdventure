package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.timtoys.corruptedadventure.condition.PoisonRule.Input;
import nl.timtoys.corruptedadventure.condition.PoisonRule.Rates;
import nl.timtoys.corruptedadventure.condition.PoisonRule.Result;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoisonRuleTest {
    /** The defaults of {@code [conditions.poison]}, read from the spec (contract §5). */
    private static final Rates DEFAULTS = new Rates(value(ServerConfig.POISON.poolRise), value(ServerConfig.POISON.gasRise),
            value(ServerConfig.POISON.fall), value(ServerConfig.POISON.outsideFall));
    /** Gas stronger than the pool, and two different fall rates, so every label is decided by its own rate. */
    private static final Rates GAS_STRONGER = new Rates(2.0f, 3.0f, 1.0f, 0.5f);

    @Test
    void contractStartValues() {
        assertEquals(new Rates(3.0f, 2.0f, 1.0f, 1.0f), DEFAULTS);
        assertEquals(1, ServerConfig.POISON.poolRadius.getDefault());
        assertEquals(0.5f, value(ServerConfig.POISON.rainRise));
        assertEquals(40.0f, value(ServerConfig.POISON.milkAmount));
        assertEquals(20.0f, value(ServerConfig.POISON.honeyAmount));
    }

    @Test
    void aPoolRisesFast() {
        assertResult(3.0f, PoisonExposure.POOL, PoisonRule.evaluate(new Input(true, true, false), DEFAULTS));
        float seconds = 100 / PoisonRule.evaluate(new Input(true, true, false), DEFAULTS).rate();
        assertTrue(seconds > 30 && seconds < 40, "0 -> 100 in " + seconds + " s");
    }

    @Test
    void headInGasRises() {
        assertResult(2.0f, PoisonExposure.GAS, PoisonRule.evaluate(new Input(true, false, true), DEFAULTS));
        assertEquals(50.0f, 100 / PoisonRule.evaluate(new Input(true, false, true), DEFAULTS).rate(), 1.0E-4f);
    }

    @Test
    void theStrongerSourceWinsAndSourcesDoNotStack() {
        assertResult(3.0f, PoisonExposure.POOL, PoisonRule.evaluate(new Input(true, true, true), DEFAULTS));
        assertResult(3.0f, PoisonExposure.GAS, PoisonRule.evaluate(new Input(true, true, true), GAS_STRONGER));
        // Equal rates: the pool keeps the label.
        assertResult(2.0f, PoisonExposure.POOL,
                PoisonRule.evaluate(new Input(true, true, true), new Rates(2.0f, 2.0f, 1.0f, 1.0f)));
    }

    @Test
    void withoutASourceTheMeterFalls() {
        // Water and the blighted glowcaps are no source: in the zone without pool or gas the meter falls.
        assertResult(-1.0f, PoisonExposure.CLEAN, PoisonRule.evaluate(new Input(true, false, false), DEFAULTS));
        assertResult(-1.0f, PoisonExposure.CLEAN, PoisonRule.evaluate(new Input(true, false, false), GAS_STRONGER));
    }

    @Test
    void outsideTheZoneTheMeterFallsEvenAtASource() {
        assertResult(-0.5f, PoisonExposure.OUTSIDE, PoisonRule.evaluate(Input.OUTSIDE, GAS_STRONGER));
        assertResult(-0.5f, PoisonExposure.OUTSIDE, PoisonRule.evaluate(new Input(false, true, true), GAS_STRONGER));
    }

    @Test
    void aFallRateOfZeroIsPositiveZero() {
        Result result = PoisonRule.evaluate(new Input(true, false, false), new Rates(3.0f, 2.0f, 0.0f, 0.0f));
        assertEquals(0, Float.compare(0.0f, result.rate()));
    }

    @Test
    void theZoneIsOverworldRingFourSwitchingAtTheMiddle() {
        assertTrue(PoisonRule.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_4)));
        assertFalse(PoisonRule.inZone(Level.NETHER, RingPosition.within(Ring.RING_4)));
        assertFalse(PoisonRule.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_3)));
        assertFalse(PoisonRule.inZone(Level.OVERWORLD, RingPosition.within(Ring.RING_5)));
        assertFalse(PoisonRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_3, Ring.RING_4, 0.49)));
        assertTrue(PoisonRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_3, Ring.RING_4, 0.5)));
        assertTrue(PoisonRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_4, Ring.RING_5, 0.49)));
        assertFalse(PoisonRule.inZone(Level.OVERWORLD, new RingPosition(Ring.RING_4, Ring.RING_5, 0.5)));
    }

    private static void assertResult(float rate, PoisonExposure exposure, Result result) {
        assertEquals(rate, result.rate(), 1.0E-6f);
        assertEquals(exposure, result.exposure());
    }

    private static float value(ModConfigSpec.DoubleValue value) {
        return value.getDefault().floatValue();
    }
}
