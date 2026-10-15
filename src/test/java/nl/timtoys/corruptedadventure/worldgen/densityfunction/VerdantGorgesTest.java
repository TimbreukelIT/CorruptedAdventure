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
            // At least 5 (one gorge near the rim has 5 since its rim lake keeps clear of steam_fens' pools, 2c part 2b).
            assertTrue(arc >= 5, "falls along the arc of gorge " + g.index() + ": " + arc);
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
                            int path = g.pathAt(c.gorgeLocal.t);
                            boolean opened = y >= path && y < path + VerdantGorges.PATH_HEIGHT;
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
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (VerdantGorges.Gorge g : sample(4)) {
            int water = g.waterLimit();
            int top = (int) VerdantDescent.level(g.top());
            for (double t = -g.arcEnd() + 1.0; t <= g.arcEnd() - 1.0; t += 1.0) {
                int y = g.pathAt(t);
                assertTrue(y - water >= 8 && top - (y + VerdantGorges.PATH_HEIGHT) >= 6, "path half way up the wall of gorge " + g.index());
                assertTrue(Math.abs(y - g.pathAt(t - 1.0)) <= 1, "the path's floor steps at most a block at t " + t + " of gorge " + g.index());
                lowest = Math.min(lowest, y - g.pathY());
                highest = Math.max(highest, y - g.pathY());
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
                    y = VerdantGorges.pathFloor(g, c.gorgeLocal);
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
        System.out.printf("path steps checked: %d, its floor %d to %d about the middle%n", steps, lowest, highest);
        assertTrue(steps > 500, "steps " + steps);
        assertTrue(lowest <= -1 && highest >= 1, "the path's floor waves");
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
                // Its sides rock, or the spring or notch of a neighbouring column of a wide fall.
                assertTrue(scan.block(sx - f.stepZ(), y, sz + f.stepX()) != WaterScan.AIR, "side" + where);
                assertTrue(scan.block(sx + f.stepZ(), y, sz - f.stepX()) != WaterScan.AIR, "side" + where);
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
                    int path = VerdantGorges.pathFloor(g, FIELD.column(px, pz).gorgeLocal);
                    if (scan.block(px, path, pz) == WaterScan.AIR && scan.block(px, path - 1, pz) == WaterScan.SOLID) {
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
                // Per fall (a wide fall's middle column may have one).
                falls += wideFallStart(g, f) ? 1 : 0;
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
                    int floor = g.pathAt(FIELD.column(x, z).gorgeLocal.t);
                    assertEquals(WaterScan.AIR, scan.block(x, floor, z), "niche" + where);
                    assertEquals(WaterScan.AIR, scan.block(x, floor + 1, z), "niche" + where);
                    assertEquals(WaterScan.SOLID, scan.block(x, floor - 1, z), "niche floor" + where);
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
     * Q3 + decision 2026-10-15, every route climbable: each ledge has a knob over its end, found by the terrain query of
     * {@code ledge_strand} ({@link IslandField#climbKnobs} over the knob's chunk, which the feature walks on every
     * placement); under it the feature always hangs a strand ({@link LedgeStrands#hang} for a knob, whatever the roll),
     * which reaches down to the ledge below (or the water or shore), and at its top a column beside it holds the ledge's
     * top to step onto. 100 % of the knobs, so 100 % of the routes.
     */
    @Test
    void everyKnobGetsAStrandToTheLedgeBelow() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int knobs = 0;
        int climbable = 0;
        int routes = 0;
        int whole = 0;
        int[] reasons = new int[4];
        List<String> missing = new ArrayList<>();
        for (VerdantGorges.Gorge g : sample(2)) {
            for (VerdantGorges.Route r : GORGES.details(g).routes()) {
                routes++;
                boolean all = true;
                for (int k = 0; k < r.count(); k++) {
                    int under = r.firstY() + k * r.spacing();
                    if (r.skipped(under)) {
                        continue;
                    }
                    knobs++;
                    int knob = under + VerdantGorges.KNOB_RISE;
                    int best = 0;
                    for (int[] c : knobColumns(g, r.t() + VerdantGorges.knob(r, k), knob)) {
                        best = Math.max(best, 1);
                        int floor = strandFloor(scan, c[0], knob, c[1]);
                        if (floor == Integer.MIN_VALUE) {
                            continue;
                        }
                        best = Math.max(best, 2);
                        for (int[] d : AROUND) {
                            if (scan.block(c[0] + d[0], under + 1, c[1] + d[1]) == WaterScan.SOLID
                                    && scan.block(c[0] + d[0], under + 2, c[1] + d[1]) == WaterScan.AIR
                                    && scan.block(c[0] + d[0], under + 3, c[1] + d[1]) == WaterScan.AIR) {
                                best = 3;
                            }
                        }
                    }
                    climbable += best == 3 ? 1 : 0;
                    all &= best == 3;
                    reasons[best]++;
                    if (best < 3) {
                        double[] q = VerdantGorges.wallAt(g, r.t() + VerdantGorges.knob(r, k), 0.8);
                        missing.add("gorge " + g.index() + " t " + Math.round(r.t()) + " k " + k + " knob " + (int) Math.floor(q[0]) + " " + knob
                                + " " + (int) Math.floor(q[1]) + " best " + best);
                    }
                }
                whole += all ? 1 : 0;
            }
        }
        System.out.printf("knobs %d, with a strand down and a ledge to step onto %d (not found %d, no strand %d, no step %d); "
                + "routes %d, climbable throughout %d%n", knobs, climbable, reasons[0], reasons[1], reasons[2], routes, whole);
        assertTrue(knobs > 60, "knobs " + knobs);
        assertEquals(knobs, climbable, "every knob climbable: " + missing);
        assertEquals(routes, whole, "every route climbable");
    }

    /**
     * The last step of a route into the ledge path: over the topmost ledge under the path a knob stands in front of the
     * path's opening (found by the terrain query, always a strand); the strand reaches the ledge, and from its top a
     * player steps into the path.
     */
    @Test
    void routesClimbIntoTheLedgePath() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int routes = 0;
        int reach = 0;
        List<String> missing = new ArrayList<>();
        for (VerdantGorges.Gorge g : sample(2)) {
            for (VerdantGorges.Route r : GORGES.details(g).routes()) {
                int below = r.belowPath();
                if (below < 0 || Math.abs(r.t()) >= g.arcEnd() - 3.0) {
                    continue;
                }
                routes++;
                int knob = VerdantGorges.pathKnobY(g, r);
                int under = r.firstY() + below * r.spacing();
                boolean ok = false;
                for (int[] c : knobColumns(g, r.t() + ((below & 1) == 0 ? -1.0 : 1.0), knob)) {
                    int x = c[0];
                    int z = c[1];
                    if (strandFloor(scan, x, knob, z) != under + 1) {
                        continue;
                    }
                    // A path block beside the strand: its floor rock, open above.
                    for (int[] d : AROUND) {
                        VerdantGorges.Local beside = FIELD.column(x + d[0], z + d[1]).gorgeLocal;
                        int floor = beside.din <= 0.0 ? VerdantGorges.pathFloor(g, beside) : Integer.MIN_VALUE;
                        ok |= floor != Integer.MIN_VALUE && floor >= knob - VerdantGorges.PATH_KNOB_RISE - 1
                                && scan.block(x + d[0], floor - 1, z + d[1]) == WaterScan.SOLID
                                && scan.block(x + d[0], floor, z + d[1]) == WaterScan.AIR
                                && scan.block(x + d[0], floor + 1, z + d[1]) == WaterScan.AIR;
                    }
                }
                reach += ok ? 1 : 0;
                if (!ok) {
                    missing.add("gorge " + g.index() + " route t " + Math.round(r.t()) + " below " + below + " under " + under);
                }
            }
        }
        System.out.printf("routes up to the ledge path %d, climbable into it %d%n", routes, reach);
        assertTrue(routes > 10, "routes " + routes);
        assertEquals(routes, reach, "into the path: " + missing);
    }

    private static final int[][] AROUND = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    /** Whether an arc fall begins a fall (its predecessor by t is not a column beside it: a wide fall counts once). */
    private static boolean wideFallStart(VerdantGorges.Gorge g, VerdantGorges.GorgeFall f) {
        VerdantGorges.GorgeFall before = null;
        for (VerdantGorges.GorgeFall o : GORGES.details(g).falls()) {
            if (o == f) {
                break;
            }
            before = o.arc() ? o : before;
        }
        return before == null || !besideEachOther(before, f);
    }

    /** Whether two arc falls are columns of one wide fall: their springs within two blocks. */
    private static boolean besideEachOther(VerdantGorges.GorgeFall a, VerdantGorges.GorgeFall b) {
        IslandField.Fall fa = a.fall().fall();
        IslandField.Fall fb = b.fall().fall();
        return Math.abs(fa.springX() - fb.springX()) <= 2 && Math.abs(fa.springZ() - fb.springZ()) <= 2;
    }

    /**
     * The knob blocks at y near the wall's point at t, as {@code ledge_strand} finds them: the terrain query over the
     * chunk the feature decorates, each also a knob per column ({@link IslandField#climbKnob}).
     */
    private static List<int[]> knobColumns(VerdantGorges.Gorge g, double t, int y) {
        double[] q = VerdantGorges.wallAt(g, t, 0.8);
        int x0 = (int) Math.floor(q[0]);
        int z0 = (int) Math.floor(q[1]);
        List<int[]> found = new ArrayList<>();
        for (net.minecraft.core.BlockPos p : FIELD.climbKnobs((x0 >> 4) << 4, (z0 >> 4) << 4, ((x0 >> 4) << 4) + 15, ((z0 >> 4) << 4) + 15)) {
            if (p.getY() == y && Math.abs(p.getX() - x0) <= 3 && Math.abs(p.getZ() - z0) <= 3) {
                assertTrue(FIELD.climbKnob(p.getX(), y, p.getZ()));
                found.add(new int[]{p.getX(), p.getZ()});
            }
        }
        // The knob's columns in the neighbouring chunks come with their own chunk's query.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                if ((x >> 4 != x0 >> 4 || z >> 4 != z0 >> 4) && FIELD.climbKnob(x, y, z)) {
                    int cx = (x >> 4) << 4;
                    int cz = (z >> 4) << 4;
                    assertTrue(FIELD.climbKnobs(cx, cz, cx + 15, cz + 15).contains(new net.minecraft.core.BlockPos(x, y, z)),
                            "knob " + x + " " + y + " " + z + " found by its chunk's query");
                    found.add(new int[]{x, z});
                }
            }
        }
        return found;
    }

    /**
     * The floor the strand under a knob reaches, where the feature hangs one (the knob rock with air below, a gap the
     * strand fills: {@link LedgeStrands#hang} for a knob gives a strand whatever the roll), else MIN_VALUE.
     */
    private static int strandFloor(WaterScan scan, int x, int knob, int z) {
        if (scan.block(x, knob, z) != WaterScan.SOLID || scan.block(x, knob - 1, z) != WaterScan.AIR) {
            return Integer.MIN_VALUE;
        }
        int floor = knob - 1;
        while (floor > 40 && scan.block(x, floor, z) == WaterScan.AIR) {
            floor--;
        }
        int gap = knob - floor - 1;
        for (float roll : new float[]{0.0F, 0.6F, 0.99F}) {
            if (LedgeStrands.hang(gap, true, roll, STRAND_CHANCE, ACCENT_CHANCE, MIN_GAP, MAX_GAP) != LedgeStrands.Hang.STRAND) {
                return Integer.MIN_VALUE;
            }
        }
        return floor;
    }

    /** {@code ring_4/verdant_ledge_strands} (VerdantFallsFeatures). */
    private static final float STRAND_CHANCE = 0.5F;
    private static final float ACCENT_CHANCE = 0.2F;
    private static final int MIN_GAP = 3;
    private static final int MAX_GAP = 24;

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

    /**
     * The waterscan's rule round the gorges and their gullies: no water of a rim lake, notch, gorge lake, gully or shore
     * against air. Gorge 53 always (the pre-INT in-game leak at its rim lake), and every gorge with an arch.
     */
    @Test
    void waterNeverStandsAgainstAirRoundTheGorges() {
        int water = 0;
        List<VerdantGorges.Gorge> scanned = new ArrayList<>(sample(4));
        scanned.add(GORGES.gorge(53));
        for (VerdantGorges.Gorge g : gorges()) {
            if (GORGES.details(g).arch() != null && !scanned.contains(g)) {
                scanned.add(g);
            }
        }
        for (VerdantGorges.Gorge g : scanned) {
            // Out along the axis: the gully runs on to the foot river.
            int half = (int) (g.radius() + 45);
            int mx = (int) (g.cx() + 30.0 * g.ox());
            int mz = (int) (g.cz() + 30.0 * g.oz());
            WaterScan scan = new WaterScan(FIELD, LAYOUT);
            List<String> over = scan.scan(mx - half, mz - half, mx + half, mz + half);
            assertTrue(over.isEmpty(), "water against air round gorge " + g.index() + ": " + over.subList(0, Math.min(10, over.size())));
            water += scan.water;
        }
        System.out.println("gorges scanned " + scanned.size() + ", water blocks " + water);
        assertTrue(water > 200_000, "water " + water);
    }

    /**
     * The pre-INT in-game leak (world ring4-vf, gorge 53's rim lake at Y 110): its bank lay 64–66 blocks into the zone,
     * in reach of steam_fens' pools at the biome edge (60), which dug it down to Y 109 (the offline scan knows no
     * features). Every rim lake now keeps its water and every neighbour of it beyond that reach, and the reported blocks
     * are rock or walled-in water.
     */
    @Test
    void rimLakesKeepClearOfSteamFensPools() {
        // steam_fens ends (and its pools are placed up to) the biome split at VERDANT_EDGE_SHARE of the zone.
        double width = VerdantDescent.width(LAYOUT);
        double edge = IslandField.VERDANT_EDGE_SHARE * width + VerdantGorges.STEAM_FENS_REACH;
        int columns = 0;
        for (VerdantGorges.Gorge g : gorges()) {
            for (double t = -g.arcEnd(); t <= g.arcEnd(); t += 1.0) {
                for (double into = -VerdantGorges.RIM_LAKE_IN; into >= -VerdantGorges.RIM_LAKE_IN - 9.0; into -= 0.5) {
                    double[] q = VerdantGorges.wallAt(g, t, into);
                    int x = (int) Math.floor(q[0]);
                    int z = (int) Math.floor(q[1]);
                    if (FIELD.column(x, z).lakeTop() != g.rimWater() || FIELD.column(x, z).gorgeLocal.din >= 0.0) {
                        continue;
                    }
                    columns++;
                    for (int[] d : AROUND) {
                        double u = LAYOUT.sampleOverworld(x + d[0], z + d[1]).blend() * width;
                        assertTrue(u >= edge, "rim lake of gorge " + g.index() + " at " + x + " " + z + ": neighbour at u " + u);
                    }
                }
            }
        }
        System.out.println("rim lake columns checked: " + columns);
        assertTrue(columns > 1000, "columns " + columns);
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        for (int[] p : new int[][]{{5883, -247}, {5883, -246}, {5884, -247}, {5895, -279}, {5895, -280}, {5896, -280}}) {
            int block = scan.block(p[0], 110, p[1]);
            assertTrue(block == WaterScan.SOLID || block == WaterScan.WATER, "at " + p[0] + " 110 " + p[1]);
            if (block == WaterScan.WATER) {
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    assertTrue(scan.block(p[0] + d[0], 110, p[1] + d[1]) != WaterScan.AIR, "beside " + p[0] + " 110 " + p[1]);
                }
            }
        }
    }

    /**
     * Decision 2026-10-15: past the amphitheatre the arms pinch into an outlet gully about 20–30 wide (measured across
     * its middle, every 4 blocks along its first 40 past its rounded mouth), never straight.
     */
    @Test
    void theArmsPinchIntoAGullyTwentyToThirtyWide() {
        List<Integer> widths = new ArrayList<>();
        for (VerdantGorges.Gorge g : gorges()) {
            for (double w = g.gullyFrom() + 4.0; w <= g.gullyFrom() + 44.0; w += 4.0) {
                int width = gullyWidth(g, w);
                if (width < 0) {
                    break;
                }
                widths.add(width);
            }
        }
        java.util.Collections.sort(widths);
        System.out.printf("gully widths %d: min %d, p50 %d, max %d%n", widths.size(), widths.get(0), widths.get(widths.size() / 2),
                widths.get(widths.size() - 1));
        assertTrue(widths.size() > 200, "widths " + widths.size());
        assertTrue(widths.get(0) >= 20 && widths.get(widths.size() - 1) <= 30, "widths " + widths.get(0) + "–" + widths.get(widths.size() - 1));
    }

    /**
     * Decision 2026-10-15 (review 2c-2b, "helling bij elke lip"): along every arm the dry shore runs on from the
     * amphitheatre to past the gully's mouth, through every lip by a ramp: a search over standing places (rock below, two
     * blocks of air, no water) with steps of at most one block, from the arm's first shore to 4 blocks into the gully (or
     * the foot river's bank, where the arm reaches it first).
     */
    @Test
    void theShoreRunsOnAlongEveryArmToTheGully() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int arms = 0;
        int walked = 0;
        List<String> missing = new ArrayList<>();
        for (VerdantGorges.Gorge g : gorges()) {
            for (int side = -1; side <= 1; side += 2) {
                arms++;
                if (shoreWalks(scan, g, side)) {
                    walked++;
                } else {
                    missing.add("gorge " + g.index() + " side " + side);
                }
            }
        }
        System.out.printf("arms %d, shore walkable to the gully %d%n", arms, walked);
        assertEquals(arms, walked, "the shore runs on: " + missing);
    }

    /** The search of one arm: over the arm's columns inside the gorge, within 9 blocks of its wall. */
    private static boolean shoreWalks(WaterScan scan, VerdantGorges.Gorge g, int side) {
        double goal = g.gullyFrom() + 4.0;
        java.util.Map<Long, Integer> stand = new java.util.HashMap<>();
        double first = Double.POSITIVE_INFINITY;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (double w = 0.0; w <= goal + 2.0; w += 1.0) {
            for (double into = 0.25; into <= 9.0; into += 0.5) {
                double[] q = VerdantGorges.wallAt(g, side * (g.arcEnd() + w), into);
                minX = Math.min(minX, (int) Math.floor(q[0]) - 2);
                maxX = Math.max(maxX, (int) Math.floor(q[0]) + 2);
                minZ = Math.min(minZ, (int) Math.floor(q[1]) - 2);
                maxZ = Math.max(maxZ, (int) Math.floor(q[1]) + 2);
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                IslandField.Column c = FIELD.column(x, z);
                VerdantGorges.Local l = c.gorgeLocal;
                if (c.gorgeZone != g || l == null || l.w < 0.0 || l.w > goal + 2.0 || l.side != side || l.din <= 0.0 || l.din > 9.0) {
                    continue;
                }
                int y = (int) Math.ceil(c.surface());
                if (scan.block(x, y - 1, z) == WaterScan.SOLID && scan.block(x, y, z) == WaterScan.AIR && scan.block(x, y + 1, z) == WaterScan.AIR) {
                    stand.put(KeyedSlots.key(x, z), y);
                    first = Math.min(first, l.w);
                }
            }
        }
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (java.util.Map.Entry<Long, Integer> e : stand.entrySet()) {
            int x = (int) (e.getKey() >> 32);
            int z = (int) (long) e.getKey();
            if (FIELD.column(x, z).gorgeLocal.w <= first + 3.0) {
                queue.add(new int[]{x, z});
                seen.add(e.getKey());
            }
        }
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            int y = stand.get(KeyedSlots.key(at[0], at[1]));
            if (FIELD.column(at[0], at[1]).gorgeLocal.w >= goal || y <= VerdantDescent.BANK_TOP + 1.0) {
                // Into the gully, or down on the foot river's bank where the arm reaches it first.
                return true;
            }
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                long key = KeyedSlots.key(at[0] + d[0], at[1] + d[1]);
                Integer next = stand.get(key);
                if (next != null && Math.abs(next - y) <= 1 && seen.add(key)) {
                    queue.add(new int[]{at[0] + d[0], at[1] + d[1]});
                }
            }
        }
        return false;
    }

    /**
     * The gully's open width at w, square to its winding course: columns across its middle inside the gorge (din > 0),
     * or −1 where the gorge ends.
     */
    private static int gullyWidth(VerdantGorges.Gorge g, double w) {
        double middle = g.centre(w);
        int n = 0;
        for (double a = middle - 40.0; a <= middle + 40.0; a += 1.0) {
            IslandField.Column c = FIELD.column((int) Math.floor(g.cx() - a * g.oz() + w * g.ox()), (int) Math.floor(g.cz() + a * g.ox() + w * g.oz()));
            if (c.gorgeZone == null) {
                return -1;
            }
            n += c.gorgeLocal.din > 0.0 ? 1 : 0;
        }
        return (int) Math.round(n / g.gullyLean(w));
    }

    /**
     * Decision 2026-10-15: a natural rock arch over about half the gullies: span 14–30, 3–4 thick at the crown, its
     * underside 8–14 over the water and rounded down to the walls, its deck 3 wide and solid from wall to wall, standing
     * on rock on both sides; open water and air under it.
     */
    @Test
    void halfTheGullieshaveARockArch() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        List<VerdantGorges.Gorge> all = gorges();
        int arches = 0;
        for (VerdantGorges.Gorge g : all) {
            VerdantGorges.Arch arch = GORGES.details(g).arch();
            if (arch == null) {
                continue;
            }
            arches++;
            String where = " (gorge " + g.index() + ", w " + arch.w() + ")";
            int clear = arch.crown() - arch.water();
            int thick = arch.deck() - arch.crown() + 1;
            assertTrue(clear >= 8 && clear <= 14, "clearance " + clear + where);
            assertTrue(thick == 3 || thick == 4, "thickness " + thick + where);
            int span = gullyWidth(g, arch.w());
            assertTrue(span >= 14 && span <= 31, "span " + span + where);
            // Across the middle row: the deck solid from wall to wall and into the rock on both sides; air under the
            // crown down to the water.
            double middle = g.centre(arch.w());
            int lo = Integer.MAX_VALUE;
            for (double a = middle - 40.0; a <= middle + 40.0; a += 0.5) {
                int x = (int) Math.floor(g.cx() - a * g.oz() + arch.w() * g.ox());
                int z = (int) Math.floor(g.cz() + a * g.ox() + arch.w() * g.oz());
                if (FIELD.column(x, z).gorgeLocal.din > 0.0) {
                    assertEquals(WaterScan.SOLID, scan.block(x, arch.deck(), z), "deck at " + x + " " + arch.deck() + " " + z + where);
                    lo = Math.min(lo, FIELD.column(x, z).solidLo);
                }
            }
            // Rounded: lowest at its ends on the walls (over a dry shore its foot leaves head room).
            assertTrue(arch.under(1.0) < arch.crown() - 2 && lo <= arch.deck(), "the underside comes down to the walls" + where);
            for (int side = -1; side <= 1; side += 2) {
                // The wall it stands on: rock at the deck's height and below, behind the ledge path's ramps.
                double[] q = VerdantGorges.wallAt(g, side * (g.arcEnd() + arch.w()), -5.0);
                for (int y = arch.under(1.0); y <= arch.deck(); y++) {
                    assertEquals(WaterScan.SOLID, scan.block((int) Math.floor(q[0]), y, (int) Math.floor(q[1])), "wall under the arch's end at "
                            + (int) Math.floor(q[0]) + " " + y + " " + (int) Math.floor(q[1]) + " din " + FIELD.column((int) Math.floor(q[0]), (int) Math.floor(q[1])).gorgeLocal.din
                            + " out " + FIELD.column((int) Math.floor(q[0]), (int) Math.floor(q[1])).gorgeOutside + where);
                }
            }
            int cx = (int) Math.floor(g.cx() - middle * g.oz() + arch.w() * g.ox());
            int cz = (int) Math.floor(g.cz() + middle * g.ox() + arch.w() * g.oz());
            assertEquals(WaterScan.WATER, scan.block(cx, arch.water() - 1, cz), "water under the arch" + where);
            for (int y = arch.water(); y < arch.crown(); y++) {
                assertEquals(WaterScan.AIR, scan.block(cx, y, cz), "open under the arch at " + y + where);
            }
            assertEquals(WaterScan.SOLID, scan.block(cx, arch.crown(), cz), "the crown" + where);
            // 3 wide along the gully: open over the water 3 blocks before and after the deck's middle.
            for (int dw = -3; dw <= 3; dw += 6) {
                int x = (int) Math.floor(g.cx() - middle * g.oz() + (arch.w() + dw) * g.ox());
                int z = (int) Math.floor(g.cz() + middle * g.ox() + (arch.w() + dw) * g.oz());
                assertEquals(WaterScan.AIR, scan.block(x, arch.deck(), z), "beside the deck" + where);
            }
        }
        System.out.printf("arches %d of %d gorges%n", arches, all.size());
        assertTrue(arches >= all.size() * 0.35 && arches <= all.size() * 0.65, "arches " + arches + " of " + all.size());
    }

    /**
     * Polish round 2026-10-15: the falls along an arc vary, in clusters and gaps (about 13 apart on average, but anywhere
     * from 5 to over 20) and 1–4 columns wide, most gorges with a main fall 3–4 wide in an alcove.
     */
    @Test
    void fallsVaryInWidthAndSpacing() {
        int[] widths = new int[6];
        List<Double> gaps = new ArrayList<>();
        int withMain = 0;
        List<VerdantGorges.Gorge> all = gorges();
        for (VerdantGorges.Gorge g : all) {
            List<VerdantGorges.GorgeFall> arc = new ArrayList<>();
            for (VerdantGorges.GorgeFall f : GORGES.details(g).falls()) {
                if (f.arc()) {
                    arc.add(f);
                }
            }
            boolean main = false;
            double previous = Double.NaN;
            int i = 0;
            while (i < arc.size()) {
                int j = i + 1;
                double sum = arc.get(i).t();
                while (j < arc.size() && besideEachOther(arc.get(j - 1), arc.get(j))) {
                    sum += arc.get(j).t();
                    j++;
                }
                int width = j - i;
                widths[Math.min(5, width)]++;
                main |= width >= 3;
                double middle = sum / width;
                if (!Double.isNaN(previous)) {
                    gaps.add(middle - previous);
                }
                previous = middle;
                i = j;
            }
            withMain += main ? 1 : 0;
        }
        double mean = gaps.stream().mapToDouble(d -> d).average().orElse(0.0);
        double sd = Math.sqrt(gaps.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0.0));
        java.util.Collections.sort(gaps);
        System.out.printf("fall widths 1–4: %d %d %d %d (wider %d), gorges with a main fall %d of %d; spacing mean %.1f, sd %.1f, %.1f–%.1f%n",
                widths[1], widths[2], widths[3], widths[4], widths[5], withMain, all.size(), mean, sd, gaps.get(0), gaps.get(gaps.size() - 1));
        for (int w = 1; w <= 4; w++) {
            assertTrue(widths[w] >= 20, "falls " + w + " wide: " + widths[w]);
        }
        assertEquals(0, widths[5], "no fall wider than 4");
        assertTrue(widths[1] > widths[3] && widths[1] > widths[4], "narrow falls the most common");
        assertTrue(withMain >= all.size() * 0.7, "gorges with a main fall " + withMain);
        assertTrue(mean >= 10.0 && mean <= 16.0, "spacing " + mean);
        assertTrue(sd >= 3.0 && gaps.get(0) <= 8.0 && gaps.get(gaps.size() - 1) >= 20.0, "clusters and gaps: sd " + sd);
    }

    /**
     * Polish round 2026-10-15: the amphitheatre's wall line is no ellipse: alcoves and rock noses move it in and out by
     * 6–12 blocks and more (its range over the arc), never turning more than 60° from the ellipse.
     */
    @Test
    void theWallLineHasAlcovesAndNoses() {
        List<Double> ranges = new ArrayList<>();
        double steepest = 0.0;
        for (VerdantGorges.Gorge g : gorges()) {
            double lo = Double.POSITIVE_INFINITY;
            double hi = Double.NEGATIVE_INFINITY;
            int alcoves = 0;
            int noses = 0;
            for (int i = 0; i < g.lobes().length; i += 3) {
                alcoves += g.lobes()[i + 1] > 0.0 ? 1 : 0;
                noses += g.lobes()[i + 1] < 0.0 ? 1 : 0;
            }
            for (double t = -g.arcEnd(); t <= g.arcEnd(); t += 1.0) {
                lo = Math.min(lo, g.wobble(t));
                hi = Math.max(hi, g.wobble(t));
                steepest = Math.max(steepest, g.wobbleLean(t));
            }
            assertTrue(alcoves >= 2 && noses >= 1, "alcoves " + alcoves + " and noses " + noses + " in gorge " + g.index());
            assertTrue(hi - lo >= 6.0, "the wall line moves in gorge " + g.index() + ": " + (hi - lo));
            assertTrue(lo >= -18.0 && hi <= 18.0, "within reach in gorge " + g.index() + ": " + lo + ".." + hi);
            ranges.add(hi - lo);
        }
        java.util.Collections.sort(ranges);
        double median = ranges.get(ranges.size() / 2);
        System.out.printf("wall line ranges %.1f–%.1f (median %.1f), steepest angle to the ellipse %.0f°%n", ranges.get(0),
                ranges.get(ranges.size() - 1), median, Math.toDegrees(Math.acos(1.0 / steepest)));
        assertTrue(median >= 12.0 && median <= 24.0, "median range " + median);
        assertTrue(steepest < 2.0, "the wall never turns more than 60° from the ellipse: " + steepest);
    }

    /**
     * Polish round 2026-10-15: the lips are travertine dams, no upright walls: along the gorge (rays out from the
     * amphitheatre across its width) the floor steps down a tread over several blocks; an upright drop of 5 or more
     * within one block (the old riser dropped 9 at once) is left mostly at the falls' chutes. From lake to lake: the last lip drops onto the foot river,
     * whose bank shapes it.
     */
    @Test
    void lipsAreTravertineDams() {
        final double riser = 5.0;
        int crossings = 0;
        int upright = 0;
        for (VerdantGorges.Gorge g : sample(2)) {
            for (double a = -g.gullyHalf() + 4.0; a <= g.gullyHalf() - 4.0; a += 2.0) {
                double previous = Double.NaN;
                int lastLevel = Integer.MIN_VALUE;
                for (double w = -g.depth() * 0.5; w <= g.gullyFrom() + 120.0; w += 1.0) {
                    double across = g.centre(Math.max(0.0, w)) + a;
                    int x = (int) Math.floor(g.cx() - across * g.oz() + w * g.ox());
                    int z = (int) Math.floor(g.cz() + across * g.ox() + w * g.oz());
                    IslandField.Column c = FIELD.column(x, z);
                    if (c.gorgeZone != g || c.gorgeLocal.din < 3.0) {
                        break;
                    }
                    // The water's surface (the first air above it) or the ground's: the sea counts as Y 63.
                    int level = c.lakeTop() != Integer.MIN_VALUE ? c.lakeTop() : c.surface() < 63.0 ? 63 : Integer.MIN_VALUE;
                    double here = Math.max(Math.ceil(c.surface()), level);
                    if (level == 63) {
                        // The last lip drops onto the foot river, whose bank (its envelope) shapes it.
                        break;
                    }
                    if (!Double.isNaN(previous) && previous - here >= riser) {
                        upright++;
                    }
                    if (level != Integer.MIN_VALUE && lastLevel != Integer.MIN_VALUE && level < lastLevel) {
                        crossings++;
                    }
                    if (level != Integer.MIN_VALUE) {
                        lastLevel = level;
                    }
                    previous = here;
                }
            }
        }
        System.out.printf("lip crossings %d, upright drops %d%n", crossings, upright);
        assertTrue(crossings > 40, "crossings " + crossings);
        assertTrue(upright <= crossings * 0.3, "upright drops " + upright + " of " + crossings);
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
