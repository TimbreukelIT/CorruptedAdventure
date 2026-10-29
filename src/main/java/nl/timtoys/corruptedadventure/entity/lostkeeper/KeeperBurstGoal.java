package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Dive and burst: it dives away into the deep, out of sight in its mist, and swims under its target. There the surface
 * churns for {@code burstWarnTicks} on a spot that no longer follows (the time to get away); then it bursts straight up
 * through it and whoever is on it (swimming, in a boat, or on the shore beside it) is hit and launched; a boat goes up too
 * and its rider falls out ({@link KeeperThrow}). Only where there is deep water under or beside the target: in open water,
 * or at a shore, where it bursts from the water beside them.
 */
final class KeeperBurstGoal extends Goal {
    private static final double START_DISTANCE = 40.0;
    private static final int DIVE_TICKS = 60;
    private static final int BURST_TICKS = 30;
    /** It dives this far from the spot, away from the target, before it turns under them. */
    private static final double DIVE_AWAY = 6.0;
    /** Its head's centre at most this deep under the surface while it lurks. */
    private static final double LURK_DEPTH = 6.0;
    /** How deep under the surface its head must be to count as dived (shallower where the water is shallow). */
    private static final double DIVED_DEPTH = 1.5;
    /** Blocks above the surface (and below it) within which the burst still hits. */
    private static final double HIT_ABOVE = 3.0;
    private static final double OUTWARD = 0.35;

    private enum Phase {
        DIVE, LURK, WARN, BURST
    }

    private final TheLostKeeper keeper;
    private final Set<Entity> hit = new HashSet<>();
    private int nextTick = -1;
    private Phase phase = Phase.DIVE;
    private int phaseTicks;
    @Nullable
    private KeeperWater.Column column;
    private Vec3 spot = Vec3.ZERO;
    private Vec3 divePoint = Vec3.ZERO;
    private double lurkY;
    private double divedY;
    private boolean struck;
    private boolean wasInWater;

    KeeperBurstGoal(TheLostKeeper keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = keeper.getTarget();
        if (!keeper.mayAttack() || keeper.isCoiling() || !keeper.canSwimHere() || !(target instanceof Player player)
                || !TheLostKeeper.isFighter(player) || player.getVehicle() == keeper) {
            if (target == null) {
                nextTick = -1;
            }
            return false;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        if (nextTick < 0) {
            nextTick = keeper.tickCount + config.burstCooldownTicks(keeper.getRandom().nextInt(1 << 16)) / 2;
        }
        if (keeper.tickCount < nextTick || player.distanceToSqr(keeper.headCentre()) > START_DISTANCE * START_DISTANCE) {
            return false;
        }
        column = waterFor(player, config);
        return column != null;
    }

    /** The deep water under the target, or beside them at a shore; null without. */
    @Nullable
    static KeeperWater.Column waterFor(Player player, LostKeeperConfig config) {
        int depth = config.burstMinDepth.get();
        KeeperWater.Column below = KeeperWater.below(player.level(), player.position());
        if (below != null) {
            return below.depth() >= depth ? below : null;
        }
        return KeeperWater.beside(player.level(), player.position(), config.burstShoreReach.get(), depth);
    }

    @Override
    public boolean canContinueToUse() {
        if (!keeper.canAttack()) {
            return false;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        return switch (phase) {
            case DIVE -> phaseTicks <= DIVE_TICKS && keeper.getTarget() != null;
            case LURK -> phaseTicks <= config.burstLurkTicks.get() && keeper.getTarget() != null;
            case WARN -> true;
            case BURST -> phaseTicks <= BURST_TICKS;
        };
    }

    @Override
    public boolean isInterruptable() {
        return phase == Phase.DIVE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        keeper.countAttack();
        hit.clear();
        struck = false;
        wasInWater = keeper.isInWater();
        setSpot(column);
        Vec3 away = KeeperThrow.away(spot, keeper.headCentre(), DIVE_AWAY);
        Vec3 aside = new Vec3(spot.x + away.x, lurkY, spot.z + away.z);
        divePoint = isWater(aside) ? aside : new Vec3(spot.x, lurkY, spot.z);
        enter(Phase.DIVE);
        keeper.triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.DIVE);
    }

    private void setSpot(KeeperWater.Column water) {
        column = water;
        Vec3 surface = water.surface();
        spot = surface;
        double floor = surface.y - water.depth();
        lurkY = Math.max(floor + 1.4, surface.y - LURK_DEPTH);
        // In shallow water (burstMinDepth 2) it lurks less than DIVED_DEPTH deep: halfway down to there counts too.
        divedY = surface.y - Math.min(DIVED_DEPTH, (surface.y - lurkY) * 0.5);
    }

    @Override
    public void stop() {
        hit.clear();
        column = null;
        keeper.face(null);
        nextTick = keeper.tickCount + TheLostKeeper.keeperConfig().burstCooldownTicks(keeper.getRandom().nextInt(1 << 16));
    }

    @Override
    public void tick() {
        phaseTicks++;
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        ServerLevel level = keeper.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        boolean inWater = keeper.isInWater();
        if (inWater && !wasInWater && level != null) {
            KeeperEffects.diveSplash(level, keeper.headCentre());
        }
        wasInWater = inWater;
        switch (phase) {
            case DIVE -> {
                keeper.steer(divePoint, 1.3);
                if (keeper.headCentre().y <= divedY && inWater) {
                    enter(Phase.LURK);
                }
            }
            case LURK -> {
                // Under the target while it still swims or stands there; the spot locks with the warning.
                if (keeper.getTarget() instanceof Player player) {
                    KeeperWater.Column water = waterFor(player, config);
                    if (water != null) {
                        setSpot(water);
                        if (KeeperWater.below(player.level(), player.position()) != null) {
                            spot = new Vec3(player.getX(), spot.y, player.getZ());
                        }
                    }
                }
                Vec3 under = new Vec3(spot.x, lurkY, spot.z);
                keeper.steer(under, 1.1);
                Vec3 head = keeper.headCentre();
                double dx = head.x - spot.x;
                double dz = head.z - spot.z;
                if (dx * dx + dz * dz <= 4.0 || phaseTicks >= config.burstLurkTicks.get()) {
                    enter(Phase.WARN);
                }
            }
            case WARN -> {
                keeper.steer(null, 0.0);
                keeper.face(spot.add(0.0, 10.0, 0.0));
                if (level != null) {
                    KeeperEffects.burstWarn(level, spot, config.burstRadius.get(), phaseTicks);
                }
                if (phaseTicks >= config.burstWarnTicks.get()) {
                    enter(Phase.BURST);
                    keeper.triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.BURST);
                }
            }
            case BURST -> {
                double above = keeper.hasCrown() ? 7.0 : 2.0;
                keeper.steer(new Vec3(spot.x, spot.y + above + 4.0, spot.z), config.burstSpeed.get());
                Vec3 head = keeper.headCentre();
                if (!struck && head.y >= spot.y - 2.0) {
                    struck = true;
                    if (level != null) {
                        KeeperEffects.burst(level, spot, config.burstRadius.get());
                    }
                }
                if (struck) {
                    strike(config);
                }
                if (head.y >= spot.y + above) {
                    phaseTicks = BURST_TICKS + 1;
                }
            }
        }
    }

    /** Everyone (and every boat) on the spot is hit once and thrown up. */
    private void strike(LostKeeperConfig config) {
        double radius = config.burstRadius.get();
        AABB area = new AABB(spot.x - radius, spot.y - 2.0, spot.z - radius, spot.x + radius, spot.y + HIT_ABOVE, spot.z + radius);
        for (Player player : keeper.fighters(START_DISTANCE + 16.0)) {
            if (hit.contains(player) || !area.intersects(player.getBoundingBox()) || !inside(player, radius)) {
                continue;
            }
            hit.add(player);
            player.hurt(keeper.damageSources().mobAttack(keeper), config.burstDamage.get().floatValue() * keeper.strength());
            KeeperThrow.launch(player, config.burstLaunch.get(), KeeperThrow.away(spot, player.position(), OUTWARD));
        }
        // An empty boat on the spot goes up too.
        for (Boat boat : keeper.level().getEntitiesOfClass(Boat.class, area)) {
            if (inside(boat, radius) && hit.add(boat)) {
                boat.setDeltaMovement(KeeperThrow.away(spot, boat.position(), OUTWARD * 0.5).add(0.0, config.burstLaunch.get() * 0.8, 0.0));
                boat.hurtMarked = true;
            }
        }
    }

    private boolean inside(Entity entity, double radius) {
        double dx = entity.getX() - spot.x;
        double dz = entity.getZ() - spot.z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private boolean isWater(Vec3 point) {
        return keeper.level().getFluidState(BlockPos.containing(point)).is(FluidTags.WATER);
    }

    private void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
    }
}
