package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.worldgen.feature.DewGeodeShape;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing2Features;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DewGeodesTest {
    private static final DewGeodeShape.Settings SETTINGS = NetherRing2Features.GEODE_SETTINGS;
    private static final DensityFunction SOLID = DensityFunctions.constant(1.0);
    private static final DensityFunction AIR = DensityFunctions.constant(-1.0);
    private static final DensityFunction INSIDE = DensityFunctions.constant(0.0);
    private static final DensityFunction OUTSIDE = DensityFunctions.constant(1.0);
    private static final DensityFunction FOREST = DensityFunctions.constant(NetherRing2Features.REGION_THRESHOLD + 0.2);
    private static final DensityFunction CAVES = DensityFunctions.constant(NetherRing2Features.REGION_THRESHOLD - 0.2);

    private static DensityFunction.NoiseHolder noise() {
        return TestFunctions.noise(42L, -4, 1.0);
    }

    private static DewGeodes geodes(DensityFunction terrain, DensityFunction edge, DensityFunction region, DensityFunction.NoiseHolder noise) {
        return new DewGeodes(terrain, edge, region, noise, SETTINGS);
    }

    private static DewGeodeShape.CellHash hash(DensityFunction.NoiseHolder noise) {
        return (x, z, salt) -> SeededHash.hash(noise, x, z, salt);
    }

    /** The geodes the density function itself finds in 16 x 16 cells (the first attempt that passes its tests). */
    private static List<DewGeodeShape.Geode> candidates(DewGeodes function) {
        List<DewGeodeShape.Geode> found = new ArrayList<>();
        for (int cx = -8; cx < 8; cx++) {
            for (int cz = -8; cz < 8; cz++) {
                DewGeodeShape.Geode geode = function.geodeOfCell(cx, cz);
                if (geode != null) {
                    found.add(geode);
                }
            }
        }
        return found;
    }

    private static double at(DensityFunction function, int x, int y, int z) {
        return function.compute(new DensityFunction.SinglePointContext(x, y, z));
    }

    @Test
    void cavityIsMinusOneInsideAndNeutralOutside() {
        DensityFunction.NoiseHolder noise = noise();
        DewGeodes function = geodes(SOLID, INSIDE, FOREST, noise);
        List<DewGeodeShape.Geode> geodes = candidates(function);
        assertTrue(geodes.size() > 50, "candidates " + geodes.size());
        for (DewGeodeShape.Geode geode : geodes) {
            assertEquals(-1.0, at(function, geode.x(), geode.y(), geode.z()), "centre " + geode);
            assertEquals(10.0, at(function, geode.x(), geode.y() + geode.radius() + 3, geode.z()), "above " + geode);
            assertEquals(10.0, at(function, geode.x() + geode.reach() + 1, geode.y(), geode.z()), "beside " + geode);
        }
    }

    @Test
    void noCavityWhereTheTerrainIsAirOrNearARingBorder() {
        DensityFunction.NoiseHolder noise = noise();
        for (DewGeodeShape.Geode geode : candidates(geodes(SOLID, INSIDE, FOREST, noise))) {
            assertEquals(10.0, at(geodes(AIR, INSIDE, FOREST, noise), geode.x(), geode.y(), geode.z()), "air");
            assertEquals(10.0, at(geodes(SOLID, OUTSIDE, FOREST, noise), geode.x(), geode.y(), geode.z()), "ring edge");
        }
    }

    @Test
    void anAirPocketOnTheWallTestRingRulesThatAttemptOut() {
        DensityFunction.NoiseHolder noise = noise();
        DewGeodes solid = geodes(SOLID, INSIDE, FOREST, noise);
        DewGeodeShape.Geode geode = candidates(solid).get(0);
        // Solid everywhere except one wall test point: that attempt is not enclosed any more (another of the cell may be).
        int[] point = geode.wallTestPoints(SETTINGS.shell(), 0).get(5);
        DensityFunction pocket = TestFunctions.of((x, y, z) -> x == point[0] && y == point[1] && z == point[2] ? -1.0 : 1.0);
        DewGeodeShape.Geode other = geodes(pocket, INSIDE, FOREST, noise).geodeOfCell(Math.floorDiv(geode.x(), SETTINGS.cellSize()), Math.floorDiv(geode.z(), SETTINGS.cellSize()));
        assertNotEquals(geode, other);
    }

    @Test
    void aCellFallsBackToALaterAttemptWhenTheFirstIsInAHall() {
        DensityFunction.NoiseHolder noise = noise();
        DewGeodes anywhere = geodes(SOLID, INSIDE, FOREST, noise);
        DewGeodeShape.Geode first = candidates(anywhere).get(0);
        int cellX = Math.floorDiv(first.x(), SETTINGS.cellSize());
        int cellZ = Math.floorDiv(first.z(), SETTINGS.cellSize());
        // Air around the first attempt's centre only: the cell takes the next attempt that is solid.
        DensityFunction hall = TestFunctions.of((x, y, z) -> first.distance(x, y, z) < first.radius() + 2.0 ? -1.0 : 1.0);
        DewGeodeShape.Geode next = geodes(hall, INSIDE, FOREST, noise).geodeOfCell(cellX, cellZ);
        assertTrue(next != null && !next.equals(first), "falls back to another attempt: " + next);
    }

    @Test
    void neverBelowTheLavaSeaAndRegimeFollowsTheRegion() {
        DensityFunction.NoiseHolder noise = noise();
        for (DewGeodeShape.Geode geode : candidates(geodes(SOLID, INSIDE, CAVES, noise))) {
            assertTrue(!geode.forest());
            assertTrue(geode.y() - geode.radius() * 1.15 > SETTINGS.bottom());
            assertTrue(geode.radius() <= SETTINGS.radiusMaxCaves());
        }
        // Where nothing but the cavity is free, no block under the sea floor is ever a cavity.
        DewGeodes function = geodes(SOLID, INSIDE, CAVES, noise);
        for (int x = -256; x < 256; x += 3) {
            for (int z = -256; z < 256; z += 3) {
                for (int y = 0; y <= SETTINGS.bottom(); y += 2) {
                    assertEquals(10.0, at(function, x, y, z));
                }
            }
        }
    }

    @Test
    void samePositionSameValueInANewInstanceAndAfterMapAll() {
        DensityFunction.NoiseHolder noise = noise();
        DewGeodes first = geodes(SOLID, INSIDE, FOREST, noise);
        DensityFunction mapped = first.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder holder) {
                return holder;
            }
        });
        DewGeodes second = geodes(SOLID, INSIDE, FOREST, noise);
        for (int x = -200; x < 200; x += 5) {
            for (int z = -200; z < 200; z += 5) {
                for (int y = 26; y < 100; y += 6) {
                    double expected = at(first, x, y, z);
                    assertEquals(expected, at(second, x, y, z));
                    assertEquals(expected, at(mapped, x, y, z));
                }
            }
        }
    }

    @Test
    void sameValuesOnTwoThreads() throws Exception {
        DensityFunction.NoiseHolder noise = noise();
        DewGeodes shared = geodes(SOLID, INSIDE, FOREST, noise);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<double[]>> runs = new ArrayList<>();
            for (int t = 0; t < 2; t++) {
                runs.add(pool.submit(() -> {
                    double[] values = new double[40 * 40];
                    int i = 0;
                    for (int x = -100; x < 100; x += 5) {
                        for (int z = -100; z < 100; z += 5) {
                            values[i++] = at(shared, x, 60, z);
                        }
                    }
                    return values;
                }));
            }
            assertTrue(java.util.Arrays.equals(runs.get(0).get(), runs.get(1).get()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rangeCoversBothValues() {
        DewGeodes function = geodes(SOLID, INSIDE, FOREST, noise());
        assertEquals(-1.0, function.minValue());
        assertEquals(10.0, function.maxValue());
    }
}
