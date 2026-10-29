package nl.timtoys.corruptedadventure.entity.lostkeeper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The course of The Lost Keeper's omens and of its wisps (pure arithmetic). */
class KeeperArrivalTest {
    @Test
    void theMistRollsInOverTheFirstHalfAndStaysFull() {
        assertEquals(0.0F, KeeperArrival.mistRoll(0.0F), 1.0E-6);
        assertEquals(0.5F, KeeperArrival.mistRoll(KeeperArrival.MIST_ROLL / 2.0F), 1.0E-6);
        assertEquals(1.0F, KeeperArrival.mistRoll(KeeperArrival.MIST_ROLL), 1.0E-6);
        assertEquals(1.0F, KeeperArrival.mistRoll(0.9F), 1.0E-6);
        float last = -1.0F;
        for (int k = 0; k <= 20; k++) {
            float roll = KeeperArrival.mistRoll(k / 20.0F);
            assertTrue(roll >= last, "it never thins while rolling in");
            last = roll;
        }
    }

    @Test
    void theGlowIsSeenAfterTheMistAndComesCloserUntilItHasCome() {
        assertTrue(KeeperArrival.glowShare(KeeperArrival.GLOW_START - 0.01F, true) < 0.0F, "not yet seen");
        assertEquals(0.0F, KeeperArrival.glowShare(KeeperArrival.GLOW_START, false), 1.0E-6);
        assertEquals(1.0F, KeeperArrival.glowShare(KeeperArrival.GLOW_END_WATER, true), 1.0E-6);
        assertTrue(KeeperArrival.glowShare(KeeperArrival.GLOW_END_WATER, false) < 1.0F, "in the air it comes until the end");
        assertEquals(1.0F, KeeperArrival.glowShare(1.0F, false), 1.0E-6);
        float last = 0.0F;
        for (int k = 0; k <= 20; k++) {
            float progress = KeeperArrival.GLOW_START + k * (1.0F - KeeperArrival.GLOW_START) / 20.0F;
            float share = KeeperArrival.glowShare(progress, false);
            assertTrue(share >= last, "it only comes closer");
            last = share;
        }
    }

    @Test
    void aWispRisesHangsAndLiftsToNothing() {
        int life = 3600;
        assertEquals(0.0F, KeeperMistTraces.strength(0, life), 1.0E-6);
        assertEquals(1.0F, KeeperMistTraces.strength(KeeperMistTraces.RISE_TICKS, life), 1.0E-6);
        assertEquals(1.0F, KeeperMistTraces.strength(life / 2, life), 1.0E-6);
        assertEquals(0.5F, KeeperMistTraces.strength(life - KeeperMistTraces.LIFT_TICKS / 2, life), 1.0E-6);
        assertEquals(0.0F, KeeperMistTraces.strength(life, life), 1.0E-6);
        // A short life still rises and lifts, never above 1.
        int shortLife = 200;
        for (int age = 0; age < shortLife; age++) {
            float strength = KeeperMistTraces.strength(age, shortLife);
            assertTrue(strength >= 0.0F && strength <= 1.0F, "0–1 at " + age);
        }
    }
}
