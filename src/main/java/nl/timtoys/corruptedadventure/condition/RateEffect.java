package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A timed effect from a consumable that multiplies the <b>rising</b> rate of one or more conditions (fase-2-items.md
 * §5.3, §6.3, §6.6), carried as a vanilla status effect. Effects of the same kind do not stack: the longest remaining time
 * wins (vanilla's rule for one amplifier). The factors come from the SERVER config, read live.
 */
public enum RateEffect {
    /** Canteen, cactus juice and dew draught: dehydration and heat rise at their {@code cooledFactor} (0.5). */
    COOLED(ModMobEffects.COOLED, Map.of(
            Condition.DEHYDRATION, () -> ServerConfig.DEHYDRATION.cooledFactor.get().floatValue(),
            Condition.HEAT, () -> ServerConfig.HEAT.cooledFactor.get().floatValue())),
    /** Purified water: poison rises at {@code cleansedFactor} (0.5). */
    CLEANSED(ModMobEffects.CLEANSED, Map.of(
            Condition.POISON, () -> ServerConfig.POISON.cleansedFactor.get().floatValue())),
    /** Hand warmer: the cold rises at {@code warmedFactor} (0.5). */
    WARMED(ModMobEffects.WARMED, Map.of(
            Condition.COLD, () -> ServerConfig.COLD.warmedFactor.get().floatValue()));

    private final Holder<MobEffect> mobEffect;
    private final Map<Condition, Supplier<Float>> factors;

    RateEffect(Holder<MobEffect> mobEffect, Map<Condition, Supplier<Float>> factors) {
        this.mobEffect = mobEffect;
        this.factors = factors;
    }

    /** Whether this effect slows {@code condition} at all. */
    public boolean slows(Condition condition) {
        return factors.containsKey(condition);
    }

    public Holder<MobEffect> mobEffect() {
        return mobEffect;
    }

    /** The factor on a rising rate of {@code condition}; 1 for a condition this effect does not slow. */
    public float factor(Condition condition) {
        Supplier<Float> factor = factors.get(condition);
        return factor == null ? 1.0F : factor.get();
    }
}
