package nl.timtoys.corruptedadventure.command.terrainmap;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnScanTest {
    @Test
    void topFindsTheExactSurfaceBetweenCoarseSamples() {
        for (int surface = -64; surface <= 200; surface += 7) {
            int top = surface;
            assertEquals(top, ColumnScan.top(y -> y <= top, -64, 319, 8));
        }
    }

    @Test
    void topSeesAFloatingIslandAtLeastAStepThick() {
        ColumnScan.Solid island = y -> y <= 70 || (y >= 200 && y <= 207);
        assertEquals(207, ColumnScan.top(island, -64, 319, 8));
    }

    @Test
    void topIsBelowTheRangeForAnEmptyColumnAndTheTopForAFullOne() {
        assertEquals(-65, ColumnScan.top(y -> false, -64, 319, 8));
        assertEquals(319, ColumnScan.top(y -> true, -64, 319, 8));
    }

    @Test
    void topStopsAtTheFirstSolidSample() {
        AtomicInteger calls = new AtomicInteger();
        ColumnScan.top(y -> {
            calls.incrementAndGet();
            return y <= 300;
        }, -64, 319, 8);
        assertTrue(calls.get() <= 3 + 8, "calls " + calls.get());
    }

    @Test
    void floorBelowACeilingIsTheFloorOfTheTallestOpening() {
        // Roof 100-127 with a pocket at 110-113, main cavern 40-99 with its floor at 39.
        ColumnScan.Solid nether = y -> y <= 39 || (y >= 100 && y < 110) || y >= 114;
        assertEquals(39, ColumnScan.floorOfTallestOpening(nether, 0, 127, 4));
    }

    @Test
    void floorIsTheTopForASolidColumnAndBelowTheRangeForAnOpenBottom() {
        assertEquals(127, ColumnScan.floorOfTallestOpening(y -> true, 0, 127, 4));
        assertEquals(-1, ColumnScan.floorOfTallestOpening(y -> y >= 100, 0, 127, 4));
    }

    @Test
    void floorIsExactForEveryHeight() {
        for (int floor = 1; floor < 90; floor++) {
            int f = floor;
            assertEquals(f, ColumnScan.floorOfTallestOpening(y -> y <= f || y >= 100, 0, 127, 4), "floor " + f);
        }
    }
}
