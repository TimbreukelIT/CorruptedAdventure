package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Verspringen: a crack warns while a rift tears open where it stands, then The Hollow steps into it and comes out of a
 * second rift behind its target. Also how a giant
 * gets past trees and caves its path finding cannot. Never while pinned; a pin during the warning cancels the step. No
 * room behind the target: the step fizzles and the cooldown starts anyway. As an attack it waits for the gate of the
 * shared reactions; as a reaction ({@link #stepTo}: away in a rest window, to the last known place in a search) it goes
 * at once, past the gate and the cooldown.
 */
final class HollowRiftStepGoal extends Goal {
    private final TheHollow hollow;
    /** Ticks the rift it comes out of stays open. */
    private static final int EXIT_OPEN_TICKS = 12;
    /**
     * Ticks a reaction's request waits for its turn: enough to outwait the attack that ended the burst (an extra attack
     * runs ~3 s), after that the moment has passed.
     */
    private static final int REQUEST_TICKS = 60;

    private int warning;
    private int nextStepTick;
    private HollowRift entry;
    /** Where a reaction wants it to go, or null for an attack step behind its target. */
    @Nullable
    private Vec3 requested;
    private int requestedUntil;
    @Nullable
    private Vec3 destination;

    HollowRiftStepGoal(TheHollow hollow) {
        this.hollow = hollow;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        nextStepTick = cooldown();
    }

    /** Whether a step is under way (the warning runs). */
    boolean isStepping() {
        return warning > 0;
    }

    /** A reaction: rift-step to near {@code point} as soon as it can (not while pinned, staggering or arriving). */
    void stepTo(Vec3 point) {
        requested = point;
        requestedUntil = hollow.tickCount + REQUEST_TICKS;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = hollow.getTarget();
        if (target == null || !target.isAlive() || hollow.isPinned() || hollow.isArriving() || hollow.isStaggered()) {
            requested = null;
            return false;
        }
        if (requested != null) {
            Vec3 point = requested;
            requested = null;
            // Dropped when held back too long (another goal had MOVE) or its rest window or search is already over.
            if (hollow.tickCount <= requestedUntil && (hollow.isResting() || hollow.isSearching())) {
                destination = point;
                return true;
            }
        }
        destination = null;
        return hollow.tickCount >= nextStepTick && hollow.mayStartAttack();
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
        // At least one tick, so tick() always gets to the step (the config's minimum is 1 too).
        warning = Math.max(1, ServerConfig.HOLLOW.riftStepWarningTicks.get());
        hollow.getNavigation().stop();
        if (destination == null) {
            hollow.countAttack();
        }
        hollow.triggerAnim(HollowAnimations.ACTION, HollowAnimations.RIFT_STEP);
        ServerLevel level = (ServerLevel) hollow.level();
        HollowEffects.riftCrack(level, hollow);
        // The rift it steps into tears open during the warning, facing its target.
        LivingEntity target = hollow.getTarget();
        float yaw = target == null ? hollow.getYRot() : yawToward(hollow.position(), target.position());
        entry = HollowRift.step(level, hollow.position(), yaw, warning, 4);
    }

    @Override
    public void stop() {
        warning = 0;
        destination = null;
        nextStepTick = hollow.tickCount + cooldown();
        if (entry != null) {
            entry.close(false);
            entry = null;
        }
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
        Optional<Vec3> spot = destination != null ? RiftStepTarget.near(level, hollow, destination)
                : RiftStepTarget.behind(level, hollow, target, ServerConfig.HOLLOW.riftStepDistance.get());
        if (spot.isEmpty()) {
            return;
        }
        HollowEffects.riftBurst(level, hollow);
        Vec3 to = spot.get();
        // It comes out of a second rift behind its target, facing it.
        HollowRift.step(level, to, yawToward(to, target.position()), 3, EXIT_OPEN_TICKS);
        hollow.teleportTo(to.x, to.y, to.z);
        hollow.resetFallDistance();
        hollow.lookAt(target, 360.0F, 360.0F);
        hollow.setYHeadRot(hollow.getYRot());
        hollow.setYBodyRot(hollow.getYRot());
        HollowEffects.riftBurst(level, hollow);
    }

    /** Minecraft's yaw (degrees) of the direction from {@code from} to {@code to}. */
    static float yawToward(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private int cooldown() {
        return ServerConfig.HOLLOW.riftStepCooldownTicks(hollow.getRandom().nextInt(1 << 16));
    }
}
