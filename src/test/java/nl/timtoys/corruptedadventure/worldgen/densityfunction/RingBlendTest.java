package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingBlendTest {
    private static final DensityFunction.FunctionContext ANYWHERE = new DensityFunction.SinglePointContext(0, 64, 0);

    private final RingBlend blend;

    RingBlendTest() {
        // Each ring has constant density 10 × ordinal.
        Map<Ring, DensityFunction> rings = new EnumMap<>(Ring.class);
        for (Ring ring : Ring.values()) {
            rings.put(ring, DensityFunctions.constant(10.0 * ring.ordinal()));
        }
        blend = new RingBlend(Level.OVERWORLD, rings);
    }

    @Test
    void insideARingUsesOnlyThatRing() {
        for (Ring ring : Ring.values()) {
            assertEquals(10.0 * ring.ordinal(), blend.compute(ANYWHERE, RingPosition.within(ring)));
        }
    }

    @Test
    void transitionIsASmoothstepBetweenNeighbours() {
        assertEquals(10.0, blend.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 0.0)));
        assertEquals(15.0, blend.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 0.5)), 1e-9);
        assertEquals(10.0 + 10.0 * 0.15625, blend.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 0.25)), 1e-9);
        assertEquals(40.0 + 10.0 * 0.84375, blend.compute(ANYWHERE, new RingPosition(Ring.RING_5, Ring.VOID, 0.75)), 1e-9);
    }

    @Test
    void smoothstepIsFlatAtBothEndsAndMonotonic() {
        assertEquals(0.0, RingBlend.smoothstep(0.0));
        assertEquals(1.0, RingBlend.smoothstep(1.0));
        double step = 1e-4;
        assertTrue(RingBlend.smoothstep(step) / step < 0.01, "kink at the start of the zone");
        assertTrue((1.0 - RingBlend.smoothstep(1.0 - step)) / step < 0.01, "kink at the end of the zone");
        for (double t = 0.0; t < 0.99; t += 0.01) {
            assertTrue(RingBlend.smoothstep(t + 0.01) > RingBlend.smoothstep(t));
        }
    }

    @Test
    void rangeCoversAllInputs() {
        assertEquals(0.0, blend.minValue());
        assertEquals(50.0, blend.maxValue());
    }

    @Test
    void mapAllGatesEveryInterpolatedMarkerWithItsOwnRingOnce() {
        Map<Ring, DensityFunction> rings = new EnumMap<>(Ring.class);
        for (Ring ring : Ring.values()) {
            rings.put(ring, DensityFunctions.add(
                    DensityFunctions.interpolated(DensityFunctions.constant(ring.ordinal())),
                    DensityFunctions.flatCache(DensityFunctions.constant(ring.ordinal()))));
        }
        // mapAll runs twice in a real world (random state, then every noise chunk); record what the second pass sees.
        DensityFunction once = new RingBlend(Level.OVERWORLD, rings).mapAll(function -> function);
        List<DensityFunction> seen = new ArrayList<>();
        once.mapAll(function -> {
            seen.add(function);
            return function;
        });
        List<RingGate> gates = seen.stream().filter(RingGate.class::isInstance).map(RingGate.class::cast).toList();
        // One per ring: the flat caches are left alone, nothing is gated twice.
        assertEquals(Ring.values().length, gates.size());
        for (Ring ring : Ring.values()) {
            assertEquals(1, gates.stream().filter(gate -> gate.ring() == ring
                    && gate.argument().equals(DensityFunctions.constant(ring.ordinal()))).count(), ring.toString());
        }
    }

    @Test
    void missingRingIsRejected() {
        Map<Ring, DensityFunction> rings = new EnumMap<>(Ring.class);
        rings.put(Ring.RING_1, DensityFunctions.zero());
        assertTrue(RingBlend.validateRings(rings).isError());
    }
}
