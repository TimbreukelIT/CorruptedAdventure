package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * The second void threshold (package 91), one effect that looks at the band every second:
 * <ul>
 *     <li><b>inner band:</b> every {@code teleportIntervalSeconds} a short teleport ({@link VoidTeleport}). The wait
 *     starts on entering the inner band at or above the threshold. When it is due but the player is not standing on
 *     the ground (jumping, riding, gliding, swimming) the teleport waits for the ground; when there is no safe spot it is
 *     skipped and the wait starts again;</li>
 *     <li><b>outer band and void rim:</b> darkness instead, no teleports;</li>
 *     <li><b>outside the zone:</b> nothing (the meter falls there).</li>
 * </ul>
 * It also drives the darkness of the third threshold ({@link VoidDarkness}): the second threshold is reached whenever
 * the third is, so this effect runs every second while any void darkness may hold, and once more when it stops.
 */
final class VoidSlip implements ThresholdEffect {
    static final VoidSlip SLIP = new VoidSlip();

    private final SlipSchedule<ServerPlayer> schedule = new SlipSchedule<>();

    private VoidSlip() {
    }

    @Override
    public void apply(ServerPlayer player) {
        VoidZone zone = zoneOf(player);
        VoidDarkness.update(player, zone);
        if (!teleports(zone)) {
            schedule.forget(player);
            return;
        }
        ServerConfig.VoidCondition config = ServerConfig.VOID;
        int now = player.server.getTickCount();
        int interval = config.teleportIntervalSeconds.get() * ConditionTicker.INTERVAL_TICKS;
        if (!schedule.isDue(player, now, interval) || !VoidTeleport.Stance.of(player).mayTeleport()) {
            return;
        }
        VoidTeleport.tryTeleport(player, config.teleportMinBlocks.get(), config.teleportMax());
        schedule.restart(player, now);
    }

    @Override
    public void clear(ServerPlayer player) {
        schedule.forget(player);
        VoidDarkness.update(player, zoneOf(player));
    }

    /** Teleports only in the inner band; the outer band and the void rim get darkness instead. */
    static boolean teleports(VoidZone zone) {
        return zone == VoidZone.INNER;
    }

    static VoidZone zoneOf(ServerPlayer player) {
        return VoidZone.of(player.level().dimension(),
                RingLayout.ringAt(player.level().dimension(), player.getX(), player.getZ()), player.getX(), player.getZ());
    }
}
