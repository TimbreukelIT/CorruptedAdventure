package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlumeBillowTest {
    private static PlumeBillow billow(long seed, double rise) {
        return new PlumeBillow(RandomSource.create(seed), 0.0, 200.0, 0.0, null, 4.0F, rise, 0.01, -0.01, 0.0F);
    }

    @Test
    void aBillowRisesAsAColumnThatSlowsAtTheTop() {
        PlumeBillow billow = billow(1L, 0.4);
        double previousStep = Double.MAX_VALUE;
        double previous = billow.y();
        while (billow.alive()) {
            billow.tick();
            if (!billow.alive()) {
                break;
            }
            double step = billow.y() - previous;
            assertTrue(step > 0.0 && step <= previousStep + 1e-9, "keeps rising, ever slower");
            previousStep = step;
            previous = billow.y();
        }
        // About 70-90 blocks for an ordinary cone (rise 0.4, decay 0.996), never a flat bank of a few blocks.
        assertTrue(previous - 200.0 > 60.0 && previous - 200.0 < 100.0, "rose " + (previous - 200.0));
    }

    @Test
    void theWindDriftStaysBounded() {
        PlumeBillow billow = billow(2L, 0.55);
        while (billow.alive()) {
            billow.tick();
            assertTrue(billow.horizontalSpeed() <= PlumeBillow.MAX_DRIFT * Math.sqrt(2.0) + 1e-9);
        }
        assertTrue(billow.horizontalDistance(new Vec3(0.0, 0.0, 0.0)) < 25.0);
    }

    @Test
    void prewarmAgesTheBillowButLeavesItAlive() {
        PlumeBillow fresh = billow(3L, 0.4);
        fresh.prewarm(0.0F);
        assertEquals(0, fresh.age());
        PlumeBillow old = billow(3L, 0.4);
        old.prewarm(0.9999F);
        assertTrue(old.alive());
        assertTrue(old.age() > 300);
        PlumeBillow half = billow(3L, 0.4);
        half.prewarm(0.5F);
        PlumeBillow ticked = billow(3L, 0.4);
        for (int i = 0; i < half.age(); i++) {
            ticked.tick();
        }
        assertEquals(ticked.y(), half.y(), 1e-9);
    }

    @Test
    void alphaFadesInFastAndOutSlowly() {
        assertEquals(0.0F, PlumeBillow.alphaAt(0.0F));
        assertEquals(0.85F, PlumeBillow.alphaAt(0.3F));
        assertEquals(0.0F, PlumeBillow.alphaAt(1.0F));
        assertTrue(PlumeBillow.alphaAt(0.8F) > 0.0F && PlumeBillow.alphaAt(0.8F) < 0.85F);
    }
}
