package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;

import java.util.EnumSet;

/**
 * Its arrival's last stretch ({@link TheLostKeeper#arrive}): it swims to the arrival's point before anything else, up out
 * of the water (bursting through the surface with spray and a roar) or through its mist in the air to its target. Ends
 * there, or when the arrival's time runs out ({@link TheLostKeeper#tick}).
 */
final class KeeperArriveGoal extends Goal {
    /** Blocks from the point at which it has arrived. */
    private static final double ARRIVED = 2.5;

    private final TheLostKeeper keeper;
    private boolean wasInWater;

    KeeperArriveGoal(TheLostKeeper keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return keeper.isAlive() && keeper.isArriving();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
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
        wasInWater = keeper.isInWater();
    }

    @Override
    public void stop() {
        keeper.endArrival();
        keeper.steer(null, 0.0);
    }

    @Override
    public void tick() {
        Vec3 point = keeper.arrivalPoint();
        if (point == null) {
            return;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        boolean inWater = keeper.isInWater();
        if (wasInWater && !inWater && keeper.level() instanceof ServerLevel level) {
            KeeperEffects.riseFromWater(level, keeper, keeper.headCentre());
            keeper.triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.ROAR);
        }
        wasInWater = inWater;
        double speed = inWater ? config.arrivalRiseSpeed.get() : config.arrivalAirSpeed.get();
        keeper.steer(point, speed);
        if (keeper.headCentre().distanceToSqr(point) <= ARRIVED * ARRIVED) {
            keeper.endArrival();
        }
    }
}
