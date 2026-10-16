package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.LongFunction;

/**
 * A small lock-free cache of values keyed by a long, for objects shared by all worldgen threads (a random state's
 * density function, a biome source): neither a {@link ThreadLocal} (no entries left behind in the worker threads) nor
 * {@link ThreadOwned} (which threads keep replacing on a shared object). Each key has one slot (of {@link #SLOTS}, or
 * the number given, by its hash); an entry is immutable, so a thread reads a whole entry or computes the value again.
 * Only a cache: values must be pure functions of the key.
 */
public final class KeyedSlots<V> {
    private static final int SLOTS = 64;
    private final AtomicReferenceArray<Entry<V>> slots;
    private final int mask;

    public KeyedSlots() {
        this(SLOTS);
    }

    /** @param slots the number of slots, a power of two */
    public KeyedSlots(int slots) {
        if (slots <= 0 || (slots & (slots - 1)) != 0) {
            throw new IllegalArgumentException("Slot count must be a power of two: " + slots);
        }
        this.slots = new AtomicReferenceArray<>(slots);
        this.mask = slots - 1;
    }

    /** The value of {@code key}, computed by {@code compute} unless its slot holds it. */
    public V get(long key, LongFunction<V> compute) {
        int slot = slot(key);
        Entry<V> entry = slots.get(slot);
        if (entry != null && entry.key == key) {
            return entry.value;
        }
        V value = compute.apply(key);
        slots.set(slot, new Entry<>(key, value));
        return value;
    }

    /** The value of {@code key} if its slot holds it, else null (so remember no null values to read them by this). */
    public V peek(long key) {
        Entry<V> entry = slots.get(slot(key));
        return entry != null && entry.key == key ? entry.value : null;
    }

    /** Remembers {@code value} for {@code key} unless its slot holds the key already (the value is pure); not null. */
    public void put(long key, V value) {
        if (value == null) {
            throw new IllegalArgumentException("KeyedSlots.put takes no null value");
        }
        int slot = slot(key);
        Entry<V> entry = slots.get(slot);
        if (entry == null || entry.key != key) {
            slots.set(slot, new Entry<>(key, value));
        }
    }

    private int slot(long key) {
        return (int) (mix(key) & mask);
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
