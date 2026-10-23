package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Ring 5's glitch fog (package 98, V98-4 A): the fog's edge stutters. Every {@link #MIN_WAIT}–{@link #MAX_WAIT} ticks
 * (4–12 s; twice as often in {@code glitch_fields}) the far plane jumps hard to {@link #MIN_FACTOR}–{@link #MAX_FACTOR}
 * of itself for {@link #MIN_TICKS}–{@link #MAX_TICKS} ticks, as if a frame dropped out; one in {@link #VIOLET_ONE_IN}
 * times the fog turns violet-black too. Pure state, one instance per client ({@link Ring5Fog}); the strength on screen
 * follows vanilla's "Screen Effect Scale" (0 = off). Client randomness: no worldgen, nothing synced.
 */
final class GlitchFog {
    static final int MIN_WAIT = 80;
    static final int MAX_WAIT = 240;
    static final int MIN_TICKS = 1;
    static final int MAX_TICKS = 3;
    static final float MIN_FACTOR = 0.4F;
    static final float MAX_FACTOR = 0.7F;
    static final int VIOLET_ONE_IN = 4;

    private float wait = -1.0F;
    private int left;
    private float factor = 1.0F;
    private boolean violet;

    /**
     * One client tick.
     *
     * @param active whether the glitch fog may run here (ring 5, no glitch storm of its own)
     * @param rate   how fast the wait runs down: 1, or 2 in {@code glitch_fields}
     */
    void tick(RandomSource random, boolean active, float rate) {
        if (!active) {
            reset();
            return;
        }
        if (left > 0) {
            left--;
            return;
        }
        if (wait < 0.0F) {
            wait = nextWait(random);
        }
        wait -= rate;
        if (wait <= 0.0F) {
            left = MIN_TICKS + random.nextInt(MAX_TICKS - MIN_TICKS + 1);
            factor = MIN_FACTOR + random.nextFloat() * (MAX_FACTOR - MIN_FACTOR);
            violet = random.nextInt(VIOLET_ONE_IN) == 0;
            wait = nextWait(random);
        }
    }

    /** No stutter, and a fresh wait once active again. */
    void reset() {
        wait = -1.0F;
        left = 0;
    }

    private static float nextWait(RandomSource random) {
        return MIN_WAIT + random.nextInt(MAX_WAIT - MIN_WAIT + 1);
    }

    /** Whether a stutter shows this tick. */
    boolean stuttering() {
        return left > 0;
    }

    /** The factor on the far plane this tick: 1 between stutters; {@code screenEffects} 0 keeps it at 1. */
    float farFactor(float screenEffects) {
        return left > 0 ? Mth.lerp(Mth.clamp(screenEffects, 0.0F, 1.0F), 1.0F, factor) : 1.0F;
    }

    /** Whether this stutter also turns the fog violet-black. */
    boolean violet() {
        return left > 0 && violet;
    }
}
