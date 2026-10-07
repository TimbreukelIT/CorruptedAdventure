package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.weather.RingWeather;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;

/**
 * The ring 3 weather on heat ({@code docs/plans/ring-3/contract.md} §5): ash rain and eruption multiply a rising heat
 * rate by {@code 1 + (m − 1) × intensity}, following the fade in time and switching hard at the ring boundary (the
 * weather of the player's own ring), like {@code HeatWaveDehydrationModifier}. A falling rate stays. Stage
 * {@link RateModifier.Stage#WEATHER}. Built by the conditions stream, not the weather stream, so neither waits on the
 * other; the ash cloud (×3) follows in wave 2.
 */
final class HeatWeatherModifier implements RateModifier {
    /**
     * Start values of {@code [weather.ring3] ashRainHeatMultiplier} / {@code eruptionHeatMultiplier} (stream A). They
     * stand in until A is merged; then {@link #multiplier} becomes {@code ServerConfig.RING_3_WEATHER.heatMultiplier}.
     */
    static final float ASH_RAIN_MULTIPLIER = 1.5F;
    static final float ERUPTION_MULTIPLIER = 2.0F;

    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        if (!HeatRule.isRisingHeat(condition, rate)) {
            return rate;
        }
        RingWeatherState state = RingWeather.state(context.level(), RingWeather.weatherRing(context.position().ring()));
        return rate * factor(state, multiplier(state.type()));
    }

    /** The factor of a weather type at full strength; 1 for every type without an effect on heat. */
    static float multiplier(RingWeatherType type) {
        // Integration (after stream A): return (float) ServerConfig.RING_3_WEATHER.heatMultiplier(type);
        return switch (type) {
            case ASH_RAIN -> ASH_RAIN_MULTIPLIER;
            case ERUPTION -> ERUPTION_MULTIPLIER;
            default -> 1.0F;
        };
    }

    /** {@code 1 + (multiplier − 1) × intensity}: follows the fade of the weather. */
    static float factor(RingWeatherState state, float multiplier) {
        return 1.0F + (multiplier - 1.0F) * state.intensity();
    }
}
