package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * The gear tier recipes (fase-2-items.md §5.1). Sunstone: a vanilla {@code smithing_transform} per piece, like the
 * netherite upgrade (diamond piece + template + ingot; enchantments and trim/coating carry over), and the template from
 * soul crystal. There is no copy recipe for the template: crafting it already is the copy.
 */
public final class GearRecipes extends RecipeSet {
    private GearRecipes() {
    }

    public static void build(RecipeOutput output) {
        Item template = ModItems.SUNSTONE_UPGRADE_SMITHING_TEMPLATE.get();
        Item ingot = ModItems.SUNSTONE_INGOT.get();
        // The categories of vanilla's netherite upgrades.
        upgrade(output, template, ingot, Items.DIAMOND_SWORD, RecipeCategory.COMBAT, ModItems.SUNSTONE_SWORD.get());
        upgrade(output, template, ingot, Items.DIAMOND_AXE, RecipeCategory.TOOLS, ModItems.SUNSTONE_AXE.get());
        upgrade(output, template, ingot, Items.DIAMOND_PICKAXE, RecipeCategory.TOOLS, ModItems.SUNSTONE_PICKAXE.get());
        upgrade(output, template, ingot, Items.DIAMOND_SHOVEL, RecipeCategory.TOOLS, ModItems.SUNSTONE_SHOVEL.get());
        upgrade(output, template, ingot, Items.DIAMOND_HOE, RecipeCategory.TOOLS, ModItems.SUNSTONE_HOE.get());
        upgrade(output, template, ingot, Items.DIAMOND_HELMET, RecipeCategory.COMBAT, ModItems.SUNSTONE_HELMET.get());
        upgrade(output, template, ingot, Items.DIAMOND_CHESTPLATE, RecipeCategory.COMBAT, ModItems.SUNSTONE_CHESTPLATE.get());
        upgrade(output, template, ingot, Items.DIAMOND_LEGGINGS, RecipeCategory.COMBAT, ModItems.SUNSTONE_LEGGINGS.get());
        upgrade(output, template, ingot, Items.DIAMOND_BOOTS, RecipeCategory.COMBAT, ModItems.SUNSTONE_BOOTS.get());

        // Provisional shape (confirm at the test stop): soul crystal around a scorched sandstone heart, diamond top and bottom.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, template)
                .define('S', ModItems.SOUL_CRYSTAL.get())
                .define('D', Items.DIAMOND)
                .define('C', ModBlocks.SCORCHED_SANDSTONE)
                .pattern("SDS")
                .pattern("SCS")
                .pattern("SDS")
                .unlockedBy(getHasName(ModItems.SOUL_CRYSTAL.get()), has(ModItems.SOUL_CRYSTAL.get()))
                .save(output, CorruptedAdventure.id(getItemName(template)));
    }

    /** Vanilla's {@code netheriteSmithing} with our template and ingot, and the id in our namespace. */
    private static void upgrade(RecipeOutput output, Item template, Item ingot, Item base, RecipeCategory category,
                                Item result) {
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(template), Ingredient.of(base), Ingredient.of(ingot),
                        category, result)
                .unlocks(getHasName(ingot), has(ingot))
                .save(output, CorruptedAdventure.id(getItemName(result) + "_smithing"));
    }
}
