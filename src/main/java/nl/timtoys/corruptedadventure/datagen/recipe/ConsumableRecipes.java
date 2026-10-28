package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * The consumables against the conditions (fase-2-items.md §5.3–§5.4, §6.3, §6.6, §7.3). The shapes are provisional (to
 * confirm at the test stop): the canteen is a leather pouch around a glass bottle, hung on a string (vanilla has no rope);
 * the crafted canteen is empty. One primordial ice fills four bottles, like a honey block. The hand warmer is a leather
 * pouch around a fire crystal, crafted empty like the canteen. The echo shell is fossil stone around a white core, in a
 * plus.
 */
public final class ConsumableRecipes extends RecipeSet {
    private ConsumableRecipes() {
    }

    public static void build(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ModItems.CANTEEN.get())
                .define('S', Items.STRING)
                .define('L', Items.LEATHER)
                .define('B', Items.GLASS_BOTTLE)
                .pattern(" S ")
                .pattern("LBL")
                .pattern(" L ")
                .unlockedBy(getHasName(Items.LEATHER), has(Items.LEATHER))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.CACTUS_JUICE.get())
                .requires(ModBlocks.BARREL_CACTUS)
                .requires(Items.GLASS_BOTTLE)
                .unlockedBy(getHasName(ModBlocks.BARREL_CACTUS), has(ModBlocks.BARREL_CACTUS))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.SALTED_JERKY.get(), 2)
                .requires(ModItems.SALT.get())
                .requires(ModTags.Items.SALTED_JERKY_MEATS)
                .unlockedBy(getHasName(ModItems.SALT.get()), has(ModItems.SALT.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.DEW_DRAUGHT.get())
                .requires(ModItems.DEW_CRYSTAL.get())
                .requires(waterBottle())
                .unlockedBy(getHasName(ModItems.DEW_CRYSTAL.get()), has(ModItems.DEW_CRYSTAL.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.PURIFIED_WATER.get(), 4)
                .requires(ModBlocks.PRIMORDIAL_ICE)
                .requires(Items.GLASS_BOTTLE, 4)
                .unlockedBy(getHasName(ModBlocks.PRIMORDIAL_ICE), has(ModBlocks.PRIMORDIAL_ICE))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ModItems.HAND_WARMER.get())
                .define('L', Items.LEATHER)
                .define('F', ModItems.FIRE_CRYSTAL.get())
                .pattern("LFL")
                .pattern(" L ")
                .unlockedBy(getHasName(ModItems.FIRE_CRYSTAL.get()), has(ModItems.FIRE_CRYSTAL.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.SKY_LIGHT_DRAUGHT.get())
                .requires(ModItems.PURITY_CRYSTAL_SHARD.get())
                .requires(Items.GLOWSTONE_DUST)
                .requires(waterBottle())
                .unlockedBy(getHasName(ModItems.PURITY_CRYSTAL_SHARD.get()), has(ModItems.PURITY_CRYSTAL_SHARD.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.ECHO_SHELL.get())
                .define('F', ModBlocks.FOSSIL_STONE)
                .define('W', ModItems.WHITE_CORE.get())
                .pattern(" F ")
                .pattern("FWF")
                .pattern(" F ")
                .unlockedBy(getHasName(ModItems.WHITE_CORE.get()), has(ModItems.WHITE_CORE.get()))
                .save(output);
    }

    /** A water bottle only (a potion item with water in it), not any potion. */
    private static Ingredient waterBottle() {
        return DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER),
                Items.POTION);
    }
}
