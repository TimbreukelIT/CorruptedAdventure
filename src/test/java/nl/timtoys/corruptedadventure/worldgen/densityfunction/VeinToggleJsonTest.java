package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Ore veins only in ring 1 (change 25): the router's vein toggle is vanilla's in ring 1 and 0 in every other ring. */
class VeinToggleJsonTest {
    private static final String WORLDGEN = "src/main/resources/data/corruptedadventure/worldgen";

    @Test
    void routerTogglesVeinsPerRing() throws IOException {
        Path root = findRoot();
        JsonObject settings = read(root.resolve("noise_settings/overworld.json"));
        assertEquals(true, settings.get("ore_veins_enabled").getAsBoolean());
        assertEquals("corruptedadventure:ring_blend/overworld_vein_toggle",
                settings.getAsJsonObject("noise_router").get("vein_toggle").getAsString());

        JsonObject blend = read(root.resolve("density_function/ring_blend/overworld_vein_toggle.json"));
        assertEquals("corruptedadventure:ring_blend", blend.get("type").getAsString());
        assertEquals("minecraft:overworld", blend.get("dimension").getAsString());
        JsonObject rings = blend.getAsJsonObject("rings");
        assertEquals("corruptedadventure:ring_1/overworld/vein_toggle", rings.get("ring_1").getAsString());
        for (String ring : new String[]{"ring_2", "ring_3", "ring_4", "ring_5", "void"}) {
            assertEquals(0.0, rings.get(ring).getAsDouble(), ring);
        }
    }

    @Test
    void ring1ToggleIsVanillas() throws IOException {
        // Vanilla 1.21.1's inline vein_toggle of noise_settings/overworld.
        JsonObject vanilla = JsonParser.parseString("""
                {"type": "minecraft:interpolated", "argument": {"type": "minecraft:range_choice", "input": "minecraft:y",
                 "max_exclusive": 51.0, "min_inclusive": -60.0,
                 "when_in_range": {"type": "minecraft:noise", "noise": "minecraft:ore_veininess", "xz_scale": 1.5, "y_scale": 1.5},
                 "when_out_of_range": 0.0}}""").getAsJsonObject();
        assertEquals(vanilla, read(findRoot().resolve("density_function/ring_1/overworld/vein_toggle.json")));
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static Path findRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(WORLDGEN);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return fail("Could not find " + WORLDGEN);
    }
}
