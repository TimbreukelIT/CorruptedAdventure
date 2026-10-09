package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing3Features;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The hand-written JSON of Nether ring 3's terrain (change 53) against the constants and rules it must agree with. */
class NetherRing3JsonTest {
    private static final Path RING_3 = WorldgenData.root().resolve("density_function/ring_3/nether");
    private static final String REGION_NOISE = "corruptedadventure:nether_ring_3_region";
    private static final String RING_3_BIOMES =
            "[\"corruptedadventure:frozen_lava_sea\",\"corruptedadventure:rime_forest\",\"corruptedadventure:glacier_rifts\"]";

    /** The weight of a region at {@code r}: the files are {@code flat_cache(clamp(region × k + c, 0, 1))}. */
    private static double weight(String file, double r) {
        JsonObject json = WorldgenData.read("density_function/ring_3/nether/" + file + ".json");
        assertEquals("minecraft:flat_cache", json.get("type").getAsString(), file);
        JsonObject clamp = json.getAsJsonObject("argument");
        assertEquals("minecraft:clamp", clamp.get("type").getAsString(), file);
        JsonObject sum = clamp.getAsJsonObject("input");
        JsonObject scaled = sum.getAsJsonObject("argument1");
        assertEquals("corruptedadventure:ring_3/nether/region", scaled.get("argument1").getAsString(), file);
        double value = scaled.get("argument2").getAsDouble() * r + sum.get("argument2").getAsDouble();
        return Math.max(clamp.get("min").getAsDouble(), Math.min(clamp.get("max").getAsDouble(), value));
    }

    @Test
    void regionWeightsTurnOverAtTheThresholds() {
        double rime = NetherRing3Features.RIME_THRESHOLD, rifts = NetherRing3Features.RIFTS_THRESHOLD;
        assertTrue(rifts < rime);
        assertEquals(0.5, weight("rime_weight", rime), 1e-9);
        assertEquals(0.5, weight("rifts_weight", rifts), 1e-9);
        // Fully in or out within ±0.04, and never both: the frozen sea (1 − both) stays in [0, 1].
        assertEquals(0.0, weight("rime_weight", rime - 0.04), 1e-9);
        assertEquals(1.0, weight("rime_weight", rime + 0.04), 1e-9);
        assertEquals(1.0, weight("rifts_weight", rifts - 0.04), 1e-9);
        assertEquals(0.0, weight("rifts_weight", rifts + 0.04), 1e-9);
        for (double r = -1.0; r <= 1.0; r += 0.01) {
            assertTrue(weight("rime_weight", r) + weight("rifts_weight", r) <= 1.0, "overlap at " + r);
        }
    }

    /**
     * {@code terrain} (54) = the cracked shape, plus the ice bridges only where the coarse bridge gate is open: outside it
     * the terrain is exactly {@code cracked} (no floor under the air in the ring transitions), and most columns cost one
     * flat-cache lookup per block. The bridges' own band is per block (raw noise), so a bridge is not cut into 4 × 4 steps.
     */
    @Test
    void terrainIsCrackedShapePlusBridges() {
        String cracked = "corruptedadventure:ring_3/nether/cracked";
        JsonObject terrain = WorldgenData.read("density_function/ring_3/nether/terrain.json");
        assertEquals("minecraft:range_choice", terrain.get("type").getAsString());
        assertEquals("corruptedadventure:ring_3/nether/bridge_gate", terrain.get("input").getAsString());
        assertTrue(terrain.get("min_inclusive").getAsDouble() > 0.0, "a closed gate (0) adds no bridge");
        assertEquals(cracked, terrain.get("when_out_of_range").getAsString());
        JsonObject withBridges = terrain.getAsJsonObject("when_in_range");
        assertEquals("minecraft:max", withBridges.get("type").getAsString());
        assertEquals(cracked, withBridges.get("argument1").getAsString());
        assertEquals("corruptedadventure:ring_3/nether/bridges", withBridges.get("argument2").getAsString());
        JsonObject bridges = WorldgenData.read("density_function/ring_3/nether/bridges.json");
        assertEquals("minecraft:clamp", bridges.get("type").getAsString());
        assertTrue(bridges.get("min").getAsDouble() <= -2.5, "the bridges never lift the air");
        assertEquals("minecraft:flat_cache", WorldgenData.read("density_function/ring_3/nether/bridge_gate.json").get("type").getAsString());
        JsonObject mask = WorldgenData.read("density_function/ring_3/nether/bridge_mask.json");
        assertFalse(mask.get("type").getAsString().equals("minecraft:flat_cache"), "the bridge band is per block");
        assertTrue(mask.toString().contains("\"corruptedadventure:nether_ring_3_bridges\""), "the band reads the raw noise");
    }

    /**
     * {@code cracked} = the interpolated shape, cut by the cracks only where the crack mask is open. Both branches name the
     * same {@code interpolated_shape}, so there is one interpolator.
     */
    @Test
    void crackedIsTheInterpolatedShapeCutByTheCracks() throws IOException {
        String shape = "corruptedadventure:ring_3/nether/interpolated_shape";
        JsonObject cracked = WorldgenData.read("density_function/ring_3/nether/cracked.json");
        assertEquals("minecraft:range_choice", cracked.get("type").getAsString());
        assertEquals("corruptedadventure:ring_3/nether/crack_mask", cracked.get("input").getAsString());
        assertTrue(cracked.get("min_inclusive").getAsDouble() > 0.0, "a closed mask (0) takes the plain shape");
        assertEquals(shape, cracked.get("when_out_of_range").getAsString());
        JsonObject cut = cracked.getAsJsonObject("when_in_range");
        assertEquals("minecraft:min", cut.get("type").getAsString());
        assertEquals(shape, cut.get("argument1").getAsString());
        assertEquals("corruptedadventure:ring_3/nether/cracks", cut.get("argument2").getAsString());
        JsonObject interpolated = WorldgenData.read("density_function/ring_3/nether/interpolated_shape.json");
        assertEquals("minecraft:interpolated", interpolated.get("type").getAsString());
        assertEquals("corruptedadventure:ring_3/nether/shape", interpolated.get("argument").getAsString());
        assertEquals("minecraft:flat_cache", WorldgenData.read("density_function/ring_3/nether/crack_mask.json").get("type").getAsString());
        // The one interpolation marker sits in interpolated_shape.json; no cell caches.
        try (Stream<Path> files = Files.list(RING_3)) {
            for (Path file : files.toList()) {
                String text = Files.readString(file);
                if (!file.getFileName().toString().equals("interpolated_shape.json")) {
                    assertFalse(text.contains("minecraft:interpolated"), file + " holds an interpolation marker");
                }
                assertFalse(text.contains("cache_once") || text.contains("cache_all_in_cell"), file + " holds a cell cache");
            }
        }
    }

    @Test
    void everyNoiseTheTerrainNamesExists() throws IOException {
        try (Stream<Path> files = Files.list(RING_3)) {
            for (Path file : files.toList()) {
                for (String name : Files.readString(file).split("\"")) {
                    if (name.startsWith("corruptedadventure:nether_ring_3_")) {
                        assertTrue(Files.exists(WorldgenData.root().resolve("noise/" + name.substring("corruptedadventure:".length()) + ".json")), name);
                    }
                }
            }
        }
    }

    /**
     * The Nether ring 3 branch of the surface rule comes before vanilla's generic floor ({@code hole} → lava below
     * Y 32), so no lava appears above ring 3's level; it reads the region on the same thresholds as the terrain, the
     * same noises as the pillars and cracks, and ends in a catch-all.
     */
    @Test
    void surfaceRuleBranchOfRing3ComesBeforeTheLavaHoleRule() {
        JsonArray sequence = WorldgenData.read("noise_settings/nether.json").getAsJsonObject("surface_rule").getAsJsonArray("sequence");
        int index = -1, hole = -1;
        for (int i = 0; i < sequence.size(); i++) {
            JsonObject rule = sequence.get(i).getAsJsonObject();
            JsonElement biomes = rule.has("if_true") ? rule.getAsJsonObject("if_true").get("biome_is") : null;
            if (biomes != null && biomes.toString().equals(RING_3_BIOMES)) {
                index = i;
            }
            if (hole < 0 && rule.toString().contains("minecraft:hole")) {
                hole = i;
            }
        }
        assertTrue(index >= 0, "no ring 3 branch");
        assertTrue(hole > index, "the ring 3 branch must come before the first rule with hole (lava)");
        JsonObject branch = sequence.get(index).getAsJsonObject();
        assertFalse(branch.toString().contains("minecraft:hole"));
        JsonArray inner = branch.getAsJsonObject("then_run").getAsJsonArray("sequence");
        assertEquals("minecraft:block", inner.get(inner.size() - 1).getAsJsonObject().get("type").getAsString(), "catch-all");

        List<JsonObject> thresholds = new ArrayList<>();
        collect(branch, "minecraft:noise_threshold", thresholds);
        double rime = NetherRing3Features.RIME_THRESHOLD, rifts = NetherRing3Features.RIFTS_THRESHOLD;
        int regionChecks = 0;
        for (JsonObject threshold : thresholds) {
            String noise = threshold.get("noise").getAsString();
            double min = threshold.get("min_threshold").getAsDouble(), max = threshold.get("max_threshold").getAsDouble();
            if (noise.equals(REGION_NOISE)) {
                regionChecks++;
                assertTrue((min == rifts && max == rime) || (min == rime && max >= 2.0) || (max == rifts && min <= -2.0),
                        "region band [" + min + ", " + max + "]");
            }
        }
        assertTrue(regionChecks >= 3);
        // Same noises as the terrain: the region axis, the pillars, the cracks, the falls, the drapes and the ice falls.
        assertEquals(REGION_NOISE, WorldgenData.read("density_function/ring_3/nether/region.json").getAsJsonObject("argument").get("noise").getAsString());
        String branchText = branch.toString();
        for (String file : new String[]{"pillar_noise", "cracks", "crack_mask", "falls", "drapes", "icefalls"}) {
            String json = WorldgenData.read("density_function/ring_3/nether/" + file + ".json").toString();
            for (String noise : new String[]{"nether_ring_3_pillars", "nether_ring_3_cracks", "nether_ring_3_crack_patches",
                    "nether_ring_3_falls", "nether_ring_3_drapes", "nether_ring_3_drape_patches", "nether_ring_3_icefalls"}) {
                if (json.contains("\"corruptedadventure:" + noise + "\"")) {
                    assertTrue(branchText.contains("\"corruptedadventure:" + noise + "\""), noise);
                }
            }
        }
    }

    /**
     * The dam of step 3|4 must not be carved (contract §4): until the Nether ring 3 biomes bring their own carvers (53b,
     * with a guard for the dam), every biome Nether ring 3 can produce has none.
     */
    @Test
    void nether3BiomesHaveNoCarversYet() {
        JsonObject source = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator").getAsJsonObject("biome_source")
                .getAsJsonObject("rings").getAsJsonObject("ring_3");
        List<String> biomes = new ArrayList<>();
        if (source.has("biome")) {
            biomes.add(source.get("biome").getAsString());
        } else {
            for (JsonElement entry : source.getAsJsonArray("biomes")) {
                biomes.add(entry.getAsJsonObject().get("biome").getAsString());
            }
        }
        Path generated = WorldgenData.root().getParent().getParent().getParent().getParent().getParent()
                .resolve("generated/resources/data/corruptedadventure/worldgen/biome");
        for (String biome : biomes) {
            JsonObject json = WorldgenData.read(generated.resolve(biome.substring("corruptedadventure:".length()) + ".json"));
            JsonElement carvers = json.get("carvers");
            assertTrue(carvers.isJsonObject() ? carvers.getAsJsonObject().isEmpty() : carvers.getAsJsonArray().isEmpty(), biome + " has carvers");
        }
    }

    /**
     * The surface paints the frozen falls and drapes (53b) and the ice falls (54) where the terrain grows them: the
     * falls from the start of their ramp, the ice falls from within theirs, the drapes only inside the {@code drapes}
     * band and its patches (where the curtain is a few blocks long, so no paint lands on a flat ceiling).
     */
    @Test
    void surfacePaintsTheFrozenFallsWhereTheTerrainGrowsThem() {
        JsonArray sequence = WorldgenData.read("noise_settings/nether.json").getAsJsonObject("surface_rule").getAsJsonArray("sequence");
        JsonObject branch = null;
        for (JsonElement rule : sequence) {
            JsonElement biomes = rule.getAsJsonObject().has("if_true") ? rule.getAsJsonObject().getAsJsonObject("if_true").get("biome_is") : null;
            if (biomes != null && biomes.toString().equals(RING_3_BIOMES)) {
                branch = rule.getAsJsonObject();
            }
        }
        assertTrue(branch != null);
        List<JsonObject> thresholds = new ArrayList<>();
        collect(branch, "minecraft:noise_threshold", thresholds);
        double fallsStart = -offsetOf(WorldgenData.read("density_function/ring_3/nether/falls.json"), "corruptedadventure:nether_ring_3_falls");
        double icefallsStart = -offsetOf(WorldgenData.read("density_function/ring_3/nether/icefalls.json"), "corruptedadventure:nether_ring_3_icefalls");
        double patchesStart = -offsetOf(WorldgenData.read("density_function/ring_3/nether/drapes.json"), "corruptedadventure:nether_ring_3_drape_patches");
        double band = bandOf(WorldgenData.read("density_function/ring_3/nether/drapes.json"));
        int found = 0;
        for (JsonObject threshold : thresholds) {
            String noise = threshold.get("noise").getAsString();
            double min = threshold.get("min_threshold").getAsDouble(), max = threshold.get("max_threshold").getAsDouble();
            switch (noise) {
                case "corruptedadventure:nether_ring_3_falls" -> {
                    assertEquals(fallsStart, min, 1e-9);
                    found++;
                }
                case "corruptedadventure:nether_ring_3_icefalls" -> {
                    assertTrue(min >= icefallsStart, "ice-fall paint starts within the ribs: " + min);
                    found++;
                }
                case "corruptedadventure:nether_ring_3_drape_patches" -> {
                    assertTrue(min > patchesStart, "drape paint starts inside the patches: " + min);
                    found++;
                }
                case "corruptedadventure:nether_ring_3_drapes" -> {
                    assertEquals(-max, min, 1e-9);
                    assertTrue(max > 0 && max < band, "drape paint lies inside the band: " + max);
                    found++;
                }
                default -> {
                }
            }
        }
        assertEquals(4, found);
    }

    /** {@code c} of the first {@code add(noise(name), c)} in the tree. */
    private static double offsetOf(JsonElement tree, String noise) {
        List<JsonObject> adds = new ArrayList<>();
        collect(tree, "minecraft:add", adds);
        for (JsonObject add : adds) {
            JsonElement first = add.get("argument1");
            if (first.isJsonObject() && first.getAsJsonObject().has("noise") && first.getAsJsonObject().get("noise").getAsString().equals(noise)) {
                return add.get("argument2").getAsDouble();
            }
        }
        throw new AssertionError("no add(" + noise + ", c)");
    }

    /** {@code w} of {@code add(w, -|noise|)}: the half width of the drapes' band. */
    private static double bandOf(JsonElement tree) {
        List<JsonObject> adds = new ArrayList<>();
        collect(tree, "minecraft:add", adds);
        for (JsonObject add : adds) {
            if (add.get("argument1").isJsonPrimitive() && add.get("argument2").toString().contains("minecraft:abs")) {
                return add.get("argument1").getAsDouble();
            }
        }
        throw new AssertionError("no band");
    }

    private static void collect(JsonElement tree, String type, List<JsonObject> out) {
        if (tree.isJsonObject()) {
            JsonObject object = tree.getAsJsonObject();
            if (object.has("type") && object.get("type").getAsString().equals(type)) {
                out.add(object);
            }
            for (var entry : object.entrySet()) {
                collect(entry.getValue(), type, out);
            }
        } else if (tree.isJsonArray()) {
            for (JsonElement element : tree.getAsJsonArray()) {
                collect(element, type, out);
            }
        }
    }
}
