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

    @Test
    void inClearWeatherOnlyOurOwnHazeAlsoBeyondTheRenderDistance() {
        // Render distance 12 chunks: vanilla's fog from 172 to 192 blocks.
        assertEquals(0.0F, VolcanoPlumeRenderer.haze(0.0, 172.0F, 192.0F, 192.0F));
        assertEquals(0.45F, VolcanoPlumeRenderer.haze(VolcanoPlumeRenderer.DRAW_DISTANCE, 172.0F, 192.0F, 192.0F), 1e-6F);
        assertEquals(0.45F * 300.0F / 512.0F, VolcanoPlumeRenderer.haze(300.0, 172.0F, 192.0F, 192.0F), 1e-6F);
        assertTrue(VolcanoPlumeRenderer.haze(256.0, 172.0F, 192.0F, 192.0F) > VolcanoPlumeRenderer.haze(128.0, 172.0F, 192.0F, 192.0F));
    }

    @Test
    void inTheWeathersFogAPlumeIsFoggedLikeTheTerrain() {
        // An eruption at full strength: the fog ends at 0.6 of the view.
        float start = 0.0F;
        float end = 115.0F;
        assertEquals(1.0F, VolcanoPlumeRenderer.haze(150.0, start, end, 192.0F));
        assertEquals(0.5F, VolcanoPlumeRenderer.haze(57.5, start, end, 192.0F), 0.05F);
        // Halfway into the shortening the weather counts for half.
        float half = VolcanoPlumeRenderer.haze(400.0, 100.0F, 168.0F, 192.0F);
        assertEquals(0.5F, half, 1e-6F);
        // Never below our own haze.
        assertTrue(VolcanoPlumeRenderer.haze(500.0, 100.0F, 180.0F, 192.0F) >= 0.45F * 500.0F / 512.0F);
    }
}
