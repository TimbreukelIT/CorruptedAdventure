package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.function.BooleanSupplier;

/**
 * Nether ring 4 purifies ({@code docs/plans/ring-4/contract.md} §5): in a biome of {@code #corruptedadventure:purifying}
 * every falling meter (poison, heat, cold, dehydration, …) falls {@code fallMultiplier} times as fast. A rising meter is
 * left alone, so purifying never turns a rise into a fall. No condition of its own. Stage
 * {@link RateModifier.Stage#PURIFYING}: last, on the rate after weather, gear and fire resistance.
 */
final class PurifyingModifier implements RateModifier {
    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        return purified(rate, () -> context.biome().is(ModTags.Biomes.PURIFYING),
                ServerConfig.PURIFYING.fallMultiplier.get().floatValue());
    }

    /** {@code rate × multiplier} if the rate falls and the biome purifies, else {@code rate}; the biome only when falling. */
    static float purified(float rate, BooleanSupplier purifying, float multiplier) {
        return rate < 0.0F && purifying.getAsBoolean() ? rate * multiplier : rate;
    }
}
