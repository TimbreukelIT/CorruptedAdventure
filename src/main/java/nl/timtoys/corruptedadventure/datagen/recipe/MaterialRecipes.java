package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Phase 2 golf 1 (stream D): the material recipes, like vanilla's. Nine raw materials, ingots or gems pack into their
 * storage block and back; four salt make a salt block and back; four crystal shards make the crystal block (like
 * amethyst, one way); every raw material and every ore smelts (furnace) and blasts (blast furnace) to its product with
 * the XP of its vanilla counterpart. All ids in our namespace: vanilla's helpers save under bare string ids, which would
 * land in {@code minecraft:}.
 */
public final class MaterialRecipes {
    private static final int SMELTING_TICKS = 200;
    private static final int BLASTING_TICKS = 100;

    private MaterialRecipes() {
    }

    public static void build(RecipeOutput output) {
        nineBlockStorage(output, ModItems.RAW_SUNSTONE, ModBlocks.RAW_SUNSTONE_BLOCK);
        nineBlockStorage(output, ModItems.RAW_TURQUOISE, ModBlocks.RAW_TURQUOISE_BLOCK);
        nineBlockStorage(output, ModItems.SUNSTONE_INGOT, ModBlocks.SUNSTONE_BLOCK);
        nineBlockStorage(output, ModItems.TURQUOISE, ModBlocks.TURQUOISE_BLOCK);
        nineBlockStorage(output, ModItems.RAW_VOLCANITE, ModBlocks.RAW_VOLCANITE_BLOCK);
        nineBlockStorage(output, ModItems.VOLCANITE_INGOT, ModBlocks.VOLCANITE_BLOCK);
        nineBlockStorage(output, ModItems.FIRE_RUBY, ModBlocks.FIRE_RUBY_BLOCK);
        nineBlockStorage(output, ModItems.RAW_TIDESILVER, ModBlocks.RAW_TIDESILVER_BLOCK);
        nineBlockStorage(output, ModItems.TIDESILVER_INGOT, ModBlocks.TIDESILVER_BLOCK);
        nineBlockStorage(output, ModItems.JADE, ModBlocks.JADE_BLOCK);
        nineBlockStorage(output, ModItems.RAW_NULLITE, ModBlocks.RAW_NULLITE_BLOCK);
        nineBlockStorage(output, ModItems.NULLITE_INGOT, ModBlocks.NULLITE_BLOCK);

        // Backlog r. 101: 4 salt <-> salt block (the salt crust drops 4 salt, like clay).
        twoByTwo(output, ModItems.SALT, ModBlocks.SALT_BLOCK);
        unpack(output, ModItems.SALT, ModBlocks.SALT_BLOCK, 4);
        // The crystal walls of the geodes and fields, like the amethyst block: 4 shards, no way back.
        twoByTwo(output, ModItems.FIRE_CRYSTAL, ModBlocks.FIRE_CRYSTAL_BLOCK);
        twoByTwo(output, ModItems.TOXIC_CRYSTAL_SHARD, ModBlocks.TOXIC_CRYSTAL_BLOCK);
        twoByTwo(output, ModItems.AQUAMARINE_SHARD, ModBlocks.AQUAMARINE_BLOCK);
        twoByTwo(output, ModItems.PURITY_CRYSTAL_SHARD, ModBlocks.PURITY_CRYSTAL_BLOCK);

        // XP like vanilla: iron/copper 0.7 (the metals; turquoise, raw like copper), diamond 1.0 (nullite: diamond's host
        // and rarity; the gems), quartz 0.2 (soul crystal), coal 0.1 (sulfur).
        cook(output, ModItems.SUNSTONE_INGOT, 0.7F, ModItems.RAW_SUNSTONE, ModBlocks.RED_CANYON_SUNSTONE_ORE,
                ModBlocks.ORANGE_CANYON_SUNSTONE_ORE, ModBlocks.CREAM_CANYON_SUNSTONE_ORE, ModBlocks.SCORCHED_SANDSTONE_SUNSTONE_ORE);
        cook(output, ModItems.TURQUOISE, 0.7F, ModItems.RAW_TURQUOISE, ModBlocks.RED_CANYON_TURQUOISE_ORE,
                ModBlocks.ORANGE_CANYON_TURQUOISE_ORE, ModBlocks.CREAM_CANYON_TURQUOISE_ORE);
        cook(output, ModItems.SOUL_CRYSTAL, 0.2F, ModBlocks.SOUL_CRYSTAL_ORE, ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE);
        cook(output, ModItems.VOLCANITE_INGOT, 0.7F, ModItems.RAW_VOLCANITE, ModBlocks.VOLCANITE_ORE,
                ModBlocks.DEEPSLATE_VOLCANITE_ORE, ModBlocks.BASALT_VOLCANITE_ORE);
        cook(output, ModItems.SULFUR, 0.1F, ModBlocks.SULFUR_ORE, ModBlocks.BASALT_SULFUR_ORE);
        cook(output, ModItems.FIRE_RUBY, 1.0F, ModBlocks.FIRE_RUBY_ORE, ModBlocks.DEEPSLATE_FIRE_RUBY_ORE,
                ModBlocks.BASALT_FIRE_RUBY_ORE);
        cook(output, ModItems.TIDESILVER_INGOT, 0.7F, ModItems.RAW_TIDESILVER, ModBlocks.TIDESILVER_ORE,
                ModBlocks.TIDESTONE_TIDESILVER_ORE, ModBlocks.DEEPSLATE_TIDESILVER_ORE);
        cook(output, ModItems.JADE, 1.0F, ModBlocks.JADE_ORE, ModBlocks.TIDESTONE_JADE_ORE, ModBlocks.DEEPSLATE_JADE_ORE);
        cook(output, ModItems.NULLITE_INGOT, 1.0F, ModItems.RAW_NULLITE, ModBlocks.NULLITE_ORE, ModBlocks.VOIDSTONE_NULLITE_ORE);
        cook(output, ModItems.WHITE_CORE, 1.0F, ModBlocks.WHITE_CORE_ORE);
    }

    /** Vanilla's {@code nineBlockStorageRecipes}: 3×3 into the block (id {@code <block>}), the block back into 9. */
    private static void nineBlockStorage(RecipeOutput output, ItemLike item, ItemLike block) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block)
                .define('#', item)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy(hasName(item), has(item))
                .save(output);
        unpack(output, item, block, 9);
    }

    /** Vanilla's {@code twoByTwoPacker}: four into the block, id {@code <block>}. */
    private static void twoByTwo(RecipeOutput output, ItemLike item, ItemLike block) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block)
                .define('#', item)
                .pattern("##")
                .pattern("##")
                .unlockedBy(hasName(item), has(item))
                .save(output);
    }

    /** The block back into {@code count} items, id {@code <item>_from_<block>}. */
    private static void unpack(RecipeOutput output, ItemLike item, ItemLike block, int count) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item, count)
                .requires(block)
                .unlockedBy(hasName(block), has(block))
                .save(output, CorruptedAdventure.id(name(item) + "_from_" + name(block)));
    }

    /**
     * Vanilla's {@code oreSmelting} and {@code oreBlasting}: every input in a furnace (200 ticks) and a blast furnace
     * (100 ticks), grouped by the product, ids {@code <product>_from_smelting_<input>} and {@code _from_blasting_}.
     */
    private static void cook(RecipeOutput output, ItemLike result, float xp, ItemLike... inputs) {
        for (ItemLike input : inputs) {
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, xp, SMELTING_TICKS)
                    .group(name(result))
                    .unlockedBy(hasName(input), has(input))
                    .save(output, CorruptedAdventure.id(name(result) + "_from_smelting_" + name(input)));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, xp, BLASTING_TICKS)
                    .group(name(result))
                    .unlockedBy(hasName(input), has(input))
                    .save(output, CorruptedAdventure.id(name(result) + "_from_blasting_" + name(input)));
        }
    }

    private static String name(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    private static String hasName(ItemLike item) {
        return "has_" + name(item);
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }
}
