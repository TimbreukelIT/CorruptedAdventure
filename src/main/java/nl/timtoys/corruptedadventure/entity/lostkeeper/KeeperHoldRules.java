package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * A fighter The Lost Keeper holds is its passenger. Sneaking does not get them off ({@link TheLostKeeper} clears the key
 * every tick); damage to the keeper, its letting go, death, a teleport and logging out do. Server-side rule on the game
 * bus.
 */
public final class KeeperHoldRules {
    private KeeperHoldRules() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, KeeperHoldRules::onLoggedOut);
    }

    /** Fired before PlayerList.remove saves the player: they leave free, and the keeper stays in the world. */
    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.getVehicle() instanceof TheLostKeeper keeper) {
            keeper.dropHeld();
        }
    }
}
