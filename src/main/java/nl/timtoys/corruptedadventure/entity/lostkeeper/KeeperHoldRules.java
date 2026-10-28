package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/**
 * A fighter The Lost Keeper holds is its passenger: sneaking does not get them off (only damage to the keeper, its
 * letting go, death or a teleport do). Server-side rule on the game bus.
 */
public final class KeeperHoldRules {
    private KeeperHoldRules() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(KeeperHoldRules::onMount);
    }

    private static void onMount(EntityMountEvent event) {
        Entity rider = event.getEntityMounting();
        if (event.isDismounting() && !event.getLevel().isClientSide()
                && event.getEntityBeingMounted() instanceof TheLostKeeper keeper && keeper.isAlive()
                && !keeper.isReleasing() && rider.isAlive() && rider.isShiftKeyDown()) {
            event.setCanceled(true);
        }
    }
}
