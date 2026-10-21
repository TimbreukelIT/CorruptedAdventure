package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
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
 * The ring 5 biomes in the preset's {@code multi_noise} (package 86, V12-A; {@code golf-1-biomes.md} §2): boxes on
 * {@code weirdness} (= the band, inner below 0, outer from 0 as {@code Ring5Band.outer}), in the inner band on
 * {@code humidity} (= {@code region_glitch}) and {@code temperature} (= {@code region_rot}), in the outer band on
 * {@code erosion} (= {@code breakup}) and {@code temperature}. The thresholds are the quantiles of the ring 5 region noises
 * (P1: inner glitch_fields ~33 %, false_meadow ~13 %, corrupted_wastes ~54 %; outer shattered_expanse from breakup 0.6, the
 * rest half void_scars, half inverted_hills).
 * <p>
 * The boxes are checked in vanilla's own quantized parameter space ({@link Climate.Parameter#span}, the codec's
 * {@code (long) (value · 10000)}): neighbours are adjacent integers, never sharing a value. A shared value would be a
 * tie, and vanilla's lookup breaks ties by its thread's previous result: not deterministic.
 */
class Ring5RegionSharesTest {
    /** humidity (region_glitch) from here: glitch_fields. */
    static final double GLITCH = 0.125;
    /** temperature (region_rot) from here, below {@link #GLITCH}: false_meadow. */
    static final double MEADOW = 0.244;
    /** erosion (breakup) from here, outer band: shattered_expanse. */
    static final double SHATTERED = 0.6;
    /** temperature (region_rot) from here, outer band below {@link #SHATTERED}: inverted_hills; below it void_scars. */
    static final double HILLS = 0.0;
    private static final String[] AXES = {"temperature", "humidity", "continentalness", "erosion", "weirdness", "depth"};

    @Test
    void thePresetListsExactlyTheRing5Biomes() {
        Set<ResourceKey<Biome>> listed = new HashSet<>();
        for (Box box : boxes()) {
            listed.add(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(box.biome)));
            for (String unused : new String[]{"continentalness", "depth"}) {
                assertEquals(Climate.Parameter.span(-2.0F, 2.0F), box.parameter(unused), box.biome + " " + unused);
            }
            assertEquals(0.0F, box.offset, box.biome);
        }
        assertEquals(Set.copyOf(Ring5Biomes.ALL), listed);
        assertEquals(Ring5Biomes.ALL.size(), boxes().size(), "one box per biome");
    }

    /**
     * Every point of the plane, the thresholds and their neighbours included, lies in exactly one quantized box, and
     * vanilla's lookup gives the biome of the rule of §2.
     */
    @Test
    void theBoxesTileThePlaneExactlyOnce() {
        List<Box> boxes = boxes();
        Climate.ParameterList<String> lookup = lookup(boxes);
        double[] temperatures = values(-2.0, 2.0, 0.0371, MEADOW, HILLS, -2.0);
        double[] humidities = values(-2.0, 2.0, 0.0419, GLITCH);
        double[] erosions = values(-2.0, 2.0, 0.0797, SHATTERED, 0.0);
        double[] bands = {-2.0, -1.0, -0.5, -0.0002, -0.0001, 0.0, 0.0001, 0.5, 1.0, 2.0};
        for (double t : temperatures) {
            for (double h : humidities) {
                for (double e : erosions) {
                    for (double w : bands) {
                        long[] q = {Climate.quantizeCoord((float) t), Climate.quantizeCoord((float) h), 0L, Climate.quantizeCoord((float) e),
                                Climate.quantizeCoord((float) w), 0L};
                        List<String> hits = new ArrayList<>();
                        for (Box box : boxes) {
                            if (box.contains(q)) {
                                hits.add(box.biome);
                            }
                        }
                        String at = t + ", " + h + ", " + e + ", " + w;
                        assertEquals(1, hits.size(), at + ": " + hits);
                        String expected = expected((float) t, (float) h, (float) e, (float) w);
                        assertEquals(expected, hits.getFirst(), at);
                        assertEquals(expected, lookup.findValue(Climate.target((float) t, (float) h, 0.0F, (float) e, 0.0F, (float) w)), at);
                    }
                }
            }
        }
    }

    /** V12-A with the pinnacle axis of 84: the pinnacle's plateau (temperature −2, erosion 0) is void_scars, whatever the humidity. */
    @Test
    void thePinnaclePlateauIsVoidScars() {
        Climate.ParameterList<String> lookup = lookup(boxes());
        for (float band : new float[]{0.0F, 0.3F, 1.0F}) {
            for (float humidity : new float[]{-1.0F, 0.0F, 0.5F, 1.0F}) {
                assertEquals("void_scars", lookup.findValue(Climate.target(-2.0F, humidity, 0.0F, 0.0F, 0.0F, band)).replace("corruptedadventure:", ""));
            }
        }
    }

    /**
     * The thresholds are the quantiles of the ring 5 region noises (5 seeds): region_glitch's 2/3 (glitch_fields a third),
     * region_rot's 0.8 (false_meadow a fifth of the rest) and its median (the outer band's half), each within 0.02.
     */
    @Test
    void theThresholdsAreTheQuantilesOfTheRing5Noises() {
        List<Double> rot = new ArrayList<>();
        List<Double> glitch = new ArrayList<>();
        for (long seed : new long[]{20260930L, 1L, 42L, 7L, 123456789L}) {
            Noises noises = new Noises(seed);
            for (int x = -64_000; x < 64_000; x += 401) {
                for (int z = -64_000; z < 64_000; z += 401) {
                    rot.add(noises.rot.getValue(x, 0, z));
                    glitch.add(noises.glitch.getValue(x, 0, z));
                }
            }
        }
        double glitchQ67 = quantile(glitch, 2.0 / 3.0);
        double rotQ80 = quantile(rot, 0.8);
        double rotQ50 = quantile(rot, 0.5);
        System.out.printf("Ring 5 quantiles: region_glitch q67 %.4f, region_rot q80 %.4f, q50 %.4f%n", glitchQ67, rotQ80, rotQ50);
        assertEquals(GLITCH, glitchQ67, 0.02, "region_glitch q67");
        assertEquals(MEADOW, rotQ80, 0.02, "region_rot q80");
        assertEquals(HILLS, rotQ50, 0.02, "region_rot q50");
    }

    /** P1 on the real noises (3 seeds, ±3 points): inner 33 / 13 / 54, outer below breakup 0.6 half and half. */
    @Test
    void sharesOfTheRealNoisesAreTheProposedOnes() {
        Climate.ParameterList<String> lookup = lookup(boxes());
        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        for (long seed : new long[]{20260930L, 1L, 42L}) {
            Noises noises = new Noises(seed);
            for (int x = -64_000; x < 64_000; x += 286) {
                for (int z = -64_000; z < 64_000; z += 286) {
                    float t = (float) noises.rot.getValue(x, 0, z);
                    float h = (float) noises.glitch.getValue(x, 0, z);
                    counts.merge(name(lookup.findValue(Climate.target(t, h, 0.0F, 0.0F, 0.0F, -1.0F))), 1, Integer::sum);
                    counts.merge(name(lookup.findValue(Climate.target(t, h, 0.0F, 0.0F, 0.0F, 1.0F))), 1, Integer::sum);
                    total++;
                }
            }
        }
        Map<String, Double> expected = Map.of("glitch_fields", 1.0 / 3.0, "false_meadow", 2.0 / 15.0, "corrupted_wastes", 8.0 / 15.0,
                "void_scars", 0.5, "inverted_hills", 0.5);
        for (Map.Entry<String, Double> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), counts.getOrDefault(entry.getKey(), 0) / (double) total, 0.03, entry.getKey());
        }
        System.out.println("Ring 5 shares per band (breakup 0): " + counts + " of " + total);
    }

    /**
     * On a raster over the real ring 5 (seed 20260930, default radii) with the real band and noises and a local breakup
     * (84's is a stub in golf 1: a ramp from 0 on the band line to 1 at the void edge): the inner biomes only where
     * {@code Ring5Band.inner}, the outer ones only where {@code outer}, every biome occurs.
     */
    @Test
    void onTheRealRingEveryBiomeOccursInItsOwnBand() {
        long seed = 20260930L;
        RingLayout layout = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(seed));
        Ring5Band band = Ring5Band.of(layout);
        Noises noises = new Noises(seed);
        Climate.ParameterList<String> lookup = lookup(boxes());
        Set<String> inner = Set.copyOf(names(Ring5Biomes.INNER));
        Map<String, Integer> counts = new HashMap<>();
        int samples = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 360.0) {
            for (int distance = 8000; distance <= 9700; distance += 25) {
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                RingPosition position = layout.sampleOverworld(x, z);
                if (position.inTransition() || position.ring() != Ring.RING_5) {
                    continue;
                }
                double value = band.value(x, z);
                if (Math.abs(value) < 0.0001) {
                    continue;   // the quantization's sliver around the line, far below a block
                }
                float breakup = (float) Math.clamp((distance - 8850.0) / (9700.0 - 8850.0), 0.0, 1.0) * Math.max(0.0F, (float) value);
                String biome = name(lookup.findValue(Climate.target((float) noises.rot.getValue(x, 0, z), (float) noises.glitch.getValue(x, 0, z),
                        0.0F, breakup, 0.0F, (float) value)));
                assertEquals(band.inner(x, z), inner.contains(biome), biome + " at " + x + ", " + z + " (band " + value + ")");
                counts.merge(biome, 1, Integer::sum);
                samples++;
            }
        }
        for (String biome : names(Ring5Biomes.ALL)) {
            assertTrue(counts.getOrDefault(biome, 0) > samples / 100, biome + " " + counts);
        }
        System.out.println("Ring 5 on seed " + seed + " (local breakup): " + counts);
    }

    /**
     * The rule of §2, written out on the quantized values the game compares (the codec truncates towards zero, so a
     * band of −0.00005 is already outer: far below a block).
     */
    private static String expected(float temperature, float humidity, float erosion, float band) {
        if (Climate.quantizeCoord(band) < 0L) {
            return atLeast(humidity, GLITCH) ? "corruptedadventure:glitch_fields"
                    : atLeast(temperature, MEADOW) ? "corruptedadventure:false_meadow" : "corruptedadventure:corrupted_wastes";
        }
        return atLeast(erosion, SHATTERED) ? "corruptedadventure:shattered_expanse"
                : atLeast(temperature, HILLS) ? "corruptedadventure:inverted_hills" : "corruptedadventure:void_scars";
    }

    private static boolean atLeast(float value, double threshold) {
        return Climate.quantizeCoord(value) >= Climate.quantizeCoord((float) threshold);
    }

    /** A raster from {@code from} to {@code to}, plus each threshold, its quantized neighbours and ±0.00005. */
    private static double[] values(double from, double to, double step, double... thresholds) {
        List<Double> values = new ArrayList<>();
        for (double v = from; v <= to; v += step) {
            values.add(v);
        }
        values.add(to);
        for (double threshold : thresholds) {
            for (double delta : new double[]{-0.0002, -0.0001, -0.00005, 0.0, 0.00005, 0.0001, 0.0002}) {
                values.add(threshold + delta);
            }
        }
        return values.stream().mapToDouble(Double::doubleValue).filter(v -> v >= -2.0 && v <= 2.0).toArray();
    }

    private static double quantile(List<Double> values, double q) {
        double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        return sorted[(int) Math.floor(q * (sorted.length - 1))];
    }

    private static String name(String biome) {
        return biome.replace("corruptedadventure:", "");
    }

    private static List<String> names(List<ResourceKey<Biome>> keys) {
        return keys.stream().map(key -> key.location().getPath()).toList();
    }

    /** Vanilla's lookup of a {@code multi_noise} source, on the preset's boxes. */
    private static Climate.ParameterList<String> lookup(List<Box> boxes) {
        List<Pair<Climate.ParameterPoint, String>> points = new ArrayList<>();
        for (Box box : boxes) {
            points.add(Pair.of(Climate.parameters(box.parameter("temperature"), box.parameter("humidity"), box.parameter("continentalness"),
                    box.parameter("erosion"), box.parameter("depth"), box.parameter("weirdness"), box.offset), box.biome));
        }
        return new Climate.ParameterList<>(points);
    }

    /** The region noises as the random state seeds them: the world's positional factory, hashed with the noise key. */
    private static final class Noises {
        final NormalNoise rot;
        final NormalNoise glitch;

        Noises(long seed) {
            var factory = new XoroshiroRandomSource(seed).forkPositional();
            rot = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:ring_5_rot")),
                    WorldgenData.noiseParameters("ring_5_rot"));
            glitch = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:ring_5_glitch")),
                    WorldgenData.noiseParameters("ring_5_glitch"));
        }
    }

    /** One box of the preset, its ranges as the codec reads them: floats, quantized by {@link Climate.Parameter#span}. */
    private record Box(String biome, Map<String, Climate.Parameter> ranges, float offset) {
        Climate.Parameter parameter(String axis) {
            return ranges.get(axis);
        }

        /** Whether the quantized point (in the order of {@link #AXES}) lies in the box. */
        boolean contains(long[] point) {
            for (int i = 0; i < AXES.length; i++) {
                Climate.Parameter parameter = ranges.get(AXES[i]);
                if (point[i] < parameter.min() || point[i] > parameter.max()) {
                    return false;
                }
            }
            return true;
        }
    }

    static List<Box> boxes() {
        JsonObject ring5 = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").getAsJsonObject("biome_source")
                .getAsJsonObject("rings").getAsJsonObject("ring_5");
        assertEquals("minecraft:multi_noise", ring5.get("type").getAsString());
        List<Box> boxes = new ArrayList<>();
        for (JsonElement entry : ring5.getAsJsonArray("biomes")) {
            JsonObject object = entry.getAsJsonObject();
            JsonObject parameters = object.getAsJsonObject("parameters");
            Map<String, Climate.Parameter> ranges = new HashMap<>();
            for (String axis : AXES) {
                assertTrue(parameters.has(axis), object.get("biome") + " " + axis);
                JsonArray range = parameters.getAsJsonArray(axis);
                ranges.put(axis, Climate.Parameter.span(range.get(0).getAsFloat(), range.get(1).getAsFloat()));
            }
            boxes.add(new Box(object.get("biome").getAsString(), ranges, parameters.get("offset").getAsFloat()));
        }
        return boxes;
    }
}
