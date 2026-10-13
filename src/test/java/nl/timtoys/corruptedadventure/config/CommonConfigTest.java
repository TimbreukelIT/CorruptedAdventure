package nl.timtoys.corruptedadventure.config;

import nl.timtoys.corruptedadventure.config.CommonConfig.OuterBoundaries;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonConfigTest {
    private static final OuterBoundaries PRE_37C = new OuterBoundaries(List.of(1500, 3500, 5500, 7500, 9500), 10000);
    private static final OuterBoundaries FROM_37C = new OuterBoundaries(List.of(1500, 3500, 6000, 7800, 9500), 10000);

    @Test
    void defaultsAreTheRing4LayoutAndValid() {
        assertEquals(List.of(1500, 3500, 6000, 8000, 9700), CommonConfig.DEFAULT_OUTER_RADII);
        assertEquals(10200, CommonConfig.DEFAULT_VOID_OUTER_RADIUS);
        assertEquals(List.of(0, 0, 0, 256, 0), CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
        RingSettings defaults = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
        assertTrue(defaults.validate().isSuccess(), () -> defaults.validate().error().orElseThrow().message());
    }

    @Test
    void bothOldDefaultSetsMoveToTheNewDefaultsIncludingTheVoidEdge() {
        OuterBoundaries expected = new OuterBoundaries(CommonConfig.DEFAULT_OUTER_RADII, 10200);
        assertEquals(expected, CommonConfig.migrated(PRE_37C));
        assertEquals(expected, CommonConfig.migrated(FROM_37C));
    }

    @Test
    void otherValuesStay() {
        // Old radii with a changed void edge, or an old void edge with changed radii: the user's choice.
        List<OuterBoundaries> custom = List.of(
                new OuterBoundaries(FROM_37C.ringOuterRadii(), 10400),
                new OuterBoundaries(PRE_37C.ringOuterRadii(), 10200),
                new OuterBoundaries(List.of(1500, 3500, 6000, 7600, 9500), 10000),
                new OuterBoundaries(List.of(1500, 3500, 5500, 7600, 9500), 10000));
        for (OuterBoundaries boundaries : custom) {
            assertSame(boundaries, CommonConfig.migrated(boundaries));
        }
        assertSame(CommonConfig.DEFAULT_BOUNDARIES, CommonConfig.migrated(CommonConfig.DEFAULT_BOUNDARIES));
    }
}
