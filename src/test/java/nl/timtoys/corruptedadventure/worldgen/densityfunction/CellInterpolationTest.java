package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CellInterpolationTest {
    @Test
    void cornersAreExactAndLinearFunctionsStayLinear() {
        DensityFunction linear = TestFunctions.of((x, y, z) -> 0.25 * x - 0.125 * y + 0.5 * z + 3.0);
        CellInterpolation interpolation = new CellInterpolation(linear);
        Random random = new Random(1);
        for (int i = 0; i < 2_000; i++) {
            double x = random.nextInt(4_000) - 2_000 + random.nextDouble();
            double y = random.nextInt(300) - 60 + random.nextDouble();
            double z = random.nextInt(4_000) - 2_000 + random.nextDouble();
            assertEquals(0.25 * x - 0.125 * y + 0.5 * z + 3.0, interpolation.sample(x, y, z), 1e-9);
        }
        DensityFunction wavy = TestFunctions.of((x, y, z) -> Math.sin(x * 0.7) * Math.cos(y * 0.3) + Math.sin(z));
        CellInterpolation corners = new CellInterpolation(wavy);
        for (int x = -16; x <= 16; x += 4) {
            for (int y = -64; y <= 64; y += 8) {
                for (int z = -16; z <= 16; z += 4) {
                    assertEquals(wavy.compute(new DensityFunction.SinglePointContext(x, y, z)), corners.sample(x, y, z), 1e-12);
                }
            }
        }
    }

    @Test
    void sameTrilinearInterpolationAsTheNoiseChunk() {
        DensityFunction wavy = TestFunctions.of((x, y, z) -> Math.sin(x * 0.7) * Math.cos(y * 0.3) + Math.sin(z * 1.3));
        CellInterpolation interpolation = new CellInterpolation(wavy);
        Random random = new Random(2);
        for (int i = 0; i < 2_000; i++) {
            int x = random.nextInt(400) - 200;
            int y = random.nextInt(300) - 64;
            int z = random.nextInt(400) - 200;
            int x0 = Math.floorDiv(x, 4) * 4;
            int y0 = Math.floorDiv(y, 8) * 8;
            int z0 = Math.floorDiv(z, 4) * 4;
            double expected = Mth.lerp3((x - x0) / 4.0, (y - y0) / 8.0, (z - z0) / 4.0,
                    at(wavy, x0, y0, z0), at(wavy, x0 + 4, y0, z0), at(wavy, x0, y0 + 8, z0), at(wavy, x0 + 4, y0 + 8, z0),
                    at(wavy, x0, y0, z0 + 4), at(wavy, x0 + 4, y0, z0 + 4), at(wavy, x0, y0 + 8, z0 + 4), at(wavy, x0 + 4, y0 + 8, z0 + 4));
            assertEquals(expected, interpolation.sample(x, y, z), 1e-12);
        }
    }

    private static double at(DensityFunction f, int x, int y, int z) {
        return f.compute(new DensityFunction.SinglePointContext(x, y, z));
    }
}
