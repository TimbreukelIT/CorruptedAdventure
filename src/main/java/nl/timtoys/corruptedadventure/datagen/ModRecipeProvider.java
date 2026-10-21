package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.concurrent.CompletableFuture;

/** Recipes. Phase 1 has only the few that make a world block usable like its vanilla counterpart. */
final class ModRecipeProvider extends RecipeProvider {
    ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Lagoon sand (66d) smelts to glass in a furnace exactly like vanilla sand: 0.1 xp, 200 ticks.
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.LAGOON_SAND), RecipeCategory.BUILDING_BLOCKS,
                        Items.GLASS, 0.1F, 200)
                .unlockedBy(getHasName(ModBlocks.LAGOON_SAND), has(ModBlocks.LAGOON_SAND))
                .save(output, CorruptedAdventure.id("glass_from_smelting_lagoon_sand"));
    }
}
