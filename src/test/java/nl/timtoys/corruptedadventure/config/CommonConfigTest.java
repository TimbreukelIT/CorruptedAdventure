package nl.timtoys.corruptedadventure.config;

import nl.timtoys.corruptedadventure.config.CommonConfig.OuterBoundaries;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonConfigTest {
    private static final OuterBoundaries PRE_37C = new OuterBoundaries(List.of(1500, 3500, 5500, 7500, 9500), 10000);
    private static final OuterBoundaries FROM_37C = new OuterBoundaries(List.of(1500, 3500, 6000, 7800, 9500), 10000);

    @Test
    void defaultsAreTheRing4LayoutAndValid() {
        assertEquals(List.of(1500, 3500, 6000, 8000, 9700), CommonConfig.DEFAULT_OUTER_RADII);
        assertEquals(10200, CommonConfig.DEFAULT_VOID_OUTER_RADIUS);
        assertEquals(List.of(0, 0, 256, 256, 0), CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
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

    @Test
    void theOldDefaultOverridesMoveToTheNewDefault() {
        assertEquals(List.of(0, 0, 0, 256, 0), CommonConfig.OLD_DEFAULT_TRANSITION_WIDTH_OVERRIDES);
        RingSettings old = settings(CommonConfig.DEFAULT_OUTER_RADII, 128, CommonConfig.OLD_DEFAULT_TRANSITION_WIDTH_OVERRIDES);
        RingSettings migrated = CommonConfig.migratedOverrides(old);
        assertEquals(CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES, migrated.transitionWidthOverrides());
        assertEquals(old.ringOuterRadii(), migrated.ringOuterRadii());
        assertEquals(old.voidOuterRadius(), migrated.voidOuterRadius());
        assertEquals(old.boundaryNoise(), migrated.boundaryNoise());
        assertEquals(old.defaultTransitionWidth(), migrated.defaultTransitionWidth());
        // Only the list counts: other radii or another transitionWidth beside it are migrated as well.
        RingSettings custom = settings(List.of(1500, 3500, 6200, 8000, 9700), 160,
                CommonConfig.OLD_DEFAULT_TRANSITION_WIDTH_OVERRIDES);
        assertEquals(CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES,
                CommonConfig.migratedOverrides(custom).transitionWidthOverrides());
    }

    @Test
    void otherOverridesStay() {
        List<List<Integer>> custom = List.of(
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES,
                List.of(0, 0, 128, 256, 0),
                List.of(0, 0, 0, 0, 0),
                List.of(0, 0, 0, 320, 0),
                List.of(64, 0, 0, 256, 0));
        for (List<Integer> overrides : custom) {
            RingSettings settings = settings(CommonConfig.DEFAULT_OUTER_RADII, 128, overrides);
            assertSame(settings, CommonConfig.migratedOverrides(settings));
        }
    }

    @Test
    void theOldDefaultOverridesStayWhenTheWiderZoneDoesNotFit() {
        // Ring 3 of 350 blocks: zone 2|3 reaches 96 + 64 = 160 into it, zone 3|4 160 at 128 wide (320 < 350, valid)
        // but 96 + 128 = 224 at 256 wide (384 > 350, invalid).
        RingSettings old = settings(List.of(1500, 3500, 3850, 8000, 9700), 128,
                CommonConfig.OLD_DEFAULT_TRANSITION_WIDTH_OVERRIDES);
        assertTrue(old.validate().isSuccess(), () -> old.validate().error().orElseThrow().message());
        assertFalse(settings(old.ringOuterRadii(), 128, CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES).validate().isSuccess());
        assertSame(old, CommonConfig.migratedOverrides(old));
    }

    private static RingSettings settings(List<Integer> radii, int transitionWidth, List<Integer> overrides) {
        return new RingSettings(radii, 10200, 96, transitionWidth, overrides);
    }
}
