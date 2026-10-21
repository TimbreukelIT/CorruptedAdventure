package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandOutput;

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
 * {@code lotus_shallows} (ring 4 decor rollout group A, 2026-10-19; sparring 2026-10-19): calm, flowering water.
 * <ul>
 *   <li>dense fields of lotus and lily pads and groups of giant lily pads, in patches with open water between them
 *   ({@link Ring4Placements#PATCHES_NOISE});</li>
 *   <li>collars of reeds and sugar cane along the banks;</li>
 *   <li>small grassy islets in the shallows ({@link ShoalFeature}) with azalea shrubs, flowers and reeds round their
 *   edge, lagoon glowgrass on and round them; flowers and azalea bushes on the banks.</li>
 * </ul>
 * The plants on the water draw before they look ({@link GroundPlantFeature}); the bushes are trees of
 * {@link IsolatedTreeFeature} and stand on ground at least two blocks over the sea, so their leaves never meet an islet
 * below the frozen surface. Amounts are starting values for the tuning loop.
 */
public final class LotusShallowsFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> ISLET = configuredKey("lotus_islet");
    static final ResourceKey<ConfiguredFeature<?, ?>> FLOATING = configuredKey("lotus_floating_plants");
    static final ResourceKey<ConfiguredFeature<?, ?>> SUGAR_CANE = configuredKey("lotus_sugar_cane");
    static final ResourceKey<ConfiguredFeature<?, ?>> BANK_FLOWERS = configuredKey("lotus_bank_flowers");
    static final ResourceKey<ConfiguredFeature<?, ?>> AZALEA_BUSH = configuredKey("lotus_azalea_bush");

    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_ISLETS = placedKey("lotus_shallows_islets");
    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_LOTUS_FIELDS = placedKey("lotus_shallows_lotus_fields");
    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_LILY_GROUPS = placedKey("lotus_shallows_lily_groups");
    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_REED_COLLARS = placedKey("lotus_shallows_reed_collars");
    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_SUGAR_CANE = placedKey("lotus_shallows_sugar_cane");
    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_BUSHES = placedKey("lotus_shallows_bushes");
    public static final ResourceKey<PlacedFeature> LOTUS_SHALLOWS_BANK_FLOWERS = placedKey("lotus_shallows_bank_flowers");

    /** Water right below: the water's surface, without asking for air here ({@link GroundPlantFeature} looks itself). */
    static final BlockPredicate WATER_BELOW = BlockPredicate.matchesFluids(new Vec3i(0, -1, 0), Fluids.WATER);

    private LotusShallowsFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        BlockStateProvider flowers = weighted(Blocks.BLUE_ORCHID.defaultBlockState(), 4, Blocks.ALLIUM.defaultBlockState(), 2,
                Blocks.AZURE_BLUET.defaultBlockState(), 3, Blocks.OXEYE_DAISY.defaultBlockState(), 2, ModBlocks.GLOW_BLOOM.get().defaultBlockState(), 2,
                Blocks.SHORT_GRASS.defaultBlockState(), 6, Blocks.FERN.defaultBlockState(), 2,
                ModBlocks.LAGOON_GLOWGRASS.get().defaultBlockState(), 2);
        // Islets of ~8–25 blocks: the grove noise at three times its frequency, its own field (salt 6); the top one block
        // over the water, the edges sink to three under it.
        FeatureUtils.register(context, ISLET, ModFeatures.SHOAL.get(), new ShoalConfiguration(ModBiomes.LOTUS_SHALLOWS, 10,
                new ClusterDensity(Ring4Placements.GROVES_NOISE, 3.0, 6, 0.3, 0.6, 0.0), 5, 3, 1,
                BlockStateProvider.simple(Blocks.CLAY), BlockStateProvider.simple(Blocks.DIRT), BlockStateProvider.simple(Blocks.GRASS_BLOCK),
                flowers, 0.55F,
                weighted(persistent(Blocks.AZALEA_LEAVES.defaultBlockState()), 3, persistent(Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState()), 2),
                0.1F, 0.55F, weighted(Blocks.SEAGRASS.defaultBlockState(), 3,
                        ModBlocks.LAGOON_GLOWGRASS.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 1), 0.15F));
        groundPlant(context, FLOATING, weighted(ModBlocks.LOTUS.get().defaultBlockState(), 3, Blocks.LILY_PAD.defaultBlockState(), 2));
        FeatureUtils.register(context, SUGAR_CANE, Feature.BLOCK_COLUMN, BlockColumnConfiguration.simple(BiasedToBottomInt.of(2, 3),
                BlockStateProvider.simple(Blocks.SUGAR_CANE)));
        groundPlant(context, BANK_FLOWERS, flowers);
        // A low azalea bush (one log under a ball of (flowering) azalea leaves), as cascade_vale's.
        FeatureUtils.register(context, AZALEA_BUSH, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(Blocks.OAK_LOG), new StraightTrunkPlacer(1, 0, 0),
                weighted(Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState(), 2, Blocks.AZALEA_LEAVES.defaultBlockState(), 3),
                new BushFoliagePlacer(ConstantInt.of(2), ConstantInt.of(1), 2), new TwoLayersFeatureSize(0, 0, 0)).build());
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        Ring4Placements.perChunk(context, LOTUS_SHALLOWS_ISLETS, ISLET);
        // On the water: dense fields in patches (open water between them), groups of giant lily pads in other patches.
        register(context, LOTUS_SHALLOWS_LOTUS_FIELDS, FLOATING, JitteredGridPlacement.of(1, 0, 0.8F, ClusterDensity.of(
                PATCHES_NOISE, -0.05, 0.3).withSalt(1).withFloor(0.04)), SURFACE, only(WATER_BELOW));
        register(context, LOTUS_SHALLOWS_LILY_GROUPS, Ring4MythicFeatures.GIANT_LILY_PAD, JitteredGridPlacement.of(4, 0, 0.4F,
                ClusterDensity.of(PATCHES_NOISE, 0.0, 0.35).withSalt(2)), SURFACE, only(Ring4Placements.ON_WATER));
        // The banks: collars of reeds and sugar cane, flowers and a few azalea bushes on higher ground.
        register(context, LOTUS_SHALLOWS_REED_COLLARS, Ring4MythicFeatures.REED_ACCENT, JitteredGridPlacement.of(4, 1, 0.55F), SURFACE,
                Ring4Filter.builder().waterNear(2).build());
        register(context, LOTUS_SHALLOWS_SUGAR_CANE, SUGAR_CANE, JitteredGridPlacement.of(2, 0, 0.35F, ClusterDensity.of(
                PATCHES_NOISE, -0.1, 0.3).withSalt(3)), SURFACE, only(BlockPredicate.allOf(ON_LAND, Ring4Placements.waterNear(1))),
                PlacementUtils.filteredByBlockSurvival(Blocks.SUGAR_CANE));
        register(context, LOTUS_SHALLOWS_BUSHES, AZALEA_BUSH, JitteredGridPlacement.of(8, 1, 0.3F, ClusterDensity.of(
                        Ring4Placements.GROVES_NOISE, -0.2, 0.1).withSalt(7).withFloor(0.2)), SURFACE, only(ON_GROUND),
                Ring4Filter.builder().ground().above(IslandOutput.Output.SURFACE, 64.0).build(),
                PlacementUtils.filteredByBlockSurvival(Blocks.AZALEA));
        register(context, LOTUS_SHALLOWS_BANK_FLOWERS, BANK_FLOWERS, JitteredGridPlacement.of(2, 0, 0.45F, ClusterDensity.of(
                PATCHES_NOISE, 0.0, 0.3).withSalt(5).withFloor(0.15)), SURFACE, only(ON_GROUND),
                Ring4Filter.builder().ground().build());
    }

    private static void groundPlant(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
                                    BlockStateProvider state) {
        FeatureUtils.register(context, key, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(state));
    }

    private static BlockState persistent(BlockState leaves) {
        return leaves.setValue(LeavesBlock.PERSISTENT, true);
    }
}
