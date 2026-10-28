package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * The sky glass lens (fase-2-items.md §7.2): a sky glass over two tidesilver ingots, like vanilla's spyglass (no glitch
 * shard, so it can be made before ring 5).
 */
public final class LensRecipes extends RecipeSet {
    private LensRecipes() {
    }

    public static void build(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SKY_GLASS_LENS.get())
                .define('G', ModBlocks.SKY_GLASS.get())
                .define('T', ModItems.TIDESILVER_INGOT.get())
                .pattern(" G ")
                .pattern(" T ")
                .pattern(" T ")
                .unlockedBy(getHasName(ModBlocks.SKY_GLASS.get()), has(ModBlocks.SKY_GLASS.get()))
                .save(output);
    }
}
