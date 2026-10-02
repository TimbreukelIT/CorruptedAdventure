package nl.timtoys.corruptedadventure.client.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.timtoys.corruptedadventure.network.WindGustPayload;

/**
 * Adds the server's wind gust to the local player's movement (main thread, like the other handlers). Additive, like
 * vanilla's explosion knockback, so the player's own walking is not overwritten.
 */
public final class WindGustHandler {
    private WindGustHandler() {
    }

    public static void handle(WindGustPayload payload, IPayloadContext context) {
        if (Float.isFinite(payload.x()) && Float.isFinite(payload.z())) {
            context.player().push(payload.x(), 0.0, payload.z());
        }
    }
}
