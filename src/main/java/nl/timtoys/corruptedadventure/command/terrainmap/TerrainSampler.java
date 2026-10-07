package nl.timtoys.corruptedadventure.command.terrainmap;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Samples the terrain of one dimension without chunks, for one thread. The final density is the router of the
 * world's random state (same noises, same {@code ring_blend} gates) with its cache markers taken out, as vanilla does
 * for the climate sampler, except the two column caches: {@code cache_2d} computes once per column and
 * {@code flat_cache} once per quart column at Y 0, as in the noise chunk (also the main saving: the scan asks many Y of
 * one column). Not modelled: the cell interpolation of {@code interpolated} (computed per block here, so edges are a
 * little sharper), local aquifer fluids and structures (beardifier).
 */
final class TerrainSampler {
    /** Step of the column scan below a ceiling: the open runs there can be short, and a column is only ~32 samples. */
    private static final int CEILING_STEP = 4;
    // Vanilla's marker types are not public; every marker of a type shares its codec (as in RingGate).
    private static final KeyDispatchDataCodec<? extends DensityFunction> FLAT_CACHE =
            DensityFunctions.flatCache(DensityFunctions.zero()).codec();
    private static final KeyDispatchDataCodec<? extends DensityFunction> CACHE_2D =
            DensityFunctions.cache2d(DensityFunctions.zero()).codec();

    static final byte NO_FLUID = 0;
    static final byte WATER = 1;
    static final byte LAVA = 2;
    static final byte NO_RING = -1;

    private final TerrainSource source;
    private final DensityFunction finalDensity;

    TerrainSampler(TerrainSource source) {
        this.source = source;
        // By identity: vanilla's records hash their whole subtree, which is slow on the deep ring router.
        Map<DensityFunction, DensityFunction> mapped = new IdentityHashMap<>();
        this.finalDensity = source.randomState().router().finalDensity().mapAll(
                function -> mapped.computeIfAbsent(function, TerrainSampler::unwrap));
    }

    private static DensityFunction unwrap(DensityFunction function) {
        if (function instanceof DensityFunctions.HolderHolder holder) {
            return holder.function().value();
        }
        if (function instanceof DensityFunctions.MarkerOrMarked marker) {
            if (marker.codec() == FLAT_CACHE) {
                return new ColumnCache(marker.wrapped(), true);
            }
            return marker.codec() == CACHE_2D ? new ColumnCache(marker.wrapped(), false) : marker.wrapped();
        }
        return function;
    }

    boolean solid(int x, int y, int z) {
        return finalDensity.compute(new DensityFunction.SinglePointContext(x, y, z)) > 0.0;
    }

    /** The visible ground: the top of the column, or below a ceiling the floor of the tallest opening. */
    int ground(int x, int z) {
        return ground(source, y -> solid(x, y, z), source.ceiling() ? CEILING_STEP : source.cellHeight());
    }

    /** The one ground rule for top views and sections ({@code step} 1: every Y is known already). */
    static int ground(TerrainSource source, ColumnScan.Solid solid, int step) {
        return source.ceiling()
                ? ColumnScan.floorOfTallestOpening(solid, source.minY(), source.maxY(), step)
                : ColumnScan.top(solid, source.minY(), source.maxY(), step);
    }

    /** The fluid the generator puts in open space here (sea, lava sea; no local aquifers). */
    byte fluid(int x, int y, int z) {
        BlockState state = source.generator().globalFluidPicker.get().computeFluid(x, y, z).at(y);
        if (state.isAir()) {
            return NO_FLUID;
        }
        return state.is(Blocks.LAVA) ? LAVA : WATER;
    }

    Holder<Biome> biome(int x, int y, int z) {
        return source.generator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y),
                QuartPos.fromBlock(z), source.randomState().sampler());
    }

    /** {@code Ring.ordinal()} of the ring that dominates here, or {@link #NO_RING} outside a ring world. */
    byte ringCode(int x, int z) {
        if (source.layout() == null) {
            return NO_RING;
        }
        Ring ring = source.layout().sample(source.dimension(), x, z).ring();
        return (byte) ring.ordinal();
    }

    static boolean hasRings(byte[] ringCodes) {
        return ringCodes.length > 0 && ringCodes[0] != NO_RING;
    }

    /**
     * The last column's value of a 2D function; {@code quart}: {@code flat_cache}, sampled at Y 0 on the quart grid.
     * Mutable, so only for the one thread that owns this sampler.
     */
    private static final class ColumnCache implements DensityFunction.SimpleFunction {
        private final DensityFunction wrapped;
        private final boolean quart;
        private long lastColumn = Long.MIN_VALUE;
        private double lastValue;

        ColumnCache(DensityFunction wrapped, boolean quart) {
            this.wrapped = wrapped;
            this.quart = quart;
        }

        @Override
        public double compute(FunctionContext context) {
            int x = quart ? QuartPos.toBlock(QuartPos.fromBlock(context.blockX())) : context.blockX();
            int z = quart ? QuartPos.toBlock(QuartPos.fromBlock(context.blockZ())) : context.blockZ();
            long column = ChunkPos.asLong(x, z);
            if (column != lastColumn) {
                lastValue = wrapped.compute(quart ? new SinglePointContext(x, 0, z) : context);
                lastColumn = column;
            }
            return lastValue;
        }

        @Override
        public double minValue() {
            return wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return wrapped.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return wrapped.codec();
        }
    }
}
