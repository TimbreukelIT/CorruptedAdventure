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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.BONE_RUBBLE;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DRY_GRASS;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.RIVERBED_DEAD_TREE;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.RIVERBED_DEAD_TREES;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.RIVERBED_DEAD_TREE_FORKED;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.RIVERBED_FLORA;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.RIVERBED_FOSSIL;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.RIVERBED_SCORCHED_PATCH;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.RIVERBED_SCORCHED_SPECK;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.RIVERBED_STONES;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.SHORT_CACTUS_COLUMN;

/**
 * Datagen bootstraps for the features of the dry riverbeds (transition biome 2→3, change 31): scorched patches that
 * grow denser towards ring 3, half-buried fossils and loose bones, stones the water carried, dead trees along the banks
 * and sparse scrub, dry grass and cactus groups. Fossils, bones, stones, trees and scrub reuse the features of
 * {@link Ring2DressingFeatures} and {@link WitheredExpanseFeatures}; only their configuration and placement are new.
 * Amounts are starting values, tuned in-game.
 * <p>
 * Same determinism rules as {@link Ring2Features}: {@link JitteredGridPlacement} instead of {@code count} +
 * {@code in_square}, everything in its own chunk (the disks of the scorched patches with a margin of their radius + 1),
 * only {@code WORLD_SURFACE_WG} and seeded noise as input.
 */
public final class DryRiverbedsFeatures {
    /** The ground of a riverbed: the clay of the bed, the coarse banks, the sand around, the gravel specks. */
    static List<Block> ground() {
        return List.of(Blocks.RED_SAND, Blocks.COARSE_DIRT, ModBlocks.CRACKED_CLAY.get(), Blocks.GRAVEL);
    }

    /** Largest radius of a scorched patch; {@link #PATCH_MARGIN} keeps its whole disk inside the chunk. */
    static final int PATCH_RADIUS = 5;
    static final int PATCH_MARGIN = PATCH_RADIUS + 1;
    static final int SPECK_RADIUS = 2;
    static final int SPECK_MARGIN = SPECK_RADIUS + 1;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    /** The top solid block, as vanilla's disks: the origin is the air above it. */
    private static final PlacementModifier TOP_SOLID = HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG);

    private DryRiverbedsFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Scorched ground: blackstone and basalt (placeholders for the ash of ring 3, change 38/39), scorched sandstone
        // where the disk would hang over air, like vanilla's sand disks. Only the ground of a riverbed turns black.
        FeatureUtils.register(context, RIVERBED_SCORCHED_PATCH, Feature.DISK, scorchedDisk(UniformInt.of(3, PATCH_RADIUS)));
        FeatureUtils.register(context, RIVERBED_SCORCHED_SPECK, Feature.DISK, scorchedDisk(UniformInt.of(1, SPECK_RADIUS)));
        // The fossil templates of the desert, on the ground of a riverbed.
        FeatureUtils.register(context, RIVERBED_FOSSIL, ModFeatures.SURFACE_FOSSIL.get(), new SurfaceFossilConfiguration(
                Stream.of("spine", "skull").flatMap(kind -> IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> ResourceLocation.withDefaultNamespace("fossil/" + kind + "_" + i))).toList(),
                UniformFloat.of(0.4F, 0.7F), 0.85F,
                HolderSet.direct(Block::builtInRegistryHolder, Stream.concat(ground().stream(), Stream.of(ModBlocks.SCORCHED_SANDSTONE.get())).toList())));
        // Dark stones from upstream, "carried by the water".
        FeatureUtils.register(context, RIVERBED_STONES, ModFeatures.RUBBLE.get(), new RubbleConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.COBBLESTONE.defaultBlockState(), 3)
                        .add(Blocks.ANDESITE.defaultBlockState(), 3)
                        .add(Blocks.BLACKSTONE.defaultBlockState(), 2)), UniformInt.of(1, 2)));
        // The bare trees of the withered expanse, with coarse dirt under them (a trunk on red sand or clay leaves a small
        // coarse patch, not the withered dirt of ring 1).
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        FeatureUtils.register(context, RIVERBED_DEAD_TREE, Feature.TREE, WitheredExpanseFeatures.bareTree(
                new StraightTrunkPlacer(4, 2, 0), new TwoLayersFeatureSize(1, 0, 1), Blocks.COARSE_DIRT));
        FeatureUtils.register(context, RIVERBED_DEAD_TREE_FORKED, Feature.TREE, WitheredExpanseFeatures.bareTree(
                new ForkingTrunkPlacer(5, 2, 2), new TwoLayersFeatureSize(1, 0, 2), Blocks.COARSE_DIRT));
        FeatureUtils.register(context, RIVERBED_DEAD_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(RIVERBED_DEAD_TREE_FORKED)), 0.7F)),
                PlacementUtils.inlinePlaced(configured.getOrThrow(RIVERBED_DEAD_TREE))));
        FeatureUtils.register(context, RIVERBED_FLORA, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.DRY_SHRUB.get().defaultBlockState(), 3)
                        .add(Blocks.DEAD_BUSH.defaultBlockState(), 2)
                        .add(ModBlocks.BARREL_CACTUS.get().defaultBlockState(), 1))));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        Block[] ground = ground().subList(0, 3).toArray(new Block[0]);
        BlockPredicate onGround = BlockPredicate.matchesBlocks(below(), ground);
        BlockPredicate cactus = BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.wouldSurvive(Blocks.CACTUS.defaultBlockState(), BlockPos.ZERO));

        // LOCAL_MODIFICATIONS, in this order (Ring2Biomes-style fixed order, see TransitionBiomes): the scorched patches
        // first, so fossils and bones skip the black ground and trees and scrub (later steps) never stand on it.
        // The patches grow denser towards ring 3 (0 at the ring 2 side, 0.9 at the ring 3 side).
        PlacementUtils.register(context, ModPlacedFeatures.DRY_RIVERBEDS_SCORCHED_PATCHES, features.getOrThrow(RIVERBED_SCORCHED_PATCH),
                JitteredGridPlacement.of(16, PATCH_MARGIN, 1.0F),
                new RingChanceFilter(RingChanceFilter.Signal.TRANSITION, Ring.RING_2, 0.0, 0.9, 0.0, 1.5),
                TOP_SOLID,
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.DRY_RIVERBEDS_SCORCHED_SPECKS, features.getOrThrow(RIVERBED_SCORCHED_SPECK),
                JitteredGridPlacement.of(8, SPECK_MARGIN, 0.7F),
                new RingChanceFilter(RingChanceFilter.Signal.TRANSITION, Ring.RING_2, 0.0, 1.0, 0.15, 1.2),
                TOP_SOLID,
                BiomeFilter.biome());
        // About one fossil per 25–30 chunks of the ring 2 half (the dune sea: one per 26), fewer towards ring 3.
        PlacementUtils.register(context, ModPlacedFeatures.DRY_RIVERBEDS_FOSSILS, features.getOrThrow(RIVERBED_FOSSIL),
                JitteredGridPlacement.of(16, 1, 0.22F),
                RingChanceFilter.transition(Ring.RING_2, 1.0, 0.3),
                SURFACE,
                BlockPredicateFilter.forPredicate(onGround),
                BiomeFilter.biome());
        // Loose bones, about one per 5 chunks.
        PlacementUtils.register(context, ModPlacedFeatures.DRY_RIVERBEDS_BONES, features.getOrThrow(BONE_RUBBLE),
                JitteredGridPlacement.of(8, 1, 0.08F),
                RingChanceFilter.transition(Ring.RING_2, 1.0, 0.5),
                SURFACE,
                BlockPredicateFilter.forPredicate(onGround),
                BiomeFilter.biome());
        // Stones on the banks and slopes of the beds, not on flat ground.
        PlacementUtils.register(context, ModPlacedFeatures.DRY_RIVERBEDS_STONES, features.getOrThrow(RIVERBED_STONES),
                JitteredGridPlacement.of(8, 1, 0.35F),
                SURFACE,
                new SlopeFilter(2, SlopeFilter.Measure.RELIEF, 2, Integer.MAX_VALUE),
                BiomeFilter.biome());

        // VEGETAL_DECORATION: dead trees on the bed and its banks, thinning towards ring 3; then scrub, dry grass on the coarse dirt, and cactus groups on the sand.
        PlacementUtils.register(context, ModPlacedFeatures.DRY_RIVERBEDS_DEAD_TREES, features.getOrThrow(RIVERBED_DEAD_TREES),
                JitteredGridPlacement.of(8, 2, 0.3F),
                RingChanceFilter.transition(Ring.RING_2, 1.0, 0.1),
                SURFACE,
                BlockPredicateFilter.forPredicate(onGround),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.DRY_RIVERBEDS_VEGETATION, features.getOrThrow(RIVERBED_FLORA),
                JitteredGridPlacement.of(4, 0, 0.4F),
                SURFACE,
                PlacementUtils.isEmpty(),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.DRY_RIVERBEDS_GRASS, features.getOrThrow(DRY_GRASS),
                JitteredGridPlacement.of(4, 0, 0.5F),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(below(), Blocks.COARSE_DIRT))),
                BiomeFilter.biome());
        // Groups of 1–2 high: one candidate per cell of 4 where the cactus noise is high (~10 %), as in the dunes.
        PlacementUtils.register(context, ModPlacedFeatures.DRY_RIVERBEDS_CACTUS_GROUPS, features.getOrThrow(SHORT_CACTUS_COLUMN),
                JitteredGridPlacement.of(4, 1, 0.85F),
                NoiseBandFilter.between(Ring2DressingFeatures.CACTUS_GROUPS, Ring2DressingFeatures.DUNE_CACTUS_NOISE, Double.MAX_VALUE),
                SURFACE,
                BlockPredicateFilter.forPredicate(cactus),
                BiomeFilter.biome());
    }

    private static DiskConfiguration scorchedDisk(UniformInt radius) {
        return new DiskConfiguration(
                new RuleBasedBlockStateProvider(
                        new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                                .add(Blocks.BLACKSTONE.defaultBlockState(), 3)
                                .add(Blocks.BASALT.defaultBlockState(), 2)
                                .add(ModBlocks.SCORCHED_SANDSTONE.get().defaultBlockState(), 2)),
                        List.of(new RuleBasedBlockStateProvider.Rule(
                                BlockPredicate.matchesBlocks(below(), Blocks.AIR),
                                BlockStateProvider.simple(ModBlocks.SCORCHED_SANDSTONE.get())))),
                BlockPredicate.matchesBlocks(ground()),
                radius,
                1);
    }

    private static BlockStateProvider weighted(SimpleWeightedRandomList.Builder<BlockState> states) {
        return new WeightedStateProvider(states);
    }

    private static Vec3i below() {
        return Direction.DOWN.getNormal();
    }
}
