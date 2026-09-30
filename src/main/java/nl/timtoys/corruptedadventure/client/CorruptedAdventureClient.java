package nl.timtoys.corruptedadventure.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Client-only entrypoint. Never loaded on a dedicated server.
 * All client code (renderers, fog, sky, weather rendering, particles, screens) lives in this package.
 */
@Mod(value = CorruptedAdventure.MOD_ID, dist = Dist.CLIENT)
public final class CorruptedAdventureClient {
    public CorruptedAdventureClient(IEventBus modEventBus, ModContainer modContainer) {
        CorruptedAdventure.LOGGER.info("The Corrupted Adventure client is loading");
    }
}
