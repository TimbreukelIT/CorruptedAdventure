package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The five ring 2 biomes on the two region axes ({@code relief}, {@code moisture}), with the noise parameters and the
 * thresholds from the JSON: the shares of the stationary noise (a large area, three seeds) are those of change 30
 * (canyonlands 30 %, hoodoo fields 20 %, dune sea 25 %, salt flats 13 %, oasis 12 %; ±3 points). One world deviates more:
 * ring 2 holds only a few dozen regions (seed 20260930: rocky 54 %). The terrain weights switch on the same thresholds.
 */
class Ring2RegionSharesTest {
    private static final String WORLDGEN = "src/main/resources/data/corruptedadventure/worldgen";
    private static final Map<String, Double> TARGET = Map.of("canyonlands", 0.30, "hoodoo_fields", 0.20, "dune_sea", 0.25,
            "salt_flats", 0.13, "oasis_flats", 0.12);

    @Test
    void sharesOfTheStationaryNoiseAreTheTargets() throws IOException {
        Path root = findRoot();
        Thresholds t = thresholds(root);
        NormalNoise.NoiseParameters relief = parameters(root, "ring_2_relief");
        NormalNoise.NoiseParameters moisture = parameters(root, "ring_2_moisture");
        double reliefOffset = reliefOffset(root);
        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        for (long seed : new long[]{20260930L, 1L, 42L}) {
            // As the random state seeds a noise: the world's positional factory, hashed with the noise key.
            var factory = new XoroshiroRandomSource(seed).forkPositional();
            NormalNoise r = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:ring_2_relief")), relief);
            NormalNoise m = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:ring_2_moisture")), moisture);
            // 200,000 points, far enough apart to sample many independent regions.
            for (int x = -44_000; x < 44_000; x += 196) {
                for (int z = -44_000; z < 44_000; z += 196) {
                    counts.merge(biome(t, r.getValue(x, 0, z) + reliefOffset, m.getValue(x, 0, z)), 1, Integer::sum);
                    total++;
                }
            }
        }
        StringBuilder shares = new StringBuilder();
        for (Map.Entry<String, Double> target : TARGET.entrySet()) {
            double share = counts.getOrDefault(target.getKey(), 0) / (double) total;
            shares.append(target.getKey()).append(' ').append(String.format("%.3f", share)).append("; ");
            assertEquals(target.getValue(), share, 0.03, target.getKey());
        }
        System.out.println("Ring 2 shares: " + shares);
    }

    @Test
    void terrainWeightsSwitchOnTheBiomeThresholds() throws IOException {
        Path root = findRoot();
        Thresholds t = thresholds(root);
        assertEquals(0.0, t.relief, "the relief offset carries T; the border stays exact where the zones mix relief with 0");
        assertCentredOn(root, "hoodoo_weight", t.hoodoo);
        assertCentredOn(root, "salt_weight", t.salt);
        assertCentredOn(root, "dune_weight", t.salt);
        assertCentredOn(root, "dune_weight", t.oasis);
    }

    private static String biome(Thresholds t, double relief, double moisture) {
        if (relief >= t.relief) {
            return moisture >= t.hoodoo ? "canyonlands" : "hoodoo_fields";
        }
        return moisture < t.salt ? "salt_flats" : moisture < t.oasis ? "dune_sea" : "oasis_flats";
    }

    /** The pair of spline points around {@code threshold} is centred on it and goes through 0.5 there. */
    private static void assertCentredOn(Path root, String name, double threshold) throws IOException {
        JsonObject function = read(root.resolve("density_function/ring_2/overworld/" + name + ".json"));
        JsonArray points = function.getAsJsonObject("argument").getAsJsonObject("spline").getAsJsonArray("points");
        for (int i = 0; i + 1 < points.size(); i++) {
            JsonObject a = points.get(i).getAsJsonObject();
            JsonObject b = points.get(i + 1).getAsJsonObject();
            double lo = a.get("location").getAsDouble();
            double hi = b.get("location").getAsDouble();
            if (lo < threshold && threshold < hi) {
                assertEquals(threshold, (lo + hi) / 2.0, 1e-6, name);
                assertEquals(0.5, (a.get("value").getAsDouble() + b.get("value").getAsDouble()) / 2.0, 1e-6, name);
                return;
            }
        }
        fail(name + " has no step around " + threshold);
    }

    private record Thresholds(double relief, double hoodoo, double salt, double oasis) {
    }

    private static Thresholds thresholds(Path root) throws IOException {
        JsonObject preset = read(root.resolve("world_preset/corrupted_world.json"));
        JsonArray biomes = preset.getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
                .getAsJsonObject("biome_source").getAsJsonObject("rings").getAsJsonObject("ring_2").getAsJsonArray("biomes");
        Map<String, JsonObject> byName = new HashMap<>();
        for (JsonElement entry : biomes) {
            byName.put(entry.getAsJsonObject().get("biome").getAsString().replace("corruptedadventure:", ""),
                    entry.getAsJsonObject().getAsJsonObject("parameters"));
        }
        assertEquals(TARGET.keySet(), byName.keySet());
        double t = byName.get("canyonlands").getAsJsonArray("temperature").get(0).getAsDouble();
        double hoodoo = byName.get("canyonlands").getAsJsonArray("humidity").get(0).getAsDouble();
        double salt = byName.get("salt_flats").getAsJsonArray("humidity").get(1).getAsDouble();
        double oasis = byName.get("oasis_flats").getAsJsonArray("humidity").get(0).getAsDouble();
        // The partition is complete: neighbours share their border.
        assertEquals(t, byName.get("hoodoo_fields").getAsJsonArray("temperature").get(0).getAsDouble());
        assertEquals(hoodoo, byName.get("hoodoo_fields").getAsJsonArray("humidity").get(1).getAsDouble());
        assertEquals(salt, byName.get("dune_sea").getAsJsonArray("humidity").get(0).getAsDouble());
        assertEquals(oasis, byName.get("dune_sea").getAsJsonArray("humidity").get(1).getAsDouble());
        for (String flat : new String[]{"salt_flats", "dune_sea", "oasis_flats"}) {
            assertEquals(t, byName.get(flat).getAsJsonArray("temperature").get(1).getAsDouble(), flat);
        }
        assertTrue(salt < oasis);
        return new Thresholds(t, hoodoo, salt, oasis);
    }

    /** {@code relief.json} is a flat cache of the noise, optionally plus a constant. */
    private static double reliefOffset(Path root) throws IOException {
        JsonObject argument = read(root.resolve("density_function/ring_2/overworld/relief.json")).getAsJsonObject("argument");
        if (argument.get("type").getAsString().equals("minecraft:add")) {
            return argument.get("argument1").getAsDouble();
        }
        assertEquals("minecraft:noise", argument.get("type").getAsString());
        return 0.0;
    }

    private static NormalNoise.NoiseParameters parameters(Path root, String name) throws IOException {
        JsonObject json = read(root.resolve("noise/" + name + ".json"));
        JsonArray amplitudes = json.getAsJsonArray("amplitudes");
        double[] rest = new double[amplitudes.size() - 1];
        for (int i = 1; i < amplitudes.size(); i++) {
            rest[i - 1] = amplitudes.get(i).getAsDouble();
        }
        return new NormalNoise.NoiseParameters(json.get("firstOctave").getAsInt(), amplitudes.get(0).getAsDouble(), rest);
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
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
