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
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing4Features;

import java.util.List;

/**
 * Datagen bootstrap for the three biomes of Nether ring 4 (65; {@code docs/plans/ring-4/contract.md} §2): the bone
 * dunes (the main biome: plains of bone sand above the sunken lava sea), the crystal spires (crystal pillars from floor
 * to ceiling) and the geode hollows (giant hollow geodes in a rock mass). Owned by stream W-nether. Which lies where is
 * decided by the {@code multi_noise} source of Nether ring 4 in the world preset on the router's {@code temperature}
 * (= {@code ring_4/nether/region}) at {@link NetherRing4Features#SPIRES_THRESHOLD} and
 * {@link NetherRing4Features#HOLLOWS_THRESHOLD}, the thresholds the terrain and the surface rule turn over at.
 * <p>
 * The sea-creature fossils of the dunes are terrain; the features dress the rest (78: the geode linings, the cladding
 * of the columns, loose bones and boulders in the dunes). No carvers: the dam of the steps 3|4 and 4|5 stays whole,
 * like Nether ring 3. The effects are the bone-desert fog of the old placeholder until stream A gives Nether
 * ring 4 its atmosphere (80, a hook in {@link BiomeAmbience} like {@code netherRing3}).
 */
public final class NetherRing4Biomes {
    /** Every biome of Nether ring 4, the only Java list of them (tags {@code is_nether_ring_4}, {@code purifying}, {@code c:}). */
    public static final List<ResourceKey<Biome>> ALL = List.of(ModBiomes.BONE_DUNES, ModBiomes.CRYSTAL_SPIRES, ModBiomes.GEODE_HOLLOWS);

    /** Hot and dry, no precipitation: a desert of bones and crystal above the lava sea. */
    static final float TEMPERATURE = 2.0F;
    /** The near-white beige fog of the bone desert (fase-1-wereld.md, Nether ring 4); A tunes it per biome in 80. */
    static final int FOG = 0xD8CBA6;
    private static final int WATER = 0x3F76E4;
    private static final int WATER_FOG = 0x050533;

    private NetherRing4Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        for (ResourceKey<Biome> key : ALL) {
            context.register(key, new Biome.BiomeBuilder()
                    .hasPrecipitation(false)
                    .temperature(TEMPERATURE)
                    .downfall(0.0F)
                    .specialEffects(new BiomeSpecialEffects.Builder()
                            .skyColor(0x000000)
                            .fogColor(FOG)
                            .waterColor(WATER)
                            .waterFogColor(WATER_FOG)
                            // Vanilla's soul sand valley music (a valley of bones) until the Nether has its own.
                            .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SOUL_SAND_VALLEY))
                            .build())
                    .mobSpawnSettings(spawns())
                    .generationSettings(generation(features, carvers, key))
                    .build());
        }
    }

    /** The dressing of 78 ({@link NetherRing4Features#features}); still no carvers (the dams stay whole). */
    static BiomeGenerationSettings generation(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers,
                                              ResourceKey<Biome> biome) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            for (ResourceKey<PlacedFeature> feature : NetherRing4Features.features(biome, step)) {
                generation.addFeature(step, features.getOrThrow(feature));
            }
        }
        return generation.build();
    }

    /**
     * A valley of bones: skeletons, ghasts and endermen like vanilla's soul sand valley, striders on the sunken lava sea;
     * no piglins or hoglins. Monsters stay vanilla until phase 4.
     */
    static MobSpawnSettings spawns() {
        return new MobSpawnSettings.Builder()
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.SKELETON, 20, 5, 5))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.GHAST, 50, 4, 4))
                .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.ENDERMAN, 1, 4, 4))
                .addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.STRIDER, 60, 1, 2))
                .addMobCharge(EntityType.SKELETON, 0.7, 0.15)
                .addMobCharge(EntityType.GHAST, 0.7, 0.15)
                .addMobCharge(EntityType.ENDERMAN, 0.7, 0.15)
                .addMobCharge(EntityType.STRIDER, 0.7, 0.15)
                .build();
    }
}
