package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring2Carvers;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring3Features;

import java.util.List;
import java.util.stream.Stream;

/**
 * Datagen bootstrap for the ring 3 overworld biomes ({@code docs/plans/ring-3/contract.md} §2): seven sub-biomes and the
 * micro-biome {@code dormant_caldera}, and the transition 3→4 {@code steam_fens} (change 48). Owned by stream W-biomes.
 * <p>
 * Which biome lies where is decided by the {@code multi_noise} source of ring 3 in the world preset, on the router's
 * {@code temperature} (= {@code ring_3/overworld/region_heat}) and {@code vegetation} (= {@code region_ground}); the
 * router's {@code weirdness} (= {@code caldera_dormant}) picks {@code dormant_caldera} anywhere. The hottest band holds
 * {@code volcanic_peaks} (mineral) and {@code basalt_flows}, the middle one {@code sulfur_fields} (mineral) and
 * {@code ember_woods}, the coolest {@code dried_seabed} (mineral), {@code ashen_wastes} and {@code titan_fields} (strange).
 * <p>
 * {@code steam_fens} is the preset's {@code transitions.ring_3} (a fixed biome over the zone 3→4), not in that source.
 * <p>
 * All hot (the steam fens wet, the rest dry), with their own sky and fog so the sky turns at the ring boundary. Every effect (colours, grass,
 * particles, sounds) comes from {@link BiomeAmbience#ring3} (stream A). Their features follow one global order
 * ({@link Ring3Features}); no water lakes, no vanilla trees or grass.
 */
public final class Ring3Biomes {
    /**
     * The biomes of the ring itself, the only Java list of them (the preset's ring 3 {@code multi_noise} holds exactly
     * these); {@link #WITH_TRANSITION} adds the transition. Hand-written JSON that lists them is in {@code CODEBASE.md}
     * ("Plekken die ring 3-biomes opsommen").
     */
    public static final List<ResourceKey<Biome>> ALL = List.of(ModBiomes.ASHEN_WASTES, ModBiomes.VOLCANIC_PEAKS,
            ModBiomes.EMBER_WOODS, ModBiomes.BASALT_FLOWS, ModBiomes.SULFUR_FIELDS, ModBiomes.DRIED_SEABED,
            ModBiomes.TITAN_FIELDS, ModBiomes.DORMANT_CALDERA);
    /** The transition 3→4 (change 48): the preset's {@code transitions.ring_3}. */
    public static final ResourceKey<Biome> TRANSITION = ModBiomes.STEAM_FENS;
    /**
     * Every biome with ring 3 worldgen: {@link #ALL} and the transition. The bootstrap, the tags {@code is_ring_3} and
     * {@code c:is_hot/overworld} and the ring 3 surface branch read it.
     */
    public static final List<ResourceKey<Biome>> WITH_TRANSITION = Stream.concat(ALL.stream(), Stream.of(TRANSITION)).toList();
    /** The steam fens are wet: the stolen water comes back there (no rain, it is too hot). */
    static final float STEAM_FENS_DOWNFALL = 0.9F;

    /**
     * Goats only on the peaks (contract §4, V7 = A): low weight, groups of 1–3, only on {@code #minecraft:goats_spawnable_on}
     * (vanilla's spawn rule; stream B adds basalt, blackstone and ash to that tag). They are the only animal of the biome,
     * so the weight alone does not make them rare: a low creature probability keeps them rare at chunk generation, and a
     * spawn cost (charge/budget, like vanilla's soul sand valley) keeps the later natural spawns sparse instead of filling
     * the creature cap. Starting values, tuned at test stop 4b.
     */
    static final int GOAT_WEIGHT = 1;
    static final int GOAT_MIN_GROUP = 1;
    static final int GOAT_MAX_GROUP = 3;
    static final float PEAKS_CREATURE_PROBABILITY = 0.03F;
    static final double GOAT_CHARGE = 1.0;
    static final double GOAT_ENERGY_BUDGET = 0.12;
    /**
     * Striders on the lava (G2-WB11 = A): vanilla's spawn rule puts them only on lava, so they live on the rivers and
     * crater lakes of every ring 3 biome but the peaks (there they would take the goats' tries). Rare through the spawn
     * cost, like the goats. Starting values, tuned at test stop 4b.
     */
    static final int STRIDER_WEIGHT = 1;
    static final int STRIDER_MIN_GROUP = 1;
    static final int STRIDER_MAX_GROUP = 2;
    static final double STRIDER_CHARGE = 1.0;
    static final double STRIDER_ENERGY_BUDGET = 0.12;

    private Ring3Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        for (ResourceKey<Biome> key : WITH_TRANSITION) {
            // Ring 2's hot, dry builder: one place for the ring-wide biome defaults. Every effect (colours, particles,
            // sounds) comes from stream A's hook.
            context.register(key, Ring2Biomes.biome(BiomeAmbience.ring3(key), downfall(key),
                    spawns(key.equals(ModBiomes.VOLCANIC_PEAKS)), generation(features, carvers, key)));
        }
    }

    private static float downfall(ResourceKey<Biome> key) {
        if (key.equals(TRANSITION)) {
            return STEAM_FENS_DOWNFALL;
        }
        return key.equals(ModBiomes.DORMANT_CALDERA) ? 0.4F : 0.0F;
    }

    /**
     * The generation of a ring 3 biome, the one place for ring 3-wide generation: vanilla's carvers capped deep as in
     * ring 2 ({@link Ring2Carvers}; near the surface they would leave sand and gravel hanging, and ring 3's own cave work
     * is change 42), then the biome's own features in the global ring 3 order ({@link Ring3Features#ORDER}). The ores
     * come through the biome modifier {@code add_ring_3_ores} ({@link Ring3BiomeModifiers}), never vanilla's own.
     */
    static BiomeGenerationSettings.Builder generation(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers,
                                                      ResourceKey<Biome> biome) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        for (ResourceKey<ConfiguredWorldCarver<?>> carver : Ring2Carvers.COPIES.keySet()) {
            generation.addCarver(GenerationStep.Carving.AIR, carver);
        }
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            for (ResourceKey<PlacedFeature> feature : Ring3Features.features(biome, step)) {
                generation.addFeature(step, feature);
            }
        }
        return generation;
    }

    /**
     * Ring 2's desert monsters (husks, vanilla until phase 4) and bats, without animals (no cattle in ring 3); on the
     * peaks the goats, elsewhere the striders on the lava.
     */
    static MobSpawnSettings spawns(boolean goats) {
        MobSpawnSettings desert = Ring2Biomes.spawns(false);
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        for (MobCategory category : MobCategory.values()) {
            if (category != MobCategory.CREATURE) {
                desert.getMobs(category).unwrap().forEach(data -> builder.addSpawn(category, data));
            }
        }
        if (goats) {
            builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, GOAT_WEIGHT, GOAT_MIN_GROUP, GOAT_MAX_GROUP));
            builder.addMobCharge(EntityType.GOAT, GOAT_CHARGE, GOAT_ENERGY_BUDGET);
            builder.creatureGenerationProbability(PEAKS_CREATURE_PROBABILITY);
        } else {
            builder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.STRIDER, STRIDER_WEIGHT, STRIDER_MIN_GROUP,
                    STRIDER_MAX_GROUP));
            builder.addMobCharge(EntityType.STRIDER, STRIDER_CHARGE, STRIDER_ENERGY_BUDGET);
            builder.creatureGenerationProbability(desert.getCreatureProbability());
        }
        return builder.build();
    }
}
