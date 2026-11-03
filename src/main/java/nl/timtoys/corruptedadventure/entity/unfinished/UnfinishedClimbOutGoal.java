package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Holds him while he climbs out of the torn ground at his arrival: no stride, no attack, no reaction meanwhile. */
final class UnfinishedClimbOutGoal extends Goal {
    private final TheUnfinished boss;

    UnfinishedClimbOutGoal(TheUnfinished boss) {
        this.boss = boss;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return boss.isClimbing();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }
}
