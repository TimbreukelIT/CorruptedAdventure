package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.List;

/**
 * The ember woods (change 47c, V3, G2-WB4 = A): a mixed wood of ember trees (6–9, round crown) and ember giants (10–12,
 * tiered crown) with bare charred trunks, open clearings with fallen logs, ember berry bushes and fire flora, and a thin
 * ash cover under the trees. Amounts are starting values, tuned at test stop 4b.
 * <p>
 * Determinism (plan §0.7): every placement starts in its own chunk on a {@link JitteredGridPlacement} grid and reads the
 * frozen {@code WORLD_SURFACE_WG} heightmap, the pure terrain and seeded noise. The clearings are a noise band snapped to
 * the grid cell, so trees and the clearing features never share a cell. Crowns may reach into the neighbouring chunks
 * (the one exception, as ring 2's palms): trunks stand at least 3 apart, so a trunk's free-space check never meets
 * another trunk, a leaf never replaces a log and a log replaces a leaf, so overlapping crowns end the same whatever the
 * order (only the leaves' {@code distance} may differ until their first update). Ground features stay below the crowns.
 * 47e (INT3): a crown on higher ground can reach the ground level of a neighbouring chunk; there a trunk or a fallen log
 * may still start in its leaves ({@link #airOrCrown}), and the ash layer and the fire flora are replaceable by trees
 * (block tag), so the crown takes them whichever chunk decorated first.
 */
public final class EmberWoodsFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_TREE = configured("ember_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_GIANT = configured("ember_giant");
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_WOOD_TREES = configured("ember_wood_trees");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOG_X = configured("fallen_charred_log_x");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOG_Z = configured("fallen_charred_log_z");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOGS = configured("fallen_charred_logs");
    static final ResourceKey<ConfiguredFeature<?, ?>> FIRE_FLORA = configured("fire_flora");
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_BERRY_BUSH = configured("ember_berry_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_ASH_DRIFT = configured("ember_woods_ash_drift");

    // Placed, in the global order of their step (Ring3Features.ORDER); all VEGETAL_DECORATION.
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_TREES = placed("ember_woods_trees");
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_FALLEN_LOGS = placed("ember_woods_fallen_logs");
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_FIRE_FLORA = placed("ember_woods_fire_flora");
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_BERRY_BUSHES = placed("ember_woods_berry_bushes");
    /** After the trees and the flora: the ash lies around what stands. */
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_ASH = placed("ember_woods_ash");

    /** The clearings of the wood: where it is at least {@link #CLEARING}, no trees. */
    static final ResourceKey<NormalNoise.NoiseParameters> CLEARINGS = noise("ring_3_ember_clearings");
    static final ResourceKey<NormalNoise.NoiseParameters> FLORA_PATCHES = noise("ring_3_fire_flora");
    static final double CLEARING = 0.35;
    /** The tree grid: cell 4, margin 1, so trunks stand at least 3 apart. */
    private static final int TREE_CELL = 4;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    /**
     * 47e (INT3): a trunk or a fallen log may start in the crown of a tree of the neighbouring chunk (a log replaces a
     * leaf, a leaf never a log), so whether that crown was there first does not matter. Air or ember leaves.
     */
    static BlockPredicate airOrCrown() {
        return BlockPredicate.anyOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesBlocks(ModBlocks.EMBER_LEAVES.get()));
    }

    private EmberWoodsFeatures() {
    }

    /** An ember tree: a charred trunk with a crown of glowing ember leaves; coarse dirt under the trunk. */
    static TreeConfiguration emberTree(StraightTrunkPlacer trunk, FoliagePlacer crown) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.CHARRED_LOG.get()),
                trunk,
                BlockStateProvider.simple(ModBlocks.EMBER_LEAVES.get()),
                crown,
                new TwoLayersFeatureSize(1, 0, 1))
                .dirt(BlockStateProvider.simple(Blocks.COARSE_DIRT))
                .ignoreVines()
                .build();
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        // 6–9 high, a round crown of radius 2–3; 10–12 high, a tiered crown.
        FeatureUtils.register(context, EMBER_TREE, Feature.TREE, emberTree(new StraightTrunkPlacer(6, 2, 1),
                new BlobFoliagePlacer(UniformInt.of(2, 3), ConstantInt.ZERO, 3)));
        FeatureUtils.register(context, EMBER_GIANT, Feature.TREE, emberTree(new StraightTrunkPlacer(10, 1, 1),
                new SpruceFoliagePlacer(UniformInt.of(2, 3), UniformInt.of(0, 1), UniformInt.of(4, 6))));
        FeatureUtils.register(context, EMBER_WOOD_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(EMBER_GIANT)), 0.2F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(Ring3DressingFeatures.CHARRED_DEAD_TREE)), 0.12F)),
                PlacementUtils.inlinePlaced(configured.getOrThrow(EMBER_TREE))));

        // A fallen trunk of 3–5 along x or z, only through air.
        FeatureUtils.register(context, FALLEN_LOG_X, Feature.BLOCK_COLUMN, fallenLog(Direction.EAST));
        FeatureUtils.register(context, FALLEN_LOG_Z, Feature.BLOCK_COLUMN, fallenLog(Direction.SOUTH));
        FeatureUtils.register(context, FALLEN_LOGS, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(FALLEN_LOG_Z)), 0.5F)),
                PlacementUtils.inlinePlaced(configured.getOrThrow(FALLEN_LOG_X))));

        FeatureUtils.register(context, FIRE_FLORA, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.ASH_SPROUTS.get().defaultBlockState(), 3)
                        .add(ModBlocks.FIRE_BLOOM.get().defaultBlockState(), 1))));
        FeatureUtils.register(context, EMBER_BERRY_BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(berryBushes()));
        FeatureUtils.register(context, EMBER_ASH_DRIFT, ModFeatures.ASH_DRIFT.get(), new AshDriftConfiguration(
                ModBlocks.ASH_LAYER.get(), Ring3DressingFeatures.openGround(), Ring3DressingFeatures.ASH_DRIFT_NOISE, 1.0,
                Ring3DressingFeatures.WIND, List.of(new AshDriftConfiguration.BiomeAmount(ModBiomes.EMBER_WOODS, 0.35F, false, false))));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementModifier ground = VolcanicFilter.builder().ground().build();
        PlacementModifier wood = NoiseBandFilter.snapped(CLEARINGS, -Double.MAX_VALUE, CLEARING, TREE_CELL);

        // About 7 trunks per chunk of wood (16 cells, chance 0.45), none in the clearings.
        PlacementUtils.register(context, EMBER_WOODS_TREES, features.getOrThrow(EMBER_WOOD_TREES),
                JitteredGridPlacement.of(TREE_CELL, 1, 0.45F), SURFACE, wood, ground,
                VolcanicLandmarkClearanceFilter.of(VolcanicLandmarkFeatures.TREE_CLEARANCE),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(airOrCrown(),
                        BlockPredicate.matchesBlocks(BlockPos.ZERO.below(), ModBlocks.ASH_BLOCK.get(), ModBlocks.SMOLDERING_ASH.get(),
                                Blocks.COARSE_DIRT, Blocks.TUFF, ModBlocks.PUMICE.get(), Blocks.SMOOTH_BASALT))),
                BiomeFilter.biome());
        // In the clearings: fallen logs (margin = longest log), berry bushes where the light lets them grow back.
        PlacementUtils.register(context, EMBER_WOODS_FALLEN_LOGS, features.getOrThrow(FALLEN_LOGS),
                JitteredGridPlacement.of(16, 5, 0.3F), SURFACE, NoiseBandFilter.snapped(CLEARINGS, CLEARING, Double.MAX_VALUE, 16),
                SlopeFilter.flat(5, 0), ground, VolcanicLandmarkClearanceFilter.of(VolcanicLandmarkFeatures.TREE_CLEARANCE),
                BlockPredicateFilter.forPredicate(airOrCrown()),
                BiomeFilter.biome());
        // Fire flora in patches (a noise), on ash and coarse dirt.
        PlacementUtils.register(context, EMBER_WOODS_FIRE_FLORA, features.getOrThrow(FIRE_FLORA),
                JitteredGridPlacement.of(2, 0, 0.35F), SURFACE, NoiseBandFilter.between(FLORA_PATCHES, 0.25, Double.MAX_VALUE), ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(ModBlocks.FIRE_BLOOM.get().defaultBlockState(), BlockPos.ZERO))),
                BiomeFilter.biome());
        PlacementUtils.register(context, EMBER_WOODS_BERRY_BUSHES, features.getOrThrow(EMBER_BERRY_BUSH),
                JitteredGridPlacement.of(8, 0, 0.15F), SURFACE, NoiseBandFilter.snapped(CLEARINGS, CLEARING, Double.MAX_VALUE, 8), ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(ModBlocks.EMBER_BERRY_BUSH.get().defaultBlockState(), BlockPos.ZERO))),
                BiomeFilter.biome());
        // Once per chunk, as the ash drifts: each column's own biome decides.
        PlacementUtils.register(context, EMBER_WOODS_ASH, features.getOrThrow(EMBER_ASH_DRIFT));
    }

    private static BlockColumnConfiguration fallenLog(Direction direction) {
        BlockState log = ModBlocks.CHARRED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, direction.getAxis());
        return new BlockColumnConfiguration(List.of(BlockColumnConfiguration.layer(UniformInt.of(3, 5), BlockStateProvider.simple(log))),
                direction, airOrCrown(), false);
    }

    /** Ember berry bushes at every age but the youngest. */
    static BlockStateProvider berryBushes() {
        SimpleWeightedRandomList.Builder<BlockState> bushes = SimpleWeightedRandomList.builder();
        for (int age = 1; age <= 3; age++) {
            bushes.add(ModBlocks.EMBER_BERRY_BUSH.get().defaultBlockState().setValue(SweetBerryBushBlock.AGE, age), 1);
        }
        return new WeightedStateProvider(bushes);
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noise(String name) {
        return ResourceKey.create(Registries.NOISE, CorruptedAdventure.id(name));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
