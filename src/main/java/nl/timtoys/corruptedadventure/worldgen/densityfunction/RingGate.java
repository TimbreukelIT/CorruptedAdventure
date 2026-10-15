package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;

import java.util.Arrays;

/**
 * {@code corruptedadventure:ring_gate}: {@code argument} where {@code ring} can have weight (inside the ring, in a
 * transition zone with it, or within {@link #MARGIN} blocks of that), {@code 0} elsewhere.
 * <p>
 * {@link RingBlend#mapAll} puts one inside every {@code interpolated} marker of each ring input. The noise chunk fills
 * every interpolator in every chunk, also of rings far away; behind the gate those cost one column lookup instead
 * of the ring's 3D terrain. {@code flat_cache} markers get a {@link RingFillGate} instead (change 39e), which only acts
 * while the noise chunk fills them. The margin keeps every cell corner (4 blocks apart) of a cell where the ring has
 * weight computed for real; it is 16 rather than 4 because the effective distance can locally run steeper than one
 * block per block, and grows with a stronger boundary noise
 * ({@link nl.timtoys.corruptedadventure.ring.RingLayout#reachMargin}). Code that resolves a density function through its
 * markers (such as {@code CellEdges#resolve}) looks through the gate as well ({@link RingGated#unwrap}, 39f).
 * <p>
 * {@code mix} is the share of the ring's outer zone over which the blend mixes it ({@link RingBlend}'s {@code mix},
 * verdant_falls: ring 3 only over the {@code steam_fens} rim): beyond it the ring has no weight, so the reach ends there
 * plus the margin, and the noise chunk skips the ring's terrain over the rest of the zone. 1: the whole zone.
 */
public record RingGate(ResourceKey<Level> dimension, Ring ring, DensityFunction argument, double mix) implements DensityFunction, RingGated {
    /** Blocks of the dimension, so cells of 4 blocks with room to spare. */
    static final double MARGIN = 16.0;

    public static final MapCodec<RingGate> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(RingGate::dimension),
            Ring.CODEC.fieldOf("ring").forGetter(RingGate::ring),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(RingGate::argument),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("mix", 1.0).forGetter(RingGate::mix)
    ).apply(instance, RingGate::new));

    public static final KeyDispatchDataCodec<RingGate> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    // Vanilla's marker types are not public; every marker of a type (also the noise chunk's own interpolator and flat
    // cache) shares its type's codec instance.
    private static final KeyDispatchDataCodec<? extends DensityFunction> INTERPOLATED =
            DensityFunctions.interpolated(DensityFunctions.zero()).codec();
    private static final KeyDispatchDataCodec<? extends DensityFunction> FLAT_CACHE =
            DensityFunctions.flatCache(DensityFunctions.zero()).codec();

    /** A gate over the ring's whole outer zone. */
    public RingGate(ResourceKey<Level> dimension, Ring ring, DensityFunction argument) {
        this(dimension, ring, argument, 1.0);
    }

    static DensityFunction gateCacheMarker(ResourceKey<Level> dimension, Ring ring, DensityFunction function) {
        return gateCacheMarker(dimension, ring, 1.0, function);
    }

    /**
     * {@code function} itself, or, if it is an {@code interpolated} marker (a gate) or a {@code flat_cache} marker (a
     * {@link RingFillGate}) whose argument is not gated yet, the same marker around its gate. Idempotent: {@code mapAll}
     * runs both when the random state wires the noises and again in every noise chunk. {@code mix}: the ring's share of
     * its outer zone ({@link RingGate}).
     */
    static DensityFunction gateCacheMarker(ResourceKey<Level> dimension, Ring ring, double mix, DensityFunction function) {
        if (function instanceof DensityFunctions.MarkerOrMarked marker
                && !(marker.wrapped() instanceof RingGate || marker.wrapped() instanceof RingFillGate)) {
            if (marker.codec() == INTERPOLATED) {
                return DensityFunctions.interpolated(new RingGate(dimension, ring, marker.wrapped(), mix));
            }
            if (marker.codec() == FLAT_CACHE) {
                return DensityFunctions.flatCache(new RingFillGate(dimension, ring, marker.wrapped(), mix));
            }
        }
        return function;
    }

    @Override
    public double compute(FunctionContext context) {
        return compute(context, reaches(context));
    }

    double compute(FunctionContext context, boolean reaches) {
        return reaches ? argument.compute(context) : 0.0;
    }

    private boolean reaches(FunctionContext context) {
        return RingColumnCache.reaches(dimension, ring, context.blockX(), context.blockZ(), MARGIN, mix);
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        // Interpolators fill a slice of cell corners at a time; usually all or none of them are in reach.
        boolean any = false;
        boolean all = true;
        for (int i = 0; i < array.length && (all || !any); i++) {
            boolean reaches = reaches(contextProvider.forIndex(i));
            any |= reaches;
            all &= reaches;
        }
        if (all) {
            argument.fillArray(array, contextProvider);
        } else if (!any) {
            Arrays.fill(array, 0.0);
        } else {
            contextProvider.fillAllDirectly(array, this);
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new RingGate(dimension, ring, argument.mapAll(visitor), mix));
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
}
