package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingAquiferTest {
    private static final DensityFunction.FunctionContext DEEP = new DensityFunction.SinglePointContext(9700, -40, 0);

    /** Like vanilla's aquifer under a floating island: local lava, with a fluid update scheduled. */
    private static final Aquifer LAVA_POCKETS = new Aquifer() {
        @Override
        public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
            return substance > 0.0 ? null : Blocks.LAVA.defaultBlockState();
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return true;
        }
    };

    private final RingAquifer aquifer = new RingAquifer(Level.OVERWORLD, LAVA_POCKETS);

    @Test
    void voidZoneStaysEmpty() {
        for (RingPosition position : new RingPosition[]{
                RingPosition.within(Ring.VOID), new RingPosition(Ring.RING_5, Ring.VOID, 0.2)}) {
            assertTrue(aquifer.computeSubstance(position, DEEP, -0.5).isAir(), "fluid in " + position);
            assertFalse(aquifer.shouldScheduleFluidUpdate());
        }
    }

    @Test
    void solidStaysSolidAndRingsKeepVanillaFluids() {
        assertNull(aquifer.computeSubstance(RingPosition.within(Ring.VOID), DEEP, 0.5));
        assertSame(Blocks.LAVA.defaultBlockState(), aquifer.computeSubstance(RingPosition.within(Ring.RING_5), DEEP, -0.5));
        assertTrue(aquifer.shouldScheduleFluidUpdate());
    }
}
