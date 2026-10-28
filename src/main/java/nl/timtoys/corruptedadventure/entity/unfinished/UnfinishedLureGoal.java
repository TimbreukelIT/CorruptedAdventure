package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;

/**
 * Lokroep, the memory of The Lost Keeper (its head and tail): the dragon head sings and every player in the fight
 * within {@code lureRadius} drifts toward him, into reach of his fist, for {@code lureTicks}. Sneaking resists
 * ({@link UnfinishedPull#SNEAK_SHARE}). Only when someone is far enough to be worth luring.
 */
final class UnfinishedLureGoal extends UnfinishedAttackGoal {
    /** Players nearer than this are not pulled (they are where he wants them). */
    private static final double NEAR = 6.0;
    /** He only sings when someone in the fight is at least this far off. */
    private static final double WORTH = 12.0;

    UnfinishedLureGoal(TheUnfinished boss) {
        super(boss, Graft.HOEDER);
    }

    @Override
    protected int cooldown() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return UnfinishedConfig.cooldown(config.lureCooldownMin, config.lureCooldownMax, roll());
    }

    @Override
    protected int duration() {
        return ServerConfig.UNFINISHED.lureTicks.get();
    }

    @Override
    protected boolean wants(LivingEntity target) {
        for (Player player : boss.fighters(ServerConfig.UNFINISHED.lureRadius.get())) {
            if (player.distanceToSqr(boss) > WORTH * WORTH) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void begin(ServerLevel level, LivingEntity target) {
        boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.LURE);
        UnfinishedEffects.lureSong(level, boss);
    }

    @Override
    protected void act(ServerLevel level, LivingEntity target, int tick) {
        if (tick % 30 == 0) {
            UnfinishedEffects.lureSong(level, boss);
        }
        double strength = ServerConfig.UNFINISHED.lureStrength.get();
        for (Player player : boss.fighters(ServerConfig.UNFINISHED.lureRadius.get())) {
            if (player.distanceToSqr(boss) <= NEAR * NEAR) {
                continue;
            }
            UnfinishedPull.toward(player, boss.position(), strength);
            if (tick % 5 == 0) {
                UnfinishedEffects.luredPlayer(level, player);
            }
        }
    }
}
