package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The determinism rule of Nether ring 2: whatever order overlapping features write in, the end state is the same. Five
 * "features" that overlap on a small in-memory hall (stone floor, stone ceiling) are applied in all 120 orders.
 */
class GrowthPriorityTest {
    private static final int FLOOR = 0;
    private static final int CEILING = 20;

    private static final BlockState STEM = ModBlocks.DRIPSHROOM_STEM.get().defaultBlockState();
    private static final BlockState CAP = ModBlocks.DRIPSHROOM_CAP.get().defaultBlockState();
    private static final BlockState LAMP = ModBlocks.DRIPSHROOM_LAMP.get().defaultBlockState();
    private static final BlockState MOLD = ModBlocks.GLOW_MOLD.get().defaultBlockState();
    private static final BlockState SOURCE = ModBlocks.DEW_SOURCE.get().defaultBlockState();

    private static final class MapWorld implements FungalWorld {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();

        MapWorld() {
            for (int x = -8; x <= 8; x++) {
                for (int z = -8; z <= 8; z++) {
                    blocks.put(new BlockPos(x, FLOOR, z), Blocks.STONE.defaultBlockState());
                    blocks.put(new BlockPos(x, CEILING, z), Blocks.STONE.defaultBlockState());
                }
            }
        }

        @Override
        public BlockState get(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public void set(BlockPos pos, BlockState state) {
            blocks.put(pos, state);
        }
    }

    private static List<Consumer<FungalWorld>> features() {
        List<Consumer<FungalWorld>> features = new ArrayList<>();
        // A stem column and a cap disc with lamps around it.
        features.add(world -> {
            for (int y = FLOOR + 1; y < CEILING; y++) {
                GrowthPriority.write(world, new BlockPos(0, y, 0), STEM);
            }
        });
        features.add(world -> {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    GrowthPriority.write(world, new BlockPos(x, 10, z), Math.floorMod(x + z, 3) == 0 ? LAMP : CAP);
                }
            }
        });
        // A stalactite through the disc.
        features.add(world -> {
            for (int i = 0; i < 6; i++) {
                DripstoneThickness thickness = i == 5 ? DripstoneThickness.TIP : DripstoneThickness.MIDDLE;
                GrowthPriority.write(world, new BlockPos(1, 16 - i, 1), ModBlocks.SLIME_STALACTITE.get().defaultBlockState()
                        .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN)
                        .setValue(BlockStateProperties.DRIPSTONE_THICKNESS, thickness));
            }
        });
        // Mould over a region that overlaps the others; two patches whose faces merge.
        features.add(world -> {
            for (int x = -4; x <= 4; x++) {
                for (int y = 1; y <= 12; y++) {
                    GrowthPriority.write(world, new BlockPos(x, y, 1), MOLD.setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
                    GrowthPriority.write(world, new BlockPos(x, y, 1), MOLD.setValue(MultifaceBlock.getFaceProperty(Direction.NORTH), Math.floorMod(x + y, 2) == 0));
                }
            }
        });
        // The geode lining: terrain to dew source (not through the priority), buds next to it in air or a lower growth.
        features.add(world -> {
            for (int x = -2; x <= 2; x++) {
                BlockPos wall = new BlockPos(x, FLOOR, 1);
                if (FungalTerrainScan.isTerrain(world.get(wall))) {
                    world.set(wall, SOURCE);
                }
                GrowthPriority.write(world, wall.above(), ModBlocks.SMALL_DEW_CRYSTAL_BUD.get().defaultBlockState());
            }
        });
        return features;
    }

    @Test
    void everyWriteOrderEndsInTheSameWorld() {
        List<Consumer<FungalWorld>> features = features();
        Map<BlockPos, BlockState> expected = null;
        int orders = 0;
        for (List<Integer> order : permutations(features.size())) {
            MapWorld world = new MapWorld();
            order.forEach(i -> features.get(i).accept(world));
            if (expected == null) {
                expected = world.blocks;
            } else {
                assertEquals(expected, world.blocks, "order " + order);
            }
            orders++;
        }
        assertEquals(120, orders);
    }

    @Test
    void theHighestRankWinsAndTerrainIsNeverOverwritten() {
        MapWorld world = new MapWorld();
        BlockPos pos = new BlockPos(0, 5, 0);
        GrowthPriority.write(world, pos, MOLD);
        GrowthPriority.write(world, pos, LAMP);
        GrowthPriority.write(world, pos, MOLD);
        assertSame(LAMP.getBlock(), world.get(pos).getBlock());
        GrowthPriority.write(world, pos, STEM);
        GrowthPriority.write(world, pos, CAP);
        assertSame(STEM.getBlock(), world.get(pos).getBlock());
        BlockPos floor = new BlockPos(0, FLOOR, 0);
        GrowthPriority.write(world, floor, STEM);
        assertSame(Blocks.STONE, world.get(floor).getBlock());
    }

    @Test
    void ranksFollowTheDesignOrder() {
        BlockState stalactite = ModBlocks.SLIME_STALACTITE.get().defaultBlockState();
        BlockState bud = ModBlocks.SMALL_DEW_CRYSTAL_BUD.get().defaultBlockState();
        assertTrue(GrowthPriority.rank(STEM) > GrowthPriority.rank(CAP));
        assertTrue(GrowthPriority.rank(CAP) > GrowthPriority.rank(LAMP));
        assertTrue(GrowthPriority.rank(LAMP) > GrowthPriority.rank(stalactite));
        assertTrue(GrowthPriority.rank(stalactite) > GrowthPriority.rank(bud));
        assertTrue(GrowthPriority.rank(bud) > GrowthPriority.rank(MOLD));
        assertTrue(GrowthPriority.rank(MOLD) > GrowthPriority.rank(ModBlocks.SPORE_SPROUTS.get().defaultBlockState()));
        assertEquals(GrowthPriority.NONE, GrowthPriority.rank(SOURCE));
        assertEquals(GrowthPriority.NONE, GrowthPriority.rank(Blocks.STONE.defaultBlockState()));
    }

    /** Nether ring 3's growths (53b) take the ranks of their Nether ring 2 counterparts, so scans see through them. */
    @Test
    void netherRing3GrowthsRankLikeTheirCounterparts() {
        assertEquals(GrowthPriority.STEM, GrowthPriority.rank(ModBlocks.RIMESHROOM_STEM.get().defaultBlockState()));
        assertEquals(GrowthPriority.STEM, GrowthPriority.rank(ModBlocks.RIMESHROOM_HYPHAE.get().defaultBlockState()));
        assertEquals(GrowthPriority.STEM, GrowthPriority.rank(ModBlocks.STRIPPED_RIMESHROOM_STEM.get().defaultBlockState()));
        assertEquals(GrowthPriority.STEM, GrowthPriority.rank(ModBlocks.STRIPPED_RIMESHROOM_HYPHAE.get().defaultBlockState()));
        assertEquals(GrowthPriority.CAP, GrowthPriority.rank(ModBlocks.RIMESHROOM_CAP.get().defaultBlockState()));
        assertEquals(GrowthPriority.STALACTITE, GrowthPriority.rank(ModBlocks.ICE_STALACTITE.get().defaultBlockState()));
        assertTrue(FungalTerrainScan.isTerrain(ModBlocks.COLD_LAVA.get().defaultBlockState()));
        assertTrue(FungalTerrainScan.isTerrain(ModBlocks.NETHER_ICE.get().defaultBlockState()));
    }

    private static List<List<Integer>> permutations(int n) {
        List<List<Integer>> result = new ArrayList<>();
        permute(new ArrayList<>(), new boolean[n], n, result);
        return result;
    }

    private static void permute(List<Integer> current, boolean[] used, int n, List<List<Integer>> result) {
        if (current.size() == n) {
            result.add(List.copyOf(current));
            return;
        }
        for (int i = 0; i < n; i++) {
            if (!used[i]) {
                used[i] = true;
                current.add(i);
                permute(current, used, n, result);
                current.remove(current.size() - 1);
                used[i] = false;
            }
        }
    }
}
