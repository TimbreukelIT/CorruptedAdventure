package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LayeredConfig;

import java.util.EnumSet;

/**
 * The dead heads spit (design: "dode koppen spuwen"): on the ground, at a target in sight between {@code spitMinRange}
 * and {@code spitRange} blocks, the two dead heads rear back with their jaws open ({@code spitWindupTicks}, dust gathering
 * in them) and then each spits a cloud of dust and bone splinters ({@link LayeredSpit}), the second a moment after the
 * first. They aim at where the target was when they reared back, so a step aside dodges it. Once per
 * {@code spitCooldownMin..Max} seconds.
 */
final class LayeredSpitGoal extends Goal {
    /** Ticks between the first and the second head's spit. */
    private static final int SECOND_HEAD_DELAY = 4;
    private static final int RECOVER_TICKS = 10;
    /** Blocks the two heads aim apart, sideways, so the pair covers a little more than one spot. */
    private static final double SPREAD = 0.6;

    private final TheLayered layered;
    private int nextTick = -1;
    private int ticks;
    private int windup;
    private Vec3 aim = Vec3.ZERO;

    LayeredSpitGoal(TheLayered layered) {
        this.layered = layered;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LayeredConfig settings = TheLayered.settings();
        LivingEntity target = layered.getTarget();
        if (target == null || !target.isAlive() || !layered.canAct() || layered.isVehicle() || layered.isOnWall()
                || !layered.mayStartAttack() || !layered.specialReady()) {
            return false;
        }
        if (nextTick < 0) {
            nextTick = layered.tickCount + cooldown() / 2;
            return false;
        }
        if (layered.tickCount < nextTick) {
            return false;
        }
        double distance = layered.distanceTo(target);
        return distance >= settings.spitMinRange.get() && distance <= settings.spitRange.get()
                && layered.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < windup + SECOND_HEAD_DELAY + RECOVER_TICKS && layered.canAct();
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
        layered.countAttack();
        LivingEntity target = layered.getTarget();
        aim = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
        ticks = 0;
        windup = TheLayered.settings().spitWindupTicks.get();
        layered.getNavigation().stop();
        layered.triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.SPIT);
        LayeredEffects.spitWindup((ServerLevel) layered.level(), layered);
    }

    @Override
    public void stop() {
        layered.specialEnded();
        nextTick = layered.tickCount + cooldown();
    }

    @Override
    public void tick() {
        ServerLevel level = (ServerLevel) layered.level();
        layered.getLookControl().setLookAt(aim.x, aim.y, aim.z, 30.0F, 30.0F);
        ticks++;
        if (ticks < windup) {
            if (ticks % 2 == 0) {
                LayeredEffects.spitGather(level, mouth(1.0));
                LayeredEffects.spitGather(level, mouth(-1.0));
            }
        } else if (ticks == windup) {
            spit(level, 1.0);
        } else if (ticks == windup + SECOND_HEAD_DELAY) {
            spit(level, -1.0);
        }
    }

    private void spit(ServerLevel level, double side) {
        Vec3 from = mouth(side);
        Vec3 sideways = new Vec3(aim.z - from.z, 0.0, from.x - aim.x);
        Vec3 toward = sideways.lengthSqr() > 1.0E-6 ? aim.add(sideways.normalize().scale(SPREAD * side * 0.5)) : aim;
        layered.spit(new LayeredSpit(from, toward));
        LayeredEffects.spit(level, from);
    }

    /** The jaws of the dead head on that side (1 left, -1 right). */
    private Vec3 mouth(double side) {
        return layered.position().add(LayeredPose.offset(LayeredPose.DEAD_MOUTH_UP, LayeredPose.DEAD_MOUTH_FORWARD,
                LayeredPose.DEAD_MOUTH_SIDE * side, layered.yBodyRot, 0.0F));
    }

    private int cooldown() {
        return TheLayered.settings().spitCooldownTicks(layered.getRandom().nextInt(1 << 16));
    }
}
