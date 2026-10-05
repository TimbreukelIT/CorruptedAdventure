package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeededHash;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The pure geometry of a dew geode (change 35), shared by the density function {@code dew_geodes} (which digs the
 * cavity) and the feature {@code dew_geode} (which lines it), so both agree on where a geode is without reading each
 * other. Everything follows from the world seed (through a cell hash) and the cell: one candidate per cell of a
 * world-fixed 2D grid, its centre kept {@link Settings#margin()} blocks from the cell edge so the whole geode, lining
 * included, stays inside its cell.
 * <p>
 * A candidate is only a geode if the terrain around it is solid ({@link #wallTestPoints}): the density function tests the
 * terrain function there, the feature the real blocks.
 */
public final class DewGeodeShape {
    private static final long SALT = 11;
    /** How much the cavity differs from a sphere: axis stretch in [1 − this, 1 + this]. */
    private static final double STRETCH = 0.15;
    /** Lining reaches this far past the cavity. */
    public static final double LINING = 1.5;

    private DewGeodeShape() {
    }

    /** {@link SeededHash} of a cell, whatever noise it comes from (a density function's holder, a feature's noise). */
    @FunctionalInterface
    public interface CellHash {
        long hash(int cellX, int cellZ, int salt);
    }

    /**
     * @param regionThreshold the region noise value at which the dripshroom forest (above) turns into spore caves (below)
     * @param radiusForest    inner radius of the cavity, per regime
     * @param yForest         centre Y range, per regime (centre of the cavity)
     * @param bottom          the cavity never reaches below this Y (the lava sea)
     * @param shell           solid rock wanted around the cavity
     */
    public record Settings(int cellSize, double regionThreshold, double chanceForest, double chanceCaves, int radiusMinForest,
                           int radiusMaxForest, int radiusMinCaves, int radiusMaxCaves, int yMinForest, int yMaxForest,
                           int yMinCaves, int yMaxCaves, int bottom, int shell) {
        public static final Codec<Settings> CODEC = RecordCodecBuilder.<Settings>create(instance -> instance.group(
                Codec.intRange(16, 256).fieldOf("cell_size").forGetter(Settings::cellSize),
                Codec.DOUBLE.fieldOf("region_threshold").forGetter(Settings::regionThreshold),
                Codec.doubleRange(0.0, 1.0).fieldOf("chance_forest").forGetter(Settings::chanceForest),
                Codec.doubleRange(0.0, 1.0).fieldOf("chance_caves").forGetter(Settings::chanceCaves),
                Codec.intRange(2, 16).fieldOf("radius_min_forest").forGetter(Settings::radiusMinForest),
                Codec.intRange(2, 16).fieldOf("radius_max_forest").forGetter(Settings::radiusMaxForest),
                Codec.intRange(2, 16).fieldOf("radius_min_caves").forGetter(Settings::radiusMinCaves),
                Codec.intRange(2, 16).fieldOf("radius_max_caves").forGetter(Settings::radiusMaxCaves),
                Codec.INT.fieldOf("y_min_forest").forGetter(Settings::yMinForest),
                Codec.INT.fieldOf("y_max_forest").forGetter(Settings::yMaxForest),
                Codec.INT.fieldOf("y_min_caves").forGetter(Settings::yMinCaves),
                Codec.INT.fieldOf("y_max_caves").forGetter(Settings::yMaxCaves),
                Codec.INT.fieldOf("bottom").forGetter(Settings::bottom),
                Codec.intRange(1, 8).fieldOf("shell").forGetter(Settings::shell)
        ).apply(instance, Settings::new)).validate(Settings::validate);

        private static DataResult<Settings> validate(Settings settings) {
            if (settings.radiusMinForest > settings.radiusMaxForest || settings.radiusMinCaves > settings.radiusMaxCaves) {
                return DataResult.error(() -> "dew geode: radius min above max");
            }
            if (settings.yMinForest > settings.yMaxForest || settings.yMinCaves > settings.yMaxCaves) {
                return DataResult.error(() -> "dew geode: y min above max");
            }
            if (settings.cellSize % 16 != 0) {
                // The feature takes the cell of a chunk's corner: a chunk must lie within one cell.
                return DataResult.error(() -> "dew geode: cell_size must be a multiple of 16");
            }
            if (settings.cellSize - 2 * settings.margin() < 4) {
                return DataResult.error(() -> "dew geode: cell_size too small for the radii");
            }
            return DataResult.success(settings);
        }

        /** Distance from a cell edge to the nearest possible centre: the whole geode with its lining fits in the cell. */
        public int margin() {
            int radius = Math.max(radiusMaxForest, radiusMaxCaves);
            return (int) Math.ceil(radius * (1.0 + STRETCH) + LINING) + 1;
        }

        public boolean forest(double region) {
            return region >= regionThreshold;
        }
    }

    /**
     * A geode candidate. The cavity is the ellipsoid {@link #distance}{@code (x, y, z) <= radius}.
     *
     * @param forest in the dripshroom forest's regime (else the spore caves')
     */
    public record Geode(int x, int y, int z, int radius, double stretchX, double stretchY, double stretchZ, boolean forest) {
        /** Stretched distance in blocks from the centre to the block {@code (x, y, z)}; the cavity is {@code <= radius}. */
        public double distance(int blockX, int blockY, int blockZ) {
            double dx = (blockX - x) / stretchX;
            double dy = (blockY - y) / stretchY;
            double dz = (blockZ - z) / stretchZ;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public boolean inCavity(int blockX, int blockY, int blockZ) {
            return distance(blockX, blockY, blockZ) <= radius;
        }

        /** Largest distance in blocks, on any axis, from the centre to the lining. */
        public int reach() {
            return (int) Math.ceil((radius + LINING) * Math.max(stretchX, Math.max(stretchY, stretchZ)));
        }

        /**
         * The 26 points around the centre (6 axes, 12 edges, 8 corners of the direction cube, normalised) at the distance
         * where the cavity's wall plus {@code shell - inset} blocks of rock must still be solid. {@code inset} 0 for the density
         * function (exact terrain), 1 for the feature (the real, interpolated terrain differs a little).
         */
        public List<int[]> wallTestPoints(int shell, int inset) {
            double stretch = Math.max(stretchX, Math.max(stretchY, stretchZ));
            double distance = radius * stretch + shell - inset;
            List<int[]> points = new ArrayList<>(26);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx != 0 || dy != 0 || dz != 0) {
                            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            points.add(new int[]{x + (int) Math.round(dx / length * distance),
                                    y + (int) Math.round(dy / length * distance),
                                    z + (int) Math.round(dz / length * distance)});
                        }
                    }
                }
            }
            return points;
        }
    }

    /** How many positions a cell tries, in order; the first that passes the wall test (density function) is the geode of the cell. */
    public static final int ATTEMPTS = 48;

    /**
     * Attempt {@code attempt} of cell ({@code cellX}, {@code cellZ}), or null. Whether the cell has a geode at all is one
     * draw per cell ({@code chance}); each attempt has its own centre, radius, height and shape. Pure: the same seed (through
     * {@code hash}) and region values give the same answer.
     *
     * @param region the region noise value at a block column (to know the regime)
     */
    @Nullable
    public static Geode candidate(Settings settings, CellHash hash, int cellX, int cellZ, int attempt, RegionAt region) {
        long cell = hash.hash(cellX, cellZ, (int) SALT);
        long h = hash.hash(cellX, cellZ, (int) SALT + 1 + attempt);
        int margin = settings.margin();
        int span = settings.cellSize() - 2 * margin;
        int x = cellX * settings.cellSize() + margin + (int) (SeededHash.unit(h, 1) * span);
        int z = cellZ * settings.cellSize() + margin + (int) (SeededHash.unit(h, 2) * span);
        boolean forest = settings.forest(region.at(x, z));
        if (SeededHash.unit(cell, 0) >= (forest ? settings.chanceForest() : settings.chanceCaves())) {
            return null;
        }
        int radiusMin = forest ? settings.radiusMinForest() : settings.radiusMinCaves();
        int radiusMax = forest ? settings.radiusMaxForest() : settings.radiusMaxCaves();
        int radius = radiusMin + (int) (SeededHash.unit(h, 3) * (radiusMax - radiusMin + 1));
        int yMin = Math.max(forest ? settings.yMinForest() : settings.yMinCaves(), settings.bottom() + (int) Math.ceil(radius * (1.0 + STRETCH)) + 1);
        int yMax = forest ? settings.yMaxForest() : settings.yMaxCaves();
        if (yMin > yMax) {
            return null;
        }
        int y = yMin + (int) (SeededHash.unit(h, 4) * (yMax - yMin + 1));
        return new Geode(x, y, z, radius, stretch(h, 5), stretch(h, 6), stretch(h, 7), forest);
    }

    /** The attempts of the cell that contains the block column ({@code x}, {@code z}), in order, without the null ones; a chunk lies within one cell. */
    public static List<Geode> candidatesOfCell(Settings settings, int x, int z, CellHash hash, RegionAt region) {
        int cellX = Math.floorDiv(x, settings.cellSize());
        int cellZ = Math.floorDiv(z, settings.cellSize());
        List<Geode> candidates = new ArrayList<>(ATTEMPTS);
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            Geode geode = candidate(settings, hash, cellX, cellZ, attempt, region);
            if (geode != null) {
                candidates.add(geode);
            }
        }
        return candidates;
    }

    private static double stretch(long hash, int n) {
        return 1.0 - STRETCH + 2.0 * STRETCH * SeededHash.unit(hash, n);
    }

    @FunctionalInterface
    public interface RegionAt {
        double at(int x, int z);
    }
}
