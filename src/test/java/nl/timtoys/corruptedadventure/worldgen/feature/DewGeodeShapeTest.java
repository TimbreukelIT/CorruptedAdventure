package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DewGeodeShapeTest {
    private static final DewGeodeShape.Settings SETTINGS = NetherRing2Features.GEODE_SETTINGS;
    private static final double THRESHOLD = SETTINGS.regionThreshold();

    /** A stand-in for the seeded cell hash: any pure mix of (x, z, salt). */
    private static DewGeodeShape.CellHash hash(long seed) {
        return (x, z, salt) -> {
            long h = seed * 0x9E3779B97F4A7C15L ^ x * 0xBF58476D1CE4E5B9L ^ z * 0x94D049BB133111EBL ^ salt * 0xD6E8FEB86659FD93L;
            h ^= h >>> 31;
            h *= 0x9E3779B97F4A7C15L;
            h ^= h >>> 29;
            return h;
        };
    }

    /** Forest on the east side of x = 0, caves on the west. */
    private static final DewGeodeShape.RegionAt REGION = (x, z) -> x >= 0 ? THRESHOLD + 0.2 : THRESHOLD - 0.2;

    @Test
    void aCellHasAttemptsAtOtherPlacesAndTheSameDrawForTheChance() {
        int cellsWithCandidates = 0;
        for (int cx = 0; cx < 40; cx++) {
            for (int cz = 0; cz < 40; cz++) {
                List<DewGeodeShape.Geode> all = DewGeodeShape.candidatesOfCell(SETTINGS, cx * SETTINGS.cellSize() + 5, cz * SETTINGS.cellSize() + 5, hash(2), REGION);
                // The chance is one draw per cell and regime: all attempts are there, or none.
                assertTrue(all.isEmpty() || all.size() == DewGeodeShape.ATTEMPTS, "attempts " + all.size());
                if (!all.isEmpty()) {
                    cellsWithCandidates++;
                    assertTrue(all.stream().map(g -> g.x() * 100000 + g.z()).distinct().count() > DewGeodeShape.ATTEMPTS / 2, "the attempts spread over the cell");
                }
            }
        }
        assertTrue(cellsWithCandidates > 700, "cells " + cellsWithCandidates);
    }

    @Test
    void sameSeedSameCandidate() {
        for (int cell = -20; cell < 20; cell++) {
            assertEquals(DewGeodeShape.candidate(SETTINGS, hash(5), cell, cell * 3, 3, REGION), DewGeodeShape.candidate(SETTINGS, hash(5), cell, cell * 3, 3, REGION));
        }
    }

    @Test
    void wholeGeodeWithLiningStaysInsideItsCell() {
        int found = 0;
        for (int cx = -30; cx < 30; cx++) {
            for (int cz = -30; cz < 30; cz++) {
                DewGeodeShape.Geode geode = DewGeodeShape.candidate(SETTINGS, hash(11), cx, cz, 0, REGION);
                if (geode == null) {
                    continue;
                }
                found++;
                int size = SETTINGS.cellSize();
                int reach = geode.reach();
                assertTrue(geode.x() - reach >= cx * size && geode.x() + reach < (cx + 1) * size, "x " + geode);
                assertTrue(geode.z() - reach >= cz * size && geode.z() + reach < (cz + 1) * size, "z " + geode);
                // So the lining lies within the 3 x 3 chunks around the chunk of the centre.
                assertTrue(reach <= 16 + 8, "reach " + reach);
                assertEquals(geode.x() >= 0, geode.forest());
                assertTrue(geode.y() - geode.radius() * 1.15 > SETTINGS.bottom(), "never into the lava sea " + geode);
            }
        }
        assertTrue(found > 1500, "found " + found);
    }

    @Test
    void radiiAndHeightsFollowTheirRegime() {
        for (int cx = -30; cx < 30; cx++) {
            for (int cz = -30; cz < 30; cz++) {
                DewGeodeShape.Geode geode = DewGeodeShape.candidate(SETTINGS, hash(3), cx, cz, 0, REGION);
                if (geode == null) {
                    continue;
                }
                if (geode.forest()) {
                    assertTrue(geode.radius() >= SETTINGS.radiusMinForest() && geode.radius() <= SETTINGS.radiusMaxForest());
                    assertTrue(geode.y() >= SETTINGS.yMinForest() && geode.y() <= SETTINGS.yMaxForest());
                } else {
                    assertTrue(geode.radius() >= SETTINGS.radiusMinCaves() && geode.radius() <= SETTINGS.radiusMaxCaves());
                    assertTrue(geode.y() >= SETTINGS.yMinCaves() && geode.y() <= SETTINGS.yMaxCaves());
                }
            }
        }
    }

    @Test
    void candidateShareFollowsTheChance() {
        int forest = 0;
        int caves = 0;
        int cells = 0;
        for (int cx = 0; cx < 100; cx++) {
            for (int cz = 0; cz < 100; cz++) {
                cells++;
                if (DewGeodeShape.candidate(SETTINGS, hash(9), cx, cz, 0, REGION) != null) {
                    forest++;
                }
                if (DewGeodeShape.candidate(SETTINGS, hash(9), -1 - cx, cz, 0, REGION) != null) {
                    caves++;
                }
            }
        }
        assertEquals(SETTINGS.chanceForest(), (double) forest / cells, 0.03);
        assertEquals(SETTINGS.chanceCaves(), (double) caves / cells, 0.03);
    }

    @Test
    void twentySixWallTestPointsLieOutsideTheCavity() {
        DewGeodeShape.Geode geode = new DewGeodeShape.Geode(100, 60, 100, 6, 1.1, 0.9, 1.0, true);
        List<int[]> points = geode.wallTestPoints(SETTINGS.shell(), 0);
        assertEquals(26, points.size());
        for (int[] point : points) {
            assertTrue(!geode.inCavity(point[0], point[1], point[2]), "inside the cavity: " + point[0] + "," + point[1] + "," + point[2]);
        }
    }

    @Test
    void settingsRejectBadValues() {
        var json = JsonParser.parseString("{\"cell_size\":32,\"region_threshold\":-0.1,\"chance_forest\":0.6,\"chance_caves\":0.9,"
                + "\"radius_min_forest\":8,\"radius_max_forest\":7,\"radius_min_caves\":4,\"radius_max_caves\":5,\"y_min_forest\":30,"
                + "\"y_max_forest\":100,\"y_min_caves\":26,\"y_max_caves\":62,\"bottom\":23,\"shell\":3}");
        assertTrue(DewGeodeShape.Settings.CODEC.parse(JsonOps.INSTANCE, json).isError(), "radius min above max");
        var good = JsonParser.parseString(json.toString().replace("\"radius_min_forest\":8", "\"radius_min_forest\":4"));
        assertNotNull(DewGeodeShape.Settings.CODEC.parse(JsonOps.INSTANCE, good).getOrThrow());
        var tiny = JsonParser.parseString(good.toString().replace("\"cell_size\":32", "\"cell_size\":16"));
        assertTrue(DewGeodeShape.Settings.CODEC.parse(JsonOps.INSTANCE, tiny).isError(), "cell too small for the radii");
        var chance = JsonParser.parseString(good.toString().replace("\"chance_forest\":0.6", "\"chance_forest\":1.5"));
        assertTrue(DewGeodeShape.Settings.CODEC.parse(JsonOps.INSTANCE, chance).isError());
    }
}
