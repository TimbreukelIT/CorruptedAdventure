package nl.timtoys.corruptedadventure.worldgen.surface;

import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The frayed inner edge of a transition zone (G2 ring 3, decision 8 = A): the zeekom's sand ↔ the steam fens' mud. */
class ZoneEdgeConditionTest {
    @Test
    void theZoneStartsHalfItsWidthBeforeTheBoundary() {
        RingSettings settings = new RingSettings(List.of(1500, 3500, 6000, 8000, 9700), 10200, 96, 128, List.of(0, 0, 256, 256, 0));
        assertEquals(6000 - 128, ZoneEdgeCondition.zoneStart(settings, Ring.RING_3), 1e-9);
        assertEquals(1500 - 64, ZoneEdgeCondition.zoneStart(settings, Ring.RING_1), 1e-9);
    }

    @Test
    void theEdgeFraysIntoTheRingAndThinsOutInwards() {
        double width = 36.0;
        // In the zone (and beyond) always.
        assertTrue(ZoneEdgeCondition.past(0.0, width, -1.0));
        assertTrue(ZoneEdgeCondition.past(50.0, width, -5.0));
        // Beyond the width never, whatever the noise.
        assertFalse(ZoneEdgeCondition.past(-width - 0.5, width, 5.0));
        // Inside the band: deeper into the ring needs a higher noise.
        assertTrue(ZoneEdgeCondition.past(-9.0, width, 0.0));
        assertFalse(ZoneEdgeCondition.past(-27.0, width, 0.0));
        assertTrue(ZoneEdgeCondition.past(-27.0, width, 0.6));
    }
}
