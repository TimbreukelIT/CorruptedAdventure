package nl.timtoys.corruptedadventure.client.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.timtoys.corruptedadventure.client.item.LensView;
import nl.timtoys.corruptedadventure.network.LensScanPayload;

/** Hands the server's lens scan to the lens's outlines (main thread, like the other handlers). */
public final class LensScanHandler {
    private LensScanHandler() {
    }

    public static void handle(LensScanPayload payload, IPayloadContext context) {
        LensView.show(payload);
    }
}
