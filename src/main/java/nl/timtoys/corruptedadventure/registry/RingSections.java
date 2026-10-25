package nl.timtoys.corruptedadventure.registry;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a register's entries into the ring sections of its source file, for the creative tab per ring. A section
 * starts where {@link #start} is called (a {@code static} block under the {@code // ---- Ring N ----} comment) and runs
 * to the next start, so the Nether ring N section that follows ring N belongs to ring N. Anything registered between two
 * starts lands in that ring, wherever in the section it stands.
 */
final class RingSections<T> {
    static final int RINGS = 5;

    private final DeferredRegister<T> register;
    private final int[] starts = new int[RINGS];
    private int started;

    RingSections(DeferredRegister<T> register) {
        this.register = register;
    }

    /** Ring {@code ring} starts with the next registered entry; rings start in order 1 to {@value #RINGS}. */
    void start(int ring) {
        if (ring != started + 1) {
            throw new IllegalStateException("ring " + ring + " starts after ring " + started);
        }
        starts[ring - 1] = register.getEntries().size();
        started = ring;
    }

    /** The entries of {@code ring} in registration order. */
    List<DeferredHolder<T, ? extends T>> entries(int ring) {
        if (started != RINGS) {
            throw new IllegalStateException("only " + started + " of " + RINGS + " ring sections started");
        }
        int from = starts[ring - 1];
        int to = ring == RINGS ? Integer.MAX_VALUE : starts[ring];
        List<DeferredHolder<T, ? extends T>> result = new ArrayList<>();
        int index = 0;
        for (DeferredHolder<T, ? extends T> entry : register.getEntries()) {
            if (index >= from && index < to) {
                result.add(entry);
            }
            index++;
        }
        return result;
    }
}
