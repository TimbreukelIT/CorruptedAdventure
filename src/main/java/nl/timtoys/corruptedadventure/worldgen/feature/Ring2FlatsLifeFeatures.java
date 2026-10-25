package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DRY_GRASS;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.DUNE_VALLEY_FLORA;
import static nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures.OASIS_TRANSITION_VEGETATION;

/**
 * Datagen bootstraps for G2 ring 2 on the flats (decisions G2-R2-2, 3 and 7, 2026-10-25; {@code docs/plans/ring-2/g2-flats.md}):
 * <ul>
 *   <li>{@code dune_sea}: thickets of valley flora in patches, dead trees half buried in the flanks (the red dunes are the
 *   surface rule, {@code tools/worldgen/ring2_flats_surface.py});</li>
 *   <li>{@code salt_flats}: pressure ridges on the cracks of the crust in patches ({@link SaltRidgeFeature}), bleached dead
 *   trees on the islands (the crystal clusters are {@code salt_flats_crystals} in {@link Ring2Features});</li>
 *   <li>{@code oasis_flats}: scrub and dry grass in patches on the open sand, dry pool hollows of cracked clay (the palm
 *   groves are {@code oasis_palms_sand} in {@link Ring2Features} with the grove noise, their grass floor the surface rule).</li>
 * </ul>
 * Same determinism rules as {@link Ring2Features}: {@link JitteredGridPlacement} cells, groups from seeded noise, every
 * feature in its own chunk (the dead trees are {@link DeadTreeFeature} ground objects). Amounts are starting values.
 */
public final class Ring2FlatsLifeFeatures {
    // Noises (worldgen/noise), shared with the surface rules where named there.
    static final ResourceKey<NormalNoise.NoiseParameters> RED_DUNES = noise("ring_2_red_dunes");
    static final ResourceKey<NormalNoise.NoiseParameters> DUNE_THICKETS = noise("ring_2_dune_thickets");
    static final ResourceKey<NormalNoise.NoiseParameters> SALT_RIDGES = noise("ring_2_salt_ridges");
    static final ResourceKey<NormalNoise.NoiseParameters> SALT_CRYSTAL_CLUSTERS = noise("ring_2_salt_crystal_clusters");
    static final ResourceKey<NormalNoise.NoiseParameters> OASIS_GROVES = noise("ring_2_oasis_groves");
    static final ResourceKey<NormalNoise.NoiseParameters> SCRUB_PATCHES = noise("ring_2_oasis_scrub");

    /** Red dunes where {@code ring_2_red_dunes} is at least this: ~25 % of the dunes (surface rule). */
    static final double RED_DUNES_FROM = 0.2;
    /** The top block also red from here where {@code minecraft:surface} is at least 0.1: a speckled fringe, no sharp edge. */
    static final double RED_FRINGE_FROM = 0.12;
    /** Thickets: no extra flora below the low end, every cell from the high end (~35 % of the valleys get some). */
    static final double THICKETS_LOW = 0.05;
    static final double THICKETS_HIGH = 0.3;
    /** Pressure ridges where {@code ring_2_salt_ridges} is at least this: ~15 % of the flats. */
    static final double RIDGES_FROM = 0.35;
    /** Crystal clusters: the single crystals only in these blobs, about as many as before (~1 per chunk). */
    static final double CRYSTALS_LOW = 0.2;
    static final double CRYSTALS_HIGH = 0.5;
    /** Palm groves on the open sand: grass from {@link #GROVE_GRASS_FROM} (surface rule), palms ramp up to the core. */
    static final double GROVE_GRASS_FROM = 0.33;
    static final double GROVE_PALMS_LOW = 0.3;
    static final double GROVE_PALMS_HIGH = 0.45;
    /** Scrub patches on the open sand (coarse dirt specks in the surface rule from the same value). */
    static final double SCRUB_LOW = 0.1;
    static final double SCRUB_HIGH = 0.35;
    /** Dune flanks for the buried trees, top block: above the valleys, below most crests. */
    static final int FLANK_BOTTOM = 70;
    static final int FLANK_TOP = 88;

    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> BURIED_DEAD_TREE = configuredKey("buried_dead_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> BLEACHED_DEAD_TREE = configuredKey("bleached_dead_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> SALT_RIDGE = configuredKey("salt_ridge");
    static final ResourceKey<ConfiguredFeature<?, ?>> DRY_HOLLOW = configuredKey("dry_hollow");

    // Placed, in the order of their biome's lists (Ring2Biomes).
    /** {@code salt_flats}, {@code LOCAL_MODIFICATIONS}: after the island fossils and bones. */
    public static final ResourceKey<PlacedFeature> SALT_FLATS_RIDGES = placedKey("salt_flats_ridges");
    /** {@code oasis_flats}, {@code LOCAL_MODIFICATIONS}: after the oasis rocks. */
    public static final ResourceKey<PlacedFeature> OASIS_DRY_HOLLOWS = placedKey("oasis_dry_hollows");
    /** {@code dune_sea}, {@code VEGETAL_DECORATION}: after the cactus groups. */
    public static final ResourceKey<PlacedFeature> DUNE_SEA_THICKETS = placedKey("dune_sea_thickets");
    public static final ResourceKey<PlacedFeature> DUNE_SEA_BURIED_TREES = placedKey("dune_sea_buried_trees");
    /** {@code salt_flats}, {@code VEGETAL_DECORATION}: after the island cacti. */
    public static final ResourceKey<PlacedFeature> SALT_ISLAND_BLEACHED_TREES = placedKey("salt_island_bleached_trees");
    /** {@code oasis_flats}, {@code VEGETAL_DECORATION}: after the transition vegetation. */
    public static final ResourceKey<PlacedFeature> OASIS_SCRUB = placedKey("oasis_scrub");
    public static final ResourceKey<PlacedFeature> OASIS_SCRUB_GRASS = placedKey("oasis_scrub_grass");

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private Ring2FlatsLifeFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // A dead tree sunk into the dune up to its crown: a trunk of 2–3 (dead_tree needs 2) with 2–4 bare limbs low down.
        FeatureUtils.register(context, BURIED_DEAD_TREE, ModFeatures.DEAD_TREE.get(), new DeadTreeConfiguration(
                ModBlocks.DEAD_LOG.get().defaultBlockState(), ModBlocks.DEAD_WOOD.get().defaultBlockState(),
                UniformInt.of(2, 3), UniformInt.of(0, 1), UniformInt.of(2, 4), UniformInt.of(2, 4), 0, 0.0F, ConstantInt.of(1),
                BlockStateProvider.simple(Blocks.AIR), 0.0F));
        // Bleached by the salt: stripped dead wood, 3–6 high, a little crooked, 2–4 limbs.
        FeatureUtils.register(context, BLEACHED_DEAD_TREE, ModFeatures.DEAD_TREE.get(), new DeadTreeConfiguration(
                ModBlocks.STRIPPED_DEAD_LOG.get().defaultBlockState(), ModBlocks.STRIPPED_DEAD_WOOD.get().defaultBlockState(),
                UniformInt.of(3, 6), UniformInt.of(0, 2), UniformInt.of(2, 4), UniformInt.of(2, 4), 0, 0.0F, ConstantInt.of(1),
                BlockStateProvider.simple(Blocks.AIR), 0.0F));
        // Pressure ridges: one salt block on every crack of the flat crust (Y 65), a break now and then, junctions
        // sometimes two high.
        BlockState salt = ModBlocks.SALT_BLOCK.get().defaultBlockState();
        FeatureUtils.register(context, SALT_RIDGE, ModFeatures.SALT_RIDGE.get(), new SaltRidgeConfiguration(salt, salt, 65,
                SALT_RIDGES, RIDGES_FROM, 0.08F, 0.35F));
        // A dry pool hollow: the oasis pool's ragged shape, one deep, a floor of cracked clay, a rim of coarse dirt with
        // dry shrubs and dead bushes, now and then a dead bush on the floor.
        FeatureUtils.register(context, DRY_HOLLOW, ModFeatures.SURFACE_POOL.get(), new SurfacePoolConfiguration(
                UniformFloat.of(2.5F, 5.0F), 0.35F, ConstantInt.of(1),
                HolderSet.direct(Block::builtInRegistryHolder, Blocks.SAND, Blocks.COARSE_DIRT), 1,
                new SurfacePoolConfiguration.Rim(weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.COARSE_DIRT.defaultBlockState(), 2)
                        .add(Blocks.SAND.defaultBlockState(), 1)), 1),
                Optional.of(new SurfacePoolConfiguration.Scatter(weighted(SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.DRY_SHRUB.get().defaultBlockState(), 3)
                        .add(Blocks.DEAD_BUSH.defaultBlockState(), 2)), 0.2F)),
                Optional.empty(), Optional.empty(),
                Optional.of(new SurfacePoolConfiguration.Scatter(BlockStateProvider.simple(Blocks.DEAD_BUSH), 0.05F)),
                Optional.empty(),
                Optional.of(BlockStateProvider.simple(ModBlocks.CRACKED_CLAY.get()))));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        BlockPredicate onDuneSand = BlockPredicate.matchesBlocks(below(), Blocks.SAND, Blocks.RED_SAND);
        BlockPredicate onIsland = BlockPredicate.matchesBlocks(below(), Blocks.SAND, Blocks.RED_SAND);

        // Salt flats: one ridge pass per chunk (the feature walks its own columns). No biome filter on the pass's one
        // point (review B #2: it cut ridge patches off on a chunk line at a biome edge); cracked salt block at Y <= 65
        // with air above only comes from the salt_flats surface rule, so the feature finds its columns itself.
        PlacementUtils.register(context, SALT_FLATS_RIDGES, features.getOrThrow(SALT_RIDGE),
                JitteredGridPlacement.of(16, 0, 1.0F),
                SURFACE);
        // Oasis: about one dry hollow per six chunks on the open sand, clear of the pools' transition ring.
        PlacementUtils.register(context, OASIS_DRY_HOLLOWS, features.getOrThrow(DRY_HOLLOW),
                JitteredGridPlacement.of(16, 1, 0.2F),
                NoiseBandFilter.between(Ring2Features.POOLS, -Double.MAX_VALUE, Ring2Features.SAND_BELOW),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(below(), Blocks.SAND)),
                BiomeFilter.biome());

        // Dune sea: thickets of the valley flora in patches, up the lowest flank (the crests stay bare).
        PlacementUtils.register(context, DUNE_SEA_THICKETS, features.getOrThrow(DUNE_VALLEY_FLORA),
                JitteredGridPlacement.of(4, 0, 0.6F, ClusterDensity.of(DUNE_THICKETS, THICKETS_LOW, THICKETS_HIGH)),
                SURFACE,
                HeightBandFilter.below(Ring2DressingFeatures.CACTUS_TOP + 1),
                PlacementUtils.isEmpty(),
                BiomeFilter.biome());
        // About one buried dead tree per four dune chunks, on the flanks; never in a fossil (its ribs arch over air, a
        // trunk there would stand on nothing).
        PlacementUtils.register(context, DUNE_SEA_BURIED_TREES, features.getOrThrow(BURIED_DEAD_TREE),
                JitteredGridPlacement.of(16, 2, 0.3F),
                SURFACE,
                HeightBandFilter.between(FLANK_BOTTOM, FLANK_TOP + 1),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, onDuneSand, noBones(2))),
                BiomeFilter.biome());

        // Salt islands: a bleached tree on about every second island (an island of ~35 blocks holds ~20 cells of 8).
        PlacementUtils.register(context, SALT_ISLAND_BLEACHED_TREES, features.getOrThrow(BLEACHED_DEAD_TREE),
                JitteredGridPlacement.of(8, 2, 0.04F),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, onIsland, noBones(2))),
                BiomeFilter.biome());

        // Oasis: scrub in patches on the open sand (the transition ring's plants), dry grass on the coarse dirt specks.
        PlacementUtils.register(context, OASIS_SCRUB, features.getOrThrow(OASIS_TRANSITION_VEGETATION),
                JitteredGridPlacement.of(4, 0, 0.5F, ClusterDensity.of(SCRUB_PATCHES, SCRUB_LOW, SCRUB_HIGH)),
                NoiseBandFilter.between(Ring2Features.POOLS, -Double.MAX_VALUE, Ring2Features.TRANSITION),
                SURFACE,
                PlacementUtils.isEmpty(),
                BiomeFilter.biome());
        PlacementUtils.register(context, OASIS_SCRUB_GRASS, features.getOrThrow(DRY_GRASS),
                JitteredGridPlacement.of(4, 0, 0.6F),
                NoiseBandFilter.between(Ring2Features.POOLS, -Double.MAX_VALUE, Ring2Features.TRANSITION),
                SURFACE,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(below(), Blocks.COARSE_DIRT))),
                BiomeFilter.biome());
    }

    /** No bone block within {@code reach} of the origin, at its height and one below (a fossil's ribs). */
    private static BlockPredicate noBones(int reach) {
        List<BlockPredicate> bones = new ArrayList<>();
        for (int dy = -1; dy <= 0; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    bones.add(BlockPredicate.matchesBlocks(new Vec3i(dx, dy, dz), Blocks.BONE_BLOCK));
                }
            }
        }
        return BlockPredicate.not(BlockPredicate.anyOf(bones));
    }

    private static BlockStateProvider weighted(SimpleWeightedRandomList.Builder<BlockState> states) {
        return new WeightedStateProvider(states);
    }

    private static Vec3i below() {
        return Direction.DOWN.getNormal();
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noise(String path) {
        return ResourceKey.create(Registries.NOISE, CorruptedAdventure.id(path));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id(path));
    }

    private static ResourceKey<PlacedFeature> placedKey(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id(path));
    }
}
