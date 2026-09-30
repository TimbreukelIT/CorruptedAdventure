package nl.timtoys.corruptedadventure.command;

import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import java.util.List;

/**
 * Where the debug commands measure and teleport to. All radii are in overworld blocks from (0,0).
 */
final class RingTargets {
    private static final double BISECTION_PRECISION = 0.01;

    private RingTargets() {
    }

    /** Unit vector from (0,0) towards a position; east at the centre itself. */
    record Heading(double x, double z) {
        static final Heading EAST = new Heading(1, 0);

        static Heading towards(double x, double z) {
            double length = Math.sqrt(x * x + z * z);
            return length < 1 ? EAST : new Heading(x / length, z / length);
        }
    }

    /**
     * @param boundary 1 (between ring 1 and 2) to 5 (between ring 5 and the void)
     * @param distance signed, in overworld blocks: positive outside the boundary, negative inside
     */
    record BoundaryDistance(int boundary, double distance) {
    }

    /** Radial middle of a ring's band; far enough from both boundaries that noise and blending never reach it. */
    static double middleRadius(RingSettings settings, Ring ring) {
        List<Integer> radii = settings.ringOuterRadii();
        int index = ring.ordinal();
        int inner = index == 0 ? 0 : radii.get(index - 1);
        int outer = index < radii.size() ? radii.get(index) : settings.voidOuterRadius();
        return (inner + outer) / 2.0;
    }

    /**
     * Radius along {@code heading} where the noisy boundary really lies: where the blend is 0.5.
     *
     * @param boundary 1 to 5, see {@link BoundaryDistance}
     */
    static double boundaryRadius(RingLayout layout, RingSettings settings, int boundary, Heading heading) {
        int radius = settings.ringOuterRadii().get(boundary - 1);
        // The noise offset is clamped to ±boundaryNoise, so the crossing lies inside this interval.
        // Should the noise cross back and forth, bisection just settles on one of the crossings.
        double low = radius - settings.boundaryNoise() - 1;
        double high = radius + settings.boundaryNoise() + 1;
        while (high - low > BISECTION_PRECISION) {
            double middle = (low + high) / 2;
            if (layout.effectiveDistance(heading.x() * middle, heading.z() * middle) < radius) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) / 2;
    }

    /**
     * Measured radially, against where each noisy boundary really lies along this heading (the noise offset at the
     * position itself differs from the one at the boundary).
     *
     * @param x overworld x (Nether positions already multiplied by {@link RingLayout#NETHER_SCALE})
     */
    static BoundaryDistance nearestBoundary(RingLayout layout, RingSettings settings, double x, double z) {
        Heading heading = Heading.towards(x, z);
        double radius = Math.sqrt(x * x + z * z);
        BoundaryDistance nearest = null;
        for (int boundary = 1; boundary <= settings.ringOuterRadii().size(); boundary++) {
            double distance = radius - boundaryRadius(layout, settings, boundary, heading);
            if (nearest == null || Math.abs(distance) < Math.abs(nearest.distance())) {
                nearest = new BoundaryDistance(boundary, distance);
            }
        }
        return nearest;
    }
}
