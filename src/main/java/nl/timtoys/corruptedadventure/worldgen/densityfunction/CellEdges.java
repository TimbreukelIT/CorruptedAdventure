package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Objects;

/**
 * {@code corruptedadventure:cell_edges}: a network of fractures, 2D. A jittered grid of {@code cell_size} gives one
 * site per cell; the edges of their Voronoi diagram are the fractures, and each edge is open with
 * {@code open_chance} (a seeded hash per pair of cells). The output is the distance in blocks to the nearest open
 * edge, capped at {@code max_distance}. The domain is warped by {@code warp} first, so the fractures meander.
 * <p>
 * For a pair of sites {@code a}, {@code b} the distance is {@code max(|h_ab|, s_ab)}: {@code h_ab} is the signed
 * distance to their bisector, {@code s_ab} how far the point lies inside the region of a third site. On the edge
 * itself that is the exact distance to the bisector; past the end of the edge it grows, so an open edge ends where
 * it meets a closed one. Both terms move at most one block per block, so the output is continuous. The warp stretches
 * the domain; the distance of the nearest edge is divided by that stretch across the edge, so it is in blocks.
 * <p>
 * Pure: a function of the seed (through the noises) and x, z only. The per-thread site cache does not change results.
 */
public final class CellEdges implements DensityFunction {
    /** Cells around the point's own cell that can hold a site within {@code max_distance} of an edge near it. */
    private static final int REACH = 2;
    private static final double WARP_Z_OFFSET = 1024.5;
    private static final long SITE_SALT = 1;
    private static final double MIN_STRETCH = 0.25;

    public static final MapCodec<CellEdges> MAP_CODEC = RecordCodecBuilder.<CellEdges>mapCodec(instance -> instance.group(
                    Codec.doubleRange(32.0, 4096.0).fieldOf("cell_size").forGetter(e -> e.cellSize),
                    Codec.doubleRange(0.0, 0.9).fieldOf("jitter").forGetter(e -> e.jitter),
                    Codec.doubleRange(0.0, 1.0).fieldOf("open_chance").forGetter(e -> e.openChance),
                    NoiseHolder.CODEC.fieldOf("warp").forGetter(e -> e.warp),
                    Codec.doubleRange(0.0, 64.0).fieldOf("warp_amplitude").forGetter(e -> e.warpAmplitude),
                    NoiseHolder.CODEC.fieldOf("hash").forGetter(e -> e.hash),
                    Codec.doubleRange(1.0, 1024.0).optionalFieldOf("max_distance", 24.0).forGetter(e -> e.maxDistance)
            ).apply(instance, CellEdges::new))
            .validate(CellEdges::validate);

    public static final KeyDispatchDataCodec<CellEdges> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final double cellSize;
    private final double jitter;
    private final double openChance;
    private final NoiseHolder warp;
    private final double warpAmplitude;
    private final NoiseHolder hash;
    private final double maxDistance;
    private final ThreadOwned<SiteCache> sites = new ThreadOwned<>(SiteCache::new);

    public CellEdges(double cellSize, double jitter, double openChance, NoiseHolder warp, double warpAmplitude,
                     NoiseHolder hash, double maxDistance) {
        this.cellSize = cellSize;
        this.jitter = jitter;
        this.openChance = openChance;
        this.warp = warp;
        this.warpAmplitude = warpAmplitude;
        this.hash = hash;
        this.maxDistance = maxDistance;
    }

    private static DataResult<CellEdges> validate(CellEdges edges) {
        // With jitter <= 0.9 the 5 × 5 cells around a point hold every site that matters within cell_size / 4.
        return edges.maxDistance <= edges.cellSize / 4.0
                ? DataResult.success(edges)
                : DataResult.error(() -> "cell_edges needs max_distance <= cell_size / 4, got " + edges.maxDistance);
    }

    /**
     * The nearest open edge to a block column, or {@link Edge#NONE} if none lies within {@code max_distance} (in the
     * warped domain). The distance is in blocks (corrected for the warp's stretch).
     * {@code along} is the position along the edge (blocks from the midpoint of its two sites, in a fixed direction
     * per edge), {@code seed} a hash of the edge for its own random properties.
     */
    public Edge nearestOpenEdge(double x, double z) {
        double qx = x + warpAmplitude * warp.getValue(x, 0.0, z);
        double qz = z + warpAmplitude * warp.getValue(x, WARP_Z_OFFSET, z);
        int ci = (int) Math.floor(qx / cellSize);
        int cj = (int) Math.floor(qz / cellSize);
        SiteCache cache = sites.get();
        int side = 2 * REACH + 1;
        double[] sx = new double[side * side];
        double[] sz = new double[side * side];
        long[] sh = new long[side * side];
        int[] si = new int[side * side];
        int[] sj = new int[side * side];
        double nearest = Double.MAX_VALUE;
        int n = 0;
        for (int dj = -REACH; dj <= REACH; dj++) {
            for (int di = -REACH; di <= REACH; di++) {
                int slot = cache.find(this, ci + di, cj + dj);
                sx[n] = cache.x[slot];
                sz[n] = cache.z[slot];
                sh[n] = cache.hash[slot];
                si[n] = ci + di;
                sj[n] = cj + dj;
                nearest = Math.min(nearest, Math.hypot(qx - sx[n], qz - sz[n]));
                n++;
            }
        }
        double[] jacobian = jacobian(x, z);
        // Only sites this close can be one of the two nearest sites of a point within max_distance (in blocks, so
        // up to max_distance times the largest stretch of the warp in the warped domain).
        double keep = nearest + 2.0 * maxDistance * maxStretch(jacobian);
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (Math.hypot(qx - sx[i], qz - sz[i]) <= keep) {
                sx[k] = sx[i];
                sz[k] = sz[i];
                sh[k] = sh[i];
                si[k] = si[i];
                sj[k] = sj[i];
                k++;
            }
        }
        double best = maxDistance;
        int bestA = -1;
        int bestB = -1;
        long bestSeed = 0;
        for (int a = 0; a < k; a++) {
            for (int b = a + 1; b < k; b++) {
                // Each edge's warped distance is divided by the warp's stretch across that edge before comparing, so
                // the result stays continuous where the nearest edge changes.
                double length = Math.hypot(sx[b] - sx[a], sz[b] - sz[a]);
                double stretch = stretch(jacobian, (sx[b] - sx[a]) / length, (sz[b] - sz[a]) / length);
                double limit = best * stretch;
                double hab = bisector(qx, qz, sx[a], sz[a], sx[b], sz[b]);
                if (Math.abs(hab) >= limit) {
                    continue;
                }
                double past = Double.NEGATIVE_INFINITY;
                for (int c = 0; c < k && past < limit; c++) {
                    if (c != a && c != b) {
                        past = Math.max(past, Math.max(-bisector(qx, qz, sx[a], sz[a], sx[c], sz[c]),
                                -bisector(qx, qz, sx[b], sz[b], sx[c], sz[c])));
                    }
                }
                double g = Math.max(Math.abs(hab), past) / stretch;
                if (g >= best) {
                    continue;
                }
                boolean aFirst = si[a] < si[b] || si[a] == si[b] && sj[a] < sj[b];
                long seed = edgeSeed(aFirst ? sh[a] : sh[b], aFirst ? sh[b] : sh[a]);
                if (SeededHash.unit(seed) < openChance) {
                    best = g;
                    bestA = aFirst ? a : b;
                    bestB = aFirst ? b : a;
                    bestSeed = seed;
                }
            }
        }
        if (bestA < 0) {
            return Edge.NONE;
        }
        double dx = sx[bestB] - sx[bestA];
        double dz = sz[bestB] - sz[bestA];
        double length = Math.hypot(dx, dz);
        double along = ((qx - (sx[bestA] + sx[bestB]) / 2.0) * -dz + (qz - (sz[bestA] + sz[bestB]) / 2.0) * dx) / length;
        return new Edge(best, bestSeed, along);
    }

    /** The Jacobian J = I + ∂w/∂p of the warp at a column (finite differences): {dqx/dx, dqx/dz, dqz/dx, dqz/dz}. */
    private double[] jacobian(double x, double z) {
        if (warpAmplitude == 0.0) {
            return new double[]{1.0, 0.0, 0.0, 1.0};
        }
        double h = 0.5;
        double a = warpAmplitude / (2.0 * h);
        return new double[]{
                1.0 + a * (warp.getValue(x + h, 0.0, z) - warp.getValue(x - h, 0.0, z)),
                a * (warp.getValue(x, 0.0, z + h) - warp.getValue(x, 0.0, z - h)),
                a * (warp.getValue(x + h, WARP_Z_OFFSET, z) - warp.getValue(x - h, WARP_Z_OFFSET, z)),
                1.0 + a * (warp.getValue(x, WARP_Z_OFFSET, z + h) - warp.getValue(x, WARP_Z_OFFSET, z - h))};
    }

    /**
     * How much the warp stretches distances across an edge with unit normal (nx, nz) in the warped domain: the length
     * of Jᵀn. Dividing by it turns the warped distance into blocks, so the warp bends the fractures without changing
     * their width.
     */
    private static double stretch(double[] j, double nx, double nz) {
        return Math.max(MIN_STRETCH, Math.hypot(j[0] * nx + j[2] * nz, j[1] * nx + j[3] * nz));
    }

    /** The largest stretch in any direction: the largest singular value of J. */
    private static double maxStretch(double[] j) {
        double t = j[0] * j[0] + j[1] * j[1] + j[2] * j[2] + j[3] * j[3];
        double det = j[0] * j[3] - j[1] * j[2];
        return Math.max(MIN_STRETCH, Math.sqrt((t + Math.sqrt(Math.max(0.0, t * t - 4.0 * det * det))) / 2.0));
    }

    /** Signed distance from q to the bisector of a and b; positive on a's side. */
    static double bisector(double qx, double qz, double ax, double az, double bx, double bz) {
        double da = (qx - ax) * (qx - ax) + (qz - az) * (qz - az);
        double db = (qx - bx) * (qx - bx) + (qz - bz) * (qz - bz);
        return (db - da) / (2.0 * Math.hypot(bx - ax, bz - az));
    }

    static long edgeSeed(long first, long second) {
        return SeededHash.mix(first ^ Long.rotateLeft(second, 23) ^ 0x2545F4914F6CDD1DL);
    }

    /** Site hash and position of cell (i, j), in the warped domain. */
    long siteHash(int i, int j) {
        return SeededHash.hash(hash, i, j, SITE_SALT);
    }

    double siteX(int i, long siteHash) {
        return (i + 0.5 + jitter * (SeededHash.unit(siteHash, 1) - 0.5)) * cellSize;
    }

    double siteZ(int j, long siteHash) {
        return (j + 0.5 + jitter * (SeededHash.unit(siteHash, 2) - 0.5)) * cellSize;
    }

    boolean isOpen(long seed) {
        return SeededHash.unit(seed) < openChance;
    }

    double cellSize() {
        return cellSize;
    }

    @Override
    public double compute(FunctionContext context) {
        return Math.min(maxDistance, nearestOpenEdge(context.blockX(), context.blockZ()).distance());
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new CellEdges(cellSize, jitter, openChance, visitor.visitNoise(warp), warpAmplitude,
                visitor.visitNoise(hash), maxDistance));
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return maxDistance;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    /**
     * {@code function} as a {@code cell_edges}, looking through holder references and markers (a JSON reference to a
     * named density function arrives as a holder). Throws if it is something else.
     */
    static CellEdges resolve(DensityFunction function) {
        DensityFunction current = function;
        for (int depth = 0; depth < 16; depth++) {
            if (current instanceof CellEdges edges) {
                return edges;
            }
            current = RingGated.unwrap(current);
            if (current == null) {
                break;
            }
        }
        throw new IllegalStateException("Expected a corruptedadventure:cell_edges density function, got " + function);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CellEdges e && cellSize == e.cellSize && jitter == e.jitter && openChance == e.openChance
                && warp.equals(e.warp) && warpAmplitude == e.warpAmplitude && hash.equals(e.hash) && maxDistance == e.maxDistance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cellSize, jitter, openChance, warp, warpAmplitude, hash, maxDistance);
    }

    /** Distance to, seed of and position along the nearest open edge. */
    public record Edge(double distance, long seed, double along) {
        public static final Edge NONE = new Edge(Double.MAX_VALUE, 0L, 0.0);

        public boolean exists() {
            return this != NONE;
        }
    }

    /** Direct-mapped per-thread cache of site positions; only a cache, results do not depend on it. */
    private static final class SiteCache {
        private static final int SIZE = 64;
        private final long[] keys = new long[SIZE];
        private final boolean[] filled = new boolean[SIZE];
        private final long[] hash = new long[SIZE];
        private final double[] x = new double[SIZE];
        private final double[] z = new double[SIZE];

        int find(CellEdges edges, int i, int j) {
            long key = (long) i << 32 | j & 0xFFFFFFFFL;
            int slot = (i & 7) | (j & 7) << 3;
            if (!filled[slot] || keys[slot] != key) {
                filled[slot] = true;
                keys[slot] = key;
                hash[slot] = edges.siteHash(i, j);
                x[slot] = edges.siteX(i, hash[slot]);
                z[slot] = edges.siteZ(j, hash[slot]);
            }
            return slot;
        }
    }
}
