package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The per-block profile of ring 5's columns ({@link Ring5Field#terrain}, {@code caveCode}, {@code caveZone}; and {@code
 * ownCave}, without one): the same values in any order of queries (the table's window moves and its columns are refilled
 * between them), blocks outside the profile's heights like the ground round them, the heights the noise settings', the
 * rule the glitch layers rely on, and the per-block rules from before the profile, also where the parts overlap.
 */
class Ring5ProfileTest {
    private static final RingLayout LAYOUT = Ring5FieldTest.LAYOUT;
    private static final int SIDE = 24;
    private static final int MIN_Y = Ring5Field.PROFILE_MIN_Y;
    private static final int END_Y = Ring5Field.PROFILE_MIN_Y + Ring5Field.PROFILE_HEIGHT;
    private static final int FUNCTIONS = 4;

    /** Corners of patches with glitch layers, a doppelganger's layered core, a void cave's hole and a castle bridge. */
    private static List<int[]> patches(Ring5Field f) {
        List<int[]> patches = new ArrayList<>();
        patches.add(firstLayered(f));
        int cells = 9800 / Ring5Copies.CELL + 1;
        int mid = Ring5Copies.SIZE / 2;
        search:
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Ring5Copies.Pair pair = f.copyPair(i, j);
                if (pair != null && f.column(pair.tx() + mid, pair.tz() + mid).layered) {
                    patches.add(new int[]{pair.tx() - 4, pair.tz() - 4});
                    break search;
                }
            }
        }
        cells = 9800 / Ring5Caves.CHAMBER_CELL + 1;
        search:
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Ring5Caves.Chamber chamber = f.chamber(i, j);
                if (chamber != null && !chamber.holes().isEmpty()) {
                    double[] hole = chamber.holes().getFirst();
                    patches.add(new int[]{(int) Math.floor(hole[0]) - SIDE / 2, (int) Math.floor(hole[1]) - SIDE / 2});
                    break search;
                }
            }
        }
        Ring5Field.Bridge bridge = f.castleBridges().getFirst();
        patches.add(new int[]{(int) Math.floor(0.5 * (bridge.fromX() + bridge.toX())) - SIDE / 2,
                (int) Math.floor(0.5 * (bridge.fromZ() + bridge.toZ())) - SIDE / 2});
        assertEquals(4, patches.size(), "patches found");
        return patches;
    }

    private static int[] firstLayered(Ring5Field f) {
        for (int distance = 8150; distance <= 9400; distance += 41) {
            int count = (int) (2.0 * Math.PI * distance / 41);
            for (int k = 0; k < count; k++) {
                double angle = 2.0 * Math.PI * (k + 0.5) / count;
                int x = (int) Math.floor(distance * Math.cos(angle));
                int z = (int) Math.floor(distance * Math.sin(angle));
                if (f.column(x, z).layered) {
                    return new int[]{x - SIDE / 2, z - SIDE / 2};
                }
            }
        }
        throw new AssertionError("no glitch layers");
    }

    private static long value(Ring5Field f, int function, int x, int y, int z) {
        return switch (function) {
            case 0 -> Double.doubleToRawLongBits(f.terrain(x, y, z));
            case 1 -> f.caveCode(x, y, z);
            case 2 -> f.ownCave(x, y, z) ? 1 : 0;
            default -> f.caveZone(x, y, z);
        };
    }

    private static void shuffle(int[] values, Random random) {
        for (int k = values.length - 1; k > 0; k--) {
            int other = random.nextInt(k + 1);
            int value = values[k];
            values[k] = values[other];
            values[other] = value;
        }
    }

    /**
     * One field asked column by column, bottom to top; another per patch in a random order of columns, outputs and
     * chunks of heights, then in single queries hopping between the patches: the same values, bit for bit.
     */
    @Test
    void sameValuesInAnyOrder() {
        List<int[]> patches = patches(Ring5FieldTest.field());
        Ring5Field ordered = Ring5FieldTest.field();
        int heights = END_Y - MIN_Y;
        int columns = SIDE * SIDE;
        long[] expected = new long[patches.size() * columns * heights * FUNCTIONS];
        for (int p = 0; p < patches.size(); p++) {
            int[] corner = patches.get(p);
            for (int column = 0; column < columns; column++) {
                for (int h = 0; h < heights; h++) {
                    for (int function = 0; function < FUNCTIONS; function++) {
                        expected[((p * columns + column) * heights + h) * FUNCTIONS + function] = value(ordered, function,
                                corner[0] + column / SIDE, MIN_Y + h, corner[1] + column % SIDE);
                    }
                }
            }
        }
        Ring5Field shuffled = Ring5FieldTest.field();
        Random random = new Random(Ring5FieldTest.SEED);
        int chunks = 6;
        int[] order = new int[heights];
        for (int h = 0; h < heights; h++) {
            order[h] = h;
        }
        int wrong = 0;
        for (int p = 0; p < patches.size(); p++) {
            int[] corner = patches.get(p);
            shuffle(order, random);
            int[] tasks = new int[columns * FUNCTIONS * chunks];
            for (int t = 0; t < tasks.length; t++) {
                tasks[t] = t;
            }
            shuffle(tasks, random);
            for (int task : tasks) {
                int chunk = task % chunks;
                int function = task / chunks % FUNCTIONS;
                int column = task / (chunks * FUNCTIONS);
                for (int k = chunk * heights / chunks; k < (chunk + 1) * heights / chunks; k++) {
                    int h = order[k];
                    long got = value(shuffled, function, corner[0] + column / SIDE, MIN_Y + h, corner[1] + column % SIDE);
                    wrong += got == expected[((p * columns + column) * heights + h) * FUNCTIONS + function] ? 0 : 1;
                }
            }
        }
        for (int q = 0; q < 50_000; q++) {
            int p = random.nextInt(patches.size());
            int column = random.nextInt(columns);
            int h = random.nextInt(heights);
            int function = random.nextInt(FUNCTIONS);
            int[] corner = patches.get(p);
            long got = value(shuffled, function, corner[0] + column / SIDE, MIN_Y + h, corner[1] + column % SIDE);
            wrong += got == expected[((p * columns + column) * heights + h) * FUNCTIONS + function] ? 0 : 1;
        }
        assertEquals(0, wrong, "queries that depend on the order");
    }

    /**
     * Outside the profile's heights a block is painted on its own: a hole runs on down into the void, full inner ground
     * on down to the bedrock and below, air above the world.
     */
    @Test
    void blocksOutsideTheHeightsAreLikeTheGroundRoundThem() {
        Ring5Field f = Ring5FieldTest.field();
        int holes = 0;
        int ground = 0;
        for (int[] corner : patches(Ring5FieldTest.field())) {
            for (int x = corner[0]; x < corner[0] + SIDE; x++) {
                for (int z = corner[1]; z < corner[1] + SIDE; z++) {
                    for (int function = 0; function < FUNCTIONS; function++) {
                        assertEquals(value(f, function, x, MIN_Y, z), value(f, function, x, MIN_Y - 1, z),
                                "below the world at " + x + ", " + z);
                        assertEquals(value(f, function, x, MIN_Y, z), value(f, function, x, MIN_Y - 300, z),
                                "far below the world at " + x + ", " + z);
                    }
                    assertEquals(-1.0, f.terrain(x, END_Y, z), "above the world at " + x + ", " + z);
                    holes += f.caveCode(x, MIN_Y - 1, z) == 3 ? 1 : 0;
                    ground += f.terrain(x, MIN_Y - 1, z) == 1.0 ? 1 : 0;
                }
            }
        }
        assertTrue(holes > 0 && ground > 0, "holes " + holes + ", ground " + ground);
    }

    /**
     * The profile's heights are the overworld noise settings' (the ones the preset's overworld generates with): every
     * block the noise chunk asks for has an entry, none is painted on its own.
     */
    @Test
    void theHeightsAreTheOverworldNoiseSettings() {
        JsonObject generator = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
        assertEquals("corruptedadventure:overworld", generator.get("settings").getAsString());
        JsonObject noise = WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("noise");
        assertEquals(noise.get("min_y").getAsInt(), Ring5Field.PROFILE_MIN_Y);
        assertEquals(noise.get("height").getAsInt(), Ring5Field.PROFILE_HEIGHT);
    }

    // The per-block rules from before the profile (main 2b61b386), straight from a column's intervals. Where the parts
    // overlap: terrain span > own cave > void > glitch layers > formula; caveCode first own cave > rim (spans do not
    // count); caveZone void or closed column > span > layers > the noise caves' zone (own caves do not count).

    private static double oldTerrain(Ring5Field f, Ring5Field.Column column, int x, int y, int z) {
        for (int k = 0; k < column.spans; k++) {
            if (y >= column.spanBottom[k] && y < column.spanTop[k]) {
                return 1.0;
            }
        }
        for (int k = 0; k < column.caves; k++) {
            if (y >= column.caveBottom[k] && y < column.caveTop[k]) {
                return -1.0;
            }
        }
        if (column.isVoid()) {
            return -1.0;
        }
        if (column.layered && y >= column.top - Ring5Inner.LAYER_DEPTH && y < column.top + Ring5Inner.LAYER_DEPTH) {
            Ring5Copies.Pair copy = column.copy;
            int[] shift = copy == null ? f.inner().layerShift(x, y, z) : f.inner().layerShift(column.sourceX, y, column.sourceZ);
            int dx = copy != null && copy.alongX() ? -shift[0] : shift[0];
            int dz = copy != null && !copy.alongX() ? -shift[1] : shift[1];
            Ring5Field.Column other = f.column(x + dx, z + dz);
            return y < (other.isVoid() ? other.height : other.top) ? 1.0 : -1.0;
        }
        double density = 0.125 * (column.top - y);
        if (column.bottom != Double.NEGATIVE_INFINITY) {
            density = Math.min(density, 0.125 * (y - column.bottom));
        }
        return Math.max(-1.0, Math.min(1.0, density));
    }

    private static int oldCaveCode(Ring5Field.Column column, int y) {
        for (int k = 0; k < column.caves; k++) {
            if (y >= column.caveBottom[k] && y < column.caveTop[k]) {
                return column.caveKind[k];
            }
        }
        if (column.rimTop != Integer.MIN_VALUE && y >= column.rimTop - Ring5Caves.RIM_DEPTH && y < column.rimTop) {
            return Ring5Caves.Kind.RIM.code();
        }
        return Ring5Caves.Kind.NONE.code();
    }

    private static int oldCaveZone(Ring5Field.Column column, int y) {
        if (column.isVoid() || column.closure >= Ring5Field.CAVES_CLOSED) {
            return 0;
        }
        for (int k = 0; k < column.spans; k++) {
            if (y >= column.spanBottom[k] && y < column.spanTop[k]) {
                return 0;
            }
        }
        double top = column.top;
        if (column.layered) {
            top -= Ring5Inner.LAYER_DEPTH;
            if (y >= top) {
                return 0;
            }
        }
        return y < top - Ring5Field.NATURAL_COVER && y >= column.bottom + Ring5Field.NATURAL_COVER ? 2 : 1;
    }

    /** A block where the profile ({@code profiled}'s, or the field's own for null) differs from the old rules, or null. */
    private static String compare(Ring5Field f, Ring5Field.Column profiled, Ring5Field.Column old, int x, int y, int z) {
        String at = " at " + x + ", " + y + ", " + z;
        double terrain = profiled == null ? f.terrain(x, y, z) : f.terrain(profiled, x, y, z);
        double oldTerrain = oldTerrain(f, old, x, y, z);
        if (Double.doubleToRawLongBits(terrain) != Double.doubleToRawLongBits(oldTerrain)) {
            return "terrain " + terrain + " != " + oldTerrain + at;
        }
        int code = profiled == null ? f.caveCode(x, y, z) : f.caveCode(profiled, x, y, z);
        if (code != oldCaveCode(old, y)) {
            return "caveCode " + code + " != " + oldCaveCode(old, y) + at;
        }
        int zone = profiled == null ? f.caveZone(x, y, z) : f.caveZone(profiled, x, y, z);
        return zone == oldCaveZone(old, y) ? null : "caveZone " + zone + " != " + oldCaveZone(old, y) + at;
    }

    /** The old rules on the ground of the patches, at every height of the world and a few outside it. */
    @Test
    void theOldRulesOnTheGround() {
        Ring5Field f = Ring5FieldTest.field();
        Ring5Field oracle = Ring5FieldTest.field();
        int[] outside = {MIN_Y - 300, MIN_Y - 1, END_Y, END_Y + 80};
        List<String> wrong = new ArrayList<>();
        int layered = 0;
        int copies = 0;
        int spans = 0;
        int caves = 0;
        int rims = 0;
        int voids = 0;
        for (int[] corner : patches(Ring5FieldTest.field())) {
            for (int x = corner[0]; x < corner[0] + SIDE; x++) {
                for (int z = corner[1]; z < corner[1] + SIDE; z++) {
                    Ring5Field.Column c = oracle.column(x, z);
                    layered += c.layered ? 1 : 0;
                    copies += c.copy != null ? 1 : 0;
                    spans += c.spans > 0 ? 1 : 0;
                    caves += c.caves > 0 ? 1 : 0;
                    rims += c.rimTop != Integer.MIN_VALUE ? 1 : 0;
                    voids += c.isVoid() ? 1 : 0;
                    for (int h = 0; h < END_Y - MIN_Y + outside.length; h++) {
                        int y = h < END_Y - MIN_Y ? MIN_Y + h : outside[h - (END_Y - MIN_Y)];
                        // The old layers read a neighbour, which may refill this column's object: asked anew each time.
                        String bad = compare(f, null, oracle.column(x, z), x, y, z);
                        if (bad != null && wrong.size() < 20) {
                            wrong.add(bad);
                        }
                    }
                }
            }
        }
        assertTrue(layered > 0 && copies > 0 && spans > 0 && caves > 0 && rims > 0 && voids > 0, "columns: layered "
                + layered + ", copies " + copies + ", spans " + spans + ", caves " + caves + ", rims " + rims + ", void " + voids);
        assertTrue(wrong.isEmpty(), String.join("\n", wrong));
    }

    /**
     * The old rules where the parts overlap, which real ground seldom has: made-up columns in a glitch field (their
     * layers read the real columns round them), with spans, own caves of different kinds, a rim, void, closure and a
     * doppelganger's mirroring over each other.
     */
    @Test
    void theOldRulesWhereThePartsOverlap() {
        Ring5Field f = Ring5FieldTest.field();
        int[] corner = firstLayered(f);
        int x = corner[0] + SIDE / 2;
        int z = corner[1] + SIDE / 2;
        double ground = Math.floor(f.column(x, z).top());
        Ring5Caves.Kind[] kinds = {Ring5Caves.Kind.CORRIDOR, Ring5Caves.Kind.CHAMBER, Ring5Caves.Kind.HOLE};
        Random random = new Random(Ring5FieldTest.SEED);
        List<String> wrong = new ArrayList<>();
        // Blocks in: a span and a cave, two caves, a cave and the rim, the layers and a span or cave, void and a span or
        // cave, the layers and nothing else (plain, mirrored).
        long[] overlaps = new long[7];
        for (int n = 0; n < 3000; n++) {
            Ring5Field.Column c = new Ring5Field.Column();
            c.reset();
            double top = ground + random.nextInt(9) - 4 + (random.nextBoolean() ? 0.0 : random.nextDouble());
            if (random.nextInt(6) == 0) {
                c.setVoid();
            } else {
                c.top = top;
                c.bottom = random.nextBoolean() ? Double.NEGATIVE_INFINITY : top - 4 - random.nextInt(60) - random.nextDouble();
            }
            c.layered = random.nextInt(3) > 0;
            c.closure = random.nextInt(3) == 0 ? Ring5Field.CAVES_CLOSED : random.nextDouble() * Ring5Field.CAVES_CLOSED;
            if (random.nextInt(3) == 0) {
                c.copy = new Ring5Copies.Pair(x - 40, z, x, z, random.nextBoolean(), 0);
                c.sourceX = x - 40 + random.nextInt(8);
                c.sourceZ = z + random.nextInt(8);
            }
            int around = (int) ground;
            for (int k = random.nextInt(4); k > 0; k--) {
                int from = around - 30 + random.nextInt(50);
                c.addSpan(from, random.nextInt(8) == 0 ? END_Y + 50 : from + 1 + random.nextInt(12));
            }
            for (int k = random.nextInt(5); k > 0; k--) {
                int from = random.nextInt(8) == 0 ? MIN_Y - 1000 : around - 30 + random.nextInt(50);
                c.addCave(from, (from < MIN_Y ? around - 30 : from + 1) + random.nextInt(30), kinds[random.nextInt(kinds.length)]);
            }
            if (random.nextBoolean()) {
                c.rimTop = around - 10 + random.nextInt(20);
            }
            f.buildProfile(c, x, z);
            for (int y = MIN_Y; y < END_Y; y++) {
                String bad = compare(f, c, c, x, y, z);
                if (bad != null && wrong.size() < 20) {
                    wrong.add(bad + " (column " + n + ")");
                }
                int inSpan = 0;
                for (int k = 0; k < c.spans; k++) {
                    inSpan += y >= c.spanBottom[k] && y < c.spanTop[k] ? 1 : 0;
                }
                int inCave = 0;
                for (int k = 0; k < c.caves; k++) {
                    inCave += y >= c.caveBottom[k] && y < c.caveTop[k] ? 1 : 0;
                }
                boolean inRim = c.rimTop != Integer.MIN_VALUE && y >= c.rimTop - Ring5Caves.RIM_DEPTH && y < c.rimTop;
                boolean inLayers = c.layered && !c.isVoid() && y >= c.top - Ring5Inner.LAYER_DEPTH
                        && y < c.top + Ring5Inner.LAYER_DEPTH;
                boolean decided = inSpan + inCave > 0;
                overlaps[0] += inSpan > 0 && inCave > 0 ? 1 : 0;
                overlaps[1] += inCave > 1 ? 1 : 0;
                overlaps[2] += inCave > 0 && inRim ? 1 : 0;
                overlaps[3] += inLayers && decided ? 1 : 0;
                overlaps[4] += c.isVoid() && decided ? 1 : 0;
                overlaps[5] += inLayers && !decided && c.copy == null ? 1 : 0;
                overlaps[6] += inLayers && !decided && c.copy != null ? 1 : 0;
            }
        }
        assertTrue(Arrays.stream(overlaps).allMatch(count -> count > 0), "overlaps " + Arrays.toString(overlaps));
        assertTrue(wrong.isEmpty(), String.join("\n", wrong));
    }

    /** The layers copy a column's {@code height} as its ground: a solid column's height is its top. */
    @Test
    void aSolidColumnsHeightIsItsTop() {
        Ring5Field f = Ring5FieldTest.field();
        int solid = 0;
        for (int[] corner : patches(Ring5FieldTest.field())) {
            for (int x = corner[0] - 8; x < corner[0] + SIDE + 8; x++) {
                for (int z = corner[1] - 8; z < corner[1] + SIDE + 8; z++) {
                    Ring5Field.Column column = f.column(x, z);
                    if (!column.isVoid()) {
                        assertEquals(column.top(), column.height(), 0.0, x + ", " + z);
                        solid++;
                    }
                }
            }
        }
        assertTrue(solid > 0);
    }
}
