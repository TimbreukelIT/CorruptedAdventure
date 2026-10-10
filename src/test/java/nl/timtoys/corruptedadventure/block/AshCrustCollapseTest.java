package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 38c: the chain of an ash crust collapse, on a world described by sets of positions. */
class AshCrustCollapseTest {
    private static final int ROOF_Y = 70;

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

    private static List<AshCrustCollapse.Step> plan(Set<BlockPos> crust, Set<BlockPos> solid, int max) {
        Predicate<BlockPos> freeBelow = pos -> !crust.contains(pos.below()) && !solid.contains(pos.below());
        return AshCrustCollapse.plan(new BlockPos(0, ROOF_Y, 0), crust::contains, freeBelow, max);
    }

    @Test
    void aHangingRoofFallsAsAWholeNearestFirst() {
        List<AshCrustCollapse.Step> steps = plan(roof(1), Set.of(), AshCrustCollapse.MAX_BLOCKS);
        assertEquals(9, steps.size());
        assertEquals(new AshCrustCollapse.Step(new BlockPos(0, ROOF_Y, 0), 0), steps.getFirst());
        assertEquals(List.of(0, 1, 1, 1, 1, 2, 2, 2, 2), steps.stream().map(AshCrustCollapse.Step::distance).toList());
        assertEquals(roof(1), steps.stream().map(AshCrustCollapse.Step::pos).collect(Collectors.toSet()));
    }

    @Test
    void crustRestingOnRockStaysAndCutsTheChain() {
        // Rock under the whole row x = 1: that row rests and the row x = 2 behind it is not connected any more.
        Set<BlockPos> crust = roof(2);
        Set<BlockPos> rock = new HashSet<>();
        for (int z = -2; z <= 2; z++) {
            rock.add(new BlockPos(1, ROOF_Y - 1, z));
        }
        Set<BlockPos> fallen = plan(crust, rock, AshCrustCollapse.MAX_BLOCKS).stream()
                .map(AshCrustCollapse.Step::pos).collect(Collectors.toSet());
        assertEquals(15, fallen.size());
        assertTrue(fallen.stream().allMatch(pos -> pos.getX() <= 0));
    }

    @Test
    void aCollapseStopsAtTheLimitWithTheNearestBlocks() {
        List<AshCrustCollapse.Step> steps = plan(roof(10), Set.of(), AshCrustCollapse.MAX_BLOCKS);
        assertEquals(AshCrustCollapse.MAX_BLOCKS, steps.size());
        for (int i = 1; i < steps.size(); i++) {
            assertTrue(steps.get(i - 1).distance() <= steps.get(i).distance());
        }
        // Manhattan rings on a flat roof: all 61 blocks up to 5 steps, then 3 of the 24 at 6 steps.
        assertEquals(61, steps.stream().filter(step -> step.distance() <= 5).count());
        assertEquals(6, steps.getLast().distance());
    }

    @Test
    void crustStackedOnTheRoofFollowsOneStepLater() {
        Set<BlockPos> crust = new HashSet<>(roof(0));
        crust.add(new BlockPos(0, ROOF_Y + 1, 0));
        crust.add(new BlockPos(0, ROOF_Y + 2, 0));
        List<AshCrustCollapse.Step> steps = plan(crust, Set.of(), AshCrustCollapse.MAX_BLOCKS);
        assertEquals(List.of(new AshCrustCollapse.Step(new BlockPos(0, ROOF_Y, 0), 0),
                new AshCrustCollapse.Step(new BlockPos(0, ROOF_Y + 1, 0), 1),
                new AshCrustCollapse.Step(new BlockPos(0, ROOF_Y + 2, 0), 2)), steps);
    }

    @Test
    void otherBlocksAndBusyCrustDoNotJoin() {
        // The crust predicate leaves out crust with a scheduled tick (an earlier collapse): it falls on its own.
        Set<BlockPos> crust = roof(1);
        BlockPos busy = new BlockPos(1, ROOF_Y, 0);
        Predicate<BlockPos> idleCrust = pos -> crust.contains(pos) && !pos.equals(busy);
        List<AshCrustCollapse.Step> steps = AshCrustCollapse.plan(new BlockPos(0, ROOF_Y, 0), idleCrust,
                pos -> true, AshCrustCollapse.MAX_BLOCKS);
        assertEquals(8, steps.size());
        assertFalse(steps.stream().anyMatch(step -> step.pos().equals(busy)));
    }

    @Test
    void theStartAloneWhenNothingHangsNextToIt() {
        assertEquals(1, plan(Set.of(new BlockPos(0, ROOF_Y, 0)), Set.of(), AshCrustCollapse.MAX_BLOCKS).size());
    }
}
