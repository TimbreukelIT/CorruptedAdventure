package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the package 76 hazards: pools and gas only in closed hollows, mud patches small and in the chunk. */
class Ring4BlightedFeaturesTest {
    private static final BlockPos TOP = new BlockPos(8, 70, 8);

    /** A pool is one deep at the rim and deeper in the middle, never wider than its radius allows. */
    @Test
    void aPoolIsABowl() {
        Set<BlockPos> pool = ToxicPoolFeature.shape(TOP, 3, 2);
        assertTrue(pool.contains(TOP) && pool.contains(TOP.below()), "deep in the middle");
        assertTrue(pool.contains(TOP.east(3)) && !pool.contains(TOP.east(3).below()), "one deep at the rim");
        for (BlockPos pos : pool) {
            assertTrue(pos.getY() == TOP.getY() || pos.getY() == TOP.getY() - 1);
            assertTrue(Math.abs(pos.getX() - TOP.getX()) <= ToxicPoolFeature.MAX_RADIUS);
        }
    }

    /** Contract §3: sludge only in closed basins, never against air or water beside or below it. */
    @Test
    void aPoolOnlyFillsAClosedBowl() {
        Set<BlockPos> pool = ToxicPoolFeature.shape(TOP, 2, 2);
        Predicate<BlockPos> solidGround = pos -> pos.getY() <= TOP.getY();
        assertTrue(ToxicPoolFeature.closed(pool, solidGround), "level ground all round");
        // A slope: one rim block beside the pool is open (air or water).
        BlockPos gap = TOP.east(3);
        assertFalse(ToxicPoolFeature.closed(pool, pos -> solidGround.test(pos) && !pos.equals(gap)));
        // A cave under the middle.
        BlockPos cave = TOP.below(2);
        assertFalse(ToxicPoolFeature.closed(pool, pos -> solidGround.test(pos) && !pos.equals(cave)));
    }

    /** Q10 = A: a gas bubble is closed on all six sides (it never meets air, water or a cave). */
    @Test
    void aGasPocketIsClosedOnAllSides() {
        Set<BlockPos> pocket = GasPocketFeature.box(4, 60, 4, 3, 2, 4);
        assertEquals(3 * 2 * 4, pocket.size());
        Predicate<BlockPos> rock = pos -> true;
        assertTrue(GasPocketFeature.closed(pocket, rock));
        BlockPos above = new BlockPos(5, 62, 5);
        assertFalse(GasPocketFeature.closed(pocket, pos -> !pos.equals(above)), "an open crust lets the gas out");
    }

    /** G1 = A: a patch is at most 4 × 4 and lies in the origin's chunk. */
    @Test
    void aMudPatchStaysSmallAndInItsChunk() {
        for (int size = 1; size <= SuckingMudPatchFeature.MAX_WIDTH; size++) {
            for (int centre = -40; centre <= 40; centre++) {
                int start = SuckingMudPatchFeature.start(centre, size);
                assertEquals(centre >> 4, start >> 4, "start " + start + " for " + centre);
                assertEquals(centre >> 4, (start + size - 1) >> 4, "end for " + centre);
                assertTrue(Math.abs(start + size / 2 - centre) < SuckingMudPatchFeature.MAX_WIDTH, "near the origin");
            }
        }
        assertEquals(4, SuckingMudPatchFeature.MAX_WIDTH);
        // A gas pocket keeps one block from the chunk's edges: all it reads round it is its own chunk.
        for (int size = 1; size <= GasPocketFeature.MAX_WIDTH; size++) {
            for (int centre = -40; centre <= 40; centre++) {
                int start = SuckingMudPatchFeature.start(centre, size, 1);
                assertTrue((start & 15) >= 1 && ((start + size - 1) & 15) <= 14 && start >> 4 == centre >> 4, "pocket " + start + " for " + centre);
            }
        }
    }
}
