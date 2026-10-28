package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Phase 2 golf 5 (stream H, fase-2-items.md §7.2): 4 bone sand + 1 purity crystal shard → 4 sky sand (shapeless), sky
 * sand smelts into sky glass like sand into glass (furnace only, 0.1 XP), 6 glass → 16 panes like vanilla's.
 */
public final class Golf5SkyGlassRecipes extends RecipeSet {
    private Golf5SkyGlassRecipes() {
    }

    public static void build(RecipeOutput output) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SKY_SAND, 4)
                .requires(ModBlocks.BONE_SAND, 4)
                .requires(ModItems.PURITY_CRYSTAL_SHARD)
                .unlockedBy(getHasName(ModItems.PURITY_CRYSTAL_SHARD), has(ModItems.PURITY_CRYSTAL_SHARD))
                .save(output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.SKY_SAND), RecipeCategory.BUILDING_BLOCKS,
                        ModBlocks.SKY_GLASS, 0.1F, 200)
                .unlockedBy(getHasName(ModBlocks.SKY_SAND), has(ModBlocks.SKY_SAND))
                .save(output, CorruptedAdventure.id("sky_glass_from_smelting_sky_sand"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.SKY_GLASS_PANE, 16)
                .define('#', ModBlocks.SKY_GLASS)
                .pattern("###")
                .pattern("###")
                .unlockedBy(getHasName(ModBlocks.SKY_GLASS), has(ModBlocks.SKY_GLASS))
                .save(output);
    }
}
