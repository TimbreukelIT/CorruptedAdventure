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
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
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
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;

import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.CANYONLANDS_VEGETATION;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.CANYON_BOULDER;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.CANYON_ROCKFALL;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.CANYON_RUBBLE;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.CANYON_SPRING;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.OASIS_ROCK;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.OASIS_SHORE_CANE;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.OASIS_SHORE_PLANTS;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.OASIS_TRANSITION_VEGETATION;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.OASIS_VEGETATION;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.PALM_BUSH;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.PALM_TREE;

/**
 * Datagen bootstraps for the features of ring 2 (changes 25 and 30 add theirs here). Amounts are starting values,
 * tuned in-game (teststop 2b).
 * <p>
 * Determinism rules (change 23c): a placement starts in its own chunk and reads only the {@code WORLD_SURFACE_WG}
 * heightmap (during the features step it shows the terrain alone, never what neighbouring chunks placed), seeded
 * noise ({@link NoiseBandFilter}) and the placement random. Every feature but the palms writes only in its own chunk.
 * Palms stand on a {@link JitteredGridPlacement} grid of cell 8 with margin 2: trunks of different cells are at least
 * 5 apart, also across chunks, and the palm features divide the cells between them through bands read at the cell
 * centre. Where crowns overlap they place the same leaves.
 */
public final class Ring2Features {
    /** The palm trunk: 6–10 logs, leaning 1–2 blocks after the first third. */
    static final PalmTrunkPlacer PALM_TRUNK = new PalmTrunkPlacer(6, 4, 0, UniformInt.of(1, 2));
    /** The palm crown: straight fronds of 2–3 leaves plus a hanging one, 2–4 diagonal fronds. */
    static final PalmFoliagePlacer PALM_CROWN = new PalmFoliagePlacer(UniformInt.of(2, 3), ConstantInt.ZERO, UniformInt.of(2, 4));
    /** A palm bush: a straight stem of 1–2 with a small crown, at most 2 blocks out. */
    static final PalmTrunkPlacer BUSH_TRUNK = new PalmTrunkPlacer(1, 1, 0, ConstantInt.ZERO);
    static final PalmFoliagePlacer BUSH_CROWN = new PalmFoliagePlacer(UniformInt.of(1, 2), ConstantInt.ZERO, UniformInt.of(1, 3));

    static final ResourceKey<NormalNoise.NoiseParameters> POOLS = noise("ring_2_pools");
    static final ResourceKey<NormalNoise.NoiseParameters> TALUS = noise("ring_2_talus");
    static final ResourceKey<NormalNoise.NoiseParameters> BOULDER_FIELDS = noise("ring_2_boulder_fields");
    static final ResourceKey<NormalNoise.NoiseParameters> PALM_GROVES = noise("ring_2_palm_groves");

    // Zones of the oasis on ring_2_pools, as in the oasis_flats surface rules (noise_settings/overworld.json): the
    // grass shore from 0.12 (water from ~0.42), the transition ring 0.02–0.12, sand below.
    static final double SHORE = 0.12;
    static final double TRANSITION = 0.02;
    static final double WATER = 0.42;
    /** Palm groves: the outer edge of the shore and the inner part of the transition ring. */
    static final double GROVE_FROM = 0.05;
    static final double GROVE_TO = 0.28;
    /** Single palms on the open sand, clear of the transition ring. */
    static final double SAND_BELOW = 0.0;
    /** Where {@code ring_2_palm_groves} is at least this, palms group; elsewhere palm bushes. */
    static final double GROVE_NOISE = 0.0;
    /** Boulder fields where {@code ring_2_boulder_fields} is at least this: ~20 % of the floors. */
    static final double BOULDER_NOISE = 0.27;
    /** The talus slopes, as the canyonlands surface rule. */
    static final double TALUS_NOISE = 0.325;
    /** Canyon floors and steps lie below this; talus slopes run to ~118, the plateau starts above. */
    static final int FLOOR_TOP = 100;
    static final int PLATEAU_BOTTOM = 118;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    /** One cell of the palm grid; see the class comment. */
    private static final int PALM_CELL = 8;
    private static final int PALM_MARGIN = 2;

    private Ring2Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Vanilla replaces any non-#dirt ground under the trunk (and grass) with the dirt provider: sand under a palm.
        FeatureUtils.register(context, PALM_TREE, Feature.TREE, palm(PALM_TRUNK, PALM_CROWN));
        FeatureUtils.register(context, PALM_BUSH, Feature.TREE, palm(BUSH_TRUNK, BUSH_CROWN));

        // The simple block feature checks survival of the chosen state itself.
        FeatureUtils.register(context, OASIS_VEGETATION, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 4)
                        .add(Blocks.FERN.defaultBlockState(), 1)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1))));
        FeatureUtils.register(context, OASIS_TRANSITION_VEGETATION, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.DRY_SHRUB.get().defaultBlockState(), 6)
                        .add(Blocks.DEAD_BUSH.defaultBlockState(), 3)
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 2)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1))));
        // At the water's edge: sugar cane where it can grow, tall grass and (large) ferns.
        FeatureUtils.register(context, OASIS_SHORE_CANE, Feature.BLOCK_COLUMN,
                BlockColumnConfiguration.simple(BiasedToBottomInt.of(1, 3), BlockStateProvider.simple(Blocks.SUGAR_CANE)));
        FeatureUtils.register(context, OASIS_SHORE_PLANTS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.TALL_GRASS.defaultBlockState(), 2)
                        .add(Blocks.FERN.defaultBlockState(), 3)
                        .add(Blocks.LARGE_FERN.defaultBlockState(), 2))));
        FeatureUtils.register(context, OASIS_ROCK, ModFeatures.RUBBLE.get(), new RubbleConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.SANDSTONE.defaultBlockState(), 3)
                        .add(ModBlocks.SCORCHED_SANDSTONE.get().defaultBlockState(), 2)), UniformInt.of(1, 2)));

        FeatureUtils.register(context, CANYONLANDS_VEGETATION, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.DRY_SHRUB.get().defaultBlockState(), 30)
                        .add(Blocks.DEAD_BUSH.defaultBlockState(), 8)
                        .add(ModBlocks.BARREL_CACTUS.get().defaultBlockState(), 5)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1))));
        FeatureUtils.register(context, CANYON_BOULDER, ModFeatures.RUBBLE.get(), new RubbleConfiguration(canyonRock(), UniformInt.of(1, 3)));
        FeatureUtils.register(context, CANYON_RUBBLE, ModFeatures.RUBBLE.get(), new RubbleConfiguration(canyonRock(), UniformInt.of(1, 2)));
        FeatureUtils.register(context, CANYON_ROCKFALL, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.CANYON_COBBLESTONE.get().defaultBlockState(), 4)
                        .add(ModBlocks.RED_CANYON_STONE.get().defaultBlockState(), 2)
                        .add(ModBlocks.ORANGE_CANYON_STONE.get().defaultBlockState(), 1)
                        .add(ModBlocks.CREAM_CANYON_STONE.get().defaultBlockState(), 1))));
        FeatureUtils.register(context, CANYON_SPRING, ModFeatures.CANYON_SPRING.get(), new CanyonSpringConfiguration(
                UniformInt.of(3, 8), UniformInt.of(3, 6), UniformInt.of(2, 3), UniformInt.of(1, 2), 8, 24, 66, FLOOR_TOP,
                HolderSet.direct(Block::builtInRegistryHolder, Blocks.RED_SAND, ModBlocks.CRACKED_CLAY.get(), Blocks.GRAVEL),
                weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.MOSS_BLOCK.defaultBlockState(), 2)
                        .add(Blocks.GRASS_BLOCK.defaultBlockState(), 1)),
                weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.FERN.defaultBlockState(), 4)
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 2)
                        .add(Blocks.LARGE_FERN.defaultBlockState(), 1)),
                0.6F, 0.5F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);

        // Canyonlands, LOCAL_MODIFICATIONS, in this order (the spring last, see CanyonSpringFeature).
        PlacementUtils.register(context, ModPlacedFeatures.CANYON_BOULDER_FIELDS, features.getOrThrow(CANYON_BOULDER),
                CountPlacement.of(6),
                InSquarePlacement.spread(),
                NoiseBandFilter.between(BOULDER_FIELDS, BOULDER_NOISE, Double.MAX_VALUE),
                SURFACE,
                HeightBandFilter.below(FLOOR_TOP),
                SlopeFilter.flat(2, 2),
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(below(), Blocks.RED_SAND, ModBlocks.CRACKED_CLAY.get(), Blocks.GRAVEL)),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.CANYON_TALUS_RUBBLE, features.getOrThrow(CANYON_RUBBLE),
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                NoiseBandFilter.between(TALUS, TALUS_NOISE, Double.MAX_VALUE),
                SURFACE,
                HeightBandFilter.between(FLOOR_TOP - 12, PLATEAU_BOTTOM + 2),
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(below(), Blocks.RED_SAND, Blocks.GRAVEL)),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.CANYON_ROCKFALL, features.getOrThrow(CANYON_ROCKFALL),
                CountPlacement.of(3),
                InSquarePlacement.spread(),
                SURFACE,
                SlopeFilter.wallFoot(3, 6),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.solid(below()))),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.PLATEAU_RUBBLE, features.getOrThrow(CANYON_RUBBLE),
                RarityFilter.onAverageOnceEvery(2),
                InSquarePlacement.spread(),
                SURFACE,
                HeightBandFilter.above(PLATEAU_BOTTOM),
                SlopeFilter.flat(2, 3),
                BiomeFilter.biome());
        // The spring searches its chunk itself (floors up to FLOOR_TOP); the origin only picks the biome.
        PlacementUtils.register(context, ModPlacedFeatures.CANYON_SPRINGS, features.getOrThrow(CANYON_SPRING),
                InSquarePlacement.spread(),
                SURFACE,
                BiomeFilter.biome());
        // Canyonlands, VEGETAL_DECORATION.
        PlacementUtils.register(context, ModPlacedFeatures.CANYONLANDS_VEGETATION, features.getOrThrow(CANYONLANDS_VEGETATION),
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                SURFACE,
                PlacementUtils.isEmpty(),
                BiomeFilter.biome());

        // Oasis, LOCAL_MODIFICATIONS: a sandstone boulder now and then in the transition ring.
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_ROCKS, features.getOrThrow(OASIS_ROCK),
                RarityFilter.onAverageOnceEvery(2),
                InSquarePlacement.spread(),
                NoiseBandFilter.between(POOLS, TRANSITION, SHORE),
                SURFACE,
                BiomeFilter.biome());
        // Oasis, VEGETAL_DECORATION, in this order. At the water's edge: the water line is thin (~2 columns per
        // chunk of the oasis), hence many tries; one block inside the chunk, so the water beside is read in this chunk.
        // Sugar cane needs water beside its ground block; tall grass and ferns also stand where the shore steps down one
        // block to the water.
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_SHORE_CANE, features.getOrThrow(OASIS_SHORE_CANE),
                CountPlacement.of(32),
                JitteredGridPlacement.of(16, 1, 1.0F),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(Blocks.SUGAR_CANE.defaultBlockState(), BlockPos.ZERO))),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_SHORE_PLANTS, features.getOrThrow(OASIS_SHORE_PLANTS),
                CountPlacement.of(32),
                JitteredGridPlacement.of(16, 1, 1.0F),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.anyOf(
                        water(1, -1, 0), water(-1, -1, 0), water(0, -1, 1), water(0, -1, -1),
                        water(1, -2, 0), water(-1, -2, 0), water(0, -2, 1), water(0, -2, -1)))),
                BiomeFilter.biome());
        // Groups of 2–4 palms where ring_2_palm_groves is high, on the outer shore and the inner transition ring.
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_PALM_GROVES, features.getOrThrow(PALM_TREE),
                JitteredGridPlacement.of(PALM_CELL, PALM_MARGIN, 0.75F),
                SURFACE,
                NoiseBandFilter.snapped(POOLS, GROVE_FROM, GROVE_TO, PALM_CELL),
                NoiseBandFilter.snapped(PALM_GROVES, GROVE_NOISE, Double.MAX_VALUE, PALM_CELL),
                PlacementUtils.filteredByBlockSurvival(ModBlocks.PALM_SAPLING.get()),
                BiomeFilter.biome());
        // A single palm on the open sand, about one per six chunks.
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_PALMS_SAND, features.getOrThrow(PALM_TREE),
                JitteredGridPlacement.of(PALM_CELL, PALM_MARGIN, 1.0F / 24.0F),
                SURFACE,
                NoiseBandFilter.snapped(POOLS, -Double.MAX_VALUE, SAND_BELOW, PALM_CELL),
                PlacementUtils.filteredByBlockSurvival(ModBlocks.PALM_SAPLING.get()),
                BiomeFilter.biome());
        // Palm bushes between the groves, on the shore and the transition ring.
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_PALM_BUSHES, features.getOrThrow(PALM_BUSH),
                JitteredGridPlacement.of(PALM_CELL, PALM_MARGIN, 0.25F),
                SURFACE,
                NoiseBandFilter.snapped(POOLS, TRANSITION, WATER, PALM_CELL),
                NoiseBandFilter.snapped(PALM_GROVES, -Double.MAX_VALUE, Math.nextDown(GROVE_NOISE), PALM_CELL),
                PlacementUtils.filteredByBlockSurvival(ModBlocks.PALM_SAPLING.get()),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_VEGETATION, features.getOrThrow(OASIS_VEGETATION),
                CountPlacement.of(10),
                InSquarePlacement.spread(),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(below(), Blocks.GRASS_BLOCK))),
                BiomeFilter.biome());
        PlacementUtils.register(context, ModPlacedFeatures.OASIS_TRANSITION_VEGETATION, features.getOrThrow(OASIS_TRANSITION_VEGETATION),
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                NoiseBandFilter.between(POOLS, TRANSITION, SHORE),
                SURFACE,
                PlacementUtils.isEmpty(),
                BiomeFilter.biome());
    }

    private static TreeConfiguration palm(PalmTrunkPlacer trunk, PalmFoliagePlacer crown) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.PALM_LOG.get()),
                trunk,
                BlockStateProvider.simple(ModBlocks.PALM_LEAVES.get()),
                crown,
                new TwoLayersFeatureSize(1, 0, 1))
                .dirt(BlockStateProvider.simple(Blocks.SAND))
                .ignoreVines()
                .build();
    }

    /** The three canyon stone tints and canyon cobblestone. */
    private static BlockStateProvider canyonRock() {
        return weighted(SimpleWeightedRandomList.<BlockState>builder()
                .add(ModBlocks.RED_CANYON_STONE.get().defaultBlockState(), 3)
                .add(ModBlocks.ORANGE_CANYON_STONE.get().defaultBlockState(), 2)
                .add(ModBlocks.CREAM_CANYON_STONE.get().defaultBlockState(), 2)
                .add(ModBlocks.CANYON_COBBLESTONE.get().defaultBlockState(), 3));
    }

    private static BlockStateProvider weighted(SimpleWeightedRandomList.Builder<BlockState> states) {
        return new WeightedStateProvider(states);
    }

    /** Water at this offset from the position. */
    private static BlockPredicate water(int dx, int dy, int dz) {
        return BlockPredicate.matchesFluids(new Vec3i(dx, dy, dz), Fluids.WATER, Fluids.FLOWING_WATER);
    }

    private static Vec3i below() {
        return Direction.DOWN.getNormal();
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noise(String path) {
        return ResourceKey.create(Registries.NOISE, CorruptedAdventure.id(path));
    }
}
