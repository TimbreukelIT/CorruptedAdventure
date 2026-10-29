package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Chases the target (up walls too: {@code WallClimberNavigation}) and bites it with the beak. The bite has a windup (it
 * rears up, then stabs down) so a player can step out of reach; it only lands if the target is still within
 * {@code biteReach} of the beak's tip then. The grab ({@link LayeredGrabGoal}) is the same stab that holds on; it takes
 * over when its cooldown allows. It chases whenever it may; the bite itself waits for {@link TheLayered#mayStartAttack()}.
 */
final class LayeredBiteGoal extends Goal {
    private static final int REPATH_TICKS = 10;

    private final TheLayered layered;
    private int windup;
    private int nextBiteTick;
    private int repath;

    LayeredBiteGoal(TheLayered layered) {
        this.layered = layered;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = layered.getTarget();
        return target != null && target.isAlive() && layered.canAct() && !layered.isVehicle();
    }

    @Override
    public boolean canContinueToUse() {
        if (windup == 0) {
            return canUse();
        }
        // A bite that has begun lands, even if it was the last of a burst (countAttack starts the rest window); not
        // once its target is gone or it lost its grip (stop() ends the windup).
        LivingEntity target = layered.getTarget();
        return target != null && target.isAlive() && layered.canAct();
    }

    @Override
    public boolean isInterruptable() {
        return windup == 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        windup = 0;
        layered.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = layered.getTarget();
        if (target == null) {
            return;
        }
        layered.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (windup > 0) {
            if (--windup == 0) {
                if (layered.inBiteReach(target)) {
                    layered.doHurtTarget(target);
                    LayeredEffects.bite((ServerLevel) layered.level(), layered);
                }
                nextBiteTick = layered.tickCount + TheLayered.settings().biteCooldownTicks.get();
            }
            return;
        }
        if (layered.inBiteReach(target) && layered.tickCount >= nextBiteTick && layered.mayStartAttack()) {
            layered.getNavigation().stop();
            layered.countAttack();
            windup = TheLayered.settings().biteWindupTicks.get();
            layered.triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.GRAB);
            LayeredEffects.biteWindup((ServerLevel) layered.level(), layered);
            return;
        }
        if (--repath <= 0) {
            repath = REPATH_TICKS;
            layered.getNavigation().moveTo(target, 1.0);
        }
    }
}
