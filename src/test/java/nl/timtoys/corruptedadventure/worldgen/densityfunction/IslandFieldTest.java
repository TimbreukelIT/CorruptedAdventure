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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ring 4's terrain field: found through the router, read access, determinism (59, 60). */
public class IslandFieldTest {
    public static final long SEED = 20260930L;
    public static final RingLayout LAYOUT = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(SEED));
    private static final IslandField FIELD = field();

    /** A noise as the overworld's random state wires {@code corruptedadventure:<name>} on the test seed. */
    static DensityFunction.NoiseHolder noise(String name) {
        NormalNoise.NoiseParameters parameters = WorldgenData.noiseParameters(name);
        return new DensityFunction.NoiseHolder(Holder.direct(parameters), NormalNoise.create(
                new XoroshiroRandomSource(SEED).forkPositional().fromHashOf(ResourceLocation.parse("corruptedadventure:" + name)), parameters));
    }

    /** The field as {@code ring_4/overworld/islands.json} configures it, on the test seed and layout. */
    public static IslandField field() {
        return field(LAYOUT);
    }

    static IslandField field(RingLayout layout) {
        JsonObject json = WorldgenData.read("density_function/ring_4/overworld/islands.json");
        JsonObject r = json.getAsJsonObject("relief");
        Ring4Relief relief = new Ring4Relief(noise(key(r, "water")), noise(key(r, "growth")), noise(key(r, "hills")),
                noise(key(r, "detail")), noise(key(r, "shoals")), noise(key(r, "rivers")), noise(key(r, "fjords")),
                noise(key(r, "warp")));
        IslandField.Settings settings = IslandField.Settings.CODEC.parse(JsonOps.INSTANCE, json.get("settings")).getOrThrow();
        return new IslandField(noise(key(json, "hash")), relief, settings, () -> layout);
    }

    private static String key(JsonObject json, String field) {
        return ResourceLocation.parse(json.get(field).getAsString()).getPath();
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

    /** No islands (nor falls or lifts) outside ring 4 and its reach. */
    @Test
    void noIslandsOutsideRing4() {
        assertTrue(FIELD.islandsWithin(-3000, -3000, 3000, 3000).isEmpty());
        assertTrue(FIELD.islandsWithin(9000, -500, 9500, 500).isEmpty());
        assertTrue(FIELD.waterfallsWithin(-500, -500, 500, 500).isEmpty());
        assertTrue(FIELD.worldTrees(6000, -500, 8000, 500).isEmpty());
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
