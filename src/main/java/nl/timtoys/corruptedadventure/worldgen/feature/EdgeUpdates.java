package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Vanilla's shape update at the edge of what a ring 4 tree or ground object wrote ({@link TreePlan#commit},
 * {@link GroundObjects}): every face between a written block and one that is not, passed on to the blocks that change
 * (a propagule whose leaf lost falls, a tall plant cut in half goes, the rest of a giant lily pad follows). The same
 * pass for trees and objects, so the end does not depend on which of them was written first.
 */
final class EdgeUpdates {
    /** How many changed blocks pass the shape update on (a giant lily pad has four parts). */
    private static final int MAX_PASSED_ON = 16;

    private EdgeUpdates() {
    }

    static void apply(WorldGenLevel level, Set<BlockPos> written) {
        Deque<BlockPos> changed = new ArrayDeque<>();
        for (BlockPos pos : written) {
            for (Direction direction : Direction.values()) {
                if (!written.contains(pos.relative(direction))) {
                    updateFace(level, pos, direction, true, changed);
                }
            }
        }
        // A block that changed (gone) tells its other neighbours, a few steps at most.
        for (int steps = 0; !changed.isEmpty() && steps < MAX_PASSED_ON; steps++) {
            BlockPos pos = changed.poll();
            for (Direction direction : Direction.values()) {
                if (!written.contains(pos.relative(direction))) {
                    updateFace(level, pos, direction, false, changed);
                }
            }
        }
    }

    /** Vanilla's {@code updateShapeAtEdge} on one face; {@code both}: also the block at {@code pos} itself. */
    private static void updateFace(WorldGenLevel level, BlockPos pos, Direction direction, boolean both, Deque<BlockPos> changed) {
        BlockPos neighbour = pos.relative(direction);
        BlockState state = level.getBlockState(pos);
        BlockState other = level.getBlockState(neighbour);
        if (both) {
            BlockState updated = state.updateShape(direction, other, level, pos, neighbour);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
                state = updated;
            }
        }
        BlockState otherUpdated = other.updateShape(direction.getOpposite(), state, level, neighbour, pos);
        if (otherUpdated != other) {
            level.setBlock(neighbour, otherUpdated, Block.UPDATE_CLIENTS);
            changed.add(neighbour);
        }
    }
}
