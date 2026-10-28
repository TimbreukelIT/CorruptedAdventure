package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Verspringen: a crack warns, then The Hollow steps out of the world and comes out behind its target. Also how a giant
 * gets past trees and caves its path finding cannot. Never while pinned; a pin during the warning cancels the step. No
 * room behind the target: the step fizzles and the cooldown starts anyway.
 */
final class HollowRiftStepGoal extends Goal {
    private final TheHollow hollow;
    private int warning;
    private int nextStepTick;

    HollowRiftStepGoal(TheHollow hollow) {
        this.hollow = hollow;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        nextStepTick = cooldown();
    }

    @Override
    public boolean canUse() {
        LivingEntity target = hollow.getTarget();
        return target != null && target.isAlive() && !hollow.isPinned() && hollow.tickCount >= nextStepTick;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = hollow.getTarget();
        return warning > 0 && target != null && target.isAlive() && !hollow.isPinned();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        warning = ServerConfig.HOLLOW.riftStepWarningTicks.get();
        hollow.getNavigation().stop();
        hollow.triggerAnim(HollowAnimations.ACTION, HollowAnimations.RIFT_STEP);
        HollowEffects.riftCrack((ServerLevel) hollow.level(), hollow);
    }

    @Override
    public void stop() {
        warning = 0;
        nextStepTick = hollow.tickCount + cooldown();
    }

    @Override
    public void tick() {
        LivingEntity target = hollow.getTarget();
        if (target == null) {
            return;
        }
        hollow.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--warning > 0) {
            return;
        }
        ServerLevel level = (ServerLevel) hollow.level();
        Optional<Vec3> spot = RiftStepTarget.behind(level, hollow, target, ServerConfig.HOLLOW.riftStepDistance.get());
        if (spot.isEmpty()) {
            return;
        }
        HollowEffects.riftBurst(level, hollow);
        Vec3 to = spot.get();
        hollow.teleportTo(to.x, to.y, to.z);
        hollow.resetFallDistance();
        hollow.lookAt(target, 360.0F, 360.0F);
        hollow.setYHeadRot(hollow.getYRot());
        hollow.setYBodyRot(hollow.getYRot());
        HollowEffects.riftBurst(level, hollow);
    }

    private int cooldown() {
        return ServerConfig.HOLLOW.riftStepCooldownTicks(hollow.getRandom().nextInt(1 << 16));
    }
}
