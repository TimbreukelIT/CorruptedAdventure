package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * A geyser: erupts on a fixed rhythm of its own and throws every entity in the {@link #REACH} blocks above it upwards,
 * {@link #WARNING_TICKS} after a warning (bubbling and a small puff). Server-side through scheduled ticks; the particles
 * and sounds reach every client through vanilla packets, so all players see the same eruption.
 *
 * <p>The rhythm is a function of the world seed, the dimension, the position and the game time ({@link Rhythm}): every
 * geyser has its own interval and offset, so a field of geysers is never in step, and a tick at any other moment (its
 * chunk was unloaded, or worldgen lost the delay) falls back into the rhythm instead of shifting it. No block state or
 * block entity needed.
 *
 * <p>Placing a geyser starts its cycle; worldgen places blocks without that, so a random tick starts the cycle of a
 * geyser that has none (within about a minute of a player coming near). Scheduled ticks are saved with the chunk.
 */
public class GeyserBlock extends Block {
    public static final MapCodec<GeyserBlock> CODEC = simpleCodec(GeyserBlock::new);
    /** Shortest interval between two eruptions of one geyser, in ticks (tuning value). */
    public static final int MIN_INTERVAL_TICKS = 80;
    /** Longest interval between two eruptions of one geyser, in ticks (tuning value). */
    public static final int MAX_INTERVAL_TICKS = 200;
    /** How long the warning comes before the eruption, in ticks (tuning value). */
    public static final int WARNING_TICKS = 20;
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

    /**
     * Starts the cycle of the geyser at {@code pos}: its first tick is the next warning, so an eruption placed by a
     * player is always announced. Also for worldgen features ({@code ModBlocks.GEYSER.get().startCycle(level, pos)}), but
     * there the proto-chunk keeps only the tick, not its delay: it fires when the chunk is loaded, at any moment of the
     * rhythm, and {@link Rhythm#step} brings it back in (once in an interval that moment is an eruption without warning).
     */
    public void startCycle(WorldGenLevel level, BlockPos pos) {
        Rhythm rhythm = Rhythm.of(level, pos);
        level.scheduleTick(pos, this, Math.max(1, rhythm.ticksToWarning(rhythm.phase(level.getLevelData().getGameTime()))));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this) && level instanceof ServerLevel server) {
            startCycle(server, pos);
        }
    }

    /** Starts the cycle of a geyser without one (placed by worldgen without {@link #startCycle}). */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            startCycle(level, pos);
        }
    }

    /**
     * One scheduled tick per event: the warning, then {@link #WARNING_TICKS} later the eruption. A tick that arrives
     * late does a shortened warning, or skips the missed eruption and waits for the next warning.
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Step step = Rhythm.of(level, pos).step(level.getGameTime());
        switch (step.action()) {
            case ERUPT -> erupt(level, pos);
            case WARN -> warn(level, pos);
            case WAIT -> {
            }
        }
        level.scheduleTick(pos, this, step.delay());
    }

    /** What a scheduled tick does, and the delay to the next one. */
    enum Action { ERUPT, WARN, WAIT }

    record Step(Action action, int delay) {
    }

    /**
     * The rhythm of one geyser: its interval ({@link #MIN_INTERVAL_TICKS}..{@link #MAX_INTERVAL_TICKS}) and its offset in
     * game time, both from a hash of the position salted with the world seed and the dimension. Deterministic, and
     * neighbouring geysers (or the same coordinates in another world) get unrelated values.
     */
    record Rhythm(int interval, int offset) {
        static Rhythm of(WorldGenLevel level, BlockPos pos) {
            return of(salt(level.getSeed(), level.getLevel().dimension().location().hashCode()), pos);
        }

        static long salt(long seed, int dimensionHash) {
            return HashCommon.mix(seed + dimensionHash);
        }

        static Rhythm of(long salt, BlockPos pos) {
            long hash = HashCommon.mix(pos.asLong() ^ salt);
            int interval = MIN_INTERVAL_TICKS + (int) Math.floorMod(hash, (long) (MAX_INTERVAL_TICKS - MIN_INTERVAL_TICKS + 1));
            return new Rhythm(interval, (int) Math.floorMod(hash >>> 32, (long) interval));
        }

        /** Where in the cycle the geyser is at {@code gameTime}: 0 is the eruption, {@code interval - WARNING_TICKS} the warning. */
        int phase(long gameTime) {
            return (int) Math.floorMod(gameTime + offset, (long) interval);
        }

        /** Ticks from {@code phase} to the next warning; 0 when the warning is now. */
        int ticksToWarning(int phase) {
            return Math.floorMod(interval - WARNING_TICKS - phase, interval);
        }

        /**
         * What the tick at {@code gameTime} does, see {@link GeyserBlock#tick}. Any moment is safe: a late warning is shortened (the
         * eruption stays on time), a tick between warning and eruption waits for the next warning.
         */
        Step step(long gameTime) {
            int phase = phase(gameTime);
            if (phase == 0) {
                return new Step(Action.ERUPT, interval - WARNING_TICKS);
            }
            if (phase >= interval - WARNING_TICKS) {
                return new Step(Action.WARN, interval - phase);
            }
            return new Step(Action.WAIT, ticksToWarning(phase));
        }
    }

    /** Bubbling and a small puff of steam; the eruption follows. Silent when the vent is covered. */
    private static void warn(ServerLevel level, BlockPos pos) {
        if (!ventOpen(level, pos)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.BUBBLE_POP, x, pos.getY() + 1.05, z, 8, 0.2, 0.0, 0.2, 0.02);
        level.sendParticles(ModParticleTypes.STEAM_PUFF.get(), x, pos.getY() + 1.1, z, 4, 0.15, 0.1, 0.15, 0.02);
        level.playSound(null, pos.above(), ModSoundEvents.GEYSER_WARNING.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private static void erupt(ServerLevel level, BlockPos pos) {
        if (!ventOpen(level, pos)) {
            return;
        }
        for (Entity entity : level.getEntities((Entity) null, column(pos),
                entity -> canBeLaunched(entity) && overVent(pos, entity.position()))) {
            entity.setDeltaMovement(launched(entity.getDeltaMovement()));
            entity.hurtMarked = true;
            entity.resetFallDistance();
        }
        level.sendParticles(ModParticleTypes.STEAM_PUFF.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                24, 0.2, 0.8, 0.2, 0.12);
        level.playSound(null, pos.above(), ModSoundEvents.GEYSER_BURST.get(), SoundSource.BLOCKS, 1.0F,
                0.9F + level.random.nextFloat() * 0.2F);
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
            level.addParticle(ModParticleTypes.STEAM_PUFF.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
        }
    }
}
