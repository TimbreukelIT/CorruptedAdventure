package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
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

import java.util.List;

/**
 * Datagen bootstrap for the transition biomes, one per boundary, filling the whole transition zone
 * ({@code transitions} of {@link RingBiomeSource}).
 * <p>
 * Feature order: vanilla's helpers come first, in vanilla's order, and our own features last in each step. Every
 * biome of one biome source must agree on the relative order of shared features, or loading the world fails with
 * "Feature order cycle".
 */
public final class TransitionBiomes {
    /** How far the sky, fog and water of {@code withered_expanse} are faded towards grey, matching its faded grass. */
    static final float WITHERED_EXPANSE_FADE = 0.4F;

    /** How far the grass and foliage of {@code dry_riverbeds} are faded towards grey (worldgen, change 31; the sky, fog and water colours are {@code Palette.DRY_RIVERBEDS}). */
    static final float DRY_RIVERBEDS_FADE = 0.3F;
    /** Scorched ground, fossils, bones and stones of the riverbeds ({@code LOCAL_MODIFICATIONS}), in this fixed order. */
    static final List<ResourceKey<PlacedFeature>> DRY_RIVERBEDS_SHAPES = List.of(ModPlacedFeatures.DRY_RIVERBEDS_SCORCHED_PATCHES,
            ModPlacedFeatures.DRY_RIVERBEDS_SCORCHED_SPECKS, ModPlacedFeatures.DRY_RIVERBEDS_FOSSILS, ModPlacedFeatures.DRY_RIVERBEDS_BONES,
            ModPlacedFeatures.DRY_RIVERBEDS_STONES);
    /** Trees, scrub, grass and cactus groups of the riverbeds ({@code VEGETAL_DECORATION}), in this fixed order. */
    static final List<ResourceKey<PlacedFeature>> DRY_RIVERBEDS_VEGETATION = List.of(ModPlacedFeatures.DRY_RIVERBEDS_DEAD_TREES,
            ModPlacedFeatures.DRY_RIVERBEDS_VEGETATION, ModPlacedFeatures.DRY_RIVERBEDS_GRASS, ModPlacedFeatures.DRY_RIVERBEDS_CACTUS_GROUPS);

    private TransitionBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(ModBiomes.WITHERED_EXPANSE, witheredExpanse(features, carvers));
        context.register(ModBiomes.DRY_RIVERBEDS, dryRiverbeds(features, carvers));
    }

    /**
     * Transition 2→3: the dry beds of the stolen water, scorched ever blacker towards ring 3. A ring 2 biome in all but
     * name ({@link Ring2Biomes#ALL}: same vanilla features, ores, carvers and spawns), with its own features last, and
     * {@link BiomeAmbience.Palette#DRY_RIVERBEDS}.
     */
    private static Biome dryRiverbeds(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = Ring2Biomes.generation(features, carvers);
        for (ResourceKey<PlacedFeature> feature : DRY_RIVERBEDS_SHAPES) {
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, feature);
        }
        for (ResourceKey<PlacedFeature> feature : DRY_RIVERBEDS_VEGETATION) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
        BiomeAmbience.Palette colours = BiomeAmbience.Palette.DRY_RIVERBEDS;
        // Grass and foliage as the canyonlands, faded like the sky.
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(2.0F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(colours.sky())
                        .fogColor(colours.fog())
                        .waterColor(colours.water())
                        .waterFogColor(colours.waterFog())
                        .grassColorOverride(BiomeAmbience.desaturate(0xBFB755, DRY_RIVERBEDS_FADE))
                        .foliageColorOverride(BiomeAmbience.desaturate(0xAEA42A, DRY_RIVERBEDS_FADE))
                        .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                        .build())
                .mobSpawnSettings(Ring2Biomes.spawns(false))
                .generationSettings(generation.build())
                .build();
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

        // Ring 1 fades towards its edge: paler sky, fog and water than the rest of ring 1, and strange cave sounds.
        BiomeAmbience.Palette colours = BiomeAmbience.Palette.RING_1.desaturated(WITHERED_EXPANSE_FADE);
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(2.0F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(colours.sky())
                        .fogColor(colours.fog())
                        .waterColor(colours.water())
                        .waterFogColor(colours.waterFog())
                        .grassColorOverride(0xA8A070)
                        .foliageColorOverride(0x9A9060)
                        .ambientMoodSound(BiomeAmbience.STRANGE.orElseThrow())
                        .build())
                .mobSpawnSettings(spawns.build())
                .generationSettings(generation.build())
                .build();
    }

    /**
     * Same as vanilla's private {@code OverworldBiomes.globalOverworldGeneration}, in the same order. Ring 2 has its
     * own variant without local lava ({@link Ring2Biomes#generation}).
     */
    private static void globalOverworldGeneration(BiomeGenerationSettings.Builder generation) {
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
    }
}
