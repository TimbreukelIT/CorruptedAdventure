package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The basins of {@code verdant_falls} (W-terrein 2c) on the test seed, block by block as the stack and the aquifer make
 * them ({@link WaterScan}): every lake, pool and gully walled in, every spring with three walls, every fall landing in
 * water, one water level per tread, the cap per chunk, the foot river round the ring.
 */
class VerdantBasinsTest {
    private static final IslandField FIELD = IslandFieldTest.field();
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;
    private static final VerdantBasins BASINS = FIELD.basins();

    /** The box (block coordinates, inclusive) round stream {@code k}: ±{@code along} of its position, u 56–river. */
    static int[] box(VerdantBasins.Stream stream, double along) {
        double r = VerdantDescent.midRadius(LAYOUT);
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (double ds = -along; ds <= along; ds += along / 4.0) {
            for (double u = 56.0; u <= 236.0; u += 18.0) {
                double[] p = VerdantDescent.onRay(LAYOUT, (stream.at() + ds) / r, VerdantDescent.zoneStart(LAYOUT) + u);
                minX = Math.min(minX, (int) Math.floor(p[0]));
                minZ = Math.min(minZ, (int) Math.floor(p[1]));
                maxX = Math.max(maxX, (int) Math.floor(p[0]));
                maxZ = Math.max(maxZ, (int) Math.floor(p[1]));
            }
        }
        return new int[]{minX, minZ, maxX, maxZ};
    }

    /** Streams spread round the ring, each with a lake on some tread. */
    private static List<VerdantBasins.Stream> sampleStreams(int wanted) {
        List<VerdantBasins.Stream> found = new ArrayList<>();
        int count = BASINS.streamCount();
        for (int k = 3; k < count && found.size() < wanted; k += count / (2 * wanted)) {
            VerdantBasins.Stream stream = BASINS.stream(k);
            if (stream == null) {
                continue;
            }
            boolean lake = false;
            for (int b = 1; b <= stream.top(); b++) {
                lake |= BASINS.segment(stream, b).lake();
            }
            if (lake) {
                found.add(stream);
            }
        }
        return found;
    }

    /**
     * The waterscan's rule offline (contract §4): around streams all round the ring, no water of a lake, pool, gully or
     * the foot river stands against air (beside or below), only the recorded falls do.
     */
    @Test
    void waterNeverStandsAgainstAir() {
        int water = 0;
        for (VerdantBasins.Stream stream : sampleStreams(6)) {
            int[] box = box(stream, 30.0);
            WaterScan scan = new WaterScan(FIELD, LAYOUT);
            List<String> over = scan.scan(box[0], box[1], box[2], box[3]);
            assertTrue(over.isEmpty(), "water against air round stream " + stream.index() + ": " + over.subList(0, Math.min(10, over.size())));
            water += scan.water;
        }
        System.out.println("water blocks scanned: " + water);
        assertTrue(water > 20_000, "water " + water);
    }

    /** Every lake, pool and gully of a tread holds still water at that tread's one level, up to its top block. */
    @Test
    void theWaterOfATreadIsFlat() {
        int columns = 0;
        Map<Integer, Integer> bodies = new HashMap<>();
        for (VerdantBasins.Stream stream : sampleStreams(6)) {
            int[] box = box(stream, 30.0);
            WaterScan scan = new WaterScan(FIELD, LAYOUT);
            for (int x = box[0]; x <= box[2]; x++) {
                for (int z = box[1]; z <= box[3]; z++) {
                    IslandField.Column c = FIELD.column(x, z);
                    if (!c.verdant || c.lakeTop() == Integer.MIN_VALUE) {
                        continue;
                    }
                    int body = c.body;
                    int lakeTop = c.lakeTop();
                    double surface = c.surface();
                    assertEquals(VerdantDescent.waterTop(body), lakeTop, "level at " + x + " " + z);
                    if (Math.ceil(surface) < lakeTop) {
                        assertEquals(WaterScan.WATER, scan.block(x, lakeTop - 1, z), "top water block at " + x + " " + z);
                        assertTrue(scan.block(x, lakeTop, z) != WaterScan.WATER, "nothing above the level at " + x + " " + z);
                        columns++;
                        bodies.merge(body, 1, Integer::sum);
                    }
                }
            }
        }
        System.out.println("water columns per tread: " + bodies);
        assertTrue(columns > 1000, "columns " + columns);
        assertTrue(bodies.size() >= 3, "treads with water " + bodies);
    }

    /** All kept falls of the descent round the ring (the test's sample of streams). */
    private static List<VerdantBasins.Fall> keptFalls(int step) {
        List<VerdantBasins.Fall> falls = new ArrayList<>();
        int count = BASINS.streamCount();
        for (int k = 0; k < count; k += step) {
            VerdantBasins.Stream stream = BASINS.stream(k);
            if (stream == null) {
                continue;
            }
            for (int b = 1; b <= stream.top(); b++) {
                for (boolean spur : new boolean[]{false, true}) {
                    VerdantBasins.Fall fall = BASINS.fall(stream, b, spur);
                    if (fall != null && FIELD.fallsOfChunk(fall.fall().fallX() >> 4, fall.fall().fallZ() >> 4).contains(fall.fall())) {
                        falls.add(fall);
                    }
                }
            }
        }
        return falls;
    }

    /**
     * Every spring: open, rock over it, rock on its two sides at its level, the brook (or rock) behind it, its fall in
     * front; every fall: open from the spring down to the water it lands in.
     */
    @Test
    void everySpringHasThreeWallsAndEveryFallLandsInWater() {
        List<VerdantBasins.Fall> falls = keptFalls(7);
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int lakes = 0;
        for (VerdantBasins.Fall f : falls) {
            IslandField.Fall fall = f.fall();
            int sx = fall.springX();
            int sz = fall.springZ();
            int y = fall.springY();
            String where = " at " + sx + " " + y + " " + sz;
            assertEquals(WaterScan.WATER, scan.block(sx, y, sz), "spring" + where);
            assertEquals(WaterScan.SOLID, scan.block(sx, y + 1, sz), "rock over the spring" + where);
            assertEquals(WaterScan.SOLID, scan.block(sx, y - 1, sz), "rock under the spring" + where);
            assertEquals(WaterScan.SOLID, scan.block(sx - f.stepZ(), y, sz + f.stepX()), "side" + where);
            assertEquals(WaterScan.SOLID, scan.block(sx + f.stepZ(), y, sz - f.stepX()), "side" + where);
            int behind = scan.block(sx - f.stepX(), y, sz - f.stepZ());
            assertTrue(behind == WaterScan.WATER || behind == WaterScan.SOLID, "behind the spring" + where);
            assertEquals(sx + f.stepX(), fall.fallX());
            assertEquals(sz + f.stepZ(), fall.fallZ());
            for (int fy = fall.bottom(); fy <= y; fy++) {
                assertEquals(WaterScan.FALL, scan.block(fall.fallX(), fy, fall.fallZ()), "fall" + where + " at " + fy);
            }
            assertEquals(WaterScan.WATER, scan.block(fall.fallX(), fall.bottom() - 1, fall.fallZ()), "landing" + where);
            assertTrue(y - fall.bottom() >= 5, "fall height" + where);
            lakes += fall.bottom() > 63 ? 1 : 0;
        }
        System.out.printf("kept falls checked %d, into a pool above the sea %d%n", falls.size(), lakes);
        assertTrue(falls.size() > 150, "falls " + falls.size());
        assertTrue(lakes > falls.size() / 2, "falls into pools " + lakes);
    }

    /** Most streams fall from every tread they cross: the falls are not lost to the rules (or the cap). */
    @Test
    void mostSegmentsGetTheirFall() {
        int asked = 0;
        int placed = 0;
        int kept = 0;
        for (int k = 0; k < BASINS.streamCount(); k += 3) {
            VerdantBasins.Stream stream = BASINS.stream(k);
            if (stream == null) {
                continue;
            }
            for (int b = 1; b <= stream.top(); b++) {
                asked++;
                VerdantBasins.Fall fall = BASINS.fall(stream, b, false);
                if (fall != null) {
                    placed++;
                    kept += FIELD.fallsOfChunk(fall.fall().fallX() >> 4, fall.fall().fallZ() >> 4).contains(fall.fall()) ? 1 : 0;
                }
            }
        }
        System.out.printf("segments %d, falls placed %d, kept %d%n", asked, placed, kept);
        assertTrue(placed >= asked * 0.9, "placed " + placed + " of " + asked);
        assertTrue(kept >= placed * 0.97, "kept " + kept + " of " + placed);
    }

    /**
     * Q4 = A: at most 6 falls of the descent per chunk (their own cap, besides the islands' 3), the first by x, z and
     * the spring's height, whatever order the chunks are asked in.
     */
    @Test
    void theFallsOfAChunkAreCappedInOrder() {
        Map<Long, List<IslandField.Fall>> byChunk = new HashMap<>();
        int most = 0;
        for (VerdantBasins.Stream stream : sampleStreams(12)) {
            int[] box = box(stream, 40.0);
            for (int cx = box[0] >> 4; cx <= box[2] >> 4; cx++) {
                for (int cz = box[1] >> 4; cz <= box[3] >> 4; cz++) {
                    List<IslandField.Fall> falls = FIELD.fallsOfChunk(cx, cz);
                    byChunk.put(KeyedSlots.key(cx, cz), falls);
                    int verdant = 0;
                    for (IslandField.Fall fall : falls) {
                        double u = LAYOUT.effectiveDistance(fall.fallX(), fall.fallZ()) - VerdantDescent.zoneStart(LAYOUT);
                        verdant += u >= 0.0 && u <= VerdantDescent.width(LAYOUT) ? 1 : 0;
                    }
                    assertTrue(verdant <= IslandField.VERDANT_FALLS_PER_CHUNK, "chunk " + cx + " " + cz + ": " + verdant);
                    most = Math.max(most, verdant);
                }
            }
        }
        // Asked again on a field built apart, the chunks in the other order: the same lists.
        IslandField other = IslandFieldTest.field();
        List<Long> keys = new ArrayList<>(byChunk.keySet());
        for (int i = keys.size() - 1; i >= 0; i--) {
            long key = keys.get(i);
            assertEquals(byChunk.get(key), other.fallsOfChunk((int) (key >> 32), (int) key), "chunk " + key);
        }
        System.out.println("most falls of the descent in a chunk: " + most);
        assertTrue(most >= 2, "most " + most);
    }

    /** Part 2: the kept falls with a niche, every k-th stream round the ring. */
    private static List<Object[]> niches(int step) {
        List<Object[]> found = new ArrayList<>();
        for (int k = 0; k < BASINS.streamCount(); k += step) {
            VerdantBasins.Stream stream = BASINS.stream(k);
            if (stream == null) {
                continue;
            }
            for (int b = 1; b <= stream.top(); b++) {
                for (boolean spur : new boolean[]{false, true}) {
                    VerdantBasins.Fall fall = BASINS.fall(stream, b, spur);
                    if (fall == null || !FIELD.fallsOfChunk(fall.fall().fallX() >> 4, fall.fall().fallZ() >> 4).contains(fall.fall())) {
                        continue;
                    }
                    VerdantBasins.Niche niche = BASINS.niche(stream, b, spur, fall);
                    if (niche != null) {
                        found.add(new Object[]{fall, niche, stream});
                    }
                }
            }
        }
        return found;
    }

    /**
     * Part 2 (plan §2.4): about one in four falls has a niche behind it at the water's height + 1, 3 wide and 3 high,
     * its floor rock up to the water's top (so the water never stands beside air), the fall in front of its mouth.
     */
    @Test
    void nichesOpenBehindTheFallsAboveTheWater() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        List<Object[]> niches = niches(1);
        int falls = keptFalls(1).size();
        for (Object[] n : niches) {
            VerdantBasins.Fall f = (VerdantBasins.Fall) n[0];
            VerdantBasins.Niche niche = (VerdantBasins.Niche) n[1];
            IslandField.Fall fall = f.fall();
            assertEquals(fall.bottom(), niche.bottom(), "the niche's floor at the water the fall lands in");
            for (int k = 0; k < niche.depth(); k++) {
                for (int j = -1; j <= 1; j++) {
                    int x = fall.springX() - k * f.stepX() - j * f.stepZ();
                    int z = fall.springZ() - k * f.stepZ() + j * f.stepX();
                    String where = " at " + x + " " + z + " (spring " + fall.springX() + " " + fall.springY() + " " + fall.springZ() + ")";
                    assertEquals(WaterScan.SOLID, scan.block(x, niche.bottom() - 1, z), "niche floor" + where);
                    for (int y = niche.bottom(); y < niche.bottom() + 3; y++) {
                        assertEquals(WaterScan.AIR, scan.block(x, y, z), "niche" + where + " y " + y);
                    }
                    assertEquals(WaterScan.SOLID, scan.block(x, niche.bottom() + 3, z), "niche roof" + where);
                }
            }
            // The fall in front of the mouth, down to the water.
            assertEquals(WaterScan.FALL, scan.block(fall.fallX(), niche.bottom(), fall.fallZ()), "the fall before the niche");
        }
        System.out.printf("kept falls %d, niches %d (%.0f %%)%n", falls, niches.size(), 100.0 * niches.size() / falls);
        assertTrue(niches.size() > falls * 0.15 && niches.size() < falls * 0.35, "niches " + niches.size() + " of " + falls);
    }

    /**
     * One in three niches has a short tunnel (3 x 3, rising 1 in 2) up to the tread above, its exit at least 6 from the
     * tread's edge; no water within two blocks of any of its open blocks.
     */
    @Test
    void tunnelsRiseToTheTreadAndKeepTwoBlocksFromWater() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int tunnels = 0;
        int niches = 0;
        for (Object[] n : niches(1)) {
            niches++;
            VerdantBasins.Fall f = (VerdantBasins.Fall) n[0];
            VerdantBasins.Niche niche = (VerdantBasins.Niche) n[1];
            if (niche.side() == 0) {
                continue;
            }
            tunnels++;
            IslandField.Fall fall = f.fall();
            int lastFloor = niche.bottom();
            int back = niche.depth() - 1;
            for (int run = 0; run <= niche.length(); run++) {
                int floor = niche.bottom() + run / 2;
                assertTrue(floor - lastFloor <= 1, "rises 1 in 2");
                lastFloor = floor;
                back = VerdantBasins.Niche.tunnelRow(niche.depth(), run);
                for (int k = back - 1; k <= back + 1; k++) {
                    int j = niche.side() * run;
                    int x = fall.springX() - k * f.stepX() - j * f.stepZ();
                    int z = fall.springZ() - k * f.stepZ() + j * f.stepX();
                    for (int y = floor; y < floor + 3; y++) {
                        String where = " at " + x + " " + y + " " + z;
                        assertEquals(WaterScan.AIR, scan.block(x, y, z), "tunnel" + where);
                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                for (int dy = -2; dy <= 2; dy++) {
                                    assertTrue(scan.block(x + dx, y + dy, z + dz) != WaterScan.WATER, "water near the tunnel" + where);
                                }
                            }
                        }
                    }
                }
            }
            // The exit: open to the sky, on the tread at least 6 from its edge.
            int j = niche.side() * niche.length();
            int ex = fall.springX() - back * f.stepX() - j * f.stepZ();
            int ez = fall.springZ() - back * f.stepZ() + j * f.stepX();
            assertTrue(FIELD.column(ex, ez).surface() <= lastFloor + 1.0, "the tunnel opens at its end");
            int b = (int) Math.round((fall.springY() + 2 - VerdantDescent.BANK_TOP) / VerdantDescent.STEP);
            double ramp = FIELD.descent().rampAt(ex, ez);
            double gx = FIELD.descent().rampAt(ex + 1, ez) - FIELD.descent().rampAt(ex - 1, ez);
            double gz = FIELD.descent().rampAt(ex, ez + 1) - FIELD.descent().rampAt(ex, ez - 1);
            assertTrue((ramp - VerdantDescent.level(b)) / (0.5 * Math.hypot(gx, gz)) >= 6.0, "exit 6 from the edge at " + ex + " " + ez);
        }
        System.out.printf("niches %d, tunnels %d%n", niches, tunnels);
        assertTrue(tunnels > niches / 5 && tunnels < niches / 2, "tunnels " + tunnels + " of " + niches);
    }

    /** One in five brooks has a rock bridge over it: span 4–8, 2–3 thick, air between the water and its underside. */
    @Test
    void rockBridgesSpanBrooks() {
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int brooks = 0;
        int bridges = 0;
        double r = VerdantDescent.midRadius(LAYOUT);
        for (int k = 0; k < BASINS.streamCount(); k++) {
            VerdantBasins.Stream stream = BASINS.stream(k);
            if (stream == null) {
                continue;
            }
            for (int b = 1; b <= stream.top(); b++) {
                brooks++;
                VerdantBasins.Bridge bridge = BASINS.bridge(stream, b);
                if (bridge == null) {
                    continue;
                }
                bridges++;
                assertTrue(bridge.span() >= 4.0 && bridge.span() <= 8.0 && bridge.thickness() >= 2 && bridge.thickness() <= 3, "size " + bridge);
                if (bridges % 4 != 0) {
                    continue;
                }
                // Over the brook's middle: water, then air, then the arch.
                double[] c = BASINS.rampPoint(bridge.centre() / r, bridge.ramp());
                assertTrue(c != null, "bridge " + bridge);
                int x = (int) Math.floor(c[0]);
                int z = (int) Math.floor(c[1]);
                int water = VerdantDescent.waterTop(b);
                String where = " at " + x + " " + z;
                assertEquals(WaterScan.WATER, scan.block(x, water - 1, z), "the brook under the bridge" + where);
                assertEquals(WaterScan.AIR, scan.block(x, water, z), "air over the water" + where);
                int y = water;
                while (y < water + 8 && scan.block(x, y, z) == WaterScan.AIR) {
                    y++;
                }
                assertEquals(WaterScan.SOLID, scan.block(x, y, z), "the arch" + where);
                assertTrue(y - water >= 1 && y <= VerdantDescent.level(b) + 2, "under the arch " + (y - water) + where);
            }
        }
        System.out.printf("brooks %d, bridges %d%n", brooks, bridges);
        assertTrue(bridges > brooks * 0.12 && bridges < brooks * 0.3, "bridges " + bridges + " of " + brooks);
    }

    /** No water against air round niches, tunnels and bridges. */
    @Test
    void waterNeverStandsAgainstAirRoundNichesAndBridges() {
        int water = 0;
        int boxes = 0;
        for (Object[] n : niches(9)) {
            int[] box = box((VerdantBasins.Stream) n[2], 30.0);
            WaterScan scan = new WaterScan(FIELD, LAYOUT);
            List<String> over = scan.scan(box[0], box[1], box[2], box[3]);
            assertTrue(over.isEmpty(), "water against air: " + over.subList(0, Math.min(10, over.size())));
            water += scan.water;
            if (++boxes >= 8) {
                break;
            }
        }
        System.out.println("water blocks scanned round niches: " + water);
        assertTrue(water > 10_000, "water " + water);
    }

    /** Q1 = A: the foot river runs round the whole ring at the sea level in ring 4's half, its bed under the sea. */
    @Test
    void theFootRiverRunsRoundTheRing() {
        double period = VerdantDescent.perimeter(LAYOUT);
        double r = VerdantDescent.midRadius(LAYOUT);
        WaterScan scan = new WaterScan(FIELD, LAYOUT);
        int samples = 0;
        for (double s = 0.0; s < period; s += 8.0) {
            VerdantDescent.Profile profile = FIELD.descent().profile(s);
            double[] p = VerdantDescent.onRay(LAYOUT, s / r, VerdantDescent.zoneStart(LAYOUT) + profile.river());
            int x = (int) Math.floor(p[0]);
            int z = (int) Math.floor(p[1]);
            RingPosition position = LAYOUT.sampleOverworld(x, z);
            assertTrue(position.ring() == Ring.RING_4, "river in ring 4's half at s " + s);
            assertTrue(FIELD.verdantHeight(x, z) <= VerdantDescent.RIVER_BED + 0.5, "river bed at s " + s);
            assertEquals(WaterScan.WATER, scan.block(x, 62, z), "river water at s " + s);
            assertEquals(WaterScan.AIR, scan.block(x, 63, z), "the sea level at s " + s);
            samples++;
        }
        assertTrue(samples > 4000, "samples " + samples);
    }

    /** The streams are periodic round the loop: cell k and k + count are one stream; a stream's falls likewise. */
    @Test
    void streamsArePeriodicRoundTheSeam() {
        int count = BASINS.streamCount();
        for (int k = -3; k <= 3; k++) {
            assertEquals(BASINS.stream(k), BASINS.stream(k + count));
        }
        VerdantBasins.Stream last = BASINS.stream(count - 1);
        VerdantBasins.Stream first = BASINS.stream(0);
        if (last != null && first != null) {
            assertTrue(first.low() < first.at() && last.high() > last.at());
            assertEquals(last.high() - VerdantDescent.perimeter(LAYOUT), first.low(), 1e-6, "the ranges meet across the seam");
        }
        int streams = 0;
        int free = 0;
        for (int k = 0; k < count; k++) {
            VerdantBasins.Stream stream = BASINS.stream(k);
            // Part 2: a cell whose water could reach into a horseshoe gorge has no stream (the gorge has its own water).
            boolean clear = FIELD.gorges().clear((k - 1) * BASINS.cellSize(), (k + 2) * BASINS.cellSize());
            free += clear ? 1 : 0;
            if (stream != null) {
                streams += clear ? 1 : 0;
                assertNotNull(stream);
                assertTrue(stream.at() >= k * BASINS.cellSize() && stream.at() < (k + 1) * BASINS.cellSize(), "stream " + k + " in its cell");
            }
        }
        System.out.printf("streams %d in %d cells clear of the gorges (of %d)%n", streams, free, count);
        assertTrue(streams > free * 0.8, "streams " + streams + " of " + free);
        assertTrue(free > count * 0.45, "basins and gorges alternate: cells clear of gorges " + free + " of " + count);
    }
}
