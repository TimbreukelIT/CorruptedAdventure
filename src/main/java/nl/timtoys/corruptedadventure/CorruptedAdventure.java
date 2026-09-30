package nl.timtoys.corruptedadventure;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Common entrypoint. Loaded on both the client and the dedicated server,
 * so it must never reference client-only classes.
 */
@Mod(CorruptedAdventure.MOD_ID)
public final class CorruptedAdventure {
    public static final String MOD_ID = "corruptedadventure";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CorruptedAdventure(IEventBus modEventBus, ModContainer modContainer) {
        // DeferredRegisters, config and network registration are added per change (see CODEBASE.md).
        LOGGER.info("The Corrupted Adventure is loading");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
