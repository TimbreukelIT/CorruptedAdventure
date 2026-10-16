package nl.timtoys.corruptedadventure.block;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import nl.timtoys.corruptedadventure.block.SwampGasBurn.Outcome;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The chain budget of the swamp gas (Q9 = A): ≤ 64 burns and ≤ 4 explosions per tick, one explosion per ~8 blocks. */
class SwampGasBurnTest {
    @Test
    void aFreshFireStartsWithABangEvenForASmallBubble() {
        SwampGasBurn burn = new SwampGasBurn();
        assertEquals(Outcome.EXPLODE, burn.next(100));
        assertEquals(Outcome.FLAME, burn.next(100));
        assertEquals(Outcome.FLAME, burn.next(102));
        // After a quiet gap a new fire starts with a bang again.
        assertEquals(Outcome.EXPLODE, burn.next(102 + SwampGasBurn.QUIET_GAP + 1));
    }

    @Test
    void aSlowChainExplodesOncePerEightBlocksNotOncePerTick() {
        SwampGasBurn burn = new SwampGasBurn();
        List<Outcome> outcomes = new ArrayList<>();
        // A one-block-wide line: one burn every two ticks.
        for (int i = 0; i < 24; i++) {
            outcomes.add(burn.next(1000 + 2L * i));
        }
        assertEquals(3, outcomes.stream().filter(outcome -> outcome == Outcome.EXPLODE).count());
        assertEquals(Outcome.EXPLODE, outcomes.get(0));
        assertEquals(Outcome.EXPLODE, outcomes.get(8));
        assertEquals(Outcome.EXPLODE, outcomes.get(16));
    }

    @Test
    void aBigChamberIsCappedPerTick() {
        SwampGasBurn burn = new SwampGasBurn();
        List<Outcome> outcomes = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            outcomes.add(burn.next(5));
        }
        long burnt = outcomes.stream().filter(outcome -> outcome != Outcome.WAIT).count();
        assertEquals(SwampGasBurn.MAX_BURNS_PER_TICK, burnt);
        assertEquals(SwampGasBurn.MAX_EXPLOSIONS_PER_TICK, outcomes.stream().filter(outcome -> outcome == Outcome.EXPLODE).count());
        // The other cluster starts of that tick flare up (sound) instead of exploding.
        assertEquals(SwampGasBurn.MAX_BURNS_PER_TICK / SwampGasBurn.BLOCKS_PER_EXPLOSION - SwampGasBurn.MAX_EXPLOSIONS_PER_TICK,
                outcomes.stream().filter(outcome -> outcome == Outcome.FLARE).count());
        // The next tick has a fresh budget.
        assertTrue(burn.next(6) != Outcome.WAIT);
    }

    @Test
    void aGameTimeThatJumpedBackStartsANewFire() {
        SwampGasBurn burn = new SwampGasBurn();
        burn.next(500);
        burn.next(500);
        assertEquals(Outcome.EXPLODE, burn.next(10));
    }

    @Test
    void openFlameAndLavaIgniteTheGasButLitGasDoesNot() {
        assertTrue(SwampGasBlock.ignites(Blocks.FIRE.defaultBlockState()));
        assertTrue(SwampGasBlock.ignites(Blocks.SOUL_FIRE.defaultBlockState()));
        assertTrue(SwampGasBlock.ignites(Blocks.LAVA.defaultBlockState()));
        assertTrue(SwampGasBlock.ignites(Blocks.TORCH.defaultBlockState()));
        assertTrue(SwampGasBlock.ignites(Blocks.WALL_TORCH.defaultBlockState()));
        assertTrue(SwampGasBlock.ignites(Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)));
        assertFalse(SwampGasBlock.ignites(Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false)));
        assertFalse(SwampGasBlock.ignites(Blocks.REDSTONE_TORCH.defaultBlockState()));
        assertFalse(SwampGasBlock.ignites(Blocks.WATER.defaultBlockState()));
        assertFalse(SwampGasBlock.ignites(ModBlocks.SWAMP_GAS.get().defaultBlockState().setValue(BlockStateProperties.LIT, true)));
    }
}
