package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * {@code corruptedadventure:void_edge}: 0 on the outer boundary of ring 5, rising to 1 at the world border
 * ({@link RingLayout#voidEdgeProgress}). Lets the void input thin out its floating islands in JSON.
 */
public record VoidEdge(ResourceKey<Level> dimension) implements DensityFunction.SimpleFunction {
    public static final MapCodec<VoidEdge> MAP_CODEC =
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").xmap(VoidEdge::new, VoidEdge::dimension);

    public static final KeyDispatchDataCodec<VoidEdge> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return RingColumnCache.voidEdgeProgress(dimension, context.blockX(), context.blockZ());
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
