package nl.timtoys.corruptedadventure.ring;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingLayoutTest {
    private static final List<Integer> RADII = CommonConfig.DEFAULT_OUTER_RADII;
    private static final RingSettings DEFAULTS = new RingSettings(RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
    private static final long SEED = 12345L;
    // Furthest a boundary effect can reach: noise plus half the transition zone (the widest one, 4|5, for the scans).
    private static final int REACH = 96 + 128;
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
                int reach = reach(DEFAULTS, i);
                assertEquals(RingPosition.within(Ring.byIndex(i)), atAngle(layout, radius - reach - 1, angle));
                assertEquals(RingPosition.within(Ring.byIndex(i + 1)), atAngle(layout, radius + reach + 1, angle));
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
            assertEquals(Ring.RING_5, atAngle(exact, RADII.getLast() - 0.1, angle).ring());
            assertEquals(Ring.VOID, atAngle(exact, RADII.getLast() + 0.1, angle).ring());
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
        assertEquals(Ring.RING_1, layout.sample(Level.NETHER, 375 - reach(DEFAULTS, 0) / 4.0 - 1, 0).ring());
        assertEquals(Ring.RING_2, layout.sample(Level.NETHER, 375 + reach(DEFAULTS, 0) / 4.0 + 1, 0).ring());
    }

    @Test
    void sameSeedGivesSameLayoutAndOtherSeedDiffers() {
        RingLayout same = new RingLayout(DEFAULTS, SEED);
        RingLayout other = new RingLayout(DEFAULTS, SEED + 1);
        boolean differs = false;
        for (int angle = 0; angle < 360; angle += 3) {
            assertEquals(boundaryDistance(layout, 6000, angle), boundaryDistance(same, 6000, angle));
            differs |= boundaryDistance(layout, 6000, angle) != boundaryDistance(other, 6000, angle);
        }
        assertTrue(differs, "different seeds produced identical boundaries");
        assertNotEquals(layout.sampleOverworld(3480, 777), other.sampleOverworld(3480, 777));
    }

    @Test
    void voidEdgeProgressStartsAtTheNoisyBoundaryAndEndsAtTheBorder() {
        for (int angle = 0; angle < 360; angle += 5) {
            double radians = Math.toRadians(angle);
            double previous = 0.0;
            for (int distance = 9700 - REACH; distance <= 10_250; distance += 5) {
                double x = Math.cos(radians) * distance;
                double z = Math.sin(radians) * distance;
                double progress = layout.voidEdgeProgress(x, z);
                assertEquals(layout.effectiveDistance(x, z) > 9700, progress > 0, "at " + distance + ", angle " + angle);
                // The boundary noise can be locally steeper than the ray, so allow small dips (as for ringProgress).
                assertTrue(progress > previous - MAX_DIP, "progress drops at " + distance + ", angle " + angle);
                if (distance >= 10_200) {
                    assertEquals(1.0, progress, 1e-9);
                }
                previous = Math.max(previous, progress);
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

    @Test
    void noiseSeedIsAStableOneWayHashOfTheWorldSeed() {
        for (long worldSeed : new long[]{0L, 1L, -1L, 20260930L, SEED, Long.MIN_VALUE}) {
            long noiseSeed = RingLayout.noiseSeed(worldSeed);
            assertEquals(noiseSeed, RingLayout.noiseSeed(worldSeed));
            assertNotEquals(worldSeed, noiseSeed);
            // Own salt: never the hashed seed vanilla already sends to clients.
            assertNotEquals(BiomeManager.obfuscateSeed(worldSeed), noiseSeed);
        }
        assertNotEquals(RingLayout.noiseSeed(20260930L), RingLayout.noiseSeed(20260931L));
    }

    @Test
    void settingsAndNoiseSeedAreKept() {
        assertEquals(DEFAULTS, layout.settings());
        assertEquals(SEED, layout.noiseSeed());
    }

    @Test
    void zoneFourFiveIs256WideInTheOverworldAndTheOthers128() {
        // Measured on the effective distance, where each zone is exactly its width wide.
        for (int boundary = 0; boundary < RADII.size(); boundary++) {
            int radius = RADII.get(boundary);
            int half = boundary == 3 ? 128 : 64;
            assertFalse(layout.positionAt(radius - half - 0.01).inTransition(), "boundary " + boundary);
            assertEquals(0.0, layout.positionAt(radius - half).blend(), 1e-9, "boundary " + boundary);
            assertEquals(0.5, layout.positionAt(radius).blend(), 1e-9, "boundary " + boundary);
            assertTrue(layout.positionAt(radius + half - 0.01).inTransition(), "boundary " + boundary);
            assertFalse(layout.positionAt(radius + half).inTransition(), "boundary " + boundary);
        }
        assertEquals(256, DEFAULTS.transitionWidth(3));
    }

    @Test
    void zoneFourFiveIs64WideInTheNether() {
        // Without boundary noise the effective distance is the plain radius, so the zone can be measured along a ray.
        RingLayout flat = new RingLayout(new RingSettings(RADII, 10200, 0, 128, DEFAULTS.transitionWidthOverrides()), SEED);
        double centre = 8000 / 4.0;
        assertFalse(flat.sample(Level.NETHER, centre - 32.01, 0).inTransition());
        assertTrue(flat.sample(Level.NETHER, centre - 32, 0).inTransition());
        assertTrue(flat.sample(Level.NETHER, centre + 31.99, 0).inTransition());
        assertFalse(flat.sample(Level.NETHER, centre + 32, 0).inTransition());
        // Boundary 3|4 stays 128 / 4 = 32 wide.
        double three = 6000 / 4.0;
        assertFalse(flat.sample(Level.NETHER, three - 16.01, 0).inTransition());
        assertTrue(flat.sample(Level.NETHER, three - 16, 0).inTransition());
        assertFalse(flat.sample(Level.NETHER, three + 16, 0).inTransition());
    }

    @Test
    void boundariesWithoutAnOverrideMatchTheSingleWidthFormula() {
        // The formula from before change 58, with one half width for every boundary.
        RingLayout uniform = new RingLayout(new RingSettings(RADII, 10200, 96, 128), SEED);
        double half = 64.0;
        for (double distance = 0; distance < 11_000; distance += 0.25) {
            RingPosition expected = oldPositionAt(distance, half);
            boolean nearFourFive = distance >= 8000 - 128 && distance < 8000 + 128;
            if (!nearFourFive) {
                assertEquals(expected, layout.positionAt(distance), "at " + distance);
            }
            assertEquals(expected, uniform.positionAt(distance), "at " + distance);
        }
        for (Ring ring : Ring.values()) {
            for (double distance = 0; distance < 11_000; distance += 7.5) {
                boolean old = oldReaches(ring, distance, 20.0, half);
                assertEquals(old, uniform.reaches(ring, distance, 20.0), ring + " at " + distance);
                boolean touchesFourFive = ring == Ring.RING_4 || ring == Ring.RING_5;
                if (!touchesFourFive) {
                    assertEquals(old, layout.reaches(ring, distance, 20.0), ring + " at " + distance);
                }
            }
        }
        // 4|5 reaches further: ring 4 up to 8000 + 128 + margin, ring 5 down to 8000 - 128 - margin.
        assertTrue(layout.reaches(Ring.RING_4, 8000 + 128 + 19.0, 20.0));
        assertFalse(uniform.reaches(Ring.RING_4, 8000 + 128 + 19.0, 20.0));
        assertTrue(layout.reaches(Ring.RING_5, 8000 - 128 - 19.0, 20.0));
        assertFalse(uniform.reaches(Ring.RING_5, 8000 - 128 - 19.0, 20.0));
    }

    /** {@code RingLayout.positionAt} as it was with a single transition width. */
    private static RingPosition oldPositionAt(double distance, double halfTransition) {
        for (int i = 0; i < RADII.size(); i++) {
            double zoneStart = RADII.get(i) - halfTransition;
            if (distance < zoneStart) {
                return RingPosition.within(Ring.byIndex(i));
            }
            if (distance < RADII.get(i) + halfTransition) {
                return new RingPosition(Ring.byIndex(i), Ring.byIndex(i + 1), (distance - zoneStart) / (2 * halfTransition));
            }
        }
        return RingPosition.within(Ring.VOID);
    }

    /** {@code RingLayout.reaches} as it was with a single transition width. */
    private static boolean oldReaches(Ring ring, double distance, double margin, double halfTransition) {
        int index = ring.ordinal();
        double lower = index == 0 ? Double.NEGATIVE_INFINITY : RADII.get(index - 1) - halfTransition - margin;
        double upper = index >= RADII.size() ? Double.POSITIVE_INFINITY : RADII.get(index) + halfTransition + margin;
        return distance > lower && distance < upper;
    }

    /** Furthest the effect of {@code boundary} can reach: noise plus half its transition zone. */
    private static int reach(RingSettings settings, int boundary) {
        return settings.boundaryNoise() + settings.transitionWidth(boundary) / 2;
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
