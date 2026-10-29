package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LayeredConfig;

import java.util.EnumSet;

/**
 * The burrow (design: "ingraven"): standing on sand or gravel, with its target on sand or gravel within
 * {@code burrowRange}, it digs itself in ({@code burrowDigTicks}, sand spraying), moves under the ground toward its
 * target ({@code burrowSpeed}; hidden and untouchable, only the moving sand shows, {@link TheLayered#submerge}), stops
 * under them (or where the sand ends, or after {@code burrowMaxSeconds}), lets the sand heave there for
 * {@code burrowWarnTicks} and bursts up ({@link LayeredImpact}: {@code burrowDamage} within {@code burrowRadius},
 * thrown up {@code burrowLaunch}). Nothing in the world changes ({@link LayeredBurrow}). With {@code burrowEnabled}
 * only; once per {@code burrowCooldownMin..Max} seconds.
 */
final class LayeredBurrowGoal extends Goal {
    private static final double MIN_DISTANCE = 3.0;
    /** Sideways blocks from its target at which it stops under them. */
    private static final double UNDER = 0.8;
    private static final double OUTWARD = 0.3;
    private static final double OWN_LIFT = 0.45;
    private static final int RECOVER_TICKS = 16;

    private enum Phase { DIG, TRAVEL, HEAVE, RECOVER }

    private final TheLayered layered;
    private int nextTick = -1;
    private Phase phase = Phase.DIG;
    private int ticks;
    private boolean done;

    LayeredBurrowGoal(TheLayered layered) {
        this.layered = layered;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LayeredConfig settings = TheLayered.settings();
        LivingEntity target = layered.getTarget();
        if (!settings.burrowEnabled.get() || target == null || !target.isAlive() || !layered.canAct()
                || layered.isVehicle() || layered.isOnWall() || !layered.onGround() || !layered.mayStartAttack()
                || !layered.specialReady()) {
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
        return distance >= MIN_DISTANCE && distance <= settings.burrowRange.get() && target.onGround()
                && LayeredBurrow.isBurrowGround(layered.level(), layered.position())
                && LayeredBurrow.isBurrowGround(layered.level(), target.position());
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
        phase = Phase.DIG;
        ticks = 0;
        layered.getNavigation().stop();
        layered.triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.BURROW);
    }

    @Override
    public void stop() {
        if (phase != Phase.RECOVER) {
            // Broken off before it burst up (it died, retreated or staggered while digging in): it comes back up.
            layered.emerge();
            layered.triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.ERUPT);
        }
        phase = Phase.DIG;
        layered.specialEnded();
        nextTick = layered.tickCount + cooldown();
    }

    @Override
    public void tick() {
        ServerLevel level = (ServerLevel) layered.level();
        LayeredConfig settings = TheLayered.settings();
        ticks++;
        switch (phase) {
            case DIG -> {
                LivingEntity target = layered.getTarget();
                if (target != null) {
                    layered.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
                LayeredEffects.dig(level, layered, LayeredImpact.ground(level, layered), ticks);
                if (ticks >= settings.burrowDigTicks.get()) {
                    LayeredEffects.submerge(level, layered, LayeredImpact.ground(level, layered));
                    layered.submerge();
                    phase = Phase.TRAVEL;
                    ticks = 0;
                }
            }
            case TRAVEL -> travel(level, settings);
            case HEAVE -> {
                LayeredEffects.heave(level, layered.position(), LayeredImpact.ground(level, layered), ticks);
                if (ticks >= settings.burrowWarnTicks.get()) {
                    erupt(level, settings);
                }
            }
            case RECOVER -> {
                if (ticks >= RECOVER_TICKS) {
                    done = true;
                }
            }
        }
    }

    private void travel(ServerLevel level, LayeredConfig settings) {
        LivingEntity target = layered.getTarget();
        Vec3 here = layered.position();
        if (target == null || ticks >= settings.burrowMaxSeconds.get() * 20) {
            startHeave();
            return;
        }
        Vec3 flat = new Vec3(target.getX() - here.x, 0.0, target.getZ() - here.z);
        double distance = flat.length();
        if (distance <= UNDER) {
            startHeave();
            return;
        }
        Vec3 wanted = here.add(flat.scale(Math.min(settings.burrowSpeed.get(), distance) / distance));
        Vec3 next = LayeredBurrow.step(level, layered, here, wanted);
        if (next == null) {
            // The sand ends here (stone, water, no room): it comes up where it is.
            startHeave();
            return;
        }
        layered.setYRot((float) Math.toDegrees(Math.atan2(-flat.x, flat.z)));
        layered.moveUnderground(next);
        if (ticks % 2 == 0) {
            LayeredEffects.trail(level, next, LayeredImpact.ground(level, layered), ticks);
        }
    }

    private void startHeave() {
        phase = Phase.HEAVE;
        ticks = 0;
    }

    private void erupt(ServerLevel level, LayeredConfig settings) {
        layered.emerge();
        double radius = settings.burrowRadius.get();
        LayeredImpact.hit(level, layered, radius, settings.burrowDamage.get(), OUTWARD, settings.burrowLaunch.get());
        LayeredEffects.erupt(level, layered, LayeredImpact.ground(level, layered), radius);
        layered.triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.ERUPT);
        layered.setDeltaMovement(0.0, OWN_LIFT, 0.0);
        phase = Phase.RECOVER;
        ticks = 0;
    }

    private int cooldown() {
        return TheLayered.settings().burrowCooldownTicks(layered.getRandom().nextInt(1 << 16));
    }
}
