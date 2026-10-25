package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G2 ring 2 flats ({@link Ring2FlatsLifeFeatures}): the shares of the stationary noises at the chosen thresholds (three
 * seeds, a large area) are the decided ones, and the surface rules ({@code tools/worldgen/ring2_flats_surface.py}) use
 * the same thresholds as the Java.
 */
class Ring2FlatsLifeTest {
    @Test
    void redDunesCoverAQuarterOfTheDunes() {
        assertEquals(0.25, share("ring_2_red_dunes", v -> v >= Ring2FlatsLifeFeatures.RED_DUNES_FROM ? 1 : 0), 0.04);
    }

    @Test
    void pressureRidgesCoverAboutFifteenPercentOfTheFlats() {
        assertEquals(0.15, share("ring_2_salt_ridges", v -> v >= Ring2FlatsLifeFeatures.RIDGES_FROM ? 1 : 0), 0.03);
    }

    @Test
    void palmGrovesCoverAboutFifteenPercentOfTheOpenSand() {
        assertEquals(0.15, share("ring_2_oasis_groves", v -> v >= Ring2FlatsLifeFeatures.GROVE_GRASS_FROM ? 1 : 0), 0.04);
    }

    @Test
    void clusteredCrystalsAreAboutAsManyAsBefore() {
        // Before: a grid of 8, chance 0.25 (one try per chunk). Now a grid of 4 at chance 0.5 thinned by the clusters.
        ClusterDensity density = ClusterDensity.of(Ring2FlatsLifeFeatures.SALT_CRYSTAL_CLUSTERS,
                Ring2FlatsLifeFeatures.CRYSTALS_LOW, Ring2FlatsLifeFeatures.CRYSTALS_HIGH);
        double perChunk = 16 * 0.5 * share("ring_2_salt_crystal_clusters", density::share);
        assertEquals(1.0, perChunk, 0.25);
    }

    @Test
    void thicketsAndScrubArePatchesNotEverywhere() {
        double thickets = share("ring_2_dune_thickets", v -> v >= Ring2FlatsLifeFeatures.THICKETS_LOW ? 1 : 0);
        double scrub = share("ring_2_oasis_scrub", v -> v >= Ring2FlatsLifeFeatures.SCRUB_LOW ? 1 : 0);
        assertTrue(thickets > 0.25 && thickets < 0.5, "thickets " + thickets);
        assertTrue(scrub > 0.25 && scrub < 0.5, "scrub " + scrub);
    }

    @Test
    void theSurfaceRulesUseTheJavaThresholds() {
        JsonObject settings = WorldgenData.read("noise_settings/overworld.json");
        assertEquals(List.of(Ring2FlatsLifeFeatures.RED_FRINGE_FROM, Ring2FlatsLifeFeatures.RED_DUNES_FROM),
                thresholds(settings, "ring_2_red_dunes").stream().distinct().toList());
        assertEquals(List.of(Ring2FlatsLifeFeatures.GROVE_GRASS_FROM), thresholds(settings, "ring_2_oasis_groves"));
        assertEquals(List.of(Ring2FlatsLifeFeatures.SCRUB_LOW), thresholds(settings, "ring_2_oasis_scrub"));
    }

    /** The mean of {@code f} over the noise of three seeds, sampled far apart. */
    private static double share(String name, DoubleUnaryOperator f) {
        NormalNoise.NoiseParameters parameters = WorldgenData.noiseParameters(name);
        double sum = 0;
        int count = 0;
        for (long seed : new long[]{20260930L, 1L, 42L}) {
            // As the random state seeds a noise: the world's positional factory, hashed with the noise key.
            var factory = new XoroshiroRandomSource(seed).forkPositional();
            NormalNoise noise = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:" + name)), parameters);
            for (int x = 0; x < 300; x++) {
                for (int z = 0; z < 300; z++) {
                    sum += f.applyAsDouble(noise.getValue(x * 97.3, 0, z * 97.3));
                    count++;
                }
            }
        }
        return sum / count;
    }

    /** The minimum thresholds of every noise_threshold condition on {@code noise} in the surface rules. */
    private static List<Double> thresholds(JsonObject settings, String noise) {
        List<Double> out = new ArrayList<>();
        collect(settings.get("surface_rule"), "corruptedadventure:" + noise, out);
        return out;
    }

    private static void collect(JsonElement element, String noise, List<Double> out) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("type") && object.get("type").getAsString().equals("minecraft:noise_threshold")
                    && object.get("noise").getAsString().equals(noise)) {
                out.add(object.get("min_threshold").getAsDouble());
            }
            object.entrySet().forEach(entry -> collect(entry.getValue(), noise, out));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collect(child, noise, out));
        }
    }
}
