package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code surface_pool} ({@link PoolLayout}) on 2.000 synthetic salt flats and dune valleys: every layout lies in its
 * chunk, its water is source blocks on one level in a closed basin (vanilla's settled state), and broken rims or
 * foreign ground give no layout.
 */
class SurfacePoolFeatureTest {
    private static final int MIN_X = 32;
    private static final int MIN_Z = -48;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** Synthetic terrain: a top height per column; optional foreign ground and a hole (air) below the top. */
    private record World(int[][] top, boolean[][] foreign, Set<BlockPos> holes) implements PoolLayout.Terrain {
        static World of(RandomSource random, int kind) {
            int[][] top = new int[48][48];
            double tilt = random.nextDouble() * 0.15;
            for (int x = 0; x < 48; x++) {
                for (int z = 0; z < 48; z++) {
                    int dx = x - 24;
                    int dz = z - 24;
                    top[x][z] = switch (kind) {
                        case 0 -> 65;                                                      // salt flat
                        case 1 -> 66 + (int) Math.floor((dx * dx + dz * dz) / 40.0);       // dune valley (bowl)
                        case 2 -> 66 + (int) Math.floor(dx * tilt);                        // gentle slope
                        default -> 66 + random.nextInt(3);                                 // bumpy floor
                    };
                }
            }
            return new World(top, new boolean[48][48], new HashSet<>());
        }

        @Override
        public int surface(int x, int z) {
            return top[x - MIN_X + 16][z - MIN_Z + 16];
        }

        @Override
        public boolean ground(int x, int y, int z) {
            return y == surface(x, z) && !foreign[x - MIN_X + 16][z - MIN_Z + 16];
        }

        @Override
        public boolean solid(int x, int y, int z) {
            return y <= surface(x, z) && !holes.contains(new BlockPos(x, y, z));
        }
    }

    @Test
    void layoutsAreClosedBasinsInTheirChunk() {
        RandomSource random = RandomSource.create(30_2L);
        int built = 0;
        int[] perKind = new int[4];
        for (int run = 0; run < 2000; run++) {
            int kind = run % 4;
            World world = World.of(random, kind);
            PoolLayout.Params params = params(random, kind == 0 ? 0 : 2);
            int ox = MIN_X + random.nextInt(16);
            int oz = MIN_Z + random.nextInt(16);
            Optional<PoolLayout.Layout> found = PoolLayout.build(world, MIN_X, MIN_Z, ox, oz, params);
            if (found.isEmpty()) {
                continue;
            }
            built++;
            perKind[kind]++;
            check(world, found.get(), params);
        }
        // Flat salt always fits; the bowls and bumps often (max_step 2).
        assertEquals(500, perKind[0], "flat salt");
        assertTrue(built > 1200, "built " + built);
    }

    @Test
    void noLayoutWithAHoleBesideTheWaterOrForeignGround() {
        RandomSource random = RandomSource.create(7L);
        for (int run = 0; run < 500; run++) {
            World world = World.of(random, 0);
            PoolLayout.Params params = params(random, 0);
            Optional<PoolLayout.Layout> plain = PoolLayout.build(world, MIN_X, MIN_Z, MIN_X + 8, MIN_Z + 8, params);
            assertTrue(plain.isPresent());
            PoolLayout.Layout layout = plain.get();
            // A hole under a rim block next to the water: the water would run out.
            BlockPos rim = layout.besideWater().get(random.nextInt(layout.besideWater().size()));
            World holed = new World(world.top(), world.foreign(), new HashSet<>(Set.of(rim)));
            assertFalse(PoolLayout.build(holed, MIN_X, MIN_Z, MIN_X + 8, MIN_Z + 8, params).isPresent(), "hole at " + rim);
            // A column of foreign ground (sand at the edge of a salt island) in the rim.
            boolean[][] foreign = new boolean[48][48];
            BlockPos edge = layout.rim().get(random.nextInt(layout.rim().size()));
            foreign[edge.getX() - MIN_X + 16][edge.getZ() - MIN_Z + 16] = true;
            assertFalse(PoolLayout.build(new World(world.top(), foreign, Set.of()), MIN_X, MIN_Z, MIN_X + 8, MIN_Z + 8, params).isPresent());
            // A rim column higher than max_step (an island).
            int[][] top = copy(world.top());
            top[edge.getX() - MIN_X + 16][edge.getZ() - MIN_Z + 16] += params.maxStep() + 1;
            assertFalse(PoolLayout.build(new World(top, new boolean[48][48], Set.of()), MIN_X, MIN_Z, MIN_X + 8, MIN_Z + 8, params).isPresent());
        }
    }

    @Test
    void trunksStayAwayFromTheChunkEdgeAndApart() {
        RandomSource random = RandomSource.create(11L);
        int trunks = 0;
        for (int run = 0; run < 2000; run++) {
            World world = World.of(random, 1);
            PoolLayout.Params params = new PoolLayout.Params(1.5F + random.nextFloat() * 1.5F, edge(random, 0.3F), 2, 2, 2);
            Optional<PoolLayout.Layout> found = PoolLayout.build(world, MIN_X, MIN_Z, MIN_X + 1 + random.nextInt(14),
                    MIN_Z + 1 + random.nextInt(14), params);
            if (found.isEmpty()) {
                continue;
            }
            List<BlockPos> chosen = PoolLayout.trunks(found.get(), MIN_X, MIN_Z, 2, random, pos -> true);
            trunks += chosen.size();
            for (BlockPos trunk : chosen) {
                int lx = trunk.getX() - MIN_X;
                int lz = trunk.getZ() - MIN_Z;
                assertTrue(Math.min(lx, 15 - lx) >= PoolLayout.TREE_EDGE && Math.min(lz, 15 - lz) >= PoolLayout.TREE_EDGE, trunk.toString());
                assertEquals(found.get().level() + 1, trunk.getY());
                for (BlockPos other : chosen) {
                    assertTrue(other == trunk || other.distSqr(trunk) >= PoolLayout.TREE_SPACING * PoolLayout.TREE_SPACING);
                }
            }
        }
        assertTrue(trunks > 500, "trunks " + trunks);
    }

    @Test
    void sameInputSameLayout() {
        for (int run = 0; run < 50; run++) {
            World world = World.of(RandomSource.create(run), run % 4);
            PoolLayout.Params params = params(RandomSource.create(run), 2);
            assertEquals(PoolLayout.build(world, MIN_X, MIN_Z, MIN_X + 5, MIN_Z + 9, params),
                    PoolLayout.build(world, MIN_X, MIN_Z, MIN_X + 5, MIN_Z + 9, params));
        }
    }

    private static void check(World world, PoolLayout.Layout layout, PoolLayout.Params params) {
        Set<BlockPos> water = new HashSet<>(layout.water());
        Set<BlockPos> carved = new HashSet<>(layout.carve());
        Set<BlockPos> rim = new HashSet<>(layout.rim());
        // In the chunk, at least one block from its edge.
        for (List<BlockPos> part : List.of(layout.water(), layout.carve(), layout.rim(), layout.floor())) {
            for (BlockPos pos : part) {
                int lx = pos.getX() - MIN_X;
                int lz = pos.getZ() - MIN_Z;
                assertTrue(lx >= 1 && lx <= 14 && lz >= 1 && lz <= 14, "outside the chunk: " + pos);
            }
        }
        assertFalse(water.isEmpty());
        for (BlockPos pos : water) {
            // One level: every column of water has its top on the water level, at most depth deep.
            assertTrue(pos.getY() <= layout.level() && pos.getY() > layout.level() - params.depth(), pos.toString());
            assertTrue(pos.getY() == layout.level() || water.contains(pos.above()));
            // Closed: beside on the same level and below, water or solid after digging (rim blocks are solid).
            for (int[] d : SIDES) {
                BlockPos side = pos.offset(d[0], 0, d[1]);
                assertTrue(water.contains(side) || solidAfter(world, side, carved, rim), "leak beside " + pos);
            }
            assertTrue(water.contains(pos.below()) || solidAfter(world, pos.below(), carved, rim), "leak below " + pos);
            // Open to the sky above the top water block.
            if (pos.getY() == layout.level()) {
                assertFalse(solidAfter(world, pos.above(), carved, rim), "buried water at " + pos);
            }
        }
        for (BlockPos pos : layout.rim()) {
            assertEquals(layout.level(), pos.getY());
            assertTrue(world.surface(pos.getX(), pos.getZ()) <= layout.level() + params.maxStep());
        }
    }

    private static boolean solidAfter(World world, BlockPos pos, Set<BlockPos> carved, Set<BlockPos> rim) {
        return rim.contains(pos) || !carved.contains(pos) && world.solid(pos.getX(), pos.getY(), pos.getZ());
    }

    private static PoolLayout.Params params(RandomSource random, int maxStep) {
        float radius = 1.5F + random.nextFloat() * 3.5F;
        return new PoolLayout.Params(radius, edge(random, 0.35F), 1 + random.nextInt(2), 1 + random.nextInt(2), maxStep);
    }

    private static float[] edge(RandomSource random, float jitter) {
        float[] edge = new float[PoolLayout.EDGE_POINTS];
        for (int i = 0; i < edge.length; i++) {
            edge[i] = 1.0F - jitter * random.nextFloat();
        }
        return edge;
    }

    private static int[][] copy(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            copy[i] = grid[i].clone();
        }
        return copy;
    }
}
