package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;

/**
 * Asregen, the memory of The Risen (its wing): the burnt wing beats and an ash cloud of {@code ashRadius} settles where
 * his target stood a second later; it rains for {@code ashSeconds} ({@link UnfinishedAsh}).
 */
final class UnfinishedAshRainGoal extends UnfinishedAttackGoal {
    UnfinishedAshRainGoal(TheUnfinished boss) {
        super(boss, Graft.HERRIJZENDE);
    }

    @Override
    protected int cooldown() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        return UnfinishedConfig.cooldown(config.ashCooldownMin, config.ashCooldownMax, roll());
    }

    @Override
    protected int duration() {
        return UnfinishedAnimations.ASH_RAIN_TICKS;
    }

    @Override
    protected boolean wants(LivingEntity target) {
        double range = ServerConfig.UNFINISHED.ashRange.get();
        return boss.distanceToSqr(target) <= range * range;
    }

    @Override
    protected void begin(ServerLevel level, LivingEntity target) {
        boss.snapYaw(TheUnfinished.yawTo(boss.position(), target.position()));
        boss.triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.ASH_RAIN);
        UnfinishedEffects.ashRainStart(level, boss);
    }

    @Override
    protected void act(ServerLevel level, LivingEntity target, int tick) {
        if (tick == UnfinishedAnimations.ASH_CLOUD_TICK) {
            UnfinishedConfig config = ServerConfig.UNFINISHED;
            boss.ash().add(target.position(), config.ashRadius.get(), config.ashSeconds.get() * 20, boss.tickCount);
        }
    }
}
