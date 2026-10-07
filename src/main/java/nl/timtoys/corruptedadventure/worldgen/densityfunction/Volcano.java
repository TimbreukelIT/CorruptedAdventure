package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import java.util.List;

/**
 * One volcano of the ring 3 terrain: a cone with a crater on top (great or ordinary), or a low caldera ring on the
 * plain. All heights are absolute Y. The outer flank rises from the plain to the rim, concave ({@code (1 − u)^p}: steep
 * under the rim, flattening to the foot) with radial gullies; inside the crest the crater wall falls to a flat floor
 * ({@code v^q}: steep under the rim). The two meet in a rounded crest (smooth minimum). The rim height varies around
 * the crest; {@link #rimLow} is its lowest possible value, and an active crater's lava lake stays well below it.
 *
 * @param x            centre x
 * @param z            centre z
 * @param kind         great, cone or caldera
 * @param plain        plain height at the centre (the flank is measured from it)
 * @param rim          mean rim height
 * @param radius       radius of the foot
 * @param crest        radius of the crest
 * @param floorRadius  radius of the flat crater floor
 * @param floor        crater floor height
 * @param p            exponent of the outer flank
 * @param q            exponent of the crater wall
 * @param dormant      a dormant crater: no lava, a lower flat floor (the {@code dormant_caldera} biome)
 * @param lake         top of the lava lake (exclusive), or {@link Double#NaN} if dormant
 * @param rimA2        amplitude of the 2-fold rim variation
 * @param rimPhase2    its phase
 * @param rimA3        amplitude of the 3-fold rim variation
 * @param rimPhase3    its phase
 * @param gullies      number of radial gullies
 * @param gullyPhase   their phase
 * @param gullyDepth   their depth as a fraction of the flank height
 * @param warp         amplitude (blocks) of the domain warp of the outline
 * @param islands      floating islands above the crater, possibly none
 */
record Volcano(double x, double z, Kind kind, double plain, double rim, double radius, double crest, double floorRadius,
               double floor, double p, double q, boolean dormant, double lake, double rimA2, double rimPhase2, double rimA3,
               double rimPhase3, int gullies, double gullyPhase, double gullyDepth, double warp, List<Island> islands) {

    enum Kind {
        GREAT, CONE, CALDERA
    }

    /** The lowest the rim gets anywhere around the crest. */
    double rimLow() {
        return rim - Math.abs(rimA2) - Math.abs(rimA3);
    }

    double rimAt(double angle) {
        return rim + rimA2 * Math.cos(2.0 * angle + rimPhase2) + rimA3 * Math.cos(3.0 * angle + rimPhase3);
    }

    /** Radius of influence, including the rounded crest and warp. */
    double reach() {
        return radius + warp + 2.0;
    }

    /**
     * The surface of this volcano at distance {@code r} (already warped) and angle {@code angle}, on a plain of height
     * {@code plainHere}. Beyond the foot it is the plain. {@code meander} (radians) bends the gullies.
     */
    double surface(double r, double angle, double plainHere, double meander) {
        double rimHere = rimAt(angle);
        double lift = rimHere - plain;
        double outer;
        if (r >= radius) {
            outer = plainHere;
        } else if (r >= crest) {
            double u = (r - crest) / (radius - crest);
            double profile = Math.pow(1.0 - u, p);
            // Gullies meander with the warp noise and differ in depth around the cone, so they never read as straight spokes.
            double gully = Math.pow(0.5 + 0.5 * Math.cos(gullies * angle + gullyPhase + meander), 4.0)
                    * (0.55 + 0.45 * Math.cos(2.0 * angle + 1.7 * gullyPhase));
            double fade = 4.0 * u * (1.0 - u);
            outer = plainHere + lift * profile * (1.0 - gullyDepth * gully * fade);
        } else {
            // Past the crest the flank keeps rising linearly, so the smooth minimum with the crater wall rounds the crest.
            outer = plainHere + lift + p * lift / (radius - crest) * (crest - r);
        }
        double wallHeight = rimHere - floor;
        double inner;
        if (r <= floorRadius) {
            inner = floor;
        } else if (r <= crest) {
            inner = floor + wallHeight * Math.pow((r - floorRadius) / (crest - floorRadius), q);
        } else {
            inner = rimHere + q * wallHeight / (crest - floorRadius) * (r - crest);
        }
        return VolcanicMath.smoothMin(inner, outer, Math.max(6.0, 0.35 * wallHeight));
    }

    /** Steepness of the flank or crater wall at distance {@code r} (blocks per block), for the crag roughness. */
    double slope(double r) {
        double lift = rim - plain;
        if (r >= radius) {
            return 0.0;
        }
        if (r >= crest) {
            double u = (r - crest) / (radius - crest);
            return p * lift * Math.pow(1.0 - u, p - 1.0) / (radius - crest);
        }
        if (r > floorRadius) {
            double v = (r - floorRadius) / (crest - floorRadius);
            return q * (rim - floor) * Math.pow(v, q - 1.0) / (crest - floorRadius);
        }
        return 0.0;
    }

    /** 1 on the crater floor, 0 from the crest outwards. */
    double craterMask(double r) {
        return 1.0 - VolcanicMath.smoothstep(floorRadius, crest, r);
    }

    /**
     * A floating island: an ellipse (half axes {@code a}, {@code a · stretch}, turned by {@code angle}) with a flat,
     * slightly domed top and an underside that tapers to a point below the centre.
     */
    record Island(double x, double z, double a, double stretch, double angle, double top, double thickness) {
        /** Normalised distance from the centre: below 1 inside. {@code warp} distorts the outline (in units of a). */
        double distance(double px, double pz, double warp) {
            double dx = px - x;
            double dz = pz - z;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double along = (dx * cos + dz * sin) / (a * stretch);
            double across = (-dx * sin + dz * cos) / a;
            return Math.sqrt(along * along + across * across) + warp;
        }

        double topAt(double d) {
            return top + 1.5 * (1.0 - d * d) - 3.0 * Math.pow(d, 6.0);
        }

        /** The underside hangs like an inverted cone with a rounded tip. */
        double bottomAt(double d) {
            double rounded = Math.pow(Math.max(0.0, 1.0 - d * d), 0.5);
            return top - 1.0 - thickness * Math.min(rounded, Math.pow(Math.max(0.0, 1.0 - d), 1.15));
        }

        double reach() {
            return a * stretch + 4.0;
        }
    }
}
