package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import nl.timtoys.corruptedadventure.config.ServerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per condition its rule and thresholds, plus the shared rate modifiers. Numbers come from {@link ServerConfig} (read
 * live); which effect belongs to which threshold is decided here. Server-side use, except {@link #thresholdLevels}
 * (the HUD reads the synced config).
 */
public final class ConditionDefinitions {
    private static final List<RateModifier> MODIFIERS = new CopyOnWriteArrayList<>();

    private ConditionDefinitions() {
    }

    public static ConditionRule rule(Condition condition) {
        return switch (condition) {
            case DEBUG -> DebugConditionDriver.rule(
                    ServerConfig.DEBUG.riseRate.get().floatValue(), ServerConfig.DEBUG.fallRate.get().floatValue());
        };
    }

    /** The configured threshold levels, rising (also on the client, for the HUD ticks). */
    public static int[] thresholdLevels(Condition condition) {
        return switch (condition) {
            case DEBUG -> ServerConfig.DEBUG.thresholds.get();
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
        };
    }

    /** Adds a modifier to the pipeline of every condition (e.g. from {@code weather/}); call during mod setup. */
    public static void addModifier(RateModifier modifier) {
        MODIFIERS.add(modifier);
    }

    public static List<RateModifier> modifiers() {
        return MODIFIERS;
    }
}
