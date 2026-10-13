package nl.timtoys.corruptedadventure.weather;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.network.RingPacketDistributor;
import nl.timtoys.corruptedadventure.network.WindGustPayload;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.ring.RingWorld;

import java.util.Locale;

/**
 * The wind of a sandstorm: its direction (one per storm, from the weather's {@code seed}; the server push, the client
 * particles and F3 all use {@link #direction}, so every client sees the same wind) and the push. The server decides
 * every gust and sends it as an additive impulse ({@link WindGustPayload}) to each exposed player in ring 2, the way
 * vanilla sends explosion knockback: the client adds it to its own movement instead of being overwritten.
 */
public final class SandstormWind {
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private SandstormWind() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SandstormWind::onPlayerTick);
    }

    /** Where the wind comes from, in compass degrees (0 = north, 90 = east), like a weather report. */
    public static int fromDegrees(RingWeatherState state) {
        return Math.floorMod(mix(state.seed()), 360);
    }

    /**
     * Whether a weather type has a wind from its seed (the direction of {@link #direction}): the sandstorm and ring 4's
     * storm wind (package 70). For F3, {@code /ca weather} and the client debug log.
     */
    public static boolean hasWind(RingWeatherType type) {
        return type == RingWeatherType.SANDSTORM || type == RingWeatherType.STORM_WIND;
    }

    /** Horizontal unit vector the wind blows towards. */
    public static Vec3 direction(RingWeatherState state) {
        return direction(fromDegrees(state));
    }

    /** Blowing away from {@code fromDegrees}: north (−Z) at 0, east (+X) at 90. */
    static Vec3 direction(int fromDegrees) {
        double radians = Math.toRadians(fromDegrees);
        return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
    }

    /** E.g. {@code 225° (from SW)}; for F3, {@code /ca weather} and the client debug log. */
    public static String describe(RingWeatherState state) {
        int degrees = fromDegrees(state);
        return String.format(Locale.ROOT, "%d° (from %s)", degrees, COMPASS[Math.floorMod(Math.round(degrees / 45.0F), 8)]);
    }

    /** One gust in blocks per tick; 0 without intensity or exposure. */
    public static Vec3 impulse(Vec3 direction, double pushStrength, float intensity, float exposure) {
        return direction.scale(pushStrength * intensity * exposure);
    }

    /**
     * Who the wind pushes: only players in ring 2 after the hard switch at 0.5 ({@code CORE}, like all gameplay since
     * change 20), not in creative or spectator and not riding something.
     */
    static boolean pushes(RingPosition position, boolean creativeOrSpectator, boolean passenger) {
        return !creativeOrSpectator && !passenger
                && RingPacketDistributor.matches(position, Ring.RING_2, RingPacketDistributor.RingMembership.CORE);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || !player.isAlive()
                || player.tickCount % ServerConfig.RING_2_WEATHER.sandstormGustTicks.get() != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !RingWorld.isActive(level.getServer())) {
            return;
        }
        RingWeatherState state = RingWeather.state(level, Ring.RING_2);
        double pushStrength = ServerConfig.RING_2_WEATHER.sandstormPushStrength.get();
        if (state.type() != RingWeatherType.SANDSTORM || state.intensity() <= 0.0F || pushStrength <= 0.0) {
            return;
        }
        RingPosition position = RingLayout.ringAt(level.dimension(), player.getX(), player.getZ());
        // Exposure on the server is on/off: the same "full sky light at eye height" as direct sun for dehydration.
        if (!pushes(position, player.isCreative() || player.isSpectator(), player.isPassenger())
                || !level.canSeeSky(BlockPos.containing(player.getEyePosition()))) {
            return;
        }
        Vec3 push = impulse(direction(state), pushStrength, state.intensity(), 1.0F);
        PacketDistributor.sendToPlayer(player, new WindGustPayload((float) push.x, (float) push.z));
    }

    /** Spreads consecutive seeds over the circle (murmur3 finaliser). */
    private static int mix(int seed) {
        int h = seed;
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h;
    }
}
