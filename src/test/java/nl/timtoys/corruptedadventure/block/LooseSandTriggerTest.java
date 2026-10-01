package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LooseSandTriggerTest {
    /** A standing player (0.6 × 1.8) with its feet on block (10, 64, -5). */
    private static final AABB PLAYER = new AABB(10.2, 64.0, -4.8, 10.8, 65.8, -4.2);

    @Test
    void scanBoxIsTheFootprintPlusOneAroundFromTheHeadUpToTwoAbove() {
        List<BlockPos> box = LooseSandTrigger.scanBox(PLAYER);
        Set<BlockPos> expected = new HashSet<>();
        for (int x = 9; x <= 11; x++) {
            for (int y = 65; y <= 67; y++) {
                for (int z = -6; z <= -4; z++) {
                    expected.add(new BlockPos(x, y, z));
                }
            }
        }
        assertEquals(27, box.size());
        assertEquals(expected, new HashSet<>(box));
    }

    @Test
    void sandThreeBlocksAboveTheHeadIsOutOfReach() {
        // Head top at 65.8; the block from 68 up has its bottom 2.2 blocks above it.
        List<BlockPos> box = LooseSandTrigger.scanBox(PLAYER);
        assertTrue(box.contains(new BlockPos(10, 67, -5)));
        assertFalse(box.contains(new BlockPos(10, 68, -5)));
        assertFalse(box.contains(new BlockPos(10, 64, -5)));
        assertFalse(box.contains(new BlockPos(12, 66, -5)));
    }

    @Test
    void scanBoxFollowsAWiderEntityAcrossBlockEdges() {
        // Straddling x = 0: the footprint covers blocks -1 and 0, plus one on each side.
        List<BlockPos> box = LooseSandTrigger.scanBox(new AABB(-0.3, 0.0, 0.2, 0.3, 1.8, 0.8));
        assertEquals(4 * 3 * 3, box.size());
        assertTrue(box.contains(new BlockPos(-2, 1, 0)));
        assertTrue(box.contains(new BlockPos(1, 3, 0)));
    }

    @Test
    void looseSandWithRoomBelowTriggers() {
        BlockState sand = ModBlocks.LOOSE_SAND.get().defaultBlockState();
        assertTrue(LooseSandTrigger.shouldTrigger(false, sand, Blocks.AIR.defaultBlockState(), false));
        assertTrue(LooseSandTrigger.shouldTrigger(false, sand, Blocks.WATER.defaultBlockState(), false));
    }

    @Test
    void spectatorsSupportedSandScheduledSandAndOtherBlocksDoNotTrigger() {
        BlockState sand = ModBlocks.LOOSE_SAND.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        assertFalse(LooseSandTrigger.shouldTrigger(true, sand, air, false));
        assertFalse(LooseSandTrigger.shouldTrigger(false, sand, Blocks.STONE.defaultBlockState(), false));
        assertFalse(LooseSandTrigger.shouldTrigger(false, sand, air, true));
        assertFalse(LooseSandTrigger.shouldTrigger(false, Blocks.SAND.defaultBlockState(), air, false));
    }
}
