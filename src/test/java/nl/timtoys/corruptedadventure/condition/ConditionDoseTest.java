package nl.timtoys.corruptedadventure.condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionDoseTest {
    @Test
    void aLoweringDoseAlwaysWorksARaisingOneOnlyForExposedPlayers() {
        assertEquals(-25.0F, ConditionDose.meterChange(-25.0F, true));
        assertEquals(-25.0F, ConditionDose.meterChange(-25.0F, false), "creative may drink");
        assertEquals(8.0F, ConditionDose.meterChange(8.0F, true));
        assertEquals(0.0F, ConditionDose.meterChange(8.0F, false), "in creative a meter never rises");
    }

    @Test
    void aDoseWithoutEffectHasNone() {
        assertFalse(ConditionDose.of(Condition.DEHYDRATION, 8.0F).effect().isPresent());
        ConditionDose sip = ConditionDose.withEffect(Condition.DEHYDRATION, -25.0F, RateEffect.COOLED, 45);
        assertTrue(sip.effect().isPresent());
        assertEquals(45, sip.effectSeconds());
        assertTrue(RateEffect.COOLED.slows(sip.condition()));
    }
}
