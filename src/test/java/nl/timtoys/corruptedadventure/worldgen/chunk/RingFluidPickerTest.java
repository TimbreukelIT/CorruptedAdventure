package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandFieldTest;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicFieldTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingFluidPickerTest {
    private static final Aquifer.FluidStatus SEA = new Aquifer.FluidStatus(63, Blocks.WATER.defaultBlockState());
    private static final RingFluidPicker PICKER = new RingFluidPicker(Level.OVERWORLD, (x, y, z) -> SEA);

    @Test
    void ringsKeepTheVanillaSea() {
        // Ring 5 is dry (82): ring5IsDryWithItsLavaLayerOnlyInTheInnerBand.
        for (Ring ring : new Ring[]{Ring.RING_1, Ring.RING_4}) {
            assertSame(SEA, PICKER.computeFluid(RingPosition.within(ring), 0, 40, 0));
        }
        // 85 (V7-A): the fracture zone 4→5 (both halves) is dry at every height (below Y −54 ring 4's dam holds rock).
        for (double blend : new double[]{0.1, 0.4, 0.6, 0.9}) {
            RingPosition zone = new RingPosition(Ring.RING_4, Ring.RING_5, blend);
            for (int y : new int[]{100, 62, 40, 1, 0, -20, -53, -60}) {
                assertTrue(PICKER.computeFluid(zone, 0, y, 0).at(y).isAir(), "zone 4→5 at " + blend + ", y " + y);
            }
        }
    }

    /**
     * 82 (V9-B): no sea in overworld ring 5 (switching hard on {@code ring()}, so also its half of the zone 5→void, which
     * has no fluid anyway), but vanilla's lava layer below Y −54 in the inner band, more than the dam inside the band
     * line; the band is only asked there. Its half of the zone 4→5 counts as ring 5 (85), but the zone's own dry rule
     * comes first; the Nether keeps its own rules.
     */
    @Test
    void ring5IsDryWithItsLavaLayerOnlyInTheInnerBand() {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        Aquifer.FluidPicker vanilla = (x, y, z) -> y < -54 ? lava : SEA;
        int[] asked = new int[1];
        double[] band = new double[1];
        RingFluidPicker picker = new RingFluidPicker(Level.OVERWORLD, vanilla, Map.of(), (x, z) -> {
            asked[0]++;
            return band[0];
        });
        RingPosition ring5 = RingPosition.within(Ring.RING_5);
        for (double b : new double[]{-RingFluidPicker.RING_5_DAM - 1.0, -RingFluidPicker.RING_5_DAM, -1.0, 0.0, 400.0}) {
            band[0] = b;
            for (int y : new int[]{-54, -40, 0, 40, 62, 63, 100}) {
                assertTrue(picker.computeFluid(ring5, 0, y, 0).at(y).isAir(), "y " + y + ", band " + b);
            }
            boolean layer = b < -RingFluidPicker.RING_5_DAM;
            for (int y : new int[]{-55, -64}) {
                assertEquals(layer, picker.computeFluid(ring5, 0, y, 0).at(y).is(Blocks.LAVA), "y " + y + ", band " + b);
            }
        }
        // The band is asked only below the layer's top.
        asked[0] = 0;
        picker.computeFluid(ring5, 0, 40, 0);
        assertEquals(0, asked[0]);
        assertTrue(RingFluidPicker.ring5Dry(Level.OVERWORLD, ring5));
        assertFalse(RingFluidPicker.ring5Dry(Level.OVERWORLD, new RingPosition(Ring.RING_5, Ring.VOID, 0.2)));
        assertFalse(RingFluidPicker.ring5Dry(Level.NETHER, ring5));
        assertFalse(RingFluidPicker.ring5Dry(Level.OVERWORLD, new RingPosition(Ring.RING_4, Ring.RING_5, 0.4)));
        assertTrue(RingFluidPicker.ring5Dry(Level.OVERWORLD, new RingPosition(Ring.RING_4, Ring.RING_5, 0.6)));
        // No lava layer in the zone, whatever the band says: the zone's rule comes first.
        RingFluidPicker lavaEverywhere = new RingFluidPicker(Level.OVERWORLD, (x, y, z) -> new Aquifer.FluidStatus(-54,
                Blocks.LAVA.defaultBlockState()), Map.of(), (x, z) -> -500.0);
        assertTrue(lavaEverywhere.computeFluid(new RingPosition(Ring.RING_4, Ring.RING_5, 0.6), 0, -60, 0).at(-60).isAir());
        for (Ring ring : new Ring[]{Ring.RING_1, Ring.RING_4, Ring.VOID}) {
            assertFalse(RingFluidPicker.ring5Dry(Level.OVERWORLD, RingPosition.within(ring)), ring.name());
        }
    }

    /**
     * 83: round each loose crack of the inner band a dam of {@link RingFluidPicker#RING_5_CRACK_DAM} blocks: no lava and
     * no open space below Y −54 there; the crack itself is open (no dam, no lava) down into the void; further away the
     * layer as before.
     */
    @Test
    void ring5LavaLayerKeepsItsDamRoundTheCracks() {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        double[] crack = new double[1];
        RingFluidPicker picker = new RingFluidPicker(Level.OVERWORLD, (x, y, z) -> y < -54 ? lava : SEA, Map.of(),
                new RingFluidPicker.Ring5Bands() {
                    @Override
                    public double signedDistance(int x, int z) {
                        return -500.0;
                    }

                    @Override
                    public double crackDistance(int x, int z) {
                        return crack[0];
                    }
                });
        RingPosition ring5 = RingPosition.within(Ring.RING_5);
        for (double d : new double[]{-2.0, 0.0, 0.5, 3.0, RingFluidPicker.RING_5_CRACK_DAM, RingFluidPicker.RING_5_CRACK_DAM + 0.5, 50.0}) {
            crack[0] = d;
            boolean layer = d > RingFluidPicker.RING_5_CRACK_DAM;
            assertEquals(layer, picker.computeFluid(ring5, 0, -60, 0).at(-60).is(Blocks.LAVA), "crack distance " + d);
            assertEquals(layer, RingFluidPicker.ring5LavaColumn(-500.0, d));
            // The dam holds rock below the layer's top round the crack, never in it.
            assertEquals(d > 0.0 && d <= RingFluidPicker.RING_5_CRACK_DAM, RingFluidPicker.ring5Dam(-500.0, d), "dam at " + d);
        }
        // The band line's dam stays; the outer band has neither.
        assertTrue(RingFluidPicker.ring5Dam(-1.0, Double.POSITIVE_INFINITY));
        assertFalse(RingFluidPicker.ring5Dam(1.0, 2.0));
        assertFalse(RingFluidPicker.ring5LavaColumn(1.0, 50.0));
    }

    /**
     * The running server's bands ({@link RingFluidPicker#CURRENT_BANDS}) without its overworld ring 5 terrain (no server
     * here, so none is registered): the crack dam cannot be decided, and the picker must fail loud rather than let the
     * lava layer through.
     */
    @Test
    void ring5CrackDamFailsLoudWithoutTheField() {
        assertThrows(IllegalStateException.class, () -> RingFluidPicker.CURRENT_BANDS.crackDistance(0, 0));
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
    void waterOnlyInOverworldRings2And4AboveTheLavaLayer() {
        RingPosition ring2 = RingPosition.within(Ring.RING_2);
        assertEquals(-54, RingFluidPicker.waterOnlyFrom(Level.OVERWORLD, ring2));
        assertEquals(Integer.MAX_VALUE, RingFluidPicker.waterOnlyFrom(Level.NETHER, ring2));
        assertEquals(-54, RingFluidPicker.waterOnlyFrom(Level.OVERWORLD, RingPosition.within(Ring.RING_4)));
        assertEquals(-54, RingFluidPicker.waterOnlyFrom(Level.OVERWORLD, new RingPosition(Ring.RING_4, Ring.RING_5, 0.4)));
        // Review 59 (#3): the whole zone 4→5, also ring 5's half (no water against lava at the 4|5 midline).
        assertEquals(-54, RingFluidPicker.waterOnlyFrom(Level.OVERWORLD, new RingPosition(Ring.RING_4, Ring.RING_5, 0.6)));
        assertEquals(Integer.MAX_VALUE, RingFluidPicker.waterOnlyFrom(Level.OVERWORLD, new RingPosition(Ring.RING_5, Ring.VOID, 0.2)));
        assertEquals(Integer.MAX_VALUE, RingFluidPicker.waterOnlyFrom(Level.NETHER, RingPosition.within(Ring.RING_4)));
        for (Ring ring : new Ring[]{Ring.RING_1, Ring.RING_3, Ring.RING_5, Ring.VOID}) {
            assertEquals(Integer.MAX_VALUE, RingFluidPicker.waterOnlyFrom(Level.OVERWORLD, RingPosition.within(ring)), ring.name());
        }
        assertEquals(-54, RingFluidPicker.waterOnlyFrom(Level.OVERWORLD, new RingPosition(Ring.RING_1, Ring.RING_2, 0.5)));
        assertEquals(Integer.MAX_VALUE, RingFluidPicker.waterOnlyFrom(Level.OVERWORLD, new RingPosition(Ring.RING_2, Ring.RING_3, 0.5)));
    }

    @Test
    void ring3BoundsPerColumn() {
        RingPosition ring3 = RingPosition.within(Ring.RING_3);
        RingPosition ring3Half = new RingPosition(Ring.RING_2, Ring.RING_3, 0.6);
        assertEquals(0, RingFluidPicker.ring3DryFrom(Level.OVERWORLD, ring3));
        assertEquals(0, RingFluidPicker.ring3DryFrom(Level.OVERWORLD, ring3Half));
        assertEquals(Integer.MAX_VALUE, RingFluidPicker.ring3DryFrom(Level.NETHER, ring3));
        assertEquals(Integer.MAX_VALUE, RingFluidPicker.ring3DryFrom(Level.OVERWORLD, new RingPosition(Ring.RING_2, Ring.RING_3, 0.4)));
        assertEquals(63, RingFluidPicker.ring3SealBelow(Level.OVERWORLD, ring3Half));
        assertEquals(63, RingFluidPicker.ring3SealBelow(Level.OVERWORLD, new RingPosition(Ring.RING_3, Ring.RING_4, 0.3)));
        assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring3SealBelow(Level.OVERWORLD, ring3));
        assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring3SealBelow(Level.OVERWORLD, new RingPosition(Ring.RING_3, Ring.RING_4, 0.7)));
        assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring3SealBelow(Level.NETHER, ring3Half));
        assertFalse(RingFluidPicker.ring3LavaColumn(Level.OVERWORLD, ring3, null));
        assertFalse(RingFluidPicker.ring3LavaColumn(Level.OVERWORLD, RingPosition.within(Ring.RING_2), null));
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

    /**
     * vf-perf2: a ring's status is remembered per vanilla status, so the per-block path reuses it; switching between
     * vanilla's statuses, between rings and into the sundered zone (its own level) still gives each its own answer.
     */
    @Test
    void rememberedStatusesFollowTheVanillaStatusAndTheRing() {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(32, Blocks.LAVA.defaultBlockState());
        Aquifer.FluidStatus water = new Aquifer.FluidStatus(40, Blocks.WATER.defaultBlockState());
        RingFluidPicker picker = new RingFluidPicker(Level.NETHER, (x, y, z) -> x < 0 ? lava : water,
                Map.of(Ring.RING_2, 22, Ring.RING_3, 12));
        RingPosition ring2 = RingPosition.within(Ring.RING_2);
        RingPosition ring3 = RingPosition.within(Ring.RING_3);
        for (int round = 0; round < 3; round++) {
            Aquifer.FluidStatus a = picker.computeFluid(ring2, -1, 10, 0);
            assertSame(Blocks.LAVA.defaultBlockState(), a.at(22));
            assertTrue(a.at(23).isAir());
            assertSame(a, picker.computeFluid(ring2, -5, 10, 0), "remembered");
            Aquifer.FluidStatus b = picker.computeFluid(ring2, 1, 10, 0);
            assertSame(Blocks.WATER.defaultBlockState(), b.at(22));
            assertTrue(b.at(23).isAir());
            Aquifer.FluidStatus c = picker.computeFluid(ring3, 1, 10, 0);
            assertSame(Blocks.WATER.defaultBlockState(), c.at(12));
            assertTrue(c.at(13).isAir());
        }
        RingFluidPicker overworld = new RingFluidPicker(Level.OVERWORLD, (x, y, z) -> SEA, Map.of(Ring.RING_4, 62));
        RingPosition zone = new RingPosition(Ring.RING_4, Ring.RING_5, 0.6);
        assertSame(Blocks.WATER.defaultBlockState(), overworld.computeFluid(RingPosition.within(Ring.RING_4), 0, 10, 0).at(1));
        assertTrue(overworld.computeFluid(zone, 0, 10, 0).at(0).isAir());
        assertTrue(overworld.computeFluid(zone, 0, 10, 0).at(1).isAir());
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

    @Test
    void ring3LevelMovesTheSeaButKeepsTheLavaLayer() {
        Aquifer.FluidStatus lavaLayer = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        RingFluidPicker picker = new RingFluidPicker(Level.OVERWORLD, (x, y, z) -> y < -54 ? lavaLayer : SEA, Map.of(Ring.RING_3, -1));
        RingPosition ring3 = RingPosition.within(Ring.RING_3);
        assertTrue(picker.computeFluid(ring3, 0, 0, 0).at(0).isAir(), "no sea at Y 0");
        assertSame(Blocks.WATER.defaultBlockState(), picker.computeFluid(ring3, 0, -1, 0).at(-1));
        // Below the lava layer's top the vanilla status (lava up to -55), not lava up to -1.
        assertSame(lavaLayer, picker.computeFluid(ring3, 0, -60, 0));
        assertSame(SEA, picker.computeFluid(RingPosition.within(Ring.RING_2), 0, 40, 0));
    }

    /**
     * 59: ring 4 dry below the lava layer; dams of rock over the whole zone 4→5 (both halves) and over ring 4's half of
     * the zone 3→4 (ring 3's half has its own seal); no carver lava in ring 4 and the dams.
     */
    @Test
    void ring4IsDryBelowTheLavaLayerWithADamOverTheZone4To5() {
        RingPosition ring4 = RingPosition.within(Ring.RING_4);
        assertEquals(-54, RingFluidPicker.ring4DryBelow(Level.OVERWORLD, ring4));
        assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring4DamBelow(Level.OVERWORLD, ring4));
        assertTrue(RingFluidPicker.ring4NoLava(Level.OVERWORLD, ring4));
        RingPosition ring4Half = new RingPosition(Ring.RING_3, Ring.RING_4, 0.7);
        assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring4DryBelow(Level.OVERWORLD, ring4Half));
        assertEquals(-54, RingFluidPicker.ring4DamBelow(Level.OVERWORLD, ring4Half));
        assertTrue(RingFluidPicker.ring4NoLava(Level.OVERWORLD, ring4Half));
        RingPosition ring3Half = new RingPosition(Ring.RING_3, Ring.RING_4, 0.3);
        assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring4DamBelow(Level.OVERWORLD, ring3Half));
        assertFalse(RingFluidPicker.ring4NoLava(Level.OVERWORLD, ring3Half));
        for (double blend : new double[]{0.01, 0.4, 0.6, 0.99}) {
            RingPosition zone = new RingPosition(Ring.RING_4, Ring.RING_5, blend);
            assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring4DryBelow(Level.OVERWORLD, zone), "blend " + blend);
            assertEquals(-54, RingFluidPicker.ring4DamBelow(Level.OVERWORLD, zone), "blend " + blend);
            assertTrue(RingFluidPicker.ring4NoLava(Level.OVERWORLD, zone), "blend " + blend);
        }
        for (RingPosition other : new RingPosition[]{RingPosition.within(Ring.RING_3), RingPosition.within(Ring.RING_5),
                new RingPosition(Ring.RING_5, Ring.VOID, 0.3)}) {
            assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring4DryBelow(Level.OVERWORLD, other), other.toString());
            assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring4DamBelow(Level.OVERWORLD, other), other.toString());
            assertFalse(RingFluidPicker.ring4NoLava(Level.OVERWORLD, other), other.toString());
        }
        assertFalse(RingFluidPicker.ring4NoLava(Level.NETHER, ring4));
        assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring4DryBelow(Level.NETHER, ring4));
        assertEquals(Integer.MIN_VALUE, RingFluidPicker.ring4DamBelow(Level.NETHER, new RingPosition(Ring.RING_4, Ring.RING_5, 0.5)));
        assertFalse(RingFluidPicker.ring4WaterColumn(Level.OVERWORLD, ring4, null));
    }

    /**
     * verdant_falls: ring 4's water branch from {@link IslandField#VERDANT_EDGE_SHARE} of the zone 3→4 outwards, also in
     * ring 3's half; no ring 3 terrain lava there. Ring 3's seal, its dryness from Y 0 and the dams stay.
     */
    @Test
    void verdantFallsStripHasRing4sWaterBranchAndKeepsRing3sSeal() {
        double share = IslandField.VERDANT_EDGE_SHARE;
        IslandField islands = IslandFieldTest.field();
        VolcanicField volcanic = VolcanicFieldTest.field();
        for (double blend : new double[]{share, 0.3, 0.49, 0.5, 0.7, 0.99}) {
            RingPosition verdant = new RingPosition(Ring.RING_3, Ring.RING_4, blend);
            assertTrue(RingFluidPicker.verdantWater(Level.OVERWORLD, verdant), "blend " + blend);
            assertTrue(RingFluidPicker.ring4WaterColumn(Level.OVERWORLD, verdant, islands), "blend " + blend);
            assertFalse(RingFluidPicker.ring4WaterColumn(Level.OVERWORLD, verdant, null));
            assertFalse(RingFluidPicker.ring3LavaColumn(Level.OVERWORLD, verdant, volcanic), "blend " + blend);
            assertFalse(RingFluidPicker.verdantWater(Level.NETHER, verdant));
            assertFalse(RingFluidPicker.ring4WaterColumn(Level.NETHER, verdant, islands));
        }
        for (double blend : new double[]{0.3, 0.49}) {
            RingPosition ring3Half = new RingPosition(Ring.RING_3, Ring.RING_4, blend);
            assertEquals(RingFluidPicker.SEA_LEVEL, RingFluidPicker.ring3SealBelow(Level.OVERWORLD, ring3Half), "the seal stays");
            assertEquals(RingFluidPicker.RING_3_DRY_FROM, RingFluidPicker.ring3DryFrom(Level.OVERWORLD, ring3Half));
        }
        assertEquals(-54, RingFluidPicker.ring4DamBelow(Level.OVERWORLD, new RingPosition(Ring.RING_3, Ring.RING_4, 0.7)));
        // The steam_fens rim is ring 3's.
        for (double blend : new double[]{0.0, 0.1, share - 1e-9}) {
            RingPosition rim = new RingPosition(Ring.RING_3, Ring.RING_4, blend);
            assertFalse(RingFluidPicker.verdantWater(Level.OVERWORLD, rim), "blend " + blend);
            assertFalse(RingFluidPicker.ring4WaterColumn(Level.OVERWORLD, rim, islands), "blend " + blend);
            assertTrue(RingFluidPicker.ring3LavaColumn(Level.OVERWORLD, rim, volcanic), "blend " + blend);
        }
        // Other zones and rings: unchanged.
        for (RingPosition other : new RingPosition[]{RingPosition.within(Ring.RING_3), new RingPosition(Ring.RING_2, Ring.RING_3, 0.9),
                new RingPosition(Ring.RING_4, Ring.RING_5, 0.3)}) {
            assertFalse(RingFluidPicker.verdantWater(Level.OVERWORLD, other), other.toString());
        }
        assertTrue(RingFluidPicker.ring3LavaColumn(Level.OVERWORLD, RingPosition.within(Ring.RING_3), volcanic));
        assertTrue(RingFluidPicker.ring4WaterColumn(Level.OVERWORLD, RingPosition.within(Ring.RING_4), islands));
        assertFalse(RingFluidPicker.ring4WaterColumn(Level.OVERWORLD, RingPosition.within(Ring.RING_3), islands));
    }
}
