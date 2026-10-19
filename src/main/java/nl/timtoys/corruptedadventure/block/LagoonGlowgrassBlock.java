package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * Glowing grass of the lagoon shores (decor rollout): a {@link MythicPlantBlock} that also stands in water
 * (waterloggable, like hanging roots) and grows on {@code #corruptedadventure:lagoon_plant_soil}, which adds the lagoon
 * floor's gravel and clay to the ring 4 plant soil. When it loses its ground under water, the water stays.
 */
public class LagoonGlowgrassBlock extends MythicPlantBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<LagoonGlowgrassBlock> CODEC = simpleCodec(LagoonGlowgrassBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public LagoonGlowgrassBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<LagoonGlowgrassBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.Blocks.LAGOON_PLANT_SOIL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        // The bush turns into air without its ground; a waterlogged one leaves its water behind.
        return updated.isAir() ? state.getFluidState().createLegacyBlock() : updated;
    }
}
