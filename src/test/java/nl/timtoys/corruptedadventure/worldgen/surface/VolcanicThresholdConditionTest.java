package nl.timtoys.corruptedadventure.worldgen.surface;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code volcanic_threshold}: the codec accepts only per-column outputs and a real range; the ring 3 surface uses it. */
class VolcanicThresholdConditionTest {
    @Test
    void codecReadsAColumnOutputAndItsRange() {
        VolcanicThresholdCondition condition = decode("{\"output\": \"lava_channel\", \"min\": 0.02, \"max\": 2.0}");
        assertEquals(new VolcanicThresholdCondition(VolcanicOutput.Output.LAVA_CHANNEL, 0.02, 2.0), condition);
    }

    @Test
    void codecRejectsPerBlockOutputsAndEmptyRanges() {
        for (String output : new String[]{"guard", "islands", "initial_height"}) {
            assertTrue(VolcanicThresholdCondition.MAP_CODEC.codec()
                    .parse(JsonOps.INSTANCE, JsonParser.parseString("{\"output\": \"" + output + "\", \"min\": 0, \"max\": 1}"))
                    .error().isPresent(), output);
        }
        assertTrue(VolcanicThresholdCondition.MAP_CODEC.codec()
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{\"output\": \"caldera_mask\", \"min\": 1, \"max\": 1}"))
                .error().isPresent());
    }

    /** Every use in the overworld surface rules decodes. */
    @Test
    void everyUseInTheSurfaceRulesDecodes() {
        List<JsonObject> uses = new ArrayList<>();
        collect(WorldgenData.read("noise_settings/overworld.json").get("surface_rule"), uses);
        assertFalse(uses.isEmpty());
        for (JsonObject use : uses) {
            JsonObject fields = use.deepCopy();
            fields.remove("type");
            decode(fields.toString());
        }
        assertFalse(WorldgenData.read("noise_settings/nether.json").toString().contains("volcanic_threshold"));
    }

    private static VolcanicThresholdCondition decode(String json) {
        return VolcanicThresholdCondition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static void collect(JsonElement element, List<JsonObject> out) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("type") && object.get("type").getAsString().equals("corruptedadventure:volcanic_threshold")) {
                out.add(object);
            }
            object.entrySet().forEach(entry -> collect(entry.getValue(), out));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collect(child, out));
        }
    }
}
