package nl.timtoys.corruptedadventure.weather;

import nl.timtoys.corruptedadventure.condition.Condition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatWaveDehydrationModifierTest {
    private static final int DURATION = 6000;

    /** A heat wave at intensity {@code intensity} (fade-in part of a long weather). */
    private static RingWeatherState heatWave(float intensity) {
        int elapsed = Math.round(intensity * RingWeatherState.FADE_TICKS);
        return new RingWeatherState(RingWeatherType.HEAT_WAVE, DURATION - elapsed, DURATION, 7);
    }

    @Test
    void factorFollowsTheFade() {
        assertEquals(1.0F, HeatWaveDehydrationModifier.factor(heatWave(0.0F), 2.0F), 1.0E-6F);
        assertEquals(1.5F, HeatWaveDehydrationModifier.factor(heatWave(0.5F), 2.0F), 1.0E-6F);
        assertEquals(2.0F, HeatWaveDehydrationModifier.factor(heatWave(1.0F), 2.0F), 1.0E-6F);
        assertEquals(3.0F, HeatWaveDehydrationModifier.factor(heatWave(1.0F), 3.0F), 1.0E-6F);
    }

    @Test
    void risingDehydrationIsMultiplied() {
        assertEquals(0.8F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, 0.4F, heatWave(1.0F), 2.0F), 1.0E-6F);
        assertEquals(0.6F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, 0.4F, heatWave(0.5F), 2.0F), 1.0E-6F);
        assertEquals(0.4F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, 0.4F, heatWave(0.0F), 2.0F), 1.0E-6F);
    }

    @Test
    void fallingOrZeroRateStays() {
        assertEquals(-0.5F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, -0.5F, heatWave(1.0F), 2.0F));
        assertEquals(-2.0F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, -2.0F, heatWave(1.0F), 2.0F));
        assertEquals(0.0F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, 0.0F, heatWave(1.0F), 2.0F));
    }

    @Test
    void otherConditionsStay() {
        assertEquals(5.0F, HeatWaveDehydrationModifier.apply(Condition.DEBUG, 5.0F, heatWave(1.0F), 2.0F));
    }

    @Test
    void otherWeatherStays() {
        RingWeatherState sandstorm = RingWeatherState.start(RingWeatherType.SANDSTORM, DURATION, 3).countDown(3000);
        RingWeatherState clear = RingWeatherState.start(RingWeatherType.CLEAR, DURATION, 0).countDown(3000);
        assertEquals(0.4F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, 0.4F, sandstorm, 2.0F));
        assertEquals(0.4F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, 0.4F, clear, 2.0F));
        assertEquals(0.4F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, 0.4F, RingWeatherState.STILL, 2.0F));
    }

    @Test
    void multiplierOneNeverChangesAnything() {
        for (float intensity : new float[]{0.0F, 0.3F, 1.0F}) {
            assertEquals(0.4F, HeatWaveDehydrationModifier.apply(Condition.DEHYDRATION, 0.4F, heatWave(intensity), 1.0F));
        }
    }
}
