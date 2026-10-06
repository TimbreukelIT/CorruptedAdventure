package nl.timtoys.corruptedadventure.weather;

import net.minecraft.util.RandomSource;

import java.util.Map;

/**
 * The weather cycle of one ring, without a level: counts down and, at the end, picks what comes next. After a weather
 * always {@code clear}; after {@code clear} a type of the ring by weight (all weights 0: {@code clear} again). Durations
 * are uniform between min and max. A ring without types stays {@link RingWeatherState#STILL}.
 * <p>
 * The random is the level's ({@code level.getRandom()}): ring weather is not worldgen, so it need not follow the seed.
 */
public final class RingWeatherCycle {
    private RingWeatherCycle() {
    }

    /** The state {@code ticks} later; a new weather starts with its full duration (overshoot is dropped). */
    public static RingWeatherState advance(RingWeatherState state, int ticks, RingWeatherSettings settings, RandomSource random) {
        if (!settings.hasTypes()) {
            return RingWeatherState.STILL;
        }
        if (state.remainingTicks() > ticks) {
            return state.countDown(ticks);
        }
        return state.type() == RingWeatherType.CLEAR ? startWeather(settings, random) : startClear(settings, random);
    }

    /** {@code clear} with a duration drawn from the settings. */
    public static RingWeatherState startClear(RingWeatherSettings settings, RandomSource random) {
        return RingWeatherState.start(RingWeatherType.CLEAR,
                between(settings.clearMinTicks(), settings.clearMaxTicks(), random), 0);
    }

    /** A type drawn by weight with a weather duration, or {@code clear} if every weight is 0. */
    static RingWeatherState startWeather(RingWeatherSettings settings, RandomSource random) {
        RingWeatherType type = pick(settings.weights(), random);
        return type == null ? startClear(settings, random) : start(type, settings, random);
    }

    /** {@code type} with a duration drawn from the settings (clear range, or the type's weather range) and a new seed. */
    public static RingWeatherState start(RingWeatherType type, RingWeatherSettings settings, RandomSource random) {
        if (type == RingWeatherType.CLEAR) {
            return startClear(settings, random);
        }
        RingWeatherSettings.Range range = settings.weatherRange(type);
        int duration = between(range.min(), range.max(), random);
        return RingWeatherState.start(type, duration, random.nextInt());
    }

    private static RingWeatherType pick(Map<RingWeatherType, Integer> weights, RandomSource random) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Map.Entry<RingWeatherType, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("Weighted draw fell through: " + weights);
    }

    private static int between(int min, int max, RandomSource random) {
        return min + random.nextInt(max - min + 1);
    }
}
