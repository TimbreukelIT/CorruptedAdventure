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
 * Applies the fluid rules of {@link RingFluidPicker} to the local fluids of vanilla's noise-based aquifer, which the
 * global picker does not cover. Void zone: open space stays empty (below Y -10 the aquifer would hang lava sheets under
 * the floating islands). Overworld ring 2: local lava becomes water ({@link RingFluidPicker#waterOnly}), with vanilla's
 * fluid update flag (set near cell borders, where the fluid may flow; inside a pool a tick would do nothing). Right
 * above vanilla's lava layer (Y −54) any aquifer fluid stays solid instead: one block of stone between the water and
 * the lava below, like vanilla's barriers (vanilla itself lets water cells touch the layer, and the contact turns into
 * an obsidian crust). Carvers fill their caves through this aquifer too.
 * <p>
 * One instance per noise chunk, so the per-call state below is single-threaded like vanilla's.
 */
final class RingAquifer implements Aquifer {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();

    private final ResourceKey<Level> dimension;
    private final Aquifer vanilla;
    private boolean suppressed;
    private boolean lid;

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
        lid = false;
        if (suppressed) {
            return AIR;
        }
        BlockState state = vanilla.computeSubstance(context, substance);
        if (state == null || !(state.is(Blocks.LAVA) || state.is(Blocks.WATER))
                || !RingFluidPicker.waterOnly(dimension, position, context.blockY())) {
            return state;
        }
        if (context.blockY() == RingFluidPicker.LAVA_LAYER_TOP) {
            lid = true;
            return null;
        }
        return WATER;
    }

    @Override
    public boolean shouldScheduleFluidUpdate() {
        return !suppressed && !lid && vanilla.shouldScheduleFluidUpdate();
    }
}
