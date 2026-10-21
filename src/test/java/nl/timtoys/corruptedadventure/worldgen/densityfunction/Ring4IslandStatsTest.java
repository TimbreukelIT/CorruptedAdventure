package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Measurements of the islands of ring 4 on the test seed (sparring 2026-10-20, B1/B2): per fifth of the ring's width
 * the islands by kind, the share of the sky they cover, stacked columns, lakes, holes on high ground without a lake,
 * pairs, arches and falls. Prints only (the tuning loop reads it before and after a change); the rules themselves are
 * checked in {@link Ring4IslandsTest} and {@link Ring4LandmarksTest}. Tagged {@code stats}: not part of {@code build};
 * run it with {@code ./gradlew test -PincludeStats --tests '*Ring4IslandStatsTest'}.
 */
@Tag("stats")
class Ring4IslandStatsTest {
    private static final int BINS = 5;

    @Test
    void printIslandStatistics() {
        IslandField field = IslandFieldTest.field();
        int cellSize = field.settings().cellSize();
        List<IslandShape> shapes = new ArrayList<>();
        for (int i = 5900 / cellSize; i <= 8100 / cellSize; i++) {
            for (int j = -1400 / cellSize; j <= 1400 / cellSize; j++) {
                shapes.addAll(field.cell(i, j));
            }
        }
        int[][] kinds = new int[BINS][4];
        double[] diameters = new double[BINS];
        int[] lakes = new int[BINS];
        int[] dryHigh = new int[BINS];
        int[] falls = new int[BINS];
        int[] lifts = new int[BINS];
        int[] sky140 = new int[BINS];
        double[] bottoms = new double[BINS];
        int pairs = 0;
        int arches = 0;
        int debris = 0;
        int[] archStages = new int[5];
        Map<IslandShape, Integer> archesPerIsland = new HashMap<>();
        for (IslandShape s : shapes) {
            int bin = bin(s.x(), s.z());
            if (bin < 0) {
                continue;
            }
            kinds[bin][s.kind()]++;
            diameters[bin] += 2.0 * s.a() * Math.sqrt(s.stretch());
            bottoms[bin] += s.lowest() - Math.max(63.0, s.groundY());
            sky140[bin] += s.top() >= 140.0 ? 1 : 0;
            IslandField.Details details = field.details(s);
            lakes[bin] += details.lakeTop() != Integer.MIN_VALUE ? 1 : 0;
            // A hole on high ground whose floor stays above the sea and that has no lake of its own: a dry pit.
            dryHigh[bin] += details.lakeTop() == Integer.MIN_VALUE && s.kind() != IslandShape.TREE
                    && s.groundY() - s.thickness() * 0.25 > 63.0 ? 1 : 0;
            falls[bin] += details.falls().size();
            lifts[bin] += details.lift() != null ? 1 : 0;
            debris += field.landmarks().debris(s).size();
            for (IslandArches.Pair pair : field.landmarks().candidatePairs(s)) {
                boolean big = pair.first().kind() != IslandShape.CLOD && pair.second().kind() != IslandShape.CLOD;
                boolean gap = pair.gap() >= IslandArches.ARCH_MIN_GAP && pair.gap() <= IslandArches.ARCH_MAX_GAP;
                boolean step = Math.abs(pair.a().top() - pair.b().top())
                        <= Math.min(IslandArches.ARCH_STEP, Math.max(3.0, IslandArches.ARCH_SLOPE * pair.gap()));
                archStages[0]++;
                archStages[1] += big ? 1 : 0;
                archStages[2] += big && gap ? 1 : 0;
                archStages[3] += big && gap && step ? 1 : 0;
                archStages[4] += pair.arch() != null ? 1 : 0;
            }
            for (IslandArches.Pair pair : field.landmarks().pairs(s)) {
                pairs++;
                if (pair.arch() != null) {
                    arches++;
                    archesPerIsland.merge(pair.first(), 1, Integer::sum);
                    archesPerIsland.merge(pair.second(), 1, Integer::sum);
                }
            }
        }
        int[] columns = new int[BINS];
        int[] covered = new int[BINS];
        int[] stacked = new int[BINS];
        int full = 0;
        for (int x = 5900; x <= 8100; x += 4) {
            for (int z = -1400; z <= 1400; z += 4) {
                int bin = bin(x, z);
                if (bin < 0) {
                    continue;
                }
                IslandField.Column c = field.islandColumn(x, z);
                columns[bin]++;
                covered[bin] += c.islandMask() > 0.0 ? 1 : 0;
                stacked[bin] += c.islandCount >= 2 ? 1 : 0;
                full += c.islandCount >= IslandField.MAX_ISLANDS ? 1 : 0;
            }
        }
        System.out.println("ring 4 islands (test seed, wedge x 5900..8100, z -1400..1400), per fifth of the ring:");
        System.out.println(" p        clods middle giants trees  mean-d  sky%  stack%  bottom  top>=140  lakes  dry-high  falls  lifts");
        for (int b = 0; b < BINS; b++) {
            int n = kinds[b][0] + kinds[b][1] + kinds[b][2] + kinds[b][3];
            System.out.printf(" %.1f-%.1f  %5d %6d %6d %5d  %6.1f  %4.1f  %6.2f  %6.1f  %8d  %5d  %8d  %5d  %5d%n",
                    b / (double) BINS, (b + 1) / (double) BINS, kinds[b][0], kinds[b][1], kinds[b][2], kinds[b][3],
                    n == 0 ? 0.0 : diameters[b] / n, 100.0 * covered[b] / Math.max(1, columns[b]),
                    100.0 * stacked[b] / Math.max(1, columns[b]), n == 0 ? 0.0 : bottoms[b] / n, sky140[b], lakes[b],
                    dryHigh[b], falls[b], lifts[b]);
        }
        long twice = archesPerIsland.values().stream().filter(v -> v > 1).count();
        System.out.printf("islands %d, pairs %d, arches %d (islands with more than one arch: %d), debris %d, columns at the cap of %d: %d%n",
                shapes.size(), pairs, arches, twice, debris, IslandField.MAX_ISLANDS, full);
        System.out.printf("arch funnel: pairs %d, no clod %d, gap %d, step %d, clear %d, kept %d%n",
                archStages[0], archStages[1], archStages[2], archStages[3], archStages[4], arches);
    }

    /** The fifth of ring 4's width the position lies in, or −1 outside 6000..8000. */
    private static int bin(double x, double z) {
        double p = (IslandFieldTest.LAYOUT.effectiveDistance(x, z) - 6000.0) / 2000.0;
        return p < 0.0 || p >= 1.0 ? -1 : Math.min(BINS - 1, (int) (p * BINS));
    }
}
