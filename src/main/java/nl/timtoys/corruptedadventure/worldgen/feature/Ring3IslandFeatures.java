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
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;

/**
 * The floating islands and archipelagos (change 47c, G2-WB13 = A): smouldering ash with drifts of ash on their tops,
 * one to three ember trees on the large ones, and fire crystals hanging from their undersides. The islands lie over
 * every ring 3 biome, so these features are in every ring 3 biome's list and find the islands through the pure
 * terrain's {@code island_mask}. Amounts are starting values, tuned at test stop 4b.
 * <p>
 * Determinism as in {@link Ring3DressingFeatures}: {@link JitteredGridPlacement} margins keep every feature in its own
 * chunk; the islands are terrain (noise), so their blocks are there before any feature.
 */
public final class Ring3IslandFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> ISLAND_EMBERS = configured("island_embers");
    static final ResourceKey<ConfiguredFeature<?, ?>> HANGING_FIRE_CRYSTALS = configured("hanging_fire_crystals");

    // Placed, in the global order of their step (Ring3Features.ORDER).
    public static final ResourceKey<PlacedFeature> ISLAND_EMBER_PATCHES = placed("island_ember_patches");
    public static final ResourceKey<PlacedFeature> ISLAND_HANGING_CRYSTALS = placed("island_hanging_crystals");
    /** VEGETAL_DECORATION. */
    public static final ResourceKey<PlacedFeature> ISLAND_EMBER_TREES = placed("island_ember_trees");

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private Ring3IslandFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // The island top's ash turns to smouldering ash in patches, with a few layers of loose ash on the middle.
        SimpleWeightedRandomList.Builder<BlockState> layers = SimpleWeightedRandomList.builder();
        for (int n = 1; n <= 3; n++) {
            layers.add(ModBlocks.ASH_LAYER.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, n), 1);
        }
        FeatureUtils.register(context, ISLAND_EMBERS, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.SMOLDERING_ASH.get().defaultBlockState(), 2)
                        .add(ModBlocks.ASH_BLOCK.get().defaultBlockState(), 1)),
                HolderSet.direct(Block::builtInRegistryHolder, ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get(), Blocks.BASALT),
                UniformInt.of(2, 4), 1.3F, ConstantInt.of(1), ConstantInt.of(1), new WeightedStateProvider(layers)));
        FeatureUtils.register(context, HANGING_FIRE_CRYSTALS, ModFeatures.HANGING_CLUSTER.get(), new HangingClusterConfiguration(
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(ModBlocks.FIRE_CRYSTAL_CLUSTER.get().defaultBlockState(), 2)
                        .add(ModBlocks.LARGE_FIRE_CRYSTAL_BUD.get().defaultBlockState(), 2)
                        .add(ModBlocks.MEDIUM_FIRE_CRYSTAL_BUD.get().defaultBlockState(), 1)),
                2, 0.3F, 40, 4));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementModifier island = VolcanicFilter.builder().above(VolcanicOutput.Output.ISLAND_MASK, 0.5).build();

        // LOCAL_MODIFICATIONS (a patch reaches 4 · 1.3 + 1 = 7: margin 7 keeps it whole).
        PlacementUtils.register(context, ISLAND_EMBER_PATCHES, features.getOrThrow(ISLAND_EMBERS),
                JitteredGridPlacement.of(16, 7, 0.6F), SURFACE, island, BiomeFilter.biome());
        PlacementUtils.register(context, ISLAND_HANGING_CRYSTALS, features.getOrThrow(HANGING_FIRE_CRYSTALS),
                JitteredGridPlacement.of(8, HangingClusterFeature.MAX_RADIUS, 0.5F), SURFACE, island, BiomeFilter.biome());
        // VEGETAL_DECORATION: an ember tree on the level middle of a large island (the crown stays in the chunk).
        PlacementUtils.register(context, ISLAND_EMBER_TREES, features.getOrThrow(EmberWoodsFeatures.EMBER_TREE),
                JitteredGridPlacement.of(16, 3, 0.25F), SURFACE,
                VolcanicFilter.builder().above(VolcanicOutput.Output.ISLAND_MASK, 0.9).build(), SlopeFilter.flat(3, 1),
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(BlockPos.ZERO.below(), ModBlocks.ASH_BLOCK.get(), ModBlocks.SMOLDERING_ASH.get(),
                                ModBlocks.PUMICE.get()))),
                BiomeFilter.biome());
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
