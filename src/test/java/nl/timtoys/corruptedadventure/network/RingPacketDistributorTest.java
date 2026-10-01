package nl.timtoys.corruptedadventure.network;

import nl.timtoys.corruptedadventure.network.RingPacketDistributor.RingMembership;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static nl.timtoys.corruptedadventure.network.RingPacketDistributor.matches;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingPacketDistributorTest {
    private static final RingPosition MIDDLE_OF_RING_2 = RingPosition.within(Ring.RING_2);
    private static final RingPosition ZONE_2_3_INNER_SIDE = new RingPosition(Ring.RING_2, Ring.RING_3, 0.2);
    private static final RingPosition ZONE_2_3_JUST_INSIDE = new RingPosition(Ring.RING_2, Ring.RING_3, 0.49);
    private static final RingPosition ZONE_2_3_ON_BOUNDARY = new RingPosition(Ring.RING_2, Ring.RING_3, 0.5);
    private static final RingPosition ZONE_2_3_OUTER_SIDE = new RingPosition(Ring.RING_2, Ring.RING_3, 0.8);
    private static final RingPosition ZONE_1_2 = new RingPosition(Ring.RING_1, Ring.RING_2, 0.3);

    @Test
    void coreFollowsTheHardSwitchAtHalfWeight() {
        assertTrue(matches(MIDDLE_OF_RING_2, Ring.RING_2, RingMembership.CORE));
        assertTrue(matches(ZONE_2_3_INNER_SIDE, Ring.RING_2, RingMembership.CORE));
        assertTrue(matches(ZONE_2_3_JUST_INSIDE, Ring.RING_2, RingMembership.CORE));
        assertFalse(matches(ZONE_2_3_ON_BOUNDARY, Ring.RING_2, RingMembership.CORE));
        assertTrue(matches(ZONE_2_3_ON_BOUNDARY, Ring.RING_3, RingMembership.CORE));
        assertFalse(matches(ZONE_2_3_OUTER_SIDE, Ring.RING_2, RingMembership.CORE));
        assertTrue(matches(ZONE_2_3_OUTER_SIDE, Ring.RING_3, RingMembership.CORE));
        assertFalse(matches(MIDDLE_OF_RING_2, Ring.RING_3, RingMembership.CORE));
    }

    @Test
    void touchingCountsBothSidesOfAZone() {
        assertTrue(matches(MIDDLE_OF_RING_2, Ring.RING_2, RingMembership.TOUCHING));
        for (RingPosition zone : new RingPosition[]{ZONE_2_3_INNER_SIDE, ZONE_2_3_ON_BOUNDARY, ZONE_2_3_OUTER_SIDE}) {
            assertTrue(matches(zone, Ring.RING_2, RingMembership.TOUCHING));
            assertTrue(matches(zone, Ring.RING_3, RingMembership.TOUCHING));
            assertFalse(matches(zone, Ring.RING_1, RingMembership.TOUCHING));
            assertFalse(matches(zone, Ring.RING_4, RingMembership.TOUCHING));
        }
    }

    @Test
    void neighbouringRingWithoutZoneDoesNotCount() {
        for (RingMembership membership : RingMembership.values()) {
            assertFalse(matches(MIDDLE_OF_RING_2, Ring.RING_1, membership));
            assertFalse(matches(MIDDLE_OF_RING_2, Ring.RING_3, membership));
            // In the zone 1 → 2, ring 3 is not touched.
            assertFalse(matches(ZONE_1_2, Ring.RING_3, membership));
        }
        assertTrue(matches(ZONE_1_2, Ring.RING_2, RingMembership.TOUCHING));
        assertFalse(matches(ZONE_1_2, Ring.RING_2, RingMembership.CORE));
    }
}
