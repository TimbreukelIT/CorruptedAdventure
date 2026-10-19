package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The optional {@code density} of {@link JitteredGridPlacement} (ring 4 decor proef, 2026-10-19): the share of a cell's
 * {@code chance} that is kept, from a seeded noise read at the cell's centre. Groves where the noise is high, open
 * ground where it is low: the grid still spaces the candidates, the noise only thins them out per cell.
 * <p>
 * The share is {@code floor + (1 − floor) · ramp}, with {@code ramp} rising linearly from 0 at {@code low} to 1 at
 * {@code high} (with {@code high < low} it falls instead: the open ground of the same noise). The noise is the world's
 * own instance of {@code noise} ({@code RandomState.getOrCreateNoise}), read at ((x + s) · {@code xz_scale}, 0,
 * (z + s) · {@code xz_scale}) with s = {@code salt} · {@link #SALT_STEP}: features with another salt get another,
 * independent field of the same noise; features with the same salt and cell size agree per cell. Pure in the seed and
 * the cell, so the placement stays independent of generation order.
 */
public record ClusterDensity(ResourceKey<NormalNoise.NoiseParameters> noise, double xzScale, int salt, double low, double high,
                             double floor) {
    /** Blocks between two salts' fields: far beyond the noise's largest features. */
    static final double SALT_STEP = 20011.0;

    public static final Codec<ClusterDensity> CODEC = RecordCodecBuilder.<ClusterDensity>create(instance -> instance.group(
            ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(ClusterDensity::noise),
            Codec.DOUBLE.optionalFieldOf("xz_scale", 1.0).forGetter(ClusterDensity::xzScale),
            Codec.INT.optionalFieldOf("salt", 0).forGetter(ClusterDensity::salt),
            Codec.DOUBLE.fieldOf("low").forGetter(ClusterDensity::low),
            Codec.DOUBLE.fieldOf("high").forGetter(ClusterDensity::high),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("floor", 0.0).forGetter(ClusterDensity::floor)
    ).apply(instance, ClusterDensity::new)).validate(ClusterDensity::validate);

    /** Groves of {@code noise} (scale 1, salt 0, no floor): nothing below {@code low}, every cell from {@code high}. */
    public static ClusterDensity of(ResourceKey<NormalNoise.NoiseParameters> noise, double low, double high) {
        return new ClusterDensity(noise, 1.0, 0, low, high, 0.0);
    }

    public ClusterDensity withSalt(int salt) {
        return new ClusterDensity(noise, xzScale, salt, low, high, floor);
    }

    public ClusterDensity withFloor(double floor) {
        return new ClusterDensity(noise, xzScale, salt, low, high, floor);
    }

    private static DataResult<ClusterDensity> validate(ClusterDensity density) {
        if (!(density.xzScale > 0.0)) {
            return DataResult.error(() -> "density: xz_scale must be positive, got " + density.xzScale);
        }
        return density.low != density.high ? DataResult.success(density)
                : DataResult.error(() -> "density: low and high must differ, both " + density.low);
    }

    /** The share of the chance kept at block x, z (the caller passes a cell's centre). */
    double share(NormalNoise instance, double x, double z) {
        return share(value(instance, x, z));
    }

    /** The noise as this density reads it at x, z. */
    double value(NormalNoise instance, double x, double z) {
        double offset = salt * SALT_STEP;
        return instance.getValue((x + offset) * xzScale, 0.0, (z + offset) * xzScale);
    }

    /** The share for a noise value. */
    double share(double value) {
        double ramp = Mth.clamp((value - low) / (high - low), 0.0, 1.0);
        return floor + (1.0 - floor) * ramp;
    }
}
