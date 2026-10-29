package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * The tail whip: when a fighter stands within reach of its rear half (on an island, a shore or in the water), the tail
 * rattles ({@code whipWindupTicks}) and the rear half sweeps sideways round {@link TheLostKeeper#WHIP_PIVOT} towards them.
 * Everyone the sweeping tail touches is hit once and thrown along the sweep, off the island or into the water. The
 * sweep itself is synced, so the client's body chain swings along. Runs beside the swimming (no move flag).
 */
final class KeeperWhipGoal extends Goal {
    /** The rear hitboxes that sweep (indices into the keeper's body parts). */
    private static final int FIRST_REAR_PART = 3;
    private static final double UPWARD = 0.5;

    private final TheLostKeeper keeper;
    private final Set<Player> hit = new HashSet<>();
    private int nextTick = -1;
    private int windup;
    private int sweep;
    private int side;

    KeeperWhipGoal(TheLostKeeper keeper) {
        this.keeper = keeper;
    }

    @Override
    public boolean canUse() {
        if (!keeper.mayAttack() || keeper.isCoiling() || keeper.getTarget() == null) {
            // Only a fight that is over restarts the cooldown; a rest window or a stagger keeps it.
            if (keeper.getTarget() == null) {
                nextTick = -1;
            }
            return false;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        if (nextTick < 0) {
            nextTick = keeper.tickCount + config.whipCooldownTicks(keeper.getRandom().nextInt(1 << 16)) / 2;
        }
        return keeper.tickCount >= nextTick && nearestInReach(config.whipReach.get() + 4.0) != null;
    }

    @Override
    public boolean canContinueToUse() {
        return keeper.isAlive() && !keeper.isStunned() && (windup > 0 || sweep > 0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        keeper.countAttack();
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        windup = config.whipWindupTicks.get();
        sweep = 0;
        hit.clear();
        Player victim = nearestInReach(config.whipReach.get() + 4.0);
        side = victim == null ? 1 : sideTowards(victim.position());
        keeper.triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.TAIL_WHIP);
        if (keeper.level() instanceof ServerLevel level) {
            KeeperEffects.whipWindup(level, keeper.body().node(KeeperBody.SEGMENTS));
        }
    }

    @Override
    public void stop() {
        hit.clear();
        nextTick = keeper.tickCount + TheLostKeeper.keeperConfig().whipCooldownTicks(keeper.getRandom().nextInt(1 << 16));
    }

    @Override
    public void tick() {
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        if (windup > 0) {
            if (--windup == 0) {
                sweep = config.whipTicks.get();
                keeper.startWhip(side);
            }
            return;
        }
        sweep--;
        double reach = config.whipReach.get();
        Vec3 pivot = keeper.body().node(TheLostKeeper.WHIP_PIVOT);
        for (Player player : keeper.fighters(KeeperBody.SEGMENTS * KeeperBody.SEGMENT_LENGTH + reach)) {
            if (hit.contains(player) || !touchesTail(player.getBoundingBox(), reach)) {
                continue;
            }
            hit.add(player);
            player.hurt(keeper.damageSources().mobAttack(keeper), config.whipDamage.get().floatValue() * keeper.strength());
            // Thrown along the sweep: sideways to the tail, in the direction it swings.
            Vec3 arm = player.position().subtract(pivot).multiply(1.0, 0.0, 1.0);
            Vec3 along = new Vec3(-arm.z, 0.0, arm.x).scale(side);
            along = along.lengthSqr() > 1.0E-4 ? along.normalize() : arm.normalize();
            double strength = config.whipKnockback.get();
            player.setDeltaMovement(along.x * strength, UPWARD, along.z * strength);
            player.hurtMarked = true;
            if (keeper.level() instanceof ServerLevel level) {
                KeeperEffects.whipHit(level, player.position());
            }
        }
    }

    /** Whether a box touches one of the rear body parts, grown by {@code reach}. */
    private boolean touchesTail(AABB box, double reach) {
        KeeperPart[] parts = keeper.bodyParts();
        for (int i = FIRST_REAR_PART; i < parts.length; i++) {
            if (parts[i].getBoundingBox().inflate(reach).intersects(box)) {
                return true;
            }
        }
        Vec3 tip = keeper.body().node(KeeperBody.SEGMENTS);
        return box.inflate(reach).contains(tip);
    }

    private Player nearestInReach(double reach) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player player : keeper.fighters(KeeperBody.SEGMENTS * KeeperBody.SEGMENT_LENGTH + reach)) {
            if (touchesTail(player.getBoundingBox(), reach)) {
                double distance = player.distanceToSqr(keeper.body().node(KeeperBody.SEGMENTS - 2));
                if (distance < best) {
                    best = distance;
                    nearest = player;
                }
            }
        }
        return nearest;
    }

    /**
     * The swing direction that brings the tail towards {@code point}: {@link KeeperBody#swing} with a positive angle
     * moves a node at offset (x, z) from the pivot along (-z, x).
     */
    private int sideTowards(Vec3 point) {
        Vec3 pivot = keeper.body().node(TheLostKeeper.WHIP_PIVOT);
        Vec3 tail = keeper.body().node(KeeperBody.SEGMENTS).subtract(pivot);
        Vec3 to = point.subtract(pivot);
        double turn = -tail.z * to.x + tail.x * to.z;
        return turn >= 0.0 ? 1 : -1;
    }
}
