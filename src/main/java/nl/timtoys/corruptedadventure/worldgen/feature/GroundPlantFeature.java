package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

/**
 * {@code corruptedadventure:ground_plant} (tree determinism, 2026-10-18): vanilla's {@code simple_block} for the ring 4
 * ground plants with a weighted pick (grass and fern, flowers, dead bushes), which draws the plant before it looks at
 * the spot and then only fills open air. A tree from a neighbouring chunk may already stand on the spot or come later
 * ({@link IsolatedTreeFeature} takes the plant's place): with the draw always made, the plants after it in the chunk
 * stay the same in either order. The placement checks the ground only ({@link Ring4Placements#ON_GROUND}); nothing grows
 * on the floating debris ({@link Ring4Terrain#onDebris}).
 */
public final class GroundPlantFeature extends Feature<SimpleBlockConfiguration> {
    public GroundPlantFeature(Codec<SimpleBlockConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        BlockState state = context.config().toPlace().getState(context.random(), pos);
        if (!level.getBlockState(pos).isAir() || !state.canSurvive(level, pos) || Ring4Terrain.onDebris(level, pos.getX(), pos.getZ())) {
            return false;
        }
        if (state.getBlock() instanceof DoublePlantBlock) {
            if (!level.isEmptyBlock(pos.above())) {
                return false;
            }
            DoublePlantBlock.placeAt(level, state, pos, Block.UPDATE_CLIENTS);
        } else {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
