package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing4Features;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The JSON of Nether ring 4's terrain (65, written by {@code tools/worldgen/nether_ring4_terrain.py} and
 * {@code nether_ring4_surface.py}) against the constants and rules it must agree with.
 */
class NetherRing4JsonTest {
    private static final String DIR = "density_function/ring_4/nether/";
    private static final Path RING_4 = WorldgenData.root().resolve(DIR);
    private static final String REF = "corruptedadventure:ring_4/nether/";
    private static final String RING_4_BIOMES =
            "[\"corruptedadventure:bone_dunes\",\"corruptedadventure:crystal_spires\",\"corruptedadventure:geode_hollows\"]";
    private static final long SEED = 20260930L;
    private static final String CA_GEODES = "corruptedadventure:giant_geodes";

    /** The weight of a region at {@code r}: the files are {@code flat_cache(clamp(region × k + c, 0, 1))}. */
    private static double weight(String file, double r) {
        JsonObject json = WorldgenData.read(DIR + file + ".json");
        assertEquals("minecraft:flat_cache", json.get("type").getAsString(), file);
        JsonObject clamp = json.getAsJsonObject("argument");
        assertEquals("minecraft:clamp", clamp.get("type").getAsString(), file);
        assertEquals(0.0, clamp.get("min").getAsDouble(), file);
        assertEquals(1.0, clamp.get("max").getAsDouble(), file);
        JsonObject sum = clamp.getAsJsonObject("input");
        JsonObject scaled = sum.getAsJsonObject("argument1");
        assertEquals(REF + "region", scaled.get("argument1").getAsString(), file);
        double value = scaled.get("argument2").getAsDouble() * r + sum.get("argument2").getAsDouble();
        return Math.max(0.0, Math.min(1.0, value));
    }

    @Test
    void regionWeightsTurnOverAtTheBiomeThresholds() {
        double spires = NetherRing4Features.SPIRES_THRESHOLD, hollows = NetherRing4Features.HOLLOWS_THRESHOLD;
        assertEquals(0.5, weight("spires_weight", spires), 1e-9);
        assertEquals(0.5, weight("hollows_weight", hollows), 1e-9);
        assertEquals(0.0, weight("spires_weight", 0.0));
        assertEquals(0.0, weight("hollows_weight", 0.0));
        assertEquals(1.0, weight("spires_weight", spires + 0.1));
        assertEquals(1.0, weight("hollows_weight", hollows - 0.1));
        assertEquals(0.0, weight("spires_weight", hollows));
        assertEquals(0.0, weight("hollows_weight", spires));
        String dunes = WorldgenData.read(DIR + "dunes_weight.json").toString();
        assertTrue(dunes.contains(REF + "spires_weight") && dunes.contains(REF + "hollows_weight"), "dunes = 1 − spires − hollows");
    }

    /** One interpolator: the terrain is the interpolated shape (the geodes, if any, cut it outside the interpolation). */
    @Test
    void terrainInterpolatesTheShapeOnce() throws IOException {
        int interpolated = 0;
        try (Stream<Path> files = Files.list(RING_4)) {
            for (Path file : files.toList()) {
                interpolated += count(Files.readString(file), "\"minecraft:interpolated\"");
            }
        }
        assertEquals(1, interpolated);
        assertEquals(REF + "shape", WorldgenData.read(DIR + "interpolated_shape.json").get("argument").getAsString());
        // both branches of terrain (with and without the geodes) name the same file: one interpolator
        String terrain = Files.readString(RING_4.resolve("terrain.json"));
        assertEquals(2, count(terrain, "\"" + REF + "interpolated_shape\""));
        assertTrue(terrain.contains("\"" + REF + "geodes\""));
        assertEquals(CA_GEODES, WorldgenData.read(DIR + "geodes.json").get("type").getAsString());
        assertEquals(REF + "shape", WorldgenData.read(DIR + "geodes.json").get("shape").getAsString(), "the geodes test the uninterpolated shape");
    }

    /** A density function file must be a number or an object (the registry codec takes no bare reference). */
    @Test
    void everyFileIsAnObject() throws IOException {
        try (Stream<Path> files = Files.list(RING_4)) {
            for (Path file : files.toList()) {
                assertTrue(com.google.gson.JsonParser.parseString(Files.readString(file)).isJsonObject(), file.getFileName().toString());
            }
        }
    }

    @Test
    void everyNoiseItReadsExists() throws IOException {
        Pattern noise = Pattern.compile("\"noise\": \"corruptedadventure:([a-z0-9_]+)\"");
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.list(RING_4)) {
            for (Path file : files.toList()) {
                Matcher matcher = noise.matcher(Files.readString(file));
                while (matcher.find()) {
                    found.add(matcher.group(1));
                    assertTrue(Files.exists(WorldgenData.root().resolve("noise/" + matcher.group(1) + ".json")), matcher.group(1));
                }
            }
        }
        assertTrue(found.contains("nether_ring_4_region") && found.contains("nether_ring_4_pillars"), found.toString());
    }

    /** The lava sea of the spires, its columns and bridges, and the mass fade out towards both boundaries, so the dam stays whole there. */
    @Test
    void shapesFadeOutAtTheBoundaries() {
        String sea = WorldgenData.read(DIR + "sea.json").toString();
        assertTrue(sea.contains(REF + "core") && sea.contains(REF + "spires_weight"), "sea = spires × core");
        assertTrue(WorldgenData.read(DIR + "floor_height.json").toString().contains(REF + "sea"), "the floor sinks with the sea");
        String shape = WorldgenData.read(DIR + "shape.json").toString();
        assertTrue(shape.contains(REF + "pillars") && shape.contains(REF + "bridges"), "columns and bridges");
        assertTrue(shape.contains(REF + "dam"), "the dam is a term of the shape");
        assertTrue(WorldgenData.read(DIR + "mass_top.json").toString().contains(REF + "edge"), "mass sinks at the edge");
        JsonObject core = WorldgenData.read(DIR + "core.json").getAsJsonObject("argument");
        assertEquals(1.0, core.get("argument1").getAsDouble(), "core = 1 − edge");
        assertEquals(REF + "edge", core.getAsJsonObject("argument2").get("argument2").getAsString());
    }

    /** The surface branch of the three biomes sits before vanilla's floor rule (holes below Y 32 → lava) and ends in a catch-all. */
    @Test
    void surfaceBranchComesBeforeVanillasLavaHoles() {
        JsonArray rules = WorldgenData.read("noise_settings/nether.json").getAsJsonObject("surface_rule").getAsJsonArray("sequence");
        int own = -1, hole = -1;
        for (int i = 0; i < rules.size(); i++) {
            String rule = rules.get(i).toString();
            JsonObject test = rules.get(i).getAsJsonObject().getAsJsonObject("if_true");
            if (test != null && test.has("biome_is") && test.get("biome_is").toString().equals(RING_4_BIOMES)) {
                own = i;
            } else if (hole < 0 && rule.contains("\"minecraft:hole\"")) {
                hole = i;
            }
        }
        assertTrue(own >= 0, "Nether ring 4 branch");
        assertTrue(own < hole, "before the hole rule");
        JsonArray branch = rules.get(own).getAsJsonObject().getAsJsonObject("then_run").getAsJsonArray("sequence");
        assertEquals("minecraft:block", branch.get(branch.size() - 1).getAsJsonObject().get("type").getAsString(), "catch-all");
        assertFalse(rules.get(own).toString().contains("\"minecraft:lava\""), "no lava from the surface");
        assertTrue(fallingBlocksAreGuarded(rules.get(own)) > 0, "the branch paints bone sand");
    }

    /** Blocks that fall: bone sand and its placeholder. */
    private static final List<String> FALLING = List.of("corruptedadventure:bone_sand", "minecraft:white_concrete_powder", "minecraft:sand");

    /**
     * Every falling block of the branch sits in a sequence right after a ceiling guard ({@code stone_depth} ceiling,
     * offset 0, not added: the block right above air gets a solid block instead), so no bone sand hangs above air.
     * Returns how many falling-block rules it found.
     */
    private static int fallingBlocksAreGuarded(JsonElement tree) {
        int found = 0;
        if (tree.isJsonObject()) {
            JsonObject object = tree.getAsJsonObject();
            if (object.has("sequence")) {
                JsonArray sequence = object.getAsJsonArray("sequence");
                for (int i = 0; i < sequence.size(); i++) {
                    JsonObject rule = sequence.get(i).getAsJsonObject();
                    if (isFallingBlock(rule)) {
                        found++;
                        assertTrue(i > 0, "a falling block without a guard before it");
                        JsonObject guard = sequence.get(i - 1).getAsJsonObject().getAsJsonObject("if_true");
                        assertTrue(guard != null && "minecraft:stone_depth".equals(guard.get("type").getAsString())
                                && "ceiling".equals(guard.get("surface_type").getAsString())
                                && guard.get("offset").getAsInt() == 0 && !guard.get("add_surface_depth").getAsBoolean(),
                                "falling block right after a ceiling guard: " + sequence.get(i - 1));
                    }
                }
            }
            for (var entry : object.entrySet()) {
                found += fallingBlocksAreGuarded(entry.getValue());
            }
        } else if (tree.isJsonArray()) {
            for (JsonElement element : tree.getAsJsonArray()) {
                found += fallingBlocksAreGuarded(element);
            }
        }
        return found;
    }

    private static boolean isFallingBlock(JsonObject rule) {
        return "minecraft:block".equals(rule.has("type") ? rule.get("type").getAsString() : null)
                && FALLING.contains(rule.getAsJsonObject("result_state").get("Name").getAsString());
    }

    /**
     * The dam ({@code k × (edge × gradient) + c}, a term of the shape's {@code max}, edge = Nether ring 4's
     * {@code ring_edge}, 1 on and beyond both boundaries) keeps its full strength up to Y {@code top} and stays solid there
     * against the lowest roughness the shape adds. Called by {@code Ring3JsonTest.everyNetherLavaStepIsCoveredBySolidGround}.
     */
    static void assertDamSolidUpTo(int top, String boundary) {
        JsonObject dam = WorldgenData.read(DIR + "dam.json");
        JsonObject scaled = dam.getAsJsonObject("argument1");
        double k = scaled.get("argument1").getAsDouble();
        double c = dam.get("argument2").getAsDouble();
        JsonObject product = scaled.getAsJsonObject("argument2");
        assertEquals(REF + "edge", product.get("argument1").getAsString());
        JsonObject gradient = product.getAsJsonObject("argument2");
        assertEquals("minecraft:y_clamped_gradient", gradient.get("type").getAsString());
        assertTrue(gradient.get("from_y").getAsInt() > top, boundary + ": Nether ring 4's dam weakens at or below Y " + top);
        JsonObject edge = WorldgenData.read(DIR + "edge.json");
        assertEquals("minecraft:flat_cache", edge.get("type").getAsString());
        edge = edge.getAsJsonObject("argument");
        assertEquals("corruptedadventure:ring_edge", edge.get("type").getAsString());
        assertEquals("ring_4", edge.get("ring").getAsString());
        assertEquals("minecraft:the_nether", edge.get("dimension").getAsString());
        assertShapeTermHolds("dam", k * gradient.get("from_value").getAsDouble() + c, boundary + ": Nether ring 4's dam can open at Y " + top);
        // No carvers cut it: the biomes have none (NetherRing4BiomesTest.noCarversAndNoFeaturesYet).
    }

    @Test
    void damCoversBothSteps() {
        assertDamSolidUpTo(32, "ring_4 | ring_5");
    }

    /**
     * {@code term} is a term of the shape's {@code max} and, at {@code strength}, stays solid against the lowest roughness
     * the shape adds ({@code (a + b × w) × noise + bias}, {@code w} in [0, 2]), sampled with the real (legacy-seeded) noise,
     * with a 25 % margin over the sampled peak.
     */
    private static void assertShapeTermHolds(String term, double strength, String message) {
        JsonObject shape = WorldgenData.read(DIR + "shape.json");
        assertEquals("minecraft:add", shape.get("type").getAsString());
        assertTrue(shape.getAsJsonObject("argument1").toString().contains("\"" + REF + term + "\""), term + " is a term of the max");
        assertEquals(REF + "rough", shape.get("argument2").getAsString());
        JsonObject rough = WorldgenData.read(DIR + "rough.json");
        JsonObject amplitude = rough.getAsJsonObject("argument1").getAsJsonObject("argument1");
        double a = amplitude.get("argument1").getAsDouble();
        double b = amplitude.getAsJsonObject("argument2").get("argument1").getAsDouble();
        double bias = rough.get("argument2").getAsDouble();
        JsonObject roughNoise = rough.getAsJsonObject("argument1").getAsJsonObject("argument2");
        assertEquals("corruptedadventure:nether_ring_4_rough", roughNoise.get("noise").getAsString());
        NormalNoise noise = NormalNoise.create(new LegacyRandomSource(SEED).forkPositional()
                .fromHashOf(ResourceLocation.parse("corruptedadventure:nether_ring_4_rough")), WorldgenData.noiseParameters("nether_ring_4_rough"));
        double yScale = roughNoise.get("y_scale").getAsDouble();
        double highest = 0.0;
        for (int x = -2100; x < 2100; x += 7) {
            for (int z = -2100; z < 2100; z += 7) {
                highest = Math.max(highest, Math.abs(noise.getValue(x, ((x * 31 + z) & 127) * yScale, z)));
            }
        }
        // the weights of spires and hollows never add up to more than 1, but bound them by 2 to be safe
        double lowest = strength - (a + 2.0 * Math.max(b, 0.0)) * highest * 1.25 + bias;
        assertTrue(lowest > 0.0, message + " (lowest density " + lowest + ")");
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            count++;
        }
        return count;
    }
}
