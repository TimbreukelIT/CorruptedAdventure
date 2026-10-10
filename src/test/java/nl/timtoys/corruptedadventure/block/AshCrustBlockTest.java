package nl.timtoys.corruptedadventure.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 38c: ash crust never schedules its own fall. Placing and updating get no level ({@code null}): the vanilla falling
 * block would schedule a tick there and throw, the crust must not touch the level at all.
 */
class AshCrustBlockTest {
    private static final BlockPos POS = new BlockPos(3, 70, -2);

    @Test
    void aNeighbourUpdateLeavesTheCrustHanging() {
        AshCrustBlock crust = ModBlocks.ASH_CRUST.get();
        BlockState state = crust.defaultBlockState();
        for (Direction direction : Direction.values()) {
            assertSame(state, crust.updateShape(state, direction, Blocks.AIR.defaultBlockState(), null, POS,
                    POS.relative(direction)));
        }
    }

    @Test
    void placingTheCrustSchedulesNothing() {
        AshCrustBlock crust = ModBlocks.ASH_CRUST.get();
        assertDoesNotThrow(() -> crust.onPlace(crust.defaultBlockState(), null, POS, Blocks.AIR.defaultBlockState(), false));
        assertDoesNotThrow(() -> crust.onPlace(crust.defaultBlockState(), null, POS, Blocks.AIR.defaultBlockState(), true));
    }

    @Test
    void itNeverRandomTicksAndKeepsTheAshDust() {
        BlockState state = ModBlocks.ASH_CRUST.get().defaultBlockState();
        assertFalse(state.isRandomlyTicking());
        assertEquals(0x8A8580, ModBlocks.ASH_CRUST.get().getDustColor(state, EmptyBlockGetter.INSTANCE, POS));
        // 38c review (user): a speck of dust now and then, ten times rarer than sand (1 in 16).
        assertEquals(160, AshCrustBlock.DUST_CHANCE);
    }
}
