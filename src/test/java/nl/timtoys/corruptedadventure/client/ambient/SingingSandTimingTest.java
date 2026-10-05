package nl.timtoys.corruptedadventure.client.ambient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingingSandTimingTest {
    @Test
    void chanceRunsFromTheDayValueToTheNightValue() {
        assertEquals(SingingSandTiming.DAY_CHANCE, SingingSandTiming.chancePerTick(0.0F));
        assertEquals(SingingSandTiming.NIGHT_CHANCE, SingingSandTiming.chancePerTick(1.0F));
        assertEquals(1.0F / 800.0F, SingingSandTiming.NIGHT_CHANCE);
        assertEquals(1.0F / 4800.0F, SingingSandTiming.DAY_CHANCE);
    }

    @Test
    void chanceRisesWithNightAndStaysAProbability() {
        float previous = -1.0F;
        for (float factor = -0.5F; factor <= 1.5F; factor += 0.01F) {
            float chance = SingingSandTiming.chancePerTick(factor);
            assertTrue(chance >= previous, "monotonic at " + factor);
            assertTrue(chance >= 0.0F && chance <= 1.0F);
            previous = chance;
        }
    }

    @Test
    void nightFactorIsDayAtNoonNightAtMidnightAndSoftAtDusk() {
        assertEquals(0.0F, SingingSandTiming.nightFactor(0.0F));
        assertEquals(1.0F, SingingSandTiming.nightFactor(0.5F));
        assertEquals(0.0F, SingingSandTiming.nightFactor(1.0F), 1.0e-6F);
        float sunset = SingingSandTiming.nightFactor(0.25F);
        assertTrue(sunset > 0.0F && sunset < 1.0F, "half way at the horizon: " + sunset);
        // Dusk and dawn mirror each other, and the factor never leaves 0..1.
        for (float time = 0.0F; time <= 1.0F; time += 0.005F) {
            float factor = SingingSandTiming.nightFactor(time);
            assertTrue(factor >= 0.0F && factor <= 1.0F);
            assertEquals(factor, SingingSandTiming.nightFactor(1.0F - time), 1.0e-5F);
        }
    }
}
