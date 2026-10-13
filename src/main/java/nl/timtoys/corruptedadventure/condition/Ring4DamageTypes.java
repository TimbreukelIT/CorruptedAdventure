package nl.timtoys.corruptedadventure.condition;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

/**
 * Datagen bootstrap of the damage types of the ring 4 conditions (poison, if it gets its own damage type), like
 * {@link Ring3DamageTypes}. Owned by stream C ({@code docs/plans/ring-4/waves.md}); empty until the poison condition needs one.
 */
public final class Ring4DamageTypes {
    private Ring4DamageTypes() {
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
    }
}
