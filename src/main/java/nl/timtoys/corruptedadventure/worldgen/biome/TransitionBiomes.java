package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
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
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;

/**
 * Datagen bootstrap for the transition biomes, one per boundary, filling the whole transition zone
 * ({@code transitions} of {@link RingBiomeSource}).
 * <p>
 * Feature order: vanilla's helpers come first, in vanilla's order, and our own features last in each step. Every
 * biome of one biome source must agree on the relative order of shared features, or loading the world fails with
 * "Feature order cycle".
 */
public final class TransitionBiomes {
    private TransitionBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(ModBiomes.WITHERED_EXPANSE, witheredExpanse(features, carvers));
    }

    /** Transition 1→2: bare trees, faded grass and sandstone; dry like a savanna, the first sign of ring 2. */
    private static Biome witheredExpanse(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ModPlacedFeatures.WITHERED_EXPANSE_ROCKS);
        // Before the vegetation, so grass and trees do not end up on sand.
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ModPlacedFeatures.WITHERED_EXPANSE_SAND_PATCHES);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.WITHERED_EXPANSE_TREES);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.WITHERED_EXPANSE_VEGETATION);

        // Vanilla monsters at night; rabbits are the only animal (weight as in the desert). They only spawn on
        // #rabbits_spawnable_on, which lacks dead grass, so for now only on the sand patches (backlog, mobs).
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.commonSpawns(spawns);
        spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));

        // Starting colours; tuned in the atmosphere change for ring 1.
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(2.0F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0x8CB6F4)
                        .fogColor(0xD6D4C0)
                        .waterColor(0x4E8A92)
                        .waterFogColor(0x1A3336)
                        .grassColorOverride(0xA8A070)
                        .foliageColorOverride(0x9A9060)
                        .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                        .build())
                .mobSpawnSettings(spawns.build())
                .generationSettings(generation.build())
                .build();
    }

    /** Same as vanilla's private {@code OverworldBiomes.globalOverworldGeneration}, in the same order. */
    private static void globalOverworldGeneration(BiomeGenerationSettings.Builder generation) {
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
    }
}
