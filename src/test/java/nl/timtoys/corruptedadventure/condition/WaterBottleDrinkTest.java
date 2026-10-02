package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterBottleDrinkTest {
    @Test
    void onlyTheDrinkableWaterBottleCounts() {
        assertTrue(WaterBottleDrink.isWaterBottle(PotionContents.createItemStack(Items.POTION, Potions.WATER)));

        assertFalse(WaterBottleDrink.isWaterBottle(PotionContents.createItemStack(Items.POTION, Potions.AWKWARD)));
        assertFalse(WaterBottleDrink.isWaterBottle(PotionContents.createItemStack(Items.POTION, Potions.MUNDANE)));
        assertFalse(WaterBottleDrink.isWaterBottle(PotionContents.createItemStack(Items.POTION, Potions.THICK)));
        assertFalse(WaterBottleDrink.isWaterBottle(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS)));
        assertFalse(WaterBottleDrink.isWaterBottle(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER)));
        assertFalse(WaterBottleDrink.isWaterBottle(PotionContents.createItemStack(Items.LINGERING_POTION, Potions.WATER)));
        assertFalse(WaterBottleDrink.isWaterBottle(new ItemStack(Items.POTION)), "a potion without contents");
        assertFalse(WaterBottleDrink.isWaterBottle(new ItemStack(Items.GLASS_BOTTLE)));
        assertFalse(WaterBottleDrink.isWaterBottle(new ItemStack(Items.MILK_BUCKET)));
        assertFalse(WaterBottleDrink.isWaterBottle(ItemStack.EMPTY));
    }
}
