package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * {@code corruptedadventure:island_field}: the overworld terrain of ring 4: the base terrain ({@link Ring4Relief},
 * package 60) and the floating islands with their mirror lakes, lifts and waterfalls (61), on a grid of its own: ring
 * 3's {@link VolcanicField} caps a column at 6 islands and 8 solids, too few for ring 4's dense islands. As a density
 * function it is the island mask of a column (0..1); {@link IslandOutput} reads its other outputs (heights, the
 * per-block islands).
 * <p>
 * The biome source's height hook ({@code RingBiomeSource}) and the aquifer's water branch ({@code RingAquifer}) read it
 * through {@link #find} on the router, like {@link VolcanicField}.
 * <p>
 * Pure: a function of the seed (through the noises and {@code hash}), the ring layout and x, z. Decisions read their
 * inputs at exact positions, never through a {@code flat_cache}; the caches only remember results.
 */
public final class IslandField implements DensityFunction {
    public static final MapCodec<IslandField> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("hash").forGetter(f -> f.hash),
            Ring4Relief.MAP_CODEC.fieldOf("relief").forGetter(f -> f.relief),
            Settings.CODEC.optionalFieldOf("settings", Settings.DEFAULT).forGetter(f -> f.settings),
            DensityFunction.HOLDER_HELPER_CODEC.optionalFieldOf("plateau").forGetter(f -> java.util.Optional.ofNullable(f.plateau))
    ).apply(instance, (hash, relief, settings, plateau) -> new IslandField(hash, relief, settings, plateau.orElse(null),
            RingLayout::current, new Decisions())));

    public static final KeyDispatchDataCodec<IslandField> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** The order of overlapping islands ({@link #uppermost}): bottom, top, then the centre as a fixed key. */
    private static final Comparator<Island> OVERLAP = Comparator.comparingInt(Island::bottom).thenComparingInt(Island::top)
            .thenComparingDouble(Island::x).thenComparingDouble(Island::z);
    private static final long WORLD_SALT = 4004;
    /** The preliminary surface: the lowest surface on rings of these radii around a quart's centre, less a margin. */
    private static final double[] INITIAL_RADII = {6.0, 12.0};
    private static final double INITIAL_MARGIN = 2.0;
    /** Where {@link #deepestFloor} looks for a sinkhole's floor: its centre and rings at these shares of its radius. */
    private static final double[] SINKHOLE_PROBES = {0.0, 0.45, 0.8};
    /** The relief of 2026-10-20 keeps this far from the zones 3→4 and 4→5, then grows over RELIEF_FADE blocks. */
    private static final double RELIEF_GAP = 24.0;
    private static final double RELIEF_FADE = 64.0;
    /** The roughness noise mostly stays within ±0.5: scaled by this and held to ±1 (as the gorges' faces, 5a). */
    private static final double WALL_ROUGH_GAIN = 2.0;
    /** How far {@link #roughFade} looks for a wall column without an exact band. */
    private static final int ROUGH_FADE = 4;
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    /** Islands one column may hold (overlapping footprints at different heights; more are dropped). */
    static final int MAX_ISLANDS = 8;
    /** An island reaches at most this far from its centre (a giant: 150 across, stretched 1.15, ragged). */
    static final double MAX_REACH = 110.0;
    /** The calm first share of the zone 4→5 (no cracks; sealed under the sea by the aquifer), for the fluid rules. */
    public static final double SUNDERED_CALM_SHARE = Ring4Sundered.CALM_SHARE;
    /**
     * The share of the zone 3→4 that is ring 3's {@code steam_fens} rim (60 of 256 blocks); from there on lies
     * {@code verdant_falls}, ring 4's descent. The one value for the terrain mix ({@code ring_blend/overworld*}
     * {@code mix.ring_3}), the biome split (the preset's {@code transition_splits.ring_3.at}) and the water branch in
     * ring 3's half ({@code RingFluidPicker.verdantWater}); the JSON is tested against it.
     */
    public static final double VERDANT_EDGE_SHARE = 60.0 / 256.0;
    /** Recorded waterfalls of the islands per chunk at most (the cap of the water branch). */
    static final int FALLS_PER_CHUNK = 3;
    /** How far (blocks) past an island's reach its lift may stand: two out from the edge, plus the edge's block. */
    static final double LIFT_REACH = 4.0;
    /** A lift keeps this Manhattan distance (blocks) from every fall, as {@link IslandPlacement} does from its own. */
    static final int LIFT_FALL_CLEARANCE = 6;
    /** Recorded waterfalls of {@code verdant_falls} per chunk at most (Q4 = A: a cap of their own, besides the islands'). */
    static final int VERDANT_FALLS_PER_CHUNK = 6;
    /** How far (blocks) under the ground of {@code verdant_falls} the caves stay closed (no cave mouths on the descent). */
    private static final int VERDANT_CAVE_DEPTH = 24;
    /** What the stack adds to vanilla's caves around a lake above the sea (closes them). */
    private static final double CAVE_LIFT = 2.0;
    /** How far (blocks) under a lake's deepest floor the caves stay closed. */
    static final int CAVE_FLOOR_MARGIN = 14;
    /**
     * The half window (blocks) of {@link #gradient}; with {@link #slope}'s neighbour well within {@link ColumnTable#MARGIN},
     * so a chunk's edge reads the table's rim.
     */
    static final int SLOPE_REACH = 2;
    /** Σ dx² over the gradient's window: (2r + 1) · 2 Σ k² for k = 1..r. */
    private static final double SLOPE_MOMENT = (2 * SLOPE_REACH + 1) * SLOPE_REACH * (SLOPE_REACH + 1) * (2 * SLOPE_REACH + 1) / 3.0;

    private final NoiseHolder hash;
    private final Ring4Relief relief;
    private final Settings settings;
    /**
     * Ring 3's terrain ({@code ring_3/overworld/field}, a {@link VolcanicField}; the JSON key {@code plateau} is
     * historical): the rim height of {@code verdant_falls} and ring 4's input in the {@code steam_fens} strip
     * ({@link VerdantDescent}). Null: the rim stands at {@link VerdantDescent#NO_RING_3}.
     */
    @Nullable
    private final DensityFunction plateau;
    private volatile VolcanicField ring3;
    private final Supplier<RingLayout> layout;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final Decisions decisions;
    // The islands of the 3 x 3 cells around a cell, for recent cells (review 61 #6: lock-free, shared by threads).
    private final KeyedSlots<List<IslandShape>> neighbourhoods = new KeyedSlots<>();
    /** The descent of the zone 3→4, its horseshoe gorges and its basins (W-terrein 2c). */
    private final VerdantDescent descent;
    private final VerdantGorges gorges;
    private final VerdantBasins basins;
    /** The tunnel net (73). */
    private final Ring4Tunnels tunnels;
    /** 74: the world trees, sky lakes and rock arches (W-terrein-b). */
    private final IslandLandmarks landmarks;

    public IslandField(NoiseHolder hash, Ring4Relief relief, Settings settings) {
        this(hash, relief, settings, null, RingLayout::current, new Decisions());
    }

    /**
     * @param plateau ring 3's terrain for the rim of {@code verdant_falls}, or null
     * @param layout  the ring layout (the running server's; tests give their own)
     */
    IslandField(NoiseHolder hash, Ring4Relief relief, Settings settings, @Nullable DensityFunction plateau, Supplier<RingLayout> layout) {
        this(hash, relief, settings, plateau, layout, new Decisions());
    }

    private IslandField(NoiseHolder hash, Ring4Relief relief, Settings settings, @Nullable DensityFunction plateau,
                        Supplier<RingLayout> layout, Decisions decisions) {
        this.hash = hash;
        this.relief = relief;
        this.settings = settings;
        this.plateau = plateau;
        this.ring3 = plateau == null ? null : VolcanicField.resolveIfBound(plateau);
        this.layout = layout;
        this.decisions = decisions;
        this.descent = new VerdantDescent(this);
        this.gorges = new VerdantGorges(this, descent);
        this.basins = new VerdantBasins(this, descent, gorges);
        this.tunnels = new Ring4Tunnels(this);
        this.landmarks = new IslandLandmarks(this, decisions.landmarks);
    }

    /** Ring 3's terrain for the rim of {@code verdant_falls}, or null. */
    @Nullable
    VolcanicField ring3() {
        VolcanicField f = ring3;
        if (f == null && plateau != null) {
            f = VolcanicField.resolve(plateau);
            ring3 = f;
        }
        return f;
    }

    Settings settings() {
        return settings;
    }

    Ring4Relief relief() {
        return relief;
    }

    RingLayout ringLayout() {
        return layout.get();
    }

    Decisions decisions() {
        return decisions;
    }

    VerdantDescent descent() {
        return descent;
    }

    VerdantBasins basins() {
        return basins;
    }

    VerdantGorges gorges() {
        return gorges;
    }

    IslandLandmarks landmarks() {
        return landmarks;
    }

    // ---------------------------------------------------------------- density function

    @Override
    public double compute(FunctionContext context) {
        return column(context.blockX(), context.blockZ()).islandMask;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        NoiseHolder mapped = visitor.visitNoise(hash);
        Ring4Relief mappedRelief = relief.map(visitor);
        DensityFunction mappedPlateau = plateau == null ? null : plateau.mapAll(visitor);
        // A noise chunk's copy keeps the cell decisions of the random state's instance (same noises, same results). The
        // plateau is compared by the ring 3 field it resolves to: the noise chunk wraps it in gates and caches of its
        // own, so the wrappers never compare equal and every chunk re-decided its island cells (INT2: the biome step,
        // which builds the noise chunk, took 56 % of the generation time in cascade_vale).
        boolean same = mapped.equals(hash) && mappedRelief.equals(relief) && samePlateau(mappedPlateau);
        return visitor.apply(new IslandField(mapped, mappedRelief, settings, mappedPlateau, layout, same ? decisions : new Decisions()));
    }

    private boolean samePlateau(@Nullable DensityFunction mappedPlateau) {
        if (plateau == null || mappedPlateau == null) {
            return plateau == mappedPlateau;
        }
        if (mappedPlateau.equals(plateau)) {
            return true;
        }
        VolcanicField field = VolcanicField.resolveIfBound(plateau);
        return field != null && field.equals(VolcanicField.resolveIfBound(mappedPlateau));
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

    // Equality without the caches: the noise chunk's memo then shares one instance between all its users.
    @Override
    public boolean equals(Object o) {
        return o instanceof IslandField f && hash.equals(f.hash) && relief.equals(f.relief) && settings.equals(f.settings)
                && Objects.equals(plateau, f.plateau);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, relief, settings, plateau);
    }

    /** The first {@code island_field} anywhere inside {@code root} (e.g. a random state's final density), or null. */
    @Nullable
    public static IslandField find(DensityFunction root) {
        IslandField[] found = new IslandField[1];
        root.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (found[0] == null && function instanceof IslandField field) {
                    found[0] = field;
                }
                return function;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                return noise;
            }
        });
        return found[0];
    }

    /** {@link #resolve}, or null while {@code function} is a holder reference that is not bound yet. */
    @Nullable
    static IslandField resolveIfBound(DensityFunction function) {
        try {
            return resolve(function);
        } catch (IllegalStateException unbound) {
            return null;
        }
    }

    /** {@code function} as an {@code island_field}, looking through holders, markers and ring gates; null if none. */
    @Nullable
    static IslandField resolve(DensityFunction function) {
        DensityFunction current = function;
        for (int depth = 0; depth < 16 && current != null; depth++) {
            if (current instanceof IslandField field) {
                return field;
            }
            current = RingGated.unwrap(current);
        }
        return null;
    }

    // ---------------------------------------------------------------- read access (W-biomes, B, A; read only)

    /** All 2D values of a column (cached per thread around the current chunk). */
    public Column column(int x, int z) {
        Column column = columns.get(x, z, IslandField::reset);
        ensureSurface(column);
        ensureIslands(column);
        return column;
    }

    /**
     * The steepness of the ground at a column (decision 2026-10-15, verdant_falls' stone on steep slopes; blocks of rise
     * per block): the largest {@link #gradient} of the column and its eight neighbours. Unlike vanilla's
     * {@code minecraft:steep} it does not depend on the slope's direction and is the same at a chunk's edge (the
     * neighbours are columns of the field, not of the chunk). Taking the largest of the neighbours means that every
     * column over a threshold has a neighbour over it too: the stone of a threshold lies in patches, never in loose
     * columns.
     */
    public double slope(int x, int z) {
        double steepest = 0.0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                steepest = Math.max(steepest, gradient(x + dx, z + dz));
            }
        }
        return steepest;
    }

    /**
     * The length of the surface's gradient at a column: the slope of the least-squares plane through the
     * {@link Column#surface} of the (2 {@link #SLOPE_REACH} + 1)² columns around it, so a single deep column nearby
     * tilts it a little instead of marking a few loose columns. Remembered per column.
     */
    double gradient(int x, int z) {
        double known = column(x, z).gradient;
        if (!Double.isNaN(known)) {
            return known;
        }
        double sx = 0.0;
        double sz = 0.0;
        for (int dz = -SLOPE_REACH; dz <= SLOPE_REACH; dz++) {
            for (int dx = -SLOPE_REACH; dx <= SLOPE_REACH; dx++) {
                double h = column(x + dx, z + dz).surface;
                sx += dx * h;
                sz += dz * h;
            }
        }
        double gradient = Math.hypot(sx, sz) / SLOPE_MOMENT;
        // Stored only now: the lookups above may have moved the table's window and refilled earlier column objects.
        column(x, z).gradient = gradient;
        return gradient;
    }

    /**
     * The island values of a column only (review 60 #5): its islands, spring, lake and fall, without the base terrain's
     * noise; the surface values ({@link Column#height}, {@link Column#surface}, …) are not filled by this call.
     */
    public Column islandColumn(int x, int z) {
        Column column = columns.get(x, z, IslandField::reset);
        ensureIslands(column);
        return column;
    }

    /**
     * Whether the block is a climbing knob on a gorge's wall in {@code verdant_falls} (decision 2026-10-15): the knob
     * over a climbing ledge's end, or in front of the ledge path's opening. {@code ledge_strand} always hangs a strand
     * under one, so every climbing route goes on from ledge to ledge.
     */
    public boolean climbKnob(int x, int y, int z) {
        Column c = column(x, z);
        return c.gorgeBlocks != null && gorges.knob(c.gorgeBlocks, c.gorgeLocal, c.gorgeInside, y);
    }

    /** The climbing knobs ({@link #climbKnob}) in a box (block coordinates, inclusive), each block once. */
    public List<BlockPos> climbKnobs(int minX, int minZ, int maxX, int maxZ) {
        List<BlockPos> found = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int[] b : gorges.knobCandidates(minX, minZ, maxX, maxZ)) {
            BlockPos pos = new BlockPos(b[0], b[1], b[2]);
            if (seen.add(pos.asLong()) && climbKnob(b[0], b[1], b[2])) {
                found.add(pos);
            }
        }
        return found;
    }

    /** All islands whose footprint covers the column. */
    public List<Island> islandsAt(int x, int z) {
        List<Island> found = islandsWithin(x, z, x, z);
        found.removeIf(island -> !island.covers(x, z));
        return found;
    }

    /** The island of the column where islands overlap ({@link #uppermost}), or null. */
    @Nullable
    public Island islandAt(int x, int z) {
        return uppermost(islandsAt(x, z));
    }

    /**
     * The one rule for overlapping islands (review 59): the island with the highest bottom, then the highest top, then
     * the largest centre x and z, so the choice never depends on the order of the list. Null for none.
     */
    @Nullable
    public static Island uppermost(Collection<Island> islands) {
        return islands.stream().max(OVERLAP).orElse(null);
    }

    /** The islands whose footprint may reach into the box (block coordinates, inclusive). */
    public List<Island> islandsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Island> found = new ArrayList<>();
        for (IslandShape shape : shapesWithin(minX, minZ, maxX, maxZ)) {
            Island island = shape.view();
            if (island.reaches(minX, minZ, maxX, maxZ)) {
                found.add(island);
            }
        }
        return found;
    }

    /**
     * The positions of the lifts (updrafts) in the box: beside about one in four middle and giant islands, none near a
     * waterfall. Pure; the lift feature fills the column of each one.
     */
    public List<Lift> liftsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Lift> found = new ArrayList<>();
        for (IslandShape shape : shapesWithin(minX, minZ, maxX, maxZ)) {
            // A lift stands two blocks out from an edge within the island's reach: skip islands too far off to have
            // one in the box, so a chunk does not decide the details of every island in the cells around it.
            double out = shape.reach() + LIFT_REACH;
            if (shape.x() + out < minX || shape.x() - out > maxX + 1 || shape.z() + out < minZ || shape.z() - out > maxZ + 1) {
                continue;
            }
            Lift lift = details(shape).lift();
            if (lift != null && lift.x() >= minX && lift.x() <= maxX && lift.z() >= minZ && lift.z() <= maxZ && clearOfFalls(lift)) {
                found.add(lift);
            }
        }
        return found;
    }

    /**
     * Whether no waterfall the terrain places comes near the lift (review lift package: placement only keeps the lift
     * away from its own island's falls; a neighbouring island's fall may stand right beside or in its column).
     */
    private boolean clearOfFalls(Lift lift) {
        int r = LIFT_FALL_CLEARANCE - 1;
        for (Waterfall fall : waterfallsWithin(lift.x() - r, lift.z() - r, lift.x() + r, lift.z() + r)) {
            if (Math.abs(fall.x() - lift.x()) + Math.abs(fall.z() - lift.z()) < LIFT_FALL_CLEARANCE) {
                return false;
            }
        }
        return true;
    }

    /**
     * The waterfalls in the box (off islands and down the descent of {@code verdant_falls}; the world trees follow in
     * 74), as the terrain places them: at most {@link #FALLS_PER_CHUNK} of the islands' and
     * {@link #VERDANT_FALLS_PER_CHUNK} of the descent's per chunk.
     */
    public List<Waterfall> waterfallsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Waterfall> found = new ArrayList<>();
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                for (Fall fall : fallsOfChunk(cx, cz)) {
                    if (fall.fallX() >= minX && fall.fallX() <= maxX && fall.fallZ() >= minZ && fall.fallZ() <= maxZ) {
                        found.add(new Waterfall(fall.fallX(), fall.fallZ(), fall.springY(), fall.bottom()));
                    }
                }
            }
        }
        return found;
    }

    /** The world trees whose reach (crown, roots) comes into the box (74). */
    public List<WorldTree> worldTrees(int minX, int minZ, int maxX, int maxZ) {
        return landmarks.worldTrees(minX, minZ, maxX, maxZ);
    }

    /**
     * Whether a piece of floating debris (sparring 2026-10-20, B2-3 a) has a block in the column (backlog r. 426: the
     * trees, rocks and ground plants stay off the debris, whose top the surface heightmaps show as the ground). Pure.
     */
    public boolean inDebris(int x, int z) {
        return landmarks.inDebris(x, z);
    }

    /** The island pairs (74: for the vines between islands, 75) whose facing edges come into the box. */
    public List<IslandPair> islandPairs(int minX, int minZ, int maxX, int maxZ) {
        return landmarks.islandPairs(minX, minZ, maxX, maxZ);
    }

    /** The sky lakes and dried-up bowls (74) of the islands whose basin comes into the box. */
    public List<SkyLake> skyLakes(int minX, int minZ, int maxX, int maxZ) {
        return landmarks.skyLakes(minX, minZ, maxX, maxZ);
    }

    /**
     * Whether a world tree has a block in the column (74; for the surface rule). Away from the trees without the column
     * itself: the surface pass asks this of every ring 4 column, whose column the table may no longer hold.
     */
    public boolean hasTree(int x, int z) {
        return landmarks.nearTree(x, z) && islandColumn(x, z).tree != null;
    }

    /** What the block is of a world tree (74; {@code corruptedadventure:world_tree}), {@link WorldTreePart#NONE} if none. */
    public WorldTreePart treePart(int x, int y, int z) {
        WorldTreeShape.Slice tree = islandColumn(x, z).tree;
        return tree == null ? WorldTreePart.NONE : tree.part(x, y, z);
    }

    /**
     * Decor D (2026-10-19): the horseshoe gorges of {@code verdant_falls} of the arc cell of (x, z) and its two
     * neighbours (a gorge never reaches past a neighbouring cell), for the giant tree beside each.
     */
    public List<GorgeSite> gorgesNear(int x, int z) {
        double period = VerdantDescent.perimeter(ringLayout());
        double s = RingArc.wrap(Math.atan2(z, x) * VerdantDescent.midRadius(ringLayout()), period);
        int k = (int) Math.floor(s / gorges.cellSize());
        List<GorgeSite> sites = new ArrayList<>(3);
        for (int dk = -1; dk <= 1; dk++) {
            VerdantGorges.Gorge g = gorges.gorge(k + dk);
            if (g != null && sites.stream().noneMatch(site -> site.index() == g.index())) {
                sites.add(new GorgeSite(g.index(), g.cx(), g.cz(), g.ox(), g.oz(), g.radius(), g.depth()));
            }
        }
        return sites;
    }

    /**
     * Decor D (2026-10-19): the land bridges of the zone 4→5 ({@code sundered_reach}) of the bridge cell of (x, z) and
     * its two neighbours, for the split rock on each.
     */
    public List<LandBridge> landBridgesNear(int x, int z) {
        double period = Ring4Sundered.perimeter(ringLayout());
        double start = Ring4Sundered.zoneStart(ringLayout());
        double end = Ring4Sundered.zoneEnd(ringLayout());
        double mid = 0.5 * (start + end);
        int count = RingArc.cellCount(period, Ring4Sundered.BRIDGE_CELL);
        int k = (int) Math.floor(Ring4Sundered.along(ringLayout(), x, z) / (period / count));
        List<LandBridge> bridges = new ArrayList<>(3);
        for (int dk = -1; dk <= 1; dk++) {
            int index = Math.floorMod(k + dk, count);
            if (bridges.stream().noneMatch(bridge -> bridge.index() == index)) {
                double[] bridge = Ring4Sundered.bridge(salt(), index, period);
                bridges.add(new LandBridge(index, bridge[0] / mid, bridge[1], start, end));
            }
        }
        return bridges;
    }

    /** The ring layout's effective distance of (x, z) (with the boundary noise): what the zones are measured in. */
    public double effectiveDistance(double x, double z) {
        return ringLayout().effectiveDistance(x, z);
    }

    // ---------------------------------------------------------------- outputs ({@link IslandOutput})

    /**
     * A lower bound of the surface around a column (the lowest surface on rings of 6 and 12 blocks around the quart's
     * centre, 8 points each, less 2; the sinkholes and island holes near it counted at their deepest), for the
     * preliminary surface: vanilla's aquifer decides from it whether open space below the sea level holds the sea, so
     * it must not lie above the floor of a lake, a river or a fjord (pitfall Y 55). Remembered per world on the 4-block
     * grid.
     */
    double initialHeight(int x, int z) {
        int qx = x & ~3;
        int qz = z & ~3;
        long key = KeyedSlots.key(qx, qz);
        Double known = decisions.initialHeights.get(key);
        if (known == null) {
            known = lowestAround(qx + 2, qz + 2);
            decisions.remember(decisions.initialHeights, key, known);
        }
        return known;
    }

    private double lowestAround(int x, int z) {
        double lowest = surfaceUncached(x, z);
        for (double radius : INITIAL_RADII) {
            for (int i = 0; i < 8; i++) {
                double angle = (i + radius / 16.0) * Math.PI / 4.0;
                lowest = Math.min(lowest, surfaceUncached(x + radius * Math.cos(angle), z + radius * Math.sin(angle)));
            }
        }
        lowest = Math.min(lowest, sinkholeFloorNear(x, z));
        lowest = Math.min(lowest, holeFloorNear(x, z));
        lowest = Math.min(lowest, verdantFloorNear(x, z));
        return lowest - INITIAL_MARGIN;
    }

    /**
     * A lower bound of the floors of the water of {@code verdant_falls} within 12 blocks of (x, z): its pools, gullies
     * and lakes (narrower than the rings of {@link #lowestAround} may see) and the foot river's bed; +infinity elsewhere.
     */
    private double verdantFloorNear(int x, int z) {
        VerdantDescent.Point p = descent.locate(x, z, new VerdantDescent.Point());
        if (p.kind != VerdantDescent.Point.DESCENT) {
            return Double.POSITIVE_INFINITY;
        }
        double reach = INITIAL_RADII[INITIAL_RADII.length - 1];
        double floor = basins.floorNear(p, reach);
        VerdantGorges.Gorge gorge = gorges.at(x, z, p, new VerdantGorges.Local());
        if (gorge != null) {
            floor = Math.min(floor, VerdantGorges.floorNear(gorge, p));
        }
        if (Math.abs(p.u - p.profile.river()) < p.profile.bedHalf() + reach + 12.0) {
            floor = Math.min(floor, VerdantDescent.RIVER_BED);
        }
        return floor;
    }

    /**
     * The deepest floor of a peat sinkhole whose bowl comes within 12 blocks of (x, z) (the rings of
     * {@link #lowestAround}), or +infinity. Only sinkholes the peat really cuts count (review 60 #2): elsewhere the
     * cell's centre may lie in a trough or a fjord, which must not pull the surface down under dry land.
     */
    private double sinkholeFloorNear(int x, int z) {
        return sinkholeFloorWithin(x, z, INITIAL_RADII[INITIAL_RADII.length - 1]);
    }

    /** The deepest floor of a peat sinkhole (that the peat really cuts) whose bowl comes within {@code ring} of (x, z). */
    double sinkholeFloorWithin(double x, double z, double ring) {
        double reach = ring + Ring4Relief.SINKHOLE_MAX_RADIUS;
        int cell = Ring4Relief.SINKHOLE_CELL;
        double floor = Double.POSITIVE_INFINITY;
        for (int i = Math.floorDiv((int) (x - reach), cell); i <= Math.floorDiv((int) (x + reach), cell); i++) {
            for (int j = Math.floorDiv((int) (z - reach), cell); j <= Math.floorDiv((int) (z + reach), cell); j++) {
                double[] hole = Ring4Relief.sinkhole(i, j, salt());
                if (hole == null || Math.hypot(hole[0] - x, hole[1] - z) - hole[2] > ring) {
                    continue;
                }
                floor = Math.min(floor, deepestSinkholeFloor(i, j, hole));
            }
        }
        return floor;
    }

    /**
     * The deepest floor of the sinkhole {@code hole} of sinkhole cell (i, j) that the peat cuts, +infinity if none;
     * remembered per world. For lower bounds of the ground (the preliminary surface, the tunnels' ceilings).
     */
    double deepestSinkholeFloor(int i, int j, double[] hole) {
        return decisions.once(decisions.sinkholeFloors, KeyedSlots.key(i, j), () -> deepestFloor(hole));
    }

    /**
     * The deepest floor of a sinkhole that the peat cuts (+infinity if none): its centre and two rings inside it. Not
     * only the centre: where the hole lies on a slope (the band's blend, the relief of 2026-10-20) or the peat thins out
     * across it, its floor is deepest on the low side.
     */
    private double deepestFloor(double[] hole) {
        double floor = Double.POSITIVE_INFINITY;
        Ring4Relief.Sample s = new Ring4Relief.Sample();
        for (double share : SINKHOLE_PROBES) {
            int points = share == 0.0 ? 1 : 8;
            for (int k = 0; k < points; k++) {
                double angle = k * Math.PI / 4.0 + share;
                double px = hole[0] + share * hole[2] * Math.cos(angle);
                double pz = hole[1] + share * hole[2] * Math.sin(angle);
                sampleRelief(px, pz, s);
                if (!Double.isNaN(s.sinkholeFloor)) {
                    floor = Math.min(floor, s.sinkholeFloor);
                }
            }
        }
        return floor;
    }

    /** The deepest floor of an island hole whose hole comes within 14 blocks of (x, z), or +infinity. */
    private double holeFloorNear(int x, int z) {
        double floor = Double.POSITIVE_INFINITY;
        double reach = INITIAL_RADII[INITIAL_RADII.length - 1] + 2.0;
        for (IslandShape shape : shapesNear(x, z)) {
            double holeReach = shape.reach() * IslandShape.HOLE_SCALE + reach;
            if (Math.abs(shape.x() - x) < holeReach && Math.abs(shape.z() - z) < holeReach
                    && shape.holeDistance(x, z) < 1.0 + reach / shape.holeRadius()) {
                floor = Math.min(floor, shape.deepestFloor());
            }
        }
        return floor;
    }

    /** Ring 4's base terrain at a position ({@link Ring4Relief#sample}), with the band and the relief's share there. */
    void sampleRelief(double x, double z, Ring4Relief.Sample s) {
        relief.sample(x, z, band(x, z), reliefShare(x, z), s, salt());
    }

    /**
     * How much of the relief of 2026-10-20 a position gets (0..1): none in and next to the zones 3→4 and 4→5, full from
     * {@link #RELIEF_GAP} + {@link #RELIEF_FADE} blocks away. So {@code verdant_falls} (whose descent reads ring 4's
     * height up to 16 blocks past its zone) and {@code sundered_reach} (and the rim before it) stay as they were.
     */
    double reliefShare(double x, double z) {
        RingLayout l = layout.get();
        double effective = l.effectiveDistance(x, z);
        double fromInner = effective - VerdantDescent.zoneStart(l) - VerdantDescent.width(l);
        double toOuter = Ring4Sundered.zoneStart(l) - Ring4Sundered.RIM_BEFORE - effective;
        return Ring4Relief.smooth(fromInner, RELIEF_GAP, RELIEF_GAP + RELIEF_FADE)
                * Ring4Relief.smooth(toOuter, RELIEF_GAP, RELIEF_GAP + RELIEF_FADE);
    }

    /** The base terrain's surface at a position (no holes), exact. */
    double baseHeight(double x, double z) {
        Ring4Relief.Sample s = new Ring4Relief.Sample();
        sampleRelief(x, z, s);
        return s.height;
    }

    /** Whether a peat sinkhole's bowl lies at the position (its floor cut per block below the base terrain). */
    boolean inSinkhole(double x, double z) {
        Ring4Relief.Sample s = new Ring4Relief.Sample();
        sampleRelief(x, z, s);
        return !Double.isNaN(s.sinkholeFloor);
    }

    /**
     * The lowest open ground at a position: the surface with the descent of {@code verdant_falls}, the islands' holes
     * and outlets, and a peat sinkhole's floor (which the terrain cuts per block, review 61 #6); exact, not in the column
     * cache. On the descent the ground of the block column the position lies in (remembered per world where it needs no
     * relief, {@link Decisions#verdantGrounds}).
     */
    double surfaceUncached(double x, double z) {
        return surfaceUncached(x, z, false);
    }

    /**
     * {@link #surfaceUncached}, less the roughness of a fjord's wall there (relief 2026-10-20: {@link #thin} may cut the
     * face that far under the surface): the lowest a block of the column may be open, for ceilings that must stay covered.
     */
    double faceUncached(double x, double z) {
        return surfaceUncached(x, z, true);
    }

    private double surfaceUncached(double x, double z, boolean rough) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        boolean outside = true;
        Ring4Relief.Sample s = null;
        double height;
        // Off the zone exactly where locate(x, z) would say so; on it, the block column's point decides.
        boolean inZone = descent.inZone(x, z);
        Double ground = inZone ? decisions.verdantGrounds.peek(KeyedSlots.key(bx, bz)) : null;
        if (ground != null) {
            // The block column's ground without ring 4's relief, remembered (vf-perf2): the rings of lowestAround meet
            // the same block columns from neighbouring quarts.
            outside = false;
            height = ground;
        } else {
            VerdantDescent.Point p = new VerdantDescent.Point();
            p.kind = VerdantDescent.Point.OUTSIDE;
            p.body = -1;
            if (inZone) {
                descent.locate(bx, bz, p);
            }
            if (p.kind == VerdantDescent.Point.OUTSIDE || VerdantDescent.needsRelief(p)) {
                s = new Ring4Relief.Sample();
                sampleRelief(x, z, s);
            }
            outside = p.kind == VerdantDescent.Point.OUTSIDE;
            height = outside ? s.height : verdantGround(bx, bz, p, s, new VerdantBasins.Water(), null);
            if (s == null) {
                // A function of the block column alone (no relief at x, z): the same for every position in it.
                decisions.verdantGrounds.put(KeyedSlots.key(bx, bz), height);
            }
        }
        double surface = withHoles(height, x, z, shapesNear(bx, bz), null);
        surface = rimmed(x, z, surface);
        Sunder sunder = sunder(x, z);
        if (sunder.kind() != Sunder.NONE) {
            surface = Math.min(surface, Ring4Sundered.ABYSS_FLOOR);
        }
        if (rough && outside && s.fjordEdge) {
            surface -= s.wallRough;
        }
        return !outside || Double.isNaN(s.sinkholeFloor) ? surface : Math.min(surface, s.sinkholeFloor);
    }

    // ---------------------------------------------------------------- verdant_falls (W-terrein 2c)

    /**
     * The ground of a column of the zone 3→4 ({@link VerdantDescent}): ring 4's input in the {@code steam_fens} strip,
     * else the treads with the basins ({@link VerdantBasins}, the cuts behind the falls' springs included) and the foot
     * river; beyond the river's middle ring 4's own terrain {@code s} blends in (needed only there,
     * {@link VerdantDescent#needsRelief}). {@code w} gets the basins' values.
     */
    private double verdantGround(int x, int z, VerdantDescent.Point p, @Nullable Ring4Relief.Sample s, VerdantBasins.Water w,
                                 @Nullable VerdantGorges.Shape gorge) {
        if (gorge != null) {
            gorge.zone = null;
            gorge.blocks = null;
        }
        if (p.kind == VerdantDescent.Point.STRIP) {
            w.height = p.treads;
            w.body = -1;
            w.lakeTop = Integer.MIN_VALUE;
            w.rim = 0.0;
            w.cliff = 0.0;
            return p.treads;
        }
        VerdantGorges.Local l = new VerdantGorges.Local();
        VerdantGorges.Gorge g = gorges.at(x, z, p, l);
        if (g != null) {
            // Part 2: a horseshoe gorge instead of the basins.
            gorges.shape(g, x, z, p, l, true, w, gorge == null ? new VerdantGorges.Shape() : gorge);
            if (gorge != null) {
                gorge.zone = g;
                // The per-block wall stands on the same ground as the column: with the foot river cut in.
                double relief = s == null ? Double.NaN : s.height;
                gorge.inside = VerdantDescent.withRiver(p, gorge.inside, relief);
                gorge.outside = VerdantDescent.withRiver(p, gorge.outside, relief);
            }
        } else {
            basins.shape(x, z, p, true, w);
        }
        return VerdantDescent.withRiver(p, w.height, s == null ? Double.NaN : s.height);
    }

    /**
     * The exact ground of a column of the zone 3→4 (for tests): the same as the column's {@link Column#height}, without
     * the column cache.
     */
    double verdantHeight(int x, int z) {
        VerdantDescent.Point p = descent.locate(x, z, new VerdantDescent.Point());
        if (p.kind == VerdantDescent.Point.OUTSIDE) {
            return baseHeight(x, z);
        }
        Ring4Relief.Sample s = null;
        if (VerdantDescent.needsRelief(p)) {
            s = new Ring4Relief.Sample();
            sampleRelief(x, z, s);
        }
        return verdantGround(x, z, p, s, new VerdantBasins.Water(), null);
    }

    /**
     * The surface {@code base} with the holes and outlets of {@code shapes} cut into it; {@code hole} (if given) gets
     * the island-hole mask (0..1, 1 well inside a hole).
     */
    private static double withHoles(double base, double x, double z, List<IslandShape> shapes, @Nullable double[] hole) {
        double surface = base;
        for (IslandShape shape : shapes) {
            double reach = shape.groundReach();
            if (Math.abs(x - shape.x()) > reach || Math.abs(z - shape.z()) > reach) {
                continue;
            }
            double floor = shape.holeFloor(x, z);
            if (!Double.isNaN(floor)) {
                surface = Math.min(surface, floor);
                if (hole != null) {
                    hole[0] = Math.max(hole[0], Mth.clamp((1.0 - shape.holeDistance(x, z)) * 4.0, 0.0, 1.0));
                }
            }
            surface = Math.min(surface, shape.outletFloor(x, z));
        }
        return surface;
    }

    /**
     * Per block: +1 inside an island, −1 in a carved opening (a spring, a peat sinkhole's bowl), the exact surface in
     * the bands of the fjords' walls and over the descent of {@code verdant_falls} ({@link #withExactBand}), 0
     * elsewhere (the terrain decides).
     */
    double thin(int x, int y, int z) {
        Column c = columns.get(x, z, IslandField::reset);
        ensureIslands(c);
        double thin = c.thin(y);
        if (thin == 0.0 && c.tree != null && c.tree.solid(x, y, z)) {
            // 74: a world tree's trunk, branch, root or leaves.
            return 1.0;
        }
        if (thin == 0.0) {
            // 73: the tunnel net, exact per block (two comparisons where a column has none).
            ensureTunnels(c);
            if (y >= c.tunnelLo && y <= c.tunnelHi && c.tunnelSpan.opens(y)) {
                return -1.0;
            }
        }
        if (thin == 0.0 && c.sinkholeDisc) {
            ensureSurface(c);
            if (!Double.isNaN(c.sinkholeFloor) && y >= Mth.ceil(c.sinkholeFloor) && y <= c.height + 2.0) {
                return -1.0;
            }
        }
        if (thin == 0.0 && c.verdant) {
            // Part 2: a niche or tunnel behind a basin fall (open), a rock bridge over a brook (rock).
            if (y >= c.carveLo && y <= c.carveHi) {
                return -1.0;
            }
            if (y >= c.solidLo && y <= c.solidHi) {
                return 1.0;
            }
        }
        if (thin == 0.0 && (c.fjordEdge || c.verdant)) {
            if (c.bandLo == Integer.MIN_VALUE) {
                c = withExactBand(x, z);
            }
            if (y >= c.bandLo && y <= c.bandHi) {
                if (c.gorgeBlocks != null) {
                    // A gorge's wall, its ledge path, niches and climbing ledges, per block.
                    return gorges.block(c.gorgeBlocks, c.gorgeLocal, c.gorgeInside, c.gorgeOutside, c.gorgeNearFall, c.gorgeEarY,
                            x, y, z);
                }
                if (c.roughAmplitude > 0.0) {
                    // A fjord's rough wall (relief 2026-10-20): ledges, overhangs and jutting rock, like the gorges' faces.
                    return y < c.surface + c.roughAmplitude * roughness(x, y, z) ? 1.0 : -1.0;
                }
                return y < c.surface ? 1.0 : -1.0;
            }
        }
        return thin;
    }

    /**
     * The exact surface per block where the 4-block grid cannot show it: a fjord's wall and rim (heuvelfix 2b, Q6: the
     * height kinks there within one cell, so the grid's bilinear surface misses it by up to 7 blocks in a sawtooth), and
     * the whole descent of {@code verdant_falls} (2c: its treads, coves, gullies, lakes and lips are cut per block, so
     * its water is walled in exactly). From a block under the lower of the exact and the bilinear surface to a block
     * over the higher one, the exact surface decides; around it the stack agrees. For the fjords not where a hole, a lake
     * or its cave band lies (their rims are measured on the grid); never where another ring's density mixes in (a
     * transition zone, but not the zone 3→4 beyond {@link #VERDANT_EDGE_SHARE}: pure ring 4 there), nor in an island's
     * hole. Returns the column with its band ({@code bandLo > bandHi}: none).
     */
    private Column withExactBand(int x, int z) {
        Column c = columns.get(x, z, IslandField::reset);
        ensureIslands(c);
        double exact = c.surface;
        boolean banded = (c.verdant || c.lakeTop == Integer.MIN_VALUE && c.caveLiftFrom > c.caveLiftTo) && c.islandHole == 0.0
                && pureRing4(layout.get().sampleOverworld(x, z));
        double bilinear = banded ? gridSurface(x, z) : exact;
        double amplitude = banded && c.wallRough > 0.0 ? c.wallRough * roughFade(x, z) : 0.0;
        // The corners may have moved the table's window: the same column again (recomputed if so).
        c = columns.get(x, z, IslandField::reset);
        ensureIslands(c);
        // A rough fjord wall moves the exact surface up and down by up to its amplitude (relief 2026-10-20).
        c.roughAmplitude = amplitude;
        double lo = Math.min(exact - amplitude, bilinear);
        double hi = Math.max(exact + amplitude, bilinear);
        if (c.gorgeBlocks != null) {
            // The wall's own range: from the gorge floor to the collar.
            lo = Math.min(lo, Math.min(c.gorgeInside, c.gorgeOutside));
            hi = Math.max(hi, Math.max(c.gorgeInside, c.gorgeOutside));
        }
        c.bandLo = banded ? Mth.floor(lo) - 1 : Integer.MAX_VALUE;
        c.bandHi = banded ? Mth.ceil(hi) + 1 : Integer.MIN_VALUE;
        return c;
    }

    /** The roughness of a fjord's wall at a block, −1..1 (times the column's amplitude: how far the face moves). */
    double roughness(int x, int y, int z) {
        return Mth.clamp(WALL_ROUGH_GAIN * relief.rough(x, y, z), -1.0, 1.0);
    }

    /**
     * 0..1: how much of its roughness a fjord wall's column keeps, by the nearest column within {@link #ROUGH_FADE}
     * blocks (8 directions) that would be rough but has no exact band (a hole, its lake and cave band, an outlet: the grid
     * shows it smooth). Without it the face would step by up to the roughness there.
     */
    private double roughFade(int x, int z) {
        for (int d = 1; d <= ROUGH_FADE; d++) {
            for (int[] dir : DIRECTIONS) {
                Column n = column(x + d * dir[0], z + d * dir[1]);
                if (n.wallRoughRaw > 0.0 && !(n.fjordEdge && n.lakeTop == Integer.MIN_VALUE && n.caveLiftFrom > n.caveLiftTo
                        && n.islandHole == 0.0)) {
                    return Ring4Relief.smooth(d, 0.0, ROUGH_FADE + 1.0);
                }
            }
        }
        return 1.0;
    }

    /** Whether ring 4's density stands alone at a position (ring 4 itself, or the zone 3→4 past ring 3's mix share). */
    private static boolean pureRing4(nl.timtoys.corruptedadventure.ring.RingPosition position) {
        if (!position.inTransition()) {
            return position.ring() == nl.timtoys.corruptedadventure.ring.Ring.RING_4;
        }
        return position.inner() == nl.timtoys.corruptedadventure.ring.Ring.RING_3 && position.blend() >= VERDANT_EDGE_SHARE;
    }

    /** The surface as the stack's interpolation shows it: the bilinear blend of the 4-block grid's corners around (x, z). */
    double gridSurface(int x, int z) {
        int cx = x & ~3;
        int cz = z & ~3;
        double tx = (x - cx) / 4.0;
        double tz = (z - cz) / 4.0;
        double s00 = column(cx, cz).surface;
        double s10 = column(cx + 4, cz).surface;
        double s01 = column(cx, cz + 4).surface;
        double s11 = column(cx + 4, cz + 4).surface;
        double near = s00 + tx * (s10 - s00);
        double far = s01 + tx * (s11 - s01);
        return near + tz * (far - near);
    }

    /** Per block: what the stack adds to the density of vanilla's caves: closed under and beside a lake above the sea. */
    double caveLift(int x, int y, int z) {
        Column c = columns.get(x, z, IslandField::reset);
        ensureIslands(c);
        if (y >= c.caveLiftFrom && y <= c.caveLiftTo) {
            return CAVE_LIFT;
        }
        // 73: closed around the net's sealed spaces (domes, the dry net, dry sinkholes).
        ensureTunnels(c);
        return y >= c.tunnelLiftLo && y <= c.tunnelLiftHi && c.tunnelSpan.lifted(y) ? CAVE_LIFT : 0.0;
    }

    // ---------------------------------------------------------------- the tunnel net (73)

    /** Fills a column's tunnel values once ({@link Ring4Tunnels#span}); its island values must be there. */
    private void ensureTunnels(Column c) {
        if (c.tunnelsFilled) {
            return;
        }
        Ring4Tunnels.Span span = tunnels.span(c);
        c.tunnelSpan = span;
        if (span != null) {
            c.tunnelLo = span.openLo;
            c.tunnelHi = span.openHi;
            c.tunnelLiftLo = span.liftMin;
            c.tunnelLiftHi = span.liftMax;
        }
        c.tunnelsFilled = true;
    }

    /**
     * The tunnel net in a column, for the aquifer: what fills its open blocks and where carvers stay out; null where
     * the net has nothing.
     */
    @Nullable
    public Ring4Tunnels.Span tunnelSpan(int x, int z) {
        Column c = columns.get(x, z, IslandField::reset);
        ensureIslands(c);
        ensureTunnels(c);
        return c.tunnelSpan;
    }

    /** What fills a block of the tunnel net ({@link Ring4Tunnels.Fill#NONE} outside it); the DF {@code tunnels}. */
    public Ring4Tunnels.Fill tunnelFill(int x, int y, int z) {
        Ring4Tunnels.Span span = tunnelSpan(x, z);
        return span == null ? Ring4Tunnels.Fill.NONE : span.netFillAt(y);
    }

    /**
     * The net's wall (77: ores, crystals and tracks in the tunnels' walls, floors and ceilings): for a block outside the
     * net, what fills the net beside it (one of its six neighbours: below, above, then the sides), else
     * {@link Ring4Tunnels.Fill#NONE}. {@code WATER}/{@code DOME_AIR}: a wall of the flooded net (mythical band);
     * {@code DRY_AIR}/{@code GAS}/{@code POOL}: of the dry net (blighted band); never both (they keep 4 blocks of rock
     * apart). Pure, like {@link #tunnelFill}. It does not say the block is rock: at an entrance's mouth, in a sinkhole's
     * bowl or where a vanilla cave meets a flooded tunnel it may be water or air; check the block itself.
     */
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public Ring4Tunnels.Fill tunnelWall(int x, int y, int z) {
        if (tunnelFill(x, y, z) != Ring4Tunnels.Fill.NONE) {
            return Ring4Tunnels.Fill.NONE;
        }
        Ring4Tunnels.Span own = tunnelSpan(x, z);
        if (own != null) {
            Ring4Tunnels.Fill vertical = own.netFillAt(y - 1);
            if (vertical == Ring4Tunnels.Fill.NONE) {
                vertical = own.netFillAt(y + 1);
            }
            if (vertical != Ring4Tunnels.Fill.NONE) {
                return vertical;
            }
        }
        for (int[] d : SIDES) {
            Ring4Tunnels.Fill side = tunnelFill(x + d[0], y, z + d[1]);
            if (side != Ring4Tunnels.Fill.NONE) {
                return side;
            }
        }
        return Ring4Tunnels.Fill.NONE;
    }

    /** The tunnels whose reach comes into the box (block coordinates, inclusive): centrelines with heights and widths. */
    public List<TunnelPath> tunnelsWithin(int minX, int minZ, int maxX, int maxZ) {
        return tunnels.pathsWithin(minX, minZ, maxX, maxZ);
    }

    /** The halls and air domes of the tunnel net whose reach comes into the box. */
    public List<TunnelRoom> tunnelRoomsWithin(int minX, int minZ, int maxX, int maxZ) {
        return tunnels.roomsWithin(minX, minZ, maxX, maxZ);
    }

    // ---------------------------------------------------------------- cells

    /** A pure per-world value for the hash grids, from the hash noise. */
    long salt() {
        Long known = decisions.salt;
        if (known == null) {
            // Pure: racing threads compute the same value.
            known = SeededHash.hash(hash, 4321, -8765, WORLD_SALT);
            decisions.salt = known;
        }
        return known;
    }

    NoiseHolder hashNoise() {
        return hash;
    }

    private double band(double x, double z) {
        return Ring4Band.of(layout.get()).value(x, z);
    }

    /** The candidates of a grid cell, decided once per world ({@link IslandPlacement#candidates}). */
    List<IslandShape> candidates(int i, int j) {
        return decisions.once(decisions.candidates, KeyedSlots.key(i, j),
                () -> landmarks.withTrees(i, j, IslandPlacement.candidates(this, i, j)));
    }

    /** The islands of a grid cell, decided once per world ({@link IslandPlacement#place}). */
    List<IslandShape> cell(int i, int j) {
        return decisions.once(decisions.cells, KeyedSlots.key(i, j), () -> IslandPlacement.place(this, i, j));
    }

    /** Whether a candidate stays ({@link IslandPlacement#kept}), decided once per world. */
    boolean kept(IslandShape shape) {
        return decisions.once(decisions.kept, shape, () -> IslandPlacement.kept(this, shape));
    }

    /** The islands of the 3 x 3 cells around the cell of (x, z): all that can reach the column (review 59 #6). */
    List<IslandShape> shapesNear(int x, int z) {
        int cell = settings.cellSize();
        int i = Math.floorDiv(x, cell);
        int j = Math.floorDiv(z, cell);
        return neighbourhoods.get(KeyedSlots.key(i, j), key -> {
            List<IslandShape> all = new ArrayList<>();
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    all.addAll(cell(i + di, j + dj));
                }
            }
            return List.copyOf(all);
        });
    }

    /** The islands of the cells that can reach into the box. */
    List<IslandShape> shapesWithin(int minX, int minZ, int maxX, int maxZ) {
        int cell = settings.cellSize();
        List<IslandShape> found = new ArrayList<>();
        for (int i = Math.floorDiv(minX, cell) - 1; i <= Math.floorDiv(maxX, cell) + 1; i++) {
            for (int j = Math.floorDiv(minZ, cell) - 1; j <= Math.floorDiv(maxZ, cell) + 1; j++) {
                found.addAll(cell(i, j));
            }
        }
        return found;
    }

    /** The lake, falls and lift of an island, decided once per world ({@link IslandPlacement#details}). */
    Details details(IslandShape shape) {
        return decisions.once(decisions.details, shape, () -> shape.kind() == IslandShape.TREE
                ? landmarks.treeDetails(shape) : IslandPlacement.details(this, shape));
    }

    /**
     * The falls whose falling column lies in chunk (cx, cz): the islands' at most {@link #FALLS_PER_CHUNK} and those of
     * {@code verdant_falls} at most {@link #VERDANT_FALLS_PER_CHUNK} (each the first ones by x, then z, then the
     * spring's height, so the cap does not depend on the order chunks are made in; the clods' falls only in the islands'
     * places left over), together in that order.
     */
    List<Fall> fallsOfChunk(int cx, int cz) {
        long key = KeyedSlots.key(cx, cz);
        List<Fall> known = decisions.chunkFalls.get(key);
        if (known == null) {
            int minX = cx << 4;
            int minZ = cz << 4;
            List<Fall> falls = new ArrayList<>();
            List<Fall> clodFalls = new ArrayList<>();
            for (IslandShape shape : shapesWithin(minX, minZ, minX + 15, minZ + 15)) {
                if (Math.abs(shape.x() - (minX + 8)) > shape.reach() + 10 || Math.abs(shape.z() - (minZ + 8)) > shape.reach() + 10) {
                    continue;
                }
                for (Fall fall : details(shape).falls()) {
                    if (fall.fallX() >> 4 == cx && fall.fallZ() >> 4 == cz) {
                        (shape.kind() == IslandShape.CLOD ? clodFalls : falls).add(fall);
                    }
                }
            }
            List<Fall> kept = new ArrayList<>(capped(falls, FALLS_PER_CHUNK));
            // Decor rollout C: the clods' small falls only take what the cap leaves (never a bigger island's fall).
            kept.addAll(capped(clodFalls, FALLS_PER_CHUNK - kept.size()));
            // verdant_falls (2c): the falls of the streams along the chunk, where the chunk lies on the descent.
            RingLayout ringLayout = layout.get();
            double u = ringLayout.effectiveDistance(minX + 8, minZ + 8) - VerdantDescent.zoneStart(ringLayout);
            if (u > -16.0 && u < VerdantDescent.width(ringLayout) + 16.0) {
                List<Fall> verdant = new ArrayList<>();
                double s = RingArc.arc(minX + 8, minZ + 8, VerdantDescent.midRadius(ringLayout));
                List<VerdantBasins.Fall> near = new ArrayList<>(basins.fallsNear(s, 16.0));
                near.addAll(gorges.fallsNear(s, 16.0));
                for (VerdantBasins.Fall fall : near) {
                    Fall f = fall.fall();
                    if (f.fallX() >> 4 == cx && f.fallZ() >> 4 == cz && !verdant.contains(f)) {
                        verdant.add(f);
                    }
                }
                kept.addAll(capped(verdant, VERDANT_FALLS_PER_CHUNK));
            }
            known = List.copyOf(kept);
            decisions.remember(decisions.chunkFalls, key, known);
        }
        return known;
    }

    /** The first {@code cap} falls by x, then z, then the spring's height. */
    private static List<Fall> capped(List<Fall> falls, int cap) {
        falls.sort(Comparator.comparingInt(Fall::fallX).thenComparingInt(Fall::fallZ).thenComparingInt(Fall::springY)
                .thenComparingInt(Fall::springX).thenComparingInt(Fall::springZ));
        return falls.subList(0, Math.min(cap, falls.size()));
    }

    private static void reset(Column column, int x, int z) {
        column.reset(x, z);
    }

    /** Computes the base terrain and the surface (with the holes) of a column once. */
    private void ensureSurface(Column c) {
        if (c.surfaceFilled) {
            return;
        }
        VerdantDescent.Point p = descent.locate(c.x, c.z, new VerdantDescent.Point());
        boolean outside = p.kind == VerdantDescent.Point.OUTSIDE;
        Ring4Relief.Sample s = null;
        if (outside || VerdantDescent.needsRelief(p)) {
            s = new Ring4Relief.Sample();
            sampleRelief(c.x, c.z, s);
        }
        if (outside) {
            c.height = s.height;
            c.river = s.river;
            c.fjord = s.fjord;
            c.sinkholeFloor = s.sinkholeFloor;
        } else {
            // The zone 3→4 (2c): steam_fens' strip, the descent of verdant_falls with its basins and the foot river.
            VerdantBasins.Water w = new VerdantBasins.Water();
            VerdantGorges.Shape gorge = new VerdantGorges.Shape();
            c.height = verdantGround(c.x, c.z, p, s, w, gorge);
            if (s == null) {
                // The same ground surfaceUncached asks of this block column (vf-perf2): no relief, a fresh gorge shape.
                decisions.verdantGrounds.put(KeyedSlots.key(c.x, c.z), c.height);
            }
            c.verdant = p.kind == VerdantDescent.Point.DESCENT;
            c.arc = p.s;
            c.body = w.body;
            c.gorgeZone = gorge.zone;
            // Before the basins' features: a rock bridge marks its deck as rock (cliff).
            c.rim = w.rim;
            c.cliff = w.cliff;
            if (gorge.zone != null) {
                c.gorgeBlocks = gorge.blocks;
                c.gorgeLocal = gorge.local;
                c.gorgeInside = gorge.inside;
                c.gorgeOutside = gorge.outside;
                c.gorgeNearFall = gorge.nearFall;
                c.gorgeEarY = gorge.earY;
                c.gorgeOpenLo = gorge.openLo;
                c.gorgeOpenHi = gorge.openHi;
                // A rock arch over the gully.
                c.solidLo = gorge.solidLo;
                c.solidHi = gorge.solidHi;
            } else if (c.verdant) {
                basins.features(c.x, c.z, p, c);
            }
            c.verdantLakeTop = w.lakeTop;
            double beyond = s == null ? 0.0 : Ring4Relief.smooth(p.u, p.profile.river(), p.width);
            c.river = Math.max(VerdantDescent.river(p), s == null ? 0.0 : beyond * s.river);
            c.fjord = s == null ? 0.0 : beyond * s.fjord;
        }
        double[] hole = {0.0};
        c.surface = withHoles(c.height, c.x, c.z, shapesNear(c.x, c.z), hole);
        c.islandHole = hole[0];
        // The sundered zone 4→5 (63): cracks drop the ground into the abyss, loose chunks float above it.
        c.surface = rimmed(c.x, c.z, c.surface);
        Sunder sunder = sunder(c.x, c.z);
        c.sundered = sunder.strength();
        c.sunderKind = sunder.kind();
        if (sunder.kind() == Sunder.CRACK) {
            c.surface = Math.min(c.surface, Ring4Sundered.ABYSS_FLOOR);
        } else if (sunder.kind() == Sunder.LOOSE) {
            // A loose chunk: no ground under it, its own thickness floats at its offset above the ground it was.
            // Not flat: the detail noise lifts the top a few blocks and thickens or thins the slab (review 63: blocky).
            double detail = relief.detailAt(c.x, c.z);
            // 5a: rounded, not a slab: thin at its outline, full thickness LOOSE_TAPER blocks in, the top dipping a little
            // towards the rim.
            double taper = Ring4Relief.smooth(sunder.inside(), 0.0, Ring4Sundered.LOOSE_TAPER);
            double top = c.surface + sunder.offset() + 3.0 * detail - 2.0 * (1.0 - taper);
            double thickness = sunder.thickness() * (1.0 + 0.4 * detail) * (0.3 + 0.7 * Math.sqrt(taper));
            c.addIsland((int) Math.ceil(top - thickness), (int) Math.floor(top));
            c.surface = Math.min(c.surface, Ring4Sundered.ABYSS_FLOOR);
        }
        // The descent's ground is exact per block: the aquifer takes its floor, not the grid's.
        c.exactFloor = c.verdant && c.surface == c.height ? c.surface : Double.NaN;
        // Only the relief's own fjord: not where the descent, a hole, the rim or the sundering changed the surface.
        c.fjordEdge = outside && s.fjordEdge && c.surface == c.height;
        c.wallRoughRaw = outside ? s.wallRough : 0.0;
        c.wallRough = c.fjordEdge ? s.wallRough : 0.0;
        c.surfaceFilled = true;
    }

    /** The ground at least {@link Ring4Sundered#RIM_TOP} in the zone 4→5 and the rim before it (5a), else unchanged. */
    private double rimmed(double x, double z, double surface) {
        RingLayout ringLayout = layout.get();
        return Ring4Sundered.inRim(ringLayout, ringLayout.effectiveDistance(x, z)) ? Math.max(surface, Ring4Sundered.RIM_TOP) : surface;
    }

    /** How a column lies in the sundered zone (63): its kind, the loose chunk's offset and thickness, the strength. */
    private record Sunder(int kind, double offset, double thickness, double strength, double inside) {
        static final int NONE = 0;
        static final int CRACK = 1;
        static final int LOOSE = 2;
    }

    /** The sundering of a column (63); {@link Sunder#NONE} outside the zone 4→5, on a bridge, and in ground. */
    private Sunder sunder(double x, double z) {
        RingLayout ringLayout = layout.get();
        double effective = ringLayout.effectiveDistance(x, z);
        if (effective < Ring4Sundered.zoneStart(ringLayout) || effective > Ring4Sundered.zoneEnd(ringLayout)) {
            return new Sunder(Sunder.NONE, 0.0, 0.0, 0.0, 0.0);
        }
        double strength = Ring4Sundered.strength(ringLayout, effective);
        if (strength <= 0.0 || Ring4Sundered.calm(ringLayout, effective)) {
            return new Sunder(Sunder.NONE, 0.0, 0.0, strength, 0.0);
        }
        long salt = salt();
        double along = Ring4Sundered.along(ringLayout, x, z);
        if (Ring4Sundered.onBridge(salt, along, Ring4Sundered.perimeter(ringLayout))) {
            return new Sunder(Sunder.NONE, 0.0, 0.0, strength, 0.0);
        }
        // 5a: the grid is bent first, so the cracks curve.
        double s = Ring4Sundered.WARP_SCALE;
        double wx = x + Ring4Sundered.WARP * relief.warpAt(x * s, z * s);
        double wz = z + Ring4Sundered.WARP * relief.warpAt(z * s + 5000.5, x * s - 5000.5);
        double[] chunk = Ring4Sundered.chunk(salt, wx, wz);
        int i = (int) chunk[0];
        int j = (int) chunk[1];
        double crack = Ring4Sundered.crack(strength);
        double[] loose = Ring4Sundered.loose(salt, i, j, strength);
        if (loose != null) {
            // A loose chunk with a ragged, rounded outline inside its cell; the rest of the cell is abyss.
            double outline = crack + Ring4Sundered.LOOSE_MARGIN + Ring4Sundered.RAG * relief.crumble(wx * 0.5, wz * 0.5);
            if (chunk[2] < outline) {
                return new Sunder(Sunder.CRACK, 0.0, 0.0, strength, 0.0);
            }
            return new Sunder(Sunder.LOOSE, loose[0], loose[1], strength, chunk[2] - outline);
        }
        if (chunk[2] < crack) {
            return new Sunder(Sunder.CRACK, 0.0, 0.0, strength, 0.0);
        }
        return new Sunder(Sunder.NONE, 0.0, 0.0, strength, 0.0);
    }

    /**
     * Computes the islands of a column once: their solid blocks, a spring, the lake above the sea (and the band kept
     * free of caves), the recorded fall through the column.
     */
    private void ensureIslands(Column c) {
        if (c.islandsFilled) {
            return;
        }
        // The loose chunks' solids come with the surface, so they are added before the islands' own.
        ensureSurface(c);
        c.islandsFilled = true;
        double[] sinkhole = Ring4Relief.sinkhole(Math.floorDiv(c.x, Ring4Relief.SINKHOLE_CELL),
                Math.floorDiv(c.z, Ring4Relief.SINKHOLE_CELL), salt());
        c.sinkholeDisc = sinkhole != null && Math.hypot(c.x - sinkhole[0], c.z - sinkhole[1]) < sinkhole[2];
        double px = c.x + 0.5;
        double pz = c.z + 0.5;
        // Review 61 #3: a column inside a hole takes that hole's lake (the lowest, where holes overlap; a hole the sea
        // fills has none), a column only in lake bands the lowest of those lakes, so a higher lake never stands in a
        // lower hole.
        boolean inHole = false;
        int holeLake = Integer.MAX_VALUE;
        int bandLake = Integer.MAX_VALUE;
        for (IslandShape shape : shapesNear(c.x, c.z)) {
            double reach = shape.groundReach();
            if (Math.abs(px - shape.x()) > reach || Math.abs(pz - shape.z()) > reach) {
                continue;
            }
            double d = shape.distance(px, pz);
            if (d < 1.0) {
                int[] interval = shape.interval(px, pz);
                if (interval != null) {
                    c.addIsland(interval[0], interval[1]);
                    c.islandMask = Math.max(c.islandMask, Mth.clamp((1.0 - d) * 4.0, 0.0, 1.0));
                }
            }
            // The hole and its lake on block positions, like the surface.
            double dh = shape.holeDistance(c.x, c.z);
            double radius = shape.holeRadius();
            if (dh >= 1.0 + IslandShape.CAVE_BAND / radius) {
                continue;
            }
            Details details = details(shape);
            if (dh < 1.0) {
                inHole = true;
                holeLake = Math.min(holeLake, details.lakeTop());
            } else if (details.lakeTop() != Integer.MIN_VALUE && dh < 1.0 + IslandShape.LAKE_BAND / radius) {
                bandLake = Math.min(bandLake, details.lakeTop());
            }
            if (details.lakeTop() != Integer.MIN_VALUE) {
                c.caveLiftFrom = Math.min(c.caveLiftFrom, details.lakeBottom() - CAVE_FLOOR_MARGIN);
                c.caveLiftTo = Math.max(c.caveLiftTo, details.lakeTop() + 2);
            }
            for (Fall fall : details.falls()) {
                if (fall.springX() == c.x && fall.springZ() == c.z && fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                    c.springY = fall.springY();
                }
            }
        }
        int lake = inHole ? holeLake : bandLake;
        c.lakeTop = lake == Integer.MAX_VALUE ? Integer.MIN_VALUE : lake;
        if (c.verdant && c.lakeTop == Integer.MIN_VALUE && c.caveLiftFrom > c.caveLiftTo && c.islandHole == 0.0) {
            verdantWater(c);
        }
        for (Fall fall : fallsOfChunk(c.x >> 4, c.z >> 4)) {
            if (fall.fallX() == c.x && fall.fallZ() == c.z && fall.springY() > c.fallTop) {
                c.fallTop = fall.springY();
                c.fallBottom = fall.bottom();
                // Only the block right above the water flows (review 61 #1).
                c.fallFlowBelow = fall.bottom();
            }
        }
        // 74: a world tree's blocks, a sky lake's basin, a rock arch.
        landmarks.fill(c);
    }

    /**
     * The water of a column of {@code verdant_falls} (2c): its body's still water ({@link VerdantBasins}), a spring if a
     * kept fall leaves from it, and the band kept free of caves: no cave mouths on the descent, so nothing opens a pool,
     * a gully or a lake from below or beside (the aquifer keeps that band rock under the exact floor).
     */
    private void verdantWater(Column c) {
        c.lakeTop = c.verdantLakeTop;
        int top = Math.max(Mth.ceil(c.surface), c.lakeTop) + 2;
        double low = c.gorgeZone != null ? Math.min(c.surface, c.gorgeInside) : c.surface;
        c.caveLiftFrom = Mth.floor(low) - VERDANT_CAVE_DEPTH;
        c.caveLiftTo = top;
        // Part 2: open space the field carves under the ground stays air (the aquifer would fill it with rock).
        if (c.carveLo <= c.carveHi) {
            c.openFrom = c.carveLo;
            c.openTo = c.carveHi;
        }
        if (c.gorgeOpenLo <= c.gorgeOpenHi) {
            c.openFrom = Math.min(c.openFrom, c.gorgeOpenLo);
            c.openTo = Math.max(c.openTo, Math.min(c.gorgeOpenHi, Mth.floor(c.surface) - 2));
        }
        if (c.gorgeZone != null) {
            VerdantGorges.GorgeFall fall = gorges.springAt(c.gorgeZone, c.x, c.z);
            if (fall != null) {
                Fall f = fall.fall().fall();
                if (fallsOfChunk(f.fallX() >> 4, f.fallZ() >> 4).contains(f)) {
                    c.springY = f.springY();
                }
            }
            return;
        }
        if (c.body >= 1) {
            for (VerdantBasins.Fall fall : basins.fallsOfBody(c.arc, c.body)) {
                Fall f = fall.fall();
                if (f.springX() == c.x && f.springZ() == c.z && fallsOfChunk(f.fallX() >> 4, f.fallZ() >> 4).contains(f)) {
                    c.springY = f.springY();
                }
            }
        }
    }

    // ---------------------------------------------------------------- data

    /**
     * The tunable numbers of the field. Every field is optional in JSON; missing ones take {@link #DEFAULT}.
     *
     * @param cellSize cell of the island grid (an island's radius never exceeds it)
     */
    public record Settings(int cellSize) {
        public static final Settings DEFAULT = new Settings(128);

        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(32, 1024).optionalFieldOf("cell_size", DEFAULT.cellSize).forGetter(Settings::cellSize)
        ).apply(instance, Settings::new));
    }

    /**
     * A floating island: centre, footprint radius (blocks) and its lowest and highest block.
     */
    public record Island(double x, double z, double radius, int bottom, int top) {
        public boolean covers(int blockX, int blockZ) {
            double dx = blockX - x;
            double dz = blockZ - z;
            return dx * dx + dz * dz <= radius * radius;
        }

        boolean reaches(int minX, int minZ, int maxX, int maxZ) {
            return x + radius >= minX && x - radius <= maxX && z + radius >= minZ && z - radius <= maxZ;
        }
    }

    /** A lift (updraft) from {@code bottom} (the water's top block) up to {@code top} in one column. */
    public record Lift(int x, int z, int bottom, int top) {
    }

    /** A recorded waterfall: falling water in one column from {@code top} down to {@code bottom}. */
    public record Waterfall(int x, int z, int top, int bottom) {
    }

    /**
     * A world tree (74): its trunk's axis, standing on its island at {@code baseY}, its crown reaching {@code top}; its
     * crown and roots reach at most {@code radius} blocks from the axis.
     */
    public record WorldTree(int x, int z, int baseY, int top, int radius) {
    }

    /**
     * A horseshoe gorge of {@code verdant_falls} (decor D): its cell's index, the amphitheatre's centre, the outward
     * direction (unit, towards the foot river), the half ellipse's half width and depth. Its wall line, wobble aside, is
     * centre + side · halfWidth · sin φ − out · depth · cos φ (φ in [−π/2, π/2], side = (−outZ, outX)).
     */
    public record GorgeSite(int index, double centreX, double centreZ, double outX, double outZ, double halfWidth, double depth) {
    }

    /**
     * A land bridge of the zone 4→5 (decor D): its cell's index, its middle line's angle (radians, as {@code atan2(z, x)}
     * up to whole turns), its half width along the zone (blocks) and the zone's inner and outer effective distance.
     */
    public record LandBridge(int index, double angle, double halfWidth, double zoneStart, double zoneEnd) {
    }

    /**
     * Two nearby islands (74, for the vines between them, 75): the column of each one's edge facing the other with its
     * top and bottom block; {@code arch}: a rock arch joins them.
     */
    public record IslandPair(Island first, Island second, int firstX, int firstZ, int firstTop, int firstBottom,
                             int secondX, int secondZ, int secondTop, int secondBottom, boolean arch) {
    }

    /**
     * A sky lake on a large high island (74): its island's centre, the basin's mean radius (blocks), the top water block
     * ({@code dry}: a dried-up bowl in {@code withered_gardens}, this its brim without water) and the length of its
     * brook to a fall (0: none).
     */
    public record SkyLake(int x, int z, double radius, int waterY, boolean dry, int brook) {
    }

    /**
     * A tunnel of the net (73): what fills it ({@link Ring4Tunnels.Fill#WATER} flooded, {@code DRY_AIR} dry), its
     * half height, and its centreline. Its cross-section is an ellipse in the vertical plane: a column at horizontal
     * distance d from the line is open over {@code y ± halfHeight · sqrt(1 − (d / halfWidth)²)}.
     */
    public record TunnelPath(Ring4Tunnels.Fill fill, double halfHeight, List<TunnelPoint> centreline) {
    }

    /** A point of a tunnel's centreline: its centre height and half width there. */
    public record TunnelPoint(double x, double y, double z, double halfWidth) {
    }

    /**
     * A hall at a node ({@code hall}) or a crest's air dome of the net (73): what fills it (water up to {@code level}
     * with air above; dry air; gas; a pool up to {@code level}), its centre, radius (the outline swings ±20 %) and its
     * lowest and highest block at the centre. {@code level} {@link Integer#MIN_VALUE} where it has none.
     */
    public record TunnelRoom(Ring4Tunnels.Fill fill, boolean hall, double x, double y, double z, double radius, int floor,
                             int top, int level) {
    }

    /**
     * An island's waterfall: the spring (a source that flows) at ({@code springX}, {@code springY}, {@code springZ}),
     * and the falling water beside it in column ({@code fallX}, {@code fallZ}) from {@code springY} down to
     * {@code bottom} (the first block above the water it falls into).
     */
    record Fall(int springX, int springZ, int springY, int fallX, int fallZ, int bottom) {
    }

    /**
     * What belongs to one island (stage 2, {@link IslandPlacement#details}): the top (exclusive) of its own lake above
     * the sea ({@link Integer#MIN_VALUE}: none, the sea fills the hole), its hole's deepest floor, its falls and lift.
     */
    record Details(int lakeTop, int lakeBottom, List<Fall> falls, @Nullable Lift lift) {
    }

    /** All 2D values of one column. Heights in blocks. */
    public static final class Column {
        boolean surfaceFilled;
        boolean islandsFilled;
        /** The base terrain's surface (without the islands' holes). */
        double height;
        /** The surface with the holes. */
        double surface;
        /** 0..1: a river's channel. */
        double river;
        /** 0..1: a fjord's channel. */
        double fjord;
        /** 0..1: an island in this column. */
        double islandMask;
        /** 0..1: how far the ground is sundered here (63; 0 outside the zone 4→5). */
        double sundered;
        /** {@link Ring4Sundered} kind of the column: {@code Sunder.NONE}, {@code CRACK} or {@code LOOSE}. */
        int sunderKind;
        /** 0..1: an island's mirror lake (1 well inside the hole). */
        double islandHole;
        /** Whether the column lies in the disc of a peat sinkhole's cell (then the base terrain decides its floor). */
        boolean sinkholeDisc;
        /** 0..1: an exact steep wall of {@code verdant_falls} (a cove's side under a fall; 2c). */
        double cliff;
        /** 0..1: a travertine rim of {@code verdant_falls} (a lake's threshold, a fall's lip; 2c). */
        double rim;
        /** The surface's gradient here ({@link IslandField#gradient}), NaN until asked for. */
        double gradient;
        /**
         * On the descent of {@code verdant_falls} (2c; past the steam_fens strip, up to the zone's outer edge): the
         * ground is exact per block ({@link #thin}). Part 2 adds its per-block shapes to these columns: the gorges'
         * walls ({@link #gorgeBlocks}), niches and tunnels ({@link #carveLo}), rock bridges ({@link #solidLo}).
         */
        boolean verdant;
        /** The column's position along the zone 3→4 (blocks of arc on its middle line), for the descent. */
        double arc;
        /** The descent's body (tread index, level 64 + 8 · body) the column lies on, −1 off the descent. */
        int body;
        /** The top (exclusive) of the basins' still water in the column ({@link VerdantBasins}), else MIN_VALUE. */
        int verdantLakeTop;
        /** The exact ground the aquifer takes as the floor of the water (the descent), NaN where the grid's counts. */
        double exactFloor;
        /** Part 2: the horseshoe gorge whose reach covers the column, else null. */
        @Nullable
        VerdantGorges.Gorge gorgeZone;
        /** The gorge whose wall shapes the column per block ({@link VerdantGorges#block}), else null; its values. */
        @Nullable
        VerdantGorges.Gorge gorgeBlocks;
        VerdantGorges.Local gorgeLocal;
        double gorgeInside;
        double gorgeOutside;
        double gorgeNearFall;
        int gorgeEarY;
        int gorgeOpenLo;
        int gorgeOpenHi;
        /** Part 2: open blocks under the ground (a niche or tunnel behind a basin fall), else carveLo > carveHi. */
        int carveLo;
        int carveHi;
        /** Part 2: rock above the ground (a rock bridge over a brook), else solidLo > solidHi. */
        int solidLo;
        int solidHi;
        /** Open space the aquifer leaves as air (carved by the field under the ground), else openFrom > openTo. */
        int openFrom;
        int openTo;
        /** The floor of a peat sinkhole, cut per block ({@link #thin}); NaN outside one. */
        double sinkholeFloor;
        /** On a fjord's wall or rim ({@link Ring4Relief.Sample#fjordEdge}): {@link #thin} may cut the surface per block. */
        boolean fjordEdge;
        /** On a fjord's wall: how far {@link #thin} may move the exact surface with the roughness (blocks; 0 elsewhere). */
        double wallRough;
        /** The relief's roughness of the column whatever its band (holes, lakes, outlets included): for the neighbours. */
        double wallRoughRaw;
        /** The roughness {@link #thin} applies: {@link #wallRough} faded by {@link #roughFade} (set with the band). */
        double roughAmplitude;
        /**
         * The blocks where the exact surface decides ({@link #withExactBand}); {@code bandLo} {@link Integer#MIN_VALUE}
         * until first asked, {@code bandLo > bandHi} for none.
         */
        int bandLo;
        int bandHi;
        /** The islands' solid blocks: {@link #islandCount} ranges [{@code islandLo}, {@code islandHi}] (inclusive). */
        int islandCount;
        final int[] islandLo = new int[MAX_ISLANDS];
        final int[] islandHi = new int[MAX_ISLANDS];
        /** A spring's block (open, filled with flowing water), else {@link Integer#MIN_VALUE}. */
        int springY;
        /** The top (exclusive) of a lake above the sea in this column, else {@link Integer#MIN_VALUE}. */
        int lakeTop;
        /** The blocks where vanilla's caves are closed (around a lake above the sea); none if from > to. */
        int caveLiftFrom;
        int caveLiftTo;
        /**
         * A recorded waterfall through this column: falling water in open space from {@link #fallBottom} up to and
         * including {@link #fallTop} (both {@link Integer#MIN_VALUE} without one); the aquifer lets the lowest blocks,
         * at and below {@link #fallFlowBelow}, flow so the water spreads where it lands.
         */
        int fallTop;
        int fallBottom;
        int fallFlowBelow;
        /** 74: the world tree's blocks in this column, else null. */
        @Nullable
        WorldTreeShape.Slice tree;
        /** 74: a sky lake's water up to (exclusive) {@code skyLakeTop} over its floor, else MIN_VALUE. */
        int skyLakeTop;
        /** 74: the last block of a sky lake's (or dry bowl's) floor, else MIN_VALUE. */
        int skyLakeFloor;
        /** 74: the blocks carvers may not cut around a sky lake, else from > to. */
        int skyGuardFrom;
        int skyGuardTo;
        int x;
        int z;
        // 73: the tunnel net in the column ({@link IslandField#ensureTunnels}): its span (null: none), the envelope of
        // the blocks it opens and of the caves it closes.
        boolean tunnelsFilled;
        @Nullable
        Ring4Tunnels.Span tunnelSpan;
        int tunnelLo;
        int tunnelHi;
        int tunnelLiftLo;
        int tunnelLiftHi;

        void reset(int x, int z) {
            this.x = x;
            this.z = z;
            surfaceFilled = false;
            islandsFilled = false;
            height = 0.0;
            surface = 0.0;
            river = 0.0;
            fjord = 0.0;
            islandMask = 0.0;
            islandHole = 0.0;
            sinkholeDisc = false;
            sinkholeFloor = Double.NaN;
            fjordEdge = false;
            wallRough = 0.0;
            wallRoughRaw = 0.0;
            roughAmplitude = 0.0;
            bandLo = Integer.MIN_VALUE;
            bandHi = Integer.MIN_VALUE;
            sundered = 0.0;
            sunderKind = Sunder.NONE;
            cliff = 0.0;
            rim = 0.0;
            gradient = Double.NaN;
            verdant = false;
            arc = 0.0;
            body = -1;
            verdantLakeTop = Integer.MIN_VALUE;
            exactFloor = Double.NaN;
            gorgeZone = null;
            gorgeBlocks = null;
            gorgeLocal = null;
            gorgeEarY = Integer.MIN_VALUE;
            gorgeOpenLo = Integer.MAX_VALUE;
            gorgeOpenHi = Integer.MIN_VALUE;
            carveLo = Integer.MAX_VALUE;
            carveHi = Integer.MIN_VALUE;
            solidLo = Integer.MAX_VALUE;
            solidHi = Integer.MIN_VALUE;
            openFrom = Integer.MAX_VALUE;
            openTo = Integer.MIN_VALUE;
            islandCount = 0;
            springY = Integer.MIN_VALUE;
            lakeTop = Integer.MIN_VALUE;
            caveLiftFrom = Integer.MAX_VALUE;
            caveLiftTo = Integer.MIN_VALUE;
            fallTop = Integer.MIN_VALUE;
            fallBottom = Integer.MIN_VALUE;
            fallFlowBelow = Integer.MIN_VALUE;
            tunnelsFilled = false;
            tunnelSpan = null;
            tunnelLo = Integer.MAX_VALUE;
            tunnelHi = Integer.MIN_VALUE;
            tunnelLiftLo = Integer.MAX_VALUE;
            tunnelLiftHi = Integer.MIN_VALUE;
            tree = null;
            skyLakeTop = Integer.MIN_VALUE;
            skyLakeFloor = Integer.MIN_VALUE;
            skyGuardFrom = Integer.MAX_VALUE;
            skyGuardTo = Integer.MIN_VALUE;
        }

        void addIsland(int lo, int hi) {
            if (islandCount < MAX_ISLANDS) {
                islandLo[islandCount] = lo;
                islandHi[islandCount] = hi;
                islandCount++;
            }
        }

        /** 74: lowers the top of the island range [lo, hi] to {@code top} (a sky lake's basin); false if none matches. */
        boolean lowerIsland(int lo, int hi, int top) {
            for (int k = 0; k < islandCount; k++) {
                if (islandLo[k] == lo && islandHi[k] == hi) {
                    islandHi[k] = top;
                    return true;
                }
            }
            return false;
        }

        /** 74: keeps carvers out of [from, to] (around a sky lake). */
        void guardSky(int from, int to) {
            skyGuardFrom = Math.min(skyGuardFrom, from);
            skyGuardTo = Math.max(skyGuardTo, to);
        }

        /** +1 inside an island, −1 in a spring's block, 0 elsewhere. */
        double thin(int y) {
            if (y == springY) {
                return -1.0;
            }
            for (int k = 0; k < islandCount; k++) {
                if (y >= islandLo[k] && y <= islandHi[k]) {
                    return 1.0;
                }
            }
            return 0.0;
        }

        public double height() {
            return height;
        }

        public double surface() {
            return surface;
        }

        public double islandMask() {
            return islandMask;
        }

        public double islandHole() {
            return islandHole;
        }

        public double sinkholeFloor() {
            return sinkholeFloor;
        }

        /** The lowest open ground of the column: {@link #surface}, or a peat sinkhole's floor below it. */
        public double lowestSurface() {
            return Double.isNaN(sinkholeFloor) ? surface : Math.min(surface, sinkholeFloor);
        }

        public double cliff() {
            return cliff;
        }

        public double rim() {
            return rim;
        }

        /** The exact floor under the column's water (the descent of {@code verdant_falls}), NaN where the grid's counts. */
        public double exactFloor() {
            return exactFloor;
        }

        /** The lowest block the aquifer leaves as air where open (carved under the ground; 2c part 2), else MAX_VALUE. */
        public int openFrom() {
            return openFrom;
        }

        /** The highest such block, else MIN_VALUE. */
        public int openTo() {
            return openTo;
        }

        public double sundered() {
            return sundered;
        }

        /** The top (exclusive) of the still sea of the abyss in this column (Y 0), else {@link Integer#MIN_VALUE}. */
        public int abyssSeaTop() {
            return sunderKind == Sunder.NONE ? Integer.MIN_VALUE : Ring4Sundered.SEA_TOP + 1;
        }

        /** The lowest block of the open abyss in this column (its floor), else {@link Integer#MAX_VALUE}. */
        public int abyssFloor() {
            return sunderKind == Sunder.NONE ? Integer.MAX_VALUE : (int) Math.floor(Ring4Sundered.ABYSS_FLOOR);
        }

        public int springY() {
            return springY;
        }

        public int lakeTop() {
            return lakeTop;
        }

        public int caveLiftFrom() {
            return caveLiftFrom;
        }

        public int caveLiftTo() {
            return caveLiftTo;
        }

        public int fallTop() {
            return fallTop;
        }

        public int fallBottom() {
            return fallBottom;
        }

        public int fallFlowBelow() {
            return fallFlowBelow;
        }

        /** 74: the top (exclusive) of a sky lake's still water in this column, else {@link Integer#MIN_VALUE}. */
        public int skyLakeTop() {
            return skyLakeTop;
        }

        /** 74: the floor's last block under a sky lake (or a dry bowl), else {@link Integer#MIN_VALUE}. */
        public int skyLakeFloor() {
            return skyLakeFloor;
        }

        /** 74: the lowest block carvers may not cut around a sky lake, else MAX_VALUE. */
        public int skyGuardFrom() {
            return skyGuardFrom;
        }

        /** 74: the highest such block, else MIN_VALUE. */
        public int skyGuardTo() {
            return skyGuardTo;
        }
    }

    /**
     * Per-world decisions and remembered results: pure functions of the seed and the cell, shared by all copies of one
     * wired field (the noise chunk makes a copy per chunk), so they are made once per world. Only a cache; a map is
     * cleared when it grows past {@link #LIMIT} entries.
     */
    static final class Decisions {
        static final int LIMIT = 100_000;
        final Map<Long, List<IslandShape>> candidates = new ConcurrentHashMap<>();
        final Map<IslandShape, Boolean> kept = new ConcurrentHashMap<>();
        final Map<Integer, VerdantDescent.Profile> verdantProfiles = new ConcurrentHashMap<>();
        final Map<Integer, Double> verdantRims = new ConcurrentHashMap<>();
        final Map<Integer, Double> verdantOuter = new ConcurrentHashMap<>();
        final Map<Integer, Double> verdantOuterMax = new ConcurrentHashMap<>();
        final Map<Integer, java.util.Optional<VerdantBasins.Stream>> verdantStreams = new ConcurrentHashMap<>();
        final Map<Long, java.util.Optional<VerdantBasins.Fall>> verdantFalls = new ConcurrentHashMap<>();
        final Map<Integer, java.util.Optional<VerdantGorges.Gorge>> verdantGorges = new ConcurrentHashMap<>();
        final Map<Integer, VerdantGorges.Details> verdantGorgeDetails = new ConcurrentHashMap<>();
        final Map<Long, java.util.Optional<VerdantBasins.Niche>> verdantNiches = new ConcurrentHashMap<>();
        final Map<Long, java.util.Optional<VerdantBasins.Bridge>> verdantBridges = new ConcurrentHashMap<>();
        final Map<Long, List<IslandShape>> cells = new ConcurrentHashMap<>();
        final Map<IslandShape, Details> details = new ConcurrentHashMap<>();
        final Map<Long, List<Fall>> chunkFalls = new ConcurrentHashMap<>();
        final Map<Long, Double> initialHeights = new ConcurrentHashMap<>();
        /** A peat sinkhole's deepest floor per cell ({@link #deepestFloor}), +infinity where the peat cuts none. */
        final Map<Long, Double> sinkholeFloors = new ConcurrentHashMap<>();
        /**
         * {@link #surfaceUncached} (vf-perf2): the ground of a block column of the zone 3→4 where it needs no relief, also
         * put by {@link #ensureSurface}.
         */
        final KeyedSlots<Double> verdantGrounds = new KeyedSlots<>(1 << 14);
        /** {@link VerdantDescent#rampAt} of whole block columns (NaN off the descent), also put by its {@code locate}. */
        final KeyedSlots<Double> verdantRamps = new KeyedSlots<>(1 << 14);
        /** 74: the world trees, sky lakes and island pairs. */
        final IslandLandmarks.Memo landmarks = new IslandLandmarks.Memo();
        volatile Long salt;

        <K, V> void remember(Map<K, V> map, K key, V value) {
            if (map.size() > LIMIT) {
                map.clear();
            }
            map.putIfAbsent(key, value);
        }

        /**
         * The remembered value of {@code key}, or {@code decide}'s (pure) value, remembered. Not {@code computeIfAbsent}:
         * a decision may ask other decisions of the same map.
         */
        <K, V> V once(Map<K, V> map, K key, java.util.function.Supplier<V> decide) {
            V known = map.get(key);
            if (known == null) {
                known = decide.get();
                remember(map, key, known);
            }
            return known;
        }
    }
}
