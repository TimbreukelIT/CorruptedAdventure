package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import javax.annotation.Nullable;

/**
 * Applies the fluid rules of {@link RingFluidPicker} to the local fluids of vanilla's noise-based aquifer, which the
 * global picker does not cover. Void zone: open space stays empty (below Y -10 the aquifer would hang lava sheets under
 * the floating islands). Overworld ring 2: local lava becomes water ({@link RingFluidPicker#waterOnly}), with vanilla's
 * fluid update flag (set near cell borders, where the fluid may flow; inside a pool a tick would do nothing). Right
 * above vanilla's lava layer (Y −54) any aquifer fluid stays solid instead: one block of stone between the water and
 * the lava below, like vanilla's barriers (vanilla itself lets water cells touch the layer, and the contact turns into
 * an obsidian crust). Overworld ring 3: lava in the lava lakes and channels of the {@link VolcanicField}
 * ({@link RingFluidPicker#ring3Lava}, no fluid update: still lava), no water from Y 0 up
 * ({@link RingFluidPicker#ring3Dry}), and rock below ring 2's sea level in its half of the zone 2→3
 * ({@link RingFluidPicker#ring3Seal}). Carvers fill their caves through this aquifer too.
 * <p>
 * One instance per noise chunk, so the per-call state below is single-threaded like vanilla's.
 */
final class RingAquifer implements Aquifer {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
    /**
     * How far the real surface can lie below the field's exact height: the terrain is interpolated between 4-block grid
     * values ({@link #SURFACE_MARGIN}), and the crags of ring 3 ({@code ring_3/overworld/terrain}: 0.3 * roughness * noise
     * in density, 1/8 per block; the noise reaches about ±1.5) take off up to {@link #CRAG_MARGIN} * roughness more.
     */
    private static final double SURFACE_MARGIN = 1.5;
    private static final double CRAG_MARGIN = 4.0;

    private final ResourceKey<Level> dimension;
    private final Aquifer vanilla;
    @Nullable
    private final VolcanicField volcanic;
    private boolean suppressed;
    private boolean lid;

    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla) {
        this(dimension, vanilla, null);
    }

    /** @param volcanic the ring 3 terrain of this noise chunk (the same instance its density uses), or null */
    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla, @Nullable VolcanicField volcanic) {
        this.dimension = dimension;
        this.vanilla = vanilla;
        this.volcanic = volcanic;
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
        if (substance <= 0.0 && RingFluidPicker.ring3Seal(dimension, position, context.blockY())) {
            suppressed = true;
            return null;
        }
        if (substance <= 0.0 && RingFluidPicker.ring3Lava(dimension, position, volcanic, context.blockX(), context.blockY(), context.blockZ())) {
            suppressed = true;
            return exposed(context.blockX(), context.blockY(), context.blockZ()) ? null : LAVA;
        }
        BlockState state = vanilla.computeSubstance(context, substance);
        if (state != null && state.is(Blocks.WATER) && RingFluidPicker.ring3Dry(dimension, position, context.blockY())) {
            suppressed = true;
            return AIR;
        }
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

    /**
     * Whether lava at this block may stand against open air beside it: a neighbour column without lava at this height
     * whose surface lies below it (the next, one block lower pool of a channel, or the shallow end of a channel). The
     * surface is the field's height minus a margin for the crags on steep crater walls (up to ~3 blocks). Such a block
     * becomes rock instead (a lip of cooled crust), so no lava flows when the chunk is first updated.
     */
    private boolean exposed(int x, int y, int z) {
        return open(x + 1, y, z) || open(x - 1, y, z) || open(x, y, z + 1) || open(x, y, z - 1);
    }

    private boolean open(int x, int y, int z) {
        VolcanicField.Column neighbour = volcanic.column(x, z);
        return !neighbour.lavaAt(y) && neighbour.height() - SURFACE_MARGIN - CRAG_MARGIN * neighbour.roughness() < y + 1.0;
    }

    @Override
    public boolean shouldScheduleFluidUpdate() {
        return !suppressed && !lid && vanilla.shouldScheduleFluidUpdate();
    }
}
