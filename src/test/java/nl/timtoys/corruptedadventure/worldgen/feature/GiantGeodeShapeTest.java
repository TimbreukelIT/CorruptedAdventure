package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The geometry of the giant geodes of Nether ring 4 (65) with the settings of {@code ring_4/nether/geodes.json}. */
class GiantGeodeShapeTest {
    private static final GiantGeodeShape.Settings SETTINGS = settings();
    private static final GiantGeodeShape.CellHash HASH = (x, z, salt) -> x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 31L;

    private static GiantGeodeShape.Settings settings() {
        JsonObject json = WorldgenData.read("density_function/ring_4/nether/geodes.json").getAsJsonObject("settings");
        return GiantGeodeShape.Settings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    @Test
    void settingsAreTheDecidedSizes() {
        assertEquals(10, SETTINGS.radiusMin(), "diameter 20");
        assertEquals(20, SETTINGS.radiusMax(), "diameter 40");
        assertEquals(3, SETTINGS.doorsMax(), "1-3 entrances");
        assertTrue(SETTINGS.floorMin() >= 40, "the floor lies on the mass (Y 40), above the dam");
    }

    /** Every candidate, with the longest doorways it may get, stays inside its cell and below {@link GiantGeodeShape.Settings#highest()}. */
    @Test
    void everyCandidateStaysInItsCell() {
        for (int cellX = -6; cellX <= 6; cellX++) {
            for (int cellZ = -6; cellZ <= 6; cellZ++) {
                for (int attempt = 0; attempt < GiantGeodeShape.ATTEMPTS; attempt++) {
                    GiantGeodeShape.Geode bare = GiantGeodeShape.candidate(SETTINGS, HASH, cellX, cellZ, attempt);
                    // nothing is open: no doorway; everything beyond the wall is open: the shortest ones
                    assertTrue(GiantGeodeShape.doors(SETTINGS, bare, 1L, (x, y, z) -> false).isEmpty());
                    long hash = GiantGeodeShape.attemptHash(HASH, cellX, cellZ, attempt);
                    List<GiantGeodeShape.Door> farthest = GiantGeodeShape.doors(SETTINGS, bare, hash,
                            (x, y, z) -> Math.hypot(x - bare.x(), z - bare.z()) >= bare.wall() + SETTINGS.doorLength() - 2);
                    assertEquals(SETTINGS.doorsMax(), farthest.size(), "three doorways round the geode");
                    GiantGeodeShape.Geode geode = bare.withDoors(farthest);
                    int size = SETTINGS.cellSize();
                    assertTrue(geode.x() - geode.reach() >= cellX * size && geode.x() + geode.reach() < (cellX + 1) * size, "x in cell");
                    assertTrue(geode.z() - geode.reach() >= cellZ * size && geode.z() + geode.reach() < (cellZ + 1) * size, "z in cell");
                    int top = geode.y() + (int) Math.ceil(geode.radius() * geode.stretchY());
                    assertTrue(top <= SETTINGS.highest(), "below highest");
                    assertTrue(geode.floor() >= SETTINGS.floorMin() && geode.floor() <= SETTINGS.floorMax());
                    assertEquals(bare, GiantGeodeShape.candidate(SETTINGS, HASH, cellX, cellZ, attempt), "pure");
                    for (int i = 0; i < farthest.size(); i++) {
                        for (int j = i + 1; j < farthest.size(); j++) {
                            double cos = farthest.get(i).dirX() * farthest.get(j).dirX() + farthest.get(i).dirZ() * farthest.get(j).dirZ();
                            assertTrue(cos <= 0.5 + 1e-9, "doorways at least 60 degrees apart");
                        }
                    }
                }
            }
        }
    }

    @Test
    void hollowAboveAFlatFloorWithADoorwayThatEndsInTheOpening() {
        GiantGeodeShape.Geode bare = new GiantGeodeShape.Geode(0, 50, 0, 12, 1.0, 1.0, 1.0, 46, List.of(), 6, 6);
        // open terrain only east of x = 28 along the floor: one doorway, east, ending 2 blocks into the opening
        List<GiantGeodeShape.Door> doors = GiantGeodeShape.doors(SETTINGS, bare, 7L, (x, y, z) -> x >= 28 && Math.abs(z) <= 12);
        assertEquals(1, doors.size());
        GiantGeodeShape.Geode geode = bare.withDoors(doors);
        GiantGeodeShape.Door door = doors.get(0);
        assertTrue(door.dirX() > 0.9, "east");
        int end = (int) Math.floor(door.end() * door.dirX());
        assertTrue(end >= 28 && end <= 31, "ends just inside the opening: " + end);
        assertTrue(geode.hollow(0, 46, 0), "on the floor");
        assertFalse(geode.hollow(0, 45, 0), "below the floor");
        assertTrue(geode.hollow(0, 61, 0), "under the dome");
        assertFalse(geode.hollow(0, 63, 0), "above the dome");
        assertFalse(geode.hollow(-20, 47, 0), "behind the wall, away from the door");
        int mid = (int) Math.round(20 * door.dirX());
        int midZ = (int) Math.round(20 * door.dirZ());
        assertTrue(geode.hollow(mid, 47, midZ), "in the doorway past the wall");
        assertFalse(geode.hollow(mid, 53, midZ), "above the doorway's arch");
        assertFalse(geode.hollow(mid + (int) Math.round(-door.dirZ() * 5), 47, midZ + (int) Math.round(door.dirX() * 5)), "beside the doorway");
        assertEquals(9, geode.domeTestPoints(4).size());
        for (int[] point : geode.domeTestPoints(4)) {
            assertTrue(point[1] > geode.y(), "dome points lie above the centre");
        }
    }
}
