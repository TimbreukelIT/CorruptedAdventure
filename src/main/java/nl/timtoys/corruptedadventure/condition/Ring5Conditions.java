package nl.timtoys.corruptedadventure.condition;

import net.neoforged.bus.api.IEventBus;

/**
 * Server-side hooks of the ring 5 conditions (stream C): night and ring weather on the void (package 91). The void's
 * thresholds (teleports, darkness, damage) need no hook of their own: they run from the condition tick. Registered from
 * {@link ConditionTicker#register}, like {@link Ring4Conditions}.
 */
final class Ring5Conditions {
    private Ring5Conditions() {
    }

    static void register(IEventBus gameBus) {
        ConditionDefinitions.addModifier(RateModifier.Stage.WEATHER, new VoidAmplifierModifier());
    }
}
