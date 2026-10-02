package nl.timtoys.corruptedadventure.condition;

/** How fast one condition's meter changes. Every condition has exactly one rule ({@link ConditionDefinitions}). */
@FunctionalInterface
public interface ConditionRule {
    /** Points per second: positive rises, negative falls. {@link RateModifier}s are applied afterwards. */
    float ratePerSecond(ConditionContext context);

    /** Why the rate is what it is (e.g. {@code sun}), for {@code /ca condition get}; empty = no label. */
    default String debugLabel(ConditionContext context) {
        return "";
    }

    /** Always falls at {@code fallRate} points per second (outside a condition's zone the meter goes down). */
    static ConditionRule recover(float fallRate) {
        return context -> -fallRate;
    }
}
