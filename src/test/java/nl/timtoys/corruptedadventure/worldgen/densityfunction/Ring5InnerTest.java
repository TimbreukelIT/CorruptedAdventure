package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ring 5's inner band (83, V4-A; relief 83b): rolling dead ground Y ~65–120 with now and then a ridge or cliff up to
 * ~140, the old low ground in the zone 4→5, the regions' shapes on it (exact stairs up the slope, round pits, the regular
 * wave, blocky glitch ground, floating cubes) and the loose cracks down into the void.
 */
class Ring5InnerTest {
    private static final RingLayout LAYOUT = Ring5FieldTest.LAYOUT;
    private static final Ring5Field FIELD = Ring5FieldTest.field();
    private static final Ring5Inner INNER = FIELD.inner();
    private static final double ZONE_END = Ring4Sundered.zoneEnd(LAYOUT);

    /** The inner band's columns on a polar raster (away from castles and bridges): {x, z}. */
    private static List<int[]> raster(int step) {
        return raster(step, 8128.0);
    }

    /** The same, from an effective distance on. */
    private static List<int[]> raster(int step, double from) {
        Ring5Band band = Ring5Band.of(LAYOUT);
        List<int[]> columns = new ArrayList<>();
        for (int distance = 8150; distance <= 9400; distance += step) {
            int count = (int) (2.0 * Math.PI * distance / step);
            for (int k = 0; k < count; k++) {
                double angle = 2.0 * Math.PI * (k + 0.5) / count;
                int x = (int) Math.floor(distance * Math.cos(angle));
                int z = (int) Math.floor(distance * Math.sin(angle));
                if (band.inner(x, z) && LAYOUT.effectiveDistance(x, z) > from) {
                    columns.add(new int[]{x, z});
                }
            }
        }
        return columns;
    }

    private static double quantile(double[] sorted, double q) {
        return sorted[(int) Math.min(sorted.length - 1, Math.floor(q * sorted.length))];
    }

    /**
     * 83b: where the relief holds fully, the ground rolls over Y ~65–120 (p5 and p95 near those), most of it in Y 62–124,
     * with ridges and cliffs above 125 on a small share only; nothing above {@link Ring5Inner#CAP}. Openings (cracks and
     * the corrupt spots' holes) at most 2 %.
     */
    @Test
    void groundRollsFrom65To120WithRareRidges() {
        List<Double> grounds = new ArrayList<>();
        int high = 0;
        int open = 0;
        int all = 0;
        for (int[] c : raster(17, ZONE_END + Ring5Inner.RELIEF_FADE)) {
            Ring5Field.Column column = FIELD.column(c[0], c[1]);
            if (column.castle() > 0.0) {
                continue;
            }
            all++;
            if (FIELD.innerCrackDistance(c[0], c[1]) <= 0.0) {
                open++;
            }
            double ground = INNER.base(c[0], c[1]);
            grounds.add(ground);
            assertTrue(ground > Ring5Inner.ROLL_LOW && ground < Ring5Inner.CAP, "ground " + ground);
            if (ground > 125.0) {
                high++;
            }
        }
        double[] sorted = grounds.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        long inRange = grounds.stream().filter(t -> t >= 62.0 && t <= 124.0).count();
        double p5 = quantile(sorted, 0.05);
        double p95 = quantile(sorted, 0.95);
        double median = quantile(sorted, 0.5);
        System.out.printf("inner band (relief 1): %d columns, p1 %.1f, p5 %.1f, median %.1f, p95 %.1f, p99 %.1f, max %.1f,"
                        + " in 62–124 %.3f, above 125 %.4f, open (cracks, holes) %.4f%n", all, quantile(sorted, 0.01), p5, median, p95,
                quantile(sorted, 0.99), sorted[sorted.length - 1], inRange / (double) grounds.size(), high / (double) grounds.size(),
                open / (double) all);
        assertTrue(p5 >= 62.0 && p5 <= 72.0, "p5 " + p5);
        assertTrue(p95 >= 108.0 && p95 <= 124.0, "p95 " + p95);
        assertTrue(median >= 82.0 && median <= 98.0, "median " + median);
        assertTrue(inRange >= 0.9 * grounds.size(), "in range " + inRange + " of " + grounds.size());
        assertTrue(high > 0 && high <= 0.06 * grounds.size(), "above 125: " + high + " of " + grounds.size());
        assertTrue(open > 0 && open <= 0.02 * all, "open " + open + " of " + all);
    }

    /** 83b: a ridge or cliff rises steeply on one side (≥ 10 within 4 blocks somewhere), and only now and then. */
    @Test
    void ridgesAreRareWithSteepFaces() {
        int ridged = 0;
        int steep = 0;
        List<int[]> raster = raster(23, ZONE_END + Ring5Inner.RELIEF_FADE);
        for (int[] c : raster) {
            double lift = INNER.ridge(c[0], c[1]);
            if (lift > 0.5 * Ring5Inner.RIDGE_HEIGHT) {
                ridged++;
            }
            if (lift > 0.0) {
                for (int d = 1; d <= 4; d++) {
                    double around = Math.min(Math.min(INNER.ridge(c[0] + d, c[1]), INNER.ridge(c[0] - d, c[1])),
                            Math.min(INNER.ridge(c[0], c[1] + d), INNER.ridge(c[0], c[1] - d)));
                    if (lift - around >= 10.0) {
                        steep++;
                        break;
                    }
                }
            }
        }
        System.out.printf("ridges: %.4f of the band above half height, %d steep faces%n", ridged / (double) raster.size(), steep);
        assertTrue(ridged > 0 && ridged <= 0.08 * raster.size(), "ridged " + ridged + " of " + raster.size());
        assertTrue(steep > 0, "no steep face");
    }

    /**
     * 83b: in the zone 4→5 the ground is the old low ground of 83 (Y 70–95 round 78, what the fracture zone blends), and
     * from there it grows into the relief without a step.
     */
    @Test
    void reliefGrowsOutOfTheZoneWithoutAStep() {
        int zone = 0;
        for (int k = 0; k < 720; k++) {
            double angle = 2.0 * Math.PI * k / 720;
            for (int r = 7900; r <= 8400; r++) {
                int x = Mth.floor(r * Math.cos(angle));
                int z = Mth.floor(r * Math.sin(angle));
                double effective = LAYOUT.effectiveDistance(x, z);
                double base = INNER.base(x, z);
                if (effective < ZONE_END) {
                    double old = Mth.clamp(Ring5Inner.ZONE_BASE + Ring5Inner.ZONE_HILLS * INNER.hills().getValue(x, 0.0, z),
                            Ring5Inner.ZONE_MIN, Ring5Inner.ZONE_MAX);
                    assertEquals(old, base, 1e-9, "zone ground at " + x + ", " + z);
                    zone++;
                }
                int nx = Mth.floor((r + 1) * Math.cos(angle));
                int nz = Mth.floor((r + 1) * Math.sin(angle));
                if (INNER.ridge(x, z) == 0.0 && INNER.ridge(nx, nz) == 0.0) {
                    assertTrue(Math.abs(INNER.base(nx, nz) - base) <= 2.0, "step at " + x + ", " + z);
                }
            }
        }
        assertTrue(zone > 10_000, "zone columns " + zone);
    }

    /**
     * 86b's sand rule ({@code corrupted_wastes}: top below Y 76) still means the low places: a minority of the plain's
     * columns, never the bulk of it.
     */
    @Test
    void lowPlacesStayAMinority() {
        int wastes = 0;
        int low = 0;
        for (int[] c : raster(13)) {
            double g = INNER.glitchWeight(c[0], c[1]);
            Ring5Field.Column column = FIELD.column(c[0], c[1]);
            if (g > 0.0 || INNER.meadowWeight(c[0], c[1], g) > 0.0 || column.castle() > 0.0 || column.isVoid()) {
                continue;
            }
            wastes++;
            if (column.top() < 76.0) {
                low++;
            }
        }
        System.out.printf("corrupted_wastes: %d columns, top below 76: %.3f%n", wastes, low / (double) wastes);
        assertTrue(low > 0 && low <= 0.25 * wastes, "low " + low + " of " + wastes);
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

    /**
     * 83b: a staircase climbs the slope: of the four directions from its origin, the ground one staircase's length away
     * is highest in the one it rises in.
     */
    @Test
    void stairsClimbTheSlope() {
        int stairs = 0;
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int i = 60; i < 70; i++) {
            for (int j = -40; j < 40; j++) {
                int[] shape = INNER.stairShape(i, j);
                if (shape == null) {
                    continue;
                }
                int direction = shape[3];
                // The first tread's columns are the lowest; one run further up the stairs the treads rise.
                int[] lowest = null;
                double lowTread = Double.POSITIVE_INFINITY;
                for (int x = i * Ring5Inner.STAIR_CELL; x < (i + 1) * Ring5Inner.STAIR_CELL; x++) {
                    for (int z = j * Ring5Inner.STAIR_CELL; z < (j + 1) * Ring5Inner.STAIR_CELL; z++) {
                        double tread = INNER.stairAt(x, z);
                        if (!Double.isNaN(tread) && tread < lowTread) {
                            lowTread = tread;
                            lowest = new int[]{x, z};
                        }
                    }
                }
                stairs++;
                int[] d = steps[direction];
                double further = INNER.stairAt(lowest[0] + d[0] * Ring5Inner.MAX_RUN, lowest[1] + d[1] * Ring5Inner.MAX_RUN);
                assertTrue(Double.isNaN(further) || further > lowTread, "stair " + i + ", " + j);
                double up = INNER.base(shape[0] + d[0] * shape[2], shape[1] + d[1] * shape[2]);
                for (int[] other : steps) {
                    assertTrue(up >= INNER.base(shape[0] + other[0] * shape[2], shape[1] + other[1] * shape[2]),
                            "stair " + i + ", " + j + " does not climb the steepest way");
                }
            }
        }
        assertTrue(stairs > 30, "stairs " + stairs);
    }

    /** {@code corrupted_wastes}: pits perfectly round with a flat floor at least 2 below their rim. */
    @Test
    void pitsAreRoundAndFlatBelowTheirRim() {
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
                // Below the middle's ground and the four rim points it was measured on, by 2–4.
                int cx = (minX + maxX) / 2;
                int cz = (minZ + maxZ) / 2;
                int radius = (across - 1) / 2;
                double rim = Math.min(INNER.base(cx, cz), Math.min(Math.min(INNER.base(cx + radius, cz), INNER.base(cx - radius, cz)),
                        Math.min(INNER.base(cx, cz + radius), INNER.base(cx, cz - radius))));
                assertTrue(floor <= Math.round(rim) - Ring5Inner.MIN_PIT_DEPTH, "pit floor " + floor + " rim " + rim);
            }
        }
        assertTrue(pits > 100, "pits " + pits);
    }

    /** {@code false_meadow}: the rolling ground (no ridges) with a regular wave of ± 3 over it, the same every wave length. */
    @Test
    void meadowIsARegularWaveOnTheRollingGround() {
        for (int x = 8000; x < 8600; x += 7) {
            for (int z = -300; z < 300; z += 11) {
                double wave = INNER.meadowTop(x, z) - INNER.rolling(x, z);
                assertTrue(Math.abs(wave) <= 2.0 * Ring5Inner.MEADOW_AMPLITUDE + 1e-9);
                assertEquals(wave, Ring5Inner.meadowWave(x + (int) Ring5Inner.MEADOW_WAVE, z - 2 * (int) Ring5Inner.MEADOW_WAVE), 1e-9);
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
                        // Whole in the glitch fields: no column under the cube is outer band, castle, crack, spot or plain ground.
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

    /** {@code glitch_fields}: every block's top lies in whole blocks of {@link Ring5Inner#QUANTUM}, within the relief. */
    @Test
    void glitchBlocksFollowTheRelief() {
        int blocks = 0;
        for (int[] c : raster(13)) {
            if (INNER.glitchWeight(c[0], c[1]) < 1.0) {
                continue;
            }
            double top = INNER.glitchTop(c[0], c[1]);
            blocks++;
            assertTrue(top >= Ring5Inner.ROLL_LOW - Ring5Inner.QUANTUM && top < Ring5Inner.CAP && top % Ring5Inner.QUANTUM == 0.0,
                    "glitch top " + top + " at " + c[0] + ", " + c[1]);
        }
        assertTrue(blocks > 50, "blocks " + blocks);
    }
}
