package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.FluidState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Palms of neighbouring grid cells (trunks 5–7 apart, also across a chunk border) give the same blocks whichever grows
 * first: a frond runs on past another palm's trunk, and a later trunk replaces leaves anyway. Uses the real trunk shape
 * and crown placer; the two tags vanilla's tree code reads are bound by hand (no datapack in unit tests).
 */
class PalmNeighboursTest {
    private static final int PAIRS = 3000;

    @BeforeEach
    void bindTags() {
        BuiltInRegistries.BLOCK.bindTags(Map.of(
                BlockTags.LOGS, List.of(holder(ModBlocks.PALM_LOG.get())),
                BlockTags.LEAVES, List.of(holder(ModBlocks.PALM_LEAVES.get())),
                BlockTags.REPLACEABLE_BY_TREES, List.of(holder(ModBlocks.PALM_LEAVES.get()))));
    }

    @AfterEach
    void resetTags() {
        BuiltInRegistries.BLOCK.resetTags();
    }

    @Test
    void sameBlocksInEitherOrder() {
        RandomSource pick = RandomSource.create(5);
        int touching = 0;
        for (int n = 0; n < PAIRS; n++) {
            BlockPos a = BlockPos.ZERO;
            // Mostly in line, where a frond of one palm can run into the other's trunk.
            BlockPos b = new BlockPos((pick.nextBoolean() ? 1 : -1) * (5 + pick.nextInt(3)), 0, pick.nextInt(3) - 1);
            long seedA = pick.nextLong();
            long seedB = pick.nextLong();
            World first = new World();
            first.palm(a, seedA);
            first.palm(b, seedB);
            World second = new World();
            second.palm(b, seedB);
            second.palm(a, seedA);
            assertEquals(first.blocks, second.blocks, "pair " + n);
            touching += first.frondOnLog || second.frondOnLog ? 1 : 0;
        }
        assertTrue(touching > 15, "the case must occur: a frond running into the other trunk in " + touching + " pairs");
    }

    @Test
    void palmBushStaysWithinTwoBlocks() {
        for (long seed = 0; seed < 200; seed++) {
            RandomSource random = RandomSource.create(seed);
            int height = Ring2Features.BUSH_TRUNK.getTreeHeight(random);
            assertTrue(height >= 1 && height <= 2, "stem " + height);
            PalmTrunkPlacer.Shape trunk = Ring2Features.BUSH_TRUNK.shape(random, height);
            assertEquals(0, trunk.lean());
            for (List<BlockPos> frond : Ring2Features.BUSH_CROWN.fronds(random)) {
                for (BlockPos offset : frond) {
                    BlockPos leaf = trunk.top().offset(offset);
                    assertTrue(Math.abs(leaf.getX()) <= 2 && Math.abs(leaf.getZ()) <= 2, "bush leaf " + leaf);
                }
            }
        }
    }

    private static Holder<Block> holder(Block block) {
        return BuiltInRegistries.BLOCK.wrapAsHolder(block);
    }

    /** Blocks by position (air where absent), with the palm placement of vanilla's tree feature (trunk, then crown). */
    private static final class World implements LevelSimulatedReader {
        private static final TreeConfiguration CONFIG = new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.PALM_LOG.get()), Ring2Features.PALM_TRUNK,
                BlockStateProvider.simple(ModBlocks.PALM_LEAVES.get()), Ring2Features.PALM_CROWN,
                new TwoLayersFeatureSize(1, 0, 1)).build();

        final Map<BlockPos, Block> blocks = new HashMap<>();
        /** A crown leaf went to (or hit) a log of the other palm. */
        boolean frondOnLog;

        void palm(BlockPos origin, long seed) {
            RandomSource random = RandomSource.create(seed);
            int height = Ring2Features.PALM_TRUNK.getTreeHeight(random);
            PalmTrunkPlacer.Shape trunk = Ring2Features.PALM_TRUNK.shape(random, height);
            for (BlockPos log : trunk.logs()) {
                BlockPos pos = origin.offset(log);
                // TrunkPlacer.placeLog: only where a tree may go (air, leaves).
                if (isStateAtPosition(pos, state -> state.isAir() || state.is(BlockTags.REPLACEABLE_BY_TREES))) {
                    frondOnLog |= blocks.containsKey(pos);
                    blocks.put(pos, ModBlocks.PALM_LOG.get());
                }
            }
            FoliagePlacer.FoliageSetter setter = new FoliagePlacer.FoliageSetter() {
                @Override
                public void set(BlockPos pos, BlockState state) {
                    blocks.put(pos.immutable(), state.getBlock());
                }

                @Override
                public boolean isSet(BlockPos pos) {
                    return blocks.containsKey(pos);
                }
            };
            Ring2Features.PALM_CROWN.createFoliage(this, setter, random, CONFIG, height,
                    new FoliagePlacer.FoliageAttachment(origin.offset(trunk.top()), 0, false), 0, 0, 0);
        }

        private BlockState state(BlockPos pos) {
            Block block = blocks.get(pos);
            return block == null ? Blocks.AIR.defaultBlockState() : block.defaultBlockState();
        }

        @Override
        public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
            return predicate.test(state(pos));
        }

        @Override
        public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
            return predicate.test(state(pos).getFluidState());
        }

        @Override
        public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
            return Optional.empty();
        }

        @Override
        public BlockPos getHeightmapPos(Heightmap.Types type, BlockPos pos) {
            return pos;
        }
    }
}
