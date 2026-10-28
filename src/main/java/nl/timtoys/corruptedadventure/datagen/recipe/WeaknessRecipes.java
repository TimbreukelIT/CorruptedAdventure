package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * The weakness items (phase 4, stream I), each from its ring's tier material. Provisional (to confirm at the test stop):
 * the sunstone flash is a mirror of three sunstone ingots round a glass pane on a turquoise grip; the volcanite horn is
 * a goat horn (any) bound with two volcanite ingots, a fire ruby as its mouthpiece.
 */
public final class WeaknessRecipes extends RecipeSet {
    private WeaknessRecipes() {
    }

    public static void build(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SUNSTONE_FLASH.get())
                .define('S', ModItems.SUNSTONE_INGOT.get())
                .define('G', Items.GLASS_PANE)
                .define('T', ModItems.TURQUOISE.get())
                .pattern(" S ")
                .pattern("SGS")
                .pattern(" T ")
                .unlockedBy(getHasName(ModItems.SUNSTONE_INGOT.get()), has(ModItems.SUNSTONE_INGOT.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.VOLCANITE_HORN.get())
                .define('V', ModItems.VOLCANITE_INGOT.get())
                .define('H', Items.GOAT_HORN)
                .define('R', ModItems.FIRE_RUBY.get())
                .pattern("VHV")
                .pattern(" R ")
                .unlockedBy(getHasName(Items.GOAT_HORN), has(Items.GOAT_HORN))
                .save(output);
    }
}
