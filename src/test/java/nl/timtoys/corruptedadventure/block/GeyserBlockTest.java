package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The geyser's eruption (the column above it, who is thrown, the push) and its rhythm. Placing and real ticks are S. */
class GeyserBlockTest {
    private static final long SALT = GeyserBlock.Rhythm.salt(20260930L, ResourceLocation.withDefaultNamespace("overworld").hashCode());

    private static GeyserBlock.Rhythm rhythm(BlockPos pos) {
        return GeyserBlock.Rhythm.of(SALT, pos);
    }

    @Test
    void intervalIsFixedPerPositionAndDiffersBetweenNeighbours() {
        Set<Integer> intervals = new HashSet<>();
        for (int x = -8; x < 8; x++) {
            for (int z = -8; z < 8; z++) {
                BlockPos pos = new BlockPos(x, 64, z);
                int interval = rhythm(pos).interval();
                assertTrue(interval >= GeyserBlock.MIN_INTERVAL_TICKS && interval <= GeyserBlock.MAX_INTERVAL_TICKS, pos + ": " + interval);
                assertEquals(rhythm(pos), rhythm(new BlockPos(x, 64, z)));
                intervals.add(interval);
            }
        }
        // 256 geysers spread over most of the 121 possible intervals.
        assertTrue(intervals.size() > 80, "only " + intervals.size() + " different intervals");
    }

    /** The same coordinates in another world or dimension get another rhythm. */
    @Test
    void seedAndDimensionChangeTheRhythm() {
        long otherSeed = GeyserBlock.Rhythm.salt(12345L, ResourceLocation.withDefaultNamespace("overworld").hashCode());
        long nether = GeyserBlock.Rhythm.salt(20260930L, ResourceLocation.withDefaultNamespace("the_nether").hashCode());
        int sameSeed = 0;
        int sameDimension = 0;
        for (int x = 0; x < 64; x++) {
            BlockPos pos = new BlockPos(x, 64, 0);
            sameSeed += rhythm(pos).equals(GeyserBlock.Rhythm.of(otherSeed, pos)) ? 1 : 0;
            sameDimension += rhythm(pos).equals(GeyserBlock.Rhythm.of(nether, pos)) ? 1 : 0;
        }
        assertTrue(sameSeed <= 2, sameSeed + " of 64 rhythms equal across seeds");
        assertTrue(sameDimension <= 2, sameDimension + " of 64 rhythms equal across dimensions");
    }

    /** Following the scheduled ticks from a fresh start: warning, eruption one second later, and so on every interval. */
    @Test
    void cycleWarnsThenEruptsOnItsOwnInterval() {
        for (BlockPos pos : List.of(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0), new BlockPos(-300, 70, 1250))) {
            GeyserBlock.Rhythm rhythm = rhythm(pos);
            long start = 123_456L;
            while (rhythm.ticksToWarning(rhythm.phase(start)) == 0) {
                start++;
            }
            long t = start + rhythm.ticksToWarning(rhythm.phase(start));
            List<GeyserBlock.Action> actions = new ArrayList<>();
            List<Long> times = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                GeyserBlock.Step step = rhythm.step(t);
                actions.add(step.action());
                times.add(t);
                assertTrue(step.delay() > 0);
                t += step.delay();
            }
            for (int i = 0; i < 10; i++) {
                assertEquals(i % 2 == 0 ? GeyserBlock.Action.WARN : GeyserBlock.Action.ERUPT, actions.get(i), pos + " step " + i);
            }
            for (int i = 1; i < 10; i += 2) {
                assertEquals(GeyserBlock.WARNING_TICKS, times.get(i) - times.get(i - 1));
                if (i >= 3) {
                    assertEquals(rhythm.interval(), times.get(i) - times.get(i - 2));
                }
            }
        }
    }

    /**
     * {@code startCycle} schedules max(1, ticksToWarning): the first tick is a warning (one tick short when the start is
     * exactly the warning moment, instead of waiting a whole extra cycle) and the eruption follows on time.
     */
    @Test
    void freshStartWarnsAtTheNextWarning() {
        GeyserBlock.Rhythm rhythm = rhythm(new BlockPos(5, 64, 9));
        for (long t = 1000; t < 1000 + rhythm.interval(); t++) {
            long first = t + Math.max(1, rhythm.ticksToWarning(rhythm.phase(t)));
            assertTrue(first - t < rhythm.interval(), "waits " + (first - t));
            GeyserBlock.Step step = rhythm.step(first);
            assertEquals(GeyserBlock.Action.WARN, step.action(), "start at " + t);
            assertTrue(step.delay() >= GeyserBlock.WARNING_TICKS - 1);
            assertEquals(0, rhythm.phase(first + step.delay()));
        }
    }

    /** A tick that comes late (its chunk was unloaded) falls back into the rhythm instead of shifting it. */
    @Test
    void lateTicksFallBackIntoTheRhythm() {
        GeyserBlock.Rhythm rhythm = rhythm(new BlockPos(-17, 80, 33));
        long eruption = 50_000L;
        while (rhythm.phase(eruption) != 0) {
            eruption++;
        }
        // A late eruption tick is skipped: wait for the next warning.
        GeyserBlock.Step late = rhythm.step(eruption + 5);
        assertEquals(GeyserBlock.Action.WAIT, late.action());
        assertEquals(eruption + rhythm.interval() - GeyserBlock.WARNING_TICKS, eruption + 5 + late.delay());
        // A late warning is shortened, the eruption stays on time.
        assertEquals(new GeyserBlock.Step(GeyserBlock.Action.WARN, 15), rhythm.step(eruption - 15));
    }

    /**
     * A first tick at any moment of the cycle (worldgen loses the delay of the tick it schedules) falls into the rhythm:
     * every eruption is on phase 0, and from the second tick on each eruption follows a warning.
     */
    @Test
    void firstTickAtAnyMomentFallsIntoTheRhythm() {
        GeyserBlock.Rhythm rhythm = rhythm(new BlockPos(40, 70, -12));
        for (long first = 7000; first < 7000 + rhythm.interval(); first++) {
            long t = first;
            GeyserBlock.Action previous = null;
            int eruptions = 0;
            while (t < first + 4L * rhythm.interval()) {
                GeyserBlock.Step step = rhythm.step(t);
                assertTrue(step.delay() > 0);
                if (step.action() == GeyserBlock.Action.ERUPT) {
                    assertEquals(0, rhythm.phase(t));
                    if (t != first) {
                        assertEquals(GeyserBlock.Action.WARN, previous, "eruption at " + t + " without warning (first tick " + first + ")");
                    }
                    eruptions++;
                }
                previous = step.action();
                t += step.delay();
            }
            assertTrue(eruptions >= 3, eruptions + " eruptions from first tick " + first);
        }
    }

    /** Neighbouring geysers seldom erupt in the same tick, counted over 2000 ticks. */
    @Test
    void neighboursRarelyEruptTogether() {
        int eruptions = 0;
        int together = 0;
        for (int x = 0; x < 16; x++) {
            GeyserBlock.Rhythm a = rhythm(new BlockPos(x, 64, 0));
            GeyserBlock.Rhythm b = rhythm(new BlockPos(x + 1, 64, 0));
            for (long t = 0; t < 2000; t++) {
                if (a.phase(t) == 0) {
                    eruptions++;
                    together += b.phase(t) == 0 ? 1 : 0;
                }
            }
        }
        assertTrue(together * 20 <= eruptions, together + " of " + eruptions + " eruptions together with the neighbour");
    }

    @Test
    void columnIsTheReachBlocksStraightAboveTheGeyser() {
        AABB column = GeyserBlock.column(new BlockPos(3, 64, -7));
        assertEquals(new AABB(3, 65, -7, 4, 65 + GeyserBlock.REACH, -6), column);
    }

    @Test
    void launchSetsAtLeastTheLaunchSpeedUpAndKeepsHorizontalMotion() {
        assertEquals(new Vec3(0.2, GeyserBlock.LAUNCH_SPEED, -0.1), GeyserBlock.launched(new Vec3(0.2, -0.5, -0.1)));
        // Something already rising faster keeps its speed.
        assertEquals(new Vec3(0.0, 1.5, 0.0), GeyserBlock.launched(new Vec3(0.0, 1.5, 0.0)));
    }

    /** Only an entity with its centre over the geyser is thrown, not one at the edge of the next block. */
    @Test
    void onlyEntitiesCentredOverTheVentAreLaunched() {
        BlockPos geyser = new BlockPos(3, 64, -7);
        assertTrue(GeyserBlock.overVent(geyser, new Vec3(3.5, 65.0, -6.5)));
        assertTrue(GeyserBlock.overVent(geyser, new Vec3(3.01, 66.0, -6.99)));
        assertFalse(GeyserBlock.overVent(geyser, new Vec3(4.2, 65.0, -6.5)));
        assertFalse(GeyserBlock.overVent(geyser, new Vec3(3.5, 65.0, -7.2)));
    }
}
