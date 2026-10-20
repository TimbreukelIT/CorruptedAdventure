package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A toxic puddle one block deep in level dry ground (ring 4 decor B: {@link ToxicPuddleFeature} and the menhir circle's
 * pool), as the rules of {@link ToxicPoolFeature} and the poison scan ask: every column's top ground block becomes a
 * sludge source only when all of them lie on one level of dry ground in one chunk, a block from its edge (everything
 * the check reads is the own chunk's), and every side and the bottom of every sludge block is more sludge or solid
 * ground (no air, fluid or plant), with no water above. Nothing flows, nothing mixes.
 */
final class SludgePuddle {
    private SludgePuddle() {
    }

    /** Whether {@code state} is ground a puddle or its rim may take: soil of the blighted band. */
    static boolean soil(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(ModBlocks.PEAT.get()) || state.is(Blocks.CLAY);
    }

    /**
     * Turns the top ground block of each of {@code columns} (x, 0, z) into {@code sludge}, if they all pass; returns
     * whether it did.
     */
    static boolean place(WorldGenLevel level, BlockState sludge, Collection<BlockPos> columns) {
        if (columns.isEmpty()) {
            return false;
        }
        BlockPos first = columns.iterator().next();
        int chunkX = first.getX() >> 4;
        int chunkZ = first.getZ() >> 4;
        int top = GroundObjects.surface(level, first.getX(), first.getZ()) - 1;
        Set<BlockPos> pool = new LinkedHashSet<>();
        for (BlockPos column : columns) {
            int x = column.getX();
            int z = column.getZ();
            if (x >> 4 != chunkX || z >> 4 != chunkZ || (x & 15) == 0 || (x & 15) == 15 || (z & 15) == 0 || (z & 15) == 15
                    || GroundObjects.surface(level, x, z) - 1 != top || !GroundObjects.dry(level, x, z)) {
                return false;
            }
            BlockPos pos = new BlockPos(x, top, z);
            if (!soil(level.getBlockState(pos)) || !level.getFluidState(pos.above()).isEmpty()) {
                return false;
            }
            pool.add(pos);
        }
        if (!ToxicPoolFeature.closed(pool, pos -> solid(level.getBlockState(pos)))) {
            return false;
        }
        for (BlockPos pos : pool) {
            level.setBlock(pos, sludge, Block.UPDATE_CLIENTS);
        }
        return true;
    }

    /** Ground that holds a puddle: not air, no fluid, not a plant. */
    private static boolean solid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
    }
}
