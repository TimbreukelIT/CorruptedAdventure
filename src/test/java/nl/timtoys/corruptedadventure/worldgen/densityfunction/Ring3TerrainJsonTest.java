package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The wiring of the ring 3 overworld terrain in the hand-written JSON. */
class Ring3TerrainJsonTest {
    private static final String DF = "density_function/";
    private static final String FIELD = "corruptedadventure:ring_3/overworld/field";

    @Test
    void contractOutputsReadTheOneField() {
        for (String name : List.of("caldera_mask", "caldera_dormant", "lava_channel", "island_mask", "roughness")) {
            JsonObject json = WorldgenData.read(DF + "ring_3/overworld/" + name + ".json");
            assertEquals("minecraft:flat_cache", json.get("type").getAsString(), name);
            JsonObject output = json.getAsJsonObject("argument");
            assertEquals("corruptedadventure:volcanic_output", output.get("type").getAsString(), name);
            assertEquals(FIELD, output.get("field").getAsString(), name);
            assertEquals(name, output.get("output").getAsString());
        }
        for (String name : List.of("guard", "islands")) {
            JsonObject json = WorldgenData.read(DF + "ring_3/overworld/" + name + ".json");
            assertEquals(FIELD, json.get("field").getAsString(), name);
            assertEquals(name, json.get("output").getAsString());
        }
        assertTrue(text("ring_3/overworld/height.json").contains(FIELD));
    }

    @Test
    void fieldNamesExistingNoisesAndTheBiomeThresholds() throws Exception {
        JsonObject field = WorldgenData.read(DF + "ring_3/overworld/field.json");
        for (String key : List.of("hash", "plains", "rolling", "flows", "heat", "ground", "channel", "ridge", "warp")) {
            String noise = field.get(key).getAsString().replace("corruptedadventure:", "");
            assertTrue(Files.exists(WorldgenData.root().resolve("noise/" + noise + ".json")), noise);
        }
        // The region axes are the router's (39a); the thresholds are the biome rectangles of ring 3.
        assertEquals("corruptedadventure:ring_3_heat", field.get("heat").getAsString());
        assertEquals("corruptedadventure:ring_3_ground", field.get("ground").getAsString());
        JsonObject settings = field.getAsJsonObject("settings");
        assertEquals(0.166, settings.get("hot").getAsDouble());
        assertEquals(-0.051, settings.get("cool").getAsDouble());
        assertEquals(-0.122, settings.get("seabed_ground").getAsDouble());
        assertEquals(0.122, settings.get("titan_ground").getAsDouble());
    }

    @Test
    void finalDensityIsTheStackWithRingThreeTerrainGuardAndIslands() {
        String text = text("ring_3/overworld.json");
        assertFalse(text.contains("ring_2/overworld/terrain"), "ring 2's terrain in ring 3's stack");
        assertTrue(text.contains("corruptedadventure:ring_3/overworld/terrain"));
        // Top slide moved up from vanilla's 240–256 so the great volcanoes (rims up to 260) are not cut off.
        assertFalse(text.contains("\"from_y\":240"));
        assertTrue(text.contains("\"from_y\":300") && text.contains("\"to_y\":316"));
        JsonObject root = WorldgenData.read(DF + "ring_3/overworld.json");
        assertEquals("minecraft:max", root.get("type").getAsString());
        JsonObject islands = root.getAsJsonObject("argument2");
        assertEquals("minecraft:interpolated", islands.get("type").getAsString());
        assertEquals("corruptedadventure:ring_3/overworld/islands", islands.get("argument").getAsString());
        JsonObject stack = root.getAsJsonObject("argument1");
        JsonObject noodle = stack.getAsJsonObject("argument2");
        assertEquals("corruptedadventure:ring_3/overworld/guard", noodle.get("argument2").getAsString());
        assertEquals(2, count(root, "corruptedadventure:ring_3/overworld/guard"), "guard keeps out both cheese/spaghetti and noodles");
    }

    @Test
    void preliminarySurfaceAndZoneBlend() {
        JsonObject initial = WorldgenData.read(DF + "ring_blend/overworld_initial.json");
        assertEquals("corruptedadventure:ring_3/overworld/initial_terrain",
                initial.getAsJsonObject("rings").getAsJsonObject("ring_3").get("argument2").getAsString());
        assertTrue(text("ring_3/overworld/initial_terrain.json").contains("minecraft:cache_2d"));
        // The ramp is w²: zero with zero slope where ring 3 starts to count, full at its core. Its weight is 1 from ring 3
        // outwards, so the zone 3→4 does not blend back to ring 2's height.
        JsonObject ramp = WorldgenData.read(DF + "ring_3/overworld/zone_ramp.json").getAsJsonObject("spline");
        assertEquals("corruptedadventure:ring_3/overworld/zone_weight", ramp.get("coordinate").getAsString());
        JsonObject weights = WorldgenData.read(DF + "ring_3/overworld/zone_weight.json").getAsJsonObject("argument").getAsJsonObject("rings");
        assertEquals(0.0, weights.get("ring_2").getAsDouble());
        for (String ring : List.of("ring_3", "ring_4", "ring_5", "void")) {
            assertEquals(1.0, weights.get(ring).getAsDouble(), ring);
        }
        var points = ramp.getAsJsonArray("points");
        assertEquals(0.0, points.get(0).getAsJsonObject().get("value").getAsDouble());
        assertEquals(0.0, points.get(0).getAsJsonObject().get("derivative").getAsDouble());
        assertEquals(1.0, points.get(points.size() - 1).getAsJsonObject().get("value").getAsDouble());
    }

    @Test
    void overworldRingThreeHasNoSeaAboveYZero() {
        JsonObject generator = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
        assertEquals(-1, generator.getAsJsonObject("fluid_levels").get("ring_3").getAsInt());
    }

    private static String text(String relative) {
        return WorldgenData.read(DF + relative).toString();
    }

    private static int count(JsonElement node, String value) {
        if (node.isJsonPrimitive()) {
            return node.getAsString().equals(value) ? 1 : 0;
        }
        int n = 0;
        if (node.isJsonObject()) {
            for (var entry : node.getAsJsonObject().entrySet()) {
                n += count(entry.getValue(), value);
            }
        } else if (node.isJsonArray()) {
            for (JsonElement element : node.getAsJsonArray()) {
                n += count(element, value);
            }
        }
        return n;
    }
}
