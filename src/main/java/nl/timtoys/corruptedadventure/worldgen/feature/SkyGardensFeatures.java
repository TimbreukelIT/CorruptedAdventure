package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.CherryTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.List;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4MythicFeatures.weighted;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_GROUND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_LAND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.PATCHES_NOISE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * {@code sky_gardens} and the undersides of the islands (decor rollout C, 2026-10-19; decisions of the sparring
 * 2026-10-19): gardens on the high islands, like {@code cascade_vale} but airier.
 * <ul>
 *   <li>small moonpetal groves ({@link Ring4MythicFeatures#SKY_GARDENS_TREES}: small and cascade-sized moonpetals on
 *   a grid of 8, {@link #TREE_PLACEMENT}), blossom and azalea bushes round them;</li>
 *   <li>flower fields in patches (glow blooms, bluets, lilies of the valley, daisies, alliums, cornflowers, pink
 *   petals, moonpetal shrubs and mist ferns) with open grass between, moss patches with carpets;</li>
 *   <li>the eye-catchers ({@link SkyLandmarks}): a giant moonpetal whose crown covers its island
 *   ({@link GiantMoonpetalFeature}) and a crumbling ring of pillars with an arch ({@link SkyRuinFeature});</li>
 *   <li>under every island of the mythical band ({@link IslandDressingFeature}): moss and aquamarine in the underside,
 *   glow lichen, spore blossoms, roots and moss strands hanging from it, vines down the outer walls.</li>
 * </ul>
 * Every plant uses the order-independent rules of ring 4 ({@link GroundPlantFeature}, own-chunk features, air only;
 * the trees {@link IsolatedTreeFeature}, the ruin a ground object). Amounts are starting values for the tuning loop.
 */
public final class SkyGardensFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> SMALL_MOONPETAL = configuredKey("sky_small_moonpetal");
    /** The trees of the sky gardens: a small moonpetal or a cascade-sized one. */
    static final ResourceKey<ConfiguredFeature<?, ?>> SKY_TREE = configuredKey("sky_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_FIELD = configuredKey("sky_flower_field");
    static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_FLOOR = configuredKey("sky_moss_floor");
    static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_CARPET = configuredKey("sky_moss_carpet");
    static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_MOONPETAL = configuredKey("giant_moonpetal");
    static final ResourceKey<ConfiguredFeature<?, ?>> RUIN = configuredKey("sky_ruin");
    static final ResourceKey<ConfiguredFeature<?, ?>> ISLAND_DRESSING = configuredKey("island_dressing");
    static final ResourceKey<ConfiguredFeature<?, ?>> ISLAND_DRESSING_BLIGHTED = configuredKey("island_dressing_blighted");

    public static final ResourceKey<PlacedFeature> SKY_GARDENS_MOSS_FLOOR = placedKey("sky_gardens_moss_floor");
    public static final ResourceKey<PlacedFeature> SKY_GARDENS_RUINS = placedKey("sky_gardens_ruins");
    public static final ResourceKey<PlacedFeature> SKY_GARDENS_GIANT_MOONPETALS = placedKey("sky_gardens_giant_moonpetals");
    public static final ResourceKey<PlacedFeature> SKY_GARDENS_BLOSSOM_BUSHES = placedKey("sky_gardens_blossom_bushes");
    public static final ResourceKey<PlacedFeature> SKY_GARDENS_AZALEA_BUSHES = placedKey("sky_gardens_azalea_bushes");
    public static final ResourceKey<PlacedFeature> SKY_GARDENS_FLOWER_FIELDS = placedKey("sky_gardens_flower_fields");
    public static final ResourceKey<PlacedFeature> SKY_GARDENS_MOSS_CARPETS = placedKey("sky_gardens_moss_carpets");
    /** Every ground biome (the islands lie over all of them): the living undersides, after the hanging strands. */
    public static final ResourceKey<PlacedFeature> ISLAND_DRESSINGS = placedKey("island_dressings");
    /** Sparring 2026-10-20 (B2-2): the dead undersides of the blighted band's islands, right after the living ones. */
    public static final ResourceKey<PlacedFeature> ISLAND_DRESSINGS_BLIGHTED = placedKey("island_dressings_blighted");

    /**
     * The small groves of the sky gardens: the grove noise at twice its frequency (groves of ~30 blocks, a few per
     * island), a tree in about half of the cells of 8 inside them.
     */
    static final ClusterDensity GROVES = new ClusterDensity(Ring4Placements.GROVES_NOISE, 2.0, 5, -0.05, 0.25, 0.0);
    /** The placement of {@link Ring4MythicFeatures#SKY_GARDENS_TREES}. */
    static final PlacementModifier TREE_PLACEMENT = JitteredGridPlacement.of(8, 2, 0.55F, GROVES);
    /** The moss patches (and their carpets): one field of the patch noise. */
    private static final int MOSS_SALT = 11;

    private SkyGardensFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        // A small moonpetal (4–5 high, one or two short branches): the young trees of the groves.
        FeatureUtils.register(context, SMALL_MOONPETAL, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.MOONPETAL_LOG.get()),
                new CherryTrunkPlacer(4, 1, 0, new WeightedListInt(SimpleWeightedRandomList.<IntProvider>builder()
                        .add(ConstantInt.of(1), 2).add(ConstantInt.of(2), 1).build()),
                        ConstantInt.of(2), UniformInt.of(-2, -1), UniformInt.of(-1, 0)),
                BlockStateProvider.simple(ModBlocks.MOONPETAL_LEAVES.get()),
                new CherryFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(4), 0.25F, 0.5F, 0.16666667F, 0.33333334F),
                new TwoLayersFeatureSize(1, 0, 1)).ignoreVines().build());
        FeatureUtils.register(context, SKY_TREE, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(SMALL_MOONPETAL)), 0.55F)),
                PlacementUtils.inlinePlaced(features.getOrThrow(ModConfiguredFeatures.MOONPETAL_TREE))));
        FeatureUtils.register(context, FLOWER_FIELD, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(flowers()));
        FeatureUtils.register(context, MOSS_FLOOR, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MOSS_BLOCK)));
        FeatureUtils.register(context, MOSS_CARPET, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MOSS_CARPET)));

        FeatureUtils.register(context, GIANT_MOONPETAL, ModFeatures.GIANT_MOONPETAL.get(), new GiantMoonpetalConfiguration(
                ModBlocks.MOONPETAL_LOG.get().defaultBlockState(), ModBlocks.MOONPETAL_WOOD.get().defaultBlockState(),
                ModBlocks.MOONPETAL_LEAVES.get().defaultBlockState()));
        // White stone gone green: quartz and calcite pillars with mossy bricks, a crystal on a tidestone in the middle.
        FeatureUtils.register(context, RUIN, ModFeatures.SKY_RUIN.get(), new SkyRuinConfiguration(
                weighted(Blocks.QUARTZ_PILLAR.defaultBlockState(), 5, Blocks.CALCITE.defaultBlockState(), 2,
                        Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 1),
                weighted(Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), 2, Blocks.QUARTZ_BRICKS.defaultBlockState(), 1),
                weighted(Blocks.POLISHED_DIORITE.defaultBlockState(), 2, Blocks.CALCITE.defaultBlockState(), 2,
                        Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 1, Blocks.MOSS_BLOCK.defaultBlockState(), 1),
                weighted(Blocks.CALCITE.defaultBlockState(), 2, Blocks.QUARTZ_SLAB.defaultBlockState(), 2,
                        Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 1, Blocks.QUARTZ_BRICKS.defaultBlockState(), 1),
                ModBlocks.POLISHED_TIDESTONE.get().defaultBlockState(),
                ModBlocks.AQUAMARINE_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP),
                Blocks.MOSS_CARPET.defaultBlockState(), Blocks.VINE.defaultBlockState()));

        // Under the islands: glow lichen, spore blossoms, (glowing) roots and aquamarine buds; moss strands; now and then
        // an aquamarine block with a cluster under it; vines down the walls.
        FeatureUtils.register(context, ISLAND_DRESSING, ModFeatures.ISLAND_DRESSING.get(), new IslandDressingConfiguration(
                Blocks.MOSS_BLOCK.defaultBlockState(), -0.1F,
                weighted(Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.UP), true), 6,
                        Blocks.SPORE_BLOSSOM.defaultBlockState(), 2,
                        ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState(), 3, Blocks.HANGING_ROOTS.defaultBlockState(), 1,
                        ModBlocks.SMALL_AQUAMARINE_BUD.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 1,
                        ModBlocks.MEDIUM_AQUAMARINE_BUD.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 1),
                0.2F,
                ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(), 0.05F, 2, 6,
                ModBlocks.AQUAMARINE_BLOCK.get().defaultBlockState(),
                weighted(ModBlocks.LARGE_AQUAMARINE_BUD.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 1,
                        ModBlocks.AQUAMARINE_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 2),
                0.006F,
                Blocks.VINE.defaultBlockState(), 0.1F, 3, false));
        // Sparring 2026-10-20 (B2-2): under the islands of the blighted band dead roots in rooted earth, grey moss
        // strands (the hanging swamp moss, longer and more of them), toxic crystal knobs; a few vines only.
        FeatureUtils.register(context, ISLAND_DRESSING_BLIGHTED, ModFeatures.ISLAND_DRESSING.get(), new IslandDressingConfiguration(
                Blocks.ROOTED_DIRT.defaultBlockState(), 0.0F,
                weighted(Blocks.HANGING_ROOTS.defaultBlockState(), 8,
                        ModBlocks.SMALL_TOXIC_CRYSTAL_BUD.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 1,
                        ModBlocks.MEDIUM_TOXIC_CRYSTAL_BUD.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 1),
                0.2F,
                ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(), 0.1F, 3, 9,
                ModBlocks.TOXIC_CRYSTAL_BLOCK.get().defaultBlockState(),
                weighted(ModBlocks.LARGE_TOXIC_CRYSTAL_BUD.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 1,
                        ModBlocks.TOXIC_CRYSTAL_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 2),
                0.012F,
                Blocks.VINE.defaultBlockState(), 0.03F, 2, true));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        Ring4Placements.perChunk(context, ISLAND_DRESSINGS, ISLAND_DRESSING);
        Ring4Placements.perChunk(context, ISLAND_DRESSINGS_BLIGHTED, ISLAND_DRESSING_BLIGHTED);
        // The moss patches before anything grows on them; the landmarks before the trees (no tree roots on a ruin's stone).
        register(context, SKY_GARDENS_MOSS_FLOOR, MOSS_FLOOR, JitteredGridPlacement.of(1, 0, 1.0F, new ClusterDensity(PATCHES_NOISE, 1.0,
                        MOSS_SALT, 0.2, 0.4, 0.0)), SURFACE, RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
                only(BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK)));
        Ring4Placements.perChunk(context, SKY_GARDENS_RUINS, RUIN);
        Ring4Placements.perChunk(context, SKY_GARDENS_GIANT_MOONPETALS, GIANT_MOONPETAL);
        // Bushes round the groves, a few out on the open grass.
        register(context, SKY_GARDENS_BLOSSOM_BUSHES, CascadeValeFeatures.BLOSSOM_BUSH, JitteredGridPlacement.of(8, 1, 0.3F,
                        new ClusterDensity(Ring4Placements.GROVES_NOISE, 2.0, 5, -0.25, 0.0, 0.2)), SURFACE, only(ON_GROUND),
                PlacementUtils.filteredByBlockSurvival(ModBlocks.MOONPETAL_SAPLING.get()));
        register(context, SKY_GARDENS_AZALEA_BUSHES, CascadeValeFeatures.AZALEA_BUSH, JitteredGridPlacement.of(16, 1, 0.5F,
                        new ClusterDensity(Ring4Placements.GROVES_NOISE, 2.0, 5, -0.25, 0.0, 0.3)), SURFACE, only(ON_GROUND),
                PlacementUtils.filteredByBlockSurvival(Blocks.AZALEA));
        // Flower fields with open grass between; carpets on the moss patches.
        register(context, SKY_GARDENS_FLOWER_FIELDS, FLOWER_FIELD, JitteredGridPlacement.of(1, 0, 0.4F, new ClusterDensity(PATCHES_NOISE,
                1.0, 12, 0.15, 0.35, 0.0)), SURFACE, only(ON_GROUND));
        register(context, SKY_GARDENS_MOSS_CARPETS, MOSS_CARPET, JitteredGridPlacement.of(2, 0, 0.3F, new ClusterDensity(PATCHES_NOISE,
                1.0, MOSS_SALT, 0.3, 0.5, 0.0)), SURFACE, only(BlockPredicate.allOf(ON_LAND,
                BlockPredicate.matchesBlocks(Ring4Placements.up(-1), Blocks.MOSS_BLOCK))));
    }

    /** The flower fields: lighter than the cascades' (more white and blue), a few pink petals, moonpetal shrubs and mist ferns. */
    private static WeightedStateProvider flowers() {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        list.add(ModBlocks.GLOW_BLOOM.get().defaultBlockState(), 4);
        list.add(Blocks.AZURE_BLUET.defaultBlockState(), 5);
        list.add(Blocks.LILY_OF_THE_VALLEY.defaultBlockState(), 4);
        list.add(Blocks.OXEYE_DAISY.defaultBlockState(), 3);
        list.add(Blocks.ALLIUM.defaultBlockState(), 2);
        list.add(Blocks.CORNFLOWER.defaultBlockState(), 2);
        list.add(ModBlocks.MOONPETAL_SHRUB.get().defaultBlockState(), 3);
        list.add(ModBlocks.MIST_FERN.get().defaultBlockState(), 4);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            list.add(Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.FACING, facing).setValue(PinkPetalsBlock.AMOUNT, 2), 1);
        }
        return new WeightedStateProvider(list);
    }
}
