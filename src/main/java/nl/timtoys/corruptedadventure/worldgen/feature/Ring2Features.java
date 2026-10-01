package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;

import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.CANYONLANDS_VEGETATION;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.OASIS_VEGETATION;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.PALM_TREE;

/**
 * Datagen bootstraps for the features of ring 2 (changes 25 and 30 add theirs here). Amounts are starting values,
 * tuned in-game.
 * <p>
 * Every placement starts in its own chunk ({@code in_square}) and reads the {@code WORLD_SURFACE_WG} heightmap: during
 * the features step only the final heightmaps are updated, so this one shows the terrain alone, never trees or
 * leaves that neighbouring chunks already placed here. That keeps the positions independent of generation order.
 */
public final class Ring2Features {
    /** The palm trunk: 6–10 logs, leaning 1–2 blocks after the first third. */
    static final PalmTrunkPlacer PALM_TRUNK = new PalmTrunkPlacer(6, 4, 0, UniformInt.of(1, 2));
    /** The palm crown: straight fronds of 2–3 leaves plus a hanging one, 2–4 diagonal fronds. */
    static final PalmFoliagePlacer PALM_CROWN = new PalmFoliagePlacer(UniformInt.of(2, 3), ConstantInt.ZERO, UniformInt.of(2, 4));

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private Ring2Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Vanilla replaces any non-#dirt ground under the trunk (and grass) with the dirt provider: sand under a palm.
        FeatureUtils.register(context, PALM_TREE, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.PALM_LOG.get()),
                PALM_TRUNK,
                BlockStateProvider.simple(ModBlocks.PALM_LEAVES.get()),
                PALM_CROWN,
                new TwoLayersFeatureSize(1, 0, 1))
                .dirt(BlockStateProvider.simple(Blocks.SAND))
                .ignoreVines()
                .build());

        // The simple block feature checks survival of the chosen state itself.
        FeatureUtils.register(context, OASIS_VEGETATION, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 4)
                        .add(Blocks.FERN.defaultBlockState(), 1)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1))));
        FeatureUtils.register(context, CANYONLANDS_VEGETATION, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.DRY_SHRUB.get().defaultBlockState(), 30)
                        .add(Blocks.DEAD_BUSH.defaultBlockState(), 8)
                        .add(ModBlocks.BARREL_CACTUS.get().defaultBlockState(), 5)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1))));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);

        // Palms crowd the grassy shore of the pools; the grass filter rejects most tries elsewhere.
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_PALMS_SHORE, features.getOrThrow(PALM_TREE),
                CountPlacement.of(24),
                InSquarePlacement.spread(),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Direction.DOWN.getNormal(), Blocks.GRASS_BLOCK)),
                PlacementUtils.filteredByBlockSurvival(ModBlocks.PALM_SAPLING.get()),
                BiomeFilter.biome());
        // A scattered palm on the open sand.
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_PALMS_SAND, features.getOrThrow(PALM_TREE),
                RarityFilter.onAverageOnceEvery(4),
                InSquarePlacement.spread(),
                SURFACE,
                PlacementUtils.filteredByBlockSurvival(ModBlocks.PALM_SAPLING.get()),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_VEGETATION, features.getOrThrow(OASIS_VEGETATION),
                CountPlacement.of(6),
                InSquarePlacement.spread(),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(Direction.DOWN.getNormal(), Blocks.GRASS_BLOCK))),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.CANYONLANDS_VEGETATION, features.getOrThrow(CANYONLANDS_VEGETATION),
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                SURFACE,
                PlacementUtils.isEmpty(),
                BiomeFilter.biome());
    }
}
