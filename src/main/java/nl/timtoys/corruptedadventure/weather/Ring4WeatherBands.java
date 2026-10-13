package nl.timtoys.corruptedadventure.weather;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;

/**
 * The band rule of the ring 4 weather (package 70): ring 4 has one cycle, and the type decides where it is felt. Dense
 * fog and storm wind belong to the mythical band, toxic rain to the blighted band, so the two bands never have weather
 * at the same time. Every other type (other rings) is not bound to a band.
 * <p>
 * Gameplay switches hard in the middle of the band transition ({@link Ring4Band#blighted}, value ≥ 0); rendering
 * blends over the whole transition ({@link #renderShare}), like the rings themselves. Pure and common: the server
 * passes its layout's band, the client the band of the synced layout.
 */
public final class Ring4WeatherBands {
    private Ring4WeatherBands() {
    }

    /** Which band a type belongs to. */
    public enum Band {
        /** Not bound to a band (clear, the weather of the other rings). */
        ANY,
        MYTHIC,
        BLIGHTED
    }

    public static Band bandOf(RingWeatherType type) {
        return switch (type) {
            case DENSE_FOG, STORM_WIND -> Band.MYTHIC;
            case TOXIC_RAIN -> Band.BLIGHTED;
            default -> Band.ANY;
        };
    }

    /** Whether {@code type} is felt at a place in the band given by {@code blighted} (gameplay, hard switch). */
    public static boolean appliesIn(RingWeatherType type, boolean blighted) {
        return switch (bandOf(type)) {
            case ANY -> true;
            case MYTHIC -> !blighted;
            case BLIGHTED -> blighted;
        };
    }

    /**
     * The weather of ring {@code ring} as felt at a place in the band given by {@code blighted}: {@code state} itself,
     * or, for a ring 4 type of the other band, {@code clear} for the rest of that weather (intensity 0, same timing).
     */
    public static RingWeatherState felt(Ring ring, RingWeatherState state, boolean blighted) {
        if (ring != Ring.RING_4 || appliesIn(state.type(), blighted)) {
            return state;
        }
        return new RingWeatherState(RingWeatherType.CLEAR, state.remainingTicks(), state.durationTicks(), 0);
    }

    /**
     * Rendering share 0–1 of {@code type} at the band value {@code bandValue} ({@link Ring4Band#value}, −1 mythical …
     * 1 blighted): a smoothstep over the transition, ½ in its middle, so fog or rain thins out instead of ending in a
     * wall. 1 for a type that is not bound to a band.
     */
    public static float renderShare(RingWeatherType type, double bandValue) {
        float blighted = RingWeatherMix.smoothstep((float) ((Mth.clamp(bandValue, -1.0, 1.0) + 1.0) * 0.5));
        return switch (bandOf(type)) {
            case ANY -> 1.0F;
            case MYTHIC -> 1.0F - blighted;
            case BLIGHTED -> blighted;
        };
    }
}
