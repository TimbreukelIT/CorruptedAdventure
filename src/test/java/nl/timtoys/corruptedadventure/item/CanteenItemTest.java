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

class CanteenItemTest {
    @Test
    void aCraftedCanteenIsEmptyAndStacksToOne() {
        ItemStack stack = new ItemStack(ModItems.CANTEEN.get());
        assertEquals(0, CanteenItem.sips(stack));
        assertEquals(1, stack.getMaxStackSize());
        assertFalse(stack.isBarVisible(), "no bar when empty");
    }

    @Test
    void sipsAreReadFromTheComponentAndClamped() {
        ItemStack stack = new ItemStack(ModItems.CANTEEN.get());
        stack.set(ModDataComponents.SIPS, 3);
        assertEquals(3, CanteenItem.sips(stack));
        assertTrue(stack.isBarVisible());
        stack.set(ModDataComponents.SIPS, 9);
        assertEquals(CanteenItem.MAX_SIPS, CanteenItem.sips(stack), "a value from a command is clamped");
    }

    @Test
    void aSipTakesOneAndNeverGoesBelowZero() {
        assertEquals(3, CanteenItem.afterSip(4));
        assertEquals(0, CanteenItem.afterSip(1));
        assertEquals(0, CanteenItem.afterSip(0));
    }

    @Test
    void theBarFillsInFourSteps() {
        assertEquals(List.of(0, 3, 7, 10, 13), List.of(CanteenItem.barWidth(0), CanteenItem.barWidth(1),
                CanteenItem.barWidth(2), CanteenItem.barWidth(3), CanteenItem.barWidth(4)));
    }

    @Test
    void theConsumablesStackAndSitInTheRing2Tab() {
        assertEquals(16, new ItemStack(ModItems.CACTUS_JUICE.get()).getMaxStackSize());
        assertEquals(64, new ItemStack(ModItems.SALTED_JERKY.get()).getMaxStackSize());
        List<Item> ring2 = ModCreativeTabs.contents(2);
        assertTrue(ring2.contains(ModItems.CANTEEN.get()));
        assertTrue(ring2.contains(ModItems.CACTUS_JUICE.get()));
        assertTrue(ring2.contains(ModItems.SALTED_JERKY.get()));
    }

    @Test
    void jerkyIsFastFood() {
        var food = new ItemStack(ModItems.SALTED_JERKY.get()).getFoodProperties(null);
        assertEquals(6, food.nutrition());
        assertEquals(6 * 1.2F * 2.0F, food.saturation(), 1.0E-4F);
        assertEquals(16, food.eatDurationTicks(), "as fast as dried kelp");
    }
}
