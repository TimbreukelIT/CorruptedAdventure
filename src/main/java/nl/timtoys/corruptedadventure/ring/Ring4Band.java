package nl.timtoys.corruptedadventure.ring;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.List;

/**
 * The band axis of overworld ring 4 (package 59): mythical on the inside (about 60 % of the ring's width), blighted on
 * the outside (about 40 %). The line between them runs at 60 % of the ring, shifted by seeded noise of up to
 * {@link #AMPLITUDE_SHARE} of the width, so blighted tongues reach into the mythical band and mythical islets lie in
 * the blighted one; the ring's inner edge always stays mythical and its outer edge blighted.
 * <p>
 * The one source for the band: the density function {@code ring_4/overworld/band} (router weirdness, the biomes and the
 * terrain) only calls {@link #value}, and the weather and the poison meter ask {@link #blighted}. Seeded with the
 * layout's {@link RingLayout#noiseSeed()} like the boundary noise, so a client that received the layout computes the
 * same band without the world seed.
 * <p>
 * Positions are overworld x, z.
 */
public final class Ring4Band {
    /** Share of ring 4's width (from its inner boundary) that is mythical, on average. */
    static final double MYTHIC_SHARE = 0.6;
    /** How far (share of ring 4's width) the noise moves the line between the bands, at most. */
    static final double AMPLITUDE_SHARE = 0.225;
    /** Width (blocks, across the line) over which {@link #value} runs from −1 to 1: where the edge biomes mix. */
    static final double TRANSITION = 64.0;
    /**
     * How fast {@link #signedDistance} changes per block across the line, typically (the median, measured on a seed):
     * the line's noise is steeper than the radial distance, mostly through its short octave. Turns the signed distance
     * into blocks across the line, so the transition is about {@link #TRANSITION} wide (it varies with the noise).
     */
    static final double SLOPE = 2.5;

    // Two octaves: long waves for the tongues, short ones so the line is never smooth.
    private static final double WAVE_FREQUENCY = 1.0 / 700.0;
    private static final double RIPPLE_FREQUENCY = 1.0 / 180.0;
    private static final double WAVE_WEIGHT = 0.7;
    private static final double RIPPLE_WEIGHT = 0.3;

    private final RingLayout layout;
    private final double line;
    private final double amplitude;
    private final SimplexNoise waves;
    private final SimplexNoise ripples;

    /** Built once per layout ({@link #of}). */
    Ring4Band(RingLayout layout) {
        this.layout = layout;
        List<Integer> radii = layout.settings().ringOuterRadii();
        double inner = radii.get(Ring.RING_3.ordinal());
        double width = radii.get(Ring.RING_4.ordinal()) - inner;
        this.line = inner + MYTHIC_SHARE * width;
        this.amplitude = AMPLITUDE_SHARE * width;
        RandomSource random = new XoroshiroRandomSource(layout.noiseSeed()).forkPositional().fromHashOf(CorruptedAdventure.id("ring_4_band"));
        this.waves = new SimplexNoise(random);
        this.ripples = new SimplexNoise(random);
    }

    /** The band of {@code layout}, kept with the layout (a server's and a client's layout in one JVM each have their own). */
    public static Ring4Band of(RingLayout layout) {
        return layout.ring4Band();
    }

    /**
     * The band axis: −1 mythical, 1 blighted, running between them over {@link #TRANSITION} blocks across the line (0 in
     * its middle). −1 everywhere inside of ring 4's mythical part, 1 everywhere outside of its blighted part.
     */
    public double value(double x, double z) {
        return Mth.clamp(signedDistance(x, z) / (SLOPE * TRANSITION / 2.0), -1.0, 1.0);
    }

    /** Whether the position lies in the blighted band: on or beyond the middle of the transition ({@link #value} ≥ 0). */
    public boolean blighted(double x, double z) {
        return signedDistance(x, z) >= 0.0;
    }

    /**
     * How far the position lies past the line between the bands, negative on the mythical side: the effective distance
     * (with the boundary noise, so the band follows ring 4's own boundaries) past the line at 60 %, plus the line's
     * noise. Radially in blocks; across the line it changes about {@link #SLOPE} per block.
     */
    double signedDistance(double x, double z) {
        double noise = WAVE_WEIGHT * waves.getValue(x * WAVE_FREQUENCY, z * WAVE_FREQUENCY)
                + RIPPLE_WEIGHT * ripples.getValue(x * RIPPLE_FREQUENCY, z * RIPPLE_FREQUENCY);
        return layout.effectiveDistance(x, z) - line + amplitude * Mth.clamp(noise, -1.0, 1.0);
    }
}
