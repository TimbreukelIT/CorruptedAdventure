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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Datagen bootstrap for the ring 3 overworld biomes ({@code docs/plans/ring-3/contract.md} §2): seven sub-biomes and the
 * micro-biome {@code dormant_caldera}; {@code steam_fens} (the transition 3→4) follows. Owned by stream W-biomes.
 * <p>
 * Which biome lies where is decided by the {@code multi_noise} source of ring 3 in the world preset, on the router's
 * {@code temperature} (= {@code ring_3/overworld/region_heat}) and {@code vegetation} (= {@code region_ground}); the
 * router's {@code weirdness} (= {@code caldera_dormant}) picks {@code dormant_caldera} anywhere. The hottest band holds
 * {@code volcanic_peaks} (mineral) and {@code basalt_flows}, the middle one {@code sulfur_fields} (mineral) and
 * {@code ember_woods}, the coolest {@code dried_seabed} (mineral), {@code ashen_wastes} and {@code titan_fields} (strange).
 * <p>
 * All dry and hot, with their own sky and fog so the sky turns at the ring boundary. The colours are provisional: the
 * ring 3 atmosphere (stream A) replaces them. No features yet (wave 2); no water lakes, no vanilla trees or grass.
 */
public final class Ring3Biomes {
    /**
     * Every biome with ring 3 worldgen, the only Java list of them: the tags {@code is_ring_3}, {@code c:is_hot/overworld}
     * and {@code c:is_dry/overworld} read it. Hand-written JSON that lists them is in {@code CODEBASE.md} ("Plekken die
     * ring 3-biomes opsommen").
     */
    public static final List<ResourceKey<Biome>> ALL = List.of(ModBiomes.ASHEN_WASTES, ModBiomes.VOLCANIC_PEAKS,
            ModBiomes.EMBER_WOODS, ModBiomes.BASALT_FLOWS, ModBiomes.SULFUR_FIELDS, ModBiomes.DRIED_SEABED,
            ModBiomes.TITAN_FIELDS, ModBiomes.DORMANT_CALDERA);

    /** Provisional ash-red base of the ring (sky, fog, water, water fog); each biome a nuance of it. */
    static final BiomeAmbience.Palette RING_3 = new BiomeAmbience.Palette(0xA8705C, 0x8A6A5A, 0x5E6A66, 0x1E2422);
    /** Provisional palette per biome, until stream A sets the ring 3 atmosphere. */
    static final Map<ResourceKey<Biome>, BiomeAmbience.Palette> PALETTES = Map.of(
            ModBiomes.ASHEN_WASTES, RING_3.withSky(0x9A8C86).withFog(0x7E7470),
            ModBiomes.VOLCANIC_PEAKS, RING_3.withSky(0xB4553C).withFog(0x8C4A3A),
            ModBiomes.EMBER_WOODS, RING_3.withSky(0xA8644A).withFog(0x7A4636),
            ModBiomes.BASALT_FLOWS, RING_3.withSky(0x8E6A60).withFog(0x5E4C48),
            ModBiomes.SULFUR_FIELDS, RING_3.withSky(0xB4A060).withFog(0xA89A62),
            ModBiomes.DRIED_SEABED, RING_3.withSky(0xB89A84).withFog(0xA89484),
            ModBiomes.TITAN_FIELDS, RING_3.withSky(0x8C7A90).withFog(0x6E6478),
            ModBiomes.DORMANT_CALDERA, RING_3.withSky(0x9CB0A0).withFog(0xA4B49C));

    /** Grass and foliage of the ash: grey olive; the dormant caldera is green. */
    private static final int ASH_GRASS = 0x8A8060;
    private static final int ASH_FOLIAGE = 0x7A7050;
    private static final int CALDERA_GRASS = 0x6FA040;
    private static final int CALDERA_FOLIAGE = 0x5E9032;

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

    private Ring3Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        for (ResourceKey<Biome> key : ALL) {
            boolean caldera = key.equals(ModBiomes.DORMANT_CALDERA);
            BiomeAmbience.Palette palette = Objects.requireNonNull(PALETTES.get(key), () -> "no palette for " + key.location());
            // Ring 2's hot, dry builder: one place for the ring-wide biome defaults.
            context.register(key, Ring2Biomes.biome(palette, caldera ? 0.4F : 0.0F, caldera ? CALDERA_GRASS : ASH_GRASS,
                    caldera ? CALDERA_FOLIAGE : ASH_FOLIAGE, Optional.empty(), spawns(key.equals(ModBiomes.VOLCANIC_PEAKS)),
                    generation(features, carvers)));
        }
    }

    /**
     * The shared base of every ring 3 biome, the one place for ring 3-wide generation: only carvers for now, vanilla's
     * carvers capped deep as in ring 2 ({@link Ring2Carvers}; near the surface they would leave sand and gravel hanging,
     * and ring 3's own cave work is change 42). No features: the ores (change 41) and the dressing come in wave 2.
     */
    static BiomeGenerationSettings.Builder generation(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        for (ResourceKey<ConfiguredWorldCarver<?>> carver : Ring2Carvers.COPIES.keySet()) {
            generation.addCarver(GenerationStep.Carving.AIR, carver);
        }
        return generation;
    }

    /**
     * Ring 2's desert monsters (husks, vanilla until phase 4) and bats, without animals (no cattle in ring 3); on the
     * peaks the goats.
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
            builder.creatureGenerationProbability(desert.getCreatureProbability());
        }
        return builder.build();
    }
}
