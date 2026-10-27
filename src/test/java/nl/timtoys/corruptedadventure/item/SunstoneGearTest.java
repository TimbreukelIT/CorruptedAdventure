package nl.timtoys.corruptedadventure.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.timtoys.corruptedadventure.registry.ModItems;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sunstone set (fase-2-items.md §5.1): netherite that lasts longer. Same attributes per piece as netherite (attack
 * damage and speed, armour, toughness, knockback resistance), fire resistant like netherite, more durability.
 */
class SunstoneGearTest {
    private static Map<Item, Item> netheriteCounterparts() {
        Map<Item, Item> pieces = new LinkedHashMap<>();
        pieces.put(ModItems.SUNSTONE_SWORD.get(), Items.NETHERITE_SWORD);
        pieces.put(ModItems.SUNSTONE_AXE.get(), Items.NETHERITE_AXE);
        pieces.put(ModItems.SUNSTONE_PICKAXE.get(), Items.NETHERITE_PICKAXE);
        pieces.put(ModItems.SUNSTONE_SHOVEL.get(), Items.NETHERITE_SHOVEL);
        pieces.put(ModItems.SUNSTONE_HOE.get(), Items.NETHERITE_HOE);
        pieces.put(ModItems.SUNSTONE_HELMET.get(), Items.NETHERITE_HELMET);
        pieces.put(ModItems.SUNSTONE_CHESTPLATE.get(), Items.NETHERITE_CHESTPLATE);
        pieces.put(ModItems.SUNSTONE_LEGGINGS.get(), Items.NETHERITE_LEGGINGS);
        pieces.put(ModItems.SUNSTONE_BOOTS.get(), Items.NETHERITE_BOOTS);
        return pieces;
    }

    @Test
    void everyPieceHasTheAttributesOfItsNetheriteCounterpart() {
        netheriteCounterparts().forEach((ours, netherite) ->
                assertEquals(amounts(netherite), amounts(ours), ours.toString()));
    }

    @Test
    void everyPieceIsFireResistantLikeNetherite() {
        netheriteCounterparts().keySet().forEach(item ->
                assertTrue(new ItemStack(item).has(DataComponents.FIRE_RESISTANT), item.toString()));
    }

    @Test
    void sunstoneLastsLonger() {
        netheriteCounterparts().forEach((ours, netherite) -> {
            int expected = ours instanceof ArmorItem armor ? armor.getType().getDurability(40) : 2200;
            assertEquals(expected, new ItemStack(ours).getMaxDamage(), ours.toString());
            assertTrue(new ItemStack(ours).getMaxDamage() > new ItemStack(netherite).getMaxDamage(), ours.toString());
        });
    }

    private static Map<Holder<Attribute>, Double> amounts(Item item) {
        Map<Holder<Attribute>, Double> amounts = new HashMap<>();
        new ItemStack(item).getAttributeModifiers().modifiers()
                .forEach(entry -> amounts.merge(entry.attribute(), entry.modifier().amount(), Double::sum));
        return amounts;
    }
}
