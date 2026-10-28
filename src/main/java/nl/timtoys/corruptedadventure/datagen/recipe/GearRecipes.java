package nl.timtoys.corruptedadventure.datagen.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.item.GearTier;
import nl.timtoys.corruptedadventure.registry.GearSet;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.List;

/**
 * The gear tier recipes (fase-2-items.md §5.1, §6.2, §6.5, §7.1). Every tier upgrades the gear of the one before with a vanilla
 * {@code smithing_transform} per piece, like the netherite upgrade (piece + template + ingot; enchantments and
 * trim/coating carry over): sunstone takes diamond gear. Each template is crafted from a material of the Nether ring
 * before its ring (the loop overworld → Nether → ring), the ingot of the tier before (diamond for sunstone) and a stone
 * of its own ring; there is no copy recipe for a template: crafting it already is the copy.
 */
public final class GearRecipes extends RecipeSet {
    /** Vanilla's diamond set, in the order of {@link GearSet#PIECES}. */
    private static final List<Item> DIAMOND = List.of(Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE,
            Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);

    private GearRecipes() {
    }

    public static void build(RecipeOutput output) {
        List<Item> previous = DIAMOND;
        for (GearSet set : ModItems.GEAR_SETS) {
            Item template = template(set.tier());
            Item ingot = ingot(set.tier());
            List<Item> pieces = set.pieces().stream().<Item>map(DeferredItem::get).toList();
            for (int i = 0; i < pieces.size(); i++) {
                upgrade(output, template, ingot, previous.get(i), category(i), pieces.get(i));
            }
            previous = pieces;
        }

        // Provisional shapes (confirm at the test stop): six of the Nether material around a stone heart, the two
        // ingots of the tier before top and bottom.
        template(output, ModItems.SUNSTONE_UPGRADE_SMITHING_TEMPLATE.get(), ModItems.SOUL_CRYSTAL.get(), Items.DIAMOND,
                ModBlocks.SCORCHED_SANDSTONE);
        template(output, ModItems.VOLCANITE_UPGRADE_SMITHING_TEMPLATE.get(), ModItems.DEW_CRYSTAL.get(),
                ModItems.SUNSTONE_INGOT.get(), ModBlocks.PUMICE);
        template(output, ModItems.TIDESILVER_UPGRADE_SMITHING_TEMPLATE.get(), ModBlocks.PRIMORDIAL_ICE,
                ModItems.VOLCANITE_INGOT.get(), ModBlocks.TIDESTONE);
    }

    private static Item template(GearTier tier) {
        return switch (tier) {
            case SUNSTONE -> ModItems.SUNSTONE_UPGRADE_SMITHING_TEMPLATE.get();
            case VOLCANITE -> ModItems.VOLCANITE_UPGRADE_SMITHING_TEMPLATE.get();
            case TIDESILVER -> ModItems.TIDESILVER_UPGRADE_SMITHING_TEMPLATE.get();
            case NULLITE -> ModItems.NULLITE_UPGRADE_SMITHING_TEMPLATE.get();
        };
    }

    private static Item ingot(GearTier tier) {
        return switch (tier) {
            case SUNSTONE -> ModItems.SUNSTONE_INGOT.get();
            case VOLCANITE -> ModItems.VOLCANITE_INGOT.get();
            case TIDESILVER -> ModItems.TIDESILVER_INGOT.get();
            case NULLITE -> ModItems.NULLITE_INGOT.get();
        };
    }

    /** The categories of vanilla's netherite upgrades: sword and armour combat, the other tools tools. */
    private static RecipeCategory category(int piece) {
        return piece == 0 || piece >= 5 ? RecipeCategory.COMBAT : RecipeCategory.TOOLS;
    }

    /** Vanilla's {@code netheriteSmithing} with our template and ingot, and the id in our namespace. */
    private static void upgrade(RecipeOutput output, Item template, Item ingot, Item base, RecipeCategory category,
                                Item result) {
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(template), Ingredient.of(base), Ingredient.of(ingot),
                        category, result)
                .unlocks(getHasName(ingot), has(ingot))
                .save(output, CorruptedAdventure.id(getItemName(result) + "_smithing"));
    }

    /** {@code SIS / SCS / SIS}: 6 {@code shell}, 2 {@code ingot}, 1 {@code core} → 1 template. */
    private static void template(RecipeOutput output, Item template, ItemLike shell, ItemLike ingot, ItemLike core) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, template)
                .define('S', shell)
                .define('I', ingot)
                .define('C', core)
                .pattern("SIS")
                .pattern("SCS")
                .pattern("SIS")
                .unlockedBy(getHasName(shell), has(shell))
                .save(output, CorruptedAdventure.id(getItemName(template)));
    }
}
