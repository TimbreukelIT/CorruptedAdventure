package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.valueproviders.UniformInt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure plan of the sulfur fields' boiling pools (G2 ring 3, decision 6 = A): clusters per cell, pure in seed and cell. */
class TerracePoolLayoutTest {
    private static final int CELL = SulfurFieldsFeatures.CLUSTER_CELL;
    private static final float CHANCE = SulfurFieldsFeatures.CLUSTER_CHANCE;
    private static final UniformInt TRIES = SulfurFieldsFeatures.CLUSTER_TRIES;

    private static List<TerracePoolLayout.Pool> cluster(long seed, int i, int j) {
        return TerracePoolLayout.cluster(seed, i, j, CELL, CHANCE, TRIES, SulfurFieldsFeatures.poolRadius());
    }

    @Test
    void aPoolReachesLessThanAChunk() {
        // Then its centre lies within one chunk of every chunk it covers, so that chunk sees the same centre checks.
        assertTrue(TerracePoolFeature.MAX_REACH < 16, "reach " + TerracePoolFeature.MAX_REACH);
    }

    @Test
    void clustersArePureStayInTheirCellAndNeverCrowd() {
        int clusters = 0;
        int pools = 0;
        int[] radii = new int[TerracePoolFeature.MAX_RADIUS + 1];
        for (int i = -40; i < 40; i++) {
            for (int j = -40; j < 40; j++) {
                List<TerracePoolLayout.Pool> plan = cluster(20260930L, i, j);
                List<TerracePoolLayout.Pool> again = cluster(20260930L, i, j);
                assertEquals(plan.size(), again.size());
                if (plan.isEmpty()) {
                    continue;
                }
                clusters++;
                for (int k = 0; k < plan.size(); k++) {
                    TerracePoolLayout.Pool pool = plan.get(k);
                    TerracePoolLayout.Pool same = again.get(k);
                    assertTrue(pool.x() == same.x() && pool.z() == same.z() && pool.radius() == same.radius() && pool.seed() == same.seed());
                    // The centre inside the cell, so a chunk only needs the cells within MAX_REACH.
                    assertTrue(pool.x() >= i * CELL && pool.x() < (i + 1) * CELL, "x " + pool);
                    assertTrue(pool.z() >= j * CELL && pool.z() < (j + 1) * CELL, "z " + pool);
                    assertTrue(pool.reach() <= TerracePoolFeature.MAX_REACH, "reach " + pool.reach());
                    radii[pool.radius()]++;
                    for (int m = 0; m < k; m++) {
                        assertFalse(TerracePoolLayout.crowds(pool, plan.get(m)), "pools " + m + " and " + k + " of cell " + i + ", " + j);
                    }
                }
                pools += plan.size();
            }
        }
        // 6400 cells, about 2 in 3 with a cluster of 3-9 tries (some dropped).
        assertTrue(clusters > 3950 && clusters < 4400, "clusters " + clusters);
        double perChunk = pools / (6400.0 * CELL * CELL / 256.0);
        // About 0.26 pools per chunk of ~21 blocks of mud each: the mud area of 47d (0.5 tries of ~10.5 blocks).
        assertTrue(perChunk > 0.2 && perChunk < 0.35, "pools per chunk " + perChunk);
        for (int r = 1; r <= TerracePoolFeature.MAX_RADIUS; r++) {
            assertTrue(radii[r] > 0, "radius " + r + " occurs");
        }
        assertTrue(radii[1] + radii[2] > radii[5] + radii[6], "small pools most often");
    }

    @Test
    void anotherSeedAnotherPlan() {
        boolean differs = false;
        for (int i = 0; i < 10 && !differs; i++) {
            differs = !centres(cluster(1L, i, 0)).equals(centres(cluster(2L, i, 0)));
        }
        assertTrue(differs);
    }

    private static List<String> centres(List<TerracePoolLayout.Pool> pools) {
        return pools.stream().map(pool -> pool.x() + "," + pool.z() + "," + pool.radius()).toList();
    }
}
