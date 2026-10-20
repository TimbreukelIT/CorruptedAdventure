package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandFieldTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Decor rollout C: the sky landmarks on the test seed and the shape of the giant moonpetal. */
class SkyLandmarksTest {
    /** A wedge of each side of ring 4 (block boxes). */
    private static final int[][] BOXES = {{6000, -900, 7400, 900}, {-7400, -900, -6000, 900}, {-900, 6000, 900, 7400}};

    @Test
    void landmarksAreRareHighAndOnTheirIsland() {
        IslandField field = IslandFieldTest.field();
        int moonpetals = 0;
        int ruins = 0;
        Set<IslandField.Island> islands = new HashSet<>();
        for (int[] box : BOXES) {
            for (SkyLandmarks.Landmark landmark : SkyLandmarks.within(field, IslandFieldTest.SEED, box[0], box[1], box[2], box[3])) {
                IslandField.Island island = landmark.island();
                assertTrue(islands.add(island), "one landmark per island " + island);
                assertTrue(island.top() >= SkyLandmarks.MIN_TOP);
                assertTrue(island.covers(landmark.x(), landmark.z()), "on its island: " + landmark);
                int margin = landmark.kind() == SkyLandmarks.Kind.RUIN ? SkyRuinFeature.REACH : GiantMoonpetalFeature.MARGIN;
                assertTrue((landmark.x() & 15) >= margin && (landmark.x() & 15) <= 15 - margin, "fits its chunk: " + landmark);
                assertTrue((landmark.z() & 15) >= margin && (landmark.z() & 15) <= 15 - margin, "fits its chunk: " + landmark);
                if (landmark.kind() == SkyLandmarks.Kind.GIANT_MOONPETAL) {
                    moonpetals++;
                    assertTrue(island.radius() >= SkyLandmarks.MOONPETAL_MIN_RADIUS && island.radius() <= SkyLandmarks.MOONPETAL_MAX_RADIUS);
                } else {
                    ruins++;
                }
                // Each chunk finds the landmark of its own box, and only there.
                int minX = landmark.x() & ~15;
                int minZ = landmark.z() & ~15;
                assertTrue(SkyLandmarks.within(field, IslandFieldTest.SEED, minX, minZ, minX + 15, minZ + 15).contains(landmark));
                assertFalse(SkyLandmarks.within(field, IslandFieldTest.SEED, minX + 16, minZ, minX + 31, minZ + 15).contains(landmark));
            }
        }
        System.out.printf("sky landmarks: %d giant moonpetals, %d ruins%n", moonpetals, ruins);
        assertTrue(moonpetals >= 2 && moonpetals <= 20, "moonpetals " + moonpetals);
        assertTrue(ruins >= 1 && ruins <= 20, "ruins " + ruins);
    }

    @Test
    void theGiantMoonpetalKeepsItsLeavesAliveAndInsideTheDecorationWindow() {
        for (int seed = 0; seed < 40; seed++) {
            int crown = GiantMoonpetalShape.MIN_CROWN + seed % (GiantMoonpetalShape.MAX_CROWN - GiantMoonpetalShape.MIN_CROWN + 1);
            GiantMoonpetalShape shape = GiantMoonpetalShape.grow(new XoroshiroRandomSource(seed), crown);
            Set<BlockPos> wood = new HashSet<>(shape.trunk());
            wood.addAll(shape.wood());
            int widest = 0;
            for (BlockPos leaf : shape.leaves()) {
                assertFalse(wood.contains(leaf), "a leaf on the wood");
                // The 2 x 2 trunk's corner stands MARGIN or more from the chunk's edges; the window reaches 16 further.
                assertTrue(leaf.getX() >= -16 - GiantMoonpetalFeature.MARGIN && leaf.getX() <= 16 + GiantMoonpetalFeature.MARGIN - 1,
                        "leaf x " + leaf);
                assertTrue(leaf.getZ() >= -16 - GiantMoonpetalFeature.MARGIN && leaf.getZ() <= 16 + GiantMoonpetalFeature.MARGIN - 1,
                        "leaf z " + leaf);
                widest = Math.max(widest, Math.max(Math.abs(leaf.getX() - 1), Math.abs(leaf.getZ() - 1)));
            }
            assertTrue(widest >= crown - 2, "crown " + crown + " reaches only " + widest);
            Map<BlockPos, Integer> distance = distances(wood, shape.leaves());
            for (BlockPos leaf : shape.leaves()) {
                assertTrue(distance.getOrDefault(leaf, 99) <= GiantMoonpetalShape.MAX_LEAF_DISTANCE, "leaf too far from the wood " + leaf);
            }
            for (BlockPos root : shape.roots()) {
                assertTrue(shape.wood().contains(root));
            }
        }
    }

    @Test
    void theGiantMoonpetalIsTheRandomsAlone() {
        RandomSource a = new XoroshiroRandomSource(7);
        RandomSource b = new XoroshiroRandomSource(7);
        assertEquals(GiantMoonpetalShape.grow(a, 12), GiantMoonpetalShape.grow(b, 12));
        List<BlockPos> first = List.copyOf(GiantMoonpetalShape.grow(new XoroshiroRandomSource(7), 12).leaves());
        List<BlockPos> again = List.copyOf(GiantMoonpetalShape.grow(new XoroshiroRandomSource(7), 12).leaves());
        assertEquals(first, again, "the same order too");
    }

    private static Map<BlockPos, Integer> distances(Set<BlockPos> wood, Set<BlockPos> leaves) {
        Map<BlockPos, Integer> distance = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>(wood);
        wood.forEach(pos -> distance.put(pos, 0));
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (leaves.contains(next) && !distance.containsKey(next)) {
                    distance.put(next, distance.get(pos) + 1);
                    queue.add(next);
                }
            }
        }
        return distance;
    }
}
