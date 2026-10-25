package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.datagen.recipe.CoatingRecipes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.concurrent.CompletableFuture;

/**
 * Recipes. Phase 1 has only the few that make a world block usable like its vanilla counterpart (lagoon sand and
 * sandstone, the corrupted wood set).
 */
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
        lagoonSandstone(output);
        ring5(output);
        CoatingRecipes.build(output);
    }

    /**
     * The corrupted wood set (88r, contract ring 5 §11) gets exactly oak's recipes: planks from the logs tag, wood and
     * stripped wood, the hanging sign, and the planks family (stairs, slab, fence, fence gate, door, trapdoor, button,
     * pressure plate, sign) through vanilla's family generator, with oak's counts, groups and unlock conditions. Sticks,
     * crafting tables, chests and the like, and glass from corrupted sand, come from the vanilla tags.
     */
    private static void ring5(RecipeOutput output) {
        planksFromLogs(output, ModBlocks.CORRUPTED_PLANKS, ModTags.Items.CORRUPTED_LOGS, 4);
        woodFromLogs(output, ModBlocks.CORRUPTED_WOOD, ModBlocks.CORRUPTED_LOG);
        woodFromLogs(output, ModBlocks.STRIPPED_CORRUPTED_WOOD, ModBlocks.STRIPPED_CORRUPTED_LOG);
        hangingSign(output, ModBlocks.CORRUPTED_HANGING_SIGN, ModBlocks.STRIPPED_CORRUPTED_LOG);
        generateRecipes(output, corruptedPlanksFamily(), FeatureFlagSet.of(FeatureFlags.VANILLA));
    }

    /** Like vanilla's {@code BlockFamilies.OAK_PLANKS}; built here, outside vanilla's family registry. */
    private static BlockFamily corruptedPlanksFamily() {
        return new BlockFamily.Builder(ModBlocks.CORRUPTED_PLANKS.get())
                .button(ModBlocks.CORRUPTED_BUTTON.get())
                .fence(ModBlocks.CORRUPTED_FENCE.get())
                .fenceGate(ModBlocks.CORRUPTED_FENCE_GATE.get())
                .pressurePlate(ModBlocks.CORRUPTED_PRESSURE_PLATE.get())
                .sign(ModBlocks.CORRUPTED_SIGN.get(), ModBlocks.CORRUPTED_WALL_SIGN.get())
                .slab(ModBlocks.CORRUPTED_SLAB.get())
                .stairs(ModBlocks.CORRUPTED_STAIRS.get())
                .door(ModBlocks.CORRUPTED_DOOR.get())
                .trapdoor(ModBlocks.CORRUPTED_TRAPDOOR.get())
                .recipeGroupPrefix("wooden")
                .recipeUnlockedBy("has_planks")
                .getFamily();
    }

    /**
     * The lagoon sandstone set (66g) gets exactly vanilla sandstone's recipes, with the same shapes, counts, categories,
     * unlock conditions and ids (in our namespace).
     */
    private static void lagoonSandstone(RecipeOutput output) {
        ItemLike sand = ModBlocks.LAGOON_SAND;
        ItemLike sandstone = ModBlocks.LAGOON_SANDSTONE;
        ItemLike chiseled = ModBlocks.CHISELED_LAGOON_SANDSTONE;
        ItemLike cut = ModBlocks.CUT_LAGOON_SANDSTONE;
        ItemLike smooth = ModBlocks.SMOOTH_LAGOON_SANDSTONE;

        twoByTwoPacker(output, RecipeCategory.BUILDING_BLOCKS, sandstone, sand);
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.LAGOON_SANDSTONE_SLAB, Ingredient.of(sandstone, chiseled))
                .unlockedBy(getHasName(sandstone), has(sandstone))
                .unlockedBy(getHasName(chiseled), has(chiseled))
                .save(output);
        stairBuilder(ModBlocks.LAGOON_SANDSTONE_STAIRS, Ingredient.of(sandstone, chiseled, cut))
                .unlockedBy(getHasName(sandstone), has(sandstone))
                .unlockedBy(getHasName(chiseled), has(chiseled))
                .unlockedBy(getHasName(cut), has(cut))
                .save(output);
        wall(output, RecipeCategory.DECORATIONS, ModBlocks.LAGOON_SANDSTONE_WALL, sandstone);
        cut(output, RecipeCategory.BUILDING_BLOCKS, cut, sandstone);
        slab(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_LAGOON_SANDSTONE_SLAB, cut);
        chiseled(output, RecipeCategory.BUILDING_BLOCKS, chiseled, ModBlocks.LAGOON_SANDSTONE_SLAB);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(sandstone), RecipeCategory.BUILDING_BLOCKS, smooth, 0.1F, 200)
                .unlockedBy(getHasName(sandstone), has(sandstone))
                .save(output);
        slab(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SMOOTH_LAGOON_SANDSTONE_SLAB, smooth);
        stairBuilder(ModBlocks.SMOOTH_LAGOON_SANDSTONE_STAIRS, Ingredient.of(smooth))
                .unlockedBy(getHasName(smooth), has(smooth))
                .save(output);

        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, cut, sandstone, 1);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.LAGOON_SANDSTONE_SLAB, sandstone, 2);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_LAGOON_SANDSTONE_SLAB, sandstone, 2);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_LAGOON_SANDSTONE_SLAB, cut, 2);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.LAGOON_SANDSTONE_STAIRS, sandstone, 1);
        stonecutting(output, RecipeCategory.DECORATIONS, ModBlocks.LAGOON_SANDSTONE_WALL, sandstone, 1);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, chiseled, sandstone, 1);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SMOOTH_LAGOON_SANDSTONE_SLAB, smooth, 2);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SMOOTH_LAGOON_SANDSTONE_STAIRS, smooth, 1);
    }

    /**
     * Vanilla's {@code stonecutterResultFromBase} with the id in our namespace: vanilla's saves under a bare string id,
     * which would land in {@code minecraft:}.
     */
    private static void stonecutting(RecipeOutput output, RecipeCategory category, ItemLike result, ItemLike material, int count) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(material), category, result, count)
                .unlockedBy(getHasName(material), has(material))
                .save(output, CorruptedAdventure.id(getConversionRecipeName(result, material) + "_stonecutting"));
    }
}
