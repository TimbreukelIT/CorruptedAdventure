package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import nl.timtoys.corruptedadventure.condition.SuckingMudDamage;

/**
 * Suffocation in sucking mud (G2–G3 of golf 2), server-side every tick for every living entity (players and mobs)
 * whose eyes are in the mud. It runs as an entity tick, not in {@code entityInside}: the server only moves a player
 * when its client sends a position, and a player stuck at the bottom sends few.
 *
 * <p>The head under the mud uses up the breath like water does, but faster: the air bar empties in about three seconds
 * ("a few seconds", the bubbles on the HUD are the warning), then {@link #DAMAGE} every {@link #DAMAGE_INTERVAL} ticks
 * through C's {@link SuckingMudDamage} (own damage type, difficulty floor). Out of the mud the breath comes back as
 * after diving. Creative and spectator players are left alone.
 */
public final class SuckingMudSuffocation {
    /** Air lost per tick with the head under; vanilla gives 4 back every tick outside water, so 5 net: 300 in 3 s. */
    static final int AIR_LOSS_PER_TICK = 9;
    static final int DAMAGE_INTERVAL = 20;
    /** One heart per second, like drowning. */
    static final float DAMAGE = 2.0F;

    private SuckingMudSuffocation() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SuckingMudSuffocation::onEntityTick);
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide() || !living.isAlive()
                || living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (!(living.level().getBlockState(BlockPos.containing(living.getEyePosition())).getBlock() instanceof SuckingMudBlock)) {
            return;
        }
        int air = nextAir(living.getAirSupply());
        living.setAirSupply(air);
        if (hurtsNow(air, living.tickCount)) {
            SuckingMudDamage.hurt(living, DAMAGE);
        }
    }

    /** The breath left after a tick with the head under (never below 0). */
    static int nextAir(int air) {
        return Math.max(0, air - AIR_LOSS_PER_TICK);
    }

    /** Whether the mud hurts this tick: out of breath, once per interval. */
    static boolean hurtsNow(int air, int tickCount) {
        return air <= 0 && tickCount % DAMAGE_INTERVAL == 0;
    }
}
