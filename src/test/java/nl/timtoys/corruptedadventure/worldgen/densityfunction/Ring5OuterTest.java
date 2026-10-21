package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ring 5's outer band and outer edge (84, V2-A, V3-A, V4-A): the break-up from the band line to the edge, the empty edge,
 * the floes' tops and undersides, the floating chunks. On a polar raster of the test seed, castles left out.
 */
class Ring5OuterTest {
    private static final RingLayout LAYOUT = Ring5FieldTest.LAYOUT;
    private static Ring5Field field;
    /** Per sampled column: effective distance, outer progress, break-up, top, bottom (NaN over the void), spans. */
    private static final List<double[]> SAMPLES = new ArrayList<>();

    @BeforeAll
    static void sample() {
        field = Ring5FieldTest.field();
        Ring5Band band = Ring5Band.of(LAYOUT);
        for (int radius = 8700; radius <= 10200; radius += 6) {
            for (int k = 0; k < 720; k++) {
                double angle = 2.0 * Math.PI * (k + 0.37) / 720.0;
                int x = (int) Math.round(radius * Math.cos(angle));
                int z = (int) Math.round(radius * Math.sin(angle));
                if (nearCastle(x, z)) {
                    continue;
                }
                double effective = LAYOUT.effectiveDistance(x, z);
                double signed = band.signedDistance(x, z);
                Ring5Field.Column c = field.column(x, z);
                SAMPLES.add(new double[]{effective, Ring5Outer.progress(effective, signed, 9700.0), c.breakup(), c.top(), c.bottom(),
                        c.spans, x, z});
            }
        }
    }

    private static boolean nearCastle(int x, int z) {
        for (Ring5Field.CastleSite site : field.castleSites()) {
            if (site.distance(x, z) <= site.radius() + site.belt() + 20) {
                return true;
            }
        }
        return false;
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private static double median(List<Double> values) {
        double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        return sorted[sorted.length / 2];
    }

    /** ~0 on the band line, 0.80–0.90 on average just inside the edge (9,600–9,700), rising strip by strip of 100. */
    @Test
    void breakupRisesFromTheLineToTheEdge() {
        List<Double> line = new ArrayList<>();
        List<Double> edge = new ArrayList<>();
        for (double[] s : SAMPLES) {
            if (s[1] >= 0.0 && s[1] < 0.03) {
                line.add(s[2]);
            }
            if (s[0] >= 9600.0 && s[0] < 9700.0) {
                edge.add(s[2]);
            }
        }
        System.out.printf("breakup: line %.3f (%d), 9600-9700 %.3f (%d)%n", mean(line), line.size(), mean(edge), edge.size());
        assertTrue(mean(line) < 0.05, "at the band line " + mean(line));
        assertTrue(mean(edge) >= 0.80 && mean(edge) <= 0.90, "at the edge " + mean(edge));
        double previous = -1.0;
        StringBuilder strips = new StringBuilder();
        for (int from = 8800; from < 9700; from += 100) {
            List<Double> strip = new ArrayList<>();
            for (double[] s : SAMPLES) {
                if (s[0] >= from && s[0] < from + 100) {
                    strip.add(s[2]);
                }
            }
            double m = mean(strip);
            strips.append(String.format(" %d:%.3f", from, m));
            assertTrue(m >= previous - 0.005, "strip " + from + ": " + m + " after " + previous);
            previous = m;
        }
        System.out.println("breakup per strip:" + strips);
    }

    /** V2-A: from ~10,050 on the edge is empty: no ground, no chunk, no deck. */
    @Test
    void theOuterEdgeIsEmptyFrom10050() {
        double empty = Ring5Field.emptyFrom(LAYOUT);
        assertEquals(10050.0, empty, 1e-9);
        int checked = 0;
        for (double[] s : SAMPLES) {
            if (s[0] >= empty) {
                checked++;
                assertTrue(Double.isNaN(s[3]) && s[5] == 0, "something at " + s[6] + ", " + s[7]);
                assertEquals(1.0, s[2]);
                for (int y = -64; y < 320; y += 3) {
                    assertTrue(field.terrain((int) s[6], y, (int) s[7]) < 0.0);
                }
            }
        }
        assertTrue(checked > 1000);
    }

    /**
     * V3-A, V4-A: floe tops Y 70–110; undersides around −40 at the band line and Y 20–40 at the edge, never below −56 and
     * never thinner than 6 blocks.
     */
    @Test
    void floesFloatFromMinus40AtTheLineTo20To40AtTheEdge() {
        List<Double> lineBottoms = new ArrayList<>();
        List<Double> edgeBottoms = new ArrayList<>();
        for (double[] s : SAMPLES) {
            if (Double.isNaN(s[3]) || s[1] < 0.0) {
                continue;
            }
            assertTrue(s[3] >= 70.0 && s[3] <= 110.0 + 1e-9 || s[1] < 0.05, "top " + s[3] + " at " + s[6] + ", " + s[7]);
            assertTrue(s[4] >= Ring5Outer.BOTTOM_MIN && s[3] - s[4] >= Ring5Outer.MIN_THICKNESS - 1e-9, "bottom " + s[4]);
            if (s[1] < 0.05) {
                lineBottoms.add(s[4]);
            } else if (s[1] > 0.95 && s[1] <= 1.0) {
                edgeBottoms.add(s[4]);
            }
        }
        System.out.printf("bottoms: line median %.1f (%d), edge median %.1f (%d)%n", median(lineBottoms), lineBottoms.size(),
                median(edgeBottoms), edgeBottoms.size());
        assertTrue(median(lineBottoms) >= -48.0 && median(lineBottoms) <= -30.0, "line " + median(lineBottoms));
        assertTrue(median(edgeBottoms) >= 15.0 && median(edgeBottoms) <= 40.0, "edge " + median(edgeBottoms));
    }

    /** Floating chunks: Y 40–160, Ø 6–40, tilt ≤ 20°, never smaller than 3 in any direction; smaller towards the edge. */
    @Test
    void fragmentsStayInTheirSizesAndShrinkOutwards() {
        List<Ring5Field.Fragment> fragments = field.fragmentsWithin(0, 0, 10200, 10200);
        List<Double> inner = new ArrayList<>();
        List<Double> outer = new ArrayList<>();
        double empty = Ring5Field.emptyFrom(LAYOUT);
        for (Ring5Field.Fragment f : fragments) {
            assertTrue(f.radius() >= 3.0 && f.radius() <= 20.0, "radius " + f.radius());
            assertTrue(f.top() - f.bottom() >= 3, "thickness " + (f.top() - f.bottom()));
            double lean = Math.tan(Math.toRadians(f.tilt())) * f.radius();
            assertTrue(f.top() - lean - 3.0 >= 40.0 - 0.5 && f.top() + lean <= 160.5, "Y " + f.bottom() + "–" + f.top());
            assertTrue(f.tilt() <= 20.0);
            double effective = LAYOUT.effectiveDistance(f.x(), f.z());
            assertTrue(effective < empty, "fragment beyond the empty edge");
            if (f.radius() > Ring5Outer.STONE_RADIUS || f.tilt() != 0.0) {
                (effective < 9300.0 ? inner : outer).add(f.radius());
            }
        }
        System.out.printf("fragments: %d, mean radius inner %.1f (%d), outer %.1f (%d)%n", fragments.size(), mean(inner), inner.size(),
                mean(outer), outer.size());
        assertTrue(inner.size() > 50 && outer.size() > 50);
        assertTrue(mean(inner) > mean(outer) + 2.0);
        // A chunk's rim is at least 3 thick: every covered column gets a span of 3 or more.
        int checked = 0;
        for (Ring5Field.Fragment f : fragments.subList(0, Math.min(200, fragments.size()))) {
            Ring5Field.Column column = field.column((int) Math.floor(f.x()), (int) Math.floor(f.z()));
            for (int k = 0; k < column.spans; k++) {
                assertTrue(column.spanTop[k] - column.spanBottom[k] >= 3);
                checked++;
            }
        }
        assertTrue(checked > 0);
    }
}
