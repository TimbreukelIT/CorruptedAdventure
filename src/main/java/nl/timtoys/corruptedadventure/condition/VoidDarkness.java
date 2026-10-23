package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * The darkness of the void: from the second threshold in the outer band and on the void rim, from the third everywhere.
 * Decided in this one place from the reached threshold count; on the void's side driven only by {@link VoidSlip} (the
 * second threshold is always reached when the third is, so its {@code apply}/{@code clear} runs whenever the void's
 * darkness may change). So crossing a threshold or a band never removes and re-adds the effect within one tick (that
 * would restart vanilla's darkness fade-in on the client), and it is refreshed once per second. The effect itself (ours
 * versus a foreign one, combined with the silence's, which {@link SilenceHeartbeat} drives) is
 * {@link ConditionDarkness}.
 */
final class VoidDarkness {
    /** The threshold count (cumulative) from which darkness holds everywhere: the third. */
    static final int EVERYWHERE_FROM = 3;
    /** The threshold count from which darkness holds in the outer band and on the void rim: the second. */
    static final int OUTER_BAND_FROM = 2;

    private VoidDarkness() {
    }

    /**
     * Applies or removes our darkness for the player's reached void thresholds (and keeps it while the silence wants
     * it). Reads {@code ConditionMeters.appliedThresholds}, which {@link ConditionTicker} updates for every condition
     * before any threshold effect runs.
     */
    static void update(ServerPlayer player, VoidZone zone) {
        ConditionDarkness.update(player, ConditionDarkness.wanted(player, amplifier(reached(player), zone)));
    }

    /** The darkness the void wants now, without a zone at hand: the band is only sampled at the second threshold. */
    static int amplifier(ServerPlayer player) {
        int reached = reached(player);
        if (reached < OUTER_BAND_FROM) {
            return ConditionDarkness.NONE;
        }
        return amplifier(reached, reached >= EVERYWHERE_FROM ? VoidZone.INNER : VoidSlip.zoneOf(player));
    }

    private static int amplifier(int reached, VoidZone zone) {
        return wanted(reached, zone) ? ServerConfig.VOID.darknessAmplifier.get() : ConditionDarkness.NONE;
    }

    private static int reached(ServerPlayer player) {
        return PlayerConditions.meters(player).appliedThresholds(Condition.VOID);
    }

    /**
     * Darkness from the third threshold everywhere, from the second only in the outer band (and on the rim). Outside the
     * zone the second threshold has no effect: fleeing the ring is rewarded at once, while the meter falls.
     */
    static boolean wanted(int reachedThresholds, VoidZone zone) {
        return reachedThresholds >= EVERYWHERE_FROM || reachedThresholds >= OUTER_BAND_FROM && zone == VoidZone.OUTER;
    }
}
