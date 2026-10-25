package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ring 2 rock landmarks (G2 ring 2): the strata table, the canyon crossing on synthetic profiles and height fields,
 * the bridge's shape (deck, abutments, one piece, the same in every chunk) and the giant hoodoo group (heights, reach,
 * every block held by the root through solid neighbours).
 */
class Ring2RockLandmarksTest {
    private static final int[][] NEIGHBOURS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    @Test
    void strataMatchTheSurfaceRules() {
        int[] count = new int[3];
        for (int y = 8; y < 56; y++) {
            count[Ring2PureTerrain.stratumIndex(y)]++;
        }
        assertEquals(22, count[0]);
        assertEquals(12, count[1]);
        assertEquals(14, count[2]);
        // Lower edges of noise_settings/overworld.json (one period): 61 cream, 64 red, 68 orange, 75 cream, 96 orange, 100 cream.
        assertEquals(0, Ring2PureTerrain.stratumIndex(60));
        assertEquals(2, Ring2PureTerrain.stratumIndex(61));
        assertEquals(0, Ring2PureTerrain.stratumIndex(64));
        assertEquals(1, Ring2PureTerrain.stratumIndex(68));
        assertEquals(2, Ring2PureTerrain.stratumIndex(75));
        assertEquals(1, Ring2PureTerrain.stratumIndex(96));
        assertEquals(2, Ring2PureTerrain.stratumIndex(100));
        assertEquals(2, Ring2PureTerrain.stratumIndex(148));
        assertEquals(0, Ring2PureTerrain.stratumIndex(104));
    }

    /** A V-free canyon profile: floor 70 within {@code half} of the crossing point, plateau 132 beyond, then {@code beyond}. */
    private static double[] profile(int half, double beyond, int landing) {
        double[] p = new double[2 * Ring2RockLandmarks.SCAN + 1];
        for (int i = -Ring2RockLandmarks.SCAN; i <= Ring2RockLandmarks.SCAN; i++) {
            int a = Math.abs(i);
            p[i + Ring2RockLandmarks.SCAN] = a < half ? 70.0 : a < half + landing ? 132.0 : beyond;
        }
        return p;
    }

    @Test
    void crossingFindsRimsDeckAndLandings() {
        double[] cross = Ring2RockLandmarks.crossingOf(profile(12, 132.0, 40), 70.0);
        assertNotNull(cross);
        assertEquals(12.0, cross[0]);
        assertEquals(12.0, cross[1]);
        assertEquals(131.0, cross[2]);
        assertNull(Ring2RockLandmarks.crossingOf(profile(23, 132.0, 40), 70.0), "too wide");
        assertNull(Ring2RockLandmarks.crossingOf(profile(6, 132.0, 40), 70.0), "too narrow");
        assertNull(Ring2RockLandmarks.crossingOf(profile(12, 132.0, 40), 115.0), "too shallow");
        assertNull(Ring2RockLandmarks.crossingOf(profile(12, 90.0, 3), 70.0), "no landing behind the rim");
    }

    /** A straight canyon along x: floor 70 where |z| < 11, plateau 130 elsewhere. */
    private static final DoubleBinaryOperator CANYON = (x, z) -> Math.abs(z) < 11.0 ? 70.0 : 130.0;

    @Test
    void crossingGoesStraightOverAStraightCanyon() {
        Ring2RockLandmarks.Bridge bridge = Ring2RockLandmarks.crossing(CANYON, 0, 0, RandomSource.create(7));
        assertNotNull(bridge);
        // Straight across within one direction step (the rims are whole blocks, so a neighbouring direction can tie).
        assertTrue(Math.abs(bridge.cos()) < 0.2);
        assertEquals(129, bridge.top());
        assertTrue(bridge.back() + bridge.ahead() >= Ring2RockLandmarks.MIN_SPAN);
        Ring2RockLandmarks.Bridge again = Ring2RockLandmarks.crossing(CANYON, 0, 0, RandomSource.create(7));
        assertEquals(bridge, again);
        assertNull(Ring2RockLandmarks.crossing((x, z) -> 130.0, 0, 0, RandomSource.create(7)), "no canyon");
    }

    @Test
    void bridgeIsOnePieceWithAbutmentsAndTheSameInEveryChunk() {
        for (int seed = 0; seed < 40; seed++) {
            Ring2RockLandmarks.Bridge bridge = Ring2RockLandmarks.crossing(CANYON, 3, -2, RandomSource.create(seed));
            assertNotNull(bridge);
            Set<Long> blocks = new HashSet<>();
            int[] box = bridge.box();
            for (int x = box[0]; x <= box[2]; x++) {
                for (int z = box[1]; z <= box[3]; z++) {
                    int[] span = bridge.interval(x, z);
                    if (span == null) {
                        continue;
                    }
                    assertTrue(span[0] <= span[1]);
                    assertTrue(span[1] <= bridge.top() + 2 && span[0] >= bridge.top() - Ring2RockLandmarks.ABUTMENT - 1);
                    for (int y = span[0]; y <= span[1]; y++) {
                        blocks.add(key(x, y, z));
                    }
                }
            }
            assertTrue(connected(blocks), "bridge " + seed + " in one piece");
            // The ends stand on the plateau: some block of each abutment reaches under the plateau top (130).
            boolean anchoredBack = false;
            boolean anchoredAhead = false;
            for (int x = box[0]; x <= box[2]; x++) {
                for (int z = box[1]; z <= box[3]; z++) {
                    int[] span = bridge.interval(x, z);
                    if (span != null && span[0] < 130) {
                        double u = bridge.along(x + 0.5, z + 0.5);
                        anchoredBack |= u < -bridge.back();
                        anchoredAhead |= u > bridge.ahead();
                    }
                }
            }
            assertTrue(anchoredBack && anchoredAhead, "bridge " + seed + " anchored at both ends");
        }
    }

    @Test
    void hoodooGroupStaysInReachAndHoldsTogether() {
        DoubleBinaryOperator floor = (x, z) -> 88.0;
        for (int seed = 0; seed < 25; seed++) {
            Ring2RockLandmarks.Hoodoo hoodoo = Ring2RockLandmarks.group(floor, 100, -40, RandomSource.create(seed));
            List<Ring2RockLandmarks.Pillar> pillars = hoodoo.pillars();
            assertTrue(pillars.size() >= 2 && pillars.size() <= 3);
            int main = pillars.getFirst().top() - pillars.getFirst().ground();
            assertTrue(main >= 55 && main <= 75, "main height " + main);
            for (Ring2RockLandmarks.Pillar p : pillars.subList(1, pillars.size())) {
                assertTrue(p.top() < pillars.getFirst().top());
            }
            int r = Ring2RockLandmarks.HOODOO_REACH;
            for (Ring2RockLandmarks.Pillar p : pillars) {
                Set<Long> blocks = new HashSet<>();
                for (int y = p.ground() - Ring2RockLandmarks.ROOT; y <= p.top(); y++) {
                    for (int x = hoodoo.x() - r - 4; x <= hoodoo.x() + r + 4; x++) {
                        for (int z = hoodoo.z() - r - 4; z <= hoodoo.z() + r + 4; z++) {
                            if (Ring2RockLandmarks.inPillar(hoodoo.seed(), p, x, y, z)) {
                                assertTrue(Math.abs(x - hoodoo.x()) <= r && Math.abs(z - hoodoo.z()) <= r, "within reach");
                                blocks.add(key(x, y, z));
                            }
                        }
                    }
                }
                // Each pillar is one piece from its root (ROOT under the floor) to its cap.
                assertTrue(connected(blocks), "hoodoo " + seed + " pillar held together");
            }
        }
    }

    @Test
    void flatFloorWantsTheHoodooBand() {
        assertTrue(Ring2RockLandmarks.flatFloor((x, z) -> 88.0, 0, 0));
        assertTrue(!Ring2RockLandmarks.flatFloor((x, z) -> 130.0, 0, 0));
        assertTrue(!Ring2RockLandmarks.flatFloor((x, z) -> 86.0 + (x > 10 ? 8.0 : 0.0), 0, 0));
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 1_000_000) << 40) ^ ((long) (y + 1024) << 20) ^ (z + 500_000);
    }

    /** Whether the blocks form one 6-connected piece. */
    private static boolean connected(Set<Long> blocks) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        long first = blocks.iterator().next();
        int[] start = unkey(first);
        queue.add(start);
        seen.add(first);
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            for (int[] n : NEIGHBOURS) {
                long k = key(at[0] + n[0], at[1] + n[1], at[2] + n[2]);
                if (blocks.contains(k) && seen.add(k)) {
                    queue.add(new int[]{at[0] + n[0], at[1] + n[1], at[2] + n[2]});
                }
            }
        }
        return seen.size() == blocks.size();
    }

    private static int[] unkey(long k) {
        int z = (int) (k & 0xFFFFF) - 500_000;
        int y = (int) ((k >>> 20) & 0xFFFFF) - 1024;
        int x = (int) (k >>> 40) - 1_000_000;
        return new int[]{x, y, z};
    }
}
