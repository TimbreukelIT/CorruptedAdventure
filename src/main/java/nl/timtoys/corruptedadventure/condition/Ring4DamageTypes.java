package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Keys and datagen bootstrap of the damage types of the ring 4 conditions, like {@link Ring3DamageTypes}. Owned by
 * stream C ({@code docs/plans/ring-4/waves.md}). Death message key: {@code death.attack.<msgId>} (+ {@code .player}
 * with a recent attacker).
 */
public final class Ring4DamageTypes {
    /** Withering at the top of the poison meter ({@link PoisonWither}); tagged like vanilla's {@code wither}. */
    public static final ResourceKey<DamageType> POISON =
            ResourceKey.create(Registries.DAMAGE_TYPE, CorruptedAdventure.id("poison"));

    private Ring4DamageTypes() {
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        // As vanilla's wither: no exhaustion, default scaling and effects.
        context.register(POISON, new DamageType("corruptedadventure.poison", 0.0f));
    }
}
