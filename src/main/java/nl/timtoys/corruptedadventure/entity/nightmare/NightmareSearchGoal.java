package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * The search of a walking Nightmare: to the last place it saw its target ({@link Nightmare#moveForReaction}), there
 * {@link Nightmare#onSniff} once, then it looks around until the search ends. Add it above the attack goals (a lower
 * priority number), so it takes over from a chase; one with its own movement reads {@link Nightmare#isSearching()} and
 * {@link Nightmare#lastKnownPosition()} instead.
 */
public final class NightmareSearchGoal extends Goal {
    private static final int REPATH_TICKS = 20;
    /** Repaths without getting near before it sniffs where it stands (the place cannot be reached). */
    private static final int MAX_REPATHS = 8;
    private static final double ARRIVE_DISTANCE = 2.5;

    private final Nightmare nightmare;
    private int repath;
    private int repaths;
    private int nextLook;
    /** The place this walk goes to, and whether it had arrived; a new place or a fresh walk counts its repaths anew. */
    @Nullable
    private Vec3 heading;
    private boolean arrived;

    public NightmareSearchGoal(Nightmare nightmare) {
        this.nightmare = nightmare;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return nightmare.isSearching() && nightmare.lastKnownPosition() != null && !nightmare.isStaggered();
    }

    @Override
    public void start() {
        repath = 0;
        repaths = 0;
        nextLook = 0;
        heading = nightmare.lastKnownPosition();
        arrived = nightmare.hasArrivedAtSearch();
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
        Vec3 place = nightmare.lastKnownPosition();
        if (place == null) {
            return;
        }
        boolean arrivedNow = nightmare.hasArrivedAtSearch();
        if (!place.equals(heading) || (arrived && !arrivedNow)) {
            // It heard its target mid-search: a fresh walk to the new place, else it would "arrive" at once.
            heading = place;
            repath = 0;
            repaths = 0;
        }
        arrived = arrivedNow;
        if (!arrivedNow) {
            double dx = place.x - nightmare.getX();
            double dz = place.z - nightmare.getZ();
            if (dx * dx + dz * dz <= ARRIVE_DISTANCE * ARRIVE_DISTANCE || repaths > MAX_REPATHS) {
                nightmare.getNavigation().stop();
                nightmare.arriveAtSearch();
                return;
            }
            nightmare.getLookControl().setLookAt(place.x, place.y + 1.0, place.z);
            if (--repath <= 0) {
                repath = REPATH_TICKS;
                repaths++;
                nightmare.moveForReaction(place, 1.0);
            }
            return;
        }
        if (--nextLook <= 0) {
            nextLook = 30 + nightmare.getRandom().nextInt(30);
            double angle = nightmare.getRandom().nextDouble() * Math.PI * 2.0;
            nightmare.getLookControl().setLookAt(nightmare.getX() + Math.cos(angle) * 8.0, nightmare.getEyeY(),
                    nightmare.getZ() + Math.sin(angle) * 8.0);
        }
    }
}
