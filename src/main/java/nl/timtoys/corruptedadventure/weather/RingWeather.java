package nl.timtoys.corruptedadventure.weather;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.condition.ConditionDefinitions;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.network.RingWeatherPayload;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingWorld;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server side of the ring weather: the cycle (every 20 ticks, paused while {@code doWeatherCycle} is off), the sync and
 * the API for gameplay (conditions read the weather through {@link #state} / {@link #stateAt}). Only in ring worlds and
 * only in dimensions with weather types (now the overworld); everywhere else every ring is {@link RingWeatherState#STILL}.
 * <p>
 * Every player gets the full weather of their dimension; the client picks the ring(s) at its own position.
 */
public final class RingWeather {
    private static final int CYCLE_INTERVAL = 20;

    private RingWeather() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RingWeather::onLevelTick);
        // The gameplay effects of the weather (change 28; later 45, 66, 85 the same way).
        SandstormWind.register(gameBus);
        ConditionDefinitions.addModifier(new HeatWaveDehydrationModifier());
        // Ring 3 (package 45): the eruption's lava bombs. Its heat modifiers belong to the conditions (stream C).
        LavaBombs.register(gameBus);
        // Ring 3 (package 50b): the volcanoes near each player, for the smoke plumes the client draws.
        VolcanoPlumes.register(gameBus);
        gameBus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> sendTo(event.getEntity()));
        gameBus.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> sendTo(event.getEntity()));
        gameBus.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> sendTo(event.getEntity()));
    }

    /** The weather data of this level; empty outside ring worlds and in dimensions without ring weather. */
    public static Optional<RingWeatherData> data(ServerLevel level) {
        if (!RingWeatherType.hasWeather(level.dimension()) || !RingWorld.isActive(level.getServer())) {
            return Optional.empty();
        }
        return Optional.of(level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                () -> RingWeatherData.create(level.dimension(), ring -> fresh(level, ring)),
                (tag, registries) -> RingWeatherData.load(tag, level.dimension(), ring -> fresh(level, ring))),
                RingWeatherData.FILE_NAME));
    }

    /** The weather of a ring in this level. */
    public static RingWeatherState state(ServerLevel level, Ring ring) {
        return data(level).map(data -> data.get(ring)).orElse(RingWeatherState.STILL);
    }

    /**
     * The weather at a position: the ring of {@code ringAt(…).ring()} (hard switch at 0.5, like the biome); the void
     * follows ring 5. For gameplay; rendering blends over the transition zone instead (client).
     */
    public static RingWeatherState stateAt(ServerLevel level, double x, double z) {
        Optional<RingWeatherData> data = data(level);
        if (data.isEmpty()) {
            return RingWeatherState.STILL;
        }
        return data.get().get(weatherRing(RingLayout.ringAt(level.dimension(), x, z).ring()));
    }

    /** The ring whose weather applies: the void has none of its own and follows ring 5. */
    public static Ring weatherRing(Ring ring) {
        return ring == Ring.VOID ? Ring.RING_5 : ring;
    }

    /**
     * Sets the weather of a ring for {@code durationTicks} and syncs it to the dimension. The cycle continues from
     * there.
     *
     * @throws IllegalArgumentException if the level has no weather for that ring or the type does not belong to it
     */
    public static void set(ServerLevel level, Ring ring, RingWeatherType type, int durationTicks) {
        RingWeatherData data = data(level).orElseThrow(() -> new IllegalArgumentException("No ring weather in " + level.dimension().location()));
        if (!type.allowedIn(level.dimension(), ring) || durationTicks <= 0) {
            throw new IllegalArgumentException(type.getSerializedName() + " for " + durationTicks + " ticks is not a weather of " + ring.getSerializedName());
        }
        data.put(ring, RingWeatherState.start(type, durationTicks, level.getRandom().nextInt()));
        sync(level, data);
    }

    private static RingWeatherState fresh(ServerLevel level, Ring ring) {
        return RingWeatherCycle.startClear(ServerConfig.ringWeather(ring), level.getRandom());
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        // runsNormally: under /tick freeze the game time stands still, so the modulo would fire every tick.
        if (!(event.getLevel() instanceof ServerLevel level) || !level.tickRateManager().runsNormally()
                || level.getGameTime() % CYCLE_INTERVAL != 0) {
            return;
        }
        data(level).ifPresent(data -> tick(level, data));
    }

    private static void tick(ServerLevel level, RingWeatherData data) {
        boolean running = cycleRunning(level);
        boolean changed = data.syncedCycleRunning() != null && data.syncedCycleRunning() != running;
        data.syncedCycleRunning(running);
        if (running) {
            for (Ring ring : List.copyOf(data.states().keySet())) {
                RingWeatherState previous = data.get(ring);
                RingWeatherState next = RingWeatherCycle.advance(previous, CYCLE_INTERVAL,
                        ServerConfig.ringWeather(ring), level.getRandom());
                data.put(ring, next);
                // Counting down only ever lowers the remaining time; anything else is a new weather.
                changed |= next.type() != previous.type() || next.remainingTicks() > previous.remainingTicks();
            }
        }
        if (changed) {
            sync(level, data);
        }
    }

    private static boolean cycleRunning(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE);
    }

    private static void sync(ServerLevel level, RingWeatherData data) {
        PacketDistributor.sendToPlayersInDimension(level, payload(level, data));
    }

    private static RingWeatherPayload payload(ServerLevel level, RingWeatherData data) {
        return new RingWeatherPayload(level.dimension(), level.getGameTime(), cycleRunning(level), data.states());
    }

    /** The full weather of the player's dimension; an empty payload in a ring-world dimension without ring weather. */
    private static void sendTo(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !RingWorld.isActive(serverPlayer.server)) {
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        RingWeatherPayload payload = data(level)
                .map(data -> payload(level, data))
                .orElseGet(() -> new RingWeatherPayload(level.dimension(), level.getGameTime(), cycleRunning(level), Map.of()));
        PacketDistributor.sendToPlayer(serverPlayer, payload);
    }
}
