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
 * Datagen bootstrap for the biomes of ring 2. All share {@link #generation} (vanilla features in vanilla order, then
 * the ring 2 ores) and {@link BiomeAmbience.Palette#RING_2}; their own features come last in each step, or loading
 * the world fails with "Feature order cycle".
 * <p>
 * Which biome lies where is decided by the {@code multi_noise} source of ring 2 in the world preset, on the router's
 * {@code temperature} (= {@code ring_2/overworld/relief}): rocky (relief ≥ 0) is {@code canyonlands}, flat is
 * {@code oasis_flats}. The terrain reads the same density function, so biome and terrain borders coincide.
 */
public final class Ring2Biomes {
    /**
     * Every biome with ring 2 worldgen, the only Java list of them: the tags {@code is_ring_2},
     * {@code c:is_hot/overworld} and {@code c:is_dry/overworld} read it. Hand-written JSON that lists them is in
     * {@code CODEBASE.md} ("Plekken die ring 2-biomes opsommen").
     */
    public static final List<ResourceKey<Biome>> ALL = List.of(ModBiomes.CANYONLANDS, ModBiomes.OASIS_FLATS);
    /**
     * Rubble and springs of the canyonlands ({@code LOCAL_MODIFICATIONS}), in this fixed order; change 30 reuses them
     * in the same order. The spring comes last: it only digs where no boulder of its chunk lies.
     */
    static final List<ResourceKey<PlacedFeature>> CANYONLANDS_ROCKS = List.of(ModPlacedFeatures.CANYON_BOULDER_FIELDS,
            ModPlacedFeatures.CANYON_TALUS_RUBBLE, ModPlacedFeatures.CANYON_ROCKFALL, ModPlacedFeatures.PLATEAU_RUBBLE,
            ModPlacedFeatures.CANYON_SPRINGS);
    /** The plants and palms of the oasis ({@code VEGETAL_DECORATION}), from the water outwards, in this fixed order. */
    static final List<ResourceKey<PlacedFeature>> OASIS_VEGETATION = List.of(ModPlacedFeatures.OASIS_SHORE_CANE, ModPlacedFeatures.OASIS_SHORE_PLANTS,
            ModPlacedFeatures.OASIS_PALM_GROVES, ModPlacedFeatures.OASIS_PALMS_SAND, ModPlacedFeatures.OASIS_PALM_BUSHES,
            ModPlacedFeatures.OASIS_VEGETATION, ModPlacedFeatures.OASIS_TRANSITION_VEGETATION);

    private Ring2Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(ModBiomes.CANYONLANDS, canyonlands(features, carvers));
        context.register(ModBiomes.OASIS_FLATS, oasisFlats(features, carvers));
    }

    /** Deep canyons and plateaus of layered canyon stone; dry scrub. */
    private static Biome canyonlands(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = generation(features, carvers);
        for (ResourceKey<PlacedFeature> feature : CANYONLANDS_ROCKS) {
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, feature);
        }
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.CANYONLANDS_VEGETATION);
        return biome(0.0F, 0xBFB755, 0xAEA42A, spawns(false), generation);
    }

    /** Sand flats with pools, green shores and palms: the only safe water of the ring. */
    private static Biome oasisFlats(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = generation(features, carvers);
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ModPlacedFeatures.OASIS_ROCKS);
        for (ResourceKey<PlacedFeature> feature : OASIS_VEGETATION) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
        // Downfall only colours (and farmland); there is no rain in ring 2.
        return biome(0.4F, 0x6FB33F, 0x5DA032, spawns(true), generation);
    }

    /**
     * The shared base of every ring 2 biome: vanilla's global overworld features, ores and soft disks, in vanilla's
     * order. The one place for ring 2-wide generation: change 25 replaces the ores here, change 26 the lava lakes
     * (part of the global features).
     */
    static BiomeGenerationSettings.Builder generation(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        TransitionBiomes.globalOverworldGeneration(generation);
        // Temporary: vanilla's ores (they only replace #stone_ore_replaceables, so none in canyon stone) until change 25.
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        return generation;
    }

    /** Vanilla desert: monsters at night with husks instead of most zombies, rabbits; camels in the oasis. */
    static MobSpawnSettings spawns(boolean oasis) {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.desertSpawns(spawns);
        if (oasis) {
            spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.CAMEL, 1, 1, 1));
        }
        return spawns.build();
    }

    private static Biome biome(float downfall, int grass, int foliage, MobSpawnSettings spawns, BiomeGenerationSettings.Builder generation) {
        BiomeAmbience.Palette colours = BiomeAmbience.Palette.RING_2;
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(2.0F)
                .downfall(downfall)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(colours.sky())
                        .fogColor(colours.fog())
                        .waterColor(colours.water())
                        .waterFogColor(colours.waterFog())
                        .grassColorOverride(grass)
                        .foliageColorOverride(foliage)
                        .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                        .build())
                .mobSpawnSettings(spawns)
                .generationSettings(generation.build())
                .build();
    }
}
