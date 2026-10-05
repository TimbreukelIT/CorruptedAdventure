package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Hanging stalactite or standing stalagmite, a copy of vanilla's {@code PointedDripstoneBlock} without growth, dripping
 * and cauldron filling. Vanilla's helpers test {@code state.is(Blocks.POINTED_DRIPSTONE)}, so a subclass would not
 * recognise itself (merging, thickness, falling); here they test {@code state.is(this)}. Same properties and shapes as
 * vanilla. Not rooted in any fluid or ground: it hangs from any sturdy block above and falls when that goes. Reused for
 * the ice stalactites of Nether ring 3. Re-compare with vanilla on a Minecraft upgrade.
 */
public class StalactiteBlock extends Block implements Fallable, SimpleWaterloggedBlock {
    public static final MapCodec<StalactiteBlock> CODEC = simpleCodec(StalactiteBlock::new);
    public static final DirectionProperty TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int DELAY_BEFORE_FALLING = 2;
    private static final double MIN_TRIDENT_VELOCITY_TO_BREAK = 0.6;
    private static final float DAMAGE_PER_FALL_DISTANCE_AND_SIZE = 1.0F;
    private static final int MAX_DAMAGE = 40;
    private static final int MAX_HEIGHT_FOR_DAMAGE_CALCULATION = 6;
    private static final float STALAGMITE_FALL_DISTANCE_OFFSET = 2.0F;
    private static final float STALAGMITE_FALL_DAMAGE_MODIFIER = 2.0F;
    private static final VoxelShape TIP_MERGE_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape TIP_SHAPE_UP = Block.box(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape TIP_SHAPE_DOWN = Block.box(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape FRUSTUM_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape MIDDLE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape BASE_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
    private static final float MAX_HORIZONTAL_OFFSET = 0.125F;

    public StalactiteBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(TIP_DIRECTION, Direction.UP)
                .setValue(THICKNESS, DripstoneThickness.TIP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends StalactiteBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidPlacement(level, pos, state.getValue(TIP_DIRECTION));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction != Direction.UP && direction != Direction.DOWN) {
            return state;
        }
        Direction tipDirection = state.getValue(TIP_DIRECTION);
        if (tipDirection == Direction.DOWN && level.getBlockTicks().hasScheduledTick(pos, this)) {
            return state;
        }
        if (direction == tipDirection.getOpposite() && !canSurvive(state, level, pos)) {
            level.scheduleTick(pos, this, tipDirection == Direction.DOWN ? DELAY_BEFORE_FALLING : 1);
            return state;
        }
        boolean tipMerge = state.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
        return state.setValue(THICKNESS, calculateThickness(level, pos, tipDirection, tipMerge));
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide) {
            BlockPos pos = hit.getBlockPos();
            if (projectile.mayInteract(level, pos) && projectile.mayBreak(level) && projectile instanceof ThrownTrident
                    && projectile.getDeltaMovement().length() > MIN_TRIDENT_VELOCITY_TO_BREAK) {
                level.destroyBlock(pos, true);
            }
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == DripstoneThickness.TIP) {
            entity.causeFallDamage(fallDistance + STALAGMITE_FALL_DISTANCE_OFFSET, STALAGMITE_FALL_DAMAGE_MODIFIER, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isStalagmite(state) && !canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        } else {
            spawnFallingStalactite(state, level, pos);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction wanted = context.getNearestLookingVerticalDirection().getOpposite();
        Direction tipDirection = calculateTipDirection(level, pos, wanted);
        if (tipDirection == null) {
            return null;
        }
        boolean tipMerge = !context.isSecondaryUseActive();
        return defaultBlockState()
                .setValue(TIP_DIRECTION, tipDirection)
                .setValue(THICKNESS, calculateThickness(level, pos, tipDirection, tipMerge))
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DripstoneThickness thickness = state.getValue(THICKNESS);
        VoxelShape shape = switch (thickness) {
            case TIP_MERGE -> TIP_MERGE_SHAPE;
            case TIP -> state.getValue(TIP_DIRECTION) == Direction.DOWN ? TIP_SHAPE_DOWN : TIP_SHAPE_UP;
            case FRUSTUM -> FRUSTUM_SHAPE;
            case MIDDLE -> MIDDLE_SHAPE;
            case BASE -> BASE_SHAPE;
        };
        Vec3 offset = state.getOffset(level, pos);
        return shape.move(offset.x, 0.0, offset.z);
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected float getMaxHorizontalOffset() {
        return MAX_HORIZONTAL_OFFSET;
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            level.levelEvent(1045, pos, 0);
        }
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingStalactite(entity);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    /** The whole hanging column falls, from the broken block down to the tip; the tip hurts what it lands on. */
    private void spawnFallingStalactite(BlockState state, ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        BlockState current = state;
        while (isStalactite(current)) {
            FallingBlockEntity falling = FallingBlockEntity.fall(level, cursor, current);
            if (isTip(current, true)) {
                int height = Math.max(1 + pos.getY() - cursor.getY(), MAX_HEIGHT_FOR_DAMAGE_CALCULATION);
                falling.setHurtsEntities(DAMAGE_PER_FALL_DISTANCE_AND_SIZE * height, MAX_DAMAGE);
                break;
            }
            cursor.move(Direction.DOWN);
            current = level.getBlockState(cursor);
        }
    }

    @Nullable
    private Direction calculateTipDirection(LevelReader level, BlockPos pos, Direction wanted) {
        if (isValidPlacement(level, pos, wanted)) {
            return wanted;
        }
        return isValidPlacement(level, pos, wanted.getOpposite()) ? wanted.getOpposite() : null;
    }

    private DripstoneThickness calculateThickness(LevelReader level, BlockPos pos, Direction tipDirection, boolean tipMerge) {
        Direction baseDirection = tipDirection.getOpposite();
        BlockState tipSide = level.getBlockState(pos.relative(tipDirection));
        if (isStalactiteWithDirection(tipSide, baseDirection)) {
            return !tipMerge && tipSide.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE
                    ? DripstoneThickness.TIP : DripstoneThickness.TIP_MERGE;
        }
        if (!isStalactiteWithDirection(tipSide, tipDirection)) {
            return DripstoneThickness.TIP;
        }
        DripstoneThickness neighbourThickness = tipSide.getValue(THICKNESS);
        if (neighbourThickness == DripstoneThickness.TIP || neighbourThickness == DripstoneThickness.TIP_MERGE) {
            return DripstoneThickness.FRUSTUM;
        }
        BlockState baseSide = level.getBlockState(pos.relative(baseDirection));
        return !isStalactiteWithDirection(baseSide, tipDirection) ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
    }

    private boolean isValidPlacement(LevelReader level, BlockPos pos, Direction tipDirection) {
        BlockPos supportPos = pos.relative(tipDirection.getOpposite());
        BlockState support = level.getBlockState(supportPos);
        return support.isFaceSturdy(level, supportPos, tipDirection) || isStalactiteWithDirection(support, tipDirection);
    }

    private boolean isTip(BlockState state, boolean tipMerge) {
        if (!state.is(this)) {
            return false;
        }
        DripstoneThickness thickness = state.getValue(THICKNESS);
        return thickness == DripstoneThickness.TIP || tipMerge && thickness == DripstoneThickness.TIP_MERGE;
    }

    private boolean isStalactite(BlockState state) {
        return isStalactiteWithDirection(state, Direction.DOWN);
    }

    private boolean isStalagmite(BlockState state) {
        return isStalactiteWithDirection(state, Direction.UP);
    }

    private boolean isStalactiteWithDirection(BlockState state, Direction direction) {
        return state.is(this) && state.getValue(TIP_DIRECTION) == direction;
    }
}
