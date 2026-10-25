package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.function.DoublePredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shares of the G2 ring 2 rock fields in the stationary noise (as the random state seeds a noise, over three seeds)
 * and the agreements between the features, the surface rules and the hoodoo density function.
 */
class Ring2RockFeaturesTest {
    /** Where the open patches of the hoodoo fields begin: the pillar weight's spline crosses min_weight 0.5 here. */
    private static final double HOODOO_OPEN_BELOW = -0.27;
    private static final double PLATEAU_PATCHES = 0.2;

    @Test
    void plateauPatchesCoverAQuarterToAThirdOfThePlateau() {
        double share = share("ring_2_plateau_patches", v -> v >= PLATEAU_PATCHES);
        assertEquals(0.275, share, 0.05, "plateau patches");
    }

    @Test
    void grovesCoverAboutAFifthOfTheLand() {
        // Half the cells of the ramp count: the share of land with at least half the grove density.
        double share = share("ring_2_canyon_groves", v -> v >= (Ring2RockFeatures.GROVE_LOW + Ring2RockFeatures.GROVE_HIGH) / 2.0);
        assertEquals(0.20, share, 0.05, "groves");
    }

    @Test
    void aboutAThirdOfTheHoodooPillarsGo() {
        // A fifth of the noise is open; in the world ~1.4 times as many pillars go (a footprint needs the weight at all
        // its points, and the edge's pillars are smaller): about 30 % fewer pillars (G2-R2-6, measured in g2-rock.md).
        double share = share("ring_2_hoodoo_density", v -> v < HOODOO_OPEN_BELOW);
        assertEquals(0.21, share, 0.04, "open hoodoo noise");
    }

    @Test
    void gulliesAreNarrowLines() {
        // A band of |noise| < w is a few blocks wide; a few per cent of the floor at one gully per ~150 blocks.
        double share = share("ring_2_hoodoo_gullies", v -> Math.abs(v) < Ring2RockFeatures.GULLY_HALF_WIDTH);
        assertTrue(share > 0.02 && share < 0.08, "gully share " + share);
    }

    @Test
    void seepZonesAndClayBreakupHaveSensibleShares() {
        double seeps = share("ring_2_canyon_seeps", v -> v >= Ring2RockFeatures.SEEP_NOISE);
        assertTrue(seeps > 0.2 && seeps < 0.5, "seep zones " + seeps);
        double sand = share("ring_2_clay_breakup", v -> v >= 0.3);
        double gravel = share("ring_2_clay_breakup", v -> v <= -0.35);
        assertTrue(sand + gravel > 0.2 && sand + gravel < 0.45, "clay broken up " + (sand + gravel));
    }

    @Test
    void surfaceRulesUseTheFeaturesBands() {
        String rules = WorldgenData.read("noise_settings/overworld.json").toString();
        String gullies = "\"noise\":\"corruptedadventure:ring_2_hoodoo_gullies\",\"min_threshold\":" + (-Ring2RockFeatures.GULLY_HALF_WIDTH)
                + ",\"max_threshold\":" + Ring2RockFeatures.GULLY_HALF_WIDTH;
        assertTrue(rules.contains(gullies), "the gully band of the surface = the rubble's band");
        assertTrue(rules.contains("\"noise\":\"corruptedadventure:ring_2_plateau_patches\",\"min_threshold\":" + PLATEAU_PATCHES));
    }

    @Test
    void hoodooPillarWeightOpensWhereTheTestSaysAndArchesStay() {
        for (String grid : new String[]{"hoodoos", "hoodoos_offset"}) {
            JsonObject json = WorldgenData.read("density_function/ring_2/overworld/" + grid + ".json");
            assertEquals("corruptedadventure:ring_2/overworld/hoodoo_pillar_weight", json.get("weight").getAsString(), grid);
            assertEquals(0.5, json.get("min_weight").getAsDouble(), grid);
        }
        assertEquals("corruptedadventure:ring_2/overworld/hoodoo_rock_weight",
                WorldgenData.read("density_function/ring_2/overworld/hoodoo_arches.json").get("weight").getAsString(),
                "the arches keep the old weight: an arch may stand in an open patch");
        JsonObject weight = WorldgenData.read("density_function/ring_2/overworld/hoodoo_pillar_weight.json");
        assertEquals("minecraft:flat_cache", weight.get("type").getAsString());
        JsonObject mul = weight.getAsJsonObject("argument");
        assertEquals("corruptedadventure:ring_2/overworld/hoodoo_rock_weight", mul.get("argument1").getAsString());
        JsonObject spline = mul.getAsJsonObject("argument2").getAsJsonObject("spline");
        JsonObject coordinate = spline.getAsJsonObject("coordinate");
        assertEquals("corruptedadventure:ring_2_hoodoo_density", coordinate.get("noise").getAsString());
        assertEquals(1.0, coordinate.get("xz_scale").getAsDouble());
        assertEquals(0.0, coordinate.get("y_scale").getAsDouble());
        JsonArray points = spline.getAsJsonArray("points");
        assertEquals(2, points.size());
        double low = points.get(0).getAsJsonObject().get("location").getAsDouble();
        double high = points.get(1).getAsJsonObject().get("location").getAsDouble();
        assertEquals(0.0, points.get(0).getAsJsonObject().get("value").getAsDouble());
        assertEquals(1.0, points.get(1).getAsJsonObject().get("value").getAsDouble());
        // Zero derivatives at both ends: the Hermite curve passes 0.5 in the middle.
        assertEquals(HOODOO_OPEN_BELOW, (low + high) / 2.0, 1e-9);
    }

    @Test
    void seepProbesAndTreesStayInTheirChunk() {
        assertTrue(Ring2RockFeatures.SEEP_REACH <= 3, "a cell of 8 allows a margin of at most 3");
        // A juniper's blob reaches 2 from its trunk; vanilla's forking trunk (the dead trees) bends at most 3.
        assertTrue(Ring2RockFeatures.TREE_MARGIN >= 4, "the forked tree's foliage attachment writes 4 from the trunk");
        assertTrue(2 * Ring2RockFeatures.TREE_MARGIN < Ring2RockFeatures.TREE_CELL, "the tree cell has room for the margin");
    }

    private static double share(String noise, DoublePredicate inside) {
        NormalNoise.NoiseParameters parameters = WorldgenData.noiseParameters(noise);
        int in = 0;
        int total = 0;
        for (long seed : new long[]{20260930L, 1L, 42L}) {
            var factory = new XoroshiroRandomSource(seed).forkPositional();
            NormalNoise sampler = NormalNoise.create(factory.fromHashOf(ResourceLocation.parse("corruptedadventure:" + noise)), parameters);
            for (int x = -20_000; x < 20_000; x += 97) {
                for (int z = -20_000; z < 20_000; z += 97) {
                    in += inside.test(sampler.getValue(x, 0, z)) ? 1 : 0;
                    total++;
                }
            }
        }
        double share = in / (double) total;
        System.out.println(noise + ": " + String.format("%.3f", share));
        return share;
    }
}
