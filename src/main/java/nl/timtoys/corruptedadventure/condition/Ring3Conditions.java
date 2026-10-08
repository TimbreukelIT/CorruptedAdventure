package nl.timtoys.corruptedadventure.condition;

import net.neoforged.bus.api.IEventBus;

/**
 * Server-side hooks of the ring 3 conditions (stream C): the heat modifiers, the burn and evaporating water; the cold
 * of Nether ring 3 (leather, freezing; package 55).
 * Registered from {@link ConditionTicker#register}, so the shared mod entrypoint stays untouched while the ring 3
 * streams work in parallel.
 */
final class Ring3Conditions {
    private Ring3Conditions() {
    }

    static void register(IEventBus gameBus) {
        ConditionDefinitions.addModifier(RateModifier.Stage.WEATHER, new HeatWeatherModifier());
        ConditionDefinitions.addModifier(RateModifier.Stage.FIRE_RESISTANCE, new FireResistanceHeatModifier());
        HeatBurn.register(gameBus);
        WaterEvaporation.register(gameBus);
        ConditionDefinitions.addModifier(RateModifier.Stage.GEAR, new LeatherColdModifier());
        ColdFreeze.register(gameBus);
    }
}
