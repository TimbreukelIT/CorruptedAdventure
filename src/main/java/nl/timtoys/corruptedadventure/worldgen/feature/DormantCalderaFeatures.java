package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;

import java.util.List;

/**
 * The dormant caldera as an overgrown crater (change 47c, V8; 47d, decision 4b-1): life has come back. Moss carpets
 * and moss hummocks cover the floor, blooming azaleas stand in groves with small azalea bushes around them, vines and
 * hanging roots drape the crater wall, dripleaf and ferns grow in the damp hollows, now and then a fallen trunk lies
 * in the moss; ember berry bushes at the wall's foot and glow lichen on it. No water (contract). Density and grouping
 * come from a noise ({@code ring_3_caldera_growth}): groves where it is high, open moss meadows where it is low.
 * Amounts are starting values, tuned at the next test stop.
 * <p>
 * Determinism as in {@link Ring3DressingFeatures}: {@link JitteredGridPlacement} with a margin of at least the reach,
 * so everything stays in its own chunk; the floor is the pure terrain's {@code caldera_dormant}.
 */
public final class DormantCalderaFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> FERN = configured("caldera_fern");
    static final ResourceKey<ConfiguredFeature<?, ?>> AZALEA_BUSH = configured("caldera_azalea_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> GLOW_LICHEN = configured("caldera_glow_lichen");
    static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_HUMMOCK = configured("caldera_moss_hummock");
    static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_CARPET = configured("caldera_moss_carpet");
    static final ResourceKey<ConfiguredFeature<?, ?>> AZALEA_SHRUB = configured("caldera_azalea_shrub");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOG_X = configured("caldera_fallen_log_x");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOG_Z = configured("caldera_fallen_log_z");
    static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_LOGS = configured("caldera_fallen_logs");
    static final ResourceKey<ConfiguredFeature<?, ?>> WALL_DRAPE = configured("caldera_wall_drape");

    // Placed, in the global order of their step (Ring3Features.ORDER).
    /** LOCAL_MODIFICATIONS: the moss hummocks shape the floor before anything grows on it. */
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_MOSS_HUMMOCKS = placed("dormant_caldera_moss_hummocks");
    // VEGETAL_DECORATION.
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_AZALEA_TREES = placed("dormant_caldera_azalea_trees");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_AZALEA_SHRUBS = placed("dormant_caldera_azalea_shrubs");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_FALLEN_LOGS = placed("dormant_caldera_fallen_logs");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_AZALEA_BUSHES = placed("dormant_caldera_azalea_bushes");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_DRIPLEAF = placed("dormant_caldera_dripleaf");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_FERNS = placed("dormant_caldera_ferns");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_BERRY_BUSHES = placed("dormant_caldera_berry_bushes");
    /** After everything that stands: the carpet fills the moss around it. */
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_MOSS_CARPETS = placed("dormant_caldera_moss_carpets");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_WALL_DRAPES = placed("dormant_caldera_wall_drapes");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_GLOW_LICHEN = placed("dormant_caldera_glow_lichen");

    /** Groves where it is high, open moss meadows where it is low. */
    static final ResourceKey<NormalNoise.NoiseParameters> GROWTH = ResourceKey.create(Registries.NOISE,
            CorruptedAdventure.id("ring_3_caldera_growth"));
    static final double GROVE = 0.15;
    /** Vanilla's azalea tree reaches 5 from its trunk (a bend of 2, a crown of 3). */
    private static final int AZALEA_TREE_REACH = 5;
    /** A fallen trunk is up to this long: the ground must be level that far. */
    private static final int FALLEN_LOG_LENGTH = 5;
    /** A moss hummock reaches 3 · 1.3 + 1 = 5. */
    private static final int HUMMOCK_REACH = 5;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    /**
     * The own chunk's live surface, for everything that grows (also on the hummocks, not under a crown): in the crater
     * every feature writes only its own chunk, so this heightmap holds only this chunk's own work, in a fixed order.
     */
    private static final PlacementModifier LIVE_SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE);

    private DormantCalderaFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        FeatureUtils.register(context, FERN, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.FERN.defaultBlockState(), 4)
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 2)
                        .add(Blocks.MOSS_CARPET.defaultBlockState(), 1))));
        FeatureUtils.register(context, AZALEA_BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.AZALEA.defaultBlockState(), 2)
                        .add(Blocks.FLOWERING_AZALEA.defaultBlockState(), 3))));
        // On the wall faces only, one block from the origin (vanilla's search goes no further) and spreading a little.
        FeatureUtils.register(context, GLOW_LICHEN, Feature.MULTIFACE_GROWTH, new MultifaceGrowthConfiguration(
                (MultifaceBlock) Blocks.GLOW_LICHEN, 1, false, false, true, 0.5F, HolderSet.direct(Block::builtInRegistryHolder,
                ModBlocks.PUMICE.get(), ModBlocks.ASH_BLOCK.get(), Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.TUFF, Blocks.BLACKSTONE)));
        // A low mound of moss: the floor raised one to two blocks.
        FeatureUtils.register(context, MOSS_HUMMOCK, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                BlockStateProvider.simple(Blocks.MOSS_BLOCK), floorGround(), UniformInt.of(2, 3), 1.3F, ConstantInt.of(1),
                UniformInt.of(1, 2), BlockStateProvider.simple(Blocks.MOSS_BLOCK)));
        FeatureUtils.register(context, MOSS_CARPET, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MOSS_CARPET)));
        // A small azalea bush: one log under a round crown of (flowering) azalea leaves.
        FeatureUtils.register(context, AZALEA_SHRUB, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.OAK_LOG), new StraightTrunkPlacer(1, 0, 0),
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.AZALEA_LEAVES.defaultBlockState(), 2)
                        .add(Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState(), 1)),
                new BushFoliagePlacer(ConstantInt.of(2), ConstantInt.of(1), 2), new TwoLayersFeatureSize(0, 0, 0))
                .dirt(BlockStateProvider.simple(Blocks.ROOTED_DIRT)).build());
        FeatureUtils.register(context, FALLEN_LOG_X, Feature.BLOCK_COLUMN, fallenLog(Direction.EAST));
        FeatureUtils.register(context, FALLEN_LOG_Z, Feature.BLOCK_COLUMN, fallenLog(Direction.SOUTH));
        FeatureUtils.register(context, FALLEN_LOGS, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(FALLEN_LOG_Z)), 0.5F)),
                PlacementUtils.inlinePlaced(configured.getOrThrow(FALLEN_LOG_X))));
        FeatureUtils.register(context, WALL_DRAPE, ModFeatures.WALL_DRAPE.get(), new WallDrapeConfiguration(UniformInt.of(4, 12), 0.7F, 0.5F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        // The crater floor: no lava, the found surface on the terrain.
        PlacementModifier floor = VolcanicFilter.builder().above(VolcanicOutput.Output.CALDERA_DORMANT, 0.5)
                .below(VolcanicOutput.Output.LAVA_BANK, 0.1).groundOnly(VolcanicFilter.DEFAULT_TOLERANCE).build();
        PlacementModifier grove = NoiseBandFilter.between(GROWTH, GROVE, Double.MAX_VALUE);

        // LOCAL_MODIFICATIONS: hummocks in the open moss meadows (the groves keep their floor for the trees).
        PlacementUtils.register(context, DORMANT_CALDERA_MOSS_HUMMOCKS, features.getOrThrow(MOSS_HUMMOCK),
                JitteredGridPlacement.of(16, HUMMOCK_REACH, 0.8F), SURFACE, NoiseBandFilter.between(GROWTH, -Double.MAX_VALUE, GROVE), floor,
                BiomeFilter.biome());

        // VEGETAL_DECORATION. Groves: living azaleas (the beacons) with small azalea bushes around them.
        PlacementUtils.register(context, DORMANT_CALDERA_AZALEA_TREES, features.getOrThrow(TreeFeatures.AZALEA_TREE),
                JitteredGridPlacement.of(16, AZALEA_TREE_REACH + 1, 0.6F), LIVE_SURFACE, grove, floor, survives(Blocks.AZALEA),
                BiomeFilter.biome());
        // Bush crowns reach 2: margin 3 (cell 8).
        // G2: the bushes follow the growth noise more strongly (thick round the groves, thin on the meadows).
        PlacementUtils.register(context, DORMANT_CALDERA_AZALEA_SHRUBS, features.getOrThrow(AZALEA_SHRUB),
                JitteredGridPlacement.of(8, 3, 0.75F, ClusterDensity.of(GROWTH, GROVE - 0.2, GROVE + 0.1)), LIVE_SURFACE, floor,
                survives(Blocks.AZALEA), BiomeFilter.biome());
        PlacementUtils.register(context, DORMANT_CALDERA_FALLEN_LOGS, features.getOrThrow(FALLEN_LOGS),
                JitteredGridPlacement.of(16, 5, 0.2F), LIVE_SURFACE, SlopeFilter.flat(FALLEN_LOG_LENGTH, 0), floor,
                BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE), BiomeFilter.biome());
        PlacementUtils.register(context, DORMANT_CALDERA_AZALEA_BUSHES, features.getOrThrow(AZALEA_BUSH),
                JitteredGridPlacement.of(4, 0, 0.7F, ClusterDensity.of(GROWTH, GROVE - 0.3, GROVE + 0.05).withFloor(0.08)), LIVE_SURFACE,
                floor, survives(Blocks.AZALEA), BiomeFilter.biome());
        // Dripleaf in the damp hollows (higher ground around), ferns everywhere.
        PlacementUtils.register(context, DORMANT_CALDERA_DRIPLEAF, features.getOrThrow(CaveFeatures.DRIPLEAF),
                JitteredGridPlacement.of(2, 0, 0.35F), LIVE_SURFACE, SlopeFilter.wallFoot(4, 2), SlopeFilter.flat(1, 1), floor,
                survives(Blocks.BIG_DRIPLEAF), BiomeFilter.biome());
        PlacementUtils.register(context, DORMANT_CALDERA_FERNS, features.getOrThrow(FERN),
                JitteredGridPlacement.of(2, 0, 0.3F), LIVE_SURFACE, floor, survives(Blocks.FERN), BiomeFilter.biome());
        // Berry bushes at the foot of the crater wall.
        PlacementUtils.register(context, DORMANT_CALDERA_BERRY_BUSHES, features.getOrThrow(EmberWoodsFeatures.EMBER_BERRY_BUSH),
                JitteredGridPlacement.of(8, 0, 0.3F), LIVE_SURFACE, SlopeFilter.wallFoot(4, 4), survives(ModBlocks.EMBER_BERRY_BUSH.get()),
                BiomeFilter.biome());
        // A carpet on most of the moss that is still bare.
        PlacementUtils.register(context, DORMANT_CALDERA_MOSS_CARPETS, features.getOrThrow(MOSS_CARPET),
                JitteredGridPlacement.of(2, 0, 0.6F), LIVE_SURFACE, floor,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(BlockPos.ZERO.below(), Blocks.MOSS_BLOCK))),
                BiomeFilter.biome());
        // Vines and hanging roots up the wall from its foot (the drape reads its column's four neighbours: margin 1).
        PlacementUtils.register(context, DORMANT_CALDERA_WALL_DRAPES, features.getOrThrow(WALL_DRAPE),
                JitteredGridPlacement.of(4, 1, 0.7F), LIVE_SURFACE, SlopeFilter.wallFoot(1, 3), BiomeFilter.biome());
        // Glow lichen next to the wall, up to 2 above its foot (the multiface feature reads 2 around: margin 3).
        PlacementUtils.register(context, DORMANT_CALDERA_GLOW_LICHEN, features.getOrThrow(GLOW_LICHEN),
                JitteredGridPlacement.of(8, 3, 0.6F), LIVE_SURFACE, SlopeFilter.wallFoot(1, 3), RandomOffsetPlacement.vertical(UniformInt.of(0, 2)),
                BiomeFilter.biome());
    }

    /** The ground of the crater floor (its surface: moss, coarse dirt, ash, pumice, basalt). */
    private static HolderSet<Block> floorGround() {
        return HolderSet.direct(Block::builtInRegistryHolder, Blocks.MOSS_BLOCK, Blocks.COARSE_DIRT, ModBlocks.ASH_BLOCK.get(),
                ModBlocks.PUMICE.get(), Blocks.BASALT);
    }

    /** A fallen azalea trunk of 3–5 along x or z, only through air. */
    private static BlockColumnConfiguration fallenLog(Direction direction) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, direction.getAxis());
        return new BlockColumnConfiguration(List.of(BlockColumnConfiguration.layer(UniformInt.of(3, FALLEN_LOG_LENGTH), BlockStateProvider.simple(log))),
                direction, BlockPredicate.ONLY_IN_AIR_PREDICATE, false);
    }

    private static PlacementModifier survives(Block block) {
        return BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.wouldSurvive(block.defaultBlockState(), BlockPos.ZERO)));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
