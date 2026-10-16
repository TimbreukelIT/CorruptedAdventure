package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The stem of the verdant vine (72b): vanilla's weeping vines plant with the tip's {@link VerdantVineBlock#FLOWERING}
 * state, passed on when the stem becomes a tip again. No item (like vanilla's vine plants).
 */
public class VerdantVinePlantBlock extends GrowingPlantBodyBlock {
    public static final MapCodec<VerdantVinePlantBlock> CODEC = simpleCodec(VerdantVinePlantBlock::new);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public VerdantVinePlantBlock(Properties properties) {
        super(properties, Direction.DOWN, SHAPE, false);
        registerDefaultState(stateDefinition.any().setValue(VerdantVineBlock.FLOWERING, false));
    }

    @Override
    protected MapCodec<VerdantVinePlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VerdantVineBlock.FLOWERING);
    }

    @Override
    protected BlockState updateHeadAfterConvertedFromBody(BlockState body, BlockState head) {
        return head.setValue(VerdantVineBlock.FLOWERING, body.getValue(VerdantVineBlock.FLOWERING));
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return ModBlocks.VERDANT_VINE.get();
    }
}
