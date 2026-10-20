package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Decor rollout group A: the landmark cells are pure per seed and cell, every chunk a landmark reaches finds it, and
 * the per-chunk parts of an arch or a spire group add up to the whole, whatever chunk asks first.
 */
class LandmarksTest {
    private static final Landmarks.Grid GRID = new Landmarks.Grid(160, 1.0F, 6, 20);
    private static final Object WORLD = new Object();

    @Test
    void aCellsLandmarkIsPureAndInsideItsCell() {
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                Landmarks.Candidate a = Landmarks.inCell(42L, 7L, GRID, cx, cz, candidate -> true);
                Landmarks.Candidate b = Landmarks.inCell(42L, 7L, GRID, cx, cz, candidate -> true);
                assertNotNull(a);
                assertEquals(a.x(), b.x());
                assertEquals(a.z(), b.z());
                assertEquals(a.random().nextLong(), b.random().nextLong(), "the shape's random is the cell's own");
                assertEquals(cx, Math.floorDiv(a.x() - GRID.margin(), GRID.cellSize()));
                assertEquals(cz, Math.floorDiv(a.z() - GRID.margin(), GRID.cellSize()));
                assertTrue(Math.floorMod(a.x(), GRID.cellSize()) >= GRID.margin());
                assertTrue(Math.floorMod(a.x(), GRID.cellSize()) < GRID.cellSize() - GRID.margin());
            }
        }
    }

    @Test
    void theFirstAcceptedCandidateWinsAndItsRandomIsFresh() {
        // Reject the first two tries: the third wins, and its random has not been drawn from by the predicate.
        int[] seen = new int[1];
        Predicate<Landmarks.Candidate> thirdOnly = candidate -> {
            candidate.random().nextLong();
            return ++seen[0] == 3;
        };
        Landmarks.Candidate third = Landmarks.inCell(9L, 1L, GRID, 2, -1, thirdOnly);
        assertNotNull(third);
        int[] count = new int[1];
        Landmarks.Candidate again = Landmarks.inCell(9L, 1L, GRID, 2, -1, candidate -> ++count[0] == 3);
        assertEquals(third.x(), again.x());
        assertEquals(third.random().nextLong(), again.random().nextLong());
        assertTrue(Landmarks.inCell(9L, 1L, GRID, 2, -1, candidate -> false) == null, "no accepted try, no landmark");
    }

    @Test
    void everyChunkInReachFindsTheSameLandmark() {
        Predicate<Landmarks.Candidate> half = candidate -> (candidate.x() + candidate.z()) % 3 != 0;
        int reach = 20;
        Set<Long> anchors = new HashSet<>();
        for (int cx = -30; cx <= 30; cx++) {
            for (int cz = -30; cz <= 30; cz++) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                for (Landmarks.Candidate found : Landmarks.reaching(WORLD, 5L, 3L, GRID, chunk, reach, half)) {
                    assertTrue(found.x() >= chunk.getMinBlockX() - reach && found.x() <= chunk.getMaxBlockX() + reach);
                    assertTrue(half.test(found));
                    anchors.add(BlockPos.asLong(found.x(), 0, found.z()));
                }
            }
        }
        // Each anchor found is the one of its cell, and every chunk around it found it.
        for (long anchor : anchors) {
            int x = BlockPos.getX(anchor);
            int z = BlockPos.getZ(anchor);
            Landmarks.Candidate own = Landmarks.inCell(5L, 3L, GRID, Math.floorDiv(x, GRID.cellSize()), Math.floorDiv(z, GRID.cellSize()), half);
            assertNotNull(own);
            assertEquals(x, own.x());
            for (int cx = (x - reach) >> 4; cx <= (x + reach) >> 4; cx++) {
                for (int cz = (z - reach) >> 4; cz <= (z + reach) >> 4; cz++) {
                    assertTrue(Landmarks.reaching(WORLD, 5L, 3L, GRID, new ChunkPos(cx, cz), reach, half).stream()
                            .anyMatch(found -> found.x() == x && found.z() == z));
                }
            }
        }
        assertFalse(anchors.isEmpty());
    }

    @Test
    void theChunkPartsOfAnArchAddUpToTheWhole() {
        RootArch arch = new RootArch(-7.3, 62.0, 4.1, 13.6, 63.0, 19.8, 71.0, 2.2, 1.2);
        Set<BlockPos> whole = new LinkedHashSet<>();
        Set<BlockPos> parts = new HashSet<>();
        for (int cx = -2; cx <= 1; cx++) {
            for (int cz = -1; cz <= 2; cz++) {
                Set<BlockPos> part = new LinkedHashSet<>();
                arch.collect(part, new ChunkPos(cx, cz));
                for (BlockPos pos : part) {
                    assertEquals(cx, pos.getX() >> 4);
                    assertEquals(cz, pos.getZ() >> 4);
                    assertTrue(parts.add(pos), "no block in two chunks");
                }
            }
        }
        // The whole, collected chunk by chunk in another order, is the same set.
        for (int cz = 2; cz >= -1; cz--) {
            for (int cx = 1; cx >= -2; cx--) {
                arch.collect(whole, new ChunkPos(cx, cz));
            }
        }
        assertEquals(parts, new HashSet<>(whole));
        assertTrue(parts.stream().anyMatch(pos -> pos.getY() >= 70), "the crown stands high");
    }

    @Test
    void aSpireNarrowsToItsTip() {
        CrystalSpireFeature.Spire spire = new CrystalSpireFeature.Spire(0.5, 0.5, 0.0, 0.0, 50, 80, 3.0, 0.3);
        assertTrue(spire.contains(0, 50, 0));
        assertTrue(spire.contains(2, 55, 0));
        assertFalse(spire.contains(5, 55, 0));
        assertFalse(spire.contains(0, 81, 0));
        int below = 0;
        int above = 0;
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                below += spire.contains(x, 60, z) ? 1 : 0;
                above += spire.contains(x, 76, z) ? 1 : 0;
            }
        }
        assertTrue(above < below, "narrower near the tip");
        assertTrue(List.of(spire).stream().allMatch(s -> s.contains(0, 79, 0) || s.contains(0, 80, 0)), "a tip at the top");
    }
}
