package nl.timtoys.corruptedadventure.weather;

import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload.Plume;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VolcanoPlumesTest {
    /** The default layout: ring 3 from 3500 to 6000, boundaries moved at most 96 blocks by the noise. */
    private static final RingLayout LAYOUT = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES),
            RingLayout.noiseSeed(20260930L));

    @Test
    void nearMeansWithinTheRadiusOfRingThree() {
        for (int angle = 0; angle < 360; angle += 45) {
            double cos = Math.cos(Math.toRadians(angle));
            double sin = Math.sin(Math.toRadians(angle));
            // Clear of the noisy boundary ± 96 plus the zone's half width of 64 and the radius of 512.
            assertTrue(VolcanoPlumes.near(LAYOUT, 4800 * cos, 4800 * sin), "ring 3 at " + angle);
            assertTrue(VolcanoPlumes.near(LAYOUT, 3100 * cos, 3100 * sin), "ring 2 near its outer boundary at " + angle);
            assertTrue(VolcanoPlumes.near(LAYOUT, 6450 * cos, 6450 * sin), "ring 4 near its inner boundary at " + angle);
            assertFalse(VolcanoPlumes.near(LAYOUT, 2700 * cos, 2700 * sin), "the core of ring 2 at " + angle);
            assertFalse(VolcanoPlumes.near(LAYOUT, 6800 * cos, 6800 * sin), "the core of ring 4 at " + angle);
        }
        assertFalse(VolcanoPlumes.near(LAYOUT, 0.0, 0.0));
    }

    @Test
    void nearestIsTheSameListWhereverThePlayerStands() {
        Plume far = new Plume(5000, 0, 200, 20, true);
        Plume middle = new Plume(4000, 300, 150, 10, false);
        Plume close = new Plume(3900, -40, 160, 12, false);
        List<Plume> fromHere = VolcanoPlumes.nearest(List.of(far, middle, close), 3880.0, -20.0);
        assertEquals(List.of(close, middle, far), fromHere);
        assertEquals(fromHere, VolcanoPlumes.nearest(List.of(close, far, middle), 4990.0, 10.0));
    }

    @Test
    void nearestKeepsAtMostTheCapAndDropsTheFarthest() {
        List<Plume> found = IntStream.range(0, VolcanoPlumesPayload.MAX_PLUMES + 6)
                .mapToObj(i -> new Plume(4000 + (VolcanoPlumesPayload.MAX_PLUMES + 5 - i) * 40, 0, 150, 10, false))
                .toList();
        List<Plume> nearest = VolcanoPlumes.nearest(found, 4000.0, 0.0);
        assertEquals(VolcanoPlumesPayload.MAX_PLUMES, nearest.size());
        assertEquals(4000, nearest.getFirst().x());
        assertTrue(nearest.stream().allMatch(plume -> plume.x() < 4000 + VolcanoPlumesPayload.MAX_PLUMES * 40));
    }

    @Test
    void nothingFoundSendsNothing() {
        assertEquals(List.of(), VolcanoPlumes.nearest(List.of(), 0.0, 0.0));
    }

    @Test
    void everyPlayerIsCheckedOncePerCycleOnTheirOwnTick() {
        for (int id : new int[]{0, 1, 39, 40, 12345, -7}) {
            long checks = IntStream.range(0, VolcanoPlumes.CHECK_TICKS * 3).filter(tick -> VolcanoPlumes.due(1000L + tick, id)).count();
            assertEquals(3, checks, "entity " + id);
        }
        long sameTick = IntStream.range(0, VolcanoPlumes.CHECK_TICKS).filter(id -> VolcanoPlumes.due(5000L, id)).count();
        assertEquals(1, sameTick);
    }
}
