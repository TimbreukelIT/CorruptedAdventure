package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageType;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Keys and datagen bootstrap of the damage types of the ring 3 conditions (heat, later cold), like
 * {@link ConditionDamageTypes}. Owned by stream C ({@code docs/plans/ring-3/waves.md}). Death message key:
 * {@code death.attack.<msgId>} (+ {@code .player} with a recent attacker).
 */
public final class Ring3DamageTypes {
    /** Burning at the top of the heat meter ({@link HeatBurn}); tagged like vanilla's {@code on_fire}. */
    public static final ResourceKey<DamageType> HEAT =
            ResourceKey.create(Registries.DAMAGE_TYPE, CorruptedAdventure.id("heat"));

    private Ring3DamageTypes() {
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        // As vanilla's on_fire: no exhaustion, default scaling, burning sound.
        context.register(HEAT, new DamageType("corruptedadventure.heat", 0.0f, DamageEffects.BURNING));
    }
}
