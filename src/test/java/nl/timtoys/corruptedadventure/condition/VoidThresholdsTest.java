package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The void thresholds (package 91): inner band at 60 teleports without darkness, outer band at 60 darkness without
 * teleports, at 90 darkness everywhere (inner band: teleports too); every 20 s, only on the ground; a foreign darkness
 * is left alone.
 */
class VoidThresholdsTest {
    private static final int INTERVAL = 20 * 20;

    @Test
    void theSecondThresholdTeleportsInsideAndDarkensOutside() {
        assertTrue(VoidSlip.teleports(VoidZone.INNER));
        assertFalse(VoidDarkness.wanted(2, VoidZone.INNER), "no darkness at 60 in the inner band");
        assertFalse(VoidSlip.teleports(VoidZone.OUTER));
        assertTrue(VoidDarkness.wanted(2, VoidZone.OUTER), "darkness at 60 in the outer band and on the rim");
        // Outside the zone: neither (the meter falls there).
        assertFalse(VoidSlip.teleports(VoidZone.OUTSIDE));
        assertFalse(VoidDarkness.wanted(2, VoidZone.OUTSIDE));
    }

    @Test
    void theThirdThresholdDarkensEverywhereAndTheTeleportsGoOn() {
        for (VoidZone zone : VoidZone.values()) {
            assertTrue(VoidDarkness.wanted(3, zone), "darkness at 90 in " + zone);
        }
        // The second threshold is still reached at 90 (cumulative), so it keeps teleporting in the inner band.
        assertTrue(VoidSlip.teleports(VoidZone.INNER));
    }

    @Test
    void belowTheSecondThresholdNoDarkness() {
        for (VoidZone zone : VoidZone.values()) {
            assertFalse(VoidDarkness.wanted(0, zone));
            assertFalse(VoidDarkness.wanted(1, zone));
        }
        // After loading, before the first tick: unknown counts as nothing reached.
        assertFalse(VoidDarkness.wanted(ConditionMeters.UNKNOWN, VoidZone.OUTER));
    }

    @Test
    void aTeleportEveryIntervalStartingFromTheFirstTick() {
        SlipSchedule<String> schedule = new SlipSchedule<>();
        assertFalse(schedule.isDue("a", 1000, INTERVAL), "the first tick starts the wait");
        assertFalse(schedule.isDue("a", 1000 + INTERVAL - 20, INTERVAL));
        assertTrue(schedule.isDue("a", 1000 + INTERVAL, INTERVAL));
        // Not on the ground: it stays due until the player lands.
        assertTrue(schedule.isDue("a", 1000 + INTERVAL + 60, INTERVAL));
        schedule.restart("a", 1000 + INTERVAL + 60);
        assertFalse(schedule.isDue("a", 1000 + 2 * INTERVAL, INTERVAL));
        assertTrue(schedule.isDue("a", 1000 + 2 * INTERVAL + 60, INTERVAL));
        // Each player has their own wait.
        assertFalse(schedule.isDue("b", 1000 + 2 * INTERVAL + 60, INTERVAL));
        // Leaving the inner band or dropping below the threshold: the wait starts over.
        schedule.forget("a");
        assertFalse(schedule.isDue("a", 5000, INTERVAL));
        assertTrue(schedule.isDue("a", 5000 + INTERVAL, INTERVAL));
    }

    @Test
    void aLiveIntervalChangeAppliesToARunningWait() {
        SlipSchedule<String> schedule = new SlipSchedule<>();
        assertFalse(schedule.isDue("a", 0, 600 * 20));
        assertFalse(schedule.isDue("a", 300, 600 * 20));
        assertTrue(schedule.isDue("a", 400, INTERVAL), "lowered to 20 s while waiting");
    }

    @Test
    void ourDarknessIsRecognisedAndAForeignOneMergedIntoItIsNot() {
        assertTrue(VoidDarkness.isOurs(VoidDarkness.ours(0)));
        assertFalse(VoidDarkness.isOurs(null));
        // A warden's darkness (260 ticks, not ambient, no particles, no icon) on top of ours.
        MobEffectInstance warden = VoidDarkness.ours(0);
        warden.update(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
        assertFalse(VoidDarkness.isOurs(warden));
        assertFalse(VoidDarkness.isOurs(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false)));
        // Our own refresh keeps it ours.
        MobEffectInstance refreshed = VoidDarkness.ours(0);
        refreshed.update(VoidDarkness.ours(0));
        assertTrue(VoidDarkness.isOurs(refreshed));
    }

    @Test
    void onlyOnTheGroundInSurvivalAndNeverRidingSleepingGlidingOrSwimming() {
        assertTrue(stance(true, true, false, false, false, false, false).mayTeleport());
        assertFalse(stance(false, true, false, false, false, false, false).mayTeleport(), "creative or spectator");
        assertFalse(stance(true, false, false, false, false, false, false).mayTeleport(), "in the air");
        assertFalse(stance(true, true, true, false, false, false, false).mayTeleport(), "riding");
        assertFalse(stance(true, true, false, true, false, false, false).mayTeleport(), "carrying a passenger");
        assertFalse(stance(true, true, false, false, true, false, false).mayTeleport(), "sleeping");
        assertFalse(stance(true, true, false, false, false, true, false).mayTeleport(), "gliding");
        assertFalse(stance(true, true, false, false, false, false, true).mayTeleport(), "in a fluid or powder snow");
    }

    private static VoidTeleport.Stance stance(boolean exposed, boolean onGround, boolean riding, boolean carrying,
                                              boolean sleeping, boolean fallFlying, boolean inFluid) {
        return new VoidTeleport.Stance(exposed, onGround, riding, carrying, sleeping, fallFlying, inFluid);
    }
}
