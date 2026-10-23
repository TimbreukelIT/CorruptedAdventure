package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Nether ring 5 biomes (87) and their place in the world preset. */
class NetherRing5BiomesTest {
    private static final Map<ResourceKey<Biome>, Biome> BIOMES = bootstrap();

    @Test
    void bothBiomesAreRegisteredAndListed() {
        assertEquals(List.of(ModBiomes.WHITE_HALLS, ModBiomes.HOLLOW_CATHEDRAL), NetherRing5Biomes.ALL);
        assertEquals(NetherRing5Biomes.ALL, List.copyOf(BIOMES.keySet()));
    }

    /** Mild and dry, white fog, black sky; silent: the silent music, no ambient loop, mood or additions (98). */
    @Test
    void whiteAndSilent() {
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : BIOMES.entrySet()) {
            Biome biome = entry.getValue();
            assertFalse(biome.hasPrecipitation());
            assertEquals(0.5F, biome.getBaseTemperature());
            assertEquals(0.0F, biome.getModifiedClimateSettings().downfall());
            assertEquals(entry.getKey() == ModBiomes.HOLLOW_CATHEDRAL ? 0xF4F4F6 : 0xE8E8EC, biome.getFogColor());
            assertEquals(0x000000, biome.getSkyColor());
            assertEquals(java.util.Optional.of(BiomeAmbience.SILENT_MUSIC), biome.getBackgroundMusic(), "silent music");
            assertTrue(biome.getAmbientLoop().isEmpty(), "no ambient loop");
            assertTrue(biome.getAmbientMood().isEmpty(), "no mood");
            assertTrue(biome.getAmbientAdditions().isEmpty(), "no additions");
            assertEquals(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, biome.getAmbientParticle().orElseThrow().getOptions(),
                    "almost no particles: rare white ash");
        }
    }

    /** No spawns (contract §2), no carvers (nothing opens below the floor), no features (96). */
    @Test
    void emptyNoCarversNoFeatures() {
        for (Biome biome : BIOMES.values()) {
            for (MobCategory category : MobCategory.values()) {
                assertTrue(biome.getMobSettings().getMobs(category).isEmpty(), category.getName());
            }
            for (GenerationStep.Carving step : GenerationStep.Carving.values()) {
                assertFalse(biome.getGenerationSettings().getCarvers(step).iterator().hasNext());
            }
            assertTrue(biome.getGenerationSettings().features().stream().allMatch(set -> set.size() == 0));
        }
    }

    /** The preset's Nether ring 5 source picks the biomes on the cathedral mask at the threshold; no lava (level −1). */
    @Test
    void presetPicksTheCathedralOnTheMask() {
        JsonObject generator = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals(-1, generator.getAsJsonObject("fluid_levels").get("ring_5").getAsInt());
        JsonObject source = generator.getAsJsonObject("biome_source").getAsJsonObject("rings").getAsJsonObject("ring_5");
        assertEquals("minecraft:multi_noise", source.get("type").getAsString());
        Map<String, double[]> temperature = new LinkedHashMap<>();
        for (JsonElement entry : source.getAsJsonArray("biomes")) {
            JsonObject parameters = entry.getAsJsonObject().getAsJsonObject("parameters");
            for (String axis : List.of("humidity", "continentalness", "erosion", "weirdness", "depth")) {
                JsonArray range = parameters.getAsJsonArray(axis);
                assertEquals(-2.0, range.get(0).getAsDouble(), axis);
                assertEquals(2.0, range.get(1).getAsDouble(), axis);
            }
            JsonArray range = parameters.getAsJsonArray("temperature");
            temperature.put(entry.getAsJsonObject().get("biome").getAsString(), new double[]{range.get(0).getAsDouble(), range.get(1).getAsDouble()});
        }
        double threshold = NetherRing5Biomes.CATHEDRAL_THRESHOLD;
        assertEquals(2, temperature.size());
        assertEquals(List.of(-2.0, threshold), List.of(temperature.get("corruptedadventure:white_halls")[0], temperature.get("corruptedadventure:white_halls")[1]));
        assertEquals(List.of(threshold, 2.0), List.of(temperature.get("corruptedadventure:hollow_cathedral")[0], temperature.get("corruptedadventure:hollow_cathedral")[1]));
    }

    private static Map<ResourceKey<Biome>, Biome> bootstrap() {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        NetherRing5Biomes.bootstrap(new BootstrapContext<>() {
            @Override
            public Holder.Reference<Biome> register(ResourceKey<Biome> key, Biome value, Lifecycle lifecycle) {
                registered.put(key, value);
                return Holder.Reference.createStandAlone(biomes.owner(), key);
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
                return new StandaloneHolderGetter<>(key -> true);
            }
        });
        return registered;
    }
}
