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

    /** Like vanilla's aquifer in a dry cave: open space without fluid, no update. */
    private static final Aquifer DRY = new Aquifer() {
        @Override
        public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
            return substance > 0.0 ? null : Blocks.AIR.defaultBlockState();
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return false;
        }
    };

    /** Vanilla lava without a scheduled update (a cell in the middle of a lava pocket). */
    private static final Aquifer STILL_LAVA = new Aquifer() {
        @Override
        public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
            return substance > 0.0 ? null : Blocks.LAVA.defaultBlockState();
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return false;
        }
    };

    /** Vanilla water with a scheduled update (a cell at a border). */
    private static final Aquifer WATER_WITH_UPDATE = new Aquifer() {
        @Override
        public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
            return substance > 0.0 ? null : Blocks.WATER.defaultBlockState();
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return true;
        }
    };

    private final RingAquifer aquifer = new RingAquifer(Level.OVERWORLD, LAVA_POCKETS);

    private static DensityFunction.FunctionContext at(int y) {
        return new DensityFunction.SinglePointContext(2500, y, 0);
    }

    @Test
    void voidZoneStaysEmpty() {
        for (RingPosition position : new RingPosition[]{
                RingPosition.within(Ring.VOID), new RingPosition(Ring.RING_5, Ring.VOID, 0.2)}) {
            assertTrue(aquifer.computeSubstance(position, DEEP, -0.5).isAir(), "fluid in " + position);
            assertFalse(aquifer.shouldScheduleFluidUpdate());
        }
    }

    @Test
    void ring2LavaAboveTheLavaLayerBecomesWaterWithVanillasUpdateFlag() {
        RingAquifer still = new RingAquifer(Level.OVERWORLD, STILL_LAVA);
        for (int y : new int[]{-53, -40, -10, 30}) {
            assertSame(Blocks.WATER.defaultBlockState(), aquifer.computeSubstance(RingPosition.within(Ring.RING_2), at(y), -0.5), "y " + y);
            assertTrue(aquifer.shouldScheduleFluidUpdate(), "border update kept at y " + y);
            assertSame(Blocks.WATER.defaultBlockState(), still.computeSubstance(RingPosition.within(Ring.RING_2), at(y), -0.5), "y " + y);
            assertFalse(still.shouldScheduleFluidUpdate(), "no update inside a pocket at y " + y);
        }
    }

    @Test
    void ring2LeavesStoneBetweenAquiferFluidAndTheLavaLayer() {
        assertNull(aquifer.computeSubstance(RingPosition.within(Ring.RING_2), at(RingFluidPicker.LAVA_LAYER_TOP), -0.5));
        assertFalse(aquifer.shouldScheduleFluidUpdate());
        // Vanilla water right above the layer too (vanilla would let it touch the lava and crust over).
        RingAquifer water = new RingAquifer(Level.OVERWORLD, WATER_WITH_UPDATE);
        assertNull(water.computeSubstance(RingPosition.within(Ring.RING_2), at(RingFluidPicker.LAVA_LAYER_TOP), -0.5));
        assertFalse(water.shouldScheduleFluidUpdate());
        assertSame(Blocks.WATER.defaultBlockState(), water.computeSubstance(RingPosition.within(Ring.RING_2), at(-53), -0.5));
        assertTrue(water.shouldScheduleFluidUpdate());
        // Ring 1 keeps vanilla's water there.
        assertSame(Blocks.WATER.defaultBlockState(), water.computeSubstance(RingPosition.within(Ring.RING_1), at(RingFluidPicker.LAVA_LAYER_TOP), -0.5));
    }

    @Test
    void ring2KeepsTheLavaLayer() {
        RingAquifer still = new RingAquifer(Level.OVERWORLD, STILL_LAVA);
        for (int y : new int[]{-55, -60, -64}) {
            assertSame(Blocks.LAVA.defaultBlockState(), still.computeSubstance(RingPosition.within(Ring.RING_2), at(y), -0.5), "y " + y);
            assertFalse(still.shouldScheduleFluidUpdate());
        }
    }

    @Test
    void otherRingsAndTheNetherKeepLava() {
        RingAquifer still = new RingAquifer(Level.OVERWORLD, STILL_LAVA);
        for (Ring ring : new Ring[]{Ring.RING_1, Ring.RING_3, Ring.RING_4, Ring.RING_5}) {
            assertSame(Blocks.LAVA.defaultBlockState(), still.computeSubstance(RingPosition.within(ring), at(-20), -0.5), ring.name());
            assertFalse(still.shouldScheduleFluidUpdate());
        }
        RingAquifer nether = new RingAquifer(Level.NETHER, STILL_LAVA);
        assertSame(Blocks.LAVA.defaultBlockState(), nether.computeSubstance(RingPosition.within(Ring.RING_2), at(20), -0.5));
        assertFalse(nether.shouldScheduleFluidUpdate());
    }

    @Test
    void transitionZoneSwitchesOnTheRing() {
        RingAquifer still = new RingAquifer(Level.OVERWORLD, STILL_LAVA);
        assertSame(Blocks.LAVA.defaultBlockState(), still.computeSubstance(new RingPosition(Ring.RING_1, Ring.RING_2, 0.49), at(-20), -0.5));
        assertSame(Blocks.WATER.defaultBlockState(), still.computeSubstance(new RingPosition(Ring.RING_1, Ring.RING_2, 0.51), at(-20), -0.5));
        assertSame(Blocks.WATER.defaultBlockState(), still.computeSubstance(new RingPosition(Ring.RING_2, Ring.RING_3, 0.49), at(-20), -0.5));
        assertSame(Blocks.LAVA.defaultBlockState(), still.computeSubstance(new RingPosition(Ring.RING_2, Ring.RING_3, 0.51), at(-20), -0.5));
    }

    @Test
    void ring2LeavesSolidAirAndUpdatesAlone() {
        RingAquifer dry = new RingAquifer(Level.OVERWORLD, DRY);
        assertNull(dry.computeSubstance(RingPosition.within(Ring.RING_2), at(-20), 0.5));
        assertTrue(dry.computeSubstance(RingPosition.within(Ring.RING_2), at(-20), -0.5).isAir());
        assertFalse(dry.shouldScheduleFluidUpdate());
        // The lid above the lava layer does not leave its state on the next call.
        aquifer.computeSubstance(RingPosition.within(Ring.RING_2), at(RingFluidPicker.LAVA_LAYER_TOP), -0.5);
        assertSame(Blocks.WATER.defaultBlockState(), aquifer.computeSubstance(RingPosition.within(Ring.RING_2), at(-20), -0.5));
        assertTrue(aquifer.shouldScheduleFluidUpdate());
    }

    @Test
    void solidStaysSolidAndRingsKeepVanillaFluids() {
        assertNull(aquifer.computeSubstance(RingPosition.within(Ring.VOID), DEEP, 0.5));
        assertSame(Blocks.LAVA.defaultBlockState(), aquifer.computeSubstance(RingPosition.within(Ring.RING_5), DEEP, -0.5));
        assertTrue(aquifer.shouldScheduleFluidUpdate());
    }
}
