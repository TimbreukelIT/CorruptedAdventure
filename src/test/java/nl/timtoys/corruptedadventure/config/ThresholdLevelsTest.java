package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdLevelsTest {
    @Test
    void thresholdsMustRiseStrictly() {
        assertTrue(ThresholdLevels.ascending(new int[]{50, 75, 100}));
        assertTrue(ThresholdLevels.ascending(new int[]{1}));
        assertFalse(ThresholdLevels.ascending(new int[]{50, 50, 100}));
        assertFalse(ThresholdLevels.ascending(new int[]{75, 50, 100}));
    }

    @Test
    void badDefaultsAreAProgrammingError() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        assertThrows(IllegalArgumentException.class,
                () -> ThresholdLevels.define(builder, "test", new int[]{75, 50}, "a", "b"));
        assertThrows(IllegalArgumentException.class,
                () -> ThresholdLevels.define(builder, "test", new int[]{50, 75}, "only one comment"));
    }

    @Test
    void serverConfigDefinesTheDebugThresholds() {
        // Building the spec must not throw (rising defaults, one comment per threshold).
        assertTrue(ServerConfig.SPEC != null && ServerConfig.DEBUG.thresholds != null);
    }
}
