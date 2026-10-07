package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A geyser: every {@link #INTERVAL_TICKS} it erupts and throws every entity in the {@link #REACH} blocks above it
 * upwards. Server-side through scheduled ticks; the particles and sound reach the clients through vanilla packets.
 * Placing a geyser starts its cycle; worldgen places blocks without that, so a random tick starts the cycle of a geyser
 * that has none (within about a minute of a player coming near). Scheduled ticks are saved with the chunk.
 */
public class GeyserBlock extends Block {
    public static final MapCodec<GeyserBlock> CODEC = simpleCodec(GeyserBlock::new);
    /** Ticks between two eruptions (tuning value). */
    public static final int INTERVAL_TICKS = 100;
    /** Height of the column above the geyser in which entities are thrown up, in blocks (tuning value). */
    public static final int REACH = 4;
    /** Upward speed an eruption gives, in blocks per tick: about five blocks high (tuning value). */
    public static final double LAUNCH_SPEED = 0.9;

    public GeyserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends GeyserBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, INTERVAL_TICKS);
        }
    }

    /** Starts the cycle of a geyser without one (placed by worldgen). */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, INTERVAL_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        erupt(level, pos);
        level.scheduleTick(pos, this, INTERVAL_TICKS);
    }

    private static void erupt(ServerLevel level, BlockPos pos) {
        if (!ventOpen(level, pos)) {
            return;
        }
        BlockPos vent = pos.above();
        for (Entity entity : level.getEntities((Entity) null, column(pos),
                entity -> canBeLaunched(entity) && overVent(pos, entity.position()))) {
            entity.setDeltaMovement(launched(entity.getDeltaMovement()));
            entity.hurtMarked = true;
            entity.resetFallDistance();
        }
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 24, 0.2, 0.8, 0.2, 0.12);
        level.playSound(null, vent, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.8F, 0.6F);
    }

    /**
     * A vent is open while nothing with a collision shape lies on it (a thin layer, a plant or water does not close it).
     * The eruption and the steam use the same rule, so the steam always warns before a push.
     */
    public static boolean ventOpen(BlockGetter level, BlockPos pos) {
        BlockPos vent = pos.above();
        return level.getBlockState(vent).getCollisionShape(level, vent).isEmpty();
    }

    /** Whether an entity stands over the vent with its centre, not just with the edge of its hitbox from next door. */
    static boolean overVent(BlockPos pos, Vec3 entityPosition) {
        return Mth.floor(entityPosition.x) == pos.getX() && Mth.floor(entityPosition.z) == pos.getZ();
    }

    /** The {@link #REACH} blocks above the geyser. */
    static AABB column(BlockPos pos) {
        return new AABB(pos.above()).expandTowards(0.0, REACH - 1, 0.0);
    }

    /** Spectators and flying players are left alone. */
    static boolean canBeLaunched(Entity entity) {
        return !entity.isSpectator() && !(entity instanceof Player player && player.getAbilities().flying);
    }

    /** The motion after an eruption: at least {@link #LAUNCH_SPEED} upwards, horizontal motion kept. */
    static Vec3 launched(Vec3 motion) {
        return new Vec3(motion.x, Math.max(motion.y, LAUNCH_SPEED), motion.z);
    }

    /**
     * Landing back on the geyser does no fall damage: the eruption throws its riders about five blocks high every few
     * seconds, and a mob standing on it would otherwise slowly die. The fall distance is still reset on landing.
     */
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
    }

    /** A thin wisp of steam between eruptions (client-side; vanilla only calls this on the client). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0 && ventOpen(level, pos)) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.04, 0.0);
        }
    }
}
