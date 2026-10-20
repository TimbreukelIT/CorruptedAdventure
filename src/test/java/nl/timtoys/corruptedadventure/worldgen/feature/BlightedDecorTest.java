package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ring 4 decor B: the landmarks are pure and stay within their reach, the dead trees and puddles in their chunk. */
class BlightedDecorTest {
    /** A flat blighted world: peat moors east of x = 0, reed marsh west of it, the drowned forest south of z = 2000. */
    private static BlightedLandmarks.World world(long seed) {
        return new BlightedLandmarks.World() {
            @Override
            public long seed() {
                return seed;
            }

            @Override
            public int ground(int x, int z) {
                return z >= 2000 ? 54 : 66;
            }

            @Override
            public boolean plain(int x, int z, int reach) {
                return true;
            }

            @Override
            public double slope(int x, int z) {
                return 0.0;
            }

            @Override
            public ResourceKey<Biome> biome(int x, int z) {
                return z >= 2000 ? ModBiomes.DROWNED_FOREST : x >= 0 ? ModBiomes.PEAT_MOORS : ModBiomes.REED_MARSH;
            }
        };
    }

    private static List<BlightedLandmarks.Site> sites(long seed, BlightedLandmarks.Kind kind, Map<Long, Optional<BlightedLandmarks.Site>> cache) {
        return BlightedLandmarks.within(world(seed), cache, kind, -3000, -3000, 3000, 5000, 0);
    }

    @Test
    void sitesArePureAndSpacedOut() {
        for (BlightedLandmarks.Kind kind : BlightedLandmarks.Kind.values()) {
            List<BlightedLandmarks.Site> first = sites(7L, kind, new HashMap<>());
            assertFalse(first.isEmpty(), kind + " has sites in a world of its biome");
            // Asked in another order (cell by cell from the far corner) the same sites come out.
            Map<Long, Optional<BlightedLandmarks.Site>> cache = new HashMap<>();
            for (int i = 15; i >= -10; i--) {
                for (int j = 16; j >= -10; j--) {
                    BlightedLandmarks.site(world(7L), cache, kind, i, j);
                }
            }
            assertEquals(Set.copyOf(first), Set.copyOf(sites(7L, kind, cache)));
            for (BlightedLandmarks.Site a : first) {
                for (BlightedLandmarks.Site b : first) {
                    if (a != b) {
                        assertTrue(Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z())) >= 2 * BlightedLandmarks.CELL_MARGIN);
                    }
                }
                if (kind == BlightedLandmarks.Kind.MENHIR_CIRCLE) {
                    assertTrue((a.x() & 15) >= 3 && (a.x() & 15) <= 12 && (a.z() & 15) >= 3 && (a.z() & 15) <= 12,
                            "the circle's pool stays in one chunk, a block from its edge");
                }
            }
            assertTrue(!first.equals(sites(8L, kind, new HashMap<>())), "another seed, other sites");
        }
    }

    @Test
    void landmarksStayWithinTheirReachAndArePure() {
        BlightedLandmarks.World world = world(11L);
        for (BlightedLandmarks.Kind kind : BlightedLandmarks.Kind.values()) {
            for (BlightedLandmarks.Site site : sites(11L, kind, new HashMap<>())) {
                LandmarkPlan plan = BlightedLandmarks.build(world, site);
                assertFalse(plan.objects.isEmpty() && plan.wood.isEmpty(), kind + " builds something");
                List<BlockPos> all = new ArrayList<>(plan.objects.keySet());
                all.addAll(plan.wood.keySet());
                plan.decor.forEach(decor -> all.add(decor.pos()));
                plan.feet.forEach(foot -> all.add(new BlockPos(foot.x(), 0, foot.z())));
                all.addAll(plan.ground.keySet());
                all.addAll(plan.groundDecor.keySet());
                for (BlockPos pos : all) {
                    assertTrue(Math.abs(pos.getX() - site.x()) <= BlightedLandmarks.MAX_REACH
                            && Math.abs(pos.getZ() - site.z()) <= BlightedLandmarks.MAX_REACH, kind + " reaches " + pos);
                }
                for (LandmarkPlan.Decor decor : plan.decor) {
                    assertTrue(decor.support().distManhattan(decor.pos()) == 1, "decor sits beside or on its support");
                }
                LandmarkPlan again = BlightedLandmarks.build(world, site);
                assertEquals(plan.objects, again.objects);
                assertEquals(plan.wood, again.wood);
                assertEquals(plan.decor, again.decor);
                assertEquals(plan.pool, again.pool);
            }
        }
    }

    @Test
    void aDeadTreeStaysInItsChunk() {
        DeadTreeConfiguration config = new DeadTreeConfiguration(Blocks.SPRUCE_LOG.defaultBlockState(), Blocks.SPRUCE_WOOD.defaultBlockState(),
                UniformInt.of(4, 12), ConstantInt.of(DeadTreeFeature.MAX_LEAN), UniformInt.of(2, 5), ConstantInt.of(DeadTreeFeature.MAX_BRANCH),
                8, 1.0F, UniformInt.of(2, 8), BlockStateProvider.simple(Blocks.MOSS_CARPET), 1.0F);
        int[][] directions = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
        int fitted = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z += 3) {
                for (int[] d : directions) {
                    DeadTreeFeature.Shape shape = DeadTreeFeature.shape(config, new XoroshiroRandomSource(x * 31L + z), 32 + x, 56, -16 + z,
                            63, 15, DeadTreeFeature.MAX_LEAN, d, (kx, kz) -> 56);
                    if (shape == null) {
                        continue;
                    }
                    fitted++;
                    for (BlockPos pos : shape.wood().keySet()) {
                        assertEquals(2, pos.getX() >> 4, "wood in the chunk");
                        assertEquals(-1, pos.getZ() >> 4, "wood in the chunk");
                    }
                    for (LandmarkPlan.Decor decor : shape.decor()) {
                        assertEquals(2, decor.pos().getX() >> 4);
                        assertEquals(-1, decor.pos().getZ() >> 4);
                        assertTrue(decor.pos().getY() >= 63, "growth only above the water");
                    }
                }
                assertNotNull(DeadTreeFeature.shape(config, new XoroshiroRandomSource(1L), 32 + x, 56, -16 + z, 63, 15, 0, new int[]{0, 0},
                                (kx, kz) -> 56),
                        "an upright tree always fits");
            }
        }
        assertTrue(fitted > 0);
    }

    @Test
    void aPuddleGrowsAroundItsCentre() {
        BlockPos centre = new BlockPos(40, 0, 40);
        for (int size = 1; size <= 7; size++) {
            for (long seed = 0; seed < 20; seed++) {
                Set<BlockPos> pool = ToxicPuddleFeature.shape(centre, size, new XoroshiroRandomSource(seed));
                assertTrue(pool.contains(centre));
                assertTrue(pool.size() <= size);
                for (BlockPos pos : pool) {
                    assertTrue(Math.abs(pos.getX() - 40) <= 1 && Math.abs(pos.getZ() - 40) <= 1);
                }
            }
        }
        // Fitted with its reach, the rim stays in the chunk and the pool a block from its edge (SludgePuddle).
        for (int x = -40; x < 40; x++) {
            BlockPos fitted = GroundObjects.fit(new BlockPos(x, 70, x), ToxicPuddleFeature.REACH);
            assertTrue((fitted.getX() & 15) - 1 >= 1 && (fitted.getX() & 15) + 1 <= 14);
            assertTrue((fitted.getX() & 15) - 3 >= 0 && (fitted.getX() & 15) + 3 <= 15);
        }
    }
}
