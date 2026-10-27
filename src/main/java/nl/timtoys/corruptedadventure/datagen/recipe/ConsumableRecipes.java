package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * The consumables against dehydration (fase-2-items.md §5.3–§5.4). The shapes are provisional (to confirm at the test
 * stop): the canteen is a leather pouch around a glass bottle, hung on a string (vanilla has no rope); the crafted
 * canteen is empty.
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
    }
}
