package nl.timtoys.corruptedadventure.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.timtoys.corruptedadventure.registry.GearSet;
import nl.timtoys.corruptedadventure.registry.ModItems;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every gear set (fase-2-items.md §6.1): tools hit like their netherite counterpart plus the tier's step in damage
 * bonus (the hoe stays at netherite's, like every vanilla hoe), armour carries the tier's stats, every piece is fire
 * resistant and lasts as long as the tier says.
 */
class GearSetsTest {
    /** Vanilla's netherite tools, in the order of {@link GearSet#tools()}. */
    private static final List<Item> NETHERITE_TOOLS = List.of(Items.NETHERITE_SWORD, Items.NETHERITE_AXE,
            Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE);
    private static final double EPSILON = 1.0E-6;

    @Test
    void theSetsAreSunstoneVolcaniteAndTidesilver() {
        assertEquals(List.of(GearTier.SUNSTONE, GearTier.VOLCANITE, GearTier.TIDESILVER),
                ModItems.GEAR_SETS.stream().map(GearSet::tier).toList());
        for (GearSet set : ModItems.GEAR_SETS) {
            for (int i = 0; i < GearSet.PIECES.size(); i++) {
                assertEquals(set.tier().getSerializedName() + "_" + GearSet.PIECES.get(i),
                        set.pieces().get(i).getId().getPath());
            }
        }
    }

    @Test
    void toolsHitLikeNetheritePlusTheTierStep() {
        for (GearSet set : ModItems.GEAR_SETS) {
            double step = set.tier().getAttackDamageBonus() - Tiers.NETHERITE.getAttackDamageBonus();
            for (int i = 0; i < NETHERITE_TOOLS.size(); i++) {
                Item ours = set.tools().get(i).get();
                Item netherite = NETHERITE_TOOLS.get(i);
                double extra = ours == set.hoe().get() ? 0 : step;
                assertEquals(amount(netherite, Attributes.ATTACK_DAMAGE) + extra, amount(ours, Attributes.ATTACK_DAMAGE),
                        EPSILON, ours.toString());
                assertEquals(amount(netherite, Attributes.ATTACK_SPEED), amount(ours, Attributes.ATTACK_SPEED), EPSILON,
                        ours.toString());
            }
        }
    }

    @Test
    void armourHasTheTierStats() {
        for (GearSet set : ModItems.GEAR_SETS) {
            for (DeferredItem<ArmorItem> piece : set.armor()) {
                ArmorItem armor = piece.get();
                assertEquals(set.tier().defense().get(armor.getType()), amount(armor, Attributes.ARMOR), EPSILON,
                        armor.toString());
                assertEquals(set.tier().toughness(), amount(armor, Attributes.ARMOR_TOUGHNESS), EPSILON, armor.toString());
                assertEquals(set.tier().knockbackResistance(), amount(armor, Attributes.KNOCKBACK_RESISTANCE), EPSILON,
                        armor.toString());
            }
        }
    }

    @Test
    void everyPieceIsFireResistantAndLastsAsTheTierSays() {
        for (GearSet set : ModItems.GEAR_SETS) {
            for (DeferredItem<? extends Item> piece : set.pieces()) {
                ItemStack stack = new ItemStack(piece.get());
                assertTrue(stack.has(DataComponents.FIRE_RESISTANT), piece.getId().toString());
                int expected = piece.get() instanceof ArmorItem armor
                        ? armor.getType().getDurability(set.tier().durabilityFactor()) : set.tier().getUses();
                assertEquals(expected, stack.getMaxDamage(), piece.getId().toString());
            }
        }
    }

    private static double amount(Item item, Holder<Attribute> attribute) {
        return new ItemStack(item).getAttributeModifiers().modifiers().stream()
                .filter(entry -> entry.attribute().equals(attribute))
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
    }
}
