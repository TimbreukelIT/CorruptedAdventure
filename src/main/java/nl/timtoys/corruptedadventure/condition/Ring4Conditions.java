package nl.timtoys.corruptedadventure.condition;

import net.neoforged.bus.api.IEventBus;

/**
 * Server-side hooks of the ring 4 conditions (stream C): the toxic rain on poison, the remedies (milk, honey) and the
 * withering at the top threshold (package 69); purifying in Nether ring 4 (package 79). Registered from
 * {@link ConditionTicker#register}, like {@link Ring3Conditions}, so the shared mod entrypoint stays untouched while the ring 4 streams work in parallel.
 */
final class Ring4Conditions {
    private Ring4Conditions() {
    }

    static void register(IEventBus gameBus) {
        ConditionDefinitions.addModifier(RateModifier.Stage.WEATHER, new ToxicRainPoisonModifier());
        ConditionDefinitions.addModifier(RateModifier.Stage.PURIFYING, new PurifyingModifier());
        PoisonRemedies.register(gameBus);
        PoisonWither.register(gameBus);
    }
}
