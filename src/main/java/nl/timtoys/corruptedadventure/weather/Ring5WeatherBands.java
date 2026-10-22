package nl.timtoys.corruptedadventure.weather;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The band rule of the ring 5 weather (package 92, OV1 = A): one cycle with shared timing, and a type of its own per
 * band. The cycle's state is the inner band's weather (drawn with the inner weights); the outer band's type follows
 * from the state's seed and the outer weights, so the server and every client (same seed, same synced SERVER config)
 * derive the same type without a payload. Seed 0 means "set by command": the set type holds in both bands.
 * <p>
 * Gameplay switches hard on {@link Ring5Band#outer} (value ≥ 0; the void counts as outer); rendering blends over the
 * transition ({@link #outerShare}). Pure and common: the server passes its layout's band, the client the synced one.
 */
public final class Ring5WeatherBands {
    private Ring5WeatherBands() {
    }

    public enum Band {
        INNER,
        OUTER
    }

    /**
     * What the outer band needs besides the state: its weights and the standstill's own duration range (ticks).
     *
     * @param outerWeights   relative chance per ring 5 type in the outer band (no {@code clear}); all 0 = clear outside
     * @param standstillTicks the standstill's own duration range, in ticks
     */
    public record Settings(Map<RingWeatherType, Integer> outerWeights, RingWeatherSettings.Range standstillTicks) {
        public Settings {
            if (outerWeights.containsKey(RingWeatherType.CLEAR) || outerWeights.values().stream().anyMatch(weight -> weight < 0)) {
                throw new IllegalArgumentException("Bad outer band weights: " + outerWeights);
            }
            // EnumMap: the draw walks the types in a fixed order, the same on server and client.
            outerWeights = outerWeights.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(outerWeights));
        }
    }

    /**
     * The band at an overworld position whose ring ({@code ringAt(…).ring()}) is {@code ring}: outer on or beyond the
     * middle of the transition, and always in the void.
     */
    public static Band bandAt(RingLayout layout, Ring ring, double x, double z) {
        return ring == Ring.VOID || Ring5Band.of(layout).outer(x, z) ? Band.OUTER : Band.INNER;
    }

    /**
     * The type of the weather in {@code band}: {@code clear} stays {@code clear}; inside the cycle's own type; outside a
     * weighted draw from the seed (or the set type for seed 0).
     */
    public static RingWeatherType typeIn(RingWeatherState state, Band band, Settings settings) {
        if (state.type() == RingWeatherType.CLEAR || band == Band.INNER || state.seed() == 0) {
            return state.type();
        }
        return pick(settings.outerWeights(), state.seed());
    }

    /**
     * The state as felt in {@code band}: the same timing with the type of {@link #typeIn}. A derived type that is
     * shorter than the window (a standstill outside during a long inner weather) holds only for the first part of the
     * window, as a state of its own length (so it fades in and out properly), and then {@code clear} for the remaining
     * time. A type that is not felt is {@code clear} with the same timing (intensity 0).
     */
    public static RingWeatherState felt(RingWeatherState state, Band band, Settings settings) {
        RingWeatherType type = typeIn(state, band, settings);
        if (type == state.type()) {
            return state;
        }
        if (type == RingWeatherType.CLEAR) {
            return clearFor(state);
        }
        int length = type == RingWeatherType.STANDSTILL ? standstillTicks(state.seed(), settings.standstillTicks()) : state.durationTicks();
        if (length >= state.durationTicks()) {
            return new RingWeatherState(type, state.remainingTicks(), state.durationTicks(), state.seed());
        }
        int elapsed = state.durationTicks() - state.remainingTicks();
        return elapsed < length ? new RingWeatherState(type, length - elapsed, length, state.seed()) : clearFor(state);
    }

    /**
     * Rendering share 0–1 of the outer band at {@code bandValue} ({@link Ring5Band#value}, −1 inner … 1 outer): a
     * smoothstep over the transition, ½ in its middle; the inner band's share is 1 minus this.
     */
    public static float outerShare(double bandValue) {
        return RingWeatherMix.smoothstep((float) ((Mth.clamp(bandValue, -1.0, 1.0) + 1.0) * 0.5));
    }

    /** The derived standstill's length from the seed, uniform in {@code range} (ticks). */
    static int standstillTicks(int seed, RingWeatherSettings.Range range) {
        int span = range.max() - range.min() + 1;
        return range.min() + Math.floorMod(mix(seed, 0x5F3759DF), span);
    }

    private static RingWeatherState clearFor(RingWeatherState state) {
        return new RingWeatherState(RingWeatherType.CLEAR, state.remainingTicks(), state.durationTicks(), 0);
    }

    /** A weighted draw from the seed; {@code clear} if every weight is 0. */
    static RingWeatherType pick(Map<RingWeatherType, Integer> weights, int seed) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return RingWeatherType.CLEAR;
        }
        int roll = Math.floorMod(mix(seed, 0x2545F491), total);
        for (Map.Entry<RingWeatherType, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("Weighted draw fell through: " + weights);
    }

    /** A 32-bit integer hash (murmur3's finaliser) of the seed and a salt: the two draws above are independent. */
    private static int mix(int seed, int salt) {
        int h = seed ^ salt;
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h;
    }
}
