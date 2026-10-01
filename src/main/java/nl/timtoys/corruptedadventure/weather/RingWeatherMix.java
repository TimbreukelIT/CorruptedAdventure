package nl.timtoys.corruptedadventure.weather;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * How strongly each ring's weather is felt at a position, for rendering: inside a ring 1, across a transition zone a
 * smoothstep from the inner to the outer ring (a sandstorm thins out towards ring 3 instead of ending in a wall). The
 * void follows ring 5. Gameplay does not blend: it uses {@link RingWeather#stateAt} (hard switch at 0.5).
 * <p>
 * Pure and common, so it can be tested; the client hook and later the boundary walls use it.
 */
public final class RingWeatherMix {
    private RingWeatherMix() {
    }

    /**
     * One ring's share at a position.
     *
     * @param weight   spatial share 0–1 (the shares at a position add up to 1)
     * @param strength {@code state.intensity() × weight}: what a renderer should show
     */
    public record Share(Ring ring, RingWeatherState state, float weight, float strength) {
    }

    /** The rings at {@code position} with their share, inner ring first; a ring with weight 0 is left out. */
    public static List<Share> at(RingPosition position, Function<Ring, RingWeatherState> states) {
        Ring inner = RingWeather.weatherRing(position.inner());
        Ring outer = RingWeather.weatherRing(position.outer());
        List<Share> shares = new ArrayList<>(2);
        if (inner == outer) {
            add(shares, inner, 1.0F, states);
            return shares;
        }
        float outerWeight = smoothstep((float) position.blend());
        add(shares, inner, 1.0F - outerWeight, states);
        add(shares, outer, outerWeight, states);
        return shares;
    }

    private static void add(List<Share> shares, Ring ring, float weight, Function<Ring, RingWeatherState> states) {
        if (weight > 0.0F) {
            RingWeatherState state = states.apply(ring);
            shares.add(new Share(ring, state, weight, state.intensity() * weight));
        }
    }

    static float smoothstep(float t) {
        float clamped = Math.clamp(t, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
