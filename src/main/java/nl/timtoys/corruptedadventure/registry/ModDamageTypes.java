package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/** Keys of the mod's damage types. The types themselves are datapack entries ({@code ConditionDamageTypes}, datagen). */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> DEHYDRATION = key("dehydration");

    private ModDamageTypes() {
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, CorruptedAdventure.id(path));
    }
}
