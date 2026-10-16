package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.ArrayList;
import java.util.List;

/**
 * The shared placement pieces of the ring 4 features (golf 2): keys under {@code ring_4/}, the frozen worldgen
 * heightmaps (the 23c rule: they show the terrain alone, also in the neighbouring chunks), and the usual block
 * predicates. Every list ends in the biome filter.
 */
final class Ring4Placements {
    /** The first open block above the ground or the water (land plants, trees, water surface plants). */
    static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    /** The first block above the solid ground, also under water (water plants, trees standing in water). */
    static final PlacementModifier FLOOR = HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG);
    /** Not on a log or leaves: decoration stays off the world tree (74) and the trees. */
    static final BlockPredicate OFF_TREES = BlockPredicate.not(BlockPredicate.anyOf(
            BlockPredicate.matchesTag(new Vec3i(0, -1, 0), BlockTags.LOGS), BlockPredicate.matchesTag(new Vec3i(0, -1, 0), BlockTags.LEAVES)));
    /** Air here and dry ground below (not a tree): land. */
    static final BlockPredicate ON_LAND = BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
            BlockPredicate.noFluid(new Vec3i(0, -1, 0)), OFF_TREES);
    /** Air here over a water source: the water's surface. */
    static final BlockPredicate ON_WATER = BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
            BlockPredicate.matchesFluids(new Vec3i(0, -1, 0), Fluids.WATER));
    /** Water here and right above: under water, at least two deep. */
    static final BlockPredicate UNDER_WATER = BlockPredicate.allOf(BlockPredicate.matchesFluids(Fluids.WATER),
            BlockPredicate.matchesFluids(new Vec3i(0, 1, 0), Fluids.WATER), OFF_TREES);

    private Ring4Placements() {
    }

    static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_4/" + name));
    }

    static ResourceKey<PlacedFeature> placedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_4/" + name));
    }

    /** Water down to {@code depth} blocks below a position (a deep pool's floor: water from the floor block up). */
    static BlockPredicate waterAbove(int depth) {
        List<BlockPredicate> water = new ArrayList<>();
        for (int dy = 0; dy < depth; dy++) {
            water.add(BlockPredicate.matchesFluids(new Vec3i(0, dy, 0), Fluids.WATER));
        }
        water.add(OFF_TREES);
        return BlockPredicate.allOf(water);
    }

    /** Water within {@code reach} blocks beside the block below (a bank): the four directions at one level down. */
    static BlockPredicate waterNear(int reach) {
        List<BlockPredicate> water = new ArrayList<>();
        for (int d = 1; d <= reach; d++) {
            water.add(BlockPredicate.matchesFluids(new Vec3i(d, -1, 0), Fluids.WATER));
            water.add(BlockPredicate.matchesFluids(new Vec3i(-d, -1, 0), Fluids.WATER));
            water.add(BlockPredicate.matchesFluids(new Vec3i(0, -1, d), Fluids.WATER));
            water.add(BlockPredicate.matchesFluids(new Vec3i(0, -1, -d), Fluids.WATER));
        }
        return BlockPredicate.anyOf(water);
    }

    static PlacementModifier only(BlockPredicate predicate) {
        return BlockPredicateFilter.forPredicate(predicate);
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

    /** {@link BlockPos#ZERO}'s offset {@code dy} up. */
    static Vec3i up(int dy) {
        return new Vec3i(0, dy, 0);
    }
}
