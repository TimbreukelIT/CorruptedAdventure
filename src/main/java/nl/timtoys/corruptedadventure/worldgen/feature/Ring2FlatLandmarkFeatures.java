package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.List;
import java.util.Optional;

/**
 * Datagen bootstraps for the eye-catchers of ring 2's flat biomes (G2 ring 2, G2-R2-1; {@link Ring2FlatLandmarks}): the
 * salt dome, the dune skeleton and the giant palm with its spring pool. Each is one placed feature per chunk without
 * modifiers, last in its biome's step (the feature itself finds the landmarks that reach the chunk). The pool is a
 * {@code surface_pool} like the dune oases', written in {@code LOCAL_MODIFICATIONS} before the oasis plants grow.
 */
public final class Ring2FlatLandmarkFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> SALT_DOME = configuredKey("salt_dome");
    static final ResourceKey<ConfiguredFeature<?, ?>> DUNE_SKELETON = configuredKey("dune_skeleton");
    static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_PALM = configuredKey("giant_palm");
    static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_PALM_POOL = configuredKey("giant_palm_pool");
    static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_PALM_SPRING = configuredKey("giant_palm_spring");

    public static final ResourceKey<PlacedFeature> SALT_FLATS_SALT_DOME = placedKey("salt_flats_salt_dome");
    public static final ResourceKey<PlacedFeature> DUNE_SEA_SKELETON = placedKey("dune_sea_skeleton");
    public static final ResourceKey<PlacedFeature> OASIS_FLATS_GIANT_PALM_SPRING = placedKey("oasis_flats_giant_palm_spring");
    public static final ResourceKey<PlacedFeature> OASIS_FLATS_GIANT_PALM = placedKey("oasis_flats_giant_palm");

    /** How far the oasis palms keep from the giant palm's footprint: a small crown (3) leaning 2, and one more. */
    static final int PALM_CLEARANCE = 6;

    private Ring2FlatLandmarkFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        FeatureUtils.register(context, SALT_DOME, ModFeatures.RING_2_FLAT_LANDMARK.get(),
                new Ring2FlatLandmarkFeature.Config(Ring2FlatLandmarks.Kind.SALT_DOME, Ring2FlatLandmarkFeature.Part.SHAPE, Optional.empty()));
        FeatureUtils.register(context, DUNE_SKELETON, ModFeatures.RING_2_FLAT_LANDMARK.get(),
                new Ring2FlatLandmarkFeature.Config(Ring2FlatLandmarks.Kind.DUNE_SKELETON, Ring2FlatLandmarkFeature.Part.SHAPE, Optional.empty()));
        FeatureUtils.register(context, GIANT_PALM, ModFeatures.RING_2_FLAT_LANDMARK.get(),
                new Ring2FlatLandmarkFeature.Config(Ring2FlatLandmarks.Kind.GIANT_PALM, Ring2FlatLandmarkFeature.Part.SHAPE, Optional.empty()));
        // The spring pool: 2 deep in the core, a grass rim with ferns and grass, sugar cane at the water; no trees (the
        // giant palm stands beside it).
        FeatureUtils.register(context, GIANT_PALM_POOL, ModFeatures.SURFACE_POOL.get(), new SurfacePoolConfiguration(
                UniformFloat.of(3.0F, 4.0F), 0.25F, ConstantInt.of(2),
                HolderSet.direct(Block::builtInRegistryHolder, Blocks.SAND, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT), 3,
                new SurfacePoolConfiguration.Rim(BlockStateProvider.simple(Blocks.GRASS_BLOCK), 2),
                Optional.of(new SurfacePoolConfiguration.Scatter(new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 5)
                        .add(Blocks.FERN.defaultBlockState(), 4)
                        .add(Blocks.TALL_GRASS.defaultBlockState(), 1)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1)), 0.55F)),
                Optional.of(new SurfacePoolConfiguration.Column(BlockStateProvider.simple(Blocks.SUGAR_CANE), UniformInt.of(1, 3), 0.4F)),
                Optional.empty(), Optional.empty(), Optional.empty()));
        FeatureUtils.register(context, GIANT_PALM_SPRING, ModFeatures.RING_2_FLAT_LANDMARK.get(),
                new Ring2FlatLandmarkFeature.Config(Ring2FlatLandmarks.Kind.GIANT_PALM, Ring2FlatLandmarkFeature.Part.SPRING,
                        Optional.of(features.getOrThrow(GIANT_PALM_POOL))));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(context, SALT_FLATS_SALT_DOME, features.getOrThrow(SALT_DOME), List.of());
        PlacementUtils.register(context, DUNE_SEA_SKELETON, features.getOrThrow(DUNE_SKELETON), List.of());
        PlacementUtils.register(context, OASIS_FLATS_GIANT_PALM_SPRING, features.getOrThrow(GIANT_PALM_SPRING), List.of());
        PlacementUtils.register(context, OASIS_FLATS_GIANT_PALM, features.getOrThrow(GIANT_PALM), List.of());
    }

    /** The clearance of the oasis palms round the giant palm (a filter for {@code Ring2Features}' palm placements). */
    public static Ring2LandmarkClearanceFilter palmClearance() {
        return Ring2LandmarkClearanceFilter.of(Ring2FlatLandmarks.Kind.GIANT_PALM, PALM_CLEARANCE);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id(name));
    }

    private static ResourceKey<PlacedFeature> placedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id(name));
    }
}
