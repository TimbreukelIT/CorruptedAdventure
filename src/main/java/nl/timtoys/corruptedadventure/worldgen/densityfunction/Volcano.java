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
 * @param pillars      basalt pillars from the ground up to the islands of an archipelago (39d), possibly none
 */
record Volcano(double x, double z, Kind kind, double plain, double rim, double radius, double crest, double floorRadius,
               double floor, double p, double q, boolean dormant, double lake, double rimA2, double rimPhase2, double rimA3,
               double rimPhase3, int gullies, double gullyPhase, double gullyDepth, double warp, List<Island> islands,
               List<Pillar> pillars) {

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
     * A basalt pillar under an archipelago (39f): from the crater floor ({@code base}, absolute Y) up into the main island
     * ({@code top}). Columnar rather than a cylinder: it flares at its foot and where it meets the island, leans and bends
     * a little, has a lobed, twisting outline and a few ledges sticking out. Pure; tested per block ({@link #contains}).
     * Its lowest block is {@link #floorY} (sunk one block into the crater floor). Equality is identity: it holds arrays.
     */
    static final class Pillar {
        private final double x;
        private final double z;
        private final double radius;
        private final double base;
        private final double top;
        private final double leanX;
        private final double leanZ;
        private final double phase3;
        private final double phase5;
        private final double[] ledgeY;
        private final double[] ledgeAngle;
        private final double[] ledgeLength;
        private final double reach;

        Pillar(double x, double z, double radius, double base, double top, double leanX, double leanZ, double phase3,
               double phase5, double[] ledgeY, double[] ledgeAngle, double[] ledgeLength) {
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.base = base;
            this.top = top;
            this.leanX = leanX;
            this.leanZ = leanZ;
            this.phase3 = phase3;
            this.phase5 = phase5;
            this.ledgeY = ledgeY;
            this.ledgeAngle = ledgeAngle;
            this.ledgeLength = ledgeLength;
            double ledge = 0.0;
            for (double l : ledgeLength) {
                ledge = Math.max(ledge, l);
            }
            this.reach = radius * 2.0 * 1.24 + Math.hypot(leanX, leanZ) + ledge + 1.0;
        }

        double x() {
            return x;
        }

        double z() {
            return z;
        }

        double base() {
            return base;
        }

        double top() {
            return top;
        }

        double leanX() {
            return leanX;
        }

        double leanZ() {
            return leanZ;
        }

        double[] ledgeY() {
            return ledgeY;
        }

        /** How far (blocks) the pillar may reach from its axis at the foot, so a column knows whether to test it. */
        double reach() {
            return reach;
        }

        /** The lowest block of the pillar: one below the crater floor it stands on, so no seam shows at its foot. */
        int floorY() {
            return (int) Math.floor(base) - 1;
        }

        /** Whether the block at (bx, y, bz) belongs to the pillar (block centre). */
        boolean contains(int bx, int y, int bz) {
            if (y < floorY() || y > top) {
                return false;
            }
            double s = Math.max(0.0, Math.min(1.0, (y + 0.5 - base) / (top - base)));
            double bend = Math.pow(s, 1.5);
            double dx = bx + 0.5 - (x + leanX * bend);
            double dz = bz + 0.5 - (z + leanZ * bend);
            double d2 = dx * dx + dz * dz;
            if (d2 > reach * reach) {
                return false;
            }
            double theta = Math.atan2(dz, dx);
            double foot = 1.0 - s;
            double r = radius * (1.0 + 1.0 * foot * foot * foot * foot + 0.7 * Math.pow(s, 6.0))
                    * (1.0 + 0.16 * Math.cos(3.0 * theta + phase3 + 2.5 * s) + 0.08 * Math.cos(5.0 * theta + phase5 - 4.0 * s));
            for (int k = 0; k < ledgeY.length; k++) {
                double dy = Math.abs(y + 0.5 - ledgeY[k]);
                if (dy <= 1.5) {
                    double off = Math.abs(Math.IEEEremainder(theta - ledgeAngle[k], Math.PI * 2.0));
                    if (off < 0.6) {
                        r += ledgeLength[k] * (1.0 - off / 0.6) * (1.0 - dy / 3.0);
                    }
                }
            }
            return d2 <= r * r;
        }
    }

    /**
     * A lava fall (39f): a spring (a source that flows, at {@code springY}, the first air above the edge column
     * ({@code springX}, {@code springZ}) of an island) and the falling lava right beside it, in the first column outside
     * the island ({@code fallX}, {@code fallZ}), from {@code springY} down to whatever lies below (the terrain decides
     * where it lands). Lava against open air is allowed here, and only here: the fall itself.
     */
    record LavaFall(int springX, int springZ, int springY, int fallX, int fallZ) {
    }

    /**
     * A floating island: an ellipse (half axes {@code a}, {@code a · stretch}, turned by {@code angle}) with a flat,
     * slightly domed top and a rocky underside (39f: a bowl with two or three hanging lobes of rock, {@code lobes}
     * around from {@code lobePhase}, instead of a single cone). {@code falls}: its lava falls (39f: springs on the edge
     * whose lava falls to the ground).
     */
    record Island(double x, double z, double a, double stretch, double angle, double top, double thickness, int lobes,
                  double lobePhase, List<LavaFall> falls) {
        /** Normalised distance from the centre: below 1 inside. {@code warp} distorts the outline (in units of a). */
        double distance(double px, double pz, double warp) {
            double dx = px - x;
            double dz = pz - z;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double along = (dx * cos + dz * sin) / (a * stretch);
            double across = (-dx * sin + dz * cos) / a;
            return ragged(Math.sqrt(along * along + across * across), Math.atan2(across, along), lobePhase) + warp;
        }

        /** The angle around the centre in the island's own frame (for the lobes of the underside). */
        double theta(double px, double pz) {
            double dx = px - x;
            double dz = pz - z;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            return Math.atan2((-dx * sin + dz * cos) / a, (dx * cos + dz * sin) / (a * stretch));
        }

        double topAt(double d) {
            return top + 1.5 * (1.0 - d * d) - 3.0 * Math.pow(d, 6.0);
        }

        /** The underside at normalised distance {@code d} and angle {@code theta}. */
        double bottomAt(double d, double theta) {
            return top - 1.0 - thickness * underside(d, theta, lobes, lobePhase);
        }

        /** The deepest the underside hangs at {@code d}, whatever the angle (for clearance checks). */
        double lowestAt(double d) {
            return top - 1.0 - thickness * underside(d, lobePhase / -lobes, lobes, lobePhase);
        }

        /**
         * The island's top (first air above it) in a column, or NaN if the column holds none of it; {@code warp} is the
         * column's warp noise (as the terrain uses it).
         */
        double columnTop(double px, double pz, double warp) {
            double d = distance(px, pz, 0.18 * warp);
            if (d >= 1.0) {
                return Double.NaN;
            }
            double topY = topAt(d);
            return Math.floor(topY - 1.0) >= Math.ceil(bottomAt(d, theta(px, pz))) ? topY : Double.NaN;
        }

        /** How far (blocks) the island and its lava falls reach from the centre along either axis. */
        double reach() {
            return a * stretch * 1.4 + 4.0;
        }
    }

    /**
     * A ragged outline (39f): the normalised distance {@code d} at angle {@code theta}, stretched by a few harmonics near
     * the edge only (up to about a sixth), so islands are no clean ellipses while their middle stays as it is. Rising in
     * {@code d}: inside the ellipse {@code d / (1 + h·d⁴)} with {@code |h| ≤ 0.16} (slope ≥ 1 − 3|h| > 0); outside it
     * the distance grows one for one from its value at the edge, so it passes 1 by {@code d ≈ 1.14} and stays there.
     */
    static double ragged(double d, double theta, double phase) {
        double h = 0.1 * Math.cos(3.0 * theta + phase) + 0.06 * Math.cos(5.0 * theta + 1.7 * phase);
        if (d <= 1.0) {
            return d / (1.0 + h * d * d * d * d);
        }
        return 1.0 / (1.0 + h) + (d - 1.0);
    }

    /** Normalised distance of the lowest point of an island's hanging lobes (the deepest of its underside). */
    static final double LOBE_D = 0.38;

    /**
     * Depth of a floating island's underside as a share of its thickness: a rounded bowl with {@code lobes} lobes of rock
     * hanging lower about a third of the way out, so it reads as a broken-off mass rather than a cone with a tip.
     */
    static double underside(double d, double theta, int lobes, double phase) {
        double bowl = Math.pow(Math.max(0.0, 1.0 - d * d), 0.8);
        double lobe = 0.5 + 0.5 * Math.cos(lobes * theta + phase);
        double u = (d - LOBE_D) / 0.26;
        double pendant = Math.exp(-u * u) * lobe * lobe;
        return 0.62 * bowl + 0.55 * pendant * Math.max(0.0, 1.0 - d);
    }
}
