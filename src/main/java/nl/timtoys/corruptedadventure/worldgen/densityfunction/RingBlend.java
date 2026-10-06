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
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code corruptedadventure:ring_blend}: the terrain of the ring a position lies in, one density function per ring
 * (plus the void edge). In a transition zone it mixes the two neighbouring rings with a smoothstep of the linear
 * {@link RingPosition#blend()}, so the terrain has no kink where the zone starts or ends.
 * <p>
 * Optional {@code switch_at}, keyed by the ring on the inside of a boundary like the biome source's transitions
 * ({@code ring_3} = boundary 3→4): in those zones it does not mix but takes the input of {@link RingPosition#ring()},
 * switching hard in the middle. For router climate at boundaries without a transition biome: {@code RingBiomeSource}
 * picks each side's source on that same rule, so each side reads its own region axis (the one its terrain follows)
 * instead of a mix with the neighbour's.
 * <p>
 * Only the one or two inputs a position needs are computed. Interpolation markers inside the inputs still work
 * because {@link #mapAll} passes the visitor on; the noise chunk then fills those interpolators in every chunk,
 * whether or not the ring is nearby, so {@link #mapAll} puts a {@link RingGate} inside each of them first.
 */
public final class RingBlend implements DensityFunction {
    private static final Codec<Map<Ring, DensityFunction>> RINGS_CODEC =
            Codec.unboundedMap(Ring.CODEC, DensityFunction.HOLDER_HELPER_CODEC).validate(RingBlend::validateRings);

    private static final Codec<Set<Ring>> SWITCH_AT_CODEC = Ring.CODEC.listOf()
            .validate(list -> list.size() == Set.copyOf(list).size()
                    ? DataResult.success(list)
                    : DataResult.error(() -> "Ring blend lists a boundary twice in switch_at: " + list))
            .<Set<Ring>>xmap(list -> list.isEmpty() ? Set.of() : EnumSet.copyOf(list), List::copyOf)
            .validate(RingBlend::validateSwitchAt);

    public static final MapCodec<RingBlend> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(blend -> blend.dimension),
            RINGS_CODEC.fieldOf("rings").forGetter(RingBlend::ringMap),
            SWITCH_AT_CODEC.optionalFieldOf("switch_at", Set.of()).forGetter(blend -> blend.switchAt)
    ).apply(instance, RingBlend::new));

    public static final KeyDispatchDataCodec<RingBlend> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final ResourceKey<Level> dimension;
    // Indexed by Ring ordinal.
    private final DensityFunction[] rings;
    private final Set<Ring> switchAt;

    public RingBlend(ResourceKey<Level> dimension, Map<Ring, DensityFunction> rings) {
        this(dimension, rings, Set.of());
    }

    public RingBlend(ResourceKey<Level> dimension, Map<Ring, DensityFunction> rings, Set<Ring> switchAt) {
        this(dimension, Arrays.stream(Ring.values()).map(rings::get).toArray(DensityFunction[]::new),
                validateRings(rings).flatMap(valid -> validateSwitchAt(switchAt)).getOrThrow(IllegalArgumentException::new));
    }

    private RingBlend(ResourceKey<Level> dimension, DensityFunction[] rings, Set<Ring> switchAt) {
        this.dimension = dimension;
        this.rings = rings;
        this.switchAt = switchAt.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(switchAt));
    }

    static DataResult<Map<Ring, DensityFunction>> validateRings(Map<Ring, DensityFunction> rings) {
        Ring[] missing = Arrays.stream(Ring.values()).filter(ring -> !rings.containsKey(ring)).toArray(Ring[]::new);
        return missing.length == 0
                ? DataResult.success(rings)
                : DataResult.error(() -> "Ring blend is missing rings: " + Arrays.toString(missing));
    }

    static DataResult<Set<Ring>> validateSwitchAt(Set<Ring> switchAt) {
        return switchAt.contains(Ring.VOID)
                ? DataResult.error(() -> "Ring blend switches at void, but there is no boundary beyond the void")
                : DataResult.success(switchAt);
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
        if (!position.inTransition() || switchAt.contains(position.inner())) {
            return rings[position.ring().ordinal()].compute(context);
        }
        double inner = rings[position.inner().ordinal()].compute(context);
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
        for (Ring ring : Ring.values()) {
            mapped[ring.ordinal()] = rings[ring.ordinal()].mapAll(gating(ring, visitor));
        }
        return visitor.apply(new RingBlend(dimension, mapped, switchAt));
    }

    /** {@code visitor}, but every interpolated marker of the input of {@code ring} gets a {@link RingGate} first. */
    private Visitor gating(Ring ring, Visitor visitor) {
        return new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return visitor.apply(RingGate.gateInterpolated(dimension, ring, function));
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                return visitor.visitNoise(noise);
            }
        };
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
