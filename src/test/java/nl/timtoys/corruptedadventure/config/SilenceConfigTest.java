package nl.timtoys.corruptedadventure.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@code [conditions.silence]} (package 97): exactly the planned keys with the confirmed defaults (contract §11). */
class SilenceConfigTest {
    private static final ServerConfig.SilenceCondition SILENCE = ServerConfig.SILENCE;

    @Test
    void silenceSectionHasThePlannedKeys() {
        assertPath("rise", SILENCE.rise);
        assertPath("openMultiplier", SILENCE.openMultiplier);
        assertPath("companionRadius", SILENCE.companionRadius);
        assertPath("companionMultiplier", SILENCE.companionMultiplier);
        assertPath("soundReliefRadius", SILENCE.soundReliefRadius);
        assertPath("soundReliefSeconds", SILENCE.soundReliefSeconds);
        assertPath("soundFall", SILENCE.soundFall);
        assertPath("outsideFall", SILENCE.outsideFall);
        assertPath("resoundingFactor", SILENCE.resoundingFactor);
        assertPath("echoShellAmount", SILENCE.echoShellAmount);
        assertPath("echoShellResoundingSeconds", SILENCE.echoShellResoundingSeconds);
        assertPath("echoShellCooldownSeconds", SILENCE.echoShellCooldownSeconds);
        assertPath("darknessAmplifier", SILENCE.darknessAmplifier);
        assertPath("damage", SILENCE.damage);
        assertPath("damageInterval", SILENCE.damageInterval);
        // threshold1..3 through ThresholdLevels; together 18 keys in the section.
        UnmodifiableConfig section = ServerConfig.SPEC.getValues().get(List.of("conditions", "silence"));
        assertEquals(18, section.size());
    }

    @Test
    void silenceDefaults() {
        assertEquals(0.33, SILENCE.rise.getDefault());
        assertEquals(1.5, SILENCE.openMultiplier.getDefault());
        assertEquals(8, SILENCE.companionRadius.getDefault());
        assertEquals(0.5, SILENCE.companionMultiplier.getDefault());
        assertEquals(16, SILENCE.soundReliefRadius.getDefault());
        assertEquals(10, SILENCE.soundReliefSeconds.getDefault());
        assertEquals(2.0, SILENCE.soundFall.getDefault());
        assertEquals(1.0, SILENCE.outsideFall.getDefault());
        assertEquals(0.5, SILENCE.resoundingFactor.getDefault());
        assertEquals(20.0, SILENCE.echoShellAmount.getDefault());
        assertEquals(45, SILENCE.echoShellResoundingSeconds.getDefault());
        assertEquals(45, SILENCE.echoShellCooldownSeconds.getDefault());
        assertArrayEquals(new int[]{30, 60, 90}, SILENCE.thresholds.defaults());
        assertEquals(0, SILENCE.darknessAmplifier.getDefault());
        assertEquals(1.0, SILENCE.damage.getDefault());
        assertEquals(2, SILENCE.damageInterval.getDefault());
    }

    private static void assertPath(String key, ModConfigSpec.ConfigValue<?> value) {
        assertEquals(List.of("conditions", "silence", key), value.getPath());
    }
}
