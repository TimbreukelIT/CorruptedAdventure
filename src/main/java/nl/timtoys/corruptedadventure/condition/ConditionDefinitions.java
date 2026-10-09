package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModDamageTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Per condition its rule and thresholds, plus the shared rate modifiers. Numbers come from {@link ServerConfig} (read
 * live); which effect belongs to which threshold is decided here. Server-side use, except {@link #thresholdLevels}
 * (the HUD reads the synced config).
 */
public final class ConditionDefinitions {
    private static final RateModifierPipeline MODIFIERS = new RateModifierPipeline();

    private ConditionDefinitions() {
    }

    public static ConditionRule rule(Condition condition) {
        return switch (condition) {
            case DEBUG -> DebugConditionDriver.rule(
                    ServerConfig.DEBUG.riseRate.get().floatValue(), ServerConfig.DEBUG.fallRate.get().floatValue());
            case DEHYDRATION -> DehydrationRule.fromConfig();
            case HEAT -> HeatRule.fromConfig();
            case COLD -> ColdRule.fromConfig();
        };
    }

    /** The configured threshold levels, rising (also on the client, for the HUD ticks). */
    public static int[] thresholdLevels(Condition condition) {
        return switch (condition) {
            case DEBUG -> ServerConfig.DEBUG.thresholds.get();
            case DEHYDRATION -> ServerConfig.DEHYDRATION.thresholds.get();
            case HEAT -> ServerConfig.HEAT.thresholds.get();
            case COLD -> ServerConfig.COLD.thresholds.get();
        };
    }

    /** The thresholds in rising order. */
    public static List<Threshold> thresholds(Condition condition) {
        int[] levels = thresholdLevels(condition);
        List<ThresholdEffect> effects = effects(condition);
        List<Threshold> thresholds = new ArrayList<>(levels.length);
        for (int i = 0; i < levels.length; i++) {
            thresholds.add(new Threshold(levels[i], effects.get(i)));
        }
        return thresholds;
    }

    /** One effect per threshold, in the order of {@code threshold1..N}. */
    private static List<ThresholdEffect> effects(Condition condition) {
        return switch (condition) {
            case DEBUG -> List.of(
                    new MobEffectThreshold(MobEffects.MOVEMENT_SLOWDOWN, ServerConfig.DEBUG.effect1Amplifier.get()),
                    new MobEffectThreshold(MobEffects.WEAKNESS, ServerConfig.DEBUG.effect2Amplifier.get()),
                    new DamageThreshold(DamageTypes.GENERIC, ServerConfig.DEBUG.damage.get().floatValue(),
                            ServerConfig.DEBUG.damageInterval.get(), DifficultyFloor.NONE));
            case DEHYDRATION -> {
                ServerConfig.DehydrationCondition config = ServerConfig.DEHYDRATION;
                yield List.of(
                        new MobEffectThreshold(MobEffects.HUNGER, config.effect1Amplifier.get()),
                        new MobEffectThreshold(MobEffects.MOVEMENT_SLOWDOWN, config.effect2Amplifier.get()),
                        new MobEffectThreshold(MobEffects.DIG_SLOWDOWN, config.effect3Amplifier.get()),
                        new DamageThreshold(ModDamageTypes.DEHYDRATION, config.damage.get().floatValue(),
                                config.damageInterval.get(), DifficultyFloor.VANILLA_STARVATION));
            }
            case HEAT -> {
                ServerConfig.HeatCondition config = ServerConfig.HEAT;
                yield List.of(
                        new MobEffectThreshold(MobEffects.MOVEMENT_SLOWDOWN, config.effect1Amplifier.get()),
                        new MobEffectThreshold(MobEffects.WEAKNESS, config.effect2Amplifier.get()),
                        new MobEffectThreshold(MobEffects.DIG_SLOWDOWN, config.effect3Amplifier.get()),
                        HeatBurn.IGNITE);
            }
            case COLD -> {
                ServerConfig.ColdCondition config = ServerConfig.COLD;
                yield List.of(
                        new MobEffectThreshold(MobEffects.MOVEMENT_SLOWDOWN, config.effect1Amplifier.get()),
                        new MobEffectThreshold(MobEffects.DIG_SLOWDOWN, config.effect2Amplifier.get()),
                        ColdFreeze.FREEZE);
            }
        };
    }

    /** Adds a ring weather modifier ({@link RateModifier.Stage#WEATHER}); call during mod setup. */
    public static void addModifier(RateModifier modifier) {
        addModifier(RateModifier.Stage.WEATHER, modifier);
    }

    /**
     * Adds a modifier to the pipeline of every condition, in its stage (weather → gear → fire resistance → purifying;
     * within a stage in registration order); call during mod setup.
     */
    public static void addModifier(RateModifier.Stage stage, RateModifier modifier) {
        MODIFIERS.add(stage, modifier);
    }

    /** All modifiers in pipeline order. */
    public static List<RateModifier> modifiers() {
        return MODIFIERS.ordered();
    }
}
