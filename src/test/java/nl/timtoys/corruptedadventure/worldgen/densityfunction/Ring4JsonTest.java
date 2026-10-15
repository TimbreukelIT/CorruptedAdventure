package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ring 4 keys of golf 0 ({@code docs/plans/ring-4/contract.md} §4, package 59): the band and region axes in the
 * router, the shared density functions every golf-1 stream loads (stubs), the initial terrain, the island field and
 * the Nether-4 region. The owners fill the stubs in golf 1.
 */
class Ring4JsonTest {
    private static final String DF = "density_function/";
    private static final String[] STUBS = {"tunnels"};
    private static final RingLayout LAYOUT = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(20260930L));

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
    void everyDensityFunctionFileIsANumberOrAnObject() throws IOException {
        try (var files = Files.walk(WorldgenData.root().resolve(DF))) {
            for (var file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonElement json = JsonParser.parseString(Files.readString(file));
                assertTrue(json.isJsonObject() || json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber(),
                        file + " must be a number or an object");
            }
        }
    }

    @Test
    void routerClimateOfRing4IsTheBandAndTheRegionAxes() {
        assertEquals("corruptedadventure:ring_4/overworld/region_water", rings("overworld_temperature").get("ring_4").getAsString());
        assertEquals("corruptedadventure:ring_4/overworld/region_growth", rings("overworld_vegetation").get("ring_4").getAsString());
        assertEquals("corruptedadventure:ring_4/overworld/band", rings("overworld_weirdness").get("ring_4").getAsString());
        // No deep-dark check of the aquifer in ring 4 (like rings 2 and 3).
        assertEquals(0.0, rings("overworld_erosion").get("ring_4").getAsDouble());
        for (String[] axis : new String[][]{{"region_water", "ring_4_water"}, {"region_growth", "ring_4_growth"}}) {
            JsonObject json = WorldgenData.read(DF + "ring_4/overworld/" + axis[0] + ".json");
            assertEquals("minecraft:flat_cache", json.get("type").getAsString());
            JsonObject noise = json.getAsJsonObject("argument");
            assertEquals("corruptedadventure:" + axis[1], noise.get("noise").getAsString());
            assertEquals(0.0, noise.get("y_scale").getAsDouble());
            assertEquals(-10, WorldgenData.read("noise/" + axis[1] + ".json").get("firstOctave").getAsInt(), axis[1]);
        }
    }

    /** One source: the band in JSON is {@link Ring4Band}'s value, on a raster across the ring. */
    @Test
    void bandDensityFunctionIsTheJavaBand() {
        DensityFunction band = DensityFunction.DIRECT_CODEC.parse(JsonOps.INSTANCE, WorldgenData.read(DF + "ring_4/overworld/band.json"))
                .getOrThrow().mapAll(new DensityFunction.Visitor() {
                    @Override
                    public DensityFunction apply(DensityFunction function) {
                        return function instanceof Ring4BandFunction ? new Ring4BandFunction(() -> LAYOUT) : function;
                    }
                });
        Ring4Band java = Ring4Band.of(LAYOUT);
        int crossings = 0;
        for (int x = -8100; x <= 8100; x += 37) {
            for (int z = -8100; z <= 8100; z += 41) {
                double value = band.compute(new DensityFunction.SinglePointContext(x, 64, z));
                assertEquals(java.value(x, z), value, x + ", " + z);
                crossings += value > -1.0 && value < 1.0 ? 1 : 0;
            }
        }
        assertTrue(crossings > 0, "the raster never crossed the transition");
    }

    @Test
    void sharedDensityFunctionsAreStubs() {
        for (String stub : STUBS) {
            assertEquals(0.0, element(DF + "ring_4/overworld/" + stub + ".json").getAsDouble(), stub);
        }
    }

    /**
     * Like ring 3's: 0.125 × initial height + a gradient that is 0 at Y = height. Until 60 the height is exactly the
     * surface of the placeholder terrain ({@code ring_4/overworld}: gradient 1 → −1 over Y 0–128 plus 0.125 × noise).
     */
    @Test
    void initialTerrainFollowsTheRealSurface() {
        assertEquals("corruptedadventure:ring_4/overworld/initial_terrain",
                rings("overworld_initial").getAsJsonObject("ring_4").get("argument2").getAsString());
        JsonObject terrain = WorldgenData.read(DF + "ring_4/overworld/initial_terrain.json");
        JsonObject scaled = terrain.getAsJsonObject("argument1");
        assertEquals(0.125, scaled.get("argument1").getAsDouble());
        assertEquals("corruptedadventure:ring_4/overworld/initial_height", scaled.getAsJsonObject("argument2").get("argument").getAsString());
        JsonObject gradient = terrain.getAsJsonObject("argument2");
        double slope = (gradient.get("to_value").getAsDouble() - gradient.get("from_value").getAsDouble())
                / (gradient.get("to_y").getAsInt() - gradient.get("from_y").getAsInt());
        assertEquals(-0.125, slope, 1e-12);
        assertEquals(0.0, gradient.get("from_value").getAsDouble() + slope * (0 - gradient.get("from_y").getAsInt()), 1e-12);
        // 60: the height is the terrain field's lower bound of the real surface.
        assertOutput(DF + "ring_4/overworld/initial_height.json", "initial_height");
        // The terrain's stack is the same gradient on the surface with the holes.
        JsonObject stack = WorldgenData.read(DF + "ring_4/overworld/terrain.json").getAsJsonObject("argument");
        assertEquals("corruptedadventure:ring_4/overworld/surface", stack.getAsJsonObject("argument1").get("argument2").getAsString());
        assertEquals(gradient, stack.getAsJsonObject("argument2"));
    }

    private static void assertOutput(String file, String output) {
        JsonObject json = WorldgenData.read(file);
        String type = json.get("type").getAsString();
        if (type.equals("minecraft:flat_cache") || type.equals("minecraft:cache_once")) {
            json = json.getAsJsonObject("argument");
        }
        assertEquals("corruptedadventure:island_output", json.get("type").getAsString(), file);
        assertEquals("corruptedadventure:ring_4/overworld/islands", json.get("field").getAsString(), file);
        assertEquals(output, json.get("output").getAsString(), file);
    }

    /** 60: the field is the ring 4 terrain; its relief reads the router's region noises; every noise exists. */
    @Test
    void ring4TerrainIsTheIslandField() {
        JsonObject islands = WorldgenData.read(DF + "ring_4/overworld/islands.json");
        assertEquals("corruptedadventure:island_field", islands.get("type").getAsString());
        List<String> noises = new ArrayList<>(List.of(islands.get("hash").getAsString()));
        JsonObject relief = islands.getAsJsonObject("relief");
        assertEquals("corruptedadventure:ring_4_water", relief.get("water").getAsString());
        assertEquals("corruptedadventure:ring_4_growth", relief.get("growth").getAsString());
        relief.entrySet().forEach(e -> noises.add(e.getValue().getAsString()));
        for (String noise : noises) {
            assertTrue(Files.exists(WorldgenData.root().resolve("noise/" + noise.substring(noise.indexOf(':') + 1) + ".json")), noise);
        }
        assertEquals("corruptedadventure:ring_4/overworld", rings("overworld").get("ring_4").getAsString());
        assertOutput(DF + "ring_4/overworld/height.json", "height");
        assertOutput(DF + "ring_4/overworld/surface.json", "surface");
        assertOutput(DF + "ring_4/overworld/thin.json", "thin");
        assertOutput(DF + "ring_4/overworld/island_mask.json", "island_mask");
        assertOutput(DF + "ring_4/overworld/island_hole.json", "island_hole");
        assertOutput(DF + "ring_4/overworld/cliff.json", "cliff");
        assertOutput(DF + "ring_4/overworld/sundered.json", "sundered");
        // 62: the plateau above the cliff 3→4 follows ring 3's terrain.
        assertEquals("corruptedadventure:ring_3/overworld/field", islands.get("plateau").getAsString());
        assertOutput(DF + "ring_4/overworld/cave_lift.json", "cave_lift");
        // The final stack: the exact per-block islands first, then the interpolated terrain with vanilla's caves.
        JsonObject overworld = WorldgenData.read(DF + "ring_4/overworld.json");
        assertEquals("minecraft:range_choice", overworld.get("type").getAsString());
        assertEquals("corruptedadventure:ring_4/overworld/thin", overworld.get("input").getAsString());
        String text = overworld.toString();
        assertTrue(text.contains("corruptedadventure:ring_4/overworld/terrain"));
        assertTrue(text.contains("corruptedadventure:ring_4/overworld/cave_lift"));
        assertTrue(!text.contains("ring_3"), "no ring 3 key in the ring 4 stack");
        // Review 60 #3: vanilla's cave entrances may break the surface (min(terrain, 5 × entrances + 4 × cave_lift)),
        // except around a lake above the sea (review 61b #1: the lift of 2 there closes them, 4 × 2 > 5 × their depth).
        assertTrue(text.contains("{\"type\":\"minecraft:mul\",\"argument1\":5.0,\"argument2\":\"minecraft:overworld/caves/entrances\"},"
                + "\"argument2\":{\"type\":\"minecraft:mul\",\"argument1\":4.0,\"argument2\":\"corruptedadventure:ring_4/overworld/cave_lift\"}"), text);
        for (String perBlock : new String[]{"thin", "cave_lift"}) {
            assertEquals("minecraft:cache_once", WorldgenData.read(DF + "ring_4/overworld/" + perBlock + ".json").get("type").getAsString());
        }
    }

    /**
     * verdant_falls: every overworld {@code ring_blend} mixes ring 3 ↔ 4 only over the {@code steam_fens} rim, with the
     * one Java share ({@link IslandField#VERDANT_EDGE_SHARE}); the Nether's blends mix over their whole zones.
     */
    @Test
    void overworldBlendsMixRing3And4OnlyOverTheSteamFensRim() throws IOException {
        int overworld = 0;
        try (var files = Files.list(WorldgenData.root().resolve(DF + "ring_blend"))) {
            for (var file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                String name = file.getFileName().toString();
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (name.startsWith("overworld")) {
                    overworld++;
                    JsonObject mix = json.getAsJsonObject("mix");
                    assertEquals(1, mix.size(), name);
                    assertEquals(IslandField.VERDANT_EDGE_SHARE, mix.get("ring_3").getAsDouble(), 0.0, name);
                } else {
                    assertTrue(!json.has("mix"), name);
                }
            }
        }
        assertEquals(8, overworld);
        assertEquals(60.0, IslandField.VERDANT_EDGE_SHARE * 256.0, 0.0, "60 of the zone's 256 blocks");
    }

    @Test
    void netherRing4HasItsRegionAndTerrain() {
        assertEquals("corruptedadventure:ring_4/nether/region", rings("nether_temperature").get("ring_4").getAsString());
        assertEquals("corruptedadventure:ring_4/nether/terrain", rings("nether").get("ring_4").getAsString());
        // The terrain itself (65): NetherRing4JsonTest.
        JsonObject region = WorldgenData.read(DF + "ring_4/nether/region.json");
        assertEquals("minecraft:flat_cache", region.get("type").getAsString());
        assertEquals("corruptedadventure:nether_ring_4_region", region.getAsJsonObject("argument").get("noise").getAsString());
        assertTrue(Files.exists(WorldgenData.root().resolve("noise/nether_ring_4_region.json")));
    }

    @Test
    void presetHasTheHeightHookOnTheHeightBiomes() {
        JsonObject hook = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").getAsJsonObject("biome_source")
                .getAsJsonObject("island_biomes");
        assertEquals(140, hook.get("min_top").getAsInt());
        // Since 64 the height biomes (placeholders until then).
        assertEquals("corruptedadventure:sky_gardens", hook.get("mythic").getAsString());
        assertEquals("corruptedadventure:withered_gardens", hook.get("blighted").getAsString());
    }
}
