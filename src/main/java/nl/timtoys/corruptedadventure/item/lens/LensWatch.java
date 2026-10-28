package nl.timtoys.corruptedadventure.item.lens;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.item.SkyGlassLensItem;
import nl.timtoys.corruptedadventure.network.LensScanPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server side of the sky glass lens (§7.2): a player who starts looking through it gets a {@link LensScan} at once
 * and then every {@link #SCAN_TICKS} while they look, sent to them alone; when they stop, one empty payload clears their
 * outlines. Nothing is kept beyond the next scan's time, so nothing is saved.
 */
public final class LensWatch {
    /** About once a second (§7.2). */
    static final int SCAN_TICKS = 20;

    /** Game time of the next scan per looking player; absent = not looking (nothing on their screen). */
    private static final Map<UUID, Long> NEXT_SCAN = new ConcurrentHashMap<>();

    private LensWatch() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LensWatch::onPlayerTick);
        gameBus.addListener(LensWatch::onLoggedOut);
    }

    /** Whether a looking player is scanned this tick: on the first tick of a look, then every {@link #SCAN_TICKS}. */
    static boolean due(Long next, long gameTime) {
        return next == null || gameTime >= next;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        UUID id = player.getUUID();
        if (!SkyGlassLensItem.lookingThrough(player)) {
            if (NEXT_SCAN.remove(id) != null) {
                PacketDistributor.sendToPlayer(player, LensScanPayload.NONE);
            }
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (!due(NEXT_SCAN.get(id), now)) {
            return;
        }
        NEXT_SCAN.put(id, now + SCAN_TICKS);
        PacketDistributor.sendToPlayer(player, LensScan.scan(player.serverLevel(), player.getEyePosition()));
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_SCAN.remove(event.getEntity().getUUID());
    }
}
