package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The silence thresholds (package 97, V1-A/V4-B): 30 and 60 are client cues only; from 90 darkness and a heartbeat that
 * hurts down to 1 health on every difficulty and never kills.
 */
class SilenceThresholdsTest {
    @Test
    void darknessAndTheHeartbeatOnlyFromTheThirdThreshold() {
        assertFalse(SilenceHeartbeat.wanted(ConditionMeters.UNKNOWN), "after loading, before the first tick");
        assertFalse(SilenceHeartbeat.wanted(0));
        assertFalse(SilenceHeartbeat.wanted(1), "30: muffled sound, client only");
        assertFalse(SilenceHeartbeat.wanted(2), "60: HUD hidden, client only");
        assertTrue(SilenceHeartbeat.wanted(3));
    }

    @Test
    void theFirstTwoThresholdsAreClientCues() {
        assertEquals(2, SilenceCue.values().length);
    }

    @Test
    void theHeartbeatNeverKillsOnAnyDifficulty() {
        for (Difficulty difficulty : Difficulty.values()) {
            assertEquals(1.0f, SilenceHeartbeat.FLOOR.damage(1.0f, 20, difficulty), difficulty.getKey());
            assertEquals(0.5f, SilenceHeartbeat.FLOOR.damage(1.0f, 1.5f, difficulty), difficulty.getKey());
            assertEquals(0.0f, SilenceHeartbeat.FLOOR.damage(1.0f, 1.0f, difficulty), "stops at 1 health");
        }
    }

    @Test
    void ourDarknessKeepsItsFlags() {
        assertTrue(ConditionDarkness.isOurs(ConditionDarkness.ours(0)));
        assertEquals(-1, ConditionDarkness.NONE);
    }

    @Test
    void voidAndSilenceShareOneDarknessTheStrongestWins() {
        assertEquals(ConditionDarkness.NONE, ConditionDarkness.combine(ConditionDarkness.NONE, ConditionDarkness.NONE));
        assertEquals(0, ConditionDarkness.combine(0, ConditionDarkness.NONE), "only the void");
        assertEquals(0, ConditionDarkness.combine(ConditionDarkness.NONE, 0), "only the silence");
        assertEquals(2, ConditionDarkness.combine(1, 2));
        assertEquals(2, ConditionDarkness.combine(2, 0));
    }

    @Test
    void oneConditionDroppingItKeepsTheDarknessTheOtherWants() {
        // The silence stops wanting it while the void still does: refreshed, never removed and re-added.
        int wanted = ConditionDarkness.combine(0, ConditionDarkness.NONE);
        assertSame(ConditionDarkness.Action.REFRESH, ConditionDarkness.decide(ConditionDarkness.ours(0), wanted));
        // Neither wants it any more: ours goes.
        assertSame(ConditionDarkness.Action.REMOVE,
                ConditionDarkness.decide(ConditionDarkness.ours(0), ConditionDarkness.NONE));
        assertSame(ConditionDarkness.Action.NOTHING, ConditionDarkness.decide(null, ConditionDarkness.NONE));
        assertSame(ConditionDarkness.Action.REFRESH, ConditionDarkness.decide(null, 0));
    }

    @Test
    void aWeakerWishLowersOurDarkness() {
        // addEffect would keep the stronger instance, so lowering replaces it.
        assertSame(ConditionDarkness.Action.LOWER, ConditionDarkness.decide(ConditionDarkness.ours(2), 0));
        assertSame(ConditionDarkness.Action.REFRESH, ConditionDarkness.decide(ConditionDarkness.ours(0), 2));
    }

    @Test
    void aForeignDarknessIsNeverRefreshedOrRemoved() {
        MobEffectInstance warden = new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false);
        assertSame(ConditionDarkness.Action.NOTHING, ConditionDarkness.decide(warden, 0));
        assertSame(ConditionDarkness.Action.NOTHING, ConditionDarkness.decide(warden, ConditionDarkness.NONE));
        MobEffectInstance merged = ConditionDarkness.ours(0);
        merged.update(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
        assertSame(ConditionDarkness.Action.NOTHING, ConditionDarkness.decide(merged, ConditionDarkness.NONE));
    }
}
