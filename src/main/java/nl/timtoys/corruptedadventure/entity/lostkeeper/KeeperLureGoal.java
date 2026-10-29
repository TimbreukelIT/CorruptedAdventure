package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;
import nl.timtoys.corruptedadventure.network.WindGustPayload;

import java.util.EnumSet;

/**
 * The siren's call of its mythical half: it stops, raises its head, its crown glows and it sings; every fighter within
 * {@code lureRadius} who can see it is pulled slowly towards it. The pull is added to the player's own movement by their
 * client (a {@link WindGustPayload}, like vanilla's explosion knockback), so walking away still gets you away, slowly,
 * and sprinting easily; standing still you drift towards its jaws. Less pull in the air, so it does not fling anyone.
 * The call ends early when someone comes within reach of its jaws: then the lunge ({@link KeeperGrabGoal}) can follow.
 */
final class KeeperLureGoal extends Goal {
    private static final double AIRBORNE_SHARE = 0.35;

    private final TheLostKeeper keeper;
    private int ticksLeft;
    private int tick;
    private int nextTick = -1;

    KeeperLureGoal(TheLostKeeper keeper) {
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
            // The first call comes a while into the fight, not at once.
            nextTick = keeper.tickCount + config.lureCooldownTicks(keeper.getRandom().nextInt(1 << 16)) / 2;
        }
        double reach = config.lureRadius.get();
        return keeper.tickCount >= nextTick && player.distanceToSqr(keeper.headCentre()) <= reach * reach;
    }

    @Override
    public boolean canContinueToUse() {
        return ticksLeft > 0 && keeper.canAttack() && !someoneAtTheJaws();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        keeper.countAttack();
        ticksLeft = TheLostKeeper.keeperConfig().lureSeconds.get() * 20;
        tick = 0;
        keeper.setLuring(true);
        keeper.steer(null, 0.0);
    }

    @Override
    public void stop() {
        keeper.setLuring(false);
        keeper.face(null);
        nextTick = keeper.tickCount + TheLostKeeper.keeperConfig().lureCooldownTicks(keeper.getRandom().nextInt(1 << 16));
    }

    @Override
    public void tick() {
        ticksLeft--;
        tick++;
        LivingEntity target = keeper.getTarget();
        keeper.steer(null, 0.0);
        if (target != null) {
            keeper.face(target.getEyePosition());
        }
        if (!(keeper.level() instanceof ServerLevel level)) {
            return;
        }
        LostKeeperConfig config = TheLostKeeper.keeperConfig();
        Vec3 head = keeper.headCentre();
        float pull = config.lurePull.get().floatValue();
        for (Player player : keeper.fighters(config.lureRadius.get())) {
            if (!(player instanceof ServerPlayer serverPlayer) || !keeper.hasLineOfSight(player)) {
                continue;
            }
            Vec3 towards = new Vec3(head.x - player.getX(), 0.0, head.z - player.getZ());
            if (towards.lengthSqr() < 1.0) {
                continue;
            }
            Vec3 push = towards.normalize().scale(pull * (player.onGround() || player.isInWater() ? 1.0 : AIRBORNE_SHARE));
            // A connection without our channel (a GameTest mock player) gets nothing.
            if (serverPlayer.connection.hasChannel(WindGustPayload.TYPE)) {
                PacketDistributor.sendToPlayer(serverPlayer, new WindGustPayload((float) push.x, (float) push.z));
            }
            if (tick % 5 == 0) {
                KeeperEffects.lureThread(level, player.position(), head);
            }
        }
        KeeperEffects.lureSong(level, keeper, tick);
    }

    private boolean someoneAtTheJaws() {
        double reach = TheLostKeeper.keeperConfig().grabReach.get() + 2.0;
        for (Player player : keeper.fighters(reach + 4.0)) {
            if (player.distanceToSqr(keeper.mouth()) <= reach * reach) {
                return true;
            }
        }
        return false;
    }
}
