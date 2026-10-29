package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LayeredConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Grabs a player and climbs a wall with them (design: "grijpen en meeklimmen"). The stab of the bite (same windup and
 * animation), but the beak holds on: the player rides it ({@link TheLayered#grab}). With a wall within
 * {@code wallSearchRadius} it pushes into the wall and climbs; it lets go after {@code carryHeight} blocks, over the
 * wall's top or after {@code carrySeconds}, and the player falls from there. Without a wall it shakes them
 * ({@code shakeDamage} every half second) and throws them up. Hits on its head free the player sooner
 * ({@link TheLayered#hurtHead}). Only on the ground, only fighters (no creative players), once per
 * {@code grabCooldownMin..Max} seconds; the first grab comes half a cooldown after it first could.
 */
final class LayeredGrabGoal extends Goal {
    private static final int SHAKE_TICKS = 10;
    private static final double FLING_UP = 1.1;
    private static final double FLING_OUT = 0.5;

    private final TheLayered layered;
    private int nextGrabTick = -1;
    private int windup;
    private boolean carrying;
    private boolean done;
    private int carryTicks;
    private double startY;
    private boolean climbed;
    @Nullable
    private Direction wall;

    LayeredGrabGoal(TheLayered layered) {
        this.layered = layered;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!(layered.getTarget() instanceof Player player) || !Nightmare.isFighter(player) || !layered.canAct()
                || layered.isVehicle() || layered.isOnWall() || player.isPassenger() || !layered.mayStartAttack()
                || !layered.specialReady()) {
            return false;
        }
        if (nextGrabTick < 0) {
            nextGrabTick = layered.tickCount + cooldown() / 2;
            return false;
        }
        return layered.tickCount >= nextGrabTick && layered.inBiteReach(player);
    }

    @Override
    public boolean canContinueToUse() {
        return !done && layered.canAct() && (carrying ? layered.isVehicle() : layered.getTarget() != null);
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
        carrying = false;
        windup = TheLayered.settings().biteWindupTicks.get();
        layered.getNavigation().stop();
        layered.triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.GRAB);
        LayeredEffects.biteWindup((ServerLevel) layered.level(), layered);
    }

    @Override
    public void stop() {
        if (layered.isVehicle()) {
            layered.releaseHeld();
        }
        carrying = false;
        wall = null;
        nextGrabTick = layered.tickCount + cooldown();
        layered.specialEnded();
        layered.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!carrying) {
            tickWindup();
        } else {
            tickCarry();
        }
    }

    private void tickWindup() {
        LivingEntity target = layered.getTarget();
        if (target == null) {
            done = true;
            return;
        }
        layered.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--windup > 0) {
            return;
        }
        if (target instanceof Player player && Nightmare.isFighter(player) && layered.inBiteReach(player)
                && layered.grab(player)) {
            LayeredConfig settings = TheLayered.settings();
            player.hurt(layered.damageSources().mobAttack(layered), settings.grabDamage.get().floatValue() * layered.strength());
            LayeredEffects.grabbed((ServerLevel) layered.level(), layered);
            carrying = true;
            carryTicks = 0;
            climbed = false;
            startY = layered.getY();
            wall = layered.findWall(settings.wallSearchRadius.get());
        } else {
            done = true;
        }
    }

    private void tickCarry() {
        Player held = layered.heldPlayer();
        if (held == null) {
            done = true;
            return;
        }
        LayeredConfig settings = TheLayered.settings();
        carryTicks++;
        if (carryTicks >= settings.carrySeconds.get() * 20) {
            done = true;
            return;
        }
        if (wall == null) {
            shake(held, settings);
            return;
        }
        // Into the wall and up: the push against it is what makes it climb (onClimbable + horizontal collision).
        layered.getMoveControl().setWantedPosition(layered.getX() + wall.getStepX() * 4.0, layered.getY() + 2.0,
                layered.getZ() + wall.getStepZ() * 4.0, 1.0);
        if (layered.isOnWall()) {
            climbed = true;
        }
        boolean overTheTop = climbed && !layered.isOnWall();
        if (overTheTop || layered.getY() - startY >= settings.carryHeight.get()) {
            done = true;
        }
    }

    /** No wall near: it shakes the held player, and halfway through its hold throws them up and away. */
    private void shake(Player held, LayeredConfig settings) {
        if (carryTicks % SHAKE_TICKS == 0) {
            held.hurt(layered.damageSources().mobAttack(layered), settings.shakeDamage.get().floatValue() * layered.strength());
            LayeredEffects.shake((ServerLevel) layered.level(), layered);
        }
        if (carryTicks >= settings.carrySeconds.get() * 10) {
            layered.releaseHeld();
            Vec3 away = held.position().subtract(layered.position()).multiply(1.0, 0.0, 1.0);
            Vec3 out = away.lengthSqr() > 1.0E-4 ? away.normalize().scale(FLING_OUT) : Vec3.ZERO;
            held.setDeltaMovement(out.x, FLING_UP, out.z);
            held.hurtMarked = true;
            done = true;
        }
    }

    private int cooldown() {
        return TheLayered.settings().grabCooldownTicks(layered.getRandom().nextInt(1 << 16));
    }
}
