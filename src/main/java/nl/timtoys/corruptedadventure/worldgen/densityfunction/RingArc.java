package nl.timtoys.corruptedadventure.worldgen.densityfunction;

/**
 * Positions along a ring (a loop round the world's centre): the arc in blocks at a radius, wrapped into one period so
 * it runs on continuously across the seam of the angle (−π | π, at x &lt; 0, z = 0), and periodic cells along it. Shared
 * by the descent of {@code verdant_falls} ({@link VerdantDescent}, {@link VerdantBasins}) and the land bridges of the
 * zone 4→5 ({@link Ring4Sundered}). Pure.
 */
final class RingArc {
    private RingArc() {
    }

    /** The loop's length (blocks) at {@code radius}. */
    static double perimeter(double radius) {
        return 2.0 * Math.PI * radius;
    }

    /** The position along the loop at {@code radius} (blocks), in [0, perimeter). */
    static double arc(double x, double z, double radius) {
        return wrap(Math.atan2(z, x) * radius, perimeter(radius));
    }

    /** {@code value} in [0, period). */
    static double wrap(double value, double period) {
        double m = value % period;
        return m < 0.0 ? m + period : m;
    }

    /** The distance between two positions on a loop of the given period (the shorter way round). */
    static double loopDistance(double a, double b, double period) {
        double d = wrap(a - b, period);
        return Math.min(d, period - d);
    }

    /** {@code a − b} the shorter way round the loop, in (−period / 2, period / 2]. */
    static double loopDelta(double a, double b, double period) {
        double d = wrap(a - b, period);
        return d > period / 2.0 ? d - period : d;
    }

    /** The number of cells of about {@code cell} blocks around a loop of {@code period}; each is period / count long. */
    static int cellCount(double period, double cell) {
        return Math.max(1, (int) Math.round(period / cell));
    }
}
