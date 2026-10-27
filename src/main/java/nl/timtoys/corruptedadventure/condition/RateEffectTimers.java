package nl.timtoys.corruptedadventure.condition;

import com.mojang.serialization.Codec;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Player attachment: per {@link RateEffect} the game time it ends at. Server-only (never synced). Game time keeps
 * running while the player is offline, so an effect can run out during a relog; it is not copied on death.
 */
public final class RateEffectTimers {
    /** Name → end time; lenient like {@link ConditionMeters#CODEC}: an unknown name is skipped, not fatal. */
    public static final Codec<RateEffectTimers> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG)
            .xmap(RateEffectTimers::fromMap, RateEffectTimers::toMap);

    private final Map<RateEffect, Long> endTimes = new EnumMap<>(RateEffect.class);

    /** Starts (or renews) the effect; one of the same kind does not stack: the later end time wins. */
    public void start(RateEffect effect, long now, int ticks) {
        long end = now + Math.max(0, ticks);
        endTimes.merge(effect, end, Math::max);
    }

    public boolean isActive(RateEffect effect, long now) {
        return remainingTicks(effect, now) > 0;
    }

    public long remainingTicks(RateEffect effect, long now) {
        Long end = endTimes.get(effect);
        return end == null ? 0 : Math.max(0, end - now);
    }

    /** {@code rate} times the factor of every active effect on {@code condition}, only if it rises. */
    public float modify(Condition condition, float rate, long now, ToDoubleFunction<RateEffect> factor) {
        if (rate <= 0.0F) {
            return rate;
        }
        float result = rate;
        for (Map.Entry<RateEffect, Long> entry : endTimes.entrySet()) {
            RateEffect effect = entry.getKey();
            if (effect.condition() == condition && entry.getValue() > now) {
                result *= (float) factor.applyAsDouble(effect);
            }
        }
        return result;
    }

    private static RateEffectTimers fromMap(Map<String, Long> map) {
        RateEffectTimers timers = new RateEffectTimers();
        map.forEach((name, end) -> {
            RateEffect effect = byName(name);
            if (effect == null) {
                CorruptedAdventure.LOGGER.warn("Dropping the timer of unknown rate effect '{}'", name);
            } else {
                timers.endTimes.put(effect, end);
            }
        });
        return timers;
    }

    private static Map<String, Long> toMap(RateEffectTimers timers) {
        Map<String, Long> map = new LinkedHashMap<>();
        timers.endTimes.forEach((effect, end) -> map.put(effect.getSerializedName(), end));
        return map;
    }

    private static RateEffect byName(String name) {
        for (RateEffect effect : RateEffect.values()) {
            if (effect.getSerializedName().equals(name)) {
                return effect;
            }
        }
        return null;
    }
}
