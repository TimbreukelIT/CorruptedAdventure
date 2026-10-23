package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dressing of Nether ring 5 (96, {@link NetherRing5Dressing}, as {@code ring_5/nether/terrain.json} configures it):
 * a light band per grid column at its own height, a white core in about one column in ten, the giant column's vein and
 * the imprint round its foot.
 */
class NetherRing5DressingTest {
    private static final NetherCathedrals TERRAIN = NetherCathedralsTest.terrain(NetherCathedralsTest.SEED);
    private static final NetherCathedrals.HallSettings HALLS = TERRAIN.settings().halls();
    private static final NetherRing5Dressing.Settings DRESSING = TERRAIN.settings().dressing();
    private static final List<Integer> LEVELS = List.of(38, 44, 50, 56);
    private static final int MIDDLE = 67;

    /** The low corner of every grid column that stands in Nether ring 5 on {@code field}. */
    static List<int[]> gridColumns(NetherCathedrals field) {
        int size = field.settings().cathedrals().cellSize(), grid = field.settings().halls().grid();
        int reach = NetherCathedralsTest.REACH;
        List<int[]> found = new ArrayList<>();
        for (int cz = Math.floorDiv(-reach, size); cz <= Math.floorDiv(reach, size); cz++) {
            for (int cx = Math.floorDiv(-reach, size); cx <= Math.floorDiv(reach, size); cx++) {
                NetherCathedrals.Cell cell = field.cellAt(cx * size, cz * size);
                for (int px = cx * size + cell.gridX() - grid; px < (cx + 1) * size; px += grid) {
                    for (int pz = cz * size + cell.gridZ() - grid; pz < (cz + 1) * size; pz += grid) {
                        if (px >= cx * size && pz >= cz * size && field.gridPillarCorner(cell, px, pz) == pack(px, pz)) {
                            found.add(new int[]{px, pz});
                        }
                    }
                }
            }
        }
        return found;
    }

    private static long pack(int x, int z) {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }

    private static void eachColumnOf(int[] corner, Consumer<int[]> visit) {
        for (int u = 0; u < HALLS.pillarSize(); u++) {
            for (int v = 0; v < HALLS.pillarSize(); v++) {
                visit.accept(new int[]{corner[0] + u, corner[1] + v, u, v});
            }
        }
    }

    @Test
    void settingsAreThePlans() {
        assertEquals(38.0, DRESSING.lightY().min());
        assertEquals(56.0, DRESSING.lightY().max());
        assertEquals(6, DRESSING.lightStep());
        assertEquals(2, DRESSING.lightHeight());
        assertEquals(0.1, DRESSING.coreChance());
        assertEquals(MIDDLE, NetherRing5Dressing.Settings.middle(HALLS));
        assertEquals(0.0, DRESSING.veinRadius());
        assertEquals(8, DRESSING.imprintWidth());
        assertEquals(1, DRESSING.imprintBorder());
    }

    /**
     * Every grid column: one band of still light 2 high round its outer ring (its 12 outer columns; none in the 4 inner
     * ones, where it would never be seen), its lowest Y one of 38/44/50/56 (each level used), above the highest plain and
     * below the core; never on the floor.
     */
    @Test
    void everyGridColumnHasItsOwnLightBand() {
        List<int[]> columns = gridColumns(TERRAIN);
        assertTrue(columns.size() > 5000, "grid columns: " + columns.size());
        int[] perLevel = new int[LEVELS.size()];
        int plainsTop = HALLS.floorTop() + (int) HALLS.plainsHeight().max();
        for (int[] corner : columns) {
            int from = TERRAIN.dressing(corner[0], corner[1]).lightFrom();
            assertTrue(LEVELS.contains(from), "band at " + from);
            assertTrue(from > plainsTop && from + 1 < MIDDLE - 1);
            perLevel[LEVELS.indexOf(from)]++;
            eachColumnOf(corner, c -> {
                NetherRing5Dressing.Column column = TERRAIN.dressing(c[0], c[1]);
                boolean outer = c[2] == 0 || c[3] == 0 || c[2] == 3 || c[3] == 3;
                assertEquals(from, column.lightFrom(), "one band per column at " + c[0] + " " + c[1]);
                assertEquals(outer ? from + 1 : from - 1, column.lightTo(), "the band only round the outside");
                assertEquals(null, column.floor());
            });
        }
        for (int count : perLevel) {
            assertTrue(count > columns.size() / 8, "every level used: " + java.util.Arrays.toString(perLevel));
        }
    }

    /**
     * About one grid column in ten holds a core: 1–3 ore stacked in exactly one of its four inner columns (invisible from
     * outside), centred on Y 67; no other column of it holds ore. Sizes 1, 2 and 3 all occur.
     */
    @Test
    void aboutOneGridColumnInTenHasAWhiteCore() {
        List<int[]> columns = gridColumns(TERRAIN);
        int cores = 0;
        Set<Integer> sizes = new TreeSet<>();
        for (int[] corner : columns) {
            List<int[]> ore = new ArrayList<>();
            eachColumnOf(corner, c -> {
                NetherRing5Dressing.Column column = TERRAIN.dressing(c[0], c[1]);
                if (column.oreFrom() <= column.oreTo()) {
                    ore.add(new int[]{c[2], c[3], column.oreFrom(), column.oreTo()});
                }
            });
            assertTrue(ore.size() <= 1, "one core column");
            if (ore.isEmpty()) {
                continue;
            }
            cores++;
            int[] core = ore.getFirst();
            assertTrue(core[0] >= 1 && core[0] <= 2 && core[1] >= 1 && core[1] <= 2, "an inner column");
            int size = core[3] - core[2] + 1;
            sizes.add(size);
            assertTrue(size >= 1 && size <= 3, "1-3 ore");
            assertTrue(core[2] <= MIDDLE && core[3] >= MIDDLE && core[2] >= MIDDLE - 1 && core[3] <= MIDDLE + 1, "halfway up");
        }
        double share = cores / (double) columns.size();
        assertTrue(share > 0.07 && share < 0.13, "cores " + share);
        assertEquals(Set.of(1, 2, 3), sizes);
    }

    /**
     * Each cathedral: ore only up the giant column's middle column, from above the floor (Y 32) to below the roof (Y 119);
     * no light band anywhere in it; the floor round the foot an imprint 8 wide, bricks on its inner and outer block,
     * polished between, nothing beyond.
     */
    @Test
    void giantColumnHasItsVeinAndImprint() {
        for (NetherCathedrals.Cathedral cathedral : NetherCathedralsTest.all(TERRAIN)) {
            int reach = cathedral.pillarRadius() + DRESSING.imprintWidth() + 3;
            int polished = 0, bricks = 0;
            for (int x = cathedral.x() - reach; x <= cathedral.x() + reach; x++) {
                for (int z = cathedral.z() - reach; z <= cathedral.z() + reach; z++) {
                    NetherRing5Dressing.Column column = TERRAIN.dressing(x, z);
                    assertTrue(column.lightFrom() > column.lightTo(), "no band in a cathedral");
                    boolean middle = x == cathedral.x() && z == cathedral.z();
                    assertEquals(middle ? HALLS.floorTop() + 1 : 1, column.oreFrom(), "vein at " + x + " " + z);
                    assertEquals(middle ? HALLS.roof() - 1 : 0, column.oreTo());
                    double t = cathedral.distance(x, z) - cathedral.pillarRadius();
                    NetherRing5Dressing.Part expected = t <= 0.0 || t > 8.0 ? null
                            : t <= 1.0 || t > 7.0 ? NetherRing5Dressing.Part.IMPRINT_BRICKS : NetherRing5Dressing.Part.IMPRINT_POLISHED;
                    assertEquals(expected, column.floor(), "imprint at " + x + " " + z);
                    assertEquals(t <= 0.0, TERRAIN.pillarAt(x, z), "the imprint starts at the foot");
                    polished += expected == NetherRing5Dressing.Part.IMPRINT_POLISHED ? 1 : 0;
                    bricks += expected == NetherRing5Dressing.Part.IMPRINT_BRICKS ? 1 : 0;
                }
            }
            assertTrue(polished > 2 * bricks / 3 && bricks > 0, "polished " + polished + ", bricks " + bricks);
        }
    }

    /** Hall floor and walls away from the giant columns: no dressing. */
    @Test
    void nothingElsewhere() {
        int sampled = 0;
        for (int x = -NetherCathedralsTest.REACH; x <= NetherCathedralsTest.REACH; x += 7) {
            for (int z = -NetherCathedralsTest.REACH; z <= NetherCathedralsTest.REACH; z += 7) {
                if (TERRAIN.pillarAt(x, z)) {
                    continue;
                }
                NetherCathedrals.Cathedral cathedral = TERRAIN.cathedralAt(x, z);
                if (cathedral != null && cathedral.distance(x, z) <= cathedral.pillarRadius() + DRESSING.imprintWidth()) {
                    continue;
                }
                assertSame(NetherRing5Dressing.NONE, TERRAIN.dressing(x, z), "nothing at " + x + " " + z);
                sampled++;
            }
        }
        assertTrue(sampled > 100_000, "sampled " + sampled);
    }

    /** The same seed, the same dressing; another seed, other bands. */
    @Test
    void pureForTheSeed() {
        NetherCathedrals again = NetherCathedralsTest.terrain(NetherCathedralsTest.SEED);
        NetherCathedrals other = NetherCathedralsTest.terrain(NetherCathedralsTest.SEED + 1);
        List<int[]> columns = gridColumns(TERRAIN);
        int differ = 0;
        for (int[] corner : columns.subList(0, 500)) {
            for (int u = 0; u < 4; u++) {
                assertEquals(TERRAIN.dressing(corner[0] + u, corner[1] + u), again.dressing(corner[0] + u, corner[1] + u));
            }
            // the grids of another seed lie elsewhere, so compare the choice per corner itself
            long salt = NetherRing5Dressing.PILLAR_SALT;
            differ += TERRAIN.pillarHash(corner[0], corner[1], salt) != other.pillarHash(corner[0], corner[1], salt) ? 1 : 0;
        }
        assertEquals(500, differ, "another seed, other choices");
    }

    /** The settings refuse a band in the plains or on the core, a vein outside the giant column, an imprint without room. */
    @Test
    void settingsRefuseADressingThatDoesNotFit() {
        assertTrue(parse(d -> d.add("light_y", range(36, 56))).isError(), "band in a plain");
        assertTrue(parse(d -> d.add("light_y", range(38, 68))).isError(), "band on the core");
        assertTrue(parse(d -> d.addProperty("vein_radius", 10.0)).isError(), "vein outside the giant column");
        assertTrue(parse(d -> d.addProperty("imprint_border", 5)).isError(), "borders wider than the imprint");
        assertTrue(parse(d -> d.addProperty("imprint_width", 40)).isError(), "imprint past the lifted roof");
        assertTrue(parse(d -> { }).isSuccess(), "the plan's settings");
    }

    private static JsonArray range(int min, int max) {
        JsonArray array = new JsonArray();
        array.add(new JsonPrimitive(min));
        array.add(new JsonPrimitive(max));
        return array;
    }

    private static com.mojang.serialization.DataResult<NetherCathedrals.Settings> parse(Consumer<JsonObject> change) {
        JsonObject settings = WorldgenData.read(NetherCathedralsTest.DIR + "terrain.json").getAsJsonObject("settings").deepCopy();
        change.accept(settings.getAsJsonObject("dressing"));
        return NetherCathedrals.Settings.CODEC.parse(JsonOps.INSTANCE, settings);
    }
}
