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
        for (String part : new String[]{"plateau_rocks", "plateau_fins", "canyon_spires"}) {
            assertTrue(rocks.contains("\"corruptedadventure:ring_2/overworld/" + part + "\""), part);
        }
        assertEquals("corruptedadventure:ring_2/overworld/terrain", ring2.get("terrain").getAsString());
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
