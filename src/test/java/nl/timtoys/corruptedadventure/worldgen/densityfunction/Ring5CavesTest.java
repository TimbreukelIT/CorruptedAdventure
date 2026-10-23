package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ring 5's own caves (93, V93-2 … V93-5 and V93-8 = A): impossible corridors in loose nets on fixed levels (3 × 3 and
 * 2 × 1, straight and axis-aligned, dead ends and loops, a shaft up to the surface), copied nets side by side, void caves
 * with holes down into the void (dammed like the cracks), some mirrored in themselves, rare void caves in thick floes of
 * the outer band. All of it deep in its band; deterministic per seed.
 */
class Ring5CavesTest {
    private static final RingLayout LAYOUT = Ring5FieldTest.LAYOUT;
    private static final Ring5Field FIELD = Ring5FieldTest.field();
    private static final int REACH = 9800;

    private static List<Ring5Caves.Net> nets(Ring5Field field) {
        List<Ring5Caves.Net> nets = new ArrayList<>();
        int cells = REACH / Ring5Caves.NET_CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Ring5Caves.Net net = field.cavesNet(i, j);
                if (net != null) {
                    nets.add(net);
                }
            }
        }
        return nets;
    }

    private static List<Ring5Caves.Chamber> chambers(Ring5Field field) {
        List<Ring5Caves.Chamber> chambers = new ArrayList<>();
        int cells = REACH / Ring5Caves.CHAMBER_CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Ring5Caves.Chamber chamber = field.chamber(i, j);
                if (chamber != null) {
                    chambers.add(chamber);
                }
            }
        }
        return chambers;
    }

    private static List<Ring5Caves.Chamber> outerChambers(Ring5Field field) {
        List<Ring5Caves.Chamber> chambers = new ArrayList<>();
        int cells = REACH / Ring5Caves.OUTER_CELL + 1;
        for (int i = -cells; i <= cells; i++) {
            for (int j = -cells; j <= cells; j++) {
                Ring5Caves.Chamber chamber = field.outerChamber(i, j);
                if (chamber != null) {
                    chambers.add(chamber);
                }
            }
        }
        return chambers;
    }

    @Test
    void sameSeedSameCaves() {
        List<Ring5Caves.Net> a = nets(FIELD);
        List<Ring5Caves.Net> b = nets(Ring5FieldTest.field());
        assertEquals(a.size(), b.size());
        for (int k = 0; k < a.size(); k++) {
            assertEquals(a.get(k).boxes, b.get(k).boxes);
        }
        List<Ring5Caves.Chamber> c = chambers(FIELD);
        List<Ring5Caves.Chamber> d = chambers(Ring5FieldTest.field());
        assertEquals(c.size(), d.size());
        for (int k = 0; k < c.size(); k++) {
            assertEquals(c.get(k).x(), d.get(k).x());
            assertEquals(c.get(k).floor(), d.get(k).floor());
            assertEquals(c.get(k).holes().size(), d.get(k).holes().size());
        }
        // Another seed, other caves.
        assertTrue(!nets(Ring5FieldTest.field(Ring5FieldTest.SEED + 1)).getFirst().boxes.equals(a.getFirst().boxes));
    }

    /**
     * About one net per 128 × 128 × 0.4 of the deep inner band; every corridor exactly 3 × 3 or 2 high × 1 wide, straight
     * and axis-aligned, its floor on one of the fixed levels; every shaft 3 × 3; one in four nets about is copied.
     */
    @Test
    void corridorsAreExactStraightAndOnTheFixedLevels() {
        List<Ring5Caves.Net> nets = nets(FIELD);
        Set<Integer> levels = new HashSet<>();
        for (int k = 0; k < Ring5Caves.LEVELS; k++) {
            levels.add(Ring5Caves.LEVEL_TOP - k * Ring5Caves.LEVEL_STEP);
        }
        int copied = 0;
        int twoLevels = 0;
        int wide = 0;
        for (Ring5Caves.Net net : nets) {
            copied += net.copies > 1 ? 1 : 0;
            twoLevels += net.low != net.top ? 1 : 0;
            wide += net.wide ? 1 : 0;
            int across = net.wide ? 3 : 1;
            int height = net.wide ? Ring5Caves.WIDE_HEIGHT : Ring5Caves.NARROW_HEIGHT;
            int surface = 0;
            for (Ring5Caves.Box box : net.boxes) {
                int dx = box.x1() - box.x0() + 1;
                int dz = box.z1() - box.z0() + 1;
                if (box.shaft()) {
                    assertEquals(3, dx);
                    assertEquals(3, dz);
                    assertTrue(levels.contains(box.y0()), "shaft floor " + box.y0());
                    surface += box.y1() == Ring5Caves.SKY ? 1 : 0;
                    continue;
                }
                assertTrue(levels.contains(box.y0()), "floor " + box.y0());
                assertEquals(height, box.y1() - box.y0());
                assertTrue(dx == across && dz > across || dz == across && dx > across || dx == across && dz == across,
                        "not a straight corridor " + box);
            }
            assertEquals(net.copies, surface, "one entrance shaft per net (per copy)");
        }
        System.out.printf("nets: %d (wide %d, two levels %d, copied %d)%n", nets.size(), wide, twoLevels, copied);
        assertTrue(nets.size() > 500, "nets " + nets.size());
        double share = (double) copied / nets.size();
        assertTrue(share > 0.18 && share < 0.32, "copied share " + share);
        assertTrue(twoLevels > 50 && wide > nets.size() / 3 && wide < 2 * nets.size() / 3);
    }

    /** A grid net's corridors: dead ends and loops (a component with at least as many edges as nodes) both occur. */
    @Test
    void gridNetsHaveDeadEndsAndLoops() {
        int deadEnds = 0;
        int loops = 0;
        int grids = 0;
        for (Ring5Caves.Net net : nets(FIELD)) {
            if (net.copies > 1) {
                continue;
            }
            grids++;
            Map<Long, Integer> degree = new HashMap<>();
            int edges = 0;
            int half = net.wide ? Ring5Caves.WIDE_HALF : Ring5Caves.NARROW_HALF;
            for (Ring5Caves.Box box : net.boxes) {
                if (box.shaft() || box.y0() != net.top) {
                    continue;
                }
                edges++;
                long a = key(box.x0() + half, box.z0() + half);
                long b = key(box.x1() - half, box.z1() - half);
                degree.merge(a, 1, Integer::sum);
                degree.merge(b, 1, Integer::sum);
            }
            deadEnds += degree.containsValue(1) ? 1 : 0;
            // Over all components: edges − nodes + components > 0 means a cycle; one component at least.
            loops += edges >= degree.size() ? 1 : 0;
        }
        System.out.printf("grid nets %d: with dead ends %d, with loops %d%n", grids, deadEnds, loops);
        assertTrue(deadEnds > grids * 0.8, "dead ends " + deadEnds);
        assertTrue(loops > grids * 0.5, "loops " + loops);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** A copied net: 2–3 copies 8–24 apart across its axis, block for block the same corridor, stubs and shaft. */
    @Test
    void copiedNetsAreTheSameCorridorSideBySide() {
        int checked = 0;
        for (Ring5Caves.Net net : nets(FIELD)) {
            if (net.copies <= 1) {
                continue;
            }
            assertTrue(net.copies >= Ring5Caves.MIN_COPIES && net.copies <= Ring5Caves.MAX_COPIES);
            assertTrue(net.copyGap >= Ring5Caves.MIN_COPY_GAP && net.copyGap <= Ring5Caves.MAX_COPY_GAP);
            int per = net.boxes.size() / net.copies;
            assertEquals(per * net.copies, net.boxes.size());
            for (int k = 1; k < net.copies; k++) {
                int dx = net.copyAlongX ? 0 : k * net.copyGap;
                int dz = net.copyAlongX ? k * net.copyGap : 0;
                for (int b = 0; b < per; b++) {
                    Ring5Caves.Box first = net.boxes.get(b);
                    Ring5Caves.Box copy = net.boxes.get(k * per + b);
                    assertEquals(new Ring5Caves.Box(first.x0() + dx, first.z0() + dz, first.x1() + dx, first.z1() + dz,
                            first.y0(), first.y1(), first.shaft()), copy);
                }
            }
            // Rock between the copies: no box of one copy touches the next one's.
            for (int b = 0; b < per; b++) {
                for (int c = per; c < 2 * per; c++) {
                    Ring5Caves.Box p = net.boxes.get(b);
                    Ring5Caves.Box q = net.boxes.get(c);
                    boolean apart = p.x1() + 1 < q.x0() || q.x1() + 1 < p.x0() || p.z1() + 1 < q.z0() || q.z1() + 1 < p.z0();
                    assertTrue(apart, "copies touch: " + p + " / " + q);
                }
            }
            checked++;
        }
        assertTrue(checked > 100, "copied nets " + checked);
    }

    /** In the field a net's corridors are open blocks (code corridor) and its entrance shaft opens up to the sky. */
    @Test
    void corridorsAreOpenInTheTerrainAndTheShaftReachesTheSurface() {
        int shafts = 0;
        for (Ring5Caves.Net net : nets(FIELD).subList(0, 120)) {
            for (Ring5Caves.Box box : net.boxes) {
                int x = (box.x0() + box.x1()) / 2;
                int z = (box.z0() + box.z1()) / 2;
                Ring5Field.Column column = FIELD.column(x, z);
                if (column.isVoid() || column.copy != null) {
                    continue; // a crack across the net, or a doppelganger's copy over it
                }
                assertTrue(FIELD.terrain(x, box.y0(), z) < 0.0, "corridor closed at " + x + ", " + box.y0() + ", " + z);
                assertEquals(Ring5Caves.Kind.CORRIDOR.code(), FIELD.caveCode(x, box.y0(), z));
                assertTrue(FIELD.terrain(x, box.y0() - 1, z) > 0.0 || column.caves > 1, "no floor under " + x + ", " + z);
                if (box.y1() == Ring5Caves.SKY) {
                    for (int y = box.y0(); y <= column.top() + 1; y++) {
                        assertTrue(FIELD.terrain(x, y, z) < 0.0, "shaft closed at " + x + ", " + y + ", " + z);
                    }
                    shafts++;
                }
            }
        }
        assertTrue(shafts > 100, "shafts " + shafts);
    }

    /**
     * Void caves: about one per 256 × 256 of the deep inner band, Ø 12–30, floor Y −40 … 10, 1–4 holes Ø 2–6 inside the
     * chamber; on a hole the column is open down past the world's bottom, the opening distance at most 0 there and at
     * most the dam's 6 within 6 of it (the lava layer's dam, {@code RingFluidPicker.ring5Dam}).
     */
    @Test
    void voidCavesHaveHolesIntoTheVoidAndAreDammed() {
        List<Ring5Caves.Chamber> chambers = chambers(FIELD);
        int mirrored = 0;
        int holes = 0;
        for (Ring5Caves.Chamber chamber : chambers) {
            assertTrue(chamber.radius() >= Ring5Caves.MIN_RADIUS && chamber.radius() <= Ring5Caves.MAX_RADIUS);
            assertTrue(chamber.floor() >= Ring5Caves.MIN_FLOOR && chamber.floor() <= Ring5Caves.MAX_FLOOR);
            assertTrue(chamber.holes().size() >= Ring5Caves.MIN_HOLES && chamber.holes().size() <= Ring5Caves.MAX_HOLES);
            mirrored += chamber.mirrored() ? 1 : 0;
            for (double[] hole : chamber.holes()) {
                holes++;
                assertTrue(hole[2] >= Ring5Caves.MIN_HOLE && hole[2] <= Ring5Caves.MAX_HOLE);
                int x = (int) Math.floor(hole[0]);
                int z = (int) Math.floor(hole[1]);
                Ring5Field.Column column = FIELD.column(x, z);
                if (column.isVoid() || column.copy != null) {
                    continue;
                }
                assertEquals(Ring5Caves.Kind.HOLE.code(), FIELD.caveCode(x, chamber.floor(), z));
                for (int y = -64; y <= chamber.floor(); y += 3) {
                    assertTrue(FIELD.terrain(x, y, z) < 0.0, "hole closed at " + x + ", " + y + ", " + z);
                }
                assertTrue(FIELD.innerOpeningDistance(x, z) <= 0.0);
                int ring = (int) Math.ceil(hole[2]) + 4;
                assertTrue(FIELD.innerOpeningDistance(x + ring, z) <= 6.0, "no dam next to a hole");
                // The rim round the hole is marked for the voidstone.
                int rimX = (int) Math.floor(hole[0] + hole[2] + 0.6);
                Ring5Field.Column rim = FIELD.column(rimX, z);
                if (FIELD.chamberHoleDistance(rimX, z) > 0.0 && rim.rimTop != Integer.MIN_VALUE && rim.caves == 1) {
                    assertEquals(Ring5Caves.Kind.RIM.code(), FIELD.caveCode(rimX, FIELD.column(rimX, z).rimTop - 1, z));
                }
            }
        }
        System.out.printf("void caves: %d (mirrored %d), holes %d%n", chambers.size(), mirrored, holes);
        assertTrue(chambers.size() > 400, "chambers " + chambers.size());
        double share = (double) mirrored / chambers.size();
        assertTrue(share > 0.15 && share < 0.35, "mirrored " + share);
    }

    /** A mirrored chamber: its open intervals are the same on both sides of its axis, block for block. */
    @Test
    void mirroredChambersMirrorThemselves() {
        int checked = 0;
        int[] a = new int[4];
        int[] b = new int[4];
        for (Ring5Caves.Chamber chamber : chambers(FIELD)) {
            if (!chamber.mirrored()) {
                continue;
            }
            boolean axisX = chamber.axis() == 0.0;
            int reach = (int) Math.ceil(chamber.radius() * (1.0 + Ring5Caves.RAGGED)) + 1;
            int cx = (int) chamber.x();
            int cz = (int) chamber.z();
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    int x = cx + dx;
                    int z = cz + dz;
                    int mx = axisX ? x : 2 * cx - x - 1;
                    int mz = axisX ? 2 * cz - z - 1 : z;
                    boolean in = chamber.column(x, z, a);
                    assertEquals(in, chamber.column(mx, mz, b), "mirror at " + x + ", " + z);
                    if (in) {
                        assertEquals(a[0], b[0]);
                        assertEquals(a[1], b[1]);
                        assertEquals(a[2], b[2]);
                    }
                }
            }
            checked++;
        }
        assertTrue(checked > 50, "mirrored chambers " + checked);
    }

    /**
     * The outer band (V93-8 A): no corridors, only a few chambers in thick floes near the band line, their holes open
     * through the floe's underside; the inner caves keep off the band line, the zone 4→5 and the castles.
     */
    @Test
    void outerBandHasOnlyRareVoidCavesInThickFloes() {
        Ring5Band band = Ring5Band.of(LAYOUT);
        List<Ring5Caves.Chamber> outer = outerChambers(FIELD);
        System.out.printf("outer void caves: %d%n", outer.size());
        assertTrue(outer.size() >= 3 && outer.size() <= 60, "outer chambers " + outer.size());
        for (Ring5Caves.Chamber chamber : outer) {
            double signed = band.signedDistance(chamber.x(), chamber.z());
            assertTrue(signed > 0.0 && signed <= Ring5Field.OUTER_CAVE_REACH, "signed " + signed);
            int x = (int) Math.floor(chamber.x());
            int z = (int) Math.floor(chamber.z());
            Ring5Field.Column column = FIELD.column(x, z);
            assertTrue(!column.isVoid() && column.top() - column.bottom() >= Ring5Caves.OUTER_MIN_THICKNESS);
            assertTrue(chamber.floor() > column.bottom() && chamber.floor() + chamber.height() < column.top());
            double[] hole = chamber.holes().getFirst();
            int hx = (int) Math.floor(hole[0]);
            int hz = (int) Math.floor(hole[1]);
            assertEquals(Ring5Caves.Kind.HOLE.code(), FIELD.caveCode(hx, chamber.floor(), hz));
            assertTrue(FIELD.terrain(hx, (int) Math.floor(FIELD.column(hx, hz).bottom()) + 1, hz) < 0.0, "hole shut in the floe");
        }
        for (Ring5Caves.Net net : nets(FIELD)) {
            for (int[] p : new int[][]{{net.minX, net.minZ}, {net.maxX, net.maxZ}, {net.minX, net.maxZ}, {net.maxX, net.minZ}}) {
                assertTrue(band.signedDistance(p[0], p[1]) <= -Ring5Field.SPOT_CLEARANCE, "net near the band line");
            }
        }
    }

    /** Vanilla's damped caves: open deep in the inner band, closed in the outer band and right next to a void-cave hole. */
    @Test
    void naturalCavesOnlyDeepInTheInnerBand() {
        Ring5Band band = Ring5Band.of(LAYOUT);
        int open = 0;
        int inner = 0;
        for (int distance = 8200; distance <= 9600; distance += 29) {
            for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 30.0) {
                int x = (int) Math.round(distance * Math.cos(angle));
                int z = (int) Math.round(distance * Math.sin(angle));
                Ring5Field.Column column = FIELD.column(x, z);
                double signed = band.signedDistance(x, z);
                if (signed >= 0.0 || column.isVoid()) {
                    assertEquals(Ring5Field.CAVES_CLOSED, column.closure(), "open outside the inner band at " + x + ", " + z);
                } else if (column.castle() == 0.0 && signed < -Ring5Field.NATURAL_LINE - Ring5Field.NATURAL_FADE && distance > 8400) {
                    inner++;
                    open += column.closure() == 0.0 ? 1 : 0;
                }
            }
        }
        assertTrue(open > 0.9 * inner, "open " + open + " of " + inner);
        for (Ring5Caves.Chamber chamber : chambers(FIELD).subList(0, 40)) {
            double[] hole = chamber.holes().getFirst();
            Ring5Field.Column column = FIELD.column((int) Math.floor(hole[0]), (int) Math.floor(hole[1]));
            if (!column.isVoid() && column.copy == null) {
                assertEquals(Ring5Field.CAVES_CLOSED, column.closure(), 1e-9);
            }
        }
    }
}
