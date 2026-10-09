package nl.timtoys.corruptedadventure.client.weather;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VolcanoPlumeRendererTest {
    @Test
    void farFormGrowsSmoothlyWithDistance() {
        assertEquals(0.0F, VolcanoPlumeRenderer.far(0.0));
        assertEquals(0.0F, VolcanoPlumeRenderer.far(160.0));
        assertEquals(1.0F, VolcanoPlumeRenderer.far(288.0));
        assertEquals(1.0F, VolcanoPlumeRenderer.far(VolcanoPlumeRenderer.DRAW_DISTANCE));
        float previous = 0.0F;
        for (double distance = 160.0; distance <= 288.0; distance += 4.0) {
            float far = VolcanoPlumeRenderer.far(distance);
            assertTrue(far >= previous && far - previous < 0.06F, "no jump at " + distance);
            previous = far;
        }
    }
}
