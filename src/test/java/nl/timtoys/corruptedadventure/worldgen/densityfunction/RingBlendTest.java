package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing2Features;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void constantInputsGiveTheWeightOfOneRing() {
        // ring_weight/overworld/ring_3 (change 31): ring 3 = 1, the rest 0, so the blend returns the smoothstep weight of ring 3.
        Map<Ring, DensityFunction> rings = new EnumMap<>(Ring.class);
        for (Ring ring : Ring.values()) {
            rings.put(ring, DensityFunctions.constant(ring == Ring.RING_3 ? 1.0 : 0.0));
        }
        RingBlend weight = new RingBlend(Level.OVERWORLD, rings);
        assertEquals(0.0, weight.compute(ANYWHERE, RingPosition.within(Ring.RING_2)));
        assertEquals(1.0, weight.compute(ANYWHERE, RingPosition.within(Ring.RING_3)));
        assertEquals(0.0, weight.compute(ANYWHERE, new RingPosition(Ring.RING_1, Ring.RING_2, 0.7)), "zone 1→2 has no ring 3 weight");
        assertEquals(0.0, weight.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 0.0)));
        assertEquals(0.5, weight.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 0.5)), 1e-9);
        assertEquals(RingBlend.smoothstep(0.25), weight.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 0.25)), 1e-9);
        assertEquals(1.0, weight.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 1.0)));
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
    void mapAllGatesEveryCacheMarkerWithItsOwnRingOnce() {
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
        List<RingFillGate> fillGates = seen.stream().filter(RingFillGate.class::isInstance).map(RingFillGate.class::cast).toList();
        // One gate (the interpolator) and one fill gate (the flat cache) per ring, nothing is gated twice.
        assertEquals(Ring.values().length, gates.size());
        assertEquals(Ring.values().length, fillGates.size());
        for (Ring ring : Ring.values()) {
            DensityFunction argument = DensityFunctions.constant(ring.ordinal());
            assertEquals(1, gates.stream().filter(gate -> gate.ring() == ring && gate.argument().equals(argument)).count(), ring.toString());
            assertEquals(1, fillGates.stream().filter(gate -> gate.ring() == ring && gate.argument().equals(argument)).count(), ring.toString());
        }
    }

    /** The gates of a ring that mixes over only part of its outer zone know that share; the other rings' the whole zone. */
    @Test
    void mapAllGivesEachGateItsRingsMixShare() {
        Map<Ring, DensityFunction> rings = new EnumMap<>(Ring.class);
        for (Ring ring : Ring.values()) {
            rings.put(ring, DensityFunctions.add(
                    DensityFunctions.interpolated(DensityFunctions.constant(ring.ordinal())),
                    DensityFunctions.flatCache(DensityFunctions.constant(ring.ordinal()))));
        }
        double share = IslandField.VERDANT_EDGE_SHARE;
        List<DensityFunction> seen = new ArrayList<>();
        new RingBlend(Level.OVERWORLD, rings, Set.of(), Map.of(Ring.RING_3, share)).mapAll(function -> function).mapAll(function -> {
            seen.add(function);
            return function;
        });
        for (DensityFunction function : seen) {
            if (function instanceof RingGate gate) {
                assertEquals(gate.ring() == Ring.RING_3 ? share : 1.0, gate.mix(), gate.ring().toString());
            } else if (function instanceof RingFillGate gate) {
                assertEquals(gate.ring() == Ring.RING_3 ? share : 1.0, gate.mix(), gate.ring().toString());
            }
        }
        assertEquals(2 * Ring.values().length, seen.stream().filter(RingGated.class::isInstance).count());
    }

    @Test
    void missingRingIsRejected() {
        Map<Ring, DensityFunction> rings = new EnumMap<>(Ring.class);
        rings.put(Ring.RING_1, DensityFunctions.zero());
        assertTrue(RingBlend.validateRings(rings).isError());
        assertThrows(IllegalArgumentException.class, () -> new RingBlend(Level.OVERWORLD, rings));
        assertTrue(decode("{\"dimension\": \"minecraft:overworld\", \"rings\": {\"ring_1\": 0.0}}").isError());
    }

    @Test
    void codecReadsSwitchAtAndRejectsVoidAndDuplicates() {
        String rings = "\"rings\": {\"ring_1\": 0.0, \"ring_2\": 10.0, \"ring_3\": 20.0, \"ring_4\": 30.0, \"ring_5\": 40.0, \"void\": 50.0}";
        RingBlend switching = decode("{\"dimension\": \"minecraft:the_nether\", " + rings + ", \"switch_at\": [\"ring_2\"]}").getOrThrow();
        assertEquals(10.0, switching.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 0.49)));
        assertEquals(5.0, switching.compute(ANYWHERE, new RingPosition(Ring.RING_1, Ring.RING_2, 0.5)), 1e-9, "zone 1→2 still mixes");
        assertTrue(decode("{\"dimension\": \"minecraft:the_nether\", " + rings + ", \"switch_at\": [\"void\"]}").isError());
        assertTrue(decode("{\"dimension\": \"minecraft:the_nether\", " + rings + ", \"switch_at\": [\"ring_2\", \"ring_2\"]}").isError(), "duplicate");
        assertThrows(IllegalArgumentException.class, () -> new RingBlend(Level.NETHER, constants(0, 0, 0, 0, 0, 0), Set.of(Ring.VOID)));
    }

    @Test
    void switchAtTakesTheInputOfTheRingThePositionBelongsTo() {
        RingBlend switching = new RingBlend(Level.OVERWORLD, constants(0, 10, 20, 30, 40, 50), Set.of(Ring.RING_3));
        assertEquals(20.0, switching.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, 0.0)));
        assertEquals(20.0, switching.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, 0.4999)));
        assertEquals(30.0, switching.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, 0.5)));
        assertEquals(30.0, switching.compute(ANYWHERE, RingPosition.within(Ring.RING_4)));
        // Other boundaries still mix.
        assertEquals(15.0, switching.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 0.5)), 1e-9);
        // mapAll keeps the mode.
        DensityFunction mapped = switching.mapAll(function -> function);
        assertEquals(20.0, ((RingBlend) mapped).compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, 0.4)));
    }

    /**
     * verdant_falls: with {@code mix} a boundary mixes only over the first share of its zone (the smoothstep stretched
     * over it), then the outer ring's input stands alone; other boundaries still mix over their whole zone.
     */
    @Test
    void mixEndsTheBlendAtItsShareOfTheZone() {
        double share = IslandField.VERDANT_EDGE_SHARE;
        RingBlend mixing = new RingBlend(Level.OVERWORLD, constants(0, 10, 20, 30, 40, 50), Set.of(), Map.of(Ring.RING_3, share));
        assertEquals(20.0, mixing.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, 0.0)));
        assertEquals(25.0, mixing.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, share / 2.0)), 1e-9);
        assertEquals(20.0 + 10.0 * RingBlend.smoothstep(0.25), mixing.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, share / 4.0)), 1e-9);
        for (double blend : new double[]{share, 0.3, 0.5, 0.9, 1.0}) {
            assertEquals(30.0, mixing.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, blend)), "blend " + blend);
        }
        // Continuous at the share (the smoothstep is flat there).
        assertEquals(30.0, mixing.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, share - 1e-6)), 1e-6);
        assertEquals(15.0, mixing.compute(ANYWHERE, new RingPosition(Ring.RING_2, Ring.RING_3, 0.5)), 1e-9, "zone 2→3 still mixes");
        assertEquals(40.0 + 10.0 * 0.84375, mixing.compute(ANYWHERE, new RingPosition(Ring.RING_5, Ring.VOID, 0.75)), 1e-9);
        // mapAll keeps the share.
        RingBlend mapped = (RingBlend) mixing.mapAll(function -> function);
        assertEquals(30.0, mapped.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, 0.4)));
        assertEquals(25.0, mapped.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, share / 2.0)), 1e-9);
    }

    /** Without {@code mix} nothing changes: the whole zone mixes, also at its very edges. */
    @Test
    void withoutMixTheWholeZoneMixesAsBefore() {
        RingBlend plain = new RingBlend(Level.OVERWORLD, constants(0, 10, 20, 30.3, 40, 50), Set.of(), Map.of());
        for (double blend = 0.0; blend <= 1.0; blend += 0.0625) {
            assertEquals(Mth.lerp(RingBlend.smoothstep(blend), 20.0, 30.3),
                    plain.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, blend)), 0.0, "blend " + blend);
        }
    }

    @Test
    void codecReadsMixAndRejectsBadShares() {
        String rings = "\"rings\": {\"ring_1\": 0.0, \"ring_2\": 10.0, \"ring_3\": 20.0, \"ring_4\": 30.0, \"ring_5\": 40.0, \"void\": 50.0}";
        String head = "{\"dimension\": \"minecraft:overworld\", " + rings;
        RingBlend mixing = decode(head + ", \"mix\": {\"ring_3\": 0.25}}").getOrThrow();
        assertEquals(30.0, mixing.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, 0.25)));
        assertEquals(25.0, mixing.compute(ANYWHERE, new RingPosition(Ring.RING_3, Ring.RING_4, 0.125)), 1e-9);
        // Written back as read.
        assertEquals(0.25, RingBlend.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, mixing).getOrThrow().getAsJsonObject()
                .getAsJsonObject("mix").get("ring_3").getAsDouble());
        assertTrue(decode(head + ", \"mix\": {\"ring_3\": 1.0}}").isSuccess());
        assertTrue(decode(head + ", \"mix\": {\"void\": 0.5}}").isError(), "no boundary beyond the void");
        assertTrue(decode(head + ", \"mix\": {\"ring_3\": 0.0}}").isError());
        assertTrue(decode(head + ", \"mix\": {\"ring_3\": 1.5}}").isError());
        assertTrue(decode(head + ", \"switch_at\": [\"ring_3\"], \"mix\": {\"ring_3\": 0.5}}").isError(), "switch and mix at one boundary");
        assertTrue(decode(head + ", \"switch_at\": [\"ring_5\"], \"mix\": {\"ring_3\": 0.5}}").isSuccess());
        assertThrows(IllegalArgumentException.class,
                () -> new RingBlend(Level.OVERWORLD, constants(0, 0, 0, 0, 0, 0), Set.of(Ring.RING_3), Map.of(Ring.RING_3, 0.5)));
        assertThrows(IllegalArgumentException.class,
                () -> new RingBlend(Level.OVERWORLD, constants(0, 0, 0, 0, 0, 0), Set.of(), Map.of(Ring.RING_3, -0.1)));
    }

    /**
     * {@code ring_blend/nether_temperature}: vanilla's noise in ring 1, the region axes in ring 2 and 3, switching from
     * boundary 2→3 outwards. Over the whole zone 2→3 each side reads exactly its own region, so ring 2's biome (split at
     * the threshold) is the one its terrain ({@code region_weight}, same threshold) makes; zone 1→2 is change 35's blend.
     */
    @Test
    void netherTemperatureFollowsEachSidesOwnRegion() {
        double threshold = NetherRing2Features.REGION_THRESHOLD;
        double vanilla = 0.37;
        for (double region2 : new double[]{-0.9, -0.3, threshold - 0.01, threshold, threshold + 0.01, 0.3, 0.9}) {
            for (double region3 : new double[]{-0.8, 0.0, 0.8}) {
                RingBlend temperature = new RingBlend(Level.NETHER, constants(vanilla, region2, region3, 0, 0, 0),
                        Set.of(Ring.RING_2, Ring.RING_3, Ring.RING_4, Ring.RING_5));
                for (double blend = 0.0; blend <= 1.0; blend += 0.01) {
                    RingPosition position = new RingPosition(Ring.RING_2, Ring.RING_3, blend);
                    double value = temperature.compute(ANYWHERE, position);
                    assertEquals(position.ring() == Ring.RING_2 ? region2 : region3, value, 0.0, "blend " + blend);
                    if (position.ring() == Ring.RING_2) {
                        assertEquals(region2 >= threshold, value >= threshold);
                    }
                    assertEquals(vanilla + RingBlend.smoothstep(blend) * (region2 - vanilla),
                            temperature.compute(ANYWHERE, new RingPosition(Ring.RING_1, Ring.RING_2, blend)), 1e-12);
                }
            }
        }
    }

    private static Map<Ring, DensityFunction> constants(double... values) {
        Map<Ring, DensityFunction> rings = new EnumMap<>(Ring.class);
        for (Ring ring : Ring.values()) {
            rings.put(ring, DensityFunctions.constant(values[ring.ordinal()]));
        }
        return rings;
    }

    private static DataResult<RingBlend> decode(String json) {
        return RingBlend.MAP_CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }
}
