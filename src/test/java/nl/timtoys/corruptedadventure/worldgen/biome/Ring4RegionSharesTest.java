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
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
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
 * The ring 4 ground biomes in the preset's {@code multi_noise} (change 64): boxes on {@code weirdness} (= the band,
 * mythical below 0, blighted from 0 as {@code Ring4Band.blighted}), {@code temperature} (= {@code region_water}) and
 * {@code humidity} (= {@code region_growth}). Per band the biomes tile the water/growth plane and share it evenly on the
 * real region noises (mythical 1/5 each, blighted 1/3 each, ±3 points over a large area and three seeds).
 */
class Ring4RegionSharesTest {
    private static final List<String> MYTHIC = names(Ring4Biomes.MYTHIC);
    private static final List<String> BLIGHTED = names(Ring4Biomes.BLIGHTED);

    @Test
    void thePresetListsExactlyTheGroundBiomes() {
        Set<ResourceKey<Biome>> listed = new HashSet<>();
        for (Box box : boxes()) {
            listed.add(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(box.biome)));
        }
        assertEquals(Set.copyOf(Ring4Biomes.GROUND), listed);
        assertEquals(Ring4Biomes.GROUND.size() + 1, boxes().size(), "one box per biome and the mist fringe");
    }

    /**
     * Within the band the climate decides (band −1, the whole mythical band but its transition half); the mythical half
     * of the band's transition (−1 &lt; band &lt; 0, about 32 blocks) is always {@code mist_fjords}, so every boundary
     * between the bands has a mist fringe (V1 = A); from 0 on the blighted biomes.
     */
    @Test
    void eachBandTilesTheRegionPlane() {
        List<Box> boxes = boxes();
        for (double w = -1.2; w <= 1.2; w += 0.0137) {
            for (double g = -1.2; g <= 1.2; g += 0.0131) {
                assertTrue(MYTHIC.contains(only(boxes, w, g, -1.0)), w + ", " + g);
                for (double band : new double[]{-0.99, -0.5, -0.001}) {
                    assertEquals("mist_fjords", only(boxes, w, g, band), w + ", " + g + ", " + band);
                }
                for (double band : new double[]{0.0, 0.5, 1.0}) {
                    assertTrue(BLIGHTED.contains(only(boxes, w, g, band)), w + ", " + g + ", " + band);
                }
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
    void sharesOfTheStationaryNoiseAreEvenPerBand() {
        List<Box> boxes = boxes();
        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        for (long seed : new long[]{20260930L, 1L, 42L}) {
            Noises noises = new Noises(seed);
            for (int x = -64_000; x < 64_000; x += 286) {
                for (int z = -64_000; z < 64_000; z += 286) {
                    double w = noises.water.getValue(x, 0, z);
                    double g = noises.growth.getValue(x, 0, z);
                    counts.merge(only(boxes, w, g, -1.0), 1, Integer::sum);
                    counts.merge(only(boxes, w, g, 1.0), 1, Integer::sum);
                    total++;
                }
            }
        }
        StringBuilder shares = new StringBuilder();
        for (String biome : MYTHIC) {
            double share = counts.getOrDefault(biome, 0) / (double) total;
            shares.append(biome).append(' ').append(String.format("%.3f", share)).append("; ");
            assertEquals(1.0 / 5.0, share, 0.03, biome);
        }
        for (String biome : BLIGHTED) {
            double share = counts.getOrDefault(biome, 0) / (double) total;
            shares.append(biome).append(' ').append(String.format("%.3f", share)).append("; ");
            assertEquals(1.0 / 3.0, share, 0.03, biome);
        }
        System.out.println("Ring 4 shares per band: " + shares);
    }

    /**
     * On a raster over the real ring 4 (seed 20260930, default radii) with the real band: every ground biome occurs, the
     * mythical biomes only where {@code Ring4Band.blighted} is false and the blighted ones only where it is true, and
     * the mythical band holds about 60 % of the ring.
     */
    @Test
    void onTheRealRingEveryBiomeOccursInItsOwnBand() {
        long seed = 20260930L;
        RingLayout layout = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(seed));
        Ring4Band band = Ring4Band.of(layout);
        Noises noises = new Noises(seed);
        List<Box> boxes = boxes();
        Map<String, Integer> counts = new HashMap<>();
        int samples = 0;
        int mythic = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 360.0) {
            for (int distance = 6000; distance <= 8000; distance += 25) {
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                RingPosition position = layout.sampleOverworld(x, z);
                if (position.inTransition() || position.ring() != Ring.RING_4) {
                    continue;
                }
                String biome = only(boxes, noises.water.getValue(x, 0, z), noises.growth.getValue(x, 0, z), band.value(x, z));
                boolean blighted = band.blighted(x, z);
                assertEquals(blighted, BLIGHTED.contains(biome), biome + " at " + x + ", " + z);
                counts.merge(biome, 1, Integer::sum);
                samples++;
                mythic += blighted ? 0 : 1;
            }
        }
        for (String biome : MYTHIC) {
            assertTrue(counts.getOrDefault(biome, 0) > samples / 100, biome + " " + counts);
        }
        for (String biome : BLIGHTED) {
            assertTrue(counts.getOrDefault(biome, 0) > samples / 100, biome + " " + counts);
        }
        assertEquals(0.6, mythic / (double) samples, 0.08, "mythical share of ring 4 (without its zones)");
        System.out.println("Ring 4 on seed " + seed + ": " + counts);
    }

    /** The region noises as the random state seeds them: the world's positional factory, hashed with the noise key. */
    private static final class Noises {
        final NormalNoise water;
        final NormalNoise growth;

        Noises(long seed) {
            var factory = new XoroshiroRandomSource(seed).forkPositional();
            water = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:ring_4_water")),
                    WorldgenData.noiseParameters("ring_4_water"));
            growth = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:ring_4_growth")),
                    WorldgenData.noiseParameters("ring_4_growth"));
        }
    }

    /** The one box containing the point; fails if none or several do. */
    private static String only(List<Box> boxes, double water, double growth, double band) {
        List<String> hits = new ArrayList<>();
        for (Box box : boxes) {
            if (box.contains("temperature", water) && box.contains("humidity", growth) && box.contains("weirdness", band)) {
                hits.add(box.biome);
            }
        }
        assertEquals(1, hits.size(), water + ", " + growth + ", " + band + ": " + hits);
        return hits.getFirst().replace("corruptedadventure:", "");
    }

    private static List<String> names(List<ResourceKey<Biome>> keys) {
        return keys.stream().map(key -> key.location().getPath()).toList();
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

    static List<Box> boxes() {
        JsonObject ring4 = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").getAsJsonObject("biome_source")
                .getAsJsonObject("rings").getAsJsonObject("ring_4");
        assertEquals("minecraft:multi_noise", ring4.get("type").getAsString());
        List<Box> boxes = new ArrayList<>();
        for (JsonElement entry : ring4.getAsJsonArray("biomes")) {
            JsonObject object = entry.getAsJsonObject();
            boxes.add(new Box(object.get("biome").getAsString(), object.getAsJsonObject("parameters")));
        }
        return boxes;
    }
}
