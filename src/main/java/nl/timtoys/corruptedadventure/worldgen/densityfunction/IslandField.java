package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
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

    private final NoiseHolder hash;
    private final Ring4Relief relief;
    private final Settings settings;
    private final Supplier<RingLayout> layout;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final Decisions decisions;
    private final ThreadOwned<Ring4Relief.Sample> reliefScratch = new ThreadOwned<>(Ring4Relief.Sample::new);

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
        int cell = settings.cellSize();
        // An island lies within one cell of the cell holding its centre.
        int minI = Math.floorDiv(minX, cell) - 1;
        int maxI = Math.floorDiv(maxX, cell) + 1;
        int minJ = Math.floorDiv(minZ, cell) - 1;
        int maxJ = Math.floorDiv(maxZ, cell) + 1;
        List<Island> found = new ArrayList<>();
        for (int i = minI; i <= maxI; i++) {
            for (int j = minJ; j <= maxJ; j++) {
                for (Island island : cell(i, j)) {
                    if (island.reaches(minX, minZ, maxX, maxZ)) {
                        found.add(island);
                    }
                }
            }
        }
        return found;
    }

    /** The positions of the lifts (updrafts) in the box (61). */
    public List<Lift> liftsWithin(int minX, int minZ, int maxX, int maxZ) {
        return List.of();
    }

    /** The waterfalls in the box: off islands, the cliff 3→4 and the world trees (61, 62, 74). */
    public List<Waterfall> waterfallsWithin(int minX, int minZ, int maxX, int maxZ) {
        return List.of();
    }

    /** The world trees in the box (74). */
    public List<WorldTree> worldTrees(int minX, int minZ, int maxX, int maxZ) {
        return List.of();
    }

    // ---------------------------------------------------------------- outputs ({@link IslandOutput})

    /**
     * A lower bound of the surface around a column (the lowest surface on rings of 6 and 12 blocks around the quart's
     * centre, 8 points each, less 2; the sinkholes near it counted at their deepest), for the preliminary surface:
     * vanilla's aquifer decides from it whether open space below the sea level holds the sea, so it must not lie above
     * a lake, a river or a fjord (pitfall Y 55). Remembered per world on the 4-block grid.
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
        return lowest - INITIAL_MARGIN;
    }

    /** The deepest floor of a peat sinkhole that may reach within 12 blocks of (x, z), or +infinity. */
    private double sinkholeFloorNear(int x, int z) {
        double reach = INITIAL_RADII[INITIAL_RADII.length - 1] + Ring4Relief.SINKHOLE_MAX_RADIUS;
        int cell = Ring4Relief.SINKHOLE_CELL;
        double floor = Double.POSITIVE_INFINITY;
        for (int i = Math.floorDiv((int) (x - reach), cell); i <= Math.floorDiv((int) (x + reach), cell); i++) {
            for (int j = Math.floorDiv((int) (z - reach), cell); j <= Math.floorDiv((int) (z + reach), cell); j++) {
                double[] hole = Ring4Relief.sinkhole(i, j, salt());
                if (hole != null && Math.abs(hole[0] - x) < reach && Math.abs(hole[1] - z) < reach) {
                    // The deepest the sinkhole can cut: from the highest peat with its full depth.
                    floor = Math.min(floor, surfaceUncached(hole[0], hole[1]));
                }
            }
        }
        return floor;
    }

    private double surfaceUncached(double x, double z) {
        Ring4Relief.Sample s = reliefScratch.get();
        relief.sample(x, z, band(x, z), s, salt());
        return s.height;
    }

    /** Per block (61): +1 inside an island, −1 in a carved opening (a spring), 0 elsewhere (the terrain decides). */
    double thin(int x, int y, int z) {
        return 0.0;
    }

    /** Per block (61): what the stack adds to the density of vanilla's caves (0 where they run freely). */
    double caveLift(int x, int y, int z) {
        return 0.0;
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

    private double band(double x, double z) {
        return Ring4Band.of(layout.get()).value(x, z);
    }

    /** The islands of a grid cell, decided once per world. */
    List<Island> cell(int i, int j) {
        long key = (long) i << 32 | j & 0xFFFFFFFFL;
        List<Island> known = decisions.cells.get(key);
        if (known == null) {
            known = placeCell(i, j);
            decisions.remember(decisions.cells, key, known);
        }
        return known;
    }

    /** The placement of a cell's islands (package 61). */
    private List<Island> placeCell(int i, int j) {
        return List.of();
    }

    private static void reset(Column column, int x, int z) {
        column.reset(x, z);
    }

    /** Computes the base terrain and the surface of a column once. */
    private void ensureSurface(Column c) {
        if (c.surfaceFilled) {
            return;
        }
        Ring4Relief.Sample s = reliefScratch.get();
        relief.sample(c.x, c.z, band(c.x, c.z), s, salt());
        c.height = s.height;
        c.surface = s.height;
        c.river = s.river;
        c.fjord = s.fjord;
        c.surfaceFilled = true;
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

    /** A lift (updraft) from {@code bottom} up to {@code top} in one column. */
    public record Lift(int x, int z, int bottom, int top) {
    }

    /** A recorded waterfall: falling water in one column from {@code top} down to {@code bottom}. */
    public record Waterfall(int x, int z, int top, int bottom) {
    }

    /** A world tree standing on the ground at {@code baseY}, its crown reaching {@code top}. */
    public record WorldTree(int x, int z, int baseY, int top) {
    }

    /** All 2D values of one column. Heights in blocks. */
    public static final class Column {
        boolean surfaceFilled;
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
        /** 0..1: an island's mirror lake (1 on its floor). */
        double islandHole;
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
            height = 0.0;
            surface = 0.0;
            river = 0.0;
            fjord = 0.0;
            islandMask = 0.0;
            islandHole = 0.0;
            fallTop = Integer.MIN_VALUE;
            fallBottom = Integer.MIN_VALUE;
            fallFlowBelow = Integer.MIN_VALUE;
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

    /**
     * Per-world decisions and remembered results: pure functions of the seed and the cell, shared by all copies of one
     * wired field (the noise chunk makes a copy per chunk), so they are made once per world. Only a cache; a map is
     * cleared when it grows past {@link #LIMIT} entries.
     */
    static final class Decisions {
        static final int LIMIT = 100_000;
        final Map<Long, List<Island>> cells = new ConcurrentHashMap<>();
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
