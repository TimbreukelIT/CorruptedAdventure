package nl.timtoys.corruptedadventure.weather;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Package 70: the ring 4 weather types, the band rule per type, the one cycle and the storm's push. */
class Ring4WeatherTest {
    private static final RingLayout LAYOUT = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(20260930L));
    private static final Ring4Band BAND = Ring4Band.of(LAYOUT);

    @Test
    void ring4HasFogRainAndStorm() {
        assertEquals(List.of(RingWeatherType.DENSE_FOG, RingWeatherType.TOXIC_RAIN, RingWeatherType.STORM_WIND),
                RingWeatherType.typesFor(Level.OVERWORLD, Ring.RING_4));
        assertEquals("storm_wind", RingWeatherType.STORM_WIND.getSerializedName());
        assertTrue(RingWeatherType.STORM_WIND.allowedIn(Level.OVERWORLD, Ring.RING_4));
        assertFalse(RingWeatherType.STORM_WIND.allowedIn(Level.OVERWORLD, Ring.RING_5));
        // Appended: the older types keep their wire index (package 92 appended STANDSTILL after it).
        assertEquals(10, RingWeatherType.STORM_WIND.ordinal());
        assertEquals(9, RingWeatherType.REVERSE_RAIN.ordinal());
    }

    @Test
    void eachTypeHasItsBand() {
        assertEquals(Ring4WeatherBands.Band.MYTHIC, Ring4WeatherBands.bandOf(RingWeatherType.DENSE_FOG));
        assertEquals(Ring4WeatherBands.Band.MYTHIC, Ring4WeatherBands.bandOf(RingWeatherType.STORM_WIND));
        assertEquals(Ring4WeatherBands.Band.BLIGHTED, Ring4WeatherBands.bandOf(RingWeatherType.TOXIC_RAIN));
        for (RingWeatherType type : RingWeatherType.values()) {
            if (type.ring() != Ring.RING_4) {
                assertEquals(Ring4WeatherBands.Band.ANY, Ring4WeatherBands.bandOf(type), type.getSerializedName());
            }
        }
        assertTrue(Ring4WeatherBands.appliesIn(RingWeatherType.DENSE_FOG, false));
        assertFalse(Ring4WeatherBands.appliesIn(RingWeatherType.DENSE_FOG, true));
        assertTrue(Ring4WeatherBands.appliesIn(RingWeatherType.STORM_WIND, false));
        assertFalse(Ring4WeatherBands.appliesIn(RingWeatherType.STORM_WIND, true));
        assertTrue(Ring4WeatherBands.appliesIn(RingWeatherType.TOXIC_RAIN, true));
        assertFalse(Ring4WeatherBands.appliesIn(RingWeatherType.TOXIC_RAIN, false));
        assertTrue(Ring4WeatherBands.appliesIn(RingWeatherType.CLEAR, true));
        assertTrue(Ring4WeatherBands.appliesIn(RingWeatherType.CLEAR, false));
    }

    /** A weather of the other band is clear at that place: intensity 0, same timing; other rings are untouched. */
    @Test
    void feltStateFollowsTheBand() {
        RingWeatherState rain = new RingWeatherState(RingWeatherType.TOXIC_RAIN, 9000, 18000, 42);
        assertSame(rain, Ring4WeatherBands.felt(Ring.RING_4, rain, true));
        RingWeatherState mythical = Ring4WeatherBands.felt(Ring.RING_4, rain, false);
        assertEquals(RingWeatherType.CLEAR, mythical.type());
        assertEquals(0.0F, mythical.intensity());
        assertEquals(9000, mythical.remainingTicks());
        assertEquals(18000, mythical.durationTicks());
        assertFalse(mythical.isStill());

        RingWeatherState fog = new RingWeatherState(RingWeatherType.DENSE_FOG, 9000, 18000, 7);
        assertSame(fog, Ring4WeatherBands.felt(Ring.RING_4, fog, false));
        assertEquals(RingWeatherType.CLEAR, Ring4WeatherBands.felt(Ring.RING_4, fog, true).type());
        RingWeatherState storm = new RingWeatherState(RingWeatherType.STORM_WIND, 9000, 18000, 7);
        assertEquals(RingWeatherType.CLEAR, Ring4WeatherBands.felt(Ring.RING_4, storm, true).type());

        RingWeatherState sandstorm = new RingWeatherState(RingWeatherType.SANDSTORM, 100, 200, 3);
        assertSame(sandstorm, Ring4WeatherBands.felt(Ring.RING_2, sandstorm, true));
        assertSame(RingWeatherState.STILL, Ring4WeatherBands.felt(Ring.RING_4, RingWeatherState.STILL, true));
    }

    /** Gameplay follows Ring4Band.blighted exactly (value ≥ 0), on the real band of the test seed. */
    @Test
    void gameplayBoundaryIsTheMiddleOfTheTransition() {
        RingWeatherState rain = RingWeatherState.start(RingWeatherType.TOXIC_RAIN, 20000, 1);
        RingWeatherState fog = RingWeatherState.start(RingWeatherType.DENSE_FOG, 20000, 1);
        int blighted = 0;
        int mythical = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 90.0) {
            for (int distance = 6100; distance <= 7950; distance += 25) {
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                boolean inBlight = BAND.blighted(x, z);
                assertEquals(inBlight, BAND.value(x, z) >= 0.0);
                boolean raining = Ring4WeatherBands.felt(Ring.RING_4, rain, inBlight).type() == RingWeatherType.TOXIC_RAIN;
                boolean foggy = Ring4WeatherBands.felt(Ring.RING_4, fog, inBlight).type() == RingWeatherType.DENSE_FOG;
                assertEquals(inBlight, raining);
                assertEquals(!inBlight, foggy);
                if (inBlight) {
                    blighted++;
                } else {
                    mythical++;
                }
            }
        }
        assertTrue(blighted > 1000 && mythical > 1000, blighted + " / " + mythical);
    }

    /** Rendering blends over the transition: half in its middle, the two bands always add up to 1. */
    @Test
    void renderShareBlendsOverTheTransition() {
        assertEquals(1.0F, Ring4WeatherBands.renderShare(RingWeatherType.DENSE_FOG, -1.0));
        assertEquals(0.0F, Ring4WeatherBands.renderShare(RingWeatherType.DENSE_FOG, 1.0));
        assertEquals(0.0F, Ring4WeatherBands.renderShare(RingWeatherType.TOXIC_RAIN, -1.0));
        assertEquals(1.0F, Ring4WeatherBands.renderShare(RingWeatherType.TOXIC_RAIN, 1.0));
        assertEquals(0.5F, Ring4WeatherBands.renderShare(RingWeatherType.STORM_WIND, 0.0), 1.0E-6F);
        assertEquals(0.5F, Ring4WeatherBands.renderShare(RingWeatherType.TOXIC_RAIN, 0.0), 1.0E-6F);
        assertEquals(1.0F, Ring4WeatherBands.renderShare(RingWeatherType.SANDSTORM, 0.3));
        float previous = 1.0F;
        for (double value = -1.0; value <= 1.0; value += 0.05) {
            float fog = Ring4WeatherBands.renderShare(RingWeatherType.DENSE_FOG, value);
            float rain = Ring4WeatherBands.renderShare(RingWeatherType.TOXIC_RAIN, value);
            assertEquals(1.0F, fog + rain, 1.0E-6F);
            assertTrue(fog <= previous + 1.0E-6F);
            previous = fog;
        }
        // Outside [-1, 1] (never from Ring4Band) it stays clamped.
        assertEquals(0.0F, Ring4WeatherBands.renderShare(RingWeatherType.DENSE_FOG, 5.0));
    }

    /** One cycle for ring 4: every type comes up, always followed by clear, so the bands never have weather together. */
    @Test
    void oneCycleForBothBands() {
        Map<RingWeatherType, Integer> weights = new EnumMap<>(RingWeatherType.class);
        RingWeatherType.typesFor(Level.OVERWORLD, Ring.RING_4).forEach(type -> weights.put(type, 1));
        RingWeatherSettings settings = RingWeatherSettings.ofSeconds(5, 10, 5, 10, weights);
        RandomSource random = RandomSource.create(70L);
        RingWeatherState state = RingWeatherCycle.startClear(settings, random);
        Set<RingWeatherType> seen = EnumSet.noneOf(RingWeatherType.class);
        RingWeatherType previous = state.type();
        for (int i = 0; i < 2000; i++) {
            state = RingWeatherCycle.advance(state, 20, settings, random);
            if (state.type() != previous) {
                if (previous != RingWeatherType.CLEAR) {
                    assertEquals(RingWeatherType.CLEAR, state.type());
                }
                seen.add(state.type());
                previous = state.type();
            }
        }
        assertEquals(EnumSet.of(RingWeatherType.CLEAR, RingWeatherType.DENSE_FOG, RingWeatherType.TOXIC_RAIN,
                RingWeatherType.STORM_WIND), seen);
    }

    @Test
    void stormPushesOnlyHighUpAndOnlyInAStorm() {
        assertTrue(StormWind.pushes(RingWeatherType.STORM_WIND, 120.0, 120, false, false));
        assertTrue(StormWind.pushes(RingWeatherType.STORM_WIND, 200.0, 120, false, false));
        assertFalse(StormWind.pushes(RingWeatherType.STORM_WIND, 119.99, 120, false, false));
        assertFalse(StormWind.pushes(RingWeatherType.STORM_WIND, 64.0, 120, false, false));
        assertFalse(StormWind.pushes(RingWeatherType.STORM_WIND, 150.0, 120, true, false));
        assertFalse(StormWind.pushes(RingWeatherType.STORM_WIND, 150.0, 120, false, true));
        // stateAt is band-aware: in the blighted band a storm reads as clear, so nothing pushes there.
        assertFalse(StormWind.pushes(RingWeatherType.CLEAR, 150.0, 120, false, false));
        assertFalse(StormWind.pushes(RingWeatherType.DENSE_FOG, 150.0, 120, false, false));
        assertFalse(StormWind.pushes(RingWeatherType.SANDSTORM, 150.0, 120, false, false));
    }

    /** The storm blows one way per storm, from its seed, like the sandstorm; the gust scales with the fade. */
    @Test
    void stormDirectionComesFromTheSeed() {
        for (int seed = -200; seed < 200; seed++) {
            RingWeatherState storm = RingWeatherState.start(RingWeatherType.STORM_WIND, 20000, seed);
            Vec3 direction = SandstormWind.direction(storm);
            assertEquals(1.0, direction.length(), 1.0E-9);
            assertEquals(0.0, direction.y);
            assertEquals(direction, SandstormWind.direction(RingWeatherState.start(RingWeatherType.STORM_WIND, 100, seed)));
            assertEquals(direction, SandstormWind.direction(RingWeatherState.start(RingWeatherType.SANDSTORM, 100, seed)));
        }
        Vec3 direction = SandstormWind.direction(RingWeatherState.start(RingWeatherType.STORM_WIND, 20000, 5));
        assertEquals(0.05, SandstormWind.impulse(direction, 0.05, 1.0F, 1.0F).length(), 1.0E-9);
        assertEquals(0.025, SandstormWind.impulse(direction, 0.05, 0.5F, 1.0F).length(), 1.0E-9);
        assertEquals(10, StormWind.GUST_TICKS);
    }
}
