package nl.timtoys.corruptedadventure.worldgen.feature;

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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

/**
 * The sulfur fields' coloured boiling pools (change 47d, decision 4b-2): pools of bubbling mud in yellow, green or
 * orange, each in a basin of travertine (a raised calcite lip, terrace steps of yellow and orange terracotta and calcite
 * around it). The coloured muds and the sulfur crystals around the vents are blocks of stream B; until they exist the
 * pools boil with {@code boiling_mud} ({@link #poolFill}, the one place to swap them in). Amounts are starting values.
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

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private SulfurFieldsFeatures() {
    }

    /** What boils in the pools (one per pool); placeholder {@code boiling_mud} until stream B's coloured muds exist. */
    static BlockStateProvider poolFill() {
        return BlockStateProvider.simple(ModBlocks.BOILING_MUD.get());
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
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

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        // About one pool per 2 chunks on level ground of the terraces.
        PlacementUtils.register(context, SULFUR_FIELDS_TERRACE_POOLS, features.getOrThrow(TERRACE_POOL),
                JitteredGridPlacement.of(16, TerracePoolFeature.MAX_REACH, 0.5F), SURFACE, SlopeFilter.flat(3, 1),
                VolcanicFilter.builder().ground().build(), BiomeFilter.biome());
    }
}
