package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ring 5's inner band (83, V4-A): dead ground Y 70–95 around 78, the regions' shapes (exact stairs, round pits, the
 * regular wave, blocky glitch ground, floating cubes) and the loose cracks down into the void.
 */
class Ring5InnerTest {
    private static final RingLayout LAYOUT = Ring5FieldTest.LAYOUT;
    private static final Ring5Field FIELD = Ring5FieldTest.field();
    private static final Ring5Inner INNER = FIELD.inner();

    /** The inner band's columns on a polar raster (away from castles and bridges): {x, z}. */
    private static List<int[]> raster(int step) {
        Ring5Band band = Ring5Band.of(LAYOUT);
        List<int[]> columns = new ArrayList<>();
        for (int distance = 8150; distance <= 9400; distance += step) {
            int count = (int) (2.0 * Math.PI * distance / step);
            for (int k = 0; k < count; k++) {
                double angle = 2.0 * Math.PI * (k + 0.5) / count;
                int x = (int) Math.floor(distance * Math.cos(angle));
                int z = (int) Math.floor(distance * Math.sin(angle));
                if (band.inner(x, z) && LAYOUT.effectiveDistance(x, z) > 8128.0) {
                    columns.add(new int[]{x, z});
                }
            }
        }
        return columns;
    }

    /** V4-A: the surface lies in Y 70–95 on at least 95 % of the band, its median at Y 76–80; cracks at most 2 %. */
    @Test
    void surfaceIs70To95AroundY78() {
        List<Double> tops = new ArrayList<>();
        int cracks = 0;
        int all = 0;
        for (int[] c : raster(17)) {
            Ring5Field.Column column = FIELD.column(c[0], c[1]);
            if (column.castle() > 0.0) {
                continue;
            }
            all++;
            if (column.isVoid()) {
                cracks++;
                continue;
            }
            tops.add(column.top());
        }
        double[] sorted = tops.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        long inRange = tops.stream().filter(t -> t >= 70.0 && t <= 95.0).count();
        double median = sorted[sorted.length / 2];
        System.out.printf("inner band: %d columns, median %.1f, p5 %.1f, p95 %.1f, in 70–95 %.3f, cracks %.4f%n", all, median,
                sorted[sorted.length / 20], sorted[19 * sorted.length / 20], inRange / (double) tops.size(), cracks / (double) all);
        assertTrue(inRange >= 0.95 * tops.size(), "in range " + inRange + " of " + tops.size());
        assertTrue(median >= 76.0 && median <= 80.0, "median " + median);
        assertTrue(cracks > 0 && cracks <= 0.02 * all, "cracks " + cracks + " of " + all);
    }

    /** {@code corrupted_wastes}: every staircase's treads lie exactly 4 apart, 3–6 of them, flat and on straight edges. */
    @Test
    void stairsAreExactly4High() {
        int stairs = 0;
        for (int i = 50; i < 80; i++) {
            for (int j = -20; j < 20; j++) {
                TreeSet<Double> treads = new TreeSet<>();
                for (int x = i * Ring5Inner.STAIR_CELL; x < (i + 1) * Ring5Inner.STAIR_CELL; x++) {
                    for (int z = j * Ring5Inner.STAIR_CELL; z < (j + 1) * Ring5Inner.STAIR_CELL; z += 3) {
                        double stair = INNER.stairAt(x, z);
                        if (!Double.isNaN(stair)) {
                            treads.add(stair);
                        }
                    }
                }
                if (treads.isEmpty()) {
                    continue;
                }
                stairs++;
                assertTrue(treads.size() >= Ring5Inner.MIN_STEPS && treads.size() <= Ring5Inner.MAX_STEPS, "treads " + treads);
                double previous = Double.NaN;
                for (double tread : treads) {
                    assertEquals(tread, Math.rint(tread));
                    if (!Double.isNaN(previous)) {
                        assertEquals(4.0, tread - previous, "treads " + treads);
                    }
                    previous = tread;
                }
            }
        }
        assertTrue(stairs > 100 && stairs < 600, "stairs " + stairs);
    }

    /** {@code corrupted_wastes}: pits perfectly round with a flat floor 2–4 down. */
    @Test
    void pitsAreRoundAndFlat() {
        int pits = 0;
        for (int i = 120; i < 150; i++) {
            for (int j = -10; j < 10; j++) {
                List<int[]> in = new ArrayList<>();
                for (int x = i * Ring5Inner.PIT_CELL; x < (i + 1) * Ring5Inner.PIT_CELL; x++) {
                    for (int z = j * Ring5Inner.PIT_CELL; z < (j + 1) * Ring5Inner.PIT_CELL; z++) {
                        if (!Double.isNaN(INNER.pitAt(x, z))) {
                            in.add(new int[]{x, z});
                        }
                    }
                }
                if (in.isEmpty()) {
                    continue;
                }
                pits++;
                double floor = INNER.pitAt(in.getFirst()[0], in.getFirst()[1]);
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int minZ = Integer.MAX_VALUE;
                int maxZ = Integer.MIN_VALUE;
                for (int[] c : in) {
                    assertEquals(floor, INNER.pitAt(c[0], c[1]));
                    minX = Math.min(minX, c[0]);
                    maxX = Math.max(maxX, c[0]);
                    minZ = Math.min(minZ, c[1]);
                    maxZ = Math.max(maxZ, c[1]);
                }
                // Round: as wide as deep, and about π r² columns.
                int across = maxX - minX + 1;
                assertEquals(across, maxZ - minZ + 1);
                assertTrue(across >= 2 * Ring5Inner.MIN_PIT_RADIUS && across <= 2 * Ring5Inner.MAX_PIT_RADIUS + 1);
                double r = (across - 1) / 2.0;
                assertEquals(Math.PI * r * r, in.size(), 0.2 * Math.PI * r * r);
            }
        }
        assertTrue(pits > 100, "pits " + pits);
    }

    /** {@code false_meadow}: Y 73–79, a regular wave (the same every wave length). */
    @Test
    void meadowIsARegularWave() {
        for (int x = 8000; x < 8300; x += 7) {
            for (int z = -300; z < 300; z += 11) {
                double top = INNER.meadowTop(x, z);
                assertTrue(top >= 73.0 && top <= 79.0);
                assertEquals(top, INNER.meadowTop(x + (int) Ring5Inner.MEADOW_WAVE, z - 2 * (int) Ring5Inner.MEADOW_WAVE), 1e-9);
            }
        }
    }

    /**
     * {@code glitch_fields}: where they hold alone the ground is blocky (4 × 4 columns of one height, a multiple of 4), the
     * top slices shifted sideways; floating cubes 3–8 big with air under them.
     */
    @Test
    void glitchFieldsAreBlockyWithLayersAndFloatingCubes() {
        int blocks = 0;
        int layered = 0;
        for (int[] c : raster(13)) {
            int qx = Math.floorDiv(c[0], 4) * 4;
            int qz = Math.floorDiv(c[1], 4) * 4;
            if (INNER.glitchWeight(qx, qz) < 1.0 || INNER.glitchWeight(qx + 3, qz + 3) < 1.0
                    || INNER.glitchWeight(qx, qz + 3) < 1.0 || INNER.glitchWeight(qx + 3, qz) < 1.0) {
                continue;
            }
            double top = INNER.top(qx, qz);
            assertEquals(0.0, top % Ring5Inner.QUANTUM, 1e-9);
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    assertEquals(top, INNER.top(qx + dx, qz + dz), 1e-9);
                }
            }
            blocks++;
            Ring5Field.Column column = FIELD.column(qx, qz);
            if (column.layered) {
                layered++;
                int[] shift = INNER.layerShift(qx, (int) top - 2, qz);
                int amount = Math.abs(shift[0]) + Math.abs(shift[1]);
                assertTrue(amount >= 2 && amount <= 6 && (shift[0] == 0 || shift[1] == 0));
                int height = INNER.layerHeight(qx, qz);
                assertTrue(height >= Ring5Inner.MIN_LAYER_HEIGHT && height <= Ring5Inner.MAX_LAYER_HEIGHT, "layer " + height);
            }
        }
        System.out.println("glitch blocks " + blocks + ", layered " + layered);
        assertTrue(blocks > 50 && layered > 50, "blocks " + blocks + ", layered " + layered);
        int cubes = 0;
        for (int i = 240; i < 300; i++) {
            for (int j = -40; j < 40; j++) {
                int[] cube = FIELD.cube(i, j);
                if (cube == null) {
                    continue;
                }
                cubes++;
                assertTrue(cube[2] >= 3 && cube[2] <= 8);
                for (int dx = 0; dx < cube[2]; dx++) {
                    for (int dz = 0; dz < cube[2]; dz++) {
                        int x = cube[0] + dx;
                        int z = cube[1] + dz;
                        Ring5Field.Column column = FIELD.column(x, z);
                        // Whole in the glitch fields: no column under the cube is outer band, castle, crack or plain ground.
                        assertTrue(column.inner && !column.isVoid() && column.castle() == 0.0 && column.glitch() > 0.0,
                                "cube cut at " + x + ", " + z);
                        assertTrue(FIELD.terrain(x, cube[3], z) > 0.0 && FIELD.terrain(x, cube[3] + cube[2] - 1, z) > 0.0);
                        assertTrue(FIELD.terrain(x, cube[3] - 1, z) < 0.0, "air under the cube at " + x + ", " + z);
                        assertTrue(cube[3] - column.top() >= Ring5Inner.MIN_CUBE_LIFT - 4, "lift");
                    }
                }
            }
        }
        assertTrue(cubes > 20, "cubes " + cubes);
    }

    /** Loose cracks: 2–6 wide, 20–120 long, down past the world's bottom with nothing solid, none near the band line or zone. */
    @Test
    void cracksReachIntoTheVoid() {
        int cracks = 0;
        Ring5Band band = Ring5Band.of(LAYOUT);
        for (int i = -40; i < 40; i++) {
            for (int j = -40; j < 40; j++) {
                double[] crack = FIELD.innerCrack(i, j);
                if (crack == null) {
                    continue;
                }
                cracks++;
                assertTrue(crack[4] >= 20.0 && crack[4] <= 120.0 && crack[5] >= 1.0 && crack[5] <= 3.0);
                int x = (int) Math.floor(crack[0] + crack[2] * crack[4] / 2.0);
                int z = (int) Math.floor(crack[1] + crack[3] * crack[4] / 2.0);
                assertTrue(band.inner(x, z));
                if (FIELD.innerCrackDistance(x, z) <= 0.0 && FIELD.column(x, z).castle() == 0.0 && FIELD.column(x, z).spans == 0) {
                    for (int y = -64; y < 320; y += 2) {
                        assertTrue(FIELD.terrain(x, y, z) < 0.0, "solid in a crack at " + x + ", " + y + ", " + z);
                    }
                    assertEquals(1.0, FIELD.column(x, z).scars());
                }
            }
        }
        assertTrue(cracks > 100, "cracks " + cracks);
    }

    /**
     * A staircase lies whole inside its own cell: a column next to the cell's edge never needs a staircase of the
     * neighbour, so none is cut off. Its columns make one full rectangle that touches neither edge of the cell.
     */
    @Test
    void stairsLieWhollyInTheirCell() {
        int stairs = 0;
        for (int i = 50; i < 70; i++) {
            for (int j = -10; j < 10; j++) {
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int minZ = Integer.MAX_VALUE;
                int maxZ = Integer.MIN_VALUE;
                int count = 0;
                for (int x = i * Ring5Inner.STAIR_CELL; x < (i + 1) * Ring5Inner.STAIR_CELL; x++) {
                    for (int z = j * Ring5Inner.STAIR_CELL; z < (j + 1) * Ring5Inner.STAIR_CELL; z++) {
                        if (!Double.isNaN(INNER.stairAt(x, z))) {
                            count++;
                            minX = Math.min(minX, x);
                            maxX = Math.max(maxX, x);
                            minZ = Math.min(minZ, z);
                            maxZ = Math.max(maxZ, z);
                        }
                    }
                }
                if (count == 0) {
                    continue;
                }
                stairs++;
                assertTrue(minX > i * Ring5Inner.STAIR_CELL && maxX < (i + 1) * Ring5Inner.STAIR_CELL - 1
                        && minZ > j * Ring5Inner.STAIR_CELL && maxZ < (j + 1) * Ring5Inner.STAIR_CELL - 1,
                        "stair touches the edge of its cell " + i + ", " + j);
                assertEquals((long) (maxX - minX + 1) * (maxZ - minZ + 1), count, "stair cut in cell " + i + ", " + j);
            }
        }
        assertTrue(stairs > 20, "stairs " + stairs);
    }

    /** {@code glitch_fields}: every block's top lies in whole blocks of {@link Ring5Inner#QUANTUM}, from 72 up to 92. */
    @Test
    void glitchBlocksStayInTheBand() {
        int blocks = 0;
        for (int[] c : raster(13)) {
            if (INNER.glitchWeight(c[0], c[1]) < 1.0) {
                continue;
            }
            double top = INNER.glitchTop(c[0], c[1]);
            blocks++;
            assertTrue(top >= 72.0 && top <= 92.0 && top % Ring5Inner.QUANTUM == 0.0, "glitch top " + top + " at " + c[0] + ", " + c[1]);
        }
        assertTrue(blocks > 50, "blocks " + blocks);
    }
}
