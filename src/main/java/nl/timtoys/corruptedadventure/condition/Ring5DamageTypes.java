package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Keys and datagen bootstrap of the damage types of the ring 5 conditions (void damage, the silence's heartbeat), like
 * {@link Ring4DamageTypes}. Owned by stream C ({@code docs/plans/ring-5/waves.md}). Death message key:
 * {@code death.attack.<msgId>} (+ {@code .player} with a recent attacker).
 */
public final class Ring5DamageTypes {
    /**
     * Being unmade at the top of the void meter (the third void threshold, a {@link DamageThreshold}); tagged like the
     * poison's wither.
     */
    public static final ResourceKey<DamageType> VOID =
            ResourceKey.create(Registries.DAMAGE_TYPE, CorruptedAdventure.id("void"));

    private Ring5DamageTypes() {
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        // As vanilla's wither: no exhaustion, default scaling and effects.
        context.register(VOID, new DamageType("corruptedadventure.void", 0.0f));
    }
}
