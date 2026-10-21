package nl.timtoys.corruptedadventure.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@code [conditions.void]} (package 91): exactly the planned keys with the confirmed defaults (contract §11). */
class VoidConfigTest {
    private static final ServerConfig.VoidCondition VOID = ServerConfig.VOID;

    @Test
    void voidSectionHasThePlannedKeys() {
        assertPath("innerRise", VOID.innerRise);
        assertPath("outerRise", VOID.outerRise);
        assertPath("sourceRise", VOID.sourceRise);
        assertPath("sourceRadius", VOID.sourceRadius);
        assertPath("nightMultiplier", VOID.nightMultiplier);
        assertPath("lightLevel", VOID.lightLevel);
        assertPath("lightFall", VOID.lightFall);
        assertPath("outsideFall", VOID.outsideFall);
        assertPath("teleportMinBlocks", VOID.teleportMinBlocks);
        assertPath("teleportMaxBlocks", VOID.teleportMaxBlocks);
        assertPath("teleportIntervalSeconds", VOID.teleportIntervalSeconds);
        assertPath("darknessAmplifier", VOID.darknessAmplifier);
        assertPath("damage", VOID.damage);
        assertPath("damageInterval", VOID.damageInterval);
        // threshold1..3 through ThresholdLevels; together 17 keys in the section.
        UnmodifiableConfig section = ServerConfig.SPEC.getValues().get(List.of("conditions", "void"));
        assertEquals(17, section.size());
    }

    @Test
    void voidDefaults() {
        assertEquals(0.28, VOID.innerRise.getDefault());
        assertEquals(0.55, VOID.outerRise.getDefault());
        assertEquals(0.55, VOID.sourceRise.getDefault());
        assertEquals(4, VOID.sourceRadius.getDefault());
        assertEquals(1.5, VOID.nightMultiplier.getDefault());
        assertEquals(12, VOID.lightLevel.getDefault());
        assertEquals(1.0, VOID.lightFall.getDefault());
        assertEquals(1.0, VOID.outsideFall.getDefault());
        assertArrayEquals(new int[]{30, 60, 90}, VOID.thresholds.defaults());
        assertEquals(1, VOID.teleportMinBlocks.getDefault());
        assertEquals(5, VOID.teleportMaxBlocks.getDefault());
        assertEquals(20, VOID.teleportIntervalSeconds.getDefault());
        assertEquals(0, VOID.darknessAmplifier.getDefault());
        assertEquals(1.0, VOID.damage.getDefault());
        assertEquals(2, VOID.damageInterval.getDefault());
    }

    private static void assertPath(String key, ModConfigSpec.ConfigValue<?> value) {
        assertEquals(List.of("conditions", "void", key), value.getPath());
    }
}
