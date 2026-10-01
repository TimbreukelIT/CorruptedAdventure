package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModBlocksTest {
    @Test
    void everyBlockExceptPottedPlantsHasABlockItemWithTheSameName() {
        assertFalse(ModBlocks.entries().isEmpty());
        for (DeferredHolder<Block, ? extends Block> block : ModBlocks.entries()) {
            Item item = block.get().asItem();
            if (block.get() instanceof FlowerPotBlock) {
                assertSame(Items.AIR, item, block.getId().toString());
                continue;
            }
            assertInstanceOf(BlockItem.class, item, block.getId().toString());
            assertEquals(block.getId(), BuiltInRegistries.ITEM.getKey(item));
        }
    }

    @Test
    void pottedPlantsHoldTheirPlant() {
        assertSame(ModBlocks.DRY_SHRUB.get(), ModBlocks.POTTED_DRY_SHRUB.get().getPotted());
        assertSame(ModBlocks.BARREL_CACTUS.get(), ModBlocks.POTTED_BARREL_CACTUS.get().getPotted());
        assertSame(ModBlocks.DESERT_FLOWER.get(), ModBlocks.POTTED_DESERT_FLOWER.get().getPotted());
    }

    @Test
    void creativeTabShowsBlocksThenStandaloneItemsInRegistrationOrder() {
        CreativeModeTab tab = ModCreativeTabs.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.DEFAULT_FLAGS, false, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
        List<Item> shown = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();

        List<Item> expected = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(expected::add);
        ModItems.entries().stream().<Item>map(DeferredHolder::get).filter(item -> !(item instanceof BlockItem)).forEach(expected::add);
        assertEquals(expected, shown);
        // Every item exactly once.
        assertEquals(ModItems.entries().size(), shown.size());
        assertEquals(new HashSet<>(ModItems.entries().stream().<Item>map(DeferredHolder::get).toList()), new HashSet<>(shown));
    }

    @Test
    void looseSandSitsBetweenTheScorchedSandstoneSetAndTheFlora() {
        List<Item> shown = new ArrayList<>();
        ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR).forEach(shown::add);
        int looseSand = shown.indexOf(ModBlocks.LOOSE_SAND.asItem());
        assertTrue(looseSand > 0);
        assertEquals(ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB.asItem(), shown.get(looseSand - 1));
        assertEquals(ModBlocks.CRACKED_CLAY.asItem(), shown.get(looseSand + 1));
    }
}
