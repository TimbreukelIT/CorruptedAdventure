package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;

import javax.annotation.Nullable;

/**
 * The stone and sandstone sets get vanilla's recipes for the vanilla set they copy: the shapes at the crafting table
 * and the stonecutter, the 2 × 2 step to the next polished or brick tier (like tuff → polished tuff → tuff bricks),
 * and cobblestone smelting back to its stone. Ids, counts, categories and unlock conditions as vanilla's, ids in our
 * namespace.
 */
public final class StoneSetRecipes extends RecipeSet {
    private static final FeatureFlagSet VANILLA = FeatureFlagSet.of(FeatureFlags.VANILLA);

    private StoneSetRecipes() {
    }

    public static void build(RecipeOutput output) {
        canyonStone(output);
        sandstone(output, null, ModBlocks.SCORCHED_SANDSTONE.get(), ModBlocks.SCORCHED_SANDSTONE_STAIRS.get(),
                ModBlocks.SCORCHED_SANDSTONE_SLAB.get(), ModBlocks.SCORCHED_SANDSTONE_WALL.get(),
                ModBlocks.CUT_SCORCHED_SANDSTONE.get(), ModBlocks.CUT_SCORCHED_SANDSTONE_SLAB.get(),
                ModBlocks.CHISELED_SCORCHED_SANDSTONE.get(), ModBlocks.SMOOTH_SCORCHED_SANDSTONE.get(),
                ModBlocks.SMOOTH_SCORCHED_SANDSTONE_STAIRS.get(), ModBlocks.SMOOTH_SCORCHED_SANDSTONE_SLAB.get());
        // Polished salt and its shapes; 4 salt <-> salt block is stream D's (materials).
        ladder(output, new Shapes(ModBlocks.SALT_BLOCK.get(), null, null, null),
                shapes(ModBlocks.POLISHED_SALT.get(), ModBlocks.POLISHED_SALT_STAIRS.get(), ModBlocks.POLISHED_SALT_SLAB.get(),
                        ModBlocks.POLISHED_SALT_WALL.get()));
        ladder(output,
                shapes(ModBlocks.PUMICE.get(), ModBlocks.PUMICE_STAIRS.get(), ModBlocks.PUMICE_SLAB.get(), ModBlocks.PUMICE_WALL.get()),
                shapes(ModBlocks.POLISHED_PUMICE.get(), ModBlocks.POLISHED_PUMICE_STAIRS.get(), ModBlocks.POLISHED_PUMICE_SLAB.get(),
                        ModBlocks.POLISHED_PUMICE_WALL.get()),
                shapes(ModBlocks.PUMICE_BRICKS.get(), ModBlocks.PUMICE_BRICK_STAIRS.get(), ModBlocks.PUMICE_BRICK_SLAB.get(),
                        ModBlocks.PUMICE_BRICK_WALL.get()));
        ladder(output,
                shapes(ModBlocks.COLD_LAVA.get(), ModBlocks.COLD_LAVA_STAIRS.get(), ModBlocks.COLD_LAVA_SLAB.get(),
                        ModBlocks.COLD_LAVA_WALL.get()),
                shapes(ModBlocks.POLISHED_COLD_LAVA.get(), ModBlocks.POLISHED_COLD_LAVA_STAIRS.get(),
                        ModBlocks.POLISHED_COLD_LAVA_SLAB.get(), ModBlocks.POLISHED_COLD_LAVA_WALL.get()),
                shapes(ModBlocks.COLD_LAVA_BRICKS.get(), ModBlocks.COLD_LAVA_BRICK_STAIRS.get(), ModBlocks.COLD_LAVA_BRICK_SLAB.get(),
                        ModBlocks.COLD_LAVA_BRICK_WALL.get()));
        // Packed nether ice from nine nether ice, like packed ice from ice.
        threeByThreePacker(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.PACKED_NETHER_ICE, ModBlocks.NETHER_ICE);
        tidestone(output);
        lagoonSandstone(output);
        ladder(output,
                shapes(ModBlocks.FOSSIL_STONE.get(), ModBlocks.FOSSIL_STONE_STAIRS.get(), ModBlocks.FOSSIL_STONE_SLAB.get(),
                        ModBlocks.FOSSIL_STONE_WALL.get()),
                shapes(ModBlocks.POLISHED_FOSSIL_STONE.get(), ModBlocks.POLISHED_FOSSIL_STONE_STAIRS.get(),
                        ModBlocks.POLISHED_FOSSIL_STONE_SLAB.get(), ModBlocks.POLISHED_FOSSIL_STONE_WALL.get()),
                shapes(ModBlocks.FOSSIL_STONE_BRICKS.get(), ModBlocks.FOSSIL_STONE_BRICK_STAIRS.get(),
                        ModBlocks.FOSSIL_STONE_BRICK_SLAB.get(), ModBlocks.FOSSIL_STONE_BRICK_WALL.get()));
        // Corrupted cobblestone smelts back to corrupted stone, like cobblestone to stone.
        smeltingResultFromBase(output, ModBlocks.CORRUPTED_STONE, ModBlocks.CORRUPTED_COBBLESTONE);
        whiteStone(output);
    }

    /**
     * Canyon cobblestone with its shapes, smelting back to orange canyon stone (the middle tint, and the cobblestone's
     * own map colour); canyon stone bricks from any mix of the three tints (like stone bricks from stone), and every
     * tint at the stonecutter gives the bricks and their shapes.
     */
    private static void canyonStone(RecipeOutput output) {
        Shapes cobblestone = shapes(ModBlocks.CANYON_COBBLESTONE.get(), ModBlocks.CANYON_COBBLESTONE_STAIRS.get(),
                ModBlocks.CANYON_COBBLESTONE_SLAB.get(), ModBlocks.CANYON_COBBLESTONE_WALL.get());
        Shapes bricks = shapes(ModBlocks.CANYON_STONE_BRICKS.get(), ModBlocks.CANYON_STONE_BRICK_STAIRS.get(),
                ModBlocks.CANYON_STONE_BRICK_SLAB.get(), ModBlocks.CANYON_STONE_BRICK_WALL.get());
        ladder(output, cobblestone);
        smeltingResultFromBase(output, ModBlocks.ORANGE_CANYON_STONE, cobblestone.block());
        ladder(output, bricks);
        polishedBuilder(RecipeCategory.BUILDING_BLOCKS, bricks.block(), Ingredient.of(ModTags.Items.CANYON_STONES))
                .unlockedBy("has_canyon_stones", has(ModTags.Items.CANYON_STONES))
                .save(output);
        for (Block tint : new Block[]{ModBlocks.RED_CANYON_STONE.get(), ModBlocks.ORANGE_CANYON_STONE.get(),
                ModBlocks.CREAM_CANYON_STONE.get()}) {
            stonecutting(output, RecipeCategory.BUILDING_BLOCKS, bricks.block(), tint, 1);
            cutShapes(output, bricks, tint);
        }
    }

    /**
     * Tidestone, polished tidestone and tidestone bricks as one ladder; cobbled tidestone has its own shapes and smelts
     * back to tidestone; mossy tidestone is tidestone with a vine or a moss block (like mossy cobblestone) and has its
     * own shapes.
     */
    private static void tidestone(RecipeOutput output) {
        Block tidestone = ModBlocks.TIDESTONE.get();
        ladder(output,
                shapes(tidestone, ModBlocks.TIDESTONE_STAIRS.get(), ModBlocks.TIDESTONE_SLAB.get(), ModBlocks.TIDESTONE_WALL.get()),
                shapes(ModBlocks.POLISHED_TIDESTONE.get(), ModBlocks.POLISHED_TIDESTONE_STAIRS.get(),
                        ModBlocks.POLISHED_TIDESTONE_SLAB.get(), ModBlocks.POLISHED_TIDESTONE_WALL.get()),
                shapes(ModBlocks.TIDESTONE_BRICKS.get(), ModBlocks.TIDESTONE_BRICK_STAIRS.get(), ModBlocks.TIDESTONE_BRICK_SLAB.get(),
                        ModBlocks.TIDESTONE_BRICK_WALL.get()));
        Shapes cobbled = shapes(ModBlocks.COBBLED_TIDESTONE.get(), ModBlocks.COBBLED_TIDESTONE_STAIRS.get(),
                ModBlocks.COBBLED_TIDESTONE_SLAB.get(), ModBlocks.COBBLED_TIDESTONE_WALL.get());
        ladder(output, cobbled);
        smeltingResultFromBase(output, tidestone, cobbled.block());

        Block mossy = ModBlocks.MOSSY_TIDESTONE.get();
        for (Block moss : new Block[]{Blocks.VINE, Blocks.MOSS_BLOCK}) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, mossy)
                    .requires(tidestone)
                    .requires(moss)
                    .group("mossy_tidestone")
                    .unlockedBy(getHasName(moss), has(moss))
                    .save(output, CorruptedAdventure.id(getConversionRecipeName(mossy, moss)));
        }
        ladder(output, shapes(mossy, ModBlocks.MOSSY_TIDESTONE_STAIRS.get(), ModBlocks.MOSSY_TIDESTONE_SLAB.get(),
                ModBlocks.MOSSY_TIDESTONE_WALL.get()));
    }

    /** The white stone ladder, and the pillar from white stone like the quartz pillar from the quartz block. */
    private static void whiteStone(RecipeOutput output) {
        Block whiteStone = ModBlocks.WHITE_STONE.get();
        ladder(output,
                shapes(whiteStone, ModBlocks.WHITE_STONE_STAIRS.get(), ModBlocks.WHITE_STONE_SLAB.get(), ModBlocks.WHITE_STONE_WALL.get()),
                shapes(ModBlocks.POLISHED_WHITE_STONE.get(), ModBlocks.POLISHED_WHITE_STONE_STAIRS.get(),
                        ModBlocks.POLISHED_WHITE_STONE_SLAB.get(), ModBlocks.POLISHED_WHITE_STONE_WALL.get()),
                shapes(ModBlocks.WHITE_STONE_BRICKS.get(), ModBlocks.WHITE_STONE_BRICK_STAIRS.get(),
                        ModBlocks.WHITE_STONE_BRICK_SLAB.get(), ModBlocks.WHITE_STONE_BRICK_WALL.get()));
        Block pillar = ModBlocks.WHITE_STONE_PILLAR.get();
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, pillar, 2)
                .define('#', whiteStone)
                .pattern("#")
                .pattern("#")
                .unlockedBy(getHasName(whiteStone), has(whiteStone))
                .unlockedBy(getHasName(pillar), has(pillar))
                .save(output);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, pillar, whiteStone, 1);
    }

    /**
     * The lagoon sandstone set (66g): lagoon sand smelts to glass and packs into sandstone like vanilla sand, and the
     * set has exactly vanilla sandstone's recipes.
     */
    private static void lagoonSandstone(RecipeOutput output) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.LAGOON_SAND), RecipeCategory.BUILDING_BLOCKS,
                        Items.GLASS, 0.1F, 200)
                .unlockedBy(getHasName(ModBlocks.LAGOON_SAND), has(ModBlocks.LAGOON_SAND))
                .save(output, CorruptedAdventure.id("glass_from_smelting_lagoon_sand"));
        sandstone(output, ModBlocks.LAGOON_SAND.get(), ModBlocks.LAGOON_SANDSTONE.get(), ModBlocks.LAGOON_SANDSTONE_STAIRS.get(),
                ModBlocks.LAGOON_SANDSTONE_SLAB.get(), ModBlocks.LAGOON_SANDSTONE_WALL.get(), ModBlocks.CUT_LAGOON_SANDSTONE.get(),
                ModBlocks.CUT_LAGOON_SANDSTONE_SLAB.get(), ModBlocks.CHISELED_LAGOON_SANDSTONE.get(),
                ModBlocks.SMOOTH_LAGOON_SANDSTONE.get(), ModBlocks.SMOOTH_LAGOON_SANDSTONE_STAIRS.get(),
                ModBlocks.SMOOTH_LAGOON_SANDSTONE_SLAB.get());
    }

    /**
     * Exactly vanilla sandstone's recipes, with the same shapes, counts, categories and unlock conditions; without a
     * sand ({@code null}: scorched sandstone, whose ring has no sand of its own) there is no packing recipe.
     */
    private static void sandstone(RecipeOutput output, @Nullable Block sand, Block sandstone, Block stairs, Block slab,
                                  Block wall, Block cut, Block cutSlab, Block chiseled, Block smooth, Block smoothStairs,
                                  Block smoothSlab) {
        if (sand != null) {
            twoByTwoPacker(output, RecipeCategory.BUILDING_BLOCKS, sandstone, sand);
        }
        slabBuilder(RecipeCategory.BUILDING_BLOCKS, slab, Ingredient.of(sandstone, chiseled))
                .unlockedBy(getHasName(sandstone), has(sandstone))
                .unlockedBy(getHasName(chiseled), has(chiseled))
                .save(output);
        stairBuilder(stairs, Ingredient.of(sandstone, chiseled, cut))
                .unlockedBy(getHasName(sandstone), has(sandstone))
                .unlockedBy(getHasName(chiseled), has(chiseled))
                .unlockedBy(getHasName(cut), has(cut))
                .save(output);
        wall(output, RecipeCategory.DECORATIONS, wall, sandstone);
        cut(output, RecipeCategory.BUILDING_BLOCKS, cut, sandstone);
        slab(output, RecipeCategory.BUILDING_BLOCKS, cutSlab, cut);
        chiseled(output, RecipeCategory.BUILDING_BLOCKS, chiseled, slab);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(sandstone), RecipeCategory.BUILDING_BLOCKS, smooth, 0.1F, 200)
                .unlockedBy(getHasName(sandstone), has(sandstone))
                .save(output);
        slab(output, RecipeCategory.BUILDING_BLOCKS, smoothSlab, smooth);
        stairBuilder(smoothStairs, Ingredient.of(smooth))
                .unlockedBy(getHasName(smooth), has(smooth))
                .save(output);

        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, cut, sandstone, 1);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, slab, sandstone, 2);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, cutSlab, sandstone, 2);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, cutSlab, cut, 2);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, stairs, sandstone, 1);
        stonecutting(output, RecipeCategory.DECORATIONS, wall, sandstone, 1);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, chiseled, sandstone, 1);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, smoothSlab, smooth, 2);
        stonecutting(output, RecipeCategory.BUILDING_BLOCKS, smoothStairs, smooth, 1);
    }

    /**
     * A stone ladder like tuff's: every tier has its shapes at the crafting table (vanilla's family generator), four of
     * a tier make four of the next, and a tier at the stonecutter gives every later tier and every shape of itself and
     * the later tiers.
     */
    private static void ladder(RecipeOutput output, Shapes... tiers) {
        for (int i = 0; i < tiers.length; i++) {
            Shapes tier = tiers[i];
            BlockFamily.Builder family = new BlockFamily.Builder(tier.block());
            if (tier.stairs() != null) {
                family.stairs(tier.stairs());
            }
            if (tier.slab() != null) {
                family.slab(tier.slab());
            }
            if (tier.wall() != null) {
                family.wall(tier.wall());
            }
            if (i + 1 < tiers.length) {
                family.polished(tiers[i + 1].block());
            }
            generateRecipes(output, family.getFamily(), VANILLA);
            for (int j = i; j < tiers.length; j++) {
                if (j > i) {
                    stonecutting(output, RecipeCategory.BUILDING_BLOCKS, tiers[j].block(), tier.block(), 1);
                }
                cutShapes(output, tiers[j], tier.block());
            }
        }
    }

    /** The stonecutter recipes from {@code material} to the shapes of {@code target}: stairs 1, slab 2, wall 1. */
    private static void cutShapes(RecipeOutput output, Shapes target, ItemLike material) {
        if (target.stairs() != null) {
            stonecutting(output, RecipeCategory.BUILDING_BLOCKS, target.stairs(), material, 1);
        }
        if (target.slab() != null) {
            stonecutting(output, RecipeCategory.BUILDING_BLOCKS, target.slab(), material, 2);
        }
        if (target.wall() != null) {
            stonecutting(output, RecipeCategory.DECORATIONS, target.wall(), material, 1);
        }
    }

    private static Shapes shapes(Block block, Block stairs, Block slab, Block wall) {
        return new Shapes(block, stairs, slab, wall);
    }

    /** A full block with its (optional) stairs, slab and wall. */
    private record Shapes(Block block, @Nullable Block stairs, @Nullable Block slab, @Nullable Block wall) {
    }
}
