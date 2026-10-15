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
 * Optional {@code mix}, keyed the same way, a share of the zone in (0, 1] (verdant_falls: {@code ring_3} =
 * {@link IslandField#VERDANT_EDGE_SHARE}): the two rings mix only over that first share of the zone, with the smoothstep
 * stretched over it, and from there on the outer ring's input stands alone. So the outer ring's terrain covers the rest
 * of the zone by itself, and exact shapes there never mix with the inner ring's density. Without it the whole zone
 * mixes; a boundary cannot both switch and mix.
 * <p>
 * Only the one or two inputs a position needs are computed. Interpolation markers inside the inputs still work
 * because {@link #mapAll} passes the visitor on; the noise chunk then fills those interpolators and flat caches in
 * every chunk, whether or not the ring is nearby, so {@link #mapAll} puts a gate inside each of them first: a
 * {@link RingGate} in an interpolator, a {@link RingFillGate} in a flat cache (39e).
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

    private static final Codec<Map<Ring, Double>> MIX_CODEC =
            Codec.unboundedMap(Ring.CODEC, Codec.DOUBLE).validate(RingBlend::validateMix);

    public static final MapCodec<RingBlend> MAP_CODEC = RecordCodecBuilder.<RingBlend>mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(blend -> blend.dimension),
            RINGS_CODEC.fieldOf("rings").forGetter(RingBlend::ringMap),
            SWITCH_AT_CODEC.optionalFieldOf("switch_at", Set.of()).forGetter(blend -> blend.switchAt),
            MIX_CODEC.optionalFieldOf("mix", Map.of()).forGetter(blend -> blend.mixAt)
    ).apply(instance, (dimension, rings, switchAt, mix) -> new RingBlend(dimension, inputs(rings), switchAt, mix)))
            .validate(blend -> validateModes(blend.switchAt, blend.mixAt).map(valid -> blend));

    public static final KeyDispatchDataCodec<RingBlend> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final ResourceKey<Level> dimension;
    // Indexed by Ring ordinal.
    private final DensityFunction[] rings;
    // For the codec; the per-block path reads switches, derived from it in the constructor.
    private final Set<Ring> switchAt;
    private final boolean[] switches;
    // For the codec; the per-block path reads mix (the share of each boundary's zone over which it mixes, indexed by the
    // inner ring's ordinal; 1 = the whole zone), derived from it in the constructor.
    private final Map<Ring, Double> mixAt;
    private final double[] mix;

    public RingBlend(ResourceKey<Level> dimension, Map<Ring, DensityFunction> rings) {
        this(dimension, rings, Set.of());
    }

    public RingBlend(ResourceKey<Level> dimension, Map<Ring, DensityFunction> rings, Set<Ring> switchAt) {
        this(dimension, rings, switchAt, Map.of());
    }

    public RingBlend(ResourceKey<Level> dimension, Map<Ring, DensityFunction> rings, Set<Ring> switchAt, Map<Ring, Double> mix) {
        this(dimension, inputs(rings),
                validateRings(rings).flatMap(valid -> validateSwitchAt(switchAt)).flatMap(valid -> validateMix(mix))
                        .flatMap(valid -> validateModes(switchAt, mix)).getOrThrow(IllegalArgumentException::new),
                mix);
    }

    private RingBlend(ResourceKey<Level> dimension, DensityFunction[] rings, Set<Ring> switchAt, Map<Ring, Double> mix) {
        this.dimension = dimension;
        this.rings = rings;
        this.switchAt = switchAt.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(switchAt));
        this.switches = new boolean[Ring.values().length];
        for (Ring ring : this.switchAt) {
            switches[ring.ordinal()] = true;
        }
        this.mixAt = mix.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(mix));
        this.mix = new double[Ring.values().length];
        Arrays.fill(this.mix, 1.0);
        this.mixAt.forEach((ring, share) -> this.mix[ring.ordinal()] = share);
    }

    private static DensityFunction[] inputs(Map<Ring, DensityFunction> rings) {
        return Arrays.stream(Ring.values()).map(rings::get).toArray(DensityFunction[]::new);
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

    static DataResult<Map<Ring, Double>> validateMix(Map<Ring, Double> mix) {
        if (mix.containsKey(Ring.VOID)) {
            return DataResult.error(() -> "Ring blend mixes at void, but there is no boundary beyond the void");
        }
        return mix.values().stream().allMatch(share -> share > 0.0 && share <= 1.0)
                ? DataResult.success(mix)
                : DataResult.error(() -> "Ring blend mix shares must lie in (0, 1]: " + mix);
    }

    static DataResult<Set<Ring>> validateModes(Set<Ring> switchAt, Map<Ring, Double> mix) {
        return switchAt.stream().anyMatch(mix::containsKey)
                ? DataResult.error(() -> "Ring blend both switches and mixes at one boundary: switch_at " + switchAt + ", mix " + mix.keySet())
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
        Ring single = single(position);
        if (single != null) {
            return rings[single.ordinal()].compute(context);
        }
        double inner = rings[position.inner().ordinal()].compute(context);
        double outer = rings[position.outer().ordinal()].compute(context);
        return Mth.lerp(smoothstep(position.blend() / mix[position.inner().ordinal()]), inner, outer);
    }

    /** The one ring whose input gives the value at {@code position}, or null where two are mixed. */
    private Ring single(RingPosition position) {
        if (!position.inTransition() || switches[position.inner().ordinal()]) {
            return position.ring();
        }
        // Only a share below 1 ends the mix early: with the whole zone mixing, its outer edge keeps the lerp it always had.
        double share = mix[position.inner().ordinal()];
        return share < 1.0 && position.blend() >= share ? position.outer() : null;
    }

    /**
     * Usually all points of an array (a slice of cell corners or the blocks of a cell) take one ring's input; then that
     * input fills the array itself, which lets its caches and interpolators work per array instead of per point.
     */
    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        Ring common = null;
        // One ring lookup per distinct column (39f): a slice of cell corners runs along Y first, the blocks of a cell
        // revisit their 16 columns for every Y. The columns seen so far, packed; past SEEN columns lookups repeat.
        long[] seen = SEEN_COLUMNS.get();
        int seenCount = 0;
        for (int i = 0; i < array.length; i++) {
            FunctionContext context = contextProvider.forIndex(i);
            int x = context.blockX();
            int z = context.blockZ();
            long key = (long) x << 32 | z & 0xFFFFFFFFL;
            if (common != null && known(seen, seenCount, key)) {
                continue;
            }
            Ring single = single(RingColumnCache.ringAt(dimension, x, z));
            if (single == null || (common != null && single != common)) {
                contextProvider.fillAllDirectly(array, this);
                return;
            }
            common = single;
            if (seenCount < seen.length) {
                seen[seenCount++] = key;
            }
        }
        if (common != null) {
            rings[common.ordinal()].fillArray(array, contextProvider);
        }
    }

    /** Distinct columns remembered per {@link #fillArray} (a cell has 16, a slice of corners 25). */
    private static final int SEEN = 32;
    private static final ThreadLocal<long[]> SEEN_COLUMNS = ThreadLocal.withInitial(() -> new long[SEEN]);

    private static boolean known(long[] seen, int count, long key) {
        // Most often the column of the previous point: search from the end.
        for (int k = count - 1; k >= 0; k--) {
            if (seen[k] == key) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        DensityFunction[] mapped = new DensityFunction[rings.length];
        for (Ring ring : Ring.values()) {
            mapped[ring.ordinal()] = rings[ring.ordinal()].mapAll(gating(ring, visitor));
        }
        return visitor.apply(new RingBlend(dimension, mapped, switchAt, mixAt));
    }

    /**
     * {@code visitor}, but every interpolated marker of the input of {@code ring} gets a {@link RingGate} first and every
     * flat-cache marker a {@link RingFillGate} ({@link RingGate#gateCacheMarker}), both knowing how far into its outer
     * zone the ring mixes ({@code mix}): the ring's input is not computed where it has no weight.
     */
    private Visitor gating(Ring ring, Visitor visitor) {
        double share = mix[ring.ordinal()];
        return new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return visitor.apply(RingGate.gateCacheMarker(dimension, ring, share, function));
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
