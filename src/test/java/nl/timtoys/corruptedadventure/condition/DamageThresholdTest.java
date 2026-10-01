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
}
