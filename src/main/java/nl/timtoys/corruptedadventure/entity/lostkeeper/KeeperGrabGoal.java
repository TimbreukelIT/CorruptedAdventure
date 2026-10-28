package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Pulling under water: it swims in, lunges ({@code grabWindupTicks}, its jaws wide) and snaps. If the target is still
 * within {@code grabReach} and there is water near them, it coils round them ({@link TheLostKeeper#hold}) and dives
 * to the deepest water it found; damage to it (from anyone) sets them free, or it lets go after {@code holdSeconds}.
 * Without water near the target the snap is a bite ({@code attackDamage}).
 */
final class KeeperGrabGoal extends Goal {
    private static final double START_DISTANCE = 20.0;
    private static final double LUNGE_DISTANCE = 7.0;
    private static final int APPROACH_TICKS = 80;
    private static final double LUNGE_SPEED = 1.7;

    private final TheLostKeeper keeper;
    private int nextTick = -1;
    private int approachTicks;
    private int windup;
    private boolean snapped;
    @Nullable
    private Vec3 dive;

    KeeperGrabGoal(TheLostKeeper keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = keeper.getTarget();
        if (!keeper.canAttack() || !(target instanceof Player player) || !TheLostKeeper.isFighter(player)
                || player.isPassenger()) {
            if (target == null) {
                nextTick = -1;
            }
            return false;
        }
        if (nextTick < 0) {
            nextTick = keeper.tickCount + TheLostKeeper.keeperConfig().grabCooldownTicks(keeper.getRandom().nextInt(1 << 16)) / 2;
        }
        return keeper.tickCount >= nextTick && player.distanceToSqr(keeper.headCentre()) <= START_DISTANCE * START_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        if (snapped) {
            return keeper.isHolding() && keeper.isAlive();
        }
        LivingEntity target = keeper.getTarget();
        return keeper.canAttack() && target != null && target.isAlive() && approachTicks <= APPROACH_TICKS;
    }

    @Override
    public boolean isInterruptable() {
        return windup == 0 && !snapped;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        approachTicks = 0;
        windup = 0;
        snapped = false;
        dive = null;
    }

    @Override
    public void stop() {
        windup = 0;
        snapped = false;
        dive = null;
        keeper.face(null);
        nextTick = keeper.tickCount + TheLostKeeper.keeperConfig().grabCooldownTicks(keeper.getRandom().nextInt(1 << 16));
    }

    @Override
    public void tick() {
        if (snapped) {
            // Holding: down into the water, deep.
            if (dive != null) {
                keeper.steer(dive, 0.8);
            }
            return;
        }
        LivingEntity target = keeper.getTarget();
        if (target == null) {
            return;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        Vec3 aim = target.getBoundingBox().getCenter();
        if (windup == 0) {
            approachTicks++;
            keeper.steer(aim, 1.2);
            if (aim.distanceToSqr(keeper.headCentre()) <= LUNGE_DISTANCE * LUNGE_DISTANCE) {
                windup = config.grabWindupTicks.get();
                keeper.triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.GRAB);
                if (keeper.level() instanceof ServerLevel level) {
                    KeeperEffects.lunge(level, keeper);
                }
            }
            return;
        }
        keeper.steer(aim, LUNGE_SPEED);
        if (--windup > 0) {
            return;
        }
        snap(target, config);
    }

    private void snap(LivingEntity target, LostKeeperConfig config) {
        double reach = config.grabReach.get();
        boolean inReach = target.getBoundingBox().inflate(reach).contains(keeper.headCentre())
                || target.getBoundingBox().inflate(reach).contains(keeper.mouth());
        if (!inReach) {
            approachTicks = APPROACH_TICKS + 1;
            return;
        }
        Vec3 water = target instanceof Player player && TheLostKeeper.isFighter(player)
                ? deepestWater(keeper.level(), target.blockPosition(), config.waterSearch.get()) : null;
        boolean caught = water != null && keeper.hold((Player) target);
        if (keeper.level() instanceof ServerLevel level) {
            KeeperEffects.snap(level, keeper, caught);
        }
        if (caught) {
            snapped = true;
            dive = water;
            return;
        }
        // No water near: a bite.
        target.hurt(keeper.damageSources().mobAttack(keeper), (float) keeper.getAttributeValue(Attributes.ATTACK_DAMAGE));
        Vec3 push = target.position().subtract(keeper.position()).multiply(1.0, 0.0, 1.0);
        if (push.lengthSqr() > 1.0E-4) {
            double strength = keeper.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
            target.knockback(strength, -push.x, -push.z);
        }
        approachTicks = APPROACH_TICKS + 1;
    }

    /**
     * The lowest water block within {@code radius} round and below {@code around} (its centre), or null without water:
     * where it dives with its catch.
     */
    @Nullable
    static Vec3 deepestWater(Level level, BlockPos around, int radius) {
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-radius, -radius, -radius), around.offset(radius, 1, radius))) {
            if (level.getFluidState(pos).is(FluidTags.WATER) && (best == null || pos.getY() < best.getY())) {
                best = pos.immutable();
            }
        }
        return best == null ? null : Vec3.atCenterOf(best);
    }
}
