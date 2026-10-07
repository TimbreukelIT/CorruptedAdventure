package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.weather.RingWeather;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;

/**
 * The ring 3 weather on heat ({@code docs/plans/ring-3/contract.md} §5): ash rain, eruption and ash cloud multiply a
 * rising heat rate by {@code 1 + (m − 1) × intensity}, following the fade in time and switching hard at the ring
 * boundary (the weather of the player's own ring), like {@code HeatWaveDehydrationModifier}. A falling rate stays.
 * Stage {@link RateModifier.Stage#WEATHER}. Built by the conditions stream, not the weather stream, so neither waited
 * on the other; the factors come from {@code [weather.ring3]} (stream A).
 * <p>
 * Only under open sky ({@link ConditionContext#canSeeSky}, the rule of the lava bombs): in a cave or under a roof the
 * weather adds nothing. The ash cloud's fade-in is its warning (~20 s): no extra heat yet, then ×3 at once (decision
 * of the user, 2026-10-07).
 */
final class HeatWeatherModifier implements RateModifier {
    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        if (!HeatRule.isRisingHeat(condition, rate)) {
            return rate;
        }
        RingWeatherState state = RingWeather.state(context.level(), RingWeather.weatherRing(context.position().ring()));
        return weathered(rate, context.canSeeSky(), state, multiplier(state.type()));
    }

    /** A rising {@code rate} with the weather on it; unchanged away from open sky. */
    static float weathered(float rate, boolean openSky, RingWeatherState state, float multiplier) {
        return openSky ? rate * factor(state, multiplier) : rate;
    }

    /** The factor of a weather type at full strength; 1 for every type without an effect on heat. */
    static float multiplier(RingWeatherType type) {
        return (float) ServerConfig.RING_3_WEATHER.heatMultiplier(type);
    }

    /** {@code 1 + (multiplier − 1) × intensity}: follows the fade of the weather. */
    static float factor(RingWeatherState state, float multiplier) {
        return 1.0F + (multiplier - 1.0F) * heatIntensity(state);
    }

    /** The weather's intensity, except 0 during the ash cloud's warning (its fade-in): the heat comes after it. */
    static float heatIntensity(RingWeatherState state) {
        if (state.type() == RingWeatherType.ASH_CLOUD
                && state.durationTicks() - state.remainingTicks() < state.type().fadeTicks()) {
            return 0.0F;
        }
        return state.intensity();
    }
}
