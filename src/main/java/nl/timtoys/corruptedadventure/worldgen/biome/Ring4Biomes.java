package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
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
import nl.timtoys.corruptedadventure.worldgen.feature.Ring4Features;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Datagen bootstrap for the ring 4 overworld biomes ({@code docs/plans/ring-4/contract.md} §2, change 64): eight ground
 * biomes, two height biomes and the two transition biomes: {@code verdant_falls} on the descent of the zone 3→4
 * (its inner strip is ring 3's {@code steam_fens}) and {@code sundered_reach} over the zone 4→5. Owned by stream W-biomes.
 * <p>
 * Where the ground biomes lie is decided by the {@code multi_noise} source of ring 4 in the world preset: the router's
 * {@code weirdness} (= {@code ring_4/overworld/band}) splits the mythical band (below 0) from the blighted one (0 and
 * above, as {@code Ring4Band.blighted}); within a band {@code temperature} (= {@code region_water}, high = open water)
 * and {@code humidity} (= {@code region_growth}, high = overgrown) pick the biome, each an even share of its band.
 * The mythical half of the band's transition (−1 &lt; band &lt; 0, about 32 blocks) is always {@code mist_fjords}: the
 * last mythical biome before the blighted band ("into the mist"). The height biomes are not in that
 * source: the preset's {@code island_biomes} hook gives them to whole high islands. {@code sundered_reach} is the
 * preset's {@code transitions.ring_4}, a fixed biome over the zone 4→5; {@code verdant_falls} is the outer part of the
 * zone 3→4 ({@code transition_splits.ring_3}, after the steam fens' strip).
 * <p>
 * Every effect (colours, particles, sounds) comes from the atmosphere's hook {@link BiomeAmbience#ring4}. Features only in {@code verdant_falls} so far ({@link Ring4Features#ORDER}); no ores yet (77).
 */
public final class Ring4Biomes {
    /** The mythical band (inner ~60 %), in the order of the preset. */
    public static final List<ResourceKey<Biome>> MYTHIC = List.of(ModBiomes.CASCADE_VALE, ModBiomes.CRYSTAL_LAGOON,
            ModBiomes.LOTUS_SHALLOWS, ModBiomes.GLOWROOT_SWAMP, ModBiomes.MIST_FJORDS);
    /** The blighted band (outer ~40 %). */
    public static final List<ResourceKey<Biome>> BLIGHTED = List.of(ModBiomes.DROWNED_FOREST, ModBiomes.PEAT_MOORS, ModBiomes.REED_MARSH);
    /** The ground biomes: exactly the preset's ring 4 {@code multi_noise}. */
    public static final List<ResourceKey<Biome>> GROUND = Stream.concat(MYTHIC.stream(), BLIGHTED.stream()).toList();
    /** The height biomes of the preset's {@code island_biomes} hook: mythic, then blighted. */
    public static final List<ResourceKey<Biome>> HEIGHT = List.of(ModBiomes.SKY_GARDENS, ModBiomes.WITHERED_GARDENS);
    /** The biomes of the ring itself: ground and height. */
    public static final List<ResourceKey<Biome>> ALL = Stream.concat(GROUND.stream(), HEIGHT.stream()).toList();
    /**
     * The transition biomes of ring 4: {@code sundered_reach} over the zone 4→5 (the preset's {@code transitions.ring_4})
     * and {@code verdant_falls} on the descent of the zone 3→4 (the preset's {@code transition_splits.ring_3}).
     */
    public static final List<ResourceKey<Biome>> TRANSITIONS = List.of(ModBiomes.SUNDERED_REACH, ModBiomes.VERDANT_FALLS);
    /** Every biome with ring 4 worldgen: {@link #ALL} and the transitions (the bootstrap, {@code is_ring_4}, the surface branch). */
    public static final List<ResourceKey<Biome>> WITH_TRANSITION = Stream.concat(ALL.stream(), TRANSITIONS.stream()).toList();
    /** The tag {@code ring_4_blighted}: the blighted band and the islands above it (read by poison and weather). */
    public static final List<ResourceKey<Biome>> BLIGHTED_TAG = Stream.concat(BLIGHTED.stream(), Stream.of(ModBiomes.WITHERED_GARDENS)).toList();

    /** Mild and rainy everywhere: the ring weather picks the kind; high enough that nothing snows, even at Y 250. */
    static final float TEMPERATURE = 0.8F;
    static final float WET = 0.9F;
    static final float LAND = 0.6F;
    /** The waterfalls' descent: the wettest of the ring. */
    static final float FALLS = 0.95F;

    /**
     * Climate per biome: how much it rains and whether it does. The colours and sounds are the atmosphere's
     * ({@link BiomeAmbience#ring4}, change 80); {@code sundered_reach} has no precipitation (the abyss).
     */
    private static final Map<ResourceKey<Biome>, Climate> CLIMATES = Map.ofEntries(
            Map.entry(ModBiomes.CASCADE_VALE, new Climate(LAND, true)),
            Map.entry(ModBiomes.CRYSTAL_LAGOON, new Climate(WET, true)),
            Map.entry(ModBiomes.LOTUS_SHALLOWS, new Climate(WET, true)),
            Map.entry(ModBiomes.GLOWROOT_SWAMP, new Climate(WET, true)),
            Map.entry(ModBiomes.MIST_FJORDS, new Climate(WET, true)),
            Map.entry(ModBiomes.DROWNED_FOREST, new Climate(WET, true)),
            Map.entry(ModBiomes.PEAT_MOORS, new Climate(WET, true)),
            Map.entry(ModBiomes.REED_MARSH, new Climate(WET, true)),
            Map.entry(ModBiomes.SKY_GARDENS, new Climate(LAND, true)),
            Map.entry(ModBiomes.WITHERED_GARDENS, new Climate(LAND, true)),
            Map.entry(ModBiomes.SUNDERED_REACH, new Climate(LAND, false)),
            Map.entry(ModBiomes.VERDANT_FALLS, new Climate(FALLS, true)));

    private Ring4Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        for (ResourceKey<Biome> key : WITH_TRANSITION) {
            Climate climate = climate(key);
            context.register(key, new Biome.BiomeBuilder()
                    .hasPrecipitation(climate.precipitation())
                    .temperature(TEMPERATURE)
                    .downfall(climate.downfall())
                    .specialEffects(BiomeAmbience.ring4(key).build())
                    .mobSpawnSettings(spawns(key))
                    .generationSettings(generation(features, carvers, key).build())
                    .build());
        }
    }

    /** The climate of a ring 4 biome; throws for any other biome. */
    static Climate climate(ResourceKey<Biome> biome) {
        Climate climate = CLIMATES.get(biome);
        if (climate == null) {
            throw new IllegalArgumentException("no ring 4 climate for " + biome);
        }
        return climate;
    }

    /**
     * The generation of a ring 4 biome, the one place for ring 4-wide generation: vanilla's carvers capped deep as in
     * ring 2 and 3 ({@link Ring2Carvers}: near the surface they leave sand hanging and cut through lake floors; their
     * lava is already decided by the ring aquifer in ring 4, change 59), then the biome's own features in the global
     * order of {@link Ring4Features#ORDER}.
     */
    static BiomeGenerationSettings.Builder generation(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers,
                                                      ResourceKey<Biome> biome) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        for (ResourceKey<ConfiguredWorldCarver<?>> carver : Ring2Carvers.COPIES.keySet()) {
            generation.addCarver(GenerationStep.Carving.AIR, carver);
        }
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            for (ResourceKey<PlacedFeature> feature : Ring4Features.features(biome, step)) {
                generation.addFeature(step, feature);
            }
        }
        return generation;
    }

    /**
     * The animals per biome (contract §4, V4 = A): the lagoon tropical fish, turtles and dolphins, the swamp frogs and
     * axolotls (on the clay patches of its lake beds), the cascades foxes and rabbits; {@code verdant_falls} frogs on
     * its grass and turtles on the sand banks of its foot river (Q9 = A: no parrots until the own trees). The blighted
     * band, the withered islands and {@code sundered_reach} have none. Every ring 4 biome has an entry, so a missing one fails loudly.
     */
    private static final Map<ResourceKey<Biome>, List<Spawn>> ANIMALS = Map.ofEntries(
            Map.entry(ModBiomes.CASCADE_VALE, List.of(
                    new Spawn(MobCategory.CREATURE, EntityType.FOX, 8, 2, 4),
                    new Spawn(MobCategory.CREATURE, EntityType.RABBIT, 4, 2, 3),
                    new Spawn(MobCategory.WATER_AMBIENT, EntityType.SALMON, 5, 1, 5))),
            Map.entry(ModBiomes.CRYSTAL_LAGOON, List.of(
                    new Spawn(MobCategory.WATER_AMBIENT, EntityType.TROPICAL_FISH, 25, 8, 8),
                    new Spawn(MobCategory.WATER_AMBIENT, EntityType.PUFFERFISH, 15, 1, 3),
                    new Spawn(MobCategory.WATER_CREATURE, EntityType.DOLPHIN, 2, 1, 2),
                    new Spawn(MobCategory.CREATURE, EntityType.TURTLE, 5, 2, 5))),
            Map.entry(ModBiomes.LOTUS_SHALLOWS, List.of(
                    new Spawn(MobCategory.CREATURE, EntityType.FROG, 6, 2, 4),
                    new Spawn(MobCategory.WATER_AMBIENT, EntityType.SALMON, 5, 1, 5))),
            Map.entry(ModBiomes.GLOWROOT_SWAMP, List.of(
                    new Spawn(MobCategory.CREATURE, EntityType.FROG, 10, 2, 5),
                    new Spawn(MobCategory.AXOLOTLS, EntityType.AXOLOTL, 10, 4, 6),
                    new Spawn(MobCategory.WATER_AMBIENT, EntityType.TROPICAL_FISH, 25, 8, 8))),
            Map.entry(ModBiomes.MIST_FJORDS, List.of(
                    new Spawn(MobCategory.WATER_AMBIENT, EntityType.COD, 15, 3, 6),
                    new Spawn(MobCategory.WATER_AMBIENT, EntityType.SALMON, 15, 1, 5),
                    new Spawn(MobCategory.WATER_CREATURE, EntityType.SQUID, 3, 1, 4),
                    new Spawn(MobCategory.CREATURE, EntityType.RABBIT, 2, 1, 2))),
            Map.entry(ModBiomes.DROWNED_FOREST, List.of()),
            Map.entry(ModBiomes.PEAT_MOORS, List.of()),
            Map.entry(ModBiomes.REED_MARSH, List.of()),
            Map.entry(ModBiomes.SKY_GARDENS, List.of(new Spawn(MobCategory.CREATURE, EntityType.RABBIT, 4, 2, 3))),
            Map.entry(ModBiomes.WITHERED_GARDENS, List.of()),
            Map.entry(ModBiomes.SUNDERED_REACH, List.of()),
            Map.entry(ModBiomes.VERDANT_FALLS, List.of(
                    new Spawn(MobCategory.CREATURE, EntityType.FROG, 10, 2, 5),
                    new Spawn(MobCategory.CREATURE, EntityType.TURTLE, 5, 2, 5))));

    /** Vanilla's monsters and bats everywhere (until phase 4), plus the biome's {@link #ANIMALS}; throws for any other biome. */
    static MobSpawnSettings spawns(ResourceKey<Biome> biome) {
        List<Spawn> animals = ANIMALS.get(biome);
        if (animals == null) {
            throw new IllegalArgumentException("no ring 4 spawns for " + biome);
        }
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.commonSpawns(spawns);
        for (Spawn spawn : animals) {
            spawns.addSpawn(spawn.category(), new MobSpawnSettings.SpawnerData(spawn.type(), spawn.weight(), spawn.min(), spawn.max()));
        }
        return spawns.build();
    }

    /** One animal spawn: category, type, weight and group size. */
    private record Spawn(MobCategory category, EntityType<?> type, int weight, int min, int max) {
    }

    /** The downfall and whether it rains (snow never: too warm). */
    record Climate(float downfall, boolean precipitation) {
    }
}
