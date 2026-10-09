package nl.timtoys.corruptedadventure.condition;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ice rule of package 55b: in the heat zone, in the sun or near a lit heat source. */
class IceMeltingTest {
    @Test
    void meltsInTheZoneInTheSunOrNearASource() {
        assertTrue(IceMelting.melts(true, () -> true, () -> false));
        assertTrue(IceMelting.melts(true, () -> false, () -> true));
        assertFalse(IceMelting.melts(true, () -> false, () -> false));
    }

    @Test
    void outsideTheZoneNothingIsLookedUp() {
        AtomicInteger lookups = new AtomicInteger();
        assertFalse(IceMelting.melts(false, counting(lookups, true), counting(lookups, true)));
        assertEquals(0, lookups.get());
    }

    /** The block scan is the expensive lookup: not needed when the sun already melts the ice. */
    @Test
    void theSunSkipsTheBlockScan() {
        AtomicInteger scans = new AtomicInteger();
        assertTrue(IceMelting.melts(true, () -> true, counting(scans, false)));
        assertEquals(0, scans.get());
    }

    private static BooleanSupplier counting(AtomicInteger counter, boolean value) {
        return () -> {
            counter.incrementAndGet();
            return value;
        };
    }
}
