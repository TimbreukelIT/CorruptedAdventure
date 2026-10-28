package nl.timtoys.corruptedadventure.entity.nightmare.director;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.timtoys.corruptedadventure.config.NightmareDirectorConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.registry.ModAttachments;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingWorld;

/**
 * The server-side threat tick: once a second per player (on the player's own tick counter), only in the overworld of a
 * ring world, the threat of the ring the player stands in rises ({@link NightmareThreat#rise}). Only for players who
 * count in a fight ({@link Nightmare#isFighter}: alive, no creative, no spectator); fake players never; on Peaceful it
 * stands still.
 */
public final class NightmareThreats {
    static final int INTERVAL_TICKS = 20;

    private NightmareThreats() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PlayerTickEvent.Post.class, NightmareThreats::onPlayerTick);
    }

    public static NightmareThreat of(Player player) {
        return player.getData(ModAttachments.NIGHTMARE_THREAT);
    }

    /** The ring whose Nightmare a position draws: {@code ringAt}, with the void edge counting as ring 5. */
    public static Ring ringAt(double x, double z) {
        return NightmareRings.LAYOUT.ringAt(x, z);
    }

    static Ring countedRing(Ring ring) {
        return ring == Ring.VOID ? Ring.RING_5 : ring;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)
                && player.tickCount % INTERVAL_TICKS == 0 && Level.OVERWORLD.equals(player.level().dimension())
                && RingWorld.isActive(player.server) && !NightmareDirector.isPeaceful(player.serverLevel())) {
            rise(player, ringAt(player.getX(), player.getZ()), 1);
        }
    }

    /** {@code seconds} of threat in {@code ring} with the config's numbers; nothing for a player out of the fight. */
    public static void rise(ServerPlayer player, Ring ring, int seconds) {
        if (!Nightmare.isFighter(player)) {
            return;
        }
        NightmareDirectorConfig config = ServerConfig.NIGHTMARE_DIRECTOR;
        double[] days = config.threatDays();
        of(player).rise(ring, seconds, config.threatThreshold.get().floatValue(), days[0], days[1], player.getRandom());
    }
}
