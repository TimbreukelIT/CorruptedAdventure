package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@code corruptedadventure:radial_falloff}: 1 within {@code inner_radius} blocks of 0,0 (in the dimension's own
 * coordinates), 0 from {@code outer_radius} on, with a smoothstep in between so nothing built on it has a kink.
 * Vanilla density functions have no x/z input; this lets JSON shape terrain around the world centre (e.g. the
 * land around spawn).
 */
public record RadialFalloff(double innerRadius, double outerRadius) implements DensityFunction.SimpleFunction {
    private static final Codec<Double> RADIUS = Codec.doubleRange(0.0, Double.MAX_VALUE);

    public static final MapCodec<RadialFalloff> MAP_CODEC = RecordCodecBuilder
            .<RadialFalloff>mapCodec(instance -> instance.group(
                    RADIUS.fieldOf("inner_radius").forGetter(RadialFalloff::innerRadius),
                    RADIUS.fieldOf("outer_radius").forGetter(RadialFalloff::outerRadius)
            ).apply(instance, RadialFalloff::new))
            .validate(RadialFalloff::validate);

    public static final KeyDispatchDataCodec<RadialFalloff> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static DataResult<RadialFalloff> validate(RadialFalloff falloff) {
        return falloff.innerRadius < falloff.outerRadius
                ? DataResult.success(falloff)
                : DataResult.error(() -> "Radial falloff needs inner_radius < outer_radius, got "
                        + falloff.innerRadius + " and " + falloff.outerRadius);
    }

    @Override
    public double compute(FunctionContext context) {
        double x = context.blockX();
        double z = context.blockZ();
        return valueAt(Math.sqrt(x * x + z * z));
    }

    double valueAt(double distance) {
        if (distance <= innerRadius) {
            return 1.0;
        }
        if (distance >= outerRadius) {
            return 0.0;
        }
        return 1.0 - RingBlend.smoothstep((distance - innerRadius) / (outerRadius - innerRadius));
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
