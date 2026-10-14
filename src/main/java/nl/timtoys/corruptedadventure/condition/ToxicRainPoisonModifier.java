package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.weather.RingWeather;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;

import java.util.function.BooleanSupplier;

/**
 * Toxic rain as a poison source ({@code docs/plans/ring-4/contract.md} §5, §6): during {@link RingWeatherType#TOXIC_RAIN}
 * in the player's own ring (so only in ring 4 of the overworld), under the open sky ({@link ConditionContext#canSeeSky},
 * eyes not under water) and in the blighted band ({@link Ring4Band#blighted}, the middle of the transition strip), the
 * poison rate becomes at least {@code rainRise × toxicRainPoisonMultiplier × intensity}: it follows the fade, and a
 * stronger source (pool, gas) wins (sources do not stack). Stage {@link RateModifier.Stage#WEATHER}. Built by the
 * conditions stream, not the weather stream ({@code docs/plans/ring-4/waves.md} §2b); the factor belongs to
 * {@code [weather.ring4]} (stream A).
 */
final class ToxicRainPoisonModifier implements RateModifier {
    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        if (condition != Condition.POISON) {
            return rate;
        }
        double x = context.player().getX();
        double z = context.player().getZ();
        // stateAt: the weather of the ring at the player's position, with the ring 4 band rule applied; the explicit
        // band check below is the same rule, kept here so the modifier reads on its own.
        return rained(rate, RingWeather.stateAt(context.level(), x, z),
                () -> context.canSeeSky() && !context.player().isUnderWater(),
                () -> Ring4Band.of(RingLayout.current()).blighted(x, z),
                ServerConfig.POISON.rainRise.get().floatValue(), rainMultiplier());
    }

    /**
     * The poison rate with the rain on it: at least {@code rainRise × multiplier × intensity} during toxic rain under
     * the open sky in the blighted band, else {@code rate} unchanged. Cheapest first: sky light and the band (noise) are
     * only asked in the rain, the band last.
     */
    static float rained(float rate, RingWeatherState state, BooleanSupplier openSky, BooleanSupplier blighted,
                        float rainRise, float multiplier) {
        if (state.type() != RingWeatherType.TOXIC_RAIN || state.intensity() <= 0.0F || !openSky.getAsBoolean()
                || !blighted.getAsBoolean()) {
            return rate;
        }
        return Math.max(rate, rainRise * multiplier * state.intensity());
    }

    /** {@code [weather.ring4] toxicRainPoisonMultiplier}. */
    static float rainMultiplier() {
        return ServerConfig.RING_4_WEATHER.toxicRainPoisonMultiplier.get().floatValue();
    }
}
