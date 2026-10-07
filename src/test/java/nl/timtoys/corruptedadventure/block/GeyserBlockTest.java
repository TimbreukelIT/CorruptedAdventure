package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The geyser's eruption: the column above it, who is thrown and the push. The cycle itself (placing, repeating) is S. */
class GeyserBlockTest {
    @Test
    void columnIsTheReachBlocksStraightAboveTheGeyser() {
        AABB column = GeyserBlock.column(new BlockPos(3, 64, -7));
        assertEquals(new AABB(3, 65, -7, 4, 65 + GeyserBlock.REACH, -6), column);
    }

    @Test
    void launchSetsAtLeastTheLaunchSpeedUpAndKeepsHorizontalMotion() {
        assertEquals(new Vec3(0.2, GeyserBlock.LAUNCH_SPEED, -0.1), GeyserBlock.launched(new Vec3(0.2, -0.5, -0.1)));
        // Something already rising faster keeps its speed.
        assertEquals(new Vec3(0.0, 1.5, 0.0), GeyserBlock.launched(new Vec3(0.0, 1.5, 0.0)));
    }

    /** Only an entity with its centre over the geyser is thrown, not one at the edge of the next block. */
    @Test
    void onlyEntitiesCentredOverTheVentAreLaunched() {
        BlockPos geyser = new BlockPos(3, 64, -7);
        assertTrue(GeyserBlock.overVent(geyser, new Vec3(3.5, 65.0, -6.5)));
        assertTrue(GeyserBlock.overVent(geyser, new Vec3(3.01, 66.0, -6.99)));
        assertFalse(GeyserBlock.overVent(geyser, new Vec3(4.2, 65.0, -6.5)));
        assertFalse(GeyserBlock.overVent(geyser, new Vec3(3.5, 65.0, -7.2)));
    }
}
