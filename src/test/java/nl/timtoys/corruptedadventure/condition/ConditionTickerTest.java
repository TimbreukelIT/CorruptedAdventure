package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConditionTickerTest {
    private static final ThresholdEffect SLOW = new NamedEffect("slow");
    private static final ThresholdEffect WEAK = new NamedEffect("weak");
    private static final ThresholdEffect HURT = new NamedEffect("hurt");
    private static final List<Threshold> THRESHOLDS =
            List.of(new Threshold(50, SLOW), new Threshold(75, WEAK), new Threshold(100, HURT));

    @Test
    void modifiersRunInRegistrationOrder() {
        List<RateModifier> modifiers = List.of(
                (condition, context, rate) -> rate + 1,
                (condition, context, rate) -> rate * 2);
        assertEquals(12, ConditionTicker.rate(Condition.DEBUG, null, 5, modifiers, true));
        assertEquals(-5, ConditionTicker.rate(Condition.DEBUG, null, -5, List.of(), true));
    }

    @Test
    void unexposedPlayersOnlyFall() {
        assertEquals(0, ConditionTicker.rate(Condition.DEBUG, null, 5, List.of(), false));
        assertEquals(-5, ConditionTicker.rate(Condition.DEBUG, null, -5, List.of(), false));
    }

    @Test
    void thresholdsAreCumulative() {
        assertEquals(0, update(49, 0).reached());
        assertEquals(List.of(), update(49, 0).apply());
        assertEquals(List.of(SLOW), update(50, 0).apply());
        assertEquals(List.of(SLOW, WEAK), update(75, 1).apply());
        assertEquals(List.of(SLOW, WEAK, HURT), update(100, 2).apply());
        assertEquals(List.of(), update(100, 3).clear());
    }

    @Test
    void droppingBelowAThresholdClearsOnlyThatOne() {
        ConditionTicker.ThresholdUpdate update = update(74, 2);
        assertEquals(1, update.reached());
        assertEquals(List.of(SLOW), update.apply());
        assertEquals(List.of(WEAK), update.clear());
        assertEquals(List.of(SLOW, WEAK, HURT), update(0, 3).clear());
    }

    @Test
    void afterLoadingEveryUnreachedThresholdIsCleared() {
        assertEquals(List.of(WEAK, HURT), update(60, ConditionMeters.UNKNOWN).clear());
    }

    @Test
    void unexposedPlayersHaveNoThresholds() {
        ConditionTicker.ThresholdUpdate update = ConditionTicker.ThresholdUpdate.of(100, false, 3, THRESHOLDS);
        assertEquals(0, update.reached());
        assertEquals(List.of(), update.apply());
        assertEquals(List.of(SLOW, WEAK, HURT), update.clear());
    }

    private static ConditionTicker.ThresholdUpdate update(float value, int previous) {
        return ConditionTicker.ThresholdUpdate.of(value, true, previous, THRESHOLDS);
    }

    private record NamedEffect(String name) implements ThresholdEffect {
        @Override
        public void apply(ServerPlayer player) {
        }

        @Override
        public void clear(ServerPlayer player) {
        }
    }
}
