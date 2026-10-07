package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;

import java.util.function.Supplier;

/**
 * {@code corruptedadventure:ring_fill_gate}: the gate inside every {@code flat_cache} marker of a ring input
 * ({@link RingBlend#mapAll}). The noise chunk fills each flat cache in advance (5 × 5 points per chunk), for every
 * ring in every chunk; that made the biome step several times vanilla's (change 39e). While a chunk's noise chunk is
 * being built ({@link #duringFill}), this gives 0 for the whole chunk unless {@code ring} reaches it; every other time
 * it is {@code argument} itself.
 * <p>
 * Unlike {@link RingGate} it never gates a single point: some ring terrain reads its flat-cached inputs at other
 * columns than its own (uphill heights of the canyon walls, the grid of the rock pillars, geode centres), up to a cell
 * away and outside the chunk. Those reads go past the chunk's cache to the argument and must keep the real value.
 * Inside the chunk the decision is all or nothing: if any terrain of the ring runs in the chunk (weight, or a gated
 * interpolator in reach), some flat-cache point of the chunk lies within 4 blocks of it and so in reach with the
 * {@link #MARGIN}, and the whole cache is real.
 */
public record RingFillGate(ResourceKey<Level> dimension, Ring ring, DensityFunction argument) implements DensityFunction {
    /** Blocks of the dimension: twice {@link RingGate#MARGIN}, as the decision covers a whole chunk at once. */
    static final double MARGIN = 32.0;

    public static final MapCodec<RingFillGate> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(RingFillGate::dimension),
            Ring.CODEC.fieldOf("ring").forGetter(RingFillGate::ring),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(RingFillGate::argument)
    ).apply(instance, RingFillGate::new));

    public static final KeyDispatchDataCodec<RingFillGate> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final ThreadLocal<Fill> FILL = new ThreadLocal<>();

    /** Runs {@code build} (creating the noise chunk of the chunk at block {@code minX}, {@code minZ}) in fill mode. */
    public static <T> T duringFill(int minX, int minZ, Supplier<T> build) {
        Fill outer = FILL.get();
        FILL.set(new Fill(minX, minZ));
        try {
            return build.get();
        } finally {
            if (outer == null) {
                FILL.remove();
            } else {
                FILL.set(outer);
            }
        }
    }

    @Override
    public double compute(FunctionContext context) {
        Fill fill = FILL.get();
        return fill == null || fill.reaches(dimension, ring) ? argument.compute(context) : 0.0;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        Fill fill = FILL.get();
        if (fill == null || fill.reaches(dimension, ring)) {
            argument.fillArray(array, contextProvider);
        } else {
            java.util.Arrays.fill(array, 0.0);
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new RingFillGate(dimension, ring, argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return Math.min(0.0, argument.minValue());
    }

    @Override
    public double maxValue() {
        return Math.max(0.0, argument.maxValue());
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    /** One chunk's fill: per ring (by ordinal) whether it reaches the chunk, decided once. */
    private static final class Fill {
        private final int minX;
        private final int minZ;
        private final byte[] reach = new byte[Ring.values().length];

        Fill(int minX, int minZ) {
            this.minX = minX;
            this.minZ = minZ;
        }

        boolean reaches(ResourceKey<Level> dimension, Ring ring) {
            byte known = reach[ring.ordinal()];
            if (known == 0) {
                known = anyPointReaches(dimension, ring) ? (byte) 1 : (byte) -1;
                reach[ring.ordinal()] = known;
            }
            return known > 0;
        }

        /** The flat-cache points of the chunk: every 4 blocks, including the far edge (5 × 5). */
        private boolean anyPointReaches(ResourceKey<Level> dimension, Ring ring) {
            for (int i = 0; i <= 16; i += 4) {
                for (int j = 0; j <= 16; j += 4) {
                    if (RingColumnCache.reaches(dimension, ring, minX + i, minZ + j, MARGIN)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
