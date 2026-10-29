package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/**
 * A player in The Layered's beak cannot get off by sneaking: the server refuses that dismount. Everything else that ends
 * a ride still does (it lets go itself, the player dies, logs out or teleports, e.g. with an ender pearl). Server-side
 * only, so the client never disagrees about who rides what.
 */
public final class LayeredGrip {
    private LayeredGrip() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LayeredGrip::onMount);
    }

    private static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide()
                && event.getEntityBeingMounted() instanceof TheLayered layered && layered.isAlive() && !layered.isReleasing()
                && event.getEntityMounting() instanceof Player player && player.isAlive() && !player.isRemoved()
                && player.isShiftKeyDown()) {
            event.setCanceled(true);
        }
    }
}
