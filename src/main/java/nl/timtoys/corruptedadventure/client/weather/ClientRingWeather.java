package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import nl.timtoys.corruptedadventure.network.RingWeatherPayload;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The ring weather of the client's dimension, as last received. The server only sends changes; in between the client
 * derives the remaining time from the level's game time (kept in step by vanilla's time packets), so it neither runs
 * ahead of a lagging server nor counts on while paused, frozen or with {@code doWeatherCycle} off. Main thread only.
 * Forgotten on logout.
 */
public final class ClientRingWeather {
    private static @Nullable ResourceKey<Level> dimension;
    private static long receivedAt;
    private static boolean cycleRunning;
    private static Map<Ring, RingWeatherState> received = Map.of();

    private ClientRingWeather() {
    }

    public static void set(RingWeatherPayload payload) {
        dimension = payload.dimension();
        receivedAt = payload.gameTime();
        cycleRunning = payload.cycleRunning();
        received = payload.states();
    }

    /**
     * The weather per ring in {@code level} now; empty if the server sent none for that dimension (no ring weather
     * there, or the payload for a new dimension has not arrived yet).
     */
    public static Optional<Map<Ring, RingWeatherState>> states(Level level) {
        if (!level.dimension().equals(dimension) || received.isEmpty()) {
            return Optional.empty();
        }
        long elapsed = cycleRunning ? Math.max(0L, level.getGameTime() - receivedAt) : 0L;
        if (elapsed == 0L) {
            return Optional.of(received);
        }
        int ticks = (int) Math.min(Integer.MAX_VALUE, elapsed);
        Map<Ring, RingWeatherState> now = new EnumMap<>(Ring.class);
        received.forEach((ring, state) -> now.put(ring, state.countDown(ticks)));
        return Optional.of(Collections.unmodifiableMap(now));
    }

    public static boolean cycleRunning() {
        return cycleRunning;
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        dimension = null;
        cycleRunning = false;
        received = Map.of();
    }
}
