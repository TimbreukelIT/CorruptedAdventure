package nl.timtoys.corruptedadventure.worldgen.biome;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.feature.RingVanillaOres;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring2BiomesTest {
    @Test
    void bootstrapRegistersExactlyTheRing2List() {
        assertEquals(Set.copyOf(Ring2Biomes.ALL), bootstrap().keySet());
        assertEquals(Set.of(ModBiomes.CANYONLANDS, ModBiomes.OASIS_FLATS), Set.copyOf(Ring2Biomes.ALL));
    }

    @Test
    void dryHotAndTheRing2Palette() {
        BiomeAmbience.Palette palette = BiomeAmbience.Palette.RING_2;
        for (Biome biome : bootstrap().values()) {
            assertFalse(biome.hasPrecipitation());
            assertEquals(2.0F, biome.getBaseTemperature());
            BiomeSpecialEffects effects = biome.getSpecialEffects();
            assertEquals(palette.sky(), effects.getSkyColor());
            assertEquals(palette.fog(), effects.getFogColor());
            assertEquals(palette.water(), effects.getWaterColor());
            assertEquals(palette.waterFog(), effects.getWaterFogColor());
        }
    }

    @Test
    void desertSpawnsAndCamelsOnlyInTheOasis() {
        Map<ResourceKey<Biome>, Biome> biomes = bootstrap();
        for (Biome biome : biomes.values()) {
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.HUSK));
            assertTrue(spawns(biome, MobCategory.CREATURE, EntityType.RABBIT));
        }
        assertTrue(spawns(biomes.get(ModBiomes.OASIS_FLATS), MobCategory.CREATURE, EntityType.CAMEL));
        assertFalse(spawns(biomes.get(ModBiomes.CANYONLANDS), MobCategory.CREATURE, EntityType.CAMEL));
    }

    @Test
    void ring2BiomesAreNoVanillaCopiesSoGetNoMinecraftTags() {
        // ModBiomeTagsProvider only mirrors minecraft: tags onto the copies of vanilla biomes.
        for (ResourceKey<Biome> biome : Ring2Biomes.ALL) {
            assertFalse(VanillaBiomeCopies.COPIES.stream().map(VanillaBiomeCopies::copyOf).anyMatch(biome::equals), biome.toString());
        }
    }

    @Test
    void ownFeaturesLastInTheirStepInAFixedOrder() {
        Map<ResourceKey<Biome>, Biome> biomes = bootstrap();
        Biome canyonlands = biomes.get(ModBiomes.CANYONLANDS);
        List<ResourceKey<PlacedFeature>> rocks = step(canyonlands, GenerationStep.Decoration.LOCAL_MODIFICATIONS);
        assertEquals(Ring2Biomes.CANYONLANDS_ROCKS, rocks.subList(rocks.size() - Ring2Biomes.CANYONLANDS_ROCKS.size(), rocks.size()));
        assertEquals(ModPlacedFeatures.CANYON_SPRINGS, rocks.getLast(), "the spring last: it avoids the boulders of its chunk");
        assertEquals(ModPlacedFeatures.CANYONLANDS_VEGETATION, step(canyonlands, GenerationStep.Decoration.VEGETAL_DECORATION).getLast());

        Biome oasis = biomes.get(ModBiomes.OASIS_FLATS);
        assertEquals(ModPlacedFeatures.OASIS_ROCKS, step(oasis, GenerationStep.Decoration.LOCAL_MODIFICATIONS).getLast());
        // After vanilla's glow lichen only our own plants: vanilla's sugar cane random_patch is replaced by the
        // chunk-local shore plants.
        List<ResourceKey<PlacedFeature>> plants = step(oasis, GenerationStep.Decoration.VEGETAL_DECORATION);
        assertEquals(Ring2Biomes.OASIS_VEGETATION, plants.subList(plants.size() - Ring2Biomes.OASIS_VEGETATION.size(), plants.size()));
        assertFalse(plants.contains(VegetationPlacements.PATCH_SUGAR_CANE_DESERT));
        assertTrue(Ring2Biomes.OASIS_VEGETATION.indexOf(ModPlacedFeatures.OASIS_SHORE_PLANTS)
                < Ring2Biomes.OASIS_VEGETATION.indexOf(ModPlacedFeatures.OASIS_PALM_GROVES));
    }

    @Test
    void noVanillaOresButUnderwaterMagmaAndTheRing2OresThroughOneModifier() {
        for (Biome biome : bootstrap().values()) {
            List<ResourceKey<PlacedFeature>> ores = step(biome, GenerationStep.Decoration.UNDERGROUND_ORES);
            for (ResourceKey<PlacedFeature> ore : RingVanillaOres.ORES) {
                assertFalse(ores.contains(ore), ore.toString());
            }
            assertTrue(ores.contains(CavePlacements.UNDERWATER_MAGMA));
            assertTrue(ores.contains(MiscOverworldPlacements.DISK_GRAVEL));
        }
        List<ResourceKey<PlacedFeature>> added = Ring2BiomeModifiers.ores();
        assertEquals(RingVanillaOres.keys(Ring.RING_2), added.subList(0, RingVanillaOres.ORES.size()));
        assertEquals(List.of(ModPlacedFeatures.SUNSTONE_ORE, ModPlacedFeatures.TURQUOISE_ORE), added.subList(RingVanillaOres.ORES.size(), added.size()));
    }

    private static List<ResourceKey<PlacedFeature>> step(Biome biome, GenerationStep.Decoration step) {
        List<HolderSet<PlacedFeature>> steps = biome.getGenerationSettings().features();
        return steps.size() <= step.ordinal() ? List.of()
                : steps.get(step.ordinal()).stream().map(holder -> holder.unwrapKey().orElseThrow()).toList();
    }

    private static boolean spawns(Biome biome, MobCategory category, EntityType<?> type) {
        return biome.getMobSettings().getMobs(category).unwrap().stream().anyMatch(data -> data.type == type);
    }

    private static Map<ResourceKey<Biome>, Biome> bootstrap() {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        Ring2Biomes.bootstrap(new BootstrapContext<>() {
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
