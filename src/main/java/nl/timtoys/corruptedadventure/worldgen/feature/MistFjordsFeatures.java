package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.ThreeLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.DarkOakFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.MegaPineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.PineFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.DarkOakTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.GiantTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandOutput;

import java.util.List;
import java.util.OptionalInt;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4MythicFeatures.weighted;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.GROVES;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_GROUND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.PATCHES_NOISE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * {@code mist_fjords} (ring 4 decor proef, 2026-10-19; decisions of the sparring 2026-10-19): rugged and rocky, the
 * trees in groves with open meadows between them.
 * <ul>
 *   <li>Trees (all {@link IsolatedTreeFeature}) on a grid of 4 in the groves of {@link Ring4Placements#GROVES}:
 *   spruces, young spruces, pines, now and then a giant 2 × 2 spruce, a dark oak or a dead snag; leaf bushes around
 *   and under them.</li>
 *   <li>The forest floor under the groves: podzol, moss and coarse dirt for the grass, dense ferns and large ferns,
 *   clumps of sweet berries.</li>
 *   <li>Mossy boulders and small rocks of stone, mossy cobblestone, andesite and tidestone, more on the open ground;
 *   stumps and fallen trunks with moss and glowcaps (the ring's mushroom) along the groves ({@link BoulderFeature}, {@link FallenTreeFeature}:
 *   ground objects, before the trees in the feature order so no tree roots in a rock).</li>
 * </ul>
 * Determinism: the grid cells and the noise are pure per seed; the objects and trees merge by
 * {@link TreeOverlap}; the plants use {@link GroundPlantFeature}. Amounts are starting values for the tuning loop.
 */
public final class MistFjordsFeatures {
    // Configured: the trees (keys of package 75 kept).
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_TREE = configuredKey("fjord_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_SPRUCE = configuredKey("fjord_spruce");
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_PINE = configuredKey("fjord_pine");
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_DARK_OAK = configuredKey("fjord_dark_oak");
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_YOUNG_SPRUCE = configuredKey("fjord_young_spruce");
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_GIANT_SPRUCE = configuredKey("fjord_giant_spruce");
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_SNAG = configuredKey("fjord_snag");
    static final ResourceKey<ConfiguredFeature<?, ?>> FJORD_BUSH = configuredKey("fjord_bush");
    // Configured: the ground.
    static final ResourceKey<ConfiguredFeature<?, ?>> FOREST_FLOOR = configuredKey("fjord_forest_floor");
    static final ResourceKey<ConfiguredFeature<?, ?>> UNDERGROWTH = configuredKey("fjord_undergrowth");
    static final ResourceKey<ConfiguredFeature<?, ?>> BERRY_BUSH = configuredKey("fjord_berry_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> BOULDER = configuredKey("fjord_boulder");
    static final ResourceKey<ConfiguredFeature<?, ?>> ROCK = configuredKey("fjord_rock");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_TREE = configuredKey("fjord_fallen_tree");

    // Placed, in the global order (Ring4Features.ORDER): floor and objects, trees, bushes, then the plants.
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_FOREST_FLOOR = placedKey("mist_fjords_forest_floor");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_BOULDERS = placedKey("mist_fjords_boulders");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_ROCKS = placedKey("mist_fjords_rocks");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_FALLEN_TREES = placedKey("mist_fjords_fallen_trees");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_TREES = placedKey("mist_fjords_trees");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_BUSHES = placedKey("mist_fjords_bushes");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_UNDERGROWTH = placedKey("mist_fjords_undergrowth");
    public static final ResourceKey<PlacedFeature> MIST_FJORDS_BERRIES = placedKey("mist_fjords_berries");

    /** The fixed seed of the forest floor's block noise (pure by position, the same in every world). */
    private static final long FOREST_FLOOR_SEED = 0xF0_2E57L;
    /** Trees stand on gentler slopes than this (package 75). */
    static final double TREE_SLOPE = 0.8;

    private MistFjordsFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        // The dark, slender trees: vanilla's spruce, pine and dark oak (TreeFeatures), as isolated trees (75).
        FeatureUtils.register(context, FJORD_SPRUCE, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.SPRUCE_LOG), new StraightTrunkPlacer(5, 2, 1), BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
                new SpruceFoliagePlacer(UniformInt.of(2, 3), UniformInt.of(0, 2), UniformInt.of(1, 2)),
                new TwoLayersFeatureSize(2, 0, 2)).ignoreVines().build());
        FeatureUtils.register(context, FJORD_PINE, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.SPRUCE_LOG), new StraightTrunkPlacer(6, 4, 0), BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
                new PineFoliagePlacer(ConstantInt.of(1), ConstantInt.of(1), UniformInt.of(3, 4)),
                new TwoLayersFeatureSize(2, 0, 2)).ignoreVines().build());
        FeatureUtils.register(context, FJORD_DARK_OAK, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.DARK_OAK_LOG), new DarkOakTrunkPlacer(6, 2, 1), BlockStateProvider.simple(Blocks.DARK_OAK_LEAVES),
                new DarkOakFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0)),
                new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty())).ignoreVines().build());
        // Decor proef: a young spruce (3–4 high, a small cone), the giant 2 × 2 spruce (vanilla's mega spruce, 13–19
        // high; without vanilla's podzol decorator, which would change the ground of a neighbouring chunk that its
        // own features read: the forest floor gives the podzol) and a dead snag (a bare trunk; its one "leaf" is wood,
        // as the withered trunks).
        FeatureUtils.register(context, FJORD_YOUNG_SPRUCE, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.SPRUCE_LOG), new StraightTrunkPlacer(3, 1, 0), BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
                new SpruceFoliagePlacer(UniformInt.of(1, 2), UniformInt.of(0, 1), UniformInt.of(1, 1)),
                new TwoLayersFeatureSize(1, 0, 1)).ignoreVines().build());
        FeatureUtils.register(context, FJORD_GIANT_SPRUCE, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.SPRUCE_LOG), new GiantTrunkPlacer(13, 2, 4), BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
                new MegaPineFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0), UniformInt.of(13, 17)),
                new TwoLayersFeatureSize(1, 1, 2)).ignoreVines().build());
        FeatureUtils.register(context, FJORD_SNAG, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.SPRUCE_LOG), new StraightTrunkPlacer(4, 3, 0), BlockStateProvider.simple(Blocks.SPRUCE_LOG),
                new BlobFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0), 0),
                new TwoLayersFeatureSize(1, 0, 1)).ignoreVines().build());
        // One tree per candidate, in this order of chances: giant 6 %, snag 6 %, dark oak 4 %, then of the rest young
        // 25 %, pine 25 %, else the spruce.
        FeatureUtils.register(context, FJORD_TREE, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(FJORD_GIANT_SPRUCE)), 0.06F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(FJORD_SNAG)), 0.06F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(FJORD_DARK_OAK)), 0.04F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(FJORD_YOUNG_SPRUCE)), 0.25F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(FJORD_PINE)), 0.33F)),
                PlacementUtils.inlinePlaced(features.getOrThrow(FJORD_SPRUCE))));
        // A leaf bush: one log under a ball of spruce leaves (vanilla's jungle bush shape).
        FeatureUtils.register(context, FJORD_BUSH, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.SPRUCE_LOG), new StraightTrunkPlacer(1, 0, 0), BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
                new BushFoliagePlacer(ConstantInt.of(2), ConstantInt.of(1), 2),
                new TwoLayersFeatureSize(0, 0, 0)).build());

        // The forest floor: the grass under the groves turns to podzol, now and then coarse dirt or moss. The block comes
        // from a fixed noise by position, not from the chunk's random, so no spot depends on an earlier one.
        FeatureUtils.register(context, FOREST_FLOOR, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new NoiseProvider(
                FOREST_FLOOR_SEED, new NormalNoise.NoiseParameters(-2, 1.0, 1.0), 1.0F, List.of(Blocks.MOSS_BLOCK.defaultBlockState(),
                Blocks.COARSE_DIRT.defaultBlockState(), Blocks.PODZOL.defaultBlockState(), Blocks.PODZOL.defaultBlockState(),
                Blocks.PODZOL.defaultBlockState(), Blocks.PODZOL.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(),
                Blocks.MOSS_BLOCK.defaultBlockState()))));
        groundPlant(context, UNDERGROWTH, weighted(Blocks.LARGE_FERN.defaultBlockState(), 3, Blocks.FERN.defaultBlockState(), 4,
                Blocks.SHORT_GRASS.defaultBlockState(), 1));
        groundPlant(context, BERRY_BUSH, weighted(Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 2), 1,
                Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3), 2));

        // Rocks: stone, mossy cobblestone, andesite and tidestone; moss on top.
        BlockStateProvider stone = weighted(Blocks.STONE.defaultBlockState(), 3, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3,
                Blocks.ANDESITE.defaultBlockState(), 2, ModBlocks.TIDESTONE.get().defaultBlockState(), 2, Blocks.COBBLESTONE.defaultBlockState(), 1);
        BlockStateProvider mossyTop = weighted(Blocks.MOSS_BLOCK.defaultBlockState(), 2, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 1);
        FeatureUtils.register(context, BOULDER, ModFeatures.BOULDER.get(), new BoulderConfiguration(stone, mossyTop, 0.45F,
                Blocks.MOSS_CARPET.defaultBlockState(), 0.25F, UniformFloat.of(1.8F, 3.4F), UniformFloat.of(2.0F, 4.0F)));
        FeatureUtils.register(context, ROCK, ModFeatures.BOULDER.get(), new BoulderConfiguration(stone, mossyTop, 0.3F,
                Blocks.MOSS_CARPET.defaultBlockState(), 0.15F, UniformFloat.of(0.6F, 1.5F), UniformFloat.of(0.8F, 1.8F)));
        FeatureUtils.register(context, FALLEN_TREE, ModFeatures.FALLEN_TREE.get(), new FallenTreeConfiguration(
                Blocks.SPRUCE_LOG.defaultBlockState(), UniformInt.of(1, 2), UniformInt.of(3, 6), 0.3F, 0.25F,
                weighted(Blocks.MOSS_CARPET.defaultBlockState(), 3, ModBlocks.GLOWCAP.get().defaultBlockState(), 1), 0.45F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        PlacementModifier ground = Ring4Filter.builder().ground().build();
        // The floor of the groves, before anything grows on it (a little wider than the trees' groves).
        register(context, MIST_FJORDS_FOREST_FLOOR, FOREST_FLOOR, JitteredGridPlacement.of(1, 0, 0.85F, ClusterDensity.of(
                        Ring4Placements.GROVES_NOISE, 0.05, 0.35)), SURFACE, ground, RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
                only(BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK)));
        // Rocks before the trees: a tree never roots in one (its sapling check finds the rock's foot), and the trees
        // never take a rock's stone (TreeOverlap.object). Boulders mostly on the open ground, small rocks everywhere.
        register(context, MIST_FJORDS_BOULDERS, BOULDER, JitteredGridPlacement.of(16, 1, 0.75F,
                ClusterDensity.of(Ring4Placements.GROVES_NOISE, 0.1, -0.2).withFloor(0.35)), SURFACE, ground);
        register(context, MIST_FJORDS_ROCKS, ROCK, JitteredGridPlacement.of(8, 1, 0.35F,
                ClusterDensity.of(Ring4Placements.GROVES_NOISE, 0.1, -0.2).withFloor(0.4)), SURFACE, ground);
        // Stumps and fallen trunks along and inside the groves, on gentle ground.
        register(context, MIST_FJORDS_FALLEN_TREES, FALLEN_TREE, JitteredGridPlacement.of(16, 1, 0.45F,
                        ClusterDensity.of(Ring4Placements.GROVES_NOISE, -0.25, 0.0)), SURFACE, ground,
                Ring4Filter.builder().below(IslandOutput.Output.SLOPE, 0.5).build());

        // The trees: dense groves (a grid of 4), open meadows between them.
        register(context, MIST_FJORDS_TREES, FJORD_TREE, JitteredGridPlacement.of(4, 1, 0.5F, ClusterDensity.of(
                        Ring4Placements.GROVES_NOISE, 0.0, 0.3)), SURFACE, only(ON_GROUND),
                Ring4Filter.builder().below(IslandOutput.Output.SLOPE, TREE_SLOPE).build(),
                PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
        // Leaf bushes round and under the groves, a few out on the meadows.
        register(context, MIST_FJORDS_BUSHES, FJORD_BUSH, JitteredGridPlacement.of(4, 1, 0.3F, ClusterDensity.of(
                        Ring4Placements.GROVES_NOISE, -0.15, 0.1).withSalt(1).withFloor(0.08)), SURFACE, only(ON_GROUND),
                Ring4Filter.builder().below(IslandOutput.Output.SLOPE, 1.0).build(),
                PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));

        // Under the groves: dense ferns; berry clumps where the patch noise is high.
        register(context, MIST_FJORDS_UNDERGROWTH, UNDERGROWTH, JitteredGridPlacement.of(2, 0, 0.6F, ClusterDensity.of(
                Ring4Placements.GROVES_NOISE, -0.05, 0.25)), SURFACE, only(ON_GROUND));
        register(context, MIST_FJORDS_BERRIES, BERRY_BUSH, JitteredGridPlacement.of(2, 0, 0.45F, ClusterDensity.of(
                PATCHES_NOISE, 0.3, 0.5).withSalt(1)), SURFACE, only(ON_GROUND));
    }

    /** A weighted ground plant ({@link GroundPlantFeature}). */
    private static void groundPlant(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
                                    BlockStateProvider state) {
        FeatureUtils.register(context, key, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(state));
    }
}
