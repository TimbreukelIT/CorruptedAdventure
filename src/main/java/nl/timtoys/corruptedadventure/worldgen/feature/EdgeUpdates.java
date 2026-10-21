package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Vanilla's shape update at the edge of what a ring 4 tree or ground object wrote ({@link TreePlan#commit},
 * {@link GroundObjects}): every face between a written block and one that is not, passed on to the blocks that change
 * (a propagule whose leaf lost falls, a tall plant cut in half goes, the rest of a giant lily pad follows). The same
 * pass for trees and objects, so the end does not depend on which of them was written first.
 * <p>
 * Hanging strands (decor A, {@link TreeOverlap#strand}) that lose their hold pass the update on in a queue of their own,
 * also into strands that were just written, so a strand cut by another tree's leaf falls whole in either order. Without
 * strands the pass is exactly the decor proef's. Sand and gravel beside what was written are left alone (backlog r. 421).
 */
final class EdgeUpdates {
    /** How many changed blocks pass the shape update on (a giant lily pad has four parts). */
    private static final int MAX_PASSED_ON = 16;
    /** How many fallen strand blocks pass it on (a strand is up to 12 long, a crown has many). */
    private static final int MAX_STRANDS_PASSED_ON = 64;

    private EdgeUpdates() {
    }

    static void apply(WorldGenLevel level, Set<BlockPos> written) {
        Deque<BlockPos> changed = new ArrayDeque<>();
        Deque<BlockPos> fallen = new ArrayDeque<>();
        for (BlockPos pos : written) {
            for (Direction direction : Direction.values()) {
                if (!written.contains(pos.relative(direction))) {
                    updateFace(level, pos, direction, true, changed, fallen);
                }
            }
        }
        // A block that changed (gone) tells its other neighbours, a few steps at most.
        int steps = 0;
        int strandSteps = 0;
        while (true) {
            BlockPos pos;
            if (!changed.isEmpty() && steps < MAX_PASSED_ON) {
                pos = changed.poll();
                steps++;
            } else if (!fallen.isEmpty() && strandSteps < MAX_STRANDS_PASSED_ON) {
                pos = fallen.poll();
                strandSteps++;
            } else {
                return;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!written.contains(next) || TreeOverlap.strand(level.getBlockState(next))) {
                    updateFace(level, pos, direction, false, changed, fallen);
                }
            }
        }
    }

    /** Vanilla's {@code updateShapeAtEdge} on one face; {@code both}: also the block at {@code pos} itself. */
    private static void updateFace(WorldGenLevel level, BlockPos pos, Direction direction, boolean both, Deque<BlockPos> changed,
                                   Deque<BlockPos> fallen) {
        BlockPos neighbour = pos.relative(direction);
        BlockState state = level.getBlockState(pos);
        BlockState other = level.getBlockState(neighbour);
        if (both) {
            BlockState updated = state.updateShape(direction, other, level, pos, neighbour);
            if (updated != state) {
                // A written strand that loses its hold tells its neighbours too; other written blocks do not.
                if (TreeOverlap.strand(state)) {
                    fallen.add(pos);
                }
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
                state = updated;
            }
        }
        if (other.getBlock() instanceof FallingBlock) {
            // Backlog r. 421: sand or gravel of the terrain only schedules its fall here, and drops into an open space
            // under it (seen under the lagoon floor) and leaves air under the sea: the terrain stays as it is.
            return;
        }
        BlockState otherUpdated = other.updateShape(direction.getOpposite(), state, level, neighbour, pos);
        if (otherUpdated != other) {
            level.setBlock(neighbour, otherUpdated, Block.UPDATE_CLIENTS);
            (TreeOverlap.strand(other) ? fallen : changed).add(neighbour);
        }
    }
}
