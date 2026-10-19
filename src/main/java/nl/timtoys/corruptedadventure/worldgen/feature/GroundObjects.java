package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The shared rules of the ring 4 ground objects (decor proef, 2026-10-19: {@link BoulderFeature},
 * {@link FallenTreeFeature}, {@link TerraceDressingFeature}), so that they stay independent of generation order next to
 * the trees ({@link IsolatedTreeFeature}), whose crowns reach in from the neighbouring chunks before or after them:
 * <ul>
 *   <li>an object decides its shape from its own random (position and world seed) and the frozen worldgen heightmaps
 *   only, never from blocks another feature may or may not have placed yet;</li>
 *   <li>it stays in its own chunk ({@link #fit});</li>
 *   <li>stone ({@link #placeObject}) goes over whatever stands above the surface, and trees never take its place
 *   ({@link TreeOverlap#object}); wood ({@link #placeWood}) takes its place by the trees' own priority
 *   ({@link TreeOverlap#wins}), so a fallen trunk and a tree merge alike in either order;</li>
 *   <li>plants and carpets on them ({@link #placeDecor}) only fill open air, as the ground plants: a tree's block
 *   wins over them.</li>
 * </ul>
 */
final class GroundObjects {
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private GroundObjects() {
    }

    /** The object's own random: the same object at the same place of the same world, whatever came before. */
    static RandomSource random(long worldSeed, BlockPos origin, long salt) {
        return new XoroshiroRandomSource(RandomSupport.mixStafford13(worldSeed ^ salt), RandomSupport.mixStafford13(origin.asLong()));
    }

    /** {@code origin} moved so that everything within {@code reach} of it lies in its own chunk. */
    static BlockPos fit(BlockPos origin, int reach) {
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        int x = Mth.clamp(origin.getX(), minX + reach, minX + 15 - reach);
        int z = Mth.clamp(origin.getZ(), minZ + reach, minZ + 15 - reach);
        return new BlockPos(x, origin.getY(), z);
    }

    /** The column's frozen {@code WORLD_SURFACE_WG} height: the first open block over the terrain. */
    static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    /** Whether the column is dry land: no water over its floor on the frozen heightmaps. */
    static boolean dry(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) == surface(level, x, z);
    }

    /** Writes the solid blocks of an object over whatever is there (never over a lift column), then the edge updates. */
    static void placeObject(WorldGenLevel level, Map<BlockPos, BlockState> blocks) {
        Set<BlockPos> written = new LinkedHashSet<>();
        blocks.forEach((pos, state) -> {
            if (!level.getBlockState(pos).is(ModBlocks.UPDRAFT.get())) {
                level.setBlock(pos, state, FLAGS);
                written.add(pos);
            }
        });
        EdgeUpdates.apply(level, written);
    }

    /**
     * Writes wood as a tree block ({@link TreeOverlap#wins}, decided for all positions first; never over an object or a
     * lift column), then the edge updates. Returns the positions that hold the wood.
     */
    static Set<BlockPos> placeWood(WorldGenLevel level, TreeOverlap.Terrain terrain, Map<BlockPos, BlockState> blocks) {
        Set<BlockPos> written = new LinkedHashSet<>();
        Set<BlockPos> winners = new LinkedHashSet<>();
        blocks.forEach((pos, state) -> {
            BlockState existing = level.getBlockState(pos);
            if (existing == state) {
                written.add(pos);
            } else if (!TreeOverlap.object(existing, pos, terrain) && TreeOverlap.wins(state, existing, level.getBlockState(pos.below()))) {
                winners.add(pos);
                written.add(pos);
            }
        });
        winners.forEach(pos -> level.setBlock(pos, blocks.get(pos), FLAGS));
        EdgeUpdates.apply(level, written);
        return written;
    }

    /** A plant or carpet: only into open air and only where it survives. */
    static boolean placeDecor(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).isAir() || !state.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, state, FLAGS);
        return true;
    }
}
