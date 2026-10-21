package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The cathedral field of Nether ring 5 ({@code nether_cathedrals}, 87): the cells, the cathedrals and their placement. */
public class NetherCathedralsTest {
    public static final long SEED = IslandFieldTest.SEED;
    static final String DIR = "density_function/ring_5/nether/";
    /** Nether ring 5 lies between these midlines (Nether blocks), plus the boundary noise. */
    static final double INNER = 2000.0;
    static final double OUTER = 2425.0;
    /** Half widths of the zones 4→5 and 5→void in the Nether (256 / 4 and 128 / 4 wide). */
    static final double INNER_ZONE_HALF = 32.0;
    static final double OUTER_ZONE_HALF = 16.0;
    static final double ZONE_MARGIN = 24.0;
    /** A box holding all of Nether ring 5. */
    static final int REACH = 2600;

    /** {@code ring_5/nether/edge.json} on a fixed layout: {@code ring_edge} as the server computes it. */
    record Edge(RingLayout layout, double width) implements DensityFunction.SimpleFunction {
        @Override
        public double compute(FunctionContext context) {
            return RingEdge.value(depth(layout, context.blockX(), context.blockZ()), width);
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 1.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }

    /** How far Nether column (x, z) lies inside Nether ring 5, in Nether blocks (like {@code RingColumnCache.boundaryDepth}). */
    static double depth(RingLayout layout, int x, int z) {
        int scale = RingLayout.NETHER_SCALE;
        return layout.boundaryDepth(Ring.RING_5, layout.effectiveDistance((double) x * scale, (double) z * scale)) / scale;
    }

    static RingLayout layout(long seed) {
        return seed == SEED ? IslandFieldTest.LAYOUT : new RingLayout(IslandFieldTest.LAYOUT.settings(), RingLayout.noiseSeed(seed));
    }

    /** A noise as the Nether's random state (legacy random source) wires {@code corruptedadventure:<name>} on {@code seed}. */
    static DensityFunction.NoiseHolder netherNoise(String name, long seed) {
        NormalNoise.NoiseParameters parameters = WorldgenData.noiseParameters(name);
        return new DensityFunction.NoiseHolder(Holder.direct(parameters), NormalNoise.create(
                new LegacyRandomSource(seed).forkPositional().fromHashOf(ResourceLocation.parse("corruptedadventure:" + name)), parameters));
    }

    /** The field as {@code ring_5/nether/<file>.json} configures it (output, noises, edge width, settings), on {@code seed}. */
    static NetherCathedrals field(String file, long seed) {
        JsonObject json = WorldgenData.read(DIR + file + ".json");
        if (json.get("type").getAsString().equals("minecraft:flat_cache")) {
            json = json.getAsJsonObject("argument");
        }
        assertEquals("corruptedadventure:nether_cathedrals", json.get("type").getAsString());
        NetherCathedrals.Output output = NetherCathedrals.Output.CODEC.parse(JsonOps.INSTANCE, json.get("output")).getOrThrow();
        NetherCathedrals.Settings settings = NetherCathedrals.Settings.CODEC.parse(JsonOps.INSTANCE, json.get("settings")).getOrThrow();
        assertEquals("corruptedadventure:ring_5/nether/edge", json.get("edge").getAsString());
        double width = WorldgenData.read(DIR + "edge.json").get("width").getAsDouble();
        return new NetherCathedrals(output, netherNoise(path(json, "cells"), seed), netherNoise(path(json, "plains"), seed),
                new Edge(layout(seed), width), settings);
    }

    static NetherCathedrals terrain(long seed) {
        return field("terrain", seed);
    }

    private static String path(JsonObject json, String field) {
        return ResourceLocation.parse(json.get(field).getAsString()).getPath();
    }

    static List<NetherCathedrals.Cathedral> all(NetherCathedrals field) {
        return field.cathedralsWithin(-REACH, -REACH, REACH, REACH);
    }

    @Test
    void edgeIsTheZonePlusTheMargin() {
        JsonObject edge = WorldgenData.read(DIR + "edge.json");
        assertEquals("corruptedadventure:ring_edge", edge.get("type").getAsString());
        assertEquals("minecraft:the_nether", edge.get("dimension").getAsString());
        assertEquals("ring_5", edge.get("ring").getAsString());
        assertEquals(INNER_ZONE_HALF + ZONE_MARGIN, edge.get("width").getAsDouble());
        // The zones of the layout: 4|5 is 256 wide in the overworld (64 in the Nether), 5|void 128 (32).
        RingLayout layout = layout(SEED);
        assertEquals(INNER_ZONE_HALF * 2 * RingLayout.NETHER_SCALE, layout.settings().transitionWidth(3), 1e-9);
        assertEquals(OUTER_ZONE_HALF * 2 * RingLayout.NETHER_SCALE, layout.settings().transitionWidth(4), 1e-9);
    }

    /** Both outputs read the same settings, noises and edge (the script writes them from one dictionary). */
    @Test
    void maskAndTerrainShareTheirSettings() {
        JsonObject region = WorldgenData.read(DIR + "region.json");
        assertEquals("minecraft:flat_cache", region.get("type").getAsString());
        JsonObject mask = region.getAsJsonObject("argument");
        JsonObject terrain = WorldgenData.read(DIR + "terrain.json");
        assertEquals("mask", mask.get("output").getAsString());
        assertEquals("terrain", terrain.get("output").getAsString());
        for (String field : List.of("cells", "plains", "edge", "settings")) {
            assertEquals(terrain.get(field), mask.get(field), field);
        }
        assertEquals("corruptedadventure:nether_ring_5_cathedral_cells", terrain.get("cells").getAsString());
        assertEquals("corruptedadventure:nether_ring_5_region", terrain.get("plains").getAsString());
    }

    /**
     * Per cell at most one cathedral, every size in its range, the footprint plus its clearance inside the cell, and the
     * whole footprint (sampled on its outline and centre) at least the margin clear of both zones, inside Nether ring 5.
     */
    @Test
    void cathedralsHaveTheirSizesAndKeepClearOfTheZones() {
        for (long seed = SEED; seed < SEED + 5; seed++) {
            NetherCathedrals field = terrain(seed);
            NetherCathedrals.CathedralSettings c = field.settings().cathedrals();
            RingLayout layout = layout(seed);
            for (NetherCathedrals.Cathedral cathedral : all(field)) {
                assertTrue(cathedral.radius() >= 60 && cathedral.radius() <= 90, "diameter 120-180: " + cathedral);
                assertTrue(cathedral.rim() >= 8 && cathedral.rim() <= 16, "rim 8-16: " + cathedral);
                assertTrue(cathedral.pillarRadius() >= 10 && cathedral.pillarRadius() <= 15, "giant column 20-30: " + cathedral);
                int cellX = Math.floorDiv(cathedral.x(), c.cellSize());
                int cellZ = Math.floorDiv(cathedral.z(), c.cellSize());
                int reach = cathedral.radius() + c.clearance();
                assertEquals(cellX, Math.floorDiv(cathedral.x() - reach, c.cellSize()));
                assertEquals(cellX, Math.floorDiv(cathedral.x() + reach, c.cellSize()));
                assertEquals(cellZ, Math.floorDiv(cathedral.z() - reach, c.cellSize()));
                assertEquals(cellZ, Math.floorDiv(cathedral.z() + reach, c.cellSize()));
                for (int i = 0; i <= 360; i++) {
                    double angle = Math.toRadians(i);
                    double r = i == 360 ? 0.0 : cathedral.radius();
                    int x = (int) Math.round(cathedral.x() + Math.cos(angle) * r);
                    int z = (int) Math.round(cathedral.z() + Math.sin(angle) * r);
                    // On the outline the edge was tested every 4 blocks; between those points the boundary noise moves < 1.
                    assertTrue(depth(layout, x, z) >= INNER_ZONE_HALF + ZONE_MARGIN - 1.0, "clear of zone 4→5: " + cathedral);
                    double distance = Math.hypot(x, z);
                    assertTrue(distance > INNER && distance < OUTER, "inside Nether ring 5: " + cathedral);
                }
            }
        }
    }

    /** About 30 all round (25–35) on every seed. */
    @Test
    void aboutThirtyCathedralsRoundTheRing() {
        for (long seed = SEED; seed < SEED + 5; seed++) {
            int count = all(terrain(seed)).size();
            assertTrue(count >= 25 && count <= 35, "seed " + seed + ": " + count + " cathedrals");
        }
    }

    /** The same seed gives the same cells; another seed other cathedrals. */
    @Test
    void pureForTheSeed() {
        List<NetherCathedrals.Cathedral> a = all(terrain(SEED));
        assertEquals(a, all(terrain(SEED)));
        assertEquals(a, all(field("region", SEED)), "the mask sees the same cathedrals");
        assertNotEquals(a, all(terrain(SEED + 1)));
    }

    /**
     * The random state wires the noises (a new map of decided cells); the copies the noise chunk makes of the wired
     * function keep that map and give the same hall.
     */
    @Test
    void copiesOfTheWiredFunctionShareTheDecidedCells() {
        NetherCathedrals wired = terrain(SEED);
        DensityFunction.Visitor chunk = new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }
        };
        NetherCathedrals copy = (NetherCathedrals) wired.mapAll(chunk);
        assertNotSame(wired, copy);
        assertSame(wired.decided(), copy.decided());
        NetherCathedrals.Cathedral first = all(wired).getFirst();
        assertEquals(first, copy.cathedralAt(first.x(), first.z()));
        assertEquals(wired.compute(new DensityFunction.SinglePointContext(first.x() + first.radius() / 2, 110, first.z())),
                copy.compute(new DensityFunction.SinglePointContext(first.x() + first.radius() / 2, 110, first.z())));
        NetherCathedrals found = NetherCathedrals.find(wired);
        assertNotNull(found);
        assertSame(wired.decided(), found.decided(), "find gives a copy of the terrain with its cells");
    }

    /** The mask: 1 inside the rim, 0 beyond the footprint, ≥ 0.5 up to the middle of the rim (the biome's threshold). */
    @Test
    void maskCoversTheHallUpToTheMiddleOfItsRim() {
        NetherCathedrals mask = field("region", SEED);
        NetherCathedrals.Cathedral cathedral = all(mask).getFirst();
        int x = cathedral.x(), z = cathedral.z();
        assertEquals(1.0, mask.compute(new DensityFunction.SinglePointContext(x, 64, z)));
        assertEquals(1.0, mask.mask(x + cathedral.radius() - cathedral.rim(), z));
        for (int r = cathedral.radius() - cathedral.rim(); r <= cathedral.radius(); r++) {
            assertEquals((cathedral.radius() - r) / (double) cathedral.rim(), mask.mask(x + r, z), 1e-9);
        }
        assertEquals(0.0, mask.mask(x + cathedral.radius(), z));
        assertEquals(0.0, mask.mask(x + cathedral.radius() + 50, z));
        assertEquals(0.0, mask.minValue());
        assertEquals(1.0, mask.maxValue());
        assertNotNull(mask.cathedralAt(x, z));
    }

    /** The settings reject a cathedral that does not fit its cell. */
    @Test
    void settingsMustFit() {
        JsonObject settings = WorldgenData.read(DIR + "terrain.json").getAsJsonObject("settings").deepCopy();
        settings.getAsJsonObject("cathedrals").addProperty("cell_size", 200);
        assertTrue(NetherCathedrals.Settings.CODEC.parse(JsonOps.INSTANCE, settings).isError());
    }
}
