package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * One mod tab with all our items: first the blocks per ring (= block registration order), then the standalone items
 * in their registration order. Our items stay out of the vanilla tabs.
 */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:main} */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.corruptedadventure.main"))
            .icon(() -> new ItemStack(ModBlocks.DEAD_LOG.get()))
            .displayItems((parameters, output) -> {
                // Blocks without an item (potted plants) would be an empty stack.
                ModBlocks.entries().stream().map(block -> block.get().asItem()).filter(item -> item != Items.AIR)
                        .forEach(output::accept);
                ModItems.entries().stream().map(DeferredHolder::get)
                        .filter(item -> !(item instanceof BlockItem))
                        .forEach(output::accept);
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
