package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.Difficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageThresholdTest {
    @Test
    void noneAlwaysHurts() {
        for (Difficulty difficulty : Difficulty.values()) {
            assertEquals(2, DifficultyFloor.NONE.damage(2, 1, difficulty));
        }
    }

    @Test
    void vanillaStarvationFollowsTheDifficulty() {
        DifficultyFloor floor = DifficultyFloor.VANILLA_STARVATION;
        assertEquals(0, floor.damage(1, 20, Difficulty.PEACEFUL));
        assertEquals(1, floor.damage(1, 11, Difficulty.EASY));
        assertEquals(0, floor.damage(1, 10, Difficulty.EASY));
        assertEquals(1, floor.damage(1, 2, Difficulty.NORMAL));
        assertEquals(0, floor.damage(1, 1, Difficulty.NORMAL));
        assertEquals(1, floor.damage(1, 1, Difficulty.HARD));
    }

    @Test
    void neverLethalStopsAtOneHealth() {
        DifficultyFloor floor = DifficultyFloor.NEVER_LETHAL;
        assertEquals(2, floor.damage(2, 10, Difficulty.HARD));
        assertEquals(0.5f, floor.damage(2, 1.5f, Difficulty.HARD));
        assertEquals(0, floor.damage(2, 1, Difficulty.HARD));
    }

    @Test
    void intervalCountsWholeSeconds() {
        assertTrue(DamageThreshold.isDue(20, 1));
        assertTrue(DamageThreshold.isDue(80, 4));
        assertFalse(DamageThreshold.isDue(100, 4));
        assertTrue(DamageThreshold.isDue(160, 4));
    }

    @Test
    void fourSecondIntervalHitsOnceEveryFourHandlerTicks() {
        // Dehydration (like vanilla starving): the handler runs every 20 ticks; 40 seconds = 10 hits, 4 s apart.
        int hits = 0;
        int last = -1;
        for (int tick = 20; tick <= 800; tick += 20) {
            if (DamageThreshold.isDue(tick, 4)) {
                if (last >= 0) {
                    assertEquals(80, tick - last);
                }
                last = tick;
                hits++;
            }
        }
        assertEquals(10, hits);
    }
}
