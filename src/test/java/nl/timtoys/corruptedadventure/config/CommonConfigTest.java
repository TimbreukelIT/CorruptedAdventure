package nl.timtoys.corruptedadventure.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonConfigTest {
    @Test
    void defaultsAreTheWiderRing3LayoutAndValid() {
        assertEquals(List.of(1500, 3500, 6000, 7800, 9500), CommonConfig.DEFAULT_OUTER_RADII);
        RingSettings defaults = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10000, 96, 128);
        assertTrue(defaults.validate().isSuccess(), () -> defaults.validate().error().orElseThrow().message());
    }

    @Test
    void onlyTheOldDefaultsAreMigrated() {
        assertEquals(CommonConfig.DEFAULT_OUTER_RADII, CommonConfig.migrated(List.of(1500, 3500, 5500, 7500, 9500)));
        List<Integer> custom = List.of(1500, 3500, 5500, 7600, 9500);
        assertSame(custom, CommonConfig.migrated(custom));
        assertSame(CommonConfig.DEFAULT_OUTER_RADII, CommonConfig.migrated(CommonConfig.DEFAULT_OUTER_RADII));
    }
}
