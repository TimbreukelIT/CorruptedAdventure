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

/** The palm as configured in {@link Ring2Features}, for 200 seeds, assuming nothing is in the way. */
class PalmTreeShapeTest {
    private static final int SEEDS = 200;

    @Test
    void trunkHeightLeanAndStraightBase() {
        for (long seed = 0; seed < SEEDS; seed++) {
            RandomSource random = RandomSource.create(seed);
            int height = Ring2Features.PALM_TRUNK.getTreeHeight(random);
            PalmTrunkPlacer.Shape trunk = Ring2Features.PALM_TRUNK.shape(random, height);
            assertTrue(height >= 6 && height <= 10, "height " + height);
            assertEquals(height, trunk.logs().size());
            assertTrue(trunk.lean() >= 1 && trunk.lean() <= 2, "lean " + trunk.lean());
            int straight = Math.ceilDiv(height, 3);
            for (int y = 0; y < height; y++) {
                BlockPos log = trunk.logs().get(y);
                assertEquals(y, log.getY());
                int sideways = Math.abs(log.getX()) + Math.abs(log.getZ());
                if (y < straight) {
                    assertEquals(0, sideways, "first third straight, seed " + seed);
                }
                // Only ever one step further than the log below, always in the same direction.
                int below = y == 0 ? 0 : Math.abs(trunk.logs().get(y - 1).getX()) + Math.abs(trunk.logs().get(y - 1).getZ());
                assertTrue(sideways - below <= 1 && sideways >= below);
                assertEquals(log, BlockPos.ZERO.relative(trunk.direction(), sideways).above(y));
            }
            assertEquals(trunk.lean(), Math.abs(trunk.top().getX()) + Math.abs(trunk.top().getZ()));
            assertEquals(height, trunk.top().getY());
        }
    }

    @Test
    void everyLeafHangsOnTheTrunkWithinDecayDistance() {
        for (long seed = 0; seed < SEEDS; seed++) {
            RandomSource random = RandomSource.create(seed);
            PalmTrunkPlacer.Shape trunk = Ring2Features.PALM_TRUNK.shape(random, Ring2Features.PALM_TRUNK.getTreeHeight(random));
            List<List<BlockPos>> fronds = Ring2Features.PALM_CROWN.fronds(random);
            Set<BlockPos> logs = new HashSet<>(trunk.logs());
            Set<BlockPos> placed = new HashSet<>(logs);
            Set<BlockPos> leaves = new HashSet<>();
            int straight = 0;
            int diagonal = 0;
            for (List<BlockPos> frond : fronds) {
                if (frond.size() > 1) {
                    if (frond.getFirst().getY() == 0) {
                        straight++;
                    } else {
                        diagonal++;
                    }
                }
                for (BlockPos offset : frond) {
                    BlockPos leaf = trunk.top().offset(offset);
                    assertFalse(logs.contains(leaf), "leaf in the trunk, seed " + seed);
                    // A frond stops at the first leaf that cannot be placed: each leaf must touch something placed before it.
                    assertTrue(touchesAny(leaf, placed), "detached leaf " + offset + ", seed " + seed);
                    placed.add(leaf);
                    leaves.add(leaf);
                }
            }
            assertEquals(4, straight);
            assertTrue(diagonal >= 2 && diagonal <= 4, "diagonal fronds " + diagonal);

            Map<BlockPos, Integer> distance = distances(logs, leaves);
            for (BlockPos leaf : leaves) {
                assertTrue(distance.containsKey(leaf), "unreachable leaf, seed " + seed);
                assertTrue(distance.get(leaf) <= 5, "leaf " + distance.get(leaf) + " steps from the trunk, seed " + seed);
            }
            for (BlockPos block : placed) {
                assertTrue(Math.abs(block.getX()) <= 6 && Math.abs(block.getZ()) <= 6, "outside ±6: " + block);
            }
        }
    }

    private static boolean touchesAny(BlockPos pos, Set<BlockPos> blocks) {
        for (Direction direction : Direction.values()) {
            if (blocks.contains(pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    /** Like vanilla's leaf distance: face steps through leaves, starting at the logs. */
    private static Map<BlockPos, Integer> distances(Set<BlockPos> logs, Set<BlockPos> leaves) {
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos log : logs) {
            distance.put(log, 0);
            queue.add(log);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
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
