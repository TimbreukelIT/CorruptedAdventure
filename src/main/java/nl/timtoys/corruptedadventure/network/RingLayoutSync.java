package nl.timtoys.corruptedadventure.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingWorld;

/**
 * Sends the ring layout to each player once, on login (also the host of an integrated server). Not again on a
 * dimension change or respawn: one layout covers the overworld and the Nether, and it is frozen per world.
 */
public final class RingLayoutSync {
    private RingLayoutSync() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RingLayoutSync::onPlayerLoggedIn);
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RingWorld.isActive(player.server)) {
            PacketDistributor.sendToPlayer(player, RingLayoutPayload.of(RingLayout.current()));
        }
    }
}
