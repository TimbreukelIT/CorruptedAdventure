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

    // ---------------------------------------------------------------- natural, not rice fields (polish round)

    /**
     * Polish round "afdaling" (2026-10-15): the stair lines bend in lobes and inlets of a few tens of blocks and wander
     * each on its own, so the treads widen and narrow along the ring instead of running as parallel rice-field lines.
     * Before the round (slow wave only): wiggle 0.6, parallel 0.93, spread 0.08.
     */
    @Test
    void theStairLinesWanderOnTheirOwn() {
        Straightness s = straightness(FIELD, LAYOUT);
        System.out.println("stair lines: " + s);
        assertTrue(s.series() >= 40, "series " + s.series());
        assertTrue(s.wiggle() >= 1.5, "wiggle " + s.wiggle());
        assertTrue(s.parallel() <= 0.65, "parallel " + s.parallel());
        assertTrue(s.spread() >= 0.12, "spread " + s.spread());
    }

    /**
     * No loose blocks (polish round "afdaling"): a riser's noise never takes the ground a hair over the next level (a
     * whole block on the tread's edge), nor the top terrace's noise over the integer it hovers about. On the treads alone
     * and on the whole ground (without the gorges' and basins' own shapes). Before the round: 562 and 313.
     */
    @Test
    void theDescentHasNoLooseBlocks() {
        Lumps own = protrusions(FIELD, LAYOUT, true);
        Lumps all = protrusions(FIELD, LAYOUT, false);
        System.out.println("loose blocks: treads " + own + ", ground " + all);
        assertTrue(own.columns() > 50000 && all.columns() > 30000, "columns " + own.columns() + " / " + all.columns());
        assertEquals(0, own.count(), "loose blocks on the treads at " + own.first());
        assertEquals(0, all.count(), "loose blocks on the ground at " + all.first());
    }

    /**
     * The risers' profile: here and there a rocky knick, steep enough (≥ 2 per block over ±2 blocks) for the surface's
     * slope rule to show stone; most risers stay the soft smootherstep of Q5.
     */
    @Test
    void someRisersAreRockyKnicks() {
        int steep = 0;
        int risers = 0;
        VerdantDescent.Point p = new VerdantDescent.Point();
        VerdantDescent.Point q = new VerdantDescent.Point();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.0137) {
            for (double u = 75.0; u < 200.0; u += 1.0) {
                int[] c = at(angle, u);
                DESCENT.locate(c[0], c[1], p);
                double f = (p.ramp - VerdantDescent.BANK_TOP) / VerdantDescent.STEP - p.body;
                if (p.kind != VerdantDescent.Point.DESCENT || p.v <= 0.0 || p.v >= 1.0 || f < VerdantDescent.TREAD) {
                    continue;
                }
                risers++;
                double gx = DESCENT.locate(c[0] + 2, c[1], q).treads - DESCENT.locate(c[0] - 2, c[1], q).treads;
                double gz = DESCENT.locate(c[0], c[1] + 2, q).treads - DESCENT.locate(c[0], c[1] - 2, q).treads;
                steep += Math.hypot(gx, gz) / 4.0 >= 2.0 ? 1 : 0;
            }
        }
        System.out.printf("riser columns %d, at least 2 per block %d%n", risers, steep);
        assertTrue(steep > risers / 100 && steep < risers / 4, "steep " + steep + " of " + risers);
    }

    /**
     * How the stair lines run (polish round "afdaling"): per tread edge (where the ramp leaves a tread for the riser
     * below it) its position across the zone along the ring, in windows of 384 blocks every 2 blocks. {@code wiggle}:
     * the RMS of that position about its moving mean over ±32 blocks (short-scale lobes and inlets; the slow wave alone
     * stays under a block); {@code parallel}: the correlation of those residuals between neighbouring edges (1: the lines
     * run in parallel; near 0: each wanders on its own, so treads widen, narrow, merge and split); {@code spread}: the
     * mean coefficient of variation of the treads' widths between neighbouring edges.
     */
    record Straightness(double wiggle, double parallel, double spread, int series) {
    }

    static Straightness straightness(IslandField field, RingLayout layout) {
        VerdantDescent descent = field.descent();
        double radius = VerdantDescent.midRadius(layout);
        double period = VerdantDescent.perimeter(layout);
        int windows = 16;
        int samples = 192;
        int bodies = 7;
        double residualSquares = 0.0;
        int residuals = 0;
        double sxy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double spreadSum = 0.0;
        int spreads = 0;
        int series = 0;
        VerdantDescent.Point p = new VerdantDescent.Point();
        for (int w = 0; w < windows; w++) {
            double s0 = w * period / windows + 173.0;
            double[][] edges = new double[bodies][samples];
            for (double[] e : edges) {
                java.util.Arrays.fill(e, Double.NaN);
            }
            for (int k = 0; k < samples; k++) {
                double angle = (s0 + 2.0 * k) / radius;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double[] start = VerdantDescent.onRay(layout, angle, VerdantDescent.zoneStart(layout) + VerdantDescent.rimAt(layout));
                double r0 = Math.hypot(start[0], start[1]);
                double previousRamp = Double.NaN;
                double previousU = Double.NaN;
                for (double r = r0; r < r0 + 200.0; r += 0.5) {
                    descent.locate(r * cos, r * sin, p);
                    if (p.kind != VerdantDescent.Point.DESCENT || p.v >= 1.0) {
                        if (p.kind == VerdantDescent.Point.DESCENT && p.v >= 1.0) {
                            break;
                        }
                        continue;
                    }
                    for (int b = 0; b < bodies; b++) {
                        double edge = VerdantDescent.level(b) + VerdantDescent.STEP * VerdantDescent.TREAD;
                        if (VerdantDescent.level(b + 1) < p.profile.rim() && previousRamp >= edge && p.ramp < edge) {
                            double t = (previousRamp - edge) / (previousRamp - p.ramp);
                            edges[b][k] = previousU + t * (p.u - previousU);
                        }
                    }
                    previousRamp = p.ramp;
                    previousU = p.u;
                }
            }
            double[][] residual = new double[bodies][];
            for (int b = 0; b < bodies; b++) {
                residual[b] = highPass(edges[b], 16);
                if (residual[b] != null) {
                    series++;
                    for (double v : residual[b]) {
                        residualSquares += v * v;
                        residuals++;
                    }
                }
            }
            for (int b = 0; b + 1 < bodies; b++) {
                if (residual[b] == null || residual[b + 1] == null) {
                    continue;
                }
                double mean = 0.0;
                double[] width = new double[samples];
                for (int k = 0; k < samples; k++) {
                    sxy += residual[b][k] * residual[b + 1][k];
                    sxx += residual[b][k] * residual[b][k];
                    syy += residual[b + 1][k] * residual[b + 1][k];
                    width[k] = edges[b][k] - edges[b + 1][k];
                    mean += width[k] / samples;
                }
                double var = 0.0;
                for (double v : width) {
                    var += (v - mean) * (v - mean) / samples;
                }
                spreadSum += Math.sqrt(var) / mean;
                spreads++;
            }
        }
        return new Straightness(Math.sqrt(residualSquares / Math.max(1, residuals)), sxy / Math.sqrt(sxx * syy),
                spreadSum / Math.max(1, spreads), series);
    }

    /** The values minus their moving mean over ±{@code half} samples, or null if any is missing. */
    private static double[] highPass(double[] values, int half) {
        for (double v : values) {
            if (Double.isNaN(v)) {
                return null;
            }
        }
        double[] out = new double[values.length];
        for (int k = 0; k < values.length; k++) {
            double sum = 0.0;
            int n = 0;
            for (int i = Math.max(0, k - half); i <= Math.min(values.length - 1, k + half); i++) {
                sum += values[i];
                n++;
            }
            out[k] = values[k] - sum / n;
        }
        return out;
    }

    /**
     * Loose single blocks on the descent (polish round "afdaling"): columns whose top block stands higher than both
     * neighbours on an axis (a 1×1 knob or a ridge one block wide), in 12 boxes of 96 × 96 round the ring. {@code own}:
     * the treads alone (the descent's own ground, from the rim to the river's beach); otherwise the whole ground, without
     * the columns within 2 blocks of a gorge, a basin's water or cut, or a spring (other features with their own shapes).
     */
    record Lumps(int count, int columns, String first) {
    }

    static Lumps protrusions(IslandField field, RingLayout layout, boolean own) {
        VerdantDescent descent = field.descent();
        int count = 0;
        int columns = 0;
        String first = "";
        for (int n = 0; n < 12; n++) {
            double angle = n * 2.0 * Math.PI / 12.0 + 0.11;
            double[] mid = VerdantDescent.onRay(layout, angle, VerdantDescent.zoneStart(layout) + 135.0);
            int x0 = (int) Math.floor(mid[0]) - 48;
            int z0 = (int) Math.floor(mid[1]) - 48;
            int size = 96;
            int[][] top = new int[size + 2][size + 2];
            boolean[][] skip = new boolean[size + 2][size + 2];
            boolean[][] modified = new boolean[size + 2][size + 2];
            VerdantDescent.Point p = new VerdantDescent.Point();
            for (int i = 0; i < size + 2; i++) {
                for (int j = 0; j < size + 2; j++) {
                    int x = x0 - 1 + i;
                    int z = z0 - 1 + j;
                    descent.locate(x, z, p);
                    boolean descentProper = p.kind == VerdantDescent.Point.DESCENT
                            && p.u < p.profile.bankTop() - 1.0 && p.u > p.rimAt + 1.0;
                    skip[i][j] = !descentProper;
                    if (own) {
                        top[i][j] = (int) Math.ceil(p.treads) - 1;
                    } else {
                        IslandField.Column c = field.column(x, z);
                        double surface = c.surface();
                        modified[i][j] = c.gorgeZone != null || c.lakeTop() != Integer.MIN_VALUE || c.springY() != Integer.MIN_VALUE
                                || Math.abs(surface - p.treads) > 1e-6;
                        top[i][j] = (int) Math.ceil(surface) - 1;
                    }
                }
            }
            for (int i = 1; i <= size; i++) {
                for (int j = 1; j <= size; j++) {
                    if (skip[i][j]) {
                        continue;
                    }
                    if (!own && near(modified, i, j, 2)) {
                        continue;
                    }
                    columns++;
                    if (loose(top, i, j)) {
                        int h = top[i][j];
                        count++;
                        if (first.length() < 200) {
                            first += (x0 - 1 + i) + " " + h + " " + (z0 - 1 + j) + "; ";
                        }
                    }
                }
            }
        }
        return new Lumps(count, columns, first);
    }

    /**
     * A loose block: a top block that stands over both neighbours on an axis and under none, alone (over all four) or in
     * a ridge one block wide (a neighbour on the same level is such a block too). Not the tooth where a curved stair line
     * crosses the block grid (one column further out than both its neighbours, but joined to its own level behind it).
     */
    static boolean loose(int[][] top, int i, int j) {
        if (!ridge(top, i, j)) {
            return false;
        }
        int h = top[i][j];
        int[][] around = {{i - 1, j}, {i + 1, j}, {i, j - 1}, {i, j + 1}};
        boolean alone = true;
        for (int[] n : around) {
            if (top[n[0]][n[1]] == h) {
                alone = false;
                if (ridge(top, n[0], n[1])) {
                    return true;
                }
            }
        }
        return alone;
    }

    /** Over both neighbours on an axis and under none of the four. */
    private static boolean ridge(int[][] top, int i, int j) {
        if (i < 1 || j < 1 || i + 1 >= top.length || j + 1 >= top[i].length) {
            return false;
        }
        int h = top[i][j];
        if (h < top[i - 1][j] || h < top[i + 1][j] || h < top[i][j - 1] || h < top[i][j + 1]) {
            return false;
        }
        return h > top[i - 1][j] && h > top[i + 1][j] || h > top[i][j - 1] && h > top[i][j + 1];
    }

    private static boolean near(boolean[][] flags, int i, int j, int reach) {
        for (int a = Math.max(0, i - reach); a <= Math.min(flags.length - 1, i + reach); a++) {
            for (int b = Math.max(0, j - reach); b <= Math.min(flags[a].length - 1, j + reach); b++) {
                if (flags[a][b]) {
                    return true;
                }
            }
        }
        return false;
    }
}
