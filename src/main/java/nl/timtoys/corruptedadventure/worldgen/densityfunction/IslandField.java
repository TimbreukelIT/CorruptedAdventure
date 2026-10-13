package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            Settings.CODEC.optionalFieldOf("settings", Settings.DEFAULT).forGetter(f -> f.settings)
    ).apply(instance, IslandField::new));

    public static final KeyDispatchDataCodec<IslandField> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** The order of overlapping islands ({@link #uppermost}): bottom, top, then the centre as a fixed key. */
    private static final Comparator<Island> OVERLAP = Comparator.comparingInt(Island::bottom).thenComparingInt(Island::top)
            .thenComparingDouble(Island::x).thenComparingDouble(Island::z);
    private static final long WORLD_SALT = 4004;
    /** The preliminary surface: the lowest surface on rings of these radii around a quart's centre, less a margin. */
    private static final double[] INITIAL_RADII = {6.0, 12.0};
    private static final double INITIAL_MARGIN = 2.0;
    /** Islands one column may hold (overlapping footprints at different heights; more are dropped). */
    static final int MAX_ISLANDS = 8;
    /** Recorded waterfalls per chunk at most (the cap of the water branch). */
    static final int FALLS_PER_CHUNK = 3;
    /** What the stack adds to vanilla's caves around a lake above the sea (closes them). */
    private static final double CAVE_LIFT = 2.0;
    /** How far (blocks) under a lake's deepest floor the caves stay closed. */
    private static final int CAVE_FLOOR_MARGIN = 14;

    private final NoiseHolder hash;
    private final Ring4Relief relief;
    private final Settings settings;
    private final Supplier<RingLayout> layout;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final Decisions decisions;
    private final ThreadOwned<Ring4Relief.Sample> reliefScratch = new ThreadOwned<>(Ring4Relief.Sample::new);
    // The islands of the 3 x 3 cells around the last cell asked for, per thread.
    private final ThreadOwned<Neighbourhood> neighbourhood = new ThreadOwned<>(Neighbourhood::new);

    public IslandField(NoiseHolder hash, Ring4Relief relief, Settings settings) {
        this(hash, relief, settings, RingLayout::current, new Decisions());
    }

    /** @param layout the ring layout (the running server's; tests give their own) */
    IslandField(NoiseHolder hash, Ring4Relief relief, Settings settings, Supplier<RingLayout> layout) {
        this(hash, relief, settings, layout, new Decisions());
    }

    private IslandField(NoiseHolder hash, Ring4Relief relief, Settings settings, Supplier<RingLayout> layout, Decisions decisions) {
        this.hash = hash;
        this.relief = relief;
        this.settings = settings;
        this.layout = layout;
        this.decisions = decisions;
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
        // A noise chunk's copy keeps the cell decisions of the random state's instance (same noises, same results).
        boolean same = mapped.equals(hash) && mappedRelief.equals(relief);
        return visitor.apply(new IslandField(mapped, mappedRelief, settings, layout, same ? decisions : new Decisions()));
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
        return o instanceof IslandField f && hash.equals(f.hash) && relief.equals(f.relief) && settings.equals(f.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, relief, settings);
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
     * The island values of a column only (review 60 #5): its islands, spring, lake and fall, without the base terrain's
     * noise; the surface values ({@link Column#height}, {@link Column#surface}, …) are not filled by this call.
     */
    public Column islandColumn(int x, int z) {
        Column column = columns.get(x, z, IslandField::reset);
        ensureIslands(column);
        return column;
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

    /** The positions of the lifts (updrafts) in the box: beside about one in four middle and giant islands. */
    public List<Lift> liftsWithin(int minX, int minZ, int maxX, int maxZ) {
        List<Lift> found = new ArrayList<>();
        for (IslandShape shape : shapesWithin(minX, minZ, maxX, maxZ)) {
            Lift lift = details(shape).lift();
            if (lift != null && lift.x() >= minX && lift.x() <= maxX && lift.z() >= minZ && lift.z() <= maxZ) {
                found.add(lift);
            }
        }
        return found;
    }

    /**
     * The waterfalls in the box (off islands; the cliff 3→4 and the world trees follow in 62 and 74), as the terrain
     * places them: at most {@link #FALLS_PER_CHUNK} per chunk.
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

    /** The world trees in the box (74). */
    public List<WorldTree> worldTrees(int minX, int minZ, int maxX, int maxZ) {
        return List.of();
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
        long key = (long) qx << 32 | qz & 0xFFFFFFFFL;
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
        return lowest - INITIAL_MARGIN;
    }

    /**
     * The deepest floor of a peat sinkhole whose bowl comes within 12 blocks of (x, z) (the rings of
     * {@link #lowestAround}), or +infinity. Only sinkholes the peat really cuts count (review 60 #2): elsewhere the
     * cell's centre may lie in a trough or a fjord, which must not pull the surface down under dry land.
     */
    private double sinkholeFloorNear(int x, int z) {
        double ring = INITIAL_RADII[INITIAL_RADII.length - 1];
        double reach = ring + Ring4Relief.SINKHOLE_MAX_RADIUS;
        int cell = Ring4Relief.SINKHOLE_CELL;
        double floor = Double.POSITIVE_INFINITY;
        for (int i = Math.floorDiv((int) (x - reach), cell); i <= Math.floorDiv((int) (x + reach), cell); i++) {
            for (int j = Math.floorDiv((int) (z - reach), cell); j <= Math.floorDiv((int) (z + reach), cell); j++) {
                double[] hole = Ring4Relief.sinkhole(i, j, salt());
                if (hole == null || Math.hypot(hole[0] - x, hole[1] - z) - hole[2] > ring) {
                    continue;
                }
                // The sinkhole is deepest at its centre.
                Ring4Relief.Sample s = reliefScratch.get();
                relief.sample(hole[0], hole[1], band(hole[0], hole[1]), s, salt());
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

    /** The base terrain's surface at a position (no holes), exact. */
    double baseHeight(double x, double z) {
        Ring4Relief.Sample s = reliefScratch.get();
        relief.sample(x, z, band(x, z), s, salt());
        return s.height;
    }

    /** The surface at a position with the islands' holes and outlets, exact and not cached. */
    double surfaceUncached(double x, double z) {
        return withHoles(baseHeight(x, z), x, z, shapesNear(Mth.floor(x), Mth.floor(z)), null);
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
     * Per block: +1 inside an island, −1 in a carved opening (a spring, a peat sinkhole's bowl), 0 elsewhere (the
     * terrain decides). The base terrain is only computed for columns inside a sinkhole's disc.
     */
    double thin(int x, int y, int z) {
        Column c = columns.get(x, z, IslandField::reset);
        ensureIslands(c);
        double thin = c.thin(y);
        if (thin == 0.0 && c.sinkholeDisc) {
            ensureSurface(c);
            if (!Double.isNaN(c.sinkholeFloor) && y >= Mth.ceil(c.sinkholeFloor) && y <= c.height + 2.0) {
                return -1.0;
            }
        }
        return thin;
    }

    /** Per block: what the stack adds to the density of vanilla's caves: closed under and beside a lake above the sea. */
    double caveLift(int x, int y, int z) {
        Column c = columns.get(x, z, IslandField::reset);
        ensureIslands(c);
        return y >= c.caveLiftFrom && y <= c.caveLiftTo ? CAVE_LIFT : 0.0;
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

    /** The islands of a grid cell, decided once per world ({@link IslandPlacement#place}). */
    List<IslandShape> cell(int i, int j) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        List<IslandShape> known = decisions.cells.get(key);
        if (known == null) {
            known = IslandPlacement.place(this, i, j);
            decisions.remember(decisions.cells, key, known);
        }
        return known;
    }

    /** The islands of the 3 x 3 cells around the cell of (x, z): all that can reach the column (review 59 #6). */
    List<IslandShape> shapesNear(int x, int z) {
        int cell = settings.cellSize();
        return neighbourhood.get().around(this, Math.floorDiv(x, cell), Math.floorDiv(z, cell));
    }

    /** The islands of the cells that can reach into the box. */
    private List<IslandShape> shapesWithin(int minX, int minZ, int maxX, int maxZ) {
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
        Details known = decisions.details.get(shape);
        if (known == null) {
            known = IslandPlacement.details(this, shape);
            decisions.remember(decisions.details, shape, known);
        }
        return known;
    }

    /**
     * The falls whose falling column lies in chunk (cx, cz), at most {@link #FALLS_PER_CHUNK}: the first ones by x,
     * then z, so the cap does not depend on the order chunks are made in.
     */
    List<Fall> fallsOfChunk(int cx, int cz) {
        long key = (long) cx << 32 | cz & 0xFFFFFFFFL;
        List<Fall> known = decisions.chunkFalls.get(key);
        if (known == null) {
            int minX = cx << 4;
            int minZ = cz << 4;
            List<Fall> falls = new ArrayList<>();
            for (IslandShape shape : shapesWithin(minX, minZ, minX + 15, minZ + 15)) {
                if (Math.abs(shape.x() - (minX + 8)) > shape.reach() + 10 || Math.abs(shape.z() - (minZ + 8)) > shape.reach() + 10) {
                    continue;
                }
                for (Fall fall : details(shape).falls()) {
                    if (fall.fallX() >> 4 == cx && fall.fallZ() >> 4 == cz) {
                        falls.add(fall);
                    }
                }
            }
            falls.sort(Comparator.comparingInt(Fall::fallX).thenComparingInt(Fall::fallZ).thenComparingInt(Fall::springY));
            known = List.copyOf(falls.subList(0, Math.min(FALLS_PER_CHUNK, falls.size())));
            decisions.remember(decisions.chunkFalls, key, known);
        }
        return known;
    }

    private static void reset(Column column, int x, int z) {
        column.reset(x, z);
    }

    /** Computes the base terrain and the surface (with the holes) of a column once. */
    private void ensureSurface(Column c) {
        if (c.surfaceFilled) {
            return;
        }
        Ring4Relief.Sample s = reliefScratch.get();
        relief.sample(c.x, c.z, band(c.x, c.z), s, salt());
        // Read the scratch sample before the islands are looked up: placing a cell samples the relief again.
        c.height = s.height;
        c.river = s.river;
        c.fjord = s.fjord;
        c.sinkholeFloor = s.sinkholeFloor;
        double[] hole = {0.0};
        c.surface = withHoles(c.height, c.x, c.z, shapesNear(c.x, c.z), hole);
        c.islandHole = hole[0];
        c.surfaceFilled = true;
    }

    /**
     * Computes the islands of a column once: their solid blocks, a spring, the lake above the sea (and the band kept
     * free of caves), the recorded fall through the column.
     */
    private void ensureIslands(Column c) {
        if (c.islandsFilled) {
            return;
        }
        c.islandsFilled = true;
        double[] sinkhole = Ring4Relief.sinkhole(Math.floorDiv(c.x, Ring4Relief.SINKHOLE_CELL),
                Math.floorDiv(c.z, Ring4Relief.SINKHOLE_CELL), salt());
        c.sinkholeDisc = sinkhole != null && Math.hypot(c.x - sinkhole[0], c.z - sinkhole[1]) < sinkhole[2];
        double px = c.x + 0.5;
        double pz = c.z + 0.5;
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
            if (details.lakeTop() != Integer.MIN_VALUE) {
                if (dh < 1.0 + IslandShape.LAKE_BAND / radius) {
                    c.lakeTop = Math.max(c.lakeTop, details.lakeTop());
                }
                c.caveLiftFrom = Math.min(c.caveLiftFrom, details.lakeBottom() - CAVE_FLOOR_MARGIN);
                c.caveLiftTo = Math.max(c.caveLiftTo, details.lakeTop() + 2);
            }
            for (Fall fall : details.falls()) {
                if (fall.springX() == c.x && fall.springZ() == c.z && fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                    c.springY = fall.springY();
                }
            }
        }
        for (Fall fall : fallsOfChunk(c.x >> 4, c.z >> 4)) {
            if (fall.fallX() == c.x && fall.fallZ() == c.z && fall.springY() > c.fallTop) {
                c.fallTop = fall.springY();
                c.fallBottom = fall.bottom();
                c.fallFlowBelow = fall.bottom() + 1;
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

    /** A world tree standing on the ground at {@code baseY}, its crown reaching {@code top}. */
    public record WorldTree(int x, int z, int baseY, int top) {
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
        /** 0..1: an island's mirror lake (1 well inside the hole). */
        double islandHole;
        /** Whether the column lies in the disc of a peat sinkhole's cell (then the base terrain decides its floor). */
        boolean sinkholeDisc;
        /** The floor of a peat sinkhole, cut per block ({@link #thin}); NaN outside one. */
        double sinkholeFloor;
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
        int x;
        int z;

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
            islandCount = 0;
            springY = Integer.MIN_VALUE;
            lakeTop = Integer.MIN_VALUE;
            caveLiftFrom = Integer.MAX_VALUE;
            caveLiftTo = Integer.MIN_VALUE;
            fallTop = Integer.MIN_VALUE;
            fallBottom = Integer.MIN_VALUE;
            fallFlowBelow = Integer.MIN_VALUE;
        }

        void addIsland(int lo, int hi) {
            if (islandCount < MAX_ISLANDS) {
                islandLo[islandCount] = lo;
                islandHi[islandCount] = hi;
                islandCount++;
            }
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
    }

    /** The islands of the 3 x 3 cells around one cell, per thread; only a cache. A new list per move, never changed. */
    static final class Neighbourhood {
        private boolean filled;
        private int ci;
        private int cj;
        private List<IslandShape> shapes = List.of();

        List<IslandShape> around(IslandField field, int i, int j) {
            if (!filled || i != ci || j != cj) {
                List<IslandShape> all = new ArrayList<>();
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        all.addAll(field.cell(i + di, j + dj));
                    }
                }
                shapes = List.copyOf(all);
                ci = i;
                cj = j;
                filled = true;
            }
            return shapes;
        }
    }

    /**
     * Per-world decisions and remembered results: pure functions of the seed and the cell, shared by all copies of one
     * wired field (the noise chunk makes a copy per chunk), so they are made once per world. Only a cache; a map is
     * cleared when it grows past {@link #LIMIT} entries.
     */
    static final class Decisions {
        static final int LIMIT = 100_000;
        final Map<Long, List<IslandShape>> cells = new ConcurrentHashMap<>();
        final Map<IslandShape, Details> details = new ConcurrentHashMap<>();
        final Map<Long, List<Fall>> chunkFalls = new ConcurrentHashMap<>();
        final Map<Long, Double> initialHeights = new ConcurrentHashMap<>();
        volatile Long salt;

        <K, V> void remember(Map<K, V> map, K key, V value) {
            if (map.size() > LIMIT) {
                map.clear();
            }
            map.putIfAbsent(key, value);
        }
    }
}
