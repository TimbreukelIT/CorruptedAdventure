package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The corrupt spots of ring 5's inner band (83b): about one per 200–300 blocks, 2–5 touching chunks, each shifted chunk
 * lifted or sunk 8–30 as a whole (hard walls on the chunk borders), one chunk fallen into the void inside a ragged rim,
 * dammed like the cracks; all of it well inside the inner band. Deterministic per seed.
 */
class Ring5CorruptSpotsTest {
    private static final RingLayout LAYOUT = Ring5FieldTest.LAYOUT;
    private static final Ring5Field FIELD = Ring5FieldTest.field();
    private static final Ring5Inner INNER = FIELD.inner();
    private static final int C = Ring5CorruptSpots.CHUNK;

    /** Every kept spot of the inner band (its cells on a box round the ring). */
    private static List<Ring5CorruptSpots.Spot> spots(Ring5Field field) {
        List<Ring5CorruptSpots.Spot> spots = new ArrayList<>();
        int cells = Math.floorDiv(9000, C * Ring5CorruptSpots.CELL_CHUNKS) + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Ring5CorruptSpots.Spot spot = field.corruptSpot(i, j);
                if (spot != null) {
                    spots.add(spot);
                }
            }
        }
        return spots;
    }

    private static double[] centre(Ring5CorruptSpots.Spot spot) {
        double x = 0.0;
        double z = 0.0;
        for (int k = 0; k < spot.size(); k++) {
            x += (spot.chunkX()[k] + 0.5) * C;
            z += (spot.chunkZ()[k] + 0.5) * C;
        }
        return new double[]{x / spot.size(), z / spot.size()};
    }

    /** The same seed gives the same spots in a fresh field; the shapes come from the world salt only. */
    @Test
    void sameSeedSameSpots() {
        List<Ring5CorruptSpots.Spot> a = spots(FIELD);
        List<Ring5CorruptSpots.Spot> b = spots(Ring5FieldTest.field());
        assertEquals(a.size(), b.size());
        for (int k = 0; k < a.size(); k++) {
            assertArrayEquals(a.get(k).chunkX(), b.get(k).chunkX());
            assertArrayEquals(a.get(k).chunkZ(), b.get(k).chunkZ());
            assertArrayEquals(a.get(k).shift(), b.get(k).shift());
            assertEquals(a.get(k).fallen(), b.get(k).fallen());
        }
    }

    /** Players meet them regularly: hundreds round the ring, the nearest neighbour mostly 160–320 away. */
    @Test
    void spotsLieAbout200To300Apart() {
        List<Ring5CorruptSpots.Spot> spots = spots(FIELD);
        double[] nearest = new double[spots.size()];
        for (int a = 0; a < spots.size(); a++) {
            double[] p = centre(spots.get(a));
            double best = Double.POSITIVE_INFINITY;
            for (int b = 0; b < spots.size(); b++) {
                if (a != b) {
                    double[] q = centre(spots.get(b));
                    best = Math.min(best, Math.hypot(p[0] - q[0], p[1] - q[1]));
                }
            }
            nearest[a] = best;
        }
        for (Ring5CorruptSpots.Spot spot : spots) {
            double[] p = centre(spot);
            if (Math.abs(p[1]) < 400.0 && p[0] > 0.0 || Math.abs(p[0]) < 400.0 && p[1] < 0.0) {
                System.out.printf("spot near an axis: chunks x %s z %s shift %s fallen %d%n", Arrays.toString(spot.chunkX()),
                        Arrays.toString(spot.chunkZ()), Arrays.toString(spot.shift()), spot.fallen());
            }
        }
        Arrays.sort(nearest);
        double median = nearest[nearest.length / 2];
        System.out.printf("corrupt spots: %d, nearest neighbour p10 %.0f, median %.0f, p90 %.0f%n", spots.size(),
                nearest[nearest.length / 10], median, nearest[9 * nearest.length / 10]);
        assertTrue(spots.size() > 300, "spots " + spots.size());
        assertTrue(median >= 160.0 && median <= 320.0, "median " + median);
        assertTrue(nearest[0] >= 64.0, "closest " + nearest[0]);
    }

    /**
     * Each spot: 2–5 touching chunks, exactly one fallen (shift 0), the others shifted 8–30 either way; both signs occur
     * over the ring.
     */
    @Test
    void spotsAreTwoToFiveTouchingChunksWithOneFallen() {
        int up = 0;
        int down = 0;
        for (Ring5CorruptSpots.Spot spot : spots(FIELD)) {
            assertTrue(spot.size() >= Ring5CorruptSpots.MIN_CHUNKS && spot.size() <= Ring5CorruptSpots.MAX_CHUNKS, "size " + spot.size());
            assertTrue(spot.fallen() >= 0 && spot.fallen() < spot.size());
            for (int k = 0; k < spot.size(); k++) {
                boolean touches = spot.size() == 1;
                for (int m = 0; m < spot.size(); m++) {
                    int d = Math.abs(spot.chunkX()[k] - spot.chunkX()[m]) + Math.abs(spot.chunkZ()[k] - spot.chunkZ()[m]);
                    touches |= d == 1;
                    if (m != k) {
                        assertTrue(d > 0, "chunk twice");
                    }
                }
                assertTrue(touches, "loose chunk in a spot");
                int shift = spot.shift()[k];
                if (k == spot.fallen()) {
                    assertEquals(0, shift);
                } else {
                    assertTrue(Math.abs(shift) >= Ring5CorruptSpots.MIN_SHIFT && Math.abs(shift) <= Ring5CorruptSpots.MAX_SHIFT, "shift " + shift);
                    if (shift > 0) {
                        up++;
                    } else {
                        down++;
                    }
                }
            }
        }
        assertTrue(up > 100 && down > 100, "up " + up + ", down " + down);
    }

    /** A shifted chunk moves as a whole: every column of it lies exactly its shift above or below the unshifted ground. */
    @Test
    void shiftedChunksMoveAsAWholeWithHardWalls() {
        int chunks = 0;
        int walls = 0;
        for (Ring5CorruptSpots.Spot spot : spots(FIELD).subList(0, 60)) {
            for (int k = 0; k < spot.size(); k++) {
                if (k == spot.fallen()) {
                    continue;
                }
                chunks++;
                int x0 = spot.chunkX()[k] * C;
                int z0 = spot.chunkZ()[k] * C;
                for (int x = x0; x < x0 + C; x++) {
                    for (int z = z0; z < z0 + C; z++) {
                        Ring5Field.Column column = FIELD.column(x, z);
                        if (column.isVoid()) {
                            continue; // a loose crack through the chunk
                        }
                        assertEquals(INNER.top(x, z) + spot.shift()[k], column.top(), 1e-9, "column " + x + ", " + z);
                    }
                }
                // The wall on the chunk's west border: the step there is the shift (give or take the ground's own slope).
                Ring5Field.Column in = FIELD.column(x0, z0 + 8);
                Ring5Field.Column out = FIELD.column(x0 - 1, z0 + 8);
                if (!in.isVoid() && !out.isVoid() && spot.chunkAt(x0 - 1, z0 + 8) < 0
                        && Math.abs(in.top() - out.top()) >= Ring5CorruptSpots.MIN_SHIFT - 3) {
                    walls++;
                }
            }
        }
        assertTrue(chunks > 60 && walls > chunks / 4, "chunks " + chunks + ", walls " + walls);
    }

    /**
     * The fallen chunk: void past the world's bottom inside a ragged rim of 1–3 solid columns; {@code innerCrackDistance} is
     * at most 0 exactly on its void columns and at most the dam's 6 within 6 of them, so the lava layer's dam holds.
     */
    @Test
    void fallenChunksOpenIntoTheVoidAndAreDammed() {
        int holes = 0;
        for (Ring5CorruptSpots.Spot spot : spots(FIELD).subList(0, 80)) {
            int x0 = spot.chunkX()[spot.fallen()] * C;
            int z0 = spot.chunkZ()[spot.fallen()] * C;
            int open = 0;
            List<int[]> voids = new ArrayList<>();
            for (int x = x0; x < x0 + C; x++) {
                for (int z = z0; z < z0 + C; z++) {
                    int inside = Math.min(Math.min(x - x0, x0 + C - 1 - x), Math.min(z - z0, z0 + C - 1 - z));
                    boolean hole = FIELD.corruptSpots().holeDistance(spot, x, z) <= 0.0;
                    assertEquals(inside >= FIELD.corruptSpots().rag(x, z), hole);
                    if (inside < Ring5CorruptSpots.MIN_RAG) {
                        assertTrue(!hole, "rim open at " + x + ", " + z);
                    }
                    if (inside >= Ring5CorruptSpots.MAX_RAG) {
                        assertTrue(hole, "core closed at " + x + ", " + z);
                    }
                    if (hole) {
                        open++;
                        voids.add(new int[]{x, z});
                        Ring5Field.Column column = FIELD.column(x, z);
                        assertTrue(column.isVoid() && column.spans == 0, "hole column " + x + ", " + z);
                        assertEquals(1.0, column.scars());
                        for (int y = -64; y < 320; y += 3) {
                            assertTrue(FIELD.terrain(x, y, z) < 0.0, "solid in a hole at " + x + ", " + y + ", " + z);
                        }
                    }
                }
            }
            assertTrue(open >= (C - 2 * Ring5CorruptSpots.MAX_RAG) * (C - 2 * Ring5CorruptSpots.MAX_RAG), "open " + open);
            holes++;
            // The dam: every column within 6 of an open one is at most 6 from an opening.
            for (int x = x0 - 7; x < x0 + C + 7; x++) {
                for (int z = z0 - 7; z < z0 + C + 7; z++) {
                    double crack = FIELD.innerCrackDistance(x, z);
                    double nearest = Double.POSITIVE_INFINITY;
                    for (int[] v : voids) {
                        nearest = Math.min(nearest, Math.hypot(x - v[0], z - v[1]));
                    }
                    if (nearest <= 6.0) {
                        assertTrue(crack <= 6.0, "dam at " + x + ", " + z + ": " + crack);
                    }
                    if (crack <= 0.0) {
                        assertTrue(FIELD.column(x, z).isVoid(), "open distance on solid ground at " + x + ", " + z);
                    }
                }
            }
        }
        assertTrue(holes >= 80);
    }

    /** Spots lie wholly inside the inner band: off the band line and the zone 4→5 by 32, off every castle's reach by 16. */
    @Test
    void spotsKeepOffTheBandLineTheZoneAndTheCastles() {
        Ring5Band band = Ring5Band.of(LAYOUT);
        double zoneEnd = Ring4Sundered.zoneEnd(LAYOUT);
        for (Ring5CorruptSpots.Spot spot : spots(FIELD)) {
            for (int k = 0; k < spot.size(); k++) {
                for (int dx = 0; dx <= C; dx += C / 2) {
                    for (int dz = 0; dz <= C; dz += C / 2) {
                        double x = spot.chunkX()[k] * C + dx;
                        double z = spot.chunkZ()[k] * C + dz;
                        assertTrue(band.signedDistance(x, z) < -Ring5Field.SPOT_CLEARANCE + 8.0, "band line at " + x + ", " + z);
                        assertTrue(LAYOUT.effectiveDistance(x, z) > zoneEnd + Ring5Field.SPOT_CLEARANCE - 8.0, "zone at " + x + ", " + z);
                        for (Ring5Field.CastleSite site : FIELD.castleSites()) {
                            assertTrue(site.distance(x, z) > site.radius() + site.belt(), "castle at " + x + ", " + z);
                        }
                    }
                }
            }
        }
    }
}
