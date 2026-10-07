package nl.timtoys.corruptedadventure.weather;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LavaBombsTest {
    private static final double EPSILON = 1.0E-9;

    @Test
    void rateFollowsFadeAndPeak() {
        assertEquals(0.2, LavaBombs.perSecond(12.0, 1.0F, false, 2.0), EPSILON);
        assertEquals(0.4, LavaBombs.perSecond(12.0, 1.0F, true, 2.0), EPSILON);
        assertEquals(0.1, LavaBombs.perSecond(12.0, 0.5F, false, 2.0), EPSILON);
        assertEquals(0.0, LavaBombs.perSecond(12.0, 0.0F, true, 2.0), EPSILON);
        assertEquals(0.0, LavaBombs.perSecond(0.0, 1.0F, true, 4.0), EPSILON);
    }

    @Test
    void accumulatorDropsExactlyTheRatePerMinute() {
        double perSecond = LavaBombs.perSecond(12.0, 1.0F, true, 2.0);
        double carry = 0.0;
        int total = 0;
        for (int second = 0; second < 600; second++) {
            LavaBombs.Release release = LavaBombs.release(carry, perSecond);
            assertTrue(release.carry() >= 0.0 && release.carry() < 1.0);
            total += release.count();
            carry = release.carry();
        }
        // 10 minutes at 24 a minute (12 × peak factor 2): the cap is never exceeded.
        assertTrue(total >= 239 && total <= 240, "bombs: " + total);
    }

    @Test
    void severalBombsInOneSecondAtHighRates() {
        LavaBombs.Release release = LavaBombs.release(0.5, LavaBombs.perSecond(60.0, 1.0F, true, 4.0));
        assertEquals(4, release.count());
        assertEquals(0.5, release.carry(), EPSILON);
    }
}
