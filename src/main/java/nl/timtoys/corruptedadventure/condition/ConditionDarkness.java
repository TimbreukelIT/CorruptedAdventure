package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * The one darkness of the conditions, shared by the void ({@link VoidDarkness}, package 91) and the silence
 * ({@link SilenceHeartbeat}, package 97). Both ask {@link #update} with {@link #wanted}, the strongest amplifier any of
 * them wants now, so one condition dropping its darkness never removes the darkness another still wants (that would
 * remove and re-add it within one tick and restart vanilla's darkness fade-in on the client).
 * <p>
 * <b>Our darkness</b> is a long instance ({@link MobEffectThreshold#DURATION_TICKS}, refreshed every second) that is
 * ambient, without particles and with an icon ({@link #isOurs}). <b>A foreign darkness</b> (a warden, a sculk shrieker)
 * is respected like a foreign wither in {@link PoisonWither}: vanilla merging it into ours takes over its flags, so from
 * then on it is foreign; we never refresh over it (that would turn it into ours) and never remove it. Once it has
 * expired, the next second puts ours back.
 */
final class ConditionDarkness {
    /** No darkness wanted. */
    static final int NONE = -1;

    private ConditionDarkness() {
    }

    /** Ours at {@code amplifier}, or none for {@link #NONE}; a foreign darkness is left alone. */
    static void update(ServerPlayer player, int amplifier) {
        switch (decide(player.getEffect(MobEffects.DARKNESS), amplifier)) {
            case REFRESH -> player.addEffect(ours(amplifier));
            case LOWER -> {
                // addEffect keeps a stronger instance and only hides the weaker one under it, so lowering needs a reset.
                player.removeEffect(MobEffects.DARKNESS);
                player.addEffect(ours(amplifier));
            }
            case REMOVE -> player.removeEffect(MobEffects.DARKNESS);
            case NOTHING -> {
            }
        }
    }

    /** The darkness all conditions want now (the void's band is sampled only when needed). */
    static int wanted(ServerPlayer player) {
        return wanted(player, VoidDarkness.amplifier(player));
    }

    /** As {@link #wanted(ServerPlayer)}, with the void's amplifier already known; the one place that combines them. */
    static int wanted(ServerPlayer player, int voidAmplifier) {
        return combine(voidAmplifier, SilenceHeartbeat.darknessAmplifier(player));
    }

    /** The strongest wanted amplifier, or {@link #NONE}. */
    static int combine(int voidAmplifier, int silenceAmplifier) {
        return Math.max(NONE, Math.max(voidAmplifier, silenceAmplifier));
    }

    /** What to do with the active darkness for the wanted amplifier (pure, testable without a player). */
    static Action decide(MobEffectInstance active, int amplifier) {
        boolean ours = isOurs(active);
        if (amplifier <= NONE) {
            return ours ? Action.REMOVE : Action.NOTHING;
        }
        if (active == null) {
            return Action.REFRESH;
        }
        if (!ours) {
            return Action.NOTHING;
        }
        return active.getAmplifier() > amplifier ? Action.LOWER : Action.REFRESH;
    }

    /** Our instance: ambient, no particles, an icon. */
    static MobEffectInstance ours(int amplifier) {
        return new MobEffectInstance(MobEffects.DARKNESS, MobEffectThreshold.DURATION_TICKS, amplifier, true, false, true);
    }

    /** Whether the active darkness is ours (all three flags); a merged foreign one has lost at least one. */
    static boolean isOurs(MobEffectInstance active) {
        return active != null && active.isAmbient() && !active.isVisible() && active.showIcon();
    }

    enum Action {
        /** Add or refresh ours. */
        REFRESH,
        /** Ours is stronger than wanted: replace it. */
        LOWER,
        /** Ours is no longer wanted. */
        REMOVE,
        /** Nothing wanted and none of ours, or a foreign darkness that is left alone. */
        NOTHING
    }
}
