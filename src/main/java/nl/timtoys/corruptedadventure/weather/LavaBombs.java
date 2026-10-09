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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The lava bombs of an eruption (ring 3, package 45): vanilla small fireballs that fall from the sky at random spots
 * around every player in ring 3 under the open sky, more often in {@code #heat_peaks}. Ordinary server entities, so
 * every client sees the same bombs. They never explode; the fire on landing follows {@code mobGriefing} (a vanilla
 * small fireball without an owner would always set fire).
 * <p>
 * The count is an accumulator per player, checked once a second: {@code lavaBombsPerMinute} × fade × peak factor, so
 * one player never gets more than {@code lavaBombsPerMinute × lavaBombPeakFactor} bombs a minute.
 * <p>
 * Several players (package 50c): every player keeps their own disc, but a spot that lies in the discs of {@code k}
 * qualifying players is kept with chance 1/k. Each of the k players offers it, so the bombs per area stay those of one
 * player everywhere: where discs overlap, and in the parts that do not. Standing together is not safer, nor more
 * dangerous. Who qualifies is worked out once a second for all players ({@link #onServerTick}).
 * <p>
 * A bomb lives at most {@link #MAX_AGE_TICKS}: one in a chunk that no longer ticks (its player went away) would hang
 * still in the air, so the server removes it, and a bomb saved with its chunk does not come back when it is loaded.
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
    /** A falling bomb lands within a few seconds; one this old hangs in a chunk that does not tick and is removed. */
    static final int MAX_AGE_TICKS = 200;

    /** Fractional bombs carried over per player (main server thread only). */
    private static final Map<UUID, Double> CARRY = new HashMap<>();
    /** Our bombs in flight with the game time they were dropped (main server thread only). */
    private static final List<Flight> FLIGHTS = new ArrayList<>();
    /** The players who qualify for bombs, refreshed once a second (main server thread only). */
    private static final Set<ServerPlayer> ELIGIBLE = new HashSet<>();

    private LavaBombs() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LavaBombs::onPlayerTick);
        gameBus.addListener(LavaBombs::onImpact);
        gameBus.addListener(LavaBombs::onServerTick);
        gameBus.addListener(LavaBombs::onJoinLevel);
        gameBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            CARRY.remove(event.getEntity().getUUID());
            ELIGIBLE.remove(event.getEntity());
        });
        gameBus.addListener(ServerStoppedEvent.class, event -> {
            CARRY.clear();
            FLIGHTS.clear();
            ELIGIBLE.clear();
        });
    }

    /** Bombs per second around one player: per minute ÷ 60 × the eruption's fade × the peak factor in a peak. */
    static double perSecond(double perMinute, float intensity, boolean peak, double peakFactor) {
        return Math.max(0.0, perMinute / 60.0 * intensity * (peak ? peakFactor : 1.0));
    }

    /** How many of {@code centres} (qualifying players) have the spot (x, z) in their disc of {@code radius}. */
    static int covering(double x, double z, double radius, List<Vec3> centres) {
        int count = 0;
        for (Vec3 centre : centres) {
            if (Mth.lengthSquared(centre.x - x, centre.z - z) <= radius * radius) {
                count++;
            }
        }
        return count;
    }

    /** Whether a spot in {@code covering} discs is kept, for a {@code roll} uniform in [0, 1): chance 1/covering. */
    static boolean kept(int covering, double roll) {
        return roll * Math.max(1, covering) < 1.0;
    }

    /** Whether a bomb dropped at {@code born} is past its life at {@code now}. */
    static boolean expired(long born, long now) {
        return now - born >= MAX_AGE_TICKS;
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
        double perSecond = ELIGIBLE.contains(player) ? rate(player) : 0.0;
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
     * no gravity). A spot that does not qualify, or that another player's disc covers and is not kept ({@link #kept}),
     * is skipped, not retried.
     */
    private static void drop(ServerLevel level, ServerPlayer player, RandomSource random) {
        double radius = ServerConfig.RING_3_WEATHER.lavaBombRadius.get();
        double distance = radius * Math.sqrt(random.nextDouble());
        double angle = random.nextDouble() * Mth.TWO_PI;
        double spotX = player.getX() + Math.cos(angle) * distance;
        double spotZ = player.getZ() + Math.sin(angle) * distance;
        int x = Mth.floor(spotX);
        int z = Mth.floor(spotZ);
        List<Vec3> centres = ELIGIBLE.stream().filter(other -> other.serverLevel() == level).map(ServerPlayer::position).toList();
        if (!kept(covering(spotX, spotZ, radius, centres), random.nextDouble())
                || !level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))
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
        if (level.addFreshEntity(bomb)) {
            FLIGHTS.add(new Flight(bomb, level.getGameTime()));
        }
        CorruptedAdventure.LOGGER.debug("[lava bomb] {}: lands near {} {} {}", player.getGameProfile().getName(), x, ground, z);
    }

    /**
     * Once a second: who qualifies for bombs (one pass over all players), forget the bombs that landed, and remove
     * those past their life (in a chunk that stopped ticking).
     */
    private static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        ELIGIBLE.clear();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (eligible(player)) {
                ELIGIBLE.add(player);
            }
        }
        FLIGHTS.removeIf(flight -> {
            if (flight.bomb().isRemoved()) {
                return true;
            }
            if (expired(flight.born(), flight.bomb().level().getGameTime())) {
                flight.bomb().discard();
                return true;
            }
            return false;
        });
    }

    /** A bomb saved with its chunk is stale by the time the chunk loads again: it does not come back. */
    private static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && !event.getLevel().isClientSide() && event.getEntity().getTags().contains(TAG)) {
            event.setCanceled(true);
        }
    }

    private record Flight(SmallFireball bomb, long born) {
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
