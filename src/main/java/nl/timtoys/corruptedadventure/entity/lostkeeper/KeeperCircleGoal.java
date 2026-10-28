package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Between attacks: it circles its target at {@code orbitRadius}, rising and sinking in long waves above it (with its crown
 * it swims through the air; without, it keeps to the target's height, in the water or crawling). Without a target it
 * roams round the place it appeared. The loops and lusses of its body come from this path.
 */
final class KeeperCircleGoal extends Goal {
    private static final int WANDER_TICKS = 120;
    private static final double WANDER_RADIUS = 24.0;

    private final TheLostKeeper keeper;
    private double angle;
    private int direction = 1;
    private int ticks;
    private Vec3 home;
    private Vec3 wanderPoint;

    KeeperCircleGoal(TheLostKeeper keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return keeper.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        if (home == null) {
            home = keeper.position();
        }
        LivingEntity target = keeper.getTarget();
        if (target != null) {
            angle = Math.atan2(keeper.getZ() - target.getZ(), keeper.getX() - target.getX());
        }
        direction = keeper.getRandom().nextBoolean() ? 1 : -1;
        wanderPoint = null;
    }

    @Override
    public void stop() {
        keeper.steer(null, 0.0);
    }

    @Override
    public void tick() {
        ticks++;
        keeper.face(null);
        LivingEntity target = keeper.getTarget();
        if (target != null && target.isAlive()) {
            double radius = TheLostKeeper.keeperConfig().orbitRadius.get();
            double speed = TheLostKeeper.keeperConfig().swimSpeed.get();
            angle += direction * speed / radius;
            // Now and then it turns round: no two passes alike.
            if (keeper.getRandom().nextInt(400) == 0) {
                direction = -direction;
            }
            double height = keeper.hasCrown() ? 6.0 + 4.0 * Mth.sin(ticks * 0.035F) : 0.5;
            Vec3 point = new Vec3(target.getX() + Math.cos(angle) * radius, target.getY() + height,
                    target.getZ() + Math.sin(angle) * radius);
            keeper.steer(point, 1.0);
            return;
        }
        if (wanderPoint == null || ticks % WANDER_TICKS == 0 || keeper.headCentre().distanceToSqr(wanderPoint) < 9.0) {
            double a = keeper.getRandom().nextDouble() * Math.PI * 2.0;
            double r = WANDER_RADIUS * (0.4 + 0.6 * keeper.getRandom().nextDouble());
            double up = keeper.hasCrown() ? 4.0 + keeper.getRandom().nextDouble() * 10.0 : 0.0;
            wanderPoint = new Vec3(home.x + Math.cos(a) * r, home.y + up, home.z + Math.sin(a) * r);
        }
        keeper.steer(wanderPoint, 0.6);
    }
}
