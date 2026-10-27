package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * Phase 2 golf 4 (stream R4, fase-2-items.md §6.6–§6.7): the ring recipes of the poison and cold coating templates
 * (4 jade + 4 tidestone + 1 primordial ice; 4 fire crystal + 4 cold lava + 1 primordial ice; like ring 2's dehydration
 * template) and the two inlaid sets (8 stones around a gem, then
 * the shapes like any stone family). {@link CoatingRecipes} keeps the copy recipe of each template.
 */
public final class Golf4MaterialRecipes extends RecipeSet {
    private static final FeatureFlagSet VANILLA = FeatureFlagSet.of(FeatureFlags.VANILLA);

    private Golf4MaterialRecipes() {
    }

    public static void build(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.POISON_COATING_SMITHING_TEMPLATE)
                .define('J', ModItems.JADE)
                .define('S', ModBlocks.TIDESTONE)
                .define('C', ModBlocks.PRIMORDIAL_ICE)
                .pattern("JSJ")
                .pattern("SCS")
                .pattern("JSJ")
                .unlockedBy(getHasName(ModItems.JADE), has(ModItems.JADE))
                .save(output, CorruptedAdventure.id("poison_coating_smithing_template_from_primordial_ice"));
        // Fire crystal is already the cold coating's material, so primordial ice (Nether ring 3's own) is the core.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COLD_COATING_SMITHING_TEMPLATE)
                .define('F', ModItems.FIRE_CRYSTAL)
                .define('S', ModBlocks.COLD_LAVA)
                .define('C', ModBlocks.PRIMORDIAL_ICE)
                .pattern("FSF")
                .pattern("SCS")
                .pattern("FSF")
                .unlockedBy(getHasName(ModItems.FIRE_CRYSTAL), has(ModItems.FIRE_CRYSTAL))
                .save(output, CorruptedAdventure.id("cold_coating_smithing_template_from_primordial_ice"));

        inlaid(output, ModBlocks.JADE_INLAID_TIDESTONE, ModBlocks.JADE_INLAID_TIDESTONE_STAIRS,
                ModBlocks.JADE_INLAID_TIDESTONE_SLAB, ModBlocks.JADE_INLAID_TIDESTONE_WALL, ModBlocks.TIDESTONE, ModItems.JADE);
        inlaid(output, ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE, ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_STAIRS,
                ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_SLAB, ModBlocks.AQUAMARINE_INLAID_LAGOON_SANDSTONE_WALL,
                ModBlocks.LAGOON_SANDSTONE, ModItems.AQUAMARINE_SHARD);
    }

    /** Like dyed terracotta: 8 around 1 → 8; then the shapes at the crafting table and the stonecutter. */
    private static void inlaid(RecipeOutput output, DeferredBlock<? extends Block> block, DeferredBlock<? extends Block> stairs,
                               DeferredBlock<? extends Block> slab, DeferredBlock<? extends Block> wall, ItemLike stone,
                               ItemLike gem) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block, 8)
                .define('#', stone)
                .define('X', gem)
                .pattern("###")
                .pattern("#X#")
                .pattern("###")
                .unlockedBy(getHasName(gem), has(gem))
                .save(output);
        generateRecipes(output, new BlockFamily.Builder(block.get())
                .stairs(stairs.get())
                .slab(slab.get())
                .wall(wall.get())
                .getFamily(), VANILLA);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, stairs, block, 1);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, slab, block, 2);
        stonecutting(output, RecipeCategory.DECORATIONS, wall, block, 1);
    }
}
