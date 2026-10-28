package nl.timtoys.corruptedadventure.registry;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.ConsumableDoses;
import nl.timtoys.corruptedadventure.entity.ArrowEffect;
import nl.timtoys.corruptedadventure.item.CanteenItem;
import nl.timtoys.corruptedadventure.item.DoseDrinkItem;
import nl.timtoys.corruptedadventure.item.DoseFoodItem;
import nl.timtoys.corruptedadventure.item.EffectArrowItem;
import nl.timtoys.corruptedadventure.item.GearItems;
import nl.timtoys.corruptedadventure.item.GearTier;
import nl.timtoys.corruptedadventure.item.HandWarmerItem;
import nl.timtoys.corruptedadventure.item.UpgradeSmithingTemplates;
import nl.timtoys.corruptedadventure.item.coating.Coating;
import nl.timtoys.corruptedadventure.item.coating.CoatingTemplates;

import java.util.Collection;
import java.util.List;

/**
 * All items: the block items (registered by {@link ModBlocks}) and the standalone items below. Standalone items
 * register before the block items (class init order), so the creative tab sorts them itself.
 *
 * <p>Creative tab per ring: a standalone item lands in the tab of the {@code // ---- Ring N ----} section it is
 * registered in (each starts with {@code SECTIONS.start(N)}; the {@code Nether ring N} section after it belongs to ring
 * N). Add new items <b>at the end</b> of the right ring or Nether ring section, never above a {@code SECTIONS.start}.
 */
public final class ModItems {
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CorruptedAdventure.MOD_ID);
    private static final RingSections<Item> SECTIONS = new RingSections<>(ITEMS);

    // ---- Ring 1 ----
    static {
        SECTIONS.start(1);
    }

    // ---- Nether ring 1 ----
    /** Drop of the soul crystal ores. Placeholder: its use, recipes and balance follow in phase 2. */
    public static final DeferredItem<Item> SOUL_CRYSTAL = ITEMS.registerSimpleItem("soul_crystal");

    // ---- Ring 2 ----
    static {
        SECTIONS.start(2);
    }

    /** Drop of the sunstone ores. Placeholder raw material: smelting, product, recipes and balance follow in phase 2. */
    public static final DeferredItem<Item> RAW_SUNSTONE = ITEMS.registerSimpleItem("raw_sunstone");
    /** Drop of the turquoise ores. Placeholder raw material: smelting, product, recipes and balance follow in phase 2. */
    public static final DeferredItem<Item> RAW_TURQUOISE = ITEMS.registerSimpleItem("raw_turquoise");
    /** Drop of the salt crust. Placeholder: its use and recipes follow in phase 2. */
    public static final DeferredItem<Item> SALT = ITEMS.registerSimpleItem("salt");
    /** Smelted from raw sunstone and its ores (phase 2 golf 1); the sunstone tier's ingot. Provisional name. */
    public static final DeferredItem<Item> SUNSTONE_INGOT = ITEMS.registerSimpleItem("sunstone_ingot");
    /** Smelted from raw turquoise and its ores (phase 2 golf 1): the cut gem. Provisional name. */
    public static final DeferredItem<Item> TURQUOISE = ITEMS.registerSimpleItem("turquoise");
    /** Upgrades diamond gear to sunstone; crafted from soul crystal (golf 2, §5.1), no copy recipe. */
    public static final DeferredItem<SmithingTemplateItem> SUNSTONE_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            UpgradeSmithingTemplates.itemName(GearTier.SUNSTONE), () -> UpgradeSmithingTemplates.create(GearTier.SUNSTONE));
    /** Coating against dehydration (applied with the turquoise gem). Where to find it: golf 2. */
    public static final DeferredItem<SmithingTemplateItem> DEHYDRATION_COATING_SMITHING_TEMPLATE = ITEMS.register(
            "dehydration_coating_smithing_template", () -> CoatingTemplates.create(Coating.DEHYDRATION));
    // The sunstone set (golf 2, §5.1): netherite's damage and speed, fire resistant like netherite; smithed from diamond.
    public static final DeferredItem<SwordItem> SUNSTONE_SWORD = ITEMS.registerItem("sunstone_sword",
            properties -> GearItems.sword(GearTier.SUNSTONE, properties.fireResistant()));
    public static final DeferredItem<AxeItem> SUNSTONE_AXE = ITEMS.registerItem("sunstone_axe",
            properties -> GearItems.axe(GearTier.SUNSTONE, properties.fireResistant()));
    public static final DeferredItem<PickaxeItem> SUNSTONE_PICKAXE = ITEMS.registerItem("sunstone_pickaxe",
            properties -> GearItems.pickaxe(GearTier.SUNSTONE, properties.fireResistant()));
    public static final DeferredItem<ShovelItem> SUNSTONE_SHOVEL = ITEMS.registerItem("sunstone_shovel",
            properties -> GearItems.shovel(GearTier.SUNSTONE, properties.fireResistant()));
    public static final DeferredItem<HoeItem> SUNSTONE_HOE = ITEMS.registerItem("sunstone_hoe",
            properties -> GearItems.hoe(GearTier.SUNSTONE, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> SUNSTONE_HELMET = ITEMS.registerItem("sunstone_helmet",
            properties -> GearItems.armor(GearTier.SUNSTONE, ArmorItem.Type.HELMET, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> SUNSTONE_CHESTPLATE = ITEMS.registerItem("sunstone_chestplate",
            properties -> GearItems.armor(GearTier.SUNSTONE, ArmorItem.Type.CHESTPLATE, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> SUNSTONE_LEGGINGS = ITEMS.registerItem("sunstone_leggings",
            properties -> GearItems.armor(GearTier.SUNSTONE, ArmorItem.Type.LEGGINGS, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> SUNSTONE_BOOTS = ITEMS.registerItem("sunstone_boots",
            properties -> GearItems.armor(GearTier.SUNSTONE, ArmorItem.Type.BOOTS, properties.fireResistant()));

    // Consumables against dehydration (phase 2 golf 2, §5.3–§5.4).
    /** Four sips per filling; filled at water, a water cauldron or a dew source. */
    public static final DeferredItem<CanteenItem> CANTEEN = ITEMS.registerItem("canteen", CanteenItem::new);
    /** Single use, gives the glass bottle back. */
    public static final DeferredItem<DoseDrinkItem> CACTUS_JUICE = ITEMS.registerItem("cactus_juice",
            properties -> new DoseDrinkItem(properties.stacksTo(16), ConsumableDoses::cactusJuice));
    /** Fast food like dried kelp, but it makes you thirsty. */
    public static final DeferredItem<DoseFoodItem> SALTED_JERKY = ITEMS.registerItem("salted_jerky",
            properties -> new DoseFoodItem(properties.food(new FoodProperties.Builder()
                    .nutrition(6).saturationModifier(1.2F).fast().build()), ConsumableDoses::saltedJerky));

    // ---- Nether ring 2 ----
    /** Drop of the grown dew crystal. Placeholder: its use, recipes and balance follow in phase 2. */
    public static final DeferredItem<Item> DEW_CRYSTAL = ITEMS.registerSimpleItem("dew_crystal");
    /** Phase 2 golf 3 (§6.4): a dripshroom cap in a bowl, mushroom stew's food (the bowl comes back). */
    public static final DeferredItem<Item> DRIPSHROOM_STEW = ITEMS.registerItem("dripshroom_stew",
            properties -> new Item(properties.stacksTo(1).food(Foods.MUSHROOM_STEW)));

    // ---- Ring 3 ----
    static {
        SECTIONS.start(3);
    }

    // Placeholders like raw_sunstone: smelting, products, recipes and balance follow in phase 2.
    /** Drop of the volcanite ores (ring 3's main metal). */
    public static final DeferredItem<Item> RAW_VOLCANITE = ITEMS.registerSimpleItem("raw_volcanite");
    /** Drop of the sulfur ores (several per ore). */
    public static final DeferredItem<Item> SULFUR = ITEMS.registerSimpleItem("sulfur");
    /** Drop of the fire ruby ores. */
    public static final DeferredItem<Item> FIRE_RUBY = ITEMS.registerSimpleItem("fire_ruby");
    /** Drop of the grown fire crystal cluster. */
    public static final DeferredItem<Item> FIRE_CRYSTAL = ITEMS.registerSimpleItem("fire_crystal");
    /** Smelted from raw volcanite and its ores (phase 2 golf 1); the volcanite tier's ingot. Provisional name. */
    public static final DeferredItem<Item> VOLCANITE_INGOT = ITEMS.registerSimpleItem("volcanite_ingot");
    /** Upgrades sunstone gear to volcanite; crafted from dew crystal (golf 3, §6.2), no copy recipe. */
    public static final DeferredItem<SmithingTemplateItem> VOLCANITE_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            UpgradeSmithingTemplates.itemName(GearTier.VOLCANITE), () -> UpgradeSmithingTemplates.create(GearTier.VOLCANITE));
    /** Coating against heat (applied with dew crystal). Where to find it: golf 3. */
    public static final DeferredItem<SmithingTemplateItem> HEAT_COATING_SMITHING_TEMPLATE = ITEMS.register(
            "heat_coating_smithing_template", () -> CoatingTemplates.create(Coating.HEAT));
    /** Fire resistance from one ember berry: 5 s. */
    public static final int EMBER_BERRY_FIRE_RESISTANCE_TICKS = 5 * 20;

    /**
     * Phase 2 golf 3 (§6.4): picked from the ember berry bush and planting it, like sweet berries; sweet berries' food plus
     * 5 s of fire resistance.
     */
    public static final DeferredItem<ItemNameBlockItem> EMBER_BERRIES = ITEMS.registerItem("ember_berries",
            properties -> new ItemNameBlockItem(ModBlocks.EMBER_BERRY_BUSH.get(), properties.food(new FoodProperties.Builder()
                    .nutrition(2).saturationModifier(0.1F)
                    .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EMBER_BERRY_FIRE_RESISTANCE_TICKS), 1.0F)
                    .build())));

    // The volcanite set (golf 3, §6.1–§6.2): fire resistant like sunstone; smithed from sunstone.
    public static final DeferredItem<SwordItem> VOLCANITE_SWORD = ITEMS.registerItem("volcanite_sword",
            properties -> GearItems.sword(GearTier.VOLCANITE, properties.fireResistant()));
    public static final DeferredItem<AxeItem> VOLCANITE_AXE = ITEMS.registerItem("volcanite_axe",
            properties -> GearItems.axe(GearTier.VOLCANITE, properties.fireResistant()));
    public static final DeferredItem<PickaxeItem> VOLCANITE_PICKAXE = ITEMS.registerItem("volcanite_pickaxe",
            properties -> GearItems.pickaxe(GearTier.VOLCANITE, properties.fireResistant()));
    public static final DeferredItem<ShovelItem> VOLCANITE_SHOVEL = ITEMS.registerItem("volcanite_shovel",
            properties -> GearItems.shovel(GearTier.VOLCANITE, properties.fireResistant()));
    public static final DeferredItem<HoeItem> VOLCANITE_HOE = ITEMS.registerItem("volcanite_hoe",
            properties -> GearItems.hoe(GearTier.VOLCANITE, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> VOLCANITE_HELMET = ITEMS.registerItem("volcanite_helmet",
            properties -> GearItems.armor(GearTier.VOLCANITE, ArmorItem.Type.HELMET, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> VOLCANITE_CHESTPLATE = ITEMS.registerItem("volcanite_chestplate",
            properties -> GearItems.armor(GearTier.VOLCANITE, ArmorItem.Type.CHESTPLATE, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> VOLCANITE_LEGGINGS = ITEMS.registerItem("volcanite_leggings",
            properties -> GearItems.armor(GearTier.VOLCANITE, ArmorItem.Type.LEGGINGS, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> VOLCANITE_BOOTS = ITEMS.registerItem("volcanite_boots",
            properties -> GearItems.armor(GearTier.VOLCANITE, ArmorItem.Type.BOOTS, properties.fireResistant()));

    /** Against heat (phase 2 golf 3, §6.3): single use, cools for a while, gives the glass bottle back. */
    public static final DeferredItem<DoseDrinkItem> DEW_DRAUGHT = ITEMS.registerItem("dew_draught",
            properties -> new DoseDrinkItem(properties.stacksTo(16), ConsumableDoses::dewDraught));

    /** Sulfur and 4 arrows make 4 (golf 3, §6.4): the target burns 5 s; it lights no blocks. */
    public static final DeferredItem<EffectArrowItem> FIRE_ARROW = ITEMS.registerItem("fire_arrow",
            properties -> new EffectArrowItem(ArrowEffect.FIRE, properties));

    // ---- Nether ring 3 ----
    /** Coating against the cold (applied with fire crystal). Where to find it: golf 4. */
    public static final DeferredItem<SmithingTemplateItem> COLD_COATING_SMITHING_TEMPLATE = ITEMS.register(
            "cold_coating_smithing_template", () -> CoatingTemplates.create(Coating.COLD));
    /** Against the cold (phase 2 golf 4, §6.6): four charges, filled with a fire crystal. */
    public static final DeferredItem<HandWarmerItem> HAND_WARMER = ITEMS.registerItem("hand_warmer", HandWarmerItem::new);

    // ---- Ring 4 ----
    static {
        SECTIONS.start(4);
    }

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
    /** Smelted from raw tidesilver and its ores (phase 2 golf 1); the tidesilver tier's ingot. Provisional name. */
    public static final DeferredItem<Item> TIDESILVER_INGOT = ITEMS.registerSimpleItem("tidesilver_ingot");
    /** Upgrades volcanite gear to tidesilver; crafted from primordial ice (golf 4, §6.5), no copy recipe. */
    public static final DeferredItem<SmithingTemplateItem> TIDESILVER_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            UpgradeSmithingTemplates.itemName(GearTier.TIDESILVER), () -> UpgradeSmithingTemplates.create(GearTier.TIDESILVER));
    /** Coating against poison (applied with primordial ice). Where to find it: golf 4. */
    public static final DeferredItem<SmithingTemplateItem> POISON_COATING_SMITHING_TEMPLATE = ITEMS.register(
            "poison_coating_smithing_template", () -> CoatingTemplates.create(Coating.POISON));
    /** Against poison (phase 2 golf 4, §6.6): single use, cleanses for a while, gives the glass bottle back. */
    public static final DeferredItem<DoseDrinkItem> PURIFIED_WATER = ITEMS.registerItem("purified_water",
            properties -> new DoseDrinkItem(properties.stacksTo(16), ConsumableDoses::purifiedWater));

    // The tidesilver set (golf 4, §6.1, §6.5): fire resistant like volcanite; smithed from volcanite.
    public static final DeferredItem<SwordItem> TIDESILVER_SWORD = ITEMS.registerItem("tidesilver_sword",
            properties -> GearItems.sword(GearTier.TIDESILVER, properties.fireResistant()));
    public static final DeferredItem<AxeItem> TIDESILVER_AXE = ITEMS.registerItem("tidesilver_axe",
            properties -> GearItems.axe(GearTier.TIDESILVER, properties.fireResistant()));
    public static final DeferredItem<PickaxeItem> TIDESILVER_PICKAXE = ITEMS.registerItem("tidesilver_pickaxe",
            properties -> GearItems.pickaxe(GearTier.TIDESILVER, properties.fireResistant()));
    public static final DeferredItem<ShovelItem> TIDESILVER_SHOVEL = ITEMS.registerItem("tidesilver_shovel",
            properties -> GearItems.shovel(GearTier.TIDESILVER, properties.fireResistant()));
    public static final DeferredItem<HoeItem> TIDESILVER_HOE = ITEMS.registerItem("tidesilver_hoe",
            properties -> GearItems.hoe(GearTier.TIDESILVER, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> TIDESILVER_HELMET = ITEMS.registerItem("tidesilver_helmet",
            properties -> GearItems.armor(GearTier.TIDESILVER, ArmorItem.Type.HELMET, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> TIDESILVER_CHESTPLATE = ITEMS.registerItem("tidesilver_chestplate",
            properties -> GearItems.armor(GearTier.TIDESILVER, ArmorItem.Type.CHESTPLATE, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> TIDESILVER_LEGGINGS = ITEMS.registerItem("tidesilver_leggings",
            properties -> GearItems.armor(GearTier.TIDESILVER, ArmorItem.Type.LEGGINGS, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> TIDESILVER_BOOTS = ITEMS.registerItem("tidesilver_boots",
            properties -> GearItems.armor(GearTier.TIDESILVER, ArmorItem.Type.BOOTS, properties.fireResistant()));

    /** A toxic crystal shard and 4 arrows make 4 (golf 4, §6.7): Poison I for 5 s. */
    public static final DeferredItem<EffectArrowItem> POISON_ARROW = ITEMS.registerItem("poison_arrow",
            properties -> new EffectArrowItem(ArrowEffect.POISON, properties));

    // ---- Nether ring 4 ----
    /** Drop of the purity crystal cluster; with bone sand the raw material of sky glass (phase 2). Placeholder. */
    public static final DeferredItem<Item> PURITY_CRYSTAL_SHARD = ITEMS.registerSimpleItem("purity_crystal_shard");

    // ---- Ring 5 ----
    static {
        SECTIONS.start(5);
    }

    // Placeholders like raw_sunstone: their use and recipes follow in phase 2.
    /** Drop of the glitch block (1-3, Fortune adds, at most 4). */
    public static final DeferredItem<Item> GLITCH_SHARD = ITEMS.registerSimpleItem("glitch_shard");
    /** Drop of the nullite ores (ring 5's metal). */
    public static final DeferredItem<Item> RAW_NULLITE = ITEMS.registerSimpleItem("raw_nullite");
    /** Smelted from raw nullite and its ores (phase 2 golf 1); the nullite tier's ingot. Provisional name. */
    public static final DeferredItem<Item> NULLITE_INGOT = ITEMS.registerSimpleItem("nullite_ingot");
    /** Upgrades tidesilver gear to nullite; crafted from sky glass (golf 5, §7.1), no copy recipe. */
    public static final DeferredItem<SmithingTemplateItem> NULLITE_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            UpgradeSmithingTemplates.itemName(GearTier.NULLITE), () -> UpgradeSmithingTemplates.create(GearTier.NULLITE));
    /** Coating against the void (applied with white core). Where to find it: golf 5. */
    public static final DeferredItem<SmithingTemplateItem> VOID_COATING_SMITHING_TEMPLATE = ITEMS.register(
            "void_coating_smithing_template", () -> CoatingTemplates.create(Coating.VOID));
    /** A glitch shard and 4 arrows make 4 (golf 5, §7.4): the mob it hits hops 1–5 blocks to a safe spot. */
    public static final DeferredItem<EffectArrowItem> GLITCH_ARROW = ITEMS.registerItem("glitch_arrow",
            properties -> new EffectArrowItem(ArrowEffect.GLITCH, properties));

    // The nullite set (golf 5, §6.1, §7.1): fire resistant like tidesilver; smithed from tidesilver.
    public static final DeferredItem<SwordItem> NULLITE_SWORD = ITEMS.registerItem("nullite_sword",
            properties -> GearItems.sword(GearTier.NULLITE, properties.fireResistant()));
    public static final DeferredItem<AxeItem> NULLITE_AXE = ITEMS.registerItem("nullite_axe",
            properties -> GearItems.axe(GearTier.NULLITE, properties.fireResistant()));
    public static final DeferredItem<PickaxeItem> NULLITE_PICKAXE = ITEMS.registerItem("nullite_pickaxe",
            properties -> GearItems.pickaxe(GearTier.NULLITE, properties.fireResistant()));
    public static final DeferredItem<ShovelItem> NULLITE_SHOVEL = ITEMS.registerItem("nullite_shovel",
            properties -> GearItems.shovel(GearTier.NULLITE, properties.fireResistant()));
    public static final DeferredItem<HoeItem> NULLITE_HOE = ITEMS.registerItem("nullite_hoe",
            properties -> GearItems.hoe(GearTier.NULLITE, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> NULLITE_HELMET = ITEMS.registerItem("nullite_helmet",
            properties -> GearItems.armor(GearTier.NULLITE, ArmorItem.Type.HELMET, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> NULLITE_CHESTPLATE = ITEMS.registerItem("nullite_chestplate",
            properties -> GearItems.armor(GearTier.NULLITE, ArmorItem.Type.CHESTPLATE, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> NULLITE_LEGGINGS = ITEMS.registerItem("nullite_leggings",
            properties -> GearItems.armor(GearTier.NULLITE, ArmorItem.Type.LEGGINGS, properties.fireResistant()));
    public static final DeferredItem<ArmorItem> NULLITE_BOOTS = ITEMS.registerItem("nullite_boots",
            properties -> GearItems.armor(GearTier.NULLITE, ArmorItem.Type.BOOTS, properties.fireResistant()));

    // ---- Nether ring 5 ----
    /** Drop of the white core ore (one per ore, Fortune adds). Placeholder without behaviour in phase 1. */
    public static final DeferredItem<Item> WHITE_CORE = ITEMS.registerSimpleItem("white_core");
    /** Coating against the silence (applied with white core). Where to find it: golf 5. */
    public static final DeferredItem<SmithingTemplateItem> SILENCE_COATING_SMITHING_TEMPLATE = ITEMS.register(
            "silence_coating_smithing_template", () -> CoatingTemplates.create(Coating.SILENCE));

    /** The gear sets per tier, lowest first. */
    public static final List<GearSet> GEAR_SETS = List.of(
            new GearSet(GearTier.SUNSTONE, SUNSTONE_SWORD, SUNSTONE_AXE, SUNSTONE_PICKAXE, SUNSTONE_SHOVEL, SUNSTONE_HOE,
                    SUNSTONE_HELMET, SUNSTONE_CHESTPLATE, SUNSTONE_LEGGINGS, SUNSTONE_BOOTS),
            new GearSet(GearTier.VOLCANITE, VOLCANITE_SWORD, VOLCANITE_AXE, VOLCANITE_PICKAXE, VOLCANITE_SHOVEL,
                    VOLCANITE_HOE, VOLCANITE_HELMET, VOLCANITE_CHESTPLATE, VOLCANITE_LEGGINGS, VOLCANITE_BOOTS),
            new GearSet(GearTier.TIDESILVER, TIDESILVER_SWORD, TIDESILVER_AXE, TIDESILVER_PICKAXE, TIDESILVER_SHOVEL,
                    TIDESILVER_HOE, TIDESILVER_HELMET, TIDESILVER_CHESTPLATE, TIDESILVER_LEGGINGS, TIDESILVER_BOOTS),
            new GearSet(GearTier.NULLITE, NULLITE_SWORD, NULLITE_AXE, NULLITE_PICKAXE, NULLITE_SHOVEL, NULLITE_HOE,
                    NULLITE_HELMET, NULLITE_CHESTPLATE, NULLITE_LEGGINGS, NULLITE_BOOTS));

    private ModItems() {
    }

    /** The set of {@code tier}; every tier has one. */
    public static GearSet gear(GearTier tier) {
        return GEAR_SETS.stream().filter(set -> set.tier() == tier).findFirst().orElseThrow();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** All items in registration order. */
    public static Collection<DeferredHolder<Item, ? extends Item>> entries() {
        return ITEMS.getEntries();
    }

    /**
     * The items registered in ring {@code ring}'s sections, in registration order; standalone items only, since the
     * block items all register after them (filter them out: they belong to their block's tab).
     */
    static List<DeferredHolder<Item, ? extends Item>> entriesOfRing(int ring) {
        return SECTIONS.entries(ring);
    }
}
