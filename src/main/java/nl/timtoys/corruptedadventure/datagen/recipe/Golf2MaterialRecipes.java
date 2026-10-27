package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Phase 2 golf 2 (stream M, fase-2-items.md §5.2–§5.4): the soul crystal block (4 crystals, one way, like amethyst),
 * the turquoise inlaid sandstone set (8 scorched sandstone around a turquoise, then the shapes like any stone family),
 * the salt lamp (a torch on a salt block, like the jack o'lantern) and the first dehydration coating template.
 */
public final class Golf2MaterialRecipes extends RecipeSet {
    private static final FeatureFlagSet VANILLA = FeatureFlagSet.of(FeatureFlags.VANILLA);

    private Golf2MaterialRecipes() {
    }

    public static void build(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SOUL_CRYSTAL_BLOCK)
                .define('#', ModItems.SOUL_CRYSTAL)
                .pattern("##")
                .pattern("##")
                .unlockedBy(getHasName(ModItems.SOUL_CRYSTAL), has(ModItems.SOUL_CRYSTAL))
                .save(output);

        inlaidSandstone(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SALT_LAMP)
                .define('A', Items.TORCH)
                .define('B', ModBlocks.SALT_BLOCK)
                .pattern("A")
                .pattern("B")
                .unlockedBy(getHasName(ModBlocks.SALT_BLOCK), has(ModBlocks.SALT_BLOCK))
                .save(output);

        // The first template comes from ring 2 itself (§5.3); CoatingRecipes keeps the copy recipe.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DEHYDRATION_COATING_SMITHING_TEMPLATE)
                .define('T', ModItems.TURQUOISE)
                .define('S', ModBlocks.SCORCHED_SANDSTONE)
                .define('C', ModBlocks.BARREL_CACTUS)
                .pattern("TST")
                .pattern("SCS")
                .pattern("TST")
                .unlockedBy(getHasName(ModItems.TURQUOISE), has(ModItems.TURQUOISE))
                .save(output, CorruptedAdventure.id("dehydration_coating_smithing_template_from_barrel_cactus"));
    }

    /** Like dyed terracotta: 8 around 1 → 8; then the shapes at the crafting table and the stonecutter. */
    private static void inlaidSandstone(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.TURQUOISE_INLAID_SANDSTONE, 8)
                .define('#', ModBlocks.SCORCHED_SANDSTONE)
                .define('X', ModItems.TURQUOISE)
                .pattern("###")
                .pattern("#X#")
                .pattern("###")
                .unlockedBy(getHasName(ModItems.TURQUOISE), has(ModItems.TURQUOISE))
                .save(output);
        generateRecipes(output, new BlockFamily.Builder(ModBlocks.TURQUOISE_INLAID_SANDSTONE.get())
                .stairs(ModBlocks.TURQUOISE_INLAID_SANDSTONE_STAIRS.get())
                .slab(ModBlocks.TURQUOISE_INLAID_SANDSTONE_SLAB.get())
                .wall(ModBlocks.TURQUOISE_INLAID_SANDSTONE_WALL.get())
                .getFamily(), VANILLA);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.TURQUOISE_INLAID_SANDSTONE_STAIRS,
                ModBlocks.TURQUOISE_INLAID_SANDSTONE, 1);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.TURQUOISE_INLAID_SANDSTONE_SLAB,
                ModBlocks.TURQUOISE_INLAID_SANDSTONE, 2);
        stonecutting(output, RecipeCategory.DECORATIONS, ModBlocks.TURQUOISE_INLAID_SANDSTONE_WALL,
                ModBlocks.TURQUOISE_INLAID_SANDSTONE, 1);
    }
}
