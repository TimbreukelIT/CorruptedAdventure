package nl.timtoys.corruptedadventure.condition;

import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Night and ring weather on the void (package 91, {@code golf-1.md} P5): the larger factor counts, not the product. */
class VoidAmplifierModifierTest {
    private static final int DURATION = 6000;
    private static final float NIGHT = 1.5F;
    private static final BooleanSupplier DAY = () -> false;
    private static final BooleanSupplier AT_NIGHT = () -> true;
    private static final Supplier<RingWeatherState> CLEAR = () -> RingWeatherState.STILL;
    private static final BooleanSupplier NEVER_NIGHT = () -> {
        throw new AssertionError("night asked for a falling rate");
    };
    /** The defaults of {@code [weather.ring5]}: the factors are read from the config, not hardcoded here. */
    private static final ToDoubleFunction<RingWeatherType> DEFAULT_VOID = type ->
            ServerConfig.RING_5_WEATHER.voidMultiplier(type, ModConfigSpec.DoubleValue::getDefault);
    private static final Supplier<RingWeatherState> NEVER_WEATHER = () -> {
        throw new AssertionError("weather asked for a falling rate");
    };

    /** A weather at {@code intensity} (fade-in part of a long weather). */
    private static Supplier<RingWeatherState> weather(RingWeatherType type, float intensity) {
        int elapsed = Math.round(intensity * type.fadeTicks());
        RingWeatherState state = new RingWeatherState(type, DURATION - elapsed, DURATION, 7);
        return () -> state;
    }

    private static float amplified(float rate, BooleanSupplier night, Supplier<RingWeatherState> weather) {
        return VoidAmplifierModifier.amplified(rate, night, weather, NIGHT, DEFAULT_VOID);
    }

    @Test
    void theFactorsAreTheConfirmedOnes() {
        assertEquals(1.5, ServerConfig.VOID.nightMultiplier.getDefault());
        assertEquals(2.0, DEFAULT_VOID.applyAsDouble(RingWeatherType.GLITCH_STORM));
        assertEquals(1.5, DEFAULT_VOID.applyAsDouble(RingWeatherType.REVERSE_RAIN));
        assertEquals(1.5, DEFAULT_VOID.applyAsDouble(RingWeatherType.STANDSTILL));
        assertEquals(1.0, DEFAULT_VOID.applyAsDouble(RingWeatherType.CLEAR));
        assertEquals(1.0, DEFAULT_VOID.applyAsDouble(RingWeatherType.TOXIC_RAIN));
    }

    @Test
    void nightAlone() {
        assertEquals(0.42F, amplified(0.28F, AT_NIGHT, CLEAR), 1.0E-6F);
        assertEquals(0.28F, amplified(0.28F, DAY, CLEAR), 1.0E-6F);
    }

    @Test
    void weatherFollowsTheFade() {
        assertEquals(0.56F, amplified(0.28F, DAY, weather(RingWeatherType.GLITCH_STORM, 1.0F)), 1.0E-6F);
        assertEquals(0.42F, amplified(0.28F, DAY, weather(RingWeatherType.GLITCH_STORM, 0.5F)), 1.0E-6F);
        assertEquals(0.42F, amplified(0.28F, DAY, weather(RingWeatherType.REVERSE_RAIN, 1.0F)), 1.0E-6F);
        assertEquals(0.28F, amplified(0.28F, DAY, weather(RingWeatherType.GLITCH_STORM, 0.0F)), 1.0E-6F);
    }

    @Test
    void nightAndWeatherDoNotStack() {
        // Night 1.5 and a full glitch storm 2.0: 2.0, not 3.0.
        assertEquals(0.56F, amplified(0.28F, AT_NIGHT, weather(RingWeatherType.GLITCH_STORM, 1.0F)), 1.0E-6F);
        // A fading storm (1.25) at night (1.5): the night counts.
        assertEquals(0.42F, amplified(0.28F, AT_NIGHT, weather(RingWeatherType.GLITCH_STORM, 0.25F)), 1.0E-6F);
        // Night and reverse rain, both 1.5: 1.5.
        assertEquals(0.42F, amplified(0.28F, AT_NIGHT, weather(RingWeatherType.REVERSE_RAIN, 1.0F)), 1.0E-6F);
        // Inner band at night in a glitch storm: 0 -> 100 in ~3 min.
        assertEquals(180.0F, 100 / amplified(0.28F, AT_NIGHT, weather(RingWeatherType.GLITCH_STORM, 1.0F)), 10.0F);
    }

    @Test
    void aFallingRateIsLeftAloneWithoutAsking() {
        assertEquals(-1.0F, amplified(-1.0F, NEVER_NIGHT, NEVER_WEATHER));
        assertEquals(0.0F, amplified(0.0F, NEVER_NIGHT, NEVER_WEATHER));
    }

    @Test
    void otherConditionsAndFallingRatesAreLeftAloneWithoutAContext() {
        VoidAmplifierModifier modifier = new VoidAmplifierModifier();
        for (Condition condition : Condition.values()) {
            if (condition != Condition.VOID) {
                assertEquals(0.55F, modifier.modify(condition, null, 0.55F));
            }
        }
        // A falling void rate never reaches the zone check, the night or the weather.
        assertEquals(-1.0F, modifier.modify(Condition.VOID, null, -1.0F));
    }
}
