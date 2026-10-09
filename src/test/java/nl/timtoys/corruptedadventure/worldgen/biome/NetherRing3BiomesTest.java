package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing3Features;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Nether ring 3 biomes (53b) and their place in the world preset. */
class NetherRing3BiomesTest {
    private static final Map<ResourceKey<Biome>, Biome> BIOMES = bootstrap();

    @Test
    void allThreeBiomesAreRegisteredAndListed() {
        assertEquals(List.of(ModBiomes.FROZEN_LAVA_SEA, ModBiomes.RIME_FOREST, ModBiomes.GLACIER_RIFTS), NetherRing3Biomes.ALL);
        assertEquals(NetherRing3Biomes.ALL, List.copyOf(BIOMES.keySet()));
    }

    @Test
    void coldWithoutPrecipitation() {
        for (Biome biome : BIOMES.values()) {
            assertFalse(biome.hasPrecipitation());
            assertTrue(biome.getBaseTemperature() < 0.15F, "cold");
            assertEquals(0.0F, biome.getModifiedClimateSettings().downfall());
        }
    }

    @Test
    void effectsComeFromTheAtmosphereHookAndOnlyTheMusicIsOurs() {
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : BIOMES.entrySet()) {
            BiomeAmbience.Palette palette = NetherRing3Atmosphere.palette(entry.getKey());
            assertEquals(palette.fog(), entry.getValue().getFogColor());
            assertEquals(palette.water(), entry.getValue().getWaterColor());
            assertEquals(0x000000, entry.getValue().getSkyColor());
            assertTrue(entry.getValue().getBackgroundMusic().isPresent());
        }
    }

    @Test
    void noCarvers() {
        for (Biome biome : BIOMES.values()) {
            assertFalse(biome.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR).iterator().hasNext());
        }
    }

    /** One global order: every biome's vegetation is a sub-sequence of {@link NetherRing3Features#VEGETATION}. */
    @Test
    void vegetationFollowsTheOneFixedOrder() {
        List<ResourceKey<PlacedFeature>> seen = new ArrayList<>();
        for (Biome biome : BIOMES.values()) {
            List<ResourceKey<PlacedFeature>> own = step(biome, GenerationStep.Decoration.VEGETAL_DECORATION);
            int last = -1;
            for (ResourceKey<PlacedFeature> key : own) {
                int index = NetherRing3Features.VEGETATION.indexOf(key);
                assertTrue(index > last, key.location() + " out of order");
                last = index;
            }
            seen.addAll(own);
        }
        assertTrue(seen.containsAll(NetherRing3Features.VEGETATION), "every vegetal feature is used");
        assertTrue(step(BIOMES.get(ModBiomes.RIME_FOREST), GenerationStep.Decoration.VEGETAL_DECORATION)
                .contains(NetherRing3Features.RIME_FOREST_RIMESHROOMS));
    }

    @Test
    void onlyOwnFeatures() {
        for (Biome biome : BIOMES.values()) {
            for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                for (ResourceKey<PlacedFeature> key : step(biome, step)) {
                    assertTrue(key.location().toString().startsWith("corruptedadventure:nether_ring_3/"), key.location().toString());
                }
            }
        }
    }

    /** G2-WN4 = A: strays, skeletons and ghasts; no piglins, hoglins or the rest of vanilla's Nether mix. */
    @Test
    void spawnsAreStraysSkeletonsAndGhasts() {
        for (Biome biome : BIOMES.values()) {
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.STRAY));
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.SKELETON));
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.GHAST));
            for (EntityType<?> absent : List.of(EntityType.PIGLIN, EntityType.HOGLIN, EntityType.ZOMBIFIED_PIGLIN,
                    EntityType.MAGMA_CUBE, EntityType.ENDERMAN, EntityType.STRIDER)) {
                for (MobCategory category : MobCategory.values()) {
                    assertFalse(spawns(biome, category, absent), absent.toString());
                }
            }
        }
    }

    /** The preset's Nether ring 3 source picks the biomes on the region axis at the terrain's thresholds. */
    @Test
    void presetPicksTheBiomesAtTheTerrainThresholds() {
        JsonObject source = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator").getAsJsonObject("biome_source")
                .getAsJsonObject("rings").getAsJsonObject("ring_3");
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
        double rime = NetherRing3Features.RIME_THRESHOLD, rifts = NetherRing3Features.RIFTS_THRESHOLD;
        assertEquals(3, temperature.size());
        assertEquals(List.of(rifts, rime), List.of(temperature.get("corruptedadventure:frozen_lava_sea")[0], temperature.get("corruptedadventure:frozen_lava_sea")[1]));
        assertEquals(List.of(rime, 2.0), List.of(temperature.get("corruptedadventure:rime_forest")[0], temperature.get("corruptedadventure:rime_forest")[1]));
        assertEquals(List.of(-2.0, rifts), List.of(temperature.get("corruptedadventure:glacier_rifts")[0], temperature.get("corruptedadventure:glacier_rifts")[1]));
    }

    private static boolean spawns(Biome biome, MobCategory category, EntityType<?> type) {
        return biome.getMobSettings().getMobs(category).unwrap().stream().anyMatch(data -> data.type == type);
    }

    private static List<ResourceKey<PlacedFeature>> step(Biome biome, GenerationStep.Decoration step) {
        List<HolderSet<PlacedFeature>> steps = biome.getGenerationSettings().features();
        return steps.size() <= step.ordinal() ? List.of()
                : steps.get(step.ordinal()).stream().map(holder -> holder.unwrapKey().orElseThrow()).toList();
    }

    private static Map<ResourceKey<Biome>, Biome> bootstrap() {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        NetherRing3Biomes.bootstrap(new BootstrapContext<>() {
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
