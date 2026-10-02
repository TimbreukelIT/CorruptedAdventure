package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/** Test helpers: seeded noises and plain functions of x, y, z for the ring 2 density function tests. */
final class TestFunctions {
    private TestFunctions() {
    }

    /** A noise holder as the random state would wire it: parameters plus a noise seeded with {@code seed}. */
    static DensityFunction.NoiseHolder noise(long seed, int firstOctave, double... amplitudes) {
        double[] rest = new double[amplitudes.length - 1];
        System.arraycopy(amplitudes, 1, rest, 0, rest.length);
        NormalNoise.NoiseParameters parameters = new NormalNoise.NoiseParameters(firstOctave, amplitudes[0], rest);
        return new DensityFunction.NoiseHolder(Holder.direct(parameters), NormalNoise.create(new XoroshiroRandomSource(seed), parameters));
    }

    /** A noise holder without a noise (reads 0 everywhere), as before the random state wires it. */
    static DensityFunction.NoiseHolder silent() {
        return new DensityFunction.NoiseHolder(Holder.direct(new NormalNoise.NoiseParameters(0, 1.0)));
    }

    @FunctionalInterface
    interface Xyz {
        double at(int x, int y, int z);
    }

    /** A density function of the block position (never serialised). */
    static DensityFunction of(Xyz f) {
        return new DensityFunction.SimpleFunction() {
            @Override
            public double compute(FunctionContext context) {
                return f.at(context.blockX(), context.blockY(), context.blockZ());
            }

            @Override
            public double minValue() {
                return -1.0E9;
            }

            @Override
            public double maxValue() {
                return 1.0E9;
            }

            @Override
            public KeyDispatchDataCodec<? extends DensityFunction> codec() {
                throw new UnsupportedOperationException("test function");
            }
        };
    }
}
