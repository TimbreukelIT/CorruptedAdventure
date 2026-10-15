package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.feature.LedgeStrands;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The horseshoe gorges of {@code verdant_falls} (W-terrein 2c part 2) on the test seed, block by block as the stack
 * and the aquifer make them ({@link WaterScan}): about fifty round the ring, walls exact and boundedly rough, the ledge
 * path behind the falls 3 wide all round the amphitheatre, every fall landing in the gorge's water, niches off the
 * path, climbing ledges the {@code ledge_strand} feature finds, water never against air, pure in any order.
 */
class VerdantGorgesTest {
    private static final IslandField FIELD = IslandFieldTest.field();
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;
    private static final VerdantGorges GORGES = FIELD.gorges();

    private static List<VerdantGorges.Gorge> gorges() {
        List<VerdantGorges.Gorge> found = new ArrayList<>();
        for (int k = 0; k < GORGES.gorgeCount(); k++) {
            VerdantGorges.Gorge g = GORGES.gorge(k);
            if (g != null) {
                found.add(g);
            }
        }
        return found;
    }

    /** Every n-th gorge round the ring. */
    private static List<VerdantGorges.Gorge> sample(int n) {
        List<VerdantGorges.Gorge> all = gorges();
        List<VerdantGorges.Gorge> found = new ArrayList<>();
        for (int i = 0; i < all.size(); i += n) {
            found.add(all.get(i));
        }
        return found;
    }

    /** One gorge per arc cell of ~700 blocks (54 cells), left out only where the rim is too low; walls 24 or 32 high. */
    @Test
    void aboutFiftyGorgesRoundTheRing() {
        assertEquals(54, GORGES.gorgeCount());
        List<VerdantGorges.Gorge> all = gorges();
        int falls = 0;
        for (VerdantGorges.Gorge g : all) {
            int height = (int) (VerdantDescent.level(g.top()) - VerdantDescent.level(g.floor()));
            assertTrue(height == 24 || height == 32, "wall " + height + " in gorge " + g.index());
            assertTrue(g.floor() >= 1, "the gorge lake above the sea in gorge " + g.index());
            assertTrue(g.radius() >= 45.0 && g.radius() <= 75.0, "half width " + g.radius());
            assertTrue(g.depth() >= 24.0 && g.depth() <= g.radius(), "depth " + g.depth());
            assertTrue(g.s0() >= (g.index() + 0.25) * GORGES.cellSize() - 1e-6 && g.s0() <= (g.index() + 0.75) * GORGES.cellSize() + 1e-6,
                    "gorge " + g.index() + " in the middle half of its cell");
            int arc = 0;
            for (VerdantGorges.GorgeFall f : GORGES.details(g).falls()) {
                arc += f.arc() ? 1 : 0;
            }
            falls += arc;
            assertTrue(arc >= 6, "falls along the arc of gorge " + g.index() + ": " + arc);
        }
        System.out.printf("gorges %d of %d, falls along the arcs %d (%.1f per gorge)%n", all.size(), GORGES.gorgeCount(), falls,
                falls / (double) all.size());
        assertTrue(all.size() >= 35, "gorges " + all.size());
    }

    /**
     * The wall is exact per block and its roughness bounded: more than {@link VerdantGorges#ROUGHNESS} + 0.5 into the
     * wall it is rock from the gorge's floor to the collar (but where the path, a ramp or a niche opens it), as far into
     * the gorge it is open (but a climbing ledge); between, the face moves, and it does move.
     */
    @Test
    void wallsAreExactAndBoundedlyRough() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int checked = 0;
        int rough = 0;
        for (VerdantGorges.Gorge g : sample(6)) {
            for (double t = -g.arcEnd() + 3.0; t <= g.arcEnd() - 3.0; t += 4.0) {
                for (double into : new double[]{-5.5, -4.2, -1.5, 1.5, 4.2, 5.5}) {
                    double[] q = VerdantGorges.wallAt(g, t, into);
                    int x = (int) Math.floor(q[0]);
                    int z = (int) Math.floor(q[1]);
                    IslandField.Column c = FIELD.column(x, z);
                    if (c.gorgeBlocks == null || c.gorgeOutside - c.gorgeInside < 6.0) {
                        continue;
                    }
                    double din = c.gorgeLocal.din;
                    int lo = (int) Math.ceil(c.gorgeInside) + 1;
                    int hi = (int) Math.floor(c.gorgeOutside) - 2;
                    for (int y = lo; y <= hi; y++) {
                        int block = scan.block(x, y, z);
                        if (din <= -VerdantGorges.ROUGHNESS - 0.5) {
                            boolean opened = y >= g.pathY() && y < g.pathY() + VerdantGorges.PATH_HEIGHT;
                            assertTrue(block == WaterScan.SOLID || opened, "rock in the wall at " + x + " " + y + " " + z);
                        } else if (din >= VerdantGorges.ROUGHNESS + 0.5) {
                            assertTrue(block != WaterScan.SOLID, "open in the gorge at " + x + " " + y + " " + z);
                        } else if ((block == WaterScan.SOLID) != (din <= 0.0)) {
                            rough++;
                        }
                        checked++;
                    }
                }
            }
        }
        System.out.printf("wall blocks checked %d, moved by the roughness (or path, ledge) %d%n", checked, rough);
        assertTrue(checked > 5000, "checked " + checked);
        assertTrue(rough > checked / 50, "the wall is rough: " + rough);
    }

    /**
     * The ledge path: all round the amphitheatre at half the wall's height, 3 wide into the wall and 4 high, its floor
     * rock; the falls stand one block beyond it (you walk behind the water).
     */
    @Test
    void theLedgePathIsThreeWideAndRunsRoundTheArc() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int steps = 0;
        for (VerdantGorges.Gorge g : sample(4)) {
            int y = g.pathY();
            int water = g.waterLimit();
            int top = (int) VerdantDescent.level(g.top());
            assertTrue(y - water >= 8 && top - (y + VerdantGorges.PATH_HEIGHT) >= 6, "path half way up the wall of gorge " + g.index());
            for (double t = -g.arcEnd() + 1.0; t <= g.arcEnd() - 1.0; t += 1.0) {
                // Every column along the wall's normal whose own distance into the wall lies in the path's 3 blocks.
                java.util.Set<Long> cells = new java.util.HashSet<>();
                for (double into = 0.5; into >= -4.0; into -= 0.25) {
                    double[] q = VerdantGorges.wallAt(g, t, into);
                    int x = (int) Math.floor(q[0]);
                    int z = (int) Math.floor(q[1]);
                    IslandField.Column c = FIELD.column(x, z);
                    double din = c.gorgeLocal.din;
                    if (din <= -VerdantGorges.PATH_DEPTH || din > 0.0 || c.gorgeLocal.w >= 0.0) {
                        // Past the arc's end the ramps begin (tested on their own).
                        continue;
                    }
                    cells.add(KeyedSlots.key(x, z));
                    String where = " at " + x + " " + y + " " + z + " (gorge " + g.index() + ", t " + Math.round(t) + ")";
                    assertEquals(WaterScan.SOLID, scan.block(x, y - 1, z), "path floor" + where);
                    for (int dy = 0; dy < VerdantGorges.PATH_HEIGHT; dy++) {
                        assertEquals(WaterScan.AIR, scan.block(x, y + dy, z), "path" + where + " + " + dy);
                    }
                }
                // A band of 3 blocks crosses the normal in 3 columns, or 2 where it runs diagonally.
                assertTrue(cells.size() >= 2 || Math.abs(t) > g.arcEnd() - 3.0,
                        "the path is about 3 wide at t " + t + " of gorge " + g.index() + ": " + cells.size());
                steps++;
            }
        }
        System.out.println("path steps checked: " + steps);
        assertTrue(steps > 500, "steps " + steps);
    }

    /** Every fall of a gorge: its spring walled in, open from the spring down to the water it lands in. */
    @Test
    void everyFallLandsInTheGorgesWater() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int arc = 0;
        int lip = 0;
        int behind = 0;
        for (VerdantGorges.Gorge g : sample(2)) {
            for (VerdantGorges.GorgeFall gf : GORGES.details(g).falls()) {
                VerdantBasins.Fall f = gf.fall();
                IslandField.Fall fall = f.fall();
                if (!FIELD.fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                    continue;
                }
                int sx = fall.springX();
                int sz = fall.springZ();
                int y = fall.springY();
                String where = " at " + sx + " " + y + " " + sz + " (gorge " + g.index() + ")";
                assertEquals(WaterScan.WATER, scan.block(sx, y, sz), "spring" + where);
                assertEquals(WaterScan.SOLID, scan.block(sx, y + 1, sz), "rock over the spring" + where);
                assertEquals(WaterScan.SOLID, scan.block(sx, y - 1, sz), "rock under the spring" + where);
                assertEquals(WaterScan.SOLID, scan.block(sx - f.stepZ(), y, sz + f.stepX()), "side" + where);
                assertEquals(WaterScan.SOLID, scan.block(sx + f.stepZ(), y, sz - f.stepX()), "side" + where);
                assertEquals(WaterScan.WATER, scan.block(sx - f.stepX(), y, sz - f.stepZ()), "the notch behind" + where);
                for (int fy = fall.bottom(); fy <= y; fy++) {
                    assertEquals(WaterScan.FALL, scan.block(fall.fallX(), fy, fall.fallZ()), "fall" + where + " at " + fy);
                }
                assertEquals(WaterScan.WATER, scan.block(fall.fallX(), fall.bottom() - 1, fall.fallZ()), "landing" + where);
                if (gf.arc()) {
                    arc++;
                    assertTrue(y - fall.bottom() >= 20, "an arc fall the wall's height" + where);
                    // Behind the water: the ledge path one block in from the falling column.
                    int px = fall.fallX() - f.stepX();
                    int pz = fall.fallZ() - f.stepZ();
                    if (scan.block(px, g.pathY(), pz) == WaterScan.AIR && scan.block(px, g.pathY() - 1, pz) == WaterScan.SOLID) {
                        behind++;
                    }
                } else {
                    lip++;
                }
            }
        }
        System.out.printf("falls checked: arc %d (path behind %d), lips %d%n", arc, behind, lip);
        assertTrue(arc > 100 && lip > 20, "arc " + arc + ", lips " + lip);
        assertTrue(behind >= arc * 0.95, "the path behind the falls: " + behind + " of " + arc);
    }

    /** One in three arc falls has a niche behind it off the ledge path: 3 wide, 5–8 deep, open from the path. */
    @Test
    void nichesOpenOffTheLedgePath() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int falls = 0;
        int niches = 0;
        for (VerdantGorges.Gorge g : sample(2)) {
            for (VerdantGorges.GorgeFall f : GORGES.details(g).falls()) {
                if (!f.arc()) {
                    continue;
                }
                falls++;
                if (f.niche() <= 0.0) {
                    continue;
                }
                niches++;
                assertTrue(f.niche() >= 5.0 && f.niche() <= 8.0, "depth " + f.niche());
                // From the path's back straight into the niche (but its rounded end), every column open at the path's
                // height with a rock floor: reached from the path.
                int cells = 0;
                for (double into = -0.5; into >= -(VerdantGorges.PATH_DEPTH + f.niche()); into -= 0.25) {
                    double[] q = VerdantGorges.wallAt(g, f.t(), into);
                    int x = (int) Math.floor(q[0]);
                    int z = (int) Math.floor(q[1]);
                    double din = FIELD.column(x, z).gorgeLocal.din;
                    if (din > 0.0 || din < -(VerdantGorges.PATH_DEPTH + 0.7 * f.niche())) {
                        continue;
                    }
                    String where = " at " + x + " " + z + " (gorge " + g.index() + ", din " + din + ")";
                    assertEquals(WaterScan.AIR, scan.block(x, g.pathY(), z), "niche" + where);
                    assertEquals(WaterScan.AIR, scan.block(x, g.pathY() + 1, z), "niche" + where);
                    assertEquals(WaterScan.SOLID, scan.block(x, g.pathY() - 1, z), "niche floor" + where);
                    cells += din <= -VerdantGorges.PATH_DEPTH ? 1 : 0;
                }
                assertTrue(cells >= 4, "niche cells past the path " + cells);
            }
        }
        System.out.printf("arc falls %d, niches %d%n", falls, niches);
        assertTrue(niches > falls / 5 && niches < falls / 2, "niches " + niches + " of " + falls);
    }

    /**
     * Q3: the climbing ledges overhang open air within 4 blocks (the feature's reach), each stacked a gap of 3–24 over
     * the next ledge or the floor, so {@code ledge_strand} hangs a strand from one to the next.
     */
    @Test
    void climbingLedgesAreFoundByTheStrandFeature() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int routes = 0;
        int ledges = 0;
        int found = 0;
        for (VerdantGorges.Gorge g : sample(3)) {
            for (VerdantGorges.Route r : GORGES.details(g).routes()) {
                routes++;
                double[] q = VerdantGorges.wallAt(g, r.t() - VerdantGorges.LEDGE_OUT * 0.0, 1.0);
                int x = (int) Math.floor(q[0]);
                int z = (int) Math.floor(q[1]);
                int top = 140;
                while (top > 40 && scan.block(x, top, z) == WaterScan.AIR) {
                    top--;
                }
                List<LedgeStrands.Overhang> overhangs = LedgeStrands.overhangs(top, 63, y -> scan.block(x, y, z) == WaterScan.AIR,
                        y -> scan.block(x, y, z) == WaterScan.SOLID);
                for (int k = 0; k < r.count(); k++) {
                    int under = r.firstY() + k * r.spacing();
                    if (r.skipped(under)) {
                        continue;
                    }
                    ledges++;
                    for (LedgeStrands.Overhang o : overhangs) {
                        if (o.underside() == under && o.gap() >= 3 && o.gap() <= 24 && openAround(scan, x, z, under)) {
                            found++;
                        }
                    }
                }
            }
        }
        System.out.printf("climbing routes %d, ledges %d, found by the strand scan %d%n", routes, ledges, found);
        assertTrue(routes > 30, "routes " + routes);
        assertTrue(found >= ledges * 0.8, "ledges found " + found + " of " + ledges);
    }

    /**
     * Q3, a real climbing route: each ledge has a knob over its end; the strand under the knob reaches down to the ledge
     * below (or the water or shore), and at the strand's top a column beside it holds the ledge's top to step onto.
     */
    @Test
    void knobsLeadFromLedgeToLedge() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int knobs = 0;
        int climbable = 0;
        int[] reasons = new int[3];
        for (VerdantGorges.Gorge g : sample(3)) {
            for (VerdantGorges.Route r : GORGES.details(g).routes()) {
                for (int k = 0; k < r.count(); k++) {
                    int under = r.firstY() + k * r.spacing();
                    if (r.skipped(under)) {
                        continue;
                    }
                    knobs++;
                    double[] q = VerdantGorges.wallAt(g, r.t() + VerdantGorges.knob(r, k), 0.8);
                    int knob = under + VerdantGorges.KNOB_RISE;
                    // Any of the knob's columns near the point (the block grid lies askew to the wall) with a strand the
                    // feature would hang (a gap of 3–24 under it, open air near) and the ledge's top beside its top.
                    int best = 0;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            int x = (int) Math.floor(q[0]) + dx;
                            int z = (int) Math.floor(q[1]) + dz;
                            if (scan.block(x, knob, z) != WaterScan.SOLID || scan.block(x, knob - 1, z) != WaterScan.AIR
                                    || scan.block(x, knob + 1, z) != WaterScan.AIR) {
                                continue;
                            }
                            best = Math.max(best, 1);
                            int floor = knob - 1;
                            while (floor > 40 && scan.block(x, floor, z) == WaterScan.AIR) {
                                floor--;
                            }
                            int gap = knob - floor - 1;
                            if (gap < 3 || gap > 24 || !openAround(scan, x, z, knob)) {
                                continue;
                            }
                            best = Math.max(best, 2);
                            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
                                if (scan.block(x + d[0], under + 1, z + d[1]) == WaterScan.SOLID
                                        && scan.block(x + d[0], under + 2, z + d[1]) == WaterScan.AIR
                                        && scan.block(x + d[0], under + 3, z + d[1]) == WaterScan.AIR) {
                                    best = 3;
                                }
                            }
                        }
                    }
                    climbable += best == 3 ? 1 : 0;
                    if (best < 3) {
                        reasons[best]++;
                    }
                }
            }
        }
        System.out.printf("knobs %d, with a strand down and a ledge to step onto %d (no knob %d, no strand %d, no step %d)%n", knobs,
                climbable, reasons[0], reasons[1], reasons[2]);
        assertTrue(knobs > 40, "knobs " + knobs);
        assertTrue(climbable >= knobs * 0.8, "climbable " + climbable + " of " + knobs);
    }

    /**
     * The last step of a route into the ledge path: over the topmost ledge under the path a knob stands in front of the
     * path's opening; the strand under it reaches the ledge, and from its top a player steps into the path.
     */
    @Test
    void routesClimbIntoTheLedgePath() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int routes = 0;
        int reach = 0;
        for (VerdantGorges.Gorge g : sample(3)) {
            for (VerdantGorges.Route r : GORGES.details(g).routes()) {
                int below = r.belowPath();
                if (below < 0 || Math.abs(r.t()) >= g.arcEnd() - 3.0) {
                    continue;
                }
                routes++;
                int knob = g.pathY() + VerdantGorges.PATH_KNOB_RISE;
                int under = r.firstY() + below * r.spacing();
                double[] q = VerdantGorges.wallAt(g, r.t() + ((below & 1) == 0 ? -1.0 : 1.0), 0.5);
                boolean ok = false;
                for (int dx = -1; dx <= 1 && !ok; dx++) {
                    for (int dz = -1; dz <= 1 && !ok; dz++) {
                        int x = (int) Math.floor(q[0]) + dx;
                        int z = (int) Math.floor(q[1]) + dz;
                        if (scan.block(x, knob, z) != WaterScan.SOLID || scan.block(x, knob - 1, z) != WaterScan.AIR) {
                            continue;
                        }
                        int floor = knob - 1;
                        while (floor > 40 && scan.block(x, floor, z) == WaterScan.AIR) {
                            floor--;
                        }
                        if (floor != under + 1 || knob - floor - 1 < 3) {
                            continue;
                        }
                        // A path block beside the strand: its floor rock, open above.
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
                            ok |= scan.block(x + d[0], g.pathY() - 1, z + d[1]) == WaterScan.SOLID
                                    && scan.block(x + d[0], g.pathY(), z + d[1]) == WaterScan.AIR
                                    && scan.block(x + d[0], g.pathY() + 1, z + d[1]) == WaterScan.AIR
                                    && FIELD.column(x + d[0], z + d[1]).gorgeLocal.din <= 0.0;
                        }
                    }
                }
                reach += ok ? 1 : 0;
            }
        }
        System.out.printf("routes up to the ledge path %d, climbable into it %d%n", routes, reach);
        assertTrue(routes > 10, "routes " + routes);
        assertTrue(reach >= routes * 0.8, "into the path " + reach + " of " + routes);
    }

    /** The feature's rule: a column within 4 whose surface (the highest non-air block) lies at or under the ledge's underside. */
    private static boolean openAround(WaterScan scan, int x, int z, int underside) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (scan.block(x + dx, underside, z + dz) == WaterScan.AIR && scan.block(x + dx, underside + 1, z + dz) == WaterScan.AIR
                        && scan.block(x + dx, underside + 6, z + dz) == WaterScan.AIR && scan.block(x + dx, underside + 20, z + dz) == WaterScan.AIR) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The waterscan's rule round the gorges: no water of a rim lake, notch, gorge lake or shore against air. */
    @Test
    void waterNeverStandsAgainstAirRoundTheGorges() {
        int water = 0;
        for (VerdantGorges.Gorge g : sample(4)) {
            int half = (int) (g.radius() + 40);
            WaterScan scan = new WaterScan(FIELD, LAYOUT);
            List<String> over = scan.scan((int) g.cx() - half, (int) g.cz() - half, (int) g.cx() + half, (int) g.cz() + half);
            assertTrue(over.isEmpty(), "water against air round gorge " + g.index() + ": " + over.subList(0, Math.min(10, over.size())));
            water += scan.water;
        }
        System.out.println("water blocks scanned round the gorges: " + water);
        assertTrue(water > 100_000, "water " + water);
    }

    /**
     * Pure: a field built apart, asked in the opposite order, gives the same gorges, falls and blocks; the gorge cells
     * wrap round the seam (a gorge's reach stays inside the middle of its cell, so none straddles the seam itself).
     */
    @Test
    void gorgesArePureWhateverTheOrderAndRoundTheSeam() {
        IslandField first = IslandFieldTest.field();
        IslandField other = IslandFieldTest.field();
        int count = first.gorges().gorgeCount();
        assertEquals(first.gorges().gorge(-1), first.gorges().gorge(count - 1));
        assertEquals(first.gorges().gorge(count), first.gorges().gorge(0));
        List<int[]> points = new ArrayList<>();
        List<VerdantGorges.Gorge> list = new ArrayList<>();
        for (int k : new int[]{0, count - 1, count / 2}) {
            VerdantGorges.Gorge g = first.gorges().gorge(k);
            if (g == null) {
                continue;
            }
            list.add(g);
            for (double t = -g.arcEnd() - 20.0; t <= g.arcEnd() + 20.0; t += 7.0) {
                for (double into : new double[]{-12.0, -2.0, 0.5, 3.0, 20.0}) {
                    double[] q = VerdantGorges.wallAt(g, t, into);
                    points.add(new int[]{(int) Math.floor(q[0]), (int) Math.floor(q[1])});
                }
            }
        }
        List<double[]> forward = new ArrayList<>();
        for (int[] p : points) {
            forward.add(values(first, p));
        }
        for (int i = points.size() - 1; i >= 0; i--) {
            assertArrayEquals(forward.get(i), values(other, points.get(i)), points.get(i)[0] + " " + points.get(i)[1]);
        }
        for (VerdantGorges.Gorge g : list) {
            VerdantGorges.Gorge again = other.gorges().gorge(g.index());
            assertEquals(g.s0(), again.s0());
            assertEquals(first.gorges().details(g).falls(), other.gorges().details(again).falls());
            assertEquals(first.gorges().details(g).routes(), other.gorges().details(again).routes());
            assertTrue(g.reach() < 0.25 * first.gorges().cellSize(), "the reach stays inside the cell");
        }
        // Across the seams: x < 0, z = 0 (the angle −π | π) and x > 0, z = 0 (where s wraps from the loop's length to 0 and
        // the cells start again), columns on both sides, both fields.
        for (int z = -6; z <= 6; z++) {
            for (int x = 5850; x <= 6200; x += 7) {
                assertArrayEquals(values(first, new int[]{-x, z}), values(other, new int[]{-x, z}), -x + " " + z);
                assertArrayEquals(values(first, new int[]{x, z}), values(other, new int[]{x, z}), x + " " + z);
            }
        }
    }

    private static double[] values(IslandField field, int[] p) {
        IslandField.Column c = field.column(p[0], p[1]);
        double[] v = {c.surface(), c.lakeTop(), c.springY(), c.fallTop(), c.caveLiftFrom(), c.rim(), c.cliff(), c.openFrom(), c.openTo(),
                0.0, 0.0, 0.0, 0.0};
        int base = (int) Math.floor(c.surface());
        for (int i = 0; i < 4; i++) {
            v[9 + i] = field.thin(p[0], base - 12 + 8 * i, p[1]);
        }
        return v;
    }
}
