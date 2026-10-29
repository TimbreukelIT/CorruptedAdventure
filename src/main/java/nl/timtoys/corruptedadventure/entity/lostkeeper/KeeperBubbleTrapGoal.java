package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;
import nl.timtoys.corruptedadventure.network.KeeperCurrentPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The bubble trap: it stops, lowers its head and calls up its breath from the deep. Under its target (and the nearest
 * other fighters, up to {@code bubbleSpots}) bubbles rise for {@code bubbleWarnTicks} on a spot that stays where it was:
 * stepping aside avoids it. Then, in or on water, a current pulls down whoever is in the column for
 * {@code bubblePullTicks} (added to their own swimming, so swimming out sideways escapes); on land, an island or in the
 * air a gust throws them up. Nothing stays behind in the world.
 */
final class KeeperBubbleTrapGoal extends Goal {
    /** Blocks above a spot on land that the gust still catches (a jump does not save you). */
    private static final double GUST_HEIGHT = 3.0;

    private final TheLostKeeper keeper;
    private final List<Spot> spots = new ArrayList<>();
    private int nextTick = -1;
    private int tick;

    /** One trap: where (on water: the surface), how deep its current goes (water only), and who it caught. */
    private static final class Spot {
        final Vec3 at;
        final boolean water;
        final double bottom;
        final Set<UUID> caught = new HashSet<>();

        Spot(Vec3 at, boolean water, double bottom) {
            this.at = at;
            this.water = water;
            this.bottom = bottom;
        }
    }

    KeeperBubbleTrapGoal(TheLostKeeper keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = keeper.getTarget();
        if (!keeper.mayAttack() || keeper.isCoiling() || !(target instanceof Player player) || !TheLostKeeper.isFighter(player)) {
            // Only a fight that is over restarts the cooldown; a rest window or a stagger keeps it.
            if (target == null) {
                nextTick = -1;
            }
            return false;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        if (nextTick < 0) {
            nextTick = keeper.tickCount + config.bubbleCooldownTicks(keeper.getRandom().nextInt(1 << 16)) / 2;
        }
        double range = config.bubbleRange.get();
        return keeper.tickCount >= nextTick && player.distanceToSqr(keeper.headCentre()) <= range * range;
    }

    @Override
    public boolean canContinueToUse() {
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        int length = config.bubbleWarnTicks.get() + (anyWater() ? config.bubblePullTicks.get() : 1);
        return keeper.isAlive() && !keeper.isStunned() && !keeper.isFalling() && tick < length;
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
        keeper.countAttack();
        tick = 0;
        spots.clear();
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        LivingEntity target = keeper.getTarget();
        List<Player> fighters = keeper.fighters(config.bubbleRange.get());
        fighters.sort(Comparator.comparingDouble((Player p) -> p == target ? -1.0 : p.distanceToSqr(keeper)));
        for (Player player : fighters) {
            if (spots.size() >= config.bubbleSpots.get()) {
                break;
            }
            spots.add(spotUnder(player));
        }
        keeper.steer(null, 0.0);
        keeper.triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.BUBBLE_CALL);
        if (keeper.level() instanceof ServerLevel level) {
            KeeperEffects.bubbleCall(level, keeper);
        }
    }

    private static Spot spotUnder(Player player) {
        KeeperWater.Column column = KeeperWater.below(player.level(), player.position());
        if (column != null) {
            Vec3 surface = column.surface();
            return new Spot(new Vec3(player.getX(), surface.y, player.getZ()), true, surface.y - column.depth());
        }
        return new Spot(player.position(), false, player.getY());
    }

    @Override
    public void stop() {
        spots.clear();
        keeper.face(null);
        nextTick = keeper.tickCount + TheLostKeeper.keeperConfig().bubbleCooldownTicks(keeper.getRandom().nextInt(1 << 16));
    }

    @Override
    public void tick() {
        tick++;
        keeper.steer(null, 0.0);
        if (!spots.isEmpty()) {
            keeper.face(spots.get(0).at);
        }
        if (!(keeper.level() instanceof ServerLevel level)) {
            return;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        int warn = config.bubbleWarnTicks.get();
        double radius = config.bubbleRadius.get();
        for (Spot spot : spots) {
            if (tick <= warn) {
                KeeperEffects.bubbleWarn(level, spot.at, spot.water, radius, tick);
            } else if (spot.water) {
                current(level, spot, config, radius, tick == warn + 1);
            } else if (tick == warn + 1) {
                gust(level, spot, config, radius);
            }
        }
    }

    /** The column of water pulls down whoever is in it; the first tick also hurts them. */
    private void current(ServerLevel level, Spot spot, LostKeeperConfig config, double radius, boolean first) {
        if (first) {
            KeeperEffects.currentStart(level, spot.at);
        }
        KeeperEffects.current(level, spot.at, spot.bottom, radius);
        float pull = config.bubblePull.get().floatValue();
        for (Player player : inColumn(spot, radius, spot.bottom - 1.0, spot.at.y + 2.0)) {
            if (spot.caught.add(player.getUUID())) {
                // Out of a boat: the current takes the fighter, not the boat.
                if (player.getVehicle() instanceof Boat) {
                    player.stopRiding();
                }
                player.hurt(keeper.damageSources().mobAttack(keeper), config.bubbleDamage.get().floatValue() * keeper.strength());
            }
            // A connection without our channel (a GameTest mock player) gets nothing.
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection.hasChannel(KeeperCurrentPayload.TYPE)) {
                PacketDistributor.sendToPlayer(serverPlayer, new KeeperCurrentPayload(0.0F, -pull, 0.0F));
            }
        }
    }

    /** On land (or in the air): one gust throws everyone on the spot up. */
    private void gust(ServerLevel level, Spot spot, LostKeeperConfig config, double radius) {
        KeeperEffects.gust(level, spot.at);
        for (Player player : inColumn(spot, radius, spot.at.y - 1.0, spot.at.y + GUST_HEIGHT)) {
            player.hurt(keeper.damageSources().mobAttack(keeper), config.bubbleDamage.get().floatValue() * keeper.strength());
            KeeperThrow.launch(player, config.bubbleLaunch.get(), KeeperThrow.away(spot.at, player.position(), 0.15));
        }
    }

    /** The fighters within {@code radius} (horizontally) of a spot, with their feet between the two heights. */
    private List<Player> inColumn(Spot spot, double radius, double low, double high) {
        List<Player> result = new ArrayList<>();
        for (Player player : keeper.fighters(TheLostKeeper.keeperConfig().bubbleRange.get() + 16.0)) {
            double dx = player.getX() - spot.at.x;
            double dz = player.getZ() - spot.at.z;
            if (dx * dx + dz * dz <= radius * radius && player.getY() >= low && player.getY() <= high) {
                result.add(player);
            }
        }
        return result;
    }

    private boolean anyWater() {
        for (Spot spot : spots) {
            if (spot.water) {
                return true;
            }
        }
        return false;
    }
}
