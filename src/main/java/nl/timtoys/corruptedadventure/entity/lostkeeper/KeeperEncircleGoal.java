package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Encircling: it swims to the edge of the group round its target, hisses and coils its body on a ring round them that
 * shrinks from {@code encircleStartRadius} (open: its body covers less than half) to {@code encircleEndRadius} (closed)
 * in {@code encircleTicks}. Its body is a wall: whoever runs into it is pushed back to their side and hurt a little. Out
 * is over the thin tail (on land a jump clears it), under the body in deep water, through the gap while the ring is still
 * open, or by hurting it ({@code encircleBreakDamage}): then it breaks off the ring. Whoever is still inside when it
 * closes is squeezed and thrown up. In the water (on its surface), or with its crown on land and on islands; not in the
 * air or crawling. The ring itself is synced ({@link TheLostKeeper#coil}): server and client lay the body on it.
 */
final class KeeperEncircleGoal extends Goal {
    private static final double START_DISTANCE = 32.0;
    private static final double GROUP_RADIUS = 8.0;
    private static final int APPROACH_TICKS = 80;
    /** Height of its head's centre above the ring's floor: its head just clears the ground. */
    private static final double HEAD_HEIGHT = 1.3;
    /** The water's surface lies this far above the ring's floor: the body floats half in it. */
    private static final double WATER_FLOOR = 1.0;
    /** Radians ahead on the ring that the head steers to. */
    private static final double LEAD = 0.5;
    private static final double TURN_SCALE = 2.2;
    private static final int RING_CHECKS = 12;
    private static final int RING_CLEAR = 9;
    private static final int WALL_HIT_TICKS = 10;
    private static final double SQUEEZE_UP = 0.9;
    /** Blocks under a fighter's feet searched for the ground (a jump's height and a little more). */
    private static final int GROUND_SEARCH = 3;

    private final TheLostKeeper keeper;
    private final Map<UUID, Integer> wallHits = new HashMap<>();
    private int nextTick = -1;
    private boolean coiling;
    private int ticks;
    private int direction = 1;
    private Vec3 centre = Vec3.ZERO;
    private float damageAtStart;

    KeeperEncircleGoal(TheLostKeeper keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = keeper.getTarget();
        if (!keeper.mayAttack() || !(target instanceof Player player) || !TheLostKeeper.isFighter(player)) {
            // Only a fight that is over restarts the cooldown; a rest window or a stagger keeps it.
            if (target == null) {
                nextTick = -1;
            }
            return false;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        if (nextTick < 0) {
            nextTick = keeper.tickCount + config.encircleCooldownTicks(keeper.getRandom().nextInt(1 << 16)) / 2;
        }
        if (keeper.tickCount < nextTick || player.distanceToSqr(keeper.headCentre()) > START_DISTANCE * START_DISTANCE) {
            return false;
        }
        Vec3 ring = ringCentre(player);
        if (ring == null) {
            return false;
        }
        if (!roomFor(ring, config.encircleStartRadius.get())) {
            // No room here now; try again a little later.
            nextTick = keeper.tickCount + 40;
            return false;
        }
        centre = ring;
        return true;
    }

    /**
     * The ring's centre on its floor, between the fighters round the target, or null where it cannot coil: in the air,
     * or on land without its crown.
     */
    private Vec3 ringCentre(Player target) {
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        KeeperWater.Footing footing = KeeperWater.footing(target, config.burstShoreReach.get(), config.burstMinDepth.get());
        double floor;
        if (footing == KeeperWater.Footing.WATER) {
            KeeperWater.Column column = KeeperWater.below(target.level(), target.position());
            if (column == null || !(keeper.hasCrown() || keeper.isInWater())) {
                return null;
            }
            floor = column.surface().y - WATER_FLOOR;
        } else if (footing == KeeperWater.Footing.AIR || !keeper.hasCrown()) {
            return null;
        } else {
            floor = groundUnder(target);
        }
        double x = 0.0;
        double z = 0.0;
        List<Player> group = keeper.fighters(START_DISTANCE + GROUP_RADIUS);
        int count = 0;
        for (Player player : group) {
            if (player.distanceToSqr(target) <= GROUP_RADIUS * GROUP_RADIUS) {
                x += player.getX();
                z += player.getZ();
                count++;
            }
        }
        return count == 0 ? new Vec3(target.getX(), floor, target.getZ()) : new Vec3(x / count, floor, z / count);
    }

    /** The top of the ground the target stands on, also mid-jump: the first block with a collision under their feet. */
    private static double groundUnder(Player target) {
        Level level = target.level();
        BlockPos.MutableBlockPos pos = BlockPos.containing(target.getX(), target.getY() + 0.01, target.getZ()).mutable();
        for (int k = 0; k <= GROUND_SEARCH; k++, pos.move(Direction.DOWN)) {
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return pos.getY() + shape.max(Direction.Axis.Y);
            }
        }
        return Mth.floor(target.getY() + 0.01);
    }

    /** Whether most of the ring at its head's height is free of blocks (water counts as free). */
    private boolean roomFor(Vec3 ring, double radius) {
        int clear = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int k = 0; k < RING_CHECKS; k++) {
            double a = Math.PI * 2.0 * k / RING_CHECKS;
            pos.set(ring.x + Math.cos(a) * radius, ring.y + HEAD_HEIGHT, ring.z + Math.sin(a) * radius);
            if (keeper.level().getBlockState(pos).getCollisionShape(keeper.level(), pos).isEmpty()) {
                clear++;
            }
        }
        return clear >= RING_CLEAR;
    }

    @Override
    public boolean canContinueToUse() {
        if (!keeper.canAttack()) {
            return false;
        }
        return coiling ? ticks <= TheLostKeeper.keeperConfig().encircleTicks.get() : ticks <= APPROACH_TICKS;
    }

    @Override
    public boolean isInterruptable() {
        return !coiling;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        keeper.countAttack();
        coiling = false;
        ticks = 0;
        wallHits.clear();
        Vec3 head = keeper.headCentre();
        // Round the way it already swims, so it does not have to turn back.
        Vec3 radial = new Vec3(head.x - centre.x, 0.0, head.z - centre.z);
        Vec3 look = keeper.getLookAngle();
        direction = radial.x * look.z - radial.z * look.x >= 0.0 ? 1 : -1;
    }

    @Override
    public void stop() {
        if (coiling) {
            keeper.uncoil();
        }
        coiling = false;
        wallHits.clear();
        keeper.setTurnScale(1.0);
        keeper.face(null);
        nextTick = keeper.tickCount + TheLostKeeper.keeperConfig().encircleCooldownTicks(keeper.getRandom().nextInt(1 << 16));
    }

    @Override
    public void tick() {
        ticks++;
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        Vec3 head = keeper.headCentre();
        double start = config.encircleStartRadius.get();
        double angle = Math.atan2(head.z - centre.z, head.x - centre.x);
        if (!coiling) {
            Vec3 entry = onRing(angle + direction * LEAD, start);
            keeper.steer(entry, 1.3);
            if (horizontalDistance(head, centre) <= start + 2.0 && Math.abs(head.y - entry.y) <= 3.0) {
                coiling = true;
                ticks = 0;
                damageAtStart = keeper.damageTaken();
                keeper.setTurnScale(TURN_SCALE);
                keeper.coil(centre, start, direction);
                if (keeper.level() instanceof ServerLevel level) {
                    KeeperEffects.ringCall(level, keeper, centre.add(0.0, HEAD_HEIGHT - 0.5, 0.0), start);
                }
            }
            return;
        }
        int length = Math.max(1, config.encircleTicks.get());
        double radius = Mth.lerp(Math.min(1.0, ticks / (double) length), start, config.encircleEndRadius.get());
        keeper.coil(centre, radius, direction);
        keeper.steer(onRing(angle + direction * LEAD, radius), config.encircleSpeed.get());
        if (keeper.level() instanceof ServerLevel level && ticks % 10 == 0) {
            KeeperEffects.ringHiss(level, keeper);
        }
        if (keeper.damageTaken() - damageAtStart >= config.encircleBreakDamage.get()) {
            keeper.triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.ROAR);
            if (keeper.level() instanceof ServerLevel level) {
                KeeperEffects.ringBreaks(level, keeper);
            }
            ticks = length + 1;
            return;
        }
        wall(config, radius);
        if (ticks >= length) {
            squeeze(config, radius);
            ticks = length + 1;
        }
    }

    /** Whoever runs into its body is pushed back to the side they are on (inside stays inside) and hurt a little. */
    private void wall(LostKeeperConfig config, double radius) {
        double push = config.encircleWallPush.get();
        for (Player player : keeper.fighters(START_DISTANCE + radius)) {
            if (!touchesBody(player.getBoundingBox())) {
                continue;
            }
            boolean inside = horizontalDistance(player.position(), centre) < radius;
            Vec3 back = KeeperThrow.away(centre, player.position(), inside ? -push : push);
            player.setDeltaMovement(back.x, Math.max(player.getDeltaMovement().y, 0.15), back.z);
            player.hurtMarked = true;
            Integer last = wallHits.get(player.getUUID());
            if (last == null || keeper.tickCount - last >= WALL_HIT_TICKS) {
                wallHits.put(player.getUUID(), keeper.tickCount);
                player.hurt(keeper.damageSources().mobAttack(keeper), config.encircleWallDamage.get().floatValue() * keeper.strength());
            }
        }
    }

    private boolean touchesBody(AABB box) {
        AABB grown = box.inflate(0.2);
        if (keeper.getBoundingBox().intersects(grown)) {
            return true;
        }
        for (KeeperPart part : keeper.bodyParts()) {
            if (part.getBoundingBox().intersects(grown)) {
                return true;
            }
        }
        return false;
    }

    /** The ring closes: everyone still inside is squeezed and thrown up. */
    private void squeeze(LostKeeperConfig config, double radius) {
        keeper.triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.SQUEEZE);
        if (keeper.level() instanceof ServerLevel level) {
            KeeperEffects.squeeze(level, centre.add(0.0, HEAD_HEIGHT, 0.0), radius);
        }
        for (Player player : keeper.fighters(START_DISTANCE + radius)) {
            if (horizontalDistance(player.position(), centre) <= radius + 0.5 && Math.abs(player.getY() - centre.y) <= 3.0) {
                player.hurt(keeper.damageSources().mobAttack(keeper), config.encircleSqueezeDamage.get().floatValue() * keeper.strength());
                KeeperThrow.launch(player, SQUEEZE_UP, KeeperThrow.away(centre, player.position(), 0.2));
            }
        }
    }

    private Vec3 onRing(double angle, double radius) {
        return new Vec3(centre.x + Math.cos(angle) * radius, centre.y + HEAD_HEIGHT, centre.z + Math.sin(angle) * radius);
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
