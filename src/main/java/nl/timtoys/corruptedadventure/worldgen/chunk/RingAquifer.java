package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import javax.annotation.Nullable;

/**
 * Keeps open space in the void zone empty. {@link RingFluidPicker} only covers the global sea: vanilla's noise-based
 * aquifer also places local fluid levels of its own, and below Y -10 it turns them into lava, which would hang as
 * lava sheets under the floating islands.
 * <p>
 * One instance per noise chunk, so the per-call state below is single-threaded like vanilla's.
 */
final class RingAquifer implements Aquifer {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ResourceKey<Level> dimension;
    private final Aquifer vanilla;
    private boolean suppressed;

    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla) {
        this.dimension = dimension;
        this.vanilla = vanilla;
    }

    @Nullable
    @Override
    public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
        return computeSubstance(RingColumnCache.ringAt(dimension, context.blockX(), context.blockZ()), context, substance);
    }

    @Nullable
    BlockState computeSubstance(RingPosition position, DensityFunction.FunctionContext context, double substance) {
        suppressed = substance <= 0.0 && RingFluidPicker.noFluid(position);
        return suppressed ? AIR : vanilla.computeSubstance(context, substance);
    }

    @Override
    public boolean shouldScheduleFluidUpdate() {
        return !suppressed && vanilla.shouldScheduleFluidUpdate();
    }
}
