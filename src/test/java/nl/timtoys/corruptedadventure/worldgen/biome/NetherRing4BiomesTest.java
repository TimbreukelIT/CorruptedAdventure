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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing4Features;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Nether ring 4 biomes (65) and their place in the world preset. */
class NetherRing4BiomesTest {
    private static final Map<ResourceKey<Biome>, Biome> BIOMES = bootstrap();

    @Test
    void allThreeBiomesAreRegisteredAndListed() {
        assertEquals(List.of(ModBiomes.BONE_DUNES, ModBiomes.CRYSTAL_SPIRES, ModBiomes.GEODE_HOLLOWS), NetherRing4Biomes.ALL);
        assertEquals(NetherRing4Biomes.ALL, List.copyOf(BIOMES.keySet()));
    }

    @Test
    void hotAndDryWithTheBoneDesertFog() {
        for (Biome biome : BIOMES.values()) {
            assertFalse(biome.hasPrecipitation());
            assertTrue(biome.getBaseTemperature() > 1.0F, "hot");
            assertEquals(0.0F, biome.getModifiedClimateSettings().downfall());
            assertEquals(NetherRing4Biomes.FOG, biome.getFogColor());
            assertEquals(0x000000, biome.getSkyColor());
            assertTrue(biome.getBackgroundMusic().isPresent());
        }
    }

    /**
     * No carvers (the dam of the steps 3|4 and 4|5 stays whole). The dressing of 78: the geode lining and the column
     * cladding in every biome (a geode or a column may cross into a chunk of another biome), in the same order; the
     * loose bones and boulders only in the dunes.
     */
    @Test
    void noCarversAndTheDressingOf78() {
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : BIOMES.entrySet()) {
            Biome biome = entry.getValue();
            for (GenerationStep.Carving step : GenerationStep.Carving.values()) {
                assertFalse(biome.getGenerationSettings().getCarvers(step).iterator().hasNext());
            }
            List<List<String>> steps = biome.getGenerationSettings().features().stream()
                    .map(set -> set.stream().map(h -> h.unwrapKey().orElseThrow().location().getPath()).toList()).toList();
            assertEquals(List.of("nether_ring_4/geode_linings", "nether_ring_4/pillar_claddings"),
                    steps.get(GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal()));
            assertEquals(entry.getKey() == ModBiomes.BONE_DUNES ? List.of("nether_ring_4/dune_bones", "nether_ring_4/dune_boulders") : List.of(),
                    steps.get(GenerationStep.Decoration.LOCAL_MODIFICATIONS.ordinal()));
            int total = steps.stream().mapToInt(List::size).sum();
            assertEquals(entry.getKey() == ModBiomes.BONE_DUNES ? 4 : 2, total);
        }
    }

    @Test
    void spawnsLikeASoulSandValley() {
        for (Biome biome : BIOMES.values()) {
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.SKELETON));
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.GHAST));
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.ENDERMAN));
            assertTrue(spawns(biome, MobCategory.CREATURE, EntityType.STRIDER));
            for (EntityType<?> absent : List.of(EntityType.PIGLIN, EntityType.HOGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.MAGMA_CUBE)) {
                for (MobCategory category : MobCategory.values()) {
                    assertFalse(spawns(biome, category, absent), absent.toString());
                }
            }
        }
    }

    /** The preset's Nether ring 4 source picks the biomes on the region axis at the terrain's thresholds; the lava sea at Y 10. */
    @Test
    void presetPicksTheBiomesAtTheTerrainThresholds() {
        JsonObject generator = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals(10, generator.getAsJsonObject("fluid_levels").get("ring_4").getAsInt());
        JsonObject source = generator.getAsJsonObject("biome_source").getAsJsonObject("rings").getAsJsonObject("ring_4");
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
        double spires = NetherRing4Features.SPIRES_THRESHOLD, hollows = NetherRing4Features.HOLLOWS_THRESHOLD;
        assertEquals(3, temperature.size());
        assertEquals(List.of(hollows, spires), List.of(temperature.get("corruptedadventure:bone_dunes")[0], temperature.get("corruptedadventure:bone_dunes")[1]));
        assertEquals(List.of(spires, 2.0), List.of(temperature.get("corruptedadventure:crystal_spires")[0], temperature.get("corruptedadventure:crystal_spires")[1]));
        assertEquals(List.of(-2.0, hollows), List.of(temperature.get("corruptedadventure:geode_hollows")[0], temperature.get("corruptedadventure:geode_hollows")[1]));
    }

    private static boolean spawns(Biome biome, MobCategory category, EntityType<?> type) {
        return biome.getMobSettings().getMobs(category).unwrap().stream().anyMatch(data -> data.type == type);
    }

    private static Map<ResourceKey<Biome>, Biome> bootstrap() {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        NetherRing4Biomes.bootstrap(new BootstrapContext<>() {
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
