package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:zone_fracture_value}: the column value of the fracture zone 4→5 (85), the density function
 * {@code zone_4_5/overworld/fracture}: 1 a crack, 0.75 a loose chunk over the ground, 0.5 a raised floe or landmark, 0
 * whole ground and everywhere outside the zone. The same shape as {@link ZoneFracture} (same hash noise, same salt);
 * 2D, for the biomes, the atmosphere and ring 4's {@code sundered} key.
 */
public final class ZoneFractureValue implements DensityFunction {
    public static final MapCodec<ZoneFractureValue> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("hash").forGetter(f -> f.hash)
    ).apply(instance, hash -> new ZoneFractureValue(hash, RingLayout::current)));

    public static final KeyDispatchDataCodec<ZoneFractureValue> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    static final double CRACK = 1.0;
    static final double CHUNK = 0.75;
    static final double FLOE = 0.5;

    private final NoiseHolder hash;
    private final Supplier<RingLayout> layout;
    private volatile ZoneFractureShape shape;
    private final ThreadOwned<Scratch> scratch = new ThreadOwned<>(Scratch::new);

    ZoneFractureValue(NoiseHolder hash, Supplier<RingLayout> layout) {
        this.hash = hash;
        this.layout = layout;
    }

    private ZoneFractureShape shape() {
        RingLayout current = layout.get();
        ZoneFractureShape known = shape;
        if (known == null || known.layout() != current) {
            known = new ZoneFractureShape(current, SeededHash.hash(hash, 4321, -6789, ZoneFracture.WORLD_SALT));
            shape = known;
        }
        return known;
    }

    /** The value of column x, z. */
    double value(int x, int z) {
        Scratch s = scratch.get();
        shape().fill(s.info, x, z, s.landmark);
        if (s.info.strength <= 0.0) {
            return 0.0;
        }
        if (s.info.kind == ZoneFractureShape.CRACK) {
            return CRACK;
        }
        if (!Double.isNaN(s.info.chunkAbove)) {
            return CHUNK;
        }
        return s.info.landmark >= 0 || !Double.isNaN(s.info.floeLift) && s.info.floeLift > 0.0 ? FLOE : 0.0;
    }

    @Override
    public double compute(FunctionContext context) {
        return value(context.blockX(), context.blockZ());
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ZoneFractureValue(visitor.visitNoise(hash), layout));
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

    @Override
    public boolean equals(Object o) {
        return o instanceof ZoneFractureValue v && hash.equals(v.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    private static final class Scratch {
        final ZoneFractureShape.Info info = new ZoneFractureShape.Info();
        final double[] landmark = new double[4];
    }
}
