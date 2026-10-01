package nl.timtoys.corruptedadventure.client.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.weather.ClientRingWeather;
import nl.timtoys.corruptedadventure.network.RingWeatherPayload;

import java.util.stream.Collectors;

/** Replaces the client's ring weather with the server's (main thread, like the other handlers). */
public final class RingWeatherHandler {
    private RingWeatherHandler() {
    }

    public static void handle(RingWeatherPayload payload, IPayloadContext context) {
        ClientRingWeather.set(payload);
        CorruptedAdventure.LOGGER.debug("Received ring weather for {} in {} (cycle {}): {}",
                context.player().getGameProfile().getName(), payload.dimension().location(),
                payload.cycleRunning() ? "running" : "paused",
                payload.states().entrySet().stream()
                        .map(entry -> entry.getKey().getSerializedName() + "=" + entry.getValue().type().getSerializedName()
                                + " " + entry.getValue().remainingTicks() + "/" + entry.getValue().durationTicks())
                        .collect(Collectors.joining(", ", "[", "]")));
    }
}
