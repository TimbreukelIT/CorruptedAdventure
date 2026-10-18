package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.block.VerdantVineBlock;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandOutput;

import java.util.Optional;

import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_GROUND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_LAND;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.ON_WATER;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.SURFACE;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.only;
import static nl.timtoys.corruptedadventure.worldgen.feature.Ring4Placements.register;

/**
 * The dressing of {@code verdant_falls} ({@code docs/plans/ring-4/verdant-falls.md} §4, golf 2 §6b, package 75b): the
 * own tropical flora, lush but open (F4 = A).
 * <ul>
 *   <li>The climbing routes (Q3 = B + ledges): strands of the climbable {@code verdant_vine} (partly flowering, F6 = C)
 *   under the overhanging ledges down to the next ledge, always under a route's knobs; a hanging orchid as a single
 *   accent under a ledge (also under the ledge path). The vine's tip is full grown (age 25), so a strand stays as long
 *   as placed; never in water.</li>
 *   <li>Trees (F2 = A, F3 = B): the emergent tree (14–22, buttress roots, umbrella crown, fruit pods and orchids on its
 *   trunk, vines and hanging orchids under its branches) on the banks of the still lakes and on the gorge rims, never
 *   over a fall; tree ferns (4–7, also the sapling's {@code ring_4/verdant_tree_fern}) on the treads and on the risers.
 *   Both keep clear of the falls and the climbing knobs.</li>
 *   <li>Plants (F5 = C): ferns everywhere, tall ferns, giant leaves and bromeliads on the treads, giant leaves and
 *   blossom bushes along the banks and basin rims (plants never take the calcite lips: they need soil), orchids and
 *   wall bromeliads on the gorge walls.</li>
 *   <li>Water (F7 = A, H2 = A): a few lotuses and water blooms on the still lakes only; brooks, falls, splash pools
 *   and the foot river stay free.</li>
 *   <li>Moss carpets on the treads' moss patches (the placeholder dressing).</li>
 * </ul>
 * Everything stands on the terrain's own ground ({@link Ring4Filter#onGround}: never on a bridge deck or a rock arch).
 * Determinism as in ring 2 (23c): placement in the own chunk on the {@code WORLD_SURFACE_WG} heightmap and the pure
 * terrain ({@link Ring4Filter}); only the trees' crowns reach into the neighbours (the 3 × 3 window). The strands come
 * first and the wall plants only take air, so no plant ever shortens a climbing route. Amounts are starting values,
 * tuned at test stop 5b.
 */
public final class VerdantFallsFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_CARPET = configured("verdant_moss_carpet");
    static final ResourceKey<ConfiguredFeature<?, ?>> LEDGE_STRANDS = configured("verdant_ledge_strands");
    static final ResourceKey<ConfiguredFeature<?, ?>> EMERGENT_TREE = configured("verdant_emergent_tree");
    static final ResourceKey<ConfiguredFeature<?, ?>> FERN = configured("verdant_fern");
    static final ResourceKey<ConfiguredFeature<?, ?>> TALL_FERN = configured("verdant_tall_fern");
    static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_LEAF = configured("verdant_giant_leaf");
    static final ResourceKey<ConfiguredFeature<?, ?>> BROMELIAD = configured("verdant_bromeliad");
    static final ResourceKey<ConfiguredFeature<?, ?>> BLOSSOM_BUSH = configured("verdant_blossom_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> WALL_PLANTS = configured("verdant_wall_plants");
    static final ResourceKey<ConfiguredFeature<?, ?>> LOTUS = configured("verdant_lotus");
    static final ResourceKey<ConfiguredFeature<?, ?>> WATER_BLOOM = configured("verdant_water_bloom");

    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_MOSS_CARPETS = placed("verdant_falls_moss_carpets");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_LEDGE_STRANDS = placed("verdant_falls_ledge_strands");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_BANK_TREES = placed("verdant_falls_bank_trees");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_RIM_TREES = placed("verdant_falls_rim_trees");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_TREAD_TREE_FERNS = placed("verdant_falls_tread_tree_ferns");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_RISER_TREE_FERNS = placed("verdant_falls_riser_tree_ferns");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_WALL_PLANTS = placed("verdant_falls_wall_plants");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_FERNS = placed("verdant_falls_ferns");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_TALL_FERNS = placed("verdant_falls_tall_ferns");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_GIANT_LEAVES = placed("verdant_falls_giant_leaves");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_BANK_GIANT_LEAVES = placed("verdant_falls_bank_giant_leaves");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_BROMELIADS = placed("verdant_falls_bromeliads");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_BLOSSOM_BUSHES = placed("verdant_falls_blossom_bushes");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_LOTUS = placed("verdant_falls_lotus");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_WATER_BLOOMS = placed("verdant_falls_water_blooms");

    /** Columns per chunk that look for ledges (about a quarter of the chunk). */
    static final int STRAND_COLUMNS = 64;
    /** The share of strands that flower (F6 = C, "deels bloeiend"). */
    static final float FLOWERING_SHARE = 0.35F;
    /** The still lakes lie on the treads (64 + 8k, water from Y 64 up); the foot river's water stands at the sea level. */
    static final int ABOVE_THE_FOOT_RIVER = 65;
    /**
     * Trees keep this far from the falls (their columns and lips) and from the climbing knobs: the umbrella's reach (6)
     * and one more, so no leaf takes a fall's water or a knob's strand.
     */
    static final int TREE_CLEARANCE = 7;
    /** Tree ferns and wall plants keep this far from the falls (F3 = B: never within 2 of a fall). */
    static final int FERN_CLEARANCE = 3;

    private VerdantFallsFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, MOSS_CARPET, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MOSS_CARPET)));
        // F6 = C: the verdant vine (tip full grown) instead of the hanging moss; the hanging orchid as the accent.
        FeatureUtils.register(context, LEDGE_STRANDS, ModFeatures.LEDGE_STRAND.get(), new LedgeStrandConfiguration(
                ModBlocks.VERDANT_VINE_PLANT.get().defaultBlockState(), ModBlocks.VERDANT_HANGING_ORCHID.get().defaultBlockState(),
                0.5F, 0.2F, 3, 24, 4,
                Optional.of(vineTip(false)),
                Optional.of(new LedgeStrandConfiguration.Variant(
                        ModBlocks.VERDANT_VINE_PLANT.get().defaultBlockState().setValue(VerdantVineBlock.FLOWERING, true), vineTip(true))),
                FLOWERING_SHARE));
        FeatureUtils.register(context, EMERGENT_TREE, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.VERDANT_LOG.get()),
                new VerdantTrunkPlacer(14, 4, 4, UniformInt.of(2, 4), ConstantInt.of(8), 0.06F, 0.05F),
                BlockStateProvider.simple(ModBlocks.VERDANT_LEAVES.get()),
                new VerdantFoliagePlacer(UniformInt.of(5, 6), ConstantInt.of(0), VerdantFoliagePlacer.Shape.UMBRELLA),
                new TwoLayersFeatureSize(1, 0, 1)).ignoreVines().build());
        // The tree fern: also what the sapling grows (F11 = A).
        FeatureUtils.register(context, ModConfiguredFeatures.VERDANT_TREE_FERN, ModFeatures.ISOLATED_TREE.get(), new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.VERDANT_LOG.get()),
                new VerdantTrunkPlacer(4, 2, 1, ConstantInt.of(0), ConstantInt.of(0), 0.0F, 0.0F),
                BlockStateProvider.simple(ModBlocks.VERDANT_LEAVES.get()),
                new VerdantFoliagePlacer(UniformInt.of(2, 3), ConstantInt.of(0), VerdantFoliagePlacer.Shape.FRONDS),
                new TwoLayersFeatureSize(1, 0, 1)).ignoreVines().build());
        simple(context, FERN, BlockStateProvider.simple(ModBlocks.VERDANT_FERN.get()));
        simple(context, TALL_FERN, BlockStateProvider.simple(ModBlocks.VERDANT_TALL_FERN.get()));
        simple(context, GIANT_LEAF, BlockStateProvider.simple(ModBlocks.VERDANT_GIANT_LEAF.get()));
        simple(context, BROMELIAD, BlockStateProvider.simple(ModBlocks.VERDANT_BROMELIAD.get()));
        simple(context, BLOSSOM_BUSH, BlockStateProvider.simple(ModBlocks.VERDANT_BLOSSOM_BUSH.get()));
        FeatureUtils.register(context, WALL_PLANTS, ModFeatures.WALL_PLANT.get(), new WallPlantConfiguration(
                Ring4MythicFeatures.weighted(ModBlocks.VERDANT_ORCHID.get().defaultBlockState(), 2,
                        ModBlocks.VERDANT_WALL_BROMELIAD.get().defaultBlockState(), 1), 0.1F, 28));
        simple(context, LOTUS, BlockStateProvider.simple(ModBlocks.LOTUS.get()));
        simple(context, WATER_BLOOM, BlockStateProvider.simple(ModBlocks.VERDANT_WATER_BLOOM.get()));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(context, VERDANT_FALLS_MOSS_CARPETS, features.getOrThrow(MOSS_CARPET),
                CountPlacement.of(6), InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(BlockPos.ZERO.below(), Blocks.MOSS_BLOCK))),
                BiomeFilter.biome());
        PlacementUtils.register(context, VERDANT_FALLS_LEDGE_STRANDS, features.getOrThrow(LEDGE_STRANDS),
                CountPlacement.of(STRAND_COLUMNS), InSquarePlacement.spread(), HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG),
                BiomeFilter.biome());

        PlacementModifier sapling = PlacementUtils.filteredByBlockSurvival(ModBlocks.VERDANT_SAPLING.get());
        PlacementModifier treeClear = Ring4Filter.builder().ground().below(IslandOutput.Output.CLIFF, 0.5)
                .clearOfFalls(TREE_CLEARANCE).clearOfKnobs(TREE_CLEARANCE).build();
        // Emergent trees on the banks of the still lakes (not the foot river) and on the gorge rims.
        register(context, VERDANT_FALLS_BANK_TREES, EMERGENT_TREE, JitteredGridPlacement.of(16, 4, 0.45F), SURFACE,
                HeightBandFilter.above(ABOVE_THE_FOOT_RIVER), only(BlockPredicate.allOf(ON_GROUND, Ring4Placements.waterNear(4))),
                sapling, treeClear);
        register(context, VERDANT_FALLS_RIM_TREES, EMERGENT_TREE, JitteredGridPlacement.of(16, 3, 0.35F), SURFACE,
                only(ON_GROUND), SlopeFilter.rimTop(6, 12), sapling, treeClear);
        // Tree ferns on the treads and on the risers (F3 = B).
        PlacementModifier fernClear = Ring4Filter.builder().ground().clearOfFalls(FERN_CLEARANCE).clearOfKnobs(FERN_CLEARANCE).build();
        register(context, VERDANT_FALLS_TREAD_TREE_FERNS, ModConfiguredFeatures.VERDANT_TREE_FERN, JitteredGridPlacement.of(8, 2, 0.35F),
                SURFACE, only(ON_GROUND), Ring4Filter.builder().below(IslandOutput.Output.SLOPE, 0.4).build(), sapling, fernClear);
        register(context, VERDANT_FALLS_RISER_TREE_FERNS, ModConfiguredFeatures.VERDANT_TREE_FERN, JitteredGridPlacement.of(8, 2, 0.25F),
                SURFACE, only(ON_GROUND), Ring4Filter.builder().range(IslandOutput.Output.SLOPE, 0.4, 1.5).build(), sapling, fernClear);
        // The gorge walls: from the foot of a wall up.
        register(context, VERDANT_FALLS_WALL_PLANTS, WALL_PLANTS, JitteredGridPlacement.of(4, 0, 0.5F), SURFACE,
                SlopeFilter.wallFoot(2, 6), Ring4Filter.builder().ground().clearOfFalls(FERN_CLEARANCE).build());

        PlacementModifier ground = Ring4Filter.builder().ground().build();
        register(context, VERDANT_FALLS_FERNS, FERN, JitteredGridPlacement.of(2, 0, 0.3F), SURFACE, only(ON_LAND), ground);
        register(context, VERDANT_FALLS_TALL_FERNS, TALL_FERN, JitteredGridPlacement.of(4, 0, 0.25F), SURFACE, only(ON_LAND), ground);
        register(context, VERDANT_FALLS_GIANT_LEAVES, GIANT_LEAF, JitteredGridPlacement.of(4, 0, 0.12F), SURFACE, only(ON_LAND), ground);
        register(context, VERDANT_FALLS_BANK_GIANT_LEAVES, GIANT_LEAF, JitteredGridPlacement.of(4, 0, 0.3F), SURFACE,
                only(BlockPredicate.allOf(ON_LAND, Ring4Placements.waterNear(2))), ground);
        register(context, VERDANT_FALLS_BROMELIADS, BROMELIAD, JitteredGridPlacement.of(4, 0, 0.12F), SURFACE, only(ON_LAND), ground);
        register(context, VERDANT_FALLS_BLOSSOM_BUSHES, BLOSSOM_BUSH, JitteredGridPlacement.of(4, 0, 0.3F), SURFACE,
                only(BlockPredicate.allOf(ON_LAND, Ring4Placements.waterNear(3))), ground);

        // A few lotuses and water blooms on the still lakes: wide water around (not a brook), above the foot river, off the falls.
        PlacementModifier stillLake = only(BlockPredicate.allOf(ON_WATER, wideWater()));
        PlacementModifier offFalls = Ring4Filter.builder().clearOfFalls(TREE_CLEARANCE).build();
        register(context, VERDANT_FALLS_LOTUS, LOTUS, JitteredGridPlacement.of(4, 0, 0.1F), SURFACE,
                HeightBandFilter.above(ABOVE_THE_FOOT_RIVER), stillLake, offFalls);
        register(context, VERDANT_FALLS_WATER_BLOOMS, WATER_BLOOM, JitteredGridPlacement.of(4, 0, 0.12F), SURFACE,
                HeightBandFilter.above(ABOVE_THE_FOOT_RIVER), stillLake, offFalls);
    }

    /** Water two blocks away on all four sides: a lake, not a brook (2–3 wide). */
    private static BlockPredicate wideWater() {
        return BlockPredicate.allOf(
                BlockPredicate.matchesFluids(new Vec3i(2, -1, 0), Fluids.WATER), BlockPredicate.matchesFluids(new Vec3i(-2, -1, 0), Fluids.WATER),
                BlockPredicate.matchesFluids(new Vec3i(0, -1, 2), Fluids.WATER), BlockPredicate.matchesFluids(new Vec3i(0, -1, -2), Fluids.WATER));
    }

    /** The vine's tip, full grown so it never grows on (B's note for 75b). */
    private static net.minecraft.world.level.block.state.BlockState vineTip(boolean flowering) {
        return ModBlocks.VERDANT_VINE.get().defaultBlockState().setValue(VerdantVineBlock.AGE, VerdantVineBlock.MAX_AGE)
                .setValue(VerdantVineBlock.FLOWERING, flowering);
    }

    private static void simple(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
                               BlockStateProvider state) {
        FeatureUtils.register(context, key, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(state));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_4/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_4/" + name));
    }
}
