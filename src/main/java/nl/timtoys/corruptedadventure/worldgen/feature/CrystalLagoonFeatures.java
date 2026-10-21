package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4MythicFeatures.weighted;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.FLOOR;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_LAND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.PATCHES_NOISE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.UNDER_WATER;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.configuredKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.placedKey;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * {@code crystal_lagoon} (ring 4 decor rollout group A, 2026-10-19; sparring 2026-10-19): clear, light and crystalline.
 * <ul>
 *   <li>Sand banks just awash and small sandy atolls raised out of the shallow water ({@link ShoalFeature}), with
 *   aquamarine buds and lagoon glowgrass on them and seagrass and (waterlogged) glowgrass on their slopes;</li>
 *   <li>aquamarine clusters and buds on the floor, with glowgrass on the banks, tidestone rocks under the water and on the shores
 *   ({@link BoulderFeature}, also {@code submerged});</li>
 *   <li>coral reefs and seagrass meadows in patches ({@link Ring4Placements#PATCHES_NOISE}) besides the even coral and
 *   seagrass of package 75;</li>
 *   <li>the eye-catcher: groups of glowing crystal spires, 8–20 out of the water, aquamarine with glowing aquamarine
 *   stone and calcite in the shell ({@link CrystalSpireFeature}).</li>
 * </ul>
 * The lagoon has no trees, so its objects only keep clear of the trees of the neighbouring biomes (biome margins).
 * Amounts are starting values for the tuning loop.
 */
public final class CrystalLagoonFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> SHOAL = configuredKey("lagoon_shoal");
    static final ResourceKey<ConfiguredFeature<?, ?>> SPIRES = configuredKey("lagoon_crystal_spires");
    static final ResourceKey<ConfiguredFeature<?, ?>> BANK_ROCK = configuredKey("lagoon_bank_rock");
    static final ResourceKey<ConfiguredFeature<?, ?>> SEABED_ROCK = configuredKey("lagoon_seabed_rock");
    static final ResourceKey<ConfiguredFeature<?, ?>> SEABED_CLUSTER = configuredKey("lagoon_seabed_cluster");
    static final ResourceKey<ConfiguredFeature<?, ?>> SEABED_BUD = configuredKey("lagoon_seabed_bud");
    static final ResourceKey<ConfiguredFeature<?, ?>> BANK_CLUSTER = configuredKey("lagoon_bank_cluster");

    // Placed, in the global order (Ring4Features.ORDER): the banks and the spires before anything grows in the water.
    public static final ResourceKey<PlacedFeature> CRYSTAL_LAGOON_SHOALS = placedKey("crystal_lagoon_shoals");
    public static final ResourceKey<PlacedFeature> CRYSTAL_LAGOON_SPIRES = placedKey("crystal_lagoon_spires");
    public static final ResourceKey<PlacedFeature> CRYSTAL_LAGOON_BANK_ROCKS = placedKey("crystal_lagoon_bank_rocks");
    public static final ResourceKey<PlacedFeature> CRYSTAL_LAGOON_SEABED_ROCKS = placedKey("crystal_lagoon_seabed_rocks");
    public static final ResourceKey<PlacedFeature> CRYSTAL_LAGOON_REEFS = placedKey("crystal_lagoon_reefs");
    public static final ResourceKey<PlacedFeature> CRYSTAL_LAGOON_SEAGRASS_MEADOWS = placedKey("crystal_lagoon_seagrass_meadows");
    public static final ResourceKey<PlacedFeature> CRYSTAL_LAGOON_SEABED_CLUSTERS = placedKey("crystal_lagoon_seabed_clusters");
    public static final ResourceKey<PlacedFeature> CRYSTAL_LAGOON_SEABED_BUDS = placedKey("crystal_lagoon_seabed_buds");
    public static final ResourceKey<PlacedFeature> CRYSTAL_LAGOON_BANK_CLUSTERS = placedKey("crystal_lagoon_bank_clusters");

    private CrystalLagoonFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // 77b (golf 3): the banks and atolls of the white lagoon sand, like the surface around them.
        BlockState sand = ModBlocks.LAGOON_SAND.get().defaultBlockState();
        // Banks of ~15–50 blocks: the grove noise at 2.5 times its frequency, its own field (salt 5). Strength 1 lifts the
        // top one block over the water, the edges sink to six under it (only where the floor lies lower).
        FeatureUtils.register(context, SHOAL, ModFeatures.SHOAL.get(), new ShoalConfiguration(ModBiomes.CRYSTAL_LAGOON, 10,
                new ClusterDensity(Ring4Placements.GROVES_NOISE, 2.5, 5, 0.15, 0.55, 0.0), 8, 6, 1,
                BlockStateProvider.simple(sand), BlockStateProvider.simple(sand), BlockStateProvider.simple(sand),
                weighted(bud(ModBlocks.LARGE_AQUAMARINE_BUD.get().defaultBlockState(), false), 2,
                        bud(ModBlocks.AQUAMARINE_CLUSTER.get().defaultBlockState(), false), 1,
                        bud(ModBlocks.MEDIUM_AQUAMARINE_BUD.get().defaultBlockState(), false), 2,
                        ModBlocks.LAGOON_GLOWGRASS.get().defaultBlockState(), 3), 0.04F,
                BlockStateProvider.simple(sand), 0.0F, 0.0F,
                weighted(Blocks.SEAGRASS.defaultBlockState(), 6, bud(ModBlocks.AQUAMARINE_CLUSTER.get().defaultBlockState(), true), 1,
                        ModBlocks.LAGOON_GLOWGRASS.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 2), 0.3F));
        // The eye-catcher: one group per cell of 192 at most (about one in every 200 × 200 blocks of open lagoon).
        FeatureUtils.register(context, SPIRES, ModFeatures.CRYSTAL_SPIRE.get(), new CrystalSpireConfiguration(
                new Landmarks.Grid(192, 0.8F, 8, 24), ModBiomes.CRYSTAL_LAGOON, UniformInt.of(10, 20), UniformFloat.of(2.2F, 3.6F),
                UniformInt.of(1, 3), ModBlocks.AQUAMARINE_BLOCK.get().defaultBlockState(),
                // White calcite veins in the crystal; no budding block (that stays in the geodes under deep water).
                weighted(ModBlocks.AQUAMARINE_BLOCK.get().defaultBlockState(), 6, ModBlocks.AQUAMARINE_STONE.get().defaultBlockState(), 3,
                        Blocks.CALCITE.defaultBlockState(), 1),
                weighted(ModBlocks.TIDESTONE.get().defaultBlockState(), 5, ModBlocks.COBBLED_TIDESTONE.get().defaultBlockState(), 2,
                        ModBlocks.POLISHED_TIDESTONE.get().defaultBlockState(), 1),
                Blocks.SEA_LANTERN.defaultBlockState(), 0.07F,
                weighted(ModBlocks.AQUAMARINE_CLUSTER.get().defaultBlockState(), 3, ModBlocks.LARGE_AQUAMARINE_BUD.get().defaultBlockState(), 2,
                        ModBlocks.MEDIUM_AQUAMARINE_BUD.get().defaultBlockState(), 1), 0.3F));
        // Tidestone rocks: low ones on the shores, rounder ones on the floor; a crystal in them now and then.
        BlockStateProvider stone = weighted(ModBlocks.TIDESTONE.get().defaultBlockState(), 6, ModBlocks.COBBLED_TIDESTONE.get().defaultBlockState(), 3,
                ModBlocks.AQUAMARINE_BLOCK.get().defaultBlockState(), 1);
        FeatureUtils.register(context, BANK_ROCK, ModFeatures.BOULDER.get(), new BoulderConfiguration(stone,
                BlockStateProvider.simple(ModBlocks.TIDESTONE.get()), 0.0F, Blocks.MOSS_CARPET.defaultBlockState(), 0.0F,
                UniformFloat.of(1.0F, 2.2F), UniformFloat.of(1.0F, 2.5F)));
        FeatureUtils.register(context, SEABED_ROCK, ModFeatures.BOULDER.get(), new BoulderConfiguration(stone,
                BlockStateProvider.simple(ModBlocks.TIDESTONE.get()), 0.0F, Blocks.MOSS_CARPET.defaultBlockState(), 0.0F,
                UniformFloat.of(1.2F, 3.0F), UniformFloat.of(1.5F, 3.5F), true));
        // Crystals: one block each, one feature per kind (no weighted pick behind a world-dependent filter).
        FeatureUtils.register(context, SEABED_CLUSTER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
                BlockStateProvider.simple(bud(ModBlocks.AQUAMARINE_CLUSTER.get().defaultBlockState(), true))));
        FeatureUtils.register(context, SEABED_BUD, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
                BlockStateProvider.simple(bud(ModBlocks.LARGE_AQUAMARINE_BUD.get().defaultBlockState(), true))));
        FeatureUtils.register(context, BANK_CLUSTER, ModFeatures.GROUND_PLANT.get(), new SimpleBlockConfiguration(
                weighted(bud(ModBlocks.AQUAMARINE_CLUSTER.get().defaultBlockState(), false), 1,
                        bud(ModBlocks.LARGE_AQUAMARINE_BUD.get().defaultBlockState(), false), 2,
                        ModBlocks.LAGOON_GLOWGRASS.get().defaultBlockState(), 3)));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        Ring4Placements.perChunk(context, CRYSTAL_LAGOON_SHOALS, SHOAL);
        Ring4Placements.perChunk(context, CRYSTAL_LAGOON_SPIRES, SPIRES);
        // Rocks: a few on the shores, more on the floor (margins as large as the rocks' reach).
        register(context, CRYSTAL_LAGOON_BANK_ROCKS, BANK_ROCK, JitteredGridPlacement.of(16, 3, 0.25F), SURFACE,
                Ring4Filter.builder().ground().build(), only(Ring4Placements.waterNear(3)));
        register(context, CRYSTAL_LAGOON_SEABED_ROCKS, SEABED_ROCK, JitteredGridPlacement.of(16, 3, 0.45F,
                ClusterDensity.of(PATCHES_NOISE, -0.3, 0.2).withSalt(6).withFloor(0.3)), SURFACE);
        // Reefs and meadows in patches; the even coral and seagrass of package 75 stay between them.
        register(context, CRYSTAL_LAGOON_REEFS, Ring4MythicFeatures.CORAL, JitteredGridPlacement.of(2, 0, 0.35F,
                ClusterDensity.of(PATCHES_NOISE, 0.25, 0.55).withSalt(3)), FLOOR, only(Ring4Placements.waterAbove(3)));
        register(context, CRYSTAL_LAGOON_SEAGRASS_MEADOWS, Ring4MythicFeatures.SEAGRASS, JitteredGridPlacement.of(1, 0, 0.7F,
                ClusterDensity.of(PATCHES_NOISE, 0.05, 0.35).withSalt(4)), FLOOR, only(UNDER_WATER));
        // Aquamarine on the floor (scattered, more in patches) and on the banks.
        register(context, CRYSTAL_LAGOON_SEABED_CLUSTERS, SEABED_CLUSTER, JitteredGridPlacement.of(4, 0, 0.2F,
                ClusterDensity.of(PATCHES_NOISE, -0.2, 0.3).withSalt(7).withFloor(0.25)), FLOOR, only(UNDER_WATER));
        register(context, CRYSTAL_LAGOON_SEABED_BUDS, SEABED_BUD, JitteredGridPlacement.of(4, 0, 0.2F,
                ClusterDensity.of(PATCHES_NOISE, -0.2, 0.3).withSalt(7).withFloor(0.25)), FLOOR, only(UNDER_WATER));
        register(context, CRYSTAL_LAGOON_BANK_CLUSTERS, BANK_CLUSTER, JitteredGridPlacement.of(4, 0, 0.18F), SURFACE,
                only(BlockPredicate.allOf(ON_LAND, Ring4Placements.waterNear(3))), Ring4Filter.builder().ground().build());
    }

    /** An aquamarine bud or cluster facing up, waterlogged or not. */
    private static BlockState bud(BlockState state, boolean waterlogged) {
        return state.setValue(AmethystClusterBlock.WATERLOGGED, waterlogged);
    }
}
