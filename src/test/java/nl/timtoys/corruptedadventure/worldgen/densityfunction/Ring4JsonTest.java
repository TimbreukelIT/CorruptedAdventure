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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ring 4 keys of golf 0 ({@code docs/plans/ring-4/contract.md} §4, package 59): the band and region axes in the
 * router, the shared density functions every golf-1 stream loads (stubs), the initial terrain, the island field and
 * the Nether-4 region. The owners fill the stubs in golf 1.
 */
class Ring4JsonTest {
    private static final String DF = "density_function/";
    private static final String[] STUBS = {"height", "island_mask", "island_hole", "cliff", "sundered", "tunnels"};
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
    void initialTerrainFollowsThePlaceholderSurface() {
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

        JsonObject placeholder = WorldgenData.read(DF + "ring_4/overworld.json").getAsJsonObject("argument");
        JsonObject placeholderGradient = placeholder.getAsJsonObject("argument1");
        JsonObject placeholderNoise = placeholder.getAsJsonObject("argument2");
        double fromY = placeholderGradient.get("from_y").getAsInt();
        double toY = placeholderGradient.get("to_y").getAsInt();
        double from = placeholderGradient.get("from_value").getAsDouble();
        double to = placeholderGradient.get("to_value").getAsDouble();
        double surface = fromY + from / (from - to) * (toY - fromY);
        double noiseBlocks = placeholderNoise.get("argument1").getAsDouble() * (toY - fromY) / (from - to);
        JsonObject height = WorldgenData.read(DF + "ring_4/overworld/initial_height.json");
        assertEquals(surface, height.get("argument1").getAsDouble());
        assertEquals(noiseBlocks, height.getAsJsonObject("argument2").get("argument1").getAsDouble());
        assertEquals(placeholderNoise.getAsJsonObject("argument2"), height.getAsJsonObject("argument2").getAsJsonObject("argument2"));
    }

    @Test
    void islandFieldExistsButIsNotInTheTerrainYet() {
        JsonObject islands = WorldgenData.read(DF + "ring_4/overworld/islands.json");
        assertEquals("corruptedadventure:island_field", islands.get("type").getAsString());
        String hash = islands.get("hash").getAsString();
        assertTrue(Files.exists(WorldgenData.root().resolve("noise/" + hash.substring(hash.indexOf(':') + 1) + ".json")), hash);
        // 61 wires it into the final density; until then the height hook and the water branch find nothing.
        assertEquals("corruptedadventure:ring_4/overworld", rings("overworld").get("ring_4").getAsString());
    }

    @Test
    void netherRing4HasItsRegionAndATerrainStubOnThePlaceholder() {
        assertEquals("corruptedadventure:ring_4/nether/region", rings("nether_temperature").get("ring_4").getAsString());
        assertEquals("corruptedadventure:ring_4/nether/terrain", rings("nether").get("ring_4").getAsString());
        assertEquals("corruptedadventure:ring_4/nether", element(DF + "ring_4/nether/terrain.json").getAsString());
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
