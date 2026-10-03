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
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
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

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.BONE_RUBBLE;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.CACTUS_COLUMN;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DESERT_FOSSIL;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DRY_GRASS;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DUNE_VALLEY_FLORA;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.PILLAR_FOOT_FLORA;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.SALT_ISLAND_FLORA;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.SHORT_CACTUS_COLUMN;

/**
 * Datagen bootstraps for the vegetation and dressing of the dune sea, the hoodoo fields and the salt islands
 * (change 30c): plants in the dune valleys (crests stay bare), cactus groups, half-buried fossils and loose bones,
 * flora at the pillar feet, life on the salt islands. The oasis flowers and shrubs are in {@link Ring2Features}, with
 * the rest of the oasis. Amounts are starting values, tuned in-game (teststop 2c).
 * <p>
 * Same determinism rules as {@link Ring2Features}: one candidate per {@link JitteredGridPlacement} cell, groups from
 * seeded noise ({@link NoiseBandFilter}) instead of random patches, everything in its own chunk.
 */
public final class Ring2DressingFeatures {
    /** Cactus groups where {@code ring_2_cactus_groups} is at least this (dunes) … */
    static final double DUNE_CACTUS_NOISE = 0.4;
    /** … and in the hoodoo fields. */
    static final double HOODOO_CACTUS_NOISE = 0.45;
    static final ResourceKey<NormalNoise.NoiseParameters> CACTUS_GROUPS = noise("ring_2_cactus_groups");
    /**
     * Dune heights of the top block (the origin lies one above it): valley floors up to {@link #VALLEY_TOP} (troughs
     * lie at Y 66–71 whatever the dune height), flanks up to {@link #FLANK_TOP}, crests above that stay bare.
     */
    static final int VALLEY_TOP = 71;
    static final int FLANK_TOP = 78;
    /** Cactus groups and loose bones also on the lowest flanks, fossils on the lower half of a flank. */
    static final int CACTUS_TOP = 74;
    static final int BONES_TOP = 74;
    static final int FOSSIL_TOP = 76;
    /** The wall of a hoodoo pillar: a column within 2 at least this much higher. */
    static final int PILLAR_FOOT_RISE = 8;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private Ring2DressingFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Dune valleys: dry shrubs, dead bushes, now and then a barrel cactus or a desert flower; on the hard valley
        // floor short grass with the dry grass tint of the dunes. The hard floor (coarse dirt and cracked clay, the
        // dune_sea surface rule) lies only in the lowest part of a valley, top block below Y 70; the flora reaches up
        // to VALLEY_TOP.
        FeatureUtils.register(context, DUNE_VALLEY_FLORA, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.DRY_SHRUB.get().defaultBlockState(), 6)
                        .add(Blocks.DEAD_BUSH.defaultBlockState(), 4)
                        .add(ModBlocks.BARREL_CACTUS.get().defaultBlockState(), 2)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1))));
        FeatureUtils.register(context, DRY_GRASS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 4)
                        .add(Blocks.TALL_GRASS.defaultBlockState(), 1))));
        // Cacti 1–3 high (mostly low); the placement checks survival of the bottom block.
        FeatureUtils.register(context, CACTUS_COLUMN, Feature.BLOCK_COLUMN,
                BlockColumnConfiguration.simple(BiasedToBottomInt.of(1, 3), BlockStateProvider.simple(Blocks.CACTUS)));
        FeatureUtils.register(context, SHORT_CACTUS_COLUMN, Feature.BLOCK_COLUMN,
                BlockColumnConfiguration.simple(BiasedToBottomInt.of(1, 2), BlockStateProvider.simple(Blocks.CACTUS)));
        FeatureUtils.register(context, PILLAR_FOOT_FLORA, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.DRY_SHRUB.get().defaultBlockState(), 4)
                        .add(Blocks.DEAD_BUSH.defaultBlockState(), 2)
                        .add(ModBlocks.BARREL_CACTUS.get().defaultBlockState(), 2)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1))));
        FeatureUtils.register(context, SALT_ISLAND_FLORA, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.DRY_SHRUB.get().defaultBlockState(), 5)
                        .add(Blocks.DEAD_BUSH.defaultBlockState(), 3)
                        .add(ModBlocks.BARREL_CACTUS.get().defaultBlockState(), 2))));
        // Vanilla's fossil templates half buried in the sand, weathered; and loose bones, mostly lying.
        FeatureUtils.register(context, DESERT_FOSSIL, ModFeatures.SURFACE_FOSSIL.get(), new SurfaceFossilConfiguration(
                Stream.of("spine", "skull").flatMap(kind -> IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> ResourceLocation.withDefaultNamespace("fossil/" + kind + "_" + i))).toList(),
                UniformFloat.of(0.4F, 0.7F), 0.85F,
                HolderSet.direct(Block::builtInRegistryHolder, Blocks.SAND, Blocks.RED_SAND)));
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        FeatureUtils.register(context, BONE_RUBBLE, ModFeatures.RUBBLE.get(), new RubbleConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(bone.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 2)
                        .add(bone.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), 2)
                        .add(bone.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), 1)), UniformInt.of(1, 2)));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        BlockPredicate onSand = BlockPredicate.matchesBlocks(below(), Blocks.SAND);
        BlockPredicate onIsland = BlockPredicate.matchesBlocks(below(), Blocks.SAND, Blocks.RED_SAND);
        BlockPredicate cactus = BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.wouldSurvive(Blocks.CACTUS.defaultBlockState(), BlockPos.ZERO));

        // Dune sea, LOCAL_MODIFICATIONS, after the oases and rocks (fossils and bones skip their water and rock): a
        // fossil about once per 25 dune chunks in the valleys and lower flanks, loose bones about once per 5.
        PlacementUtils.register(context, ModPlacedFeatures.DUNE_SEA_FOSSILS, features.getOrThrow(DESERT_FOSSIL),
                JitteredGridPlacement.of(16, 1, 0.2F),
                SURFACE,
                HeightBandFilter.below(FOSSIL_TOP + 1),
                BlockPredicateFilter.forPredicate(onSand),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.DUNE_SEA_BONES, features.getOrThrow(BONE_RUBBLE),
                JitteredGridPlacement.of(8, 1, 0.3F),
                SURFACE,
                HeightBandFilter.below(BONES_TOP + 1),
                BlockPredicateFilter.forPredicate(onSand),
                BiomeFilter.biome());
        // Dune sea, VEGETAL_DECORATION, after dune_sea_vegetation (the flanks): the valleys and their cactus groups.
        PlacementUtils.register(context, ModPlacedFeatures.DUNE_SEA_VALLEY_FLORA, features.getOrThrow(DUNE_VALLEY_FLORA),
                JitteredGridPlacement.of(4, 0, 0.25F),
                SURFACE,
                HeightBandFilter.below(VALLEY_TOP + 1),
                PlacementUtils.isEmpty(),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.DUNE_SEA_VALLEY_GRASS, features.getOrThrow(DRY_GRASS),
                JitteredGridPlacement.of(4, 0, 0.5F),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(below(), Blocks.COARSE_DIRT))),
                BiomeFilter.biome());
        // Groups of 2–5: one candidate per cell of 4 (at least 3 apart, all four neighbours in the chunk, so the
        // survival check never reads another chunk) where the cactus noise is high (~10 % of the valleys).
        PlacementUtils.register(context, ModPlacedFeatures.DUNE_SEA_CACTUS_GROUPS, features.getOrThrow(CACTUS_COLUMN),
                JitteredGridPlacement.of(4, 1, 0.85F),
                NoiseBandFilter.between(CACTUS_GROUPS, DUNE_CACTUS_NOISE, Double.MAX_VALUE),
                SURFACE,
                HeightBandFilter.below(CACTUS_TOP + 1),
                BlockPredicateFilter.forPredicate(cactus),
                BiomeFilter.biome());

        // Hoodoo fields, VEGETAL_DECORATION, after hoodoo_fields_vegetation: flora at the pillar feet (where a pillar
        // wall rises within 2), cactus groups on the red sand (beside a pillar the cactus fails, as it should).
        PlacementUtils.register(context, ModPlacedFeatures.HOODOO_PILLAR_FOOT_FLORA, features.getOrThrow(PILLAR_FOOT_FLORA),
                JitteredGridPlacement.of(4, 0, 0.6F),
                SURFACE,
                SlopeFilter.wallFoot(2, PILLAR_FOOT_RISE),
                PlacementUtils.isEmpty(),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.HOODOO_CACTUS_GROUPS, features.getOrThrow(CACTUS_COLUMN),
                JitteredGridPlacement.of(4, 1, 0.5F),
                NoiseBandFilter.between(CACTUS_GROUPS, HOODOO_CACTUS_NOISE, Double.MAX_VALUE),
                SURFACE,
                BlockPredicateFilter.forPredicate(cactus),
                BiomeFilter.biome());

        // Salt flats, LOCAL_MODIFICATIONS, after the island rocks: fossils and bones on the islands and the sand
        // patches on the dune side; the crust stays dead. One fossil try per chunk: only ~5 % of the flats is island,
        // and a fossil needs sand under its whole footprint.
        PlacementUtils.register(context, ModPlacedFeatures.SALT_ISLAND_FOSSILS, features.getOrThrow(DESERT_FOSSIL),
                JitteredGridPlacement.of(16, 1, 1.0F),
                SURFACE,
                BlockPredicateFilter.forPredicate(onIsland),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.SALT_ISLAND_BONES, features.getOrThrow(BONE_RUBBLE),
                JitteredGridPlacement.of(8, 1, 0.15F),
                SURFACE,
                BlockPredicateFilter.forPredicate(onIsland),
                BiomeFilter.biome());
        // Salt flats, VEGETAL_DECORATION, after the crystal fields: life on the islands.
        PlacementUtils.register(context, ModPlacedFeatures.SALT_ISLAND_FLORA, features.getOrThrow(SALT_ISLAND_FLORA),
                JitteredGridPlacement.of(4, 0, 0.35F),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, onIsland)),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.SALT_ISLAND_CACTUS, features.getOrThrow(SHORT_CACTUS_COLUMN),
                JitteredGridPlacement.of(8, 1, 0.2F),
                SURFACE,
                BlockPredicateFilter.forPredicate(cactus),
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
