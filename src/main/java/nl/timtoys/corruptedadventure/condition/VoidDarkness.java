package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * The darkness of the void: from the second threshold in the outer band and on the void rim, from the third everywhere.
 * Decided in this one place from the reached threshold count, and driven only by {@link VoidSlip} (the second threshold
 * is always reached when the third is, so its {@code apply}/{@code clear} runs whenever the darkness may change). So
 * crossing a threshold or a band never removes and re-adds the effect within one tick (that would restart vanilla's
 * darkness fade-in on the client), and it is refreshed once per second.
 * <p>
 * <b>Our darkness</b> is a long instance ({@link MobEffectThreshold#DURATION_TICKS}, refreshed every second) that is
 * ambient, without particles and with an icon ({@link #isOurs}). <b>A foreign darkness</b> (a warden, a sculk shrieker)
 * is respected like a foreign wither in {@link PoisonWither}: vanilla merging it into ours takes over its flags, so from
 * then on it is foreign; we never refresh over it (that would turn it into ours) and never remove it. Once it has
 * expired, the next second puts ours back.
 */
final class VoidDarkness {
    /** The threshold count (cumulative) from which darkness holds everywhere: the third. */
    static final int EVERYWHERE_FROM = 3;
    /** The threshold count from which darkness holds in the outer band and on the void rim: the second. */
    static final int OUTER_BAND_FROM = 2;

    private VoidDarkness() {
    }

    /**
     * Applies or removes our darkness for the player's reached void thresholds. Reads
     * {@code ConditionMeters.appliedThresholds}, which {@link ConditionTicker} updates for every condition before any
     * threshold effect runs.
     */
    static void update(ServerPlayer player, VoidZone zone) {
        int reached = PlayerConditions.meters(player).appliedThresholds(Condition.VOID);
        MobEffectInstance active = player.getEffect(MobEffects.DARKNESS);
        if (wanted(reached, zone)) {
            if (active == null || isOurs(active)) {
                player.addEffect(ours(ServerConfig.VOID.darknessAmplifier.get()));
            }
        } else if (isOurs(active)) {
            player.removeEffect(MobEffects.DARKNESS);
        }
    }

    /**
     * Darkness from the third threshold everywhere, from the second only in the outer band (and on the rim). Outside the
     * zone the second threshold has no effect: fleeing the ring is rewarded at once, while the meter falls.
     */
    static boolean wanted(int reachedThresholds, VoidZone zone) {
        return reachedThresholds >= EVERYWHERE_FROM || reachedThresholds >= OUTER_BAND_FROM && zone == VoidZone.OUTER;
    }

    /** Our instance: ambient, no particles, an icon. */
    static MobEffectInstance ours(int amplifier) {
        return new MobEffectInstance(MobEffects.DARKNESS, MobEffectThreshold.DURATION_TICKS, amplifier, true, false, true);
    }

    /** Whether the active darkness is ours (all three flags); a merged foreign one has lost at least one. */
    static boolean isOurs(MobEffectInstance active) {
        return active != null && active.isAmbient() && !active.isVisible() && active.showIcon();
    }
}
