package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Change 39f on the test seed: islands below the clouds with real lava falls, columnar pillars and rocky undersides,
 * collapses instead of cave entrances, rift arches per valley, the micro relief's raised blocks, the thin kinds of the
 * surface, and the read-only accessors (each one what the terrain uses).
 */
class Ring3PolishTest {
    private static final double CLOUDS = 192.0;
    private static VolcanicField field;
    private static List<Volcano> volcanoes;

    @BeforeAll
    static void setUp() {
        field = VolcanicFieldTest.field();
        volcanoes = new ArrayList<>();
        int size = field.settings().cellSize();
        for (int i = -6200 / size - 1; i <= 6200 / size; i++) {
            for (int j = -6200 / size - 1; j <= 6200 / size; j++) {
                Volcano v = field.volcano(i, j);
                if (v != null) {
                    volcanoes.add(v);
                }
            }
        }
    }

    private static List<Volcano.Island> allIslands() {
        List<Volcano.Island> islands = new ArrayList<>();
        for (Volcano v : volcanoes) {
            islands.addAll(v.islands());
        }
        for (int i = -7; i <= 7; i++) {
            for (int j = -7; j <= 7; j++) {
                islands.addAll(field.channelCluster(i, j));
            }
        }
        return islands;
    }

    private static List<Landmarks.FloatingVolcano> floatingVolcanoes() {
        List<Landmarks.FloatingVolcano> found = new ArrayList<>();
        for (int i = -4; i <= 3; i++) {
            for (int j = -4; j <= 3; j++) {
                Landmarks.FloatingVolcano f = field.floatingVolcano(i, j);
                if (f != null) {
                    found.add(f);
                }
            }
        }
        return found;
    }

    @Test
    void islandsHangBelowTheCloudsAndTheFloatingVolcanoOnlyPiercesThem() {
        for (Volcano.Island island : allIslands()) {
            assertTrue(island.top() + 1.5 < CLOUDS, "island in the clouds: " + island.top());
        }
        List<Landmarks.FloatingVolcano> floating = floatingVolcanoes();
        assertTrue(floating.size() >= 2, "floating volcanoes " + floating.size());
        for (Landmarks.FloatingVolcano f : floating) {
            assertTrue(f.base() >= 160.0 && f.base() + 3.0 < CLOUDS, "plateau " + f.base());
            assertTrue(f.base() + f.cone() >= 200.0 && f.base() + f.cone() <= 230.0, "crater rim " + (f.base() + f.cone()));
        }
    }

    @Test
    void lavaFallsPourFromTheEdgeToTheGround() {
        int falls = 0;
        List<List<Volcano.LavaFall>> sets = new ArrayList<>();
        for (Volcano.Island island : allIslands()) {
            if (field.islandClear(island)) {
                sets.add(island.falls());
            }
        }
        for (Landmarks.FloatingVolcano f : floatingVolcanoes()) {
            sets.add(f.falls());
        }
        for (List<Volcano.LavaFall> set : sets) {
            for (Volcano.LavaFall fall : set) {
                falls++;
                assertEquals(1, Math.abs(fall.springX() - fall.fallX()) + Math.abs(fall.springZ() - fall.fallZ()),
                        "the fall runs beside its spring: " + fall);
                VolcanicField.Column spring = field.column(fall.springX(), fall.springZ());
                assertTrue(spring.springAt(fall.springY()) && spring.islandAt(fall.springY() - 1), "spring on the edge: " + fall);
                VolcanicField.Column column = field.column(fall.fallX(), fall.fallZ());
                if (column.fallTop() == Integer.MIN_VALUE) {
                    continue; // something solid stands at its top (another island of the cluster)
                }
                assertEquals(fall.springY(), column.fallTop(), "fall top");
                assertTrue(column.fallBottom() <= column.height() + 2.0 || column.islandAt(column.fallBottom() - 1)
                        || column.lavaAt(column.fallBottom() - 1) || column.thinKind(column.fallBottom() - 1) >= 0,
                        "the fall lands on something: " + column.fallBottom() + " over " + column.height());
                for (int y = column.fallBottom(); y <= column.fallTop(); y++) {
                    assertTrue(column.lavaAt(y) && column.thinKind(y) < 0, "open lava all the way down at " + y);
                }
                // Only its lowest blocks flow (they spread where the lava lands).
                assertTrue(column.fallFlowBelow() < column.fallTop() || column.fallTop() - column.fallBottom() < 5);
            }
        }
        assertTrue(falls >= 30, "lava falls " + falls);
    }

    @Test
    void pillarsAreColumnsThatFlareLeanAndHaveLedges() {
        int pillars = 0;
        for (Volcano v : volcanoes) {
            for (Volcano.Pillar p : v.pillars()) {
                pillars++;
                int midY = (int) Math.floor((p.base() + p.top()) * 0.5);
                int footY = (int) Math.floor(p.base()) + 1;
                assertTrue(blocksAt(p, midY) > 0 && blocksAt(p, footY) > 1.5 * blocksAt(p, midY),
                        "the foot flares: " + blocksAt(p, footY) + " vs " + blocksAt(p, midY));
                // Not a disc: the outline is lobed, so the widths along x and along z differ somewhere up the pillar.
                boolean lobed = false;
                for (int y = footY; y < p.top() && !lobed; y++) {
                    lobed = Math.abs(width(p, y, true) - width(p, y, false)) >= 2;
                }
                assertTrue(lobed, "a lobed outline");
                assertTrue(Math.hypot(p.leanX(), p.leanZ()) >= 3.0 && p.ledgeY().length >= 1);
            }
        }
        assertTrue(pillars >= 4, "pillars " + pillars);
    }

    private static int blocksAt(Volcano.Pillar p, int y) {
        int count = 0;
        int r = (int) Math.ceil(p.reach());
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (p.contains((int) Math.floor(p.x()) + dx, y, (int) Math.floor(p.z()) + dz)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int width(Volcano.Pillar p, int y, boolean alongX) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int r = (int) Math.ceil(p.reach());
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (p.contains((int) Math.floor(p.x()) + dx, y, (int) Math.floor(p.z()) + dz)) {
                    int v = alongX ? dx : dz;
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
        }
        return max - min;
    }

    @Test
    void undersidesHangInLobesRatherThanOneTip() {
        for (int lobes = 2; lobes <= 3; lobes++) {
            double centre = Volcano.underside(0.0, 0.0, lobes, 0.0);
            double lobe = Volcano.underside(0.38, 0.0, lobes, 0.0);
            double between = Volcano.underside(0.38, Math.PI / lobes, lobes, 0.0);
            assertTrue(lobe > centre && lobe > between + 0.2, "lobes hang lowest: " + centre + " " + lobe + " " + between);
            assertTrue(Volcano.underside(1.0, 0.0, lobes, 0.0) < 0.01, "thin at the edge");
        }
    }

    @Test
    void collapsesAreRaggedSteppedAndSomeLeadToTheCaves() {
        int count = 0;
        int shafts = 0;
        int overhangs = 0;
        int cells = 6200 / CellShapes.COLLAPSE_CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                CellShapes.Collapse c = field.collapse(i, j);
                if (c == null) {
                    continue;
                }
                count++;
                assertTrue(field.clearOfVolcanoes(c.x(), c.z(), c.reach()), "collapse on a volcano");
                // Ragged: the rim's radius varies by at least a fifth around the centre.
                double lo = Double.MAX_VALUE;
                double hi = 0.0;
                for (int k = 0; k < 32; k++) {
                    double r = c.outline(k * Math.PI / 16.0);
                    lo = Math.min(lo, r);
                    hi = Math.max(hi, r);
                }
                assertTrue(hi > 1.2 * lo, "ragged rim " + lo + ".." + hi);
                // Stepped: the cut has three levels between the rim and the floor.
                assertEquals(c.depth(), c.cut(0.3), 1e-6);
                assertEquals(c.depth() / 3.0, c.cut(0.9), 1e-6);
                assertEquals(2.0 * c.depth() / 3.0, c.cut(0.75), 1e-6);
                assertEquals(0.0, c.cut(1.0), 0.0);
                VolcanicField.Column centre = field.column((int) Math.floor(c.x()), (int) Math.floor(c.z()));
                assertEquals(1.0, centre.collapse, 1e-9);
                if (c.shaft()) {
                    shafts++;
                    VolcanicField.Column shaft = field.column((int) Math.floor(c.shaftX()), (int) Math.floor(c.shaftZ()));
                    int floor = (int) Math.floor(shaft.height());
                    assertTrue(shaft.thin(floor) < 0.0 && shaft.thin(floor - 20) < 0.0, "shaft down from the floor " + floor + " voids " + shaft.voidCount + " " + shaft.voidBottom[0] + ".." + shaft.voidTop[0] + " solids " + shaft.solidCount + " collapse " + shaft.collapse + " " + c);
                }
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8.0;
                    double r = c.outline(a) * 1.08;
                    VolcanicField.Column rim = field.column((int) Math.floor(c.x() + Math.cos(a) * r), (int) Math.floor(c.z() + Math.sin(a) * r));
                    int top = (int) Math.floor(rim.height());
                    for (int v = 0; v < rim.voidCount; v++) {
                        // The overhang's opening ends three blocks below the top (a lava tube may lie lower too).
                        if (rim.collapse > 0.0 && rim.voidTop[v] == top - 3) {
                            overhangs++;
                            assertTrue(rim.thin(top - 3) < 0.0 && rim.thin(top - 2) == 0.0 && rim.voidBottom[v] <= top - 5,
                                    "an overhanging lip of three blocks over " + c);
                        }
                    }
                }
            }
        }
        // About one per 250–350 blocks on the plains (V1 = A).
        assertTrue(count >= 150 && count <= 450, "collapses " + count);
        assertTrue(shafts > count / 4 && shafts < count * 3 / 4, "shafts " + shafts);
        assertTrue(overhangs > count / 2, "overhangs " + overhangs);
    }

    @Test
    void caveEntrancesAreSealedBelowTheSurfaceExceptUnderACollapse() {
        String text = WorldgenData.read("density_function/ring_3/overworld.json").toString();
        // min(terrain, max(5 · entrances, 1 − terrain − 2 · collapse)): within 8 blocks of the surface (terrain < 1) the
        // second term is positive, so no entrance opens there, unless a collapse lowers it.
        assertTrue(text.contains("{\"type\":\"minecraft:mul\",\"argument1\":-2.0,\"argument2\":\"corruptedadventure:ring_3/overworld/collapse\"}"));
        assertTrue(text.contains("\"argument1\":{\"type\":\"minecraft:mul\",\"argument1\":5.0,\"argument2\":\"minecraft:overworld/caves/entrances\"}"));
        JsonObject collapse = WorldgenData.read("density_function/ring_3/overworld/collapse.json");
        assertEquals("minecraft:flat_cache", collapse.get("type").getAsString());
    }

    @Test
    void riftArchesStandAboutOncePerValley() {
        List<Landmarks.Arch> arches = new ArrayList<>();
        int cells = 6200 / Landmarks.RIFT_ARCH_CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Landmarks.Arch a = field.riftArch(i, j);
                if (a != null) {
                    arches.add(a);
                }
            }
        }
        assertTrue(arches.size() >= 15 && arches.size() <= 45, "rift arches " + arches.size());
        for (Landmarks.Arch a : arches) {
            for (Landmarks.Arch b : arches) {
                if (a != b) {
                    assertTrue(Math.hypot(a.x() - b.x(), a.z() - b.z()) > 200.0, "two arches close together");
                }
            }
        }
    }

    @Test
    void theMicroReliefRaisesSomeColumnsBelowEachStep() {
        int plain = 0;
        int raised = 0;
        int lowRaised = 0;
        double raisedFrac = 0.0;
        double keptFrac = 0.0;
        for (int x = -4900; x < -4600; x += 3) {
            for (int z = 500; z < 800; z += 3) {
                VolcanicField.Column c = field.column(x, z);
                int top = (int) Math.floor(c.height());
                if (c.cliff > 0.0 || c.roughness > 0.0 || c.rise > 0.0 || c.collapse > 0.0) {
                    continue;
                }
                plain++;
                double frac = c.height() - top;
                if (c.thinKind(top + 1) == Landmarks.KIND_BIOME) {
                    raised++;
                    raisedFrac += frac;
                    lowRaised += frac < 0.3 ? 1 : 0;
                    assertTrue(c.thinKind(top) == Landmarks.KIND_BIOME && c.thinKind(top + 2) < 0, "one block up, gap filled");
                } else {
                    keptFrac += frac;
                }
            }
        }
        assertTrue(plain > 1000 && raised > plain / 10 && raised < plain * 7 / 10, raised + " of " + plain);
        // G2: mostly just below the next step, but in places a whole tread (the line merges with the one below).
        assertTrue(raisedFrac / raised > keptFrac / (plain - raised) + 0.15, "raised blocks lie high in their tread");
        assertTrue(lowRaised > raised / 20, lowRaised + " raised low in their tread of " + raised);
    }

    @Test
    void theMicroReliefFraysEveryStepLineOnItsOwn() {
        // G2: the pattern is drawn anew for every block of height, so neighbouring step lines do not fray in step.
        Ring3Relief relief = field.relief();
        int differ = 0;
        int n = 0;
        double smoothMax = 0.0;
        for (int x = -4900; x < -4600; x += 7) {
            for (int z = 500; z < 800; z += 7) {
                n++;
                if (Math.abs(relief.microSteps(x, z, 100) - relief.microSteps(x, z, 101)) > 0.3) {
                    differ++;
                }
                smoothMax = Math.max(smoothMax, Math.abs(relief.microRelief(x, z, 90.0 + (x + z) % 9)));
            }
        }
        assertTrue(differ > n / 3, differ + " of " + n);
        assertTrue(smoothMax <= Ring3Relief.MICRO_BUMP + Ring3Relief.MICRO_RIPPLE + 1e-9, "the smooth part stays within ±1.5: " + smoothMax);
    }

    @Test
    void thinSolidsCarryTheirSurfaceKind() {
        int needles = 0;
        for (int i = -32; i <= 32 && needles < 5; i++) {
            for (int j = -32; j <= 32 && needles < 5; j++) {
                Landmarks.Needle n = field.needle(i, j);
                if (n == null) {
                    continue;
                }
                needles++;
                VolcanicField.Column c = field.column((int) Math.floor(n.x()), (int) Math.floor(n.z()));
                int ground = (int) Math.floor(c.height());
                assertEquals(Landmarks.KIND_OBSIDIAN, c.thinKind(ground + 20), "needle");
                assertEquals(Landmarks.KIND_OBSIDIAN, (int) field.thinKind((int) Math.floor(n.x()), ground + 40, (int) Math.floor(n.z())));
            }
        }
        assertTrue(needles >= 3);
        // A floating island's blocks are kind island; above it nothing.
        for (Landmarks.FloatingVolcano f : floatingVolcanoes()) {
            int x = (int) Math.floor(f.x() + f.radius() * 0.6);
            int z = (int) Math.floor(f.z());
            VolcanicField.Column c = field.column(x, z);
            assertTrue(c.islandCount > 0);
            int top = (int) Math.floor(c.islandTop[0]) - 1;
            assertEquals(Landmarks.KIND_ISLAND, c.thinKind(top));
            assertEquals(-1, c.thinKind(top + 1));
            assertEquals(1.0, field.thin(x, top, z));
        }
        // The veins wind: the same block height gives different values across a few columns.
        double a = field.vein(100, 120, 100);
        double b = field.vein(104, 120, 100);
        assertTrue(a >= 0.0 && a < 1.0 && Math.abs(a - b) > 1e-6);
    }

    @Test
    void accessorsGiveWhatTheTerrainUses() {
        int x = 4500;
        int z = -300;
        List<VolcanicField.VolcanoInfo> near = field.volcanoesWithin(x, z, 900);
        int expected = 0;
        for (Volcano v : volcanoes) {
            if (Math.hypot(v.x() - x, v.z() - z) <= 900) {
                expected++;
                assertTrue(near.stream().anyMatch(info -> info.x() == (int) Math.floor(v.x()) && info.z() == (int) Math.floor(v.z())
                        && info.craterY() == (int) Math.floor(v.rim()) && info.dormant() == v.dormant()), "volcano " + v);
            }
        }
        assertTrue(expected > 0);
        assertEquals(expected, near.stream().filter(info -> !isFloating(info)).count());
        VolcanicField.VolcanoInfo nearest = field.nearestVolcano(x, z);
        assertNotNull(nearest);
        // The floating volcanoes are in the list too.
        Landmarks.FloatingVolcano f = floatingVolcanoes().get(0);
        assertTrue(field.volcanoesWithin((int) f.x(), (int) f.z(), 10).stream().anyMatch(Ring3PolishTest::isFloating));

        long salt = field.salt();
        int mounds = 0;
        int sinkholes = 0;
        for (int px = 4000; px < 5000; px += 37) {
            for (int pz = 200; pz < 900; pz += 37) {
                VolcanicField.Mound m = field.moundAt(px, pz);
                if (m != null) {
                    mounds++;
                    assertEquals(CellShapes.mound(salt, m.x(), m.z()) * field.regions(m.x(), m.z()).sulfurS(), m.height(), 1e-9);
                }
                VolcanicField.Sinkhole s = field.sinkholeAt(px, pz);
                if (s != null) {
                    sinkholes++;
                    assertEquals(CellShapes.sinkhole(salt, s.x(), s.z()) * field.regions(s.x(), s.z()).sulfurS(), s.depth(), 1e-9);
                }
                VolcanicField.Sinkhole c = field.collapseAt(px, pz);
                CellShapes.Collapse terrain = field.collapse(Math.floorDiv(px, CellShapes.COLLAPSE_CELL), Math.floorDiv(pz, CellShapes.COLLAPSE_CELL));
                if (terrain == null) {
                    assertNull(c);
                } else {
                    assertEquals(terrain.x(), c.x(), 0.0);
                    assertEquals(terrain.depth(), c.depth(), 0.0);
                    assertEquals(terrain.shaft(), c.shaft());
                }
            }
        }
        assertTrue(mounds > 0 && sinkholes > 0, mounds + " mounds, " + sinkholes + " sinkholes");

        int cages = 0;
        for (int i = -16; i <= 16; i++) {
            for (int j = -16; j <= 16; j++) {
                TitanBones.Cage cage = TitanBones.cage(field, i, j);
                if (cage == null) {
                    continue;
                }
                cages++;
                VolcanicField.Ribcage found = field.ribcageNear((int) cage.x(), (int) cage.z());
                assertNotNull(found);
                assertEquals(cage.x(), found.x(), 0.0);
                assertEquals(cage.vx().length, found.vertebrae().size());
                VolcanicField.Point v = found.vertebrae().get(found.vertebrae().size() / 2);
                VolcanicField.Column column = field.column((int) Math.floor(v.x()), (int) Math.floor(v.z()));
                assertTrue(column.thinKind((int) Math.floor(column.height()) + 2) == Landmarks.KIND_BIOME, "a vertebra stands there");
                assertTrue(!found.ribFeet().isEmpty());
            }
        }
        assertTrue(cages >= 1);
    }

    private static boolean isFloating(VolcanicField.VolcanoInfo info) {
        return floatingVolcanoes().stream().anyMatch(f -> (int) Math.floor(f.x()) == info.x() && (int) Math.floor(f.z()) == info.z());
    }

    // ---------------------------------------------------------------- review fixes (39f)

    @Test
    void theRaggedOutlineRisesAndStaysOutsideBeyondTheReach() {
        for (double phase = 0.0; phase < 6.3; phase += 0.7) {
            for (int k = 0; k < 64; k++) {
                double theta = k * Math.PI / 32.0;
                double previous = -1.0;
                for (double d = 0.0; d <= 2.0; d += 0.01) {
                    double r = Volcano.ragged(d, theta, phase);
                    assertTrue(r > previous, "rising at d " + d + " theta " + theta + " phase " + phase);
                    previous = r;
                }
                // Stretched by at most about a sixth: outside by 1.2, and beyond an island's reach (1.4 a·stretch).
                assertTrue(Volcano.ragged(1.2, theta, phase) >= 1.0 && Volcano.ragged(1.4, theta, phase) >= 1.0);
            }
        }
    }

    @Test
    void pillarsStartAtTheCraterFloorAndEndInsideTheIsland() {
        int pillars = 0;
        for (Volcano v : volcanoes) {
            for (Volcano.Pillar p : v.pillars()) {
                if (!field.islandClear(v.islands().get(0))) {
                    continue;
                }
                pillars++;
                VolcanicField.Column foot = field.column((int) Math.floor(p.x()), (int) Math.floor(p.z()));
                for (int y = p.floorY() - 40; y < p.floorY(); y++) {
                    assertTrue(foot.thinKind(y) != Landmarks.KIND_BASALT, "basalt below the pillar's floor at " + y);
                }
                assertEquals(Landmarks.KIND_BASALT, foot.thinKind(p.floorY()), "the pillar's lowest block");
                int topX = (int) Math.floor(p.x() + p.leanX());
                int topZ = (int) Math.floor(p.z() + p.leanZ());
                VolcanicField.Column head = field.column(topX, topZ);
                int top = (int) Math.floor(p.top());
                assertTrue(head.islandAt(top) && p.contains(topX, top, topZ), "the pillar ends inside the island: " + top);
            }
        }
        assertTrue(pillars >= 4, "pillars " + pillars);
    }

    @Test
    void clearIslandsHangAboveTheGroundUnderTheirLobes() {
        for (Volcano.Island island : allIslands()) {
            if (!field.islandClear(island)) {
                continue;
            }
            double r = Volcano.LOBE_D * island.a();
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8.0;
                int x = (int) Math.floor(island.x() + Math.cos(a) * r);
                int z = (int) Math.floor(island.z() + Math.sin(a) * r);
                VolcanicField.Column c = field.column(x, z);
                double d = island.distance(x + 0.5, z + 0.5, 0.18 * field.warpAt(x + 0.5, z + 0.5));
                assertTrue(island.bottomAt(d, island.theta(x + 0.5, z + 0.5)) > c.height() + 3.0,
                        "a lobe in the ground at " + x + ", " + z + ": " + island);
            }
        }
    }

    @Test
    void lavaFallsStayWithinTheirShapesReach() {
        for (Volcano.Island island : allIslands()) {
            for (Volcano.LavaFall fall : island.falls()) {
                assertTrue(Math.abs(fall.fallX() + 0.5 - island.x()) <= island.reach()
                        && Math.abs(fall.fallZ() + 0.5 - island.z()) <= island.reach(), "fall beyond the reach: " + fall);
            }
        }
        for (Landmarks.FloatingVolcano f : floatingVolcanoes()) {
            for (Volcano.LavaFall fall : f.falls()) {
                assertTrue(Math.abs(fall.fallX() + 0.5 - f.x()) < f.reach() && Math.abs(fall.fallZ() + 0.5 - f.z()) < f.reach(),
                        "fall beyond the reach: " + fall);
            }
        }
    }

    @Test
    void aRiftArchOnlyYieldsToANeighbourThatHasOne() {
        int cells = 6200 / Landmarks.RIFT_ARCH_CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                double[] deepest = field.riftFloor(i, j);
                if (field.riftArch(i, j) != null || Landmarks.riftArchCandidate(field, i, j, deepest) == null) {
                    continue;
                }
                boolean yielded = false;
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        double[] other = field.riftFloor(i + di, j + dj);
                        yielded |= (di != 0 || dj != 0) && field.riftArch(i + di, j + dj) != null
                                && Math.hypot(other[0] - deepest[0], other[1] - deepest[1]) < Landmarks.RIFT_ARCH_SPACING;
                    }
                }
                assertTrue(yielded, "cell " + i + ", " + j + " has no arch and no neighbour with one");
            }
        }
        // Whatever asks first, the same arches.
        VolcanicField reversed = VolcanicFieldTest.field();
        for (int i = cells; i >= -cells; i--) {
            for (int j = cells; j >= -cells; j--) {
                assertEquals(field.riftArch(i, j), reversed.riftArch(i, j), "cell " + i + ", " + j);
            }
        }
    }
}
