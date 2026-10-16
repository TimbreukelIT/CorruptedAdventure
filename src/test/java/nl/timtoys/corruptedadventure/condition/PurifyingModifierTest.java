package nl.timtoys.corruptedadventure.condition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Purifying in Nether ring 4 (package 79): every falling meter falls faster, a rising one is left alone. */
class PurifyingModifierTest {
    private static final float FACTOR = 3.0F;
    private static final BooleanSupplier PURIFYING = () -> true;
    private static final BooleanSupplier ELSEWHERE = () -> false;
    private static final BooleanSupplier NEVER = () -> {
        throw new AssertionError("biome asked for a rate that does not fall");
    };

    @Test
    void aFallingMeterFallsThreeTimesAsFast() {
        // Poison away from every source (-1/s) and heat or cold outside their zone (-0.5/s).
        assertEquals(-3.0F, PurifyingModifier.purified(-1.0F, PURIFYING, FACTOR), 1.0E-6F);
        assertEquals(-1.5F, PurifyingModifier.purified(-0.5F, PURIFYING, FACTOR), 1.0E-6F);
        float seconds = 100 / -PurifyingModifier.purified(-1.0F, PURIFYING, FACTOR);
        assertEquals(33.3F, seconds, 0.1F, "a full poison meter clears in ~33 s instead of 100 s");
    }

    @Test
    void outsideAPurifyingBiomeNothingChanges() {
        assertEquals(-1.0F, PurifyingModifier.purified(-1.0F, ELSEWHERE, FACTOR));
    }

    @Test
    void aRisingOrStillMeterIsLeftAloneWithoutAskingTheBiome() {
        assertEquals(0.42F, PurifyingModifier.purified(0.42F, NEVER, FACTOR));
        assertEquals(0.0F, PurifyingModifier.purified(0.0F, NEVER, FACTOR));
    }

    @Test
    void factorOneTurnsItOff() {
        assertEquals(-1.0F, PurifyingModifier.purified(-1.0F, PURIFYING, 1.0F));
    }

    @Test
    void purifyingRunsLastOnTheModifiedRate() {
        // Registered first, still after a weather modifier that turned a fall into a slower fall.
        RateModifierPipeline pipeline = new RateModifierPipeline();
        pipeline.add(RateModifier.Stage.PURIFYING,
                (condition, context, rate) -> PurifyingModifier.purified(rate, PURIFYING, FACTOR));
        pipeline.add(RateModifier.Stage.WEATHER, (condition, context, rate) -> rate + 0.5F);
        List<RateModifier> ordered = pipeline.ordered();
        assertEquals(-1.5F, ConditionTicker.rate(Condition.DEBUG, null, -1.0F, ordered, true), 1.0E-6F);
    }
}
