package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import nl.timtoys.corruptedadventure.config.ServerConfig;

import java.util.EnumSet;

/**
 * Chases the target and slams it with the giant arm. Unlike vanilla's melee goal the slam has a windup (the arm goes up,
 * then comes down) so a player can step out of reach; the hit only lands if the target is still in reach then. No new
 * slam while out of phase, and a windup that ends while out of phase hits nothing.
 */
final class HollowSlamGoal extends Goal {
    private static final int REPATH_TICKS = 10;

    private final TheHollow hollow;
    private int windup;
    private int nextSlamTick;
    private int repath;

    HollowSlamGoal(TheHollow hollow) {
        this.hollow = hollow;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = hollow.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
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
        hollow.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = hollow.getTarget();
        if (target == null) {
            return;
        }
        hollow.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (windup > 0) {
            if (--windup == 0) {
                if (!hollow.isPhased() && hollow.isWithinMeleeAttackRange(target)) {
                    hollow.doHurtTarget(target);
                    HollowEffects.slamHit((ServerLevel) hollow.level(), hollow);
                }
                nextSlamTick = hollow.tickCount + ServerConfig.HOLLOW.slamCooldownTicks.get();
            }
            return;
        }
        if (hollow.isWithinMeleeAttackRange(target) && hollow.tickCount >= nextSlamTick && !hollow.isPhased()) {
            hollow.getNavigation().stop();
            windup = ServerConfig.HOLLOW.slamWindupTicks.get();
            hollow.triggerAnim(HollowAnimations.ACTION, HollowAnimations.SLAM);
            HollowEffects.slamWindup((ServerLevel) hollow.level(), hollow);
            return;
        }
        if (--repath <= 0) {
            repath = REPATH_TICKS;
            hollow.getNavigation().moveTo(target, 1.0);
        }
    }
}
