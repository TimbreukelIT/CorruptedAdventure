package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.worldgen.feature.DewGeodeShape;

import javax.annotation.Nullable;

/**
 * {@code corruptedadventure:dew_geodes} (change 35): the cavities of the dew geodes of Nether ring 2, −1 inside a cavity
 * and +10 (neutral under {@code min}) everywhere else. Meant as {@code min(terrain, dew_geodes)} outside the
 * interpolation, so the cavities are exact ellipsoids. The cavity exists from the noise step on: before the carvers and
 * before every feature, so all of them see the same terrain whatever the order the chunks are decorated in (a feature
 * that dug it would break that). The lining is the feature {@code dew_geode}.
 * <p>
 * The geometry is {@link DewGeodeShape}. A candidate only becomes a cavity if it lies at least {@code edge}-clear of the
 * ring's borders ({@code edge} = 0) and its centre and the 26 {@link DewGeodeShape.Geode#wallTestPoints test points} around
 * it are solid in {@code terrain}: a geode always sits enclosed in a wall, never in a hall. The terrain function must hold
 * no {@code interpolated} marker.
 * <p>
 * Pure: from the seeded cell hash, the region and terrain functions and the position; the per-thread cell cache only
 * remembers answers. One cell lookup per block (the geode lies within its cell), 27 terrain evaluations per cell.
 */
public final class DewGeodes implements DensityFunction {
    private static final double OUTSIDE = 10.0;
    private static final double INSIDE = -1.0;
    private static final int CACHE_SIZE = 8;

    public static final MapCodec<DewGeodes> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("terrain").forGetter(d -> d.terrain),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("edge").forGetter(d -> d.edge),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("region").forGetter(d -> d.region),
            NoiseHolder.CODEC.fieldOf("noise").forGetter(d -> d.noise),
            DewGeodeShape.Settings.CODEC.fieldOf("settings").forGetter(d -> d.settings)
    ).apply(instance, DewGeodes::new));

    public static final KeyDispatchDataCodec<DewGeodes> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final DensityFunction terrain;
    private final DensityFunction edge;
    private final DensityFunction region;
    private final NoiseHolder noise;
    private final DewGeodeShape.Settings settings;
    private final ThreadOwned<Cells> cells = new ThreadOwned<>(Cells::new);

    public DewGeodes(DensityFunction terrain, DensityFunction edge, DensityFunction region, NoiseHolder noise, DewGeodeShape.Settings settings) {
        this.terrain = terrain;
        this.edge = edge;
        this.region = region;
        this.noise = noise;
        this.settings = settings;
    }

    @Override
    public double compute(FunctionContext context) {
        int size = settings.cellSize();
        DewGeodeShape.Geode geode = cells.get().get(this, Math.floorDiv(context.blockX(), size), Math.floorDiv(context.blockZ(), size));
        return geode != null && geode.inCavity(context.blockX(), context.blockY(), context.blockZ()) ? INSIDE : OUTSIDE;
    }

    /**
     * The geode of cell (cellX, cellZ): the first of its attempts that passes the border and wall tests, or null. Pure. The
     * more attempts a cell has, the more of them find a solid enough place (walls and pillars are a minority of a hall's
     * volume), without moving the geode out of its cell.
     */
    @Nullable
    DewGeodeShape.Geode geodeOfCell(int cellX, int cellZ) {
        for (int attempt = 0; attempt < DewGeodeShape.ATTEMPTS; attempt++) {
            DewGeodeShape.Geode geode = DewGeodeShape.candidate(settings, (x, z, salt) -> SeededHash.hash(noise, x, z, salt), cellX, cellZ, attempt,
                    (x, z) -> region.compute(new SinglePointContext(x, 0, z)));
            if (geode != null && enclosed(geode)) {
                return geode;
            }
        }
        return null;
    }

    private boolean enclosed(DewGeodeShape.Geode geode) {
        if (edge.compute(new SinglePointContext(geode.x(), geode.y(), geode.z())) > 0.0
                || terrain.compute(new SinglePointContext(geode.x(), geode.y(), geode.z())) <= 0.0) {
            return false;
        }
        for (int[] point : geode.wallTestPoints(settings.shell(), 0)) {
            if (terrain.compute(new SinglePointContext(point[0], point[1], point[2])) <= 0.0) {
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
     * Only the noises are wired, the markers inside {@code terrain}, {@code edge} and {@code region} are not: the noise
     * chunk would turn a {@code flat_cache} into its grid lookup (values at quart positions inside the chunk, exact ones
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
        return visitor.apply(new DewGeodes(terrain.mapAll(noisesOnly), edge.mapAll(noisesOnly), region.mapAll(noisesOnly),
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
        private final DewGeodeShape.Geode[] geodes = new DewGeodeShape.Geode[CACHE_SIZE];
        private final boolean[] filled = new boolean[CACHE_SIZE];

        @Nullable
        DewGeodeShape.Geode get(DewGeodes owner, int cellX, int cellZ) {
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
