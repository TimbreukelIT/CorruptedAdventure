package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Package 74 on the test seed: the world trees (placement, terrain, falls beside the roots), the sky lakes (walled in
 * per block, dry over the blighted band), the island pairs and the rock arches; all pure.
 */
class Ring4LandmarksTest {
    private static IslandField field;
    private static int cellSize;
    /** The islands of a wedge across the whole ring, like {@link Ring4IslandsTest}, and one across the blighted band. */
    private static final List<IslandShape> SHAPES = new ArrayList<>();

    @BeforeAll
    static void place() {
        field = IslandFieldTest.field();
        cellSize = field.settings().cellSize();
        for (int i = 5900 / cellSize; i <= 8100 / cellSize; i++) {
            for (int j = -1400 / cellSize; j <= 1400 / cellSize; j++) {
                SHAPES.addAll(field.cell(i, j));
            }
        }
        for (int i = -8100 / cellSize - 1; i <= -7000 / cellSize; i++) {
            for (int j = -1400 / cellSize; j <= 1400 / cellSize; j++) {
                SHAPES.addAll(field.cell(i, j));
            }
        }
    }

    // ---------------------------------------------------------------- world trees

    /** ~1 per 2,500 blocks along the ring (~15), each clearly in the mythical band and clear of the zones. */
    @Test
    void worldTreesStandInTheMythicalBandOutsideTheZones() {
        List<WorldTreeShape> trees = field.landmarks().trees();
        int cells = RingArc.cellCount(RingArc.perimeter(6000.0), WorldTrees.ARC_CELL);
        System.out.printf("world trees: %d of %d arc cells%n", trees.size(), cells);
        assertEquals(15, cells);
        assertTrue(trees.size() >= 12 && trees.size() <= cells, "trees " + trees.size());
        Ring4Band band = Ring4Band.of(IslandFieldTest.LAYOUT);
        for (WorldTreeShape tree : trees) {
            int height = tree.top - tree.base + 1;
            System.out.printf("  tree at %.0f, %.0f: base %d, top %d (height %d), trunk %.1f across, reach %.0f, island %.0f across, %d roots, %d falls%n",
                    tree.x, tree.z, tree.base, tree.top, height, 2 * tree.trunkRadius, tree.reach,
                    2 * tree.island.a() * Math.sqrt(tree.island.stretch()), tree.roots.size(), field.details(tree.island).falls().size());
            assertTrue(height >= WorldTrees.MIN_HEIGHT - 1 && height <= WorldTrees.MAX_HEIGHT + 1, "height " + height);
            assertTrue(tree.top <= WorldTrees.MAX_CROWN);
            assertTrue(2 * tree.trunkRadius >= 8.0 && 2 * tree.trunkRadius <= 12.0);
            assertTrue(band.value(tree.x, tree.z) <= WorldTrees.BAND_MAX);
            for (int n = 0; n < 32; n++) {
                double t = n * Math.PI / 16.0;
                RingPosition p = IslandFieldTest.LAYOUT.sampleOverworld(tree.x + (tree.reach + WorldTrees.ZONE_MARGIN) * Math.cos(t),
                        tree.z + (tree.reach + WorldTrees.ZONE_MARGIN) * Math.sin(t));
                assertTrue(!p.inTransition() && p.ring() == Ring.RING_4, "zone near the tree at " + tree.x + ", " + tree.z);
            }
            // The tree's island is one of the islands of its cell.
            int i = Math.floorDiv((int) Math.floor(tree.x), cellSize);
            int j = Math.floorDiv((int) Math.floor(tree.z), cellSize);
            assertTrue(field.cell(i, j).contains(tree.island));
        }
    }

    /** No other island grows into a tree: its crown, its roots or its island's hole. */
    @Test
    void noIslandGrowsIntoAWorldTree() {
        for (WorldTreeShape tree : field.landmarks().trees()) {
            int r = (int) Math.ceil(tree.reach) + cellSize;
            for (IslandShape other : field.shapesWithin((int) tree.x - r, (int) tree.z - r, (int) tree.x + r, (int) tree.z + r)) {
                if (!other.equals(tree.island)) {
                    assertFalse(WorldTrees.clashes(tree, other), other + " grows into the tree at " + tree.x + ", " + tree.z);
                }
            }
        }
    }

    /** The tree is terrain: trunk (log), branches and roots (wood), crown (leaves), all solid in {@code thin}. */
    @Test
    void theWorldTreeIsTerrain() {
        List<WorldTreeShape> trees = field.landmarks().trees();
        assertFalse(trees.isEmpty());
        int shell = 0;
        int shellKept = 0;
        for (WorldTreeShape tree : trees) {
            int x = (int) Math.floor(tree.x);
            int z = (int) Math.floor(tree.z);
            for (int y = tree.base; y <= tree.trunkTop; y += 7) {
                assertEquals(WorldTreePart.LOG, field.treePart(x, y, z), "trunk at " + y);
                assertEquals(1.0, field.thin(x, y, z));
            }
            assertEquals(WorldTreePart.NONE, field.treePart(x, tree.top + 2, z));
            assertEquals(WorldTreePart.LEAVES, field.treePart(x, tree.trunkTop + 2, z), "the dome over the trunk");
            WorldTreeShape.Tube root = tree.roots.get(0);
            int k = root.px().length / 2;
            int rx = (int) Math.floor(root.px()[k]);
            int rz = (int) Math.floor(root.pz()[k]);
            int ry = (int) Math.floor(root.py()[k]);
            assertEquals(WorldTreePart.WOOD, field.treePart(rx, ry, rz), "root at " + rx + " " + ry + " " + rz);
            assertEquals(1.0, field.thin(rx, ry, rz));
            WorldTreeShape.Tube branch = tree.branches.get(0);
            int bk = branch.px().length / 2;
            assertEquals(WorldTreePart.WOOD, field.treePart((int) Math.floor(branch.px()[bk]), (int) Math.floor(branch.py()[bk]),
                    (int) Math.floor(branch.pz()[bk])));
            // Leaves in the crown's shell: some kept, some not (a leafy edge); over the dome's top.
            WorldTreeShape.Blob dome = tree.leaves.get(0);
            for (int dx = -40; dx <= 40; dx++) {
                for (int dz = -40; dz <= 40; dz++) {
                    int y = (int) Math.floor(dome.y() + 0.5 * dome.ry());
                    double[] range = new double[2];
                    if (dome.range(x + dx + 0.5, z + dz + 0.5, 0.0, range) && Math.abs(range[1] - 0.5 - y) < 1.0
                            && field.treePart(x + dx, y, z + dz) != WorldTreePart.WOOD) {
                        shell++;
                        shellKept += field.treePart(x + dx, y, z + dz) == WorldTreePart.LEAVES ? 1 : 0;
                    }
                }
            }
        }
        System.out.printf("crown shell: %d of %d kept%n", shellKept, shell);
        assertTrue(shell > 20 && shellKept > 0 && shellKept < shell, shellKept + " of " + shell);
    }

    /** The tree's falls land free of its roots and trunk: the spring and the whole falling column are open. */
    @Test
    void worldTreeFallsPourBesideTheRoots() {
        int falls = 0;
        for (WorldTreeShape tree : field.landmarks().trees()) {
            assertFalse(field.details(tree.island).falls().isEmpty(), "no fall at the tree at " + tree.x + ", " + tree.z);
            for (IslandField.Fall fall : field.details(tree.island).falls()) {
                falls++;
                assertTrue(WorldTrees.free(tree, fall), fall.toString());
                if (field.fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                    assertEquals(-1.0, field.thin(fall.springX(), fall.springY(), fall.springZ()));
                    for (int y = fall.bottom(); y <= fall.springY(); y++) {
                        assertTrue(field.thin(fall.fallX(), y, fall.fallZ()) <= 0.0, "fall " + fall + " at " + y);
                    }
                }
            }
        }
        System.out.println("world tree falls: " + falls);
        assertTrue(falls >= field.landmarks().trees().size(), "falls " + falls);
    }

    // ---------------------------------------------------------------- sky lakes

    /**
     * Every sky lake is walled in per block (the waterscan's rule): beside and under each water block lies water of the
     * lake or solid rock (or the spring its brook feeds). Dry bowls only over the blighted band.
     */
    @Test
    void skyLakesAreWalledInPerBlock() {
        int lakes = 0;
        int dry = 0;
        int brooks = 0;
        int water = 0;
        Ring4Band band = Ring4Band.of(IslandFieldTest.LAYOUT);
        for (IslandShape s : SHAPES) {
            SkyLakes.Lake lake = field.landmarks().lake(s);
            if (lake == null) {
                continue;
            }
            assertTrue(s.highest() >= SkyLakes.SKY_TOP);
            assertEquals(band.blighted(s.x(), s.z()), lake.dry());
            if (lake.dry()) {
                dry++;
            } else {
                lakes++;
            }
            brooks += lake.brook().isEmpty() ? 0 : 1;
            int columns = 0;
            for (int x = lake.minX(); x <= lake.maxX(); x++) {
                for (int z = lake.minZ(); z <= lake.maxZ(); z++) {
                    IslandField.Column c = field.islandColumn(x, z);
                    int floor = c.skyLakeFloor();
                    int top = c.skyLakeTop();
                    if (floor == Integer.MIN_VALUE) {
                        continue;
                    }
                    columns++;
                    assertEquals(1.0, field.thin(x, floor, z), "floor at " + x + ", " + z);
                    if (lake.dry()) {
                        assertEquals(Integer.MIN_VALUE, top);
                        continue;
                    }
                    assertEquals(lake.water() + 1, top);
                    for (int y = floor + 1; y < top; y++) {
                        water++;
                        assertTrue(field.thin(x, y, z) <= 0.0, "water block is open at " + x + " " + y + " " + z);
                        assertTrue(wetOrSolid(x + 1, y, z) && wetOrSolid(x - 1, y, z) && wetOrSolid(x, y, z + 1)
                                && wetOrSolid(x, y, z - 1), "water beside air at " + x + " " + y + " " + z);
                    }
                }
            }
            assertTrue(columns > 0, "a lake without columns on " + s);
        }
        System.out.printf("sky lakes %d (%d with a brook), dry bowls %d, water blocks %d%n", lakes, brooks, dry, water);
        assertTrue(lakes > 0, "no sky lake");
        assertTrue(dry > 0, "no dry bowl");
    }

    private static boolean wetOrSolid(int x, int y, int z) {
        IslandField.Column c = field.islandColumn(x, z);
        if (y < c.skyLakeTop() && y > c.skyLakeFloor()) {
            return true;
        }
        if (c.springY() == y) {
            return true;
        }
        return field.thin(x, y, z) > 0.0;
    }

    /** A brook joins its lake to a kept fall's spring at the lake's level. */
    @Test
    void brooksLeadTheLakeToAFall() {
        for (IslandShape s : SHAPES) {
            SkyLakes.Lake lake = field.landmarks().lake(s);
            if (lake == null || lake.brook().isEmpty()) {
                continue;
            }
            boolean fed = false;
            for (IslandField.Fall fall : field.details(s).falls()) {
                if (fall.springY() == lake.water() && field.fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                    long inner = KeyedSlots.key(fall.springX() - Integer.signum(fall.fallX() - fall.springX()),
                            fall.springZ() - Integer.signum(fall.fallZ() - fall.springZ()));
                    fed |= lake.brook().contains(inner);
                }
            }
            assertTrue(fed, "brook of " + s);
        }
    }

    // ---------------------------------------------------------------- pairs and arches

    /** Pairs: facing edges 3–40 apart, found from their first island; arches solid, clear of falls and other islands. */
    @Test
    void islandPairsAndArches() {
        int pairs = 0;
        int arches = 0;
        for (IslandShape s : SHAPES) {
            for (IslandArches.Pair pair : field.landmarks().pairs(s)) {
                pairs++;
                assertTrue(pair.gap() >= IslandArches.MIN_GAP && pair.gap() <= IslandArches.MAX_GAP);
                assertTrue(IslandArches.before(pair.first(), pair.second()));
                assertEquals(s, pair.first());
                IslandArches.Arch arch = pair.arch();
                if (arch == null) {
                    continue;
                }
                arches++;
                int solid = 0;
                int previous = Integer.MIN_VALUE;
                int steps = (int) Math.ceil(pair.gap()) * 2;
                for (int n = 0; n <= steps; n++) {
                    double t = n / (double) steps;
                    int x = (int) Math.floor(arch.ax() + t * (arch.bx() - arch.ax()));
                    int z = (int) Math.floor(arch.az() + t * (arch.bz() - arch.az()));
                    int[] interval = arch.interval(x, z);
                    if (interval == null) {
                        continue;
                    }
                    solid++;
                    assertEquals(1.0, field.thin(x, interval[1], z), "arch deck at " + x + ", " + z);
                    if (previous != Integer.MIN_VALUE) {
                        assertTrue(Math.abs(interval[1] - previous) <= 2, "deck steps " + previous + " → " + interval[1]);
                    }
                    previous = interval[1];
                }
                assertTrue(solid > pair.gap(), "a broken arch " + pair);
            }
        }
        System.out.printf("island pairs %d, rock arches %d%n", pairs, arches);
        assertTrue(pairs > 50, "pairs " + pairs);
        assertTrue(arches > 0, "no arch");
        // Sparring 2026-10-20 (B2-1): every suitable pair may get one, but an island holds at most one arch.
        Map<IslandShape, Integer> perIsland = new HashMap<>();
        for (IslandShape s : SHAPES) {
            for (IslandArches.Pair pair : field.landmarks().pairs(s)) {
                if (pair.arch() != null) {
                    perIsland.merge(pair.first(), 1, Integer::sum);
                    perIsland.merge(pair.second(), 1, Integer::sum);
                }
            }
        }
        perIsland.forEach((island, n) -> assertTrue(n <= 1, n + " arches at " + island));
        // Backlog r. 429: no two kept arches cross (an X between four islands).
        List<IslandArches.Arch> kept = new ArrayList<>();
        for (IslandShape s : SHAPES) {
            for (IslandArches.Pair pair : field.landmarks().pairs(s)) {
                if (pair.arch() != null) {
                    kept.add(pair.arch());
                }
            }
        }
        for (int a = 0; a < kept.size(); a++) {
            for (int b = a + 1; b < kept.size(); b++) {
                assertFalse(IslandArches.crosses(kept.get(a), kept.get(b)), "crossing arches " + kept.get(a) + " " + kept.get(b));
            }
        }
        // Review golf 3: every candidate arch's box lies within archReach of its first island (the windows of the cells,
        // the debris and archKept), and that bound within MAX_ARCH_WINDOW.
        int candidates = 0;
        for (IslandShape s : SHAPES) {
            double reach = IslandArches.archReach(s);
            assertTrue(reach <= IslandArches.MAX_ARCH_WINDOW, "window " + s);
            for (IslandArches.Pair pair : field.landmarks().candidatePairs(s)) {
                IslandArches.Arch arch = pair.arch();
                if (arch == null) {
                    continue;
                }
                candidates++;
                assertTrue(arch.minX() >= s.x() - reach && arch.maxX() <= s.x() + reach && arch.minZ() >= s.z() - reach
                        && arch.maxZ() <= s.z() + reach, "arch past its reach: " + arch + " of " + s);
            }
        }
        assertTrue(candidates >= arches, "candidates " + candidates);
        // The public accessor finds them too.
        assertFalse(field.islandPairs(6500, -1000, 8000, 1000).isEmpty());
    }

    /** Backlog r. 429: two arches cross where their rock meets (an X), not side by side or far above each other. */
    @Test
    void archesCrossWhereTheirRockMeets() {
        IslandArches.Arch eastWest = IslandArches.Arch.between(new IslandArches.Edge(0, 0, 100, 94),
                new IslandArches.Edge(20, 0, 100, 94), 1L);
        IslandArches.Arch northSouth = IslandArches.Arch.between(new IslandArches.Edge(10, -10, 100, 94),
                new IslandArches.Edge(10, 10, 100, 94), 2L);
        IslandArches.Arch beside = IslandArches.Arch.between(new IslandArches.Edge(0, 12, 100, 94),
                new IslandArches.Edge(20, 12, 100, 94), 3L);
        IslandArches.Arch high = IslandArches.Arch.between(new IslandArches.Edge(10, -10, 130, 124),
                new IslandArches.Edge(10, 10, 130, 124), 4L);
        assertTrue(IslandArches.crosses(eastWest, northSouth));
        assertTrue(IslandArches.crosses(northSouth, eastWest));
        assertFalse(IslandArches.crosses(eastWest, beside));
        assertFalse(IslandArches.crosses(eastWest, high));
    }

    /**
     * Sparring 2026-10-20 (B2-3 a): debris round the big islands is terrain, floats free (air round it, no island, fall
     * or lift near it) and stays near its island.
     */
    @Test
    void debrisFloatsFreeRoundTheBigIslands() {
        int pieces = 0;
        int islands = 0;
        int toppedColumns = 0;
        for (IslandShape s : SHAPES) {
            List<IslandDebris.Piece> debris = field.landmarks().debris(s);
            if (s.kind() == IslandShape.CLOD || s.kind() == IslandShape.TREE) {
                assertTrue(debris.isEmpty());
                continue;
            }
            islands += debris.isEmpty() ? 0 : 1;
            for (IslandDebris.Piece piece : debris) {
                pieces++;
                assertTrue(Math.hypot(piece.x() - s.x(), piece.z() - s.z()) <= s.reach() + IslandDebris.MAX_OUT, "far " + piece);
                assertEquals(1.0, field.thin(piece.x(), piece.y(), piece.z()), "debris is terrain: " + piece);
                assertEquals(0.0, field.thin(piece.x(), piece.y() + 1, piece.z()), "air above " + piece);
                assertEquals(0.0, field.thin(piece.x(), piece.y() - piece.size(), piece.z()), "air below " + piece);
                // Backlog r. 428: clear of the ground under the corners of its box too.
                int lowest = piece.y() - piece.size() + 1;
                assertTrue(lowest >= field.baseHeight(piece.minX() + 0.5, piece.minZ() + 0.5) + IslandDebris.GROUND_CLEARANCE
                        && lowest >= field.baseHeight(piece.maxX() + 0.5, piece.maxZ() + 0.5) + IslandDebris.GROUND_CLEARANCE
                        && lowest >= field.baseHeight(piece.minX() + 0.5, piece.maxZ() + 0.5) + IslandDebris.GROUND_CLEARANCE
                        && lowest >= field.baseHeight(piece.maxX() + 0.5, piece.minZ() + 0.5) + IslandDebris.GROUND_CLEARANCE,
                        "on the ground: " + piece);
                // Backlog r. 426: the features see every column of it.
                for (int x = piece.minX(); x <= piece.maxX(); x++) {
                    for (int z = piece.minZ(); z <= piece.maxZ(); z++) {
                        int[] rock = piece.interval(x, z);
                        if (rock != null) {
                            assertTrue(field.inDebris(x, z), "not listed: " + piece + " at " + x + ", " + z);
                            // Review golf 3: on the piece (its top is the ground) it counts; on an island top above it not.
                            assertTrue(field.inDebris(x, z, rock[1]), "not on its top: " + piece + " at " + x + ", " + z);
                            // Review golf 3: water_near sees the column under the piece as covered, not the piece itself.
                            assertTrue(field.islandColumn(x, z).underIsland(rock[0] - 1), "not over: " + piece + " at " + x + ", " + z);
                            assertFalse(field.islandColumn(x, z).underIsland(rock[1]), "in: " + piece + " at " + x + ", " + z);
                            for (IslandShape other : field.shapesNear(x, z)) {
                                int[] above = other.interval(x + 0.5, z + 0.5);
                                if (above != null && above[0] > rock[1]) {
                                    toppedColumns++;
                                    assertFalse(field.inDebris(x, z, above[1]), "island top over " + piece + " at " + x + ", " + z);
                                }
                            }
                        }
                    }
                }
                for (IslandShape other : field.shapesNear(piece.x(), piece.z())) {
                    assertTrue(other.interval(piece.x() + 0.5, piece.z() + 0.5) == null
                            || other.interval(piece.x() + 0.5, piece.z() + 0.5)[0] > piece.y() + IslandDebris.KEEP_CLEAR_Y
                            || other.interval(piece.x() + 0.5, piece.z() + 0.5)[1] < piece.y() - piece.size() - 1, "in an island: " + piece);
                }
                assertTrue(field.waterfallsWithin(piece.minX() - 2, piece.minZ() - 2, piece.maxX() + 2, piece.maxZ() + 2).isEmpty(),
                        "fall at " + piece);
                assertTrue(field.liftsWithin(piece.minX() - 2, piece.minZ() - 2, piece.maxX() + 2, piece.maxZ() + 2).isEmpty(), "lift at " + piece);
            }
        }
        System.out.printf("debris: %d pieces round %d islands, %d columns under an island top%n", pieces, islands, toppedColumns);
        assertTrue(pieces > 50, "pieces " + pieces);
    }

    /**
     * Backlog r. 420: {@link IslandField#islandBlockNear} (the ore filter of ring 4) sees every island, arch and debris
     * block in its box, and nothing far under the islands.
     */
    @Test
    void islandBlockNearSeesEveryIslandBlock() {
        int reach = 2;
        int checked = 0;
        int seen = 0;
        java.util.Random random = new java.util.Random(420);
        for (IslandShape s : SHAPES) {
            if (s.kind() == IslandShape.TREE) {
                continue;
            }
            List<int[]> probes = new ArrayList<>();
            // Round the island's edge and under its belly, at its arches and its debris.
            for (int n = 0; n < 6; n++) {
                double t = random.nextDouble() * Math.PI * 2.0;
                double r = s.reach() * (0.6 + 0.6 * random.nextDouble());
                probes.add(new int[]{(int) Math.floor(s.x() + r * Math.cos(t)), s.lowest() - 4 + random.nextInt(s.highest() - s.lowest() + 8),
                        (int) Math.floor(s.z() + r * Math.sin(t))});
            }
            for (IslandArches.Pair pair : field.landmarks().pairs(s)) {
                if (pair.arch() != null) {
                    IslandArches.Arch arch = pair.arch();
                    int x = (int) Math.floor((arch.ax() + arch.bx()) / 2.0);
                    int z = (int) Math.floor((arch.az() + arch.bz()) / 2.0);
                    probes.add(new int[]{x + 1, (arch.topA() + arch.topB()) / 2 - 3, z - 1});
                }
            }
            for (IslandDebris.Piece piece : field.landmarks().debris(s)) {
                probes.add(new int[]{piece.x() + 2, piece.y() + 2, piece.z() - 1});
            }
            for (int[] p : probes) {
                boolean block = false;
                for (int x = p[0] - reach; x <= p[0] + reach && !block; x++) {
                    for (int z = p[2] - reach; z <= p[2] + reach && !block; z++) {
                        for (int y = p[1] - reach; y <= p[1] + reach && !block; y++) {
                            block = field.thin(x, y, z) == 1.0;
                        }
                    }
                }
                checked++;
                seen += block ? 1 : 0;
                if (block) {
                    assertTrue(field.islandBlockNear(p[0], p[1], p[2], reach), "missed at " + p[0] + " " + p[1] + " " + p[2]);
                }
            }
            // Far under the island, in the ground below its hole: nothing.
            assertFalse(field.islandBlockNear((int) Math.floor(s.x()), (int) Math.floor(s.groundY()) - 40, (int) Math.floor(s.z()), 6));
        }
        System.out.printf("islandBlockNear: %d probes, %d next to an island block%n", checked, seen);
        assertTrue(seen > 500, "seen " + seen);
    }

    // ---------------------------------------------------------------- determinism

    /** Pure: a second field, asked in the reverse order, gives the same trees, lakes, pairs and blocks. */
    @Test
    void landmarksArePureWhateverTheOrder() {
        IslandField other = IslandFieldTest.field();
        Map<IslandShape, String> theirs = new HashMap<>();
        for (int i = 8100 / cellSize; i >= 5900 / cellSize; i--) {
            for (int j = 1400 / cellSize; j >= -1400 / cellSize; j--) {
                List<IslandShape> cell = new ArrayList<>(other.cell(i, j));
                Collections.reverse(cell);
                for (IslandShape s : cell) {
                    theirs.put(s, describe(other, s));
                }
            }
        }
        int compared = 0;
        for (IslandShape s : SHAPES) {
            String known = theirs.get(s);
            if (known != null) {
                compared++;
                assertEquals(describe(field, s), known);
            }
        }
        assertTrue(compared > 100, "compared " + compared);
        assertEquals(field.worldTrees(-9000, -9000, 9000, 9000), other.worldTrees(-9000, -9000, 9000, 9000));
        WorldTreeShape tree = other.landmarks().trees().get(0);
        int x = (int) Math.floor(tree.x);
        int z = (int) Math.floor(tree.z);
        for (int y = tree.base - 10; y <= tree.top + 2; y += 3) {
            for (int d = -30; d <= 30; d += 7) {
                assertEquals(field.treePart(x + d, y, z - d), other.treePart(x + d, y, z - d));
                assertEquals(field.thin(x + d, y, z + d), other.thin(x + d, y, z + d));
            }
        }
        assertNotNull(tree.view());
    }

    private static String describe(IslandField f, IslandShape s) {
        SkyLakes.Lake lake = f.landmarks().lake(s);
        return s + " " + (lake == null ? "-" : lake.view() + " " + lake.brook()) + " "
                + f.landmarks().pairs(s).stream().map(p -> p.view() + " " + p.arch()).toList() + " " + f.landmarks().debris(s);
    }
}
