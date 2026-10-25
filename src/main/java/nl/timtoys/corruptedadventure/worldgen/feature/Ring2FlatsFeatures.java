package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;

import java.util.List;
import java.util.Optional;

import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.BRINE_POOL;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DUNE_OASIS;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DUNE_ROCK;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.PALM_BUSH;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.PALM_TREE;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.SALT_CRYSTAL;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.SALT_ISLAND_ROCK;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.SALT_PILLAR;

/**
 * Datagen bootstraps for the shapes and water of the salt flats and the dune sea (change 30b): brine pools, salt
 * chimneys, rocks on the salt islands and crystal fields; rock islands and hidden oases in the dunes. Amounts are
 * starting values, tuned in-game (teststop 2c). Same determinism rules as {@link Ring2Features}: one candidate per
 * {@link JitteredGridPlacement} cell, everything in its own chunk (only the oasis palm crowns reach the neighbours,
 * under the palm rules of change 23c).
 */
public final class Ring2FlatsFeatures {
    static final ResourceKey<NormalNoise.NoiseParameters> SALT_PILLARS = noise("ring_2_salt_pillars");
    static final ResourceKey<NormalNoise.NoiseParameters> SALT_CRYSTAL_FIELDS = noise("ring_2_salt_crystal_fields");
    /** The same noise and scale as {@code dune_height}: high values are the high dunes with deep valleys. */
    static final ResourceKey<NormalNoise.NoiseParameters> DUNE_AMPLITUDE = noise("ring_2_dune_amplitude");
    /** Pillar fields where {@code ring_2_salt_pillars} is at least this: ~12 % of the flats (measured). */
    static final double PILLAR_FIELD_NOISE = 0.4;
    /** Crystal fields where {@code ring_2_salt_crystal_fields} is at least this: ~10 % of the flats (measured). */
    static final double CRYSTAL_FIELD_NOISE = 0.4;
    /** Oases only among the high dunes. */
    static final double HIGH_DUNES = 0.15;
    /**
     * Oases only on valley floors: the top block at most Y 71 (the origin lies on top of it); among the high dunes the
     * crests stand 85–99, so that is a deep valley. Most dune troughs lie at Y 66–72 (measured, change 30b).
     */
    static final int VALLEY_FLOOR = 72;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private Ring2FlatsFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        BlockState crystal = ModBlocks.SALT_CRYSTAL.get().defaultBlockState();

        // A shallow brine pool sunk into the crust: one deep over the salt block below, a clean crust rim with crystals.
        FeatureUtils.register(context, BRINE_POOL, ModFeatures.SURFACE_POOL.get(), new SurfacePoolConfiguration(
                UniformFloat.of(2.0F, 5.0F), 0.35F, ConstantInt.of(1),
                HolderSet.direct(Block::builtInRegistryHolder, ModBlocks.SALT_CRUST.get(), ModBlocks.SALT_BLOCK.get()), 0,
                new SurfacePoolConfiguration.Rim(BlockStateProvider.simple(ModBlocks.SALT_CRUST.get()), 1),
                Optional.empty(), Optional.empty(),
                Optional.of(new SurfacePoolConfiguration.Scatter(BlockStateProvider.simple(crystal), 0.12F)),
                Optional.of(new SurfacePoolConfiguration.Scatter(
                        BlockStateProvider.simple(crystal.setValue(BlockStateProperties.WATERLOGGED, true)), 0.04F)),
                Optional.empty()));
        // A hidden oasis in a dune valley: water 2 deep in the core, a grass rim with plants, sugar cane and 1–2 palms.
        FeatureUtils.register(context, DUNE_OASIS, ModFeatures.SURFACE_POOL.get(), new SurfacePoolConfiguration(
                UniformFloat.of(1.5F, 3.0F), 0.3F, ConstantInt.of(2),
                HolderSet.direct(Block::builtInRegistryHolder, Blocks.SAND, Blocks.RED_SAND), 2,
                new SurfacePoolConfiguration.Rim(BlockStateProvider.simple(Blocks.GRASS_BLOCK), 2),
                Optional.of(new SurfacePoolConfiguration.Scatter(weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 5)
                        .add(Blocks.FERN.defaultBlockState(), 3)
                        .add(Blocks.TALL_GRASS.defaultBlockState(), 1)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1)), 0.5F)),
                Optional.of(new SurfacePoolConfiguration.Column(BlockStateProvider.simple(Blocks.SUGAR_CANE), UniformInt.of(1, 2), 0.3F)),
                Optional.empty(), Optional.empty(),
                Optional.of(new SurfacePoolConfiguration.Trees(SimpleWeightedRandomList.<Holder<ConfiguredFeature<?, ?>>>builder()
                        .add(features.getOrThrow(PALM_TREE), 3)
                        .add(features.getOrThrow(PALM_BUSH), 1).build(),
                        UniformInt.of(1, 2),
                        BlockPredicate.wouldSurvive(ModBlocks.PALM_SAPLING.get().defaultBlockState(), BlockPos.ZERO)))));

        // Salt chimneys: 1–3 tapering columns of salt block, 2–6 high, now and then a crystal on top.
        FeatureUtils.register(context, SALT_PILLAR, ModFeatures.OUTCROP.get(), new OutcropConfiguration(
                UniformInt.of(1, 3), 3.0F, UniformFloat.of(0.8F, 1.6F), UniformInt.of(2, 6), 0.5F, 1, 0.0F,
                SimpleWeightedRandomList.<List<BlockState>>builder().add(List.of(ModBlocks.SALT_BLOCK.get().defaultBlockState()), 1).build(),
                Optional.of(new SurfacePoolConfiguration.Scatter(BlockStateProvider.simple(crystal), 0.3F))));
        // Rock islands in the dunes: 2–5 knobs up to 12 high in bands of sandstone or canyon stone, a third with a collar.
        FeatureUtils.register(context, DUNE_ROCK, ModFeatures.OUTCROP.get(), new OutcropConfiguration(
                UniformInt.of(2, 5), 4.0F, UniformFloat.of(1.5F, 3.5F), UniformInt.of(4, 12), 0.6F, 2, 0.35F,
                SimpleWeightedRandomList.<List<BlockState>>builder()
                        .add(List.of(Blocks.SANDSTONE.defaultBlockState(), Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
                                ModBlocks.SCORCHED_SANDSTONE.get().defaultBlockState()), 3)
                        .add(List.of(ModBlocks.RED_CANYON_STONE.get().defaultBlockState(), ModBlocks.ORANGE_CANYON_STONE.get().defaultBlockState(),
                                ModBlocks.CREAM_CANYON_STONE.get().defaultBlockState()), 2)
                        .build(),
                Optional.empty()));
        // Small rocks on the salt islands.
        FeatureUtils.register(context, SALT_ISLAND_ROCK, ModFeatures.RUBBLE.get(), new RubbleConfiguration(
                weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.SANDSTONE.defaultBlockState(), 3)
                        .add(ModBlocks.SCORCHED_SANDSTONE.get().defaultBlockState(), 2)
                        .add(ModBlocks.RED_CANYON_STONE.get().defaultBlockState(), 1)), UniformInt.of(1, 3)));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        BlockPredicate onSalt = BlockPredicate.matchesBlocks(below(), ModBlocks.SALT_CRUST.get(), ModBlocks.SALT_BLOCK.get());

        // Salt flats, LOCAL_MODIFICATIONS, in this order: the pools first, the pillars and rocks fail on their water.
        PlacementUtils.register(context, ModPlacedFeatures.SALT_FLATS_BRINE_POOLS, features.getOrThrow(BRINE_POOL),
                JitteredGridPlacement.of(16, 1, 0.15F),
                SURFACE,
                BlockPredicateFilter.forPredicate(onSalt),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.SALT_FLATS_PILLAR_FIELDS, features.getOrThrow(SALT_PILLAR),
                JitteredGridPlacement.of(8, 2, 0.5F),
                NoiseBandFilter.between(SALT_PILLARS, PILLAR_FIELD_NOISE, Double.MAX_VALUE),
                SURFACE,
                BlockPredicateFilter.forPredicate(onSalt),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.SALT_FLATS_PILLARS, features.getOrThrow(SALT_PILLAR),
                JitteredGridPlacement.of(16, 1, 0.06F),
                SURFACE,
                BlockPredicateFilter.forPredicate(onSalt),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.SALT_ISLAND_ROCKS, features.getOrThrow(SALT_ISLAND_ROCK),
                JitteredGridPlacement.of(8, 1, 0.3F),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(below(), Blocks.SAND, Blocks.RED_SAND)),
                BiomeFilter.biome());
        // Salt flats, VEGETAL_DECORATION, after salt_flats_crystals: dense crystal fields, glowing faintly at night.
        PlacementUtils.register(context, ModPlacedFeatures.SALT_FLATS_CRYSTAL_FIELDS, features.getOrThrow(SALT_CRYSTAL),
                JitteredGridPlacement.of(4, 0, 0.35F),
                NoiseBandFilter.between(SALT_CRYSTAL_FIELDS, CRYSTAL_FIELD_NOISE, Double.MAX_VALUE),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, onSalt)),
                BiomeFilter.biome());

        // Dune sea, LOCAL_MODIFICATIONS, in this order: the oases first, the rocks fail on their water and grass. Only ~5 % of
        // the high-dune columns pass the valley filters, so the grid chance is high: ~1 oasis per 30–50 chunks of high dunes.
        PlacementUtils.register(context, ModPlacedFeatures.DUNE_SEA_OASES, features.getOrThrow(DUNE_OASIS),
                JitteredGridPlacement.of(16, 1, 0.25F),
                NoiseBandFilter.between(DUNE_AMPLITUDE, HIGH_DUNES, Double.MAX_VALUE),
                SURFACE,
                HeightBandFilter.below(VALLEY_FLOOR),
                SlopeFilter.flat(4, 2),
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(below(), Blocks.SAND, Blocks.RED_SAND)),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.DUNE_SEA_OUTCROPS, features.getOrThrow(DUNE_ROCK),
                JitteredGridPlacement.of(16, 1, 0.1F),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(below(), Blocks.SAND, Blocks.RED_SAND)),
                BiomeFilter.biome());
    }

    private static BlockStateProvider weighted(SimpleWeightedRandomList.Builder<BlockState> states) {
        return new WeightedStateProvider(states);
    }

    private static Vec3i below() {
        return Direction.DOWN.getNormal();
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noise(String path) {
        return ResourceKey.create(Registries.NOISE, CorruptedAdventure.id(path));
    }
}
