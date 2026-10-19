package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.List;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4MythicFeatures.weighted;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.FLOOR;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.GROVES_NOISE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_GROUND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.PATCHES_NOISE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * The dressing of the blighted ground biomes (ring 4 decor B, 2026-10-19; group B of the decor rollout: "a light horror
 * edge", rich but decaying): {@code drowned_forest}, {@code peat_moors} and {@code reed_marsh}, on top of package 76
 * ({@link Ring4BlightedFeatures}: the withered trunks, reeds, hazards, glowcaps and dead bushes stay).
 * <ul>
 *   <li>Dense dead woods: crooked dead trees ({@link DeadTreeFeature}) in groves with hanging vines, standing in the
 *   drowned forest's water and in clumps in the marsh, lone snags on the moor; thick reed beds and thickets.</li>
 *   <li>Toxic puddles ({@link ToxicPuddleFeature}) in mud with dead bushes on the moor and in the marsh, denser towards
 *   ring 5 as the other hazards.</li>
 *   <li>Rot: fallen and rotting withered trunks with moss and fungi, stumps of withered (and stripped) moonpetal wood
 *   ({@link FallenTreeFeature}), rings of blighted glowcaps ({@link FungusRingFeature}), dry heath on the peat.</li>
 *   <li>The landmarks ({@link BlightedLandmarks}, {@link BlightedLandmarkFeature}): the drowned giant, the stone circle
 *   and the bone carcass, a few per 500 blocks of their biome; the other features keep off them
 *   ({@link LandmarkClearanceFilter}).</li>
 * </ul>
 * Every feature follows the order-independent rules of ring 4 (own random, frozen heightmaps, own chunk, air only for
 * plants; the landmarks pure per world cell). Amounts are starting values for the tuning loop.
 */
public final class BlightedDecorFeatures {
    // Configured: the landmarks.
    static final ResourceKey<ConfiguredFeature<?, ?>> DROWNED_GIANT = configuredKey("drowned_giant");
    static final ResourceKey<ConfiguredFeature<?, ?>> STONE_CIRCLE = configuredKey("stone_circle");
    static final ResourceKey<ConfiguredFeature<?, ?>> BONE_CARCASS = configuredKey("bone_carcass");
    // Configured: the dead trees.
    static final ResourceKey<ConfiguredFeature<?, ?>> DROWNED_DEAD_TREE = configuredKey("drowned_dead_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> DROWNED_DEAD_TREE_ROTTEN = configuredKey("drowned_dead_tree_rotten");
    static final ResourceKey<ConfiguredFeature<?, ?>> DROWNED_DEAD_TREES = configuredKey("drowned_dead_trees");
    static final ResourceKey<ConfiguredFeature<?, ?>> MARSH_DEAD_TREE = configuredKey("marsh_dead_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> MARSH_DEAD_TREE_ROTTEN = configuredKey("marsh_dead_tree_rotten");
    static final ResourceKey<ConfiguredFeature<?, ?>> MARSH_DEAD_TREES = configuredKey("marsh_dead_trees");
    static final ResourceKey<ConfiguredFeature<?, ?>> MOOR_SNAG = configuredKey("moor_snag");
    static final ResourceKey<ConfiguredFeature<?, ?>> MOOR_SNAG_ROTTEN = configuredKey("moor_snag_rotten");
    static final ResourceKey<ConfiguredFeature<?, ?>> MOOR_SNAGS = configuredKey("moor_snags");
    // Configured: rot, puddles, mushrooms, reeds, heath.
    static final ResourceKey<ConfiguredFeature<?, ?>> ROTTING_LOG = configuredKey("rotting_withered_log");
    static final ResourceKey<ConfiguredFeature<?, ?>> ROTTING_LOG_STRIPPED = configuredKey("rotting_stripped_withered_log");
    static final ResourceKey<ConfiguredFeature<?, ?>> ROTTING_LOGS = configuredKey("rotting_withered_logs");
    static final ResourceKey<ConfiguredFeature<?, ?>> WITHERED_STUMP = configuredKey("withered_stump");
    static final ResourceKey<ConfiguredFeature<?, ?>> WITHERED_STUMP_STRIPPED = configuredKey("stripped_withered_stump");
    static final ResourceKey<ConfiguredFeature<?, ?>> WITHERED_STUMPS = configuredKey("withered_stumps");
    static final ResourceKey<ConfiguredFeature<?, ?>> TOXIC_PUDDLE = configuredKey("toxic_puddle");
    static final ResourceKey<ConfiguredFeature<?, ?>> FUNGUS_RING = configuredKey("blighted_fungus_ring");
    static final ResourceKey<ConfiguredFeature<?, ?>> REED_THICKET = configuredKey("reed_thicket");
    static final ResourceKey<ConfiguredFeature<?, ?>> REED_BED = configuredKey("reed_bed");
    static final ResourceKey<ConfiguredFeature<?, ?>> HEATH = configuredKey("peat_heath");

    // Placed, in the global order (Ring4Features.ORDER). LOCAL_MODIFICATIONS: the puddles shape the ground.
    public static final ResourceKey<PlacedFeature> PEAT_MOORS_TOXIC_PUDDLES = placedKey("peat_moors_toxic_puddles");
    public static final ResourceKey<PlacedFeature> REED_MARSH_TOXIC_PUDDLES = placedKey("reed_marsh_toxic_puddles");
    // VEGETAL_DECORATION: the landmarks, the rot on the ground, the dead trees, the reeds, then the small plants.
    public static final ResourceKey<PlacedFeature> DROWNED_FOREST_GIANTS = placedKey("drowned_forest_giants");
    public static final ResourceKey<PlacedFeature> PEAT_MOORS_STONE_CIRCLES = placedKey("peat_moors_stone_circles");
    public static final ResourceKey<PlacedFeature> BLIGHTED_CARCASSES = placedKey("blighted_carcasses");
    public static final ResourceKey<PlacedFeature> BLIGHTED_WITHERED_STUMPS = placedKey("blighted_withered_stumps");
    public static final ResourceKey<PlacedFeature> DROWNED_FOREST_ROTTING_LOGS = placedKey("drowned_forest_rotting_logs");
    public static final ResourceKey<PlacedFeature> PEAT_MOORS_ROTTING_LOGS = placedKey("peat_moors_rotting_logs");
    public static final ResourceKey<PlacedFeature> REED_MARSH_ROTTING_LOGS = placedKey("reed_marsh_rotting_logs");
    public static final ResourceKey<PlacedFeature> DROWNED_FOREST_DEAD_TREES = placedKey("drowned_forest_dead_trees");
    public static final ResourceKey<PlacedFeature> REED_MARSH_DEAD_TREES = placedKey("reed_marsh_dead_trees");
    public static final ResourceKey<PlacedFeature> PEAT_MOORS_SNAGS = placedKey("peat_moors_snags");
    public static final ResourceKey<PlacedFeature> DROWNED_FOREST_REED_BEDS = placedKey("drowned_forest_reed_beds");
    public static final ResourceKey<PlacedFeature> REED_MARSH_REED_THICKETS = placedKey("reed_marsh_reed_thickets");
    public static final ResourceKey<PlacedFeature> BLIGHTED_FUNGUS_RINGS = placedKey("blighted_fungus_rings");
    public static final ResourceKey<PlacedFeature> PEAT_MOORS_HEATH = placedKey("peat_moors_heath");

    // How far each feature reaches beyond its placed origin (fit, lean, branches, trunk length): its clearance from the
    // landmarks, so nothing of it lands inside one.
    private static final int PUDDLE_CLEARANCE = 2 * ToxicPuddleFeature.REACH;
    private static final int LOG_CLEARANCE = FallenTreeFeature.MAX_LENGTH + 2;
    private static final int TREE_CLEARANCE = DeadTreeFeature.MAX_LEAN + DeadTreeFeature.MAX_BRANCH + 1;
    private static final int RING_CLEARANCE = 2 * FungusRingFeature.MAX_RADIUS;
    private static final int REED_CLEARANCE = 3;

    private BlightedDecorFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        BlockState sludge = ModBlocks.TOXIC_POOL.get().defaultBlockState();
        landmark(context, DROWNED_GIANT, BlightedLandmarks.Kind.DROWNED_GIANT, sludge);
        landmark(context, STONE_CIRCLE, BlightedLandmarks.Kind.MENHIR_CIRCLE, sludge);
        landmark(context, BONE_CARCASS, BlightedLandmarks.Kind.BONE_CARCASS, sludge);

        BlockState log = ModBlocks.WITHERED_MOONPETAL_LOG.get().defaultBlockState();
        BlockState wood = ModBlocks.WITHERED_MOONPETAL_WOOD.get().defaultBlockState();
        BlockState strippedLog = ModBlocks.STRIPPED_WITHERED_MOONPETAL_LOG.get().defaultBlockState();
        BlockState strippedWood = ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD.get().defaultBlockState();
        BlockState glowcap = ModBlocks.BLIGHTED_GLOWCAP.get().defaultBlockState();
        BlockStateProvider growth = weighted(Blocks.MOSS_CARPET.defaultBlockState(), 3, glowcap, 1);
        // The drowned forest: tall enough to stand out of water up to 8 deep, leaning, many vines.
        deadTree(context, features, DROWNED_DEAD_TREE, DROWNED_DEAD_TREE_ROTTEN, DROWNED_DEAD_TREES, strippedLog, strippedWood,
                new DeadTreeConfiguration(log, wood, UniformInt.of(6, 12), UniformInt.of(1, 3), UniformInt.of(2, 5), UniformInt.of(2, 5), 8,
                        0.25F, UniformInt.of(3, 8), growth, 0.25F));
        // The marsh: in clumps, crooked, draped in vines over the reeds.
        deadTree(context, features, MARSH_DEAD_TREE, MARSH_DEAD_TREE_ROTTEN, MARSH_DEAD_TREES, strippedLog, strippedWood,
                new DeadTreeConfiguration(log, wood, UniformInt.of(6, 11), UniformInt.of(1, 3), UniformInt.of(2, 5), UniformInt.of(2, 5), 2,
                        0.3F, UniformInt.of(2, 7), growth, 0.25F));
        // The moor: low, lone snags with a branch or two.
        deadTree(context, features, MOOR_SNAG, MOOR_SNAG_ROTTEN, MOOR_SNAGS, strippedLog, strippedWood,
                new DeadTreeConfiguration(log, wood, UniformInt.of(4, 8), UniformInt.of(0, 2), UniformInt.of(1, 3), UniformInt.of(1, 4), 1,
                        0.12F, UniformInt.of(1, 4), growth, 0.3F));

        // Rot: fallen trunks (some peeled and pale) with moss and fungi, stumps of withered wood.
        // No vanilla mushrooms: their survival reads the light, which depends on the neighbours' generation (det check decor B).
        BlockStateProvider rot = weighted(Blocks.MOSS_CARPET.defaultBlockState(), 3, glowcap, 2);
        FeatureUtils.register(context, ROTTING_LOG, ModFeatures.FALLEN_TREE.get(), new FallenTreeConfiguration(log, UniformInt.of(1, 2),
                UniformInt.of(3, 6), 0.15F, 0.5F, rot, 0.5F));
        FeatureUtils.register(context, ROTTING_LOG_STRIPPED, ModFeatures.FALLEN_TREE.get(), new FallenTreeConfiguration(strippedLog,
                UniformInt.of(1, 2), UniformInt.of(3, 6), 0.15F, 0.6F, rot, 0.55F));
        selector(context, features, ROTTING_LOGS, ROTTING_LOG_STRIPPED, 0.35F, ROTTING_LOG);
        FeatureUtils.register(context, WITHERED_STUMP, ModFeatures.FALLEN_TREE.get(), new FallenTreeConfiguration(wood, UniformInt.of(1, 3),
                ConstantInt.of(3), 1.0F, 0.0F, rot, 0.4F));
        FeatureUtils.register(context, WITHERED_STUMP_STRIPPED, ModFeatures.FALLEN_TREE.get(), new FallenTreeConfiguration(strippedWood,
                UniformInt.of(1, 2), ConstantInt.of(3), 1.0F, 0.0F, rot, 0.4F));
        selector(context, features, WITHERED_STUMPS, WITHERED_STUMP_STRIPPED, 0.35F, WITHERED_STUMP);

        FeatureUtils.register(context, TOXIC_PUDDLE, ModFeatures.TOXIC_PUDDLE.get(), new ToxicPuddleFeature.Config(sludge, UniformInt.of(1, 5),
                weighted(Blocks.MUD.defaultBlockState(), 5, Blocks.COARSE_DIRT.defaultBlockState(), 1),
                weighted(Blocks.DEAD_BUSH.defaultBlockState(), 4, glowcap, 1), 0.35F));
        FeatureUtils.register(context, FUNGUS_RING, ModFeatures.FUNGUS_RING.get(), new FungusRingFeature.Config(
                BlockStateProvider.simple(glowcap), UniformInt.of(2, 4), 0.7F));
        FeatureUtils.register(context, REED_THICKET, ModFeatures.REED_CLUMP.get(), new ReedClumpConfiguration(3, 0.85F, 2, 3));
        FeatureUtils.register(context, REED_BED, ModFeatures.REED_CLUMP.get(), new ReedClumpConfiguration(2, 0.7F, 2, 3));
        FeatureUtils.register(context, HEATH, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(
                weighted(Blocks.DEAD_BUSH.defaultBlockState(), 3, Blocks.SHORT_GRASS.defaultBlockState(), 3)));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        PlacementModifier ground = Ring4Filter.builder().ground().build();
        // LOCAL_MODIFICATIONS: the puddles, after the hazards of 76, denser towards ring 5.
        register(context, PEAT_MOORS_TOXIC_PUDDLES, TOXIC_PUDDLE, JitteredGridPlacement.of(16, 4, 0.55F), SURFACE, ground,
                outward(), LandmarkClearanceFilter.of(PUDDLE_CLEARANCE));
        register(context, REED_MARSH_TOXIC_PUDDLES, TOXIC_PUDDLE, JitteredGridPlacement.of(16, 4, 0.4F), SURFACE, ground,
                outward(), LandmarkClearanceFilter.of(PUDDLE_CLEARANCE));

        // VEGETAL_DECORATION. The landmarks once per chunk (they find themselves), before everything that grows.
        Ring4Placements.perChunk(context, DROWNED_FOREST_GIANTS, DROWNED_GIANT);
        Ring4Placements.perChunk(context, PEAT_MOORS_STONE_CIRCLES, STONE_CIRCLE);
        Ring4Placements.perChunk(context, BLIGHTED_CARCASSES, BONE_CARCASS);
        // Rot on the ground before the trees (ground objects: no tree roots in a stump).
        register(context, BLIGHTED_WITHERED_STUMPS, WITHERED_STUMPS, JitteredGridPlacement.of(16, 1, 0.25F,
                ClusterDensity.of(GROVES_NOISE, -0.3, 0.0).withFloor(0.3)), SURFACE, ground, LandmarkClearanceFilter.of(LOG_CLEARANCE));
        register(context, DROWNED_FOREST_ROTTING_LOGS, ROTTING_LOGS, JitteredGridPlacement.of(16, 1, 0.6F,
                ClusterDensity.of(GROVES_NOISE, -0.25, 0.05).withFloor(0.25)), SURFACE, ground, LandmarkClearanceFilter.of(LOG_CLEARANCE));
        register(context, PEAT_MOORS_ROTTING_LOGS, ROTTING_LOGS, JitteredGridPlacement.of(16, 1, 0.45F), SURFACE, ground,
                LandmarkClearanceFilter.of(LOG_CLEARANCE));
        register(context, REED_MARSH_ROTTING_LOGS, ROTTING_LOGS, JitteredGridPlacement.of(16, 1, 0.3F), SURFACE, ground,
                LandmarkClearanceFilter.of(LOG_CLEARANCE));
        // The dead woods: dense groves in the drowned forest (with its withered trunks: the same grove noise), clumps
        // in the marsh, lone snags on the moor.
        register(context, DROWNED_FOREST_DEAD_TREES, DROWNED_DEAD_TREES, JitteredGridPlacement.of(4, 1, 0.3F,
                ClusterDensity.of(GROVES_NOISE, -0.05, 0.3)), FLOOR, LandmarkClearanceFilter.of(TREE_CLEARANCE));
        register(context, REED_MARSH_DEAD_TREES, MARSH_DEAD_TREES, JitteredGridPlacement.of(4, 1, 0.45F,
                ClusterDensity.of(GROVES_NOISE, 0.15, 0.4)), FLOOR, LandmarkClearanceFilter.of(TREE_CLEARANCE));
        register(context, PEAT_MOORS_SNAGS, MOOR_SNAGS, JitteredGridPlacement.of(16, 2, 0.25F,
                ClusterDensity.of(GROVES_NOISE, -0.1, 0.2).withFloor(0.25)), FLOOR, LandmarkClearanceFilter.of(TREE_CLEARANCE));
        // Reeds that close the view: beds in the drowned forest's shallows, thickets in the marsh (in patches).
        register(context, DROWNED_FOREST_REED_BEDS, REED_BED, JitteredGridPlacement.of(8, 2, 0.5F, ClusterDensity.of(PATCHES_NOISE,
                0.0, 0.3)), SURFACE, LandmarkClearanceFilter.of(REED_CLEARANCE));
        register(context, REED_MARSH_REED_THICKETS, REED_THICKET, JitteredGridPlacement.of(8, 3, 0.8F, ClusterDensity.of(PATCHES_NOISE,
                -0.15, 0.2).withSalt(1)), SURFACE, LandmarkClearanceFilter.of(REED_CLEARANCE));
        // Fungus rings on dry ground; the dry heath of the moor in patches.
        register(context, BLIGHTED_FUNGUS_RINGS, FUNGUS_RING, JitteredGridPlacement.of(16, 0, 0.3F), SURFACE, ground,
                LandmarkClearanceFilter.of(RING_CLEARANCE));
        register(context, PEAT_MOORS_HEATH, HEATH, JitteredGridPlacement.of(2, 0, 0.35F, ClusterDensity.of(PATCHES_NOISE, 0.0, 0.35)
                .withSalt(3)), SURFACE, only(ON_GROUND), LandmarkClearanceFilter.of(0));
    }

    private static void landmark(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
                                 BlightedLandmarks.Kind kind, BlockState sludge) {
        FeatureUtils.register(context, key, ModFeatures.BLIGHTED_LANDMARK.get(), new BlightedLandmarkFeature.Config(kind, sludge));
    }

    /** A dead tree of withered moonpetal, its rotten (stripped, pale) twin, and a pick of the two (a quarter rotten). */
    private static void deadTree(BootstrapContext<ConfiguredFeature<?, ?>> context, HolderGetter<ConfiguredFeature<?, ?>> features,
                                 ResourceKey<ConfiguredFeature<?, ?>> key, ResourceKey<ConfiguredFeature<?, ?>> rottenKey,
                                 ResourceKey<ConfiguredFeature<?, ?>> pickKey, BlockState strippedLog, BlockState strippedWood,
                                 DeadTreeConfiguration config) {
        FeatureUtils.register(context, key, ModFeatures.DEAD_TREE.get(), config);
        FeatureUtils.register(context, rottenKey, ModFeatures.DEAD_TREE.get(), new DeadTreeConfiguration(strippedLog, strippedWood,
                config.height(), config.lean(), config.branches(), config.branchLength(), config.maxWater(), config.vineChance(),
                config.vineLength(), config.decor(), config.decorChance()));
        selector(context, features, pickKey, rottenKey, 0.25F, key);
    }

    private static void selector(BootstrapContext<ConfiguredFeature<?, ?>> context, HolderGetter<ConfiguredFeature<?, ?>> features,
                                 ResourceKey<ConfiguredFeature<?, ?>> key, ResourceKey<ConfiguredFeature<?, ?>> rare, float chance,
                                 ResourceKey<ConfiguredFeature<?, ?>> otherwise) {
        FeatureUtils.register(context, key, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
                List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(rare)), chance)),
                PlacementUtils.inlinePlaced(features.getOrThrow(otherwise))));
    }

    /** Denser towards ring 5, as the hazards of package 76. */
    private static PlacementModifier outward() {
        return Ring4BlightedFeatures.outward();
    }
}
