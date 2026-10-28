package nl.timtoys.corruptedadventure.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * A dazzled or stunned mob has no target: the effects drop it every tick, and this listener (server, game bus) cancels
 * every new target while either lasts, so a target goal cannot pick one up again between two effect ticks. Covers goal
 * mobs ({@code setTarget}) and brain mobs (their attack target memory) alike.
 */
public final class WeakenedTargeting {
    private WeakenedTargeting() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(WeakenedTargeting::onChangeTarget);
    }

    /** Whether {@code entity} can take no target now. */
    public static boolean blinded(LivingEntity entity) {
        return entity.hasEffect(ModMobEffects.DAZZLED) || entity.hasEffect(ModMobEffects.STUNNED);
    }

    static void dropTarget(Mob mob) {
        if (mob.getTarget() != null) {
            mob.setTarget(null);
        }
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && blinded(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
