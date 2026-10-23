package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ring 5 keys ({@code docs/plans/ring-5/contract.md} §4, package 82 onwards): the band, region and break-up axes in
 * the router, the shared density functions as the field's outputs (93: also the caves), the terrain with vanilla's damped
 * caves under it, the initial terrain, the field, Nether-5's terrain and region and the preset's ring 5 biome sources.
 */
class Ring5JsonTest {
    private static final String DF = "density_function/";
    private static final RingLayout LAYOUT = Ring5FieldTest.LAYOUT;

    private static JsonObject rings(String blend) {
        return WorldgenData.read(DF + "ring_blend/" + blend + ".json").getAsJsonObject("rings");
    }

    @Test
    void routerOfRing5IsTheRegionBandAndBreakupAxes() {
        // The pinnacle axis (84): region_rot, but −2 on a castle's plateau so it takes void_scars (V12-A).
        assertEquals("corruptedadventure:ring_5/overworld/climate_rot", rings("overworld_temperature").get("ring_5").getAsString());
        JsonObject climate = WorldgenData.read(DF + "ring_5/overworld/climate_rot.json");
        assertEquals("minecraft:flat_cache", climate.get("type").getAsString());
        JsonObject choice = climate.getAsJsonObject("argument");
        assertEquals("minecraft:range_choice", choice.get("type").getAsString());
        assertEquals("corruptedadventure:ring_5/overworld/castles", choice.get("input").getAsString());
        assertEquals(0.99, choice.get("min_inclusive").getAsDouble());
        assertEquals(-2.0, choice.get("when_in_range").getAsDouble());
        assertEquals("corruptedadventure:ring_5/overworld/region_rot", choice.get("when_out_of_range").getAsString());
        assertEquals("corruptedadventure:ring_5/overworld/region_glitch", rings("overworld_vegetation").get("ring_5").getAsString());
        assertEquals("corruptedadventure:ring_5/overworld/band", rings("overworld_weirdness").get("ring_5").getAsString());
        // V12-A: the break-up on the erosion slot; V9: no aquifer flooding (the fluid rule decides anyway).
        assertEquals("corruptedadventure:ring_5/overworld/breakup", rings("overworld_erosion").get("ring_5").getAsString());
        assertEquals(0.0, rings("overworld_floodedness").get("ring_5").getAsDouble());
        assertEquals(0.0, rings("overworld_vein_toggle").get("ring_5").getAsDouble());
        JsonObject initial = rings("overworld_initial").getAsJsonObject("ring_5");
        assertEquals(0.390625, initial.get("argument1").getAsDouble());
        assertEquals("corruptedadventure:ring_5/overworld/initial_terrain", initial.get("argument2").getAsString());
        assertEquals("corruptedadventure:ring_5/overworld", rings("overworld").get("ring_5").getAsString());
        for (String[] axis : new String[][]{{"region_rot", "ring_5_rot"}, {"region_glitch", "ring_5_glitch"}}) {
            JsonObject json = WorldgenData.read(DF + "ring_5/overworld/" + axis[0] + ".json");
            assertEquals("minecraft:flat_cache", json.get("type").getAsString());
            JsonObject noise = json.getAsJsonObject("argument");
            assertEquals("corruptedadventure:" + axis[1], noise.get("noise").getAsString());
            assertEquals(0.0, noise.get("y_scale").getAsDouble());
            assertEquals(-10, WorldgenData.read("noise/" + axis[1] + ".json").get("firstOctave").getAsInt(), axis[1]);
        }
        // The boundary 5→void still switches hard; the zone 4→5 has its transition biome (sundered_reach).
        for (String file : new String[]{"overworld_temperature", "overworld_vegetation", "overworld_weirdness"}) {
            var switchAt = WorldgenData.read(DF + "ring_blend/" + file + ".json").getAsJsonArray("switch_at");
            assertEquals(1, switchAt.size(), file);
            assertEquals("ring_5", switchAt.get(0).getAsString(), file);
        }
    }

    /** One source: the band in JSON is {@link Ring5Band}'s value, on a raster across the ring. */
    @Test
    void bandDensityFunctionIsTheJavaBand() {
        DensityFunction band = DensityFunction.DIRECT_CODEC.parse(JsonOps.INSTANCE, WorldgenData.read(DF + "ring_5/overworld/band.json"))
                .getOrThrow().mapAll(function -> function instanceof Ring5BandFunction ? new Ring5BandFunction(() -> LAYOUT) : function);
        Ring5Band java = Ring5Band.of(LAYOUT);
        int crossings = 0;
        for (int x = -9800; x <= 9800; x += 37) {
            for (int z = -9800; z <= 9800; z += 41) {
                double value = band.compute(new DensityFunction.SinglePointContext(x, 64, z));
                assertEquals(java.value(x, z), value, x + ", " + z);
                crossings += value > -1.0 && value < 1.0 ? 1 : 0;
            }
        }
        assertTrue(crossings > 0, "the raster never crossed the transition");
    }

    @Test
    void sharedDensityFunctionsAreStubsOrFieldOutputs() {
        assertEquals("corruptedadventure:zone_fracture_value",
                WorldgenData.read(DF + "zone_4_5/overworld/fracture.json").getAsJsonObject("argument").get("type").getAsString());
        assertOutput(DF + "ring_5/overworld/breakup.json", "breakup", true);
        assertOutput(DF + "ring_5/overworld/castles.json", "castles", true);
        assertOutput(DF + "ring_5/overworld/initial_height.json", "initial_height", false);
        assertOutput(DF + "ring_5/overworld/height.json", "height", true);
        assertOutput(DF + "ring_5/overworld/scars.json", "scars", true);
        assertOutput(DF + "ring_5/overworld/inverted.json", "inverted", true);
        assertOutput(DF + "ring_5/overworld/glitch.json", "glitch", true);
        // 93: the own caves per block (no cache) and the closure of vanilla's damped caves.
        assertOutput(DF + "ring_5/overworld/caves.json", "caves", false);
        assertOutput(DF + "ring_5/overworld/cave_closure.json", "cave_closure", false);
        assertOutput(DF + "ring_5/overworld/cave_zone.json", "cave_zone", false);
        JsonObject field = WorldgenData.read(DF + "ring_5/overworld/field.json");
        assertEquals("corruptedadventure:ring_5_field", field.get("type").getAsString());
        assertEquals("corruptedadventure:ring_5_cells", field.get("hash").getAsString());
        // 83: no placeholder any more in ring 5's overworld terrain.
        assertTrue(!field.has("placeholder"));
        assertTrue(Files.exists(WorldgenData.root().resolve("noise/ring_5_cells.json")));
        JsonObject noises = field.getAsJsonObject("noises");
        // The terrain reads the router's own region axis.
        assertEquals("corruptedadventure:ring_5_rot", noises.get("rot").getAsString());
        assertEquals("corruptedadventure:ring_5_glitch", noises.get("glitch").getAsString());
        for (String noise : new String[]{"hills", "relief", "detail", "scars"}) {
            assertEquals("corruptedadventure:ring_5_" + noise, noises.get(noise).getAsString());
            assertTrue(Files.exists(WorldgenData.root().resolve("noise/ring_5_" + noise + ".json")), noise);
        }
        // The terrain: the field's per-block density, exact (no interpolation), with vanilla's damped caves (93, V93-1 A)
        // under it by the field's cave zone: the cheese and spaghetti deep under the ground (2), the entrances within 8 of
        // its top (1, the closure 4×: 5× entrances reach about −6), none on exact shapes and closed ground (0); the caves
        // interpolated, the closure not.
        JsonObject terrain = WorldgenData.read(DF + "ring_5/overworld/terrain.json");
        assertEquals("minecraft:cache_once", terrain.get("type").getAsString());
        assertEquals("terrain", terrain.getAsJsonObject("argument").get("output").getAsString());
        String zone = "corruptedadventure:ring_5/overworld/cave_zone";
        String closure = "corruptedadventure:ring_5/overworld/cave_closure";
        String ground = "corruptedadventure:ring_5/overworld/terrain";
        JsonObject output = WorldgenData.read(DF + "ring_5/overworld.json");
        assertEquals("minecraft:range_choice", output.get("type").getAsString());
        assertEquals(zone, output.get("input").getAsString());
        assertEquals(1.5, output.get("min_inclusive").getAsDouble());
        JsonObject deep = output.getAsJsonObject("when_in_range");
        assertEquals("minecraft:min", deep.get("type").getAsString());
        assertEquals(ground, deep.get("argument1").getAsString());
        assertEquals("corruptedadventure:ring_5/overworld/natural_caves", deep.getAsJsonObject("argument2").get("argument1").getAsString());
        assertEquals(closure, deep.getAsJsonObject("argument2").get("argument2").getAsString());
        JsonObject shallowChoice = output.getAsJsonObject("when_out_of_range");
        assertEquals(zone, shallowChoice.get("input").getAsString());
        assertEquals(0.5, shallowChoice.get("min_inclusive").getAsDouble());
        assertEquals(1.5, shallowChoice.get("max_exclusive").getAsDouble());
        assertEquals(ground, shallowChoice.get("when_out_of_range").getAsString());
        JsonObject shallow = shallowChoice.getAsJsonObject("when_in_range");
        assertEquals(ground, shallow.get("argument1").getAsString());
        JsonObject entrances = shallow.getAsJsonObject("argument2").getAsJsonObject("argument1");
        assertEquals(5.0, entrances.get("argument1").getAsDouble());
        assertEquals("minecraft:interpolated", entrances.getAsJsonObject("argument2").get("type").getAsString());
        JsonObject shut = shallow.getAsJsonObject("argument2").getAsJsonObject("argument2");
        assertEquals(4.0, shut.get("argument1").getAsDouble());
        assertEquals(closure, shut.get("argument2").getAsString());
        assertEquals("minecraft:interpolated", WorldgenData.read(DF + "ring_5/overworld/natural_caves.json").get("type").getAsString());
        // V2-A: the void edge is the same field (no seam at the hard switch 5→void), also for the initial density; without
        // the natural caves, which are closed beyond the inner band anyway.
        JsonObject voidTerrain = WorldgenData.read(DF + "void/overworld.json");
        assertEquals(0.0, voidTerrain.get("argument1").getAsDouble());
        assertEquals("corruptedadventure:ring_5/overworld/terrain", voidTerrain.get("argument2").getAsString());
        assertEquals("corruptedadventure:ring_5/overworld/initial_terrain",
                rings("overworld_initial").getAsJsonObject("void").get("argument2").getAsString());
    }

    private static void assertOutput(String file, String output, boolean flatCache) {
        JsonObject json = WorldgenData.read(file);
        assertEquals(flatCache, json.get("type").getAsString().equals("minecraft:flat_cache"), file);
        if (flatCache) {
            json = json.getAsJsonObject("argument");
        }
        assertEquals("corruptedadventure:ring_5_output", json.get("type").getAsString(), file);
        assertEquals("corruptedadventure:ring_5/overworld/field", json.get("field").getAsString(), file);
        assertEquals(output, json.get("output").getAsString(), file);
    }

    /** Like rings 3 and 4: 0.125 × initial height + a gradient that is 0 at Y = height. */
    @Test
    void initialTerrainFollowsTheSkeletonSurface() {
        JsonObject terrain = WorldgenData.read(DF + "ring_5/overworld/initial_terrain.json");
        JsonObject scaled = terrain.getAsJsonObject("argument1");
        assertEquals(0.125, scaled.get("argument1").getAsDouble());
        assertEquals("minecraft:cache_2d", scaled.getAsJsonObject("argument2").get("type").getAsString());
        assertEquals("corruptedadventure:ring_5/overworld/initial_height", scaled.getAsJsonObject("argument2").get("argument").getAsString());
        JsonObject gradient = terrain.getAsJsonObject("argument2");
        double slope = (gradient.get("to_value").getAsDouble() - gradient.get("from_value").getAsDouble())
                / (gradient.get("to_y").getAsInt() - gradient.get("from_y").getAsInt());
        assertEquals(-0.125, slope, 1e-12);
        assertEquals(0.0, gradient.get("from_value").getAsDouble() + slope * (0 - gradient.get("from_y").getAsInt()), 1e-12);
    }

    /**
     * 83: the inner band's ground is the field's (no skeleton), full down to the bedrock (outside the own caves of 93);
     * no placeholder anywhere.
     */
    @Test
    void innerBandIsTheFieldsGroundWithoutPlaceholder() throws IOException {
        Ring5Field field = Ring5FieldTest.field();
        Ring5Band band = Ring5Band.of(LAYOUT);
        int inner = 0;
        for (int distance = 8200; distance <= 9300; distance += 23) {
            for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 24.0) {
                int x = (int) Math.round(distance * Math.cos(angle));
                int z = (int) Math.round(distance * Math.sin(angle));
                Ring5Field.Column column = field.column(x, z);
                if (!band.inner(x, z) || column.isVoid() || column.castle() > 0.0 || column.spans > 0 || column.caves > 0) {
                    continue;
                }
                assertTrue(field.initialHeight(x, z) <= column.top());
                if (!column.layered) {
                    int top = (int) Math.ceil(column.top()) - 1;
                    assertTrue(field.terrain(x, top, z) > 0.0 && field.terrain(x, top + 1, z) <= 0.0, x + ", " + z);
                }
                assertTrue(field.terrain(x, -63, z) > 0.0, "full down to the bedrock at " + x + ", " + z);
                inner++;
            }
        }
        assertTrue(inner > 100);
        try (var files = Files.walk(WorldgenData.root().resolve(DF + "ring_5/overworld"))) {
            for (var file : files.filter(Files::isRegularFile).toList()) {
                assertTrue(!Files.readString(file).contains("placeholder"), file.toString());
            }
        }
    }

    @Test
    void netherRing5HasItsTerrainAndRegion() {
        // 87 (W-nether): the terrain and the region are the two outputs of nether_cathedrals (NetherRing5JsonTest).
        assertEquals("corruptedadventure:ring_5/nether/terrain", rings("nether").get("ring_5").getAsString());
        assertEquals("corruptedadventure:nether_cathedrals", WorldgenData.read(DF + "ring_5/nether/terrain.json").get("type").getAsString());
        assertEquals("corruptedadventure:ring_5/nether/region", rings("nether_temperature").get("ring_5").getAsString());
        JsonObject region = WorldgenData.read(DF + "ring_5/nether/region.json");
        assertEquals("minecraft:flat_cache", region.get("type").getAsString());
        assertEquals("corruptedadventure:nether_ring_5_region", region.getAsJsonObject("argument").get("plains").getAsString());
        assertEquals(-9, WorldgenData.read("noise/nether_ring_5_region.json").get("firstOctave").getAsInt());
    }

    /** V12-A: no Java hook; ring 5's sources in the preset (overworld 86, W-biomes; Nether 87, W-nether). */
    @Test
    void presetRing5Sources() {
        JsonObject dimensions = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions");
        // 86 (W-biomes): the overworld's ring 5 is its own multi_noise (Ring5RegionSharesTest checks the boxes).
        assertEquals("minecraft:multi_noise", dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
                .getAsJsonObject("biome_source").getAsJsonObject("rings").getAsJsonObject("ring_5").get("type").getAsString());
        // Nether ring 5: its own biomes since 87 (NetherRing5BiomesTest).
        assertEquals("minecraft:multi_noise", dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator")
                .getAsJsonObject("biome_source").getAsJsonObject("rings").getAsJsonObject("ring_5").get("type").getAsString());
    }
}
