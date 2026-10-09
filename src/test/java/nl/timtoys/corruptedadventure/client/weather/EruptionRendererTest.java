package nl.timtoys.corruptedadventure.client.weather;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EruptionRendererTest {
    @Test
    void theRedIsFullInTheFirstHalfOfTheFade() {
        assertEquals(0.0F, EruptionRenderer.colourAmount(0.0F));
        assertEquals(0.5F, EruptionRenderer.colourAmount(0.25F), 1e-6F);
        assertEquals(1.0F, EruptionRenderer.colourAmount(0.5F));
        assertEquals(1.0F, EruptionRenderer.colourAmount(1.0F));
        assertEquals(0.0F, EruptionRenderer.colourAmount(-0.1F));
    }
}
