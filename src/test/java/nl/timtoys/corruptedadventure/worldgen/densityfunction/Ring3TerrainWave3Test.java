package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ring 3 wave 3 terrain fixes on the test seed: 42c (the band around the abyss's lava sea reaches its tapered ends), 39g
 * (the scorched shore of a lava river lies within a few blocks of its lava's edge, whatever its width).
 */
class Ring3TerrainWave3Test {
    /** The terrain's cells: caves are interpolated between their corners (every 4 blocks in x and z). */
    private static final int CELL = 4;

    private static VolcanicField field;

    @BeforeAll
    static void setUp() {
        field = VolcanicFieldTest.field();
    }

    @Test
    void everyCellCornerBesideTheAbyssSeaLiesInItsBand() {
        // 42c: a cave beside the lava is interpolated from the corners of its cell; the shaft at (4.409-4.412, Y 5-9,
        // -510...-508) opened where the abyss tapers to its end and those corners lay outside the band (0.3 half widths).
        int sea = 0;
        int corners = 0;
        Set<Long> checked = new HashSet<>();
        int cells = 6200 / Landmarks.ABYSS_CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Landmarks.Abyss a = field.abyssAt(i, j);
                if (a == null) {
                    continue;
                }
                int reach = (int) Math.ceil(a.length() * 0.5 + 2.0);
                for (int x = (int) a.x() - reach; x <= a.x() + reach; x++) {
                    for (int z = (int) a.z() - reach; z <= a.z() + reach; z++) {
                        if (!a.near(x + 0.5, z + 0.5, 2.0)) {
                            continue;
                        }
                        VolcanicField.Column c = field.column(x, z);
                        if (c.lavaTop() != Landmarks.ABYSS_LAVA || a.across(x + 0.5, z + 0.5) >= 1.0) {
                            continue;
                        }
                        sea++;
                        int[][] beside = {{x + 1, z}, {x - 1, z}, {x, z + 1}, {x, z - 1}};
                        for (int[] b : beside) {
                            int x0 = Math.floorDiv(b[0], CELL) * CELL;
                            int z0 = Math.floorDiv(b[1], CELL) * CELL;
                            for (int dx = 0; dx <= CELL; dx += CELL) {
                                for (int dz = 0; dz <= CELL; dz += CELL) {
                                    int cx = x0 + dx;
                                    int cz = z0 + dz;
                                    if (!checked.add((long) cx << 32 | (cz & 0xFFFFFFFFL))) {
                                        continue;
                                    }
                                    VolcanicField.Column corner = field.column(cx, cz);
                                    assertTrue(corner.guardBottom() <= c.lavaBottom() && corner.guardTop() >= c.lavaTop(),
                                            "cell corner " + cx + "," + cz + " beside the sea at " + x + "," + z + " outside the band");
                                    corners++;
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(sea > 10_000 && corners > 1_000, "sea columns " + sea + ", corners " + corners);
        // The leak of INT-afr: every corner of the cell of the shaft.
        for (int cx = 4408; cx <= 4412; cx += CELL) {
            for (int cz = -512; cz <= -508; cz += CELL) {
                VolcanicField.Column corner = field.column(cx, cz);
                assertTrue(corner.guardBottom() <= 5 && corner.guardTop() >= 9, "corner " + cx + "," + cz);
            }
        }
    }

    @Test
    void theLavaEdgeIsWhereTheChannelDropsBelowTheLava() {
        // The channel's profile lowers the ground towards the bed two blocks below the lava (VolcanicField.fill).
        for (double above = 0.25; above < 40.0; above *= 1.5) {
            int level = 60;
            double pre = level + above;
            double edge = VolcanicField.lavaEdge(pre, level);
            double t = (edge - 0.55) / 0.45;
            double profile = 1.0 - t * t * (3.0 - 2.0 * t);
            assertEquals(level, pre - profile * (pre - (level - 2)), 1e-9, "edge at " + above + " above the lava");
            assertTrue(edge > 0.55 && edge < 1.0);
        }
        assertEquals(1.0, VolcanicField.lavaEdge(59.0, 60), 0.0, "ground below the lava: the whole channel holds lava");
    }

    @Test
    void theScorchedShoreHugsTheLavaWhateverTheRiversWidth() {
        // 39g: the shore (and its magma) lies within a few blocks of lava that shows (the field's ground below its lava);
        // before, the dry slope of a wide channel was shore too, up to ~0.45 half widths plus 3 blocks from the lava.
        int[][] areas = {{3936, -3600, 4319, -3233}, {-4000, -128, -3633, 255}, {-4080, -1984, -3697, -1601},
                {4720, -576, 4975, -337}, {3920, 320, 4111, 511}};
        int shore = 0;
        int magma = 0;
        int oldBankFar = 0;
        for (int[] a : areas) {
            for (int x = a[0]; x <= a[2]; x++) {
                for (int z = a[1]; z <= a[3]; z++) {
                    VolcanicField.Column c = field.column(x, z);
                    if (c.lavaBank > 0.0 && c.lavaShore == 0.0 && lavaWithin(x, z, 4) == 0) {
                        oldBankFar++;
                    }
                    if (c.lavaShore <= 0.0 || c.lavaShore >= 1.0) {
                        continue;
                    }
                    shore++;
                    // The edge comes from the column's own ground; the terrain's interpolation moves it by a block or so.
                    assertTrue(lavaWithin(x, z, 4) > 0, "shore far from lava at " + x + "," + z);
                    if (c.lavaShore >= 2.0 / 3.0) {
                        magma++;
                        assertTrue(lavaWithin(x, z, 2) > 0, "magma shore far from lava at " + x + "," + z);
                    }
                }
            }
        }
        assertTrue(shore > 1000 && magma > 100, "shore " + shore + ", magma " + magma);
        System.out.println("39g: shore columns " + shore + ", magma columns " + magma + ", old bank columns > 4 from lava " + oldBankFar);
    }

    /** The number of columns within {@code r} (square) of (x, z) where lava shows (the field's ground below its lava). */
    private static int lavaWithin(int x, int z, int r) {
        int n = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                VolcanicField.Column c = field.column(x + dx, z + dz);
                if (c.lavaTop() != Integer.MIN_VALUE && c.height() < c.lavaTop()) {
                    n++;
                }
            }
        }
        return n;
    }
}
