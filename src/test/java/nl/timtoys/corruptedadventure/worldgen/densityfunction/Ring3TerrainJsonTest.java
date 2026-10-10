package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonArray;
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
    void contractOutputsReadTheOneField() throws Exception {
        for (String name : List.of("caldera_mask", "caldera_dormant", "lava_channel", "island_mask", "roughness", "collapse")) {
            JsonObject json = WorldgenData.read(DF + "ring_3/overworld/" + name + ".json");
            assertEquals("minecraft:flat_cache", json.get("type").getAsString(), name);
            JsonObject output = json.getAsJsonObject("argument");
            assertEquals("corruptedadventure:volcanic_output", output.get("type").getAsString(), name);
            assertEquals(FIELD, output.get("field").getAsString(), name);
            assertEquals(name, output.get("output").getAsString());
        }
        // 39f: the islands are part of the thin shapes (exact per block), no output of their own.
        assertFalse(Files.exists(WorldgenData.root().resolve(DF + "ring_3/overworld/islands.json")));
        for (String name : List.of("guard", "thin")) {
            JsonObject json = WorldgenData.read(DF + "ring_3/overworld/" + name + ".json");
            assertEquals(FIELD, json.get("field").getAsString(), name);
            assertEquals(name, json.get("output").getAsString());
        }
        assertTrue(text("ring_3/overworld/height.json").contains(FIELD));
    }

    @Test
    void fieldNamesExistingNoisesAndTheBiomeThresholds() throws Exception {
        JsonObject field = WorldgenData.read(DF + "ring_3/overworld/field.json");
        for (String key : List.of("hash", "plains", "rolling", "flows", "heat", "ground", "ridge", "warp")) {
            String noise = field.get(key).getAsString().replace("corruptedadventure:", "");
            assertTrue(Files.exists(WorldgenData.root().resolve("noise/" + noise + ".json")), noise);
        }
        JsonObject relief = field.getAsJsonObject("relief");
        for (String key : List.of("highlands", "shapes", "detail", "rifts")) {
            String noise = relief.get(key).getAsString().replace("corruptedadventure:", "");
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
    void finalDensityIsTheStackWithRingThreeTerrainAndGuard() {
        String text = text("ring_3/overworld.json");
        assertFalse(text.contains("ring_2/overworld/terrain"), "ring 2's terrain in ring 3's stack");
        assertTrue(text.contains("corruptedadventure:ring_3/overworld/terrain"));
        // Top slide moved up from vanilla's 240–256 so the great volcanoes (rims up to 260) are not cut off.
        assertFalse(text.contains("\"from_y\":240"));
        assertTrue(text.contains("\"from_y\":300") && text.contains("\"to_y\":316"));
        JsonObject root = WorldgenData.read(DF + "ring_3/overworld.json");
        // 39d: range_choice on the thin shapes (per block, not interpolated): ±1 there, else the stack. 39f: the islands
        // are thin shapes too (exact per block), no longer max(stack, interpolated(islands)).
        assertEquals("minecraft:range_choice", root.get("type").getAsString());
        assertEquals("corruptedadventure:ring_3/overworld/thin", root.get("input").getAsString());
        assertEquals("corruptedadventure:ring_3/overworld/thin", root.get("when_out_of_range").getAsString());
        JsonObject stack = root.getAsJsonObject("when_in_range");
        assertEquals("minecraft:min", stack.get("type").getAsString());
        assertFalse(text.contains("ring_3/overworld/islands"));
        JsonObject noodle = stack.getAsJsonObject("argument2");
        assertEquals("corruptedadventure:ring_3/overworld/guard", noodle.get("argument2").getAsString());
        assertEquals(2, count(root, "corruptedadventure:ring_3/overworld/guard"), "guard keeps out both cheese/spaghetti and noodles");
        // 42: the cave lift raises the caves (near the surface, deep, and the noodles), never the terrain itself.
        assertEquals(3, count(root, "corruptedadventure:ring_3/overworld/cave_lift"));
        JsonObject lift = WorldgenData.read(DF + "ring_3/overworld/cave_lift.json");
        assertEquals("cave_lift", lift.get("output").getAsString());
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

    @Test
    void ringThreeAquifersStayDeepAndHaveNoDeepDark() {
        // 42: ring 3's own floodedness: vanilla's below Y −24, then falling to −2 by Y −8 (no local aquifer fluid in the
        // tubes, chambers or caves above, V8); a constant erosion (no deep-dark check of the aquifer, like ring 2).
        JsonObject floodedness = WorldgenData.read(DF + "ring_blend/overworld_floodedness.json").getAsJsonObject("rings");
        assertEquals("corruptedadventure:ring_3/overworld/floodedness", floodedness.get("ring_3").getAsString());
        JsonObject own = WorldgenData.read(DF + "ring_3/overworld/floodedness.json");
        assertEquals("minecraft:aquifer_fluid_level_floodedness", own.getAsJsonObject("argument1").get("noise").getAsString());
        JsonObject gradient = own.getAsJsonObject("argument2");
        assertEquals("minecraft:y_clamped_gradient", gradient.get("type").getAsString());
        assertTrue(gradient.get("to_y").getAsInt() <= 0 && gradient.get("to_value").getAsDouble() <= -1.0);
        assertEquals(0.0, gradient.get("from_value").getAsDouble());
        // Erosion 0 only where ring 3 has all the weight: in the zones its own value is vanilla's, so the zones (and
        // ring 4's half of 3→4) keep the erosion they had (review 42).
        JsonObject erosion = WorldgenData.read(DF + "ring_blend/overworld_erosion.json").getAsJsonObject("rings");
        assertEquals("corruptedadventure:ring_3/overworld/erosion", erosion.get("ring_3").getAsString());
        JsonObject own3 = WorldgenData.read(DF + "ring_3/overworld/erosion.json");
        assertEquals(0.0, own3.get("when_in_range").getAsDouble());
        assertEquals("minecraft:overworld/erosion", own3.get("when_out_of_range").getAsString());
        assertTrue(own3.get("min_inclusive").getAsDouble() > 0.99);
        JsonObject weight = own3.getAsJsonObject("input").getAsJsonObject("argument").getAsJsonObject("rings");
        for (String ring : List.of("ring_1", "ring_2", "ring_4", "ring_5", "void")) {
            assertEquals(0.0, weight.get(ring).getAsDouble(), ring);
        }
        assertEquals(1.0, weight.get("ring_3").getAsDouble());
        // The other rings keep theirs.
        assertEquals("minecraft:overworld/erosion", erosion.get("ring_1").getAsString());
    }

    @Test
    void fireGeodesAreAnOutputOfTheField() {
        JsonObject json = WorldgenData.read(DF + "ring_3/overworld/fire_geodes.json");
        assertEquals("corruptedadventure:volcanic_output", json.get("type").getAsString());
        assertEquals(FIELD, json.get("field").getAsString());
        assertEquals("fire_geodes", json.get("output").getAsString());
    }

    @Test
    void caveWallsAreTheSecondTopLevelSurfaceRule() {
        // 42: the walls lie far below the preliminary surface, so their rule stands outside that branch, right after
        // the bedrock floor, guarded by the ring and a per-column test.
        JsonArray rules = WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("surface_rule")
                .getAsJsonArray("sequence");
        assertEquals("minecraft:bedrock_floor", rules.get(0).getAsJsonObject().getAsJsonObject("if_true").get("random_name").getAsString());
        JsonObject walls = rules.get(1).getAsJsonObject();
        assertEquals("corruptedadventure:ring_reach", walls.getAsJsonObject("if_true").get("type").getAsString());
        assertEquals("ring_3", walls.getAsJsonObject("if_true").get("ring").getAsString());
        JsonObject perColumn = walls.getAsJsonObject("then_run").getAsJsonObject("if_true");
        assertEquals("cave_walls", perColumn.get("output").getAsString());
        String text = walls.toString();
        assertTrue(text.contains("\"output\":\"cave_wall\"") && text.contains("minecraft:smooth_basalt"));
        // The walls stop under the ground; a thin roof at a mouth keeps the biome's own rules (39c ash crust).
        assertFalse(text.contains("corruptedadventure:ash_crust"));
        // 42d: the ash crust of 38c (hangs until a player stands under it) on roofs of one block and in front of every
        // mouth (a thin solid of its own kind), in the ring 3 branch, not in the walls' rule.
        String all = WorldgenData.read("noise_settings/overworld.json").toString();
        assertTrue(all.contains("corruptedadventure:ash_crust"));
        assertTrue(all.contains("{\"type\":\"corruptedadventure:volcanic_threshold\",\"output\":\"thin_kind\",\"min\":5.5,\"max\":6.5}"),
                "the mouth's roof (thin kind " + Landmarks.KIND_CRUST + ")");
        assertEquals(6, Landmarks.KIND_CRUST);
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
