package nl.timtoys.corruptedadventure.entity.unfinished;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnfinishedArrivalMathTest {
    @Test
    void theGripsComeFasterTowardTheEnd() {
        int span = 480;
        assertEquals(0.0, UnfinishedArrival.grips(0, span));
        // One per 50 ticks at the start, one per 12 at the end: the mean rate over the span.
        assertEquals(span * (1.0 / 50.0 + 1.0 / 12.0) / 2.0, UnfinishedArrival.grips(span, span), 1e-9);
        double early = UnfinishedArrival.grips(50, span) - UnfinishedArrival.grips(0, span);
        double late = UnfinishedArrival.grips(span, span) - UnfinishedArrival.grips(span - 50, span);
        assertTrue(late > early * 3.0, "late " + late + ", early " + early);
    }

    @Test
    void aThreeByFiveLensIsAFullThreeByFive() {
        int count = 0;
        for (int across = -3; across <= 3; across++) {
            for (int along = -2; along <= 2; along++) {
                if (UnfinishedTear.inLens(across, along, 5, 3)) {
                    count++;
                    assertTrue(Math.abs(across) <= 2 && Math.abs(along) <= 1, across + ", " + along);
                }
            }
        }
        assertEquals(15, count);
    }

    @Test
    void theTearIsLongAcrossHisWay() {
        assertTrue(UnfinishedTear.inLens(4, 0, 9, 5));
        assertFalse(UnfinishedTear.inLens(0, 4, 9, 5));
        assertFalse(UnfinishedTear.inLens(4, 2, 9, 5));
    }
}
