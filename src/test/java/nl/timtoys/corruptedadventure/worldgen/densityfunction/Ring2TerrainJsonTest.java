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

    @Test
    void riverbedsCutHeightAndInitialHeightOnceAndOnlyTheirOwnBase() throws IOException {
        // Change 31: height and initial_height are thin wrappers  h − min(cut, max(0, h − 66)) around their *_base.
        Path root = findRoot();
        for (String name : new String[]{"height", "initial_height"}) {
            String wrapper = Files.readString(root.resolve("ring_2/overworld/" + name + ".json"));
            assertEquals(2, count(wrapper, "\"corruptedadventure:ring_2/overworld/" + name + "_base\""), name + " names its base twice (flat caches)");
            assertEquals(1, count(wrapper, "corruptedadventure:ring_2/overworld/riverbed_cut"), name);
            assertTrue(wrapper.contains("\"minecraft:min\"") && wrapper.contains("-66.0"), name + ": never below Y 66");
            String base = Files.readString(root.resolve("ring_2/overworld/" + name + "_base.json"));
            assertEquals(0, count(base, "riverbed"), name + "_base knows no riverbeds");
        }
        JsonObject cut = JsonParser.parseString(Files.readString(root.resolve("ring_2/overworld/riverbed_cut.json"))).getAsJsonObject();
        assertEquals("minecraft:flat_cache", cut.get("type").getAsString());
        String cutText = cut.toString();
        assertTrue(cutText.contains("ring_2/overworld/riverbed_mask") && cutText.contains("ring_2/overworld/riverbeds"));
    }

    @Test
    void ringThreeInputCarriesTheSameCutInItsOwnGradientUnits() throws IOException {
        // Ring 2's term saturates (the squeeze) where the blended surface lies 12+ blocks from ring 2's own height, so in the
        // zone the cut must also lower ring 3's input: a cut of c blocks = c · slope of ring 3's gradient (2 / 128 per block).
        JsonObject ring3 = JsonParser.parseString(Files.readString(findRoot().resolve("ring_3/overworld.json"))).getAsJsonObject();
        JsonObject sum = ring3.getAsJsonObject("argument");
        JsonObject gradient = sum.getAsJsonObject("argument1").getAsJsonObject("argument1");
        double slope = (gradient.get("from_value").getAsDouble() - gradient.get("to_value").getAsDouble())
                / (gradient.get("to_y").getAsDouble() - gradient.get("from_y").getAsDouble());
        JsonObject cut = sum.getAsJsonObject("argument2");
        assertEquals("minecraft:mul", cut.get("type").getAsString());
        assertEquals(-slope, cut.get("argument1").getAsDouble(), 1e-12);
        assertEquals("corruptedadventure:ring_2/overworld/riverbed_cut", cut.get("argument2").getAsString());
        // The mask rises in the first quarter of the zone and fades out again before ring 3 proper (no beds in its core).
        JsonObject mask = JsonParser.parseString(Files.readString(findRoot().resolve("ring_2/overworld/riverbed_mask.json"))).getAsJsonObject();
        var points = mask.getAsJsonObject("spline").getAsJsonArray("points");
        assertEquals(0.0, points.get(0).getAsJsonObject().get("value").getAsDouble());
        assertEquals(1.0, points.get(1).getAsJsonObject().get("value").getAsDouble());
        assertEquals(0.0, points.get(points.size() - 1).getAsJsonObject().get("value").getAsDouble());
        assertTrue(points.get(points.size() - 1).getAsJsonObject().get("location").getAsDouble() < 1.0);
    }

    @Test
    void poolsRunDryInTheZoneWithTheirOwnSteepRamp() throws IOException {
        Path root = findRoot();
        String pools = Files.readString(root.resolve("ring_2/overworld/pools.json"));
        assertEquals(1, count(pools, "corruptedadventure:ring_2/overworld/pools_dry"));
        assertEquals(0, count(pools, "riverbed"), "the pools use their own ramp, not the riverbed mask");
        JsonObject dry = JsonParser.parseString(Files.readString(root.resolve("ring_2/overworld/pools_dry.json"))).getAsJsonObject();
        var points = dry.getAsJsonObject("spline").getAsJsonArray("points");
        assertEquals(0.0, points.get(0).getAsJsonObject().get("value").getAsDouble());
        assertEquals(0.03, points.get(1).getAsJsonObject().get("location").getAsDouble());
        assertEquals(1.0, points.get(1).getAsJsonObject().get("value").getAsDouble());
        assertEquals("corruptedadventure:ring_weight/overworld/ring_3", dry.getAsJsonObject("spline").get("coordinate").getAsString());
    }

    @Test
    void ringThreeWeightIsARingBlendOfConstants() throws IOException {
        JsonObject weight = JsonParser.parseString(Files.readString(findRoot().resolve("ring_weight/overworld/ring_3.json"))).getAsJsonObject();
        JsonObject blend = weight.getAsJsonObject("argument");
        assertEquals("corruptedadventure:ring_blend", blend.get("type").getAsString());
        for (var ring : blend.getAsJsonObject("rings").entrySet()) {
            assertEquals(ring.getKey().equals("ring_3") ? 1.0 : 0.0, ring.getValue().getAsDouble(), ring.getKey());
        }
    }

    @Test
    void riverbedTerrainAndSurfaceRuleShareOneNoiseAndTheSameKnots() throws IOException {
        // Terrain: |noise| with xz_scale 1 and y_scale 0 samples getValue(x, 0, z), as the surface rule's noise_threshold.
        Path root = findRoot();
        JsonObject beds = JsonParser.parseString(Files.readString(root.resolve("ring_2/overworld/riverbeds.json"))).getAsJsonObject();
        JsonObject coordinate = beds.getAsJsonObject("spline").getAsJsonObject("coordinate");
        assertEquals("minecraft:abs", coordinate.get("type").getAsString());
        JsonObject noise = coordinate.getAsJsonObject("argument");
        assertEquals("corruptedadventure:dry_riverbeds_channel", noise.get("noise").getAsString());
        assertEquals(1.0, noise.get("xz_scale").getAsDouble());
        assertEquals(0.0, noise.get("y_scale").getAsDouble());
        var points = beds.getAsJsonObject("spline").getAsJsonArray("points");
        java.util.Set<Double> knots = new java.util.HashSet<>();
        for (var point : points) {
            knots.add(point.getAsJsonObject().get("location").getAsDouble());
        }
        double floor = 0.04;
        double bank = 0.07;
        assertTrue(knots.contains(floor) && knots.contains(bank), "the knots of the bed floor and bank");
        assertEquals(5.0, points.get(0).getAsJsonObject().get("value").getAsDouble(), "full depth in the middle");
        assertEquals(0.0, points.get(points.size() - 1).getAsJsonObject().get("value").getAsDouble(), "no depth beyond the bank");

        // Surface rule: every noise_threshold on the channel noise sits on those knots.
        Path settings = root.getParent().resolve("noise_settings/overworld.json");
        java.util.List<double[]> thresholds = new java.util.ArrayList<>();
        collectChannelThresholds(JsonParser.parseString(Files.readString(settings)), thresholds);
        assertTrue(thresholds.size() >= 3, "floor, bank and steep bank");
        for (double[] range : thresholds) {
            assertEquals(-range[0], range[1], 1e-12, "symmetric around the channel");
            assertTrue(range[1] == floor || range[1] == bank, "threshold " + range[1] + " is not a knot of riverbeds.json");
        }
    }

    private static void collectChannelThresholds(com.google.gson.JsonElement node, java.util.List<double[]> out) {
        if (node.isJsonObject()) {
            JsonObject object = node.getAsJsonObject();
            if (object.has("noise") && object.get("noise").isJsonPrimitive()
                    && object.get("noise").getAsString().equals("corruptedadventure:dry_riverbeds_channel")) {
                out.add(new double[]{object.get("min_threshold").getAsDouble(), object.get("max_threshold").getAsDouble()});
            }
            object.entrySet().forEach(entry -> collectChannelThresholds(entry.getValue(), out));
        } else if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(element -> collectChannelThresholds(element, out));
        }
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
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
