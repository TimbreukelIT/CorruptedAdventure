package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LayeredConfig;

import java.util.EnumSet;

/**
 * The pounce from a wall or a ledge (design: "sprong van de wand"): it crouches (bones rattling, dust trickling off it,
 * {@code pounceWindupTicks}), then leaps in an arc onto the place its target stood when it leapt and lands with a blow
 * ({@link LayeredImpact}: {@code pounceDamage} within {@code pounceRadius}, thrown away {@code pounceKnockback}). From a wall onto a target not above it; from a ledge onto one at least
 * {@code pounceMinDrop} blocks below; 3..{@code pounceRange} blocks away sideways, in sight. The arc is flown by setting
 * its motion each tick ({@link TheLayered#startLeap}), so it does not depend on drag; a wall in the way stops it and it
 * falls from there.
 */
final class LayeredPounceGoal extends Goal {
    private static final double MIN_DISTANCE = 3.0;
    /** Blocks the arc rises above the higher of its two ends. */
    private static final double ARC_RISE = 1.5;
    private static final double MAX_STEP = 2.0;
    private static final int FALL_TIMEOUT_TICKS = 60;
    private static final int RECOVER_TICKS = 12;
    private static final double IMPACT_LIFT = 0.35;

    private enum Phase { CROUCH, FLIGHT, FALL, RECOVER }

    private final TheLayered layered;
    private int nextTick = -1;
    private Phase phase = Phase.CROUCH;
    private int ticks;
    private int flightTicks;
    private boolean done;
    private Vec3 from = Vec3.ZERO;
    private Vec3 to = Vec3.ZERO;

    LayeredPounceGoal(TheLayered layered) {
        this.layered = layered;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = layered.getTarget();
        if (target == null || !target.isAlive() || !layered.canAct() || layered.isVehicle() || !layered.mayStartAttack()
                || !layered.specialReady()) {
            return false;
        }
        if (nextTick < 0) {
            nextTick = layered.tickCount + cooldown() / 2;
            return false;
        }
        return layered.tickCount >= nextTick && canPounceAt(target);
    }

    private boolean canPounceAt(LivingEntity target) {
        LayeredConfig settings = TheLayered.settings();
        double drop = layered.getY() - target.getY();
        boolean perch = layered.isOnWall() ? drop >= -1.0 : layered.onGround() && drop >= settings.pounceMinDrop.get();
        if (!perch) {
            return false;
        }
        double dx = target.getX() - layered.getX();
        double dz = target.getZ() - layered.getZ();
        double sideways = Math.sqrt(dx * dx + dz * dz);
        return sideways >= MIN_DISTANCE && sideways <= settings.pounceRange.get() && layered.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return !done && layered.isAlive() && !layered.isFallen() && !layered.isKnockedOff();
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
        done = false;
        phase = Phase.CROUCH;
        ticks = TheLayered.settings().pounceWindupTicks.get();
        layered.getNavigation().stop();
        layered.requestCling(true);
        layered.triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.POUNCE);
        LayeredEffects.pounceWindup((ServerLevel) layered.level(), layered);
    }

    @Override
    public void stop() {
        layered.requestCling(false);
        layered.endLeap();
        layered.specialEnded();
        nextTick = layered.tickCount + cooldown();
    }

    @Override
    public void tick() {
        switch (phase) {
            case CROUCH -> crouch();
            case FLIGHT -> fly();
            case FALL -> {
                if (layered.onGround() || layered.isInWater()) {
                    land();
                } else if (++ticks >= FALL_TIMEOUT_TICKS) {
                    done = true;
                }
            }
            case RECOVER -> {
                if (--ticks <= 0) {
                    done = true;
                }
            }
        }
    }

    private void crouch() {
        LivingEntity target = layered.getTarget();
        if (target != null) {
            layered.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        LayeredEffects.pounceCrouch((ServerLevel) layered.level(), layered);
        if (--ticks > 0) {
            return;
        }
        if (target == null || !target.isAlive()) {
            done = true;
            return;
        }
        from = layered.position();
        to = landing(target);
        double sideways = Math.sqrt(Mth.square(to.x - from.x) + Mth.square(to.z - from.z));
        flightTicks = Mth.clamp((int) Math.round(sideways * 1.2), 10, 24);
        ticks = 0;
        phase = Phase.FLIGHT;
        layered.requestCling(false);
        layered.startLeap(to);
        LayeredEffects.leap((ServerLevel) layered.level(), layered);
    }

    /** Where its target stands now, at most {@code pounceRange} away sideways. */
    private Vec3 landing(LivingEntity target) {
        double range = TheLayered.settings().pounceRange.get();
        Vec3 at = target.position();
        Vec3 flat = new Vec3(at.x - layered.getX(), 0.0, at.z - layered.getZ());
        if (flat.length() > range) {
            flat = flat.normalize().scale(range);
            return new Vec3(layered.getX() + flat.x, at.y, layered.getZ() + flat.z);
        }
        return at;
    }

    private void fly() {
        ticks++;
        if (ticks > 2 && layered.onGround()) {
            land();
            return;
        }
        if (ticks > flightTicks) {
            // Not down yet (it hit something, or the ground is lower): it falls the rest of the way.
            phase = Phase.FALL;
            ticks = 0;
            return;
        }
        Vec3 next = arc(from, to, (double) ticks / flightTicks);
        Vec3 step = next.subtract(layered.position());
        if (step.length() > MAX_STEP) {
            step = step.normalize().scale(MAX_STEP);
        }
        layered.setDeltaMovement(step);
    }

    /** The point at {@code s} (0..1) of a parabola from {@code a} to {@code b}, its top {@link #ARC_RISE} above the higher end. */
    static Vec3 arc(Vec3 a, Vec3 b, double s) {
        double rise = Math.max(a.y, b.y) + ARC_RISE - (a.y + b.y) / 2.0;
        return new Vec3(Mth.lerp(s, a.x, b.x), Mth.lerp(s, a.y, b.y) + 4.0 * rise * s * (1.0 - s), Mth.lerp(s, a.z, b.z));
    }

    private void land() {
        layered.endLeap();
        layered.setDeltaMovement(Vec3.ZERO);
        ServerLevel level = (ServerLevel) layered.level();
        LayeredConfig settings = TheLayered.settings();
        double radius = settings.pounceRadius.get();
        LayeredImpact.hit(level, layered, radius, settings.pounceDamage.get(), settings.pounceKnockback.get(), IMPACT_LIFT);
        LayeredEffects.pounceImpact(level, layered, LayeredImpact.ground(level, layered), radius);
        layered.triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.POUNCE_LAND);
        phase = Phase.RECOVER;
        ticks = RECOVER_TICKS;
    }

    private int cooldown() {
        return TheLayered.settings().pounceCooldownTicks(layered.getRandom().nextInt(1 << 16));
    }
}
