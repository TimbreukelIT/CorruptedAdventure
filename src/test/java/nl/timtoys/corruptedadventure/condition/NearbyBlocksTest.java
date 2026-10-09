package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearbyBlocksTest {
    private static final BlockPos FEET = new BlockPos(4750, 90, -12);

    @Test
    void theBoxCoversFeetAndHeadPlusTheRadiusOnce() {
        int[] offsets = NearbyBlocks.offsets(4);
        assertEquals(9 * 10 * 9 * 3, offsets.length);
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < offsets.length; i += 3) {
            assertTrue(Math.abs(offsets[i]) <= 4 && Math.abs(offsets[i + 2]) <= 4);
            assertTrue(offsets[i + 1] >= -4 && offsets[i + 1] <= 5);
            assertTrue(seen.add(BlockPos.asLong(offsets[i], offsets[i + 1], offsets[i + 2])));
        }
    }

    @Test
    void nearestFirst() {
        int[] offsets = NearbyBlocks.offsets(4);
        // The feet and head blocks first, the far corners last.
        Set<Integer> firstTwoDy = Set.of(offsets[1], offsets[4]);
        assertEquals(Set.of(0, 1), firstTwoDy);
        assertEquals(0, offsets[0]);
        assertEquals(0, offsets[2]);
        int last = offsets.length - 3;
        assertEquals(4, Math.abs(offsets[last]));
        assertEquals(4, Math.abs(offsets[last + 2]));
    }

    @Test
    void findsASourceAtTheEdgeOfTheBoxButNotBeyond() {
        assertTrue(NearbyBlocks.scan(FEET, 4, pos -> pos.equals(FEET.offset(4, 5, -4))));
        assertTrue(NearbyBlocks.scan(FEET, 4, pos -> pos.equals(FEET.offset(-4, -4, 4))));
        assertFalse(NearbyBlocks.scan(FEET, 4, pos -> pos.equals(FEET.offset(5, 0, 0))));
        assertFalse(NearbyBlocks.scan(FEET, 4, pos -> pos.equals(FEET.offset(0, 6, 0))));
        assertFalse(NearbyBlocks.scan(FEET, 4, pos -> pos.equals(FEET.offset(0, -5, 0))));
    }

    @Test
    void stopsAtTheFirstHit() {
        AtomicInteger lookups = new AtomicInteger();
        assertTrue(NearbyBlocks.scan(FEET, 4, pos -> {
            lookups.incrementAndGet();
            return pos.equals(FEET.below());
        }));
        assertTrue(lookups.get() <= 10, "lava under the feet is found within the first blocks, not after " + lookups.get());
        lookups.set(0);
        assertFalse(NearbyBlocks.scan(FEET, 4, pos -> lookups.incrementAndGet() < 0));
        assertEquals(810, lookups.get());
    }

    @Test
    void radiusOutsideTheConfigRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> NearbyBlocks.offsets(0));
        assertThrows(IllegalArgumentException.class, () -> NearbyBlocks.offsets(9));
        assertEquals(17 * 18 * 17 * 3, NearbyBlocks.offsets(8).length);
    }
}
