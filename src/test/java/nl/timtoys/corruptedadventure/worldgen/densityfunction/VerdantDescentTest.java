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
            assertTrue(p.river() > VerdantDescent.width(LAYOUT) / 2.0 + 40.0 && p.river() + p.bedHalf() < VerdantDescent.width(LAYOUT) - 10.0,
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

    /** Across the seam of the angle (x < 0, z = 0) the descent runs on: same profile, ground without a step. */
    @Test
    void theDescentRunsOnAcrossTheSeam() {
        for (double u = 64.0; u < 250.0; u += 3.0) {
            double[] p = VerdantDescent.onRay(LAYOUT, Math.PI, VerdantDescent.zoneStart(LAYOUT) + u);
            int x = (int) Math.floor(p[0]);
            // The treads (without the basins, which may lie on one side only): across the seam they change no more than
            // over the same distance on either side of it.
            double[] h = new double[4];
            for (int i = 0; i < 4; i++) {
                h[i] = DESCENT.locate(x, -3 + 2 * i, new VerdantDescent.Point()).treads;
            }
            double across = Math.abs(h[2] - h[1]);
            assertTrue(across <= Math.max(Math.abs(h[1] - h[0]), Math.abs(h[3] - h[2])) + 0.75,
                    "ground across the seam at x " + x + ": " + java.util.Arrays.toString(h));
        }
        double period = VerdantDescent.perimeter(LAYOUT);
        double s1 = RingArc.arc(-6100.0, 0.5, VerdantDescent.midRadius(LAYOUT));
        double s2 = RingArc.arc(-6100.0, -0.5, VerdantDescent.midRadius(LAYOUT));
        assertTrue(RingArc.loopDistance(s1, s2, period) < 1.0, "s " + s1 + " / " + s2);
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
}
