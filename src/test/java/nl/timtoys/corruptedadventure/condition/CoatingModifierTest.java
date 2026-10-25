package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.item.coating.Coating;
import nl.timtoys.corruptedadventure.item.coating.CoatingTrims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The coating modifier (fase-2-items.md §4): linear per coated piece, only on a rising meter of its own condition. */
class CoatingModifierTest {
    private static final float REDUCTION = 0.1875F;

    @Test
    void theDefaultIsTheDesignValue() {
        assertEquals(REDUCTION, ServerConfig.COATING.reductionPerPiece.getDefault().floatValue(), 1.0E-6F);
    }

    @Test
    void linearPerPieceAsInTheDesign() {
        // fase-2-items.md §4: 1 piece 0.81x, 2 pieces 0.63x, 3 pieces 0.44x, 4 pieces 0.25x.
        assertEquals(1.0F, CoatingModifier.factor(0, REDUCTION));
        assertEquals(0.81F, round2(CoatingModifier.factor(1, REDUCTION)));
        assertEquals(0.63F, round2(CoatingModifier.factor(2, REDUCTION)));
        assertEquals(0.44F, round2(CoatingModifier.factor(3, REDUCTION)));
        assertEquals(0.25F, CoatingModifier.factor(4, REDUCTION), 1.0E-6F);
        // A full set rises ~4x slower.
        assertEquals(4.0F, 1.0F / CoatingModifier.factor(4, REDUCTION), 1.0E-5F);
    }

    @Test
    void neverBelowZero() {
        assertEquals(0.0F, CoatingModifier.factor(4, 0.3F));
    }

    @Test
    void aFallingOrStillMeterAndTheDebugConditionAreLeftAlone() {
        // Returns before the player (null here) is looked at.
        CoatingModifier modifier = new CoatingModifier();
        assertEquals(-1.0F, modifier.modify(Condition.HEAT, null, -1.0F));
        assertEquals(0.0F, modifier.modify(Condition.VOID, null, 0.0F));
        assertEquals(0.5F, modifier.modify(Condition.DEBUG, null, 0.5F));
    }

    @Test
    void onlyPiecesWithTheConditionsOwnCoatingCount() {
        List<ItemStack> armor = List.of(
                CoatingTrims.coated(Items.IRON_HELMET, Coating.HEAT),
                CoatingTrims.coated(Items.IRON_CHESTPLATE, Coating.HEAT),
                CoatingTrims.coated(Items.IRON_LEGGINGS, Coating.COLD),
                new ItemStack(Items.IRON_BOOTS));
        assertEquals(2, CoatingModifier.coatedPieces(armor, Coating.HEAT.pattern()));
        assertEquals(1, CoatingModifier.coatedPieces(armor, Coating.COLD.pattern()));
        assertEquals(0, CoatingModifier.coatedPieces(armor, Coating.POISON.pattern()));
        // Void and silence share their material, not their pattern.
        List<ItemStack> white = List.of(CoatingTrims.coated(Items.IRON_HELMET, Coating.VOID), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY);
        assertEquals(1, CoatingModifier.coatedPieces(white, Coating.VOID.pattern()));
        assertEquals(0, CoatingModifier.coatedPieces(white, Coating.SILENCE.pattern()));
    }

    private static float round2(float value) {
        return Math.round(value * 100.0F) / 100.0F;
    }
}
