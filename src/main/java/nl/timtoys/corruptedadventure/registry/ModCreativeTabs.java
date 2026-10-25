package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One tab per ring, {@code corruptedadventure:ring_1} to {@code ring_5}, each with its Nether ring (phase 2, §1.5): first
 * the ring's blocks (block registration order), then its standalone items (item registration order). The ring of a
 * block or item is the {@code // ---- Ring N ----} section of {@link ModBlocks} or {@link ModItems} it is registered in.
 * Our items stay out of the vanilla tabs.
 */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CorruptedAdventure.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RING_1 = ring(1, () -> ModBlocks.DEAD_LOG);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RING_2 = ring(2, () -> ModItems.RAW_SUNSTONE);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RING_3 = ring(3, () -> ModItems.RAW_VOLCANITE);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RING_4 = ring(4, () -> ModItems.RAW_TIDESILVER);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RING_5 = ring(5, () -> ModItems.RAW_NULLITE);

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    /** {@code itemGroup.corruptedadventure.ring_<ring>} */
    public static String titleKey(int ring) {
        return "itemGroup." + CorruptedAdventure.MOD_ID + ".ring_" + ring;
    }

    /**
     * The items of ring {@code ring}'s tab in tab order. Blocks without an item (potted plants) would be an empty stack;
     * a wall variant shares the item of its standing block (the wall bromeliad, the signs), so each item once.
     */
    public static List<Item> contents(int ring) {
        List<Item> items = new ArrayList<>();
        ModBlocks.entriesOfRing(ring).stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR)
                .distinct().forEach(items::add);
        ModItems.entriesOfRing(ring).stream().<Item>map(DeferredHolder::get).filter(item -> !(item instanceof BlockItem))
                .forEach(items::add);
        return items;
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> ring(int ring, Supplier<ItemLike> icon) {
        CreativeModeTab.Builder builder = CreativeModeTab.builder()
                .title(Component.translatable(titleKey(ring)))
                .icon(() -> new ItemStack(icon.get()))
                .displayItems((parameters, output) -> contents(ring).forEach(output::accept));
        if (ring > 1) {
            builder.withTabsBefore(ResourceKey.create(Registries.CREATIVE_MODE_TAB, CorruptedAdventure.id("ring_" + (ring - 1))));
        }
        return TABS.register("ring_" + ring, builder::build);
    }
}
