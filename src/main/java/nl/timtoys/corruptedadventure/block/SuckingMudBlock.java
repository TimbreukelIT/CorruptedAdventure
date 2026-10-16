package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * Sucking mud, deadly quicksand (Q12 = B, G1–G3 of golf 2): a living entity has no floor in it and sinks about one
 * block per two seconds, moving at about 0.4× its walking speed; holding jump lifts it slowly, so it can climb out
 * against the bank, also with its head under. Items, boats and falling blocks lie on top (the mud is solid for them).
 * The suffocation with the head under is {@link SuckingMudSuffocation} (server).
 *
 * <p>The motion runs in {@code entityInside} on both sides, like vanilla's bubble column and powder snow: the client
 * moves its own player, the server the mobs. It sets the velocity of the next tick exactly, compensating the gravity
 * that {@code LivingEntity.travel} subtracts after the move. Always recognisable (G1 = A): T's wet, glossy texture, a
 * bubble now and then and a slurp ({@code animateTick}).
 */
public class SuckingMudBlock extends Block {
    public static final MapCodec<SuckingMudBlock> CODEC = simpleCodec(SuckingMudBlock::new);
    /** Sinking per tick: about one block per two seconds (G2 = A). */
    static final double SINK_PER_TICK = 0.025;
    /** Rising per tick while jumping: slowly out (about 0.8 blocks a second). */
    static final double RISE_PER_TICK = 0.04;
    /**
     * Horizontal velocity kept per tick. With the air control vanilla gives a falling entity (0.02 per tick, friction
     * 0.91) this settles at about 0.4× walking speed.
     */
    static final double HORIZONTAL_DRAG = 0.87;
    /** Vertical drag {@code LivingEntity.travel} applies after the gravity. */
    private static final double VERTICAL_DRAG = 0.98;
    /** Vanilla's gravity cap under Slow Falling ({@code LivingEntity.travel}). */
    private static final double SLOW_FALLING_GRAVITY = 0.01;
    /** One in this many animate ticks of a block with air above shows a bubble. */
    private static final int BUBBLE_CHANCE = 12;
    /** One in this many animate ticks tries the slurp; the limiter thins a large patch further. */
    private static final int SLURP_CHANCE = 80;
    private static final AmbientSoundLimiter SLURP = new AmbientSoundLimiter(30);

    public SuckingMudBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SuckingMudBlock> codec() {
        return CODEC;
    }

    /** No floor for living entities (they sink); solid for everything else, so items do not sink. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof LivingEntity) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living) || living.isSpectator() || living.isFallFlying()
                || living instanceof Player player && player.getAbilities().flying
                || !ColumnContact.isTheOne(level, pos, entity, this)) {
            return;
        }
        living.resetFallDistance();
        Vec3 motion = living.getDeltaMovement();
        living.setDeltaMovement(motion.x * HORIZONTAL_DRAG,
                nextVerticalSpeed(living.jumping, travelGravity(living.getGravity(), motion.y, living.hasEffect(MobEffects.SLOW_FALLING))),
                motion.z * HORIZONTAL_DRAG);
    }

    /**
     * The gravity {@code LivingEntity.travel} subtracts after this move: Slow Falling caps it at 0.01 while the entity is
     * not rising ({@code vertical} is the velocity it moved with, which travel checks). Without this, Slow Falling would
     * let an entity float out of the mud.
     */
    static double travelGravity(double gravity, double vertical, boolean slowFalling) {
        return slowFalling && vertical <= 0.0 ? Math.min(gravity, SLOW_FALLING_GRAVITY) : gravity;
    }

    /**
     * The vertical velocity to set now so the next tick moves exactly {@link #RISE_PER_TICK} up (jumping) or
     * {@link #SINK_PER_TICK} down: travel subtracts {@code gravity} and multiplies by 0.98 before that move.
     */
    static double nextVerticalSpeed(boolean jumping, double gravity) {
        return (jumping ? RISE_PER_TICK : -SINK_PER_TICK) / VERTICAL_DRAG + gravity;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (random.nextInt(BUBBLE_CHANCE) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.15 + random.nextDouble() * 0.7, pos.getY() + 1.0,
                    pos.getZ() + 0.15 + random.nextDouble() * 0.7, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(SLURP_CHANCE) == 0 && SLURP.tryAcquire(level, pos)) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ModSoundEvents.SUCKING_MUD_SLURP.get(),
                    SoundSource.BLOCKS, 0.6F, 0.85F + random.nextFloat() * 0.3F, false);
        }
    }
}
