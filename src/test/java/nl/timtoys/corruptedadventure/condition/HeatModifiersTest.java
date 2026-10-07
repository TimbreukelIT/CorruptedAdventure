package nl.timtoys.corruptedadventure.condition;

import net.neoforged.neoforge.common.ModConfigSpec;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The heat modifiers (ring 3 weather, fire resistance) and the order of the modifier pipeline. */
class HeatModifiersTest {
    private static final int DURATION = 6000;

    /** A weather at {@code intensity} (fade-in part of a long weather). */
    private static RingWeatherState weather(RingWeatherType type, float intensity) {
        int elapsed = Math.round(intensity * type.fadeTicks());
        return new RingWeatherState(type, DURATION - elapsed, DURATION, 7);
    }

    /** A weather at {@code intensity} in its fade-out. */
    private static RingWeatherState fadingOut(RingWeatherType type, float intensity) {
        return new RingWeatherState(type, Math.round(intensity * type.fadeTicks()), DURATION, 7);
    }

    @Test
    void contractStartValues() {
        assertEquals(1.5F, defaultMultiplier(RingWeatherType.ASH_RAIN));
        assertEquals(2.0F, defaultMultiplier(RingWeatherType.ERUPTION));
        assertEquals(3.0F, defaultMultiplier(RingWeatherType.ASH_CLOUD));
    }

    @Test
    void weatherFactorFollowsTheFade() {
        assertEquals(1.0F, factor(RingWeatherType.ASH_RAIN, 0.0F), 1.0E-6F);
        assertEquals(1.25F, factor(RingWeatherType.ASH_RAIN, 0.5F), 1.0E-6F);
        assertEquals(1.5F, factor(RingWeatherType.ASH_RAIN, 1.0F), 1.0E-6F);
        assertEquals(1.5F, factor(RingWeatherType.ERUPTION, 0.5F), 1.0E-6F);
        assertEquals(2.0F, factor(RingWeatherType.ERUPTION, 1.0F), 1.0E-6F);
        assertEquals(1.25F, HeatWeatherModifier.factor(fadingOut(RingWeatherType.ASH_RAIN, 0.5F),
                defaultMultiplier(RingWeatherType.ASH_RAIN)), 1.0E-6F);
    }

    @Test
    void ashCloudAddsNoHeatDuringItsWarning() {
        assertEquals(400, RingWeatherType.ASH_CLOUD.fadeTicks());
        assertEquals(1.0F, factor(RingWeatherType.ASH_CLOUD, 0.0F), 1.0E-6F);
        assertEquals(1.0F, factor(RingWeatherType.ASH_CLOUD, 0.5F), 1.0E-6F);
        assertEquals(1.0F, factor(RingWeatherType.ASH_CLOUD, 399.0F / 400.0F), 1.0E-6F);
        assertEquals(3.0F, factor(RingWeatherType.ASH_CLOUD, 1.0F), 1.0E-6F);
        assertEquals(3.0F, HeatWeatherModifier.factor(new RingWeatherState(RingWeatherType.ASH_CLOUD, DURATION / 2,
                DURATION, 7), defaultMultiplier(RingWeatherType.ASH_CLOUD)), 1.0E-6F);
        // The fade-out is no warning: the heat follows the fade like every other weather.
        assertEquals(2.0F, HeatWeatherModifier.factor(fadingOut(RingWeatherType.ASH_CLOUD, 0.5F),
                defaultMultiplier(RingWeatherType.ASH_CLOUD)), 1.0E-6F);
    }

    @Test
    void weatherOnlyCountsUnderOpenSky() {
        RingWeatherState eruption = weather(RingWeatherType.ERUPTION, 1.0F);
        float multiplier = defaultMultiplier(RingWeatherType.ERUPTION);
        assertEquals(1.1F, HeatWeatherModifier.weathered(0.55F, true, eruption, multiplier), 1.0E-6F);
        assertEquals(0.55F, HeatWeatherModifier.weathered(0.55F, false, eruption, multiplier), 1.0E-6F);
        RingWeatherState cloud = weather(RingWeatherType.ASH_CLOUD, 1.0F);
        assertEquals(0.55F, HeatWeatherModifier.weathered(0.55F, false, cloud,
                defaultMultiplier(RingWeatherType.ASH_CLOUD)), 1.0E-6F);
    }

    @Test
    void otherWeatherDoesNothing() {
        for (RingWeatherType type : RingWeatherType.values()) {
            if (type != RingWeatherType.ASH_RAIN && type != RingWeatherType.ERUPTION
                    && type != RingWeatherType.ASH_CLOUD) {
                assertEquals(1.0F, HeatWeatherModifier.multiplier(type), type.name());
                assertEquals(1.0F, factor(type, 1.0F), type.name());
            }
        }
        assertEquals(1.0F, HeatWeatherModifier.factor(RingWeatherState.STILL,
                HeatWeatherModifier.multiplier(RingWeatherState.STILL.type())));
    }

    @Test
    void heatModifiersOnlyTouchRisingHeat() {
        assertTrue(HeatRule.isRisingHeat(Condition.HEAT, 0.55F));
        assertFalse(HeatRule.isRisingHeat(Condition.HEAT, -0.5F));
        assertFalse(HeatRule.isRisingHeat(Condition.HEAT, 0.0F));
        assertFalse(HeatRule.isRisingHeat(Condition.DEHYDRATION, 0.4F));
        assertFalse(HeatRule.isRisingHeat(Condition.DEBUG, 5.0F));
    }

    @Test
    void pipelineRunsByStageThenRegistrationOrder() {
        RateModifierPipeline pipeline = new RateModifierPipeline();
        List<String> calls = new ArrayList<>();
        pipeline.add(RateModifier.Stage.PURIFYING, tracer(calls, "purifying"));
        pipeline.add(RateModifier.Stage.FIRE_RESISTANCE, tracer(calls, "fire"));
        pipeline.add(RateModifier.Stage.WEATHER, tracer(calls, "weather1"));
        pipeline.add(RateModifier.Stage.GEAR, tracer(calls, "gear"));
        pipeline.add(RateModifier.Stage.WEATHER, tracer(calls, "weather2"));
        pipeline.ordered().forEach(modifier -> modifier.modify(Condition.HEAT, null, 1.0F));
        assertEquals(List.of("weather1", "weather2", "gear", "fire", "purifying"), calls);
    }

    @Test
    void tickerAppliesTheModifiersInPipelineOrder() {
        RateModifierPipeline pipeline = new RateModifierPipeline();
        // A purifying modifier that turns any rise into a fall must see the result of the multipliers before it.
        pipeline.add(RateModifier.Stage.PURIFYING, (condition, context, rate) -> rate - 1.0F);
        pipeline.add(RateModifier.Stage.WEATHER, (condition, context, rate) -> rate * 2.0F);
        assertEquals(0.1F, ConditionTicker.rate(Condition.HEAT, null, 0.55F, pipeline.ordered(), true), 1.0E-6F);
    }

    private static float factor(RingWeatherType type, float intensity) {
        return HeatWeatherModifier.factor(weather(type, intensity), defaultMultiplier(type));
    }

    /**
     * The production mapping (type → config value) with the config at its defaults: {@link HeatWeatherModifier#multiplier}
     * reads the loaded server config, which a unit test does not have.
     */
    private static float defaultMultiplier(RingWeatherType type) {
        return (float) ServerConfig.RING_3_WEATHER.heatMultiplier(type, ModConfigSpec.DoubleValue::getDefault);
    }

    private static RateModifier tracer(List<String> calls, String name) {
        return (condition, context, rate) -> {
            calls.add(name);
            return rate;
        };
    }
}
