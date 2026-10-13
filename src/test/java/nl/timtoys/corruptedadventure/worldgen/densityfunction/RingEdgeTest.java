package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.config.RingSettings;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingEdgeTest {
    @Test
    void oneOnAndOutsideTheBoundaryZeroAtWidthInside() {
        assertEquals(1.0, RingEdge.value(0.0, 48.0));
        assertEquals(1.0, RingEdge.value(-30.0, 48.0));
        assertEquals(0.0, RingEdge.value(48.0, 48.0));
        assertEquals(0.0, RingEdge.value(200.0, 48.0));
        assertEquals(0.5, RingEdge.value(24.0, 48.0), 1e-12);
    }

    @Test
    void fallsMonotonically() {
        double previous = 2.0;
        for (double depth = -10.0; depth <= 60.0; depth += 0.5) {
            double value = RingEdge.value(depth, 48.0);
            assertTrue(value <= previous + 1e-12, "depth " + depth);
            previous = value;
        }
    }

    @Test
    void measuresToTheNearestOfBothBoundaries() {
        RingLayout layout = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), 1L);
        // Ring 2 runs from 1.500 to 3.500 (overworld blocks).
        assertEquals(100.0, layout.boundaryDepth(Ring.RING_2, 1600.0), 1e-9);
        assertEquals(100.0, layout.boundaryDepth(Ring.RING_2, 3400.0), 1e-9);
        assertEquals(-50.0, layout.boundaryDepth(Ring.RING_2, 1450.0), 1e-9);
        assertEquals(-50.0, layout.boundaryDepth(Ring.RING_2, 3550.0), 1e-9);
        assertEquals(500.0, layout.boundaryDepth(Ring.RING_2, 2000.0), 1e-9);
        // Ring 1 has no inner boundary.
        assertEquals(1500.0, layout.boundaryDepth(Ring.RING_1, 0.0), 1e-9);
    }

    @Test
    void codecRoundTrip() {
        var json = JsonParser.parseString("{\"dimension\":\"minecraft:the_nether\",\"ring\":\"ring_2\",\"width\":48.0}");
        RingEdge edge = RingEdge.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(new RingEdge(Level.NETHER, Ring.RING_2, 48.0), edge);
        assertEquals(json, RingEdge.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, edge).getOrThrow());
        assertTrue(RingEdge.MAP_CODEC.codec().parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"dimension\":\"minecraft:the_nether\",\"ring\":\"ring_2\",\"width\":0.0}")).isError());
    }
}
