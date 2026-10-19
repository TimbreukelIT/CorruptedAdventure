package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ring 4 decor proef: the ground objects stay in their own chunk and draw from their own random. */
class GroundObjectsTest {
    @Test
    void fitKeepsTheReachInTheChunk() {
        for (int x = -40; x < 40; x++) {
            for (int reach = 0; reach <= 4; reach++) {
                BlockPos fitted = GroundObjects.fit(new BlockPos(x, 70, 15 - x), reach);
                assertEquals(x >> 4, (fitted.getX() - reach) >> 4);
                assertEquals(x >> 4, (fitted.getX() + reach) >> 4);
                assertEquals((15 - x) >> 4, (fitted.getZ() - reach) >> 4);
                assertEquals((15 - x) >> 4, (fitted.getZ() + reach) >> 4);
                if ((x & 15) >= reach && (x & 15) <= 15 - reach) {
                    assertEquals(x, fitted.getX(), "an origin that fits stays");
                }
            }
        }
    }

    @Test
    void aFallenTrunkStaysInItsChunkAlongItsDirection() {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int x = -20; x < 20; x++) {
                for (int reach = 0; reach <= 9; reach++) {
                    BlockPos origin = new BlockPos(x, 70, 37);
                    BlockPos moved = FallenTreeFeature.inChunk(origin, direction, reach);
                    BlockPos end = moved.relative(direction, reach);
                    assertEquals(origin.getX() >> 4, moved.getX() >> 4);
                    assertEquals(origin.getX() >> 4, end.getX() >> 4, direction + " " + x + " " + reach);
                    assertEquals(origin.getZ() >> 4, end.getZ() >> 4, direction + " " + x + " " + reach);
                }
            }
        }
    }

    @Test
    void ownRandomByPositionAndSeed() {
        BlockPos pos = new BlockPos(100, 70, -200);
        assertEquals(GroundObjects.random(5L, pos, 1L).nextLong(), GroundObjects.random(5L, pos, 1L).nextLong());
        assertTrue(GroundObjects.random(5L, pos, 1L).nextLong() != GroundObjects.random(6L, pos, 1L).nextLong());
        assertTrue(GroundObjects.random(5L, pos, 1L).nextLong() != GroundObjects.random(5L, pos.east(), 1L).nextLong());
        assertTrue(GroundObjects.random(5L, pos, 1L).nextLong() != GroundObjects.random(5L, pos, 2L).nextLong());
    }
}
