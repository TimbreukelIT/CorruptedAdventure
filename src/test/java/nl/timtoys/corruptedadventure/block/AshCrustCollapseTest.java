package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 38c: the chain of an ash crust collapse, on a world of position sets. {@link World} also plays the ticks the way the
 * game does (a block falls only with room below at its tick; a falling block is gone, it lands far below).
 */
class AshCrustCollapseTest {
    private static final int ROOF_Y = 70;
    private static final BlockPos START = new BlockPos(0, ROOF_Y, 0);
    private static final int WARNING = 15;

    /** Crust, rock and scheduled ticks; the rest is air. */
    private static final class World implements AshCrustCollapse.Crusts {
        final Set<BlockPos> crust = new HashSet<>();
        final Set<BlockPos> rock = new HashSet<>();
        final Map<BlockPos, Integer> ticks = new HashMap<>();
        private static final Comparator<Map.Entry<BlockPos, Integer>> TICK_ORDER =
                Comparator.<Map.Entry<BlockPos, Integer>>comparingInt(Map.Entry::getValue).thenComparing(Map.Entry::getKey);
        int now;

        @Override
        public boolean isCrust(BlockPos pos) {
            return crust.contains(pos);
        }

        @Override
        public boolean freeBelow(BlockPos pos) {
            return !crust.contains(pos.below()) && !rock.contains(pos.below());
        }

        @Override
        public boolean isScheduled(BlockPos pos) {
            return ticks.containsKey(pos);
        }

        /** Like the game: a second tick for the same block is ignored. */
        @Override
        public void schedule(BlockPos pos, int delay) {
            ticks.putIfAbsent(pos.immutable(), now + delay);
        }

        Set<BlockPos> hanging() {
            return crust.stream().filter(this::freeBelow).collect(Collectors.toSet());
        }

        /** Runs every scheduled tick in time order: crust with room below falls, the crust on it follows. */
        void run() {
            while (!ticks.isEmpty()) {
                Map.Entry<BlockPos, Integer> next = ticks.entrySet().stream().min(TICK_ORDER).orElseThrow();
                ticks.remove(next.getKey());
                now = next.getValue();
                if (crust.contains(next.getKey()) && freeBelow(next.getKey())) {
                    crust.remove(next.getKey());
                    AshCrustCollapse.fell(this, next.getKey());
                }
            }
        }
    }

    /** A flat roof of crust at {@link #ROOF_Y}, x and z from {@code -r} to {@code r}. */
    private static Set<BlockPos> roof(int r) {
        Set<BlockPos> roof = new HashSet<>();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                roof.add(new BlockPos(x, ROOF_Y, z));
            }
        }
        return roof;
    }

    private static World world(Set<BlockPos> crust) {
        World world = new World();
        world.crust.addAll(crust);
        return world;
    }

    private static Set<BlockPos> positions(List<AshCrustCollapse.Step> steps) {
        return steps.stream().map(AshCrustCollapse.Step::pos).collect(Collectors.toSet());
    }

    /** After a collapse nothing floats that did not hang before: every crust left with room below already had it. */
    private static void assertNothingNewFloats(Set<BlockPos> hangingBefore, World world) {
        for (BlockPos pos : world.hanging()) {
            assertTrue(hangingBefore.contains(pos), "left floating: " + pos);
        }
    }

    @Test
    void aHangingRoofFallsAsAWholeNearestFirst() {
        List<AshCrustCollapse.Step> steps = AshCrustCollapse.plan(world(roof(1)), START, AshCrustCollapse.MAX_BLOCKS);
        assertEquals(9, steps.size());
        assertEquals(new AshCrustCollapse.Step(START, 0), steps.getFirst());
        assertTrue(steps.stream().skip(1).allMatch(step -> step.distance() == 1));
        assertEquals(roof(1), positions(steps));
    }

    @Test
    void crustRestingOnRockStaysAndCutsTheChain() {
        // Rock under the whole row x = 1: that row rests and the row x = 2 behind it is not connected any more.
        World world = world(roof(2));
        for (int z = -2; z <= 2; z++) {
            world.rock.add(new BlockPos(1, ROOF_Y - 1, z));
        }
        Set<BlockPos> fallen = positions(AshCrustCollapse.plan(world, START, AshCrustCollapse.MAX_BLOCKS));
        assertEquals(15, fallen.size());
        assertTrue(fallen.stream().allMatch(pos -> pos.getX() <= 0));
    }

    @Test
    void aCollapseStopsAtTheLimitWithTheNearestBlocks() {
        List<AshCrustCollapse.Step> steps = AshCrustCollapse.plan(world(roof(10)), START, AshCrustCollapse.MAX_BLOCKS);
        assertEquals(AshCrustCollapse.MAX_BLOCKS, steps.size());
        for (int i = 1; i < steps.size(); i++) {
            assertTrue(steps.get(i - 1).distance() <= steps.get(i).distance());
        }
        // Rings of 8, 16, 24 around the start: all 49 blocks up to 3 steps, then 15 of the 32 at 4 steps.
        assertEquals(49, steps.stream().filter(step -> step.distance() <= 3).count());
        assertEquals(4, steps.getLast().distance());
    }

    /** Review 38c (3): a stepped or uneven roof (diagonal, one lower, one higher) comes down as one piece. */
    @Test
    void steppedAndUnevenRoofsFollow() {
        Set<BlockPos> crust = Set.of(START, new BlockPos(1, ROOF_Y - 1, 0), new BlockPos(2, ROOF_Y - 1, 1),
                new BlockPos(-1, ROOF_Y + 1, -1), new BlockPos(-2, ROOF_Y + 2, -1));
        assertEquals(crust, positions(AshCrustCollapse.plan(world(crust), START, AshCrustCollapse.MAX_BLOCKS)));
    }

    /** Review 38c (1): crust stacked on falling crust falls too, also above blocks past the limit of the plan. */
    @Test
    void crustOnAFallenBlockFollowsWhateverThePlanSaid() {
        World world = world(roof(10));
        for (int dy = 1; dy <= 3; dy++) {
            world.crust.add(START.above(dy));
            world.crust.add(new BlockPos(3, ROOF_Y + dy, 0));
            world.crust.add(new BlockPos(8, ROOF_Y + dy, 0));
        }
        Set<BlockPos> hangingBefore = world.hanging();
        AshCrustCollapse.start(world, START, WARNING);
        world.run();
        assertFalse(world.crust.contains(START.above(3)));
        assertNothingNewFloats(hangingBefore, world);
        // Past the limit the roof just keeps hanging, with its stack on it.
        assertTrue(world.crust.contains(new BlockPos(8, ROOF_Y, 0)));
        assertTrue(world.crust.contains(new BlockPos(8, ROOF_Y + 3, 0)));
    }

    /** Review 38c (2): crust that already has a tick is a passage, so two collapses side by side leave no rest. */
    @Test
    void anEarlierCollapseIsAPassageNotAWall() {
        Set<BlockPos> strip = new HashSet<>();
        for (int x = 0; x < 20; x++) {
            strip.add(new BlockPos(x, ROOF_Y, 0));
        }
        World world = world(strip);
        BlockPos busy = new BlockPos(5, ROOF_Y, 0);
        world.crust.add(busy.above());
        world.schedule(busy, 100);
        Set<BlockPos> hangingBefore = world.hanging();

        List<AshCrustCollapse.Step> steps = AshCrustCollapse.plan(world, START, AshCrustCollapse.MAX_BLOCKS);
        assertFalse(positions(steps).contains(busy));
        assertTrue(positions(steps).contains(new BlockPos(19, ROOF_Y, 0)));

        AshCrustCollapse.start(world, START, WARNING);
        world.run();
        assertTrue(world.crust.isEmpty(), "left: " + world.crust);
        assertNothingNewFloats(hangingBefore, world);
    }

    /** Review 38c (6): a block that got support after the plan stays, held up; the rest still falls. */
    @Test
    void supportAddedAfterThePlanHoldsOnlyItsOwnBlock() {
        World world = world(roof(2));
        world.crust.add(START.above());
        Set<BlockPos> hangingBefore = world.hanging();
        AshCrustCollapse.start(world, START, WARNING);
        BlockPos middle = new BlockPos(1, ROOF_Y, 0);
        world.rock.add(START.below());
        world.rock.add(middle.below());
        world.run();
        assertEquals(Set.of(START, START.above(), middle), world.crust);
        assertNothingNewFloats(hangingBefore, world);
    }

    @Test
    void theStartAloneWhenNothingHangsNextToIt() {
        assertEquals(1, AshCrustCollapse.plan(world(Set.of(START)), START, AshCrustCollapse.MAX_BLOCKS).size());
    }
}
