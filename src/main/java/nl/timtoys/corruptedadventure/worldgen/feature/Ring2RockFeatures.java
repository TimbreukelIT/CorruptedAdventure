package nl.timtoys.corruptedadventure.worldgen.feature;

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
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.ArrayList;
import java.util.List;

import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.CANYON_RUBBLE;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.RIVERBED_DEAD_TREES;

/**
 * Datagen bootstraps of the G2 ring 2 refresh for the rocky biomes (decisions G2-R2-4 and 5, 2026-10-25): groves of
 * gnarled dead trees and junipers with scrub and leaf bushes in the canyonlands, green seeps on the canyon floor (round
 * the wall springs and in patches), and rubble in the dry gullies of the hoodoo floor (G2-R2-6; the gullies themselves
 * and the plateau patches are surface rules, the open hoodoo patches the density function {@code hoodoo_pillar_weight}).
 * Amounts are starting values.
 * <p>
 * Same determinism rules as {@link Ring2Features}: {@link JitteredGridPlacement} with margins that keep every block a
 * feature writes or reads inside its own chunk, only {@code WORLD_SURFACE_WG} and seeded noise as input.
 */
public final class Ring2RockFeatures {
    /** The grove field of the canyonlands: no tree or grove scrub below {@link #GROVE_LOW}, every cell from {@link #GROVE_HIGH}. */
    static final ResourceKey<NormalNoise.NoiseParameters> GROVES = noise("ring_2_canyon_groves");
    static final double GROVE_LOW = 0.17;
    static final double GROVE_HIGH = 0.37;
    /** Seep patches on the canyon floor where this noise is at least {@link #SEEP_NOISE}. */
    static final ResourceKey<NormalNoise.NoiseParameters> SEEPS = noise("ring_2_canyon_seeps");
    static final double SEEP_NOISE = 0.1;
    /** The dry gullies of the hoodoo floor: |noise| below {@link #GULLY_HALF_WIDTH} (the surface rule uses the same band). */
    static final ResourceKey<NormalNoise.NoiseParameters> GULLIES = noise("ring_2_hoodoo_gullies");
    static final double GULLY_HALF_WIDTH = 0.018;
    /** The top of the hoodoo floor (88 ± 3 with its step; the cap rock starts at Y 94). */
    static final int HOODOO_FLOOR_TOP = 96;
    /**
     * Trees: a juniper's crown reaches 2 from its straight trunk, a forked dead tree bends at most 3 (no leaves); this
     * margin keeps every block of them in the chunk.
     */
    static final int TREE_MARGIN = 3;
    /**
     * Water this many blocks beside a seep's origin; the grid margin keeps every probe inside the chunk, and most of a
     * seep (radius 2–3, stretched up to 1.3) as well: the patch writes only its own chunk.
     */
    static final int SEEP_REACH = 3;

    static final ResourceKey<ConfiguredFeature<?, ?>> CANYON_JUNIPER = configuredKey("canyon_juniper");
    static final ResourceKey<ConfiguredFeature<?, ?>> CANYON_GROVE_TREE = configuredKey("canyon_grove_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> CANYON_GROVE_SCRUB = configuredKey("canyon_grove_scrub");
    static final ResourceKey<ConfiguredFeature<?, ?>> CANYON_SEEP = configuredKey("canyon_seep");
    static final ResourceKey<ConfiguredFeature<?, ?>> CANYON_SEEP_PLANTS = configuredKey("canyon_seep_plants");

    public static final ResourceKey<PlacedFeature> CANYON_SPRING_SEEPS = placedKey("canyon_spring_seeps");
    public static final ResourceKey<PlacedFeature> CANYON_FLOOR_SEEPS = placedKey("canyon_floor_seeps");
    public static final ResourceKey<PlacedFeature> HOODOO_GULLY_RUBBLE = placedKey("hoodoo_gully_rubble");
    public static final ResourceKey<PlacedFeature> CANYON_GROVE_TREES = placedKey("canyon_grove_trees");
    public static final ResourceKey<PlacedFeature> CANYON_GROVE_SHRUBS = placedKey("canyon_grove_shrubs");
    public static final ResourceKey<PlacedFeature> CANYON_SEEP_GREENERY = placedKey("canyon_seep_greenery");

    /** Canyonlands, {@code LOCAL_MODIFICATIONS}, after the springs (a seep round a spring needs its water). */
    public static final List<ResourceKey<PlacedFeature>> CANYONLANDS_SEEPS = List.of(CANYON_SPRING_SEEPS, CANYON_FLOOR_SEEPS);
    /** Canyonlands, {@code VEGETAL_DECORATION}, after the scrub: groves (trees before their scrub), then the seep greenery. */
    public static final List<ResourceKey<PlacedFeature>> CANYONLANDS_GROVES = List.of(CANYON_GROVE_TREES, CANYON_GROVE_SHRUBS,
            CANYON_SEEP_GREENERY);
    /** Hoodoo fields, {@code LOCAL_MODIFICATIONS}, after the rockfall. */
    public static final List<ResourceKey<PlacedFeature>> HOODOO_GULLIES = List.of(HOODOO_GULLY_RUBBLE);

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private Ring2RockFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        // A juniper: a short trunk of dead wood with a dense crown of oak leaves (tinted by the dry canyon foliage
        // colour), coarse dirt under it; the forked dead trees of the groves give the gnarled shapes.
        FeatureUtils.register(context, CANYON_JUNIPER, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.DEAD_LOG.get()),
                new StraightTrunkPlacer(2, 1, 0),
                BlockStateProvider.simple(Blocks.OAK_LEAVES),
                new BlobFoliagePlacer(UniformInt.of(1, 2), ConstantInt.of(0), 2),
                new TwoLayersFeatureSize(1, 0, 1))
                .dirt(BlockStateProvider.simple(Blocks.COARSE_DIRT))
                .ignoreVines()
                .build());
        // Groves mix junipers and the bare dead trees of the dry riverbeds.
        FeatureUtils.register(context, CANYON_GROVE_TREE, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(CANYON_JUNIPER)), 0.6F)),
                PlacementUtils.inlinePlaced(configured.getOrThrow(RIVERBED_DEAD_TREES))));
        // Grove scrub: the canyon's dry plants and single persistent leaf blocks as low bushes.
        FeatureUtils.register(context, CANYON_GROVE_SCRUB, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.DRY_SHRUB.get().defaultBlockState(), 4)
                        .add(Blocks.DEAD_BUSH.defaultBlockState(), 2)
                        .add(ModBlocks.BARREL_CACTUS.get().defaultBlockState(), 1)
                        .add(ModBlocks.DESERT_FLOWER.get().defaultBlockState(), 1)
                        .add(Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 3))));
        // A seep: moss, grass and coarse dirt instead of the floor's sand, clay and gravel.
        FeatureUtils.register(context, CANYON_SEEP, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.MOSS_BLOCK.defaultBlockState(), 3)
                        .add(Blocks.GRASS_BLOCK.defaultBlockState(), 2)
                        .add(Blocks.COARSE_DIRT.defaultBlockState(), 1)),
                HolderSet.direct(Block::builtInRegistryHolder, floor()), UniformInt.of(2, 3), 1.3F, ConstantInt.of(1),
                ConstantInt.ZERO, BlockStateProvider.simple(Blocks.MOSS_BLOCK)));
        FeatureUtils.register(context, CANYON_SEEP_PLANTS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(weighted(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.FERN.defaultBlockState(), 4)
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 3)
                        .add(Blocks.MOSS_CARPET.defaultBlockState(), 2)
                        .add(Blocks.LARGE_FERN.defaultBlockState(), 1)
                        .add(Blocks.AZALEA.defaultBlockState(), 1))));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        BlockPredicate onFloor = BlockPredicate.matchesBlocks(below(), floor().toArray(new Block[0]));
        ClusterDensity groves = ClusterDensity.of(GROVES, GROVE_LOW, GROVE_HIGH);

        // Canyonlands, LOCAL_MODIFICATIONS. Green round the springs' water (a grid margin of SEEP_REACH keeps the probes
        // in the chunk), then patches on the canyon floor.
        PlacementUtils.register(context, CANYON_SPRING_SEEPS, features.getOrThrow(CANYON_SEEP),
                JitteredGridPlacement.of(8, SEEP_REACH, 1.0F),
                SURFACE,
                HeightBandFilter.below(Ring2Features.FLOOR_TOP),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(onFloor, waterWithin(SEEP_REACH))),
                BiomeFilter.biome());
        PlacementUtils.register(context, CANYON_FLOOR_SEEPS, features.getOrThrow(CANYON_SEEP),
                JitteredGridPlacement.of(8, SEEP_REACH, 1.0F),
                NoiseBandFilter.between(SEEPS, SEEP_NOISE, Double.MAX_VALUE),
                SURFACE,
                HeightBandFilter.below(Ring2Features.FLOOR_TOP),
                SlopeFilter.flat(2, 2),
                BlockPredicateFilter.forPredicate(onFloor),
                BiomeFilter.biome());

        // Hoodoo fields, LOCAL_MODIFICATIONS: rubble washed into the gullies.
        PlacementUtils.register(context, HOODOO_GULLY_RUBBLE, features.getOrThrow(CANYON_RUBBLE),
                JitteredGridPlacement.of(8, 1, 0.5F),
                NoiseBandFilter.between(GULLIES, -GULLY_HALF_WIDTH, GULLY_HALF_WIDTH),
                SURFACE,
                HeightBandFilter.below(HOODOO_FLOOR_TOP),
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(below(), Blocks.GRAVEL, Blocks.RED_SAND)),
                BiomeFilter.biome());

        // Canyonlands, VEGETAL_DECORATION: up to four trees per grove chunk (a few in a grove's core, the ramp thins the edges), the scrub of the
        // same grove field, then the greenery of the seeps (and of the spring rims).
        PlacementUtils.register(context, CANYON_GROVE_TREES, features.getOrThrow(CANYON_GROVE_TREE),
                JitteredGridPlacement.of(8, TREE_MARGIN, 0.35F, groves),
                SURFACE,
                SlopeFilter.flat(2, 3),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(below(), Blocks.RED_SAND, Blocks.COARSE_DIRT, ModBlocks.CRACKED_CLAY.get()))),
                BiomeFilter.biome());
        PlacementUtils.register(context, CANYON_GROVE_SHRUBS, features.getOrThrow(CANYON_GROVE_SCRUB),
                JitteredGridPlacement.of(4, 0, 0.45F, groves),
                SURFACE,
                PlacementUtils.isEmpty(),
                BiomeFilter.biome());
        PlacementUtils.register(context, CANYON_SEEP_GREENERY, features.getOrThrow(CANYON_SEEP_PLANTS),
                JitteredGridPlacement.of(4, 0, 0.6F),
                SURFACE,
                HeightBandFilter.below(Ring2Features.FLOOR_TOP),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(below(), Blocks.MOSS_BLOCK, Blocks.GRASS_BLOCK))),
                BiomeFilter.biome());
    }

    /** The loose ground of a canyon floor that a seep may turn green. */
    static List<Block> floor() {
        return List.of(Blocks.RED_SAND, ModBlocks.CRACKED_CLAY.get(), Blocks.GRAVEL);
    }

    /** Water within {@code reach} blocks beside the block below the origin, or one lower (a spring's pool or brook). */
    static BlockPredicate waterWithin(int reach) {
        List<BlockPredicate> water = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                water.add(BlockPredicate.matchesFluids(new Vec3i(dx, -1, dz), Fluids.WATER));
                water.add(BlockPredicate.matchesFluids(new Vec3i(dx, -2, dz), Fluids.WATER));
            }
        }
        return BlockPredicate.anyOf(water);
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

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id(name));
    }

    private static ResourceKey<PlacedFeature> placedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id(name));
    }
}
