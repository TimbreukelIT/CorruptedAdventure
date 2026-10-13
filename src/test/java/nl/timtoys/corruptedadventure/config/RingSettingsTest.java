package nl.timtoys.corruptedadventure.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingSettingsTest {
    private static final RingSettings DEFAULTS = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);

    @Test
    void aSnapshotWithoutOverridesGivesEveryBoundaryTheBaseWidth() {
        // Written before change 58 (37c defaults).
        JsonElement old = JsonParser.parseString("""
                {"ring_outer_radii": [1500, 3500, 6000, 7800, 9500], "void_outer_radius": 10000,
                 "boundary_noise": 96, "transition_width": 128}""");
        RingSettings settings = RingSettings.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
        assertEquals(RingSettings.NO_OVERRIDES, settings.transitionWidthOverrides());
        for (int boundary = 0; boundary < RingSettings.BOUNDARY_COUNT; boundary++) {
            assertEquals(128, settings.transitionWidth(boundary));
        }
    }

    @Test
    void roundTripWritesAndKeepsTheOverrides() {
        JsonElement json = RingSettings.CODEC.encodeStart(JsonOps.INSTANCE, DEFAULTS).getOrThrow();
        assertEquals(JsonParser.parseString("[0, 0, 0, 256, 0]"), json.getAsJsonObject().get("transition_width_overrides"));
        RingSettings decoded = RingSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(DEFAULTS, decoded);
        assertEquals(List.of(128, 128, 128, 256, 128),
                List.of(decoded.transitionWidth(0), decoded.transitionWidth(1), decoded.transitionWidth(2),
                        decoded.transitionWidth(3), decoded.transitionWidth(4)));
    }

    @Test
    void anOldSnapshotIsWrittenBackWithAllZeroOverrides() {
        RingSettings old = new RingSettings(List.of(1500, 3500, 6000, 7800, 9500), 10000, 96, 128);
        JsonElement json = RingSettings.CODEC.encodeStart(JsonOps.INSTANCE, old).getOrThrow();
        assertEquals(JsonParser.parseString("[0, 0, 0, 0, 0]"), json.getAsJsonObject().get("transition_width_overrides"));
    }

    @Test
    void overridesMustBeFiveNonNegativeValues() {
        assertError(withOverrides("[0, 0, 0, 256]"), "Expected 5 transition_width_overrides");
        assertError(withOverrides("[0, 0, 0, 256, 0, 0]"), "Expected 5 transition_width_overrides");
        assertError(withOverrides("[0, 0, -1, 256, 0]"), "must be >= 0");
    }

    @Test
    void eachBandMustBeWiderThanTheReachOfItsOwnBoundaries() {
        // Ring 5 is 1700 wide: boundary 4|5 reaches 96 + 128 into it, 5|void 96 + 64; together 384.
        assertTrue(DEFAULTS.validate().isSuccess());
        // 5|void at 2 * 1100 = 2200: 96 + 1100 + 96 + 128 = 1420 still fits ring 5, but not the 500 wide void edge.
        RingSettings wideVoidZone = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128, List.of(0, 0, 0, 256, 2200));
        assertError(wideVoidZone.validate(), "void edge (9700 - 10200) must be wider than 2392 blocks");
        // Ring 5 (1700 wide) against a 4|5 zone of 3000: 96 + 1500 + 96 + 64 = 1756.
        RingSettings wideRing5Zone = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 20000, 96, 128, List.of(0, 0, 0, 3000, 0));
        assertError(wideRing5Zone.validate(), "ring 5 (8000 - 9700) must be wider than 1756 blocks");
        // The same override on 1|2 instead fails in ring 1, which only has its outer boundary: 96 + 1500.
        RingSettings wideRing1Zone = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128, List.of(3000, 0, 0, 0, 0));
        assertError(wideRing1Zone.validate(), "ring 1 (0 - 1500) must be wider than 1596 blocks");
    }

    private static DataResult<RingSettings> withOverrides(String overrides) {
        JsonObject json = RingSettings.CODEC.encodeStart(JsonOps.INSTANCE, DEFAULTS).getOrThrow().getAsJsonObject();
        json.add("transition_width_overrides", JsonParser.parseString(overrides));
        return RingSettings.CODEC.parse(JsonOps.INSTANCE, json);
    }

    private static void assertError(DataResult<RingSettings> result, String message) {
        assertTrue(result.isError(), "expected an error containing: " + message);
        String actual = result.error().orElseThrow().message();
        assertTrue(actual.contains(message), actual);
    }
}
