package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A collapse of an ash crust roof (38c): the crust above a player and the crust that hangs connected to it. Ash crust
 * ignores neighbour updates ({@link AshCrustBlock}), so the chain sand gets from vanilla updates is planned here
 * instead: when a player starts a collapse, the connected hanging crust is found at once (breadth-first, at most
 * {@link #MAX_BLOCKS}) and each block gets a scheduled tick a little later the further it is from the start. Started
 * only by a player, so worldgen stays deterministic. Server-side only.
 */
public final class AshCrustCollapse {
    /** At most this many blocks fall in one collapse, the start included (tuning value). */
    static final int MAX_BLOCKS = 64;
    /** Ticks between one ring of the chain and the next (tuning value; sand's own delay is 2). */
    static final int STEP_TICKS = 2;

    /**
     * Faces the chain spreads over: the four sides and up (crust stacked on a falling block). Not down: a block joins
     * only when it hangs, so the block under it is never crust that still has to be found.
     */
    private static final Direction[] SPREAD = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};

    private AshCrustCollapse() {
    }

    /** One block of a collapse and how many steps it lies from the start. */
    record Step(BlockPos pos, int distance) {
    }

    /**
     * Schedules the fall of the crust at {@code start} after {@code delay} ticks and of the connected hanging crust
     * after {@code delay} plus {@link #STEP_TICKS} per step. Blocks that already have a scheduled tick (an earlier
     * collapse) are left to it.
     */
    public static void start(ServerLevel level, BlockPos start, Block crust, int delay) {
        Predicate<BlockPos> idleCrust = pos -> level.getBlockState(pos).is(crust)
                && !level.getBlockTicks().hasScheduledTick(pos, crust);
        Predicate<BlockPos> freeBelow = pos -> FallingBlock.isFree(level.getBlockState(pos.below()));
        for (Step step : plan(start, idleCrust, freeBelow, MAX_BLOCKS)) {
            level.scheduleTick(step.pos(), crust, delay + STEP_TICKS * step.distance());
        }
    }

    /**
     * The blocks of a collapse, nearest first, starting with {@code start}. A neighbour joins when it is crust and
     * hangs: free below, or resting on a block of this collapse (which falls first). Pure: the world is only seen
     * through the predicates.
     */
    static List<Step> plan(BlockPos start, Predicate<BlockPos> crust, Predicate<BlockPos> freeBelow, int max) {
        List<Step> steps = new ArrayList<>();
        Set<BlockPos> planned = new HashSet<>();
        ArrayDeque<Step> queue = new ArrayDeque<>();
        Step first = new Step(start.immutable(), 0);
        planned.add(first.pos());
        queue.add(first);
        while (!queue.isEmpty() && steps.size() < max) {
            Step step = queue.poll();
            steps.add(step);
            for (Direction direction : SPREAD) {
                BlockPos next = step.pos().relative(direction);
                if (planned.contains(next) || !crust.test(next)) {
                    continue;
                }
                if (planned.contains(next.below()) || freeBelow.test(next)) {
                    planned.add(next);
                    queue.add(new Step(next, step.distance() + 1));
                }
            }
        }
        return steps;
    }
}
