package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * What every attack of The Unfinished shares: one at a time (they all hold MOVE and LOOK) with a gap after each
 * ({@link TheUnfinished#attackDone}), an own cooldown (the first one half a cooldown after he is summoned, so they do not
 * all come at once), a fixed length in ticks, and the graft it needs: a memory whose graft fell away is gone, and one
 * that is running stops when its graft falls. {@link #act} gets the tick number from 1.
 */
abstract class UnfinishedAttackGoal extends Goal {
    protected final TheUnfinished boss;
    @Nullable
    private final Graft graft;
    private int ticks;
    private int nextUseTick = -1;

    UnfinishedAttackGoal(TheUnfinished boss, @Nullable Graft graft) {
        this.boss = boss;
        this.graft = graft;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** Ticks until the next use may start, drawn anew after each use. */
    protected abstract int cooldown();

    /** Ticks the attack lasts. */
    protected abstract int duration();

    /** Whether it wants to start against this target now (the gap, cooldown and graft are checked already). */
    protected abstract boolean wants(LivingEntity target);

    protected abstract void begin(ServerLevel level, LivingEntity target);

    protected abstract void act(ServerLevel level, LivingEntity target, int tick);

    protected void end(ServerLevel level) {
    }

    /** Whether it is over before its full length (an attack whose length is not fixed). */
    protected boolean finished() {
        return false;
    }

    /** Whether it may start before its cooldown is over (the rift step when its target runs far). */
    protected boolean urgent(LivingEntity target) {
        return false;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = boss.getTarget();
        if (target == null || !target.isAlive() || !boss.canStartAttack() || (graft != null && !boss.hasGraft(graft))) {
            return false;
        }
        if (nextUseTick < 0) {
            nextUseTick = boss.tickCount + cooldown() / 2;
        }
        return (boss.tickCount >= nextUseTick || urgent(target)) && wants(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = boss.getTarget();
        return ticks < duration() && !finished() && target != null && target.isAlive() && (graft == null || boss.hasGraft(graft));
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
        ticks = 0;
        boss.getNavigation().stop();
        boss.attackStarted();
        begin((ServerLevel) boss.level(), boss.getTarget());
    }

    @Override
    public void tick() {
        LivingEntity target = boss.getTarget();
        if (target != null) {
            act((ServerLevel) boss.level(), target, ++ticks);
        }
    }

    @Override
    public void stop() {
        end((ServerLevel) boss.level());
        ticks = 0;
        nextUseTick = boss.tickCount + cooldown();
        boss.attackDone();
    }

    protected int roll() {
        return boss.getRandom().nextInt(1 << 16);
    }
}
