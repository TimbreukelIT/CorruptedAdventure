package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code corruptedadventure:sea_fossils} (78, Q15 = B): the sea-creature fossils of Nether ring 4's {@code bone_dunes} as
 * terrain. Wraps the terrain of the dunes ({@code terrain}, meant to be the interpolated shape) outside the interpolation,
 * so the thin bones are exact: +1 in a bone ({@link SeaFossilShape}), the flat floor inside the fossil (solid below,
 * open above, ramping to the natural dunes round it), and {@code terrain} everywhere else.
 * <p>
 * Cost: a column outside every fossil's rectangle pays one cell lookup (a small per-thread memory) and a rectangle test
 * per block before it hands over to {@code terrain}; only the columns of a fossil (a few per cent of the dunes) build
 * their bone mask and floor once ({@link ColumnTable}) and then test one bit per block.
 * <p>
 * Pure: the cell hash comes from {@code noise} (seeded), the placement from {@code region}, {@code edge} and
 * {@code floor} at fixed points. A cell is decided once per world: the copies the noise chunk makes share one map of
 * decided cells ({@link #mapAll}), as {@link GiantGeodes} does.
 */
public final class SeaFossils implements DensityFunction {
    private static final double BONE = 1.0;
    private static final double OPEN = -1.0;
    /** The flat floor is filled solid down to this far below it (deeper the dunes are solid anyway). */
    private static final int FILL_DEPTH = 12;
    /** The dunes are cut open up to this far above their natural floor (the interpolated floor lies a little off it). */
    private static final int CUT_ABOVE = 3;
    private static final int CACHE_SIZE = 8;
    /**
     * Every block a fossil changes lies in Y [{@code LOWEST}, {@code HIGHEST}] (a fossil whose floor would take it
     * outside is not placed: {@link SeaFossilShape#fossilOfCell}); the rest of the column skips the lookups.
     */
    static final int LOWEST = 24;
    static final int HIGHEST = 110;
    /** {@code ring_4/nether/shape} in the dunes: (floor − y) / 8 − 0.05 > 0 below floor − 0.4. */
    static final double SURFACE_OFFSET = 0.4;
    private static final int SHARED_LIMIT = 1 << 14;

    public static final MapCodec<SeaFossils> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("terrain").forGetter(d -> d.terrain),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("floor").forGetter(d -> d.floor),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("region").forGetter(d -> d.region),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("edge").forGetter(d -> d.edge),
            NoiseHolder.CODEC.fieldOf("noise").forGetter(d -> d.noise),
            SeaFossilShape.Settings.CODEC.fieldOf("settings").forGetter(d -> d.settings)
    ).apply(instance, SeaFossils::new));

    public static final KeyDispatchDataCodec<SeaFossils> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final DensityFunction terrain;
    private final DensityFunction floor;
    private final DensityFunction region;
    private final DensityFunction edge;
    private final NoiseHolder noise;
    private final SeaFossilShape.Settings settings;
    private final Map<Long, Optional<SeaFossilShape.Fossil>> decided;
    private final ThreadOwned<Cells> cells = new ThreadOwned<>(Cells::new);
    private final ColumnTable<Column> columns = new ColumnTable<>(Column::new);
    private final ColumnTable.Filler<Column> filler = (column, x, z) -> column.fill(this, x, z);

    public SeaFossils(DensityFunction terrain, DensityFunction floor, DensityFunction region, DensityFunction edge, NoiseHolder noise,
                      SeaFossilShape.Settings settings) {
        this(terrain, floor, region, edge, noise, settings, new ConcurrentHashMap<>());
    }

    private SeaFossils(DensityFunction terrain, DensityFunction floor, DensityFunction region, DensityFunction edge, NoiseHolder noise,
                       SeaFossilShape.Settings settings, Map<Long, Optional<SeaFossilShape.Fossil>> decided) {
        this.terrain = terrain;
        this.floor = floor;
        this.region = region;
        this.edge = edge;
        this.noise = noise;
        this.settings = settings;
        this.decided = decided;
    }

    @Override
    public double compute(FunctionContext context) {
        int y = context.blockY();
        if (y < LOWEST || y > HIGHEST) {
            return terrain.compute(context);
        }
        int x = context.blockX();
        int z = context.blockZ();
        int size = settings.cellSize();
        SeaFossilShape.Fossil fossil = cells.get().get(this, Math.floorDiv(x, size), Math.floorDiv(z, size));
        if (fossil == null || !fossil.touches(x, z)) {
            return terrain.compute(context);
        }
        Column column = columns.get(x, z, filler);
        double value = column.value(y);
        return Double.isNaN(value) ? terrain.compute(context) : value;
    }

    /** The fossil whose cell holds the column, or null (tests, the surface condition). */
    @Nullable
    public SeaFossilShape.Fossil fossilAt(int x, int z) {
        int size = settings.cellSize();
        return fossilOfCell(Math.floorDiv(x, size), Math.floorDiv(z, size));
    }

    /** The fossil of a cell from the map shared by the copies of this wired function, deciding it the first time. */
    @Nullable
    public SeaFossilShape.Fossil fossilOfCell(int cellX, int cellZ) {
        long key = (long) cellX << 32 | cellZ & 0xFFFFFFFFL;
        Optional<SeaFossilShape.Fossil> known = decided.get(key);
        if (known == null) {
            if (decided.size() > SHARED_LIMIT) {
                decided.clear();
            }
            // Pure, so two threads deciding the same cell at once put the same answer.
            SeaFossilShape.Fossil fossil = SeaFossilShape.fossilOfCell(settings, (cx, cz, salt) -> SeededHash.hash(noise, cx, cz, salt),
                    cellX, cellZ, this::region, this::edge, this::naturalFloor);
            known = Optional.ofNullable(fossil != null && withinGuard(fossil) ? fossil : null);
            decided.put(key, known);
        }
        return known.orElse(null);
    }

    /** Whether everything the fossil changes (bone mask, floor fill) lies within Y [{@link #LOWEST}, {@link #HIGHEST}]. */
    static boolean withinGuard(SeaFossilShape.Fossil fossil) {
        return fossil.floor() - Math.max(FILL_DEPTH, SeaFossilShape.BELOW) >= LOWEST && fossil.maskBase() + 63 <= HIGHEST;
    }

    public SeaFossilShape.Settings settings() {
        return settings;
    }

    private double region(int x, int z) {
        return region.compute(new SinglePointContext(x, 0, z));
    }

    private double edge(int x, int z) {
        return edge.compute(new SinglePointContext(x, 0, z));
    }

    /**
     * The first open Y of the dunes as they generate (continuous): {@code floor} is the height in {@code below(floor)} =
     * (floor − y) / 8, and the dunes' roughness is −0.05, so the density crosses 0 at {@code floor − 0.4}.
     */
    private double naturalFloor(int x, int z) {
        return floor.compute(new SinglePointContext(x, 0, z)) - SURFACE_OFFSET;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    /**
     * {@code terrain} is mapped with the visitor itself (the noise chunk must reach its interpolation marker); of
     * {@code floor}, {@code region} and {@code edge} only the noises are wired, so a cell gives the same answer in every
     * chunk that evaluates it (see {@link GiantGeodes#mapAll}). Copies of the wired function share the decided cells.
     */
    @Override
    public DensityFunction mapAll(Visitor visitor) {
        Visitor noisesOnly = new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder holder) {
                return visitor.visitNoise(holder);
            }
        };
        NoiseHolder wired = visitor.visitNoise(noise);
        boolean sameNoise = noise.noise() != null && wired.noise() == noise.noise();
        return visitor.apply(new SeaFossils(terrain.mapAll(visitor), floor.mapAll(noisesOnly), region.mapAll(noisesOnly),
                edge.mapAll(noisesOnly), wired, settings, sameNoise ? decided : new ConcurrentHashMap<>()));
    }

    /** The map of decided cells (tests: which copies share it). */
    Map<Long, Optional<SeaFossilShape.Fossil>> decided() {
        return decided;
    }

    /** The first {@code sea_fossils} anywhere inside {@code root} (e.g. a random state's final density), or null. */
    @Nullable
    public static SeaFossils find(DensityFunction root) {
        SeaFossils[] found = new SeaFossils[1];
        root.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (found[0] == null && function instanceof SeaFossils fossils) {
                    found[0] = fossils;
                }
                return function;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder holder) {
                return holder;
            }
        });
        return found[0];
    }

    @Override
    public double minValue() {
        return Math.min(OPEN, terrain.minValue());
    }

    @Override
    public double maxValue() {
        return Math.max(BONE, terrain.maxValue());
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    /** What a fossil column holds: its bone mask, and the floor it ramps to. */
    private static final class Column {
        private long bone;
        private int base;
        /** First open Y of the floor (blocks below are solid), or Integer.MIN_VALUE where the floor is not touched. */
        private int target;
        private int cutTop;

        void fill(SeaFossils owner, int x, int z) {
            SeaFossilShape.Fossil fossil = owner.cells.get().get(owner, Math.floorDiv(x, owner.settings.cellSize()),
                    Math.floorDiv(z, owner.settings.cellSize()));
            bone = fossil.boneMask(x, z);
            base = fossil.maskBase();
            double weight = fossil.floorWeight(x, z);
            if (weight <= 0.0) {
                target = Integer.MIN_VALUE;
                return;
            }
            double natural = owner.naturalFloor(x, z);
            target = (int) Math.ceil(natural + (fossil.floor() - natural) * weight - 1e-9);
            cutTop = Math.max(target, (int) Math.ceil(natural)) + CUT_ABOVE;
        }

        /** +1 in a bone or under the floor, −1 above the floor up to the cut, NaN for "the terrain decides". */
        double value(int y) {
            int i = y - base;
            if (i >= 0 && i < 64 && (bone >>> i & 1L) != 0L) {
                return BONE;
            }
            if (target != Integer.MIN_VALUE) {
                if (y < target && y >= target - FILL_DEPTH) {
                    return BONE;
                }
                if (y >= target && y < cutTop) {
                    return OPEN;
                }
            }
            return Double.NaN;
        }
    }

    /** A small direct-mapped memory of the cells a thread asked for; a chunk lies in one cell. */
    private static final class Cells {
        private final long[] keys = new long[CACHE_SIZE];
        private final SeaFossilShape.Fossil[] fossils = new SeaFossilShape.Fossil[CACHE_SIZE];
        private final boolean[] filled = new boolean[CACHE_SIZE];

        @Nullable
        SeaFossilShape.Fossil get(SeaFossils owner, int cellX, int cellZ) {
            long key = (long) cellX << 32 | cellZ & 0xFFFFFFFFL;
            int index = (int) ((cellX * 31L + cellZ * 17L) & (CACHE_SIZE - 1));
            if (!filled[index] || keys[index] != key) {
                keys[index] = key;
                fossils[index] = owner.fossilOfCell(cellX, cellZ);
                filled[index] = true;
            }
            return fossils[index];
        }
    }
}
