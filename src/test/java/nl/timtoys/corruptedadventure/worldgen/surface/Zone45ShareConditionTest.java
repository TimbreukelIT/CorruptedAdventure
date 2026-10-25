package nl.timtoys.corruptedadventure.worldgen.surface;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code zone_4_5_share}: from the share across the zone 4→5 up to and beyond its outer edge (walk 1 review). */
class Zone45ShareConditionTest {
    @Test
    void theZoneFromTheShareOn() {
        assertFalse(Zone45ShareCondition.reached(new RingPosition(Ring.RING_4, Ring.RING_5, 0.49), 0.5));
        assertTrue(Zone45ShareCondition.reached(new RingPosition(Ring.RING_4, Ring.RING_5, 0.5), 0.5));
        assertTrue(Zone45ShareCondition.reached(new RingPosition(Ring.RING_4, Ring.RING_5, 0.999), 0.75));
    }

    @Test
    void beyondTheZoneAlways() {
        // The outer edge of sundered_reach (the biome reaches a little past the zone): no strip of bare coarse dirt.
        assertTrue(Zone45ShareCondition.reached(RingPosition.within(Ring.RING_5), 0.75));
        assertTrue(Zone45ShareCondition.reached(new RingPosition(Ring.RING_5, Ring.VOID, 0.2), 0.75));
        assertTrue(Zone45ShareCondition.reached(RingPosition.within(Ring.VOID), 0.75));
    }

    @Test
    void neverOnRing4sSideOrElsewhere() {
        assertFalse(Zone45ShareCondition.reached(RingPosition.within(Ring.RING_4), 0.0));
        assertFalse(Zone45ShareCondition.reached(new RingPosition(Ring.RING_3, Ring.RING_4, 0.9), 0.0));
    }
}
