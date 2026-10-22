package nl.timtoys.corruptedadventure.condition;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * When a player's last void teleport (or the start of the wait) was ({@link VoidSlip}), in server ticks; the interval
 * is passed on every ask, so a live config change applies at once. In memory only, not saved: after a login (a new
 * player object) the wait starts again. Weak keys, so a player object that is gone is forgotten without a logout hook.
 * Server thread only.
 *
 * @param <K> the player (a type parameter so the timing is testable without a server)
 */
final class SlipSchedule<K> {
    private final Map<K, Integer> since = new WeakHashMap<>();

    /**
     * Whether a teleport is due at {@code now}: {@code intervalTicks} since the last one or the start of the wait. The
     * first ask starts the wait and is never due.
     */
    boolean isDue(K key, int now, int intervalTicks) {
        Integer start = since.putIfAbsent(key, now);
        return start != null && now - start >= intervalTicks;
    }

    /** After a teleport, or a try without a safe spot: the wait starts again from {@code now}. */
    void restart(K key, int now) {
        since.put(key, now);
    }

    /** Below the threshold or out of the inner band: the next time the wait starts from the beginning. */
    void forget(K key) {
        since.remove(key);
    }
}
