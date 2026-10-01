package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** The ring 2 overworld input is vanilla's final density (as in ring 1) with only the terrain swapped. */
class Ring2TerrainJsonTest {
    private static final String DENSITY_FUNCTIONS = "src/main/resources/data/corruptedadventure/worldgen/density_function";

    @Test
    void ring2IsTheRing1FormulaWithItsOwnTerrain() throws IOException {
        Path root = findRoot();
        String ring1 = Files.readString(root.resolve("ring_1/overworld.json"));
        String ring2 = Files.readString(root.resolve("ring_2/overworld.json"));
        assertEquals(3, ring2.split("corruptedadventure:ring_2/overworld/terrain", -1).length - 1);
        assertEquals(ring1, ring2.replace("corruptedadventure:ring_2/overworld/terrain", "corruptedadventure:ring_1/overworld/sloped_cheese"));
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
