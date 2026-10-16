package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyedSlotsTest {
    @Test
    void peekSeesOnlyWhatWasPutForThatKey() {
        KeyedSlots<Double> slots = new KeyedSlots<>(1 << 10);
        long key = KeyedSlots.key(17, -42);
        assertNull(slots.peek(key));
        slots.put(key, 3.5);
        assertEquals(3.5, slots.peek(key));
        assertNull(slots.peek(KeyedSlots.key(-42, 17)));
        // A value is pure: a second put of the key keeps the first.
        slots.put(key, 3.5);
        assertEquals(3.5, slots.peek(key));
        assertThrows(IllegalArgumentException.class, () -> slots.put(KeyedSlots.key(1, 1), null));
    }

    @Test
    void getRemembersANullValueToo() {
        KeyedSlots<String> slots = new KeyedSlots<>();
        AtomicInteger calls = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            assertNull(slots.get(5L, key -> {
                calls.incrementAndGet();
                return null;
            }));
        }
        assertEquals(1, calls.get());
    }

    @Test
    void getComputesOnceWhileTheSlotHoldsTheKey() {
        KeyedSlots<String> slots = new KeyedSlots<>();
        AtomicInteger calls = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            assertEquals("7", slots.get(7L, key -> {
                calls.incrementAndGet();
                return Long.toString(key);
            }));
        }
        assertEquals(1, calls.get());
    }

    @Test
    void slotCountMustBeAPowerOfTwo() {
        assertThrows(IllegalArgumentException.class, () -> new KeyedSlots<>(100));
        assertThrows(IllegalArgumentException.class, () -> new KeyedSlots<>(0));
    }
}
