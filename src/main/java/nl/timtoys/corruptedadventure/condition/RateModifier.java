package nl.timtoys.corruptedadventure.condition;

/**
 * Changes a rule's rate before it is applied: ring weather, fire resistance, gear, purifying zones. Runs for every
 * condition on every tick, also outside its zone (so a modifier can speed up falling meters). Registered through
 * {@link ConditionDefinitions#addModifier} in a {@link Stage}; stages run in their order, within a stage in
 * registration order.
 */
@FunctionalInterface
public interface RateModifier {
    float modify(Condition condition, ConditionContext context, float rate);

    /**
     * Where in the pipeline a modifier runs (backlog r. 95, decided in change 43). Weather first: it scales the
     * exposure itself; then what the player wears and drinks; a purifying zone last, so it can override everything.
     */
    enum Stage {
        /** Ring weather (heat wave, ash rain, eruption, later ash cloud). */
        WEATHER,
        /** Gear (phase 2). */
        GEAR,
        /** The fire resistance effect on heat. */
        FIRE_RESISTANCE,
        /** Purifying zones (Nether ring 4); always last. */
        PURIFYING
    }
}
