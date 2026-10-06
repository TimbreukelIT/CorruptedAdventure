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
    private static final RingSettings DEFAULTS = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10000, 96, 128);
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
            RingLayout layout = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10000, noise, 128), 20260930L);
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
    }

    @Test
    void onlyInterpolatedMarkersAreGatedAndNeverTwice() {
        DensityFunction inner = DensityFunctions.constant(1.0);
        DensityFunction once = RingGate.gateInterpolated(Level.OVERWORLD, Ring.RING_2, DensityFunctions.interpolated(inner));
        RingGate gate = assertInstanceOf(RingGate.class, ((DensityFunctions.MarkerOrMarked) once).wrapped());
        assertEquals(Ring.RING_2, gate.ring());
        assertSame(inner, gate.argument());
        assertSame(once, RingGate.gateInterpolated(Level.OVERWORLD, Ring.RING_2, once));

        DensityFunction flat = DensityFunctions.flatCache(inner);
        assertSame(flat, RingGate.gateInterpolated(Level.OVERWORLD, Ring.RING_2, flat));
        assertSame(inner, RingGate.gateInterpolated(Level.OVERWORLD, Ring.RING_2, inner));
    }
}
