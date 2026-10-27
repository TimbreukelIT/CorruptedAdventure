package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import nl.timtoys.corruptedadventure.datagen.recipe.CoatingRecipes;
import nl.timtoys.corruptedadventure.datagen.recipe.ConsumableRecipes;
import nl.timtoys.corruptedadventure.datagen.recipe.FloraRecipes;
import nl.timtoys.corruptedadventure.datagen.recipe.Golf2MaterialRecipes;
import nl.timtoys.corruptedadventure.datagen.recipe.Golf3Ring3Recipes;
import nl.timtoys.corruptedadventure.datagen.recipe.GearRecipes;
import nl.timtoys.corruptedadventure.datagen.recipe.MaterialRecipes;
import nl.timtoys.corruptedadventure.datagen.recipe.StoneSetRecipes;
import nl.timtoys.corruptedadventure.datagen.recipe.WoodSetRecipes;

import java.util.concurrent.CompletableFuture;

/** Recipes: one line per recipe group in {@code datagen/recipe/}. */
final class ModRecipeProvider extends RecipeProvider {
    ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        WoodSetRecipes.build(output);
        StoneSetRecipes.build(output);
        FloraRecipes.build(output);
        MaterialRecipes.build(output);
        CoatingRecipes.build(output);
        Golf2MaterialRecipes.build(output);
        Golf3Ring3Recipes.build(output);
        ConsumableRecipes.build(output);
        GearRecipes.build(output);
    }
}
