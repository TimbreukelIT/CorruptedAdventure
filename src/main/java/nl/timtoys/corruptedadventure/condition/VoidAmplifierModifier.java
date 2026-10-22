package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.weather.RingWeather;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * Night and ring weather on the void (package 91, {@code golf-1.md} P5): in the void zone a rising void rate is
 * multiplied by the larger of the night factor ({@code [conditions.void] nightMultiplier}, while the sun is down) and the
 * weather factor ({@code 1 + (factor(type) − 1) × intensity}, so it follows the fade). The two do not stack: their
 * maximum counts, not their product. A falling rate is left alone. The weather is the one the player feels here,
 * {@link RingWeather#stateAt} (which applies ring 5's band rule; this modifier never picks the band for the weather
 * itself). Stage {@link RateModifier.Stage#WEATHER}. The weather factors are {@code [weather.ring5]}'s void multipliers
 * (stream A, {@link ServerConfig.Ring5WeatherEffects#voidMultiplier}).
 */
final class VoidAmplifierModifier implements RateModifier {

    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        if (condition != Condition.VOID || rate <= 0.0F
                || !VoidZone.inZone(context.level().dimension(), context.position())) {
            return rate; // most calls: no closure, no config read, no weather lookup
        }
        return amplified(rate, () -> !context.isSunUp(),
                () -> RingWeather.stateAt(context.level(), context.player().getX(), context.player().getZ()),
                ServerConfig.VOID.nightMultiplier.get().floatValue(), ServerConfig.RING_5_WEATHER::voidMultiplier);
    }

    /**
     * {@code rate × max(night factor, weather factor)} for a rising rate, else {@code rate}; night and weather are only
     * asked for a rising rate.
     */
    static float amplified(float rate, BooleanSupplier night, Supplier<RingWeatherState> weather, float nightMultiplier,
                           ToDoubleFunction<RingWeatherType> weatherMultiplier) {
        if (rate <= 0.0F) {
            return rate;
        }
        float nightFactor = night.getAsBoolean() ? nightMultiplier : 1.0F;
        RingWeatherState state = weather.get();
        float fullFactor = (float) weatherMultiplier.applyAsDouble(state.type());
        float weatherFactor = 1.0F + (fullFactor - 1.0F) * state.intensity();
        return rate * Math.max(1.0F, Math.max(nightFactor, weatherFactor));
    }
}
