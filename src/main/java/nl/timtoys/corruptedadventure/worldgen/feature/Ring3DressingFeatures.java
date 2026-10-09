package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;

import java.util.List;

/**
 * The dressing of the ring 3 biomes (change 47; G2-WB7–WB9 = A): fumaroles and mud in the sulfur fields, lava tongues,
 * hornitos, smouldering vents and boulders on the basalt flows, dead reefs, kelp bales, sea monster fossils and dead
 * bushes in the dried seabed, pumice boulders and charred dead trees on the ash plain, impact craters and blackstone
 * rocks on the peaks, and the ash drifts of the whole ring. Amounts are starting values, tuned at test stop 4b.
 * <p>
 * Determinism (rules 23c, plan §0.7): {@link JitteredGridPlacement} instead of {@code count} + {@code in_square}, the
 * margin of each grid at least the reach of its feature, so every feature writes only its own chunk; inputs are the
 * frozen {@code WORLD_SURFACE_WG} heightmap, the pure ring 3 terrain ({@link VolcanicFilter}), seeded noise and the
 * placement random. Everything on the ground goes through {@link VolcanicFilter.Builder#ground()} (plan §0.1–0.4).
 */
public final class Ring3DressingFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> FUMAROLE = configured("fumarole");
    static final ResourceKey<ConfiguredFeature<?, ?>> MUD_POOL = configured("mud_pool");
    static final ResourceKey<ConfiguredFeature<?, ?>> LAVA_TONGUE = configured("lava_tongue");
    static final ResourceKey<ConfiguredFeature<?, ?>> HORNITO = configured("hornito");
    static final ResourceKey<ConfiguredFeature<?, ?>> SMOLDERING_VENT = configured("smoldering_vent");
    static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_BOULDER = configured("basalt_boulder");
    static final ResourceKey<ConfiguredFeature<?, ?>> CORAL_REEF = configured("coral_reef");
    static final ResourceKey<ConfiguredFeature<?, ?>> KELP_BALE = configured("kelp_bale");
    static final ResourceKey<ConfiguredFeature<?, ?>> SEA_FOSSIL = configured("sea_fossil");
    static final ResourceKey<ConfiguredFeature<?, ?>> PUMICE_BOULDER = configured("pumice_boulder");
    static final ResourceKey<ConfiguredFeature<?, ?>> CHARRED_DEAD_TREE = configured("charred_dead_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> CHARRED_DEAD_TREE_FORKED = configured("charred_dead_tree_forked");
    static final ResourceKey<ConfiguredFeature<?, ?>> CHARRED_DEAD_TREES = configured("charred_dead_trees");
    static final ResourceKey<ConfiguredFeature<?, ?>> IMPACT_CRATER = configured("impact_crater");
    static final ResourceKey<ConfiguredFeature<?, ?>> BLACKSTONE_ROCK = configured("blackstone_rock");
    static final ResourceKey<ConfiguredFeature<?, ?>> ASH_DRIFT = configured("ash_drift");
    static final ResourceKey<ConfiguredFeature<?, ?>> DEAD_CORAL = configured("dead_coral");
    static final ResourceKey<ConfiguredFeature<?, ?>> DEAD_BUSH = configured("dead_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> CHARRED_STUMP = configured("charred_stump");

    // Placed, in the global order of their step (Ring3Features.ORDER).
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_MOUND_FUMAROLES = placed("sulfur_fields_mound_fumaroles");
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_FUMAROLES = placed("sulfur_fields_fumaroles");
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_MUD_POOLS = placed("sulfur_fields_mud_pools");
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_LAVA_TONGUES = placed("basalt_flows_lava_tongues");
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_HORNITOS = placed("basalt_flows_hornitos");
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_VENTS = placed("basalt_flows_vents");
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_BOULDERS = placed("basalt_flows_boulders");
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_CORAL_REEFS = placed("dried_seabed_coral_reefs");
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_KELP_BALES = placed("dried_seabed_kelp_bales");
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_FOSSILS = placed("dried_seabed_fossils");
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_PUMICE_BOULDERS = placed("ashen_wastes_pumice_boulders");
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_DEAD_TREES = placed("ashen_wastes_dead_trees");
    public static final ResourceKey<PlacedFeature> VOLCANIC_PEAKS_IMPACT_CRATERS = placed("volcanic_peaks_impact_craters");
    public static final ResourceKey<PlacedFeature> VOLCANIC_PEAKS_BLACKSTONE_ROCKS = placed("volcanic_peaks_blackstone_rocks");
    /** Last of LOCAL_MODIFICATIONS: the drifts lie around what stands, never on it. */
    public static final ResourceKey<PlacedFeature> ASH_DRIFTS = placed("ash_drifts");
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_DEAD_CORAL = placed("dried_seabed_dead_coral");
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_DEAD_BUSHES = placed("dried_seabed_dead_bushes");
    /** Added to {@code dry_riverbeds} by the biome modifier {@code add_ring_3_riverbed_stumps} (backlog r. 122). */
    public static final ResourceKey<PlacedFeature> DRY_RIVERBEDS_CHARRED_STUMPS = placed("dry_riverbeds_charred_stumps");

    /** The patchiness of the ash drifts. */
    static final ResourceKey<NormalNoise.NoiseParameters> ASH_DRIFT_NOISE = ResourceKey.create(Registries.NOISE, CorruptedAdventure.id("ring_3_ash_drift"));
    /** The dune wind of the terrain (Ring3Relief.WIND, 39d): the drifts lie in the lee of the dunes. */
    static final double WIND = 0.35;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private Ring3DressingFeatures() {
    }

    /** The open ground blocks of the ring 3 surface (biome tops, without lava, magma and rock walls). */
    static HolderSet<Block> openGround() {
        return HolderSet.direct(Block::builtInRegistryHolder, ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get(), ModBlocks.SULFUR_CRUST.get(),
                ModBlocks.SMOLDERING_ASH.get(), Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BLACKSTONE, Blocks.GRAVEL, Blocks.SAND,
                Blocks.CLAY, Blocks.CALCITE, Blocks.YELLOW_TERRACOTTA, Blocks.SOUL_SOIL, Blocks.TUFF);
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        HolderSet<Block> sulfurGround = blocks(ModBlocks.SULFUR_CRUST.get(), Blocks.CALCITE, Blocks.YELLOW_TERRACOTTA, Blocks.SMOOTH_BASALT,
                ModBlocks.ASH_BLOCK.get());
        HolderSet<Block> basaltGround = blocks(Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BLACKSTONE, ModBlocks.ASH_BLOCK.get());

        // Sulfur fields: a fumarole in a collar of sulfur crust, a few sulfur ore blocks under it.
        FeatureUtils.register(context, FUMAROLE, ModFeatures.GROUND_VENT.get(), new GroundVentConfiguration(
                ModBlocks.FUMAROLE.get().defaultBlockState(), BlockStateProvider.simple(ModBlocks.SULFUR_CRUST.get()), UniformInt.of(1, 2), 0.8F,
                sulfurGround, List.of(
                        OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), ModBlocks.SULFUR_ORE.get().defaultBlockState()),
                        OreConfiguration.target(new BlockMatchTest(Blocks.TUFF), ModBlocks.BASALT_SULFUR_ORE.get().defaultBlockState()),
                        OreConfiguration.target(new BlockMatchTest(Blocks.SMOOTH_BASALT), ModBlocks.BASALT_SULFUR_ORE.get().defaultBlockState()),
                        OreConfiguration.target(new BlockMatchTest(Blocks.BASALT), ModBlocks.BASALT_SULFUR_ORE.get().defaultBlockState())),
                UniformInt.of(1, 3)));
        FeatureUtils.register(context, MUD_POOL, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                BlockStateProvider.simple(ModBlocks.BOILING_MUD.get()), sulfurGround, UniformInt.of(1, 3), 1.3F, ConstantInt.of(1),
                ConstantInt.ZERO, BlockStateProvider.simple(ModBlocks.BOILING_MUD.get())));

        // Basalt flows (G2-WB8): long lobes of cooled lava, spatter cones with a glowing cap, smouldering vents, boulders.
        FeatureUtils.register(context, LAVA_TONGUE, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                BlockStateProvider.simple(Blocks.BLACKSTONE), basaltGround, UniformInt.of(2, 3), 2.5F, UniformInt.of(1, 2),
                UniformInt.of(0, 1), weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.BLACKSTONE.defaultBlockState(), 4)
                        .add(Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 1))));
        FeatureUtils.register(context, HORNITO, ModFeatures.HORNITO.get(), new HornitoConfiguration(
                weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.BASALT.defaultBlockState(), 3)
                        .add(Blocks.BLACKSTONE.defaultBlockState(), 2)),
                weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.MAGMA_BLOCK.defaultBlockState(), 3)
                        .add(ModBlocks.SMOLDERING_STONE.get().defaultBlockState(), 1)),
                UniformInt.of(2, HornitoFeature.MAX_HEIGHT), basaltGround));
        FeatureUtils.register(context, SMOLDERING_VENT, ModFeatures.GROUND_VENT.get(), new GroundVentConfiguration(
                ModBlocks.SMOLDERING_STONE.get().defaultBlockState(), weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.BLACKSTONE.defaultBlockState(), 2)
                        .add(ModBlocks.SMOLDERING_STONE.get().defaultBlockState(), 1)), UniformInt.of(1, 2), 0.6F,
                basaltGround, List.of(
                        OreConfiguration.target(new BlockMatchTest(Blocks.BASALT), Blocks.MAGMA_BLOCK.defaultBlockState()),
                        OreConfiguration.target(new BlockMatchTest(Blocks.BLACKSTONE), Blocks.MAGMA_BLOCK.defaultBlockState())),
                UniformInt.of(1, 2)));
        FeatureUtils.register(context, BASALT_BOULDER, ModFeatures.RUBBLE.get(), new RubbleConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.BASALT.defaultBlockState(), 3)
                        .add(Blocks.SMOOTH_BASALT.defaultBlockState(), 2)
                        .add(Blocks.BLACKSTONE.defaultBlockState(), 1)), UniformInt.of(1, 3)));

        // Dried seabed (G2-WB7): dead reefs on the ridges, kelp bales and dead bushes in the bowl, sea monster fossils.
        FeatureUtils.register(context, CORAL_REEF, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                deadCoralBlocks(), blocks(Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY, ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get()),
                UniformInt.of(2, 4), 1.6F, ConstantInt.of(1), UniformInt.of(1, 2), deadCoralBlocks()));
        FeatureUtils.register(context, KELP_BALE, ModFeatures.RUBBLE.get(), new RubbleConfiguration(
                BlockStateProvider.simple(Blocks.DRIED_KELP_BLOCK), UniformInt.of(1, 2)));
        FeatureUtils.register(context, SEA_FOSSIL, ModFeatures.SURFACE_FOSSIL.get(), new SurfaceFossilConfiguration(
                List.of(template("sea_serpent_1"), template("sea_serpent_2"), template("sea_serpent_3"), template("sea_skull_1"), template("sea_skull_2")),
                UniformFloat.of(0.3F, 0.6F), 0.85F, blocks(Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY, ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get())));
        FeatureUtils.register(context, DEAD_CORAL, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(deadCoralPlants()));
        FeatureUtils.register(context, DEAD_BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.DEAD_BUSH)));

        // Ash plain and peaks (G2-WB9).
        FeatureUtils.register(context, PUMICE_BOULDER, ModFeatures.RUBBLE.get(), new RubbleConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.PUMICE.get().defaultBlockState(), 4)
                        .add(ModBlocks.POLISHED_PUMICE.get().defaultBlockState(), 1)), UniformInt.of(1, 3)));
        FeatureUtils.register(context, CHARRED_DEAD_TREE, Feature.TREE, charredTree(new StraightTrunkPlacer(3, 2, 0), new TwoLayersFeatureSize(1, 0, 1)));
        FeatureUtils.register(context, CHARRED_DEAD_TREE_FORKED, Feature.TREE, charredTree(new ForkingTrunkPlacer(4, 2, 1), new TwoLayersFeatureSize(1, 0, 2)));
        FeatureUtils.register(context, CHARRED_DEAD_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(CHARRED_DEAD_TREE_FORKED)), 0.4F)),
                PlacementUtils.inlinePlaced(configured.getOrThrow(CHARRED_DEAD_TREE))));
        FeatureUtils.register(context, IMPACT_CRATER, ModFeatures.IMPACT_CRATER.get(), new ImpactCraterConfiguration(
                UniformInt.of(2, ImpactCraterFeature.MAX_RADIUS),
                weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.BLACKSTONE.defaultBlockState(), 3)
                        .add(Blocks.BASALT.defaultBlockState(), 1)),
                weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.OBSIDIAN.defaultBlockState(), 4)
                        .add(Blocks.CRYING_OBSIDIAN.defaultBlockState(), 1)),
                0.3F, Blocks.MAGMA_BLOCK.defaultBlockState(),
                blocks(Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BLACKSTONE, ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get())));
        FeatureUtils.register(context, BLACKSTONE_ROCK, ModFeatures.RUBBLE.get(), new RubbleConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.BLACKSTONE.defaultBlockState(), 3)
                        .add(Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 1)), UniformInt.of(2, 3)));

        // Ash drifts (backlog r. 193; decision 39f: also on the steps of the plain).
        FeatureUtils.register(context, ASH_DRIFT, ModFeatures.ASH_DRIFT.get(), new AshDriftConfiguration(
                ModBlocks.ASH_LAYER.get(), openGround(), ASH_DRIFT_NOISE, 1.0, WIND, List.of(
                        new AshDriftConfiguration.BiomeAmount(ModBiomes.ASHEN_WASTES, 1.0F, false, true),
                        new AshDriftConfiguration.BiomeAmount(ModBiomes.TITAN_FIELDS, 0.5F, false, true),
                        new AshDriftConfiguration.BiomeAmount(ModBiomes.VOLCANIC_PEAKS, 0.6F, true, true),
                        new AshDriftConfiguration.BiomeAmount(ModBiomes.DRIED_SEABED, 0.3F, false, false))));

        // Dry riverbeds (backlog r. 122): a charred stump of 1–2 here and there on the ash patches.
        FeatureUtils.register(context, CHARRED_STUMP, Feature.BLOCK_COLUMN, BlockColumnConfiguration.simple(
                UniformInt.of(1, 2), BlockStateProvider.simple(ModBlocks.CHARRED_LOG.get())));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementModifier ground = VolcanicFilter.builder().ground().build();

        // LOCAL_MODIFICATIONS.
        // Sulfur fields: a fumarole on the top of every fumarole mound (39f accessor) whose collar fits in the chunk ...
        PlacementUtils.register(context, SULFUR_FIELDS_MOUND_FUMAROLES, features.getOrThrow(FUMAROLE),
                TerrainLandmarkPlacement.of(TerrainLandmarkPlacement.Landmark.MOUND, 1.0F, 0, 2), SURFACE, ground, BiomeFilter.biome());
        // ... and about one per 3 chunks on the level floors of the terrace bowls.
        PlacementUtils.register(context, SULFUR_FIELDS_FUMAROLES, features.getOrThrow(FUMAROLE),
                JitteredGridPlacement.of(8, 2, 0.08F), SURFACE, SlopeFilter.flat(2, 1), ground, BiomeFilter.biome());
        // Mud on the floor of the sinkholes (8–16 deep, steep walls; 39f accessor), in the middle.
        PlacementUtils.register(context, SULFUR_FIELDS_MUD_POOLS, features.getOrThrow(MUD_POOL),
                TerrainLandmarkPlacement.of(TerrainLandmarkPlacement.Landmark.SINKHOLE, 1.0F, 1, 3), SURFACE,
                VolcanicFilter.builder().below(VolcanicOutput.Output.LAVA_BANK, 0.1).groundOnly(VolcanicFilter.DEFAULT_TOLERANCE).build(),
                BiomeFilter.biome());
        // Basalt flows.
        PlacementUtils.register(context, BASALT_FLOWS_LAVA_TONGUES, features.getOrThrow(LAVA_TONGUE),
                JitteredGridPlacement.of(16, 6, 0.3F), SURFACE, ground, BiomeFilter.biome());
        PlacementUtils.register(context, BASALT_FLOWS_HORNITOS, features.getOrThrow(HORNITO),
                JitteredGridPlacement.of(16, HornitoFeature.MAX_REACH, 0.2F), SURFACE, SlopeFilter.flat(3, 1), ground, BiomeFilter.biome());
        PlacementUtils.register(context, BASALT_FLOWS_VENTS, features.getOrThrow(SMOLDERING_VENT),
                JitteredGridPlacement.of(8, 2, 0.15F), SURFACE, SlopeFilter.flat(2, 1), ground, BiomeFilter.biome());
        PlacementUtils.register(context, BASALT_FLOWS_BOULDERS, features.getOrThrow(BASALT_BOULDER),
                JitteredGridPlacement.of(8, 2, 0.15F), SURFACE, ground, BiomeFilter.biome());
        // Dried seabed: reefs on the crests (nothing higher within 6, lower ground within 4).
        PlacementUtils.register(context, DRIED_SEABED_CORAL_REEFS, features.getOrThrow(CORAL_REEF),
                JitteredGridPlacement.of(16, 5, 0.7F), SURFACE, new SlopeFilter(6, SlopeFilter.Measure.RISE, Integer.MIN_VALUE, 0),
                new SlopeFilter(4, SlopeFilter.Measure.RELIEF, 2, Integer.MAX_VALUE), ground, BiomeFilter.biome());
        PlacementUtils.register(context, DRIED_SEABED_KELP_BALES, features.getOrThrow(KELP_BALE),
                JitteredGridPlacement.of(8, 2, 0.12F), SURFACE, ground, BiomeFilter.biome());
        PlacementUtils.register(context, DRIED_SEABED_FOSSILS, features.getOrThrow(SEA_FOSSIL),
                JitteredGridPlacement.of(16, 1, 0.12F), SURFACE, SlopeFilter.flat(3, 2), ground, BiomeFilter.biome());
        // Ash plain: pumice boulders; charred dead trees in the gullies (before the drifts, which then lie around them).
        PlacementUtils.register(context, ASHEN_WASTES_PUMICE_BOULDERS, features.getOrThrow(PUMICE_BOULDER),
                JitteredGridPlacement.of(8, 2, 0.12F), SURFACE, ground, BiomeFilter.biome());
        // Forked trunks lean up to 4 blocks: a margin of 4 keeps them in the chunk.
        PlacementUtils.register(context, ASHEN_WASTES_DEAD_TREES, features.getOrThrow(CHARRED_DEAD_TREES),
                JitteredGridPlacement.of(16, 4, 0.35F), SURFACE, SlopeFilter.wallFoot(4, 2), ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(below(), ModBlocks.ASH_BLOCK.get(), Blocks.GRAVEL, ModBlocks.PUMICE.get()))),
                BiomeFilter.biome());
        // Peaks: impact craters of old lava bombs and low blackstone rocks.
        PlacementUtils.register(context, VOLCANIC_PEAKS_IMPACT_CRATERS, features.getOrThrow(IMPACT_CRATER),
                JitteredGridPlacement.of(16, ImpactCraterFeature.MAX_RADIUS + 1, 0.15F), SURFACE, SlopeFilter.flat(4, 1), ground, BiomeFilter.biome());
        PlacementUtils.register(context, VOLCANIC_PEAKS_BLACKSTONE_ROCKS, features.getOrThrow(BLACKSTONE_ROCK),
                JitteredGridPlacement.of(8, 2, 0.12F), SURFACE, ground, BiomeFilter.biome());
        // Once per chunk that holds one of the drift biomes: the feature covers the chunk and each column's own biome
        // decides (a biome filter would test the chunk's corner at the bottom of the world).
        PlacementUtils.register(context, ASH_DRIFTS, features.getOrThrow(ASH_DRIFT));

        // VEGETAL_DECORATION. Dead coral on the reefs, read on the own chunk's live heightmap (the reef stands above
        // the frozen worldgen one).
        PlacementUtils.register(context, DRIED_SEABED_DEAD_CORAL, features.getOrThrow(DEAD_CORAL),
                JitteredGridPlacement.of(4, 0, 0.5F), HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(below(), Blocks.DEAD_BRAIN_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK,
                                Blocks.DEAD_FIRE_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK))),
                BiomeFilter.biome());
        PlacementUtils.register(context, DRIED_SEABED_DEAD_BUSHES, features.getOrThrow(DEAD_BUSH),
                JitteredGridPlacement.of(4, 0, 0.08F), SURFACE, ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(Blocks.DEAD_BUSH.defaultBlockState(), BlockPos.ZERO))),
                BiomeFilter.biome());
        // Dry riverbeds: stumps on the ash patches, which grow denser towards ring 3.
        PlacementUtils.register(context, DRY_RIVERBEDS_CHARRED_STUMPS, features.getOrThrow(CHARRED_STUMP),
                JitteredGridPlacement.of(8, 1, 0.25F), RingChanceFilter.transition(Ring.RING_2, 0.0, 1.0), SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(below(), ModBlocks.ASH_BLOCK.get()))),
                BiomeFilter.biome());
    }

    /** A bare charred tree (no crown); ash under the trunk where the ground is no {@code #dirt}. */
    private static TreeConfiguration charredTree(TrunkPlacer trunk, TwoLayersFeatureSize size) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.CHARRED_LOG.get()),
                trunk,
                BlockStateProvider.simple(Blocks.AIR),
                new BlobFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0), 0),
                size)
                .dirt(BlockStateProvider.simple(ModBlocks.ASH_BLOCK.get()))
                .ignoreVines()
                .build();
    }

    private static BlockStateProvider deadCoralBlocks() {
        return weighted(SimpleWeightedRandomList.<BlockState>builder()
                .add(Blocks.DEAD_BRAIN_CORAL_BLOCK.defaultBlockState(), 2)
                .add(Blocks.DEAD_BUBBLE_CORAL_BLOCK.defaultBlockState(), 1)
                .add(Blocks.DEAD_FIRE_CORAL_BLOCK.defaultBlockState(), 1)
                .add(Blocks.DEAD_HORN_CORAL_BLOCK.defaultBlockState(), 2)
                .add(Blocks.DEAD_TUBE_CORAL_BLOCK.defaultBlockState(), 1));
    }

    /** Dead coral and fans standing dry (vanilla's default state is waterlogged). */
    private static BlockStateProvider deadCoralPlants() {
        SimpleWeightedRandomList.Builder<BlockState> plants = SimpleWeightedRandomList.builder();
        for (Block block : List.of(Blocks.DEAD_BRAIN_CORAL, Blocks.DEAD_BUBBLE_CORAL, Blocks.DEAD_FIRE_CORAL, Blocks.DEAD_HORN_CORAL,
                Blocks.DEAD_TUBE_CORAL, Blocks.DEAD_BRAIN_CORAL_FAN, Blocks.DEAD_BUBBLE_CORAL_FAN, Blocks.DEAD_FIRE_CORAL_FAN,
                Blocks.DEAD_HORN_CORAL_FAN, Blocks.DEAD_TUBE_CORAL_FAN)) {
            plants.add(block.defaultBlockState().setValue(BaseCoralPlantTypeBlock.WATERLOGGED, false), 1);
        }
        return weighted(plants);
    }

    private static ResourceLocation template(String name) {
        return CorruptedAdventure.id("ring_3/" + name);
    }

    private static HolderSet<Block> blocks(Block... blocks) {
        return HolderSet.direct(Block::builtInRegistryHolder, blocks);
    }

    private static BlockStateProvider weighted(SimpleWeightedRandomList.Builder<BlockState> states) {
        return new WeightedStateProvider(states);
    }

    private static Vec3i below() {
        return Direction.DOWN.getNormal();
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
