package nl.timtoys.corruptedadventure.weather;

import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandstormWindTest {
    private static RingWeatherState storm(int seed) {
        return RingWeatherState.start(RingWeatherType.SANDSTORM, 6000, seed);
    }

    @Test
    void directionIsAHorizontalUnitVectorFixedPerSeed() {
        for (int seed = -500; seed < 500; seed++) {
            Vec3 direction = SandstormWind.direction(storm(seed));
            assertEquals(1.0, direction.length(), 1.0E-9);
            assertEquals(0.0, direction.y);
            assertEquals(direction, SandstormWind.direction(storm(seed)));
            int degrees = SandstormWind.fromDegrees(storm(seed));
            assertTrue(degrees >= 0 && degrees < 360);
        }
    }

    @Test
    void consecutiveSeedsCoverAllQuadrants() {
        Set<Integer> quadrants = new HashSet<>();
        int[] perQuadrant = new int[4];
        for (int seed = 0; seed < 1000; seed++) {
            int quadrant = SandstormWind.fromDegrees(storm(seed)) / 90;
            quadrants.add(quadrant);
            perQuadrant[quadrant]++;
        }
        assertEquals(4, quadrants.size());
        for (int count : perQuadrant) {
            assertTrue(count > 150, "quadrants should be roughly even: " + count);
        }
    }

    @Test
    void directionBlowsAwayFromWhereTheWindComesFrom() {
        assertVector(0, 0, 1, SandstormWind.direction(0));     // from north → south (+Z)
        assertVector(-1, 0, 0, SandstormWind.direction(90));   // from east → west (−X)
        assertVector(0, 0, -1, SandstormWind.direction(180));  // from south → north
        assertVector(1, 0, 0, SandstormWind.direction(270));   // from west → east
    }

    @Test
    void describeNamesTheCompassPoint() {
        for (int seed = 0; seed < 200; seed++) {
            RingWeatherState state = storm(seed);
            String text = SandstormWind.describe(state);
            assertTrue(text.startsWith(SandstormWind.fromDegrees(state) + "° (from "), text);
        }
    }

    @Test
    void impulseScalesWithStrengthIntensityAndExposure() {
        Vec3 direction = SandstormWind.direction(225);
        assertEquals(0.0, SandstormWind.impulse(direction, 0.08, 0.0F, 1.0F).length());
        assertEquals(0.0, SandstormWind.impulse(direction, 0.08, 1.0F, 0.0F).length());
        Vec3 full = SandstormWind.impulse(direction, 0.08, 1.0F, 1.0F);
        assertEquals(0.08, full.length(), 1.0E-9);
        Vec3 half = SandstormWind.impulse(direction, 0.08, 0.5F, 1.0F);
        assertEquals(0.04, half.length(), 1.0E-9);
        assertEquals(0.16, SandstormWind.impulse(direction, 0.16, 1.0F, 1.0F).length(), 1.0E-9);
        assertEquals(0.0, full.normalize().subtract(direction).length(), 1.0E-9);
    }

    @Test
    void onlyCoreRing2IsPushed() {
        assertTrue(SandstormWind.pushes(RingPosition.within(Ring.RING_2), false, false));
        assertFalse(SandstormWind.pushes(RingPosition.within(Ring.RING_1), false, false));
        assertFalse(SandstormWind.pushes(RingPosition.within(Ring.RING_3), false, false));
        assertFalse(SandstormWind.pushes(RingPosition.within(Ring.VOID), false, false));
        // Boundary 1 (ring 1 → 2) and boundary 2 (ring 2 → 3): hard switch at 0.5.
        assertFalse(SandstormWind.pushes(new RingPosition(Ring.RING_1, Ring.RING_2, 0.49), false, false));
        assertTrue(SandstormWind.pushes(new RingPosition(Ring.RING_1, Ring.RING_2, 0.51), false, false));
        assertTrue(SandstormWind.pushes(new RingPosition(Ring.RING_2, Ring.RING_3, 0.49), false, false));
        assertFalse(SandstormWind.pushes(new RingPosition(Ring.RING_2, Ring.RING_3, 0.51), false, false));
    }

    @Test
    void creativeSpectatorAndPassengersAreNotPushed() {
        RingPosition ring2 = RingPosition.within(Ring.RING_2);
        assertFalse(SandstormWind.pushes(ring2, true, false));
        assertFalse(SandstormWind.pushes(ring2, false, true));
        assertFalse(SandstormWind.pushes(ring2, true, true));
    }

    private static void assertVector(double x, double y, double z, Vec3 actual) {
        assertEquals(x, actual.x, 1.0E-9);
        assertEquals(y, actual.y, 1.0E-9);
        assertEquals(z, actual.z, 1.0E-9);
    }
}
