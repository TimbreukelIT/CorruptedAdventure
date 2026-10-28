package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;

import java.util.EnumSet;
import java.util.List;

/**
 * Wisselen: with at least two players in the fight, The Hollow marks two of them and after a short warning they change
 * places (multiplayer). It stands still while casting. A player who died, left or went too far during the warning breaks
 * the swap; the cooldown starts anyway.
 */
final class HollowSwapGoal extends Goal {
    private final TheHollow hollow;
    private ServerPlayer first;
    private ServerPlayer second;
    private int warning;
    private int nextSwapTick;

    HollowSwapGoal(TheHollow hollow) {
        this.hollow = hollow;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        // The first swap never comes right after the summon or the arrival.
        nextSwapTick = cooldown() / 2;
    }

    @Override
    public boolean canUse() {
        if (hollow.tickCount < nextSwapTick || hollow.getTarget() == null) {
            return false;
        }
        List<Player> fighters = hollow.fighters();
        fighters.removeIf(player -> !(player instanceof ServerPlayer));
        if (fighters.size() < 2) {
            return false;
        }
        int a = hollow.getRandom().nextInt(fighters.size());
        int b = (a + 1 + hollow.getRandom().nextInt(fighters.size() - 1)) % fighters.size();
        first = (ServerPlayer) fighters.get(a);
        second = (ServerPlayer) fighters.get(b);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return warning > 0;
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
        // At least one tick, so tick() always gets to the swap (the config's minimum is 1 too).
        warning = Math.max(1, ServerConfig.HOLLOW.swapWarningTicks.get());
        hollow.getNavigation().stop();
        hollow.triggerAnim(HollowAnimations.ACTION, HollowAnimations.SWAP);
        ServerLevel level = (ServerLevel) hollow.level();
        HollowEffects.swapMark(level, first);
        HollowEffects.swapMark(level, second);
    }

    @Override
    public void stop() {
        warning = 0;
        first = null;
        second = null;
        nextSwapTick = hollow.tickCount + cooldown();
    }

    @Override
    public void tick() {
        hollow.getLookControl().setLookAt(first, 30.0F, 30.0F);
        if (--warning > 0) {
            return;
        }
        if (stillInFight(first) && stillInFight(second)) {
            swap(first, second);
        }
    }

    private boolean stillInFight(ServerPlayer player) {
        double reach = ServerConfig.HOLLOW.common.fightRadius.get() * 1.5;
        return Nightmare.isFighter(player) && !player.hasDisconnected() && player.level() == hollow.level()
                && player.distanceToSqr(hollow) <= reach * reach;
    }

    private static void swap(ServerPlayer a, ServerPlayer b) {
        a.stopRiding();
        b.stopRiding();
        Vec3 at = a.position();
        Vec3 bt = b.position();
        ServerLevel level = a.serverLevel();
        a.teleportTo(bt.x, bt.y, bt.z);
        b.teleportTo(at.x, at.y, at.z);
        a.resetFallDistance();
        b.resetFallDistance();
        HollowEffects.swapped(level, a);
        HollowEffects.swapped(level, b);
    }

    private int cooldown() {
        return ServerConfig.HOLLOW.swapCooldown.get() * 20;
    }
}
