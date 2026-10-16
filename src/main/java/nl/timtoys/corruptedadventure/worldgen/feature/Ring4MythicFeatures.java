package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.rootplacers.AboveRootPlacement;
import net.minecraft.world.level.levelgen.feature.rootplacers.MangroveRootPlacement;
import net.minecraft.world.level.levelgen.feature.rootplacers.MangroveRootPlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.AttachedToLeavesDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.CherryTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.UpwardsBranchingTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandOutput;

import java.util.List;
import java.util.Optional;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.FLOOR;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_LAND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_WATER;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.UNDER_WATER;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * The dressing of the mythical band and {@code sky_gardens} (package 75; contract ring 4 §2–§3, design "Biomes"), and
 * what all ground biomes share under the floating islands: the glow on the ground in their shadow (teststop 5a
 * decision; Q13 = B for the blighted palette) and what hangs from their undersides.
 * <ul>
 *   <li>{@code cascade_vale}: moonpetal trees (the cascade tree, also the sapling's {@code ring_4/moonpetal_tree}),
 *   grass and ferns, glow blooms along the lakes;</li>
 *   <li>{@code crystal_lagoon}: living coral, seagrass and sea pickles in the clear water;</li>
 *   <li>{@code lotus_shallows}: giant lily pads, lotus, reed accents on the banks, grass;</li>
 *   <li>{@code glowroot_swamp}: giant mangroves (vanilla mangrove wood and roots, no vines) with glowcaps and moss on
 *   their roots, glowcaps on the ground;</li>
 *   <li>{@code mist_fjords}: spruces, pines and a few dark oaks on the flat tops, moss and ferns;</li>
 *   <li>{@code sky_gardens}: moonpetal trees, glow blooms and flowers;</li>
 *   <li>bubble springs on the floor of deep pools (lagoon, lotus, swamp, fjords).</li>
 * </ul>
 * The hanging vines between island pairs and the plants around the sky lakes follow the terrain of package 74.
 * <p>
 * Determinism as in ring 2 and 3 (23c): placement in the own chunk on the frozen worldgen heightmaps
 * ({@link Ring4Placements}), {@link JitteredGridPlacement} cells, own-chunk features ({@link LocalCoralFeature},
 * {@link GiantLilyPadFeature}, {@link ReedClumpFeature}, {@link IslandGlowFeature}, {@link IslandUndersideFeature});
 * only the trees' crowns reach into the neighbours (the 3 × 3 window). Amounts are starting values, tuned at test
 * stop 5b.
 */
public final class Ring4MythicFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> SWAMP_TREE = configuredKey("swamp_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_TREE = configuredKey("fjord_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> MEADOW_GRASS = configuredKey("meadow_grass");
    static final ResourceKey<ConfiguredFeature<?, ?>> GLOW_BLOOM = configuredKey("glow_bloom");
    static final ResourceKey<ConfiguredFeature<?, ?>> GLOWCAPS = configuredKey("glowcaps");
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_FERNS = configuredKey("fjord_ferns");
    static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_CARPET = configuredKey("moss_carpet");
    static final ResourceKey<ConfiguredFeature<?, ?>> SKY_FLOWERS = configuredKey("sky_flowers");
    static final ResourceKey<ConfiguredFeature<?, ?>> LOTUS = configuredKey("lotus");
    static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_LILY_PAD = configuredKey("giant_lily_pad");
    static final ResourceKey<ConfiguredFeature<?, ?>> REED_ACCENT = configuredKey("reed_accent");
    static final ResourceKey<ConfiguredFeature<?, ?>> CORAL = configuredKey("coral");
    static final ResourceKey<ConfiguredFeature<?, ?>> SEAGRASS = configuredKey("seagrass");
    static final ResourceKey<ConfiguredFeature<?, ?>> SEA_PICKLES = configuredKey("sea_pickles");
    static final ResourceKey<ConfiguredFeature<?, ?>> BUBBLE_SPRING = configuredKey("bubble_spring");
    static final ResourceKey<ConfiguredFeature<?, ?>> ISLAND_GLOW = configuredKey("island_glow");
    static final ResourceKey<ConfiguredFeature<?, ?>> ISLAND_UNDERSIDE = configuredKey("island_underside");
    static final ResourceKey<ConfiguredFeature<?, ?>> ISLAND_VINE = configuredKey("island_vine");

    // Placed, in the global order of their step (Ring4Features.ORDER). All VEGETAL_DECORATION.
    /** Every ground biome: the glow on the dry land under the islands (before anything hangs from them). */
    public static final ResourceKey<PlacedFeature> ISLAND_GLOW_GROUND = placedKey("island_glow_ground");
    /** Every ground biome: moss strands and glowing roots under the islands. */
    public static final ResourceKey<PlacedFeature> ISLAND_UNDERSIDES = placedKey("island_undersides");
    /** Every ground biome: the vines between the island pairs (74). */
    public static final ResourceKey<PlacedFeature> ISLAND_VINES = placedKey("island_vines");
    public static final ResourceKey<PlacedFeature> CASCADE_VALE_TREES = placedKey("cascade_vale_trees");
    public static final ResourceKey<PlacedFeature> SKY_GARDENS_TREES = placedKey("sky_gardens_trees");
    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_TREES = placedKey("glowroot_swamp_trees");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_TREES = placedKey("mist_fjords_trees");
    public static final ResourceKey<PlacedFeature> LAGOON_CORAL = placedKey("crystal_lagoon_coral");
    public static final ResourceKey<PlacedFeature> LAGOON_SEAGRASS = placedKey("crystal_lagoon_seagrass");
    public static final ResourceKey<PlacedFeature> LAGOON_SEA_PICKLES = placedKey("crystal_lagoon_sea_pickles");
    public static final ResourceKey<PlacedFeature> SHALLOWS_SEAGRASS = placedKey("shallows_seagrass");
    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_GIANT_LILY_PADS = placedKey("lotus_shallows_giant_lily_pads");
    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_LOTUS = placedKey("lotus_shallows_lotus");
    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_REEDS = placedKey("lotus_shallows_reeds");
    public static final ResourceKey<PlacedFeature> DEEP_POOL_BUBBLE_SPRINGS = placedKey("deep_pool_bubble_springs");
    public static final ResourceKey<PlacedFeature> MYTHIC_GRASS = placedKey("mythic_grass");
    public static final ResourceKey<PlacedFeature> LAKESIDE_GLOW_BLOOMS = placedKey("lakeside_glow_blooms");
    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_GLOWCAPS = placedKey("glowroot_swamp_glowcaps");
    public static final ResourceKey<PlacedFeature> GLOWROOT_SWAMP_MOSS = placedKey("glowroot_swamp_moss");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_FERNS = placedKey("mist_fjords_ferns");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_MOSS = placedKey("mist_fjords_moss");
    public static final ResourceKey<PlacedFeature> SKY_GARDENS_FLOWERS = placedKey("sky_gardens_flowers");
    /** The banks of the sky lakes (74): more flowers round the water, none in it. */
    public static final ResourceKey<PlacedFeature> SKY_LAKE_BANK_FLOWERS = placedKey("sky_lake_bank_flowers");

    /** The light of a glow cell must reach its corners ({@link IslandGlowFeature}): 4 × 4 cells, sources of light ≥ 5. */
    static final int GLOW_SPACING = 4;

    private Ring4MythicFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        // The cascade tree: vanilla's cherry shape in moonpetal wood (white-silver bark, pink-lilac blossom); the sapling grows it.
        FeatureUtils.register(context, ModConfiguredFeatures.MOONPETAL_TREE, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.MOONPETAL_LOG.get()),
                new CherryTrunkPlacer(7, 1, 0, new WeightedListInt(SimpleWeightedRandomList.<IntProvider>builder()
                        .add(ConstantInt.of(1), 1).add(ConstantInt.of(2), 1).add(ConstantInt.of(3), 1).build()),
                        UniformInt.of(2, 4), UniformInt.of(-4, -3), UniformInt.of(-1, 0)),
                BlockStateProvider.simple(ModBlocks.MOONPETAL_LEAVES.get()),
                new CherryFoliagePlacer(ConstantInt.of(4), ConstantInt.of(0), ConstantInt.of(5), 0.25F, 0.5F, 0.16666667F, 0.33333334F),
                new TwoLayersFeatureSize(1, 0, 2)).ignoreVines().build());
        // The giant mangrove of the swamp: vanilla's tall mangrove a size up, glowcaps and moss on its roots, hanging
        // propagules, no vines (vanilla vines are not part of ring 4) and no bees.
        FeatureUtils.register(context, SWAMP_TREE, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.MANGROVE_LOG),
                new UpwardsBranchingTrunkPlacer(5, 2, 7, UniformInt.of(1, 6), 0.5F, UniformInt.of(0, 1),
                        blocks.getOrThrow(BlockTags.MANGROVE_LOGS_CAN_GROW_THROUGH)),
                BlockStateProvider.simple(Blocks.MANGROVE_LEAVES),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(2), 70),
                Optional.of(new MangroveRootPlacer(UniformInt.of(3, 7), BlockStateProvider.simple(Blocks.MANGROVE_ROOTS),
                        Optional.of(new AboveRootPlacement(weighted(Blocks.MOSS_CARPET.defaultBlockState(), 2,
                                ModBlocks.GLOWCAP.get().defaultBlockState(), 1), 0.5F)),
                        new MangroveRootPlacement(blocks.getOrThrow(BlockTags.MANGROVE_ROOTS_CAN_GROW_THROUGH),
                                HolderSet.direct(Block::builtInRegistryHolder, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS),
                                BlockStateProvider.simple(Blocks.MUDDY_MANGROVE_ROOTS), 8, 15, 0.2F))),
                new TwoLayersFeatureSize(3, 0, 2))
                .decorators(List.of(new AttachedToLeavesDecorator(0.14F, 1, 0, new RandomizedIntStateProvider(
                        BlockStateProvider.simple(Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.HANGING, true)),
                        MangrovePropaguleBlock.AGE, UniformInt.of(0, 4)), 2, List.of(Direction.DOWN))))
                .ignoreVines().build());
        // The dark, slender trees on the fjords' tops: spruce and pine, now and then a dark oak.
        FeatureUtils.register(context, FJORD_TREE, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(TreeFeatures.PINE)), 0.4F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(TreeFeatures.DARK_OAK)), 0.1F)),
                PlacementUtils.inlinePlaced(features.getOrThrow(TreeFeatures.SPRUCE))));

        simple(context, MEADOW_GRASS, weighted(Blocks.SHORT_GRASS.defaultBlockState(), 6, Blocks.FERN.defaultBlockState(), 1));
        simple(context, GLOW_BLOOM, BlockStateProvider.simple(ModBlocks.GLOW_BLOOM.get()));
        simple(context, GLOWCAPS, weighted(ModBlocks.GLOWCAP.get().defaultBlockState(), 1));
        simple(context, FJORD_FERNS, weighted(Blocks.FERN.defaultBlockState(), 4, Blocks.LARGE_FERN.defaultBlockState(), 1,
                Blocks.SHORT_GRASS.defaultBlockState(), 2));
        simple(context, MOSS_CARPET, BlockStateProvider.simple(Blocks.MOSS_CARPET));
        simple(context, SKY_FLOWERS, weighted(ModBlocks.GLOW_BLOOM.get().defaultBlockState(), 3, Blocks.ALLIUM.defaultBlockState(), 2,
                Blocks.AZURE_BLUET.defaultBlockState(), 2, Blocks.CORNFLOWER.defaultBlockState(), 2,
                Blocks.LILY_OF_THE_VALLEY.defaultBlockState(), 1));
        simple(context, LOTUS, BlockStateProvider.simple(ModBlocks.LOTUS.get()));
        FeatureUtils.register(context, GIANT_LILY_PAD, ModFeatures.GIANT_LILY_PAD.get(), NoneFeatureConfiguration.INSTANCE);
        FeatureUtils.register(context, REED_ACCENT, ModFeatures.REED_CLUMP.get(), new ReedClumpConfiguration(1, 0.6F, 2, 3));
        FeatureUtils.register(context, CORAL, ModFeatures.LOCAL_CORAL.get(), NoneFeatureConfiguration.INSTANCE);
        simple(context, SEAGRASS, BlockStateProvider.simple(Blocks.SEAGRASS));
        SimpleWeightedRandomList.Builder<BlockState> pickles = SimpleWeightedRandomList.builder();
        for (int n = 1; n <= 4; n++) {
            pickles.add(Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, n), 5 - n);
        }
        simple(context, SEA_PICKLES, new WeightedStateProvider(pickles));
        FeatureUtils.register(context, BUBBLE_SPRING, Feature.REPLACE_SINGLE_BLOCK, new ReplaceBlockConfiguration(List.of(
                OreConfiguration.target(AlwaysTrueTest.INSTANCE, ModBlocks.BUBBLE_SPRING.get().defaultBlockState()))));

        // Under the islands: mythical glow blooms and glowcaps with aquamarine buds; blighted glowcaps with toxic crystal
        // clusters, their buds and the faint blighted glowcap (Q13 = B, G4 = B; no budding block at the surface, H5 = A).
        FeatureUtils.register(context, ISLAND_GLOW, ModFeatures.ISLAND_GLOW.get(), new IslandGlowConfiguration(
                new IslandGlowConfiguration.Palette(
                        weighted(ModBlocks.GLOW_BLOOM.get().defaultBlockState(), 2, ModBlocks.GLOWCAP.get().defaultBlockState(), 1),
                        BlockStateProvider.simple(ModBlocks.GLOWCAP.get()),
                        weighted(ModBlocks.SMALL_AQUAMARINE_BUD.get().defaultBlockState(), 2, ModBlocks.MEDIUM_AQUAMARINE_BUD.get().defaultBlockState(), 2,
                                ModBlocks.LARGE_AQUAMARINE_BUD.get().defaultBlockState(), 1),
                        0.35F),
                // The light duty only for light 6 or more (the cluster's 5 leaves a far corner one block up dark).
                new IslandGlowConfiguration.Palette(
                        BlockStateProvider.simple(ModBlocks.GLOWCAP.get()),
                        BlockStateProvider.simple(ModBlocks.GLOWCAP.get()),
                        weighted(ModBlocks.TOXIC_CRYSTAL_CLUSTER.get().defaultBlockState(), 3, ModBlocks.BLIGHTED_GLOWCAP.get().defaultBlockState(), 3,
                                ModBlocks.SMALL_TOXIC_CRYSTAL_BUD.get().defaultBlockState(), 2, ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD.get().defaultBlockState(), 2,
                                ModBlocks.LARGE_TOXIC_CRYSTAL_BUD.get().defaultBlockState(), 1),
                        0.5F),
                GLOW_SPACING));
        FeatureUtils.register(context, ISLAND_UNDERSIDE, ModFeatures.ISLAND_UNDERSIDE.get(), new IslandUndersideConfiguration(
                ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(),
                weighted(ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState(), 3, Blocks.HANGING_ROOTS.defaultBlockState(), 1),
                0.05F, 2, 7, 0.004F, 40, 0.04F));
        // The vines between the islands: a rope of roots (it holds itself), moss and glowing roots hanging under it.
        FeatureUtils.register(context, ISLAND_VINE, ModFeatures.ISLAND_VINES.get(), new IslandVinesConfiguration(
                Blocks.MANGROVE_ROOTS.defaultBlockState(), ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(),
                ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState(), 0.08F, 0.4F, 0.2F, 0.06F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        Ring4Placements.perChunk(context, ISLAND_GLOW_GROUND, ISLAND_GLOW);
        Ring4Placements.perChunk(context, ISLAND_UNDERSIDES, ISLAND_UNDERSIDE);
        Ring4Placements.perChunk(context, ISLAND_VINES, ISLAND_VINE);

        // Trees: one cell of 8 holds at most one; the crowns reach into the neighbours (3 × 3 window).
        register(context, CASCADE_VALE_TREES, ModConfiguredFeatures.MOONPETAL_TREE, JitteredGridPlacement.of(8, 2, 0.4F), SURFACE,
                PlacementUtils.filteredByBlockSurvival(ModBlocks.MOONPETAL_SAPLING.get()));
        register(context, SKY_GARDENS_TREES, ModConfiguredFeatures.MOONPETAL_TREE, JitteredGridPlacement.of(16, 3, 0.6F), SURFACE,
                PlacementUtils.filteredByBlockSurvival(ModBlocks.MOONPETAL_SAPLING.get()));
        register(context, GLOWROOT_SWAMP_TREES, SWAMP_TREE, JitteredGridPlacement.of(8, 2, 0.35F), FLOOR,
                SurfaceWaterDepthFilter.forMaxDepth(4), PlacementUtils.filteredByBlockSurvival(Blocks.MANGROVE_PROPAGULE));
        register(context, MIST_FJORDS_TREES, FJORD_TREE, JitteredGridPlacement.of(8, 2, 0.35F), SURFACE,
                Ring4Filter.builder().below(IslandOutput.Output.SLOPE, 0.8).build(),
                PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));

        // Water: coral, seagrass and pickles in the lagoon; seagrass in the other clear waters; the pads and lotus on
        // still shallow water; bubble springs on the floor of deep pools (six or more deep).
        register(context, LAGOON_CORAL, CORAL, JitteredGridPlacement.of(4, 0, 0.3F), FLOOR, only(Ring4Placements.waterAbove(4)));
        register(context, LAGOON_SEAGRASS, SEAGRASS, JitteredGridPlacement.of(2, 0, 0.35F), FLOOR, only(UNDER_WATER));
        register(context, LAGOON_SEA_PICKLES, SEA_PICKLES, JitteredGridPlacement.of(4, 0, 0.08F), FLOOR, only(UNDER_WATER));
        register(context, SHALLOWS_SEAGRASS, SEAGRASS, JitteredGridPlacement.of(4, 0, 0.3F), FLOOR, only(UNDER_WATER));
        register(context, LOTUS_SHALLOWS_GIANT_LILY_PADS, GIANT_LILY_PAD, JitteredGridPlacement.of(8, 1, 0.45F), SURFACE, only(ON_WATER));
        register(context, LOTUS_SHALLOWS_LOTUS, LOTUS, JitteredGridPlacement.of(4, 0, 0.25F), SURFACE, only(ON_WATER));
        register(context, LOTUS_SHALLOWS_REEDS, REED_ACCENT, JitteredGridPlacement.of(16, 1, 0.5F), SURFACE,
                only(Ring4Placements.waterNear(2)));
        register(context, DEEP_POOL_BUBBLE_SPRINGS, BUBBLE_SPRING, JitteredGridPlacement.of(16, 1, 0.2F), FLOOR,
                only(Ring4Placements.waterAbove(6)), RandomOffsetPlacement.vertical(ConstantInt.of(-1)));

        // Land.
        register(context, MYTHIC_GRASS, MEADOW_GRASS, JitteredGridPlacement.of(2, 0, 0.3F), SURFACE, only(ON_LAND));
        register(context, LAKESIDE_GLOW_BLOOMS, GLOW_BLOOM, JitteredGridPlacement.of(4, 0, 0.35F), SURFACE,
                only(BlockPredicate.allOf(ON_LAND, Ring4Placements.waterNear(2))));
        register(context, GLOWROOT_SWAMP_GLOWCAPS, GLOWCAPS, JitteredGridPlacement.of(4, 0, 0.2F), SURFACE, only(ON_LAND));
        register(context, GLOWROOT_SWAMP_MOSS, MOSS_CARPET, JitteredGridPlacement.of(4, 0, 0.15F), SURFACE,
                only(BlockPredicate.allOf(ON_LAND, BlockPredicate.matchesBlocks(Ring4Placements.up(-1), Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK))));
        register(context, MIST_FJORDS_FERNS, FJORD_FERNS, JitteredGridPlacement.of(4, 0, 0.45F), SURFACE, only(ON_LAND));
        register(context, MIST_FJORDS_MOSS, MOSS_CARPET, JitteredGridPlacement.of(4, 0, 0.25F), SURFACE,
                only(BlockPredicate.allOf(ON_LAND, BlockPredicate.matchesBlocks(Ring4Placements.up(-1), Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK))));
        register(context, SKY_GARDENS_FLOWERS, SKY_FLOWERS, JitteredGridPlacement.of(4, 0, 0.4F), SURFACE, only(ON_LAND));
        register(context, SKY_LAKE_BANK_FLOWERS, SKY_FLOWERS, JitteredGridPlacement.of(2, 0, 0.5F), SURFACE,
                only(BlockPredicate.allOf(ON_LAND, Ring4Placements.waterNear(1))));
    }

    private static void simple(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
                               BlockStateProvider state) {
        FeatureUtils.register(context, key, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(state));
    }

    /** A weighted provider from (state, weight) pairs. */
    static WeightedStateProvider weighted(Object... stateWeightPairs) {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        for (int i = 0; i < stateWeightPairs.length; i += 2) {
            list.add((BlockState) stateWeightPairs[i], (Integer) stateWeightPairs[i + 1]);
        }
        return new WeightedStateProvider(list);
    }
}
