package nl.timtoys.corruptedadventure.item.coating;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import nl.timtoys.corruptedadventure.condition.Condition;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The coatings are vanilla armor trims (fase-2-items.md §4): vanilla's smithing recipes put them on, replace them and keep
 * them through a netherite upgrade. Runs vanilla's recipe classes on our trim registries (the same bootstraps as runData).
 */
class CoatingSmithingTest {
    private static final Ingredient ARMOR = Ingredient.of(Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE);

    private static ItemStack trim(ItemStack armor, Coating coating) {
        SmithingTrimRecipe recipe = new SmithingTrimRecipe(Ingredient.of(coating.template()), ARMOR,
                Ingredient.of(coating.material().item()));
        SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(coating.template()), armor,
                new ItemStack(coating.material().item()));
        assertTrue(recipe.matches(input, null), coating.name());
        return recipe.assemble(input, CoatingTrims.LOOKUP);
    }

    @Test
    void everyConditionButDebugHasItsOwnCoating() {
        Set<Condition> covered = EnumSet.noneOf(Condition.class);
        for (Coating coating : Coating.values()) {
            assertEquals(coating, Coating.of(coating.condition()).orElseThrow());
            covered.add(coating.condition());
        }
        assertEquals(EnumSet.complementOf(EnumSet.of(Condition.DEBUG)), covered);
        assertTrue(Coating.of(Condition.DEBUG).isEmpty());
        assertEquals("corruptedadventure:heat_coating", Coating.HEAT.pattern().location().toString());
    }

    @Test
    void theRecipeAppliesTheCoatingOfTheTemplateWithItsMaterial() {
        ItemStack coated = trim(new ItemStack(Items.DIAMOND_CHESTPLATE), Coating.HEAT);
        ArmorTrim trim = coated.get(DataComponents.TRIM);
        assertNotNull(trim);
        assertTrue(trim.pattern().is(Coating.HEAT.pattern()));
        assertTrue(trim.material().is(CoatingMaterial.DEW_CRYSTAL.key()));
    }

    @Test
    void aNewCoatingReplacesTheOld() {
        ItemStack heat = trim(new ItemStack(Items.DIAMOND_CHESTPLATE), Coating.HEAT);
        ItemStack poison = trim(heat, Coating.POISON);
        assertTrue(poison.get(DataComponents.TRIM).pattern().is(Coating.POISON.pattern()));
        // The same coating again gives nothing, like vanilla (no wasted material).
        assertTrue(trim(poison, Coating.POISON).isEmpty());
    }

    @Test
    void voidTakesSkyGlassAndSilenceTheWhiteCore() {
        // Golf 5 (fase-2-items.md §7.3): the void coating moved from the white core to sky glass.
        ItemStack voidCoated = trim(new ItemStack(Items.DIAMOND_CHESTPLATE), Coating.VOID);
        assertTrue(voidCoated.get(DataComponents.TRIM).material().is(CoatingMaterial.SKY_GLASS.key()));
        ItemStack silence = trim(voidCoated, Coating.SILENCE);
        assertTrue(silence.get(DataComponents.TRIM).pattern().is(Coating.SILENCE.pattern()));
        assertTrue(silence.get(DataComponents.TRIM).material().is(CoatingMaterial.WHITE_CORE.key()));
    }

    @Test
    void anUpgradeKeepsTheCoating() {
        // Vanilla's netherite upgrade stands for the tier upgrades of golf 2-5 (also smithing_transform).
        ItemStack coated = trim(new ItemStack(Items.DIAMOND_CHESTPLATE), Coating.COLD);
        SmithingTransformRecipe upgrade = new SmithingTransformRecipe(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(Items.DIAMOND_CHESTPLATE), Ingredient.of(Items.NETHERITE_INGOT),
                new ItemStack(Items.NETHERITE_CHESTPLATE));
        ItemStack upgraded = upgrade.assemble(new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                coated, new ItemStack(Items.NETHERITE_INGOT)), CoatingTrims.LOOKUP);
        assertEquals(Items.NETHERITE_CHESTPLATE, upgraded.getItem());
        assertTrue(upgraded.get(DataComponents.TRIM).pattern().is(Coating.COLD.pattern()));
    }
}
