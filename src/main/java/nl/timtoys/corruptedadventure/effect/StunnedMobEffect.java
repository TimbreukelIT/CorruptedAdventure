package nl.timtoys.corruptedadventure.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * "Stunned" ({@code corruptedadventure:stunned}), given by the volcanite horn (phase 4, ring 3 weakness): frozen stiff.
 * Walking, flying and jumping speed drop to zero (attribute modifiers, so vanilla adds and removes them with the
 * effect); a mob stops its path, its sideways motion and loses its target and cannot take a new one
 * ({@link WeakenedTargeting}), so it cannot attack either. Gravity still works: what the horn stuns in the air falls if
 * it has gravity, a flier without it hangs still. A Nightmare can react on top of this (The Risen stiffens and falls): it
 * checks for the effect itself.
 */
public final class StunnedMobEffect extends MobEffect {
    private static final ResourceLocation MODIFIER_ID = CorruptedAdventure.id("effect.stunned");

    public StunnedMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x9A8C84);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.FLYING_SPEED, MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        freeze(entity);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        freeze(entity);
        return true;
    }

    /**
     * After a stunned mob's tick (server, game bus, registered with {@link WeakenedTargeting#register}): back to where it
     * stood sideways. Fliers with their own move control (phantom, vex, ghast) steer by setting their motion straight,
     * after the effect tick and regardless of the speed attributes, so only this keeps them in place.
     */
    static void hold(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide() && held(mob)
                && mob.hasEffect(ModMobEffects.STUNNED)) {
            mob.setPos(mob.xo, mob.getY(), mob.zo);
            stopSideways(mob);
        }
    }

    private static void freeze(LivingEntity entity) {
        if (!entity.level().isClientSide() && entity instanceof Mob mob) {
            mob.getNavigation().stop();
            if (held(mob)) {
                stopSideways(mob);
            }
            WeakenedTargeting.dropTarget(mob);
        }
    }

    /** A mount a player steers (a ridden horse) is never stunned: its speed modifier would freeze the player too. */
    static void applicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(ModMobEffects.STUNNED)
                && event.getEntity().getControllingPassenger() instanceof Player) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** A hit still knocks it back (for as long as it flinches); a rider goes where its vehicle goes. */
    private static boolean held(Mob mob) {
        return mob.hurtTime == 0 && !mob.isPassenger();
    }

    private static void stopSideways(Mob mob) {
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.0, 1.0, 0.0));
    }
}
