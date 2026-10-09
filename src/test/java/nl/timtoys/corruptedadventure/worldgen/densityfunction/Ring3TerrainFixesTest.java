package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Package 42b on the test seed: no carved tunnel under lava, walls of ring 3 rock in deep cuts (the abyss, collapse
 * shafts) instead of vanilla stone.
 */
class Ring3TerrainFixesTest {
    private static VolcanicField field;

    @BeforeAll
    static void setUp() {
        field = VolcanicFieldTest.field();
    }

    @Test
    void noOpeningRunsThroughLavaOrRightBesideIt() {
        // The basalt flows' tunnels (39d) under a river poured lava into themselves ("a diagonal wall of lava", INT2).
        int lavaColumns = 0;
        int[][] areas = {{4720, -576, 4975, -337}, {3936, -3600, 4319, -3233}, {-4080, -1984, -3697, -1601}};
        for (int[] a : areas) {
            for (int x = a[0]; x <= a[2]; x++) {
                for (int z = a[1]; z <= a[3]; z++) {
                    VolcanicField.Column c = field.column(x, z);
                    if (c.lavaTop() == Integer.MIN_VALUE) {
                        continue;
                    }
                    lavaColumns++;
                    int[][] around = {{x, z}, {x + 1, z}, {x - 1, z}, {x, z + 1}, {x, z - 1}};
                    for (int[] p : around) {
                        VolcanicField.Column n = field.column(p[0], p[1]);
                        for (int y = c.lavaBottom() - 2; y < c.lavaTop(); y++) {
                            assertFalse(n.voidAt(y), "an opening at " + p[0] + "," + y + "," + p[1] + " in or beside lava");
                        }
                    }
                }
            }
        }
        assertTrue(lavaColumns > 1000, "lava columns " + lavaColumns);
    }

    @Test
    void theAbyssWallsAreRing3RockDownToTheFloor() {
        int walls = 0;
        int cells = 6200 / Landmarks.ABYSS_CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Landmarks.Abyss a = field.abyssAt(i, j);
                if (a == null) {
                    continue;
                }
                // Walk out from the axis across the abyss to the first column well above the floor: a wall.
                for (double s = 0.0; s < a.width() * 1.5; s += 1.0) {
                    int x = (int) Math.floor(a.x() - a.sin() * s);
                    int z = (int) Math.floor(a.z() + a.cos() * s);
                    VolcanicField.Column c = field.column(x, z);
                    if (c.height() > Landmarks.ABYSS_FLOOR + 40.0) {
                        assertTrue(c.deepWall(Landmarks.ABYSS_FLOOR + 4), "wall rock near the floor at " + x + "," + z);
                        assertTrue(c.deepWall((int) c.height() - 25), "wall rock below the package at " + x + "," + z);
                        assertFalse(c.deepWall((int) c.height() + 1));
                        walls++;
                        break;
                    }
                }
            }
        }
        assertTrue(walls >= 2, "abyss walls " + walls);
        // The plain keeps stone below its package.
        VolcanicField.Column flat = null;
        for (int x = -4900; x < -4600 && flat == null; x += 7) {
            VolcanicField.Column c = field.column(x, 700);
            if (c.cutDepth < 8.0) {
                flat = c;
            }
        }
        assertTrue(flat != null && !flat.deepWall((int) flat.height() - 30));
    }

    @Test
    void collapseShaftsHaveWallsOfRing3Rock() {
        int checked = 0;
        int cells = 6200 / CellShapes.COLLAPSE_CELL + 1;
        for (int i = -cells; i <= cells && checked < 20; i++) {
            for (int j = -cells; j <= cells && checked < 20; j++) {
                CellShapes.Collapse col = field.collapse(i, j);
                if (col == null || !col.shaft()) {
                    continue;
                }
                for (int dx = -8; dx <= 8; dx++) {
                    int x = (int) Math.floor(col.shaftX()) + dx;
                    int z = (int) Math.floor(col.shaftZ());
                    double outside = col.shaftOutside(x + 0.5, z + 0.5);
                    if (outside < 0.5 || outside > 2.0) {
                        continue;
                    }
                    VolcanicField.Column c = field.column(x, z);
                    VolcanicField.Column in = field.column((int) Math.floor(col.shaftX()), z);
                    int y = (int) Math.floor(in.height()) - 12;
                    if (!in.voidAt(y)) {
                        continue;
                    }
                    assertEquals(2, c.caveWall(y), "the shaft's wall at " + x + "," + y + "," + z);
                    checked++;
                    break;
                }
            }
        }
        assertTrue(checked >= 5, "shaft walls " + checked);
    }
}
