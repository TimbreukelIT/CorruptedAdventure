package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.ArrayList;
import java.util.List;

/**
 * The shared placement pieces of the ring 5 features (94/95): keys under {@code ring_5/}, the frozen worldgen heightmap
 * and the one rule that keeps the ring's dressing independent of generation order.
 * <p>
 * <b>Order rule.</b> Every ring 5 feature writes only into its own chunk (a ground object, or this chunk's part of a pure
 * plan: {@link MirroredTreesFeature}, {@link MemoryFeature}), so all blocks of a chunk are written during that chunk's own
 * decoration, in the fixed order of {@link Ring5Features#ORDER}. The ores reach into the next chunk but only replace
 * stone, and no dressing is made of a block an ore replaces. What an object writes goes over air and over the plants of
 * the ground ({@link #free}), so a plant (of this chunk, placed earlier) never decides whether an object stands.
 */
final class Ring5Placements {
    /** The first open block above the ground. */
    static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private Ring5Placements() {
    }

    static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_5/" + name));
    }

    static ResourceKey<PlacedFeature> placedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_5/" + name));
    }

    /** Whether an object may take the place of {@code state}: air, or a plant of the ground (grass, flowers, a dead bush). */
    static boolean free(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.FLOWERS) || state.is(Blocks.DEAD_BUSH);
    }

    /** Registers {@code feature} with {@code modifiers} and a biome filter last. */
    static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                         ResourceKey<ConfiguredFeature<?, ?>> feature, PlacementModifier... modifiers) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        List<PlacementModifier> list = new ArrayList<>(List.of(modifiers));
        list.add(BiomeFilter.biome());
        PlacementUtils.register(context, key, features.getOrThrow(feature), List.copyOf(list));
    }

    /** Registers {@code feature} once per chunk at its corner, without a biome filter (the feature looks per column). */
    static void perChunk(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                         ResourceKey<ConfiguredFeature<?, ?>> feature) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementUtils.register(context, key, features.getOrThrow(feature), List.of());
    }
}
