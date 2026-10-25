package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * The pure plan of {@link TerracePoolFeature} (G2 ring 3, decision 6 = A), without world access: the sulfur fields'
 * boiling pools lie in clusters, one cluster at most per cell of a world grid, with open ground between. A cluster is a
 * function of the seed and its cell only: whether there is one, its centre (in the middle third of the cell) and its
 * pools, each with a centre within {@link #spread} of the cluster's, a radius, a stretch, an angle and a ragged edge.
 * Tries that would crowd an earlier pool of the cluster (lip into lip) are dropped, so pools of a cluster never merge,
 * though their terraces may join. Every chunk a pool covers computes the same plan.
 */
final class TerracePoolLayout {
    static final int MIN_CELL = 32;
    static final int MAX_CELL = 64;
    static final int MAX_POOLS = 12;
    static final int EDGE_POINTS = 9;
    /** The ragged edge keeps 0.6–1 of the radius per edge point. */
    static final float EDGE_KEEP = 0.6F;
    static final float MAX_STRETCH = 1.35F;
    /** Between two pools' nominal (stretched) edges: their lips touch at most. */
    static final double GAP = 2.5;
    private static final long SALT = 0x5DEECE66DL * 31L + 4706L;

    private TerracePoolLayout() {
    }

    /**
     * One pool.
     *
     * @param x       centre column
     * @param z       centre column
     * @param radius  radius before the edge and the stretch
     * @param stretch how much longer it is along {@code angle} than across
     * @param angle   the direction of the stretch
     * @param edge    per edge point the share of the radius it keeps
     * @param seed    the pool's own seed (its colour)
     */
    record Pool(int x, int z, int radius, float stretch, float angle, float[] edge, long seed) {
        /** How far the pool with its lip and terraces reaches from its centre, in whole blocks. */
        int reach() {
            return Mth.ceil((radius + TerracePoolFeature.TERRACE) * stretch);
        }
    }

    /**
     * How far from its cluster's centre a pool's centre lies at most. The cluster's centre lies in the middle third of
     * the cell, so every pool's centre lies inside its cell and its blocks at most {@link TerracePoolFeature#MAX_REACH}
     * beyond.
     */
    static int spread(int cell) {
        return cell / 3;
    }

    /** The pools of cell (i, j), in their fixed order; empty without a cluster. */
    static List<Pool> cluster(long seed, int i, int j, int cell, float chance, IntProvider pools, IntProvider radius) {
        RandomSource random = RandomSource.create(mix(seed ^ SALT ^ mix(i * 0x9E3779B97F4A7C15L + j * 0xC2B2AE3D27D4EB4FL)));
        if (random.nextFloat() >= chance) {
            return List.of();
        }
        double centreX = i * cell + cell / 3.0 + random.nextDouble() * (cell / 3.0);
        double centreZ = j * cell + cell / 3.0 + random.nextDouble() * (cell / 3.0);
        int spread = spread(cell);
        int tries = pools.sample(random);
        List<Pool> kept = new ArrayList<>(tries);
        for (int t = 0; t < tries; t++) {
            // Always the same draws per try, so a dropped try does not move the next ones.
            int r = radius.sample(random);
            double direction = random.nextDouble() * Mth.TWO_PI;
            double distance = spread * Math.sqrt(random.nextDouble());
            float stretch = 1.0F + random.nextFloat() * (MAX_STRETCH - 1.0F);
            float angle = random.nextFloat() * Mth.TWO_PI;
            float[] edge = new float[EDGE_POINTS];
            for (int e = 0; e < EDGE_POINTS; e++) {
                edge[e] = EDGE_KEEP + (1.0F - EDGE_KEEP) * random.nextFloat();
            }
            long poolSeed = random.nextLong();
            Pool pool = new Pool(Mth.floor(centreX + Math.cos(direction) * distance), Mth.floor(centreZ + Math.sin(direction) * distance), r,
                    stretch, angle, edge, poolSeed);
            if (kept.stream().noneMatch(other -> crowds(pool, other))) {
                kept.add(pool);
            }
        }
        return kept;
    }

    /** Whether two pools lie so close that their lips would overlap. */
    static boolean crowds(Pool a, Pool b) {
        double apart = a.radius() * a.stretch() + b.radius() * b.stretch() + GAP;
        double dx = a.x() - b.x();
        double dz = a.z() - b.z();
        return dx * dx + dz * dz < apart * apart;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
