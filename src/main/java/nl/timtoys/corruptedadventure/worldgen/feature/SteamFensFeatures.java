package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.List;
import java.util.Optional;

/**
 * The steam fens, the transition 3→4 (change 48, G2-WB12 = A): here the stolen water comes back and boils against the
 * heat. Boiling mud pools, geysers that launch whoever stands on them, fumaroles as steam columns, and small boiling
 * pools of water in the hollows above Y 63. The terrain stays the blend with the ring 4 placeholder until ring 4.
 * Amounts are starting values, tuned at test stop 4b.
 * <p>
 * In the transition zone the pure ring 3 terrain is only half of the blended surface, so these placements read the
 * frozen {@code WORLD_SURFACE_WG} heightmap and the blocks of their own chunk only (no {@link VolcanicFilter}).
 * Determinism as in {@link Ring3DressingFeatures}: {@link JitteredGridPlacement} margins keep everything in its chunk;
 * the pools' water is in its settled state ({@link SurfacePoolFeature}); a geyser starts its cycle when placed
 * ({@link GroundVentFeature}).
 */
public final class SteamFensFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> BOILING_POOL = configured("boiling_pool");
    static final ResourceKey<ConfiguredFeature<?, ?>> FENS_MUD_POOL = configured("fens_mud_pool");
    static final ResourceKey<ConfiguredFeature<?, ?>> GEYSER = configured("geyser");
    static final ResourceKey<ConfiguredFeature<?, ?>> STEAM_VENT = configured("steam_vent");

    // Placed, in the global order of their step (Ring3Features.ORDER); all LOCAL_MODIFICATIONS, the pools first.
    public static final ResourceKey<PlacedFeature> STEAM_FENS_BOILING_POOLS = placed("steam_fens_boiling_pools");
    public static final ResourceKey<PlacedFeature> STEAM_FENS_MUD_POOLS = placed("steam_fens_mud_pools");
    public static final ResourceKey<PlacedFeature> STEAM_FENS_GEYSERS = placed("steam_fens_geysers");
    public static final ResourceKey<PlacedFeature> STEAM_FENS_STEAM_VENTS = placed("steam_fens_steam_vents");

    /** Water stays above sea level: below it, ring3Seal fills the ring 3 half of the zone with rock (plan §1 48). */
    static final int POOLS_ABOVE = 64;
    /**
     * How far (blocks) a pool reaches from its origin. Every column within it stays on steam_fens' own ground
     * ({@link BeforeSplitFilter}), so no pool digs into the descent of verdant_falls past the split (its top terrace, rim
     * lakes, source pools). A mud pool: 3 · 1.4 + 1 (as its grid margin), cut off at its chunk.
     */
    static final int MUD_POOL_REACH = 6;
    /**
     * A boiling pool's radius ≤ 3.5 with its rim of 1 reaches 4.5 from its centre, but {@code PoolLayout} shifts the
     * centre up to 4 blocks per axis into the chunk (its grid margin is 1): up to 10 from the origin.
     */
    static final int BOILING_POOL_REACH = 10;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    /**
     * G2 ring 3 (decision 8 = A): pools, geysers and vents lie in clusters instead of an even grid. One noise for all of
     * them (same salt): boiling pools and geysers in the cores (about 3 in 10 of the fens), mud pools in a wider halo
     * around them, the vents with a floor everywhere. Mean amounts per chunk stay about those of 48.
     */
    static final ResourceKey<NormalNoise.NoiseParameters> CLUSTER_NOISE = ResourceKey.create(Registries.NOISE,
            CorruptedAdventure.id("ring_3_fens_clusters"));
    static final ClusterDensity CLUSTERS = ClusterDensity.of(CLUSTER_NOISE, 0.0, 0.35);
    static final ClusterDensity HALO = ClusterDensity.of(CLUSTER_NOISE, -0.25, 0.1);

    private SteamFensFeatures() {
    }

    /** The open ground of the steam fens (its surface, {@code tools/worldgen/ring3_surface.py}). */
    static HolderSet<Block> ground() {
        return HolderSet.direct(Block::builtInRegistryHolder, Blocks.MUD, Blocks.PACKED_MUD, Blocks.CLAY, Blocks.COARSE_DIRT, Blocks.GRAVEL,
                Blocks.DIRT, ModBlocks.ASH_BLOCK.get(), ModBlocks.SULFUR_CRUST.get());
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        BlockStateProvider crust = new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                .add(Blocks.PACKED_MUD.defaultBlockState(), 2)
                .add(ModBlocks.SULFUR_CRUST.get().defaultBlockState(), 1)
                .add(Blocks.CLAY.defaultBlockState(), 1));
        // A hot spring: water one deep in a closed hollow, a rim of packed mud and sulfur crust.
        FeatureUtils.register(context, BOILING_POOL, ModFeatures.SURFACE_POOL.get(), new SurfacePoolConfiguration(
                UniformFloat.of(1.5F, 3.5F), 0.35F, ConstantInt.of(1), ground(), 1,
                new SurfacePoolConfiguration.Rim(crust, 1), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty()));
        FeatureUtils.register(context, FENS_MUD_POOL, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                BlockStateProvider.simple(ModBlocks.BOILING_MUD.get()), ground(), UniformInt.of(1, 3), 1.4F, ConstantInt.of(1),
                ConstantInt.ZERO, BlockStateProvider.simple(ModBlocks.BOILING_MUD.get())));
        // The geyser starts its cycle as it is placed (GroundVentFeature).
        FeatureUtils.register(context, GEYSER, ModFeatures.GROUND_VENT.get(), new GroundVentConfiguration(
                ModBlocks.GEYSER.get().defaultBlockState(), crust, UniformInt.of(1, 2), 0.7F, ground(), List.of(), ConstantInt.ZERO));
        FeatureUtils.register(context, STEAM_VENT, ModFeatures.GROUND_VENT.get(), new GroundVentConfiguration(
                ModBlocks.FUMAROLE.get().defaultBlockState(), crust, UniformInt.of(0, 1), 0.6F, ground(), List.of(), ConstantInt.ZERO));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementModifier open = BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE);
        // Off the descent of verdant_falls: the BiomeFilter only sees the origin, these see the whole reach.
        PlacementModifier boilingOwnGround = BeforeSplitFilter.of(Ring.RING_3, IslandField.VERDANT_EDGE_SHARE, BOILING_POOL_REACH);
        PlacementModifier mudOwnGround = BeforeSplitFilter.of(Ring.RING_3, IslandField.VERDANT_EDGE_SHARE, MUD_POOL_REACH);
        PlacementModifier ventsOwnGround = BeforeSplitFilter.of(Ring.RING_3, IslandField.VERDANT_EDGE_SHARE, GroundVentFeature.MAX_COLLAR);

        // Cell 8 instead of 16: up to four per chunk in a cluster (PoolLayout still shifts each pool into its chunk).
        PlacementUtils.register(context, STEAM_FENS_BOILING_POOLS, features.getOrThrow(BOILING_POOL),
                JitteredGridPlacement.of(8, 1, 0.26F, CLUSTERS), SURFACE, HeightBandFilter.above(POOLS_ABOVE), open, BiomeFilter.biome(),
                boilingOwnGround);
        // A mud pool reaches 3 · 1.4 + 1 = 6: margin 6 keeps it whole.
        PlacementUtils.register(context, STEAM_FENS_MUD_POOLS, features.getOrThrow(FENS_MUD_POOL),
                JitteredGridPlacement.of(16, 6, 1.0F, HALO), SURFACE, SlopeFilter.flat(2, 1), open, BiomeFilter.biome(), mudOwnGround);
        PlacementUtils.register(context, STEAM_FENS_GEYSERS, features.getOrThrow(GEYSER),
                JitteredGridPlacement.of(8, GroundVentFeature.MAX_COLLAR, 0.26F, CLUSTERS), SURFACE, SlopeFilter.flat(2, 1), open,
                BiomeFilter.biome(), ventsOwnGround);
        PlacementUtils.register(context, STEAM_FENS_STEAM_VENTS, features.getOrThrow(STEAM_VENT),
                JitteredGridPlacement.of(8, 1, 0.5F, CLUSTERS.withFloor(0.3)), SURFACE, SlopeFilter.flat(1, 1), open, BiomeFilter.biome(),
                ventsOwnGround);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
