package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The base terrain of ring 4 (package 60) on the test seed: low, about half water, landforms per kind of region. */
class Ring4ReliefTest {
    private static IslandField field;
    /** Columns across ring 4 proper: x, z, surface, water axis, growth axis, band, river. */
    private static final List<double[]> COLUMNS = new ArrayList<>();

    @BeforeAll
    static void sample() {
        field = IslandFieldTest.field();
        Ring4Relief relief = field.relief();
        Ring4Band band = Ring4Band.of(IslandFieldTest.LAYOUT);
        for (int angle = 0; angle < 360; angle += 3) {
            double cos = Math.cos(Math.toRadians(angle));
            double sin = Math.sin(Math.toRadians(angle));
            for (int r = 6100; r <= 7900; r += 29) {
                int x = (int) Math.round(r * cos);
                int z = (int) Math.round(r * sin);
                var position = IslandFieldTest.LAYOUT.sampleOverworld(x, z);
                if (position.inTransition() || position.ring() != Ring.RING_4) {
                    continue;
                }
                IslandField.Column c = field.column(x, z);
                COLUMNS.add(new double[]{x, z, c.surface(), relief.waterAxis(x, z), relief.growthAxis(x, z),
                        band.value(x, z), c.river});
            }
        }
    }

    private static double waterShare(java.util.function.Predicate<double[]> filter) {
        int water = 0;
        int all = 0;
        for (double[] c : COLUMNS) {
            if (filter.test(c)) {
                all++;
                water += c[2] < Ring4Relief.SEA ? 1 : 0;
            }
        }
        return all == 0 ? Double.NaN : water / (double) all;
    }

    @Test
    void aboutHalfIsWater() {
        double all = waterShare(c -> true);
        double mythic = waterShare(c -> c[5] < 0.0);
        double blighted = waterShare(c -> c[5] >= 0.0);
        System.out.printf("water share: all %.3f, mythic %.3f, blighted %.3f (%d columns)%n", all, mythic, blighted, COLUMNS.size());
        double[] cuts = {-0.45, -0.3, -0.15, -0.05, 0.0, 0.1, 0.2, 0.3, 0.45};
        StringBuilder axis = new StringBuilder("water axis below:");
        StringBuilder growth = new StringBuilder("growth axis below:");
        for (double cut : cuts) {
            axis.append(String.format(" %.2f=%.3f", cut, COLUMNS.stream().filter(c -> c[3] < cut).count() / (double) COLUMNS.size()));
            growth.append(String.format(" %.2f=%.3f", cut, COLUMNS.stream().filter(c -> c[4] < cut).count() / (double) COLUMNS.size()));
        }
        System.out.println(axis);
        System.out.println(growth);
        for (double[] range : new double[][]{{-9, -0.15}, {-0.15, 0.2}, {0.2, 9}}) {
            System.out.printf("water share for axis %.2f..%.2f: mythic %.3f, blighted %.3f%n", range[0], range[1],
                    waterShare(c -> c[3] >= range[0] && c[3] < range[1] && c[5] < 0), waterShare(c -> c[3] >= range[0] && c[3] < range[1] && c[5] >= 0));
        }
        assertTrue(all > 0.42 && all < 0.58, "water share " + all);
        assertTrue(mythic > 0.35 && mythic < 0.65, "mythic " + mythic);
        assertTrue(blighted > 0.35 && blighted < 0.65, "blighted " + blighted);
    }

    @Test
    void lowAroundTheSeaWithHighlandsAndDeepChannels() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        int high = 0;
        for (double[] c : COLUMNS) {
            min = Math.min(min, c[2]);
            max = Math.max(max, c[2]);
            high += c[2] > 85.0 ? 1 : 0;
        }
        System.out.printf("surface %.1f .. %.1f, above 85: %.3f%n", min, max, high / (double) COLUMNS.size());
        assertTrue(min > 30.0, "min " + min);
        assertTrue(max < 125.0 && max > 90.0, "max " + max);
        double highShare = high / (double) COLUMNS.size();
        assertTrue(highShare > 0.02 && highShare < 0.3, "highlands " + highShare);
    }

    /**
     * Each landform under its biome (W-biomes' thresholds): open water in the lagoon, the lotus shallows and the
     * drowned forest; land in the cascades, the fjord plateaus and the peat; the swamp and the reed marsh around the sea.
     */
    @Test
    void landformsFollowTheBiomeThresholds() {
        double b = Ring4Relief.BLEND;
        double split = Ring4Relief.WATER_SPLIT;
        double growth = Ring4Relief.MYTHIC_GROWTH;
        java.util.function.Predicate<double[]> mythic = c -> c[5] <= -1.0;
        java.util.function.Predicate<double[]> blighted = c -> c[5] >= 0.5;
        double lagoon = waterShare(mythic.and(c -> c[4] < growth - b && c[3] > b));
        double lotus = waterShare(mythic.and(c -> c[4] > growth + b && c[3] > split + b));
        double swamp = waterShare(mythic.and(c -> c[4] > growth + b && Math.abs(c[3]) < split - b));
        double cascades = waterShare(mythic.and(c -> c[4] > growth + b && c[3] < -split - b && c[6] < 0.01));
        double fjords = waterShare(mythic.and(c -> c[4] < growth - b && c[3] < -b && c[6] < 0.01));
        double drowned = waterShare(blighted.and(c -> c[3] > split + b));
        double reed = waterShare(blighted.and(c -> c[3] < split - b && c[4] > b));
        double peat = waterShare(blighted.and(c -> c[3] < split - b && c[4] < -b && c[6] < 0.01));
        System.out.printf("water share per biome: lagoon %.2f lotus %.2f swamp %.2f cascades %.2f fjords %.2f drowned %.2f reed %.2f peat %.2f%n",
                lagoon, lotus, swamp, cascades, fjords, drowned, reed, peat);
        assertTrue(lagoon > 0.9 && lotus > 0.9 && drowned > 0.9, "open water");
        assertTrue(cascades < 0.1 && fjords < 0.35 && peat < 0.2, "land");
        assertTrue(swamp > 0.25 && swamp < 0.75, "swamp " + swamp);
        assertTrue(reed > 0.2 && reed < 0.75, "reed " + reed);
    }

    /** The mythical half of the band's transition (always mist_fjords) is never a lagoon: a low plateau. */
    @Test
    void theMistRimIsNoLagoon() {
        int rim = 0;
        int deep = 0;
        for (double[] c : COLUMNS) {
            if (c[5] > -0.5 && c[5] < -0.1 && c[6] < 0.01) {
                rim++;
                deep += c[2] < 55.0 && field.column((int) c[0], (int) c[1]).fjord < 0.01 ? 1 : 0;
            }
        }
        assertTrue(rim > 5, "rim columns " + rim);
        assertTrue(deep <= rim / 10, deep + " of " + rim);
    }

    /** Rivers lie at the sea level: their beds below it, so they hold the sea and can be sailed. */
    @Test
    void riverBedsLieBelowTheSea() {
        int rivers = 0;
        for (double[] c : COLUMNS) {
            if (c[6] > 0.999) {
                rivers++;
                assertTrue(c[2] <= Ring4Relief.RIVER_BED + 0.1, "river at " + c[0] + ", " + c[1] + ": " + c[2]);
            }
        }
        assertTrue(rivers > 20, "rivers " + rivers);
    }

    @Test
    void terracesHaveFlatTreadsAndSteepRisers() {
        assertEquals(0.0, Ring4Relief.terrace(0.0));
        assertEquals(6.0, Ring4Relief.terrace(6.0), 1e-9);
        assertEquals(6.0, Ring4Relief.terrace(9.0), 1e-9);
        assertEquals(12.0, Ring4Relief.terrace(12.0), 1e-9);
        assertEquals(6.0, Ring4Relief.terrace(10.5), 1e-9);
        assertTrue(Ring4Relief.terrace(11.0) > 6.0 && Ring4Relief.terrace(11.0) < 12.0);
        for (double v = 0.0; v < 60.0; v += 0.25) {
            assertTrue(Ring4Relief.terrace(v + 0.25) >= Ring4Relief.terrace(v));
        }
    }

    @Test
    void sinkholesStayInsideTheirCell() {
        long salt = field.salt();
        int found = 0;
        for (int i = -20; i < 20; i++) {
            for (int j = -20; j < 20; j++) {
                double[] hole = Ring4Relief.sinkhole(i, j, salt);
                if (hole == null) {
                    continue;
                }
                found++;
                int cell = Ring4Relief.SINKHOLE_CELL;
                assertTrue(hole[0] - hole[2] >= i * cell && hole[0] + hole[2] <= (i + 1) * cell);
                assertTrue(hole[1] - hole[2] >= j * cell && hole[1] + hole[2] <= (j + 1) * cell);
            }
        }
        assertTrue(found > 500 && found < 1100, "sinkholes " + found);
    }

    /** The preliminary surface never lies above the real surface of a quart (else lakes and rivers get air). */
    @Test
    void initialHeightIsALowerBoundOfTheQuart() {
        int checked = 0;
        for (double[] c : COLUMNS) {
            int qx = (int) c[0] & ~3;
            int qz = (int) c[1] & ~3;
            double initial = field.initialHeight(qx, qz);
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    double surface = field.column(qx + dx, qz + dz).surface();
                    assertTrue(initial <= surface, (qx + dx) + ", " + (qz + dz) + ": " + initial + " > " + surface);
                    checked++;
                }
            }
        }
        assertTrue(checked > 1000);
    }
}
