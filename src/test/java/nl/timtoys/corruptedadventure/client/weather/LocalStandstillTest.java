package nl.timtoys.corruptedadventure.client.weather;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalStandstillTest {
    @Test
    void fullInsideThreeQuartersThenThinningToNothingAtTheRadius() {
        assertEquals(1.0F, LocalStandstill.falloff(0.0, 48.0));
        assertEquals(1.0F, LocalStandstill.falloff(36.0, 48.0));
        assertEquals(0.5F, LocalStandstill.falloff(42.0, 48.0), 1e-6F);
        assertEquals(0.0F, LocalStandstill.falloff(48.0, 48.0));
        assertEquals(0.0F, LocalStandstill.falloff(100.0, 48.0));
    }
}
