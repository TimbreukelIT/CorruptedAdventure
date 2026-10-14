package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A walkable lily pad of 2 × 2 blocks (decision golf 1): four parts that live and die together, like a bed or a door.
 * Each part behaves like vanilla's lily pad (on water, boats break it); when one part goes, the others vanish with it.
 * Only the {@link Part#NORTH_WEST} part carries the loot, so the whole pad drops one item. A {@link BushBlock} with
 * {@link WaterlilyBlock}'s rules, because that class fixes its codec type.
 */
public class GiantLilyPadBlock extends BushBlock {
    public static final MapCodec<GiantLilyPadBlock> CODEC = simpleCodec(GiantLilyPadBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    /** The whole quarter: the four parts make one closed pad to walk on. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.5, 16.0);

    public GiantLilyPadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, Part.NORTH_WEST));
    }

    @Override
    public MapCodec<GiantLilyPadBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Vanilla's lily pad rule: on still water or ice, with nothing liquid in its own spot. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return (level.getFluidState(pos).getType() == Fluids.WATER || state.getBlock() instanceof IceBlock)
                && level.getFluidState(pos.above()).getType() == Fluids.EMPTY;
    }

    /** Like vanilla's lily pad: a boat breaks it (and with it the whole pad). */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level instanceof ServerLevel && entity instanceof Boat) {
            level.destroyBlock(pos, true, entity);
        }
    }

    /**
     * The clicked spot is the corner next to the player; the pad extends in the player's facing and to the right of it.
     * {@code null} (no placement) unless all four spots can take a lily pad.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos clicked = context.getClickedPos();
        BlockPos northWest = northWestCorner(clicked, facing);
        Level level = context.getLevel();
        // The item checks the clicked spot itself (block and entities); the other three get the same checks here.
        CollisionContext collision = context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer());
        for (Part part : Part.values()) {
            BlockPos pos = part.from(northWest);
            if (!pos.equals(clicked) && (!level.getBlockState(pos).canBeReplaced(context)
                    || !level.isUnobstructed(defaultBlockState(), pos, collision))) {
                return null;
            }
            if (!level.getWorldBorder().isWithinBounds(pos) || !mayPlaceOn(level.getBlockState(pos.below()), level, pos.below())) {
                return null;
            }
        }
        return defaultBlockState().setValue(PART, Part.at(northWest, clicked));
    }

    /** The north-west corner of the pad placed at {@code clicked} by a player facing {@code facing}. */
    static BlockPos northWestCorner(BlockPos clicked, Direction facing) {
        BlockPos far = clicked.relative(facing).relative(facing.getClockWise());
        return new BlockPos(Math.min(clicked.getX(), far.getX()), clicked.getY(), Math.min(clicked.getZ(), far.getZ()));
    }

    /** Places the three other parts around the part the item placed. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockPos northWest = state.getValue(PART).northWest(pos);
            for (Part part : Part.values()) {
                if (part != state.getValue(PART)) {
                    level.setBlock(part.from(northWest), state.setValue(PART, part), Block.UPDATE_ALL);
                }
            }
        }
    }

    /** A part whose sibling is gone, or that lost its water, disappears; the next update takes the rest along. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        if (direction.getAxis().isHorizontal() && state.getValue(PART).hasSibling(direction)
                && !(neighbour.is(this) && neighbour.getValue(PART) == state.getValue(PART).sibling(direction))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    /** A rotated pad (structures, commands) keeps its parts in place: each part turns with its position. */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(PART, state.getValue(PART).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(PART, state.getValue(PART).mirror(mirror));
    }

    /** In creative nothing drops: the loot part is removed first without drops (like the head of a bed). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) != Part.NORTH_WEST) {
            BlockPos northWest = state.getValue(PART).northWest(pos);
            BlockState loot = level.getBlockState(northWest);
            if (loot.is(this) && loot.getValue(PART) == Part.NORTH_WEST) {
                level.setBlock(northWest, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, northWest, Block.getId(loot));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** The quarter of the pad; north is −z and west is −x, as everywhere in Minecraft. */
    public enum Part implements StringRepresentable {
        NORTH_WEST("north_west", 0, 0),
        NORTH_EAST("north_east", 1, 0),
        SOUTH_WEST("south_west", 0, 1),
        SOUTH_EAST("south_east", 1, 1);

        private final String name;
        private final int dx;
        private final int dz;

        Part(String name, int dx, int dz) {
            this.name = name;
            this.dx = dx;
            this.dz = dz;
        }

        /** This part's position in the pad with its north-west corner at {@code northWest}. */
        public BlockPos from(BlockPos northWest) {
            return northWest.offset(dx, 0, dz);
        }

        /** The north-west corner of the pad that has this part at {@code pos}. */
        public BlockPos northWest(BlockPos pos) {
            return pos.offset(-dx, 0, -dz);
        }

        /** The part in the corner {@code rotation} turns this one into (the pad turns around its centre). */
        public Part rotate(Rotation rotation) {
            return of(rotation.rotate(northSouth()), rotation.rotate(eastWest()));
        }

        /** The part in the corner {@code mirror} reflects this one into. */
        public Part mirror(Mirror mirror) {
            return of(mirror.mirror(northSouth()), mirror.mirror(eastWest()));
        }

        private Direction northSouth() {
            return dz == 0 ? Direction.NORTH : Direction.SOUTH;
        }

        private Direction eastWest() {
            return dx == 0 ? Direction.WEST : Direction.EAST;
        }

        /** The part in the corner of these two horizontal sides (one north/south, one east/west, in any order). */
        private static Part of(Direction first, Direction second) {
            boolean east = first == Direction.EAST || second == Direction.EAST;
            boolean south = first == Direction.SOUTH || second == Direction.SOUTH;
            for (Part part : values()) {
                if ((part.dx == 1) == east && (part.dz == 1) == south) {
                    return part;
                }
            }
            throw new IllegalStateException();
        }

        static Part at(BlockPos northWest, BlockPos pos) {
            for (Part part : values()) {
                if (part.from(northWest).equals(pos)) {
                    return part;
                }
            }
            throw new IllegalArgumentException(pos + " is not in the pad at " + northWest);
        }

        /** Whether the pad continues on the {@code direction} side of this part. */
        boolean hasSibling(Direction direction) {
            return switch (direction) {
                case WEST -> dx == 1;
                case EAST -> dx == 0;
                case NORTH -> dz == 1;
                case SOUTH -> dz == 0;
                default -> false;
            };
        }

        /** The part on the {@code direction} side (only where {@link #hasSibling} holds). */
        Part sibling(Direction direction) {
            int x = dx + direction.getStepX();
            int z = dz + direction.getStepZ();
            for (Part part : values()) {
                if (part.dx == x && part.dz == z) {
                    return part;
                }
            }
            throw new IllegalArgumentException(this + " has no part to the " + direction);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
