package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring4Tunnels;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.ThreadOwned;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Applies the fluid rules of {@link RingFluidPicker} to the local fluids of vanilla's noise-based aquifer, which the
 * global picker does not cover. Void zone: open space stays empty (below Y -10 the aquifer would hang lava sheets under
 * the floating islands). Overworld ring 2: local lava becomes water ({@link RingFluidPicker#waterOnlyFrom}), with vanilla's
 * fluid update flag (set near cell borders, where the fluid may flow; inside a pool a tick would do nothing). Right
 * above vanilla's lava layer (Y −54) any aquifer fluid stays solid instead: one block of stone between the water and
 * the lava below, like vanilla's barriers (vanilla itself lets water cells touch the layer, and the contact turns into
 * an obsidian crust). Overworld ring 3: lava in the lava lakes and channels of the {@link VolcanicField}
 * ({@link RingFluidPicker#ring3LavaColumn}, no fluid update: still lava), no water from Y 0 up
 * ({@link RingFluidPicker#ring3DryFrom}), rock below ring 2's sea level in its half of the zone 2→3
 * ({@link RingFluidPicker#ring3SealBelow}), and a closed rock floor right above vanilla's lava layer: open space at
 * Y −54 (air, water or lava, also for carvers) is always rock, so no water, not even water flowing down from higher up,
 * can reach the lava layer and turn it into obsidian depending on the load order (47e, decision B, INT3). 42: the openings of the lava tubes, magma chambers and fire geodes stay free
 * of aquifer fluids (air, or lava in a gutter or a chamber's lake, crusted over where it would touch open air), and the
 * rock of their guard bands stays rock. Carvers fill their caves through this aquifer too, so they cannot cut into a
 * chamber, a gutter or a geode either. Overworld ring 4 (59): no lava at all, local lava becomes water like in ring 2
 * ({@link RingFluidPicker#waterOnlyFrom}, also over the whole zone 4→5), the bottom below Y −54 is dry ({@link RingFluidPicker#ring4DryBelow}) with a
 * dam of rock over the zone 4→5 and ring 4's half of the zone 3→4 ({@link RingFluidPicker#ring4DamBelow}), carvers
 * place no lava there ({@link #carverLava}), and the recorded waterfalls of its
 * {@link IslandField} fall as water ({@link RingFluidPicker#ring4WaterColumn}); 61: their springs flow, and the lakes
 * above the sea in the islands' holes hold still water, with a band carvers may not cut. verdant_falls: that water
 * branch also holds in the {@code verdant_falls} strip of ring 3's half of the zone 3→4
 * ({@link RingFluidPicker#verdantWater}), without ring 3's terrain lava; its seal below Y 63 stays. 73: ring 4's
 * tunnel net fills its own open space ({@link Ring4Tunnels.Span}: water and air domes, the dry net's air, swamp gas,
 * poison pools, dry sinkholes) and keeps carvers out of the rock around its sealed spaces; all other open space of ring
 * 4 is water up to the sea's top block and air above ({@link RingFluidPicker#ring4Flooded}), not vanilla's local levels.
 * The fracture zone 4→5 (85, V7-A): dry, open space from Y −54 up is air ({@link RingFluidPicker#fractureZone}), its calm
 * first strip sealed below the sea level ({@link RingFluidPicker#fractureSealBelow}).
 * Overworld ring 5 (82, V9-B; behind the zone 4→5): dry, all open space is air ({@link RingFluidPicker#ring5Dry}), except
 * vanilla's lava layer below Y −54 in the inner band ({@link RingFluidPicker#ring5LavaColumn}, left to vanilla) behind a
 * dam of rock along the line between the bands ({@link RingFluidPicker#ring5Dam}); carvers place no lava outside the
 * layer ({@link #carverLava}).
 * <p>
 * Per column (opt/aquifer): the rules of a column are painted into a profile, one entry per block of its height, on the
 * column's first query: the outcome where a rule decides, else "ask vanilla" with the rule on vanilla's answer. Where
 * vanilla's answer is known without asking, the profile holds it too: its sky ({@link VanillaSky}) over the noise aquifer
 * in the noise fill, the fluid picker's status over the disabled one (the Nether, {@code Aquifer.createDisabled}); vanilla's
 * fluid update flag after every call stays the old chain's (a carver reads it again at its lava level). A block then only reads
 * its entry; the rules' tests per block (in this ring? below that Y?) went one way in one region and the other way in the
 * next, and a branch the JIT never saw taken compiles to a trap that late in a session deoptimizes on every block where
 * it is taken, without recompiling (2026-10-26). The only branch left per block, decided or asked, goes both ways in
 * every ring: the sky is decided everywhere. The profiles live in a table per thread, not per noise chunk (a chunk in
 * flight holds none).
 * <p>
 * One instance per noise chunk, so the per-call state below is single-threaded like vanilla's.
 */
final class RingAquifer implements Aquifer {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
    /** Falling lava (a lava fall, 39f): level 8 is a falling flow, which stays put under lava and spreads where it lands. */
    private static final BlockState FALLING_LAVA = LAVA.setValue(LiquidBlock.LEVEL, 8);
    /** Falling water (a recorded waterfall of ring 4, 59), like {@link #FALLING_LAVA}. */
    private static final BlockState FALLING_WATER = WATER.setValue(LiquidBlock.LEVEL, 8);
    /**
     * How far the crags of ring 3 ({@code ring_3/overworld/terrain}: 0.3 * roughness * noise in density, 1/8 per block;
     * the noise reaches about ±1.5) can move the surface, per unit of roughness (42b: on top of the exact interpolation,
     * {@link #surfaceAt}).
     */
    private static final double CRAG_MARGIN = 4.0;
    /** The terrain's cells: the density is interpolated between values on this grid (x, z). */
    private static final int CELL = 4;
    /** The overworld's height, for the aquifers of the tests. */
    private static final int OVERWORLD_MIN_Y = -64;
    private static final int OVERWORLD_HEIGHT = 384;

    // The outcomes, indices into pick: vanilla's own answer (KEEP), then the states the rules place (ROCK: null, the
    // default block). The last two are ring 3's lava with its crust decided when the block is asked (the check reads
    // the columns around, and rock in the lava's range is never asked): the terrain's lava ({@link #exposed}) and the lava
    // of a gutter or a chamber's lake ({@link #openBesideOrBelow}).
    static final int KEEP = 0;
    static final int ROCK = 1;
    static final int TO_AIR = 2;
    static final int TO_WATER = 3;
    static final int TO_LAVA = 4;
    static final int TO_FALLING_LAVA = 5;
    static final int TO_FALLING_WATER = 6;
    static final int TO_GAS = 7;
    static final int TO_POOL = 8;
    static final int LAVA_UNLESS_EXPOSED = 9;
    static final int LAVA_UNLESS_OPEN = 10;
    // A profile entry (one byte, read unsigned). Below ASK: a decided outcome (bits 0–3) and whether it is scheduled to
    // flow (SPRING). From ASK up: vanilla's answer is asked, ASK + the rule on it: bit 0 its air becomes rock; bits 1–2
    // its water, bits 3–4 its lava become that outcome (KEEP, ROCK, TO_AIR or TO_WATER).
    static final int OUTCOME_BITS = 0b1111;
    static final int SPRING = 1 << 4;
    static final int ASK = 1 << 5;
    /** The outcome of a rule (entry − ASK) per class of vanilla's answer (null, air, water, lava): {@code rule << 2 | class}. */
    private static final byte[] AFTER = new byte[32 << 2];

    static {
        for (int rule = 0; rule < 32; rule++) {
            AFTER[rule << 2] = KEEP;
            AFTER[rule << 2 | 1] = (byte) ((rule & 1) != 0 ? ROCK : KEEP);
            AFTER[rule << 2 | 2] = (byte) (rule >> 1 & 3);
            AFTER[rule << 2 | 3] = (byte) (rule >> 3 & 3);
        }
    }

    /**
     * One id per aquifer, the key of its columns in a thread's table (with the carvers' pass in bit 0). The carvers' pass
     * paints its columns anew: the table rarely still holds the noise fill's (other chunks fill on the thread in between,
     * the carvers often run on another one), and keeping them per noise chunk would cost every chunk in flight its
     * profiles.
     */
    private static final AtomicLong IDS = new AtomicLong();

    private final ResourceKey<Level> dimension;
    private final Aquifer vanilla;
    @Nullable
    private final VolcanicField volcanic;
    @Nullable
    private final IslandField islands;
    private final RingLookup rings;
    private final RingFluidPicker.Ring5Bands bands;
    @Nullable
    private final VanillaSky sky;
    /** Vanilla's aquifer is the disabled one over this picker (its answer is the picker's status), or null. */
    @Nullable
    private final RingFluidPicker.ChunkPicker plain;
    private final int minY;
    private final int height;
    private final long id = IDS.incrementAndGet() << 1;
    private final ThreadOwned<Profiles> profiles = new ThreadOwned<>(Profiles::forThread);
    /** The outcome states; [KEEP] is vanilla's answer of the current call, the tunnel net's fills come on their first use. */
    private final BlockState[] pick = {null, null, AIR, WATER, LAVA, FALLING_LAVA, FALLING_WATER, null, null};
    private boolean tunnelFills;
    /** The rules of the column being painted. */
    private final Rules rules = new Rules();
    private final byte[] single = new byte[1];
    /** Set while the chunk's carvers run ({@code RingChunkGenerator#applyCarvers}): their open space is a carved tunnel. */
    private boolean carving;
    private long owner = id;
    private boolean spring;
    private boolean asked;

    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla) {
        this(dimension, vanilla, null, null);
    }

    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla, @Nullable VolcanicField volcanic) {
        this(dimension, vanilla, volcanic, null);
    }

    /**
     * @param volcanic the ring 3 terrain of this noise chunk (the same instance its density uses), or null
     * @param islands  the ring 4 islands of this noise chunk (likewise), or null
     */
    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla, @Nullable VolcanicField volcanic, @Nullable IslandField islands) {
        this(dimension, vanilla, volcanic, islands, (x, z) -> RingColumnCache.ringAt(dimension, x, z));
    }

    /** @param rings the ring of a column: the running server's layout in worldgen, a fixed one in tests */
    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla, @Nullable VolcanicField volcanic, @Nullable IslandField islands,
                RingLookup rings) {
        this(dimension, vanilla, volcanic, islands, rings, RingFluidPicker.CURRENT_BANDS);
    }

    /** @param bands ring 5's band: the running server's layout in worldgen, a fixed one in tests */
    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla, @Nullable VolcanicField volcanic, @Nullable IslandField islands,
                RingLookup rings, RingFluidPicker.Ring5Bands bands) {
        this(dimension, vanilla, volcanic, islands, rings, bands, null, null, OVERWORLD_MIN_Y, OVERWORLD_HEIGHT);
    }

    /**
     * The aquifer of a noise chunk in worldgen.
     *
     * @param sky    vanilla's sky when {@code vanilla} is the noise aquifer, else null
     * @param plain  the noise chunk's fluid picker when {@code vanilla} is the disabled aquifer over it, else null
     * @param minY   the noise chunk's lowest block
     * @param height its height: every block the noise chunk and the carvers ask for
     */
    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla, @Nullable VolcanicField volcanic, @Nullable IslandField islands,
                @Nullable VanillaSky sky, @Nullable RingFluidPicker.ChunkPicker plain, int minY, int height) {
        this(dimension, vanilla, volcanic, islands, (x, z) -> RingColumnCache.ringAt(dimension, x, z), RingFluidPicker.CURRENT_BANDS,
                sky, plain, minY, height);
    }

    /** The same with the ring and the band given, for tests. */
    RingAquifer(ResourceKey<Level> dimension, Aquifer vanilla, @Nullable VolcanicField volcanic, @Nullable IslandField islands,
                RingLookup rings, RingFluidPicker.Ring5Bands bands, @Nullable VanillaSky sky,
                @Nullable RingFluidPicker.ChunkPicker plain, int minY, int height) {
        this.dimension = dimension;
        this.vanilla = vanilla;
        this.volcanic = volcanic;
        this.islands = islands;
        this.rings = rings;
        this.bands = bands;
        this.sky = sky;
        this.plain = plain;
        this.minY = minY;
        this.height = height;
    }

    /** The ring of a column (block x, z). */
    @FunctionalInterface
    interface RingLookup {
        RingPosition at(int x, int z);
    }

    @Nullable
    @Override
    public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
        if (substance > 0.0) {
            return rock();
        }
        return openSpace(context, substance);
    }

    /**
     * Rock, most blocks: vanilla places nothing there and no rule applies before it, so neither the ring nor vanilla is
     * asked. Both of vanilla's aquifers answer null for any positive substance and set no fluid update
     * ({@code NoiseBasedAquifer.computeSubstance}, {@code Aquifer.createDisabled}; {@code VanillaSkyTest}); a wrapped
     * aquifer that placed something in rock would have to be asked here. Its own flag, left from an earlier call, is only
     * read right after asking it ({@link #shouldScheduleFluidUpdate}), so it needs no reset.
     */
    @Nullable
    private BlockState rock() {
        spring = false;
        asked = false;
        return null;
    }

    /**
     * Whether this aquifer decides what a carver places at or below its lava level in the column, where vanilla puts
     * lava without asking (through {@link CarverLava}): in ring 4 and its dams ({@link RingFluidPicker#ring4NoLava}), and in
     * ring 5 outside its lava layer (82: the dam and the outer band). Elsewhere vanilla's lava stays.
     */
    boolean decidesCarverLava(int x, int z) {
        Profiles table = profiles.get();
        if (holdsAnother(table, x, z)) {
            loadRules(rings.at(x, z), x, z);
            return rules.noLava;
        }
        return table.noLava[slot(table, x, z)];
    }

    /**
     * What a carver places instead of lava where {@link #decidesCarverLava}: the aquifer's own answer for the open
     * space (air on the dry bottom, null in a dam: not cut).
     */
    @Nullable
    BlockState carverLava(DensityFunction.FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();
        if (holdsAnother(profiles.get(), x, z)) {
            return outcome(entryOutside(x, context.blockY(), z), context, 0.0);
        }
        return openSpace(context, 0.0);
    }

    /**
     * Whether the column's slot holds another column of this pass (review 59): the carver-lava check paints a column of
     * another chunk with the same low bits on its own, for that call only, so it never pushes out the noise chunk's own.
     */
    private boolean holdsAnother(Profiles table, int x, int z) {
        int i = (x & 15) | (z & 15) << 4;
        return table.owner[i] == owner && (table.keyX[i] != x || table.keyZ[i] != z);
    }

    /** Marks the carvers' pass: while it runs, open space asked for is a tunnel a carver wants to cut. */
    void carving(boolean carving) {
        this.carving = carving;
        owner = id | (carving ? 1 : 0);
    }

    /** The same with the ring given, for tests: the column is not remembered. */
    @Nullable
    BlockState computeSubstance(RingPosition position, DensityFunction.FunctionContext context, double substance) {
        if (substance > 0.0) {
            return rock();
        }
        int x = context.blockX();
        int z = context.blockZ();
        loadRules(position, x, z);
        paint(x, z, context.blockY(), single, 1);
        return outcome(single[0] & 0xFF, context, substance);
    }

    /** Open space ({@code substance <= 0}): the entry of the block in its column's profile. */
    @Nullable
    private BlockState openSpace(DensityFunction.FunctionContext context, double substance) {
        int x = context.blockX();
        int y = context.blockY();
        int z = context.blockZ();
        Profiles table = profiles.get();
        int i = slot(table, x, z);
        int index = y - minY;
        int entry = index >= 0 && index < height ? table.entries[i][index] & 0xFF : entryOutside(x, y, z);
        return outcome(entry, context, substance);
    }

    /** The outcome of an entry: decided, or vanilla's answer with the entry's rule on it. */
    @Nullable
    private BlockState outcome(int entry, DensityFunction.FunctionContext context, double substance) {
        if (entry < ASK) {
            spring = (entry & SPRING) != 0;
            asked = false;
            int decided = entry & OUTCOME_BITS;
            return decided < LAVA_UNLESS_EXPOSED ? pick[decided] : ring3Lava(decided, context);
        }
        spring = false;
        BlockState state = vanilla.computeSubstance(context, substance);
        pick[KEEP] = state;
        int after = AFTER[(entry - ASK) << 2 | kind(state)];
        // Vanilla's fluid update stays with its own answer and with its fluid turned into water, not with the rock or air
        // a rule puts in its place: a carver reads the flag again for the lava it places at its lava level without asking.
        asked = after == KEEP || after == TO_WATER;
        return pick[after];
    }

    /** Ring 3's lava ({@link #LAVA_UNLESS_EXPOSED}, {@link #LAVA_UNLESS_OPEN}): a lip of crust where it would flow. */
    @Nullable
    private BlockState ring3Lava(int decided, DensityFunction.FunctionContext context) {
        int x = context.blockX();
        int y = context.blockY();
        int z = context.blockZ();
        boolean crust = decided == LAVA_UNLESS_EXPOSED ? exposed(x, y, z) : openBesideOrBelow(volcanic.column(x, z), x, y, z);
        return crust ? null : LAVA;
    }

    /** The class of vanilla's answer for {@link #AFTER}: null, air (or any other state), water, lava. */
    private static int kind(@Nullable BlockState state) {
        return state == null ? 0 : state.is(Blocks.WATER) ? 2 : state.is(Blocks.LAVA) ? 3 : 1;
    }

    /** The slot of a column in the thread's table, its profile painted on first use. */
    private int slot(Profiles table, int x, int z) {
        int i = (x & 15) | (z & 15) << 4;
        if (table.owner[i] != owner || table.keyX[i] != x || table.keyZ[i] != z) {
            loadRules(rings.at(x, z), x, z);
            byte[] entries = table.entries[i];
            if (entries == null || entries.length < height) {
                // At least the overworld's: a thread paints columns of both dimensions.
                entries = new byte[Math.max(height, OVERWORLD_HEIGHT)];
                table.entries[i] = entries;
            }
            paint(x, z, minY, entries, height);
            table.noLava[i] = rules.noLava;
            table.owner[i] = owner;
            table.keyX[i] = x;
            table.keyZ[i] = z;
        }
        return i;
    }

    /**
     * The entry of a block painted on its own: outside the noise chunk's height (never in worldgen), or in a column of
     * another chunk ({@link #holdsAnother}).
     */
    private int entryOutside(int x, int y, int z) {
        loadRules(rings.at(x, z), x, z);
        paint(x, z, y, single, 1);
        return single[0] & 0xFF;
    }

    // ---------------------------------------------------------------- the rules of a column

    /** The rules of one column: {@link RingFluidPicker}'s per-column bounds and the fields' columns. */
    static final class Rules {
        boolean noFluid;
        int sealBelow;
        // The column's lava range [lavaBottom, lavaTop) when its lava counts, else lavaTop = Integer.MIN_VALUE (no lava
        // at any Y); springY = the Y of its lava spring (39d), else Integer.MIN_VALUE.
        int lavaTop;
        int lavaBottom;
        int springY;
        // The column's lava fall (39f): falling lava in [fallBottom, fallTop], let flow at and below fallFlow.
        int fallTop;
        int fallBottom;
        int fallFlow;
        int dryFrom;
        // The range of the column's carved openings and guard bands (42), else caveBottom > caveTop.
        int caveBottom;
        int caveTop;
        // The band around the column's lava lake, river or lava sea (42b: closed to carvers), else guardTop < guardBottom.
        int guardBottom;
        int guardTop;
        int waterOnlyFrom;
        int dryBelow;
        // Ring 5 (82): open space is air from this Y up (Integer.MAX_VALUE: no such rule).
        int airFrom;
        boolean noLava;
        // The column's recorded waterfall (ring 4, 59): falling water in [waterFallBottom, waterFallTop], let flow at and
        // below waterFallFlow.
        int waterFallTop;
        int waterFallBottom;
        int waterFallFlow;
        // Ring 4 (61): an island's spring (a water source that flows), the top (exclusive) of a lake above the sea, and the
        // band around such a lake that carvers may not cut (from caveFrom to caveTo).
        int waterSpringY;
        int lakeTop;
        int lakeFloor;
        int lakeGuardBottom;
        int lakeGuardTop;
        // verdant_falls (2c part 2): open space the field carves under the ground (niches, tunnels, a gorge's ledge path
        // and wall), left as air: [openFrom, openTo], else openFrom > openTo.
        int openFrom;
        int openTo;
        // 73: ring 4's own water rule for the column, and its tunnel net (null: none there).
        boolean flooded;
        @Nullable
        Ring4Tunnels.Span tunnels;
        // 74: a sky lake's still water (skyFloor, skyTop) in the basin cut into an island's top, and the blocks around it
        // carvers may not cut [skyGuardFrom, skyGuardTo].
        int skyTop;
        int skyFloor;
        int skyGuardFrom;
        int skyGuardTo;
    }

    private void loadRules(RingPosition position, int x, int z) {
        Rules r = rules;
        r.noFluid = RingFluidPicker.noFluid(position);
        r.sealBelow = Math.max(Math.max(RingFluidPicker.ring3SealBelow(dimension, position), RingFluidPicker.ring4DamBelow(dimension, position)),
                RingFluidPicker.fractureSealBelow(dimension, position));
        r.dryBelow = RingFluidPicker.ring4DryBelow(dimension, position);
        r.noLava = RingFluidPicker.ring4NoLava(dimension, position);
        r.airFrom = Integer.MAX_VALUE;
        if (RingFluidPicker.fractureZone(dimension, position)) {
            // The fracture zone 4→5 (85, V7-A): dry from vanilla's lava layer up, ring 4's dam below it.
            r.airFrom = RingFluidPicker.LAVA_LAYER_TOP;
        } else if (RingFluidPicker.ring5Dry(dimension, position)) {
            // Ring 5 (82, V9-B): the band is asked once per column. The lava layer stays vanilla's; the dam holds rock
            // below its top; the outer band is air down to the void.
            double band = bands.signedDistance(x, z);
            // The cracks of the inner band (83) are asked there only.
            double crack = band < 0.0 ? bands.crackDistance(x, z) : Double.POSITIVE_INFINITY;
            boolean lava = RingFluidPicker.ring5LavaColumn(band, crack);
            boolean dam = RingFluidPicker.ring5Dam(band, crack);
            r.airFrom = lava || dam ? RingFluidPicker.LAVA_LAYER_TOP : Integer.MIN_VALUE;
            if (dam) {
                r.sealBelow = Math.max(r.sealBelow, RingFluidPicker.LAVA_LAYER_TOP);
            }
            r.noLava = !lava;
        }
        boolean falls = RingFluidPicker.ring4WaterColumn(dimension, position, islands);
        IslandField.Column islandColumn = falls ? islands.islandColumn(x, z) : null;
        r.waterFallTop = falls ? islandColumn.fallTop() : Integer.MIN_VALUE;
        r.waterFallBottom = falls ? islandColumn.fallBottom() : Integer.MIN_VALUE;
        r.waterFallFlow = falls ? islandColumn.fallFlowBelow() : Integer.MIN_VALUE;
        r.waterSpringY = falls ? islandColumn.springY() : Integer.MIN_VALUE;
        r.lakeTop = falls ? islandColumn.lakeTop() : Integer.MIN_VALUE;
        r.lakeGuardBottom = falls ? islandColumn.caveLiftFrom() : Integer.MAX_VALUE;
        r.lakeGuardTop = falls ? islandColumn.caveLiftTo() : Integer.MIN_VALUE;
        r.openFrom = falls ? islandColumn.openFrom() : Integer.MAX_VALUE;
        r.openTo = falls ? islandColumn.openTo() : Integer.MIN_VALUE;
        r.skyTop = falls ? islandColumn.skyLakeTop() : Integer.MIN_VALUE;
        r.skyFloor = falls ? islandColumn.skyLakeFloor() : Integer.MIN_VALUE;
        r.skyGuardFrom = falls ? islandColumn.skyGuardFrom() : Integer.MAX_VALUE;
        r.skyGuardTo = falls ? islandColumn.skyGuardTo() : Integer.MIN_VALUE;
        double exactFloor = falls ? islandColumn.exactFloor() : Double.NaN;
        // 73 (after the island column's values: it asks the column again).
        r.flooded = RingFluidPicker.ring4Flooded(dimension, position);
        r.tunnels = falls ? islands.tunnelSpan(x, z) : null;
        // Review 61b #2, #7 (last: it reads other columns, which may reuse the island column read above): in the band
        // around a lake above the sea the ground starts just under the terrain's interpolated surface; open space below
        // it (a cave the lift missed, a sinkhole the surface does not know) stays rock, the lake fills its bowl above.
        // verdant_falls (2c): the descent is exact per block, so its own floor counts (the grid would fill a gully in).
        r.lakeFloor = r.lakeGuardBottom == Integer.MAX_VALUE ? Integer.MAX_VALUE
                : Mth.floor(Double.isNaN(exactFloor) ? lakeSurface(x, z) : exactFloor) - 1;
        boolean lava = RingFluidPicker.ring3LavaColumn(dimension, position, volcanic);
        VolcanicField.Column column = lava ? volcanic.column(x, z) : null;
        r.lavaTop = lava ? column.lavaTop() : Integer.MIN_VALUE;
        r.lavaBottom = lava ? column.lavaBottom() : Integer.MIN_VALUE;
        r.springY = lava ? column.springY() : Integer.MIN_VALUE;
        r.fallTop = lava ? column.fallTop() : Integer.MIN_VALUE;
        r.fallBottom = lava ? column.fallBottom() : Integer.MIN_VALUE;
        r.fallFlow = lava ? column.fallFlowBelow() : Integer.MIN_VALUE;
        r.caveBottom = lava ? Math.min(column.voidMin(), column.deepGuardMin()) : Integer.MAX_VALUE;
        r.caveTop = lava ? Math.max(column.voidMax(), column.deepGuardMax()) : Integer.MIN_VALUE;
        r.guardBottom = lava ? column.guardBottom() : Integer.MAX_VALUE;
        r.guardTop = lava ? column.guardTop() : Integer.MIN_VALUE;
        r.dryFrom = RingFluidPicker.ring3DryFrom(dimension, position);
        r.waterOnlyFrom = RingFluidPicker.waterOnlyFrom(dimension, position);
        // 47e: no lava fall (spring and falling lava) whose lava may reach water (last: the check reads other columns).
        if (lava && !volcanic.springStaysDry(column)) {
            r.springY = Integer.MIN_VALUE;
        }
        if (lava && !volcanic.fallStaysDry(column)) {
            r.fallTop = Integer.MIN_VALUE;
            r.fallBottom = Integer.MIN_VALUE;
            r.fallFlow = Integer.MIN_VALUE;
        }
    }

    // ---------------------------------------------------------------- the profile

    /**
     * Paints the entries of the column's blocks from {@code baseY} up ({@code n} of them) from {@link #rules}: vanilla's
     * answer with the rule on it first (the lowest precedence) and what is known of it, then the rules from the last to
     * the first, each over the blocks where it decides (the first rule that decides a block wins, as in a chain of
     * tests per block).
     */
    private void paint(int x, int z, int baseY, byte[] out, int n) {
        paint(rules, x, z, baseY, out, n);
    }

    /** {@link #paint} of the given rules, also for the tests. */
    void paint(Rules r, int x, int z, int baseY, byte[] out, int n) {
        if (r.noFluid) {
            // The void edge: nothing comes before it.
            Arrays.fill(out, 0, n, (byte) TO_AIR);
            return;
        }
        // Vanilla is asked only from the seal and the dry bottom up to the air of the dry rings (the rules before it).
        int askFrom = Math.max(r.sealBelow, r.dryBelow);
        int askTo = r.airFrom;
        paintAsk(r, baseY, out, n);
        if (r.flooded) {
            // 73: one flat level for all of ring 4's open space: water up to the sea's top block, air above, still; rock
            // on Y −54. It decides every block, so nothing painted before it (vanilla, the terrain's lava) would show.
            fill(out, baseY, n, Long.MIN_VALUE, Ring4Tunnels.SEA_TOP + 1L, TO_WATER);
            fill(out, baseY, n, Ring4Tunnels.SEA_TOP + 1L, Long.MAX_VALUE, TO_AIR);
            fill(out, baseY, n, RingFluidPicker.LAVA_LAYER_TOP, RingFluidPicker.LAVA_LAYER_TOP + 1L, ROCK);
        } else if (askFrom < askTo) {
            paintKnown(x, z, baseY, out, n, askFrom, askTo);
            if (r.caveBottom <= r.caveTop) {
                paintCaves(r, x, z, baseY, out, n, askFrom, askTo);
            }
            // The terrain's lava (ring 3): rock where it would stand against open air.
            fill(out, baseY, n, Math.max(r.lavaBottom, askFrom), Math.min(r.lavaTop, askTo), LAVA_UNLESS_EXPOSED);
            if (carving) {
                // A carver may not cut into the band around lava (42b): its tunnel would open the lava to the air beside it.
                fill(out, baseY, n, r.guardBottom, r.guardTop + 1L, ROCK);
            }
        }
        if (r.tunnels != null) {
            paintTunnels(r.tunnels, baseY, out, n);
        }
        // A lake above the sea in an island's hole (61): still water, walled in by its rim.
        fill(out, baseY, n, r.lakeFloor, r.lakeTop, TO_WATER);
        // Under the ground around a lake above the sea: rock, so no water there ever stands on air (review 61b #2).
        fill(out, baseY, n, r.lakeGuardBottom, Math.min(r.lakeGuardTop + 1L, r.lakeFloor), ROCK);
        // A niche, a tunnel or a gorge's ledge path the terrain cut under the ground (2c part 2): air, never the guard's
        // rock nor vanilla's water.
        fill(out, baseY, n, r.openFrom, r.openTo + 1L, TO_AIR);
        if (carving) {
            // A carver may not cut under, beside or into a lake above the sea (61), nor a sky lake's basin, its brook or
            // its rim (74): its tunnel would drain the lake.
            fill(out, baseY, n, r.lakeGuardBottom, r.lakeGuardTop + 1L, ROCK);
            fill(out, baseY, n, r.skyGuardFrom, r.skyGuardTo + 1L, ROCK);
        }
        // A sky lake on a high island (74): still water in its basin, walled in by the island's rim.
        fill(out, baseY, n, r.skyFloor + 1L, r.skyTop, TO_WATER);
        // An island's spring (61): a source scheduled to flow, it pours into its fall beside it.
        fill(out, baseY, n, r.waterSpringY, r.waterSpringY + 1L, TO_WATER | SPRING);
        // A recorded waterfall of ring 4 (59): falling water down to where it lands, only its lowest blocks let flow.
        fill(out, baseY, n, r.waterFallBottom, r.waterFallTop + 1L, TO_FALLING_WATER);
        fill(out, baseY, n, r.waterFallBottom, Math.min(r.waterFallTop, r.waterFallFlow) + 1L, TO_FALLING_WATER | SPRING);
        // A lava fall from a floating island (39f): falling lava from the spring down to where it lands; only its lowest
        // blocks are let flow, so it spreads there. Lava against open air: the one allowed exception.
        fill(out, baseY, n, r.fallBottom, r.fallTop + 1L, TO_FALLING_LAVA);
        fill(out, baseY, n, r.fallBottom, Math.min(r.fallTop, r.fallFlow) + 1L, TO_FALLING_LAVA | SPRING);
        // A lava spring on a floating island or volcano (39d): scheduled to flow, it pours off the edge.
        fill(out, baseY, n, r.springY, r.springY + 1L, TO_LAVA | SPRING);
        // Ring 4's dry bottom (59); ring 5 is dry (82): no sea, no aquifer water or lava, never scheduled to flow; the seals
        // and dams hold rock.
        fill(out, baseY, n, Long.MIN_VALUE, r.dryBelow, TO_AIR);
        fill(out, baseY, n, r.airFrom, Long.MAX_VALUE, TO_AIR);
        fill(out, baseY, n, Long.MIN_VALUE, r.sealBelow, ROCK);
    }

    /**
     * Vanilla's answer with the rule on it ({@link #afterVanilla}), everywhere. The rule only changes at the lava
     * layer's top and the column's bounds, so it is worked out once per stretch between them.
     */
    private static void paintAsk(Rules r, int baseY, byte[] out, int n) {
        int top = RingFluidPicker.LAVA_LAYER_TOP;
        long from = baseY;
        long end = (long) baseY + n;
        while (from < end) {
            long to = nextBound(from, end, top);
            to = nextBound(from, to, top + 1);
            to = nextBound(from, to, r.dryFrom);
            to = nextBound(from, to, r.waterOnlyFrom);
            int y = (int) from;
            int rule = afterVanilla(r, y, false, false) == ROCK ? 1 : 0;
            rule |= afterVanilla(r, y, true, false) << 1 | afterVanilla(r, y, false, true) << 3;
            fill(out, baseY, n, from, to, ASK + rule);
            from = to;
        }
    }

    /** {@code bound} if it lies in (from, to), else {@code to}. */
    private static long nextBound(long from, long to, long bound) {
        return bound > from && bound < to ? bound : to;
    }

    /**
     * The rules on vanilla's answer (not null) at y: ring 3 stays dry, rings 2 and 4 have water instead of lava. The
     * outcome: {@link #KEEP}, {@link #ROCK}, {@link #TO_AIR} or {@link #TO_WATER}.
     */
    private static int afterVanilla(Rules r, int y, boolean water, boolean lava) {
        if (r.dryFrom != Integer.MAX_VALUE && y == RingFluidPicker.LAVA_LAYER_TOP) {
            // 47e (B, INT3): a closed floor of rock over vanilla's lava layer in ring 3, whatever vanilla puts on it: deep
            // water placed right on it, or flowing down into a cave from higher up, met the lava and crusted over when it
            // first flowed, a moment that depends on the loading order. The layer stays lava below.
            return ROCK;
        }
        if (y >= r.dryFrom && water) {
            return TO_AIR;
        }
        if (r.dryFrom != Integer.MAX_VALUE && y >= RingFluidPicker.LAVA_LAYER_TOP && lava) {
            // 42b (V2 = A): no local aquifer lava in ring 3 (its pools stood against the air of their caves); lava lies
            // only where the terrain puts it. Vanilla's lava layer below stays.
            return TO_AIR;
        }
        if (y < r.waterOnlyFrom || !(lava || water)) {
            return KEEP;
        }
        return y == RingFluidPicker.LAVA_LAYER_TOP ? ROCK : TO_WATER;
    }

    /**
     * What is known of vanilla's answer without asking, between {@code askFrom} and {@code askTo}: over the noise aquifer
     * its sky (air, with the rule on it; not in the carvers' pass, see {@link VanillaSky}), over the disabled aquifer the
     * fluid picker's status at every block.
     */
    private void paintKnown(int x, int z, int baseY, byte[] out, int n, int askFrom, int askTo) {
        int from = Math.max(askFrom, baseY);
        int to = (int) Math.min(askTo, (long) baseY + n);
        if (sky != null && !carving) {
            from = Math.max(from, sky.from(x, z, to));
            for (int y = from; y < to; y++) {
                out[y - baseY] = (byte) (AFTER[(out[y - baseY] - ASK) << 2 | 1] == ROCK ? ROCK : TO_AIR);
            }
        } else if (plain != null && from < to) {
            // Each half only where it is asked (the Nether never reaches below the lava layer's top).
            int top = RingFluidPicker.LAVA_LAYER_TOP;
            Aquifer.FluidStatus above = to > top ? plain.status(x, z, false) : null;
            Aquifer.FluidStatus below = from < top ? plain.status(x, z, true) : null;
            for (int y = from; y < to; y++) {
                BlockState state = (y < top ? below : above).at(y);
                int outcome = AFTER[(out[y - baseY] - ASK) << 2 | kind(state)];
                // Vanilla's own instance only where it is one of the outcome states; else it stays asked.
                int known = outcome != KEEP ? outcome : state == AIR ? TO_AIR : state == WATER ? TO_WATER
                        : state == LAVA ? TO_LAVA : -1;
                if (known >= 0) {
                    out[y - baseY] = (byte) known;
                }
            }
        }
    }

    /** 42: in the openings of tubes, chambers and geodes air or their lava (its crust when asked); rock in their guard bands. */
    private void paintCaves(Rules r, int x, int z, int baseY, byte[] out, int n, int askFrom, int askTo) {
        int from = Math.max(Math.max(r.caveBottom, askFrom), baseY);
        int to = (int) Math.min(Math.min(r.caveTop + 1L, askTo), (long) baseY + n);
        for (int y = from; y < to; y++) {
            // The column again for every block: the neighbours' checks may reuse the object for another column.
            VolcanicField.Column column = volcanic.column(x, z);
            if (column.voidAt(y)) {
                // Lava in a gutter or a lake: a lip of crust where open air lies beside or below it (a gutter's steps).
                out[y - baseY] = (byte) (column.subLavaAt(y) ? LAVA_UNLESS_OPEN : TO_AIR);
            } else if (column.deepGuardAt(y)) {
                out[y - baseY] = ROCK;
            }
        }
    }

    /** 73: the net's own fill, laid out so that it never stands beside the wrong neighbour (never let flow). */
    private void paintTunnels(Ring4Tunnels.Span span, int baseY, byte[] out, int n) {
        if (!tunnelFills) {
            pick[TO_GAS] = TunnelFills.GAS;
            pick[TO_POOL] = TunnelFills.POOL;
            tunnelFills = true;
        }
        for (int k = 0; k < n; k++) {
            int y = baseY + k;
            if (carving && span.lifted(y)) {
                // A carver may not cut the rock around the net's sealed spaces (its tunnel would bring water).
                out[k] = ROCK;
                continue;
            }
            Ring4Tunnels.Fill fill = span.fillAt(y);
            if (fill != Ring4Tunnels.Fill.NONE) {
                int outcome = switch (fill) {
                    case WATER -> TO_WATER;
                    case GAS -> TO_GAS;
                    case POOL -> TO_POOL;
                    default -> TO_AIR;
                };
                out[k] = (byte) outcome;
            }
        }
    }

    /** Sets the entries of the blocks from {@code from} to {@code to} (exclusive, any range) to {@code value}. */
    private static void fill(byte[] out, int baseY, int n, long from, long to, int value) {
        // Clamped before subtracting: the open ends (Long.MIN_VALUE, Long.MAX_VALUE) would overflow.
        long low = Math.max(from, baseY);
        long high = Math.min(to, (long) baseY + n);
        if (low < high) {
            Arrays.fill(out, (int) (low - baseY), (int) (high - baseY), (byte) value);
        }
    }

    // ---------------------------------------------------------------- ring 3's terrain lava

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

    /**
     * The ring 4 terrain's surface in a column as the noise chunk builds it: the bilinear blend of its cell corners'
     * surfaces (with the islands' holes), like {@link #groundAt} for ring 3.
     */
    private double lakeSurface(int x, int z) {
        int x0 = Math.floorDiv(x, CELL) * CELL;
        int z0 = Math.floorDiv(z, CELL) * CELL;
        double fx = (x - x0) / (double) CELL;
        double fz = (z - z0) / (double) CELL;
        double s00 = islands.column(x0, z0).surface();
        double s10 = islands.column(x0 + CELL, z0).surface();
        double s01 = islands.column(x0, z0 + CELL).surface();
        double s11 = islands.column(x0 + CELL, z0 + CELL).surface();
        return (s00 * (1.0 - fx) + s10 * fx) * (1.0 - fz) + (s01 * (1.0 - fx) + s11 * fx) * fz;
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

    /** The block states of the net's fills (the mod's blocks, looked up once they are registered). */
    private static final class TunnelFills {
        private static final BlockState GAS = ModBlocks.SWAMP_GAS.get().defaultBlockState();
        private static final BlockState POOL = ModBlocks.TOXIC_POOL.get().defaultBlockState();
    }

    /**
     * The profiles of the columns a thread painted last, of any aquifer: one table per thread, so a noise chunk waiting for
     * its next step holds none. Direct-mapped on the low 4 bits of x and z (the noise chunk and its carvers stay inside
     * the chunk), keyed by aquifer and pass ({@link #owner}): a column of another chunk or of the carvers' pass is painted
     * anew.
     */
    private static final class Profiles {
        private static final ThreadLocal<Profiles> PER_THREAD = ThreadLocal.withInitial(Profiles::new);
        private static final int COLUMNS = 16 * 16;

        private final long[] owner = new long[COLUMNS];
        private final int[] keyX = new int[COLUMNS];
        private final int[] keyZ = new int[COLUMNS];
        private final boolean[] noLava = new boolean[COLUMNS];
        private final byte[][] entries = new byte[COLUMNS][];

        static Profiles forThread() {
            return PER_THREAD.get();
        }
    }

    @Override
    public boolean shouldScheduleFluidUpdate() {
        return spring | asked & vanilla.shouldScheduleFluidUpdate();
    }
}
