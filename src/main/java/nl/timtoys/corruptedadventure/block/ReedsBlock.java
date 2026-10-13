package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Reeds of the ring 4 marshes (decision golf 1, V2 = B): a walk-through column like sugar cane, up to
 * {@link #MAX_HEIGHT} high. It stands on dirt or sand (with or without water), the bottom block may stand waterlogged
 * in water one deep, and it grows like sugar cane (random ticks, server-side), but only where the bottom stands in or
 * next to water. The top block shows the plume ({@link #TOP}, model only).
 */
public class ReedsBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<ReedsBlock> CODEC = simpleCodec(ReedsBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** No reeds above: this block carries the plume. */
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    /** Grown reeds are at most this many blocks high (worldgen places 2–3). */
    public static final int MAX_HEIGHT = 3;
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public ReedsBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(WATERLOGGED, false).setValue(TOP, true));
    }

    @Override
    protected MapCodec<ReedsBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, WATERLOGGED, TOP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean inWater = level.getFluidState(pos).getType() == Fluids.WATER;
        return defaultBlockState().setValue(WATERLOGGED, inWater).setValue(TOP, !level.getBlockState(pos.above()).is(this));
    }

    /**
     * On reeds, or on dirt or sand. A waterlogged block must be the bottom one with no water above it (water one deep):
     * the stem never stands under water.
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (state.getValue(WATERLOGGED) && (below.is(this) || !level.getFluidState(pos.above()).isEmpty())) {
            return false;
        }
        if (below.is(this)) {
            return true;
        }
        var soil = below.canSustainPlant(level, pos.below(), Direction.UP, state);
        if (!soil.isDefault()) {
            return soil.isTrue();
        }
        return below.is(BlockTags.DIRT) || below.is(BlockTags.SAND);
    }

    /** Like sugar cane: a block that lost its footing breaks a tick later, so a column falls apart from the bottom up. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        if (!state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == Direction.UP) {
            state = state.setValue(TOP, !neighbour.is(this));
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    /** Sugar cane's growth (age 0–15, then a new block on top) up to {@link #MAX_HEIGHT}, only next to or in water. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isEmptyBlock(pos.above())) {
            return;
        }
        int height = 1;
        while (level.getBlockState(pos.below(height)).is(this)) {
            height++;
        }
        if (height >= MAX_HEIGHT || !hasWater(level, pos.below(height - 1))
                || !CommonHooks.canCropGrow(level, pos, state, true)) {
            return;
        }
        int age = state.getValue(AGE);
        if (age == 15) {
            BlockState grown = defaultBlockState();
            level.setBlockAndUpdate(pos.above(), grown);
            CommonHooks.fireCropGrowPost(level, pos.above(), grown);
            level.setBlock(pos, level.getBlockState(pos).setValue(AGE, 0), Block.UPDATE_INVISIBLE);
        } else {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_INVISIBLE);
        }
    }

    /** The bottom block stands in water, or water lies beside it or beside the ground it stands on. */
    private static boolean hasWater(LevelReader level, BlockPos bottom) {
        if (level.getFluidState(bottom).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(bottom.relative(direction)).is(FluidTags.WATER)
                    || level.getFluidState(bottom.below().relative(direction)).is(FluidTags.WATER)
                    || level.getBlockState(bottom.below().relative(direction)).is(Blocks.FROSTED_ICE)) {
                return true;
            }
        }
        return false;
    }
}
