package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.RateEffectMobEffect;

/** Status effects. */
public final class ModMobEffects {
    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, CorruptedAdventure.MOD_ID);

    /** Canteen and cactus juice (phase 2 golf 2): dehydration rises slower while it lasts. Icon {@code mob_effect/cooled}. */
    public static final DeferredHolder<MobEffect, RateEffectMobEffect> COOLED =
            MOB_EFFECTS.register("cooled", () -> new RateEffectMobEffect(0x7FD4FF));

    private ModMobEffects() {
    }

    public static void register(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
    }
}
