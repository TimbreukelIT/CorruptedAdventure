package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * {@code corruptedadventure:ring_blend}: the terrain of the ring a position lies in, one density function per ring
 * (plus the void edge). In a transition zone it mixes the two neighbouring rings with a smoothstep of the linear
 * {@link RingPosition#blend()}, so the terrain has no kink where the zone starts or ends.
 * <p>
 * Only the one or two inputs a position needs are computed. Interpolation markers inside the inputs still work
 * because {@link #mapAll} passes the visitor on, but note that the noise chunk then fills those interpolators for
 * every chunk, whether or not the ring is nearby.
 */
public final class RingBlend implements DensityFunction {
    private static final Codec<Map<Ring, DensityFunction>> RINGS_CODEC =
            Codec.unboundedMap(Ring.CODEC, DensityFunction.HOLDER_HELPER_CODEC).validate(RingBlend::validateRings);

    public static final MapCodec<RingBlend> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(blend -> blend.dimension),
            RINGS_CODEC.fieldOf("rings").forGetter(RingBlend::ringMap)
    ).apply(instance, RingBlend::new));

    public static final KeyDispatchDataCodec<RingBlend> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final ResourceKey<Level> dimension;
    // Indexed by Ring ordinal.
    private final DensityFunction[] rings;

    public RingBlend(ResourceKey<Level> dimension, Map<Ring, DensityFunction> rings) {
        this.dimension = dimension;
        this.rings = Arrays.stream(Ring.values()).map(rings::get).toArray(DensityFunction[]::new);
    }

    private RingBlend(ResourceKey<Level> dimension, DensityFunction[] rings) {
        this.dimension = dimension;
        this.rings = rings;
    }

    static DataResult<Map<Ring, DensityFunction>> validateRings(Map<Ring, DensityFunction> rings) {
        Ring[] missing = Arrays.stream(Ring.values()).filter(ring -> !rings.containsKey(ring)).toArray(Ring[]::new);
        return missing.length == 0
                ? DataResult.success(rings)
                : DataResult.error(() -> "Ring blend is missing rings: " + Arrays.toString(missing));
    }

    private Map<Ring, DensityFunction> ringMap() {
        Map<Ring, DensityFunction> map = new EnumMap<>(Ring.class);
        for (Ring ring : Ring.values()) {
            map.put(ring, rings[ring.ordinal()]);
        }
        return map;
    }

    /** Classic smoothstep 3t² − 2t³: flat at both ends of the zone, 1.5× the linear slope in the middle. */
    static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    @Override
    public double compute(FunctionContext context) {
        return compute(context, RingColumnCache.ringAt(dimension, context.blockX(), context.blockZ()));
    }

    double compute(FunctionContext context, RingPosition position) {
        double inner = rings[position.inner().ordinal()].compute(context);
        if (!position.inTransition()) {
            return inner;
        }
        double outer = rings[position.outer().ordinal()].compute(context);
        return Mth.lerp(smoothstep(position.blend()), inner, outer);
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        DensityFunction[] mapped = new DensityFunction[rings.length];
        for (int i = 0; i < rings.length; i++) {
            mapped[i] = rings[i].mapAll(visitor);
        }
        return visitor.apply(new RingBlend(dimension, mapped));
    }

    @Override
    public double minValue() {
        return Arrays.stream(rings).mapToDouble(DensityFunction::minValue).min().orElseThrow();
    }

    @Override
    public double maxValue() {
        return Arrays.stream(rings).mapToDouble(DensityFunction::maxValue).max().orElseThrow();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
