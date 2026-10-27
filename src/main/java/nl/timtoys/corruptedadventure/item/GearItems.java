package nl.timtoys.corruptedadventure.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import nl.timtoys.corruptedadventure.registry.ModArmorMaterials;

/**
 * The tools and armour of a {@link GearTier}, built like vanilla's netherite set: the same attack damage and speed per
 * kind (the tier's damage bonus on top; the hoe stays at 1), armour durability from the tier's durability factor. Extra properties (fire
 * resistance) come from the caller.
 */
public final class GearItems {
    private GearItems() {
    }

    public static SwordItem sword(GearTier tier, Item.Properties properties) {
        return new SwordItem(tier, properties.attributes(SwordItem.createAttributes(tier, 3, -2.4F)));
    }

    public static AxeItem axe(GearTier tier, Item.Properties properties) {
        return new AxeItem(tier, properties.attributes(AxeItem.createAttributes(tier, 5.0F, -3.0F)));
    }

    public static PickaxeItem pickaxe(GearTier tier, Item.Properties properties) {
        return new PickaxeItem(tier, properties.attributes(PickaxeItem.createAttributes(tier, 1.0F, -2.8F)));
    }

    public static ShovelItem shovel(GearTier tier, Item.Properties properties) {
        return new ShovelItem(tier, properties.attributes(ShovelItem.createAttributes(tier, 1.5F, -3.0F)));
    }

    /** Like every vanilla hoe, it hits for 1: the base damage cancels the tier's bonus (netherite's -4). */
    public static HoeItem hoe(GearTier tier, Item.Properties properties) {
        return new HoeItem(tier, properties.attributes(HoeItem.createAttributes(tier, -tier.getAttackDamageBonus(), 0.0F)));
    }

    public static ArmorItem armor(GearTier tier, ArmorItem.Type type, Item.Properties properties) {
        return new ArmorItem(ModArmorMaterials.of(tier), type,
                properties.durability(type.getDurability(tier.durabilityFactor())));
    }
}
