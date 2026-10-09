package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Package 55b: a wet sponge dries when placed, but not when it just soaked up water (it replaces a dry sponge), so a
 * sponge beside covered water soaks up once instead of pumping it all away. The exposure itself needs a server (W).
 */
class SpongeDryingTest {
    @Test
    void aPlacedWetSpongeDries() {
        assertTrue(SpongeDrying.driesOnPlace(Blocks.AIR.defaultBlockState()));
        assertTrue(SpongeDrying.driesOnPlace(Blocks.WATER.defaultBlockState()));
        assertTrue(SpongeDrying.driesOnPlace(Blocks.GRASS_BLOCK.defaultBlockState()));
    }

    @Test
    void aSpongeThatJustSoakedUpWaterStaysWet() {
        assertFalse(SpongeDrying.driesOnPlace(Blocks.SPONGE.defaultBlockState()));
    }
}
