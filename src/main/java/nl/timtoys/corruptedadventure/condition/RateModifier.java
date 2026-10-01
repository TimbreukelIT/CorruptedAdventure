package nl.timtoys.corruptedadventure.condition;

/**
 * Changes a rule's rate before it is applied: ring weather, fire resistance, gear, purifying zones. Runs for every
 * condition on every tick, also outside its zone (so a modifier can speed up falling meters). Registered through
 * {@link ConditionDefinitions#addModifier}, applied in registration order.
 */
@FunctionalInterface
public interface RateModifier {
    float modify(Condition condition, ConditionContext context, float rate);
}
