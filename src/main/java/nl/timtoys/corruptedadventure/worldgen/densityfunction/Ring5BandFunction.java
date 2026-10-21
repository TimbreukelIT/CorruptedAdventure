package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import java.util.function.Supplier;

/**
 * {@code corruptedadventure:ring_5_band}: the band axis of overworld ring 5, −1 inner (dead) to 1 outer (gone)
 * ({@link Ring5Band#value}). No parameters: the band's numbers live in {@link Ring5Band} only, so the void meter, the
 * sky and the fluid rule, which ask it directly, see the same line.
 */
public final class Ring5BandFunction implements DensityFunction.SimpleFunction {
    public static final MapCodec<Ring5BandFunction> MAP_CODEC = MapCodec.unit(() -> new Ring5BandFunction(RingLayout::current));

    public static final KeyDispatchDataCodec<Ring5BandFunction> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final Supplier<RingLayout> layout;

    /** @param layout the ring layout (the running server's, {@link RingLayout#current()}; tests give their own) */
    Ring5BandFunction(Supplier<RingLayout> layout) {
        this.layout = layout;
    }

    @Override
    public double compute(FunctionContext context) {
        return Ring5Band.of(layout.get()).value(context.blockX(), context.blockZ());
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
