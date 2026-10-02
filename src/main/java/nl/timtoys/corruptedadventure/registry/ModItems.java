package nl.timtoys.corruptedadventure.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.Collection;

/**
 * All items: the block items (registered by {@link ModBlocks}) and the standalone items below. Standalone items
 * register before the block items (class init order), so the creative tab sorts them itself.
 */
public final class ModItems {
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CorruptedAdventure.MOD_ID);

    // ---- Nether ring 1 ----
    /** Drop of the soul crystal ores. Placeholder: its use, recipes and balance follow in phase 2. */
    public static final DeferredItem<Item> SOUL_CRYSTAL = ITEMS.registerSimpleItem("soul_crystal");

    // ---- Ring 2 ----
    /** Drop of the sunstone ores. Placeholder raw material: smelting, product, recipes and balance follow in phase 2. */
    public static final DeferredItem<Item> RAW_SUNSTONE = ITEMS.registerSimpleItem("raw_sunstone");
    /** Drop of the turquoise ores. Placeholder raw material: smelting, product, recipes and balance follow in phase 2. */
    public static final DeferredItem<Item> RAW_TURQUOISE = ITEMS.registerSimpleItem("raw_turquoise");
    /** Drop of the salt crust. Placeholder: its use and recipes follow in phase 2. */
    public static final DeferredItem<Item> SALT = ITEMS.registerSimpleItem("salt");

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** All items in registration order (the creative tab order). */
    public static Collection<DeferredHolder<Item, ? extends Item>> entries() {
        return ITEMS.getEntries();
    }
}
