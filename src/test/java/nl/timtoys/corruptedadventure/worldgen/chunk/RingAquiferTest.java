package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicFieldTest;
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
        // The ring 3 half of the zone 2→3 is sealed below ring 2's sea level (no water wall at the middle line).
        assertNull(still.computeSubstance(new RingPosition(Ring.RING_2, Ring.RING_3, 0.51), at(-20), -0.5));
        assertSame(Blocks.LAVA.defaultBlockState(), still.computeSubstance(RingPosition.within(Ring.RING_3), at(-20), -0.5));
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

    // ---------------------------------------------------------------- overworld ring 3 (volcanic field)

    private static final VolcanicField FIELD = VolcanicFieldTest.field();

    /** A lava column of the field in the given area whose 4 neighbours hold the same lava top (inside a lake or bed). */
    private static int[] innerLavaColumn(int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x += 2) {
            for (int z = z0; z <= z1; z += 2) {
                int top = FIELD.column(x, z).lavaTop();
                if (top != Integer.MIN_VALUE && FIELD.column(x, z).height() < top - 1
                        && FIELD.column(x + 1, z).lavaTop() == top && FIELD.column(x - 1, z).lavaTop() == top
                        && FIELD.column(x, z + 1).lavaTop() == top && FIELD.column(x, z - 1).lavaTop() == top) {
                    return new int[]{x, top, z};
                }
            }
        }
        throw new AssertionError("no lava in the area");
    }

    @Test
    void ring3LakesAndChannelsHoldStillLava() {
        RingAquifer ring3 = new RingAquifer(Level.OVERWORLD, DRY, FIELD);
        int[] lava = innerLavaColumn(-5200, -2700, -4400, -2000);
        RingPosition inRing3 = RingPosition.within(Ring.RING_3);
        assertSame(Blocks.LAVA.defaultBlockState(),
                ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(lava[0], lava[1] - 1, lava[2]), -0.5));
        assertFalse(ring3.shouldScheduleFluidUpdate(), "still lava");
        // Rock stays rock, the air above the lava stays vanilla's, and the rule is ring 3's alone.
        assertNull(ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(lava[0], lava[1] - 1, lava[2]), 0.5));
        assertSame(Blocks.AIR.defaultBlockState(),
                ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(lava[0], lava[1], lava[2]), -0.5));
        assertSame(Blocks.AIR.defaultBlockState(), ring3.computeSubstance(RingPosition.within(Ring.RING_4),
                new DensityFunction.SinglePointContext(lava[0], lava[1] - 1, lava[2]), -0.5));
    }

    @Test
    void ring3LavaBesideOpenAirBecomesCrust() {
        RingAquifer ring3 = new RingAquifer(Level.OVERWORLD, DRY, FIELD);
        RingPosition inRing3 = RingPosition.within(Ring.RING_3);
        int crusts = 0;
        for (int x = -6000; x <= 6000 && crusts == 0; x++) {
            for (int z = -6000; z <= 6000 && crusts == 0; z += 89) {
                VolcanicField.Column c = FIELD.column(x, z);
                if (c.lavaTop() == Integer.MIN_VALUE) {
                    continue;
                }
                int y = c.lavaTop() - 1;
                VolcanicField.Column side = FIELD.column(x + 1, z);
                // The rule of RingAquifer.open: the field's height less the interpolation and crag margins.
                if (side.lavaTop() == Integer.MIN_VALUE && side.height() - 1.5 - 4.0 * side.roughness() < y + 1.0) {
                    assertNull(ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(x, y, z), -0.5), x + "," + z);
                    assertFalse(ring3.shouldScheduleFluidUpdate());
                    crusts++;
                }
            }
        }
        assertTrue(crusts > 0, "no lava edge in the sample");
    }

    @Test
    void ring3RiverStepsStayLavaFalls() {
        // 39d: lava beside a neighbour that holds lower lava itself (one step down a river) stays lava: a small lava fall,
        // no lip of crust across the river.
        RingAquifer ring3 = new RingAquifer(Level.OVERWORLD, DRY, FIELD);
        RingPosition inRing3 = RingPosition.within(Ring.RING_3);
        int steps = 0;
        for (int x = -6000; x <= 6000 && steps < 3; x += 2) {
            for (int z = -6000; z <= 6000 && steps < 3; z += 97) {
                VolcanicField.Column c = FIELD.column(x, z);
                if (c.lavaTop() == Integer.MIN_VALUE) {
                    continue;
                }
                int y = c.lavaTop() - 1;
                VolcanicField.Column side = FIELD.column(x + 1, z);
                VolcanicField.Column[] others = {FIELD.column(x - 1, z), FIELD.column(x, z + 1), FIELD.column(x, z - 1)};
                boolean othersClosed = true;
                for (VolcanicField.Column o : others) {
                    othersClosed &= o.lavaAt(y) || o.height() - 1.5 - 4.0 * o.roughness() >= y + 1.0;
                }
                if (side.lavaTop() == c.lavaTop() - 1 && othersClosed && c.height() < y) {
                    assertSame(Blocks.LAVA.defaultBlockState(),
                            ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(x, y, z), -0.5), x + "," + z);
                    steps++;
                }
            }
        }
        assertTrue(steps > 0, "no river step in the sample");
    }

    @Test
    void ring3HasNoWaterFromYZeroUp() {
        RingAquifer ring3 = new RingAquifer(Level.OVERWORLD, WATER_WITH_UPDATE, FIELD);
        assertSame(Blocks.AIR.defaultBlockState(), ring3.computeSubstance(RingPosition.within(Ring.RING_3), at(40), -0.5));
        assertFalse(ring3.shouldScheduleFluidUpdate());
        assertSame(Blocks.AIR.defaultBlockState(), ring3.computeSubstance(RingPosition.within(Ring.RING_3), at(0), -0.5));
        // Deep aquifers keep their water, and ring 2 keeps all of it.
        assertSame(Blocks.WATER.defaultBlockState(), ring3.computeSubstance(RingPosition.within(Ring.RING_3), at(-1), -0.5));
        assertSame(Blocks.WATER.defaultBlockState(), ring3.computeSubstance(RingPosition.within(Ring.RING_2), at(40), -0.5));
        assertTrue(ring3.shouldScheduleFluidUpdate());
    }

    @Test
    void ring3HalvesOfItsZonesAreSealedBelowTheSeaLevel() {
        RingAquifer ring3 = new RingAquifer(Level.OVERWORLD, WATER_WITH_UPDATE, FIELD);
        RingPosition ring3Half = new RingPosition(Ring.RING_2, Ring.RING_3, 0.6);
        assertNull(ring3.computeSubstance(ring3Half, at(62), -0.5));
        assertFalse(ring3.shouldScheduleFluidUpdate());
        assertSame(Blocks.AIR.defaultBlockState(), ring3.computeSubstance(ring3Half, at(63), -0.5));
        assertSame(Blocks.WATER.defaultBlockState(), ring3.computeSubstance(new RingPosition(Ring.RING_2, Ring.RING_3, 0.4), at(62), -0.5));
        // The same at the zone 3→4 (ring 4 keeps the sea), on ring 3's side only.
        assertNull(ring3.computeSubstance(new RingPosition(Ring.RING_3, Ring.RING_4, 0.3), at(-5), -0.5));
        assertSame(Blocks.WATER.defaultBlockState(), ring3.computeSubstance(new RingPosition(Ring.RING_3, Ring.RING_4, 0.7), at(-5), -0.5));
    }
}
