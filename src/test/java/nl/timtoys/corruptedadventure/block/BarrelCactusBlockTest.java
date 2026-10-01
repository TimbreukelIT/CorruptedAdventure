package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarrelCactusBlockTest {
    private static final BlockPos CACTUS = new BlockPos(4, 70, 4);

    /** A 0.6-wide entity whose box ends at {@code maxX}, standing on the cactus's floor. */
    private static AABB entityEndingAt(double maxX) {
        return new AABB(maxX - 0.6, 70.0, 4.2, maxX, 71.8, 4.8);
    }

    @Test
    void entityPushedAgainstTheSideTouches() {
        // Collision stops the entity exactly at the shape's west face (x = 4 + 3/16).
        assertTrue(BarrelCactusBlock.touches(entityEndingAt(4.0 + 3.0 / 16.0), CACTUS));
    }

    @Test
    void entityInTheSameCellWithoutContactIsSafe() {
        // Inside the block cell (vanilla still calls entityInside), but 0.1 short of the spines.
        assertFalse(BarrelCactusBlock.touches(entityEndingAt(4.0 + 3.0 / 16.0 - 0.1), CACTUS));
    }

    @Test
    void entityStandingOnTopTouches() {
        AABB onTop = new AABB(4.2, 70.0 + 10.0 / 16.0, 4.2, 4.8, 72.4, 4.8);
        assertTrue(BarrelCactusBlock.touches(onTop, CACTUS));
    }
}
