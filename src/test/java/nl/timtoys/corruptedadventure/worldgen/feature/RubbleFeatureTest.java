package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shape core of {@code rubble} ({@link RubbleShape}) on bumpy synthetic ground with obstacles. */
class RubbleFeatureTest {
    private static final int ORIGINS = 2000;

    @Test
    void everyBlockInTheChunkAndResting() {
        int[] blocksPerSize = new int[RubbleShape.MAX_SIZE + 1];
        for (int n = 0; n < ORIGINS; n++) {
            RandomSource random = RandomSource.create(n);
            Ground ground = new Ground(n);
            int cx = random.nextInt(200) - 100;
            int cz = random.nextInt(200) - 100;
            BlockPos origin = new BlockPos(cx * 16 + random.nextInt(16), 0, cz * 16 + random.nextInt(16));
            int size = 1 + random.nextInt(RubbleShape.MAX_SIZE);
            List<BlockPos> blocks = RubbleShape.blocks(origin, size, random, ground);
            Set<BlockPos> boulder = new HashSet<>(blocks);
            assertEquals(blocks.size(), boulder.size(), "no block twice");
            blocksPerSize[size] += blocks.size();
            for (BlockPos pos : blocks) {
                assertEquals(cx, pos.getX() >> 4, "seed " + n);
                assertEquals(cz, pos.getZ() >> 4, "seed " + n);
                int top = ground.groundTop(pos.getX(), pos.getZ());
                if (pos.getY() == top) {
                    assertTrue(size > 1, "only bigger boulders are buried");
                    assertTrue(ground.solid(pos.getX(), pos.getY(), pos.getZ()));
                    assertTrue(ground.solid(pos.getX(), pos.getY() - 1, pos.getZ()), "never buried over a hole");
                } else {
                    assertTrue(pos.getY() > top, "never below the ground top");
                    assertTrue(ground.free(pos.getX(), pos.getY(), pos.getZ()), "only into free space");
                    BlockPos below = pos.below();
                    assertTrue(boulder.contains(below) || below.getY() == top && ground.solid(below.getX(), below.getY(), below.getZ()),
                            "rests on the ground or the boulder, seed " + n + " " + pos);
                }
            }
            assertTrue(blocks.size() <= maxBlocks(size), "size " + size + ": " + blocks.size());
        }
        assertTrue(blocksPerSize[1] < blocksPerSize[2] && blocksPerSize[2] < blocksPerSize[3], "bigger boulders, more blocks");
    }

    @Test
    void nothingWithoutSolidGround() {
        RubbleShape.Terrain water = new RubbleShape.Terrain() {
            @Override
            public int groundTop(int x, int z) {
                return 62;
            }

            @Override
            public boolean solid(int x, int y, int z) {
                return false;
            }

            @Override
            public boolean free(int x, int y, int z) {
                return true;
            }
        };
        for (int size = 1; size <= RubbleShape.MAX_SIZE; size++) {
            assertTrue(RubbleShape.blocks(new BlockPos(5, 0, 5), size, RandomSource.create(size), water).isEmpty());
        }
    }

    @Test
    void columnsOfEachSize() {
        for (long seed = 0; seed < 200; seed++) {
            RandomSource random = RandomSource.create(seed);
            assertEquals(List.of(new RubbleShape.Column(0, 0, 1)), RubbleShape.columns(1, random));
            List<RubbleShape.Column> two = RubbleShape.columns(2, random);
            assertTrue(two.size() >= 3 && two.size() <= 4);
            assertEquals(1, two.stream().filter(c -> c.height() == 2).count());
            List<RubbleShape.Column> three = RubbleShape.columns(3, random);
            assertTrue(three.size() >= 5 && three.size() <= 9);
            assertFalse(three.stream().anyMatch(c -> c.height() > 3));
        }
    }

    private static int maxBlocks(int size) {
        return switch (size) {
            case 1 -> 1;
            case 2 -> 4 + 5;
            default -> 9 + 3 + 4 * 2 + 4;
        };
    }

    /** Ground that wobbles ±2 around Y 70 with a few obstacles (plants that cannot be replaced) above it. */
    private record Ground(long seed) implements RubbleShape.Terrain {
        @Override
        public int groundTop(int x, int z) {
            return 70 + (int) Math.floorMod(hash(x, 0, z), 5L) - 2;
        }

        @Override
        public boolean solid(int x, int y, int z) {
            return y <= groundTop(x, z) && Math.floorMod(hash(x, y, z), 40L) != 0;
        }

        @Override
        public boolean free(int x, int y, int z) {
            return y > groundTop(x, z) && Math.floorMod(hash(x, y, z), 9L) != 0;
        }

        private long hash(int x, int y, int z) {
            long h = seed * 0x9E3779B97F4A7C15L + x * 31L + y * 1_000_003L + z * 998_244_353L;
            h ^= h >>> 29;
            h *= 0xBF58476D1CE4E5B9L;
            return h ^ (h >>> 32);
        }
    }
}
