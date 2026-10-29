package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * The one target goal of a Nightmare (replaces vanilla's {@code HurtByTargetGoal} + {@code NearestAttackableTargetGoal},
 * which would switch to whoever hit last): the fighters within its follow range, through walls, chosen by
 * {@link NightmareAggro} and reconsidered once a second. Add it as {@code targetSelector.addGoal(1, new
 * NightmareTargetGoal(this))}.
 */
public final class NightmareTargetGoal extends Goal {
    private static final int CHOOSE_TICKS = 20;

    private final Nightmare nightmare;
    @Nullable
    private Player candidate;
    private int nextChoice;

    public NightmareTargetGoal(Nightmare nightmare) {
        this.nightmare = nightmare;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    private List<Player> fighters() {
        return nightmare.fighters(nightmare.getAttributeValue(Attributes.FOLLOW_RANGE));
    }

    @Override
    public boolean canUse() {
        if (nightmare.tickCount < nextChoice) {
            return false;
        }
        nextChoice = nightmare.tickCount + CHOOSE_TICKS / 2;
        List<Player> fighters = fighters();
        if (fighters.isEmpty()) {
            // The fight is over: an old attacker does not win the next one.
            nightmare.aggro().clear();
            return false;
        }
        candidate = nightmare.aggro().choose(nightmare, fighters, null, nightmare.tickCount);
        return candidate != null;
    }

    @Override
    public void start() {
        nightmare.setTarget(candidate);
        nextChoice = nightmare.tickCount + CHOOSE_TICKS;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(nightmare.getTarget() instanceof Player target) || !Nightmare.isFighter(target)) {
            return false;
        }
        double range = nightmare.getAttributeValue(Attributes.FOLLOW_RANGE);
        return target.level() == nightmare.level() && target.distanceToSqr(nightmare) <= range * range;
    }

    @Override
    public void tick() {
        if (nightmare.tickCount < nextChoice || !(nightmare.getTarget() instanceof Player current)) {
            return;
        }
        nextChoice = nightmare.tickCount + CHOOSE_TICKS;
        Player chosen = nightmare.aggro().choose(nightmare, fighters(), current, nightmare.tickCount);
        if (chosen != null && chosen != current) {
            nightmare.setTarget(chosen);
        }
    }

    @Override
    public void stop() {
        nightmare.setTarget(null);
        candidate = null;
    }
}
