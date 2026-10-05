package nl.timtoys.corruptedadventure.worldgen.biome;

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
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherRing2BiomesTest {
    private static final Map<ResourceKey<Biome>, Biome> BIOMES = bootstrap();

    @Test
    void bothBiomesAreRegisteredAndListed() {
        assertEquals(List.of(ModBiomes.DRIPSHROOM_FOREST, ModBiomes.SPORE_CAVES), NetherRing2Biomes.ALL);
        assertEquals(NetherRing2Biomes.ALL, List.copyOf(BIOMES.keySet()));
    }

    @Test
    void hotAndDryWithoutPrecipitation() {
        for (Biome biome : BIOMES.values()) {
            assertFalse(biome.hasPrecipitation());
            assertEquals(2.0F, biome.getBaseTemperature());
            assertEquals(0.0F, biome.getModifiedClimateSettings().downfall());
        }
    }

    @Test
    void fogAndWaterComeFromTheAmbiencePalettes() {
        assertEquals(BiomeAmbience.Palette.DRIPSHROOM_FOREST.fog(), BIOMES.get(ModBiomes.DRIPSHROOM_FOREST).getFogColor());
        assertEquals(BiomeAmbience.Palette.SPORE_CAVES.fog(), BIOMES.get(ModBiomes.SPORE_CAVES).getFogColor());
        for (Biome biome : BIOMES.values()) {
            assertEquals(BiomeAmbience.Palette.NETHER_RING_2.water(), biome.getWaterColor());
            assertEquals(0x000000, biome.getSkyColor());
        }
    }

    @Test
    void carverIsTheRingNetherCave() {
        for (Biome biome : BIOMES.values()) {
            List<ResourceKey<?>> carvers = StreamSupport.stream(biome.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR).spliterator(), false)
                    .<ResourceKey<?>>map(holder -> holder.unwrapKey().orElseThrow()).toList();
            assertEquals(1, carvers.size());
            assertEquals("corruptedadventure:nether_ring_2/cave", carvers.get(0).location().toString());
        }
    }

    @Test
    void undergroundDecorationIsOresThenGeodeLiningInBothBiomes() {
        List<ResourceKey<PlacedFeature>> expected = new java.util.ArrayList<>(nl.timtoys.corruptedadventure.worldgen.feature.NetherRing2Features.ORES);
        expected.add(ModPlacedFeatures.DEW_GEODE);
        for (Biome biome : BIOMES.values()) {
            assertEquals(expected, step(biome, GenerationStep.Decoration.UNDERGROUND_DECORATION));
        }
    }

    @Test
    void vegetationFollowsTheFixedOrderAndOnlyTheForestHasPillars() {
        assertEquals(NetherRing2Biomes.FOREST_VEGETATION, step(BIOMES.get(ModBiomes.DRIPSHROOM_FOREST), GenerationStep.Decoration.VEGETAL_DECORATION));
        assertEquals(NetherRing2Biomes.CAVES_VEGETATION, step(BIOMES.get(ModBiomes.SPORE_CAVES), GenerationStep.Decoration.VEGETAL_DECORATION));
        assertTrue(NetherRing2Biomes.FOREST_VEGETATION.contains(ModPlacedFeatures.DRIPSHROOM_FOREST_PILLARS));
        assertFalse(NetherRing2Biomes.CAVES_VEGETATION.stream().anyMatch(key -> key.location().getPath().contains("pillars")));
    }

    @Test
    void noVanillaFeaturesAtAll() {
        // No lava springs, fire, glowstone, basalt or vanilla fungi: they read their neighbours and do not fit the theme.
        for (Biome biome : BIOMES.values()) {
            for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                for (ResourceKey<PlacedFeature> key : step(biome, step)) {
                    assertEquals("corruptedadventure", key.location().getNamespace(), key.location().toString());
                }
            }
        }
    }

    @Test
    void spawnsAreTheVanillaMixWithAnOccasionalGhast() {
        for (Biome biome : BIOMES.values()) {
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.ZOMBIFIED_PIGLIN));
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.ENDERMAN));
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.GHAST));
            assertTrue(spawns(biome, MobCategory.CREATURE, EntityType.STRIDER));
            assertFalse(spawns(biome, MobCategory.MONSTER, EntityType.PIGLIN));
            assertFalse(spawns(biome, MobCategory.MONSTER, EntityType.MAGMA_CUBE));
        }
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
        NetherRing2Biomes.bootstrap(new BootstrapContext<>() {
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
