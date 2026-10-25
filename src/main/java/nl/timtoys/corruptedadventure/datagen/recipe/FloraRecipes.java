package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/** Dyes from our flowers, like vanilla's one-flower-one-dye recipes (same group and unlock), ids in our namespace. */
public final class FloraRecipes extends RecipeSet {
    private FloraRecipes() {
    }

    public static void build(RecipeOutput output) {
        // The desert flower's petals are orange (like the orange tulip: one dye).
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.ORANGE_DYE)
                .requires(ModBlocks.DESERT_FLOWER)
                .group("orange_dye")
                .unlockedBy(getHasName(ModBlocks.DESERT_FLOWER), has(ModBlocks.DESERT_FLOWER))
                .save(output, CorruptedAdventure.id(getConversionRecipeName(Items.ORANGE_DYE, ModBlocks.DESERT_FLOWER)));
    }
}
