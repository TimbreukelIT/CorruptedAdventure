package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mirrored doppelgangers of ring 5's inner band (93, V93-6/7 = A): a 48 × 48 square that stands mirrored 64–160
 * further along x or z, in quiet inner band; inside its blending edge of 8–12 the target is its source block for block
 * (ground, shapes, the own caves), at the edge it blends from its own ground. Deterministic per seed.
 */
class Ring5CopiesTest {
    private static final Ring5Field FIELD = Ring5FieldTest.field();
    private static final int S = Ring5Copies.SIZE;

    private static List<Ring5Copies.Pair> pairs(Ring5Field field) {
        List<Ring5Copies.Pair> pairs = new ArrayList<>();
        int cells = 9800 / Ring5Copies.CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Ring5Copies.Pair pair = field.copyPair(i, j);
                if (pair != null) {
                    pairs.add(pair);
                }
            }
        }
        return pairs;
    }

    @Test
    void sameSeedSamePairs() {
        assertEquals(pairs(FIELD), pairs(Ring5FieldTest.field()));
    }

    /** About one per 600 × 600 of the inner band; centre to centre 64–160 along x or z; the mirror maps blocks onto blocks. */
    @Test
    void pairsLieAlongAnAxisSixtyFourToOneHundredSixtyApart() {
        List<Ring5Copies.Pair> pairs = pairs(FIELD);
        System.out.printf("doppelgangers: %d%n", pairs.size());
        assertTrue(pairs.size() >= 30 && pairs.size() <= 120, "pairs " + pairs.size());
        for (Ring5Copies.Pair pair : pairs) {
            int along = pair.alongX() ? Math.abs(pair.tx() - pair.sx()) : Math.abs(pair.tz() - pair.sz());
            int across = pair.alongX() ? pair.tz() - pair.sz() : pair.tx() - pair.sx();
            assertEquals(0, across);
            assertTrue(along >= 64 && along <= 160, "apart " + along);
            assertTrue(pair.edge() >= Ring5Copies.MIN_EDGE && pair.edge() <= Ring5Copies.MAX_EDGE);
            // The mirror: the target's side facing the source shows the source's side facing the target.
            int nearT = pair.alongX() ? (pair.tx() > pair.sx() ? pair.tx() : pair.tx() + S - 1) : 0;
            if (pair.alongX()) {
                int nearS = pair.tx() > pair.sx() ? pair.sx() + S - 1 : pair.sx();
                assertEquals(nearS, pair.sourceX(nearT, pair.tz()));
            }
            for (int k = 0; k < S; k++) {
                int x = pair.tx() + k;
                int z = pair.tz() + (k * 7) % S;
                assertTrue(pair.inSource(pair.sourceX(x, z), pair.sourceZ(x, z)));
            }
        }
    }

    /**
     * Inside the blending edge the target is the source mirrored: the same ground, the same own caves and, away from the
     * edge by a layer's shift, the same terrain block for block; at the border it is (almost) its own ground.
     */
    @Test
    void theTargetIsItsSourceMirrored() {
        int checked = 0;
        for (Ring5Copies.Pair pair : pairs(FIELD).subList(0, 20)) {
            for (int x = pair.tx(); x < pair.tx() + S; x++) {
                for (int z = pair.tz(); z < pair.tz() + S; z++) {
                    int sx = pair.sourceX(x, z);
                    int sz = pair.sourceZ(x, z);
                    Ring5Field.Column target = FIELD.column(x, z);
                    double targetTop = target.top();
                    double targetHeight = target.height();
                    int targetCaves = target.caves;
                    int depth = pair.depth(x, z);
                    Ring5Field.Column source = FIELD.column(sx, sz);
                    if (!pair.core(x, z)) {
                        if (depth == 0) {
                            double own = FIELD.innerGround(x, z);
                            double copy = FIELD.innerGround(sx, sz);
                            assertTrue(Math.abs(targetTop - own) <= 0.1 * Math.abs(copy - own) + 1e-9, "edge at " + x + ", " + z);
                        }
                        continue;
                    }
                    assertEquals(source.top(), targetTop, 1e-9);
                    assertEquals(source.height(), targetHeight, 1e-9);
                    assertEquals(source.caves, targetCaves);
                    if (depth >= pair.edge() + Ring5Inner.MAX_SHIFT) {
                        for (int y = -64; y < 200; y += 5) {
                            assertEquals(FIELD.terrain(sx, y, sz), FIELD.terrain(x, y, z), 1e-12, x + ", " + y + ", " + z);
                        }
                        checked++;
                    }
                    // The dam sees the mirrored holes.
                    assertTrue(FIELD.innerOpeningDistance(x, z) <= FIELD.innerOpeningDistance(sx, sz) + 1e-9);
                }
            }
        }
        assertTrue(checked > 5000, "checked " + checked);
    }

    /** The squares lie in quiet inner band: no crack or spot hole within 16, no glitch cube on them. */
    @Test
    void squaresLieInQuietInnerBand() {
        for (Ring5Copies.Pair pair : pairs(FIELD)) {
            for (int[] corner : new int[][]{{pair.sx(), pair.sz()}, {pair.tx(), pair.tz()}}) {
                for (int x = corner[0]; x < corner[0] + S; x += 3) {
                    for (int z = corner[1]; z < corner[1] + S; z += 3) {
                        assertTrue(FIELD.innerCrackDistance(x, z) > 8.0, "crack near a doppelganger at " + x + ", " + z);
                        assertEquals(0, FIELD.column(x, z).spans, "cube on a doppelganger at " + x + ", " + z);
                    }
                }
            }
        }
    }
}
