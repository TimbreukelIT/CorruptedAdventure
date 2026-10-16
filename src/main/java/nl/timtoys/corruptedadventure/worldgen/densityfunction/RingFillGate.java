package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:ring_fill_gate}: the gate inside every {@code flat_cache} marker of a ring input
 * ({@link RingBlend#mapAll}). The noise chunk fills each flat cache in advance (5 × 5 points per chunk), for every
 * ring in every chunk; that made the biome step several times vanilla's (change 39e). While a chunk's noise chunk is
 * being built ({@link #duringFill}; 39f: also while a base-height query iterates its column, which builds a one-cell
 * noise chunk), this gives 0 for the whole chunk unless {@code ring} reaches it; every other time it is
 * {@code argument} itself.
 * <p>
 * Unlike {@link RingGate} it never gates a single point: some ring terrain reads its flat-cached inputs at other
 * columns than its own (uphill heights of the canyon walls, the grid of the rock pillars, geode centres), up to a cell
 * away and outside the chunk. Those reads go past the chunk's cache to the argument and must keep the real value.
 * Inside the chunk the decision is all or nothing: if any terrain of the ring runs in the chunk (weight at a block, or
 * a gated interpolator in reach at a cell corner), it does so within at most 2 blocks per axis (2.83 diagonally) of a
 * flat-cache point (the points lie every 4 blocks, edges included; the corners are such points), which is well inside
 * {@link #MARGIN} even with {@link RingGate#MARGIN} on top (16 + 2.83 < 32), and the whole cache is real. {@code mix}
 * ends the ring's reach early in its outer zone, as for {@link RingGate}.
 */
public record RingFillGate(ResourceKey<Level> dimension, Ring ring, DensityFunction argument, double mix) implements DensityFunction, RingGated {
    /** Blocks of the dimension: twice {@link RingGate#MARGIN}, as the decision covers a whole chunk at once. */
    static final double MARGIN = 32.0;

    public static final MapCodec<RingFillGate> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(RingFillGate::dimension),
            Ring.CODEC.fieldOf("ring").forGetter(RingFillGate::ring),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(RingFillGate::argument),
            RingGate.MIX_CODEC.optionalFieldOf("mix", 1.0).forGetter(RingFillGate::mix)
    ).apply(instance, RingFillGate::new));

    public static final KeyDispatchDataCodec<RingFillGate> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final ThreadLocal<Fill> FILL = new ThreadLocal<>();

    /** A gate over the ring's whole outer zone. */
    public RingFillGate(ResourceKey<Level> dimension, Ring ring, DensityFunction argument) {
        this(dimension, ring, argument, 1.0);
    }

    /** Runs {@code build} (creating the noise chunk of the chunk at block {@code minX}, {@code minZ}) in fill mode. */
    public static <T> T duringFill(int minX, int minZ, Supplier<T> build) {
        return duringFill(minX, minZ, false, build);
    }

    /**
     * The same; with {@code prune} {@link RingBlend#mapAll} also leaves the inputs of rings that cannot reach the chunk
     * unmapped ({@link #prunedDuringFill}). Only without blending: the noise chunk's blend markers depend on the chunk.
     */
    public static <T> T duringFill(int minX, int minZ, boolean prune, Supplier<T> build) {
        Fill outer = FILL.get();
        FILL.set(new Fill(minX, minZ, prune));
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

    /** Whether a noise chunk is being built on this thread (fill mode): its density functions are its own copies. */
    static boolean inFill() {
        return FILL.get() != null;
    }

    /**
     * Whether the noise chunk being built on this thread prunes (vf-perf2) and {@code ring} (mixing over {@code mix} of
     * its outer zone) cannot have weight in it, decided like the gates' fill; false outside fill mode.
     */
    static boolean prunedDuringFill(ResourceKey<Level> dimension, Ring ring, double mix) {
        Fill fill = FILL.get();
        return fill != null && fill.prune && !fill.reaches(dimension, ring, mix);
    }

    @Override
    public double compute(FunctionContext context) {
        Fill fill = FILL.get();
        return fill == null || fill.reaches(dimension, ring, mix) ? argument.compute(context) : 0.0;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        Fill fill = FILL.get();
        if (fill == null || fill.reaches(dimension, ring, mix)) {
            argument.fillArray(array, contextProvider);
        } else {
            Arrays.fill(array, 0.0);
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new RingFillGate(dimension, ring, argument.mapAll(visitor), mix));
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

    /**
     * One chunk's fill: per ring whether it reaches the chunk, decided once. A fill builds one noise chunk, so all its
     * gates belong to one dimension: the first to ask (39f). A gate of another dimension (never seen) is decided
     * without remembering, so it can never read a wrong decision. Likewise one mix share per ring (all blends of a
     * dimension mix a boundary alike): the first asked is remembered, another one is decided without remembering.
     */
    private static final class Fill {
        private final int minX;
        private final int minZ;
        private ResourceKey<Level> dimension;
        private final byte[] reach = new byte[Ring.values().length];
        private final double[] shares = new double[Ring.values().length];

        private final boolean prune;

        Fill(int minX, int minZ, boolean prune) {
            this.minX = minX;
            this.minZ = minZ;
            this.prune = prune;
        }

        boolean reaches(ResourceKey<Level> dimension, Ring ring, double mix) {
            if (this.dimension == null) {
                this.dimension = dimension;
            } else if (this.dimension != dimension) {
                return anyPointReaches(dimension, ring, mix);
            }
            byte known = reach[ring.ordinal()];
            if (known == 0) {
                known = anyPointReaches(dimension, ring, mix) ? (byte) 1 : (byte) -1;
                reach[ring.ordinal()] = known;
                shares[ring.ordinal()] = mix;
            } else if (shares[ring.ordinal()] != mix) {
                return anyPointReaches(dimension, ring, mix);
            }
            return known > 0;
        }

        /** The flat-cache points of the chunk: every 4 blocks, including the far edge (5 × 5). */
        private boolean anyPointReaches(ResourceKey<Level> dimension, Ring ring, double mix) {
            for (int i = 0; i <= 16; i += 4) {
                for (int j = 0; j <= 16; j += 4) {
                    if (RingColumnCache.reaches(dimension, ring, minX + i, minZ + j, MARGIN, mix)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
