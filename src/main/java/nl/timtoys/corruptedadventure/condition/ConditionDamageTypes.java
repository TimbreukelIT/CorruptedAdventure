package nl.timtoys.corruptedadventure.condition;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;
import nl.timtoys.corruptedadventure.registry.ModDamageTypes;

/**
 * Datagen bootstrap of the damage types of the conditions; later conditions add theirs here. Death message key:
 * {@code death.attack.<msgId>} (+ {@code .player} with a recent attacker).
 */
public final class ConditionDamageTypes {
    private ConditionDamageTypes() {
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        // As vanilla's starve: no exhaustion, default scaling, effects and death message.
        context.register(ModDamageTypes.DEHYDRATION, new DamageType("corruptedadventure.dehydration", 0.0f));
    }
}
