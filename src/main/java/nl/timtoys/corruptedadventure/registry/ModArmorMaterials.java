package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.item.GearTier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One armour material per {@link GearTier} (stats there), like vanilla netherite: its equip sound and one layer
 * {@code corruptedadventure:<tier>} (the layer textures come with the armour pieces in waves 2–5). No armour items
 * yet. The durability factor is not part of an {@link ArmorMaterial}; the armour items take it from the tier.
 */
public final class ModArmorMaterials {
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, CorruptedAdventure.MOD_ID);

    private static final Map<GearTier, DeferredHolder<ArmorMaterial, ArmorMaterial>> BY_TIER = new EnumMap<>(GearTier.class);

    static {
        for (GearTier tier : GearTier.values()) {
            BY_TIER.put(tier, ARMOR_MATERIALS.register(tier.getSerializedName(), () -> new ArmorMaterial(
                    tier.defense(), tier.getEnchantmentValue(), SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(tier.repairTag()),
                    List.of(new ArmorMaterial.Layer(CorruptedAdventure.id(tier.getSerializedName()))),
                    tier.toughness(), tier.knockbackResistance())));
        }
    }

    private ModArmorMaterials() {
    }

    public static DeferredHolder<ArmorMaterial, ArmorMaterial> of(GearTier tier) {
        return BY_TIER.get(tier);
    }

    public static void register(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
    }
}
