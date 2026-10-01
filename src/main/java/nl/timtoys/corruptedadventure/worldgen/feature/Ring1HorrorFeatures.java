package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;
import nl.timtoys.corruptedadventure.ring.Ring;

/**
 * Datagen bootstraps for ring 1's horror features: bare patches with dead trees, more of them towards the edge of the
 * ring, and rare unnatural tunnels spread evenly over the ring. Biomes get them through {@code Ring1BiomeModifiers}.
 * Amounts are starting values, tuned in-game.
 */
public final class Ring1HorrorFeatures {
    private Ring1HorrorFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);

        FeatureUtils.register(context, ModConfiguredFeatures.BARE_PATCH, ModFeatures.BARE_PATCH.get(), new BarePatchConfiguration(
                UniformInt.of(4, BarePatchFeature.MAX_RADIUS),
                0.35F,
                BlockStateProvider.simple(ModBlocks.DEAD_GRASS_BLOCK.get()),
                BlockStateProvider.simple(ModBlocks.WITHERED_DIRT.get()),
                UniformInt.of(1, 2),
                HolderSet.direct(BuiltInRegistries.BLOCK::wrapAsHolder,
                        Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.ROOTED_DIRT),
                ModBlocks.DEAD_LOG.get(),
                features.getOrThrow(ModConfiguredFeatures.WITHERED_TREES),
                UniformInt.of(1, 3),
                0.05F));

        FeatureUtils.register(context, ModConfiguredFeatures.UNNATURAL_TUNNEL, ModFeatures.UNNATURAL_TUNNEL.get(), new UnnaturalTunnelConfiguration(
                UniformInt.of(12, 28),
                UniformInt.of(6, 14),
                0.5F,
                blocks.getOrThrow(BlockTags.OVERWORLD_CARVER_REPLACEABLES),
                0.7F));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);

        // No count or rarity: the chance per chunk comes from the ring progress alone (0 until ~500 blocks, 0.12 at the edge).
        PlacementUtils.register(context, ModPlacedFeatures.BARE_PATCH, features.getOrThrow(ModConfiguredFeatures.BARE_PATCH),
                InSquarePlacement.spread(),
                RingChanceFilter.progress(Ring.RING_1, 0.0, 0.12, 0.33, 2.0),
                HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                BiomeFilter.biome());
        // Many tries find no mouth (solid rock, open air), so fewer tunnels exist than this rarity suggests.
        PlacementUtils.register(context, ModPlacedFeatures.UNNATURAL_TUNNEL, features.getOrThrow(ModConfiguredFeatures.UNNATURAL_TUNNEL),
                RarityFilter.onAverageOnceEvery(12),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(160)),
                BiomeFilter.biome());
    }
}
