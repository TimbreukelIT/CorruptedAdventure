package nl.timtoys.corruptedadventure.weather;

import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.ConditionContext;
import nl.timtoys.corruptedadventure.condition.RateModifier;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * Heat wave: dehydration rises faster ({@code heatWaveDehydrationMultiplier}, 2× by default). Reads the weather of the
 * player's ring itself, so the conditions code does not know about weather; only a rising rate is multiplied (the
 * dehydration rule rises only in direct sun), never a falling one. Follows the fade in time and switches hard at the
 * ring boundary, like the push of the sandstorm.
 */
public final class HeatWaveDehydrationModifier implements RateModifier {
    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        if (condition != Condition.DEHYDRATION || rate <= 0.0F) {
            return rate;
        }
        RingWeatherState state = RingWeather.state(context.level(), RingWeather.weatherRing(context.position().ring()));
        return apply(condition, rate, state, ServerConfig.RING_2_WEATHER.heatWaveDehydrationMultiplier.get().floatValue());
    }

    static float apply(Condition condition, float rate, RingWeatherState state, float multiplier) {
        if (condition != Condition.DEHYDRATION || rate <= 0.0F) {
            return rate;
        }
        return rate * factor(state, multiplier);
    }

    /** {@code 1 + (multiplier − 1) × intensity} during a heat wave, otherwise 1. */
    static float factor(RingWeatherState state, float multiplier) {
        if (state.type() != RingWeatherType.HEAT_WAVE) {
            return 1.0F;
        }
        return 1.0F + (multiplier - 1.0F) * state.intensity();
    }
}
