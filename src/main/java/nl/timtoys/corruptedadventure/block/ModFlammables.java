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
        // Glow mold burns like glow lichen; the dripshroom set is Nether wood and gets no odds (like crimson and warped).
        fire.setFlammable(ModBlocks.GLOW_MOLD.get(), 15, 100);

        // Ring 4: the moonpetal set and its withered trunk burn like oak; the hanging moss like vines, the glowing roots
        // like hanging roots, the glow bloom like flowers. Peat is a fuel but does not burn in the world (user decision
        // after the review of 66): no fire spreading over the peat moors.
        for (Block log : new Block[] {
                ModBlocks.MOONPETAL_LOG.get(), ModBlocks.MOONPETAL_WOOD.get(), ModBlocks.STRIPPED_MOONPETAL_LOG.get(),
                ModBlocks.STRIPPED_MOONPETAL_WOOD.get(), ModBlocks.WITHERED_MOONPETAL_LOG.get(), ModBlocks.WITHERED_MOONPETAL_WOOD.get(),
                ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG.get(), ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD.get()}) {
            fire.setFlammable(log, 5, 5);
        }
        for (Block planks : new Block[] {
                ModBlocks.MOONPETAL_PLANKS.get(), ModBlocks.MOONPETAL_STAIRS.get(), ModBlocks.MOONPETAL_SLAB.get(),
                ModBlocks.MOONPETAL_FENCE.get(), ModBlocks.MOONPETAL_FENCE_GATE.get()}) {
            fire.setFlammable(planks, 5, 20);
        }
        fire.setFlammable(ModBlocks.MOONPETAL_LEAVES.get(), 30, 60);
        fire.setFlammable(ModBlocks.HANGING_SWAMP_MOSS.get(), 15, 100);
        fire.setFlammable(ModBlocks.GLOWING_HANGING_ROOTS.get(), 30, 60);
        fire.setFlammable(ModBlocks.GLOW_BLOOM.get(), 60, 100);
    }
}
