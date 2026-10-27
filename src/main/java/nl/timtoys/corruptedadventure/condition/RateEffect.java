package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

import java.util.function.Supplier;

/**
 * A timed effect from a consumable that multiplies one condition's <b>rising</b> rate (fase-2-items.md §5.3), carried as
 * a vanilla status effect. Effects of the same kind do not stack: the longest remaining time wins (vanilla's rule for
 * one amplifier). The factor comes from the SERVER config, read live.
 */
public enum RateEffect {
    /** Canteen and cactus juice: dehydration rises at {@code cooledFactor} (0.5). */
    COOLED(Condition.DEHYDRATION, ModMobEffects.COOLED,
            () -> ServerConfig.DEHYDRATION.cooledFactor.get().floatValue());

    private final Condition condition;
    private final Holder<MobEffect> mobEffect;
    private final Supplier<Float> factor;

    RateEffect(Condition condition, Holder<MobEffect> mobEffect, Supplier<Float> factor) {
        this.condition = condition;
        this.mobEffect = mobEffect;
        this.factor = factor;
    }

    public Condition condition() {
        return condition;
    }

    public Holder<MobEffect> mobEffect() {
        return mobEffect;
    }

    public float factor() {
        return factor.get();
    }
}
