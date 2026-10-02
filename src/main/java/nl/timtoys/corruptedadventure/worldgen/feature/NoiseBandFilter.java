package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;

/**
 * Placement filter {@code corruptedadventure:noise_band}: keeps a position where a seeded noise lies in
 * [{@code min}, {@code max}]. The noise is the world's own instance ({@code RandomState.getOrCreateNoise}), sampled at
 * (x · {@code xz_scale}, 0, z · {@code xz_scale}): exactly a {@code minecraft:noise} density function with that
 * scale and {@code y_scale} 0, and with scale 1 exactly a surface rule's {@code noise_threshold}. So features fall on
 * the same zones as the terrain and the surface (the oasis rings on {@code ring_2_pools}, the talus slopes).
 * <p>
 * With {@code snap} the noise is read at the centre of the position's cell of that size instead: every candidate of
 * one {@code jittered_grid} cell gets the same answer, so features with disjoint bands never share a cell.
 */
public final class NoiseBandFilter extends PlacementFilter {
    public static final MapCodec<NoiseBandFilter> CODEC = RecordCodecBuilder.<NoiseBandFilter>mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(filter -> filter.noise),
            Codec.DOUBLE.optionalFieldOf("xz_scale", 1.0).forGetter(filter -> filter.xzScale),
            Codec.DOUBLE.optionalFieldOf("min", -Double.MAX_VALUE).forGetter(filter -> filter.min),
            Codec.DOUBLE.optionalFieldOf("max", Double.MAX_VALUE).forGetter(filter -> filter.max),
            Codec.intRange(0, 16).optionalFieldOf("snap", 0).forGetter(filter -> filter.snap)
    ).apply(instance, NoiseBandFilter::new)).validate(NoiseBandFilter::validate);

    private final ResourceKey<NormalNoise.NoiseParameters> noise;
    private final double xzScale;
    private final double min;
    private final double max;
    private final int snap;

    public NoiseBandFilter(ResourceKey<NormalNoise.NoiseParameters> noise, double xzScale, double min, double max, int snap) {
        this.noise = noise;
        this.xzScale = xzScale;
        this.min = min;
        this.max = max;
        this.snap = snap;
    }

    /** Positions where {@code noise} (scale 1) lies in [{@code min}, {@code max}]. */
    public static NoiseBandFilter between(ResourceKey<NormalNoise.NoiseParameters> noise, double min, double max) {
        return new NoiseBandFilter(noise, 1.0, min, max, 0);
    }

    /** As {@link #between}, read at the centre of the position's cell of size {@code snap}. */
    public static NoiseBandFilter snapped(ResourceKey<NormalNoise.NoiseParameters> noise, double min, double max, int snap) {
        return new NoiseBandFilter(noise, 1.0, min, max, snap);
    }

    private static DataResult<NoiseBandFilter> validate(NoiseBandFilter filter) {
        if (!(filter.xzScale > 0.0)) {
            return DataResult.error(() -> "noise_band: xz_scale must be positive, got " + filter.xzScale);
        }
        if (filter.min > filter.max) {
            return DataResult.error(() -> "noise_band: min " + filter.min + " is above max " + filter.max);
        }
        if (filter.snap != 0 && 16 % filter.snap != 0) {
            return DataResult.error(() -> "noise_band: snap must be 0 or divide 16, got " + filter.snap);
        }
        return DataResult.success(filter);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        NormalNoise instance = context.getLevel().getLevel().getChunkSource().randomState().getOrCreateNoise(noise);
        double value = sample(instance, xzScale, snapped(pos.getX(), snap), snapped(pos.getZ(), snap));
        return min <= value && value <= max;
    }

    /** The value a {@code minecraft:noise} density function with this scale and {@code y_scale} 0 gives at x, z. */
    static double sample(NormalNoise noise, double xzScale, double x, double z) {
        return noise.getValue(x * xzScale, 0.0, z * xzScale);
    }

    /** The coordinate itself, or with {@code snap} the centre of its cell (cells start at multiples of {@code snap}). */
    static double snapped(int coordinate, int snap) {
        return snap == 0 ? coordinate : Math.floorDiv(coordinate, snap) * snap + snap / 2.0;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.NOISE_BAND.get();
    }
}
