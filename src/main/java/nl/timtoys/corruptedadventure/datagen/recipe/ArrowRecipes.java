package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * The fire, poison and glitch arrows (fase-2-items.md §6.4, §6.7, §7.4): one material and 4 arrows make 4. The shape is provisional
 * (to confirm at the test stop): the material in the middle, the arrows on its four sides.
 */
public final class ArrowRecipes extends RecipeSet {
    private ArrowRecipes() {
    }

    public static void build(RecipeOutput output) {
        arrows(output, ModItems.FIRE_ARROW.get(), ModItems.SULFUR.get());
        arrows(output, ModItems.POISON_ARROW.get(), ModItems.TOXIC_CRYSTAL_SHARD.get());
        arrows(output, ModItems.GLITCH_ARROW.get(), ModItems.GLITCH_SHARD.get());
    }

    private static void arrows(RecipeOutput output, Item result, Item material) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result, 4)
                .define('A', Items.ARROW)
                .define('M', material)
                .pattern(" A ")
                .pattern("AMA")
                .pattern(" A ")
                .unlockedBy(getHasName(material), has(material))
                .save(output);
    }
}
