package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantGeodeShape;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code corruptedadventure:giant_geodes} (65): the hollows of the giant geodes of Nether ring 4's {@code geode_hollows},
 * −1 inside a hollow (the cavity above its flat floor, and its doorways) and +10 (neutral under {@code min}) everywhere
 * else. Meant as {@code min(interpolated(shape), giant_geodes)} outside the interpolation, so the hollows are exact. The
 * hollow exists from the noise step on, before every feature, so the lining (78) sees the same terrain in every load order.
 * <p>
 * The geometry is {@link GiantGeodeShape}. A candidate only becomes a geode if:
 * <ul>
 *     <li>its centre lies deep in the hollows (region at most {@code region_max}) and every open block of it, doorways
 *     included, lies where the hollows have weight (region at most {@code region_reach_max} at the wall in 8 directions
 *     and along every doorway): no geode is cut in half by the mass's edge;</li>
 *     <li>it is clear of the ring's borders ({@code edge} = 0 at the same points);</li>
 *     <li>its centre and its dome ({@link GiantGeodeShape.Geode#domeTestPoints}: {@code shell} blocks of rock above the
 *     cavity) are solid, and</li>
 *     <li>it finds one to {@code doors_max} doorways that reach open terrain ({@link GiantGeodeShape#doors}).</li>
 * </ul>
 * Solid and open are tested on the terrain as it generates: {@code shape} (the uninterpolated terrain) interpolated
 * over the noise cells of the Nether ({@link #CELL_WIDTH} × {@link #CELL_HEIGHT}), like the noise chunk does, so an
 * accepted doorway always meets the opening and a dome never breaks open between two corners.
 * <p>
 * Pure: from the seeded cell hash, the region, edge and shape functions and the position. The geode of a cell is looked
 * up once per world: the copies the noise chunk makes of the wired function share one map of the cells already decided
 * ({@link #mapAll}); a per-thread memory sits in front of it. One cell lookup per block (the geode with its doorways lies
 * within its cell).
 */
public final class GiantGeodes implements DensityFunction {
    /** The noise cell of {@code noise_settings/nether.json}: size_horizontal 1 and size_vertical 2 (× 4 blocks). */
    static final int CELL_WIDTH = 4;
    static final int CELL_HEIGHT = 8;
    private static final double OUTSIDE = 10.0;
    private static final double INSIDE = -1.0;
    private static final int CACHE_SIZE = 8;
    /** The shared map only holds the cells of one ring; cleared if something asks far more (it never should). */
    private static final int SHARED_LIMIT = 1 << 14;

    public static final MapCodec<GiantGeodes> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shape").forGetter(d -> d.shape),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("edge").forGetter(d -> d.edge),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("region").forGetter(d -> d.region),
            NoiseHolder.CODEC.fieldOf("noise").forGetter(d -> d.noise),
            GiantGeodeShape.Settings.CODEC.fieldOf("settings").forGetter(d -> d.settings)
    ).apply(instance, GiantGeodes::new));

    public static final KeyDispatchDataCodec<GiantGeodes> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final DensityFunction shape;
    private final DensityFunction edge;
    private final DensityFunction region;
    private final NoiseHolder noise;
    private final GiantGeodeShape.Settings settings;
    private final Map<Long, Optional<GiantGeodeShape.Geode>> decided;
    private final ThreadOwned<Cells> cells = new ThreadOwned<>(Cells::new);

    public GiantGeodes(DensityFunction shape, DensityFunction edge, DensityFunction region, NoiseHolder noise, GiantGeodeShape.Settings settings) {
        this(shape, edge, region, noise, settings, new ConcurrentHashMap<>());
    }

    private GiantGeodes(DensityFunction shape, DensityFunction edge, DensityFunction region, NoiseHolder noise,
                        GiantGeodeShape.Settings settings, Map<Long, Optional<GiantGeodeShape.Geode>> decided) {
        this.shape = shape;
        this.edge = edge;
        this.region = region;
        this.noise = noise;
        this.settings = settings;
        this.decided = decided;
    }

    @Override
    public double compute(FunctionContext context) {
        int y = context.blockY();
        if (y < settings.floorMin() || y > settings.highest()) {
            return OUTSIDE;
        }
        int size = settings.cellSize();
        GiantGeodeShape.Geode geode = cells.get().get(this, Math.floorDiv(context.blockX(), size), Math.floorDiv(context.blockZ(), size));
        return geode != null && geode.hollow(context.blockX(), y, context.blockZ()) ? INSIDE : OUTSIDE;
    }

    /**
     * The geode of the cell that holds column (x, z), or null: for the lining (78), from the same map of decided cells
     * as the terrain, so the lining lines exactly the hollow the terrain dug. Pure and safe from any thread.
     */
    @Nullable
    public GiantGeodeShape.Geode geodeAt(int x, int z) {
        int size = settings.cellSize();
        return shared(Math.floorDiv(x, size), Math.floorDiv(z, size));
    }

    /** The first {@code giant_geodes} anywhere inside {@code root} (e.g. a random state's final density), or null. */
    @Nullable
    public static GiantGeodes find(DensityFunction root) {
        GiantGeodes[] found = new GiantGeodes[1];
        root.mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (found[0] == null && function instanceof GiantGeodes geodes) {
                    found[0] = geodes;
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

    /** The geode of a cell from the map shared by the copies of this wired function, deciding it the first time. */
    @Nullable
    private GiantGeodeShape.Geode shared(int cellX, int cellZ) {
        long key = (long) cellX << 32 | cellZ & 0xFFFFFFFFL;
        Optional<GiantGeodeShape.Geode> known = decided.get(key);
        if (known == null) {
            if (decided.size() > SHARED_LIMIT) {
                decided.clear();
            }
            // Pure, so two threads deciding the same cell at once put the same answer.
            known = Optional.ofNullable(geodeOfCell(cellX, cellZ));
            decided.put(key, known);
        }
        return known.orElse(null);
    }

    /** The geode of cell (cellX, cellZ): the first of its attempts that passes every test and finds a doorway, or null. Pure. */
    @Nullable
    GiantGeodeShape.Geode geodeOfCell(int cellX, int cellZ) {
        GiantGeodeShape.CellHash hash = (x, z, salt) -> SeededHash.hash(noise, x, z, salt);
        if (!GiantGeodeShape.hasGeode(settings, hash, cellX, cellZ)) {
            return null;
        }
        for (int attempt = 0; attempt < GiantGeodeShape.ATTEMPTS; attempt++) {
            GiantGeodeShape.Geode geode = GiantGeodeShape.candidate(settings, hash, cellX, cellZ, attempt);
            if (!inPlace(geode) || !enclosed(geode)) {
                continue;
            }
            List<GiantGeodeShape.Door> doors = GiantGeodeShape.doors(settings, geode, GiantGeodeShape.attemptHash(hash, cellX, cellZ, attempt),
                    (x, y, z) -> interpolated(x, y, z) <= 0.0 && interpolated(x, y + 1, z) <= 0.0,
                    door -> doorInPlace(geode, door));
            if (!doors.isEmpty()) {
                return geode.withDoors(doors);
            }
        }
        return null;
    }

    /** Deep in the hollows at the centre; in the hollows and clear of the borders at the wall in 8 directions. */
    private boolean inPlace(GiantGeodeShape.Geode geode) {
        if (region(geode.x(), geode.z()) > settings.regionMax()) {
            return false;
        }
        double wall = geode.wall() + settings.doorWidth();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            if (!clear(geode.x() + (int) Math.round(Math.cos(angle) * wall), geode.z() + (int) Math.round(Math.sin(angle) * wall))) {
                return false;
            }
        }
        return clear(geode.x(), geode.z());
    }

    /** A doorway lies in the hollows and clear of the borders over its whole length (every 2 blocks and its end, ± half its width). */
    private boolean doorInPlace(GiantGeodeShape.Geode geode, GiantGeodeShape.Door door) {
        double half = settings.doorWidth() * 0.5;
        for (double t = geode.wall(); ; t = Math.min(t + 2.0, door.end())) {
            for (double side : new double[]{-half, 0.0, half}) {
                int x = geode.x() + (int) Math.round(door.dirX() * t - door.dirZ() * side);
                int z = geode.z() + (int) Math.round(door.dirZ() * t + door.dirX() * side);
                if (!clear(x, z)) {
                    return false;
                }
            }
            if (t >= door.end()) {
                return true;
            }
        }
    }

    /** Where the hollows have weight and the ring's border is not near. */
    private boolean clear(int x, int z) {
        return region(x, z) <= settings.regionReachMax() && edge.compute(new SinglePointContext(x, 0, z)) <= 0.0;
    }

    private double region(int x, int z) {
        return region.compute(new SinglePointContext(x, 0, z));
    }

    /** The centre and the dome (with its shell) solid in the terrain as it generates. */
    private boolean enclosed(GiantGeodeShape.Geode geode) {
        if (interpolated(geode.x(), geode.y(), geode.z()) <= 0.0) {
            return false;
        }
        for (int[] point : geode.domeTestPoints(settings.shell())) {
            if (interpolated(point[0], point[1], point[2]) <= 0.0) {
                return false;
            }
        }
        return true;
    }

    /** {@code shape} at the block as the noise chunk makes it: trilinear between the corners of its noise cell. */
    double interpolated(int x, int y, int z) {
        int x0 = Math.floorDiv(x, CELL_WIDTH) * CELL_WIDTH;
        int y0 = Math.floorDiv(y, CELL_HEIGHT) * CELL_HEIGHT;
        int z0 = Math.floorDiv(z, CELL_WIDTH) * CELL_WIDTH;
        double fx = (x - x0) / (double) CELL_WIDTH;
        double fy = (y - y0) / (double) CELL_HEIGHT;
        double fz = (z - z0) / (double) CELL_WIDTH;
        double v000 = at(x0, y0, z0), v100 = at(x0 + CELL_WIDTH, y0, z0);
        double v010 = at(x0, y0 + CELL_HEIGHT, z0), v110 = at(x0 + CELL_WIDTH, y0 + CELL_HEIGHT, z0);
        double v001 = at(x0, y0, z0 + CELL_WIDTH), v101 = at(x0 + CELL_WIDTH, y0, z0 + CELL_WIDTH);
        double v011 = at(x0, y0 + CELL_HEIGHT, z0 + CELL_WIDTH), v111 = at(x0 + CELL_WIDTH, y0 + CELL_HEIGHT, z0 + CELL_WIDTH);
        double v00 = v000 + (v100 - v000) * fx, v10 = v010 + (v110 - v010) * fx;
        double v01 = v001 + (v101 - v001) * fx, v11 = v011 + (v111 - v011) * fx;
        double v0 = v00 + (v10 - v00) * fy, v1 = v01 + (v11 - v01) * fy;
        return v0 + (v1 - v0) * fz;
    }

    private double at(int x, int y, int z) {
        return shape.compute(new SinglePointContext(x, y, z));
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    /**
     * Only the noises are wired, the markers inside {@code shape}, {@code edge} and {@code region} are not: the noise chunk
     * would turn a {@code flat_cache} into its grid lookup (values at quart positions inside the chunk, exact ones
     * outside), and then the same cell would give another answer in each chunk that evaluates it.
     * <p>
     * The first mapping (the random state wires the noise) starts a new map of decided cells; the copies made from the
     * wired function (one per noise chunk) keep it, so a cell is decided once per world, not once per chunk.
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
        return visitor.apply(new GiantGeodes(shape.mapAll(noisesOnly), edge.mapAll(noisesOnly), region.mapAll(noisesOnly),
                wired, settings, sameNoise ? decided : new ConcurrentHashMap<>()));
    }

    /** The map of decided cells (tests: which copies share it). */
    Map<Long, Optional<GiantGeodeShape.Geode>> decided() {
        return decided;
    }

    @Override
    public double minValue() {
        return INSIDE;
    }

    @Override
    public double maxValue() {
        return OUTSIDE;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    /** A small direct-mapped memory of the cells a thread asked for; a chunk touches one to four. */
    private static final class Cells {
        private final long[] keys = new long[CACHE_SIZE];
        private final GiantGeodeShape.Geode[] geodes = new GiantGeodeShape.Geode[CACHE_SIZE];
        private final boolean[] filled = new boolean[CACHE_SIZE];

        @Nullable
        GiantGeodeShape.Geode get(GiantGeodes owner, int cellX, int cellZ) {
            long key = (long) cellX << 32 | cellZ & 0xFFFFFFFFL;
            int index = (int) ((cellX * 31L + cellZ * 17L) & (CACHE_SIZE - 1));
            if (!filled[index] || keys[index] != key) {
                keys[index] = key;
                geodes[index] = owner.shared(cellX, cellZ);
                filled[index] = true;
            }
            return geodes[index];
        }
    }
}
