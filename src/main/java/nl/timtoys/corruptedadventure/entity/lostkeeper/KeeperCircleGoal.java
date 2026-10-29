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
 * <p>
 * The shared reactions with its own swimming: in a rest window after a burst of attacks it swims to the base's
 * {@link TheLostKeeper#restPoint() rest point} (backing off or circling wide); lost out of sight it swims to where it last
 * saw its target, sniffs there and then circles the place slowly, looking round, until the search ends.
 */
final class KeeperCircleGoal extends Goal {
    private static final int WANDER_TICKS = 120;
    private static final double WANDER_RADIUS = 24.0;
    private static final int REST_REPOINT_TICKS = 10;
    /** Blocks (horizontally) from the last known place at which the search has arrived. */
    private static final double ARRIVE_DISTANCE = 3.5;
    private static final double SEARCH_LOOP_RADIUS = 5.0;

    private final TheLostKeeper keeper;
    private double angle;
    private int direction = 1;
    private int ticks;
    private Vec3 home;
    private Vec3 wanderPoint;
    private Vec3 restPoint;

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
        restPoint = null;
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
        if (keeper.isSearching() && keeper.lastKnownPosition() != null) {
            search(keeper.lastKnownPosition());
            return;
        }
        if (target != null && target.isAlive() && keeper.isResting()) {
            rest(target);
            return;
        }
        restPoint = null;
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

    /** The rest window: to the shared rest point, at the height it circles at (with its crown above, else level). */
    private void rest(LivingEntity target) {
        if (restPoint == null || ticks % REST_REPOINT_TICKS == 0) {
            Vec3 point = keeper.restPoint();
            if (point != null) {
                double height = keeper.hasCrown() ? 6.0 + 2.0 * Mth.sin(ticks * 0.035F) : 0.5;
                restPoint = new Vec3(point.x, target.getY() + height, point.z);
            }
        }
        if (restPoint != null) {
            keeper.steer(restPoint, 0.9);
        }
    }

    /** The search: to the last known place, sniff there, then slow loops round it while it looks about. */
    private void search(Vec3 place) {
        double height = keeper.hasCrown() ? 3.0 : 1.0;
        Vec3 head = keeper.headCentre();
        if (!keeper.hasArrivedAtSearch()) {
            double dx = place.x - head.x;
            double dz = place.z - head.z;
            if (dx * dx + dz * dz <= ARRIVE_DISTANCE * ARRIVE_DISTANCE) {
                keeper.arriveAtSearch();
            } else {
                keeper.steer(place.add(0.0, height, 0.0), 0.9);
                return;
            }
        }
        angle += direction * 0.4 * TheLostKeeper.keeperConfig().swimSpeed.get() / SEARCH_LOOP_RADIUS;
        keeper.steer(new Vec3(place.x + Math.cos(angle) * SEARCH_LOOP_RADIUS, place.y + height,
                place.z + Math.sin(angle) * SEARCH_LOOP_RADIUS), 0.4);
    }
}
