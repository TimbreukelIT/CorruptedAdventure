package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * What one consumable does to one condition (fase-2-items.md §5.3): points off (or on) the meter at once, and optionally
 * a timed {@link RateEffect}. Server-side; build it when the item is used, so the config is read live
 * ({@link ConsumableDoses}).
 *
 * @param meterDelta    added to the meter at once; negative lowers it
 * @param effectSeconds how long {@code effect} lasts; ignored without an effect
 */
public record ConditionDose(Condition condition, float meterDelta, Optional<RateEffect> effect, int effectSeconds) {
    public static ConditionDose of(Condition condition, float meterDelta) {
        return new ConditionDose(condition, meterDelta, Optional.empty(), 0);
    }

    public static ConditionDose withEffect(Condition condition, float meterDelta, RateEffect effect, int seconds) {
        return new ConditionDose(condition, meterDelta, Optional.of(effect), seconds);
    }

    public void apply(ServerPlayer player) {
        float change = meterChange(meterDelta, !player.isCreative() && !player.isSpectator());
        if (change != 0.0F) {
            PlayerConditions.add(player, condition, change);
        }
        effect.ifPresent(rateEffect -> RateEffects.start(player, rateEffect, effectSeconds * 20));
    }

    /** Like the ticker: in creative and spectator a meter never rises, so a positive dose does nothing there. */
    static float meterChange(float delta, boolean exposed) {
        return exposed || delta < 0.0F ? delta : 0.0F;
    }
}
