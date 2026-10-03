package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * A pure hash of a few integers and the world seed, for the cell-based density functions. Density functions do not
 * get the world seed; a {@link DensityFunction.NoiseHolder} does (wired by the random state). Its noise is sampled at
 * a non-integer point derived from the integers (Perlin noise is 0 on the integer lattice) and the bits of that value
 * are mixed with the integers themselves, so neighbouring inputs give unrelated outputs. Surface rule conditions get a
 * {@link NormalNoise} from the random state instead; that overload gives the same hash for the same noise.
 */
public final class SeededHash {
    private SeededHash() {
    }

    static long hash(DensityFunction.NoiseHolder noise, long a, long b, long salt) {
        return combine(noise.getValue(sampleX(a, salt), sampleY(salt), sampleZ(b)), a, b, salt);
    }

    public static long hash(NormalNoise noise, long a, long b, long salt) {
        return combine(noise.getValue(sampleX(a, salt), sampleY(salt), sampleZ(b)), a, b, salt);
    }

    private static double sampleX(long a, long salt) {
        return a * 7.1234 + 0.317 + salt * 0.0731;
    }

    private static double sampleY(long salt) {
        return salt * 13.371 + 0.5;
    }

    private static double sampleZ(long b) {
        return b * 7.1234 + 0.771;
    }

    private static long combine(double sample, long a, long b, long salt) {
        long ints = mix(a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L);
        return mix(Double.doubleToRawLongBits(sample) ^ ints);
    }

    /** SplitMix64's finaliser. */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A uniform value in [0, 1) from 53 bits of {@code hash}. */
    static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    /** The {@code n}-th independent uniform value in [0, 1) drawn from {@code hash}. */
    public static double unit(long hash, int n) {
        return unit(mix(hash + n * 0x9E3779B97F4A7C15L));
    }
}
