package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.List;

/** Lets the empty vanilla flower pot accept our plants (NeoForge {@code addPlant}), once in common setup. */
public final class ModFlowerPots {
    private ModFlowerPots() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModFlowerPots::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        // The pot's plant map is a plain HashMap; common setup runs in parallel across mods.
        event.enqueueWork(ModFlowerPots::addPlants);
    }

    private static void addPlants() {
        FlowerPotBlock emptyPot = (FlowerPotBlock) Blocks.FLOWER_POT;
        for (DeferredBlock<FlowerPotBlock> pot : List.of(
                ModBlocks.POTTED_DRY_SHRUB, ModBlocks.POTTED_BARREL_CACTUS, ModBlocks.POTTED_DESERT_FLOWER,
                ModBlocks.POTTED_PALM_SAPLING)) {
            emptyPot.addPlant(BuiltInRegistries.BLOCK.getKey(pot.get().getPotted()), pot);
        }
    }
}
