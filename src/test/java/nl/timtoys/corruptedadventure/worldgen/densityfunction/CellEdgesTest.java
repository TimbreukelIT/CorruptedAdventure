package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellEdgesTest {
    private static final DensityFunction.NoiseHolder WARP = TestFunctions.noise(7L, -7, 1.0, 0.5);
    private static final DensityFunction.NoiseHolder HASH = TestFunctions.noise(11L, 0, 1.0);

    private static CellEdges edges(double warpAmplitude, double openChance) {
        return new CellEdges(110.0, 0.8, openChance, WARP, warpAmplitude, HASH, 24.0);
    }

    /** The site of cell (i, j). */
    private static double[] site(CellEdges e, int i, int j) {
        long h = e.siteHash(i, j);
        return new double[]{e.siteX(i, h), e.siteZ(j, h)};
    }

    @Test
    void distanceIsTheExactDistanceToTheBisectorOnAnEdge() {
        CellEdges e = edges(0.0, 1.0);
        Random random = new Random(3);
        int checked = 0;
        for (int n = 0; n < 4_000 && checked < 500; n++) {
            int i = random.nextInt(40) - 20;
            int j = random.nextInt(40) - 20;
            int[] d = random.nextBoolean() ? new int[]{1, 0} : new int[]{0, 1};
            double[] a = site(e, i, j);
            double[] b = site(e, i + d[0], j + d[1]);
            double mx = (a[0] + b[0]) / 2;
            double mz = (a[1] + b[1]) / 2;
            double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
            double nx = (b[0] - a[0]) / len;
            double nz = (b[1] - a[1]) / len;
            double t = random.nextDouble() * 5.0;
            double qx = mx + nx * t;
            double qz = mz + nz * t;
            // Only where a and b are the two nearest sites to the foot point (the midpoint lies on their common edge)
            // with room to spare, so the nearest open edge is this one.
            if (!secondNearestBy(e, mx, mz, a, b, 30.0)) {
                continue;
            }
            assertEquals(t, e.nearestOpenEdge(qx, qz).distance(), 0.05, "at " + qx + "," + qz);
            checked++;
        }
        assertTrue(checked > 100, "checked " + checked);
    }

    /** Whether a and b are the nearest two sites to (x, z) and every other site is at least {@code margin} farther. */
    private static boolean secondNearestBy(CellEdges e, double x, double z, double[] a, double[] b, double margin) {
        double ab = Math.max(Math.hypot(x - a[0], z - a[1]), Math.hypot(x - b[0], z - b[1]));
        int ci = (int) Math.floor(x / e.cellSize());
        int cj = (int) Math.floor(z / e.cellSize());
        for (int i = ci - 3; i <= ci + 3; i++) {
            for (int j = cj - 3; j <= cj + 3; j++) {
                double[] s = site(e, i, j);
                if ((s[0] == a[0] && s[1] == a[1]) || (s[0] == b[0] && s[1] == b[1])) {
                    continue;
                }
                if (Math.hypot(x - s[0], z - s[1]) < ab + margin) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void continuousAlsoWithWarp() {
        CellEdges e = edges(14.0, 0.6);
        for (int z = -2_000; z < 2_000; z += 97) {
            double previous = e.compute(new DensityFunction.SinglePointContext(-2_000, 0, z));
            for (int x = -1_999; x < 2_000; x++) {
                double d = e.compute(new DensityFunction.SinglePointContext(x, 0, z));
                // The distance is in blocks up to a first-order correction for the warp's stretch (0.6–1.45 with these
                // settings): where the stretch changes, the distance tilts a little (near a fracture, where slots are
                // cut, by at most ~25 %), but it never jumps.
                double limit = d < 5.0 && previous < 5.0 ? 1.3 : 1.6;
                assertTrue(Math.abs(d - previous) <= limit || d >= 23.0 || previous >= 23.0,
                        "jump " + previous + " -> " + d + " at " + x + "," + z);
                previous = d;
            }
        }
    }

    @Test
    void aboutSixtyPercentOfTheEdgesAreOpen() {
        CellEdges e = edges(0.0, 0.6);
        int open = 0;
        int total = 0;
        for (int i = -40; i < 40; i++) {
            for (int j = -32; j < 32; j++) {
                long a = e.siteHash(i, j);
                long b = e.siteHash(i + 1, j);
                open += e.isOpen(CellEdges.edgeSeed(a, b)) ? 1 : 0;
                total++;
            }
        }
        assertTrue(total >= 5_000);
        assertEquals(0.6, open / (double) total, 0.05);
    }

    @Test
    void sameSeedSameOutputAcrossThreadsAndInstances() throws Exception {
        CellEdges e = edges(14.0, 0.6);
        DensityFunction mapped = e.mapAll(f -> f);
        double[] expected = new double[500];
        for (int n = 0; n < expected.length; n++) {
            expected[n] = e.compute(new DensityFunction.SinglePointContext(n * 13 - 3_000, 0, n * 7 - 1_000));
        }
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = pool.submit(() -> check(e, expected));
            Future<?> b = pool.submit(() -> check(mapped, expected));
            a.get();
            b.get();
        } finally {
            pool.shutdown();
        }
    }

    private static void check(DensityFunction f, double[] expected) {
        for (int round = 0; round < 20; round++) {
            for (int n = expected.length - 1; n >= 0; n--) {
                assertEquals(expected[n], f.compute(new DensityFunction.SinglePointContext(n * 13 - 3_000, 0, n * 7 - 1_000)));
            }
        }
    }

    @Test
    void codecRoundTripAndValidation() {
        String json = """
                {"cell_size": 110.0, "jitter": 0.8, "open_chance": 0.6, "warp": {"firstOctave": -7, "amplitudes": [1.0, 0.5]},
                 "warp_amplitude": 14.0, "hash": {"firstOctave": 0, "amplitudes": [1.0]}, "max_distance": 24.0}""";
        CellEdges e = CellEdges.MAP_CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        assertEquals(110.0, e.cellSize());
        assertEquals(24.0, e.maxValue());
        assertEquals(0.0, e.minValue());
        JsonElement encoded = CellEdges.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, e).getOrThrow();
        CellEdges again = CellEdges.MAP_CODEC.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(encoded, CellEdges.MAP_CODEC.codec().encodeStart(JsonOps.INSTANCE, again).getOrThrow());
        assertEquals(0.6, encoded.getAsJsonObject().get("open_chance").getAsDouble());
        for (String bad : new String[]{json.replace("\"cell_size\": 110.0", "\"cell_size\": 16.0"),
                json.replace("\"open_chance\": 0.6", "\"open_chance\": 1.2"),
                json.replace("\"max_distance\": 24.0", "\"max_distance\": 40.0")}) {
            assertTrue(CellEdges.MAP_CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(bad)).error().isPresent(), bad);
        }
    }
}
