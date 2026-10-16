package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * The updraft, the lift from a lake up to an island (contract ring 4 §3, Q11 = A): a walk-through, see-through column
 * of rising streaks that carries players, mobs and items up. Shared {@code entityInside} code like vanilla's bubble
 * column: it runs on both sides (the client moves its own player, the server the rest) without a client class. Unlike
 * the bubble column it works in the air, so the push includes the entity's own gravity.
 */
public class UpdraftBlock extends Block {
    public static final MapCodec<UpdraftBlock> CODEC = simpleCodec(UpdraftBlock::new);
    /** Gain in upward speed per tick (blocks per tick), on top of cancelling gravity. */
    static final double LIFT_PER_TICK = 0.05;
    /** Top speed (blocks per tick, ~8 blocks a second), reached after about eight ticks. */
    static final double MAX_RISE = 0.4;
    /** One in this many animate ticks shows a rising bubble. */
    private static final int BUBBLE_CHANCE = 3;
    private static final int FLOW_SOUND_CHANCE = 60;
    private static final AmbientSoundLimiter FLOW = new AmbientSoundLimiter(40);

    public UpdraftBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<UpdraftBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.isSpectator() || entity instanceof Player player && player.getAbilities().flying
                || !ColumnContact.isTheOne(level, pos, entity, this)) {
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, nextVerticalSpeed(motion.y, entity.getGravity()), motion.z);
        entity.resetFallDistance();
    }

    /** One tick of lift: {@link #LIFT_PER_TICK} faster, at most {@link #MAX_RISE}, plus the gravity the entity will lose. */
    static double nextVerticalSpeed(double vertical, double gravity) {
        return Math.min(vertical + LIFT_PER_TICK, MAX_RISE) + gravity;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(BUBBLE_CHANCE) == 0) {
            level.addParticle(ModParticleTypes.UPDRAFT_BUBBLE.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + random.nextDouble(), pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.05, 0.0);
        }
        if (random.nextInt(FLOW_SOUND_CHANCE) == 0 && FLOW.tryAcquire(level, pos)) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSoundEvents.UPDRAFT_FLOW.get(),
                    SoundSource.BLOCKS, 0.5F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }

    /** A column draws only its outside: faces between two updraft blocks are hidden. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }

    /** No outline, like the bubble column: you aim through it. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
