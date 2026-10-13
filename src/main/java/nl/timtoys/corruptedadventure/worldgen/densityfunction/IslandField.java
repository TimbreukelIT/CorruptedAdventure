package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code corruptedadventure:island_field}: the floating islands of overworld ring 4 (with their mirror lakes, lifts,
 * waterfalls and the world trees), on a grid of its own: ring 3's {@link VolcanicField} caps a column at 6 islands and
 * 8 solids, too few for ring 4's dense islands. As a density function it is the island mask of a column (0..1).
 * <p>
 * Skeleton (package 59): the grid, the per-column table, the per-world cell decisions and the read access exist; the
 * placement ({@link #placeCell}, package 61) places nothing yet. The biome source's height hook
 * ({@code RingBiomeSource}) and the aquifer's water branch ({@code RingAquifer}) read it through {@link #find} on the
 * router, like {@link VolcanicField}.
 * <p>
 * Pure: a function of the seed (through {@code hash}), the ring layout and x, z. Decisions read their inputs at exact
 * positions, never through a {@code flat_cache}; the caches only remember results.
 */
public final class IslandField implements DensityFunction {
    public static final MapCodec<IslandField> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("hash").forGetter(f -> f.hash),
            Settings.CODEC.optionalFieldOf("settings", Settings.DEFAULT).forGetter(f -> f.settings)
    ).apply(instance, IslandField::new));

    public static final KeyDispatchDataCodec<IslandField> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** The order of overlapping islands ({@link #uppermost}): bottom, top, then the centre as a fixed key. */
    private static final Comparator<Island> OVERLAP = Comparator.comparingInt(Island::bottom).thenComparingInt(Island::top)
            .thenComparingDouble(Island::x).thenComparingDouble(Island::z);

    private final NoiseHolder hash;
    private final Settings settings;
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final Decisions decisions;

    public IslandField(NoiseHolder hash, Settings settings) {
        this(hash, settings, new Decisions());
    }

    private IslandField(NoiseHolder hash, Settings settings, Decisions decisions) {
        this.hash = hash;
        this.settings = settings;
        this.decisions = decisions;
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
        // A noise chunk's copy keeps the cell decisions of the random state's instance (same noise, same results).
        return visitor.apply(new IslandField(mapped, settings, mapped.equals(hash) ? decisions : new Decisions()));
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
        return o instanceof IslandField f && hash.equals(f.hash) && settings.equals(f.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, settings);
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

    // ---------------------------------------------------------------- read access (W-biomes, B, A; read only)

    /** All 2D values of a column (cached per thread around the current chunk). */
    public Column column(int x, int z) {
        return columns.get(x, z, this::fill);
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

    // ---------------------------------------------------------------- cells

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

    /** The placement of a cell's islands (package 61); the skeleton places none. */
    private List<Island> placeCell(int i, int j) {
        return List.of();
    }

    private void fill(Column column, int x, int z) {
        column.reset(x, z);
    }

    // ---------------------------------------------------------------- data

    /**
     * The tunable numbers of the field. Every field is optional in JSON; missing ones take {@link #DEFAULT}.
     *
     * @param cellSize cell of the island grid (61 decides how many islands a cell holds)
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
        /** 0..1: an island in this column. */
        double islandMask;
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
            islandMask = 0.0;
            fallTop = Integer.MIN_VALUE;
            fallBottom = Integer.MIN_VALUE;
            fallFlowBelow = Integer.MIN_VALUE;
        }

        public double islandMask() {
            return islandMask;
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
     * Per-cell decisions: pure functions of the seed and the cell, shared by all copies of one wired field (the noise
     * chunk makes a copy per chunk), so they are made once per world. Only a cache; a map is cleared when it grows past
     * {@link #LIMIT} entries.
     */
    static final class Decisions {
        static final int LIMIT = 100_000;
        final Map<Long, List<Island>> cells = new ConcurrentHashMap<>();

        <K, V> void remember(Map<K, V> map, K key, V value) {
            if (map.size() > LIMIT) {
                map.clear();
            }
            map.putIfAbsent(key, value);
        }
    }
}
