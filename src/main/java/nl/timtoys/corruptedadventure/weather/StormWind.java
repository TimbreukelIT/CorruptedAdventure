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
import nl.timtoys.corruptedadventure.network.WindGustPayload;
import nl.timtoys.corruptedadventure.ring.RingWorld;

/**
 * The push of ring 4's storm wind (package 70), the sandstorm's pattern (change 28): one direction per storm from the
 * weather's {@code seed} ({@link SandstormWind#direction}, so server, client and F3 agree), an additive gust
 * ({@link WindGustPayload}) every {@link #GUST_TICKS} to each exposed player. Only high up, on and between the islands
 * (feet at or above {@code stormPushMinY}); lower down the storm is only atmosphere. Where the storm blows follows
 * {@link RingWeather#stateAt}: ring 4 after the hard switch at 0.5 and its mythical band only.
 */
public final class StormWind {
    /** Ticks between two gusts, the sandstorm's default (the contract has no key for it). */
    static final int GUST_TICKS = 10;

    private StormWind() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(StormWind::onPlayerTick);
    }

    /**
     * Whether the storm pushes a player whose felt weather ({@link RingWeather#stateAt}) is {@code type}: storm wind,
     * feet at or above {@code minY}, not in creative or spectator and not riding something. Seeing the sky is checked
     * by the caller.
     */
    static boolean pushes(RingWeatherType type, double feetY, int minY, boolean creativeOrSpectator, boolean passenger) {
        return type == RingWeatherType.STORM_WIND && feetY >= minY && !creativeOrSpectator && !passenger;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || !player.isAlive()
                || player.tickCount % GUST_TICKS != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !RingWorld.isActive(level.getServer())) {
            return;
        }
        double pushStrength = ServerConfig.RING_4_WEATHER.stormPushStrength.get();
        if (pushStrength <= 0.0) {
            return;
        }
        RingWeatherState state = RingWeather.stateAt(level, player.getX(), player.getZ());
        if (!pushes(state.type(), player.getY(), ServerConfig.RING_4_WEATHER.stormPushMinY.get(),
                player.isCreative() || player.isSpectator(), player.isPassenger())
                || state.intensity() <= 0.0F || !level.canSeeSky(BlockPos.containing(player.getEyePosition()))) {
            return;
        }
        Vec3 push = SandstormWind.impulse(SandstormWind.direction(state), pushStrength, state.intensity(), 1.0F);
        PacketDistributor.sendToPlayer(player, new WindGustPayload((float) push.x, (float) push.z));
    }
}
