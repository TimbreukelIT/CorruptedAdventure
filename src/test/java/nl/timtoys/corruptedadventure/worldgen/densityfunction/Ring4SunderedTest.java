package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sundered reach (package 63) on the test seed: the zone 4→5 breaks into chunks over an abyss with a still sea. */
class Ring4SunderedTest {
    private static final IslandField FIELD = IslandFieldTest.field();
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;

    @Test
    void theZoneIsCalmAtItsInnerEdgeAndFullyBrokenFurtherOut() {
        double start = Ring4Sundered.zoneStart(LAYOUT);
        double end = Ring4Sundered.zoneEnd(LAYOUT);
        assertEquals(8000.0 - 128.0, start, 1e-9);
        assertEquals(8000.0 + 128.0, end, 1e-9);
        assertEquals(0.0, Ring4Sundered.strength(LAYOUT, start), 1e-9);
        assertEquals(1.0, Ring4Sundered.strength(LAYOUT, start + Ring4Sundered.FULL_AT * (end - start)), 1e-9);
        assertTrue(Ring4Sundered.strength(LAYOUT, start + 0.3 * (end - start)) > 0.0);
    }

    /** Land bridges every 450–850 blocks along the zone, each crossing it whole. */
    @Test
    void landBridgesStandAboutEveryFiveHundredToEightHundredBlocks() {
        long salt = FIELD.salt();
        List<Double> along = new ArrayList<>();
        for (int k = -3; k <= 12; k++) {
            double[] bridge = Ring4Sundered.bridge(salt, k);
            along.add(bridge[0]);
            assertTrue(Ring4Sundered.onBridge(salt, bridge[0]), "the centre of bridge " + k);
        }
        for (int k = 1; k < along.size(); k++) {
            double gap = along.get(k) - along.get(k - 1);
            assertTrue(gap >= 450.0 && gap <= 850.0, "gap " + gap);
        }
    }

    /** The whole zone 4→5: the share of abyss (cracks or loose chunks) is at most 40 % (decision of the user). */
    @Test
    void theAbyssIsAtMostFortyPercentOfTheZone() {
        for (long seed : new long[]{IslandFieldTest.SEED, 42L, 987654321L}) {
            IslandField field = IslandFieldTest.field(seed);
            double start = Ring4Sundered.zoneStart(LAYOUT);
            double end = Ring4Sundered.zoneEnd(LAYOUT);
            int all = 0;
            int abyss = 0;
            for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.01) {
                for (double r = start; r <= end; r += 9.0) {
                    int x = (int) Math.floor(r * Math.cos(angle));
                    int z = (int) Math.floor(r * Math.sin(angle));
                    double effective = LAYOUT.effectiveDistance(x, z);
                    if (effective < start || effective > end) {
                        continue;
                    }
                    all++;
                    IslandField.Column c = field.column(x, z);
                    if (c.abyssSeaTop() != Integer.MIN_VALUE) {
                        abyss++;
                        assertEquals(Ring4Sundered.SEA_TOP + 1, c.abyssSeaTop());
                        assertTrue(c.surface() <= Ring4Sundered.ABYSS_FLOOR + 1e-9, "abyss floor at " + x + ", " + z + ": " + c.surface());
                    }
                }
            }
            double share = abyss / (double) all;
            System.out.printf("sundered seed %d: %d of %d columns over the whole zone are abyss (%.3f)%n", seed, abyss, all, share);
            assertTrue(share > 0.05 && share <= 0.40, "seed " + seed + ": abyss share " + share);
        }
    }

    /** Inside ring 4 and ring 5 proper there is no abyss, and nothing is sundered. */
    @Test
    void nothingIsSunderedOutsideTheZone() {
        for (int x = 6000; x <= 7800; x += 97) {
            for (int z = -600; z <= 600; z += 89) {
                IslandField.Column c = FIELD.column(x, z);
                assertEquals(Integer.MIN_VALUE, c.abyssSeaTop(), x + ", " + z);
                assertEquals(0.0, c.sundered(), x + ", " + z);
            }
        }
    }

    /** A loose chunk is a solid floating above its ground (a thin interval), and its column has no ground under it. */
    @Test
    void looseChunksFloatAsSolids() {
        double start = Ring4Sundered.zoneStart(LAYOUT);
        double end = Ring4Sundered.zoneEnd(LAYOUT);
        int loose = 0;
        java.util.Set<Integer> tops = new java.util.TreeSet<>();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.01) {
            for (double r = start + 0.8 * (end - start); r <= end; r += 9.0) {
                int x = (int) Math.floor(r * Math.cos(angle));
                int z = (int) Math.floor(r * Math.sin(angle));
                IslandField.Column c = FIELD.column(x, z);
                if (c.abyssSeaTop() == Integer.MIN_VALUE || c.islandCount == 0) {
                    continue;
                }
                boolean solid = false;
                int top = Integer.MIN_VALUE;
                for (int y = -20; y <= 300; y++) {
                    if (FIELD.thin(x, y, z) > 0.0) {
                        solid = true;
                        top = y;
                    }
                }
                if (solid) {
                    loose++;
                    tops.add(top);
                    assertTrue(c.surface() <= Ring4Sundered.ABYSS_FLOOR + 1e-9);
                }
            }
        }
        System.out.println("loose columns with a floating solid: " + loose);
        assertTrue(loose > 0, "no loose chunk found");
        // Not flat (review 63): the slabs' tops differ from column to column.
        assertTrue(tops.size() >= 4, "distinct tops " + tops.size());
    }
}
