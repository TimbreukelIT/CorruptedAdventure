package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The descent of {@code verdant_falls} (W-terrein 2c) on the test seed: the zone's coordinates, the rim profile, the
 * treads on 64 + 8k, the walkable risers, ring 3's half above the seal, the drop to the foot river, the seam.
 */
class VerdantDescentTest {
    private static final IslandField FIELD = IslandFieldTest.field();
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;
    private static final VerdantDescent DESCENT = FIELD.descent();

    /** The block column on the ray at {@code angle} at {@code u} blocks across the zone. */
    private static int[] at(double angle, double u) {
        double[] p = VerdantDescent.onRay(LAYOUT, angle, VerdantDescent.zoneStart(LAYOUT) + u);
        return new int[]{(int) Math.floor(p[0]), (int) Math.floor(p[1])};
    }

    @Test
    void theZoneSplitsAtTheSteamFensRim() {
        assertEquals(256.0, VerdantDescent.width(LAYOUT));
        assertEquals(6000.0 - 128.0, VerdantDescent.zoneStart(LAYOUT), 1e-9);
        assertEquals(60.0, VerdantDescent.rimAt(LAYOUT), 1e-9);
        assertEquals(VerdantDescent.rimAt(LAYOUT) / VerdantDescent.width(LAYOUT), IslandField.VERDANT_EDGE_SHARE, 1e-12);
        // Ring 4's water branch begins where the descent does.
        int[] rim = at(1.0, 61.5);
        RingPosition position = LAYOUT.sampleOverworld(rim[0], rim[1]);
        assertTrue(position.inTransition() && position.blend() >= IslandField.VERDANT_EDGE_SHARE, "rim " + position);
        VerdantDescent.Point p = DESCENT.locate(rim[0], rim[1], new VerdantDescent.Point());
        assertEquals(VerdantDescent.Point.DESCENT, p.kind);
        int[] fens = at(1.0, 58.0);
        assertEquals(VerdantDescent.Point.STRIP, DESCENT.locate(fens[0], fens[1], new VerdantDescent.Point()).kind);
        int[] inside = at(1.0, -2.0);
        assertEquals(VerdantDescent.Point.OUTSIDE, DESCENT.locate(inside[0], inside[1], new VerdantDescent.Point()).kind);
    }

    /** The rim height: ring 3's at the rim, smoothed along the ring and kept 88–120; per world, periodic. */
    @Test
    void theRimProfileIsSmoothBoundedAndPeriodic() {
        int count = DESCENT.gridCount();
        double previous = DESCENT.sample(count - 1).rim();
        double steepest = 0.0;
        for (int j = 0; j < count; j++) {
            VerdantDescent.Profile p = DESCENT.sample(j);
            assertTrue(p.rim() >= VerdantDescent.RIM_MIN && p.rim() <= VerdantDescent.RIM_MAX, "rim " + p.rim());
            assertTrue(p.topWidth() >= 6.0 && p.topWidth() <= 14.0, "top terrace " + p.topWidth());
            assertTrue(p.bedHalf() >= 5.0 && p.bedHalf() <= 9.0, "bed " + p.bedHalf());
            assertTrue(p.river() >= 150.0 && p.river() + p.bedHalf() < VerdantDescent.width(LAYOUT) - 10.0,
                    "river " + p.river());
            steepest = Math.max(steepest, Math.abs(p.rim() - previous));
            previous = p.rim();
        }
        // ±128 blocks of smoothing: under a block per 5 blocks along the ring (the descent falls ~0.25 per block across
        // it), so the stair lines never turn more than ~35° from the ring.
        System.out.println("steepest rim change per sample: " + steepest);
        assertTrue(steepest < 0.8, "rim changes by " + steepest + " per sample");
        double period = VerdantDescent.perimeter(LAYOUT);
        for (double s = 0.0; s < period; s += 977.3) {
            assertSameProfile(DESCENT.profile(s), DESCENT.profile(s + period));
            assertSameProfile(DESCENT.profile(s), DESCENT.profile(s - period));
        }
    }

    /**
     * On the treads the ground is level on 64 + 8k (the cascades' levels; Q5: 60 % tread), the risers between are
     * rounded and walkable: never more than a block of rise per block outside the coves under the falls.
     */
    @Test
    void treadsAreLevelAndRisersWalkable() {
        int treads = 0;
        int steps = 0;
        int steep = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.0137) {
            for (double u = 75.0; u < 200.0; u += 1.0) {
                int[] c = at(angle, u);
                VerdantDescent.Point p = DESCENT.locate(c[0], c[1], new VerdantDescent.Point());
                if (p.kind != VerdantDescent.Point.DESCENT || p.v <= 0.0 || p.v >= 1.0) {
                    continue;
                }
                double f = (p.ramp - VerdantDescent.BANK_TOP) / VerdantDescent.STEP - p.body;
                if (f < VerdantDescent.TREAD - 0.02) {
                    assertEquals(VerdantDescent.level(p.body), p.treads, 1e-9, "tread at " + c[0] + " " + c[1]);
                    treads++;
                }
                // Walkable: the top blocks of neighbouring columns differ by at most one (a jump), on risers too.
                int here = (int) Math.ceil(p.treads) - 1;
                int east = (int) Math.ceil(DESCENT.locate(c[0] + 1, c[1], new VerdantDescent.Point()).treads) - 1;
                int south = (int) Math.ceil(DESCENT.locate(c[0], c[1] + 1, new VerdantDescent.Point()).treads) - 1;
                steps++;
                steep += Math.max(Math.abs(east - here), Math.abs(south - here)) >= 2 ? 1 : 0;
            }
        }
        System.out.printf("tread columns %d, columns %d, a step of 2 or more to a neighbour: %d%n", treads, steps, steep);
        assertTrue(treads > steps / 3, "treads " + treads + " of " + steps);
        // The risers' rounded shape (Q5: smootherstep over 40 % of a step) peaks at ~1.9 times their mean slope, so in the
        // middle of the steeper risers a neighbour may lie two blocks lower: a few percent of the descent.
        assertTrue(steep < steps / 12, "steep columns " + steep + " of " + steps);
    }

    private static void assertSameProfile(VerdantDescent.Profile a, VerdantDescent.Profile b) {
        assertEquals(a.rim(), b.rim(), 1e-9);
        assertEquals(a.topWidth(), b.topWidth(), 1e-9);
        assertEquals(a.river(), b.river(), 1e-9);
        assertEquals(a.bedHalf(), b.bedHalf(), 1e-9);
        assertEquals(a.curve(), b.curve(), 1e-9);
    }

    /** Risers (the treads' height between two levels, along the ray): at least 8 wide where the drop allows (p10). */
    @Test
    void risersAreWide() {
        List<Double> widths = new ArrayList<>();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.0291) {
            double start = Double.NaN;
            boolean tread = false;
            for (double u = 60.0; u < 215.0; u += 0.25) {
                double[] p = VerdantDescent.onRay(LAYOUT, angle, VerdantDescent.zoneStart(LAYOUT) + u);
                VerdantDescent.Point point = DESCENT.locate(p[0], p[1], new VerdantDescent.Point());
                if (point.kind != VerdantDescent.Point.DESCENT) {
                    continue;
                }
                double f = (point.ramp - VerdantDescent.BANK_TOP) / VerdantDescent.STEP - Math.floor((point.ramp - VerdantDescent.BANK_TOP) / VerdantDescent.STEP);
                boolean riser = f >= VerdantDescent.TREAD && point.v > 0.0 && point.v < 1.0;
                if (riser && Double.isNaN(start) && tread) {
                    start = u;
                } else if (!riser && !Double.isNaN(start)) {
                    // A whole riser: from a tread to the next (not the top terrace's partial one, not the foot's).
                    if (point.v < 1.0) {
                        widths.add(u - start);
                    }
                    start = Double.NaN;
                }
                tread |= !riser && point.v > 0.0;
            }
        }
        Collections.sort(widths);
        double p10 = widths.get(widths.size() / 10);
        double median = widths.get(widths.size() / 2);
        System.out.printf("risers %d, p10 %.1f, median %.1f, min %.1f%n", widths.size(), p10, median, widths.get(0));
        assertTrue(p10 >= 8.0, "riser p10 " + p10);
        assertTrue(median >= 10.0, "riser median " + median);
    }

    /** Ring 3's half of the zone is sealed below Y 63: the descent's ground (with its basins) stays at 65 or higher there. */
    @Test
    void theDescentStaysAboveTheSealInRing3sHalf() {
        double lowest = Double.POSITIVE_INFINITY;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.0071) {
            for (double u = 60.5; u < 128.0; u += 1.5) {
                int[] c = at(angle, u);
                RingPosition position = LAYOUT.sampleOverworld(c[0], c[1]);
                if (position.ring() != Ring.RING_3 || position.blend() < IslandField.VERDANT_EDGE_SHARE) {
                    continue;
                }
                lowest = Math.min(lowest, FIELD.verdantHeight(c[0], c[1]));
            }
        }
        System.out.println("lowest ground in ring 3's half of verdant_falls: " + lowest);
        assertTrue(lowest >= 65.0, "lowest " + lowest);
    }

    /** From the top terrace to the foot river's water (Y 63): about 28–45 blocks (Q1 = A). */
    @Test
    void theDescentDropsToTheFootRiver() {
        List<Double> drops = new ArrayList<>();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.013) {
            VerdantDescent.Profile profile = DESCENT.profile(angle * VerdantDescent.midRadius(LAYOUT));
            int[] top = at(angle, VerdantDescent.rimAt(LAYOUT) + profile.topWidth() / 2.0);
            drops.add(DESCENT.locate(top[0], top[1], new VerdantDescent.Point()).treads - 63.0);
            int[] river = at(angle, profile.river());
            assertTrue(FIELD.verdantHeight(river[0], river[1]) <= VerdantDescent.RIVER_BED + 0.5, "river bed at " + angle);
        }
        Collections.sort(drops);
        double p10 = drops.get(drops.size() / 10);
        double p90 = drops.get(drops.size() * 9 / 10);
        System.out.printf("drop p10 %.1f, median %.1f, p90 %.1f%n", p10, drops.get(drops.size() / 2), p90);
        assertTrue(drops.get(0) >= 24.0 && drops.get(drops.size() - 1) <= 57.0, "drops " + drops.get(0) + "–" + drops.get(drops.size() - 1));
        double median = drops.get(drops.size() / 2);
        assertTrue(p10 >= 24.0 && p90 <= 50.0 && median >= 28.0 && median <= 45.0, "drop p10 " + p10 + ", median " + median + ", p90 " + p90);
    }

    /**
     * Across the seam of the angle (x < 0, z = 0) and across the arc's own seam, where s wraps from the perimeter to 0
     * (x > 0, z = 0), the descent runs on: same profile, ground without a step.
     */
    @Test
    void theDescentRunsOnAcrossTheSeam() {
        for (double angle : new double[]{Math.PI, 0.0}) {
            for (double u = 64.0; u < 250.0; u += 3.0) {
                double[] p = VerdantDescent.onRay(LAYOUT, angle, VerdantDescent.zoneStart(LAYOUT) + u);
                int x = (int) Math.floor(p[0]);
                // The treads (without the basins, which may lie on one side only): across the seam they change no more
                // than over the same distance on either side of it.
                double[] h = new double[4];
                for (int i = 0; i < 4; i++) {
                    h[i] = DESCENT.locate(x, -3 + 2 * i, new VerdantDescent.Point()).treads;
                }
                double across = Math.abs(h[2] - h[1]);
                assertTrue(across <= Math.max(Math.abs(h[1] - h[0]), Math.abs(h[3] - h[2])) + 0.75,
                        "ground across the seam at x " + x + ": " + java.util.Arrays.toString(h));
            }
        }
        double period = VerdantDescent.perimeter(LAYOUT);
        double radius = VerdantDescent.midRadius(LAYOUT);
        assertTrue(RingArc.loopDistance(RingArc.arc(-6100.0, 0.5, radius), RingArc.arc(-6100.0, -0.5, radius), period) < 1.0);
        double s1 = RingArc.arc(6100.0, 0.5, radius);
        double s2 = RingArc.arc(6100.0, -0.5, radius);
        assertTrue(s1 < 1.0 && s2 > period - 1.0 && RingArc.loopDistance(s1, s2, period) < 1.0, "s " + s1 + " / " + s2);
    }

    @Test
    void ringArcWrapsAndMeasuresTheShortWay() {
        assertEquals(3.0, RingArc.wrap(13.0, 10.0), 1e-12);
        assertEquals(7.0, RingArc.wrap(-3.0, 10.0), 1e-12);
        assertEquals(2.0, RingArc.loopDistance(9.0, 1.0, 10.0), 1e-12);
        assertEquals(2.0, RingArc.loopDelta(1.0, 9.0, 10.0), 1e-12);
        assertEquals(-2.0, RingArc.loopDelta(9.0, 1.0, 10.0), 1e-12);
        assertEquals(785, RingArc.cellCount(2.0 * Math.PI * 6000.0, 48.0));
    }
    /**
     * The foot river's outer bank (decision 2026-10-15, "river inwards"): from the river's bed out to the zone's edge and
     * 16 blocks into ring 4, along every ray of the test seed, the ground never jumps by more than 2 per block, except
     * where ring 4's own terrain jumps as much at the same place (its fjord walls are 2:1 by design, Q6, and its risers
     * peak at ~1.9 times their slope): those steps belong to ring 4, not to the bank, and count with half a block of slack.
     */
    @Test
    void theRiversOuterBankIsNoWall() {
        List<Double> jumps = new ArrayList<>();
        double worst = 0.0;
        String where = "";
        int rays = 0;
        for (int n = 0; n < 3142; n++) {
            double angle = n * 2.0 * Math.PI / 3142.0;
            VerdantDescent.Profile profile = DESCENT.profile(RingArc.wrap(angle * VerdantDescent.midRadius(LAYOUT), VerdantDescent.perimeter(LAYOUT)));
            double[] start = VerdantDescent.onRay(LAYOUT, angle, VerdantDescent.zoneStart(LAYOUT) + profile.river() + profile.bedHalf());
            double[] end = VerdantDescent.onRay(LAYOUT, angle, VerdantDescent.zoneStart(LAYOUT) + VerdantDescent.width(LAYOUT) + 16.0);
            double r0 = Math.hypot(start[0], start[1]);
            double r1 = Math.hypot(end[0], end[1]);
            int px = Integer.MIN_VALUE;
            int pz = 0;
            double ph = 0.0;
            double pr = 0.0;
            for (double r = r0; r <= r1; r += 1.0) {
                int x = (int) Math.floor(r * Math.cos(angle));
                int z = (int) Math.floor(r * Math.sin(angle));
                if (x == px && z == pz) {
                    continue;
                }
                double h = FIELD.verdantHeight(x, z);
                double own = FIELD.baseHeight(x, z);
                if (px != Integer.MIN_VALUE) {
                    double d = Math.hypot(x - px, z - pz);
                    double jump = Math.abs(h - ph) / d;
                    double ring4 = Math.abs(own - pr) / d;
                    double added = jump > ring4 + 0.5 ? jump : Math.min(jump, 2.0);
                    jumps.add(added);
                    if (added > worst) {
                        worst = added;
                        where = x + " " + z + ": " + ph + " -> " + h;
                    }
                }
                px = x;
                pz = z;
                ph = h;
                pr = own;
            }
            rays++;
        }
        Collections.sort(jumps);
        double p90 = jumps.get(jumps.size() * 9 / 10);
        double p99 = jumps.get(jumps.size() * 99 / 100);
        System.out.printf("outer bank: rays %d, steps %d, p90 %.2f, p99 %.2f, max %.2f at %s%n", rays, jumps.size(), p90, p99, worst, where);
        assertTrue(worst <= 2.0, "outer bank jumps " + worst + " per block at " + where);
    }
}
