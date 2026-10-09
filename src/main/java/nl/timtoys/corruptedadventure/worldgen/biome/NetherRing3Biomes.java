package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing3Features;

import java.util.List;
import java.util.Map;

/**
 * Datagen bootstrap for the three biomes of Nether ring 3 (53b): the frozen lava sea (the main biome, open plain with
 * ice pillars), the rime forest (halls with frozen rimeshrooms) and the glacier rifts (ice mass with rifts and tunnels).
 * Owned by stream W-nether. Which lies where is decided by the {@code multi_noise} source of Nether ring 3 in the world
 * preset on the router's {@code temperature} (= {@code ring_3/nether/region}) at {@link NetherRing3Features#RIME_THRESHOLD}
 * and {@link NetherRing3Features#RIFTS_THRESHOLD}, the thresholds the terrain and the surface rule turn over at.
 * <p>
 * Every effect comes from {@link BiomeAmbience#netherRing3} (stream A); this class only adds the music. No carvers
 * (G2-WN5 = A: the terrain has its halls, rifts and tunnels, and the dam of step 3|4 stays whole). Features come in the one
 * order of {@link NetherRing3Features#VEGETATION}, each biome its own.
 */
public final class NetherRing3Biomes {
    /** Every biome of Nether ring 3, the only Java list of them (tags {@code is_nether_ring_3}, {@code c:is_cold/nether}). */
    public static final List<ResourceKey<Biome>> ALL = List.of(ModBiomes.FROZEN_LAVA_SEA, ModBiomes.RIME_FOREST, ModBiomes.GLACIER_RIFTS);

    /** The vegetal features per biome, each a sub-list of {@link NetherRing3Features#VEGETATION} in its order. */
    static final Map<ResourceKey<Biome>, List<ResourceKey<PlacedFeature>>> VEGETATION = Map.of(
            ModBiomes.FROZEN_LAVA_SEA, List.of(NetherRing3Features.FROZEN_LAVA_SEA_STALACTITES),
            ModBiomes.RIME_FOREST, List.of(NetherRing3Features.RIME_FOREST_RIMESHROOMS,
                    NetherRing3Features.RIME_FOREST_SMALL_RIMESHROOMS, NetherRing3Features.RIME_FOREST_STALACTITES),
            ModBiomes.GLACIER_RIFTS, List.of());

    /** Cold, no precipitation: only for the feel (water never exists in the Nether). */
    static final float TEMPERATURE = -0.5F;

    private NetherRing3Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        for (ResourceKey<Biome> key : ALL) {
            context.register(key, new Biome.BiomeBuilder()
                    .hasPrecipitation(false)
                    .temperature(TEMPERATURE)
                    .downfall(0.0F)
                    .specialEffects(BiomeAmbience.netherRing3(key)
                            // Vanilla's nether wastes music until the Nether has its own.
                            .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_NETHER_WASTES))
                            .build())
                    .mobSpawnSettings(spawns())
                    .generationSettings(generation(features, carvers, VEGETATION.get(key)).build())
                    .build());
        }
    }

    static BiomeGenerationSettings.Builder generation(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers,
                                                      List<ResourceKey<PlacedFeature>> vegetation) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        for (ResourceKey<PlacedFeature> feature : vegetation) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
        return generation;
    }

    /**
     * Cold and hostile (G2-WN4 = A): strays (slowness arrows, the frozen skeletons of phase 4 to come), skeletons and
     * ghasts over the open plain; no piglins, hoglins, endermen or striders. Strays need sky light by vanilla's rule;
     * {@link BiomeSpawnPlacements} lets them spawn in these biomes too.
     */
    static MobSpawnSettings spawns() {
        return new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.STRAY, 40, 2, 4))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 20, 2, 4))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.GHAST, 30, 4, 4))
                .build();
    }
}
