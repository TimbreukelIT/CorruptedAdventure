package nl.timtoys.corruptedadventure.command;

import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingTargetsTest {
    private static final RingSettings DEFAULTS = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10000, 96, 128);

    private final RingLayout layout = new RingLayout(DEFAULTS, 12345L);

    @Test
    void headingFallsBackToEastAtTheCentre() {
        assertEquals(RingTargets.Heading.EAST, RingTargets.Heading.towards(0, 0));
        assertEquals(new RingTargets.Heading(0, -1), RingTargets.Heading.towards(0, -250));
    }

    @Test
    void middleRadiusIsTheMiddleOfTheNominalBand() {
        assertEquals(750, RingTargets.middleRadius(DEFAULTS, Ring.RING_1));
        assertEquals(4750, RingTargets.middleRadius(DEFAULTS, Ring.RING_3));
        assertEquals(6900, RingTargets.middleRadius(DEFAULTS, Ring.RING_4));
        assertEquals(8650, RingTargets.middleRadius(DEFAULTS, Ring.RING_5));
        assertEquals(9750, RingTargets.middleRadius(DEFAULTS, Ring.VOID));
    }

    @Test
    void ringMiddlesAreFullyInsideTheirRing() {
        for (int angle = 0; angle < 360; angle += 5) {
            RingTargets.Heading heading = heading(angle);
            for (Ring ring : Ring.values()) {
                double radius = RingTargets.middleRadius(DEFAULTS, ring);
                assertEquals(RingPosition.within(ring), layout.sampleOverworld(heading.x() * radius, heading.z() * radius));
            }
        }
    }

    @Test
    void boundaryRadiusLandsHalfwayThroughTheTransition() {
        for (int angle = 0; angle < 360; angle += 5) {
            RingTargets.Heading heading = heading(angle);
            for (int boundary = 1; boundary <= RingSettings.RING_COUNT; boundary++) {
                double radius = RingTargets.boundaryRadius(layout, DEFAULTS, boundary, heading);
                RingPosition position = layout.sampleOverworld(heading.x() * radius, heading.z() * radius);
                assertEquals(Ring.byIndex(boundary - 1), position.inner());
                assertEquals(Ring.byIndex(boundary), position.outer());
                assertEquals(0.5, position.blend(), 0.01, "boundary " + boundary + ", angle " + angle);
            }
        }
    }

    @Test
    void nearestBoundaryIsSignedAndPicksTheClosestRadius() {
        RingTargets.Heading heading = heading(40);
        double onBoundary = RingTargets.boundaryRadius(layout, DEFAULTS, 2, heading);

        RingTargets.BoundaryDistance at = RingTargets.nearestBoundary(layout, DEFAULTS, heading.x() * onBoundary, heading.z() * onBoundary);
        assertEquals(2, at.boundary());
        assertEquals(0, at.distance(), 0.01);

        // Exact radial distance to the boundary along this heading, not skewed by the noise at the position itself.
        double near = onBoundary - 50;
        assertEquals(-50, RingTargets.nearestBoundary(layout, DEFAULTS, heading.x() * near, heading.z() * near).distance(), 0.01);

        double inside = RingTargets.middleRadius(DEFAULTS, Ring.RING_2) + 200;
        RingTargets.BoundaryDistance before = RingTargets.nearestBoundary(layout, DEFAULTS, heading.x() * inside, heading.z() * inside);
        assertEquals(2, before.boundary());
        assertTrue(before.distance() < 0, "distance " + before.distance());

        double outside = 3500 + 300;
        RingTargets.BoundaryDistance after = RingTargets.nearestBoundary(layout, DEFAULTS, heading.x() * outside, heading.z() * outside);
        assertEquals(2, after.boundary());
        assertTrue(after.distance() > 0, "distance " + after.distance());
    }

    private static RingTargets.Heading heading(int degrees) {
        double radians = Math.toRadians(degrees);
        return new RingTargets.Heading(Math.cos(radians), Math.sin(radians));
    }
}
