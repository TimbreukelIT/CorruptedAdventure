package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModCreativeTabsTest {
    private static final List<DeferredHolder<CreativeModeTab, CreativeModeTab>> TABS = List.of(
            ModCreativeTabs.RING_1, ModCreativeTabs.RING_2, ModCreativeTabs.RING_3, ModCreativeTabs.RING_4,
            ModCreativeTabs.RING_5);

    private static List<Item> shown(int ring) {
        CreativeModeTab tab = TABS.get(ring - 1).get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.DEFAULT_FLAGS, false, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)));
        return tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
    }

    private static int ringOf(ItemLike item) {
        for (int ring = 1; ring <= 5; ring++) {
            if (shown(ring).contains(item.asItem())) {
                return ring;
            }
        }
        return 0;
    }

    @Test
    void eachTabShowsItsBlocksThenItsStandaloneItems() {
        for (int ring = 1; ring <= 5; ring++) {
            assertEquals(ModCreativeTabs.contents(ring), shown(ring), "ring " + ring);
        }
    }

    @Test
    void everyItemIsInExactlyOneTab() {
        List<Item> all = new ArrayList<>();
        for (int ring = 1; ring <= 5; ring++) {
            all.addAll(shown(ring));
        }
        assertEquals(ModItems.entries().size(), all.size());
        assertEquals(new HashSet<>(ModItems.entries().stream().<Item>map(DeferredHolder::get).toList()), new HashSet<>(all));
    }

    @Test
    void theNetherRingSharesTheTabOfItsRing() {
        assertEquals(1, ringOf(ModBlocks.DEAD_LOG));
        assertEquals(1, ringOf(ModBlocks.SOUL_CRYSTAL_ORE));
        assertEquals(1, ringOf(ModItems.SOUL_CRYSTAL));
        assertEquals(2, ringOf(ModItems.RAW_SUNSTONE));
        assertEquals(2, ringOf(ModItems.DEW_CRYSTAL));
        assertEquals(2, ringOf(ModBlocks.DRIPSHROOM_STEM));
        assertEquals(3, ringOf(ModBlocks.PRIMORDIAL_ICE));
        assertEquals(3, ringOf(ModItems.VOLCANITE_INGOT));
        assertEquals(4, ringOf(ModItems.TOXIC_POOL_BUCKET));
        assertEquals(4, ringOf(ModItems.PURITY_CRYSTAL_SHARD));
        assertEquals(4, ringOf(ModBlocks.JADE_BLOCK));
        assertEquals(5, ringOf(ModItems.WHITE_CORE));
        assertEquals(5, ringOf(ModBlocks.NULLITE_BLOCK));
    }

    @Test
    void blocksComeBeforeStandaloneItems() {
        List<Item> ring2 = shown(2);
        assertTrue(ring2.indexOf(ModBlocks.DEW_CRYSTAL_CLUSTER.asItem()) < ring2.indexOf(ModItems.RAW_SUNSTONE.get()));
        assertEquals(ModItems.TURQUOISE.get(), ring2.get(ring2.indexOf(ModItems.SUNSTONE_INGOT.get()) + 1));
    }
}
