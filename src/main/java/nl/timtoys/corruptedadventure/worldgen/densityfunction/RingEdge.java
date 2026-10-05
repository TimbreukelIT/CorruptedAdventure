package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;

/**
 * {@code corruptedadventure:ring_edge}: 1 on and outside the (noisy) boundaries of {@code ring}, falling with a smoothstep
 * to 0 at {@code width} blocks inside the ring (measured to the nearest of its two boundaries). A 2D function: lets a
 * ring's terrain build a bank along its borders, where neighbouring rings have another fluid level.
 */
public record RingEdge(ResourceKey<Level> dimension, Ring ring, double width) implements DensityFunction.SimpleFunction {
    public static final MapCodec<RingEdge> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(RingEdge::dimension),
            Ring.CODEC.validate(r -> r == Ring.VOID ? com.mojang.serialization.DataResult.error(() -> "ring_edge: the void has no outer boundary") : com.mojang.serialization.DataResult.success(r)).fieldOf("ring").forGetter(RingEdge::ring),
            Codec.doubleRange(1.0, 4096.0).fieldOf("width").forGetter(RingEdge::width)
    ).apply(instance, RingEdge::new));

    public static final KeyDispatchDataCodec<RingEdge> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** The value at {@code depth} blocks inside the ring (negative outside): 1 at depth ≤ 0, 0 at depth ≥ {@code width}. */
    static double value(double depth, double width) {
        if (depth <= 0.0) {
            return 1.0;
        }
        if (depth >= width) {
            return 0.0;
        }
        return 1.0 - RingBlend.smoothstep(depth / width);
    }

    @Override
    public double compute(FunctionContext context) {
        return value(RingColumnCache.boundaryDepth(dimension, ring, context.blockX(), context.blockZ()), width);
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
