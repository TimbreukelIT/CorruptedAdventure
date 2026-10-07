package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing2Features;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ring 3 keys of golf 0 ({@code docs/plans/ring-3/contract.md} §4): region axes in the router, the shared
 * density functions every golf-1 stream loads, the third router axis and the Nether-3 fluid level. Only the wiring and
 * the region noises are fixed here; the owners fill the stubs in golf 1.
 */
class Ring3JsonTest {
    private static final long SEED = 20260930L;
    private static final String[] SHARED = {"ring_3/overworld/region_heat", "ring_3/overworld/region_ground",
            "ring_3/overworld/height", "ring_3/overworld/caldera_mask", "ring_3/overworld/caldera_dormant",
            "ring_3/overworld/lava_channel", "ring_3/overworld/island_mask", "ring_3/nether/region", "ring_3/nether/terrain"};
    private static JsonObject blend(String file) {
        JsonObject json = WorldgenData.read("density_function/ring_blend/" + file + ".json");
        assertEquals("corruptedadventure:ring_blend", json.get("type").getAsString(), file);
        return json;
    }

    private static JsonObject rings(String file) {
        return blend(file).getAsJsonObject("rings");
    }

    private static List<String> switchAt(String file) {
        JsonArray boundaries = blend(file).getAsJsonArray("switch_at");
        return boundaries == null ? List.of() : boundaries.asList().stream().map(JsonElement::getAsString).toList();
    }

    private static JsonArray multiNoise(String dimension, String ring) {
        return WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions").getAsJsonObject(dimension)
                .getAsJsonObject("generator").getAsJsonObject("biome_source").getAsJsonObject("rings").getAsJsonObject(ring)
                .getAsJsonArray("biomes");
    }

    @Test
    void everySharedDensityFunctionOfTheContractExists() {
        for (String name : SHARED) {
            assertTrue(Files.exists(WorldgenData.root().resolve("density_function/" + name + ".json")), name);
        }
        assertEquals("corruptedadventure:ring_3/nether/terrain", rings("nether").get("ring_3").getAsString());
    }

    @Test
    void routerClimateOfRing3IsTheRegionAxes() {
        JsonObject temperature = rings("overworld_temperature");
        JsonObject vegetation = rings("overworld_vegetation");
        assertEquals("corruptedadventure:ring_3/overworld/region_heat", temperature.get("ring_3").getAsString());
        assertEquals("corruptedadventure:ring_3/overworld/region_ground", vegetation.get("ring_3").getAsString());
        // Ring 1 and 2 stay as they were (change 23/30).
        assertEquals("minecraft:temperature", temperature.getAsJsonObject("ring_1").get("noise").getAsString());
        assertEquals("minecraft:vegetation", vegetation.getAsJsonObject("ring_1").get("noise").getAsString());
        assertEquals("corruptedadventure:ring_2/overworld/relief", temperature.get("ring_2").getAsString());
        assertEquals("corruptedadventure:ring_2/overworld/moisture", vegetation.get("ring_2").getAsString());
        for (JsonObject blend : new JsonObject[]{temperature, vegetation}) {
            for (String ring : new String[]{"ring_4", "ring_5", "void"}) {
                assertEquals(0.0, blend.get(ring).getAsDouble(), ring);
            }
        }
    }

    /**
     * The climate switches hard (no mix) at every boundary without a transition biome, so each side's biomes read its
     * own axes. Compared with the preset's {@code transitions}: a new transition biome (e.g. {@code steam_fens} as
     * {@code transitions.ring_3}) makes this fail until the boundary leaves {@code switch_at}. One deliberate exception:
     * Nether 1→2 keeps change 35's mix (approved ring 2 generation; backlog).
     */
    @Test
    void climateSwitchesExactlyAtBoundariesWithoutATransitionBiome() {
        for (String file : new String[]{"overworld_temperature", "overworld_vegetation", "overworld_weirdness"}) {
            assertEquals(withoutTransition("minecraft:overworld", Set.of()), Set.copyOf(switchAt(file)), file);
        }
        assertEquals(withoutTransition("minecraft:the_nether", Set.of("ring_1")), Set.copyOf(switchAt("nether_temperature")));
        // Terrain and the other router fields keep mixing.
        for (String file : new String[]{"overworld", "nether", "overworld_initial", "overworld_vein_toggle", "overworld_floodedness", "overworld_erosion"}) {
            assertEquals(List.of(), switchAt(file), file);
        }
    }

    /** Boundaries (keyed by their inner ring, void has none) without a transition biome in the preset, minus {@code mixing}. */
    private static Set<String> withoutTransition(String dimension, Set<String> mixing) {
        JsonObject transitions = generator(dimension).getAsJsonObject("biome_source").getAsJsonObject("transitions");
        Set<String> boundaries = new HashSet<>(Set.of("ring_1", "ring_2", "ring_3", "ring_4", "ring_5"));
        if (transitions != null) {
            boundaries.removeAll(transitions.keySet());
        }
        boundaries.removeAll(mixing);
        return boundaries;
    }

    private static JsonObject generator(String dimension) {
        return WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions").getAsJsonObject(dimension)
                .getAsJsonObject("generator");
    }

    @Test
    void netherTemperatureIsTheRegionOfEachRing() {
        JsonObject temperature = rings("nether_temperature");
        assertEquals("minecraft:temperature", temperature.getAsJsonObject("ring_1").get("noise").getAsString());
        assertEquals(1.0, temperature.getAsJsonObject("ring_1").get("xz_scale").getAsDouble());
        assertEquals("corruptedadventure:ring_2/nether/region", temperature.get("ring_2").getAsString());
        assertEquals("corruptedadventure:ring_3/nether/region", temperature.get("ring_3").getAsString());
        for (String ring : new String[]{"ring_4", "ring_5", "void"}) {
            assertEquals(0.0, temperature.get(ring).getAsDouble(), ring);
        }
    }

    /**
     * Weirdness is 0 outside rings 1 and 3. Safe for ring 2: every ring 2 biome takes all weirdness, and the zones 1→2
     * and 2→3 have fixed transition biomes, so ring 1's vanilla climate is only read where the blend is vanilla's.
     */
    @Test
    void routerWeirdnessIsVanillasInRing1AndTheDormantCalderaInRing3() {
        JsonObject router = WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("noise_router");
        assertEquals("corruptedadventure:ring_blend/overworld_weirdness", router.get("ridges").getAsString());
        JsonObject weirdness = rings("overworld_weirdness");
        // Vanilla 1.21.1's router value, literally: ring 1's biomes (rivers, peaks, plateaus) must not move.
        assertEquals("minecraft:overworld/ridges", weirdness.get("ring_1").getAsString());
        assertEquals("corruptedadventure:ring_3/overworld/caldera_dormant", weirdness.get("ring_3").getAsString());
        for (String ring : new String[]{"ring_2", "ring_4", "ring_5", "void"}) {
            assertEquals(0.0, weirdness.get(ring).getAsDouble(), ring);
        }
        for (JsonElement biome : multiNoise("minecraft:overworld", "ring_2")) {
            JsonArray range = biome.getAsJsonObject().getAsJsonObject("parameters").getAsJsonArray("weirdness");
            assertEquals(-2.0, range.get(0).getAsDouble(), biome.toString());
            assertEquals(2.0, range.get(1).getAsDouble(), biome.toString());
        }
        JsonObject transitions = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").getAsJsonObject("biome_source")
                .getAsJsonObject("transitions");
        assertTrue(transitions.has("ring_1") && transitions.has("ring_2"));
    }

    @Test
    void regionAxesAreFlatCachesOfTheirOwnNoise() {
        String[][] axes = {{"ring_3/overworld/region_heat", "ring_3_heat"}, {"ring_3/overworld/region_ground", "ring_3_ground"},
                {"ring_3/nether/region", "nether_ring_3_region"}};
        for (String[] axis : axes) {
            JsonObject json = WorldgenData.read("density_function/" + axis[0] + ".json");
            assertEquals("minecraft:flat_cache", json.get("type").getAsString(), axis[0]);
            JsonObject noise = json.getAsJsonObject("argument");
            assertEquals("minecraft:noise", noise.get("type").getAsString(), axis[0]);
            assertEquals("corruptedadventure:" + axis[1], noise.get("noise").getAsString(), axis[0]);
            assertEquals(1.0, noise.get("xz_scale").getAsDouble(), axis[0]);
            assertEquals(0.0, noise.get("y_scale").getAsDouble(), axis[0]);
            assertTrue(Files.exists(WorldgenData.root().resolve("noise/" + axis[1] + ".json")), axis[1]);
        }
    }

    /**
     * No lava sea in Nether-3, and no lava wall where the fluid level steps at a midline ({@code RingFluidPicker}
     * switches hard on {@code RingPosition.ring()}): at every boundary between two levels, the ring with the lower one
     * must be solid up to the higher one near the boundary, or open space there would hold a wall of lava. Ring 2 and
     * ring 3 each have their dam ({@code ring_edge}, change 35 / 53). In the world: {@code tools/nether-lava-walls.py}.
     */
    @Test
    void everyNetherLavaStepIsCoveredBySolidGround() {
        JsonObject levels = generator("minecraft:the_nether").getAsJsonObject("fluid_levels");
        int seaLevel = WorldgenData.read("noise_settings/nether.json").get("sea_level").getAsInt();
        String[] rings = {"ring_1", "ring_2", "ring_3", "ring_4", "ring_5"};
        assertEquals(22, levels.get("ring_2").getAsInt());
        // Ring 3 = ring 2's level (answer 2026-10-06): no step at 2|3, where ring 2's carvers cut through the dam.
        // Below the contract's floor (Y ~35): lava under the ice, no visible sea.
        assertEquals(levels.get("ring_2").getAsInt(), levels.get("ring_3").getAsInt());
        assertTrue(levels.get("ring_3").getAsInt() < 35);
        int steps = 0;
        for (int i = 0; i + 1 < rings.length; i++) {
            int inner = levels.has(rings[i]) ? levels.get(rings[i]).getAsInt() : seaLevel;
            int outer = levels.has(rings[i + 1]) ? levels.get(rings[i + 1]).getAsInt() : seaLevel;
            if (inner == outer) {
                continue;
            }
            steps++;
            String low = inner < outer ? rings[i] : rings[i + 1];
            int top = Math.max(inner, outer);
            String boundary = rings[i] + " | " + rings[i + 1];
            switch (low) {
                // The dam: 2.5 × edge × a gradient that is positive below to_y; edge = 1 on and beyond both of ring 2's boundaries.
                case "ring_2" -> assertTrue(find(WorldgenData.read("density_function/ring_2/nether/dam.json"), "minecraft:y_clamped_gradient")
                        .get("to_y").getAsInt() > top, boundary + ": ring 2's dam must reach above Y " + top);
                case "ring_3" -> assertRing3DamSolidUpTo(top, boundary);
                default -> throw new AssertionError(boundary + ": lava step from Y " + top + " with nothing guarding the " + low + " side");
            }
        }
        assertEquals(2, steps, "steps 1|2 (32/22) and 3|4 (22/32)");
    }

    /**
     * Ring 3's dam ({@code k × (edge × gradient) + c}, a term of the shape's {@code max}) keeps its full strength up to
     * Y {@code top} and stays solid there against the lowest roughness the shape adds; the cracks (cut outside the
     * interpolation) never open where the edge is 1. The roughness ({@code (a + b × w) × noise + bias}, {@code w} in
     * [0, 1]) is sampled with the real (legacy-seeded) noise, with a margin over the sampled peak.
     */
    private static void assertRing3DamSolidUpTo(int top, String boundary) {
        JsonObject dam = WorldgenData.read("density_function/ring_3/nether/dam.json");
        JsonObject scaled = dam.getAsJsonObject("argument1");
        double k = scaled.get("argument1").getAsDouble();
        double c = dam.get("argument2").getAsDouble();
        JsonObject product = scaled.getAsJsonObject("argument2");
        assertEquals("corruptedadventure:ring_3/nether/edge", product.get("argument1").getAsString());
        JsonObject gradient = product.getAsJsonObject("argument2");
        assertEquals("minecraft:y_clamped_gradient", gradient.get("type").getAsString());
        assertTrue(gradient.get("from_y").getAsInt() > top, boundary + ": ring 3's dam weakens at or below Y " + top);
        // edge = a flat cache of ring 3's ring_edge (1 on both boundaries)
        JsonObject edge = WorldgenData.read("density_function/ring_3/nether/edge.json");
        assertEquals("minecraft:flat_cache", edge.get("type").getAsString());
        edge = edge.getAsJsonObject("argument");
        assertEquals("corruptedadventure:ring_edge", edge.get("type").getAsString());
        assertEquals("ring_3", edge.get("ring").getAsString());
        assertEquals("minecraft:the_nether", edge.get("dimension").getAsString());
        double strength = k * gradient.get("from_value").getAsDouble() + c;
        assertShapeTermHolds("dam", strength, boundary + ": ring 3's dam can open at Y " + top);

        String mask = WorldgenData.read("density_function/ring_3/nether/crack_mask.json").toString();
        assertTrue(mask.contains("\"corruptedadventure:ring_3/nether/core\""), "crack mask × core");
        JsonObject core = WorldgenData.read("density_function/ring_3/nether/core.json").getAsJsonObject("argument");
        assertEquals(1.0, core.get("argument1").getAsDouble(), "core = 1 − edge");
        assertEquals(-1.0, core.getAsJsonObject("argument2").get("argument1").getAsDouble(), "core = 1 − edge");
        assertEquals("corruptedadventure:ring_3/nether/edge", core.getAsJsonObject("argument2").get("argument2").getAsString());
    }

    /**
     * No lava pools in Nether-3 outside the warm cracks: the shape's {@code lid} holds full strength up to the ring's
     * lava level, so no open space reaches Y 22 even where a low hall floor meets the strongest roughness.
     */
    @Test
    void nether3IsSolidUpToItsLavaLevelOutsideTheCracks() {
        int level = generator("minecraft:the_nether").getAsJsonObject("fluid_levels").get("ring_3").getAsInt();
        JsonObject lid = WorldgenData.read("density_function/ring_3/nether/lid.json");
        assertEquals("minecraft:y_clamped_gradient", lid.get("type").getAsString());
        assertTrue(lid.get("from_y").getAsInt() > level, "the lid weakens at or below the lava level");
        assertShapeTermHolds("lid", lid.get("from_value").getAsDouble(), "Nether-3 can open at the lava level Y " + level);
    }

    /**
     * {@code term} is a term of the shape's {@code max} and, at {@code strength}, stays solid against the lowest
     * roughness the shape adds ({@code (a + b × w) × noise + bias}, {@code w} in [0, 1]), sampled with the real
     * (legacy-seeded) noise, with a 25 % margin over the sampled peak.
     */
    private static void assertShapeTermHolds(String term, double strength, String message) {
        JsonObject shape = WorldgenData.read("density_function/ring_3/nether/shape.json");
        assertEquals("minecraft:add", shape.get("type").getAsString());
        assertTrue(shape.getAsJsonObject("argument1").toString().contains("\"corruptedadventure:ring_3/nether/" + term + "\""), term + " is a term of the max");
        assertEquals("corruptedadventure:ring_3/nether/rough", shape.get("argument2").getAsString());
        JsonObject rough = WorldgenData.read("density_function/ring_3/nether/rough.json");
        JsonObject amplitude = rough.getAsJsonObject("argument1").getAsJsonObject("argument1");
        double a = amplitude.get("argument1").getAsDouble();
        double b = amplitude.getAsJsonObject("argument2").get("argument1").getAsDouble();
        double bias = rough.get("argument2").getAsDouble();
        JsonObject roughNoise = rough.getAsJsonObject("argument1").getAsJsonObject("argument2");
        assertEquals("corruptedadventure:nether_ring_3_rough", roughNoise.get("noise").getAsString());
        NormalNoise noise = netherNoise("nether_ring_3_rough");
        double yScale = roughNoise.get("y_scale").getAsDouble();
        double highest = 0.0;
        for (int x = -1600; x < 1600; x += 7) {
            for (int z = -1600; z < 1600; z += 7) {
                highest = Math.max(highest, Math.abs(noise.getValue(x, ((x * 31 + z) & 127) * yScale, z)));
            }
        }
        double lowest = strength - (a + Math.max(b, 0.0)) * highest * 1.25 + bias;
        assertTrue(lowest > 0.0, message + " (lowest density " + lowest + ")");
    }

    /** The first object of {@code type} in a JSON tree (depth first). */
    private static JsonObject find(JsonElement tree, String type) {
        if (tree.isJsonObject()) {
            JsonObject object = tree.getAsJsonObject();
            if (object.has("type") && object.get("type").getAsString().equals(type)) {
                return object;
            }
            for (var entry : object.entrySet()) {
                JsonObject found = find(entry.getValue(), type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Patches of ~500–1,000 blocks (fase-1-wereld §1.10), measured as the mean run along a line before the cell changes.
     * Ring 3's two axes, each cut into terciles (3 × 3 cells for the 8 biomes of 39b), make patches at least as large as
     * ring 2's five biomes on their preset thresholds (approved at teststop 3). Measured on a large area, so it holds for
     * any seed: −11 gives ~580 against ring 2's ~500 (on −10 ring 3 would give ~310).
     */
    @Test
    void overworldAxesArePatchesAtLeastAsLargeAsRing2s() {
        NormalNoise heat = noise("ring_3_heat");
        NormalNoise ground = noise("ring_3_ground");
        Stats stats = Stats.of(heat, ground);
        Stats.Axis h = stats.a(), g = stats.b();
        assertEquals(0.0, h.mean(), 0.05);
        assertEquals(0.0, g.mean(), 0.05);
        // Thresholds for 39b live near the middle: the axes stay within ±0.6 almost everywhere.
        assertTrue(h.withinPointSix() > 0.95 && g.withinPointSix() > 0.95, h.withinPointSix() + " / " + g.withinPointSix());
        assertTrue(Math.abs(stats.correlation()) < 0.05, "independent axes");

        double cutH = 0.4307 * h.sd(), cutG = 0.4307 * g.sd(); // terciles of a normal distribution
        double ring3 = meanRun((x, z) -> tercile(heat.getValue(x, 0, z), cutH) * 3 + tercile(ground.getValue(x, 0, z), cutG));
        double ring2 = ring2BiomeRun();
        assertTrue(ring3 >= ring2, "ring 3 run " + ring3 + " < ring 2 run " + ring2);
    }

    /** Nether-3 on the same footing (÷4 like the coordinates): three biomes on one axis against Nether-2's two. */
    @Test
    void nether3AxisMakesPatchesAtLeastAsLargeAsNether2s() {
        NormalNoise region = netherNoise("nether_ring_3_region");
        double cut = 0.4307 * Stats.of(region, region).a().sd();
        double nether3 = meanRun((x, z) -> tercile(region.getValue(x, 0, z), cut));
        NormalNoise ring2Region = netherNoise("nether_ring_2_region");
        double threshold = NetherRing2Features.REGION_THRESHOLD;
        double nether2 = meanRun((x, z) -> ring2Region.getValue(x, 0, z) >= threshold ? 1 : 0);
        assertTrue(nether3 >= nether2, "Nether-3 run " + nether3 + " < Nether-2 run " + nether2);
    }

    /** Ring 2's biome on its two axes, with the thresholds of the preset (same partition as {@code Ring2RegionSharesTest}). */
    private static double ring2BiomeRun() {
        double t = 0.0, hoodoo = 0.0, salt = 0.0, oasis = 0.0;
        for (JsonElement entry : multiNoise("minecraft:overworld", "ring_2")) {
            JsonObject parameters = entry.getAsJsonObject().getAsJsonObject("parameters");
            switch (entry.getAsJsonObject().get("biome").getAsString()) {
                case "corruptedadventure:canyonlands" -> {
                    t = parameters.getAsJsonArray("temperature").get(0).getAsDouble();
                    hoodoo = parameters.getAsJsonArray("humidity").get(0).getAsDouble();
                }
                case "corruptedadventure:salt_flats" -> salt = parameters.getAsJsonArray("humidity").get(1).getAsDouble();
                case "corruptedadventure:oasis_flats" -> oasis = parameters.getAsJsonArray("humidity").get(0).getAsDouble();
                default -> {
                }
            }
        }
        NormalNoise relief = noise("ring_2_relief");
        NormalNoise moisture = noise("ring_2_moisture");
        double rocky = t, hoodooEdge = hoodoo, saltEdge = salt, oasisEdge = oasis;
        return meanRun((x, z) -> {
            double m = moisture.getValue(x, 0, z);
            if (relief.getValue(x, 0, z) >= rocky) {
                return m >= hoodooEdge ? 0 : 1;
            }
            return m < saltEdge ? 2 : m < oasisEdge ? 3 : 4;
        });
    }

    private record Stats(Axis a, Axis b, double correlation) {
        record Axis(double mean, double sd, double withinPointSix) {
        }

        /** Both axes and their correlation in one pass over 40,000 points on a 600-block grid (many independent patches). */
        static Stats of(NormalNoise a, NormalNoise b) {
            double sa = 0, sb = 0, saa = 0, sbb = 0, sab = 0;
            int withinA = 0, withinB = 0, n = 0;
            for (int x = -60_000; x < 60_000; x += 600) {
                for (int z = -60_000; z < 60_000; z += 600) {
                    double va = a.getValue(x, 0, z);
                    double vb = b == a ? va : b.getValue(x, 0, z);
                    sa += va;
                    sb += vb;
                    saa += va * va;
                    sbb += vb * vb;
                    sab += va * vb;
                    withinA += Math.abs(va) <= 0.6 ? 1 : 0;
                    withinB += Math.abs(vb) <= 0.6 ? 1 : 0;
                    n++;
                }
            }
            double ma = sa / n, mb = sb / n;
            double va = saa / n - ma * ma, vb = sbb / n - mb * mb;
            return new Stats(new Axis(ma, Math.sqrt(va), withinA / (double) n), new Axis(mb, Math.sqrt(vb), withinB / (double) n),
                    (sab / n - ma * mb) / Math.sqrt(va * vb));
        }
    }

    private interface Cell {
        int at(int x, int z);
    }

    /** Mean distance between cell changes along 60 lines of 120,000 blocks (alternately along x and z, step 4). */
    private static double meanRun(Cell cell) {
        long length = 0;
        int changes = 0;
        for (int line = 0; line < 60; line++) {
            boolean alongZ = line % 2 == 1;
            int across = -60_000 + line * 2_000;
            int previous = alongZ ? cell.at(across, -60_000) : cell.at(-60_000, across);
            for (int t = -60_000 + 4; t <= 60_000; t += 4) {
                int current = alongZ ? cell.at(across, t) : cell.at(t, across);
                if (current != previous) {
                    changes++;
                }
                previous = current;
            }
            length += 120_000;
        }
        return length / (double) Math.max(1, changes);
    }

    private static int tercile(double value, double cut) {
        return value < -cut ? 0 : value < cut ? 1 : 2;
    }

    /** The noise seeded as the overworld's random state seeds it for the test seed. */
    private static NormalNoise noise(String name) {
        return NormalNoise.create(new XoroshiroRandomSource(SEED).forkPositional().fromHashOf(ResourceLocation.parse("corruptedadventure:" + name)),
                WorldgenData.noiseParameters(name));
    }

    /** The Nether's noise settings use {@code legacy_random_source}: the random state seeds its noises from that. */
    private static NormalNoise netherNoise(String name) {
        return NormalNoise.create(new LegacyRandomSource(SEED).forkPositional().fromHashOf(ResourceLocation.parse("corruptedadventure:" + name)),
                WorldgenData.noiseParameters(name));
    }
}
