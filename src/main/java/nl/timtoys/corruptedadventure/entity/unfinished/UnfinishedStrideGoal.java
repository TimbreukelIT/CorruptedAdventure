package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import nl.timtoys.corruptedadventure.config.ServerConfig;

import java.util.EnumSet;

/**
 * His way of walking: no path finding (a 20-block colossus fits nowhere), but great slow steps straight toward his target
 * over whatever lies between, climbing onto it ({@link TheUnfinished#strideToward}). He turns before he walks, stops at
 * {@code strideStopDistance}, and stands still at a void edge or unloaded ground; past that the rift step takes over.
 */
final class UnfinishedStrideGoal extends Goal {
    private final TheUnfinished boss;

    UnfinishedStrideGoal(TheUnfinished boss) {
        this.boss = boss;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = boss.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double stop = ServerConfig.UNFINISHED.strideStopDistance.get();
        double dx = target.getX() - boss.getX();
        double dz = target.getZ() - boss.getZ();
        return dx * dx + dz * dz > stop * stop;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        boss.setStriding(true);
    }

    @Override
    public void stop() {
        boss.setStriding(false);
    }

    @Override
    public void tick() {
        LivingEntity target = boss.getTarget();
        if (target != null) {
            boss.setStriding(boss.strideToward(target.position()));
        }
    }
}
