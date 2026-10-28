package nl.timtoys.corruptedadventure.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * "Stunned" ({@code corruptedadventure:stunned}), given by the volcanite horn (phase 4, ring 3 weakness): frozen stiff.
 * Walking, flying and jumping speed drop to zero (attribute modifiers, so vanilla adds and removes them with the
 * effect); a mob stops its path and loses its target and cannot take a new one ({@link WeakenedTargeting}), so it
 * cannot attack either. Gravity still works: what the horn stuns in the air falls if it has gravity. A Nightmare can
 * react on top of this (The Risen stiffens and falls): it checks for the effect itself.
 */
public final class StunnedMobEffect extends MobEffect {
    public StunnedMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x9A8C84);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, CorruptedAdventure.id("effect.stunned"), -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.FLYING_SPEED, CorruptedAdventure.id("effect.stunned"), -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, CorruptedAdventure.id("effect.stunned"), -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide() && entity instanceof Mob mob) {
            mob.getNavigation().stop();
            WeakenedTargeting.dropTarget(mob);
        }
        return true;
    }
}
