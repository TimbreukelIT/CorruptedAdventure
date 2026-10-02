package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The thresholds {@code threshold1..N} of one condition section, 1–100 each. They must rise strictly; if not, the
 * defaults are used (and {@link #validate} logs it once per load).
 */
public final class ThresholdLevels {
    private final String section;
    private final int[] defaults;
    private final List<ModConfigSpec.IntValue> values = new ArrayList<>();

    private ThresholdLevels(String section, int[] defaults) {
        this.section = section;
        this.defaults = defaults.clone();
    }

    /** Defines {@code threshold1..N} in the builder's current section; {@code comments[i]} describes threshold i+1. */
    static ThresholdLevels define(ModConfigSpec.Builder builder, String section, int[] defaults, String... comments) {
        if (!ascending(defaults) || comments.length != defaults.length) {
            throw new IllegalArgumentException("Bad threshold defaults for " + section);
        }
        ThresholdLevels levels = new ThresholdLevels(section, defaults);
        for (int i = 0; i < defaults.length; i++) {
            levels.values.add(builder
                    .comment(comments[i] + " Thresholds must rise from threshold1 to threshold" + defaults.length + ".")
                    .defineInRange("threshold" + (i + 1), defaults[i], 1, 100));
        }
        return levels;
    }

    /** The configured levels, or the defaults if they do not rise strictly. */
    public int[] get() {
        int[] configured = values.stream().mapToInt(ModConfigSpec.IntValue::get).toArray();
        return ascending(configured) ? configured : defaults.clone();
    }

    int[] defaults() {
        return defaults.clone();
    }

    void validate() {
        int[] configured = values.stream().mapToInt(ModConfigSpec.IntValue::get).toArray();
        if (!ascending(configured)) {
            CorruptedAdventure.LOGGER.error("Thresholds of [{}] do not rise ({}); using the defaults {}",
                    section, Arrays.toString(configured), Arrays.toString(defaults));
        }
    }

    static boolean ascending(int[] levels) {
        for (int i = 1; i < levels.length; i++) {
            if (levels[i] <= levels[i - 1]) {
                return false;
            }
        }
        return true;
    }
}
