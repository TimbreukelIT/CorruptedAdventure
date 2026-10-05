package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import nl.timtoys.corruptedadventure.worldgen.feature.DewGeodeShape;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing2Features;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The hand-written JSON of Nether ring 2 against the Java constants it must agree with. */
class NetherRing2JsonTest {
    private static final Path WORLDGEN = findWorldgen();
    private static final Path RING_2 = WORLDGEN.resolve("density_function/ring_2/nether");

    private static Path findWorldgen() {
        String relative = "src/main/resources/data/corruptedadventure/worldgen";
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(relative))) {
                return dir.resolve(relative);
            }
        }
        throw new IllegalStateException("Could not find " + relative);
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test
    void geodeSettingsInTheDensityFunctionAreTheFeaturesSettings() throws IOException {
        JsonObject geodes = read(RING_2.resolve("dew_geodes.json"));
        assertEquals("corruptedadventure:dew_geodes", geodes.get("type").getAsString());
        DewGeodeShape.Settings settings = DewGeodeShape.Settings.CODEC.parse(JsonOps.INSTANCE, geodes.get("settings")).getOrThrow();
        assertEquals(NetherRing2Features.GEODE_SETTINGS, settings);
        assertEquals("corruptedadventure:nether_ring_2_geode_cells", geodes.get("noise").getAsString());
        assertEquals(NetherRing2Features.REGION_THRESHOLD, settings.regionThreshold());
    }

    @Test
    void geodeDensityFunctionReadsTheTerrainWithoutAnInterpolationMarker() throws IOException {
        JsonObject geodes = read(RING_2.resolve("dew_geodes.json"));
        assertEquals("corruptedadventure:ring_2/nether/terrain", geodes.get("terrain").getAsString());
        try (Stream<Path> files = Files.list(RING_2)) {
            for (Path file : files.toList()) {
                String text = Files.readString(file);
                assertFalse(text.contains("interpolated") || text.contains("cache_2d") || text.contains("cache_once"), file + " holds a cache or interpolation marker");
            }
        }
        // The one interpolation sits in ring_2/nether.json, around the terrain, next to the geodes outside it.
        JsonObject input = read(WORLDGEN.resolve("density_function/ring_2/nether.json"));
        assertEquals("minecraft:min", input.get("type").getAsString());
        assertEquals("minecraft:interpolated", input.getAsJsonObject("argument1").get("type").getAsString());
        assertEquals("corruptedadventure:ring_2/nether/terrain", input.getAsJsonObject("argument1").get("argument").getAsString());
        assertEquals("corruptedadventure:ring_2/nether/dew_geodes", input.get("argument2").getAsString());
    }

    @Test
    void theDamAndTheGeodeBorderShareOneEdge() throws IOException {
        JsonObject edge = read(RING_2.resolve("edge.json"));
        assertEquals("corruptedadventure:ring_edge", edge.get("type").getAsString());
        assertEquals("minecraft:the_nether", edge.get("dimension").getAsString());
        assertEquals("ring_2", edge.get("ring").getAsString());
        assertEquals(48.0, edge.get("width").getAsDouble());
        assertEquals("corruptedadventure:ring_2/nether/edge", read(RING_2.resolve("dew_geodes.json")).get("edge").getAsString());
        assertTrue(Files.readString(RING_2.resolve("dam.json")).contains("corruptedadventure:ring_2/nether/edge"));
        // Solid up to Y 33 (density > 0 below), air from there on.
        String dam = Files.readString(RING_2.resolve("dam.json"));
        assertTrue(dam.contains("\"from_y\": 29") && dam.contains("\"to_y\": 33"));
    }

    @Test
    void regionWeightTurnsOverAtTheBiomeThreshold() throws IOException {
        JsonObject weight = read(RING_2.resolve("region_weight.json"));
        // A flat cache around it: the 2D parts of the terrain are evaluated once per column, not once per cell corner.
        assertEquals("minecraft:flat_cache", weight.get("type").getAsString());
        weight = weight.getAsJsonObject("argument");
        assertEquals("minecraft:clamp", weight.get("type").getAsString());
        JsonObject sum = weight.getAsJsonObject("input");
        double offset = sum.get("argument2").getAsDouble();
        double scale = sum.getAsJsonObject("argument1").get("argument2").getAsDouble();
        // 0 at threshold − 0.05, 1 at threshold + 0.05.
        double t = NetherRing2Features.REGION_THRESHOLD;
        assertEquals(0.0, scale * (t - 0.05) + offset, 1e-9);
        assertEquals(1.0, scale * (t + 0.05) + offset, 1e-9);
    }

    @Test
    void routerTemperatureIsTheRingBlendWithVanillasNoiseInRing1AndTheRegionInRing2() throws IOException {
        JsonObject settings = read(WORLDGEN.resolve("noise_settings/nether.json"));
        assertEquals("corruptedadventure:ring_blend/nether_temperature", settings.getAsJsonObject("noise_router").get("temperature").getAsString());
        JsonObject blend = read(WORLDGEN.resolve("density_function/ring_blend/nether_temperature.json"));
        JsonObject ring1 = blend.getAsJsonObject("rings").getAsJsonObject("ring_1");
        assertEquals("minecraft:shifted_noise", ring1.get("type").getAsString());
        assertEquals("minecraft:temperature", ring1.get("noise").getAsString());
        assertEquals(1.0, ring1.get("xz_scale").getAsDouble());
        assertEquals("corruptedadventure:ring_2/nether/region", blend.getAsJsonObject("rings").get("ring_2").getAsString());
        // ring 1 stays exactly vanilla's (S compares the worlds), the vegetation is untouched
        assertEquals("minecraft:vegetation", settings.getAsJsonObject("noise_router").getAsJsonObject("vegetation").get("noise").getAsString());
    }

    @Test
    void surfaceRuleBranchOfRing2SitsAtIndex3BeforeTheLavaHoleRule() throws IOException {
        JsonArray sequence = read(WORLDGEN.resolve("noise_settings/nether.json")).getAsJsonObject("surface_rule").getAsJsonArray("sequence");
        JsonObject branch = sequence.get(3).getAsJsonObject();
        JsonArray biomes = branch.getAsJsonObject("if_true").getAsJsonArray("biome_is");
        assertEquals("[\"corruptedadventure:dripshroom_forest\",\"corruptedadventure:spore_caves\"]", biomes.toString());
        // The generic floor (hole -> lava) comes later, so it never reaches the ring 2 biomes.
        String after = sequence.asList().subList(4, sequence.size()).toString();
        assertTrue(after.contains("minecraft:hole"));
        assertFalse(branch.toString().contains("minecraft:hole"));
        // Everything that is left in the branch becomes soaked netherrack.
        JsonArray inner = branch.getAsJsonObject("then_run").getAsJsonArray("sequence");
        assertEquals("corruptedadventure:soaked_netherrack", inner.get(inner.size() - 1).getAsJsonObject().getAsJsonObject("result_state").get("Name").getAsString());
        assertTrue(branch.toString().contains("corruptedadventure:spore_moss"));
    }

    @Test
    void presetGivesRing2TwoBiomesAndALowerLavaSea() throws IOException {
        JsonObject nether = read(WORLDGEN.resolve("world_preset/corrupted_world.json")).getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals(22, nether.getAsJsonObject("fluid_levels").get("ring_2").getAsInt());
        JsonObject source = nether.getAsJsonObject("biome_source").getAsJsonObject("rings").getAsJsonObject("ring_2");
        assertEquals("minecraft:multi_noise", source.get("type").getAsString());
        JsonArray biomes = source.getAsJsonArray("biomes");
        assertEquals(2, biomes.size());
        double t = NetherRing2Features.REGION_THRESHOLD;
        JsonObject forest = biomes.get(0).getAsJsonObject();
        JsonObject caves = biomes.get(1).getAsJsonObject();
        assertEquals("corruptedadventure:dripshroom_forest", forest.get("biome").getAsString());
        assertEquals("corruptedadventure:spore_caves", caves.get("biome").getAsString());
        assertEquals(t, forest.getAsJsonObject("parameters").getAsJsonArray("temperature").get(0).getAsDouble());
        assertEquals(t, caves.getAsJsonObject("parameters").getAsJsonArray("temperature").get(1).getAsDouble());
        assertFalse(Files.readString(WORLDGEN.resolve("world_preset/corrupted_world.json")).contains("placeholder_nether_ring_2"));
        // The overworld has no fluid levels.
        assertFalse(read(WORLDGEN.resolve("world_preset/corrupted_world.json")).getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").has("fluid_levels"));
    }

    @Test
    void everyNoiseTheTerrainNamesExists() throws IOException {
        try (Stream<Path> files = Files.list(RING_2)) {
            for (Path file : files.toList()) {
                String text = Files.readString(file);
                for (String name : text.split("\"")) {
                    if (name.startsWith("corruptedadventure:nether_ring_2_")) {
                        assertTrue(Files.exists(WORLDGEN.resolve("noise/" + name.substring("corruptedadventure:".length()) + ".json")), name);
                    }
                }
            }
        }
        JsonElement unused = read(WORLDGEN.resolve("noise/nether_ring_2_moss.json"));
        assertTrue(unused.isJsonObject());
    }
}
