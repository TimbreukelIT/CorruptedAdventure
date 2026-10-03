package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The ring 2 overworld stack is vanilla's final density (as in ring 1) with only the terrain swapped; the ring 2 input
 * wraps it in {@code canyon_details} with the rock shapes.
 */
class Ring2TerrainJsonTest {
    private static final String DENSITY_FUNCTIONS = "src/main/resources/data/corruptedadventure/worldgen/density_function";

    @Test
    void ring2StackIsTheRing1FormulaWithItsOwnTerrain() throws IOException {
        Path root = findRoot();
        String ring1 = Files.readString(root.resolve("ring_1/overworld.json"));
        String stack = Files.readString(root.resolve("ring_2/overworld/stack.json"));
        assertEquals(3, stack.split("corruptedadventure:ring_2/overworld/terrain", -1).length - 1);
        assertEquals(ring1, stack.replace("corruptedadventure:ring_2/overworld/terrain", "corruptedadventure:ring_1/overworld/sloped_cheese"));
    }

    @Test
    void ring2InputIsTheStackWithItsFineForms() throws IOException {
        JsonObject ring2 = JsonParser.parseString(Files.readString(findRoot().resolve("ring_2/overworld.json"))).getAsJsonObject();
        assertEquals("corruptedadventure:canyon_details", ring2.get("type").getAsString());
        assertEquals("corruptedadventure:ring_2/overworld/stack", ring2.get("argument").getAsString());
        String rocks = ring2.get("rocks").toString();
        for (String part : new String[]{"plateau_rocks", "plateau_fins", "canyon_spires", "hoodoos", "hoodoos_offset", "hoodoo_arches"}) {
            assertTrue(rocks.contains("\"corruptedadventure:ring_2/overworld/" + part + "\""), part);
        }
        assertEquals("corruptedadventure:ring_2/overworld/terrain", ring2.get("terrain").getAsString());
    }

    @Test
    void saltIslandsRaiseOnlyTheSaltFlats() throws IOException {
        // flat_height = oasis · (1 − salt − dune) + (66 + islands) · salt + dune · dune_height (change 30b).
        JsonObject flat = JsonParser.parseString(Files.readString(findRoot().resolve("ring_2/overworld/flat_height.json"))).getAsJsonObject();
        JsonObject salt = flat.getAsJsonObject("argument1").getAsJsonObject("argument2");
        assertEquals("minecraft:mul", salt.get("type").getAsString());
        assertEquals("corruptedadventure:ring_2/overworld/salt_weight", salt.get("argument2").getAsString());
        JsonObject island = salt.getAsJsonObject("argument1");
        assertEquals(66.0, island.get("argument1").getAsDouble());
        JsonObject spline = island.getAsJsonObject("argument2").getAsJsonObject("spline");
        assertEquals("corruptedadventure:ring_2_salt_islands", spline.getAsJsonObject("coordinate").get("noise").getAsString());
        assertEquals(1.0, spline.getAsJsonObject("coordinate").get("xz_scale").getAsDouble(), "same scale as noise_threshold");
        var points = spline.getAsJsonArray("points");
        assertEquals(0.0, points.get(0).getAsJsonObject().get("value").getAsDouble());
        assertEquals(2.0, points.get(points.size() - 1).getAsJsonObject().get("value").getAsDouble());
        assertEquals(1, flat.toString().split("ring_2_salt_islands", -1).length - 1, "the islands once, only in the salt term");
    }

    private static Path findRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(DENSITY_FUNCTIONS);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return fail("Could not find " + DENSITY_FUNCTIONS);
    }
}
