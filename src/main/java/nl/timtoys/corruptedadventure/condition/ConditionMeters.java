package nl.timtoys.corruptedadventure.condition;

import com.mojang.serialization.Codec;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Player attachment: one meter 0–100 per {@link Condition}. Floats, because rates can be fractional (0.25 per second).
 * On the server this is the truth (saved with the player); on the client it only holds the last synced, rounded values.
 * <p>
 * Besides the meters it keeps two transient things that are not saved: the rounded values last sent to the client, and
 * per condition how many thresholds were applied last time ({@link #UNKNOWN} after loading, so the first tick clears
 * every threshold that is not reached).
 */
public final class ConditionMeters {
    public static final float MIN = 0;
    public static final float MAX = 100;
    static final int UNKNOWN = -1;

    private static final Condition[] CONDITIONS = Condition.values();

    /**
     * Name → value. Lenient on purpose: an unknown name (a condition renamed or removed in a later version) is skipped,
     * instead of failing the whole attachment and wiping every meter of the player.
     */
    public static final Codec<ConditionMeters> CODEC = Codec.unboundedMap(Codec.STRING, Codec.FLOAT)
            .xmap(ConditionMeters::fromMap, ConditionMeters::toMap);

    private final float[] values = new float[CONDITIONS.length];
    private final int[] lastSynced = new int[CONDITIONS.length];
    private final int[] appliedThresholds = new int[CONDITIONS.length];

    public ConditionMeters() {
        Arrays.fill(lastSynced, -1);
        Arrays.fill(appliedThresholds, UNKNOWN);
    }

    public float get(Condition condition) {
        return values[condition.ordinal()];
    }

    /** Sets the meter, clamped to 0–100 (NaN counts as 0). */
    public void set(Condition condition, float value) {
        values[condition.ordinal()] = clamp(value);
    }

    public void add(Condition condition, float delta) {
        set(condition, get(condition) + delta);
    }

    /** The value as the client sees it. */
    public int rounded(Condition condition) {
        return Math.round(get(condition));
    }

    /** All rounded values in {@link Condition#values()} order, as sent to the client. */
    public int[] roundedValues() {
        int[] rounded = new int[CONDITIONS.length];
        for (Condition condition : CONDITIONS) {
            rounded[condition.ordinal()] = rounded(condition);
        }
        return rounded;
    }

    /** True if a rounded value differs from the last {@link #markSynced} (always true before the first sync). */
    public boolean roundedChanged() {
        return !Arrays.equals(roundedValues(), lastSynced);
    }

    public void markSynced() {
        System.arraycopy(roundedValues(), 0, lastSynced, 0, lastSynced.length);
    }

    /** Client side: takes over the server's rounded values (missing or extra entries are ignored). */
    public void applySync(int[] rounded) {
        for (int i = 0; i < Math.min(rounded.length, CONDITIONS.length); i++) {
            set(CONDITIONS[i], rounded[i]);
        }
        markSynced();
    }

    int appliedThresholds(Condition condition) {
        return appliedThresholds[condition.ordinal()];
    }

    void setAppliedThresholds(Condition condition, int count) {
        appliedThresholds[condition.ordinal()] = count;
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return MIN;
        }
        return Math.max(MIN, Math.min(MAX, value));
    }

    private static ConditionMeters fromMap(Map<String, Float> map) {
        ConditionMeters meters = new ConditionMeters();
        map.forEach((name, value) -> {
            Condition condition = byName(name);
            if (condition == null) {
                CorruptedAdventure.LOGGER.warn("Dropping the meter of unknown condition '{}'", name);
            } else {
                meters.set(condition, value);
            }
        });
        return meters;
    }

    private static Map<String, Float> toMap(ConditionMeters meters) {
        Map<String, Float> map = new LinkedHashMap<>();
        for (Condition condition : CONDITIONS) {
            map.put(condition.getSerializedName(), meters.get(condition));
        }
        return map;
    }

    private static Condition byName(String name) {
        for (Condition condition : CONDITIONS) {
            if (condition.getSerializedName().equals(name)) {
                return condition;
            }
        }
        return null;
    }
}
