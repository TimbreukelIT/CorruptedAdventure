package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Direction;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4MythicFeatures.weighted;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_GROUND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_LAND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_WATER;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.PATCHES_NOISE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * {@code cascade_vale} (ring 4 decor proef, 2026-10-19; decisions of the sparring 2026-10-19): lush. The moonpetal
 * trees themselves stay as they are, in groves now ({@link Ring4MythicFeatures#CASCADE_VALE_TREES}); around them:
 * <ul>
 *   <li>flower fields in patches (glow blooms, alliums, azure bluets, pink petals, moonpetal shrubs and mist ferns)
 *   with open grass between them;</li>
 *   <li>low blossom: moonpetal and flowering azalea bushes (trees of {@link IsolatedTreeFeature}) and the verdant
 *   blossom bushes of {@link VerdantFallsFeatures};</li>
 *   <li>the banks: reeds, giant lily pads and lotus on the water (the glow blooms along the lakes stay);</li>
 *   <li>the terrace edges ({@link TerraceDressingFeature}): vines down the walls, moss on the rims.</li>
 * </ul>
 * Every plant uses the order-independent rules of ring 4 ({@link GroundPlantFeature}, own-chunk features, air only).
 * Amounts are starting values for the tuning loop.
 */
public final class CascadeValeFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> BLOSSOM_BUSH = configuredKey("cascade_blossom_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> AZALEA_BUSH = configuredKey("cascade_azalea_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_FIELD = configuredKey("cascade_flower_field");
    static final ResourceKey<ConfiguredFeature<?, ?>> VERDANT_BLOSSOM = configuredKey("cascade_verdant_blossom");
    static final ResourceKey<ConfiguredFeature<?, ?>> TERRACE_DRESSING = configuredKey("cascade_terrace_dressing");

    public static final ResourceKey<PlacedFeature> CASCADE_VALE_BLOSSOM_BUSHES = placedKey("cascade_vale_blossom_bushes");
    public static final ResourceKey<PlacedFeature> CASCADE_VALE_AZALEA_BUSHES = placedKey("cascade_vale_azalea_bushes");
    public static final ResourceKey<PlacedFeature> CASCADE_VALE_LILY_PADS = placedKey("cascade_vale_lily_pads");
    public static final ResourceKey<PlacedFeature> CASCADE_VALE_LOTUS = placedKey("cascade_vale_lotus");
    public static final ResourceKey<PlacedFeature> CASCADE_VALE_REEDS = placedKey("cascade_vale_reeds");
    public static final ResourceKey<PlacedFeature> CASCADE_VALE_TERRACE_EDGES = placedKey("cascade_vale_terrace_edges");
    public static final ResourceKey<PlacedFeature> CASCADE_VALE_FLOWER_FIELDS = placedKey("cascade_vale_flower_fields");
    public static final ResourceKey<PlacedFeature> CASCADE_VALE_VERDANT_BLOSSOMS = placedKey("cascade_vale_verdant_blossoms");

    private CascadeValeFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Low blossom: a ball of moonpetal blossom or of (flowering) azalea leaves on one log (vanilla's jungle bush shape).
        FeatureUtils.register(context, BLOSSOM_BUSH, ModFeatures.ISOLATED_TREE.get(), bush(
                BlockStateProvider.simple(ModBlocks.MOONPETAL_LOG.get()), BlockStateProvider.simple(ModBlocks.MOONPETAL_LEAVES.get())));
        FeatureUtils.register(context, AZALEA_BUSH, ModFeatures.ISOLATED_TREE.get(), bush(BlockStateProvider.simple(Blocks.OAK_LOG),
                weighted(Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState(), 2, Blocks.AZALEA_LEAVES.defaultBlockState(), 3)));
        FeatureUtils.register(context, FLOWER_FIELD, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(flowers()));
        FeatureUtils.register(context, VERDANT_BLOSSOM, ModFeatures.GROUND_PLANT.get(),
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.VERDANT_BLOSSOM_BUSH.get())));
        FeatureUtils.register(context, TERRACE_DRESSING, ModFeatures.TERRACE_DRESSING.get(), new TerraceDressingConfiguration(1,
                Blocks.VINE.defaultBlockState(), 0.3F, 3, Blocks.MOSS_BLOCK.defaultBlockState(), 0.3F,
                Blocks.MOSS_CARPET.defaultBlockState(), 0.15F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        // Bushes after the trees (Ring4Features.ORDER): round the groves, a few on the open grass.
        register(context, CASCADE_VALE_BLOSSOM_BUSHES, BLOSSOM_BUSH, JitteredGridPlacement.of(4, 1, 0.2F, ClusterDensity.of(
                        Ring4Placements.GROVES_NOISE, -0.25, 0.0).withSalt(2).withFloor(0.15)), SURFACE, only(ON_GROUND),
                PlacementUtils.filteredByBlockSurvival(ModBlocks.MOONPETAL_SAPLING.get()));
        register(context, CASCADE_VALE_AZALEA_BUSHES, AZALEA_BUSH, JitteredGridPlacement.of(8, 1, 0.3F, ClusterDensity.of(
                        Ring4Placements.GROVES_NOISE, -0.25, 0.0).withSalt(3).withFloor(0.1)), SURFACE, only(ON_GROUND),
                PlacementUtils.filteredByBlockSurvival(Blocks.AZALEA));

        // Water: lily pads and lotus on still water, reeds on the banks.
        register(context, CASCADE_VALE_LILY_PADS, Ring4MythicFeatures.GIANT_LILY_PAD, JitteredGridPlacement.of(8, 1, 0.3F), SURFACE,
                only(ON_WATER));
        register(context, CASCADE_VALE_LOTUS, Ring4MythicFeatures.LOTUS, JitteredGridPlacement.of(4, 0, 0.12F), SURFACE, only(ON_WATER));
        // Backlog r. 367: the bank on the frozen heightmaps (a neighbour's tree in the water shifts no reed's draws).
        register(context, CASCADE_VALE_REEDS, Ring4MythicFeatures.REED_ACCENT, JitteredGridPlacement.of(8, 1, 0.5F), SURFACE,
                Ring4Filter.builder().waterNear(2).build());

        // Land: the terrace edges first (moss under the rim's plants), then the flower fields and the verdant blossom.
        register(context, CASCADE_VALE_TERRACE_EDGES, TERRACE_DRESSING, JitteredGridPlacement.of(1, 0, 1.0F), SURFACE);
        register(context, CASCADE_VALE_FLOWER_FIELDS, FLOWER_FIELD, JitteredGridPlacement.of(1, 0, 0.55F, ClusterDensity.of(
                PATCHES_NOISE, 0.1, 0.3)), SURFACE, only(ON_GROUND));
        register(context, CASCADE_VALE_VERDANT_BLOSSOMS, VERDANT_BLOSSOM, JitteredGridPlacement.of(4, 0, 0.35F, ClusterDensity.of(
                PATCHES_NOISE, 0.15, 0.35).withSalt(2)), SURFACE, only(ON_LAND));
    }

    private static TreeConfiguration bush(BlockStateProvider log, BlockStateProvider leaves) {
        return new TreeConfiguration.TreeConfigurationBuilder(log, new StraightTrunkPlacer(1, 0, 0), leaves,
                new BushFoliagePlacer(ConstantInt.of(2), ConstantInt.of(1), 2), new TwoLayersFeatureSize(0, 0, 0)).build();
    }

    /**
     * The flower fields: glow blooms, alliums, azure bluets, pink petals (1–4 petals, every way round), moonpetal
     * shrubs and mist ferns among them.
     */
    private static WeightedStateProvider flowers() {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        list.add(ModBlocks.GLOW_BLOOM.get().defaultBlockState(), 6);
        list.add(Blocks.ALLIUM.defaultBlockState(), 5);
        list.add(Blocks.AZURE_BLUET.defaultBlockState(), 6);
        list.add(ModBlocks.MOONPETAL_SHRUB.get().defaultBlockState(), 4);
        list.add(ModBlocks.MIST_FERN.get().defaultBlockState(), 5);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int amount = 2; amount <= 4; amount++) {
                list.add(Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.FACING, facing)
                        .setValue(PinkPetalsBlock.AMOUNT, amount), 1);
            }
        }
        return new WeightedStateProvider(list);
    }
}
