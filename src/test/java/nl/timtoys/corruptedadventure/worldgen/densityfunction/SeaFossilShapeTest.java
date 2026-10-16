package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.feature.NetherRing4Features;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The geometry of the sea-creature fossils of Nether ring 4 (78): gaps, flat floor, exclusion zones, rate. */
class SeaFossilShapeTest {
    static final SeaFossilShape.Settings SETTINGS = SeaFossilShape.Settings.CODEC.parse(JsonOps.INSTANCE,
            WorldgenData.read("density_function/ring_4/nether/fossils.json").getAsJsonObject("settings")).getOrThrow();
    /** Nether ring 4 without boundary noise: 1.500–2.000; {@code edge} (48 wide) is 0 deeper than 48 inside. */
    static final double INNER = 1500.0;
    static final double OUTER = 2000.0;
    static final double EDGE_WIDTH = 48.0;

    /** The world as the placement sees it: region and dunes noise of one seed, a radial ring edge. */
    record World(NormalNoise region, NormalNoise dunes, NormalNoise cells) {
        static World of(long seed) {
            return new World(NormalNoise.create(new LegacyRandomSource(seed), WorldgenData.noiseParameters("nether_ring_4_region")),
                    NormalNoise.create(new LegacyRandomSource(seed + 1), WorldgenData.noiseParameters("nether_ring_4_dunes")),
                    NormalNoise.create(new LegacyRandomSource(seed + 2), WorldgenData.noiseParameters("nether_ring_4_fossil_cells")));
        }

        double region(int x, int z) {
            return region.getValue(x, 0.0, z);
        }

        double edge(int x, int z) {
            double r = Math.hypot(x, z);
            return Math.min(r - INNER, OUTER - r) >= EDGE_WIDTH ? 0.0 : 1.0;
        }

        /** The dunes' first open Y: floor 43 + 3 (1 − 2|n|), less the 0.4 of the roughness. */
        double floor(int x, int z) {
            return 43.0 + 3.0 * (1.0 - 2.0 * Math.abs(dunes.getValue(x, 0.0, z))) - SeaFossils.SURFACE_OFFSET;
        }

        SeaFossilShape.Fossil fossil(int cellX, int cellZ) {
            return SeaFossilShape.fossilOfCell(SETTINGS, (cx, cz, salt) -> SeededHash.hash(cells, cx, cz, salt), cellX, cellZ,
                    this::region, this::edge, this::floor);
        }

        /** Every fossil of the ring. */
        List<SeaFossilShape.Fossil> fossils() {
            List<SeaFossilShape.Fossil> found = new ArrayList<>();
            int cells = (int) Math.ceil(OUTER / SETTINGS.cellSize());
            for (int i = -cells; i < cells; i++) {
                for (int j = -cells; j < cells; j++) {
                    SeaFossilShape.Fossil fossil = fossil(i, j);
                    if (fossil != null) {
                        found.add(fossil);
                    }
                }
            }
            return found;
        }
    }

    /** Fossils of many placements, each built on a flat natural floor. */
    private static List<SeaFossilShape.Fossil> samples() {
        List<SeaFossilShape.Fossil> fossils = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            long h = SeededHash.mix(0x5EAF0551L + i);
            fossils.add(SeaFossilShape.candidate(SETTINGS, h, i % 7, i / 7, (x, z) -> 43.0, 43));
        }
        return fossils;
    }

    private static boolean bone(SeaFossilShape.Fossil fossil, double s, double l, int y) {
        int x = (int) Math.floor(fossil.x() + s * fossil.cos() - l * fossil.sin());
        int z = (int) Math.floor(fossil.z() + s * fossil.sin() + l * fossil.cos());
        int i = y - fossil.maskBase();
        return i >= 0 && i < 64 && (fossil.boneMask(x, z) >>> i & 1L) != 0L;
    }

    @Test
    void settingsFromTheJsonKeepTheFossilInItsCell() {
        for (SeaFossilShape.Fossil fossil : samples()) {
            int cellX = Math.floorDiv((int) fossil.x(), SETTINGS.cellSize());
            int cellZ = Math.floorDiv((int) fossil.z(), SETTINGS.cellSize());
            for (double s : new double[]{fossil.sMin() - fossil.ramp(), fossil.sMax() + fossil.ramp()}) {
                for (double l : new double[]{-fossil.lMax() - fossil.ramp(), fossil.lMax() + fossil.ramp()}) {
                    int x = (int) Math.floor(fossil.x() + s * fossil.cos() - l * fossil.sin());
                    int z = (int) Math.floor(fossil.z() + s * fossil.sin() + l * fossil.cos());
                    assertEquals(cellX, Math.floorDiv(x, SETTINGS.cellSize()), "corner in the cell");
                    assertEquals(cellZ, Math.floorDiv(z, SETTINGS.cellSize()), "corner in the cell");
                }
            }
            double length = fossil.sMax() - fossil.sMin();
            assertTrue(length >= 80.0, "80+ long: " + length);
        }
    }

    @Test
    void ribsLeaveGapsOfAtLeastFour() {
        for (SeaFossilShape.Fossil fossil : samples()) {
            List<SeaFossilShape.Rib> ribs = fossil.ribs();
            assertTrue(ribs.size() >= 3, "a ribcage");
            for (int k = 1; k < ribs.size(); k++) {
                assertTrue(ribs.get(k).s() - ribs.get(k - 1).s() - SeaFossilShape.RIB_THICKNESS >= SeaFossilShape.MIN_GAP);
            }
            // on the block grid: along the side walls, at walking height, every gap between two ribs is >= 4 blocks
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 1; k < ribs.size(); k++) {
                    double mid = (ribs.get(k - 1).s() + ribs.get(k).s()) * 0.5;
                    double l = side * (fossil.floorHalfWidth(mid) - 1.0);
                    double clear = 0.0;
                    for (double s = mid; s < ribs.get(k).s() && !blocked(fossil, s, l); s += 0.25) {
                        clear += 0.25;
                    }
                    for (double s = mid - 0.25; s > ribs.get(k - 1).s() && !blocked(fossil, s, l); s -= 0.25) {
                        clear += 0.25;
                    }
                    assertTrue(clear >= SeaFossilShape.MIN_GAP, "gap " + clear + " at rib " + k);
                }
            }
        }
    }

    private static boolean blocked(SeaFossilShape.Fossil fossil, double s, double l) {
        for (int y = fossil.floor(); y < fossil.floor() + 3; y++) {
            if (bone(fossil, s, l, y)) {
                return true;
            }
        }
        return false;
    }

    /** Inside the ribcage, the neck and the back of the skull the floor is flat and open: no bone in the 3 blocks above the floor, 4 from the walls. */
    @Test
    void insideIsHollowAndFlat() {
        for (SeaFossilShape.Fossil fossil : samples()) {
            // the ribcage, the neck and the back half of the skull (the snout is low inside)
            for (double s = -fossil.cageHalf() + 1.0; s <= fossil.skullStart() + fossil.skullLength() * 0.5; s += 1.0) {
                // the skull is a low dome: only its middle is walkable
                double w = s < fossil.skullStart() ? fossil.floorHalfWidth(s) - 4.0 : fossil.floorHalfWidth(s) * 0.4;
                for (double l = -w; l <= w; l += 1.0) {
                    int x = (int) Math.floor(fossil.x() + s * fossil.cos() - l * fossil.sin());
                    int z = (int) Math.floor(fossil.z() + s * fossil.sin() + l * fossil.cos());
                    assertEquals(1.0, fossil.floorWeight(x, z), 1e-9, "flat floor");
                    for (int y = fossil.floor(); y < fossil.floor() + 3; y++) {
                        assertFalse(bone(fossil, s, l, y), "open at " + s + ", " + l + ", y " + (y - fossil.floor()) + " cage " + fossil.cageHalf() + " skull " + fossil.skullStart() + "/" + fossil.skullLength() + "/" + fossil.skullWidth() + "/" + fossil.skullHeight() + " v " + fossil.vertebrae().stream().filter(v -> Math.abs(fossil.along(v.x(), v.z()) - 44.5) < 3).map(v -> fossil.along(v.x(), v.z()) + ":" + (v.y() - fossil.floor())).toList());
                    }
                }
            }
            // the cage is high: at its middle there is room up to most of the arch
            int y = fossil.floor() + (int) (fossil.height() * 0.6);
            assertFalse(bone(fossil, 0.0, fossil.halfWidth() * 0.3, y), "hall height");
        }
    }

    /** Half buried: the ribs go on under the floor, and the bones reach well above it. */
    @Test
    void halfBuried() {
        for (SeaFossilShape.Fossil fossil : samples()) {
            SeaFossilShape.Rib rib = fossil.ribs().get(fossil.ribs().size() / 2);
            double l = rib.halfWidth() - 1.0;
            if (rib.broken() != 0 && Math.signum(l) == rib.broken()) {
                l = -l;
            }
            assertTrue(bone(fossil, rib.s(), l, fossil.floor() - 2), "rib under the floor");
            assertTrue(bone(fossil, rib.s(), 0.0, fossil.floor() + (int) rib.height() - 1), "rib top");
        }
    }

    /** Every fossil lies in the bone dunes, never over the spires' sea or in the hollows, and clear of the zones. */
    @Test
    void onlyInTheDunesAndClearOfTheZones() {
        for (long seed : new long[]{20260930L, 1L, 42L}) {
            World world = World.of(seed);
            for (SeaFossilShape.Fossil fossil : world.fossils()) {
                for (double s = fossil.sMin() - fossil.ramp(); s <= fossil.sMax() + fossil.ramp(); s += 1.0) {
                    for (double l = -fossil.lMax() - fossil.ramp(); l <= fossil.lMax() + fossil.ramp(); l += 1.0) {
                        int x = (int) Math.floor(fossil.x() + s * fossil.cos() - l * fossil.sin());
                        int z = (int) Math.floor(fossil.z() + s * fossil.sin() + l * fossil.cos());
                        if (fossil.boneMask(x, z) == 0L && fossil.floorWeight(x, z) <= 0.0) {
                            continue;
                        }
                        double region = world.region(x, z);
                        assertTrue(region < NetherRing4Features.SPIRES_THRESHOLD - 0.04, "not where the spires start: " + region);
                        assertTrue(region > NetherRing4Features.HOLLOWS_THRESHOLD + 0.04, "not where the hollows start: " + region);
                        double r = Math.hypot(x, z);
                        assertTrue(r - INNER >= EDGE_WIDTH && OUTER - r >= EDGE_WIDTH, "clear of the zones and dams: r " + r);
                    }
                }
            }
        }
    }

    /** ~1 per 256 × 256 of bone dunes (G5 = A), deterministic. */
    @Test
    void rateInTheDunes() {
        double total = 0.0;
        double dunes = 0.0;
        for (long seed : new long[]{20260930L, 1L, 42L, 7L}) {
            World world = World.of(seed);
            List<SeaFossilShape.Fossil> fossils = world.fossils();
            assertEquals(fossils, world.fossils(), "deterministic");
            total += fossils.size();
            for (int x = -2000; x < 2000; x += 8) {
                for (int z = -2000; z < 2000; z += 8) {
                    double r = Math.hypot(x, z);
                    double region = world.region(x, z);
                    if (r >= INNER && r < OUTER && Math.abs(region) < 0.14) {
                        dunes += 64.0;
                    }
                }
            }
        }
        double perCell = total / (dunes / (256.0 * 256.0));
        System.out.printf("sea fossils: %.0f in 4 seeds, %.2f per 256x256 of dunes%n", total, perCell);
        assertTrue(perCell > 0.3 && perCell < 1.3, "rate " + perCell);
    }

    @Test
    void sameCellSameFossil() {
        World a = World.of(5L);
        World b = World.of(5L);
        for (int i = -8; i < 8; i++) {
            assertEquals(a.fossil(i, 6), b.fossil(i, 6));
        }
        assertNotNull(SETTINGS);
    }
}
