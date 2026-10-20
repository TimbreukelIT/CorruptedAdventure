package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The base terrain of ring 4 (package 60) on the test seed: low, about half water, landforms per kind of region. */
class Ring4ReliefTest {
    private static IslandField field;
    /** Columns across ring 4 proper: x, z, surface, water axis, growth axis, band, river. */
    private static final List<double[]> COLUMNS = new ArrayList<>();

    @BeforeAll
    static void sample() {
        field = IslandFieldTest.field();
        Ring4Relief relief = field.relief();
        Ring4Band band = Ring4Band.of(IslandFieldTest.LAYOUT);
        for (int angle = 0; angle < 360; angle += 3) {
            double cos = Math.cos(Math.toRadians(angle));
            double sin = Math.sin(Math.toRadians(angle));
            for (int r = 6100; r <= 7900; r += 29) {
                int x = (int) Math.round(r * cos);
                int z = (int) Math.round(r * sin);
                var position = IslandFieldTest.LAYOUT.sampleOverworld(x, z);
                if (position.inTransition() || position.ring() != Ring.RING_4
                        || Ring4Sundered.inRim(IslandFieldTest.LAYOUT, IslandFieldTest.LAYOUT.effectiveDistance(x, z))) {
                    // Not the rim before the zone 4→5 either (5a): its ground is raised to stop the sea.
                    continue;
                }
                IslandField.Column c = field.column(x, z);
                COLUMNS.add(new double[]{x, z, c.surface(), relief.waterAxis(x, z), relief.growthAxis(x, z),
                        band.value(x, z), c.river, c.height()});
            }
        }
    }

    private static double waterShare(java.util.function.Predicate<double[]> filter) {
        int water = 0;
        int all = 0;
        for (double[] c : COLUMNS) {
            if (filter.test(c)) {
                all++;
                water += c[2] < Ring4Relief.SEA ? 1 : 0;
            }
        }
        return all == 0 ? Double.NaN : water / (double) all;
    }

    @Test
    void aboutHalfIsWater() {
        double all = waterShare(c -> true);
        double mythic = waterShare(c -> c[5] < 0.0);
        double blighted = waterShare(c -> c[5] >= 0.0);
        System.out.printf("water share: all %.3f, mythic %.3f, blighted %.3f (%d columns)%n", all, mythic, blighted, COLUMNS.size());
        double[] cuts = {-0.45, -0.3, -0.15, -0.05, 0.0, 0.1, 0.2, 0.3, 0.45};
        StringBuilder axis = new StringBuilder("water axis below:");
        StringBuilder growth = new StringBuilder("growth axis below:");
        for (double cut : cuts) {
            axis.append(String.format(" %.2f=%.3f", cut, COLUMNS.stream().filter(c -> c[3] < cut).count() / (double) COLUMNS.size()));
            growth.append(String.format(" %.2f=%.3f", cut, COLUMNS.stream().filter(c -> c[4] < cut).count() / (double) COLUMNS.size()));
        }
        System.out.println(axis);
        System.out.println(growth);
        for (double[] range : new double[][]{{-9, -0.15}, {-0.15, 0.2}, {0.2, 9}}) {
            System.out.printf("water share for axis %.2f..%.2f: mythic %.3f, blighted %.3f%n", range[0], range[1],
                    waterShare(c -> c[3] >= range[0] && c[3] < range[1] && c[5] < 0), waterShare(c -> c[3] >= range[0] && c[3] < range[1] && c[5] >= 0));
        }
        assertTrue(all > 0.42 && all < 0.58, "water share " + all);
        assertTrue(mythic > 0.35 && mythic < 0.65, "mythic " + mythic);
        assertTrue(blighted > 0.35 && blighted < 0.65, "blighted " + blighted);
    }

    @Test
    void lowAroundTheSeaWithHighlandsAndDeepChannels() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int high = 0;
        double holes = Double.MAX_VALUE;
        for (double[] c : COLUMNS) {
            // The base terrain (the islands' holes come on top, 61).
            min = Math.min(min, c[7]);
            max = Math.max(max, c[7]);
            high += c[7] > 85.0 ? 1 : 0;
            holes = Math.min(holes, c[2]);
        }
        assertTrue(holes >= IslandShape.HOLE_FLOOR_MIN, "deepest hole " + holes);
        System.out.printf("surface %.1f .. %.1f, above 85: %.3f%n", min, max, high / (double) COLUMNS.size());
        assertTrue(min > 30.0, "min " + min);
        assertTrue(max < 125.0 && max > 90.0, "max " + max);
        double highShare = high / (double) COLUMNS.size();
        assertTrue(highShare > 0.02 && highShare < 0.3, "highlands " + highShare);
    }

    /**
     * Each landform under its biome (W-biomes' thresholds): open water in the lagoon, the lotus shallows and the
     * drowned forest; land in the cascades, the fjord plateaus and the peat; the swamp and the reed marsh around the sea.
     */
    @Test
    void landformsFollowTheBiomeThresholds() {
        double b = Ring4Relief.BLEND;
        double split = Ring4Relief.WATER_SPLIT;
        double growth = Ring4Relief.MYTHIC_GROWTH;
        java.util.function.Predicate<double[]> mythic = c -> c[5] <= -1.0;
        java.util.function.Predicate<double[]> blighted = c -> c[5] >= 0.5;
        double lagoon = waterShare(mythic.and(c -> c[4] < growth - b && c[3] > b));
        double lotus = waterShare(mythic.and(c -> c[4] > growth + b && c[3] > split + b));
        double swamp = waterShare(mythic.and(c -> c[4] > growth + b && Math.abs(c[3]) < split - b));
        double cascades = waterShare(mythic.and(c -> c[4] > growth + b && c[3] < -split - b && c[6] < 0.01));
        double fjords = waterShare(mythic.and(c -> c[4] < growth - b && c[3] < -b && c[6] < 0.01));
        double drowned = waterShare(blighted.and(c -> c[3] > split + b));
        double reed = waterShare(blighted.and(c -> c[3] < split - b && c[4] > b));
        double peat = waterShare(blighted.and(c -> c[3] < split - b && c[4] < -b && c[6] < 0.01));
        System.out.printf("water share per biome: lagoon %.2f lotus %.2f swamp %.2f cascades %.2f fjords %.2f drowned %.2f reed %.2f peat %.2f%n",
                lagoon, lotus, swamp, cascades, fjords, drowned, reed, peat);
        assertTrue(lagoon > 0.9 && lotus > 0.9 && drowned > 0.9, "open water");
        assertTrue(cascades < 0.1 && fjords < 0.35 && peat < 0.2, "land");
        assertTrue(swamp > 0.25 && swamp < 0.75, "swamp " + swamp);
        assertTrue(reed > 0.2 && reed < 0.75, "reed " + reed);
    }

    /** The mythical half of the band's transition (always mist_fjords) is never a lagoon: a low plateau. */
    @Test
    void theMistRimIsNoLagoon() {
        int rim = 0;
        int deep = 0;
        for (double[] c : COLUMNS) {
            if (c[5] > -0.5 && c[5] < -0.1 && c[6] < 0.01) {
                rim++;
                deep += c[2] < 55.0 && field.column((int) c[0], (int) c[1]).fjord < 0.01 ? 1 : 0;
            }
        }
        assertTrue(rim > 5, "rim columns " + rim);
        assertTrue(deep <= rim / 10, deep + " of " + rim);
    }

    /** Rivers lie at the sea level: their beds below it, so they hold the sea and can be sailed. */
    @Test
    void riverBedsLieBelowTheSea() {
        int rivers = 0;
        for (double[] c : COLUMNS) {
            if (c[6] > 0.999) {
                rivers++;
                assertTrue(c[2] <= Ring4Relief.RIVER_BED + 0.1, "river at " + c[0] + ", " + c[1] + ": " + c[2]);
            }
        }
        assertTrue(rivers > 20, "rivers " + rivers);
    }

    /**
     * The constants that turn the river and fjord lines into blocks are the lines' median slope near their middle:
     * then a valley's width in blocks is what the code says.
     */
    @Test
    void lineGradientsMatchTheirConstants() {
        Ring4Relief relief = field.relief();
        List<Double> rivers = new ArrayList<>();
        List<Double> fjords = new ArrayList<>();
        for (double[] c : COLUMNS) {
            double x = c[0];
            double z = c[1];
            double r = relief.riverLineAt(x, z);
            if (r < 0.15) {
                rivers.add(Math.hypot(relief.riverLineAt(x + 1, z) - r, relief.riverLineAt(x, z + 1) - r));
            }
            double f = relief.fjordLineAt(x, z);
            if (f < 0.15) {
                fjords.add(Math.hypot(relief.fjordLineAt(x + 1, z) - f, relief.fjordLineAt(x, z + 1) - f));
            }
        }
        double river = median(rivers);
        double fjord = median(fjords);
        System.out.printf("median line slope: rivers %.5f (%d), fjords %.5f (%d)%n", river, rivers.size(), fjord, fjords.size());
        assertEquals(Ring4Relief.RIVER_GRADIENT, river, 0.25 * river);
        assertEquals(Ring4Relief.FJORD_GRADIENT, fjord, 0.25 * fjord);
    }

    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compare);
        return sorted.get(sorted.size() / 2);
    }

    /**
     * Review 60 #1: rivers through high land run in valleys that widen with the land's height, not in slots: 12 blocks
     * from the bed's edge the ground has risen at most a valley side's worth.
     */
    @Test
    void riversRunInValleysNotSlots() {
        List<Double> rises = new ArrayList<>();
        double far = Ring4Relief.RIVER_HALF + 12.0;
        for (double[] c : COLUMNS) {
            if (c[6] < 0.999) {
                continue;
            }
            double highest = -Double.MAX_VALUE;
            for (int k = 0; k < 8; k++) {
                double t = k * Math.PI / 4.0;
                highest = Math.max(highest,
                        field.column((int) Math.round(c[0] + far * Math.cos(t)), (int) Math.round(c[1] + far * Math.sin(t))).height());
            }
            rises.add(highest - Ring4Relief.RIVER_BED);
        }
        assertTrue(rises.size() > 20, "beds " + rises.size());
        // The line's slope varies around its median: the typical bed sees a valley side, never a slot's wall.
        double rise = median(rises);
        System.out.printf("median rise %.1f blocks at %.1f from the bed's middle%n", rise, far);
        assertTrue(rise <= 1.6 * 12.0 * Ring4Relief.RIVER_SLOPE + 4.0, "median rise " + rise);
    }

    @Test
    void terracesHaveFlatTreadsAndSteepRisers() {
        double step = Ring4Relief.TERRACE_STEP;
        double tread = Ring4Relief.TERRACE_TREAD * step;
        assertEquals(0.0, Ring4Relief.terrace(0.0));
        assertEquals(step, Ring4Relief.terrace(step), 1e-9);
        assertEquals(step, Ring4Relief.terrace(step + tread * 0.5), 1e-9);
        assertEquals(step, Ring4Relief.terrace(step + tread), 1e-9);
        assertEquals(2.0 * step, Ring4Relief.terrace(2.0 * step), 1e-9);
        double riser = Ring4Relief.terrace(step + tread + 0.5 * (step - tread));
        assertEquals(1.5 * step, riser, 1e-9, "a riser's middle lies halfway up");
        for (double v = 0.0; v < 60.0; v += 0.25) {
            assertTrue(Ring4Relief.terrace(v + 0.25) >= Ring4Relief.terrace(v));
        }
        // Q5: tread 60 %, riser 40 %, rounded at its edge and its foot (flat slope at both ends of the riser).
        assertEquals(0.6, Ring4Relief.TERRACE_TREAD);
        double e = 0.01;
        assertEquals(step, Ring4Relief.terrace(step + tread + e), 1e-4, "round edge");
        assertEquals(2.0 * step, Ring4Relief.terrace(2.0 * step - e), 1e-4, "round foot");
        // Detail only on the risers: none on a tread.
        assertEquals(0.0, Ring4Relief.riserBump(step + 0.5 * tread));
        assertEquals(1.0, Ring4Relief.riserBump(step + tread + 0.5 * (step - tread)), 1e-12);
    }

    // ---------------------------------------------------------------- the 4-block grid (heuvelfix, 2b)

    /**
     * The terrain as the stack renders it against the exact relief, over windows across ring 4 proper: the surface
     * between the corners of the 4-block grid is the bilinear blend of the corners' values (the {@code interpolated}
     * stack), except where {@code thin} decides per block. The measurement of the plan (verdant-falls.md §1.2) as a
     * lasting check.
     */
    static final class GridSurvey {
        static final int WINDOWS = 400;
        static final int SIZE = 48;
        /** Cascade core (weight > 0.9, no river): |exact − bilinear| and the top block's difference. */
        final List<Double> coreError = new ArrayList<>();
        final List<Integer> coreTop = new ArrayList<>();
        /**
         * The same where the whole cell lies in the core: the terraces themselves, without the seams where the growth
         * axis runs so steep that the cascades meet another landform within a few blocks (a slope the grid smooths).
         */
        final List<Double> cellError = new ArrayList<>();
        final List<Integer> cellTop = new ArrayList<>();
        /** Cascade area (weight > 0.1, no river): the exact slope. */
        final List<Double> cascadeSlope = new ArrayList<>();
        /** The same for the cascade core (weight > 0.9) and the blend (0.1..0.9). */
        final List<Double> coreSlope = new ArrayList<>();
        final List<Double> blendSlope = new ArrayList<>();
        /** Cascade core columns whose exact height lies exactly on a tread's level. */
        int coreLevel;
        /** Fjord rim (fjord weight > 0.9, |cut − plateau| ≤ 4, no river or hole): the rendered top's difference. */
        final List<Integer> rimTop = new ArrayList<>();
        /** The wall (cut 4 or more below the plateau): how far the rendered top lies beyond the wall's roughness. */
        final List<Integer> wallTop = new ArrayList<>();
        /** The rough wall (roughness >= 3 blocks): the rendered top's difference from the smooth exact surface. */
        final List<Integer> roughTop = new ArrayList<>();
        /** The plateau just behind the rim (cut 4..12 above it): where a cell's corner may still lie on the wall. */
        final List<Integer> behindTop = new ArrayList<>();
        final List<Double> plateauBumps = new ArrayList<>();

        static GridSurvey run() {
            GridSurvey survey = new GridSurvey();
            Ring4Relief relief = field.relief();
            Ring4Band band = Ring4Band.of(IslandFieldTest.LAYOUT);
            java.util.Random random = new java.util.Random(4402);
            int windows = 0;
            while (windows < WINDOWS) {
                double angle = random.nextDouble() * 2.0 * Math.PI;
                double r = 6150.0 + random.nextDouble() * 1650.0;
                int x0 = (int) Math.round(r * Math.cos(angle)) & ~3;
                int z0 = (int) Math.round(r * Math.sin(angle)) & ~3;
                if (!ring4Proper(x0, z0) || !ring4Proper(x0 + SIZE, z0 + SIZE) || !ring4Proper(x0, z0 + SIZE) || !ring4Proper(x0 + SIZE, z0)) {
                    continue;
                }
                windows++;
                survey.window(relief, band, x0, z0);
            }
            return survey;
        }

        private static boolean ring4Proper(int x, int z) {
            var position = IslandFieldTest.LAYOUT.sampleOverworld(x, z);
            return !position.inTransition() && position.ring() == Ring.RING_4
                    && !Ring4Sundered.inRim(IslandFieldTest.LAYOUT, IslandFieldTest.LAYOUT.effectiveDistance(x, z));
        }

        private void window(Ring4Relief relief, Ring4Band band, int x0, int z0) {
            int n = SIZE + 1;
            Ring4Relief.Sample[][] samples = new Ring4Relief.Sample[n][n];
            double[][] h = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    Ring4Relief.Sample s = new Ring4Relief.Sample();
                    field.sampleRelief(x0 + i, z0 + j, s);
                    samples[i][j] = s;
                    h[i][j] = s.height;
                }
            }
            for (int i = 1; i < SIZE; i++) {
                for (int j = 1; j < SIZE; j++) {
                    Ring4Relief.Sample s = samples[i][j];
                    int ci = i & ~3;
                    int cj = j & ~3;
                    boolean riverFree = s.riverDepth == 0.0;
                    for (int[] c : new int[][]{{ci, cj}, {ci + 4, cj}, {ci, cj + 4}, {ci + 4, cj + 4}, {i - 1, j}, {i + 1, j}, {i, j - 1}, {i, j + 1}}) {
                        riverFree &= samples[c[0]][c[1]].riverDepth == 0.0;
                    }
                    if (!riverFree) {
                        continue;
                    }
                    double bilinear = bilinear(h, i, j);
                    if (s.cascade > 0.9) {
                        double error = Math.abs(h[i][j] - bilinear);
                        int topOff = Math.abs(top(h[i][j]) - top(bilinear));
                        coreError.add(error);
                        coreTop.add(topOff);
                        if (samples[ci][cj].cascade > 0.9 && samples[ci + 4][cj].cascade > 0.9 && samples[ci][cj + 4].cascade > 0.9
                                && samples[ci + 4][cj + 4].cascade > 0.9) {
                            cellError.add(error);
                            cellTop.add(topOff);
                        }
                        double rise = h[i][j] - Ring4Relief.SEA - 1.0;
                        coreLevel += Math.abs(rise - Ring4Relief.TERRACE_STEP * Math.round(rise / Ring4Relief.TERRACE_STEP)) < 1e-9 ? 1 : 0;
                    }
                    if (s.cascade > 0.1) {
                        double slope = Math.hypot(h[i + 1][j] - h[i - 1][j], h[i][j + 1] - h[i][j - 1]) / 2.0;
                        cascadeSlope.add(slope);
                        (s.cascade > 0.9 ? coreSlope : blendSlope).add(slope);
                    }
                    if (s.fjordLand > 0.9 && !Double.isNaN(s.fjordRim)) {
                        if (i % 8 == 0 && j % 8 == 0 && i >= 8 && j >= 8 && i <= SIZE - 8 && j <= SIZE - 8 && plateau(samples, i, j)) {
                            plateauBumps.add(spread(h, i, j));
                        }
                        Integer rendered = renderedTopDifference(x0 + i, z0 + j);
                        if (rendered != null) {
                            if (Math.abs(s.fjordRim) <= 4.0) {
                                rimTop.add(rendered);
                            } else if (s.fjordRim < -4.0) {
                                wallTop.add(Math.max(0, rendered - (int) Math.ceil(s.wallRough)));
                                if (s.wallRough >= 3.0) {
                                    roughTop.add(rendered);
                                }
                            } else if (s.fjordRim <= 12.0) {
                                behindTop.add(rendered);
                            }
                        }
                    }
                }
            }
        }

        /** Whether the 16 x 16 blocks around (i, j) are all fjord plateau (away from the cut and from rivers). */
        private static boolean plateau(Ring4Relief.Sample[][] samples, int i, int j) {
            for (int di = -8; di <= 8; di += 2) {
                for (int dj = -8; dj <= 8; dj += 2) {
                    Ring4Relief.Sample s = samples[i + di][j + dj];
                    if (s.fjordLand < 0.99 || !(s.fjordRim > 6.0) || s.riverDepth != 0.0) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * The bumps of the exact height over the 16 x 16 blocks around (i, j) (every second block): the standard
         * deviation around the best-fitting plane, so the plateau's slope does not count.
         */
        private static double spread(double[][] h, int i, int j) {
            // The offsets are symmetric about 0: the plane's coefficients separate.
            double sum = 0.0;
            double sx = 0.0;
            double sz = 0.0;
            double xx = 0.0;
            int count = 0;
            for (int di = -8; di <= 8; di += 2) {
                for (int dj = -8; dj <= 8; dj += 2) {
                    double v = h[i + di][j + dj];
                    sum += v;
                    sx += di * v;
                    sz += dj * v;
                    xx += di * di;
                    count++;
                }
            }
            double mean = sum / count;
            double ax = sx / xx;
            double az = sz / xx;
            double sq = 0.0;
            for (int di = -8; di <= 8; di += 2) {
                for (int dj = -8; dj <= 8; dj += 2) {
                    double r = h[i + di][j + dj] - mean - ax * di - az * dj;
                    sq += r * r;
                }
            }
            return Math.sqrt(sq / count);
        }

        private static double bilinear(double[][] h, int i, int j) {
            int ci = i & ~3;
            int cj = j & ~3;
            double tx = (i - ci) / 4.0;
            double tz = (j - cj) / 4.0;
            double a = h[ci][cj] + tx * (h[ci + 4][cj] - h[ci][cj]);
            double b = h[ci][cj + 4] + tx * (h[ci + 4][cj + 4] - h[ci][cj + 4]);
            return a + tz * (b - a);
        }

        /** The top solid block of a surface height (the stack's density is 0.125 · (surface − y)). */
        static int top(double surface) {
            return (int) Math.ceil(surface) - 1;
        }

        /**
         * |rendered top − exact top| of a column as the stack builds it (the grid's bilinear surface, unless
         * {@code thin} decides per block), or null for a column with an island, a hole, a lake or a spring.
         */
        static Integer renderedTopDifference(int x, int z) {
            IslandField.Column c = field.column(x, z);
            if (c.islandMask() > 0.0 || c.islandHole() > 0.0 || c.surface() != c.height() || c.lakeTop() != Integer.MIN_VALUE
                    || c.springY() != Integer.MIN_VALUE || c.caveLiftFrom() <= c.caveLiftTo() || !Double.isNaN(c.sinkholeFloor())) {
                return null;
            }
            return Math.abs(renderedTop(field, x, z) - top(c.surface()));
        }

        /** The top solid block of a column as the stack builds it (caves aside): {@code thin}, else the grid's surface. */
        static int renderedTop(IslandField field, int x, int z) {
            IslandField.Column c = field.column(x, z);
            double exact = c.surface();
            double rough = c.wallRough;
            double bilinear = field.gridSurface(x, z);
            int from = (int) Math.ceil(Math.max(exact + rough, bilinear)) + 3;
            int to = (int) Math.floor(Math.min(exact - rough, bilinear)) - 3;
            for (int y = from; y >= to; y--) {
                double thin = field.thin(x, y, z);
                if (thin >= 0.5 || thin >= -0.5 && y < bilinear) {
                    return y;
                }
            }
            return to - 1;
        }
    }

    private static GridSurvey survey;

    private static synchronized GridSurvey survey() {
        if (survey == null) {
            survey = GridSurvey.run();
            GridSurvey g = survey;
            System.out.printf("cascade core %d columns: |error| mean %.3f p99 %.3f max %.3f, top off >=1 %.4f >=2 %.4f; whole cells %d: p99 %.3f max %.3f >=2 %.4f; on a level %.3f%n",
                    g.coreError.size(), mean(g.coreError), quantile(g.coreError, 0.99), quantile(g.coreError, 1.0), share(g.coreTop, 1),
                    share(g.coreTop, 2), g.cellError.size(), quantile(g.cellError, 0.99), quantile(g.cellError, 1.0), share(g.cellTop, 2),
                    g.coreLevel / (double) g.coreError.size());
            System.out.printf("level (slope < 0.05): cascade area %.3f (%d), core %.3f, blend %.3f; steep (>= 0.6) %.3f%n", flat(g.cascadeSlope),
                    g.cascadeSlope.size(), flat(g.coreSlope), flat(g.blendSlope),
                    g.cascadeSlope.stream().filter(v -> v >= 0.6).count() / (double) g.cascadeSlope.size());
            System.out.printf("fjord rim %d columns: top off >=1 %.4f >=2 %.4f; wall %d: beyond its roughness >=1 %.4f >=2 %.4f; rough wall %d: off >=2 %.4f >=4 %.4f; behind the rim %d: >=1 %.4f >=2 %.4f; plateau bumps %d: median %.3f p90 %.3f max %.3f%n",
                    g.rimTop.size(), share(g.rimTop, 1), share(g.rimTop, 2), g.wallTop.size(), share(g.wallTop, 1), share(g.wallTop, 2),
                    g.roughTop.size(), share(g.roughTop, 2), share(g.roughTop, 4),
                    g.behindTop.size(), share(g.behindTop, 1), share(g.behindTop, 2),
                    g.plateauBumps.size(), quantile(g.plateauBumps, 0.5), quantile(g.plateauBumps, 0.9), quantile(g.plateauBumps, 1.0));
        }
        return survey;
    }

    private static double flat(List<Double> slopes) {
        return slopes.stream().filter(v -> v < 0.05).count() / (double) slopes.size();
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(v -> v).average().orElse(Double.NaN);
    }

    private static double quantile(List<Double> values, double q) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compare);
        return sorted.get((int) Math.min(sorted.size() - 1, Math.floor(q * sorted.size())));
    }

    private static double share(List<Integer> values, int atLeast) {
        return values.stream().filter(v -> v >= atLeast).count() / (double) values.size();
    }

    /**
     * Q5: the risers are wide and round enough for the grid: inside the terraces it misses the exact height by at most
     * half a block for 99 % of the columns and practically never puts the top block 2 off (before: p99 0.98, max 2.6).
     */
    @Test
    void terracesFollowTheGrid() {
        GridSurvey g = survey();
        assertTrue(g.cellError.size() > 20_000, "cells " + g.cellError.size());
        assertTrue(quantile(g.cellError, 0.99) <= 0.6, "p99 " + quantile(g.cellError, 0.99));
        // The rolling under the terraces (2026-10-20) steepens the rise in rare spots: a narrower riser (max 1.5 → 2).
        assertTrue(quantile(g.cellError, 1.0) <= 2.0, "max " + quantile(g.cellError, 1.0));
        // An error over 1 can still put the top block 2 off where it straddles two whole blocks: once in ~66,000.
        assertTrue(share(g.cellTop, 2) <= 0.0001, "top block 2 off " + share(g.cellTop, 2));
        // The whole core, with the steep seams to other landforms: rare.
        assertTrue(quantile(g.coreError, 0.99) <= 0.6, "core p99 " + quantile(g.coreError, 0.99));
        assertTrue(share(g.coreTop, 2) <= 0.002, "core top 2 off " + share(g.coreTop, 2));
    }

    /**
     * H2/H4: treads are level (the detail noise only on the risers, the steps after the mix): most of the core lies
     * exactly on a tread's level, and the blends with other landforms are far more level than before (0.41); the
     * narrower treads of Q5 cost some of the core's level ground (before 0.85).
     */
    @Test
    void treadsAreLevel() {
        GridSurvey g = survey();
        assertTrue(g.coreLevel / (double) g.coreError.size() > 0.6, "on a level " + g.coreLevel / (double) g.coreError.size());
        assertTrue(flat(g.coreSlope) > 0.72, "core " + flat(g.coreSlope));
        // The relief of the neighbours (swamp undulation, fjord hills; 2026-10-20) tilts the blends a little (0.52 → 0.49).
        assertTrue(flat(g.blendSlope) > 0.45, "blend " + flat(g.blendSlope));
        assertTrue(flat(g.cascadeSlope) > 0.66, "cascade area " + flat(g.cascadeSlope));
    }

    /**
     * Q6: a fjord's rim and wall are cut per block ({@code thin}), so the stack shows them exactly, no sawtooth on the
     * grid (before: 38 % of the rim's columns 2 or more blocks off, up to 6). The wall's roughness (2026-10-20) moves
     * its face on purpose, within its own range; the rim stays as cut.
     */
    @Test
    void fjordRimsAreExactPerBlock() {
        GridSurvey g = survey();
        assertTrue(g.rimTop.size() > 500 && g.wallTop.size() > 5000, "rim " + g.rimTop.size() + ", wall " + g.wallTop.size());
        assertTrue(share(g.rimTop, 1) <= 0.01, "rim off " + share(g.rimTop, 1));
        assertEquals(0.0, share(g.rimTop, 2), "rim 2 off");
        assertTrue(share(g.wallTop, 2) <= 0.002, "wall 2 off beyond its roughness " + share(g.wallTop, 2));
        assertTrue(share(g.behindTop, 1) <= 0.01, "behind the rim off " + share(g.behindTop, 1));
    }

    /** A2-3 (2026-10-20): the fjords' walls are rough (ledges, jutting rock), not a straight striped face. */
    @Test
    void fjordWallsAreRough() {
        GridSurvey g = survey();
        assertTrue(g.roughTop.size() > 1000, "rough wall columns " + g.roughTop.size());
        assertTrue(share(g.roughTop, 2) > 0.2, "rough wall 2 off " + share(g.roughTop, 2));
    }

    /**
     * A2-3 (2026-10-20): hills and rock knobs on the fjord plateaus (before: median 0.05, the plateaus read flat), within
     * the small relief of A1-2.
     */
    @Test
    void fjordPlateausHaveHillsAndKnobs() {
        GridSurvey g = survey();
        assertTrue(g.plateauBumps.size() > 50, "plateau samples " + g.plateauBumps.size());
        assertTrue(quantile(g.plateauBumps, 0.5) > 0.25, "median " + quantile(g.plateauBumps, 0.5));
        assertTrue(quantile(g.plateauBumps, 1.0) < 0.5 * (Ring4Relief.FJORD_HILL + Ring4Relief.FJORD_KNOB),
                "max " + quantile(g.plateauBumps, 1.0));
    }

    /**
     * A2-1 (2026-10-20): the moors get hummocks and low ridges, the hollows between them stay above the sea (no new
     * water: A1-4), and nothing rises more than the small relief of A1-2.
     */
    @Test
    void peatHasHummocksAboveTheSea() {
        Ring4Relief relief = field.relief();
        int peat = 0;
        int raised = 0;
        double low = Double.MAX_VALUE;
        double most = 0.0;
        for (double[] c : COLUMNS) {
            Ring4Relief.Sample s = new Ring4Relief.Sample();
            field.sampleRelief(c[0], c[1], s);
            if (s.peat < 0.99 || s.riverDepth > 0.0) {
                continue;
            }
            peat++;
            double hummocks = relief.hummocks(c[0], c[1]);
            raised += hummocks > 2.0 ? 1 : 0;
            most = Math.max(most, hummocks);
            low = Math.min(low, s.height);
        }
        System.out.printf("peat %d columns: hummocks > 2 %.3f, highest %.2f, lowest ground %.2f%n", peat, raised / (double) peat, most, low);
        assertTrue(peat > 100, "peat columns " + peat);
        assertTrue(raised / (double) peat > 0.25 && raised / (double) peat < 0.8, "raised " + raised / (double) peat);
        assertTrue(most <= Ring4Relief.MOUND_MAX + Ring4Relief.RIDGE, "highest " + most);
        assertTrue(low >= Ring4Relief.SEA + 1.0, "lowest " + low);
    }

    @Test
    void sinkholesStayInsideTheirCell() {
        long salt = field.salt();
        int found = 0;
        for (int i = -20; i < 20; i++) {
            for (int j = -20; j < 20; j++) {
                double[] hole = Ring4Relief.sinkhole(i, j, salt);
                if (hole == null) {
                    continue;
                }
                found++;
                int cell = Ring4Relief.SINKHOLE_CELL;
                assertTrue(hole[0] - hole[2] >= i * cell && hole[0] + hole[2] <= (i + 1) * cell);
                assertTrue(hole[1] - hole[2] >= j * cell && hole[1] + hole[2] <= (j + 1) * cell);
            }
        }
        assertTrue(found > 500 && found < 1100, "sinkholes " + found);
    }

    /** The preliminary surface never lies above the real surface of a quart (else lakes and rivers get air). */
    @Test
    void initialHeightIsALowerBoundOfTheQuart() {
        int checked = 0;
        for (double[] c : COLUMNS) {
            int qx = (int) c[0] & ~3;
            int qz = (int) c[1] & ~3;
            double initial = field.initialHeight(qx, qz);
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    IslandField.Column column = field.column(qx + dx, qz + dz);
                    double surface = Double.isNaN(column.sinkholeFloor()) ? column.surface() : Math.min(column.surface(), column.sinkholeFloor());
                    assertTrue(initial <= surface, (qx + dx) + ", " + (qz + dz) + ": " + initial + " > " + surface);
                    checked++;
                }
            }
        }
        assertTrue(checked > 1000);
    }
}
