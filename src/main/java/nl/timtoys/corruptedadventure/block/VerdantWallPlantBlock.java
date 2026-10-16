package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A plant on a wall or trunk (72b): the orchid on the emergent trunks and gorge walls, and the wall bromeliad (the
 * standing bromeliad's wall form, like a wall torch). {@link #FACING} points away from the wall, which must be sturdy on
 * that side; without it the plant drops. Walk-through; pops a copy with bonemeal.
 */
public class VerdantWallPlantBlock extends HorizontalDirectionalBlock implements PoppingBonemealable {
    public static final MapCodec<VerdantWallPlantBlock> CODEC = simpleCodec(VerdantWallPlantBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(1.0, 0.0, 12.0, 15.0, 16.0, 16.0),
            Direction.SOUTH, Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 4.0),
            Direction.WEST, Block.box(12.0, 0.0, 1.0, 16.0, 16.0, 15.0),
            Direction.EAST, Block.box(0.0, 0.0, 1.0, 4.0, 16.0, 15.0));

    public VerdantWallPlantBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends VerdantWallPlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    /** Against the wall the player looks at, or the next sturdy wall around (like a wall torch or a ladder). */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
