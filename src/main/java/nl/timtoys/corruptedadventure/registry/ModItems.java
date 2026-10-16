package nl.timtoys.corruptedadventure.registry;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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

    // ---- Nether ring 2 ----
    /** Drop of the grown dew crystal. Placeholder: its use, recipes and balance follow in phase 2. */
    public static final DeferredItem<Item> DEW_CRYSTAL = ITEMS.registerSimpleItem("dew_crystal");

    // ---- Ring 3 ----
    // Placeholders like raw_sunstone: smelting, products, recipes and balance follow in phase 2.
    /** Drop of the volcanite ores (ring 3's main metal). */
    public static final DeferredItem<Item> RAW_VOLCANITE = ITEMS.registerSimpleItem("raw_volcanite");
    /** Drop of the sulfur ores (several per ore). */
    public static final DeferredItem<Item> SULFUR = ITEMS.registerSimpleItem("sulfur");
    /** Drop of the fire ruby ores. */
    public static final DeferredItem<Item> FIRE_RUBY = ITEMS.registerSimpleItem("fire_ruby");
    /** Drop of the grown fire crystal cluster. */
    public static final DeferredItem<Item> FIRE_CRYSTAL = ITEMS.registerSimpleItem("fire_crystal");

    // ---- Ring 4 ----
    // Placeholders like raw_sunstone: smelting, products, recipes and balance follow in phase 2.
    /** Drop of the tidesilver ores (ring 4's main metal). */
    public static final DeferredItem<Item> RAW_TIDESILVER = ITEMS.registerSimpleItem("raw_tidesilver");
    /** Drop of the jade ores. */
    public static final DeferredItem<Item> JADE = ITEMS.registerSimpleItem("jade");
    /** Drop of the grown toxic crystal cluster. */
    public static final DeferredItem<Item> TOXIC_CRYSTAL_SHARD = ITEMS.registerSimpleItem("toxic_crystal_shard");
    /** Drop of the grown aquamarine cluster. */
    public static final DeferredItem<Item> AQUAMARINE_SHARD = ITEMS.registerSimpleItem("aquamarine_shard");
    /** A bucket of the toxic pool (contract ring 4 §3): picks it up and pours it out; its use follows in phase 2. */
    public static final DeferredItem<BucketItem> TOXIC_POOL_BUCKET = ITEMS.registerItem("toxic_pool_bucket",
            properties -> new BucketItem(ModFluids.TOXIC_POOL.get(), properties.craftRemainder(Items.BUCKET).stacksTo(1)));
    /** Drop of the verdant fruit pod (72b, H3 = A). Placeholder without food value: eating and recipes in phase 2. */
    public static final DeferredItem<Item> VERDANT_FRUIT = ITEMS.registerSimpleItem("verdant_fruit");

    // ---- Nether ring 4 ----
    /** Drop of the purity crystal cluster; with bone sand the raw material of sky glass (phase 2). Placeholder. */
    public static final DeferredItem<Item> PURITY_CRYSTAL_SHARD = ITEMS.registerSimpleItem("purity_crystal_shard");

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
