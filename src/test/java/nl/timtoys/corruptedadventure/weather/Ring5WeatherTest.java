package nl.timtoys.corruptedadventure.weather;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Package 92: the ring 5 weather types, one cycle with an own type per band (OV1 = A), the band switch and blend. */
class Ring5WeatherTest {
    private static final RingLayout LAYOUT = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(20260930L));
    /** The P2 defaults of the outer band and the standstill's 40–120 s. */
    private static final Ring5WeatherBands.Settings SETTINGS = new Ring5WeatherBands.Settings(Map.of(
            RingWeatherType.GLITCH_STORM, 20, RingWeatherType.REVERSE_RAIN, 70, RingWeatherType.STANDSTILL, 10),
            new RingWeatherSettings.Range(800, 2400));

    @Test
    void standstillIsAppendedSoTheOldIndicesStay() {
        assertEquals(RingWeatherType.values().length - 1, RingWeatherType.STANDSTILL.ordinal());
        assertEquals(8, RingWeatherType.GLITCH_STORM.ordinal());
        assertEquals(9, RingWeatherType.REVERSE_RAIN.ordinal());
        assertEquals(10, RingWeatherType.STORM_WIND.ordinal());
        assertEquals("standstill", RingWeatherType.STANDSTILL.getSerializedName());
        assertEquals(100, RingWeatherType.STANDSTILL.fadeTicks());
        assertEquals(RingWeatherState.FADE_TICKS, RingWeatherType.GLITCH_STORM.fadeTicks());
        assertEquals(List.of(RingWeatherType.GLITCH_STORM, RingWeatherType.REVERSE_RAIN, RingWeatherType.STANDSTILL),
                RingWeatherType.typesFor(Level.OVERWORLD, Ring.RING_5));
        assertTrue(RingWeatherType.STANDSTILL.allowedIn(Level.OVERWORLD, Ring.RING_5));
    }

    @Test
    void innerBandFeelsTheCycleItself() {
        RingWeatherState storm = new RingWeatherState(RingWeatherType.GLITCH_STORM, 9000, 18000, 42);
        assertSame(storm, Ring5WeatherBands.felt(storm, Ring5WeatherBands.Band.INNER, SETTINGS));
        RingWeatherState clear = RingWeatherState.start(RingWeatherType.CLEAR, 5000, 0);
        assertSame(clear, Ring5WeatherBands.felt(clear, Ring5WeatherBands.Band.OUTER, SETTINGS));
        assertSame(RingWeatherState.STILL, Ring5WeatherBands.felt(RingWeatherState.STILL, Ring5WeatherBands.Band.OUTER, SETTINGS));
    }

    /** The outer type comes from the seed alone: the same on every side, and spread by the outer weights (± 2 pt). */
    @Test
    void outerTypeFollowsTheSeedAndTheWeights() {
        Map<RingWeatherType, Integer> counts = new EnumMap<>(RingWeatherType.class);
        int runs = 10_000;
        for (int seed = 1; seed <= runs; seed++) {
            RingWeatherState state = RingWeatherState.start(RingWeatherType.GLITCH_STORM, 20000, seed * 7919);
            RingWeatherType outer = Ring5WeatherBands.typeIn(state, Ring5WeatherBands.Band.OUTER, SETTINGS);
            assertEquals(outer, Ring5WeatherBands.typeIn(RingWeatherState.start(RingWeatherType.STANDSTILL, 900, seed * 7919),
                    Ring5WeatherBands.Band.OUTER, SETTINGS));
            counts.merge(outer, 1, Integer::sum);
        }
        assertEquals(0.20, counts.getOrDefault(RingWeatherType.GLITCH_STORM, 0) / (double) runs, 0.02);
        assertEquals(0.70, counts.getOrDefault(RingWeatherType.REVERSE_RAIN, 0) / (double) runs, 0.02);
        assertEquals(0.10, counts.getOrDefault(RingWeatherType.STANDSTILL, 0) / (double) runs, 0.02);
    }

    @Test
    void allOuterWeightsZeroLeaveTheOuterBandClear() {
        Ring5WeatherBands.Settings none = new Ring5WeatherBands.Settings(Map.of(RingWeatherType.GLITCH_STORM, 0,
                RingWeatherType.REVERSE_RAIN, 0, RingWeatherType.STANDSTILL, 0), SETTINGS.standstillTicks());
        RingWeatherState storm = new RingWeatherState(RingWeatherType.GLITCH_STORM, 9000, 18000, 42);
        RingWeatherState outer = Ring5WeatherBands.felt(storm, Ring5WeatherBands.Band.OUTER, none);
        assertEquals(new RingWeatherState(RingWeatherType.CLEAR, 9000, 18000, 0), outer);
        assertEquals(0.0F, outer.intensity());
    }

    /** {@code /ca weather 5 <type>} starts with seed 0: that type in both bands. */
    @Test
    void seedZeroHoldsTheSetTypeInBothBands() {
        for (RingWeatherType type : RingWeatherType.typesFor(Level.OVERWORLD, Ring.RING_5)) {
            RingWeatherState set = RingWeatherState.start(type, 1200, 0);
            assertSame(set, Ring5WeatherBands.felt(set, Ring5WeatherBands.Band.OUTER, SETTINGS));
            assertSame(set, Ring5WeatherBands.felt(set, Ring5WeatherBands.Band.INNER, SETTINGS));
        }
    }

    /** A derived standstill outside lasts its own length at the start of a long window, then clear with the same time left. */
    @Test
    void derivedStandstillIsShortThenClear() {
        int seed = seedWithOuter(RingWeatherType.GLITCH_STORM, RingWeatherType.STANDSTILL);
        int window = 18000;
        int length = Ring5WeatherBands.standstillTicks(seed, SETTINGS.standstillTicks());
        assertTrue(length >= 800 && length <= 2400, "length " + length);
        RingWeatherState start = RingWeatherState.start(RingWeatherType.GLITCH_STORM, window, seed);
        RingWeatherState felt = Ring5WeatherBands.felt(start, Ring5WeatherBands.Band.OUTER, SETTINGS);
        assertEquals(new RingWeatherState(RingWeatherType.STANDSTILL, length, length, seed), felt);
        RingWeatherState later = start.countDown(length / 2);
        assertEquals(new RingWeatherState(RingWeatherType.STANDSTILL, length - length / 2, length, seed),
                Ring5WeatherBands.felt(later, Ring5WeatherBands.Band.OUTER, SETTINGS));
        RingWeatherState after = start.countDown(length);
        assertEquals(new RingWeatherState(RingWeatherType.CLEAR, window - length, window, 0),
                Ring5WeatherBands.felt(after, Ring5WeatherBands.Band.OUTER, SETTINGS));
        // The full fade: in, out, and nothing once it is over.
        assertEquals(1.0F, Ring5WeatherBands.felt(later, Ring5WeatherBands.Band.OUTER, SETTINGS).intensity());
        assertEquals(0.0F, Ring5WeatherBands.felt(start.countDown(length + 20), Ring5WeatherBands.Band.OUTER, SETTINGS).intensity());

        // In a short window (an inner standstill) the outer type just takes the window.
        int other = seedWithOuter(RingWeatherType.STANDSTILL, RingWeatherType.REVERSE_RAIN);
        RingWeatherState still = new RingWeatherState(RingWeatherType.STANDSTILL, 1000, 1500, other);
        assertEquals(new RingWeatherState(RingWeatherType.REVERSE_RAIN, 1000, 1500, other),
                Ring5WeatherBands.felt(still, Ring5WeatherBands.Band.OUTER, SETTINGS));
    }

    /** Gameplay switches exactly on Ring5Band.outer of the test seed; the void counts as outer. */
    @Test
    void bandSwitchIsRing5BandOuter() {
        Ring5Band band = Ring5Band.of(LAYOUT);
        int inner = 0;
        int outer = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 90.0) {
            for (int distance = 8400; distance <= 10150; distance += 25) {
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                Ring ring = LAYOUT.sampleOverworld(x, z).ring();
                Ring5WeatherBands.Band expected = ring == Ring.VOID || band.outer(x, z) ? Ring5WeatherBands.Band.OUTER
                        : Ring5WeatherBands.Band.INNER;
                assertEquals(expected, Ring5WeatherBands.bandAt(LAYOUT, ring, x, z));
                if (ring == Ring.RING_5) {
                    if (expected == Ring5WeatherBands.Band.OUTER) {
                        outer++;
                    } else {
                        inner++;
                    }
                }
            }
        }
        assertTrue(inner > 1000 && outer > 1000, inner + " / " + outer);
        assertEquals(Ring5WeatherBands.Band.OUTER, Ring5WeatherBands.bandAt(LAYOUT, Ring.VOID, 0.0, 0.0));
    }

    /** stateAt (through its pure part): ring 5 switches on Ring5Band.outer, the void follows ring 5 as outer. */
    @Test
    void stateAtSwitchesOnTheBand() {
        int seed = seedWithOuter(RingWeatherType.GLITCH_STORM, RingWeatherType.REVERSE_RAIN);
        RingWeatherState storm = RingWeatherState.start(RingWeatherType.GLITCH_STORM, 20000, seed).countDown(5000);
        Map<Ring, RingWeatherState> states = new EnumMap<>(Ring.class);
        states.put(Ring.RING_5, storm);
        states.put(Ring.RING_4, RingWeatherState.start(RingWeatherType.CLEAR, 100, 0));
        Ring5Band band = Ring5Band.of(LAYOUT);
        int checked = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 45.0) {
            for (int distance = 8400; distance <= 10400; distance += 50) {
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                Ring ring = LAYOUT.sampleOverworld(x, z).ring();
                if (ring != Ring.RING_5 && ring != Ring.VOID) {
                    continue;
                }
                boolean outside = ring == Ring.VOID || band.outer(x, z);
                RingWeatherState felt = RingWeather.feltAt(LAYOUT, Level.OVERWORLD, states::get, x, z);
                assertEquals(outside ? RingWeatherType.REVERSE_RAIN : RingWeatherType.GLITCH_STORM, felt.type());
                assertEquals(storm.remainingTicks(), felt.remainingTicks());
                checked++;
            }
        }
        assertTrue(checked > 3000, "checked " + checked);
        // A set weather (seed 0) and clear are the same everywhere.
        RingWeatherState set = RingWeatherState.start(RingWeatherType.STANDSTILL, 1200, 0);
        states.put(Ring.RING_5, set);
        assertSame(set, RingWeather.feltAt(LAYOUT, Level.OVERWORLD, states::get, 9900.0, 0.0));
    }

    @Test
    void outerShareBlendsOverTheTransition() {
        assertEquals(0.0F, Ring5WeatherBands.outerShare(-1.0));
        assertEquals(1.0F, Ring5WeatherBands.outerShare(1.0));
        assertEquals(0.5F, Ring5WeatherBands.outerShare(0.0), 1.0E-6F);
        assertEquals(1.0F, Ring5WeatherBands.outerShare(4.0));
        float previous = 0.0F;
        for (double value = -1.0; value <= 1.0; value += 0.05) {
            float share = Ring5WeatherBands.outerShare(value);
            assertTrue(share >= previous - 1.0E-6F);
            previous = share;
        }
    }

    /** One cycle: every inner type comes up, always clear in between; the standstill lasts 40–120 s, never seed 0. */
    @Test
    void oneCycleWithAllThreeTypes() {
        Map<RingWeatherType, Integer> weights = new EnumMap<>(RingWeatherType.class);
        weights.put(RingWeatherType.GLITCH_STORM, 55);
        weights.put(RingWeatherType.REVERSE_RAIN, 15);
        weights.put(RingWeatherType.STANDSTILL, 30);
        RingWeatherSettings settings = RingWeatherSettings.ofSeconds(5, 10, 600, 1200, weights,
                Map.of(RingWeatherType.STANDSTILL, new RingWeatherSettings.Range(40, 120)));
        RandomSource random = RandomSource.create(92L);
        RingWeatherState state = RingWeatherCycle.startClear(settings, random);
        Set<RingWeatherType> seen = EnumSet.noneOf(RingWeatherType.class);
        RingWeatherType previous = state.type();
        for (int i = 0; i < 20000; i++) {
            state = RingWeatherCycle.advance(state, 20, settings, random);
            if (state.type() != previous) {
                if (previous != RingWeatherType.CLEAR) {
                    assertEquals(RingWeatherType.CLEAR, state.type());
                } else {
                    assertTrue(state.seed() != 0);
                }
                if (state.type() == RingWeatherType.STANDSTILL) {
                    assertTrue(state.durationTicks() >= 800 && state.durationTicks() <= 2400, "standstill " + state.durationTicks());
                } else if (state.type() != RingWeatherType.CLEAR) {
                    assertTrue(state.durationTicks() >= 12000, state.type() + " " + state.durationTicks());
                }
                seen.add(state.type());
                previous = state.type();
            }
        }
        assertEquals(EnumSet.of(RingWeatherType.CLEAR, RingWeatherType.GLITCH_STORM, RingWeatherType.REVERSE_RAIN,
                RingWeatherType.STANDSTILL), seen);
    }

    private static int seedWithOuter(RingWeatherType inner, RingWeatherType outer) {
        for (int seed = 1; ; seed++) {
            RingWeatherState state = RingWeatherState.start(inner, 18000, seed);
            if (Ring5WeatherBands.typeIn(state, Ring5WeatherBands.Band.OUTER, SETTINGS) == outer) {
                return seed;
            }
        }
    }
}
