package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

/**
 * The placeholder dressing of {@code verdant_falls} ({@code docs/plans/ring-4/verdant-falls.md} §4): bare until the
 * own tropical flora exists (no trees, no vanilla jungle, no vines). Moss carpets on the moss patches of the treads
 * (the patches themselves are surface rules) and the climbing routes on the walls (Q3 = B + ledges, user's decision):
 * strands of {@code hanging_swamp_moss} under overhanging ledges down to the next ledge, {@code glowing_hanging_roots}
 * only as a single glowing accent right under a ledge. No water.
 * <p>
 * Determinism as in ring 2 (23c): placement in the own chunk on the {@code WORLD_SURFACE_WG} heightmap, which no
 * feature changes; the carpets lie on that height, the strands hang below it ({@link LedgeStrandFeature}), so neither
 * reads what the other writes. Amounts are starting values, tuned at the test stop.
 */
public final class VerdantFallsFeatures {
    static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_CARPET = configured("verdant_moss_carpet");
    static final ResourceKey<ConfiguredFeature<?, ?>> LEDGE_STRANDS = configured("verdant_ledge_strands");

    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_MOSS_CARPETS = placed("verdant_falls_moss_carpets");
    public static final ResourceKey<PlacedFeature> VERDANT_FALLS_LEDGE_STRANDS = placed("verdant_falls_ledge_strands");

    /** Columns per chunk that look for ledges (about a quarter of the chunk). */
    static final int STRAND_COLUMNS = 64;

    private VerdantFallsFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        FeatureUtils.register(context, MOSS_CARPET, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MOSS_CARPET)));
        FeatureUtils.register(context, LEDGE_STRANDS, ModFeatures.LEDGE_STRAND.get(), new LedgeStrandConfiguration(
                ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(), ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState(),
                0.5F, 0.2F, 3, 24, 4));
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
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_4/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_4/" + name));
    }
}
