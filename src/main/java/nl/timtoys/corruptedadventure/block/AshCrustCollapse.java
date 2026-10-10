package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A collapse of an ash crust roof (38c). Ash crust ignores neighbour updates ({@link AshCrustBlock}), so the chain sand
 * gets from vanilla updates is made here, in two parts:
 * <ul>
 *   <li>{@link #plan}: when a player starts a collapse, the connected hanging crust is found at once (breadth-first
 *   over all 26 neighbours, so stepped and uneven roofs follow too; at most {@link #MAX_BLOCKS}) and each block gets a
 *   scheduled tick a little later the further it is from the start. Crust that already has a tick (an earlier
 *   collapse) is a passage, not scheduled again.</li>
 *   <li>{@link #fell}: whenever a crust really falls, the crust resting on it follows, whatever the plan or its limit
 *   said, so nothing is left floating on a block that fell.</li>
 * </ul>
 * Every tick decides again (vanilla's falling-block tick: only with room below), so a block that got support after the
 * plan stays, held by that support, and the rest of the plan still falls. Started only by a player, so worldgen stays
 * deterministic. Server-side only.
 */
public final class AshCrustCollapse {
    /** At most this many blocks are scheduled by one collapse, the start included (tuning value). */
    static final int MAX_BLOCKS = 64;
    /** At most this many blocks are looked at, passages included (bounds the search through an earlier collapse). */
    static final int MAX_VISITED = 4 * MAX_BLOCKS;
    /** Ticks between one ring of the chain and the next (tuning value; sand's own delay is 2). */
    static final int STEP_TICKS = 2;

    private AshCrustCollapse() {
    }

    /** One block of a collapse and how many steps it lies from the start. */
    record Step(BlockPos pos, int distance) {
    }

    /** The world as a collapse sees it; the server level in the game, a set of positions in tests. */
    interface Crusts {
        boolean isCrust(BlockPos pos);

        /** Room to fall: vanilla's {@link FallingBlock#isFree} of the block below. */
        boolean freeBelow(BlockPos pos);

        boolean isScheduled(BlockPos pos);

        void schedule(BlockPos pos, int delay);
    }

    static Crusts in(ServerLevel level, Block crust) {
        return new Crusts() {
            @Override
            public boolean isCrust(BlockPos pos) {
                return level.getBlockState(pos).is(crust);
            }

            @Override
            public boolean freeBelow(BlockPos pos) {
                return FallingBlock.isFree(level.getBlockState(pos.below()));
            }

            @Override
            public boolean isScheduled(BlockPos pos) {
                return level.getBlockTicks().hasScheduledTick(pos, crust);
            }

            @Override
            public void schedule(BlockPos pos, int delay) {
                level.scheduleTick(pos, crust, delay);
            }
        };
    }

    /** Schedules the crust at {@code start} after {@code delay} ticks and the connected hanging crust after it. */
    static void start(Crusts world, BlockPos start, int delay) {
        for (Step step : plan(world, start, MAX_BLOCKS)) {
            world.schedule(step.pos(), delay + STEP_TICKS * step.distance());
        }
    }

    /** The crust at {@code pos} just fell: the crust resting on it follows. */
    static void fell(Crusts world, BlockPos pos) {
        BlockPos above = pos.above();
        if (world.isCrust(above) && !world.isScheduled(above)) {
            world.schedule(above, STEP_TICKS);
        }
    }

    public static void start(ServerLevel level, BlockPos start, Block crust, int delay) {
        start(in(level, crust), start, delay);
    }

    public static void fell(ServerLevel level, BlockPos pos, Block crust) {
        fell(in(level, crust), pos);
    }

    /**
     * The blocks a collapse schedules, nearest first, starting with {@code start} (unless it already has a tick). A
     * neighbour (any of the 26) joins when it is crust and hangs (room below), or when it is crust that already has a
     * tick: that one is passed through but not scheduled again. Distances count steps through passages too.
     */
    static List<Step> plan(Crusts world, BlockPos start, int max) {
        List<Step> steps = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<Step> queue = new ArrayDeque<>();
        Step first = new Step(start.immutable(), 0);
        visited.add(first.pos());
        queue.add(first);
        if (!world.isScheduled(first.pos())) {
            steps.add(first);
        }
        search:
        while (!queue.isEmpty()) {
            Step step = queue.poll();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (steps.size() >= max || visited.size() >= MAX_VISITED) {
                            break search;
                        }
                        BlockPos next = step.pos().offset(dx, dy, dz);
                        if (visited.contains(next) || !world.isCrust(next)) {
                            continue;
                        }
                        boolean scheduled = world.isScheduled(next);
                        if (!scheduled && !world.freeBelow(next)) {
                            continue;
                        }
                        visited.add(next);
                        Step joined = new Step(next, step.distance() + 1);
                        queue.add(joined);
                        if (!scheduled) {
                            steps.add(joined);
                        }
                    }
                }
            }
        }
        return steps;
    }
}
