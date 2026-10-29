package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The rest window of a walking Nightmare: it keeps its eyes on its target and heads for {@link Nightmare#restPoint()}
 * (backing off or circling) through {@link Nightmare#moveForReaction}. Add it above the attack goals (a lower priority
 * number); one with its own movement reads {@link Nightmare#isResting()} and {@link Nightmare#restPoint()} instead.
 */
public final class NightmarePaceGoal extends Goal {
    private static final int REPATH_TICKS = 10;

    private final Nightmare nightmare;
    private int repath;

    public NightmarePaceGoal(Nightmare nightmare) {
        this.nightmare = nightmare;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return nightmare.isResting() && nightmare.getTarget() != null && !nightmare.isStaggered();
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        nightmare.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = nightmare.getTarget();
        if (target == null) {
            return;
        }
        nightmare.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--repath <= 0) {
            repath = REPATH_TICKS;
            Vec3 point = nightmare.restPoint();
            if (point != null) {
                nightmare.moveForReaction(point, 1.0);
            }
        }
    }
}
