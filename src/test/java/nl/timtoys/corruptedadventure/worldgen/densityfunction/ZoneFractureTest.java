package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fracture zone 4→5 (85, V7-A, V8-A): exactly its argument outside the zone and on its calm edges; inside dense ground
 * with dry cracks 2–8 wide and 30–60 deep, crossings along every crack, raised floes, loose chunks, landmarks; the same
 * across the seam of the angle; pure.
 */
public class ZoneFractureTest {
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;
    private static final double START = 8000.0 - 128.0;
    private static final double END = 8000.0 + 128.0;

    /** A terrain-like argument with a different value at every block (noise in x, y and z). */
    private static final DensityFunction ARGUMENT = DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 2.0, -2.0),
            new DensityFunction.SimpleFunction() {
                private final DensityFunction.NoiseHolder noise = IslandFieldTest.noise("placeholder_terrain", IslandFieldTest.SEED);

                @Override
                public double compute(FunctionContext context) {
                    return noise.getValue(context.blockX(), context.blockY(), context.blockZ());
                }

                @Override
                public double minValue() {
                    return -noise.maxValue();
                }

                @Override
                public double maxValue() {
                    return noise.maxValue();
                }

                @Override
                public net.minecraft.util.KeyDispatchDataCodec<? extends DensityFunction> codec() {
                    throw new UnsupportedOperationException();
                }
            });

    public static ZoneFracture fracture(long seed) {
        RingLayout layout = seed == IslandFieldTest.SEED ? LAYOUT : new RingLayout(LAYOUT.settings(), RingLayout.noiseSeed(seed));
        return new ZoneFracture(ARGUMENT, IslandFieldTest.noise("zone_4_5_cells", seed), () -> layout);
    }

    private static final ZoneFracture FRACTURE = fracture(IslandFieldTest.SEED);

    /**
     * Bit-equal with and without the wrapper outside the zone 4→5 and on its calm edges (± 16 blocks), point by point and
     * per array; the strength is 0 exactly there.
     */
    @Test
    void identityOutsideTheZoneAndOnItsEdges() {
        int checked = 0;
        for (int distance = 7600; distance <= 8400; distance += 7) {
            for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 32.0) {
                int x = (int) Math.round(distance * Math.cos(angle));
                int z = (int) Math.round(distance * Math.sin(angle));
                double effective = LAYOUT.effectiveDistance(x, z);
                boolean calm = effective <= START + ZoneFractureShape.CALM || effective >= END - ZoneFractureShape.CALM;
                assertEquals(calm, ZoneFracture.strength(LAYOUT, effective) == 0.0, "strength at " + effective);
                if (!calm) {
                    continue;
                }
                checked++;
                for (int y = -64; y < 320; y += 11) {
                    DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(x, y, z);
                    assertEquals(Double.doubleToRawLongBits(ARGUMENT.compute(context)), Double.doubleToRawLongBits(FRACTURE.compute(context)));
                }
            }
        }
        assertTrue(checked > 1000);
        double[] expected = new double[48];
        double[] actual = new double[48];
        DensityFunction.ContextProvider column = new DensityFunction.ContextProvider() {
            @Override
            public DensityFunction.FunctionContext forIndex(int index) {
                return new DensityFunction.SinglePointContext(7700, -64 + 8 * index, 17);
            }

            @Override
            public void fillAllDirectly(double[] values, DensityFunction function) {
                for (int i = 0; i < values.length; i++) {
                    values[i] = function.compute(forIndex(i));
                }
            }
        };
        ARGUMENT.fillArray(expected, column);
        FRACTURE.fillArray(actual, column);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(Double.doubleToRawLongBits(expected[i]), Double.doubleToRawLongBits(actual[i]), "index " + i);
        }
    }

    /**
     * Dense ground (no crack) on ≥ 70 % of the zone on average and ≥ 60 % in every strip of 32 blocks across it; cracks
     * 2–8 wide at full strength; their floors 30–60 under the ground (here the fixed ground of a router without terrains),
     * open from the floor up and the argument below it.
     */
    @Test
    void mostlyDenseWithCracks2To8Wide30To60Deep() {
        int strips = (int) ((END - START) / 32.0);
        int[] dense = new int[strips];
        int[] all = new int[strips];
        List<Integer> runs = new ArrayList<>();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.003) {
            int run = 0;
            for (double r = START - 100.0; r <= END + 100.0; r += 1.0) {
                int x = (int) Math.floor(r * Math.cos(angle));
                int z = (int) Math.floor(r * Math.sin(angle));
                double effective = LAYOUT.effectiveDistance(x, z);
                ZoneFracture.Column c = FRACTURE.column(x, z);
                boolean crack = c.info.kind == ZoneFractureShape.CRACK;
                if (effective >= START && effective < END) {
                    int strip = Math.min(strips - 1, (int) ((effective - START) / 32.0));
                    all[strip]++;
                    dense[strip] += crack ? 0 : 1;
                }
                if (crack) {
                    double s = c.info.strength;
                    assertTrue(c.info.half >= ZoneFractureShape.MIN_HALF * s - 1e-9 && c.info.half <= ZoneFractureShape.MAX_HALF * s + 1e-9);
                    double depth = ZoneFracture.NO_TERRAIN_HEIGHT - c.floor;
                    assertTrue(depth >= 28.0 && depth <= 62.0, "depth " + depth);
                    assertTrue(FRACTURE.crackAt(x, c.floor, z) && !FRACTURE.crackAt(x, c.floor - 1, z));
                    if (Double.isNaN(c.info.chunkAbove)) {
                        assertEquals(-1.0, FRACTURE.compute(new DensityFunction.SinglePointContext(x, c.floor + 3, z)));
                    }
                    DensityFunction.SinglePointContext below = new DensityFunction.SinglePointContext(x, c.floor - 1, z);
                    assertEquals(ARGUMENT.compute(below), FRACTURE.compute(below));
                    run++;
                } else {
                    if (run > 0 && effective > START + 70.0 && effective < END - 70.0) {
                        runs.add(run);
                    }
                    run = 0;
                }
            }
        }
        double total = 0.0;
        StringBuilder shares = new StringBuilder();
        for (int i = 0; i < strips; i++) {
            double share = dense[i] / (double) all[i];
            shares.append(String.format(" %.3f", share));
            assertTrue(share >= 0.6, "strip " + i + ": " + share);
            total += share;
        }
        System.out.println("dense per strip:" + shares + ", runs " + runs.size());
        assertTrue(total / strips >= 0.7 && total / strips <= 0.85, "dense " + total / strips);
        int[] sorted = runs.stream().mapToInt(Integer::intValue).sorted().toArray();
        System.out.println("crack runs radially: median " + sorted[sorted.length / 2] + ", p10 " + sorted[sorted.length / 10]
                + ", p90 " + sorted[9 * sorted.length / 10]);
        int median = sorted[sorted.length / 2];
        assertTrue(median >= 2 && median <= 8, "median crack run " + median);
    }

    /** Along every crack a crossing every 24–34 blocks (never more than 40 apart), whatever the edge. */
    @Test
    void everyCrackHasACrossingWithin40Blocks() {
        for (long pair = 1; pair < 2000; pair++) {
            long hash = SeededHash.mix(pair * 7919L);
            double last = Double.NaN;
            double gap = 0.0;
            for (double along = -200.0; along <= 200.0; along += 0.25) {
                if (ZoneFractureShape.crossingAt(hash, along)) {
                    if (!Double.isNaN(last)) {
                        gap = Math.max(gap, along - last);
                    }
                    last = along;
                }
            }
            assertTrue(gap > 0.0 && gap <= 40.0, "gap " + gap);
        }
        // On the ground: crossings on cracks all round the zone.
        int crossings = 0;
        ZoneFractureShape shape = FRACTURE.shape();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.002) {
            int x = (int) Math.floor(8000.0 * Math.cos(angle));
            int z = (int) Math.floor(8000.0 * Math.sin(angle));
            ZoneFractureShape.Cell cell = shape.cell(x + 0.5, z + 0.5);
            crossings += cell.crossing() && cell.edge() < cell.half() ? 1 : 0;
        }
        assertTrue(crossings > 20, "crossings " + crossings);
    }

    /** Raised floes 8–30 up at their point, tilted up to 30°; loose chunks Ø 4–16 over the cracks, ≥ 3 thick, mostly outside. */
    @Test
    void floesAndChunksStayInTheirSizes() {
        int floes = 0;
        for (long cell = 0; cell < 5000; cell++) {
            double[] floe = ZoneFractureShape.floeOf(SeededHash.mix(cell * 104729L));
            if (floe != null) {
                floes++;
                assertTrue(floe[0] >= 8.0 && floe[0] <= 30.0 && floe[1] >= 0.0 && floe[1] <= 30.0);
            }
        }
        assertTrue(floes > 600 && floes < 1400, "floes " + floes);
        ZoneFractureShape shape = FRACTURE.shape();
        int inner = 0;
        int outer = 0;
        for (int i = -220; i <= 220; i++) {
            for (int j = -220; j <= 220; j++) {
                double[] chunk = shape.chunk(i, j);
                if (chunk == null) {
                    continue;
                }
                assertTrue(chunk[2] >= 2.0 && chunk[2] <= 8.0 && chunk[4] >= 3.0 && chunk[3] >= 4.0 && chunk[3] <= 16.0);
                ZoneFracture.Column c = FRACTURE.column((int) Math.floor(chunk[0]), (int) Math.floor(chunk[1]));
                assertEquals(ZoneFractureShape.CRACK, c.info.kind, "a chunk over a crack");
                double share = (LAYOUT.effectiveDistance(chunk[0], chunk[1]) - START) / (END - START);
                if (share >= 0.5) {
                    outer++;
                } else {
                    inner++;
                }
            }
        }
        System.out.println("chunks inner " + inner + ", outer " + outer);
        assertTrue(outer > 2 * inner && inner > 0, "inner " + inner + ", outer " + outer);
    }

    /** V8-A: a raised flat landmark every ~650 blocks along the zone (550–750 on average), top Ø ≥ 16, no crack on it. */
    @Test
    void landmarksEvery650AlongTheZone() {
        List<ZoneFracture.Landmark> landmarks = FRACTURE.landmarks();
        assertTrue(landmarks.size() >= 70 && landmarks.size() <= 85, "landmarks " + landmarks.size());
        double sum = 0.0;
        for (int k = 0; k < landmarks.size(); k++) {
            ZoneFracture.Landmark a = landmarks.get(k);
            ZoneFracture.Landmark b = landmarks.get((k + 1) % landmarks.size());
            double gap = Math.hypot(a.x() - b.x(), a.z() - b.z());
            assertTrue(gap >= 430.0 && gap <= 870.0, "gap " + gap);
            sum += gap;
            assertTrue(2 * a.radius() >= 16);
            assertTrue(a.topY() >= ZoneFracture.NO_TERRAIN_HEIGHT + 10 && a.topY() <= ZoneFracture.NO_TERRAIN_HEIGHT + 16);
            for (int dx = -a.radius() + 1; dx < a.radius(); dx++) {
                for (int dz = -a.radius() + 1; dz < a.radius(); dz++) {
                    if (dx * dx + dz * dz < (a.radius() - 1) * (a.radius() - 1)) {
                        ZoneFracture.Column c = FRACTURE.column(a.x() + dx, a.z() + dz);
                        assertTrue(c.info.kind != ZoneFractureShape.CRACK && c.extraTop == a.topY(), "flat top at " + dx + ", " + dz);
                    }
                }
            }
            assertTrue(FRACTURE.landmarksWithin(a.x() - 20, a.z() - 20, a.x() + 20, a.z() + 20).contains(a));
        }
        double mean = sum / landmarks.size();
        assertTrue(mean >= 550.0 && mean <= 750.0, "mean " + mean);
    }

    /** Lesson 63: the grid runs on across the seam of the angle (x &lt; 0, z = 0): no straight line of cracks there. */
    @Test
    void periodicAcrossTheSeam() {
        ZoneFractureShape shape = FRACTURE.shape();
        int same = 0;
        int total = 0;
        for (int x = -8100; x <= -7900; x++) {
            ZoneFractureShape.Cell north = shape.cell(x + 0.5, -0.5);
            ZoneFractureShape.Cell south = shape.cell(x + 0.5, 0.5);
            total++;
            same += north.i() == south.i() && north.j() == south.j() ? 1 : 0;
        }
        assertTrue(same > 0.85 * total, "same cell across the seam " + same + " of " + total);
    }

    /** Pure: the same seed gives the same fracture, a noise chunk's copy shares the decisions, another seed differs. */
    @Test
    void pureAndSharedByCopies() {
        ZoneFracture again = fracture(IslandFieldTest.SEED);
        ZoneFracture other = fracture(IslandFieldTest.SEED + 1);
        int differ = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.01) {
            int x = (int) Math.floor(8000.0 * Math.cos(angle));
            int z = (int) Math.floor(8000.0 * Math.sin(angle));
            assertEquals(FRACTURE.column(x, z).info.kind, again.column(x, z).info.kind);
            differ += FRACTURE.column(x, z).info.kind != other.column(x, z).info.kind ? 1 : 0;
        }
        assertTrue(differ > 10);
        ZoneFracture copy = (ZoneFracture) FRACTURE.mapAll(function -> function);
        assertSame(FRACTURE.shape(), copy.shape());
        assertEquals(FRACTURE, copy);
        // The column value (zone_4_5/overworld/fracture) is the same shape.
        ZoneFractureValue value = new ZoneFractureValue(IslandFieldTest.noise("zone_4_5_cells", IslandFieldTest.SEED), () -> LAYOUT);
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.01) {
            int x = (int) Math.floor(8020.0 * Math.cos(angle));
            int z = (int) Math.floor(8020.0 * Math.sin(angle));
            boolean crack = FRACTURE.column(x, z).info.kind == ZoneFractureShape.CRACK;
            assertEquals(crack, value.value(x, z) == ZoneFractureValue.CRACK);
        }
    }

    /** V8-A: ring 4's own terrain has no abyss; its ground in the zone stays at least Y 64 (the rim). */
    @Test
    void ring4KeepsOnlyItsRim() {
        IslandField islands = IslandFieldTest.field();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.02) {
            for (double r = START + 2.0; r < END - 2.0; r += 9.0) {
                int x = (int) Math.floor(r * Math.cos(angle));
                int z = (int) Math.floor(r * Math.sin(angle));
                if (Ring4Sundered.inRim(LAYOUT, LAYOUT.effectiveDistance(x, z))) {
                    assertTrue(islands.column(x, z).surface() >= Ring4Sundered.RIM_TOP, "rim at " + x + ", " + z);
                }
            }
        }
    }

    /**
     * {@code mapAll} hands the visitor on to the argument: a visitor that wraps the interpolated marker (as
     * {@code RingBlend} puts its gates there) reaches it, and the wrapper stays on top.
     */
    @Test
    void mapAllKeepsTheGatesInsideTheArgument() {
        DensityFunction marker = DensityFunctions.interpolated(ARGUMENT);
        List<DensityFunction> visited = new ArrayList<>();
        DensityFunction mapped = new ZoneFracture(marker, IslandFieldTest.noise("zone_4_5_cells", IslandFieldTest.SEED), () -> LAYOUT)
                .mapAll(function -> {
                    visited.add(function);
                    return function instanceof DensityFunctions.MarkerOrMarked ? DensityFunctions.flatCache(function) : function;
                });
        ZoneFracture wrapper = assertInstanceOf(ZoneFracture.class, mapped);
        assertTrue(wrapper.argument() instanceof DensityFunctions.MarkerOrMarked);
        assertTrue(visited.stream().anyMatch(f -> f instanceof DensityFunctions.MarkerOrMarked));
    }

    /**
     * 86c: {@code find} sees the fracture also inside the holder the wired router keeps it in (a random state's final
     * density is a {@code HolderHolder}) and under a marker, and nothing in another function.
     */
    @Test
    void findUnwrapsTheRoutersHolder() {
        assertSame(FRACTURE, ZoneFracture.find(FRACTURE));
        assertSame(FRACTURE, ZoneFracture.find(new DensityFunctions.HolderHolder(Holder.direct(FRACTURE))));
        assertSame(FRACTURE, ZoneFracture.find(DensityFunctions.interpolated(new DensityFunctions.HolderHolder(Holder.direct(FRACTURE)))));
        assertNull(ZoneFracture.find(ARGUMENT));
    }

    /** The overworld router: {@code final_density} is the zone fracture around the ring blend; the column value its own DF. */
    @Test
    void overworldFinalDensityIsTheFractureAroundTheRingBlend() {
        var json = WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("noise_router").getAsJsonObject("final_density");
        assertEquals("corruptedadventure:zone_fracture", json.get("type").getAsString());
        assertEquals("corruptedadventure:ring_blend/overworld", json.get("argument").getAsString());
        assertEquals("corruptedadventure:zone_4_5_cells", json.get("hash").getAsString());
        var value = WorldgenData.read("density_function/zone_4_5/overworld/fracture.json");
        assertEquals("minecraft:flat_cache", value.get("type").getAsString());
        assertEquals("corruptedadventure:zone_fracture_value", value.getAsJsonObject("argument").get("type").getAsString());
        var sundered = WorldgenData.read("density_function/ring_4/overworld/sundered.json");
        assertEquals("corruptedadventure:zone_4_5/overworld/fracture", sundered.get("argument2").getAsString());
    }
}
