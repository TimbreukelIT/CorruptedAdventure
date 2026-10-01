package nl.timtoys.corruptedadventure.client.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.ClientRingLayout;
import nl.timtoys.corruptedadventure.network.RingLayoutPayload;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/** Builds the client's {@link RingLayout} from the server's settings and noise seed. */
public final class RingLayoutHandler {
    private RingLayoutHandler() {
    }

    public static void handle(RingLayoutPayload payload, IPayloadContext context) {
        payload.settings().validate().ifSuccess(settings -> {
            ClientRingLayout.set(new RingLayout(settings, payload.noiseSeed()));
            CorruptedAdventure.LOGGER.info("Received ring layout from the server: {}", settings);
        }).ifError(error -> {
            ClientRingLayout.set(null);
            CorruptedAdventure.LOGGER.error("Ignoring invalid ring layout from the server: {}", error.message());
        });
    }
}
