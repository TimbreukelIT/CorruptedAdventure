package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlopeFilterTest {
    /** A canyon wall: plateau (Y 130) for x < 0, floor (Y 70) from x = 0, with a two-block bump at x = 10, z = 0. */
    private static final SlopeFilter.Heights WALL = (x, z) -> x < 0 ? 130 : x == 10 && z == 0 ? 72 : 70;

    @Test
    void riseFindsTheFootOfAWallNotItsTop() {
        for (int x = 0; x <= 2; x++) {
            assertEquals(60, SlopeFilter.measure(SlopeFilter.Measure.RISE, 3, x, 5, WALL), "foot at x " + x);
        }
        assertEquals(0, SlopeFilter.measure(SlopeFilter.Measure.RISE, 3, 3, 5, WALL), "the disc of radius 3 ends at x 0");
        assertEquals(0, SlopeFilter.measure(SlopeFilter.Measure.RISE, 3, -1, 5, WALL), "top edge");
        assertEquals(60, SlopeFilter.measure(SlopeFilter.Measure.RELIEF, 3, -1, 5, WALL), "relief sees the drop");
    }

    @Test
    void reliefFindsFlatGround() {
        assertEquals(0, SlopeFilter.measure(SlopeFilter.Measure.RELIEF, 2, 20, 20, WALL));
        assertEquals(2, SlopeFilter.measure(SlopeFilter.Measure.RELIEF, 2, 9, 0, WALL), "bump beside");
        assertEquals(2, SlopeFilter.measure(SlopeFilter.Measure.RELIEF, 2, 10, 0, WALL), "on the bump");
        assertEquals(0, SlopeFilter.measure(SlopeFilter.Measure.RISE, 2, 10, 0, WALL), "nothing higher than the bump");
    }

    @Test
    void discNotSquare() {
        // (2, 2) is outside a disc of radius 2: a single high column there is not seen.
        SlopeFilter.Heights spike = (x, z) -> x == 2 && z == 2 ? 90 : 70;
        assertEquals(0, SlopeFilter.measure(SlopeFilter.Measure.RISE, 2, 0, 0, spike));
        assertEquals(20, SlopeFilter.measure(SlopeFilter.Measure.RISE, 3, 0, 0, spike));
    }

    @Test
    void codec() {
        assertTrue(SlopeFilter.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{\"radius\":3,\"measure\":\"rise\",\"min\":6}")).isSuccess());
        assertFalse(SlopeFilter.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{\"radius\":3,\"min\":6,\"max\":2}")).isSuccess());
        assertFalse(SlopeFilter.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{\"radius\":9}")).isSuccess());
        assertEquals("{\"radius\":2,\"max\":2}",
                SlopeFilter.CODEC.codec().encodeStart(JsonOps.INSTANCE, SlopeFilter.flat(2, 2)).getOrThrow().toString());
    }
}
