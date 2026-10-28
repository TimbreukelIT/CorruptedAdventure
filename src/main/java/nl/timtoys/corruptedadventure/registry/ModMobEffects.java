package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.RateEffectMobEffect;
import nl.timtoys.corruptedadventure.effect.DazzledMobEffect;
import nl.timtoys.corruptedadventure.effect.StunnedMobEffect;

/** Status effects. */
public final class ModMobEffects {
    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, CorruptedAdventure.MOD_ID);

    /**
     * Canteen, cactus juice (phase 2 golf 2) and dew draught (golf 3): dehydration and heat rise slower while it lasts.
     * Icon {@code mob_effect/cooled}.
     */
    public static final DeferredHolder<MobEffect, RateEffectMobEffect> COOLED =
            MOB_EFFECTS.register("cooled", () -> new RateEffectMobEffect(0x7FD4FF));
    /** Purified water (golf 4): poison rises slower while it lasts. Icon {@code mob_effect/cleansed}. */
    public static final DeferredHolder<MobEffect, RateEffectMobEffect> CLEANSED =
            MOB_EFFECTS.register("cleansed", () -> new RateEffectMobEffect(0xB8F0D0));
    /** Hand warmer (golf 4): the cold rises slower while it lasts. Icon {@code mob_effect/warmed}. */
    public static final DeferredHolder<MobEffect, RateEffectMobEffect> WARMED =
            MOB_EFFECTS.register("warmed", () -> new RateEffectMobEffect(0xFF9A3C));
    /** Sky light (golf 5): the void rises slower while it lasts. Icon {@code mob_effect/anchored}. */
    public static final DeferredHolder<MobEffect, RateEffectMobEffect> ANCHORED =
            MOB_EFFECTS.register("anchored", () -> new RateEffectMobEffect(0xF4E6A6));
    /** Echo shell (golf 5): the silence rises slower while it lasts. Icon {@code mob_effect/resounding}. */
    public static final DeferredHolder<MobEffect, RateEffectMobEffect> RESOUNDING =
            MOB_EFFECTS.register("resounding", () -> new RateEffectMobEffect(0xB9A8F0));

    // Weaknesses of the Nightmares (phase 4, stream I); the Nightmares react to them on top of the mob behaviour.
    /** Sunstone flash (ring 2): a short white-out; mobs lose their target. Icon {@code mob_effect/dazzled}. */
    public static final DeferredHolder<MobEffect, DazzledMobEffect> DAZZLED =
            MOB_EFFECTS.register("dazzled", DazzledMobEffect::new);
    /** Volcanite horn (ring 3): frozen stiff, no target. Icon {@code mob_effect/stunned}. */
    public static final DeferredHolder<MobEffect, StunnedMobEffect> STUNNED =
            MOB_EFFECTS.register("stunned", StunnedMobEffect::new);

    private ModMobEffects() {
    }

    public static void register(IEventBus modBus) {
        MOB_EFFECTS.register(modBus);
    }
}
