package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
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
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring2Carvers;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2RockFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2FlatsLifeFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2RockLandmarkFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2FlatLandmarkFeatures;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Datagen bootstrap for the biomes of ring 2. All share {@link #generation} (vanilla features in vanilla order; the
 * ring 2 ores come through {@link Ring2BiomeModifiers}) and their own {@link BiomeAmbience.Palette} (a nuance of {@code RING_2}); their own features
 * come last in each step, or loading the world fails with "Feature order cycle".
 * <p>
 * Which biome lies where is decided by the {@code multi_noise} source of ring 2 in the world preset, on two region axes:
 * the router's {@code temperature} (= {@code ring_2/overworld/relief}: rocky ≥ 0, flat below) and {@code vegetation}
 * (= {@code ring_2/overworld/moisture}). Rocky: {@code hoodoo_fields} (dry) and {@code canyonlands}; flat:
 * {@code salt_flats} (dry), {@code dune_sea} and {@code oasis_flats} (moist). The terrain reads the same density
 * functions, so biome and terrain borders coincide.
 */
public final class Ring2Biomes {
    /**
     * Every biome with ring 2 worldgen, the only Java list of them: the tags {@code is_ring_2},
     * {@code c:is_hot/overworld} and {@code c:is_dry/overworld} read it. Hand-written JSON that lists them is in
     * {@code CODEBASE.md} ("Plekken die ring 2-biomes opsommen").
     */
    public static final List<ResourceKey<Biome>> ALL = List.of(ModBiomes.CANYONLANDS, ModBiomes.OASIS_FLATS,
            ModBiomes.HOODOO_FIELDS, ModBiomes.SALT_FLATS, ModBiomes.DUNE_SEA, ModBiomes.DRY_RIVERBEDS);
    /**
     * Rubble and springs of the canyonlands ({@code LOCAL_MODIFICATIONS}), in this fixed order; change 30 reuses them
     * in the same order. The spring comes last: it only digs where no boulder of its chunk lies.
     */
    static final List<ResourceKey<PlacedFeature>> CANYONLANDS_ROCKS = List.of(ModPlacedFeatures.CANYON_BOULDER_FIELDS,
            ModPlacedFeatures.CANYON_TALUS_RUBBLE, ModPlacedFeatures.CANYON_ROCKFALL, ModPlacedFeatures.PLATEAU_RUBBLE,
            ModPlacedFeatures.CANYON_SPRINGS);
    /**
     * The plants and palms of the oasis ({@code VEGETAL_DECORATION}), from the water outwards, in this fixed order. The
     * flowers and shrubs (change 30c) come after the palms: a palm that stands blocks a shrub, never the other way round.
     */
    static final List<ResourceKey<PlacedFeature>> OASIS_VEGETATION = List.of(ModPlacedFeatures.OASIS_SHORE_CANE, ModPlacedFeatures.OASIS_SHORE_PLANTS,
            ModPlacedFeatures.OASIS_PALM_GROVES, ModPlacedFeatures.OASIS_PALMS_SAND, ModPlacedFeatures.OASIS_PALM_BUSHES,
            ModPlacedFeatures.OASIS_VEGETATION, ModPlacedFeatures.OASIS_FLOWERS_WARM, ModPlacedFeatures.OASIS_FLOWERS_WHITE,
            ModPlacedFeatures.OASIS_FLOWERS_PURPLE, ModPlacedFeatures.OASIS_EDGE_SHRUBS, ModPlacedFeatures.OASIS_TRANSITION_VEGETATION);
    /** The plants of the hoodoo fields ({@code VEGETAL_DECORATION}): between the pillars, at their feet, cactus groups. */
    static final List<ResourceKey<PlacedFeature>> HOODOO_FIELDS_VEGETATION = List.of(ModPlacedFeatures.HOODOO_FIELDS_VEGETATION,
            ModPlacedFeatures.HOODOO_PILLAR_FOOT_FLORA, ModPlacedFeatures.HOODOO_CACTUS_GROUPS);

    /**
     * Shapes and water of the salt flats ({@code LOCAL_MODIFICATIONS}, change 30b), in this fixed order: the brine pools
     * first, the pillars and rocks fail on their water.
     */
    static final List<ResourceKey<PlacedFeature>> SALT_FLATS_SHAPES = List.of(ModPlacedFeatures.SALT_FLATS_BRINE_POOLS,
            ModPlacedFeatures.SALT_FLATS_PILLAR_FIELDS, ModPlacedFeatures.SALT_FLATS_PILLARS, ModPlacedFeatures.SALT_ISLAND_ROCKS);
    /** Fossils and bones on the salt islands ({@code LOCAL_MODIFICATIONS}, change 30c), after the shapes. */
    static final List<ResourceKey<PlacedFeature>> SALT_ISLAND_BONES = List.of(ModPlacedFeatures.SALT_ISLAND_FOSSILS,
            ModPlacedFeatures.SALT_ISLAND_BONES);
    /** The crystals of the salt flats ({@code VEGETAL_DECORATION}): the single ones, then the fields (change 30b). */
    static final List<ResourceKey<PlacedFeature>> SALT_FLATS_CRYSTALS = List.of(ModPlacedFeatures.SALT_FLATS_CRYSTALS,
            ModPlacedFeatures.SALT_FLATS_CRYSTAL_FIELDS);
    /** Life on the salt islands ({@code VEGETAL_DECORATION}, change 30c), after the crystals. */
    static final List<ResourceKey<PlacedFeature>> SALT_ISLAND_VEGETATION = List.of(ModPlacedFeatures.SALT_ISLAND_FLORA,
            ModPlacedFeatures.SALT_ISLAND_CACTUS);
    /** Oases and rock islands of the dune sea ({@code LOCAL_MODIFICATIONS}, change 30b): the oases first. */
    static final List<ResourceKey<PlacedFeature>> DUNE_SEA_SHAPES = List.of(ModPlacedFeatures.DUNE_SEA_OASES, ModPlacedFeatures.DUNE_SEA_OUTCROPS);
    /** Fossils and bones of the dune sea ({@code LOCAL_MODIFICATIONS}, change 30c), after the shapes. */
    static final List<ResourceKey<PlacedFeature>> DUNE_SEA_BONES = List.of(ModPlacedFeatures.DUNE_SEA_FOSSILS, ModPlacedFeatures.DUNE_SEA_BONES);
    /** The plants of the dune sea ({@code VEGETAL_DECORATION}): flanks, then the valleys (change 30c); crests bare. */
    static final List<ResourceKey<PlacedFeature>> DUNE_SEA_VEGETATION = List.of(ModPlacedFeatures.DUNE_SEA_VEGETATION,
            ModPlacedFeatures.DUNE_SEA_VALLEY_FLORA, ModPlacedFeatures.DUNE_SEA_VALLEY_GRASS, ModPlacedFeatures.DUNE_SEA_CACTUS_GROUPS);

    /** G2 (G2-R2-3), after the island bones ({@code LOCAL_MODIFICATIONS}): pressure ridges on the crust in patches. */
    static final List<ResourceKey<PlacedFeature>> SALT_FLATS_RIDGES = List.of(Ring2FlatsLifeFeatures.SALT_FLATS_RIDGES);
    /** G2 (G2-R2-3), after the island vegetation ({@code VEGETAL_DECORATION}): bleached dead trees on the islands. */
    static final List<ResourceKey<PlacedFeature>> SALT_ISLAND_TREES = List.of(Ring2FlatsLifeFeatures.SALT_ISLAND_BLEACHED_TREES);
    /** G2 (G2-R2-2), after the dune plants ({@code VEGETAL_DECORATION}): thickets in the valleys, buried dead trees on the flanks. */
    static final List<ResourceKey<PlacedFeature>> DUNE_SEA_THICKETS = List.of(Ring2FlatsLifeFeatures.DUNE_SEA_THICKETS,
            Ring2FlatsLifeFeatures.DUNE_SEA_BURIED_TREES);
    /** G2 (G2-R2-7), after the oasis rocks ({@code LOCAL_MODIFICATIONS}): dry pool hollows on the open sand. */
    static final List<ResourceKey<PlacedFeature>> OASIS_HOLLOWS = List.of(Ring2FlatsLifeFeatures.OASIS_DRY_HOLLOWS);
    /** G2 (G2-R2-7), after the oasis vegetation ({@code VEGETAL_DECORATION}): scrub and dry grass on the open sand. */
    static final List<ResourceKey<PlacedFeature>> OASIS_SCRUB = List.of(Ring2FlatsLifeFeatures.OASIS_SCRUB,
            Ring2FlatsLifeFeatures.OASIS_SCRUB_GRASS);
    /**
     * The eye-catchers of the flat biomes (G2 ring 2, {@code Ring2FlatLandmarkFeatures}): each last in its step, after
     * everything of its chunk it may stand on or over; the giant palm's spring pool before the oasis plants.
     */
    static final List<ResourceKey<PlacedFeature>> FLAT_LANDMARKS = List.of(Ring2FlatLandmarkFeatures.SALT_FLATS_SALT_DOME,
            Ring2FlatLandmarkFeatures.DUNE_SEA_SKELETON, Ring2FlatLandmarkFeatures.OASIS_FLATS_GIANT_PALM_SPRING,
            Ring2FlatLandmarkFeatures.OASIS_FLATS_GIANT_PALM);

    /** In place of vanilla's soft disks, in vanilla's order. */
    static final List<ResourceKey<PlacedFeature>> DISKS = List.of(ModPlacedFeatures.RING_2_DISK_SAND, ModPlacedFeatures.RING_2_DISK_CLAY,
            ModPlacedFeatures.RING_2_DISK_GRAVEL);

    private Ring2Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(ModBiomes.CANYONLANDS, canyonlands(features, carvers));
        context.register(ModBiomes.OASIS_FLATS, oasisFlats(features, carvers));
        context.register(ModBiomes.HOODOO_FIELDS, hoodooFields(features, carvers));
        context.register(ModBiomes.SALT_FLATS, saltFlats(features, carvers));
        context.register(ModBiomes.DUNE_SEA, duneSea(features, carvers));
    }

    /** Deep canyons and plateaus of layered canyon stone; dry scrub. */
    private static Biome canyonlands(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = generation(features, carvers);
        for (ResourceKey<PlacedFeature> feature : CANYONLANDS_ROCKS) {
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, feature);
        }
        // G2: seeps after the springs, groves and seep greenery after the scrub (Ring2RockFeatures).
        for (ResourceKey<PlacedFeature> feature : Ring2RockFeatures.CANYONLANDS_SEEPS) {
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, feature);
        }
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, Ring2RockLandmarkFeatures.CANYONLANDS_ROCK_BRIDGES);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.CANYONLANDS_VEGETATION);
        for (ResourceKey<PlacedFeature> feature : Ring2RockFeatures.CANYONLANDS_GROVES) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
        return biome(BiomeAmbience.Palette.CANYONLANDS, 0.0F, 0xBFB755, 0xAEA42A, Optional.empty(), spawns(false), generation);
    }

    /** Sand flats with pools, green shores and palms: the only safe water of the ring. */
    private static Biome oasisFlats(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = generation(features, carvers);
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ModPlacedFeatures.OASIS_ROCKS);
        for (ResourceKey<PlacedFeature> feature : OASIS_HOLLOWS) {
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, feature);
        }
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, Ring2FlatLandmarkFeatures.OASIS_FLATS_GIANT_PALM_SPRING);
        for (ResourceKey<PlacedFeature> feature : concat(OASIS_VEGETATION, OASIS_SCRUB)) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, Ring2FlatLandmarkFeatures.OASIS_FLATS_GIANT_PALM);
        // Downfall only colours (and farmland); there is no rain in ring 2.
        return biome(BiomeAmbience.Palette.OASIS_FLATS, 0.4F, 0x6FB33F, 0x5DA032, OASIS_BIRDS, spawns(true), generation);
    }

    /**
     * Birds and insects of the oasis: the only ring 2 biome with an additions sound (the singing sand is client code on
     * the biome key). Soft and rare: vanilla's biomes use 0.0111 for loud ones.
     */
    static final Optional<AmbientAdditionsSettings> OASIS_BIRDS = Optional.of(new AmbientAdditionsSettings(ModSoundEvents.RING_2_OASIS, 0.004));

    /** Pillars, arches and balanced rocks of layered canyon stone; scrub and cacti between them, rock fallen at the pillar feet. */
    private static Biome hoodooFields(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = generation(features, carvers);
        // A subset of CANYONLANDS_ROCKS in the same order (one global order per step).
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ModPlacedFeatures.CANYON_ROCKFALL);
        for (ResourceKey<PlacedFeature> feature : Ring2RockFeatures.HOODOO_GULLIES) {
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, feature);
        }
        generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, Ring2RockLandmarkFeatures.HOODOO_FIELDS_GIANT_HOODOOS);
        for (ResourceKey<PlacedFeature> feature : HOODOO_FIELDS_VEGETATION) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
        return biome(BiomeAmbience.Palette.HOODOO_FIELDS, 0.0F, 0xBFB755, 0xAEA42A, Optional.empty(), spawns(false), generation);
    }

    /**
     * White and flat: salt crust cracked into polygons, brine pools, salt chimneys, crystal fields and low sand islands
     * with scrub, cacti and bones; nothing to eat, only monsters.
     */
    private static Biome saltFlats(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = generation(features, carvers);
        for (ResourceKey<PlacedFeature> feature : concat(concat(SALT_FLATS_SHAPES, SALT_ISLAND_BONES), SALT_FLATS_RIDGES)) {
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, feature);
        }
        for (ResourceKey<PlacedFeature> feature : concat(concat(SALT_FLATS_CRYSTALS, SALT_ISLAND_VEGETATION), SALT_ISLAND_TREES)) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, Ring2FlatLandmarkFeatures.SALT_FLATS_SALT_DOME);
        return biome(BiomeAmbience.Palette.SALT_FLATS, 0.0F, 0xC9C08A, 0xB5AC6E, Optional.empty(), withoutCreatures(spawns(false)), generation);
    }

    /**
     * High rolling dunes with bare crests and scrubby valleys (cactus groups, dry grass, half-buried fossils), a rare rock
     * island as a landmark, hidden oases in deep valleys.
     */
    private static Biome duneSea(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = generation(features, carvers);
        for (ResourceKey<PlacedFeature> feature : concat(DUNE_SEA_SHAPES, DUNE_SEA_BONES)) {
            generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, feature);
        }
        for (ResourceKey<PlacedFeature> feature : concat(DUNE_SEA_VEGETATION, DUNE_SEA_THICKETS)) {
            generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, feature);
        }
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, Ring2FlatLandmarkFeatures.DUNE_SEA_SKELETON);
        return biome(BiomeAmbience.Palette.DUNE_SEA, 0.0F, 0xBFB755, 0xAEA42A, Optional.empty(), spawns(false), generation);
    }

    /**
     * The shared base of every ring 2 biome: vanilla's global overworld features and soft disks, in vanilla's order,
     * without vanilla's ores (see {@link Ring2BiomeModifiers}) and without local lava (change 26: no lava lakes, no
     * lava springs; the aquifer turns its lava into water, {@code RingAquifer}). Vanilla's carvers only deep
     * ({@link Ring2Carvers}), its disks with a chunk-local origin ({@code Ring2Features.RING_2_DISKS}). The one place
     * for ring 2-wide generation.
     */
    static BiomeGenerationSettings.Builder generation(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(features, carvers);
        // TransitionBiomes.globalOverworldGeneration without the lava lakes and lava springs, and with vanilla's carvers
        // only deep (Ring2Carvers: near the surface they leave sand hanging over their tunnels).
        for (ResourceKey<ConfiguredWorldCarver<?>> carver : Ring2Carvers.COPIES.keySet()) {
            generation.addCarver(GenerationStep.Carving.AIR, carver);
        }
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        generation.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, MiscOverworldPlacements.SPRING_WATER);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
        // Of vanilla's addDefaultOres only the non-ore, in its place. The ores come tapered, with sunstone and turquoise,
        // through the biome modifier add_ring_2_ores (Ring2BiomeModifiers).
        generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, CavePlacements.UNDERWATER_MAGMA);
        for (ResourceKey<PlacedFeature> disk : DISKS) {
            generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, disk);
        }
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

    /** {@code spawns} without its creatures (animals); monsters, bats and the rest stay. */
    static MobSpawnSettings withoutCreatures(MobSpawnSettings spawns) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
        for (MobCategory category : MobCategory.values()) {
            if (category != MobCategory.CREATURE) {
                spawns.getMobs(category).unwrap().forEach(data -> builder.addSpawn(category, data));
            }
        }
        builder.creatureGenerationProbability(spawns.getCreatureProbability());
        return builder.build();
    }

    private static List<ResourceKey<PlacedFeature>> concat(List<ResourceKey<PlacedFeature>> first, List<ResourceKey<PlacedFeature>> then) {
        return Stream.concat(first.stream(), then.stream()).toList();
    }

    /** A hot, dry biome without rain (temperature 2.0, legacy cave mood). */
    static Biome biome(BiomeAmbience.Palette colours, float downfall, int grass, int foliage, Optional<AmbientAdditionsSettings> additions, MobSpawnSettings spawns, BiomeGenerationSettings.Builder generation) {
        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
                .skyColor(colours.sky())
                .fogColor(colours.fog())
                .waterColor(colours.water())
                .waterFogColor(colours.waterFog())
                .grassColorOverride(grass)
                .foliageColorOverride(foliage)
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
        additions.ifPresent(effects::ambientAdditionsSound);
        return biome(effects, downfall, spawns, generation);
    }

    /** A hot, dry biome without rain (temperature 2.0) with the given effects as they are; ring 3 builds its biomes here. */
    static Biome biome(BiomeSpecialEffects.Builder effects, float downfall, MobSpawnSettings spawns, BiomeGenerationSettings.Builder generation) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(2.0F)
                .downfall(downfall)
                .specialEffects(effects.build())
                .mobSpawnSettings(spawns)
                .generationSettings(generation.build())
                .build();
    }
}
