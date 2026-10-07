package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.effect.MobEffects;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * Fire resistance slows rising heat ({@code fireResistanceFactor}, 0.5 by default); a falling rate stays. Stage
 * {@link RateModifier.Stage#FIRE_RESISTANCE}: after the weather (and later gear).
 */
final class FireResistanceHeatModifier implements RateModifier {
    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        return HeatRule.isRisingHeat(condition, rate) && context.player().hasEffect(MobEffects.FIRE_RESISTANCE)
                ? rate * ServerConfig.HEAT.fireResistanceFactor.get().floatValue()
                : rate;
    }
}
