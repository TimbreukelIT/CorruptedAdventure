package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockPillarsTest {
    private static final int CELL = 24;
    private static final int MARGIN = 1;
    private static final DensityFunction.NoiseHolder HASH = TestFunctions.noise(42L, 0, 1.0);
    /** Gently rolling floor (at most ~1 block per block). */
    private static final DensityFunction FLOOR = TestFunctions.of((x, y, z) -> 100.0 + 3.0 * Math.sin(x / 9.0) + 2.0 * Math.cos(z / 7.0));

    private static final Shape NEEDLE = new Shape.Needle(new Range(8, 20), new Range(1.5, 3), new Range(0.7, 1.5), new Range(0.5, 1.5), 0.25,
            Optional.empty());
    private static final Shape CAPPED = new Shape.Needle(new Range(8, 20), new Range(1.5, 3), new Range(0.7, 1.5), new Range(0, 1), 0.3,
            Optional.of(new Shape.Cap(1.0, new Range(1, 2), new Range(2, 3))));
    private static final Shape BOULDER = new Shape.Boulder(new Range(2, 4), new Range(0.6, 0.9), new Range(0.3, 0.5));
    private static final Shape FIN = new Shape.Fin(new Range(12, 30), new Range(2, 4), new Range(6, 18), Shape.Orientation.ALIGNED, 30.0, 300.0,
            Optional.empty());
    private static final Shape ARCH = new Shape.Fin(new Range(12, 30), new Range(2, 4), new Range(10, 18), Shape.Orientation.RANDOM, 25.0, 256.0,
            Optional.of(new Shape.Hole(1.0, new Range(0.3, 0.5), new Range(0.4, 0.6))));

    private static RockPillars pillars(Shape shape, double weight) {
        return new RockPillars(HASH, CELL, 1.0, MARGIN, DensityFunctions.constant(weight), 0.25, FLOOR, Optional.empty(), 0.0, 1024.0,
                Optional.empty(), List.of(new RockPillars.WeightedShape(1, shape)));
    }

    @Test
    void shapesNeverFloatStayInTheirCellAndKeepTheirHeight() {
        int arches = 0;
        for (Shape shape : List.of(NEEDLE, CAPPED, BOULDER, FIN, ARCH)) {
            for (double weight : new double[]{0.3, 0.6, 1.0}) {
                RockPillars rocks = pillars(shape, weight);
                for (int n = 0; n < 2_000; n++) {
                    int i = n % 50 - 25;
                    int j = n / 50 - 20;
                    RockPillars.Placed placed = rocks.place(i, j);
                    assertNotNull(placed, "chance 1 places a shape in every cell");
                    arches += checkCell(rocks, placed, i, j, shape, weight) ? 1 : 0;
                }
            }
        }
        assertTrue(arches > 100, "arches: " + arches);
    }

    /** Flood fill from the base of every column; returns whether the cell holds an arch (a column with a gap). */
    private static boolean checkCell(RockPillars rocks, RockPillars.Placed placed, int i, int j, Shape shape, double weight) {
        Set<Long> solid = new HashSet<>();
        ArrayDeque<long[]> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        int top = Integer.MIN_VALUE;
        boolean gap = false;
        for (int x = i * CELL - 2; x < (i + 1) * CELL + 2; x++) {
            for (int z = j * CELL - 2; z < (j + 1) * CELL + 2; z++) {
                int[] spans = rocks.solidAt(x, z);
                if (spans.length == 0) {
                    continue;
                }
                assertTrue(x >= i * CELL + MARGIN && x < (i + 1) * CELL - MARGIN && z >= j * CELL + MARGIN && z < (j + 1) * CELL - MARGIN,
                        shape.type() + " outside its cell at " + x + "," + z);
                if (shape != CAPPED) {
                    // Only the overhang of a cap starts higher (it hangs on the stem; the flood fill checks that).
                    assertEquals(placed.base(), spans[0], "every column starts at the base");
                }
                gap |= spans.length > 2 && shape != CAPPED;
                for (int s = 0; s < spans.length; s += 2) {
                    for (int y = spans[s]; y <= spans[s + 1]; y++) {
                        solid.add(key(x, y, z));
                    }
                    top = Math.max(top, spans[s + 1]);
                }
                long base = key(x, placed.base(), z);
                if (solid.contains(base) && seen.add(base)) {
                    queue.add(new long[]{x, placed.base(), z});
                }
            }
        }
        while (!queue.isEmpty()) {
            long[] p = queue.poll();
            for (int[] d : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                long k = key((int) p[0] + d[0], (int) p[1] + d[1], (int) p[2] + d[2]);
                if (solid.contains(k) && seen.add(k)) {
                    queue.add(new long[]{p[0] + d[0], p[1] + d[1], p[2] + d[2]});
                }
            }
        }
        assertEquals(solid.size(), seen.size(), shape.type() + " has blocks without a connection to its base in cell " + i + "," + j);
        double maxHeight = switch (shape) {
            case Shape.Needle needle -> needle.height().max() * weight;
            case Shape.Fin fin -> fin.height().max() * weight;
            case Shape.Boulder boulder -> boulder.radius().max() * 2;
        };
        assertTrue(top - placed.floor() <= maxHeight + 1, shape.type() + " too high: " + (top - placed.floor()));
        if (gap) {
            // The hole is open above the highest floor of the footprint, and its legs reach the base.
            assertTrue(placed.draw() instanceof Shape.FinDraw fin && fin.hasHole(placed));
        }
        return gap;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0xFFFFF) << 40 | ((long) y & 0xFFFFF) << 20 | (long) z & 0xFFFFF;
    }

    @Test
    void weightExcludeAndFloorStepDropCells() {
        RockPillars low = new RockPillars(HASH, CELL, 1.0, MARGIN, DensityFunctions.constant(0.2), 0.3, FLOOR, Optional.empty(), 0.0, 1024.0,
                Optional.empty(), List.of(new RockPillars.WeightedShape(1, NEEDLE)));
        RockPillars excluded = new RockPillars(HASH, CELL, 1.0, MARGIN, DensityFunctions.constant(1.0), 0.3, FLOOR,
                Optional.of(DensityFunctions.constant(5.0)), 3.0, 1024.0, Optional.empty(), List.of(new RockPillars.WeightedShape(1, NEEDLE)));
        DensityFunction steep = TestFunctions.of((x, y, z) -> 100.0 + 2.0 * x);
        RockPillars onAWall = new RockPillars(HASH, CELL, 1.0, MARGIN, DensityFunctions.constant(1.0), 0.3, steep, Optional.empty(), 0.0, 4.0,
                Optional.empty(), List.of(new RockPillars.WeightedShape(1, BOULDER)));
        RockPillars none = new RockPillars(HASH, CELL, 0.0, MARGIN, DensityFunctions.constant(1.0), 0.3, FLOOR, Optional.empty(), 0.0, 1024.0,
                Optional.empty(), List.of(new RockPillars.WeightedShape(1, BOULDER)));
        for (int i = -10; i < 10; i++) {
            for (int j = -10; j < 10; j++) {
                assertNull(low.place(i, j));
                assertNull(excluded.place(i, j));
                assertNull(onAWall.place(i, j));
                assertNull(none.place(i, j));
            }
        }
        RockPillars far = new RockPillars(HASH, CELL, 1.0, MARGIN, DensityFunctions.constant(1.0), 0.3, FLOOR,
                Optional.of(DensityFunctions.constant(50.0)), 3.0, 1024.0, Optional.empty(), List.of(new RockPillars.WeightedShape(1, NEEDLE)));
        assertNotNull(far.place(3, 4));
    }

    @Test
    void rootRunsTheColumnsDeeper() {
        RockPillars rooted = new RockPillars(HASH, CELL, 1.0, MARGIN, DensityFunctions.constant(1.0), 0.3, FLOOR, Optional.empty(), 0.0, 1024.0,
                Optional.of(50), List.of(new RockPillars.WeightedShape(1, BOULDER)));
        RockPillars.Placed placed = rooted.place(2, -3);
        assertEquals(50, placed.base());
        assertEquals(placed.floor() - 2, pillars(BOULDER, 1.0).place(2, -3).base());
    }

    @Test
    void pureAcrossThreadsAndInstances() throws Exception {
        RockPillars rocks = pillars(FIN, 1.0);
        DensityFunction mapped = rocks.mapAll(f -> f);
        int[][] expected = new int[64][];
        for (int n = 0; n < 64; n++) {
            expected[n] = rocks.solidAt(n * 7, -n * 5);
        }
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = pool.submit(() -> check(rocks, expected));
            Future<?> b = pool.submit(() -> check((RockPillars) mapped, expected));
            a.get();
            b.get();
        } finally {
            pool.shutdown();
        }
        assertEquals(rocks.compute(new DensityFunction.SinglePointContext(30, 105, 30)),
                mapped.compute(new DensityFunction.SinglePointContext(30, 105, 30)));
    }

    private static void check(RockPillars rocks, int[][] expected) {
        for (int round = 0; round < 50; round++) {
            for (int n = 63; n >= 0; n--) {
                assertTrue(java.util.Arrays.equals(expected[n], rocks.solidAt(n * 7, -n * 5)));
            }
        }
    }

    @Test
    void codecRoundTripAndValidation() {
        String json = """
                {"hash": {"firstOctave": 0, "amplitudes": [1.0]}, "cell_size": 24, "chance": 0.35, "weight": 1.0, "floor": 100.0,
                 "exclude": 30.0, "exclude_below": 6.0, "max_floor_step": 3.0, "root_y": 50,
                 "shapes": [
                   {"weight": 35, "shape": {"type": "needle", "height": [8, 20], "base_radius": [1.5, 3.0], "top_radius": [0.7, 1.5],
                                            "cap": {"chance": 0.5, "extra_radius": [1, 2], "thickness": [2, 3]}}},
                   {"weight": 35, "shape": {"type": "boulder", "radius": [2, 4]}},
                   {"weight": 30, "shape": {"type": "fin", "length": [12, 30], "thickness": [2, 4], "height": [6, 18],
                                            "orientation": "aligned", "hole": {"chance": 0.2, "width": [0.3, 0.5], "height": [0.4, 0.6]}}}]}""";
        JsonElement parsed = JsonParser.parseString(json);
        RockPillars rocks = RockPillars.MAP_CODEC.codec().parse(JsonOps.INSTANCE, parsed).getOrThrow();
        assertEquals(24, rocks.cellSize());
        JsonElement encoded = RockPillars.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, rocks).getOrThrow();
        RockPillars again = RockPillars.MAP_CODEC.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(encoded, RockPillars.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, again).getOrThrow());
        assertEquals(-1.0, rocks.minValue());
        assertEquals(1.0, rocks.maxValue());

        assertError(json.replace("\"height\": [8, 20]", "\"height\": [20, 8]"));
        assertError(json.replace("\"chance\": 0.35", "\"chance\": 1.5"));
        assertError(json.replace("\"cell_size\": 24", "\"cell_size\": 5"));
        assertError(json.replace("\"type\": \"boulder\"", "\"type\": \"cube\""));
        JsonObject noShapes = parsed.getAsJsonObject().deepCopy();
        noShapes.add("shapes", JsonParser.parseString("[]"));
        assertTrue(RockPillars.MAP_CODEC.codec().parse(JsonOps.INSTANCE, noShapes).error().isPresent());
    }

    private static void assertError(String json) {
        assertTrue(RockPillars.MAP_CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent(), json);
    }

    @Test
    void outsideAnyShapeTheDensityIsMinusOne() {
        RockPillars rocks = pillars(NEEDLE, 1.0);
        RockPillars.Placed placed = rocks.place(0, 0);
        assertEquals(1.0, rocks.compute(new DensityFunction.SinglePointContext((int) placed.x(), placed.floor(), (int) placed.z())));
        assertEquals(-1.0, rocks.compute(new DensityFunction.SinglePointContext((int) placed.x(), placed.floor() + 40, (int) placed.z())));
    }
}
