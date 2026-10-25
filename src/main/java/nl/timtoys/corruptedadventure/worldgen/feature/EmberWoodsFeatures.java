package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
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
 * <p>
 * G2 (decisions 7 = A and the sub-zone of 2): larger clearings and a thinner wood in places; burn patches in the hearts
 * of the clearings (smouldering ash, stumps, fire flora); half-burnt trees and bare giants whose crowns hold charred wood,
 * which behaves as a log in the rules above (never replaced by a leaf, it replaces leaves) and stays within 2 of its
 * trunk.
 */
public final class EmberWoodsFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_TREE = configured("ember_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_GIANT = configured("ember_giant");
    static final ResourceKey<ConfiguredFeature<?, ?>> HALF_BURNT_EMBER_TREE = configured("half_burnt_ember_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> BARE_EMBER_GIANT = configured("bare_ember_giant");
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_WOOD_TREES = configured("ember_wood_trees");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOG_X = configured("fallen_charred_log_x");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOG_Z = configured("fallen_charred_log_z");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOGS = configured("fallen_charred_logs");
    static final ResourceKey<ConfiguredFeature<?, ?>> FIRE_FLORA = configured("fire_flora");
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_BERRY_BUSH = configured("ember_berry_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_ASH_DRIFT = configured("ember_woods_ash_drift");
    static final ResourceKey<ConfiguredFeature<?, ?>> CHARRED_STUMP = configured("ember_charred_stump");
    static final ResourceKey<ConfiguredFeature<?, ?>> GLOWING_STUMP = configured("ember_glowing_stump");
    static final ResourceKey<ConfiguredFeature<?, ?>> BURN_STUMPS = configured("ember_burn_stumps");

    // Placed, in the global order of their step (Ring3Features.ORDER); all VEGETAL_DECORATION.
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_TREES = placed("ember_woods_trees");
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_FALLEN_LOGS = placed("ember_woods_fallen_logs");
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_FIRE_FLORA = placed("ember_woods_fire_flora");
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_BERRY_BUSHES = placed("ember_woods_berry_bushes");
    /** G2: the burn patches in the hearts of the clearings: stumps, glowing and charred, and some fire flora. */
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_BURN_STUMPS = placed("ember_woods_burn_stumps");
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_BURN_FLORA = placed("ember_woods_burn_flora");
    /** After the trees and the flora: the ash lies around what stands. */
    public static final ResourceKey<PlacedFeature> EMBER_WOODS_ASH = placed("ember_woods_ash");

    /**
     * The clearings of the wood: where it is at least {@link #CLEARING}, no trees. G2 (decision 7 = A): larger and more
     * of them (the noise one octave coarser, the threshold lower), and the wood itself thinned in places
     * ({@link #CANOPY_GAPS}), so from above it is no closed carpet.
     */
    static final ResourceKey<NormalNoise.NoiseParameters> CLEARINGS = noise("ring_3_ember_clearings");
    static final ResourceKey<NormalNoise.NoiseParameters> FLORA_PATCHES = noise("ring_3_fire_flora");
    static final double CLEARING = 0.2;
    /**
     * G2 (decision 2, the ember woods' sub-zone): the hearts of the clearings, where the noise is at least this, are burn
     * patches: smouldering ash without trees (the surface, {@code tools/worldgen/ring3_surface.py}, reads the same
     * noise and threshold), glowing and charred stumps, a little fire flora.
     */
    static final double BURN = 0.42;
    /**
     * Thinner wood in places: the clearings' noise read finer and elsewhere (another salt), keeping a share of the trees
     * from 1 (dense) down to the floor (open wood with single trees).
     */
    static final ClusterDensity CANOPY_GAPS = new ClusterDensity(CLEARINGS, 3.0, 1, -0.3, 0.1, 0.3);
    /** The tree grid: cell 4, margin 1, so trunks stand at least 3 apart. */
    private static final int TREE_CELL = 4;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    /**
     * 47e (INT3): a trunk or a fallen log may start in the crown of a tree of the neighbouring chunk (a log replaces a
     * leaf, a leaf never a log), so whether that crown was there first does not matter. Air or ember leaves.
     */
    static BlockPredicate airOrCrown() {
        return BlockPredicate.anyOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesBlocks(ModBlocks.EMBER_LEAVES.get(), ModBlocks.CHARRED_WOOD.get()));
    }

    private EmberWoodsFeatures() {
    }

    /** An ember tree: a charred trunk with a crown of glowing ember leaves; coarse dirt under the trunk. */
    static TreeConfiguration emberTree(StraightTrunkPlacer trunk, FoliagePlacer crown) {
        return emberTree(trunk, BlockStateProvider.simple(ModBlocks.EMBER_LEAVES.get()), crown);
    }

    /**
     * G2 (decision 7 = A): an ember tree with charred wood between the ember leaves of its crown (half burnt), or charred
     * wood alone (a bare giant's branch stubs). Charred wood is no leaf: as a log it is never replaced by a leaf of
     * another crown and replaces their leaves itself, so overlapping crowns still end the same whatever the order. Such
     * crowns reach at most 2 from their trunk, so they never reach another trunk (trunks stand at least 3 apart).
     */
    static TreeConfiguration emberTree(StraightTrunkPlacer trunk, BlockStateProvider crownBlocks, FoliagePlacer crown) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.CHARRED_LOG.get()),
                trunk,
                crownBlocks,
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
        // G2: half burnt, 6–8 high, a smaller round crown (radius 2) of ember leaves and charred wood 1 : 1.
        BlockState charredWood = ModBlocks.CHARRED_WOOD.get().defaultBlockState();
        FeatureUtils.register(context, HALF_BURNT_EMBER_TREE, Feature.TREE, emberTree(new StraightTrunkPlacer(6, 2, 0),
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.EMBER_LEAVES.get().defaultBlockState(), 1)
                        .add(charredWood, 1)),
                new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.ZERO, 3)));
        // G2: a bare giant, 11–14 high, only charred branch stubs (radius 0 and 1 by turns) down its upper half.
        FeatureUtils.register(context, BARE_EMBER_GIANT, Feature.TREE, emberTree(new StraightTrunkPlacer(11, 2, 1),
                BlockStateProvider.simple(charredWood), new SpruceFoliagePlacer(ConstantInt.of(1), ConstantInt.ZERO, UniformInt.of(5, 7))));
        // Of the trunks: giant 20 %, bare dead trunk ~10 %, half burnt ~14 %, bare giant ~6 %, ember tree the rest.
        FeatureUtils.register(context, EMBER_WOOD_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(EMBER_GIANT)), 0.2F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(Ring3DressingFeatures.CHARRED_DEAD_TREE)), 0.12F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(HALF_BURNT_EMBER_TREE)), 0.2F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(BARE_EMBER_GIANT)), 0.1F)),
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
        // G2: stumps of the burn patches: charred (1–3 logs) or still glowing (1–2 logs under a cap of smouldering ash). As
        // the fallen logs they may start in a crown: every block of the column replaces a leaf or a crown's charred wood
        // and no crown replaces it, so the stump ends the same whatever decorated first (a leaf cap would not).
        BlockState log = ModBlocks.CHARRED_LOG.get().defaultBlockState();
        FeatureUtils.register(context, CHARRED_STUMP, Feature.BLOCK_COLUMN, new BlockColumnConfiguration(List.of(
                BlockColumnConfiguration.layer(UniformInt.of(1, 3), BlockStateProvider.simple(log))), Direction.UP, airOrCrown(), false));
        FeatureUtils.register(context, GLOWING_STUMP, Feature.BLOCK_COLUMN, new BlockColumnConfiguration(List.of(
                BlockColumnConfiguration.layer(UniformInt.of(1, 2), BlockStateProvider.simple(log)),
                BlockColumnConfiguration.layer(ConstantInt.of(1), BlockStateProvider.simple(ModBlocks.SMOLDERING_ASH.get()))),
                Direction.UP, airOrCrown(), false));
        FeatureUtils.register(context, BURN_STUMPS, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(GLOWING_STUMP)), 0.4F)),
                PlacementUtils.inlinePlaced(configured.getOrThrow(CHARRED_STUMP))));
        // G2: not on the smouldering ash, so the burn patches keep their glow (the drift fell before the fire).
        FeatureUtils.register(context, EMBER_ASH_DRIFT, ModFeatures.ASH_DRIFT.get(), new AshDriftConfiguration(
                ModBlocks.ASH_LAYER.get(), HolderSet.direct(Block::builtInRegistryHolder, ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get(),
                        Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BLACKSTONE, Blocks.GRAVEL, Blocks.TUFF),
                Ring3DressingFeatures.ASH_DRIFT_NOISE, 1.0,
                Ring3DressingFeatures.WIND, List.of(new AshDriftConfiguration.BiomeAmount(ModBiomes.EMBER_WOODS, 0.35F, false, false))));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementModifier ground = VolcanicFilter.builder().ground().build();
        PlacementModifier wood = NoiseBandFilter.snapped(CLEARINGS, -Double.MAX_VALUE, CLEARING, TREE_CELL);

        // At most 7 trunks per chunk of wood (16 cells, chance 0.45), down to 2 in its open parts; none in the clearings.
        PlacementUtils.register(context, EMBER_WOODS_TREES, features.getOrThrow(EMBER_WOOD_TREES),
                JitteredGridPlacement.of(TREE_CELL, 1, 0.45F, CANOPY_GAPS), SURFACE, wood, ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(airOrCrown(),
                        BlockPredicate.matchesBlocks(BlockPos.ZERO.below(), ModBlocks.ASH_BLOCK.get(), ModBlocks.SMOLDERING_ASH.get(),
                                Blocks.COARSE_DIRT, Blocks.TUFF, ModBlocks.PUMICE.get(), Blocks.SMOOTH_BASALT))),
                BiomeFilter.biome());
        // In the clearings: fallen logs (margin = longest log), berry bushes where the light lets them grow back.
        PlacementUtils.register(context, EMBER_WOODS_FALLEN_LOGS, features.getOrThrow(FALLEN_LOGS),
                JitteredGridPlacement.of(16, 5, 0.3F), SURFACE, NoiseBandFilter.snapped(CLEARINGS, CLEARING, Double.MAX_VALUE, 16),
                SlopeFilter.flat(5, 0), ground, BlockPredicateFilter.forPredicate(airOrCrown()),
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
        // G2: the burn patches. Stumps on the smouldering ash (one per 8 x 8 at most), fire flora where plain ash shows.
        PlacementModifier burn = NoiseBandFilter.between(CLEARINGS, BURN, Double.MAX_VALUE);
        PlacementUtils.register(context, EMBER_WOODS_BURN_STUMPS, features.getOrThrow(BURN_STUMPS),
                JitteredGridPlacement.of(8, 1, 0.45F), SURFACE, burn, ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(airOrCrown(),
                        BlockPredicate.matchesBlocks(BlockPos.ZERO.below(), ModBlocks.SMOLDERING_ASH.get(), ModBlocks.ASH_BLOCK.get(),
                                Blocks.COARSE_DIRT))),
                BiomeFilter.biome());
        PlacementUtils.register(context, EMBER_WOODS_BURN_FLORA, features.getOrThrow(FIRE_FLORA),
                JitteredGridPlacement.of(2, 0, 0.3F), SURFACE, burn, ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(ModBlocks.FIRE_BLOOM.get().defaultBlockState(), BlockPos.ZERO))),
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
