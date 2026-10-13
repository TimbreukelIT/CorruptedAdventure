package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import java.util.function.Supplier;

/**
 * {@code corruptedadventure:ring_4_band}: the band axis of overworld ring 4, −1 mythical to 1 blighted
 * ({@link Ring4Band#value}). No parameters: the band's numbers live in {@link Ring4Band} only, so the weather and the
 * poison meter, which ask it directly, see the same line.
 */
public final class Ring4BandFunction implements DensityFunction.SimpleFunction {
    public static final MapCodec<Ring4BandFunction> MAP_CODEC = MapCodec.unit(() -> new Ring4BandFunction(RingLayout::current));

    public static final KeyDispatchDataCodec<Ring4BandFunction> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final Supplier<RingLayout> layout;

    /** @param layout the ring layout (the running server's, {@link RingLayout#current()}; tests give their own) */
    Ring4BandFunction(Supplier<RingLayout> layout) {
        this.layout = layout;
    }

    @Override
    public double compute(FunctionContext context) {
        return Ring4Band.of(layout.get()).value(context.blockX(), context.blockZ());
    }

    @Override
    public double minValue() {
        return -1.0;
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
