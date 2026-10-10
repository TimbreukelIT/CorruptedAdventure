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
 * ({@link RingFluidPicker#ring3SealBelow}). 42: the openings of the lava tubes, magma chambers and fire geodes stay free
 * of aquifer fluids (air, or lava in a gutter or a chamber's lake, crusted over where it would touch open air), and the
 * rock of their guard bands stays rock. Carvers fill their caves through this aquifer too, so they cannot cut into a
 * chamber, a gutter or a geode either.
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
     * How far the crags of ring 3 ({@code ring_3/overworld/terrain}: 0.3 * roughness * noise in density, 1/8 per block;
     * the noise reaches about ±1.5) can move the surface, per unit of roughness (42b: on top of the exact interpolation,
     * {@link #surfaceAt}).
     */
    private static final double CRAG_MARGIN = 4.0;
    /** The terrain's cells: the density is interpolated between values on this grid (x, z). */
    private static final int CELL = 4;

    private final ResourceKey<Level> dimension;
    private final Aquifer vanilla;
    @Nullable
    private final VolcanicField volcanic;
    private boolean suppressed;
    /** Set while the chunk's carvers run ({@code RingChunkGenerator#applyCarvers}): their open space is a carved tunnel. */
    private boolean carving;
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
    // The range of the column's carved openings and guard bands (42), else caveBottom > caveTop.
    private final int[] caveBottom = new int[COLUMNS + 1];
    private final int[] caveTop = new int[COLUMNS + 1];
    // The band around the column's lava lake, river or lava sea (42b: closed to carvers), else guardTop < guardBottom.
    private final int[] guardBottom = new int[COLUMNS + 1];
    private final int[] guardTop = new int[COLUMNS + 1];
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

    /** Marks the carvers' pass: while it runs, open space asked for is a tunnel a carver wants to cut. */
    void carving(boolean carving) {
        this.carving = carving;
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
        caveBottom[i] = lava ? Math.min(column.voidMin(), column.deepGuardMin()) : Integer.MAX_VALUE;
        caveTop[i] = lava ? Math.max(column.voidMax(), column.deepGuardMax()) : Integer.MIN_VALUE;
        guardBottom[i] = lava ? column.guardBottom() : Integer.MAX_VALUE;
        guardTop[i] = lava ? column.guardTop() : Integer.MIN_VALUE;
        dryFrom[i] = RingFluidPicker.ring3DryFrom(dimension, position);
        waterOnlyFrom[i] = RingFluidPicker.waterOnlyFrom(dimension, position);
        // 47e: no lava fall (spring and falling lava) whose lava may reach water (last: the check reads other columns).
        if (lava && !volcanic.springStaysDry(column)) {
            springY[i] = Integer.MIN_VALUE;
        }
        if (lava && !volcanic.fallStaysDry(column)) {
            fallTop[i] = Integer.MIN_VALUE;
            fallBottom[i] = Integer.MIN_VALUE;
            fallFlow[i] = Integer.MIN_VALUE;
        }
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
        if (carving && y >= guardBottom[i] && y <= guardTop[i]) {
            // A carver may not cut into the band around lava (42b): its tunnel would open the lava to the air beside it.
            return null;
        }
        if (VolcanicField.Column.lavaAt(y, lavaTop[i], lavaBottom[i], Integer.MIN_VALUE)) {
            return exposed(context.blockX(), y, context.blockZ()) ? null : LAVA;
        }
        if (y >= caveBottom[i] && y <= caveTop[i]) {
            VolcanicField.Column column = volcanic.column(context.blockX(), context.blockZ());
            if (column.voidAt(y)) {
                if (!column.subLavaAt(y)) {
                    return AIR;
                }
                // Lava in a gutter or a lake: a lip of crust where open air lies beside or below it (a gutter's steps).
                return openBesideOrBelow(column, context.blockX(), y, context.blockZ()) ? null : LAVA;
            }
            if (column.deepGuardAt(y)) {
                return null;
            }
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
        if (dryFrom[i] != Integer.MAX_VALUE && y >= RingFluidPicker.LAVA_LAYER_TOP && state.is(Blocks.LAVA)) {
            // 42b (V2 = A): no local aquifer lava in ring 3 (its pools stood against the air of their caves); lava lies
            // only where the terrain puts it. Vanilla's lava layer below stays.
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
     * Whether lava at this block may stand against open air: a neighbour (beside it, or right below it) that may be open
     * space without lava. Such a block becomes rock instead (a lip of cooled crust), so no lava flows when the chunk is
     * first updated. 42b: the neighbour's ground is the terrain's real, interpolated surface ({@link #groundAt}), not the
     * field's height, and its carved openings count as open.
     */
    boolean exposed(int x, int y, int z) {
        VolcanicField.Column own = volcanic.column(x, z);
        return !holdsLava(own, y - 1) && open(own, x, y - 1, z, -1.0)
                || edge(x + 1, y, z) || edge(x - 1, y, z) || edge(x, y, z + 1) || edge(x, y, z - 1);
    }

    /** Whether the block beside lava may be open air: open, without lava, and no step of a river onto lava. */
    private boolean edge(int x, int y, int z) {
        VolcanicField.Column neighbour = volcanic.column(x, z);
        if (holdsLava(neighbour, y) || !open(neighbour, x, y, z, -1.0)) {
            return false;
        }
        // A step down a river (39d): the neighbour's lava lies one block lower, and that block really becomes lava (the
        // same decision the aquifer takes there, its own crust check included), so the step is a small lava fall onto
        // lava. Otherwise the lava would spill onto rock or crust: an edge.
        return !(neighbour.lavaTop() == y && becomesLava(neighbour, x, y - 1, z));
    }

    /**
     * Whether the aquifer fills this block with the terrain's lava: inside the column's lava, open for certain, not
     * crusted (the terrain's lava lies only inside ring 3 proper, where the aquifer places it: rivers end where the core
     * fades, craters and the abyss lie inside it). Recurses only downwards (each step one block lower), so it ends within
     * a lava band.
     */
    private boolean becomesLava(VolcanicField.Column column, int x, int y, int z) {
        return holdsLava(column, y) && open(column, x, y, z, 1.0) && !exposed(x, y, z);
    }

    private static boolean holdsLava(VolcanicField.Column column, int y) {
        return y < column.lavaTop() && y >= column.lavaBottom();
    }

    /**
     * Whether the block is open space: a carved opening, or above the terrain's surface ({@link #groundAt}) with the
     * crags taken at their lowest ({@code crags} −1: may be open) or highest (+1: open for certain). A thin solid (or a
     * floating island) is never open.
     */
    private boolean open(VolcanicField.Column column, int x, int y, int z, double crags) {
        if (column.thinSolidAt(y)) {
            return false;
        }
        if (column.voidAt(y)) {
            return true;
        }
        groundAt(x, z);
        return y >= ground[0] + crags * CRAG_MARGIN * ground[1];
    }

    // The ground of the last column asked (groundAt): the interpolated surface and the largest corner roughness.
    private final double[] ground = new double[2];

    /**
     * The terrain's surface in a column as the noise chunk builds it, into {@link #ground} (42b): the ring 3 stack is
     * linear in y near the surface (0.125 * (height - y); caves kept out around lava by the guard, at least a cell wide),
     * and the noise chunk interpolates it between the corners of its cells (every 4 blocks in x and z), so a block is rock
     * below the bilinear blend of the corner heights and open from it up; the crags (interpolated too) move that by up
     * to {@link #CRAG_MARGIN} times the largest corner roughness. The field's own height of the column can lie well
     * above the blend where the heights jump (a channel's bed beside its bank, two rivers at different levels).
     */
    private void groundAt(int x, int z) {
        int x0 = Math.floorDiv(x, CELL) * CELL;
        int z0 = Math.floorDiv(z, CELL) * CELL;
        double fx = (x - x0) / (double) CELL;
        double fz = (z - z0) / (double) CELL;
        VolcanicField.Column c00 = volcanic.column(x0, z0);
        VolcanicField.Column c10 = volcanic.column(x0 + CELL, z0);
        VolcanicField.Column c01 = volcanic.column(x0, z0 + CELL);
        VolcanicField.Column c11 = volcanic.column(x0 + CELL, z0 + CELL);
        ground[0] = (c00.height() * (1.0 - fx) + c10.height() * fx) * (1.0 - fz) + (c01.height() * (1.0 - fx) + c11.height() * fx) * fz;
        ground[1] = Math.max(Math.max(c00.roughness(), c10.roughness()), Math.max(c01.roughness(), c11.roughness()));
    }

    /** The terrain's surface in a column ({@link #groundAt}), for the tests. */
    double surfaceAt(int x, int z) {
        groundAt(x, z);
        return ground[0];
    }

    /** Whether an opening without lava lies beside or right below the block (42: lava there would flow). */
    private boolean openBesideOrBelow(VolcanicField.Column column, int x, int y, int z) {
        return column.voidAt(y - 1) && !column.subLavaAt(y - 1) || dryOpening(x + 1, y, z) || dryOpening(x - 1, y, z)
                || dryOpening(x, y, z + 1) || dryOpening(x, y, z - 1);
    }

    private boolean dryOpening(int x, int y, int z) {
        VolcanicField.Column neighbour = volcanic.column(x, z);
        return neighbour.voidAt(y) && !neighbour.subLavaAt(y);
    }

    @Override
    public boolean shouldScheduleFluidUpdate() {
        return spring || !suppressed && !lid && vanilla.shouldScheduleFluidUpdate();
    }
}
