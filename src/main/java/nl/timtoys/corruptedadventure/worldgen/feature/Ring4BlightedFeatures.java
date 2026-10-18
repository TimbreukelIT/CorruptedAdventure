package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.List;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.FLOOR;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_LAND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * The dressing of the blighted band and {@code withered_gardens} (package 76; contract ring 4 §2–§3, golf 2 Q10, Q12,
 * G1): the dead {@code withered_moonpetal} trees standing in the drowned forest's dark water and on the withered
 * islands, reeds higher than the player (dense in {@code reed_marsh}, sparse elsewhere), blighted glowcaps, dead
 * bushes; and the hazards: small patches of deadly sucking mud on the banks of {@code peat_moors} and
 * {@code drowned_forest} (≤ 4 × 4, 2–3 deep; never in the reeds of {@code reed_marsh}), closed toxic pools, closed
 * gas bubbles under the peat crust and one or two layers of gas on the floor of the peat sinkholes. The hazards and
 * the blighted glowcaps grow denser towards ring 5 ({@link RingChanceFilter} on ring 4's progress).
 * <p>
 * The hazards shape the ground ({@code LOCAL_MODIFICATIONS}) before anything grows on it: reeds and plants never stand
 * on mud or at a pool's rim by chance of order. Determinism as in {@link Ring4MythicFeatures}: own-chunk features on
 * the frozen worldgen heightmaps; only the dead trees' forks may reach a block or two into a neighbour.
 */
public final class Ring4BlightedFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> WITHERED_TREE = configuredKey("withered_moonpetal_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> WITHERED_TREE_FORKED = configuredKey("withered_moonpetal_tree_forked");
    static final ResourceKey<ConfiguredFeature<?, ?>> WITHERED_TREES = configuredKey("withered_moonpetal_trees");
    static final ResourceKey<ConfiguredFeature<?, ?>> DENSE_REEDS = configuredKey("dense_reeds");
    static final ResourceKey<ConfiguredFeature<?, ?>> SPARSE_REEDS = configuredKey("sparse_reeds");
    static final ResourceKey<ConfiguredFeature<?, ?>> BLIGHTED_GLOWCAP = configuredKey("blighted_glowcap");
    static final ResourceKey<ConfiguredFeature<?, ?>> DEAD_BUSH = configuredKey("dead_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> SUCKING_MUD = configuredKey("sucking_mud");
    static final ResourceKey<ConfiguredFeature<?, ?>> TOXIC_POOL = configuredKey("toxic_pool");
    static final ResourceKey<ConfiguredFeature<?, ?>> GAS_POCKET = configuredKey("swamp_gas_pocket");
    static final ResourceKey<ConfiguredFeature<?, ?>> SINKHOLE_GAS = configuredKey("sinkhole_gas");

    // Placed, in the global order of their step (Ring4Features.ORDER).
    /** LOCAL_MODIFICATIONS: the hazards, before anything grows. */
    public static final ResourceKey<PlacedFeature> PEAT_MOORS_GAS_POCKETS = placedKey("peat_moors_gas_pockets");
    public static final ResourceKey<PlacedFeature> PEAT_MOORS_SINKHOLE_GAS = placedKey("peat_moors_sinkhole_gas");
    public static final ResourceKey<PlacedFeature> PEAT_MOORS_TOXIC_POOLS = placedKey("peat_moors_toxic_pools");
    public static final ResourceKey<PlacedFeature> BLIGHTED_TOXIC_POOLS = placedKey("blighted_toxic_pools");
    public static final ResourceKey<PlacedFeature> BANK_SUCKING_MUD = placedKey("bank_sucking_mud");
    /** VEGETAL_DECORATION. */
    public static final ResourceKey<PlacedFeature> DROWNED_FOREST_TREES = placedKey("drowned_forest_trees");
    public static final ResourceKey<PlacedFeature> WITHERED_GARDENS_TREES = placedKey("withered_gardens_trees");
    public static final ResourceKey<PlacedFeature> REED_MARSH_REEDS = placedKey("reed_marsh_reeds");
    public static final ResourceKey<PlacedFeature> BLIGHTED_BANK_REEDS = placedKey("blighted_bank_reeds");
    public static final ResourceKey<PlacedFeature> REED_MARSH_GLOWCAPS = placedKey("reed_marsh_blighted_glowcaps");
    public static final ResourceKey<PlacedFeature> BLIGHTED_GLOWCAPS = placedKey("blighted_glowcaps");
    public static final ResourceKey<PlacedFeature> BLIGHTED_DEAD_BUSHES = placedKey("blighted_dead_bushes");

    /** The deepest water a dead tree stands in (most trunks reach out of it; a short one stays a sunken trunk). */
    static final int DROWNED_WATER = 10;
    /** The hazards and the blighted glowcaps grow denser towards ring 5: this chance at ring 4's inner edge, 1 at its outer. */
    static final double INNER_CHANCE = 0.35;

    private Ring4BlightedFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        // Dead, bare and grey: only the trunk (withered moonpetal has no leaves), tall enough to stand out of deep water.
        FeatureUtils.register(context, WITHERED_TREE, ModFeatures.ISOLATED_TREE.get(), bareTree(new StraightTrunkPlacer(9, 4, 2)));
        FeatureUtils.register(context, WITHERED_TREE_FORKED, ModFeatures.ISOLATED_TREE.get(), bareTree(new ForkingTrunkPlacer(8, 3, 3)));
        FeatureUtils.register(context, WITHERED_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(WITHERED_TREE_FORKED)), 0.6F)),
                PlacementUtils.inlinePlaced(features.getOrThrow(WITHERED_TREE))));
        FeatureUtils.register(context, DENSE_REEDS, ModFeatures.REED_CLUMP.get(), new ReedClumpConfiguration(3, 0.6F, 2, 3));
        FeatureUtils.register(context, SPARSE_REEDS, ModFeatures.REED_CLUMP.get(), new ReedClumpConfiguration(2, 0.35F, 2, 3));
        FeatureUtils.register(context, BLIGHTED_GLOWCAP, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.BLIGHTED_GLOWCAP.get())));
        FeatureUtils.register(context, DEAD_BUSH, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(
                Ring4MythicFeatures.weighted(Blocks.DEAD_BUSH.defaultBlockState(), 3, Blocks.SHORT_GRASS.defaultBlockState(), 1)));
        FeatureUtils.register(context, SUCKING_MUD, ModFeatures.SUCKING_MUD_PATCH.get(), new SuckingMudPatchConfiguration(
                UniformInt.of(2, SuckingMudPatchFeature.MAX_WIDTH), UniformInt.of(2, 3),
                HolderSet.direct(Block::builtInRegistryHolder, Blocks.MUD, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL,
                        Blocks.GRASS_BLOCK, Blocks.CLAY, Blocks.ROOTED_DIRT, ModBlocks.PEAT.get()),
                HolderSet.direct(context.lookup(Registries.BIOME).getOrThrow(ModBiomes.PEAT_MOORS),
                        context.lookup(Registries.BIOME).getOrThrow(ModBiomes.DROWNED_FOREST))));
        FeatureUtils.register(context, TOXIC_POOL, ModFeatures.TOXIC_POOL.get(), new ToxicPoolConfiguration(
                ModBlocks.TOXIC_POOL.get().defaultBlockState(), UniformInt.of(1, 3), UniformInt.of(1, 2)));
        FeatureUtils.register(context, GAS_POCKET, ModFeatures.GAS_POCKET.get(), new GasPocketConfiguration(
                ModBlocks.SWAMP_GAS.get().defaultBlockState(), UniformInt.of(2, 4), UniformInt.of(1, 2), UniformInt.of(1, 2)));
        FeatureUtils.register(context, SINKHOLE_GAS, ModFeatures.SINKHOLE_GAS.get(), new SinkholeGasConfiguration(
                ModBlocks.SWAMP_GAS.get().defaultBlockState(), 4, 9));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        // LOCAL_MODIFICATIONS: the hazards, on the terrain's own ground (not on islands).
        register(context, PEAT_MOORS_GAS_POCKETS, GAS_POCKET, JitteredGridPlacement.of(8, 1, 0.35F), SURFACE, only(ON_LAND),
                Ring4Filter.builder().ground().build(), outward());
        Ring4Placements.perChunk(context, PEAT_MOORS_SINKHOLE_GAS, SINKHOLE_GAS);
        register(context, PEAT_MOORS_TOXIC_POOLS, TOXIC_POOL, JitteredGridPlacement.of(16, 4, 0.45F), SURFACE, only(ON_LAND),
                Ring4Filter.builder().ground().build(), outward());
        register(context, BLIGHTED_TOXIC_POOLS, TOXIC_POOL, JitteredGridPlacement.of(16, 4, 0.15F), SURFACE, only(ON_LAND),
                Ring4Filter.builder().ground().build(), outward());
        register(context, BANK_SUCKING_MUD, SUCKING_MUD, JitteredGridPlacement.of(16, 2, 0.4F), SURFACE,
                only(BlockPredicate.allOf(ON_LAND, Ring4Placements.waterNear(2))), Ring4Filter.builder().ground().build(), outward());

        // VEGETAL_DECORATION.
        register(context, DROWNED_FOREST_TREES, WITHERED_TREES, JitteredGridPlacement.of(8, 1, 0.45F), FLOOR,
                SurfaceWaterDepthFilter.forMaxDepth(DROWNED_WATER), only(BlockPredicate.matchesTag(Ring4Placements.up(-1), BlockTags.DIRT)));
        register(context, WITHERED_GARDENS_TREES, WITHERED_TREES, JitteredGridPlacement.of(16, 2, 0.5F), SURFACE,
                only(BlockPredicate.allOf(Ring4Placements.ON_GROUND, BlockPredicate.matchesTag(Ring4Placements.up(-1), BlockTags.DIRT))));
        register(context, REED_MARSH_REEDS, DENSE_REEDS, JitteredGridPlacement.of(8, 3, 0.85F), SURFACE);
        register(context, BLIGHTED_BANK_REEDS, SPARSE_REEDS, JitteredGridPlacement.of(16, 3, 0.4F), SURFACE,
                only(Ring4Placements.waterNear(3)));
        register(context, REED_MARSH_GLOWCAPS, BLIGHTED_GLOWCAP, JitteredGridPlacement.of(4, 0, 0.12F), SURFACE, only(ON_LAND), outward());
        register(context, BLIGHTED_GLOWCAPS, BLIGHTED_GLOWCAP, JitteredGridPlacement.of(4, 0, 0.05F), SURFACE, only(ON_LAND), outward());
        register(context, BLIGHTED_DEAD_BUSHES, DEAD_BUSH, JitteredGridPlacement.of(4, 0, 0.12F), SURFACE, only(Ring4Placements.ON_GROUND));
    }

    /** Denser towards ring 5: {@link #INNER_CHANCE} at ring 4's inner edge, rising linearly to 1 at its outer one. */
    private static PlacementModifier outward() {
        return RingChanceFilter.progress(Ring.RING_4, INNER_CHANCE, 1.0, 0.0, 1.0);
    }

    /**
     * A dead tree: the withered trunk only, on mud. Its "foliage" is one more block of the trunk above each trunk end
     * (not air, as the withered expanse's bare trees: under water an air leaf would leave a bubble against the water).
     */
    private static TreeConfiguration bareTree(TrunkPlacer trunk) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.WITHERED_MOONPETAL_LOG.get()),
                trunk,
                BlockStateProvider.simple(ModBlocks.WITHERED_MOONPETAL_LOG.get()),
                new BlobFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0), 0),
                new TwoLayersFeatureSize(1, 0, 1))
                .dirt(BlockStateProvider.simple(Blocks.MUD))
                .ignoreVines()
                .build();
    }
}
