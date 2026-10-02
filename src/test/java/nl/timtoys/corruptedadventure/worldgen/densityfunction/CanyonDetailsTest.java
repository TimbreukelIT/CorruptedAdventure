package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** canyon_details on synthetic terrain: a wall along z (wavy, with a little 3D noise) and a flat plateau for slots. */
class CanyonDetailsTest {
    private static final String RING_2 = "src/main/resources/data/corruptedadventure/worldgen/density_function/ring_2/overworld.json";
    private static final JsonObject CONFIG = readConfig();
    private static final CanyonDetails.SlotSettings SLOTS = CanyonDetails.SlotSettings.CODEC
            .parse(JsonOps.INSTANCE, CONFIG.get("slots")).getOrThrow();
    private static final CanyonDetails.WallSettings WALLS = CanyonDetails.WallSettings.CODEC
            .parse(JsonOps.INSTANCE, CONFIG.get("walls")).getOrThrow();
    /** Every band gets a ledge and a niche, at full reach and depth. */
    private static final CanyonDetails.WallSettings EVERYWHERE = CanyonDetails.WallSettings.of(WALLS.minSlope(), WALLS.minWeight(),
            WALLS.bandSettings(), new CanyonDetails.LedgeSettings(-1.0, WALLS.ledgeRamp(), WALLS.ledgeReach(), WALLS.ledgeFootDistance(),
                    WALLS.ledgeMinHeight()),
            new CanyonDetails.NicheSettings(-1.0, WALLS.nicheRamp(), WALLS.nicheMinFloor(), WALLS.nicheHeight(), WALLS.nicheDepth()),
            WALLS.rimSettings());
    private static final DensityFunction.NoiseHolder BANDS = TestFunctions.noise(5L, -5, 1.0, 0.5);
    private static final int X0 = -24;
    private static final int X1 = 40;
    private static final int Z0 = -40;
    private static final int Z1 = 40;
    private static final int Y0 = 60;
    private static final int Y1 = 160;

    /** Floor 70 at x ≤ 0, a wall of {@code slope} blocks per block, plateau 142; wavy along z. */
    private static double wallHeight(int x, int z, double slope) {
        double u = x - 4.0 * Math.sin(z / 13.0);
        return Math.max(70.0, Math.min(142.0, 70.0 + slope * u));
    }

    private static DensityFunction rawTerrain(double slope, double roughness) {
        return TestFunctions.of((x, y, z) -> (wallHeight(x, z, slope) - y) / 8.0
                + roughness * Math.sin(x * 0.9 + y * 0.45) * Math.cos(z * 0.6 - y * 0.3));
    }

    /** The terrain as the world has it: interpolated over the noise cells (the argument; caves left out). */
    private static DensityFunction terrain(double slope, double roughness) {
        CellInterpolation cells = new CellInterpolation(rawTerrain(slope, roughness));
        return TestFunctions.of(cells::sample);
    }

    private static CanyonDetails wall(double slope, double roughness, CanyonDetails.WallSettings walls) {
        DensityFunction terrain = terrain(slope, roughness);
        DensityFunction raw = rawTerrain(slope, roughness);
        DensityFunction height = TestFunctions.of((x, y, z) -> wallHeight(x, z, slope));
        CellEdges noSlots = new CellEdges(110.0, 0.8, 0.0, TestFunctions.silent(), 0.0, TestFunctions.noise(1L, 0, 1.0), 24.0);
        return new CanyonDetails(terrain, List.of(), raw, noSlots, DensityFunctions.constant(0.0), height, DensityFunctions.constant(142.0),
                DensityFunctions.constant(142.0), DensityFunctions.constant(70.0), DensityFunctions.constant(1.0), Optional.empty(),
                BANDS, TestFunctions.silent(), SLOTS, walls);
    }

    private static double at(DensityFunction f, int x, int y, int z) {
        return f.compute(new DensityFunction.SinglePointContext(x, y, z));
    }

    @Test
    void wallFeaturesNeverLeaveFloatingBlocks() {
        for (CanyonDetails.WallSettings walls : List.of(WALLS, EVERYWHERE)) {
            for (double slope : new double[]{2.5, 4.0, 7.0}) {
                CanyonDetails details = wall(slope, 0.12, walls);
                DensityFunction terrain = terrain(slope, 0.12);
                int w = X1 - X0;
                int d = Z1 - Z0;
                int h = Y1 - Y0;
                boolean[] solid = new boolean[w * d * h];
                int added = 0;
                int cut = 0;
                for (int x = X0; x < X1; x++) {
                    for (int z = Z0; z < Z1; z++) {
                        for (int y = Y0; y < Y1; y++) {
                            double value = at(details, x, y, z);
                            double before = at(terrain, x, y, z);
                            solid[index(x, y, z)] = value > 0.0;
                            added += value > 0.0 && before <= 0.0 ? 1 : 0;
                            cut += value <= 0.0 && before > 0.0 ? 1 : 0;
                        }
                    }
                }
                assertTrue(added > 0, "ledges at slope " + slope);
                assertTrue(cut > 0, "niches at slope " + slope);
                assertEquals(0, floating(solid), "floating blocks at slope " + slope);
            }
        }
    }

    private static int index(int x, int y, int z) {
        return ((y - Y0) * (Z1 - Z0) + (z - Z0)) * (X1 - X0) + (x - X0);
    }

    /** Solid blocks not connected to the bottom layer or the plateau side of the box. */
    private static int floating(boolean[] solid) {
        boolean[] seen = new boolean[solid.length];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int x = X0; x < X1; x++) {
            for (int z = Z0; z < Z1; z++) {
                for (int y = Y0; y < Y1; y++) {
                    int i = index(x, y, z);
                    if (solid[i] && (y == Y0 || x == X1 - 1 || z == Z0 || z == Z1 - 1)) {
                        seen[i] = true;
                        queue.add(new int[]{x, y, z});
                    }
                }
            }
        }
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int[] dir : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                int x = p[0] + dir[0];
                int y = p[1] + dir[1];
                int z = p[2] + dir[2];
                if (x < X0 || x >= X1 || y < Y0 || y >= Y1 || z < Z0 || z >= Z1) {
                    continue;
                }
                int i = index(x, y, z);
                if (solid[i] && !seen[i]) {
                    seen[i] = true;
                    queue.add(new int[]{x, y, z});
                }
            }
        }
        int floating = 0;
        for (int i = 0; i < solid.length; i++) {
            floating += solid[i] && !seen[i] ? 1 : 0;
        }
        return floating;
    }

    @Test
    void ledgesOnlyOutsideTheWallOnTheirBandsNichesOnlyInsideWithinTheirDepth() {
        CanyonDetails details = wall(5.0, 0.0, EVERYWHERE);
        DensityFunction terrain = terrain(5.0, 0.0);
        double maxDepth = EVERYWHERE.nicheDepth().max() + 1.0;
        for (int x = X0; x < X1; x++) {
            for (int z = Z0; z < Z1; z++) {
                CanyonDetails.Column column = details.columnAt(x, z);
                for (int y = Y0; y < Y1; y++) {
                    double value = at(details, x, y, z);
                    double before = at(terrain, x, y, z);
                    if (value > 0.0 && before <= 0.0) {
                        assertTrue(inAddRows(column, y), "ledge block outside its rows at " + x + "," + y + "," + z);
                    }
                    if (value <= 0.0 && before > 0.0) {
                        // Inside the wall, and within the niche depth of its face (downhill from the column).
                        boolean nearFace = false;
                        for (int k = 1; k <= maxDepth && !nearFace; k++) {
                            nearFace = at(terrain, (int) Math.floor(x - k * column.ux), y, (int) Math.floor(z - k * column.uz)) <= 0.0;
                        }
                        assertTrue(nearFace, "niche block too deep at " + x + "," + y + "," + z);
                        assertTrue(y >= EVERYWHERE.nicheMinFloor(), "niche floor below the minimum at " + y);
                    }
                }
            }
        }
    }

    private static boolean inAddRows(CanyonDetails.Column column, int y) {
        for (int i = 0; i < column.features; i++) {
            if (column.featureAdds[i] && y >= column.featureLo[i] && y <= column.featureHi[i]) {
                return true;
            }
        }
        return false;
    }

    @Test
    void gentleSlopesGetNothingAndOutsideTheWindowIsTheArgument() {
        CanyonDetails gentle = wall(1.0, 0.0, EVERYWHERE);
        DensityFunction terrain = terrain(1.0, 0.0);
        for (int x = X0; x < X1; x += 3) {
            for (int z = Z0; z < Z1; z += 3) {
                for (int y = Y0; y < Y1; y += 2) {
                    assertEquals(at(terrain, x, y, z), at(gentle, x, y, z), "at " + x + "," + y + "," + z);
                }
            }
        }
        CanyonDetails steep = wall(5.0, 0.0, EVERYWHERE);
        DensityFunction steepTerrain = terrain(5.0, 0.0);
        for (int x = X0; x < X1; x += 3) {
            for (int y : new int[]{-30, 0, 250, 300}) {
                assertEquals(at(steepTerrain, x, y, 5), at(steep, x, y, 5));
            }
        }
    }

    /** A flat plateau at 130 cut by slots (every edge open, no warp). */
    private static CanyonDetails plateau() {
        DensityFunction terrain = TestFunctions.of((x, y, z) -> (130.0 - y) / 8.0);
        CellEdges slots = new CellEdges(110.0, 0.8, 1.0, TestFunctions.silent(), 0.0, TestFunctions.noise(9L, 0, 1.0), 24.0);
        return new CanyonDetails(terrain, List.of(), terrain, slots, DensityFunctions.constant(1.0), DensityFunctions.constant(130.0),
                DensityFunctions.constant(130.0), DensityFunctions.constant(130.0), DensityFunctions.constant(69.0),
                DensityFunctions.constant(0.0), Optional.empty(), BANDS, TestFunctions.silent(), SLOTS, WALLS);
    }

    @Test
    void slotsHaveTheirWidthAndNeverCutBelowTheirBottom() {
        CanyonDetails details = plateau();
        double floorLimit = Math.max(SLOTS.minBottom(), 69.0 + SLOTS.floorClearance());
        int slotColumns = 0;
        for (int x = 0; x < 330; x++) {
            for (int z = 0; z < 330; z += 3) {
                CanyonDetails.Column column = details.columnAt(x, z);
                boolean top = at(details, x, 129, z) < 0.0;
                if (top) {
                    slotColumns++;
                    assertTrue(column.slot && column.slotDistance < SLOTS.width().max() / 2.0, "too wide at " + x + "," + z);
                    assertTrue(column.slotBottom >= floorLimit && column.slotBottom <= 130.0 - SLOTS.depth().min() + 0.01);
                }
                for (int y = 60; y <= (int) floorLimit; y++) {
                    assertTrue(at(details, x, y, z) > 0.0, "cut below the slot bottom at " + x + "," + y + "," + z);
                }
            }
        }
        assertTrue(slotColumns > 100, "slot columns " + slotColumns);
    }

    @Test
    void bridgesComeAtTheirSpacing() {
        CanyonDetails details = plateau();
        int bridges = 0;
        double length = 0.0;
        for (long seed = 1; seed <= 20; seed++) {
            boolean in = false;
            for (double along = -5_000.0; along < 5_000.0; along += 0.25) {
                CanyonDetails.Bridge bridge = details.bridge(new CellEdges.Edge(0.0, seed * 0x9E3779B97F4A7C15L, along), 90.0, 130.0);
                if (bridge != null && !in) {
                    bridges++;
                }
                if (bridge != null) {
                    length += 0.25;
                    assertTrue(bridge.hi() >= bridge.lo());
                    assertTrue(bridge.chockstone() ? bridge.lo() > 90 && bridge.hi() < 130 : bridge.hi() >= 130);
                }
                in = bridge != null;
            }
        }
        double expected = 20 * 10_000.0 / SLOTS.bridgeSpacing();
        assertEquals(expected, bridges, expected * 0.1);
        double mean = length / bridges;
        assertTrue(mean >= SLOTS.bridgeLength().min() - 0.5 && mean <= SLOTS.bridgeLength().max() + 0.5, "mean bridge length " + mean);
    }

    @Test
    void rocksAreMergedFromTheColumnTable() {
        DensityFunction terrain = TestFunctions.of((x, y, z) -> (100.0 - y) / 8.0);
        RockPillars rocks = new RockPillars(TestFunctions.noise(3L, 0, 1.0), 24, 1.0, 1, DensityFunctions.constant(1.0), 0.3,
                DensityFunctions.constant(100.0), Optional.empty(), 0.0, 1024.0, Optional.of(50),
                List.of(new RockPillars.WeightedShape(1, new Shape.Boulder(new Range(2, 4), new Range(0.6, 0.9), new Range(0.3, 0.5)))));
        CellEdges noSlots = new CellEdges(110.0, 0.8, 0.0, TestFunctions.silent(), 0.0, TestFunctions.noise(1L, 0, 1.0), 24.0);
        CanyonDetails details = new CanyonDetails(terrain, List.of(rocks), terrain, noSlots, DensityFunctions.constant(0.0),
                DensityFunctions.constant(100.0), DensityFunctions.constant(100.0), DensityFunctions.constant(100.0),
                DensityFunctions.constant(69.0), DensityFunctions.constant(0.0), Optional.empty(), BANDS, TestFunctions.silent(), SLOTS, WALLS);
        int solid = 0;
        for (int x = 0; x < 48; x++) {
            for (int z = 0; z < 48; z++) {
                for (int y = 40; y < 120; y++) {
                    double expected = Math.max(at(terrain, x, y, z), at(rocks, x, y, z));
                    assertEquals(expected > 0.0, at(details, x, y, z) > 0.0, "at " + x + "," + y + "," + z);
                    solid += at(rocks, x, y, z) > 0.0 && at(terrain, x, y, z) <= 0.0 ? 1 : 0;
                }
            }
        }
        assertTrue(solid > 0);
    }

    @Test
    void settingsCodecsRoundTripAndValidate() {
        // The density function itself refers to named functions (a registry); its settings are checked here.
        assertEquals(SLOTS, CanyonDetails.SlotSettings.CODEC.parse(JsonOps.INSTANCE,
                CanyonDetails.SlotSettings.CODEC.encodeStart(JsonOps.INSTANCE, SLOTS).getOrThrow()).getOrThrow());
        assertEquals(WALLS, CanyonDetails.WallSettings.CODEC.parse(JsonOps.INSTANCE,
                CanyonDetails.WallSettings.CODEC.encodeStart(JsonOps.INSTANCE, WALLS).getOrThrow()).getOrThrow());
        JsonElement walls = CanyonDetails.WallSettings.CODEC.encodeStart(JsonOps.INSTANCE, WALLS).getOrThrow();
        assertEquals(CONFIG.getAsJsonObject("walls").getAsJsonObject("bands"), walls.getAsJsonObject().getAsJsonObject("bands"));
        JsonObject badSlots = CONFIG.getAsJsonObject("slots").deepCopy();
        badSlots.addProperty("bridge_jitter", 60.0);
        assertTrue(CanyonDetails.SlotSettings.CODEC.parse(JsonOps.INSTANCE, badSlots).error().isPresent());
        JsonObject badBands = CONFIG.getAsJsonObject("walls").deepCopy();
        badBands.getAsJsonObject("bands").add("cream", JsonParser.parseString("[[40, 50]]"));
        assertTrue(CanyonDetails.WallSettings.CODEC.parse(JsonOps.INSTANCE, badBands).error().isPresent());
        JsonObject badRange = CONFIG.getAsJsonObject("walls").deepCopy();
        badRange.getAsJsonObject("niches").add("depth", JsonParser.parseString("[8.0, 3.0]"));
        assertTrue(CanyonDetails.WallSettings.CODEC.parse(JsonOps.INSTANCE, badRange).error().isPresent());
        CanyonDetails details = wall(4.0, 0.0, WALLS);
        assertTrue(details.minValue() <= -1.0 && details.maxValue() >= 1.0);
    }

    private static JsonObject readConfig() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(RING_2);
            if (Files.isRegularFile(candidate)) {
                try {
                    return JsonParser.parseString(Files.readString(candidate)).getAsJsonObject();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return fail("Could not find " + RING_2);
    }
}
