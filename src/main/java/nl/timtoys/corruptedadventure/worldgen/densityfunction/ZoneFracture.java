package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * {@code corruptedadventure:zone_fracture}: the fracture zone 4→5 (ring 5, V7/V8), wrapped around the overworld's
 * {@code noise_router.final_density}, so it cuts after the ring blend and ring 4 and ring 5 see the same fracture
 * (cracks become air, raised floes and loose chunks extra rock). One owner (W-terrein) for the zone's shape.
 * <p>
 * Golf 0 (82) is the skeleton: {@link #strength} is 0 everywhere, so the wrapper is exactly {@link #argument}
 * everywhere (bit-equal; {@code compute} and {@code fillArray} pass straight through). Package 85 fills in the shape:
 * outside the zone 4→5 (± 16 blocks) and at strength 0 it stays exactly the argument.
 * <p>
 * {@link #mapAll} hands the visitor on to the argument, so the ring blend's gates and the fields found through the
 * router ({@code IslandField.find}, {@code Ring5Field.find}) stay where they were.
 */
public record ZoneFracture(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<ZoneFracture> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(ZoneFracture::argument)
    ).apply(instance, ZoneFracture::new));

    public static final KeyDispatchDataCodec<ZoneFracture> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /**
     * How strongly the zone is fractured at an effective distance ({@link RingLayout#effectiveDistance}), 0..1: 0 outside
     * the zone 4→5. Golf 0: 0 everywhere (85).
     */
    public static double strength(RingLayout layout, double effectiveDistance) {
        return 0.0;
    }

    /** Whether the block is cut open by a crack of the fracture zone (air after the blend). Golf 0: never (85). */
    public static boolean crackAt(RingLayout layout, int x, int y, int z) {
        return false;
    }

    /** Whether the block is extra rock of a raised floe or loose chunk of the fracture zone. Golf 0: never (85). */
    public static boolean floeAt(RingLayout layout, int x, int y, int z) {
        return false;
    }

    @Override
    public double compute(FunctionContext context) {
        return argument.compute(context);
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        argument.fillArray(array, contextProvider);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ZoneFracture(argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return argument.minValue();
    }

    @Override
    public double maxValue() {
        return argument.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
