package nl.timtoys.corruptedadventure.condition;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateEffectTimersTest {
    private static final float HALF = 0.5F;

    private static float modify(RateEffectTimers timers, Condition condition, float rate, long now) {
        return timers.modify(condition, rate, now, effect -> HALF);
    }

    @Test
    void anEffectRunsForItsTicksAndThenEnds() {
        RateEffectTimers timers = new RateEffectTimers();
        assertFalse(timers.isActive(RateEffect.COOLED, 0));
        timers.start(RateEffect.COOLED, 1000, 900);
        assertTrue(timers.isActive(RateEffect.COOLED, 1000));
        assertEquals(900, timers.remainingTicks(RateEffect.COOLED, 1000));
        assertTrue(timers.isActive(RateEffect.COOLED, 1899));
        assertFalse(timers.isActive(RateEffect.COOLED, 1900));
        assertEquals(0, timers.remainingTicks(RateEffect.COOLED, 5000));
    }

    @Test
    void theSameKindDoesNotStackTheLaterEndWins() {
        RateEffectTimers timers = new RateEffectTimers();
        timers.start(RateEffect.COOLED, 0, 900);   // canteen, 45 s
        timers.start(RateEffect.COOLED, 100, 400); // cactus juice, 20 s, while 40 s are left
        assertEquals(800, timers.remainingTicks(RateEffect.COOLED, 100), "a shorter dose does not cut it short");
        timers.start(RateEffect.COOLED, 600, 900); // a new sip sets the time again, it does not add
        assertEquals(900, timers.remainingTicks(RateEffect.COOLED, 600));
        assertEquals(HALF, modify(timers, Condition.DEHYDRATION, 1.0F, 600), "one factor, not two");
    }

    @Test
    void onlyARisingRateOfItsOwnConditionIsMultiplied() {
        RateEffectTimers timers = new RateEffectTimers();
        timers.start(RateEffect.COOLED, 0, 900);
        assertEquals(0.2F, modify(timers, Condition.DEHYDRATION, 0.4F, 10), 1.0E-6F);
        assertEquals(-0.5F, modify(timers, Condition.DEHYDRATION, -0.5F, 10), "falling stays");
        assertEquals(0.0F, modify(timers, Condition.DEHYDRATION, 0.0F, 10));
        assertEquals(0.55F, modify(timers, Condition.HEAT, 0.55F, 10), "another condition");
        assertEquals(0.4F, modify(timers, Condition.DEHYDRATION, 0.4F, 900), "expired");
    }

    @Test
    void timersSurviveSavingAndUnknownNamesAreDropped() {
        RateEffectTimers timers = new RateEffectTimers();
        timers.start(RateEffect.COOLED, 50, 400);
        var json = RateEffectTimers.CODEC.encodeStart(JsonOps.INSTANCE, timers).getOrThrow().getAsJsonObject();
        json.addProperty("renamed_effect", 999L);
        RateEffectTimers loaded = RateEffectTimers.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(400, loaded.remainingTicks(RateEffect.COOLED, 50));
        assertTrue(RateEffectTimers.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).isSuccess());
    }
}
