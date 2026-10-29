package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import nl.timtoys.corruptedadventure.config.HollowConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The Hollow's extra attacks (life round): every few seconds, between its slams, it picks one that fits its target's
 * distance ({@link HollowAttackPicker}) and plays it out: a windup with a clear warning, the strike, a short recovery.
 * It stands while doing so. Out of phase it starts none, and a strike of its arms while out of phase hits nothing
 * (the claws of the rift rain are the void's, they still bite). Broken off early, claws that have not come up close.
 */
final class HollowExtraAttackGoal extends Goal {
    /** Ticks it stays after the strike before it moves again. */
    private static final int RECOVERY_TICKS = 15;

    private final TheHollow hollow;
    @Nullable
    private HollowAttack attack;
    @Nullable
    private HollowAttack last;
    private int age;
    private int strikeTick;
    private int endTick;
    private int nextAttackTick;
    /** The sweep's direction, fixed when the arm is drawn back. */
    private float lockedYaw;
    private final List<HollowVoidClaw> claws = new ArrayList<>();

    HollowExtraAttackGoal(TheHollow hollow) {
        this.hollow = hollow;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        // The first one comes a little after the fight starts.
        nextAttackTick = cooldown() / 2;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = hollow.getTarget();
        if (target == null || !target.isAlive() || hollow.tickCount < nextAttackTick || hollow.isPhased()
                || hollow.isArriving()) {
            return false;
        }
        HollowConfig config = ServerConfig.HOLLOW;
        double half = hollow.getBbWidth() * 0.5;
        HollowAttackPicker.Ranges ranges = new HollowAttackPicker.Ranges(half + config.sweepReach.get(),
                config.groundSlamRange.get(), config.riftRainMinRange.get(), config.riftRainMaxRange.get());
        double dx = target.getX() - hollow.getX();
        double dz = target.getZ() - hollow.getZ();
        int inWave = hollow.fighters(config.groundSlamRadius.get()).size();
        attack = HollowAttackPicker.pick(ranges, Math.sqrt(dx * dx + dz * dz), inWave, last, hollow.getRandom().nextInt(1 << 16));
        if (attack == null) {
            // Nothing fits (too far): look again in a second.
            nextAttackTick = hollow.tickCount + 20;
        }
        return attack != null;
    }

    @Override
    public boolean canContinueToUse() {
        return attack != null && age < endTick;
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
        HollowConfig config = ServerConfig.HOLLOW;
        age = 0;
        hollow.getNavigation().stop();
        ServerLevel level = (ServerLevel) hollow.level();
        LivingEntity target = hollow.getTarget();
        if (target != null) {
            hollow.lookAt(target, 360.0F, 360.0F);
        }
        lockedYaw = hollow.getYRot();
        hollow.setYBodyRot(lockedYaw);
        hollow.setYHeadRot(lockedYaw);
        hollow.triggerAnim(HollowAnimations.ACTION, attack.animation);
        switch (attack) {
            case GROUND_SLAM -> {
                strikeTick = config.groundSlamWindupTicks.get();
                endTick = strikeTick + RECOVERY_TICKS;
                HollowEffects.groundSlamWindup(level, hollow);
            }
            case RIFT_RAIN -> {
                strikeTick = config.riftRainWindupTicks.get();
                // It keeps its hand in the ground until the claws are up.
                endTick = strikeTick + config.riftRainWarningTicks.get() + RECOVERY_TICKS;
                HollowEffects.riftRainDig(level, hollow);
            }
            case SWEEP -> {
                strikeTick = config.sweepWindupTicks.get();
                endTick = strikeTick + RECOVERY_TICKS;
                HollowEffects.sweepWindup(level, hollow);
            }
        }
    }

    @Override
    public void stop() {
        if (age < endTick) {
            // Broken off: what has not come up yet closes.
            for (HollowVoidClaw claw : claws) {
                claw.cancel();
            }
        }
        claws.clear();
        last = attack;
        attack = null;
        nextAttackTick = hollow.tickCount + cooldown();
    }

    @Override
    public void tick() {
        age++;
        LivingEntity target = hollow.getTarget();
        if (attack == HollowAttack.SWEEP) {
            hollow.setYRot(lockedYaw);
            hollow.setYBodyRot(lockedYaw);
            hollow.setYHeadRot(lockedYaw);
        } else if (target != null && age < strikeTick) {
            hollow.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (age != strikeTick) {
            return;
        }
        ServerLevel level = (ServerLevel) hollow.level();
        HollowConfig config = ServerConfig.HOLLOW;
        switch (attack) {
            case GROUND_SLAM -> {
                if (!hollow.isPhased()) {
                    HollowEffects.groundSlamImpact(level, hollow);
                    HollowShockwave.spawn(level, hollow, hollow.position(), config.groundSlamRadius.get().floatValue(),
                            config.groundSlamWaveSpeed.get().floatValue(), config.groundSlamDamage.get().floatValue(),
                            config.groundSlamLift.get());
                }
            }
            case RIFT_RAIN -> {
                if (target != null && target.isAlive()) {
                    claws.addAll(HollowRiftRain.open(level, hollow, target));
                }
            }
            case SWEEP -> {
                if (!hollow.isPhased()) {
                    HollowSweep.strike(level, hollow, lockedYaw);
                }
            }
        }
    }

    private int cooldown() {
        return ServerConfig.HOLLOW.extraAttackCooldownTicks(hollow.getRandom().nextInt(1 << 16));
    }
}
