package nl.timtoys.corruptedadventure.weather;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void aSpotIsKeptWithChanceOneOverTheDiscsCoveringIt() {
        List<Vec3> two = List.of(new Vec3(0.0, 64.0, 0.0), new Vec3(30.0, 64.0, 0.0));
        assertEquals(2, LavaBombs.covering(15.0, 0.0, 32.0, two));
        assertEquals(1, LavaBombs.covering(-20.0, 0.0, 32.0, two));
        assertEquals(0, LavaBombs.covering(100.0, 0.0, 32.0, two));
        assertTrue(LavaBombs.kept(1, 0.99));
        assertTrue(LavaBombs.kept(2, 0.49));
        assertFalse(LavaBombs.kept(2, 0.5));
        assertTrue(LavaBombs.kept(0, 0.99));
    }

    @Test
    void theBombsPerAreaStayThoseOfOnePlayerWhereverDiscsOverlap() {
        double radius = 32.0;
        List<Vec3> players = List.of(new Vec3(0.0, 64.0, 0.0), new Vec3(radius, 64.0, 0.0), new Vec3(16.0, 64.0, 24.0));
        Random random = new Random(20260930L);
        int perPlayer = 400_000;
        int inOverlap = 0;
        int inSingle = 0;
        // Two probe squares of 8 x 8: one covered by all three discs, one by the first player only.
        for (Vec3 player : players) {
            for (int i = 0; i < perPlayer; i++) {
                double distance = radius * Math.sqrt(random.nextDouble());
                double angle = random.nextDouble() * 2.0 * Math.PI;
                double x = player.x + Math.cos(angle) * distance;
                double z = player.z + Math.sin(angle) * distance;
                if (!LavaBombs.kept(LavaBombs.covering(x, z, radius, players), random.nextDouble())) {
                    continue;
                }
                if (x >= 12.0 && x < 20.0 && z >= 4.0 && z < 12.0) {
                    inOverlap++;
                } else if (x >= -28.0 && x < -20.0 && z >= -4.0 && z < 4.0) {
                    inSingle++;
                }
            }
        }
        assertEquals(3, LavaBombs.covering(16.0, 8.0, radius, players));
        assertEquals(1, LavaBombs.covering(-24.0, 0.0, radius, players));
        // One player's density: perPlayer / (pi r^2) per block, times 64 blocks per square (~7.960).
        double expected = perPlayer / (Math.PI * radius * radius) * 64.0;
        assertEquals(expected, inOverlap, expected * 0.05, "overlap of three discs");
        assertEquals(expected, inSingle, expected * 0.05, "one disc only");
    }

    @Test
    void bombsExpireAfterTenSeconds() {
        assertFalse(LavaBombs.expired(1000L, 1000L));
        assertFalse(LavaBombs.expired(1000L, 1000L + LavaBombs.MAX_AGE_TICKS - 1));
        assertTrue(LavaBombs.expired(1000L, 1000L + LavaBombs.MAX_AGE_TICKS));
        assertEquals(200, LavaBombs.MAX_AGE_TICKS);
    }
}
