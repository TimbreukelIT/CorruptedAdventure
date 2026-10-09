package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
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
 * the floating islands). Overworld ring 2: local lava becomes water ({@link RingFluidPicker#waterOnlyFrom}), with vanilla's
 * fluid update flag (set near cell borders, where the fluid may flow; inside a pool a tick would do nothing). Right
 * above vanilla's lava layer (Y −54) any aquifer fluid stays solid instead: one block of stone between the water and
 * the lava below, like vanilla's barriers (vanilla itself lets water cells touch the layer, and the contact turns into
 * an obsidian crust). Overworld ring 3: lava in the lava lakes and channels of the {@link VolcanicField}
 * ({@link RingFluidPicker#ring3LavaColumn}, no fluid update: still lava), no water from Y 0 up
 * ({@link RingFluidPicker#ring3DryFrom}), and rock below ring 2's sea level in its half of the zone 2→3
 * ({@link RingFluidPicker#ring3SealBelow}). Carvers fill their caves through this aquifer too.
 * <p>
 * One instance per noise chunk, so the per-call state below is single-threaded like vanilla's.
 */
final class RingAquifer implements Aquifer {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
    /** Falling lava (a lava fall, 39f): level 8 is a falling flow, which stays put under lava and spreads where it lands. */
    private static final BlockState FALLING_LAVA = LAVA.setValue(LiquidBlock.LEVEL, 8);
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
    private boolean spring;

    // The rules per column of the chunk (RingFluidPicker's per-column bounds), so the per-block path only compares Y
    // and never branches on the ring itself (one ring check per block made C2 deoptimize there over and over, change
    // 39e). Direct-mapped on the low 4 bits of x and z: the noise chunk changes column with every block (y → x → z
    // inside a cell), and the carvers stay inside the chunk too. The last slot is for the tests' explicit positions.
    private static final int COLUMNS = 16 * 16;
    private final boolean[] known = new boolean[COLUMNS + 1];
    private final int[] keyX = new int[COLUMNS + 1];
    private final int[] keyZ = new int[COLUMNS + 1];
    private final boolean[] noFluid = new boolean[COLUMNS + 1];
    private final int[] sealBelow = new int[COLUMNS + 1];
    // The column's lava range [lavaBottom, lavaTop) when its lava counts, else lavaTop = Integer.MIN_VALUE (no lava at
    // any Y); springY = the Y of its lava spring (39d), else Integer.MIN_VALUE.
    private final int[] lavaTop = new int[COLUMNS + 1];
    private final int[] lavaBottom = new int[COLUMNS + 1];
    private final int[] springY = new int[COLUMNS + 1];
    // The column's lava fall (39f): falling lava in [fallBottom, fallTop], let flow at and below fallFlow.
    private final int[] fallTop = new int[COLUMNS + 1];
    private final int[] fallBottom = new int[COLUMNS + 1];
    private final int[] fallFlow = new int[COLUMNS + 1];
    private final int[] dryFrom = new int[COLUMNS + 1];
    private final int[] waterOnlyFrom = new int[COLUMNS + 1];

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
        return compute(-1, context, substance);
    }

    /** The same with the ring given, for tests: the column is not remembered. */
    @Nullable
    BlockState computeSubstance(RingPosition position, DensityFunction.FunctionContext context, double substance) {
        load(COLUMNS, position, context.blockX(), context.blockZ());
        return compute(COLUMNS, context, substance);
    }

    /** @param slot the column's slot, or −1 to look it up only when a rule needs it */
    @Nullable
    private BlockState compute(int slot, DensityFunction.FunctionContext context, double substance) {
        if (substance > 0.0) {
            // Rock, most blocks: no rule applies before vanilla, which places nothing there, so the ring is not needed.
            BlockState state = vanilla.computeSubstance(context, substance);
            suppressed = false;
            lid = false;
            spring = false;
            return state == null ? null : afterVanilla(slot(slot, context), context.blockY(), state);
        }
        return openSpace(slot(slot, context), context, substance);
    }

    private int slot(int slot, DensityFunction.FunctionContext context) {
        return slot >= 0 ? slot : column(context.blockX(), context.blockZ());
    }

    /** The slot of a column, filled on first use. */
    private int column(int x, int z) {
        int i = (x & 15) | (z & 15) << 4;
        if (!known[i] || keyX[i] != x || keyZ[i] != z) {
            load(i, RingColumnCache.ringAt(dimension, x, z), x, z);
            known[i] = true;
            keyX[i] = x;
            keyZ[i] = z;
        }
        return i;
    }

    private void load(int i, RingPosition position, int x, int z) {
        noFluid[i] = RingFluidPicker.noFluid(position);
        sealBelow[i] = RingFluidPicker.ring3SealBelow(dimension, position);
        boolean lava = RingFluidPicker.ring3LavaColumn(dimension, position, volcanic);
        VolcanicField.Column column = lava ? volcanic.column(x, z) : null;
        lavaTop[i] = lava ? column.lavaTop() : Integer.MIN_VALUE;
        lavaBottom[i] = lava ? column.lavaBottom() : Integer.MIN_VALUE;
        springY[i] = lava ? column.springY() : Integer.MIN_VALUE;
        fallTop[i] = lava ? column.fallTop() : Integer.MIN_VALUE;
        fallBottom[i] = lava ? column.fallBottom() : Integer.MIN_VALUE;
        fallFlow[i] = lava ? column.fallFlowBelow() : Integer.MIN_VALUE;
        dryFrom[i] = RingFluidPicker.ring3DryFrom(dimension, position);
        waterOnlyFrom[i] = RingFluidPicker.waterOnlyFrom(dimension, position);
    }

    /** Open space ({@code substance <= 0}): the rules of {@link RingFluidPicker} in their order. */
    @Nullable
    private BlockState openSpace(int i, DensityFunction.FunctionContext context, double substance) {
        int y = context.blockY();
        suppressed = true;
        lid = false;
        spring = false;
        if (noFluid[i]) {
            return AIR;
        }
        if (y < sealBelow[i]) {
            return null;
        }
        if (y == springY[i]) {
            // A lava spring on a floating island or volcano (39d): scheduled to flow, it pours off the edge.
            suppressed = false;
            spring = true;
            return LAVA;
        }
        if (y <= fallTop[i] && y >= fallBottom[i]) {
            // A lava fall from a floating island (39f): falling lava from the spring down to where it lands; only its
            // lowest blocks are let flow, so it spreads there. Lava against open air: the one allowed exception.
            spring = y <= fallFlow[i];
            suppressed = !spring;
            return FALLING_LAVA;
        }
        if (VolcanicField.Column.lavaAt(y, lavaTop[i], lavaBottom[i], Integer.MIN_VALUE)) {
            return exposed(context.blockX(), y, context.blockZ()) ? null : LAVA;
        }
        suppressed = false;
        BlockState state = vanilla.computeSubstance(context, substance);
        return state == null ? null : afterVanilla(i, y, state);
    }

    /** The rules on vanilla's state: ring 3 stays dry, ring 2 has water instead of lava. */
    private BlockState afterVanilla(int i, int y, BlockState state) {
        if (y >= dryFrom[i] && state.is(Blocks.WATER)) {
            suppressed = true;
            return AIR;
        }
        if (y < waterOnlyFrom[i] || !(state.is(Blocks.LAVA) || state.is(Blocks.WATER))) {
            return state;
        }
        if (y == RingFluidPicker.LAVA_LAYER_TOP) {
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
        // A neighbour whose lava lies at most one block lower (the next step down a river, 39d) is no edge: the step
        // stays a small lava fall instead of a lip of crust across the river. Lower lava (rivers crossing at different
        // heights) still gets a crust.
        return (neighbour.lavaTop() < y || y < neighbour.lavaBottom())
                && neighbour.height() - SURFACE_MARGIN - CRAG_MARGIN * neighbour.roughness() < y + 1.0
                && !neighbour.islandAt(y);
    }

    @Override
    public boolean shouldScheduleFluidUpdate() {
        return spring || !suppressed && !lid && vanilla.shouldScheduleFluidUpdate();
    }
}
