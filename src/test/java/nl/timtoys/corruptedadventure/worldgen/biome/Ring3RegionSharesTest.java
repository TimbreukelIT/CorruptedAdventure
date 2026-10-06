package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ring 3 biomes in the preset's {@code multi_noise} (change 39b): boxes on {@code temperature} (= {@code region_heat}),
 * {@code humidity} (= the router's vegetation, {@code region_ground}) and {@code weirdness} (= {@code caldera_dormant}).
 * The seven sub-biomes tile the heat/ground plane at weirdness 0, the dormant caldera takes weirdness ≥ 0.5 everywhere.
 * With the region noises the seven share the ring evenly (1/7 each, ±3 points over a large area and three seeds); one
 * world deviates more, ring 3 holds only ~100 regions.
 */
class Ring3RegionSharesTest {
    private static final List<String> SUB_BIOMES = List.of("ashen_wastes", "volcanic_peaks", "ember_woods", "basalt_flows",
            "sulfur_fields", "dried_seabed", "titan_fields");

    @Test
    void thePresetListsExactlyTheRing3Biomes() {
        Set<ResourceKey<Biome>> listed = new HashSet<>();
        for (Box box : boxes()) {
            listed.add(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(box.biome)));
        }
        assertEquals(Set.copyOf(Ring3Biomes.ALL), listed);
        assertEquals(Ring3Biomes.ALL.size(), boxes().size(), "one box per biome");
    }

    @Test
    void theSubBiomesTileTheRegionPlaneAndTheCalderaTakesHighWeirdness() {
        List<Box> boxes = boxes();
        for (double h = -1.2; h <= 1.2; h += 0.0137) {
            for (double g = -1.2; g <= 1.2; g += 0.0131) {
                assertTrue(SUB_BIOMES.contains(only(boxes, h, g, 0.0)), h + ", " + g);
                assertEquals("dormant_caldera", only(boxes, h, g, 1.0), h + ", " + g);
            }
        }
        for (Box box : boxes) {
            for (String unused : new String[]{"continentalness", "erosion", "depth"}) {
                assertEquals(-2.0, box.range(unused)[0], box.biome + " " + unused);
                assertEquals(2.0, box.range(unused)[1], box.biome + " " + unused);
            }
        }
    }

    @Test
    void sharesOfTheStationaryNoiseAreEven() {
        List<Box> boxes = boxes();
        NormalNoise.NoiseParameters heat = WorldgenData.noiseParameters("ring_3_heat");
        NormalNoise.NoiseParameters ground = WorldgenData.noiseParameters("ring_3_ground");
        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        for (long seed : new long[]{20260930L, 1L, 42L}) {
            // As the random state seeds a noise: the world's positional factory, hashed with the noise key.
            var factory = new XoroshiroRandomSource(seed).forkPositional();
            NormalNoise h = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:ring_3_heat")), heat);
            NormalNoise g = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:ring_3_ground")), ground);
            // 200,000 points, far enough apart to sample many independent regions.
            for (int x = -64_000; x < 64_000; x += 286) {
                for (int z = -64_000; z < 64_000; z += 286) {
                    counts.merge(only(boxes, h.getValue(x, 0, z), g.getValue(x, 0, z), 0.0), 1, Integer::sum);
                    total++;
                }
            }
        }
        StringBuilder shares = new StringBuilder();
        for (String biome : SUB_BIOMES) {
            double share = counts.getOrDefault(biome, 0) / (double) total;
            shares.append(biome).append(' ').append(String.format("%.3f", share)).append("; ");
            assertEquals(1.0 / 7.0, share, 0.03, biome);
        }
        System.out.println("Ring 3 shares: " + shares);
    }

    /** The one box containing the point; fails if none or several do (borders aside, the sample grid avoids them). */
    private static String only(List<Box> boxes, double heat, double ground, double weirdness) {
        List<String> hits = new ArrayList<>();
        for (Box box : boxes) {
            if (box.contains("temperature", heat) && box.contains("humidity", ground) && box.contains("weirdness", weirdness)) {
                hits.add(box.biome);
            }
        }
        assertEquals(1, hits.size(), heat + ", " + ground + ", " + weirdness + ": " + hits);
        return hits.getFirst().replace("corruptedadventure:", "");
    }

    private record Box(String biome, JsonObject parameters) {
        double[] range(String axis) {
            JsonArray range = parameters.getAsJsonArray(axis);
            return new double[]{range.get(0).getAsDouble(), range.get(1).getAsDouble()};
        }

        /** Half-open, so neighbours that share a border do not overlap. */
        boolean contains(String axis, double value) {
            double[] range = range(axis);
            return range[0] <= value && value < range[1];
        }
    }

    private static List<Box> boxes() {
        JsonObject ring3 = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").getAsJsonObject("biome_source")
                .getAsJsonObject("rings").getAsJsonObject("ring_3");
        assertEquals("minecraft:multi_noise", ring3.get("type").getAsString());
        List<Box> boxes = new ArrayList<>();
        for (JsonElement entry : ring3.getAsJsonArray("biomes")) {
            JsonObject object = entry.getAsJsonObject();
            boxes.add(new Box(object.get("biome").getAsString(), object.getAsJsonObject("parameters")));
        }
        return boxes;
    }
}
