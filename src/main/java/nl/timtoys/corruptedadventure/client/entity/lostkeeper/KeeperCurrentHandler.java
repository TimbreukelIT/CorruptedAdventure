package nl.timtoys.corruptedadventure.client.entity.lostkeeper;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.timtoys.corruptedadventure.network.KeeperCurrentPayload;

/** Adds the current of The Lost Keeper's bubble trap to the local player's movement (additive, as the wind gust). */
public final class KeeperCurrentHandler {
    private KeeperCurrentHandler() {
    }

    public static void handle(KeeperCurrentPayload payload, IPayloadContext context) {
        if (Float.isFinite(payload.x()) && Float.isFinite(payload.y()) && Float.isFinite(payload.z())) {
            context.player().push(payload.x(), payload.y(), payload.z());
        }
    }
}
