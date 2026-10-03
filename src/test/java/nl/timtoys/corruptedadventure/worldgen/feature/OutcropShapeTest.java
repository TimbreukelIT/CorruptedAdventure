package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code outcrop} ({@link OutcropShape}) for the salt chimneys and the dune rocks of change 30b, 2.000 rocks each on
 * flat, sloping and rolling synthetic ground: in the chunk, every block held by the ground through solid neighbours
 * (collar too), heights in range, a collar roofs at least three columns, and the same input gives the same rock.
 */
class OutcropShapeTest {
    private static final int MIN_X = -64;
    private static final int MIN_Z = 16;
    private static final int[][] NEIGHBOURS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** The salt chimney and dune rock settings of Ring2FlatsFeatures. */
    private record Config(int minKnobs, int maxKnobs, float spread, float minRadius, float maxRadius, int minHeight, int maxHeight,
                          float taper, int sink, float overhang) {
        static final Config SALT_PILLAR = new Config(1, 3, 3.0F, 0.8F, 1.6F, 2, 6, 0.5F, 1, 0.0F);
        static final Config DUNE_ROCK = new Config(2, 5, 4.0F, 1.5F, 3.5F, 4, 12, 0.6F, 2, 0.35F);

        OutcropShape.Params params(RandomSource random) {
            int knobs = minKnobs + random.nextInt(maxKnobs - minKnobs + 1);
            float[] radius = new float[knobs];
            for (int i = 0; i < knobs; i++) {
                radius[i] = minRadius + random.nextFloat() * (maxRadius - minRadius);
            }
            return new OutcropShape.Params(knobs, spread, radius, minHeight + random.nextInt(maxHeight - minHeight + 1), taper, sink,
                    random.nextFloat() < overhang);
        }
    }

    /** Ground per column: flat, a slope or rolling dunes; free above it. */
    private record Ground(int kind, double phase) implements OutcropShape.Terrain {
        @Override
        public int ground(int x, int z) {
            return switch (kind) {
                case 0 -> 65;
                case 1 -> 70 + (int) Math.floor((x - MIN_X) * 0.6 + (z - MIN_Z) * 0.3);
                default -> 72 + (int) Math.round(6 * Math.sin(x * 0.25 + phase) * Math.cos(z * 0.2));
            };
        }

        @Override
        public boolean free(int x, int y, int z) {
            return y > ground(x, z);
        }
    }

    @Test
    void saltPillars() {
        check(Config.SALT_PILLAR, 1);
    }

    @Test
    void duneRocks() {
        check(Config.DUNE_ROCK, 2);
    }

    @Test
    void unusableGroundGivesNoRock() {
        RandomSource random = RandomSource.create(3L);
        OutcropShape.Terrain water = new OutcropShape.Terrain() {
            @Override
            public int ground(int x, int z) {
                return x == MIN_X + 8 ? Integer.MIN_VALUE : 65;
            }

            @Override
            public boolean free(int x, int y, int z) {
                return y > 65;
            }
        };
        for (int run = 0; run < 200; run++) {
            OutcropShape.Params params = Config.DUNE_ROCK.params(random);
            assertTrue(OutcropShape.build(water, MIN_X, MIN_Z, MIN_X + 8, MIN_Z + 8, params, random).isEmpty());
        }
    }

    @Test
    void pure() {
        for (int run = 0; run < 100; run++) {
            Ground ground = new Ground(run % 3, run);
            OutcropShape.Params params = Config.DUNE_ROCK.params(RandomSource.create(run));
            Optional<OutcropShape.Rock> a = OutcropShape.build(ground, MIN_X, MIN_Z, MIN_X + 3, MIN_Z + 12, params, RandomSource.create(run));
            Optional<OutcropShape.Rock> b = OutcropShape.build(ground, MIN_X, MIN_Z, MIN_X + 3, MIN_Z + 12, params, RandomSource.create(run));
            assertEquals(a, b);
        }
    }

    private static void check(Config config, long seed) {
        RandomSource random = RandomSource.create(seed);
        int built = 0;
        int collars = 0;
        int overhangs = 0;
        int maxWidth = 0;
        for (int run = 0; run < 2000; run++) {
            Ground ground = new Ground(run % 3, random.nextDouble() * 10);
            OutcropShape.Params params = config.params(random);
            overhangs += params.overhang() ? 1 : 0;
            Optional<OutcropShape.Rock> found = OutcropShape.build(ground, MIN_X, MIN_Z, MIN_X + random.nextInt(16),
                    MIN_Z + random.nextInt(16), params, random);
            if (found.isEmpty()) {
                continue;
            }
            built++;
            OutcropShape.Rock rock = found.get();
            Set<BlockPos> blocks = new HashSet<>(rock.blocks());
            assertEquals(rock.blocks().size(), blocks.size(), "a block twice");
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int lowGround = Integer.MAX_VALUE;
            int highGround = Integer.MIN_VALUE;
            for (BlockPos pos : blocks) {
                int lx = pos.getX() - MIN_X;
                int lz = pos.getZ() - MIN_Z;
                assertTrue(lx >= 1 && lx <= 14 && lz >= 1 && lz <= 14, "outside the chunk: " + pos);
                int g = ground.ground(pos.getX(), pos.getZ());
                lowGround = Math.min(lowGround, g);
                highGround = Math.max(highGround, g);
                assertTrue(pos.getY() > g - config.sink(), "too deep: " + pos);
                minX = Math.min(minX, pos.getX());
                maxX = Math.max(maxX, pos.getX());
            }
            maxWidth = Math.max(maxWidth, maxX - minX + 1);
            for (BlockPos pos : blocks) {
                assertTrue(pos.getY() <= highGround + params.height(), "too high: " + pos);
            }
            assertTrue(grounded(blocks, ground), "a floating block");
            int roofed = roofed(blocks, ground);
            if (rock.roofed() > 0) {
                collars++;
                assertTrue(rock.roofed() >= OutcropShape.MIN_ROOFED);
                assertEquals(rock.roofed(), roofed);
            } else {
                assertEquals(0, roofed, "overhang without a collar");
            }
        }
        assertTrue(built > 1900, "built " + built);
        assertTrue(maxWidth <= 14, "width " + maxWidth);
        if (config.overhang() > 0) {
            // Most rolled collars find three roofed columns.
            assertTrue(collars > overhangs / 2, collars + " of " + overhangs);
        } else {
            assertEquals(0, collars);
        }
    }

    /** Flood fill from the blocks on or in the ground through face neighbours: reaches every block. */
    private static boolean grounded(Set<BlockPos> blocks, Ground ground) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> reached = new HashSet<>();
        for (BlockPos pos : blocks) {
            if (pos.getY() - 1 <= ground.ground(pos.getX(), pos.getZ())) {
                queue.add(pos);
                reached.add(pos);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos from = queue.poll();
            for (int[] d : NEIGHBOURS) {
                BlockPos next = from.offset(d[0], d[1], d[2]);
                if (blocks.contains(next) && reached.add(next)) {
                    queue.add(next);
                }
            }
        }
        return reached.size() == blocks.size();
    }

    /** Columns with a rock block that has air below it above the ground (shade under a collar). */
    private static int roofed(Set<BlockPos> blocks, Ground ground) {
        Set<Long> columns = new HashSet<>();
        for (BlockPos pos : blocks) {
            if (!blocks.contains(pos.below()) && pos.getY() - 1 > ground.ground(pos.getX(), pos.getZ())) {
                columns.add(PoolLayout.pack(pos.getX(), pos.getZ()));
            }
        }
        return columns.size();
    }
}
