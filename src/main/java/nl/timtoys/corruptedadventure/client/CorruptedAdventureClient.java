package nl.timtoys.corruptedadventure.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.network.RingLayoutHandler;
import nl.timtoys.corruptedadventure.network.ClientboundHandlers;
import nl.timtoys.corruptedadventure.network.RingLayoutPayload;

/**
 * Client-only entrypoint. Never loaded on a dedicated server.
 * All client code (renderers, fog, sky, weather rendering, particles, screens) lives in this package.
 * Clientbound payload handlers are linked here through {@link ClientboundHandlers}, before any connection exists.
 */
@Mod(value = CorruptedAdventure.MOD_ID, dist = Dist.CLIENT)
public final class CorruptedAdventureClient {
    public CorruptedAdventureClient(IEventBus modEventBus, ModContainer modContainer) {
        CorruptedAdventure.LOGGER.info("The Corrupted Adventure client is loading");
        ClientboundHandlers.set(RingLayoutPayload.TYPE, RingLayoutHandler::handle);
        NeoForge.EVENT_BUS.addListener(ClientRingLayout::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(RingDebugOverlay::onDebugText);
    }
}
