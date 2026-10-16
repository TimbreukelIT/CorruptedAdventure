package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Package 75b: the crowns of the verdant tree keep every leaf alive, the climbing strands' tips and flowering share. */
class VerdantFloraTest {
    /** Leaves decay beyond distance 6 from a log: every leaf of a crown lies within 6 steps (through leaves) of one. */
    @Test
    void everyLeafOfTheUmbrellaIsWithinReachOfTheTrunkOrABranch() {
        for (int radius = 5; radius <= 6; radius++) {
            for (long seed = 0; seed < 20; seed++) {
                List<BlockPos> leaves = VerdantFoliagePlacer.shape(VerdantFoliagePlacer.Shape.UMBRELLA, radius, RandomSource.create(seed));
                assertTrue(maxDistance(leaves, umbrellaLogs()) <= 6, "radius " + radius + " seed " + seed);
            }
        }
    }

    @Test
    void everyFrondLeafIsWithinReachOfTheTrunk() {
        for (int radius = 2; radius <= 3; radius++) {
            List<BlockPos> leaves = VerdantFoliagePlacer.shape(VerdantFoliagePlacer.Shape.FRONDS, radius, RandomSource.create(1));
            assertTrue(maxDistance(leaves, Set.of(new BlockPos(0, -1, 0))) <= 6, "radius " + radius);
            for (BlockPos leaf : leaves) {
                assertTrue(Math.abs(leaf.getX()) <= radius && Math.abs(leaf.getZ()) <= radius, "a tree fern stays narrow");
            }
        }
    }

    /** The umbrella is wide and flat: its disc reaches the radius, the dome is smaller and only one block higher. */
    @Test
    void theUmbrellaIsWideAndFlat() {
        List<BlockPos> leaves = VerdantFoliagePlacer.shape(VerdantFoliagePlacer.Shape.UMBRELLA, 5, RandomSource.create(7));
        assertTrue(leaves.contains(new BlockPos(5, -1, 0)) && leaves.contains(new BlockPos(0, -1, -5)));
        assertFalse(leaves.contains(new BlockPos(5, 0, 0)), "the dome is smaller than the disc");
        for (BlockPos leaf : leaves) {
            assertTrue(leaf.getY() >= -2 && leaf.getY() <= 0);
        }
    }

    /** B's note for 75b: the vine's tip at the bottom of a strand, the flowering variant whole (tip and stem). */
    @Test
    void aStrandEndsInItsTipAndFlowersWhole() {
        LedgeStrandConfiguration config = new LedgeStrandConfiguration(null, null, 0.5F, 0.2F, 3, 24, 4,
                java.util.Optional.empty(), java.util.Optional.empty(), 0.0F);
        assertEquals(null, config.strandAt(70, 70, true), "no tip configured: the strand block all the way");
        assertFalse(LedgeStrands.inVariant(1, 2, 3, 0.0F));
        int flowering = 0;
        int total = 0;
        for (int x = 0; x < 64; x++) {
            for (int z = 0; z < 64; z++) {
                total++;
                if (LedgeStrands.inVariant(x, 80, z, VerdantFallsFeatures.FLOWERING_SHARE)) {
                    flowering++;
                }
            }
        }
        double share = flowering / (double) total;
        assertTrue(Math.abs(share - VerdantFallsFeatures.FLOWERING_SHARE) < 0.05, "flowering share " + share);
    }

    /** The trunk top, the eight branches (one out, one up, one out) under the umbrella's attachment at (0, 0, 0). */
    private static Set<BlockPos> umbrellaLogs() {
        Set<BlockPos> logs = new HashSet<>();
        logs.add(new BlockPos(0, -1, 0));
        logs.add(new BlockPos(0, -2, 0));
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] d : directions) {
            logs.add(new BlockPos(d[0], -2, d[1]));
            logs.add(new BlockPos(2 * d[0], -1, 2 * d[1]));
            logs.add(new BlockPos(3 * d[0], -1, 3 * d[1]));
        }
        return logs;
    }

    /** The largest leaf distance (vanilla's rule: steps through leaves from a log), 99 for a leaf no log reaches. */
    private static int maxDistance(List<BlockPos> leafList, Set<BlockPos> logs) {
        Set<BlockPos> leaves = new HashSet<>(leafList);
        leaves.removeAll(logs);
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos log : logs) {
            for (Direction direction : Direction.values()) {
                BlockPos next = log.relative(direction);
                if (leaves.contains(next) && !distance.containsKey(next)) {
                    distance.put(next, 1);
                    queue.add(next);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos leaf = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = leaf.relative(direction);
                if (leaves.contains(next) && !distance.containsKey(next)) {
                    distance.put(next, distance.get(leaf) + 1);
                    queue.add(next);
                }
            }
        }
        int max = 0;
        for (BlockPos leaf : leaves) {
            max = Math.max(max, distance.getOrDefault(leaf, 99));
        }
        return max;
    }
}
