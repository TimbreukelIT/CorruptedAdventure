package nl.timtoys.corruptedadventure.condition;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

/**
 * Datagen bootstrap of the damage types of the ring 3 conditions (heat, later cold), like {@link ConditionDamageTypes}.
 * Owned by stream C ({@code docs/plans/ring-3/waves.md}); empty until the heat condition exists.
 */
public final class Ring3DamageTypes {
    private Ring3DamageTypes() {
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
    }
}
