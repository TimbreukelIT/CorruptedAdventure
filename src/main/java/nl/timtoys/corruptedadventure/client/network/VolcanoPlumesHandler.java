package nl.timtoys.corruptedadventure.client.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.timtoys.corruptedadventure.client.weather.VolcanoPlumeRenderer;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload;

/** Hands the server's list of nearby volcanoes to the plume renderer (main thread, like the other handlers). */
public final class VolcanoPlumesHandler {
    private VolcanoPlumesHandler() {
    }

    public static void handle(VolcanoPlumesPayload payload, IPayloadContext context) {
        VolcanoPlumeRenderer.set(payload.plumes());
    }
}
