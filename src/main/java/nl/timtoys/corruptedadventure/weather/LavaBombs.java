package nl.timtoys.corruptedadventure.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The lava bombs of an eruption (ring 3, package 45): vanilla small fireballs that fall from the sky at random spots
 * around every player in ring 3 under the open sky, more often in {@code #heat_peaks}. Ordinary server entities, so
 * every client sees the same bombs. They never explode; the fire on landing follows {@code mobGriefing} (a vanilla
 * small fireball without an owner would always set fire).
 * <p>
 * The count is an accumulator per player, checked once a second: {@code lavaBombsPerMinute} × fade × peak factor, so
 * one player never gets more than {@code lavaBombsPerMinute × lavaBombPeakFactor} bombs a minute.
 */
public final class LavaBombs {
    /** Entity tag of our bombs (saved with the entity, so a reloaded bomb keeps following {@code mobGriefing}). */
    public static final String TAG = CorruptedAdventure.MOD_ID + ".lava_bomb";
    private static final int CHECK_TICKS = 20;
    /** A bomb starts this many blocks (plus up to {@link #EXTRA_DROP}) above the ground where it lands. */
    private static final int MIN_DROP = 40;
    private static final int EXTRA_DROP = 20;
    /** At least this much fall, else there is no room for a bomb (a roof of the world, the build limit). */
    private static final int MIN_FALL = 12;
    /** Largest horizontal offset of the start from the landing spot: the bombs come in at an angle. */
    private static final double MAX_SLANT = 16.0;

    /** Fractional bombs carried over per player (main server thread only). */
    private static final Map<UUID, Double> CARRY = new HashMap<>();

    private LavaBombs() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LavaBombs::onPlayerTick);
        gameBus.addListener(LavaBombs::onImpact);
        gameBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> CARRY.remove(event.getEntity().getUUID()));
        gameBus.addListener(ServerStoppedEvent.class, event -> CARRY.clear());
    }

    /** Bombs per second around one player: per minute ÷ 60 × the eruption's fade × the peak factor in a peak. */
    static double perSecond(double perMinute, float intensity, boolean peak, double peakFactor) {
        return Math.max(0.0, perMinute / 60.0 * intensity * (peak ? peakFactor : 1.0));
    }

    /** One second of the accumulator: the whole bombs to drop now and the fraction carried to the next second. */
    static Release release(double carry, double perSecond) {
        double total = carry + perSecond;
        int count = (int) Math.floor(total);
        return new Release(count, total - count);
    }

    record Release(int count, double carry) {
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK_TICKS != 0) {
            return;
        }
        double perSecond = eligible(player) ? rate(player) : 0.0;
        if (perSecond <= 0.0) {
            CARRY.remove(player.getUUID());
            return;
        }
        Release release = release(CARRY.getOrDefault(player.getUUID(), 0.0), perSecond);
        CARRY.put(player.getUUID(), release.carry());
        for (int i = 0; i < release.count(); i++) {
            drop(player.serverLevel(), player, player.getRandom());
        }
    }

    /** A real living player (spectators excluded) in ring 3 of a ring world's overworld who sees the sky. */
    private static boolean eligible(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        return !(player instanceof FakePlayer) && player.isAlive() && !player.isSpectator()
                && level.dimension() == Level.OVERWORLD && RingWorld.isActive(level.getServer())
                && RingLayout.ringAt(level.dimension(), player.getX(), player.getZ()).ring() == Ring.RING_3
                && level.canSeeSky(BlockPos.containing(player.getEyePosition()));
    }

    private static double rate(ServerPlayer player) {
        RingWeatherState state = RingWeather.state(player.serverLevel(), Ring.RING_3);
        if (state.type() != RingWeatherType.ERUPTION) {
            return 0.0;
        }
        ServerConfig.Ring3WeatherEffects config = ServerConfig.RING_3_WEATHER;
        boolean peak = player.serverLevel().getBiome(player.blockPosition()).is(ModTags.Biomes.HEAT_PEAKS);
        return perSecond(config.lavaBombsPerMinute.get(), state.intensity(), peak, config.lavaBombPeakFactor.get());
    }

    /**
     * One bomb: a spot uniform in the disc of {@code lavaBombRadius} around the player, in ring 3 and loaded; it starts
     * 40–60 blocks above the ground there, up to 16 blocks to the side, and flies straight at it (a small fireball has
     * no gravity). A spot that does not qualify is skipped, not retried.
     */
    private static void drop(ServerLevel level, ServerPlayer player, RandomSource random) {
        double distance = ServerConfig.RING_3_WEATHER.lavaBombRadius.get() * Math.sqrt(random.nextDouble());
        double angle = random.nextDouble() * Mth.TWO_PI;
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))
                || RingLayout.ringAt(level.dimension(), x + 0.5, z + 0.5).ring() != Ring.RING_3) {
            return;
        }
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        double startY = Math.min(ground + MIN_DROP + random.nextInt(EXTRA_DROP + 1), level.getMaxBuildHeight() - 1);
        double slant = random.nextDouble() * MAX_SLANT;
        double slantAngle = random.nextDouble() * Mth.TWO_PI;
        double startX = x + 0.5 + Math.cos(slantAngle) * slant;
        double startZ = z + 0.5 + Math.sin(slantAngle) * slant;
        if (startY - ground < MIN_FALL || !level.hasChunkAt(BlockPos.containing(startX, startY, startZ))) {
            return;
        }
        Vec3 movement = new Vec3(x + 0.5 - startX, ground - startY, z + 0.5 - startZ);
        SmallFireball bomb = new SmallFireball(level, startX, startY, startZ, movement);
        bomb.addTag(TAG);
        level.addFreshEntity(bomb);
        CorruptedAdventure.LOGGER.debug("[lava bomb] {}: lands near {} {} {}", player.getGameProfile().getName(), x, ground, z);
    }

    /** Landing on a block: a splash of lava for everyone; the fire is vanilla's, unless {@code mobGriefing} is off. */
    private static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level) || !projectile.getTags().contains(TAG)
                || event.getRayTraceResult().getType() != HitResult.Type.BLOCK) {
            return;
        }
        Vec3 at = event.getRayTraceResult().getLocation();
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 6, 0.3, 0.2, 0.3, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.3, at.z, 4, 0.3, 0.2, 0.3, 0.02);
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            event.setCanceled(true);
            projectile.discard();
        }
    }
}
