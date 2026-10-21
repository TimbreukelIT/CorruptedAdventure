package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ring 5 keys of golf 0 ({@code docs/plans/ring-5/contract.md} §4, package 82): the band, region and break-up axes in
 * the router, the shared density functions the golf-1 streams load (stubs), the initial terrain on the skeleton surface,
 * the field and the Nether-5 region; Nether-5's terrain and the preset's ring 5 biomes stay the placeholders.
 */
class Ring5JsonTest {
    private static final String DF = "density_function/";
    private static final String[] STUBS = {"height", "scars", "glitch", "inverted", "caves"};
    private static final RingLayout LAYOUT = Ring5FieldTest.LAYOUT;

    private static JsonElement element(String relative) {
        try {
            return JsonParser.parseString(Files.readString(WorldgenData.root().resolve(relative)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonObject rings(String blend) {
        return WorldgenData.read(DF + "ring_blend/" + blend + ".json").getAsJsonObject("rings");
    }

    @Test
    void routerOfRing5IsTheRegionBandAndBreakupAxes() {
        assertEquals("corruptedadventure:ring_5/overworld/region_rot", rings("overworld_temperature").get("ring_5").getAsString());
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
        for (String stub : STUBS) {
            assertEquals(0.0, element(DF + "ring_5/overworld/" + stub + ".json").getAsDouble(), stub);
        }
        assertEquals(0.0, element(DF + "zone_4_5/overworld/fracture.json").getAsDouble());
        assertOutput(DF + "ring_5/overworld/breakup.json", "breakup", true);
        assertOutput(DF + "ring_5/overworld/castles.json", "castles", true);
        assertOutput(DF + "ring_5/overworld/initial_height.json", "initial_height", false);
        JsonObject field = WorldgenData.read(DF + "ring_5/overworld/field.json");
        assertEquals("corruptedadventure:ring_5_field", field.get("type").getAsString());
        assertEquals("corruptedadventure:ring_5_cells", field.get("hash").getAsString());
        assertEquals("corruptedadventure:placeholder_terrain", field.get("placeholder").getAsString());
        assertTrue(Files.exists(WorldgenData.root().resolve("noise/ring_5_cells.json")));
        // The skeleton terrain: the field's per-block density, interpolated (RingBlend gates the marker).
        JsonObject terrain = WorldgenData.read(DF + "ring_5/overworld.json");
        assertEquals("minecraft:interpolated", terrain.get("type").getAsString());
        JsonObject output = terrain.getAsJsonObject("argument");
        assertEquals("corruptedadventure:ring_5_output", output.get("type").getAsString());
        assertEquals("terrain", output.get("output").getAsString());
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
     * The skeleton surface (82, until 83/84): ring 5's old placeholder surface, Y 85 + 8 × noise, lowered by 6 × band in
     * the outer band (the step that shows the band line); the field's terrain is rock below it and open above.
     */
    @Test
    void skeletonSurfaceIsThePlaceholderWithTheBandStep() {
        Ring5Field field = Ring5FieldTest.field();
        DensityFunction.NoiseHolder noise = IslandFieldTest.noise("placeholder_terrain", Ring5FieldTest.SEED);
        Ring5Band band = Ring5Band.of(LAYOUT);
        int inner = 0;
        int stepped = 0;
        for (int distance = 8000; distance <= 9700; distance += 23) {
            for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 24.0) {
                int x = (int) Math.round(distance * Math.cos(angle));
                int z = (int) Math.round(distance * Math.sin(angle));
                if (field.column(x, z).castle() > 0.0) {
                    continue;
                }
                double b = band.value(x, z);
                double expected = 85.0 + 8.0 * noise.getValue(x, 0.0, z) - 6.0 * Math.max(0.0, b);
                assertEquals(expected, field.initialHeight(x, z), 1e-9, x + ", " + z);
                int top = (int) Math.ceil(expected) - 1;
                assertTrue(field.terrain(x, top, z) > 0.0 && field.terrain(x, top + 1, z) <= 0.0, x + ", " + z);
                inner += b == -1.0 ? 1 : 0;
                stepped += b == 1.0 ? 1 : 0;
            }
        }
        assertTrue(inner > 100 && stepped > 100);
    }

    @Test
    void netherRing5KeepsItsPlaceholderTerrainAndGetsItsRegion() {
        assertEquals("corruptedadventure:ring_5/nether/terrain", rings("nether").get("ring_5").getAsString());
        JsonObject terrain = WorldgenData.read(DF + "ring_5/nether/terrain.json");
        // A reference to the placeholder: + 0 (a density function file cannot be a bare reference).
        assertEquals("minecraft:add", terrain.get("type").getAsString());
        assertEquals(0.0, terrain.get("argument1").getAsDouble());
        assertEquals("corruptedadventure:ring_5/nether", terrain.get("argument2").getAsString());
        assertEquals("minecraft:interpolated", WorldgenData.read(DF + "ring_5/nether.json").get("type").getAsString());
        assertEquals("corruptedadventure:ring_5/nether/region", rings("nether_temperature").get("ring_5").getAsString());
        JsonObject region = WorldgenData.read(DF + "ring_5/nether/region.json");
        assertEquals("minecraft:flat_cache", region.get("type").getAsString());
        assertEquals("corruptedadventure:nether_ring_5_region", region.getAsJsonObject("argument").get("noise").getAsString());
        assertEquals(-9, WorldgenData.read("noise/nether_ring_5_region.json").get("firstOctave").getAsInt());
    }

    /** V12-A: no Java hook; ring 5's sources in the preset (overworld 86, W-biomes; Nether placeholder until 87, W-nether). */
    @Test
    void presetRing5Sources() {
        JsonObject dimensions = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions");
        // 86 (W-biomes): the overworld's ring 5 is its own multi_noise (Ring5RegionSharesTest checks the boxes).
        assertEquals("minecraft:multi_noise", dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
                .getAsJsonObject("biome_source").getAsJsonObject("rings").getAsJsonObject("ring_5").get("type").getAsString());
        assertEquals("corruptedadventure:placeholder_nether_ring_5", dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator")
                .getAsJsonObject("biome_source").getAsJsonObject("rings").getAsJsonObject("ring_5").get("biome").getAsString());
    }
}
