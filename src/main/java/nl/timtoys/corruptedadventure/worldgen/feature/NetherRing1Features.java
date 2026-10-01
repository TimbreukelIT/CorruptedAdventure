package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;

import java.util.List;

/**
 * Datagen bootstraps for Nether ring 1's own features: the soul crystal ore, plus a floor variant in the crimson forest.
 * Biomes get them through {@code Ring1BiomeModifiers}.
 */
public final class NetherRing1Features {
    private NetherRing1Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // One vein, the variant per replaced block (like vanilla's stone/deepslate ores). Only netherrack and crimson
        // nylium: no basalt, blackstone or warped nylium. Exposed blocks stay, so the ore shows in the crimson floor.
        FeatureUtils.register(context, ModConfiguredFeatures.SOUL_CRYSTAL_ORE, Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(new BlockMatchTest(Blocks.NETHERRACK), ModBlocks.SOUL_CRYSTAL_ORE.get().defaultBlockState()),
                OreConfiguration.target(new BlockMatchTest(Blocks.CRIMSON_NYLIUM), ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get().defaultBlockState())),
                4, 0.0F));
        // Crimson nylium is a single floor layer, so the veins above rarely hit it: a small vein on the floors themselves.
        FeatureUtils.register(context, ModConfiguredFeatures.CRIMSON_SOUL_CRYSTAL_ORE, Feature.ORE, new OreConfiguration(
                new BlockMatchTest(Blocks.CRIMSON_NYLIUM), ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get().defaultBlockState(), 3, 0.0F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);

        // Absolute anchors: in the Nether (height 256) belowTop(10) is Y 245, above the bedrock roof.
        PlacementUtils.register(context, ModPlacedFeatures.SOUL_CRYSTAL_ORE, features.getOrThrow(ModConfiguredFeatures.SOUL_CRYSTAL_ORE),
                CountPlacement.of(2),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(10), VerticalAnchor.absolute(117)),
                BiomeFilter.biome());
        // Scans down to the nearest crimson floor and starts the vein in the block above it. The scan also stops on the ore
        // itself: veins of neighbouring chunks may already have turned the floor into ore, and the result must not depend
        // on which chunk was decorated first (count_on_every_layer does: neighbours' fungi change the layers).
        PlacementUtils.register(context, ModPlacedFeatures.CRIMSON_SOUL_CRYSTAL_ORE, features.getOrThrow(ModConfiguredFeatures.CRIMSON_SOUL_CRYSTAL_ORE),
                RarityFilter.onAverageOnceEvery(2),
                CountPlacement.of(20),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(127)),
                EnvironmentScanPlacement.scanningFor(Direction.DOWN,
                        BlockPredicate.matchesBlocks(Blocks.CRIMSON_NYLIUM, ModBlocks.CRIMSON_SOUL_CRYSTAL_ORE.get()), 32),
                RandomOffsetPlacement.vertical(ConstantInt.of(1)),
                BiomeFilter.biome());
    }
}
