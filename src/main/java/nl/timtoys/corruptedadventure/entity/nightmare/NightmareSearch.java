package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where it last saw its target and whether it is searching (fase-4-nightmares-concept.md "Leven-ronde"): out of sight
 * for a while, it goes to that place, sniffs and looks around; sight ends the search, and so does the search's time
 * (then it sniffs the target out: the last known place becomes where the target is). Pure; {@link Nightmare} feeds it.
 */
final class NightmareSearch {
    @Nullable
    private Vec3 lastKnown;
    private int unseenTicks;
    private int searchTicks;
    private boolean arrived;

    @Nullable
    Vec3 lastKnown() {
        return lastKnown;
    }

    boolean isSearching() {
        return searchTicks > 0;
    }

    boolean hasArrived() {
        return arrived;
    }

    void arrive() {
        arrived = true;
    }

    /** The target is in sight at {@code at}: the place is known, a search ends. True if that ended a search. */
    boolean seen(Vec3 at) {
        lastKnown = at;
        unseenTicks = 0;
        return stop();
    }

    /**
     * The target is out of sight for {@code ticks} more (it stands at {@code at}). True when that starts a search.
     * Without a known place yet it knows the target's: it found the target through walls, as all Nightmares do.
     */
    boolean unseen(int ticks, Vec3 at, int searchAfterTicks, int searchLengthTicks) {
        if (lastKnown == null) {
            lastKnown = at;
        }
        if (searchTicks > 0) {
            searchTicks -= ticks;
            if (searchTicks <= 0) {
                // Given up: it sniffs the target out, and the next search starts from there.
                stop();
                lastKnown = at;
                unseenTicks = 0;
            }
            return false;
        }
        unseenTicks += ticks;
        if (unseenTicks < searchAfterTicks) {
            return false;
        }
        searchTicks = searchLengthTicks;
        arrived = false;
        return true;
    }

    /** It heard the target (a hit from out of sight): the place is known, a search goes there anew. */
    void heard(Vec3 at) {
        lastKnown = at;
        arrived = false;
    }

    /** True if a search was on. */
    boolean stop() {
        boolean was = searchTicks > 0;
        searchTicks = 0;
        arrived = false;
        return was;
    }

    /** No target: nothing known. */
    void reset() {
        stop();
        lastKnown = null;
        unseenTicks = 0;
    }
}
