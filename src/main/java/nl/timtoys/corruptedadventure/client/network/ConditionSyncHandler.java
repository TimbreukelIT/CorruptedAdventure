package nl.timtoys.corruptedadventure.client.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.network.ConditionSyncPayload;
import nl.timtoys.corruptedadventure.registry.ModAttachments;

import java.util.Arrays;

/**
 * Puts the server's rounded meters into the local player's attachment; the HUD reads them from there. Runs on the main
 * thread (the registrar's default), after the respawn/dimension packet that created the current local player.
 */
public final class ConditionSyncHandler {
    private ConditionSyncHandler() {
    }

    public static void handle(ConditionSyncPayload payload, IPayloadContext context) {
        int[] values = payload.values();
        context.player().getData(ModAttachments.CONDITION_METERS).applySync(values);
        CorruptedAdventure.LOGGER.debug("Received condition meters for {}: {}",
                context.player().getGameProfile().getName(), Arrays.toString(values));
    }
}
