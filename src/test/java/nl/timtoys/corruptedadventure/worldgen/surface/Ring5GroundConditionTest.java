package nl.timtoys.corruptedadventure.worldgen.surface;

import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5FieldTest;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parts of {@code ring_5_ground} that meet 93's caves (INT3), on the test seed: the voidstone edge round a void cave's
 * hole, the biomes' surfaces only near the column's top (not on the cave floors under it), the inverted hills' meadow only
 * on the hill's underside.
 */
class Ring5GroundConditionTest {
    private static final Ring5World WORLD = Ring5World.of(Ring5FieldTest.field(), Ring5FieldTest.LAYOUT,
            Ring5World.salt(Ring5FieldTest.SEED));

    /** The void cave of {@code tools/test/areas/ring5.txt} {@code ch93} (chunks 520..522, -40..-38): its holes' rims. */
    @Test
    void holesOfVoidCavesGetTheVoidEdge() {
        Ring5Field field = WORLD.field();
        long seed = WORLD.salt();
        int rims = 0;
        for (int x = 520 * 16; x < 523 * 16; x++) {
            for (int z = -40 * 16; z < -37 * 16; z++) {
                int rim = field.holeRimTop(x, z);
                if (rim == Integer.MIN_VALUE) {
                    continue;
                }
                rims++;
                Ring5GroundCondition.Column column = Ring5GroundCondition.Column.ofWorld(WORLD, x, z);
                int wall = rim - Ring5Field.HOLE_RIM_DEPTH;
                for (int y = wall; y < rim; y++) {
                    assertTrue(column.test(Ring5GroundCondition.Part.HOLE_RIM, y, seed, x, z), x + " " + y + " " + z);
                    assertEquals(4, field.caveCode(x, y, z), "the rim's code");
                }
                assertFalse(column.test(Ring5GroundCondition.Part.HOLE_RIM, rim, seed, x, z));
                assertFalse(column.test(Ring5GroundCondition.Part.HOLE_WALL, wall, seed, x, z));
                assertTrue(column.test(Ring5GroundCondition.Part.HOLE_WALL, wall - 1, seed, x, z));
                assertTrue(column.test(Ring5GroundCondition.Part.HOLE_WALL_TOP, wall - 1, seed, x, z));
                assertTrue(column.test(Ring5GroundCondition.Part.HOLE_WALL, -60, seed, x, z), "down to the bottom");
                assertFalse(column.test(Ring5GroundCondition.Part.HOLE_WALL_TOP, wall - 1 - Ring5GroundCondition.MAX_WALL_TOP, seed, x, z));
            }
        }
        assertTrue(rims > 20, "rim columns " + rims);
    }

    /** In ring 5's core the biomes' surfaces reach 8 under the column's pure top (16 under a glitch field's layers). */
    @Test
    void upperStopsWhereTheCavesBegin() {
        long seed = WORLD.salt();
        int plain = 0;
        for (int x = 8400; x < 8400 + 256; x += 7) {
            for (int z = -8; z < 248; z += 7) {
                Ring5Field.Column field = WORLD.field().column(x, z);
                if (field.isVoid() || !WORLD.core(x, z)) {
                    continue;
                }
                Ring5GroundCondition.Column column = Ring5GroundCondition.Column.ofWorld(WORLD, x, z);
                int top = WORLD.top(x, z);
                int depth = field.layered() ? Ring5GroundCondition.UPPER_LAYERS : Ring5GroundCondition.UPPER_DEPTH;
                assertTrue(column.test(Ring5GroundCondition.Part.UPPER, top - depth + 1, seed, x, z));
                assertFalse(column.test(Ring5GroundCondition.Part.UPPER, top - depth, seed, x, z));
                assertTrue(column.test(Ring5GroundCondition.Part.UPPER, top + 20, seed, x, z), "above the top");
                plain += field.layered() ? 0 : 1;
            }
        }
        assertTrue(plain > 100, "plain " + plain);
    }

    /** {@code inverted_hills} (perf area {@code inverted}): the upside-down meadow only in the hill's lowest blocks. */
    @Test
    void invertedHillsDressOnlyTheirUnderside() {
        long seed = WORLD.salt();
        int hills = 0;
        for (int x = -8 * 16; x < 8 * 16; x += 3) {
            for (int z = -558 * 16; z < -542 * 16; z += 3) {
                Ring5GroundCondition.Column column = Ring5GroundCondition.Column.ofWorld(WORLD, x, z);
                if (column.hillBottom() == Integer.MIN_VALUE) {
                    continue;
                }
                hills++;
                int bottom = column.hillBottom();
                assertTrue(WORLD.field().terrain(x, bottom, z) > 0.0 && WORLD.field().terrain(x, bottom - 1, z) <= 0.0, "the hill's lowest block");
                assertTrue(column.test(Ring5GroundCondition.Part.INVERTED_HILL, bottom + Ring5GroundCondition.HILL_UNDERSIDE - 1, seed, x, z));
                assertFalse(column.test(Ring5GroundCondition.Part.INVERTED_HILL, bottom + Ring5GroundCondition.HILL_UNDERSIDE, seed, x, z));
            }
        }
        assertTrue(hills > 100, "hill columns " + hills);
    }
}
