package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Base of the recipe groups in this package. It extends {@link RecipeProvider} only to reach vanilla's protected static
 * recipe helpers; no subclass is ever instantiated (they only have static methods), so the null arguments never run.
 */
abstract class RecipeSet extends RecipeProvider {
    RecipeSet() {
        super(null, null);
    }

    /**
     * Vanilla's {@code stonecutterResultFromBase} with the id in our namespace: vanilla's saves under a bare string id,
     * which would land in {@code minecraft:}.
     */
    static void stonecutting(RecipeOutput output, RecipeCategory category, ItemLike result, ItemLike material, int count) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(material), category, result, count)
                .unlockedBy(getHasName(material), has(material))
                .save(output, CorruptedAdventure.id(getConversionRecipeName(result, material) + "_stonecutting"));
    }
}
