package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void waterOnlyInOverworldRing2AboveTheLavaLayer() {
        RingPosition ring2 = RingPosition.within(Ring.RING_2);
        assertTrue(RingFluidPicker.waterOnly(Level.OVERWORLD, ring2, -54));
        assertTrue(RingFluidPicker.waterOnly(Level.OVERWORLD, ring2, 62));
        assertFalse(RingFluidPicker.waterOnly(Level.OVERWORLD, ring2, -55));
        assertFalse(RingFluidPicker.waterOnly(Level.NETHER, ring2, 20));
        for (Ring ring : new Ring[]{Ring.RING_1, Ring.RING_3, Ring.RING_4, Ring.RING_5, Ring.VOID}) {
            assertFalse(RingFluidPicker.waterOnly(Level.OVERWORLD, RingPosition.within(ring), -20), ring.name());
        }
        assertTrue(RingFluidPicker.waterOnly(Level.OVERWORLD, new RingPosition(Ring.RING_1, Ring.RING_2, 0.5), -20));
        assertFalse(RingFluidPicker.waterOnly(Level.OVERWORLD, new RingPosition(Ring.RING_2, Ring.RING_3, 0.5), -20));
    }

    @Test
    void noFluidIsUnchangedByWaterOnly() {
        assertFalse(RingFluidPicker.noFluid(RingPosition.within(Ring.RING_2)));
        assertTrue(RingFluidPicker.noFluid(new RingPosition(Ring.RING_5, Ring.VOID, 0.1)));
    }

    @Test
    void fluidLevelPerRingFillsUpToAndIncludingItsY() {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(32, Blocks.LAVA.defaultBlockState());
        RingFluidPicker picker = new RingFluidPicker(Level.NETHER, (x, y, z) -> lava, Map.of(Ring.RING_2, 22));
        RingPosition ring2 = RingPosition.within(Ring.RING_2);
        assertTrue(picker.computeFluid(ring2, 0, 22, 0).at(22).is(Blocks.LAVA));
        assertTrue(picker.computeFluid(ring2, 0, 23, 0).at(23).isAir());
        assertTrue(picker.computeFluid(ring2, 0, 5, 0).at(5).is(Blocks.LAVA));
        // Other rings keep the dimension's level (lava up to Y 31).
        assertSame(lava, picker.computeFluid(RingPosition.within(Ring.RING_1), 0, 25, 0));
        assertSame(lava, picker.computeFluid(RingPosition.within(Ring.RING_3), 0, 25, 0));
    }

    @Test
    void fluidLevelSwitchesAtHalfWeightLikeTheBiome() {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(32, Blocks.LAVA.defaultBlockState());
        RingFluidPicker picker = new RingFluidPicker(Level.NETHER, (x, y, z) -> lava, Map.of(Ring.RING_2, 22));
        assertTrue(picker.computeFluid(new RingPosition(Ring.RING_1, Ring.RING_2, 0.49), 0, 28, 0).at(28).is(Blocks.LAVA));
        assertTrue(picker.computeFluid(new RingPosition(Ring.RING_1, Ring.RING_2, 0.51), 0, 28, 0).at(28).isAir());
        assertTrue(picker.computeFluid(new RingPosition(Ring.RING_2, Ring.RING_3, 0.49), 0, 28, 0).at(28).isAir());
        assertTrue(picker.computeFluid(new RingPosition(Ring.RING_2, Ring.RING_3, 0.51), 0, 28, 0).at(28).is(Blocks.LAVA));
    }

    @Test
    void voidStillWinsOverAFluidLevel() {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(32, Blocks.LAVA.defaultBlockState());
        RingFluidPicker picker = new RingFluidPicker(Level.NETHER, (x, y, z) -> lava, Map.of(Ring.RING_5, 22));
        for (int y = 0; y < 40; y += 8) {
            assertTrue(picker.computeFluid(new RingPosition(Ring.RING_5, Ring.VOID, 0.7), 0, y, 0).at(y).isAir());
        }
    }
}
