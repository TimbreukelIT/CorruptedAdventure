package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * A player in The Layered's beak cannot get off by sneaking ({@link TheLayered#holdOn} clears their sneak before it
 * counts), but everything else that ends a ride still does: it lets go itself, the player dies, or teleports with an
 * ender pearl or chorus fruit. Logging out lets go too: vanilla would otherwise save The Layered as the player's vehicle
 * and take it out of the world until they come back. Server-side only.
 */
public final class LayeredGrip {
    private LayeredGrip() {
    }

    public static void register(IEventBus gameBus) {
        // Fired at the start of PlayerList.remove, before the player (with the vehicle they ride) is saved.
        gameBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> letGo(event.getEntity()));
        // A player saved while held (an autosave, then a crash) comes back riding a copy of it: let go there too.
        gameBus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> letGo(event.getEntity()));
    }

    /** The Layered that holds {@code player}, if any, lets go of them. */
    private static void letGo(Player player) {
        if (!player.level().isClientSide() && player.getVehicle() instanceof TheLayered layered) {
            layered.releaseHeld();
        }
    }
}
