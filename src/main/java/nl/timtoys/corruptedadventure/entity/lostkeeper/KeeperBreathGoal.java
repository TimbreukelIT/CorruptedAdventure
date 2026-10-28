package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.ConditionDose;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;

import java.util.EnumSet;

/**
 * The poison breath of its rotten half: it swims in close, stops, rears back ({@code breathWindupTicks}: the time to get
 * out of the way), then breathes a green cone at its target for {@code breathTicks}, following it slowly. Every fighter in the cone it can
 * see gets {@code breathPoison} points a tick on the ring 4 poison meter (creative and spectators not); where the breath
 * ends it leaves lingering clouds ({@link KeeperPoisonCloud}).
 */
final class KeeperBreathGoal extends Goal {
    /** It starts from up to this far beyond its range and swims in first, for at most {@link #APPROACH_TICKS}. */
    private static final double APPROACH = 10.0;
    private static final int APPROACH_TICKS = 60;
    private static final int CLOUD_TICKS = 10;
    /** The breath follows its target slower than it swims. */
    private static final double TRACK_DEGREES = 2.5;

    private final TheLostKeeper keeper;
    private int tick;
    private int approachTicks;
    private int nextTick = -1;
    private Vec3 direction = Vec3.ZERO;

    KeeperBreathGoal(TheLostKeeper keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = keeper.getTarget();
        if (!keeper.canAttack() || !(target instanceof Player player) || !TheLostKeeper.isFighter(player)) {
            nextTick = -1;
            return false;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        if (nextTick < 0) {
            nextTick = keeper.tickCount + config.breathCooldownTicks(keeper.getRandom().nextInt(1 << 16)) / 2;
        }
        double reach = config.breathRange.get() + APPROACH;
        return keeper.tickCount >= nextTick && player.distanceToSqr(keeper.headCentre()) <= reach * reach
                && keeper.hasLineOfSight(player);
    }

    @Override
    public boolean canContinueToUse() {
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        return keeper.canAttack() && approachTicks <= APPROACH_TICKS && keeper.getTarget() != null
                && tick < config.breathWindupTicks.get() + config.breathTicks.get();
    }

    @Override
    public boolean isInterruptable() {
        return tick == 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        tick = 0;
        approachTicks = 0;
    }

    @Override
    public void stop() {
        keeper.face(null);
        nextTick = keeper.tickCount + TheLostKeeper.keeperConfig().breathCooldownTicks(keeper.getRandom().nextInt(1 << 16));
    }

    @Override
    public void tick() {
        LivingEntity target = keeper.getTarget();
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        Vec3 mouth = keeper.mouth();
        if (tick == 0) {
            // Swim in until the target is well within reach, then rear back.
            double close = config.breathRange.get() * 0.7;
            if (target != null && target.distanceToSqr(mouth) > close * close && ++approachTicks <= APPROACH_TICKS) {
                keeper.steer(target.getEyePosition().add(0.0, 3.0, 0.0), 1.2);
                return;
            }
            approachTicks = 0;
            direction = keeper.getLookAngle();
            keeper.triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.BREATH);
            if (keeper.level() instanceof ServerLevel level) {
                KeeperEffects.breathWindup(level, keeper);
            }
        }
        tick++;
        keeper.steer(null, 0.0);
        if (target != null) {
            Vec3 aim = target.getBoundingBox().getCenter().subtract(mouth);
            if (aim.lengthSqr() > 1.0E-4) {
                direction = KeeperMotion.turn(direction, aim.normalize(), Math.toRadians(TRACK_DEGREES));
            }
            keeper.face(mouth.add(direction.scale(8.0)));
        }
        if (tick <= config.breathWindupTicks.get() || !(keeper.level() instanceof ServerLevel level)) {
            return;
        }
        double range = config.breathRange.get();
        double cos = Math.cos(Math.toRadians(config.breathAngle.get()));
        ConditionDose dose = ConditionDose.of(Condition.POISON, config.breathPoison.get().floatValue());
        for (Player player : keeper.fighters(range + 6.0)) {
            if (player instanceof ServerPlayer serverPlayer
                    && inCone(mouth, direction, player.getBoundingBox().getCenter(), range, cos) && keeper.hasLineOfSight(player)) {
                dose.apply(serverPlayer);
            }
        }
        KeeperEffects.breath(level, keeper, direction, range, tick);
        if (tick % CLOUD_TICKS == 0) {
            Vec3 end = mouth.add(direction.scale(range));
            Vec3 hit = level.clip(new ClipContext(mouth, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, keeper)).getLocation();
            keeper.addCloud(hit.subtract(direction.scale(0.5)));
        }
    }

    /** Whether {@code point} is in the cone from {@code apex} along the unit {@code axis}, {@code range} long. */
    static boolean inCone(Vec3 apex, Vec3 axis, Vec3 point, double range, double cosHalfAngle) {
        Vec3 offset = point.subtract(apex);
        double distance = offset.length();
        if (distance < 1.0) {
            return true;
        }
        double along = offset.dot(axis);
        return along > 0.0 && along <= range && along / distance >= cosHalfAngle;
    }
}
