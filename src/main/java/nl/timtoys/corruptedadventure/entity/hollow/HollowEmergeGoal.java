package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Holds The Hollow still while it pulls itself out of its arrival rift, so no attack or wandering starts. */
final class HollowEmergeGoal extends Goal {
    private final TheHollow hollow;

    HollowEmergeGoal(TheHollow hollow) {
        this.hollow = hollow;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return hollow.isEmerging();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public void start() {
        hollow.getNavigation().stop();
    }
}
