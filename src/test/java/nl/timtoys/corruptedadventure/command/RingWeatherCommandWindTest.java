package nl.timtoys.corruptedadventure.command;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import nl.timtoys.corruptedadventure.weather.SandstormWind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code /ca weather} shows the wind of the sandstorm and of ring 4's storm (package 70 review). */
class RingWeatherCommandWindTest {
    @Test
    void stormWindShowsItsDirection() {
        RingWeatherState storm = RingWeatherState.start(RingWeatherType.STORM_WIND, 12000, 99);
        assertTrue(RingWeatherCommand.describe(Ring.RING_4, storm).endsWith(", wind " + SandstormWind.describe(storm)));
        RingWeatherState sandstorm = RingWeatherState.start(RingWeatherType.SANDSTORM, 12000, 99);
        assertTrue(RingWeatherCommand.describe(Ring.RING_2, sandstorm).contains(", wind "));
        RingWeatherState fog = RingWeatherState.start(RingWeatherType.DENSE_FOG, 12000, 99);
        assertFalse(RingWeatherCommand.describe(Ring.RING_4, fog).contains("wind"));
    }
}
