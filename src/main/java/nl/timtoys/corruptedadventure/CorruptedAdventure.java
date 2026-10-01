package nl.timtoys.corruptedadventure;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import nl.timtoys.corruptedadventure.block.ModFlammables;
import nl.timtoys.corruptedadventure.command.CaCommand;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettingsSnapshot;
import nl.timtoys.corruptedadventure.datagen.DataGenerators;
import nl.timtoys.corruptedadventure.registry.ModBiomeSources;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModChunkGenerators;
import nl.timtoys.corruptedadventure.registry.ModCreativeTabs;
import nl.timtoys.corruptedadventure.registry.ModDensityFunctionTypes;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.border.RingWorldBorder;
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
        // DeferredRegisters and network registration are added per change (see CODEBASE.md).
        LOGGER.info("The Corrupted Adventure is loading");
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModFlammables.register(modEventBus);
        ModBiomeSources.register(modEventBus);
        ModDensityFunctionTypes.register(modEventBus);
        ModChunkGenerators.register(modEventBus);
        DataGenerators.register(modEventBus);
        RingSettingsSnapshot.register(NeoForge.EVENT_BUS);
        RingLayout.register(NeoForge.EVENT_BUS);
        RingWorldBorder.register(NeoForge.EVENT_BUS);
        CaCommand.register(NeoForge.EVENT_BUS);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
