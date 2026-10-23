package nl.timtoys.corruptedadventure.ring;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.List;

/**
 * The band axis of overworld ring 5 (package 82): dead on the inside (solid grey ground), gone on the outside (falling
 * away into the void), each about half of the ring's width. The line between them runs at {@link #INNER_SHARE} of the
 * ring, shifted by seeded noise of up to {@link #AMPLITUDE_SHARE} of the width (V1-A: about ±250 blocks), so tongues of
 * one band reach into the other; the ring's inner edge always stays inner and its outer edge outer.
 * <p>
 * The one source for the band, like {@link Ring4Band}: the density function {@code ring_5/overworld/band} (router
 * weirdness, the biomes and the terrain) only calls {@link #value}; the void meter (teleports only in the inner band),
 * the sky and the fluid rule ask {@link #inner} or {@link #outer}. Seeded with the layout's {@link RingLayout#noiseSeed()},
 * so a client that received the layout computes the same band without the world seed.
 * <p>
 * Positions are overworld x, z.
 */
public final class Ring5Band {
    /** Share of ring 5's width (from its inner boundary) that is inner band, on average. */
    public static final double INNER_SHARE = 0.5;
    /** How far (share of ring 5's width) the noise moves the line between the bands, at most (V1-A). */
    public static final double AMPLITUDE_SHARE = 0.15;
    /** Width (blocks, across the line) over which {@link #value} runs from −1 to 1 (sparring ring 5, round 2). */
    static final double TRANSITION = 200.0;
    /**
     * How fast {@link #signedDistance} changes per block across the line, typically (the median, measured on a seed,
     * {@code Ring5BandTest}): the line's noise makes it steeper than the radial distance. Turns the signed distance into
     * blocks across the line, so the transition is about {@link #TRANSITION} wide (it varies with the noise).
     */
    static final double SLOPE = 1.8;

    // Two octaves (V1-A): long waves for the tongues, short ones so the line is never smooth.
    private static final double WAVE_FREQUENCY = 1.0 / 600.0;
    private static final double RIPPLE_FREQUENCY = 1.0 / 150.0;
    private static final double WAVE_WEIGHT = 0.7;
    private static final double RIPPLE_WEIGHT = 0.3;

    private final RingLayout layout;
    private final double line;
    private final double amplitude;
    private final SimplexNoise waves;
    private final SimplexNoise ripples;

    /** Built once per layout ({@link #of}). */
    Ring5Band(RingLayout layout) {
        this.layout = layout;
        List<Integer> radii = layout.settings().ringOuterRadii();
        double inner = radii.get(Ring.RING_4.ordinal());
        double width = radii.get(Ring.RING_5.ordinal()) - inner;
        this.line = inner + INNER_SHARE * width;
        this.amplitude = AMPLITUDE_SHARE * width;
        RandomSource random = new XoroshiroRandomSource(layout.noiseSeed()).forkPositional().fromHashOf(CorruptedAdventure.id("ring_5_band"));
        this.waves = new SimplexNoise(random);
        this.ripples = new SimplexNoise(random);
    }

    /** The band of {@code layout}, kept with the layout (a server's and a client's layout in one JVM each have their own). */
    public static Ring5Band of(RingLayout layout) {
        return layout.ring5Band();
    }

    /**
     * The band axis: −1 inner (dead), 1 outer (gone), running between them over {@link #TRANSITION} blocks across the
     * line (0 in its middle). −1 everywhere inside of ring 5's inner part, 1 everywhere outside of its outer part.
     */
    public double value(double x, double z) {
        return Mth.clamp(signedDistance(x, z) / (SLOPE * TRANSITION / 2.0), -1.0, 1.0);
    }

    /** Whether the position lies in the outer band: on or beyond the middle of the transition ({@link #value} ≥ 0). */
    public boolean outer(double x, double z) {
        return signedDistance(x, z) >= 0.0;
    }

    /** Whether the position lies in the inner band ({@link #value} &lt; 0); the opposite of {@link #outer}. */
    public boolean inner(double x, double z) {
        return !outer(x, z);
    }

    /**
     * How far the position lies past the line between the bands, negative on the inner side: the effective distance
     * (with the boundary noise, so the band follows ring 5's own boundaries) past the line at {@link #INNER_SHARE}, plus
     * the line's noise. Radially in blocks; across the line it changes about {@link #SLOPE} per block. For rules that need
     * a strip along the line (the fluid rule's dam, 82).
     */
    public double signedDistance(double x, double z) {
        double noise = WAVE_WEIGHT * waves.getValue(x * WAVE_FREQUENCY, z * WAVE_FREQUENCY)
                + RIPPLE_WEIGHT * ripples.getValue(x * RIPPLE_FREQUENCY, z * RIPPLE_FREQUENCY);
        return layout.effectiveDistance(x, z) - line + amplitude * Mth.clamp(noise, -1.0, 1.0);
    }
}
