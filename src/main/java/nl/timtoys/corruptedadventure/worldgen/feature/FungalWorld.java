package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The part of a world the Nether ring 2 features read and write ({@link FungalTerrainScan}, {@link GrowthPriority}): a
 * block view and a sink, so the rules can be tested on a few blocks in a map.
 */
public interface FungalWorld {
    BlockState get(BlockPos pos);

    /** Writes without neighbour updates (worldgen). */
    void set(BlockPos pos, BlockState state);

    static FungalWorld of(WorldGenLevel level) {
        return new FungalWorld() {
            @Override
            public BlockState get(BlockPos pos) {
                return level.getBlockState(pos);
            }

            @Override
            public void set(BlockPos pos, BlockState state) {
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        };
    }
}
