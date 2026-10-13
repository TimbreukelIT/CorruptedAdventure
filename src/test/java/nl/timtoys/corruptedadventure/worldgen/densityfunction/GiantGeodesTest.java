package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantGeodeShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/** The density function {@code giant_geodes} (65): its view of the generated terrain and the cells its copies share. */
class GiantGeodesTest {
    private static final GiantGeodeShape.Settings SETTINGS = GiantGeodeShape.Settings.CODEC.parse(JsonOps.INSTANCE,
            WorldgenData.read("density_function/ring_4/nether/geodes.json").getAsJsonObject("settings")).getOrThrow();

    /** A plain function of the position, so the interpolation can be checked by hand. */
    private record Field() implements DensityFunction.SimpleFunction {
        @Override
        public double compute(FunctionContext context) {
            return context.blockX() * context.blockX() * 0.01 + context.blockY() * context.blockZ() * 0.1 - 3.0;
        }

        @Override
        public double minValue() {
            return -1000.0;
        }

        @Override
        public double maxValue() {
            return 1000.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }

    private static GiantGeodes geodes(NormalNoise wired) {
        NormalNoise.NoiseParameters parameters = WorldgenData.noiseParameters("nether_ring_4_geode_cells");
        return new GiantGeodes(new Field(), DensityFunctions.zero(), DensityFunctions.zero(),
                new DensityFunction.NoiseHolder(Holder.direct(parameters), wired), SETTINGS);
    }

    /** The noise cell of the Nether's noise settings: the interpolation the geodes test against is the one that generates. */
    @Test
    void cellIsTheNetherNoiseCell() {
        JsonObject noise = WorldgenData.read("noise_settings/nether.json").getAsJsonObject("noise");
        assertEquals(noise.get("size_horizontal").getAsInt() * 4, GiantGeodes.CELL_WIDTH);
        assertEquals(noise.get("size_vertical").getAsInt() * 4, GiantGeodes.CELL_HEIGHT);
    }

    @Test
    void interpolatedIsExactOnTheCornersAndTrilinearBetween() {
        GiantGeodes geodes = geodes(null);
        Field field = new Field();
        assertEquals(field.compute(new DensityFunction.SinglePointContext(8, 16, -4)), geodes.interpolated(8, 16, -4), 1e-9);
        // halfway on x between the corners 8 and 12: the mean of the corners, not the value at 10 (x² is not linear)
        double expected = (field.compute(new DensityFunction.SinglePointContext(8, 16, -4))
                + field.compute(new DensityFunction.SinglePointContext(12, 16, -4))) / 2.0;
        assertEquals(expected, geodes.interpolated(10, 16, -4), 1e-9);
        // y·z is bilinear, so between corners it is exact
        assertEquals(field.compute(new DensityFunction.SinglePointContext(8, 20, -2)), geodes.interpolated(8, 20, -2), 1e-9);
    }

    /**
     * The random state wires the noise (a new map of decided cells); the copies the noise chunk makes of the wired
     * function keep that map, so a cell is decided once per world.
     */
    @Test
    void copiesOfTheWiredFunctionShareTheDecidedCells() {
        NormalNoise noise = NormalNoise.create(new LegacyRandomSource(20260930L), WorldgenData.noiseParameters("nether_ring_4_geode_cells"));
        GiantGeodes unwired = geodes(null);
        DensityFunction.Visitor wiring = new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder holder) {
                return new DensityFunction.NoiseHolder(holder.noiseData(), noise);
            }
        };
        DensityFunction.Visitor chunk = new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function;
            }
        };
        GiantGeodes wired = (GiantGeodes) unwired.mapAll(wiring);
        GiantGeodes copy = (GiantGeodes) wired.mapAll(chunk);
        GiantGeodes other = (GiantGeodes) unwired.mapAll(wiring);
        assertNotSame(wired, copy);
        assertSame(wired.decided(), copy.decided());
        assertNotSame(unwired.decided(), wired.decided());
        assertNotSame(wired.decided(), other.decided(), "another world wires its own");
    }
}
