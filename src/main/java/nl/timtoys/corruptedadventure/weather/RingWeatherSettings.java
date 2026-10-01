package nl.timtoys.corruptedadventure.weather;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The cycle of one ring, in ticks (filled from the SERVER config). A ring without weather types has no weights:
 * {@link #NONE}. A type with weight 0 never comes up in the cycle ({@code /ca weather} can still set it).
 */
public record RingWeatherSettings(int clearMinTicks, int clearMaxTicks, int weatherMinTicks, int weatherMaxTicks,
                                  Map<RingWeatherType, Integer> weights) {
    public static final int TICKS_PER_SECOND = 20;

    /** A ring without ring weather. */
    public static final RingWeatherSettings NONE = new RingWeatherSettings(0, 0, 0, 0, Map.of());

    public RingWeatherSettings {
        if (clearMinTicks > clearMaxTicks || weatherMinTicks > weatherMaxTicks || clearMinTicks < 0 || weatherMinTicks < 0) {
            throw new IllegalArgumentException("Bad ring weather durations: clear " + clearMinTicks + "-" + clearMaxTicks
                    + ", weather " + weatherMinTicks + "-" + weatherMaxTicks);
        }
        if (weights.containsKey(RingWeatherType.CLEAR) || weights.values().stream().anyMatch(weight -> weight < 0)) {
            throw new IllegalArgumentException("Bad ring weather weights: " + weights);
        }
        // EnumMap: the weighted draw walks the types in a fixed order, so a seeded random gives a fixed sequence.
        weights = weights.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(weights));
    }

    /** Durations as configured, in seconds. */
    public static RingWeatherSettings ofSeconds(int clearMinSeconds, int clearMaxSeconds, int weatherMinSeconds,
                                                int weatherMaxSeconds, Map<RingWeatherType, Integer> weights) {
        return new RingWeatherSettings(clearMinSeconds * TICKS_PER_SECOND, clearMaxSeconds * TICKS_PER_SECOND,
                weatherMinSeconds * TICKS_PER_SECOND, weatherMaxSeconds * TICKS_PER_SECOND, weights);
    }

    public boolean hasTypes() {
        return !weights.isEmpty();
    }
}
