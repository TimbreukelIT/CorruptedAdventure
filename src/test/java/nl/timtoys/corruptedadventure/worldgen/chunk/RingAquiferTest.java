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
import nl.timtoys.corruptedadventure.worldgen.feature.FireGeodeShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        for (Ring ring : new Ring[]{Ring.RING_1, Ring.RING_4, Ring.RING_5}) {
            assertSame(Blocks.LAVA.defaultBlockState(), still.computeSubstance(RingPosition.within(ring), at(-20), -0.5), ring.name());
            assertFalse(still.shouldScheduleFluidUpdate());
        }
        // 42b (V2 = A): ring 3 has no local aquifer lava (its pools stood against the air of their caves); vanilla's lava
        // layer below Y -54 stays.
        assertSame(Blocks.AIR.defaultBlockState(), still.computeSubstance(RingPosition.within(Ring.RING_3), at(-20), -0.5));
        assertFalse(still.shouldScheduleFluidUpdate());
        assertSame(Blocks.LAVA.defaultBlockState(), still.computeSubstance(RingPosition.within(Ring.RING_3), at(-60), -0.5));
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
        // 42b: ring 3 itself has no local aquifer lava.
        assertSame(Blocks.AIR.defaultBlockState(), still.computeSubstance(RingPosition.within(Ring.RING_3), at(-20), -0.5));
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

    /** Ring 3's aquifer over the test field, with vanilla's aquifer dry (open space stays air). */
    private static RingAquifer ring3() {
        return new RingAquifer(Level.OVERWORLD, DRY, FIELD);
    }

    /** Whether lava at the block is closed in below and on the three sides other than +x (lava or surely rock there). */
    private static boolean closedElsewhere(RingAquifer aquifer, int x, int y, int z) {
        int[][] others = {{x, y - 1, z}, {x - 1, y, z}, {x, y, z + 1}, {x, y, z - 1}};
        for (int[] o : others) {
            VolcanicField.Column n = FIELD.column(o[0], o[2]);
            boolean lava = o[1] < n.lavaTop() && o[1] >= n.lavaBottom();
            boolean rock = !n.voidAt(o[1]) && o[1] < aquifer.surfaceAt(o[0], o[2]) - 4.0 * n.roughness();
            if (!lava && !rock) {
                return false;
            }
        }
        return true;
    }

    @Test
    void ring3CarversStayOutOfTheBandAroundLava() {
        // 42b: a carver asks with substance 0; in the band around a lava lake it gets rock (it cannot cut a tunnel there
        // that would open the lava), while the noise's own open space above the lake stays air.
        RingAquifer ring3 = ring3();
        int[] lava = innerLavaColumn(-5200, -2700, -4400, -2000);
        RingPosition inRing3 = RingPosition.within(Ring.RING_3);
        DensityFunction.FunctionContext above = new DensityFunction.SinglePointContext(lava[0], lava[1] + 1, lava[2]);
        assertSame(Blocks.AIR.defaultBlockState(), ring3.computeSubstance(inRing3, above, -0.5));
        assertNull(ring3.computeSubstance(inRing3, above, 0.0));
        assertNull(ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(lava[0], lava[1] - 8, lava[2]), 0.0));
    }

    @Test
    void ring3LavaBesideOpenAirBecomesCrust() {
        RingAquifer ring3 = ring3();
        RingPosition inRing3 = RingPosition.within(Ring.RING_3);
        int crusts = 0;
        for (int x = -6000; x <= 6000 && crusts < 5; x++) {
            for (int z = -6000; z <= 6000 && crusts < 5; z += 89) {
                VolcanicField.Column c = FIELD.column(x, z);
                if (c.lavaTop() == Integer.MIN_VALUE) {
                    continue;
                }
                int y = c.lavaTop() - 1;
                VolcanicField.Column side = FIELD.column(x + 1, z);
                // 42b: a neighbour without lava at this height whose real (interpolated) surface lies at or below it.
                if ((side.lavaTop() <= y - 1 || y < side.lavaBottom()) && !side.voidAt(y) && !side.thinSolidAt(y)
                        && y >= ring3.surfaceAt(x + 1, z) + 0.5 && side.roughness() == 0.0) {
                    assertNull(ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(x, y, z), -0.5), x + "," + z);
                    assertFalse(ring3.shouldScheduleFluidUpdate());
                    crusts++;
                }
            }
        }
        assertTrue(crusts > 0, "no lava edge in the sample");
    }

    @Test
    void ring3RiverStepsStayLavaFallsOnlyOntoLava() {
        // 39d: lava beside a neighbour whose own lava lies one block lower and surely fills that block (one step down a
        // river) stays lava: a small lava fall onto lava. 42b: where that lower block may be rock (the bed's interpolated
        // surface lies higher than the field says), the lava would spill: a lip of crust.
        RingAquifer ring3 = ring3();
        RingPosition inRing3 = RingPosition.within(Ring.RING_3);
        int falls = 0;
        int spills = 0;
        for (int x = -6000; x <= 6000 && (falls < 3 || spills < 3); x += 2) {
            for (int z = -6000; z <= 6000 && (falls < 3 || spills < 3); z += 97) {
                VolcanicField.Column c = FIELD.column(x, z);
                if (c.lavaTop() == Integer.MIN_VALUE) {
                    continue;
                }
                int y = c.lavaTop() - 1;
                VolcanicField.Column side = FIELD.column(x + 1, z);
                if (side.lavaTop() != y || side.roughness() > 0.0 || c.roughness() > 0.0 || side.voidAt(y) || side.voidAt(y - 1)
                        || y < ring3.surfaceAt(x + 1, z) + 0.5 || y < ring3.surfaceAt(x, z) + 0.5 || !closedElsewhere(ring3, x, y, z)) {
                    continue;
                }
                BlockState state = ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(x, y, z), -0.5);
                if (y - 1 >= ring3.surfaceAt(x + 1, z)) {
                    assertSame(Blocks.LAVA.defaultBlockState(), state, "a fall onto lava at " + x + "," + z);
                    falls++;
                } else {
                    assertNull(state, "a spill onto rock at " + x + "," + z);
                    spills++;
                }
            }
        }
        assertTrue(falls > 0 && spills > 0, "falls " + falls + ", spills " + spills);
    }

    @Test
    void ring3LavaNeverStandsAgainstOpenAir() {
        // 42b: everywhere in sample areas (lakes, rivers, the basalt flows, the abyss), a block the aquifer fills with lava
        // has, on every side and below, rock, lava, or (a step down a river) the air over lava that surely fills the block
        // below. "Open" uses the exact interpolated surface (the stack is linear in y; around lava the guard keeps caves
        // out), the carved openings and the thin solids.
        RingAquifer ring3 = ring3();
        RingPosition inRing3 = RingPosition.within(Ring.RING_3);
        int[][] areas = {{3936, -3600, 4319, -3233}, {-4000, -128, -3633, 255}, {-4080, -1984, -3697, -1601},
                {4720, -576, 4975, -337}, {3920, 320, 4111, 511}};
        int lava = 0;
        for (int[] a : areas) {
            for (int x = a[0]; x <= a[2]; x++) {
                for (int z = a[1]; z <= a[3]; z++) {
                    VolcanicField.Column c = FIELD.column(x, z);
                    if (c.lavaTop() == Integer.MIN_VALUE || c.roughness() > 0.0) {
                        continue;
                    }
                    for (int y = Math.max(c.lavaBottom(), (int) Math.ceil(ring3.surfaceAt(x, z))); y < c.lavaTop(); y++) {
                        if (c.thinSolidAt(y) || c.islandAt(y)) {
                            continue;
                        }
                        BlockState state = ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(x, y, z), -0.5);
                        if (state != Blocks.LAVA.defaultBlockState()) {
                            continue;
                        }
                        lava++;
                        assertHeld(ring3, x, y - 1, z, false);
                        assertHeld(ring3, x + 1, y, z, true);
                        assertHeld(ring3, x - 1, y, z, true);
                        assertHeld(ring3, x, y, z + 1, true);
                        assertHeld(ring3, x, y, z - 1, true);
                    }
                }
            }
        }
        assertTrue(lava > 10_000, "lava blocks " + lava);
    }

    /** The block beside (or below) lava: rock for certain, lava, or (beside only) the air of a river step onto lava. */
    private static void assertHeld(RingAquifer aquifer, int x, int y, int z, boolean beside) {
        VolcanicField.Column n = FIELD.column(x, z);
        boolean lava = y < n.lavaTop() && y >= n.lavaBottom();
        boolean open = !n.islandAt(y) && !n.thinSolidAt(y) && (n.voidAt(y) || y >= aquifer.surfaceAt(x, z) - 4.0 * n.roughness());
        if (!open || lava) {
            return;
        }
        boolean step = beside && n.lavaTop() == y && y - 1 >= n.lavaBottom() && (n.voidAt(y - 1) || y - 1 >= aquifer.surfaceAt(x, z) + 4.0 * n.roughness());
        assertTrue(step, "open air beside lava at " + x + "," + y + "," + z);
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
    void ring3CavesHoldNoAquiferFluidAndTheirGuardStaysRock() {
        // 42: a fire geode below Y 0 (where vanilla's aquifer may hold water): its cavity is air, the rock of its guard band
        // stays rock even where a carver asks (substance 0), so no cave or carver opens it.
        RingAquifer ring3 = new RingAquifer(Level.OVERWORLD, WATER_WITH_UPDATE, FIELD);
        RingPosition inRing3 = RingPosition.within(Ring.RING_3);
        FireGeodeShape.Geode geode = null;
        for (int x = -5000; x <= 5000 && geode == null; x += 48) {
            FireGeodeShape.Geode g = FIELD.fireGeode(x, 1500);
            if (g != null && g.y() < -10) {
                geode = g;
            }
        }
        assertNotNull(geode);
        assertSame(Blocks.AIR.defaultBlockState(),
                ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(geode.x(), geode.y(), geode.z()), -1.0));
        assertFalse(ring3.shouldScheduleFluidUpdate());
        int rim = geode.y() + geode.reach() + 1;
        assertNull(ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(geode.x(), rim, geode.z()), 0.0));
        // Outside the cave's band the rule is vanilla's again.
        assertSame(Blocks.WATER.defaultBlockState(),
                ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(geode.x(), geode.y() - geode.reach() - 4, geode.z()), -0.5));
        // A magma chamber's lake: still lava under the open chamber, rock below it.
        int[] lake = chamberLake();
        assertSame(Blocks.LAVA.defaultBlockState(),
                ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(lake[0], lake[1], lake[2]), -1.0));
        assertFalse(ring3.shouldScheduleFluidUpdate());
        assertSame(Blocks.AIR.defaultBlockState(),
                ring3.computeSubstance(inRing3, new DensityFunction.SinglePointContext(lake[0], lake[1] + 1, lake[2]), -1.0));
    }

    /** The top of a magma chamber's lake (x, y, z) inside it: a column whose lake and four neighbours' lakes reach as high. */
    private static int[] chamberLake() {
        for (int x = -5800; x <= -4000; x += 8) {
            for (int z = -2000; z <= 2000; z += 8) {
                VolcanicField.Column c = FIELD.column(x, z);
                int top = c.subLavaMax();
                if (top != Integer.MIN_VALUE && top - c.subLavaMin() >= 2 && c.subLavaAt(top) && c.voidAt(top + 1)
                        && FIELD.column(x + 1, z).subLavaAt(top) && FIELD.column(x - 1, z).subLavaAt(top)
                        && FIELD.column(x, z + 1).subLavaAt(top) && FIELD.column(x, z - 1).subLavaAt(top)) {
                    return new int[]{x, top, z};
                }
            }
        }
        throw new AssertionError("no magma chamber in the area");
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
