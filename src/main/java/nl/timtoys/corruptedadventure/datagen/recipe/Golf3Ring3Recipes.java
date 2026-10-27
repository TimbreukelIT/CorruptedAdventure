package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Phase 2 golf 3 (stream R3, fase-2-items.md §6.3–§6.4): the first heat coating template, gunpowder from sulfur, the
 * sulfur block (9 both ways), the fire ruby inlaid pumice set (like the turquoise inlaid sandstone), the sulfur lamp
 * (like the redstone lamp), the pumice raft, the dew crystal block (like amethyst) and the dripshroom stew (like mushroom
 * stew). Ids in our namespace, also where the result is vanilla's.
 */
public final class Golf3Ring3Recipes extends RecipeSet {
    private static final FeatureFlagSet VANILLA = FeatureFlagSet.of(FeatureFlags.VANILLA);

    private Golf3Ring3Recipes() {
    }

    public static void build(RecipeOutput output) {
        // The first template comes from ring 3 itself (§6.3); CoatingRecipes keeps the copy recipe.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HEAT_COATING_SMITHING_TEMPLATE)
                .define('R', ModItems.FIRE_RUBY)
                .define('P', ModBlocks.PUMICE)
                .define('D', ModItems.DEW_CRYSTAL)
                .pattern("RPR")
                .pattern("PDP")
                .pattern("RPR")
                .unlockedBy(getHasName(ModItems.FIRE_RUBY), has(ModItems.FIRE_RUBY))
                .save(output, CorruptedAdventure.id("heat_coating_smithing_template_from_fire_ruby"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, Items.GUNPOWDER, 2)
                .requires(ModItems.SULFUR)
                .requires(Items.CHARCOAL)
                .unlockedBy(getHasName(ModItems.SULFUR), has(ModItems.SULFUR))
                .save(output, CorruptedAdventure.id("gunpowder_from_sulfur"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SULFUR_BLOCK)
                .define('#', ModItems.SULFUR)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .unlockedBy(getHasName(ModItems.SULFUR), has(ModItems.SULFUR))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SULFUR, 9)
                .requires(ModBlocks.SULFUR_BLOCK)
                .unlockedBy(getHasName(ModBlocks.SULFUR_BLOCK), has(ModBlocks.SULFUR_BLOCK))
                .save(output, CorruptedAdventure.id("sulfur_from_sulfur_block"));

        inlaidPumice(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.SULFUR_LAMP)
                .define('S', ModItems.SULFUR)
                .define('G', Items.GLASS)
                .pattern(" S ")
                .pattern("SGS")
                .pattern(" S ")
                .unlockedBy(getHasName(ModItems.SULFUR), has(ModItems.SULFUR))
                .save(output);

        // Not 2 × 2: that is polished pumice.
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.PUMICE_RAFT, 2)
                .define('#', ModBlocks.PUMICE)
                .pattern("# #")
                .pattern("# #")
                .unlockedBy(getHasName(ModBlocks.PUMICE), has(ModBlocks.PUMICE))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DEW_CRYSTAL_BLOCK)
                .define('#', ModItems.DEW_CRYSTAL)
                .pattern("##")
                .pattern("##")
                .unlockedBy(getHasName(ModItems.DEW_CRYSTAL), has(ModItems.DEW_CRYSTAL))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.DRIPSHROOM_STEW)
                .requires(ModBlocks.DRIPSHROOM_CAP)
                .requires(Items.BOWL)
                .unlockedBy(getHasName(ModBlocks.DRIPSHROOM_CAP), has(ModBlocks.DRIPSHROOM_CAP))
                .save(output);
    }

    /** Like the turquoise inlaid sandstone: 8 pumice around a fire ruby → 8; then the shapes (table and stonecutter). */
    private static void inlaidPumice(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.FIRE_RUBY_INLAID_PUMICE, 8)
                .define('#', ModBlocks.PUMICE)
                .define('X', ModItems.FIRE_RUBY)
                .pattern("###")
                .pattern("#X#")
                .pattern("###")
                .unlockedBy(getHasName(ModItems.FIRE_RUBY), has(ModItems.FIRE_RUBY))
                .save(output);
        generateRecipes(output, new BlockFamily.Builder(ModBlocks.FIRE_RUBY_INLAID_PUMICE.get())
                .stairs(ModBlocks.FIRE_RUBY_INLAID_PUMICE_STAIRS.get())
                .slab(ModBlocks.FIRE_RUBY_INLAID_PUMICE_SLAB.get())
                .wall(ModBlocks.FIRE_RUBY_INLAID_PUMICE_WALL.get())
                .getFamily(), VANILLA);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.FIRE_RUBY_INLAID_PUMICE_STAIRS,
                ModBlocks.FIRE_RUBY_INLAID_PUMICE, 1);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.FIRE_RUBY_INLAID_PUMICE_SLAB,
                ModBlocks.FIRE_RUBY_INLAID_PUMICE, 2);
        stonecutting(output, RecipeCategory.DECORATIONS, ModBlocks.FIRE_RUBY_INLAID_PUMICE_WALL,
                ModBlocks.FIRE_RUBY_INLAID_PUMICE, 1);
    }
}
