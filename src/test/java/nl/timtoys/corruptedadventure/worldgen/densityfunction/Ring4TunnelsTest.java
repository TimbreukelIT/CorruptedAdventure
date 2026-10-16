package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tunnel net of ring 4 (package 73) on the test seed: inside its band, away from the zones; what fills it never
 * stands beside or over the wrong neighbour (offline, per block: flat water levels, sealed domes, the dry net, gas and
 * closed pools); entrances; determinism.
 */
class Ring4TunnelsTest {
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;
    private static IslandField field;
    private static Ring4Tunnels tunnels;
    /** Every element that stays in the cells of a wedge across the ring (both sides of the x axis). */
    private static final List<Ring4Tunnels.Element> ELEMENTS = new ArrayList<>();
    private static final int I0 = 5800 / Ring4Tunnels.CELL;
    private static final int I1 = 8200 / Ring4Tunnels.CELL;
    private static final int J0 = -1500 / Ring4Tunnels.CELL;
    private static final int J1 = 1500 / Ring4Tunnels.CELL;

    @BeforeAll
    static void decide() {
        field = IslandFieldTest.field();
        tunnels = new Ring4Tunnels(field);
        long start = System.nanoTime();
        for (int i = I0; i <= I1; i++) {
            for (int j = J0; j <= J1; j++) {
                ELEMENTS.addAll(tunnels.finalCell(i, j));
            }
        }
        System.out.printf("decided %d cells in %.0f ms%n", (I1 - I0 + 1) * (J1 - J0 + 1), (System.nanoTime() - start) / 1e6);
    }

    private static Map<String, Integer> census() {
        Map<String, Integer> count = new LinkedHashMap<>();
        for (Ring4Tunnels.Element e : ELEMENTS) {
            String kind;
            if (e instanceof Ring4Tunnels.Seg seg) {
                boolean entrance = seg.nodeB == Long.MIN_VALUE && seg.nodeA != Long.MIN_VALUE && !seg.owner.mixed;
                kind = (seg.flooded() ? "flooded" : "dry") + (entrance ? " entrance" : seg.owner.mixed ? " half (plug)" : " tunnel");
            } else if (e instanceof Ring4Tunnels.Room room) {
                kind = room.owner != null ? "crest dome"
                        : room.flooded() ? (room.sealed ? "dome hall" : "flooded hall")
                        : room.kind == 3 ? "gas hall" : room.kind == 4 ? "pool hall" : "dry hall";
            } else {
                kind = "sink";
            }
            count.merge(kind, 1, Integer::sum);
        }
        return count;
    }

    @Test
    void theNetHasAllItsParts() {
        Map<String, Integer> count = census();
        System.out.println("tunnel net (wedge x 5800..8200, z ±1500): " + count);
        for (String kind : new String[]{"flooded tunnel", "dry tunnel", "crest dome", "dome hall", "gas hall", "pool hall",
                "flooded entrance", "dry entrance"}) {
            assertTrue(count.getOrDefault(kind, 0) > 0, "no " + kind + " in " + count);
        }
    }

    /** Q6 = A: open space and its closed caves at least 32 blocks from the zones 3→4 and 4→5. */
    @Test
    void theNetKeepsAwayFromTheZones() {
        double inner = VerdantDescent.zoneStart(LAYOUT) + VerdantDescent.width(LAYOUT);
        double outer = Ring4Sundered.zoneStart(LAYOUT);
        for (Ring4Tunnels.Element e : ELEMENTS) {
            for (double[] s : e.spheres()) {
                double d = LAYOUT.effectiveDistance(s[0], s[2]);
                assertTrue(d - s[3] >= inner + Ring4Tunnels.ZONE_GAP - 1.0 && d + s[3] <= outer - Ring4Tunnels.ZONE_GAP + 1.0,
                        "open space at " + s[0] + ", " + s[2] + " (effective " + d + ", reach " + s[3] + ")");
            }
        }
        // Per column along the zones' edges, both sides of the axis: nothing.
        for (double angle = -0.2; angle <= 0.2; angle += 0.0005) {
            for (double r : new double[]{inner - 40.0, inner, inner + 20.0, outer - 20.0, outer, outer + 40.0}) {
                int x = (int) Math.floor(r * Math.cos(angle));
                int z = (int) Math.floor(r * Math.sin(angle));
                double e = LAYOUT.effectiveDistance(x + 0.5, z + 0.5);
                if (e < inner + Ring4Tunnels.ZONE_GAP - 8.0 || e > outer - Ring4Tunnels.ZONE_GAP + 8.0) {
                    Ring4Tunnels.Span span = field.tunnelSpan(x, z);
                    assertTrue(span == null || span.openLo() > span.openHi(), "net at " + x + ", " + z);
                }
            }
        }
    }

    /**
     * Offline per block around every kind of element: water never beside air, gas or a pool, never over air; air and
     * gas under the sea level only beside rock (caves closed there, under the ground) or their own kind; a pool only
     * beside and over rock or itself.
     */
    @Test
    void whatFillsTheNetNeverMeetsTheWrongNeighbour() {
        List<int[]> boxes = new ArrayList<>();
        Map<String, Integer> boxed = new LinkedHashMap<>();
        for (Ring4Tunnels.Element e : ELEMENTS) {
            String kind = e instanceof Ring4Tunnels.Room room ? "room" + room.kind + room.sealed + (room.owner != null)
                    : e instanceof Ring4Tunnels.Sink ? "sink" : "seg" + e.flooded() + (((Ring4Tunnels.Seg) e).owner.mixed);
            if (boxed.merge(kind, 1, Integer::sum) <= 6) {
                double[] s = e.spheres()[e.spheres().length / 2];
                boxes.add(new int[]{(int) s[0] - 40, (int) s[2] - 40, (int) s[0] + 40, (int) s[2] + 40});
            }
        }
        Probe probe = new Probe();
        int open = 0;
        List<String> bad = new ArrayList<>();
        for (int[] box : boxes) {
            for (int x = box[0]; x <= box[2]; x++) {
                for (int z = box[1]; z <= box[3]; z++) {
                    Ring4Tunnels.Span span = field.tunnelSpan(x, z);
                    if (span == null) {
                        continue;
                    }
                    for (int k = 0; k < span.n; k++) {
                        for (int y = span.lo[k]; y <= span.hi[k]; y++) {
                            Ring4Tunnels.Fill fill = span.fillAt(y);
                            open++;
                            check(probe, fill, x, y, z, bad);
                        }
                    }
                }
            }
        }
        System.out.printf("checked %d open blocks in %d boxes, %d bad%n", open, boxes.size(), bad.size());
        assertTrue(bad.isEmpty(), bad.size() + " bad, e.g. " + bad.subList(0, Math.min(12, bad.size())));
    }

    private static void check(Probe probe, Ring4Tunnels.Fill fill, int x, int y, int z, List<String> bad) {
        int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : sides) {
            Ring4Tunnels.Fill other = probe.fill(x + d[0], y, z + d[1]);
            if (!besides(fill, other, y) || other == Ring4Tunnels.Fill.NONE && needsRock(fill, y)
                    && !probe.rock(x + d[0], y, z + d[1])) {
                bad.add(fill + "|" + other + " at " + x + " " + y + " " + z + " → " + d[0] + "," + d[1]);
            }
        }
        Ring4Tunnels.Fill below = probe.fill(x, y - 1, z);
        if (!over(fill, below) || below == Ring4Tunnels.Fill.NONE && fill == Ring4Tunnels.Fill.POOL && !probe.rock(x, y - 1, z)) {
            bad.add(fill + " over " + below + " at " + x + " " + y + " " + z);
        }
        Ring4Tunnels.Fill above = probe.fill(x, y + 1, z);
        boolean airLike = fill != Ring4Tunnels.Fill.WATER;
        if (above == Ring4Tunnels.Fill.NONE && airLike && y + 1 <= Ring4Tunnels.SEA_TOP && !probe.rock(x, y + 1, z)) {
            bad.add("open " + fill + " under the sea level at " + x + " " + y + " " + z + " under no rock");
        }
    }

    private static boolean isAir(Ring4Tunnels.Fill f) {
        return f == Ring4Tunnels.Fill.DOME_AIR || f == Ring4Tunnels.Fill.DRY_AIR || f == Ring4Tunnels.Fill.GAS;
    }

    /** Whether two fills may stand side by side (NONE: decided by {@link #needsRock}). */
    private static boolean besides(Ring4Tunnels.Fill a, Ring4Tunnels.Fill b, int y) {
        if (b == Ring4Tunnels.Fill.NONE || a == b) {
            return true;
        }
        if (a == Ring4Tunnels.Fill.WATER || b == Ring4Tunnels.Fill.WATER || a == Ring4Tunnels.Fill.POOL || b == Ring4Tunnels.Fill.POOL) {
            return false;
        }
        return isAir(a) && isAir(b);
    }

    /** Whether a fill beside open space outside the net needs rock there (the flood rule fills it with water below Y 63). */
    private static boolean needsRock(Ring4Tunnels.Fill fill, int y) {
        return fill == Ring4Tunnels.Fill.POOL || isAir(fill) && y <= Ring4Tunnels.SEA_TOP;
    }

    /** Whether {@code upper} may stand over {@code lower}. */
    private static boolean over(Ring4Tunnels.Fill upper, Ring4Tunnels.Fill lower) {
        if (lower == Ring4Tunnels.Fill.NONE || upper == lower) {
            return true;
        }
        boolean fluid = upper == Ring4Tunnels.Fill.WATER || upper == Ring4Tunnels.Fill.POOL;
        return !fluid;
    }

    /** Per-block reads of the field for the check. */
    private static final class Probe {
        Ring4Tunnels.Fill fill(int x, int y, int z) {
            Ring4Tunnels.Span span = field.tunnelSpan(x, z);
            return span == null ? Ring4Tunnels.Fill.NONE : span.fillAt(y);
        }

        /**
         * Whether a block outside the net is rock for certain: under the terrain's interpolated surface (and a peat
         * sinkhole's floor), with vanilla's caves closed at it and at the 8 corners of its cell of the stack's grid.
         */
        boolean rock(int x, int y, int z) {
            IslandField.Column c = field.column(x, z);
            // A sinkhole's bowl is cut per block from the ceiling of its floor; the rest is the interpolated stack.
            if (c.sinkholeDisc && !Double.isNaN(c.sinkholeFloor) && y >= Math.ceil(c.sinkholeFloor)) {
                return false;
            }
            double grid = field.gridSurface(x, z);
            if (y >= Math.min(c.surface, grid) - 1.0) {
                return false;
            }
            if (field.caveLift(x, y, z) < 2.0) {
                return false;
            }
            int x0 = x & ~3;
            int z0 = z & ~3;
            int y0 = Math.floorDiv(y, 8) * 8;
            for (int dx = 0; dx <= 4; dx += 4) {
                for (int dy = 0; dy <= 8; dy += 8) {
                    for (int dz = 0; dz <= 4; dz += 4) {
                        if (field.caveLift(x0 + dx, y0 + dy, z0 + dz) < 2.0) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    /** Entrances open: a flooded one's end into its hole's water, a dry one's floor at its bowl's floor. */
    @Test
    void entrancesOpenIntoTheirHolesAndBowls() {
        int flooded = 0;
        int dry = 0;
        for (Ring4Tunnels.Element e : ELEMENTS) {
            if (!(e instanceof Ring4Tunnels.Seg seg) || seg.nodeB != Long.MIN_VALUE || seg.owner.mixed) {
                continue;
            }
            int last = seg.to;
            int x = (int) Math.floor(seg.line.x[last]);
            int z = (int) Math.floor(seg.line.z[last]);
            double top = seg.profile.at(seg.line.s[last]) + seg.hh;
            IslandField.Column c = field.column(x, z);
            if (seg.flooded()) {
                flooded++;
                assertTrue(top > c.surface() + 1.0, "flooded entrance at " + x + ", " + z + ": top " + top + ", floor " + c.surface());
                assertTrue(c.surface() < Ring4Tunnels.SEA_TOP - 4, "in the sea: " + c.surface());
            } else {
                dry++;
                assertTrue(!Double.isNaN(c.sinkholeFloor()), "dry entrance at " + x + ", " + z + " ends in a bowl");
                double floor = seg.profile.at(seg.line.s[last]) - seg.hh;
                assertTrue(Math.abs(floor - c.sinkholeFloor()) < 2.5, "ramp floor " + floor + " vs bowl " + c.sinkholeFloor());
            }
        }
        System.out.printf("entrances: flooded %d, dry %d%n", flooded, dry);
        assertTrue(flooded > 0 && dry > 0);
    }

    /**
     * Never under a lake above the sea (Q6 = A), the world trees' hole lakes (74) included: no open block of the net in
     * a lake's closed band, along every tunnel and hall; the world trees' columns are checked too.
     */
    @Test
    void theNetStaysOutOfTheBandsOfLakesAboveTheSea() {
        List<int[]> columns = new ArrayList<>();
        for (Ring4Tunnels.Element e : ELEMENTS) {
            for (double[] s : e.spheres()) {
                for (int k = -2; k <= 2; k++) {
                    columns.add(new int[]{(int) Math.floor(s[0]) + 2 * k, (int) Math.floor(s[2]) - k});
                }
            }
        }
        for (IslandField.WorldTree tree : field.worldTrees(5800, -1500, 8200, 1500)) {
            for (int dx = -60; dx <= 60; dx += 3) {
                for (int dz = -60; dz <= 60; dz += 3) {
                    columns.add(new int[]{tree.x() + dx, tree.z() + dz});
                }
            }
        }
        int guarded = 0;
        for (int[] p : columns) {
            IslandField.Column c = field.column(p[0], p[1]);
            int from = c.caveLiftFrom();
            int to = c.caveLiftTo();
            if (from > to || c.lakeTop() == Integer.MIN_VALUE && from == Integer.MAX_VALUE) {
                continue;
            }
            guarded++;
            Ring4Tunnels.Span span = field.tunnelSpan(p[0], p[1]);
            for (int y = from; span != null && y <= to; y++) {
                assertTrue(!span.opens(y), "the net opens " + p[0] + " " + y + " " + p[1] + " in a lake's band " + from + ".." + to);
            }
        }
        System.out.printf("lake bands checked in %d of %d columns%n", guarded, columns.size());
    }

    /** Tunnels are 5–12 wide and 5–7 high. */
    @Test
    void tunnelsAreFiveToTwelveWide() {
        for (Ring4Tunnels.Element e : ELEMENTS) {
            if (e instanceof Ring4Tunnels.Seg seg) {
                for (int p = seg.from; p <= seg.to; p++) {
                    assertTrue(seg.w[p] >= 2.5 && seg.w[p] <= 6.0, "half width " + seg.w[p]);
                }
                assertTrue(seg.hh >= 2.5 && seg.hh <= 3.5, "half height " + seg.hh);
            }
        }
    }

    /** Determinism: a field built apart, asked in the opposite order, gives the same spans. */
    @Test
    void theNetIsPureWhateverTheOrder() {
        IslandField other = IslandFieldTest.field();
        List<int[]> points = new ArrayList<>();
        for (Ring4Tunnels.Element e : ELEMENTS.subList(0, Math.min(60, ELEMENTS.size()))) {
            double[] s = e.spheres()[0];
            for (int k = -3; k <= 3; k++) {
                points.add(new int[]{(int) s[0] + 2 * k, (int) s[2] - k});
            }
        }
        List<String> forward = new ArrayList<>();
        for (int[] p : points) {
            forward.add(describe(field.tunnelSpan(p[0], p[1])));
        }
        for (int k = points.size() - 1; k >= 0; k--) {
            int[] p = points.get(k);
            assertEquals(forward.get(k), describe(other.tunnelSpan(p[0], p[1])), p[0] + ", " + p[1]);
        }
    }

    private static String describe(Ring4Tunnels.Span span) {
        if (span == null) {
            return "-";
        }
        StringBuilder b = new StringBuilder();
        for (int y = -64; y < 80; y++) {
            b.append(span.fillAt(y).code()).append(span.lifted(y) ? 'L' : '.');
        }
        return b.toString();
    }

    /** No net and no work outside ring 4. */
    @Test
    void nothingOutsideRing4() {
        assertTrue(tunnels.elementsOfChunk(3000 >> 4, 0).isEmpty());
        assertTrue(tunnels.elementsOfChunk(9000 >> 4, 0).isEmpty());
        assertNull(field.tunnelSpan(6000, 0));
        assertNotNull(tunnels);
    }
}
