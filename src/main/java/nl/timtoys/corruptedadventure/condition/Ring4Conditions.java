package nl.timtoys.corruptedadventure.condition;

import net.neoforged.bus.api.IEventBus;

/**
 * Server-side hooks of the ring 4 conditions (stream C): the toxic rain on poison, the remedies (milk, honey) and the
 * withering at the top threshold (package 69). Registered from {@link ConditionTicker#register}, like
 * {@link Ring3Conditions}, so the shared mod entrypoint stays untouched while the ring 4 streams work in parallel.
 */
final class Ring4Conditions {
    private Ring4Conditions() {
    }

    static void register(IEventBus gameBus) {
        ConditionDefinitions.addModifier(RateModifier.Stage.WEATHER, new ToxicRainPoisonModifier());
        PoisonRemedies.register(gameBus);
        PoisonWither.register(gameBus);
    }
}
