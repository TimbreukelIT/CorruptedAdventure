package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import java.util.function.Supplier;

/**
 * A cache object for the thread that uses a density function, without a {@link ThreadLocal}. The noise chunk makes a
 * fresh copy of every density function for each chunk ({@code mapAll}); a {@code ThreadLocal} per copy would leave
 * thousands of dead entries in the worker threads' thread-local maps, and every {@code ThreadLocal.get} in worldgen
 * (also vanilla's and {@code RingColumnCache}'s) slows down with them. A copy is used by one thread at a time (a chunk
 * moves from status to status), so the object is simply replaced when another thread asks for it.
 * <p>
 * Safe for concurrent use: a thread only ever gets an object made for itself (the owner is a final field of the
 * holder), at worst a new one.
 */
public final class ThreadOwned<T> {
    private final Supplier<T> factory;
    private Holder<T> current;

    public ThreadOwned(Supplier<T> factory) {
        this.factory = factory;
    }

    public T get() {
        Holder<T> holder = current;
        Thread thread = Thread.currentThread();
        if (holder == null || holder.thread != thread) {
            holder = new Holder<>(thread, factory.get());
            current = holder;
        }
        return holder.value;
    }

    private record Holder<T>(Thread thread, T value) {
    }
}
