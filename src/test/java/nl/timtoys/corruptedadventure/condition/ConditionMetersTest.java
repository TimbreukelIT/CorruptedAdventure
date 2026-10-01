package nl.timtoys.corruptedadventure.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionMetersTest {
    @Test
    void setAndAddClampToTheRange() {
        ConditionMeters meters = new ConditionMeters();
        meters.set(Condition.DEBUG, 150);
        assertEquals(100, meters.get(Condition.DEBUG));
        meters.add(Condition.DEBUG, -30.5f);
        assertEquals(69.5f, meters.get(Condition.DEBUG));
        meters.add(Condition.DEBUG, -200);
        assertEquals(0, meters.get(Condition.DEBUG));
        meters.set(Condition.DEBUG, Float.NaN);
        assertEquals(0, meters.get(Condition.DEBUG));
        meters.add(Condition.DEBUG, 0.25f);
        assertEquals(0.25f, meters.get(Condition.DEBUG));
    }

    @Test
    void syncOnlyWhenARoundedValueChanges() {
        ConditionMeters meters = new ConditionMeters();
        assertTrue(meters.roundedChanged(), "never synced yet");
        meters.set(Condition.DEBUG, 0.4f);
        meters.markSynced();
        assertFalse(meters.roundedChanged());
        meters.set(Condition.DEBUG, 0.6f);
        assertTrue(meters.roundedChanged(), "0.4 -> 0.6 rounds from 0 to 1");
        meters.markSynced();
        meters.set(Condition.DEBUG, 0.9f);
        assertFalse(meters.roundedChanged(), "0.6 -> 0.9 both round to 1");
    }

    @Test
    void applySyncTakesTheServerValues() {
        ConditionMeters client = new ConditionMeters();
        client.applySync(new int[]{42});
        assertEquals(42, client.rounded(Condition.DEBUG));
        assertFalse(client.roundedChanged());
        client.applySync(new int[]{7, 99});
        assertEquals(7, client.rounded(Condition.DEBUG), "extra entries are ignored");
    }

    @Test
    void codecRoundTrip() {
        ConditionMeters meters = new ConditionMeters();
        meters.set(Condition.DEBUG, 63.25f);
        JsonElement json = ConditionMeters.CODEC.encodeStart(JsonOps.INSTANCE, meters).getOrThrow();
        assertEquals(63.25f, json.getAsJsonObject().get("debug").getAsFloat());
        ConditionMeters decoded = ConditionMeters.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(63.25f, decoded.get(Condition.DEBUG));
    }

    @Test
    void unknownConditionIsSkippedAndTheRestKept() {
        JsonObject json = JsonParser.parseString("{\"removed_condition\": 80.0, \"debug\": 12.5}").getAsJsonObject();
        ConditionMeters decoded = ConditionMeters.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(12.5f, decoded.get(Condition.DEBUG));
    }

    @Test
    void loadedValuesAreClamped() {
        JsonObject json = JsonParser.parseString("{\"debug\": 250.0}").getAsJsonObject();
        assertEquals(100, ConditionMeters.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().get(Condition.DEBUG));
    }

    @Test
    void appliedThresholdsStartUnknown() {
        assertEquals(ConditionMeters.UNKNOWN, new ConditionMeters().appliedThresholds(Condition.DEBUG));
    }
}
