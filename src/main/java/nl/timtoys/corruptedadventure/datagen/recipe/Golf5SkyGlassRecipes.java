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
 * Phase 2 golf 5 (stream H, fase-2-items.md §7.2–§7.4): 4 bone sand + 1 purity crystal shard → 4 sky sand (shapeless),
 * sky sand smelts into sky glass like sand into glass (furnace only, 0.1 XP), 6 glass → 16 panes like vanilla's; 9 glitch
 * shards ↔ the glitch shard block; the ring recipes of the void and silence coating templates (like golf 4's, 4 corners
 * + 4 ring stones around a core; {@link CoatingRecipes} keeps the copy recipe of each template).
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

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.GLITCH_SHARD_BLOCK)
                .define('#', ModItems.GLITCH_SHARD)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy(getHasName(ModItems.GLITCH_SHARD), has(ModItems.GLITCH_SHARD))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.GLITCH_SHARD, 9)
                .requires(ModBlocks.GLITCH_SHARD_BLOCK)
                .unlockedBy(getHasName(ModBlocks.GLITCH_SHARD_BLOCK), has(ModBlocks.GLITCH_SHARD_BLOCK))
                .save(output, CorruptedAdventure.id("glitch_shard_from_glitch_shard_block"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.VOID_COATING_SMITHING_TEMPLATE)
                .define('G', ModItems.GLITCH_SHARD)
                .define('V', ModBlocks.VOIDSTONE)
                .define('H', ModBlocks.SKY_GLASS)
                .pattern("GVG")
                .pattern("VHV")
                .pattern("GVG")
                .unlockedBy(getHasName(ModItems.GLITCH_SHARD), has(ModItems.GLITCH_SHARD))
                .save(output, CorruptedAdventure.id("void_coating_smithing_template_from_sky_glass"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SILENCE_COATING_SMITHING_TEMPLATE)
                .define('Z', ModItems.PURITY_CRYSTAL_SHARD)
                .define('W', ModBlocks.WHITE_STONE)
                .define('K', ModItems.WHITE_CORE)
                .pattern("ZWZ")
                .pattern("WKW")
                .pattern("ZWZ")
                .unlockedBy(getHasName(ModItems.PURITY_CRYSTAL_SHARD), has(ModItems.PURITY_CRYSTAL_SHARD))
                .save(output, CorruptedAdventure.id("silence_coating_smithing_template_from_white_core"));
    }
}
