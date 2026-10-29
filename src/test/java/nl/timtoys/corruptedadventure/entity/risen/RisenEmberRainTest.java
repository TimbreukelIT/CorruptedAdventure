package nl.timtoys.corruptedadventure.entity.risen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ember rain's beat: no volley during the windup, the first right after it, then every {@code volley} ticks. */
class RisenEmberRainTest {
    @Test
    void volleysStartRightAfterTheWindupAndKeepTheirBeat() {
        assertFalse(RisenEmberRain.isVolley(30, 30, 4), "still the windup");
        assertTrue(RisenEmberRain.isVolley(31, 30, 4), "first volley");
        assertFalse(RisenEmberRain.isVolley(32, 30, 4));
        assertTrue(RisenEmberRain.isVolley(35, 30, 4), "next volley");
    }

    @Test
    void aVolleyEveryTickDropsEveryTick() {
        int volleys = 0;
        for (int tick = 1; tick <= 20; tick++) {
            if (RisenEmberRain.isVolley(tick, 10, 1)) {
                volleys++;
            }
        }
        assertEquals(10, volleys);
    }
}
