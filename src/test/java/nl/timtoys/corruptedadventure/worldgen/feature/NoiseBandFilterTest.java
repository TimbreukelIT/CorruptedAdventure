package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoiseBandFilterTest {
    private static final NormalNoise.NoiseParameters POOLS = new NormalNoise.NoiseParameters(-7, 0.4, 2.0, 1.0);

    @Test
    void sampleEqualsANoiseDensityFunctionWithTheSameNoiseAndScale() {
        NormalNoise noise = NormalNoise.create(new XoroshiroRandomSource(20260930L), POOLS);
        for (double scale : new double[]{1.0, 0.25, 2.0}) {
            DensityFunction function = DensityFunctions.noise(Holder.direct(POOLS), scale, 0.0).mapAll(new DensityFunction.Visitor() {
                @Override
                public DensityFunction apply(DensityFunction f) {
                    return f;
                }

                @Override
                public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder holder) {
                    return new DensityFunction.NoiseHolder(holder.noiseData(), noise);
                }
            });
            for (int x = -600; x <= 600; x += 37) {
                for (int z = -600; z <= 600; z += 41) {
                    double expected = function.compute(new DensityFunction.SinglePointContext(x, 70, z));
                    assertEquals(expected, NoiseBandFilter.sample(noise, scale, x, z), 0.0, x + "," + z);
                }
            }
        }
    }

    @Test
    void bandIsInclusiveLikeASurfaceRuleThreshold() {
        NormalNoise noise = NormalNoise.create(new XoroshiroRandomSource(7L), POOLS);
        int inside = 0;
        for (int x = 0; x < 512; x += 3) {
            for (int z = 0; z < 512; z += 3) {
                double value = NoiseBandFilter.sample(noise, 1.0, x, z);
                // The oasis transition ring of the surface rules: noise_threshold min 0.02 (and not yet 0.12).
                boolean ring = value >= 0.02 && value <= 0.12;
                assertEquals(ring, value >= Ring2Features.TRANSITION && value <= Ring2Features.SHORE);
                inside += ring ? 1 : 0;
            }
        }
        assertTrue(inside > 0);
    }

    @Test
    void snapReadsOneValuePerCell() {
        for (int x = -40; x < 40; x++) {
            double centre = NoiseBandFilter.snapped(x, 8);
            assertEquals(Math.floorDiv(x, 8) * 8 + 4.0, centre);
            assertEquals(centre, NoiseBandFilter.snapped(Math.floorDiv(x, 8) * 8, 8));
            assertEquals(x, NoiseBandFilter.snapped(x, 0));
        }
    }

    @Test
    void codecRoundTripAndValidation() {
        NoiseBandFilter filter = NoiseBandFilter.snapped(Ring2Features.POOLS, 0.05, 0.28, 8);
        JsonElement json = NoiseBandFilter.CODEC.codec().encodeStart(JsonOps.INSTANCE, filter).getOrThrow();
        assertEquals(JsonParser.parseString("{\"noise\":\"corruptedadventure:ring_2_pools\",\"min\":0.05,\"max\":0.28,\"snap\":8}"), json);
        assertTrue(NoiseBandFilter.CODEC.codec().parse(JsonOps.INSTANCE, json).isSuccess());
        assertFalse(parse("{\"noise\":\"corruptedadventure:ring_2_pools\",\"min\":0.3,\"max\":0.2}").isSuccess());
        assertFalse(parse("{\"noise\":\"corruptedadventure:ring_2_pools\",\"snap\":3}").isSuccess());
        assertFalse(parse("{\"noise\":\"corruptedadventure:ring_2_pools\",\"xz_scale\":0}").isSuccess());
        assertTrue(parse("{\"noise\":\"corruptedadventure:ring_2_pools\"}").isSuccess());
    }

    private static DataResult<NoiseBandFilter> parse(String json) {
        return NoiseBandFilter.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }
}
