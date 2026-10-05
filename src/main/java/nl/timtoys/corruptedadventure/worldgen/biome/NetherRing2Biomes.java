package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModConfiguredCarvers;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing2Features;

import java.util.List;

/**
 * Datagen bootstrap for the two biomes of Nether ring 2 (change 35): the dripshroom forest (tall halls, the main biome)
 * and the spore caves (lower, denser). Which lies where is decided by the {@code multi_noise} source of Nether ring 2 in
 * the world preset on the router's {@code temperature} (= {@code ring_2/nether/region}); the terrain reads the same noise,
 * so the biome border coincides with the terrain transition.
 * <p>
 * Features come in vanilla's order per step with our own last, in the same relative order in both biomes (or loading the
 * world fails with "Feature order cycle"). No lava springs, fire, glowstone or basalt: they read their neighbours (generation
 * order) and do not fit the wet theme; the light comes from our own blocks.
 */
public final class NetherRing2Biomes {
    /** Every biome of Nether ring 2, the only Java list of them (tag {@code is_nether_ring_2}). */
    public static final List<ResourceKey<Biome>> ALL = List.of(ModBiomes.DRIPSHROOM_FOREST, ModBiomes.SPORE_CAVES);

    /** The vegetal decoration of the forest, in this fixed order; the caves' is the same order without the pillars. */
    static final List<ResourceKey<PlacedFeature>> FOREST_VEGETATION = List.of(ModPlacedFeatures.DRIPSHROOM_FOREST_PILLARS,
            ModPlacedFeatures.DRIPSHROOM_FOREST_FUNGI, ModPlacedFeatures.DRIPSHROOM_FOREST_STALACTITES,
            ModPlacedFeatures.DRIPSHROOM_FOREST_MOLD, ModPlacedFeatures.DRIPSHROOM_FOREST_SPROUTS);
    static final List<ResourceKey<PlacedFeature>> CAVES_VEGETATION = List.of(ModPlacedFeatures.SPORE_CAVES_FUNGI,
            ModPlacedFeatures.SPORE_CAVES_STALACTITES, ModPlacedFeatures.SPORE_CAVES_MOLD, ModPlacedFeatures.SPORE_CAVES_SPROUTS);

    private NetherRing2Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(ModBiomes.DRIPSHROOM_FOREST, biome(BiomeAmbience.dripshroomForestEffects(), generation(features, carvers, FOREST_VEGETATION)));
        context.register(ModBiomes.SPORE_CAVES, biome(BiomeAmbience.sporeCavesEffects(), generation(features, carvers, CAVES_VEGETATION)));
    }

    static BiomeGenerationSettings.Builder generation(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers,
                                                      List<ResourceKey<PlacedFeature>> vegetation) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        // Vanilla's nether_cave settings in our carver type: its lava follows the lava sea (Y 22) instead of a fixed Y 31.
        generation.addCarver(GenerationStep.Carving.AIR, ModConfiguredCarvers.NETHER_RING_2_CAVE);
        for (ResourceKey<PlacedFeature> ore : NetherRing2Features.ORES) {
            generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, ore);
        }
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, ModPlacedFeatures.DEW_GEODE);
        for (ResourceKey<PlacedFeature> feature : vegetation) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
        return generation;
    }

    /** Vanilla's Nether mix (zombified piglins, endermen, striders on the lava) with only the odd ghast; no piglins, hoglins or magma cubes. */
    static MobSpawnSettings spawns() {
        return new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.GHAST, 10, 4, 4))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.ZOMBIFIED_PIGLIN, 100, 4, 4))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.ENDERMAN, 1, 4, 4))
                .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.STRIDER, 60, 1, 2))
                .build();
    }

    /** Effects from {@link BiomeAmbience} (fog, drips, spores: change 36); the mood sound and the music are set here. */
    private static Biome biome(BiomeSpecialEffects.Builder effects, BiomeGenerationSettings.Builder generation) {
        effects.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                // Vanilla's nether wastes music until the Nether has its own.
                .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_NETHER_WASTES));
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(2.0F)
                .downfall(0.0F)
                .specialEffects(effects.build())
                .mobSpawnSettings(spawns())
                .generationSettings(generation.build())
                .build();
    }
}
