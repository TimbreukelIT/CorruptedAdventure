package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/**
 * Swamp gas (contract ring 4 §3, package 72): an invisible, walk-through, replaceable gas in the gas chambers and under
 * the peat crust; a yellow haze now and then ({@code animateTick}). Breathing it is poison (the conditions read
 * {@code #poison_gases}).
 *
 * <p>Fire, a torch, a lit campfire or candle or lava next to it, flint and steel or a fire charge on it, or an
 * explosion that reaches it ({@link SwampGasIgnition}) light it. A lit block burns on its scheduled tick and lights its
 * gas neighbours a moment later, so the fire runs through a cluster as a wave over several ticks, never recursively in
 * one tick (Q9 = A). {@link SwampGasBurn} caps the work per tick and decides which blocks explode (power
 * {@value #EXPLOSION_POWER}; block damage only with {@code mobGriefing}) and which burn off as flames. Server-side.
 */
public class SwampGasBlock extends Block {
    public static final MapCodec<SwampGasBlock> CODEC = simpleCodec(SwampGasBlock::new);
    /** Lit: burns on its next scheduled tick. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    static final float EXPLOSION_POWER = 2.5F;
    /** Ticks before a lit block's gas neighbours burn: the speed of the wave. */
    static final int SPREAD_DELAY = 2;
    /** One in this many animate ticks shows a wisp of haze. */
    private static final int HAZE_CHANCE = 5;
    /** Set state without neighbour or shape updates: lighting gas must not cascade through updates. */
    private static final int QUIET_UPDATE = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Map<Level, SwampGasBurn> BURNS = new WeakHashMap<>();

    public SwampGasBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<SwampGasBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    /** Whether {@code state} next to the gas sets it alight: open flame or lava. */
    static boolean ignites(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BaseFireBlock || block instanceof TorchBlock || state.getFluidState().getType().isSame(Fluids.LAVA)) {
            return true;
        }
        return (block instanceof CampfireBlock || block instanceof AbstractCandleBlock)
                && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    /**
     * Lights the gas at {@code pos} (if it is unlit swamp gas): it burns after {@code delay} ticks. Returns whether it
     * was lit now.
     */
    public static boolean ignite(LevelAccessor level, BlockPos pos, int delay) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SwampGasBlock) || state.getValue(LIT) || level.isClientSide()) {
            return false;
        }
        level.setBlock(pos, state.setValue(LIT, true), QUIET_UPDATE);
        level.scheduleTick(pos, state.getBlock(), Math.max(1, delay));
        return true;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide() && !state.getValue(LIT) && ignites(neighborState)) {
            level.scheduleTick(pos, this, 1);
            return state.setValue(LIT, true);
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide() || state.getValue(LIT) || oldState.is(this)) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (ignites(level.getBlockState(pos.relative(direction)))) {
                ignite(level, pos, 1);
                return;
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        SwampGasBurn.Outcome outcome = BURNS.computeIfAbsent(level, key -> new SwampGasBurn()).next(level.getGameTime());
        if (outcome == SwampGasBurn.Outcome.WAIT) {
            level.scheduleTick(pos, this, 1);
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (Direction direction : Direction.values()) {
            ignite(level, pos.relative(direction), SPREAD_DELAY);
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (outcome) {
            // Block damage follows mobGriefing (no source entity: the game rule decides, like a creeper).
            case EXPLODE -> level.explode(null, x, y, z, EXPLOSION_POWER, false, Level.ExplosionInteraction.MOB);
            case FLARE -> {
                level.playSound(null, pos, ModSoundEvents.SWAMP_GAS_IGNITE.get(), SoundSource.BLOCKS, 1.0F,
                        0.9F + random.nextFloat() * 0.2F);
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 8, 0.35, 0.35, 0.35, 0.03);
            }
            default -> level.sendParticles(ParticleTypes.FLAME, x, y, z, 3, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /** The explosion lit it ({@link SwampGasIgnition}); it burns on its own tick instead of vanishing. */
    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> dropConsumer) {
        ignite(level, pos, 1);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(HAZE_CHANCE) != 0) {
            return;
        }
        level.addParticle(ModParticleTypes.SWAMP_GAS.get(), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(), 0.0, 0.005, 0.0);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** No outline: like air, you cannot aim at the gas or break it. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return true;
    }
}
