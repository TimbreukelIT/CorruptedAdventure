package nl.timtoys.corruptedadventure.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.timtoys.corruptedadventure.registry.ModCreativeTabs;
import nl.timtoys.corruptedadventure.registry.ModDataComponents;
import nl.timtoys.corruptedadventure.registry.ModItems;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandWarmerItemTest {
    @Test
    void aCraftedHandWarmerIsEmptyAndStacksToOne() {
        ItemStack stack = new ItemStack(ModItems.HAND_WARMER.get());
        assertEquals(0, HandWarmerItem.charges(stack));
        assertEquals(1, stack.getMaxStackSize());
        assertFalse(stack.isBarVisible(), "no bar when empty");
    }

    @Test
    void chargesAreReadFromTheComponentAndClamped() {
        ItemStack stack = new ItemStack(ModItems.HAND_WARMER.get());
        stack.set(ModDataComponents.CHARGES, 3);
        assertEquals(3, HandWarmerItem.charges(stack));
        assertTrue(stack.isBarVisible());
        stack.set(ModDataComponents.CHARGES, 9);
        assertEquals(HandWarmerItem.MAX_CHARGES, HandWarmerItem.charges(stack), "a value from a command is clamped");
    }

    @Test
    void aChargeTakesOneAndNeverGoesBelowZero() {
        assertEquals(3, HandWarmerItem.afterCharge(4));
        assertEquals(0, HandWarmerItem.afterCharge(1));
        assertEquals(0, HandWarmerItem.afterCharge(0));
        assertEquals(List.of(0, 3, 7, 10, 13), List.of(HandWarmerItem.barWidth(0), HandWarmerItem.barWidth(1),
                HandWarmerItem.barWidth(2), HandWarmerItem.barWidth(3), HandWarmerItem.barWidth(4)));
    }

    @Test
    void theConsumablesSitInTheTabOfTheirCondition() {
        assertEquals(16, new ItemStack(ModItems.DEW_DRAUGHT.get()).getMaxStackSize());
        assertEquals(16, new ItemStack(ModItems.PURIFIED_WATER.get()).getMaxStackSize());
        List<Item> ring3 = ModCreativeTabs.contents(3);
        assertTrue(ring3.contains(ModItems.DEW_DRAUGHT.get()), "heat: ring 3");
        assertTrue(ring3.contains(ModItems.HAND_WARMER.get()), "cold: Nether ring 3");
        assertTrue(ModCreativeTabs.contents(4).contains(ModItems.PURIFIED_WATER.get()), "poison: ring 4");
    }
}
