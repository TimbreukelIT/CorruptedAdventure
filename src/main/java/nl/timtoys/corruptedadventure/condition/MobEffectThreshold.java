package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * A mob effect while the threshold is reached. Refreshed every second with a long duration, because vanilla makes the
 * icon blink below 200 ticks; ambient and without particles. Below the threshold it is removed at once, but only if
 * the active instance is ambient (any amplifier: the config may have changed since it was applied), so an active
 * potion of the same effect survives. Known limit: if our instance is stronger than a potion, vanilla hides the potion
 * under ours, and removing ours removes the hidden potion too (its instance is not reachable).
 */
public record MobEffectThreshold(Holder<MobEffect> effect, int amplifier) implements ThresholdEffect {
    /** 12 s: the handler refreshes every second, so the remaining duration never drops to vanilla's 200-tick blink. */
    static final int DURATION_TICKS = 240;

    @Override
    public void apply(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(effect, DURATION_TICKS, amplifier, true, false, true));
    }

    @Override
    public void clear(ServerPlayer player) {
        MobEffectInstance active = player.getEffect(effect);
        if (active != null && active.isAmbient()) {
            player.removeEffect(effect);
        }
    }
}
