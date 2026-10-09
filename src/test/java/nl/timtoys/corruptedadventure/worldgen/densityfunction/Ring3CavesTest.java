package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.worldgen.feature.FireGeodeShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Package 42 on the test seed: lava tubes (dry main tubes with mouths and skylights, branches with a lava gutter),
 * magma chambers, fire geodes, and the fading of vanilla caves; pure and the same whoever asks first.
 */
class Ring3CavesTest {
    private static final int CELLS = 6400 / LavaTubes.CELL + 1;
    private static VolcanicField field;
    private static final List<int[]> cells = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        field = VolcanicFieldTest.field();
        for (int i = -CELLS; i <= CELLS; i++) {
            for (int j = -CELLS; j <= CELLS; j++) {
                if (!field.tubeSystem(i, j).tubes().isEmpty()) {
                    cells.add(new int[]{i, j});
                }
            }
        }
    }

    private static LavaTubes.TubeSystem system(int[] cell) {
        return field.tubeSystem(cell[0], cell[1]);
    }

    @Test
    void tubeSystemsArePureAndTheSameWhoeverAsksFirst() throws Exception {
        assertTrue(cells.size() >= 150, "tube systems " + cells.size());
        // A noise chunk's copy shares the decisions.
        VolcanicField copy = (VolcanicField) field.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
                return noise;
            }
        });
        int[] first = cells.get(0);
        assertSame(system(first), copy.tubeSystem(first[0], first[1]));
        // A fresh field on two threads, the cells in opposite orders: the same systems, the same tiles.
        VolcanicField fresh = VolcanicFieldTest.field();
        List<int[]> sample = cells.subList(0, 40);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> forward = pool.submit(() -> sample.forEach(c -> fresh.tubeSystem(c[0], c[1])));
            Future<?> backward = pool.submit(() -> {
                for (int k = sample.size() - 1; k >= 0; k--) {
                    fresh.tubeSystem(sample.get(k)[0], sample.get(k)[1]);
                }
            });
            forward.get();
            backward.get();
        } finally {
            pool.shutdown();
        }
        for (int[] c : sample) {
            assertSameSystem(system(c), fresh.tubeSystem(c[0], c[1]));
        }
        LavaTubes.Tube tube = system(first).tubes().get(0);
        int x = (int) Math.floor(tube.x()[tube.points() / 2]);
        int z = (int) Math.floor(tube.z()[tube.points() / 2]);
        for (int dy = -12; dy <= 12; dy++) {
            int y = (int) Math.floor(tube.floor()[tube.points() / 2]) + dy;
            assertEquals(field.thin(x, y, z), fresh.thin(x, y, z));
            assertEquals(field.caveWall(x, y, z), fresh.caveWall(x, y, z));
        }
    }

    private static void assertSameSystem(LavaTubes.TubeSystem a, LavaTubes.TubeSystem b) {
        assertEquals(a.tubes().size(), b.tubes().size());
        for (int k = 0; k < a.tubes().size(); k++) {
            LavaTubes.Tube ta = a.tubes().get(k);
            LavaTubes.Tube tb = b.tubes().get(k);
            assertTrue(Arrays.equals(ta.x(), tb.x()) && Arrays.equals(ta.z(), tb.z()) && Arrays.equals(ta.floor(), tb.floor())
                    && Arrays.equals(ta.half(), tb.half()) && ta.lava() == tb.lava());
        }
        assertEquals(a.skylights(), b.skylights());
        assertEquals(a.chamber(), b.chamber());
    }

    @Test
    void everyTileKnowsTheTubesAndChambersThatReachIt() {
        for (int[] cell : cells.subList(0, 40)) {
            LavaTubes.TubeSystem s = system(cell);
            for (LavaTubes.Tube tube : s.tubes()) {
                for (int k = 0; k < tube.points(); k += 4) {
                    LavaTubes.Tile tile = tileAt(tube.x()[k], tube.z()[k]);
                    assertTrue(Arrays.asList(tile.tubes()).contains(tube), "a tube missing from its tile");
                }
            }
            LavaTubes.Chamber ch = s.chamber();
            if (ch != null) {
                assertTrue(Arrays.asList(tileAt(ch.x() + ch.reach() - 1.0, ch.z()).chambers()).contains(ch), "a chamber missing");
            }
        }
    }

    private static LavaTubes.Tile tileAt(double x, double z) {
        return LavaTubes.tile(field, Math.floorDiv((int) Math.floor(x), LavaTubes.TILE), Math.floorDiv((int) Math.floor(z), LavaTubes.TILE));
    }

    @Test
    void mainTubesAreDryDeepAndOpenAtTheirMouths() {
        int mouths = 0;
        int skylights = 0;
        for (int[] cell : cells) {
            LavaTubes.TubeSystem s = system(cell);
            LavaTubes.Tube main = s.tubes().get(0);
            assertTrue(!main.lava() && main.half()[0] >= 2.5 && main.half()[0] <= 4.5, "5-9 wide, dry");
            VolcanicField.Column probe = new VolcanicField.Column();
            for (int k = 0; k < main.points(); k++) {
                double floor = main.floor()[k];
                assertTrue(floor >= LavaTubes.LOWEST_FLOOR - 1e-9, "floor " + floor);
                if (k > 0) {
                    double step = Math.abs(floor - main.floor()[k - 1]);
                    assertTrue(step <= LavaTubes.MOUTH_SLOPE * LavaTubes.STEP + 1e-9, "slope " + step);
                }
                // Never under lava with less than the roof between (also along the mouth ramps).
                VolcanicField.Column c = field.traceColumn(main.x()[k], main.z()[k], probe);
                if (c.hasLava()) {
                    assertTrue(floor + LavaTubes.Tube.clearance(main.half()[k]) + LavaTubes.ROOF <= c.lavaBottom() + 1e-9);
                }
            }
            if (main.floor()[0] >= field.traceGround(main.x()[0], main.z()[0]) - 4.0) {
                mouths++;
            }
            for (LavaTubes.Skylight light : s.skylights()) {
                skylights++;
                assertTrue(light.top() > field.traceGround(light.x(), light.z()) && light.bottom() < light.top());
            }
        }
        assertTrue(mouths >= cells.size() * 9 / 10, "open mouths " + mouths + " of " + cells.size());
        assertTrue(skylights >= cells.size(), "skylights " + skylights);
    }

    @Test
    void chambersLieInTheirCellInTheCoreWithABranchDownToTheirLake() {
        int chambers = 0;
        for (int[] cell : cells) {
            LavaTubes.TubeSystem s = system(cell);
            LavaTubes.Chamber ch = s.chamber();
            if (ch == null) {
                continue;
            }
            chambers++;
            assertTrue(ch.x() >= cell[0] * LavaTubes.CELL + LavaTubes.CHAMBER_INSET
                    && ch.x() <= (cell[0] + 1) * LavaTubes.CELL - LavaTubes.CHAMBER_INSET
                    && ch.z() >= cell[1] * LavaTubes.CELL + LavaTubes.CHAMBER_INSET
                    && ch.z() <= (cell[1] + 1) * LavaTubes.CELL - LavaTubes.CHAMBER_INSET, "inside its cell");
            assertTrue(ch.a() >= 15.0 && ch.a() <= 30.0, "30-60 wide");
            assertTrue(ch.dome() + ch.bowl() >= 14.0 && ch.dome() + ch.bowl() <= 30.0, "15-30 high");
            assertTrue(ch.bottom() >= -40.0 && ch.top() <= 10.0, "Y -40..10: " + ch.bottom() + ".." + ch.top());
            assertTrue(ch.lake() > ch.bottom() && ch.lake() < ch.centreY());
            assertTrue(field.regions(ch.x(), ch.z()).core() >= 0.99 && !field.nearAbyss(ch.x(), ch.z(), ch.reach()),
                    "in the core, away from the abyss and its lava sea");
            LavaTubes.Tube branch = s.tubes().get(1);
            assertTrue(branch.lava());
            int last = branch.points() - 1;
            assertEquals(ch.lake() + 1.0, branch.floor()[last], 1e-9);
            assertTrue(ch.distance(branch.x()[last], branch.z()[last], 0.0) < 0.6, "the branch ends inside the chamber");
            // The lake holds lava at its centre, its floor is rock (crust) and nothing is open below it.
            VolcanicField.Column centre = field.column((int) Math.floor(ch.x()), (int) Math.floor(ch.z()));
            assertTrue(centre.subLavaAt(ch.lake()) && !centre.voidAt(centre.subLavaMin() - 1));
            assertTrue(centre.guard(ch.lake()) == 1.0 && centre.guard((int) ch.top() + 2) == 1.0, "no caves around it");
        }
        assertTrue(chambers >= cells.size() * 6 / 10, "chambers " + chambers + " of " + cells.size());
    }

    @Test
    void lavaUnderTheGroundIsHeldByRockOrMoreLava() {
        // The aquifer's rule (RingAquifer): lava where the opening holds sub-ground lava, unless a dry opening lies beside
        // or below it (then a lip of crust). What it keeps as lava has only lava, rock or guarded rock around it.
        int checked = 0;
        for (int[] cell : cells.subList(0, Math.min(cells.size(), 60))) {
            LavaTubes.TubeSystem s = system(cell);
            if (s.tubes().size() < 2) {
                continue;
            }
            LavaTubes.Tube branch = s.tubes().get(1);
            for (int k = 0; k < branch.points(); k++) {
                int x = (int) Math.floor(branch.x()[k]);
                int z = (int) Math.floor(branch.z()[k]);
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        VolcanicField.Column c = field.column(x + dx, z + dz);
                        for (int y = c.subLavaMin(); y <= c.subLavaMax(); y++) {
                            if (!c.subLavaAt(y) || crusted(x + dx, y, z + dz)) {
                                continue;
                            }
                            checked++;
                            assertHeld(c, y - 1);
                            assertHeld(field.column(x + dx + 1, z + dz), y);
                            assertHeld(field.column(x + dx - 1, z + dz), y);
                            assertHeld(field.column(x + dx, z + dz + 1), y);
                            assertHeld(field.column(x + dx, z + dz - 1), y);
                        }
                    }
                }
            }
        }
        assertTrue(checked > 1000, "lava blocks checked " + checked);
    }

    private static boolean crusted(int x, int y, int z) {
        VolcanicField.Column c = field.column(x, z);
        return c.voidAt(y - 1) && !c.subLavaAt(y - 1) || dry(x + 1, y, z) || dry(x - 1, y, z) || dry(x, y, z + 1) || dry(x, y, z - 1);
    }

    private static boolean dry(int x, int y, int z) {
        VolcanicField.Column c = field.column(x, z);
        return c.voidAt(y) && !c.subLavaAt(y);
    }

    /** A neighbour of lava: lava itself, or rock that no vanilla cave or carver may open (inside the guard band). */
    private static void assertHeld(VolcanicField.Column c, int y) {
        if (c.voidAt(y)) {
            assertTrue(c.subLavaAt(y), "open air beside lava at " + y);
        } else {
            assertTrue(c.deepGuardAt(y), "unguarded rock beside lava at " + y);
        }
    }

    @Test
    void tubeWallsWrapEveryOpening() {
        int sections = 0;
        int wrapped = 0;
        for (int[] cell : cells.subList(0, 20)) {
            LavaTubes.Tube main = system(cell).tubes().get(0);
            for (int k = 2; k < main.points() - 2; k += 5) {
                int x = (int) Math.floor(main.x()[k]);
                int z = (int) Math.floor(main.z()[k]);
                VolcanicField.Column c = field.column(x, z);
                int floor = (int) Math.floor(main.floor()[k]);
                int top = floor;
                while (c.voidAt(top + 1) && top < floor + 12) {
                    top++;
                }
                sections++;
                // A smooth skin under and over the opening, the shell below it (unless another shape crosses here).
                if (c.voidAt(floor) && c.caveWall(floor - 1) == 1 && c.caveWall(top + 1) == 1 && c.caveWall(floor - 3) == 2) {
                    wrapped++;
                }
            }
        }
        assertTrue(wrapped >= sections * 9 / 10, "wrapped " + wrapped + " of " + sections);
    }

    @Test
    void fireGeodesAreClosedDeepAndInsideTheirCell() {
        int cells = 0;
        int geodes = 0;
        int checked = 0;
        int n = 6200 / FireGeodeShape.CELL;
        for (int i = -n; i <= n; i += 3) {
            for (int j = -n; j <= n; j += 3) {
                if (field.regions((i + 0.5) * FireGeodeShape.CELL, (j + 0.5) * FireGeodeShape.CELL).core() < 0.99) {
                    continue;
                }
                cells++;
                FireGeodeShape.Geode g = field.fireGeodeCell(i, j);
                if (g == null) {
                    continue;
                }
                geodes++;
                assertSame(g, field.fireGeode(g.x(), g.z()));
                int reach = g.reach();
                assertTrue(g.x() - reach >= i * FireGeodeShape.CELL && g.x() + reach < (i + 1) * FireGeodeShape.CELL
                        && g.z() - reach >= j * FireGeodeShape.CELL && g.z() + reach < (j + 1) * FireGeodeShape.CELL, "inside its cell");
                assertTrue(g.radius() >= 4 && g.radius() <= 7 && g.y() >= FireGeodeShape.Y_MIN && g.y() <= FireGeodeShape.Y_MAX);
                assertTrue(!field.nearAbyss(g.x(), g.z(), reach));
                if (checked < 25) {
                    checked++;
                    for (int x = g.x() - reach; x <= g.x() + reach; x++) {
                        for (int z = g.z() - reach; z <= g.z() + reach; z++) {
                            VolcanicField.Column c = field.column(x, z);
                            for (int y = g.y() - reach; y <= g.y() + reach; y++) {
                                if (g.inCavity(x, y, z)) {
                                    assertTrue(c.voidAt(y), "the cavity is carved");
                                    assertEquals(-1.0, field.fireGeodes(x, y, z));
                                } else {
                                    assertEquals(10.0, field.fireGeodes(x, y, z));
                                }
                                assertEquals(1.0, c.guard(y), "closed: no vanilla cave reaches it");
                            }
                        }
                    }
                }
            }
        }
        // About one per 12 chunks (a cell is 9 chunks).
        double chunksPerGeode = cells * 9.0 / geodes;
        assertTrue(chunksPerGeode > 10.0 && chunksPerGeode < 15.0, "chunks per geode " + chunksPerGeode);
    }

    @Test
    void vanillaCavesCloseGraduallyAboveY140AndInsideVolcanoes() {
        // A plain column away from volcanoes: caves run freely at Y 100, shrink from Y 126, are gone from Y 146.
        VolcanicField.Column plain = null;
        for (int x = -4900; x < -4600 && plain == null; x += 7) {
            VolcanicField.Column c = field.column(x, 700);
            if (c.rise == 0.0) {
                plain = c;
            }
        }
        assertNotNull(plain);
        assertEquals(0.0, plain.caveLift(100));
        assertEquals(0.0, plain.caveLift(126));
        assertTrue(plain.caveLift(136) > 0.5 && plain.caveLift(136) < 1.5, "half way");
        assertEquals(2.0, plain.caveLift(146));
        // Rising, never falling, with height.
        for (int y = 100; y < 160; y++) {
            assertTrue(plain.caveLift(y + 1) >= plain.caveLift(y));
        }
        // A volcano's body: gone in its cone, free far below the plain it stands on.
        int found = 0;
        for (Volcano v : volcanoes()) {
            if (v.kind() == Volcano.Kind.CALDERA) {
                continue;
            }
            int x = (int) Math.floor(v.x() + 0.55 * v.radius());
            VolcanicField.Column c = field.column(x, (int) Math.floor(v.z()));
            double plainY = c.height() - c.rise;
            if (c.rise < 30.0 || plainY > 120.0) {
                continue;
            }
            found++;
            assertEquals(2.0, c.caveLift((int) Math.ceil(plainY + 4.0)), "no cave mouths on the flank");
            assertEquals(0.0, c.caveLift((int) Math.floor(plainY - 20.0)));
        }
        assertTrue(found >= 5, "cones " + found);
    }

    @Test
    void lavaRangesAndGuardBandsOfAColumnStaySeparate() {
        // A gutter at Y 40 over a chamber's lake at Y −20..−15 in one column, with openings around both and a dry tube in
        // between: only the gutter and the lake are lava; only the bands around them are kept free of caves.
        VolcanicField.Column c = new VolcanicField.Column();
        c.reset(0, 0);
        c.height = 100.0;
        c.addCave(new int[]{40, 45}, new int[]{39, 46}, new int[]{37, 48}, 40);
        c.addSubLava(40, 40);
        c.addDeepGuard(35, 41);
        c.addCave(new int[]{-20, -5}, new int[]{-21, -4}, new int[]{-23, -2}, -14);
        c.addSubLava(-20, -15);
        c.addDeepGuard(-25, 0);
        c.addCave(new int[]{20, 26}, new int[]{19, 27}, new int[]{17, 29}, Integer.MIN_VALUE);
        assertTrue(c.subLavaAt(40) && c.subLavaAt(-20) && c.subLavaAt(-15));
        assertTrue(!c.subLavaAt(-14) && !c.subLavaAt(22) && !c.subLavaAt(41) && !c.subLavaAt(10));
        assertTrue(c.deepGuardAt(35) && c.deepGuardAt(0) && !c.deepGuardAt(10) && !c.deepGuardAt(30));
        assertEquals(-1.0, c.guard(22), "a vanilla cave may meet the dry tube");
        assertEquals(1.0, c.guard(-24));
        // The walls stop three blocks under the ground.
        c.reset(0, 0);
        c.height = 60.0;
        c.addCave(new int[]{50, 62}, new int[]{49, 63}, new int[]{47, 65}, Integer.MIN_VALUE);
        assertEquals(1, c.caveWall(49));
        assertEquals(2, c.caveWall(47));
        assertEquals(0, c.caveWall(58), "no wall paint at the surface around a mouth");
    }

    private static List<Volcano> volcanoes() {
        List<Volcano> found = new ArrayList<>();
        int size = field.settings().cellSize();
        for (int i = -6200 / size; i <= 6200 / size; i++) {
            for (int j = -6200 / size; j <= 6200 / size; j++) {
                Volcano v = field.volcano(i, j);
                if (v != null) {
                    found.add(v);
                }
            }
        }
        return found;
    }
}
