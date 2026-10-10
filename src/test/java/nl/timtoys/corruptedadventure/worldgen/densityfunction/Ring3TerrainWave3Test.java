package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ring 3 wave 3 terrain fixes on the test seed: 42c (the band around the abyss's lava sea reaches its tapered ends). */
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
}
