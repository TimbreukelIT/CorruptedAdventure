package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseThresholdCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.List;

import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DEAD_TREE;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DEAD_TREE_FORKED;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.WITHERED_SANDSTONE_ROCK;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.WITHERED_SAND_PATCH;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.WITHERED_TREES;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.WITHERED_VEGETATION;
import static nl.timtoys.corruptedadventure.registry.ModPlacedFeatures.WITHERED_EXPANSE_ROCKS;
import static nl.timtoys.corruptedadventure.registry.ModPlacedFeatures.WITHERED_EXPANSE_SAND_PATCHES;
import static nl.timtoys.corruptedadventure.registry.ModPlacedFeatures.WITHERED_EXPANSE_TREES;
import static nl.timtoys.corruptedadventure.registry.ModPlacedFeatures.WITHERED_EXPANSE_VEGETATION;

/**
 * Datagen bootstraps for the features of the withered expanse (transition biome 1→2). Bare trees thin out towards
 * ring 2, sandstone rocks and sand patches grow denser ({@link RingChanceFilter} on the transition of boundary 1).
 * Amounts are starting values, tuned in-game.
 */
public final class WitheredExpanseFeatures {
    private WitheredExpanseFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);

        FeatureUtils.register(context, DEAD_TREE, Feature.TREE,
                bareTree(new StraightTrunkPlacer(4, 2, 0), new TwoLayersFeatureSize(1, 0, 1)));
        FeatureUtils.register(context, DEAD_TREE_FORKED, Feature.TREE,
                bareTree(new ForkingTrunkPlacer(5, 2, 2), new TwoLayersFeatureSize(1, 0, 2)));
        FeatureUtils.register(context, WITHERED_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(DEAD_TREE_FORKED)), 0.7F)),
                PlacementUtils.inlinePlaced(features.getOrThrow(DEAD_TREE))));

        FeatureUtils.register(context, WITHERED_VEGETATION, Feature.RANDOM_PATCH, FeatureUtils.simpleRandomPatchConfiguration(16,
                PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                        SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.SHORT_GRASS.defaultBlockState(), 3)
                                .add(Blocks.DEAD_BUSH.defaultBlockState(), 2))))));

        FeatureUtils.register(context, WITHERED_SANDSTONE_ROCK, Feature.FOREST_ROCK,
                new BlockStateConfiguration(Blocks.SANDSTONE.defaultBlockState()));

        // Sand with coarse dirt specks; sandstone where it would hang over air, like vanilla's sand disks.
        FeatureUtils.register(context, WITHERED_SAND_PATCH, Feature.DISK, new DiskConfiguration(
                new RuleBasedBlockStateProvider(
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.SAND.defaultBlockState(), 3)
                                .add(Blocks.COARSE_DIRT.defaultBlockState(), 1)),
                        List.of(new RuleBasedBlockStateProvider.Rule(
                                BlockPredicate.matchesBlocks(Direction.DOWN.getNormal(), Blocks.AIR),
                                BlockStateProvider.simple(Blocks.SANDSTONE)))),
                BlockPredicate.matchesBlocks(List.of(ModBlocks.DEAD_GRASS_BLOCK.get(), ModBlocks.WITHERED_DIRT.get(), Blocks.COARSE_DIRT)),
                UniformInt.of(2, 4),
                1));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);

        // Groves and open stretches; no sapling to check survival with, so the ground is checked directly.
        PlacementUtils.register(context, WITHERED_EXPANSE_TREES, features.getOrThrow(WITHERED_TREES),
                NoiseThresholdCountPlacement.of(0.0, 1, 5),
                InSquarePlacement.spread(),
                SurfaceWaterDepthFilter.forMaxDepth(0),
                PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesTag(Direction.DOWN.getNormal(), BlockTags.DIRT)),
                RingChanceFilter.transition(Ring.RING_1, 1.0, 0.2),
                BiomeFilter.biome());
        PlacementUtils.register(context, WITHERED_EXPANSE_VEGETATION, features.getOrThrow(WITHERED_VEGETATION),
                CountPlacement.of(3),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome());
        PlacementUtils.register(context, WITHERED_EXPANSE_ROCKS, features.getOrThrow(WITHERED_SANDSTONE_ROCK),
                RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(),
                RingChanceFilter.transition(Ring.RING_1, 0.1, 1.0),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome());
        PlacementUtils.register(context, WITHERED_EXPANSE_SAND_PATCHES, features.getOrThrow(WITHERED_SAND_PATCH),
                CountPlacement.of(2),
                InSquarePlacement.spread(),
                RingChanceFilter.transition(Ring.RING_1, 0.0, 0.8),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                BiomeFilter.biome());
    }

    /** A trunk without leaves: the foliage is air. Keeps the ground block when it is already {@code #dirt}. */
    private static TreeConfiguration bareTree(TrunkPlacer trunk, TwoLayersFeatureSize size) {
        return bareTree(trunk, size, ModBlocks.WITHERED_DIRT.get());
    }

    /** As {@link #bareTree(TrunkPlacer, TwoLayersFeatureSize)}, with {@code dirt} under a trunk that does not stand on {@code #dirt}. */
    static TreeConfiguration bareTree(TrunkPlacer trunk, TwoLayersFeatureSize size, Block dirt) {
        Block log = ModBlocks.DEAD_LOG.get();
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(log),
                trunk,
                BlockStateProvider.simple(Blocks.AIR),
                new BlobFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0), 0),
                size)
                .dirt(BlockStateProvider.simple(dirt))
                .ignoreVines()
                .build();
    }
}
