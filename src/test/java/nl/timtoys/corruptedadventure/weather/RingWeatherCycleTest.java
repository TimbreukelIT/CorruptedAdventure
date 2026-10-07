package nl.timtoys.corruptedadventure.weather;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.ring.Ring;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingWeatherCycleTest {
    private static final int STEP = 20;

    /** The vanilla rhythm (defaults): clear 600–9000 s, weather 600–1200 s. */
    private static final RingWeatherSettings VANILLA = RingWeatherSettings.ofSeconds(600, 9000, 600, 1200, equal(Ring.RING_2));
    /** Short test values: 5–10 s each. */
    private static final RingWeatherSettings SHORT = RingWeatherSettings.ofSeconds(5, 10, 5, 10, equal(Ring.RING_3));

    @Test
    void vanillaDefaultsInTicks() {
        assertEquals(12_000, VANILLA.clearMinTicks());
        assertEquals(180_000, VANILLA.clearMaxTicks());
        assertEquals(12_000, VANILLA.weatherMinTicks());
        assertEquals(24_000, VANILLA.weatherMaxTicks());
    }

    @Test
    void durationsStayWithinTheirRangeAndClearAlternates() {
        for (RingWeatherSettings settings : List.of(VANILLA, SHORT)) {
            Set<RingWeatherType> allowed = settings.weights().keySet();
            List<RingWeatherState> sequence = run(settings, 12345L, 200);
            for (int i = 0; i < sequence.size(); i++) {
                RingWeatherState state = sequence.get(i);
                if (state.type() == RingWeatherType.CLEAR) {
                    assertBetween(state.durationTicks(), settings.clearMinTicks(), settings.clearMaxTicks());
                } else {
                    assertTrue(allowed.contains(state.type()), state.type().getSerializedName());
                    assertBetween(state.durationTicks(), settings.weatherMinTicks(), settings.weatherMaxTicks());
                    // after a weather always clear
                    if (i + 1 < sequence.size()) {
                        assertEquals(RingWeatherType.CLEAR, sequence.get(i + 1).type());
                    }
                }
                assertEquals(state.durationTicks(), state.remainingTicks());
            }
            assertTrue(sequence.stream().anyMatch(state -> state.type() != RingWeatherType.CLEAR));
        }
    }

    @Test
    void everyTypeOfARingComesUpWithEqualWeights() {
        Set<RingWeatherType> seen = EnumSet.noneOf(RingWeatherType.class);
        run(SHORT, 7L, 200).forEach(state -> seen.add(state.type()));
        assertEquals(EnumSet.of(RingWeatherType.CLEAR, RingWeatherType.ASH_RAIN, RingWeatherType.ERUPTION,
                RingWeatherType.ASH_CLOUD), seen);
    }

    @Test
    void weightZeroIsNeverChosen() {
        Map<RingWeatherType, Integer> weights = equal(Ring.RING_4);
        weights.put(RingWeatherType.DENSE_FOG, 0);
        RingWeatherSettings settings = RingWeatherSettings.ofSeconds(5, 10, 5, 10, weights);
        assertFalse(run(settings, 99L, 200).stream().anyMatch(state -> state.type() == RingWeatherType.DENSE_FOG));
    }

    @Test
    void allWeightsZeroStaysClear() {
        Map<RingWeatherType, Integer> weights = equal(Ring.RING_5);
        weights.replaceAll((type, weight) -> 0);
        RingWeatherSettings settings = RingWeatherSettings.ofSeconds(5, 10, 5, 10, weights);
        List<RingWeatherState> sequence = run(settings, 3L, 20);
        assertTrue(sequence.stream().allMatch(state -> state.type() == RingWeatherType.CLEAR && !state.isStill()));
    }

    @Test
    void ringWithoutTypesStaysStill() {
        RandomSource random = RandomSource.create(1L);
        assertSame(RingWeatherState.STILL, RingWeatherCycle.advance(RingWeatherState.STILL, STEP, RingWeatherSettings.NONE, random));
        RingWeatherState clear = RingWeatherState.start(RingWeatherType.CLEAR, 100, 0);
        assertSame(RingWeatherState.STILL, RingWeatherCycle.advance(clear, STEP, RingWeatherSettings.NONE, random));
    }

    @Test
    void countsDownInStepsOfTwenty() {
        RingWeatherState state = RingWeatherState.start(RingWeatherType.SANDSTORM, 100, 5);
        RandomSource random = RandomSource.create(1L);
        state = RingWeatherCycle.advance(state, STEP, VANILLA, random);
        assertEquals(80, state.remainingTicks());
        assertEquals(RingWeatherType.SANDSTORM, state.type());
        assertEquals(5, state.seed());
        for (int i = 0; i < 3; i++) {
            state = RingWeatherCycle.advance(state, STEP, VANILLA, random);
        }
        assertEquals(20, state.remainingTicks());
        state = RingWeatherCycle.advance(state, STEP, VANILLA, random);
        assertEquals(RingWeatherType.CLEAR, state.type());
        assertEquals(state.durationTicks(), state.remainingTicks());
    }

    @Test
    void sameSeedSameSequence() {
        assertEquals(run(SHORT, 2026L, 50), run(SHORT, 2026L, 50));
        assertNotEquals(run(SHORT, 2026L, 50), run(SHORT, 2027L, 50));
    }

    @Test
    void everyNewWeatherDrawsASeed() {
        List<RingWeatherState> weathers = run(SHORT, 11L, 100).stream()
                .filter(state -> state.type() != RingWeatherType.CLEAR).toList();
        assertTrue(weathers.stream().map(RingWeatherState::seed).distinct().count() > 1);
        assertTrue(run(SHORT, 11L, 100).stream().filter(state -> state.type() == RingWeatherType.CLEAR)
                .allMatch(state -> state.seed() == 0));
    }

    @Test
    void startUsesTheMatchingRange() {
        RandomSource random = RandomSource.create(5L);
        for (int i = 0; i < 50; i++) {
            assertBetween(RingWeatherCycle.start(RingWeatherType.CLEAR, VANILLA, random).durationTicks(), 12_000, 180_000);
            assertBetween(RingWeatherCycle.start(RingWeatherType.SANDSTORM, VANILLA, random).durationTicks(), 12_000, 24_000);
        }
    }

    @Test
    void aTypeWithItsOwnRangeLastsThatLong() {
        Map<RingWeatherType, Integer> weights = Map.of(RingWeatherType.ASH_RAIN, 45, RingWeatherType.ERUPTION, 40,
                RingWeatherType.ASH_CLOUD, 15);
        RingWeatherSettings settings = RingWeatherSettings.ofSeconds(5, 10, 600, 1200, weights,
                Map.of(RingWeatherType.ASH_CLOUD, new RingWeatherSettings.Range(120, 240)));
        List<RingWeatherState> weathers = run(settings, 45L, 400).stream()
                .filter(state -> state.type() != RingWeatherType.CLEAR).toList();
        assertTrue(weathers.stream().anyMatch(state -> state.type() == RingWeatherType.ASH_CLOUD));
        for (RingWeatherState state : weathers) {
            if (state.type() == RingWeatherType.ASH_CLOUD) {
                assertBetween(state.durationTicks(), 2_400, 4_800);
            } else {
                assertBetween(state.durationTicks(), 12_000, 24_000);
            }
        }
        // The rare one stays rare: 15 of 100.
        long clouds = weathers.stream().filter(state -> state.type() == RingWeatherType.ASH_CLOUD).count();
        assertTrue(clouds > weathers.size() * 0.05 && clouds < weathers.size() * 0.3, clouds + " of " + weathers.size());
    }

    /** Every new state (the moment the weather changes) over {@code changes} changes, starting with a clear spell. */
    private static List<RingWeatherState> run(RingWeatherSettings settings, long seed, int changes) {
        RandomSource random = RandomSource.create(seed);
        RingWeatherState state = RingWeatherCycle.startClear(settings, random);
        List<RingWeatherState> sequence = new ArrayList<>(List.of(state));
        while (sequence.size() < changes) {
            RingWeatherState next = RingWeatherCycle.advance(state, STEP, settings, random);
            if (next.remainingTicks() > state.remainingTicks() || next.type() != state.type()) {
                sequence.add(next);
            }
            state = next;
        }
        return sequence;
    }

    private static Map<RingWeatherType, Integer> equal(Ring ring) {
        Map<RingWeatherType, Integer> weights = new EnumMap<>(RingWeatherType.class);
        RingWeatherType.typesFor(Level.OVERWORLD, ring).forEach(type -> weights.put(type, 1));
        return weights;
    }

    private static void assertBetween(int value, int min, int max) {
        assertTrue(value >= min && value <= max, value + " not in " + min + "-" + max);
    }
}
