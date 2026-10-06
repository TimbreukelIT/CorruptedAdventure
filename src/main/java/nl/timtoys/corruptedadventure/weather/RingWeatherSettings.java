package nl.timtoys.corruptedadventure.weather;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The cycle of one ring, in ticks (filled from the SERVER config). A ring without weather types has no weights:
 * {@link #NONE}. A type with weight 0 never comes up in the cycle ({@code /ca weather} can still set it). A type in
 * {@code durations} lasts its own range instead of the ring's weather range (the ash cloud of ring 3).
 */
public record RingWeatherSettings(int clearMinTicks, int clearMaxTicks, int weatherMinTicks, int weatherMaxTicks,
                                  Map<RingWeatherType, Integer> weights, Map<RingWeatherType, Range> durations) {
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
        if (durations.containsKey(RingWeatherType.CLEAR)) {
            throw new IllegalArgumentException("clear has no weather duration: " + durations);
        }
        // EnumMap: the weighted draw walks the types in a fixed order, so a seeded random gives a fixed sequence.
        weights = weights.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(weights));
        durations = durations.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(durations));
    }

    /** Every type lasts the ring's weather range. */
    public RingWeatherSettings(int clearMinTicks, int clearMaxTicks, int weatherMinTicks, int weatherMaxTicks,
                               Map<RingWeatherType, Integer> weights) {
        this(clearMinTicks, clearMaxTicks, weatherMinTicks, weatherMaxTicks, weights, Map.of());
    }

    /** Durations as configured, in seconds; every type lasts the ring's weather range. */
    public static RingWeatherSettings ofSeconds(int clearMinSeconds, int clearMaxSeconds, int weatherMinSeconds,
                                                int weatherMaxSeconds, Map<RingWeatherType, Integer> weights) {
        return ofSeconds(clearMinSeconds, clearMaxSeconds, weatherMinSeconds, weatherMaxSeconds, weights, Map.of());
    }

    /** Durations as configured, in seconds, including the types with a range of their own (also in seconds). */
    public static RingWeatherSettings ofSeconds(int clearMinSeconds, int clearMaxSeconds, int weatherMinSeconds,
                                                int weatherMaxSeconds, Map<RingWeatherType, Integer> weights,
                                                Map<RingWeatherType, Range> durationSeconds) {
        Map<RingWeatherType, Range> durations = new EnumMap<>(RingWeatherType.class);
        durationSeconds.forEach((type, range) -> durations.put(type,
                new Range(range.min() * TICKS_PER_SECOND, range.max() * TICKS_PER_SECOND)));
        return new RingWeatherSettings(clearMinSeconds * TICKS_PER_SECOND, clearMaxSeconds * TICKS_PER_SECOND,
                weatherMinSeconds * TICKS_PER_SECOND, weatherMaxSeconds * TICKS_PER_SECOND, weights, durations);
    }

    public boolean hasTypes() {
        return !weights.isEmpty();
    }

    /** How long {@code type} lasts: its own range, else the ring's weather range. */
    public Range weatherRange(RingWeatherType type) {
        Range own = durations.get(type);
        return own != null ? own : new Range(weatherMinTicks, weatherMaxTicks);
    }

    /** A duration range, 0 ≤ min ≤ max (ticks or seconds, as the caller says). */
    public record Range(int min, int max) {
        public Range {
            if (min < 0 || min > max) {
                throw new IllegalArgumentException("Bad duration range: " + min + "-" + max);
            }
        }
    }
}
