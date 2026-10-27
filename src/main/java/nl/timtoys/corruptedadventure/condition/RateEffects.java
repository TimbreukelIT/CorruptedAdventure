package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import nl.timtoys.corruptedadventure.registry.ModAttachments;

/**
 * Server API for the timed {@link RateEffect}s of a player, and their {@link RateModifier} (stage
 * {@link RateModifier.Stage#GEAR}: what the player wears and drinks). With a coating the factors multiply.
 */
public final class RateEffects implements RateModifier {
    RateEffects() {
    }

    public static RateEffectTimers timers(ServerPlayer player) {
        return player.getData(ModAttachments.RATE_EFFECTS);
    }

    public static void start(ServerPlayer player, RateEffect effect, int ticks) {
        timers(player).start(effect, player.serverLevel().getGameTime(), ticks);
    }

    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        if (rate <= 0.0F || !context.player().hasData(ModAttachments.RATE_EFFECTS)) {
            return rate;
        }
        return timers(context.player()).modify(condition, rate, context.level().getGameTime(), RateEffect::factor);
    }
}
