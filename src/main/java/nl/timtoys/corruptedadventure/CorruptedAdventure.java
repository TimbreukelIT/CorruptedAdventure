package nl.timtoys.corruptedadventure;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import nl.timtoys.corruptedadventure.block.LooseSandTrigger;
import nl.timtoys.corruptedadventure.block.ModFlammables;
import nl.timtoys.corruptedadventure.block.ModFlowerPots;
import nl.timtoys.corruptedadventure.block.ModSigns;
import nl.timtoys.corruptedadventure.block.SuckingMudSuffocation;
import nl.timtoys.corruptedadventure.block.SwampGasIgnition;
import nl.timtoys.corruptedadventure.command.CaCommand;
import nl.timtoys.corruptedadventure.condition.ConditionTicker;
import nl.timtoys.corruptedadventure.condition.DebugConditionDriver;
import nl.timtoys.corruptedadventure.condition.PlayerConditions;
import nl.timtoys.corruptedadventure.condition.WaterBottleDrink;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettingsSnapshot;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.datagen.DataGenerators;
import nl.timtoys.corruptedadventure.fluid.ToxicPoolInteractions;
import nl.timtoys.corruptedadventure.network.ModNetwork;
import nl.timtoys.corruptedadventure.network.RingLayoutSync;
import nl.timtoys.corruptedadventure.registry.ModAttachments;
import nl.timtoys.corruptedadventure.registry.ModBiomeSources;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModChunkGenerators;
import nl.timtoys.corruptedadventure.registry.ModCreativeTabs;
import nl.timtoys.corruptedadventure.registry.ModDataMaps;
import nl.timtoys.corruptedadventure.registry.ModDensityFunctionTypes;
import nl.timtoys.corruptedadventure.registry.ModCarverTypes;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.registry.ModFluidTypes;
import nl.timtoys.corruptedadventure.registry.ModFluids;
import nl.timtoys.corruptedadventure.registry.ModFoliagePlacerTypes;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import nl.timtoys.corruptedadventure.registry.ModSurfaceConditions;
import nl.timtoys.corruptedadventure.registry.ModTreeDecoratorTypes;
import nl.timtoys.corruptedadventure.registry.ModTrunkPlacerTypes;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.weather.RingWeather;
import nl.timtoys.corruptedadventure.worldgen.biome.BiomeSpawnPlacements;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring3StriderSpawns;
import nl.timtoys.corruptedadventure.worldgen.border.RingWorldBorder;
import nl.timtoys.corruptedadventure.worldgen.chunk.IslandFields;
import nl.timtoys.corruptedadventure.worldgen.chunk.Ring5Fields;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring4Tunnels;
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
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        CommonConfig.register(modEventBus);
        ServerConfig.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModFluidTypes.register(modEventBus);
        ModFluids.register(modEventBus);
        ToxicPoolInteractions.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModFlammables.register(modEventBus);
        ModFlowerPots.register(modEventBus);
        ModSigns.register(modEventBus);
        ModBiomeSources.register(modEventBus);
        BiomeSpawnPlacements.register(modEventBus);
        ModDensityFunctionTypes.register(modEventBus);
        ModChunkGenerators.register(modEventBus);
        ModPlacementModifierTypes.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModCarverTypes.register(modEventBus);
        ModSurfaceConditions.register(modEventBus);
        ModTrunkPlacerTypes.register(modEventBus);
        ModFoliagePlacerTypes.register(modEventBus);
        ModTreeDecoratorTypes.register(modEventBus);
        ModSoundEvents.register(modEventBus);
        ModParticleTypes.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModDataMaps.register(modEventBus);
        DataGenerators.register(modEventBus);
        ModNetwork.register(modEventBus);
        RingSettingsSnapshot.register(NeoForge.EVENT_BUS);
        RingLayout.register(NeoForge.EVENT_BUS);
        IslandFields.register(NeoForge.EVENT_BUS);
        Ring5Fields.register(NeoForge.EVENT_BUS);
        Ring4Tunnels.register(NeoForge.EVENT_BUS);
        RingWorldBorder.register(NeoForge.EVENT_BUS);
        RingLayoutSync.register(NeoForge.EVENT_BUS);
        CaCommand.register(NeoForge.EVENT_BUS);
        ConditionTicker.register(NeoForge.EVENT_BUS);
        PlayerConditions.register(NeoForge.EVENT_BUS);
        DebugConditionDriver.register(NeoForge.EVENT_BUS);
        WaterBottleDrink.register(NeoForge.EVENT_BUS);
        RingWeather.register(NeoForge.EVENT_BUS);
        LooseSandTrigger.register(NeoForge.EVENT_BUS);
        SwampGasIgnition.register(NeoForge.EVENT_BUS);
        SuckingMudSuffocation.register(NeoForge.EVENT_BUS);
        Ring3StriderSpawns.register(NeoForge.EVENT_BUS);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
