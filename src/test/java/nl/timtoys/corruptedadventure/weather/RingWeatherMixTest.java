package nl.timtoys.corruptedadventure.weather;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingWeatherMixTest {
    private static final float EPSILON = 1.0E-5F;
    /** Ring 2: sandstorm at full strength; ring 3: clear; ring 5: glitch storm at half strength. */
    private static final Function<Ring, RingWeatherState> STATES;

    static {
        Map<Ring, RingWeatherState> states = new EnumMap<>(Ring.class);
        states.put(Ring.RING_2, new RingWeatherState(RingWeatherType.SANDSTORM, 6000, 12_000, 0));
        states.put(Ring.RING_3, RingWeatherState.start(RingWeatherType.CLEAR, 12_000, 0));
        states.put(Ring.RING_4, RingWeatherState.start(RingWeatherType.CLEAR, 12_000, 0));
        states.put(Ring.RING_5, new RingWeatherState(RingWeatherType.GLITCH_STORM, RingWeatherState.FADE_TICKS / 2, 12_000, 0));
        STATES = ring -> states.getOrDefault(ring, RingWeatherState.STILL);
    }

    @Test
    void insideARingOneShare() {
        List<RingWeatherMix.Share> shares = RingWeatherMix.at(RingPosition.within(Ring.RING_2), STATES);
        assertEquals(1, shares.size());
        assertEquals(Ring.RING_2, shares.getFirst().ring());
        assertEquals(1.0F, shares.getFirst().weight(), EPSILON);
        assertEquals(1.0F, shares.getFirst().strength(), EPSILON);
    }

    @Test
    void ringOneHasNoWeather() {
        RingWeatherMix.Share share = RingWeatherMix.at(RingPosition.within(Ring.RING_1), STATES).getFirst();
        assertTrue(share.state().isStill());
        assertEquals(0.0F, share.strength());
    }

    @Test
    void transitionZoneBlendsWithSmoothstep() {
        List<RingWeatherMix.Share> start = RingWeatherMix.at(new RingPosition(Ring.RING_2, Ring.RING_3, 0.0), STATES);
        assertEquals(1, start.size()); // outer weight 0 is left out
        assertEquals(1.0F, start.getFirst().strength(), EPSILON);

        List<RingWeatherMix.Share> middle = RingWeatherMix.at(new RingPosition(Ring.RING_2, Ring.RING_3, 0.5), STATES);
        assertEquals(List.of(Ring.RING_2, Ring.RING_3), middle.stream().map(RingWeatherMix.Share::ring).toList());
        assertEquals(0.5F, middle.get(0).strength(), EPSILON);
        assertEquals(0.0F, middle.get(1).strength(), EPSILON); // clear

        List<RingWeatherMix.Share> quarter = RingWeatherMix.at(new RingPosition(Ring.RING_2, Ring.RING_3, 0.25), STATES);
        float outer = 0.25F * 0.25F * (3 - 2 * 0.25F);
        assertEquals(1 - outer, quarter.get(0).weight(), EPSILON);
        assertEquals(outer, quarter.get(1).weight(), EPSILON);
        assertEquals(1.0F, quarter.get(0).weight() + quarter.get(1).weight(), EPSILON);
    }

    @Test
    void strengthFallsMonotonicallyAcrossTheZone() {
        float previous = Float.MAX_VALUE;
        for (int i = 0; i <= 20; i++) {
            float strength = RingWeatherMix.at(new RingPosition(Ring.RING_2, Ring.RING_3, i / 20.0), STATES).getFirst().strength();
            assertTrue(strength <= previous + EPSILON);
            previous = strength;
        }
    }

    @Test
    void voidFollowsRingFive() {
        List<RingWeatherMix.Share> inVoid = RingWeatherMix.at(RingPosition.within(Ring.VOID), STATES);
        assertEquals(Ring.RING_5, inVoid.getFirst().ring());
        assertEquals(0.5F, inVoid.getFirst().strength(), EPSILON);

        List<RingWeatherMix.Share> zone = RingWeatherMix.at(new RingPosition(Ring.RING_5, Ring.VOID, 0.7), STATES);
        assertEquals(1, zone.size());
        assertEquals(1.0F, zone.getFirst().weight(), EPSILON);
    }

    /** {@code weight} is the share's weight from {@code at}, without the list (ash clouds, package 50). */
    @Test
    void weightMatchesTheSharesOfAt() {
        for (RingPosition position : List.of(RingPosition.within(Ring.RING_3), RingPosition.within(Ring.RING_2),
                new RingPosition(Ring.RING_2, Ring.RING_3, 0.3), new RingPosition(Ring.RING_3, Ring.RING_4, 0.8),
                new RingPosition(Ring.RING_5, Ring.VOID, 0.7))) {
            for (Ring ring : Ring.values()) {
                float expected = (float) RingWeatherMix.at(position, STATES).stream()
                        .filter(share -> share.ring() == ring).mapToDouble(RingWeatherMix.Share::weight).sum();
                assertEquals(expected, RingWeatherMix.weight(position, ring), EPSILON, position + " " + ring);
            }
        }
    }

    @Test
    void smoothstepEnds() {
        assertEquals(0.0F, RingWeatherMix.smoothstep(-1.0F));
        assertEquals(0.5F, RingWeatherMix.smoothstep(0.5F), EPSILON);
        assertEquals(1.0F, RingWeatherMix.smoothstep(2.0F));
    }
}
