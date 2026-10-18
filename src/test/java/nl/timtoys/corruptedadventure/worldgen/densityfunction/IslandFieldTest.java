package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ring 4's terrain field: found through the router, read access, determinism (59, 60). */
public class IslandFieldTest {
    public static final long SEED = 20260930L;
    public static final RingLayout LAYOUT = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(SEED));
    private static final IslandField FIELD = field();

    /** A noise as the overworld's random state wires {@code corruptedadventure:<name>} on {@code seed}. */
    static DensityFunction.NoiseHolder noise(String name, long seed) {
        NormalNoise.NoiseParameters parameters = WorldgenData.noiseParameters(name);
        return new DensityFunction.NoiseHolder(Holder.direct(parameters), NormalNoise.create(
                new XoroshiroRandomSource(seed).forkPositional().fromHashOf(ResourceLocation.parse("corruptedadventure:" + name)), parameters));
    }

    /** The field as {@code ring_4/overworld/islands.json} configures it, on the test seed and layout. */
    public static IslandField field() {
        return field(SEED);
    }

    /** The same on another seed (its own layout noise too). */
    static IslandField field(long seed) {
        RingLayout layout = seed == SEED ? LAYOUT : new RingLayout(LAYOUT.settings(), RingLayout.noiseSeed(seed));
        JsonObject json = WorldgenData.read("density_function/ring_4/overworld/islands.json");
        JsonObject r = json.getAsJsonObject("relief");
        Ring4Relief relief = new Ring4Relief(noise(key(r, "water"), seed), noise(key(r, "growth"), seed),
                noise(key(r, "hills"), seed), noise(key(r, "detail"), seed), noise(key(r, "shoals"), seed),
                noise(key(r, "rivers"), seed), noise(key(r, "fjords"), seed), noise(key(r, "warp"), seed));
        IslandField.Settings settings = IslandField.Settings.CODEC.parse(JsonOps.INSTANCE, json.get("settings")).getOrThrow();
        // The plateau above the cliff 3→4: ring 3's field on the test seed (its layout is the same); none on other seeds.
        return new IslandField(noise(key(json, "hash"), seed), relief, settings, seed == SEED ? VolcanicFieldTest.field() : null,
                () -> layout);
    }

    private static String key(JsonObject json, String field) {
        return ResourceLocation.parse(json.get(field).getAsString()).getPath();
    }

    /**
     * INT2: a noise chunk wraps the ring 3 plateau in caches and gates of its own; its copy of the field must still keep
     * the island decisions of the random state's instance (else every chunk re-decides its cells), and a copy with
     * other noises must not.
     */
    @Test
    void aNoiseChunkCopyKeepsTheDecisionsThroughAWrappedPlateau() {
        IslandField field = field();
        DensityFunction copy = field.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function instanceof VolcanicField ? DensityFunctions.flatCache(function) : function;
            }
        });
        assertSame(field.decisions(), ((IslandField) copy).decisions());
        DensityFunction otherNoises = field.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
                return new DensityFunction.NoiseHolder(noise.noiseData());
            }
        });
        assertNotSame(field.decisions(), ((IslandField) otherNoises).decisions());
    }

    @Test
    void foundInsideARouterTree() {
        DensityFunction root = DensityFunctions.add(DensityFunctions.constant(1.0),
                DensityFunctions.max(DensityFunctions.zero(), DensityFunctions.flatCache(FIELD)));
        // mapAll rebuilds the tree, so the field found is an equal copy.
        assertEquals(FIELD, IslandField.find(root));
        assertEquals(FIELD, IslandField.find(new IslandOutput(FIELD, IslandOutput.Output.THIN)));
        assertNull(IslandField.find(DensityFunctions.add(DensityFunctions.constant(1.0), DensityFunctions.zero())));
    }

    @Test
    void aNoiseChunkCopyIsEqualAndKeepsTheDecisions() {
        DensityFunction copy = FIELD.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }
        });
        assertEquals(FIELD, copy);
        assertEquals(FIELD.hashCode(), copy.hashCode());
    }

    /** Determinism: two fields built apart, asked in opposite orders, give the same columns (no state in the values). */
    @Test
    void columnsArePureWhateverTheOrder() {
        IslandField other = field();
        List<int[]> points = new ArrayList<>();
        for (int x = 6100; x <= 7900; x += 173) {
            for (int z = -900; z <= 900; z += 157) {
                points.add(new int[]{x, z});
            }
        }
        List<double[]> forward = new ArrayList<>();
        for (int[] p : points) {
            IslandField.Column c = FIELD.column(p[0], p[1]);
            forward.add(new double[]{c.surface(), c.height(), FIELD.initialHeight(p[0], p[1]), c.islandMask(), c.lakeTop(),
                    c.fallTop(), FIELD.thin(p[0], 150, p[1])});
        }
        for (int k = points.size() - 1; k >= 0; k--) {
            int[] p = points.get(k);
            IslandField.Column c = other.column(p[0], p[1]);
            double[] expected = forward.get(k);
            assertEquals(expected[0], c.surface(), p[0] + ", " + p[1]);
            assertEquals(expected[1], c.height());
            assertEquals(expected[2], other.initialHeight(p[0], p[1]));
            assertEquals(expected[3], c.islandMask());
            assertEquals(expected[4], c.lakeTop());
            assertEquals(expected[5], c.fallTop());
            assertEquals(expected[6], other.thin(p[0], 150, p[1]));
        }
        // The cell decisions too, asked in the other order.
        for (int i = 70; i >= 40; i--) {
            assertEquals(FIELD.cell(i, 3), other.cell(i, 3));
        }
    }

    /**
     * verdant_falls (2c): the descent, its basins, springs and falls are pure: a field built apart, asked in the opposite
     * order (columns and chunks), gives the same values, also across the seam of the angle (x < 0, z = 0).
     */
    @Test
    void theDescentIsPureWhateverTheOrder() {
        IslandField first = field();
        IslandField other = field();
        List<int[]> points = new ArrayList<>();
        // 0.048 and 1.35: the middles of gorges 0 and 11 (part 2), with their walls, paths, niches and falls.
        for (double angle : new double[]{0.0, 0.048, 1.3, 1.35, 2.6, Math.PI - 0.004, Math.PI, -Math.PI + 0.004, 4.4}) {
            for (double u = 52.0; u < 256.0; u += 7.3) {
                for (double ds = -24.0; ds <= 24.0; ds += 6.1) {
                    double[] p = VerdantDescent.onRay(LAYOUT, angle + ds / VerdantDescent.midRadius(LAYOUT),
                            VerdantDescent.zoneStart(LAYOUT) + u);
                    points.add(new int[]{(int) Math.floor(p[0]), (int) Math.floor(p[1])});
                }
            }
        }
        List<double[]> forward = new ArrayList<>();
        for (int[] p : points) {
            IslandField.Column c = first.column(p[0], p[1]);
            double[] values = {c.surface(), c.lakeTop(), c.rim(), c.cliff(), c.springY(), c.fallTop(), c.caveLiftFrom(),
                    first.initialHeight(p[0], p[1]), 0.0, 0.0, c.openFrom(), c.openTo(), 0.0, 0.0, 0.0};
            values[8] = first.thin(p[0], (int) Math.floor(values[0]), p[1]);
            values[9] = first.fallsOfChunk(p[0] >> 4, p[1] >> 4).size();
            for (int i = 0; i < 3; i++) {
                values[12 + i] = first.thin(p[0], (int) Math.floor(values[0]) - 6 - 7 * i, p[1]);
            }
            forward.add(values);
        }
        int lakes = 0;
        for (int k = points.size() - 1; k >= 0; k--) {
            int[] p = points.get(k);
            double fallCount = other.fallsOfChunk(p[0] >> 4, p[1] >> 4).size();
            IslandField.Column c = other.column(p[0], p[1]);
            double[] values = {c.surface(), c.lakeTop(), c.rim(), c.cliff(), c.springY(), c.fallTop(), c.caveLiftFrom(),
                    other.initialHeight(p[0], p[1]), 0.0, fallCount, c.openFrom(), c.openTo(), 0.0, 0.0, 0.0};
            values[8] = other.thin(p[0], (int) Math.floor(values[0]), p[1]);
            for (int i = 0; i < 3; i++) {
                values[12 + i] = other.thin(p[0], (int) Math.floor(values[0]) - 6 - 7 * i, p[1]);
            }
            assertArrayEquals(forward.get(k), values, p[0] + ", " + p[1]);
            lakes += c.lakeTop() != Integer.MIN_VALUE ? 1 : 0;
        }
        assertTrue(lakes > 0, "no water among the points");
    }

    /** No islands (nor falls, lifts or world trees) outside ring 4 and its reach. */
    @Test
    void noIslandsOutsideRing4() {
        assertTrue(FIELD.islandsWithin(-3000, -3000, 3000, 3000).isEmpty());
        assertTrue(FIELD.islandsWithin(9000, -500, 9500, 500).isEmpty());
        assertTrue(FIELD.waterfallsWithin(-500, -500, 500, 500).isEmpty());
        assertTrue(FIELD.worldTrees(-4000, -4000, 4000, 4000).isEmpty());
        assertTrue(FIELD.worldTrees(-9500, 8000, 9500, 9500).isEmpty());
    }

    /** Review 59 (2nd): one rule for overlapping islands, whatever the order: bottom, top, then the centre. */
    @Test
    void overlappingIslandsAreChosenByOneRuleWhateverTheOrder() {
        IslandField.Island low = new IslandField.Island(7000.0, 0.0, 20.0, 100, 160);
        IslandField.Island high = new IslandField.Island(7010.0, 0.0, 20.0, 130, 150);
        IslandField.Island sameBottomHigherTop = new IslandField.Island(7005.0, 0.0, 20.0, 130, 170);
        IslandField.Island twin = new IslandField.Island(7006.0, 0.0, 20.0, 130, 170);
        List<IslandField.Island> islands = new ArrayList<>(List.of(low, high, sameBottomHigherTop, twin));
        for (int i = 0; i < 8; i++) {
            Collections.shuffle(islands, new Random(i));
            assertEquals(twin, IslandField.uppermost(islands));
        }
        assertEquals(high, IslandField.uppermost(List.of(low, high)));
        assertNull(IslandField.uppermost(List.of()));
    }

    @Test
    void islandCoversItsFootprint() {
        IslandField.Island island = new IslandField.Island(7000.5, 10.5, 20.0, 110, 150);
        assertTrue(island.covers(7000, 10));
        assertTrue(island.covers(7019, 10));
        assertTrue(!island.covers(7022, 10));
        assertTrue(island.reaches(7015, 25, 7100, 100));
        assertTrue(!island.reaches(7030, 40, 7100, 100));
    }
}
