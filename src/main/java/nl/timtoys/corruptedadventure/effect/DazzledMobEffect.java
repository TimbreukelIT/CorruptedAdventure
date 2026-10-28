package nl.timtoys.corruptedadventure.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * "Dazzled" ({@code corruptedadventure:dazzled}), given by the sunstone flash (phase 4, ring 2 weakness): a short, mild
 * blindness. A mob loses its target and cannot take a new one while it lasts ({@link WeakenedTargeting}); a player sees
 * a white-out that fades ({@code client/effect/DazzleOverlay}), no gameplay effect beyond that. A Nightmare can react
 * on top of this (The Layered loses its grip): it checks for the effect itself.
 */
public final class DazzledMobEffect extends MobEffect {
    public DazzledMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFD25E);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        blind(entity);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        blind(entity);
        return true;
    }

    private static void blind(LivingEntity entity) {
        if (!entity.level().isClientSide() && entity instanceof Mob mob) {
            WeakenedTargeting.dropTarget(mob);
        }
    }
}
