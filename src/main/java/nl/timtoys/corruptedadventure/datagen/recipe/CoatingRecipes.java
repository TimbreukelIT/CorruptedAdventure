package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTrimRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.item.coating.Coating;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The coating recipes (fase-2-items.md §4.4): per coating a vanilla {@code smithing_trim} recipe that takes only that
 * coating's template and exactly its material (vanilla's own trim recipes take {@code #trim_materials}, which our
 * materials are not in), and a placeholder copy recipe like vanilla's template duplication: template + 7 × material +
 * a stone of the coating's ring → 2 templates. Where the first template is found follows per golf.
 */
public final class CoatingRecipes {
    private CoatingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (Coating coating : Coating.values()) {
            Item template = coating.template();
            Item material = coating.material().item();
            SmithingTrimRecipeBuilder.smithingTrim(Ingredient.of(template), Ingredient.of(ItemTags.TRIMMABLE_ARMOR),
                            Ingredient.of(material), RecipeCategory.MISC)
                    .unlocks("has_smithing_trim_template", InventoryChangeTrigger.TriggerInstance.hasItems(template))
                    .save(output, CorruptedAdventure.id(coating.id() + "_smithing_trim"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, template, 2)
                    .define('#', material)
                    .define('C', ringStone(coating))
                    .define('S', template)
                    .pattern("#S#")
                    .pattern("#C#")
                    .pattern("###")
                    .unlockedBy("has_" + coating.id() + "_smithing_template",
                            InventoryChangeTrigger.TriggerInstance.hasItems(template))
                    .save(output, CorruptedAdventure.id(coating.id() + "_smithing_template"));
        }
    }

    /** The stone that stands for the ring of the condition, like vanilla's cobblestone, sandstone or netherrack. */
    private static ItemLike ringStone(Coating coating) {
        return switch (coating) {
            case DEHYDRATION -> ModBlocks.SCORCHED_SANDSTONE;
            case HEAT -> ModBlocks.PUMICE;
            case COLD -> ModBlocks.COLD_LAVA;
            case POISON -> ModBlocks.TIDESTONE;
            case VOID -> ModBlocks.VOIDSTONE;
            case SILENCE -> ModBlocks.WHITE_STONE;
        };
    }
}
