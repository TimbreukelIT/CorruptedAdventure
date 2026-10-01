package nl.timtoys.corruptedadventure.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/** Fire odds of our flammable blocks, set once in common setup. */
public final class ModFlammables {
    private ModFlammables() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ModFlammables::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        // FireBlock's odds maps are not thread-safe; common setup runs in parallel across mods.
        event.enqueueWork(ModFlammables::setFireOdds);
    }

    /**
     * Same odds as oak (and oak leaves) in {@code FireBlock.bootStrap} (doors, trapdoors, buttons, pressure plates and
     * saplings do not burn) and as the dead bush and flowers.
     */
    private static void setFireOdds() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (Block log : new Block[] {
                ModBlocks.DEAD_LOG.get(), ModBlocks.DEAD_WOOD.get(), ModBlocks.STRIPPED_DEAD_LOG.get(), ModBlocks.STRIPPED_DEAD_WOOD.get(),
                ModBlocks.PALM_LOG.get(), ModBlocks.PALM_WOOD.get(), ModBlocks.STRIPPED_PALM_LOG.get(), ModBlocks.STRIPPED_PALM_WOOD.get()}) {
            fire.setFlammable(log, 5, 5);
        }
        for (Block planks : new Block[] {
                ModBlocks.DEAD_PLANKS.get(), ModBlocks.DEAD_STAIRS.get(), ModBlocks.DEAD_SLAB.get(), ModBlocks.DEAD_FENCE.get(), ModBlocks.DEAD_FENCE_GATE.get(),
                ModBlocks.PALM_PLANKS.get(), ModBlocks.PALM_STAIRS.get(), ModBlocks.PALM_SLAB.get(), ModBlocks.PALM_FENCE.get(), ModBlocks.PALM_FENCE_GATE.get()}) {
            fire.setFlammable(planks, 5, 20);
        }
        fire.setFlammable(ModBlocks.PALM_LEAVES.get(), 30, 60);
        // Like the dead bush and flowers.
        fire.setFlammable(ModBlocks.DRY_SHRUB.get(), 60, 100);
        fire.setFlammable(ModBlocks.DESERT_FLOWER.get(), 60, 100);
    }
}
