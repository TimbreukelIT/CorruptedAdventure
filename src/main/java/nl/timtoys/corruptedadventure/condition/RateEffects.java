package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * Starts the {@link RateEffect}s of consumables and applies them as a {@link RateModifier} (stage
 * {@link RateModifier.Stage#GEAR}: what the player wears and drinks). With a coating the factors multiply.
 */
public final class RateEffects implements RateModifier {
    private static final RateEffect[] EFFECTS = RateEffect.values();

    RateEffects() {
    }

    /** Vanilla's {@code addEffect}: a shorter dose of the same effect does not cut a longer one short. */
    public static void start(ServerPlayer player, RateEffect effect, int ticks) {
        if (ticks > 0) {
            player.addEffect(new MobEffectInstance(effect.mobEffect(), ticks));
        }
    }

    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        return modify(condition, rate, effect -> context.player().hasEffect(effect.mobEffect()), RateEffect::factor);
    }

    /** {@code rate} times the factor of every active effect on {@code condition}, only if it rises. */
    static float modify(Condition condition, float rate, Predicate<RateEffect> active, ToDoubleFunction<RateEffect> factor) {
        if (rate <= 0.0F) {
            return rate;
        }
        float result = rate;
        for (RateEffect effect : EFFECTS) {
            if (effect.condition() == condition && active.test(effect)) {
                result *= (float) factor.applyAsDouble(effect);
            }
        }
        return result;
    }
}
