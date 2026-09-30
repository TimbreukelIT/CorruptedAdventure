package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingFluidPickerTest {
    private static final Aquifer.FluidStatus SEA = new Aquifer.FluidStatus(63, Blocks.WATER.defaultBlockState());
    private static final RingFluidPicker PICKER = new RingFluidPicker(Level.OVERWORLD, (x, y, z) -> SEA);

    @Test
    void ringsKeepTheVanillaSea() {
        for (Ring ring : new Ring[]{Ring.RING_1, Ring.RING_4, Ring.RING_5}) {
            assertSame(SEA, PICKER.computeFluid(RingPosition.within(ring), 0, 40, 0));
        }
        assertSame(SEA, PICKER.computeFluid(new RingPosition(Ring.RING_4, Ring.RING_5, 0.9), 0, 40, 0));
    }

    @Test
    void voidEdgeAndItsTransitionHaveNoFluid() {
        RingPosition[] positions = {
                RingPosition.within(Ring.VOID),
                new RingPosition(Ring.RING_5, Ring.VOID, 0.0),
                new RingPosition(Ring.RING_5, Ring.VOID, 0.6)
        };
        for (RingPosition position : positions) {
            for (int y = -64; y < 64; y += 8) {
                assertTrue(PICKER.computeFluid(position, 0, y, 0).at(y).isAir(), "fluid at y " + y + " in " + position);
            }
        }
    }
}
