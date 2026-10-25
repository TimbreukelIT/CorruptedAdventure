package nl.timtoys.corruptedadventure.worldgen.feature;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The ridge rules of {@link SaltRidgeFeature}: breaks, junctions, neighbours only inside the chunk and at one height. */
class SaltRidgeFeatureTest {
    @Test
    void aBreakWinsAndOnlyJunctionsGrowTwoHigh() {
        assertEquals(0, SaltRidgeFeature.height(4, 0.05F, 0.0F, 0.08F, 0.35F));
        assertEquals(1, SaltRidgeFeature.height(2, 0.5F, 0.0F, 0.08F, 0.35F), "a plain crack");
        assertEquals(2, SaltRidgeFeature.height(3, 0.5F, 0.2F, 0.08F, 0.35F), "three cracks meet");
        assertEquals(1, SaltRidgeFeature.height(4, 0.5F, 0.5F, 0.08F, 0.35F), "a junction that stays low");
    }

    @Test
    void neighboursCountCracksAtTheSameHeightInsideTheChunk() {
        boolean[] edge = new boolean[256];
        int[] top = new int[256];
        java.util.Arrays.fill(top, 65);
        // A cross at (5, 5) and a crack running along the chunk edge at x = 0.
        for (int[] c : new int[][]{{5, 5}, {4, 5}, {6, 5}, {5, 4}, {5, 6}, {0, 3}, {0, 4}, {0, 5}}) {
            edge[c[1] * 16 + c[0]] = true;
        }
        assertEquals(4, SaltRidgeFeature.neighbours(edge, top, 5, 5));
        assertEquals(1, SaltRidgeFeature.neighbours(edge, top, 4, 5));
        assertEquals(2, SaltRidgeFeature.neighbours(edge, top, 0, 4), "nothing is read beyond the chunk edge");
        top[5 * 16 + 6] = 66;
        assertEquals(3, SaltRidgeFeature.neighbours(edge, top, 5, 5), "a crack one higher does not join");
    }
}
