package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

/**
 * The sulfur fields' coloured boiling pools (change 47d, decision 4b-2): pools of bubbling mud in yellow, green or
 * orange, each in a basin of travertine (a raised calcite lip, terrace steps of yellow and orange terracotta and calcite
 * around it); sulfur crystals on the crusts, and (through the fumarole's collar, {@code Ring3DressingFeatures}) around
 * the vents. The muds and crystals are stream B's (47d). Amounts are starting values.
 * <p>
 * Determinism as in {@link Ring3DressingFeatures}: {@link JitteredGridPlacement} with a margin of the pool's reach, the
 * frozen {@code WORLD_SURFACE_WG} heightmap, the pure terrain and the placement random.
 */
public final class SulfurFieldsFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> TERRACE_POOL = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            CorruptedAdventure.id("ring_3/sulfur_terrace_pool"));
    /** LOCAL_MODIFICATIONS, first of the sulfur fields: the pools shape the ground the vents stand on. */
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_TERRACE_POOLS = ResourceKey.create(Registries.PLACED_FEATURE,
            CorruptedAdventure.id("ring_3/sulfur_fields_terrace_pools"));
    static final ResourceKey<ConfiguredFeature<?, ?>> CRUST_CRYSTAL = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            CorruptedAdventure.id("ring_3/sulfur_crust_crystal"));
    /** VEGETAL_DECORATION: crystals on the sulfur crust, in groups (a noise). */
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_CRUST_CRYSTALS = ResourceKey.create(Registries.PLACED_FEATURE,
            CorruptedAdventure.id("ring_3/sulfur_fields_crust_crystals"));

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private SulfurFieldsFeatures() {
    }

    /** What boils in the pools: yellow, green or orange sulfur mud, one colour per pool (drawn once per pool). */
    static BlockStateProvider poolFill() {
        return new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                .add(ModBlocks.YELLOW_SULFUR_MUD.get().defaultBlockState(), 3)
                .add(ModBlocks.GREEN_SULFUR_MUD.get().defaultBlockState(), 2)
                .add(ModBlocks.ORANGE_SULFUR_MUD.get().defaultBlockState(), 2));
    }

    /** Sulfur crystals standing up (their default facing): mostly buds, now and then a full needle. */
    static BlockStateProvider crystals() {
        return new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                .add(ModBlocks.SMALL_SULFUR_CRYSTAL_BUD.get().defaultBlockState(), 3)
                .add(ModBlocks.MEDIUM_SULFUR_CRYSTAL_BUD.get().defaultBlockState(), 3)
                .add(ModBlocks.LARGE_SULFUR_CRYSTAL_BUD.get().defaultBlockState(), 2)
                .add(ModBlocks.SULFUR_CRYSTAL.get().defaultBlockState(), 2));
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, CRUST_CRYSTAL, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(crystals()));
        FeatureUtils.register(context, TERRACE_POOL, ModFeatures.TERRACE_POOL.get(), new TerracePoolConfiguration(
                poolFill(),
                BlockStateProvider.simple(Blocks.CALCITE),
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.YELLOW_TERRACOTTA.defaultBlockState(), 3)
                        .add(Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 2)
                        .add(Blocks.CALCITE.defaultBlockState(), 2)
                        .add(ModBlocks.SULFUR_CRUST.get().defaultBlockState(), 1)),
                UniformInt.of(1, TerracePoolFeature.MAX_RADIUS),
                HolderSet.direct(Block::builtInRegistryHolder, ModBlocks.SULFUR_CRUST.get(), Blocks.CALCITE, Blocks.YELLOW_TERRACOTTA,
                        Blocks.SMOOTH_BASALT, ModBlocks.ASH_BLOCK.get(), Blocks.TUFF)));
    }

    private static void placedCrystals(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(context, SULFUR_FIELDS_CRUST_CRYSTALS, features.getOrThrow(CRUST_CRYSTAL),
                JitteredGridPlacement.of(2, 0, 0.3F), SURFACE, NoiseBandFilter.between(EmberWoodsFeatures.FLORA_PATCHES, 0.3, Double.MAX_VALUE),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(BlockPos.ZERO.below(), ModBlocks.SULFUR_CRUST.get()))),
                BiomeFilter.biome());
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        placedCrystals(context);
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        // About one pool per 2 chunks on level ground of the terraces.
        PlacementUtils.register(context, SULFUR_FIELDS_TERRACE_POOLS, features.getOrThrow(TERRACE_POOL),
                JitteredGridPlacement.of(16, TerracePoolFeature.MAX_REACH, 0.5F), SURFACE, SlopeFilter.flat(3, 1),
                VolcanicFilter.builder().ground().build(), BiomeFilter.biome());
    }
}
