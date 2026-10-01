package nl.timtoys.corruptedadventure.ring;

import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.RingSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingLayoutTest {
    private static final List<Integer> RADII = List.of(1500, 3500, 5500, 7500, 9500);
    private static final RingSettings DEFAULTS = new RingSettings(RADII, 10000, 96, 128);
    private static final long SEED = 12345L;
    // Furthest a boundary effect can reach: noise plus half the transition zone.
    private static final int REACH = 96 + 64;
    private static final double MAX_DIP = 0.02;

    private final RingLayout layout = new RingLayout(DEFAULTS, SEED);

    @Test
    void centreIsRingOne() {
        assertEquals(RingPosition.within(Ring.RING_1), layout.sampleOverworld(0, 0));
    }

    @Test
    void positionsClearOfBoundariesAreFullyInsideTheirRing() {
        for (int angle = 0; angle < 360; angle += 5) {
            for (int i = 0; i < RADII.size(); i++) {
                int radius = RADII.get(i);
                assertEquals(RingPosition.within(Ring.byIndex(i)), atAngle(layout, radius - REACH - 1, angle));
                assertEquals(RingPosition.within(Ring.byIndex(i + 1)), atAngle(layout, radius + REACH + 1, angle));
            }
            assertEquals(RingPosition.within(Ring.VOID), atAngle(layout, 100_000, angle));
        }
    }

    @Test
    void everyBoundaryHasATransitionBetweenNeighbouringRings() {
        for (int angle = 0; angle < 360; angle += 5) {
            for (int i = 0; i < RADII.size(); i++) {
                boolean sawTransition = false;
                for (int distance = RADII.get(i) - REACH; distance <= RADII.get(i) + REACH; distance++) {
                    RingPosition position = atAngle(layout, distance, angle);
                    if (position.inTransition()) {
                        sawTransition = true;
                        assertEquals(Ring.byIndex(i), position.inner());
                        assertEquals(Ring.byIndex(i + 1), position.outer());
                        assertTrue(position.blend() >= 0 && position.blend() < 1, "blend " + position.blend());
                    }
                }
                assertTrue(sawTransition, "no transition at ring " + (i + 1) + ", angle " + angle);
            }
        }
    }

    @Test
    void boundaryNoiseMovesBoundariesWithinLimit() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int angle = 0; angle < 360; angle++) {
            double boundary = boundaryDistance(layout, 3500, angle);
            min = Math.min(min, boundary);
            max = Math.max(max, boundary);
        }
        assertTrue(min >= 3500 - 96 - 1 && max <= 3500 + 96 + 1, "boundary between " + min + " and " + max);
        assertTrue(max - min > 48, "boundary is (nearly) a circle: " + min + " to " + max);
    }

    @Test
    void withoutNoiseAndTransitionBoundariesAreExactCircles() {
        RingLayout exact = new RingLayout(new RingSettings(RADII, 10000, 0, 0), SEED);
        for (int angle = 0; angle < 360; angle += 15) {
            assertEquals(Ring.RING_1, atAngle(exact, 1499.9, angle).ring());
            assertEquals(Ring.RING_2, atAngle(exact, 1500.1, angle).ring());
            assertEquals(Ring.RING_5, atAngle(exact, 9499.9, angle).ring());
            assertEquals(Ring.VOID, atAngle(exact, 9500.1, angle).ring());
        }
    }

    @Test
    void netherUsesOverworldLayoutAtQuarterScale() {
        for (int x = -3000; x <= 3000; x += 37) {
            for (int z = -3000; z <= 3000; z += 41) {
                assertEquals(layout.sampleOverworld(x * 4.0, z * 4.0), layout.sample(Level.NETHER, x, z));
                assertEquals(layout.sampleOverworld(x, z), layout.sample(Level.OVERWORLD, x, z));
            }
        }
        // Ring 1 ends near 1500 / 4 = 375 Nether blocks.
        assertEquals(Ring.RING_1, layout.sample(Level.NETHER, 375 - REACH / 4.0 - 1, 0).ring());
        assertEquals(Ring.RING_2, layout.sample(Level.NETHER, 375 + REACH / 4.0 + 1, 0).ring());
    }

    @Test
    void sameSeedGivesSameLayoutAndOtherSeedDiffers() {
        RingLayout same = new RingLayout(DEFAULTS, SEED);
        RingLayout other = new RingLayout(DEFAULTS, SEED + 1);
        boolean differs = false;
        for (int angle = 0; angle < 360; angle += 3) {
            assertEquals(boundaryDistance(layout, 5500, angle), boundaryDistance(same, 5500, angle));
            differs |= boundaryDistance(layout, 5500, angle) != boundaryDistance(other, 5500, angle);
        }
        assertTrue(differs, "different seeds produced identical boundaries");
        assertNotEquals(layout.sampleOverworld(3480, 777), other.sampleOverworld(3480, 777));
    }

    @Test
    void voidEdgeProgressStartsAtTheNoisyBoundaryAndEndsAtTheBorder() {
        for (int angle = 0; angle < 360; angle += 5) {
            double radians = Math.toRadians(angle);
            double previous = 0.0;
            for (int distance = 9500 - REACH; distance <= 10_050; distance += 5) {
                double x = Math.cos(radians) * distance;
                double z = Math.sin(radians) * distance;
                double progress = layout.voidEdgeProgress(x, z);
                assertEquals(layout.effectiveDistance(x, z) > 9500, progress > 0, "at " + distance + ", angle " + angle);
                assertTrue(progress >= previous, "progress drops at " + distance + ", angle " + angle);
                if (distance >= 10_000) {
                    assertEquals(1.0, progress, 1e-9);
                }
                previous = progress;
            }
        }
    }

    @Test
    void ringProgressRunsFromTheCentreToTheNoisyBoundaryOfRingOne() {
        assertEquals(0.0, layout.ringProgress(Ring.RING_1, 0, 0).orElseThrow(), 1e-9);
        for (int angle = 0; angle < 360; angle += 5) {
            double boundary = boundaryDistance(layout, RADII.getFirst(), angle);
            double previous = 0.0;
            for (double distance = 0; distance < boundary - 1; distance += 8) {
                double progress = progressAt(layout, Ring.RING_1, distance, angle).orElseThrow();
                // The boundary noise can be locally steeper than the ray, so allow small dips.
                assertTrue(progress > previous - MAX_DIP && progress < 1.0, "progress " + progress + " at " + distance + ", angle " + angle);
                previous = Math.max(previous, progress);
            }
            assertEquals(1.0, progressAt(layout, Ring.RING_1, boundary - 0.5, angle).orElseThrow(), 0.01, "angle " + angle);
            assertTrue(progressAt(layout, Ring.RING_1, boundary + 0.5, angle).isEmpty(), "angle " + angle);
        }
    }

    @Test
    void ringProgressOfAnOuterRingStartsAtItsInnerBoundaryAndMatchesItsRing() {
        for (int angle = 0; angle < 360; angle += 5) {
            double inner = boundaryDistance(layout, RADII.get(0), angle);
            double outer = boundaryDistance(layout, RADII.get(1), angle);
            assertEquals(0.0, progressAt(layout, Ring.RING_2, inner + 0.5, angle).orElseThrow(), 0.01, "angle " + angle);
            assertEquals(1.0, progressAt(layout, Ring.RING_2, outer - 0.5, angle).orElseThrow(), 0.01, "angle " + angle);
            for (double distance = 0; distance < RADII.get(2); distance += 50) {
                boolean inRing = atAngle(layout, distance, angle).ring() == Ring.RING_2;
                assertEquals(inRing, progressAt(layout, Ring.RING_2, distance, angle).isPresent(), distance + ", angle " + angle);
            }
        }
    }

    @Test
    void ringNumbering() {
        assertEquals(1, Ring.RING_1.number());
        assertEquals(6, Ring.VOID.number());
        assertEquals(Ring.RING_3, Ring.byIndex(2));
        assertFalse(RingPosition.within(Ring.RING_2).inTransition());
    }

    private static OptionalDouble progressAt(RingLayout layout, Ring ring, double distance, int angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        return layout.ringProgress(ring, Math.cos(radians) * distance, Math.sin(radians) * distance);
    }

    private static RingPosition atAngle(RingLayout layout, double distance, int angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        return layout.sampleOverworld(Math.cos(radians) * distance, Math.sin(radians) * distance);
    }

    /** Distance along the ray where the position switches to the outer ring of the boundary at {@code radius}. */
    private static double boundaryDistance(RingLayout layout, int radius, int angleDegrees) {
        Ring inner = atAngle(layout, radius - REACH - 1, angleDegrees).ring();
        for (double distance = radius - REACH; distance <= radius + REACH; distance += 0.5) {
            if (atAngle(layout, distance, angleDegrees).ring() != inner) {
                return distance;
            }
        }
        throw new AssertionError("no boundary near " + radius + " at angle " + angleDegrees);
    }
}
