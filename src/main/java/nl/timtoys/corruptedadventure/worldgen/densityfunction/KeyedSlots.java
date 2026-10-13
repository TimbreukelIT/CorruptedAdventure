package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.LongFunction;

/**
 * A small lock-free cache of values keyed by a long, for objects shared by all worldgen threads (a random state's
 * density function, a biome source): neither a {@link ThreadLocal} (no entries left behind in the worker threads) nor
 * {@link ThreadOwned} (which threads keep replacing on a shared object). Each key has one slot ({@link #SLOTS}, by its
 * hash); an entry is immutable, so a thread reads a whole entry or computes the value again. Only a cache: values must
 * be pure functions of the key.
 */
public final class KeyedSlots<V> {
    private static final int SLOTS = 64;
    private final AtomicReferenceArray<Entry<V>> slots = new AtomicReferenceArray<>(SLOTS);

    /** The value of {@code key}, computed by {@code compute} unless its slot holds it. */
    public V get(long key, LongFunction<V> compute) {
        int slot = (int) (mix(key) & (SLOTS - 1));
        Entry<V> entry = slots.get(slot);
        if (entry != null && entry.key == key) {
            return entry.value;
        }
        V value = compute.apply(key);
        slots.set(slot, new Entry<>(key, value));
        return value;
    }

    /** Packs two ints into a key. */
    public static long key(int a, int b) {
        return (long) a << 32 | b & 0xFFFFFFFFL;
    }

    private static long mix(long key) {
        return SeededHash.mix(key);
    }

    private record Entry<V>(long key, V value) {
    }
}
