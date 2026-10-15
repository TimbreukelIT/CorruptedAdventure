package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingGateTest {
    private static final RingSettings DEFAULTS = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
    private static final RingLayout LAYOUT = new RingLayout(DEFAULTS, 20260930L);
    private static final DensityFunction.FunctionContext ANYWHERE = new DensityFunction.SinglePointContext(0, 64, 0);

    @Test
    void reachCoversTheRingItsZonesAndTheMargin() {
        double m = RingGate.MARGIN;
        // Ring 2 has weight from 1500 − 64 (start of zone 1→2) to 3500 + 64 (end of zone 2→3).
        for (double d : new double[]{1436.5, 1500, 1563, 2500, 3436, 3500, 3563.5}) {
            assertTrue(LAYOUT.reaches(Ring.RING_2, d, 0), "inside at " + d);
        }
        assertTrue(LAYOUT.reaches(Ring.RING_2, 1436 - m + 0.5, m));
        assertFalse(LAYOUT.reaches(Ring.RING_2, 1436 - m - 0.5, m));
        assertTrue(LAYOUT.reaches(Ring.RING_2, 3564 + m - 0.5, m));
        assertFalse(LAYOUT.reaches(Ring.RING_2, 3564 + m + 0.5, m));
        assertFalse(LAYOUT.reaches(Ring.RING_2, 700, m));
        assertFalse(LAYOUT.reaches(Ring.RING_2, 4500, m));
        // Ring 1 starts at the centre, the void never ends.
        assertTrue(LAYOUT.reaches(Ring.RING_1, 0, m));
        assertFalse(LAYOUT.reaches(Ring.RING_1, 1564 + m + 0.5, m));
        assertTrue(LAYOUT.reaches(Ring.VOID, 1e9, m));
        assertFalse(LAYOUT.reaches(Ring.VOID, 9436 - m - 0.5, m));
    }

    @Test
    void everyCellCornerOfAWeightedColumnIsInReach() {
        // Wherever a ring has weight, the corners of its cell (up to 4 blocks away) must still be computed for real,
        // also with a much stronger boundary noise than the default.
        for (int noise : new int[]{96, 400, 1000}) {
            RingLayout layout = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, noise, 128,
                    CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), 20260930L);
            double margin = layout.reachMargin(RingGate.MARGIN, 1);
            for (int x = 1000; x < 4000; x += 5) {
                for (int z = -1200; z < 1200; z += 37) {
                    RingPosition position = layout.sampleOverworld(x, z);
                    for (Ring ring : weighted(position)) {
                        for (int dx = -4; dx <= 4; dx += 4) {
                            for (int dz = -4; dz <= 4; dz += 4) {
                                assertTrue(layout.reaches(ring, layout.effectiveDistance(x + dx, z + dz), margin),
                                        ring + " at " + (x + dx) + "," + (z + dz) + ", noise " + noise);
                            }
                        }
                    }
                }
            }
        }
        assertEquals(RingGate.MARGIN, LAYOUT.reachMargin(RingGate.MARGIN, 1));
        assertEquals(4 * RingGate.MARGIN, LAYOUT.reachMargin(RingGate.MARGIN, 4));
    }

    /**
     * verdant_falls: ring 3 mixes only over the first {@link IslandField#VERDANT_EDGE_SHARE} of the zone 3→4, so its
     * reach ends there (plus the margin); its inner side and the other rings stay as they were.
     */
    @Test
    void aMixShareEndsTheReachInTheOuterZone() {
        double m = RingGate.MARGIN;
        double share = IslandField.VERDANT_EDGE_SHARE;
        // Zone 3→4: 6000 ± 128, so ring 3 has weight up to 5872 + 60.
        assertTrue(LAYOUT.reaches(Ring.RING_3, 5932 + m - 0.5, m, share));
        assertFalse(LAYOUT.reaches(Ring.RING_3, 5932 + m + 0.5, m, share));
        assertTrue(LAYOUT.reaches(Ring.RING_3, 6128 + m - 0.5, m), "without the share: the whole zone");
        for (double d = 0.0; d < 11000.0; d += 7.25) {
            for (Ring ring : Ring.values()) {
                assertEquals(LAYOUT.reaches(ring, d, m), LAYOUT.reaches(ring, d, m, 1.0), ring + " at " + d);
                if (ring != Ring.RING_3 || d < 5900.0) {
                    assertEquals(LAYOUT.reaches(ring, d, m), LAYOUT.reaches(ring, d, m, ring == Ring.RING_3 ? share : 1.0), ring + " at " + d);
                }
            }
        }
    }

    /** With the share, every cell corner of a column where ring 3 still has weight stays in its reach (also with strong noise). */
    @Test
    void everyCellCornerOfAMixedColumnIsInReach() {
        double share = IslandField.VERDANT_EDGE_SHARE;
        for (int noise : new int[]{96, 400, 1000}) {
            RingLayout layout = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, noise, 128,
                    CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), 20260930L);
            double margin = layout.reachMargin(RingGate.MARGIN, 1);
            int weighted = 0;
            for (int x = 4800; x < 7200; x += 3) {
                for (int z = -1500; z < 1500; z += 41) {
                    if (!ring3Weighted(layout.sampleOverworld(x, z), share)) {
                        continue;
                    }
                    weighted++;
                    for (int dx = -4; dx <= 4; dx += 4) {
                        for (int dz = -4; dz <= 4; dz += 4) {
                            assertTrue(layout.reaches(Ring.RING_3, layout.effectiveDistance(x + dx, z + dz), margin, share),
                                    "ring 3 at " + (x + dx) + "," + (z + dz) + ", noise " + noise);
                        }
                    }
                }
            }
            assertTrue(weighted > 1000, "weighted columns " + weighted);
        }
    }

    /** Whether ring 3 has weight in {@link RingBlend} with {@code mix.ring_3} = {@code share} at a position. */
    private static boolean ring3Weighted(RingPosition position, double share) {
        if (!position.inTransition()) {
            return position.ring() == Ring.RING_3;
        }
        if (position.outer() == Ring.RING_3) {
            return position.blend() > 0.0;
        }
        return position.inner() == Ring.RING_3 && position.blend() < share;
    }

    /**
     * Bit-equal: the terrain through the zone 3→4 is the same with the share in ring 3's gate as with a gate that never
     * closes. Ring 3's input is read as the noise chunk reads an interpolator: the bilinear blend of its cell's corners,
     * each corner gated (0 out of reach); only less of it is computed.
     */
    @Test
    void theMixedGateLeavesTheDensityBitEqual() {
        double share = IslandField.VERDANT_EDGE_SHARE;
        double margin = LAYOUT.reachMargin(RingGate.MARGIN, 1);
        DensityFunction terrain = new Wave();
        RingGate gate = new RingGate(Level.OVERWORLD, Ring.RING_3, terrain, share);
        int[] skipped = {0};
        DensityFunction gated = new Cornered(gate, (x, z) -> {
            boolean reaches = LAYOUT.reaches(Ring.RING_3, LAYOUT.effectiveDistance(x, z), margin, share);
            skipped[0] += reaches ? 0 : 1;
            return reaches;
        });
        DensityFunction open = new Cornered(gate, (x, z) -> true);
        RingBlend withGate = blend(gated, share);
        RingBlend without = blend(open, share);
        int points = 0;
        for (int x = 5700; x <= 6300; x += 1) {
            for (int z = -900; z <= 900; z += 37) {
                RingPosition position = LAYOUT.sampleOverworld(x, z);
                DensityFunction.FunctionContext context = new DensityFunction.SinglePointContext(x, 70, z);
                assertEquals(without.compute(context, position), withGate.compute(context, position), "at " + x + "," + z);
                points++;
            }
        }
        assertTrue(points > 20_000);
        assertTrue(skipped[0] == 0, "the blend never asks ring 3 out of its reach: " + skipped[0]);
        // Out there the gate would say 0: the noise chunk's interpolators of ring 3 stay empty beyond the share.
        assertFalse(LAYOUT.reaches(Ring.RING_3, 6000.0, margin, share));
    }

    private static RingBlend blend(DensityFunction ring3, double share) {
        java.util.Map<Ring, DensityFunction> rings = new java.util.EnumMap<>(Ring.class);
        for (Ring ring : Ring.values()) {
            rings.put(ring, DensityFunctions.constant(10.0 * ring.ordinal()));
        }
        rings.put(Ring.RING_3, ring3);
        rings.put(Ring.RING_4, new Wave());
        return new RingBlend(Level.OVERWORLD, rings, java.util.Set.of(), java.util.Map.of(Ring.RING_3, share));
    }

    /** A terrain that differs at every block. */
    private record Wave() implements DensityFunction.SimpleFunction {
        @Override
        public double compute(FunctionContext context) {
            return Math.sin(context.blockX() * 0.37) * 7.0 + Math.cos(context.blockZ() * 0.11) * 3.0 + context.blockY() * 0.01;
        }

        @Override
        public double minValue() {
            return -11.0;
        }

        @Override
        public double maxValue() {
            return 11.0;
        }

        @Override
        public net.minecraft.util.KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }

    /** {@code gate} as an interpolator gives it: bilinear between its 4-block cell's corners, each gated by {@code reach}. */
    private record Cornered(RingGate gate, java.util.function.BiPredicate<Integer, Integer> reach) implements DensityFunction.SimpleFunction {
        @Override
        public double compute(FunctionContext context) {
            int x0 = Math.floorDiv(context.blockX(), 4) * 4;
            int z0 = Math.floorDiv(context.blockZ(), 4) * 4;
            double fx = (context.blockX() - x0) / 4.0;
            double fz = (context.blockZ() - z0) / 4.0;
            double c00 = corner(x0, context.blockY(), z0);
            double c10 = corner(x0 + 4, context.blockY(), z0);
            double c01 = corner(x0, context.blockY(), z0 + 4);
            double c11 = corner(x0 + 4, context.blockY(), z0 + 4);
            return (c00 * (1.0 - fx) + c10 * fx) * (1.0 - fz) + (c01 * (1.0 - fx) + c11 * fx) * fz;
        }

        private double corner(int x, int y, int z) {
            return gate.compute(new SinglePointContext(x, y, z), reach.test(x, z));
        }

        @Override
        public double minValue() {
            return gate.minValue();
        }

        @Override
        public double maxValue() {
            return gate.maxValue();
        }

        @Override
        public net.minecraft.util.KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }

    private static List<Ring> weighted(RingPosition position) {
        if (!position.inTransition()) {
            return List.of(position.inner());
        }
        return position.blend() == 0.0 ? List.of(position.inner()) : List.of(position.inner(), position.outer());
    }

    @Test
    void gateGivesTheArgumentInReachAndZeroOutside() {
        RingGate gate = new RingGate(Level.OVERWORLD, Ring.RING_2, DensityFunctions.constant(-3.5));
        assertEquals(-3.5, gate.compute(ANYWHERE, true));
        assertEquals(0.0, gate.compute(ANYWHERE, false));
        assertEquals(-3.5, gate.minValue());
        assertEquals(0.0, gate.maxValue());
    }

    @Test
    void codecRoundTrip() {
        RingGate gate = new RingGate(Level.NETHER, Ring.RING_3, DensityFunctions.constant(2.0));
        JsonElement json = RingGate.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, gate).getOrThrow();
        RingGate parsed = RingGate.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(Level.NETHER, parsed.dimension());
        assertEquals(Ring.RING_3, parsed.ring());
        assertEquals(2.0, parsed.argument().compute(ANYWHERE));
        assertEquals(json, RingGate.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, parsed).getOrThrow());
        assertEquals("ring_3", json.getAsJsonObject().get("ring").getAsString());
        assertEquals(1.0, parsed.mix());
        RingGate mixed = new RingGate(Level.OVERWORLD, Ring.RING_3, DensityFunctions.constant(2.0), 0.25);
        JsonElement mixedJson = RingGate.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, mixed).getOrThrow();
        assertEquals(0.25, RingGate.MAP_CODEC.codec().parse(JsonOps.INSTANCE, mixedJson).getOrThrow().mix());
    }

    @Test
    void interpolatedMarkersGetAGateFlatCachesAFillGateNeverTwice() {
        DensityFunction inner = DensityFunctions.constant(1.0);
        DensityFunction interpolated = RingGate.gateCacheMarker(Level.OVERWORLD, Ring.RING_2, DensityFunctions.interpolated(inner));
        RingGate gate = assertInstanceOf(RingGate.class, ((DensityFunctions.MarkerOrMarked) interpolated).wrapped());
        assertEquals(Ring.RING_2, gate.ring());
        assertSame(inner, gate.argument());
        assertSame(interpolated, RingGate.gateCacheMarker(Level.OVERWORLD, Ring.RING_2, interpolated));

        DensityFunction flat = DensityFunctions.flatCache(inner);
        DensityFunction gatedFlat = RingGate.gateCacheMarker(Level.OVERWORLD, Ring.RING_2, flat);
        assertSame(((DensityFunctions.MarkerOrMarked) flat).codec(), ((DensityFunctions.MarkerOrMarked) gatedFlat).codec());
        RingFillGate fillGate = assertInstanceOf(RingFillGate.class, ((DensityFunctions.MarkerOrMarked) gatedFlat).wrapped());
        assertEquals(Ring.RING_2, fillGate.ring());
        assertSame(inner, fillGate.argument());
        assertSame(gatedFlat, RingGate.gateCacheMarker(Level.OVERWORLD, Ring.RING_2, gatedFlat));

        DensityFunction cache2d = DensityFunctions.cache2d(inner);
        assertSame(cache2d, RingGate.gateCacheMarker(Level.OVERWORLD, Ring.RING_2, cache2d));
        assertSame(inner, RingGate.gateCacheMarker(Level.OVERWORLD, Ring.RING_2, inner));
    }
}
