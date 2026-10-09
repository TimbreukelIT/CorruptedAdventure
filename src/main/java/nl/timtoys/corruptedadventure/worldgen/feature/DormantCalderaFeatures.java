package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;

/**
 * The dormant caldera, the oasis of ring 3 (change 47c, V8, G2-WB5 = A): ferns and azalea bushes on the green floor
 * (the surface already lays moss and coarse dirt), glow lichen along the crater wall, now and then a living azalea tree
 * as a beacon, and ember berry bushes at the foot of the wall. Amounts are starting values, tuned at test stop 4b.
 * <p>
 * Determinism as in {@link Ring3DressingFeatures}: {@link JitteredGridPlacement} with a margin of at least the reach,
 * so everything stays in its own chunk; the floor is the pure terrain's {@code caldera_dormant}.
 */
public final class DormantCalderaFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> FERN = configured("caldera_fern");
    static final ResourceKey<ConfiguredFeature<?, ?>> AZALEA_BUSH = configured("caldera_azalea_bush");
    static final ResourceKey<ConfiguredFeature<?, ?>> GLOW_LICHEN = configured("caldera_glow_lichen");

    // Placed, in the global order of their step (Ring3Features.ORDER); all VEGETAL_DECORATION.
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_AZALEA_TREES = placed("dormant_caldera_azalea_trees");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_AZALEA_BUSHES = placed("dormant_caldera_azalea_bushes");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_FERNS = placed("dormant_caldera_ferns");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_BERRY_BUSHES = placed("dormant_caldera_berry_bushes");
    public static final ResourceKey<PlacedFeature> DORMANT_CALDERA_GLOW_LICHEN = placed("dormant_caldera_glow_lichen");

    /** Vanilla's azalea tree reaches 5 from its trunk (a bend of 2, a crown of 3). */
    private static final int AZALEA_TREE_REACH = 5;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private DormantCalderaFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, FERN, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.FERN.defaultBlockState(), 4)
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 1))));
        FeatureUtils.register(context, AZALEA_BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.AZALEA.defaultBlockState(), 2)
                        .add(Blocks.FLOWERING_AZALEA.defaultBlockState(), 1))));
        // On the wall faces only, one block from the origin (vanilla's search goes no further) and spreading a little.
        FeatureUtils.register(context, GLOW_LICHEN, Feature.MULTIFACE_GROWTH, new MultifaceGrowthConfiguration(
                (MultifaceBlock) Blocks.GLOW_LICHEN, 1, false, false, true, 0.5F, HolderSet.direct(Block::builtInRegistryHolder,
                ModBlocks.PUMICE.get(), ModBlocks.ASH_BLOCK.get(), Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.TUFF, Blocks.BLACKSTONE)));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        // The crater floor: no lava, the found surface on the terrain.
        PlacementModifier floor = VolcanicFilter.builder().above(VolcanicOutput.Output.CALDERA_DORMANT, 0.5)
                .below(VolcanicOutput.Output.LAVA_BANK, 0.1).groundOnly(VolcanicFilter.DEFAULT_TOLERANCE).build();

        // About one living azalea per 20 chunks of floor, the beacon of the oasis.
        PlacementUtils.register(context, DORMANT_CALDERA_AZALEA_TREES, features.getOrThrow(TreeFeatures.AZALEA_TREE),
                JitteredGridPlacement.of(16, AZALEA_TREE_REACH + 1, 0.05F), SURFACE, floor,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(Blocks.AZALEA.defaultBlockState(), BlockPos.ZERO))),
                BiomeFilter.biome());
        PlacementUtils.register(context, DORMANT_CALDERA_AZALEA_BUSHES, features.getOrThrow(AZALEA_BUSH),
                JitteredGridPlacement.of(8, 0, 0.35F), SURFACE, floor, survives(Blocks.AZALEA), BiomeFilter.biome());
        PlacementUtils.register(context, DORMANT_CALDERA_FERNS, features.getOrThrow(FERN),
                JitteredGridPlacement.of(2, 0, 0.2F), SURFACE, floor, survives(Blocks.FERN), BiomeFilter.biome());
        // Berry bushes at the foot of the crater wall.
        PlacementUtils.register(context, DORMANT_CALDERA_BERRY_BUSHES, features.getOrThrow(EmberWoodsFeatures.EMBER_BERRY_BUSH),
                JitteredGridPlacement.of(8, 0, 0.3F), SURFACE, SlopeFilter.wallFoot(4, 4), survives(ModBlocks.EMBER_BERRY_BUSH.get()),
                BiomeFilter.biome());
        // Glow lichen next to the wall, up to 2 above its foot (the multiface feature reads 2 around: margin 3).
        PlacementUtils.register(context, DORMANT_CALDERA_GLOW_LICHEN, features.getOrThrow(GLOW_LICHEN),
                JitteredGridPlacement.of(8, 3, 0.6F), SURFACE, SlopeFilter.wallFoot(1, 3), RandomOffsetPlacement.vertical(UniformInt.of(0, 2)),
                BiomeFilter.biome());
    }

    private static PlacementModifier survives(Block block) {
        return BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.wouldSurvive(block.defaultBlockState(), BlockPos.ZERO)));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
