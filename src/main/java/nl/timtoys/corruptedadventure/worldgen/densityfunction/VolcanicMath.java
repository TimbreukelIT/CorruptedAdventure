package nl.timtoys.corruptedadventure.worldgen.densityfunction;

/** Small smooth-shape functions for the ring 3 terrain. */
final class VolcanicMath {
    private VolcanicMath() {
    }

    /** 0 below {@code edge0}, 1 above {@code edge1}, smooth in between; edges may be reversed (falling ramp). */
    static double smoothstep(double edge0, double edge1, double x) {
        double t = (x - edge0) / (edge1 - edge0);
        t = t < 0.0 ? 0.0 : Math.min(t, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** Polynomial smooth minimum: equals {@code min} when the two differ by at least {@code k}, rounded below that. */
    static double smoothMin(double a, double b, double k) {
        double h = Math.max(k - Math.abs(a - b), 0.0) / k;
        return Math.min(a, b) - h * h * k * 0.25;
    }

    static double smoothMax(double a, double b, double k) {
        return -smoothMin(-a, -b, k);
    }

    /**
     * A staircase over [0, 1] with {@code steps} treads: flat treads joined by smooth risers that take
     * {@code sharpness} of a step. 0 → 0, 1 → 1, continuous and monotone.
     */
    static double stair(double x, int steps, double sharpness) {
        double s = Math.max(0.0, Math.min(1.0, x)) * steps;
        double k = Math.floor(s);
        double f = s - k;
        double riser = smoothstep(0.0, 1.0, (f - 0.5) / sharpness + 0.5);
        return (k + riser) / steps;
    }
}
