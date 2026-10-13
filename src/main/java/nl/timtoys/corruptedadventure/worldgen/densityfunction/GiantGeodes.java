package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantGeodeShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * {@code corruptedadventure:giant_geodes} (65): the hollows of the giant geodes of Nether ring 4's {@code geode_hollows},
 * −1 inside a hollow (the cavity above its flat floor, and its doorway) and +10 (neutral under {@code min}) everywhere
 * else. Meant as {@code min(terrain, giant_geodes)} outside the interpolation, so the hollows are exact. The hollow exists
 * from the noise step on, before every feature, so the lining (78) sees the same terrain in every load order.
 * <p>
 * The geometry is {@link GiantGeodeShape}. A candidate only becomes a geode if its centre lies in the hollows (region at
 * most {@code region_max}), clear of the ring's borders ({@code edge} = 0 at the centre and at the four ends of its
 * reach) and its dome is enclosed: the {@code shape} function (the uninterpolated terrain, without markers that cache per
 * chunk) is solid at the centre and at the {@link GiantGeodeShape.Geode#domeTestPoints dome test points}. The sides may
 * break out of the rock.
 * <p>
 * Every geode gets one to {@code doors_max} doorways that each reach open terrain ({@link GiantGeodeShape#doors}); an
 * attempt without one is no geode. Pure: from the seeded cell hash, the region, edge and shape functions and the
 * position; the per-thread cell cache only remembers answers. One cell lookup per block (the geode with its doorways lies
 * within its cell).
 */
public final class GiantGeodes implements DensityFunction {
    private static final double OUTSIDE = 10.0;
    private static final double INSIDE = -1.0;
    private static final int CACHE_SIZE = 8;

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
    private final ThreadOwned<Cells> cells = new ThreadOwned<>(Cells::new);

    public GiantGeodes(DensityFunction shape, DensityFunction edge, DensityFunction region, NoiseHolder noise, GiantGeodeShape.Settings settings) {
        this.shape = shape;
        this.edge = edge;
        this.region = region;
        this.noise = noise;
        this.settings = settings;
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

    /** The geode of cell (cellX, cellZ): the first of its attempts that passes the region, border and dome tests and finds a doorway, or null. Pure. */
    @Nullable
    GiantGeodeShape.Geode geodeOfCell(int cellX, int cellZ) {
        GiantGeodeShape.CellHash hash = (x, z, salt) -> SeededHash.hash(noise, x, z, salt);
        if (!GiantGeodeShape.hasGeode(settings, hash, cellX, cellZ)) {
            return null;
        }
        for (int attempt = 0; attempt < GiantGeodeShape.ATTEMPTS; attempt++) {
            GiantGeodeShape.Geode geode = GiantGeodeShape.candidate(settings, hash, cellX, cellZ, attempt);
            if (!enclosed(geode)) {
                continue;
            }
            List<GiantGeodeShape.Door> doors = GiantGeodeShape.doors(settings, geode, GiantGeodeShape.attemptHash(hash, cellX, cellZ, attempt),
                    (x, y, z) -> shape.compute(new SinglePointContext(x, y, z)) <= 0.0);
            if (!doors.isEmpty()) {
                return geode.withDoors(doors);
            }
        }
        return null;
    }

    /** In the hollows, clear of the borders (the whole reach a doorway may have), the centre and the dome in rock. */
    private boolean enclosed(GiantGeodeShape.Geode geode) {
        if (region.compute(new SinglePointContext(geode.x(), 0, geode.z())) > settings.regionMax()) {
            return false;
        }
        int reach = (int) Math.ceil(geode.wall() + settings.doorLength() + settings.doorWidth() * 0.5);
        int[][] ends = {{0, 0}, {reach, 0}, {-reach, 0}, {0, reach}, {0, -reach}};
        for (int[] end : ends) {
            if (edge.compute(new SinglePointContext(geode.x() + end[0], 0, geode.z() + end[1])) > 0.0) {
                return false;
            }
        }
        if (shape.compute(new SinglePointContext(geode.x(), geode.y(), geode.z())) <= 0.0) {
            return false;
        }
        for (int[] point : geode.domeTestPoints(settings.shell())) {
            if (shape.compute(new SinglePointContext(point[0], point[1], point[2])) <= 0.0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    /**
     * Only the noises are wired, the markers inside {@code shape}, {@code edge} and {@code region} are not: the noise chunk
     * would turn a {@code flat_cache} into its grid lookup (values at quart positions inside the chunk, exact ones
     * outside), and then the same cell would give another answer in each chunk that evaluates it.
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
        return visitor.apply(new GiantGeodes(shape.mapAll(noisesOnly), edge.mapAll(noisesOnly), region.mapAll(noisesOnly),
                visitor.visitNoise(noise), settings));
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
                geodes[index] = owner.geodeOfCell(cellX, cellZ);
                filled[index] = true;
            }
            return geodes[index];
        }
    }
}
