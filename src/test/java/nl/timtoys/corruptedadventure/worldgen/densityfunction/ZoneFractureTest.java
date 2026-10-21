package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fracture zone 4→5 (82 skeleton): exactly its argument everywhere, and transparent for the router's visitors. */
class ZoneFractureTest {
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;

    /** A terrain-like argument with a different value at every block (noise in x, y and z). */
    private static final DensityFunction ARGUMENT = DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 2.0, -2.0),
            new DensityFunction.SimpleFunction() {
                private final DensityFunction.NoiseHolder noise = IslandFieldTest.noise("placeholder_terrain", IslandFieldTest.SEED);

                @Override
                public double compute(FunctionContext context) {
                    return noise.getValue(context.blockX(), context.blockY(), context.blockZ());
                }

                @Override
                public double minValue() {
                    return -noise.maxValue();
                }

                @Override
                public double maxValue() {
                    return noise.maxValue();
                }

                @Override
                public net.minecraft.util.KeyDispatchDataCodec<? extends DensityFunction> codec() {
                    throw new UnsupportedOperationException();
                }
            });

    /** Bit-equal with and without the wrapper on a raster across the zone 4→5 (both halves, ± 200), point by point and per array. */
    @Test
    void identityAroundTheZone45() {
        ZoneFracture fracture = new ZoneFracture(ARGUMENT);
        for (int distance = 7600; distance <= 8400; distance += 13) {
            for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 16.0) {
                int x = (int) Math.round(distance * Math.cos(angle));
                int z = (int) Math.round(distance * Math.sin(angle));
                assertEquals(0.0, ZoneFracture.strength(LAYOUT, LAYOUT.effectiveDistance(x, z)));
                for (int y = -64; y < 320; y += 11) {
                    DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(x, y, z);
                    assertEquals(Double.doubleToRawLongBits(ARGUMENT.compute(context)), Double.doubleToRawLongBits(fracture.compute(context)));
                    assertFalse(ZoneFracture.crackAt(LAYOUT, x, y, z) || ZoneFracture.floeAt(LAYOUT, x, y, z));
                }
            }
        }
        double[] expected = new double[48];
        double[] actual = new double[48];
        DensityFunction.ContextProvider column = new DensityFunction.ContextProvider() {
            @Override
            public DensityFunction.FunctionContext forIndex(int index) {
                return new DensityFunction.SinglePointContext(8000, -64 + 8 * index, 17);
            }

            @Override
            public void fillAllDirectly(double[] values, DensityFunction function) {
                for (int i = 0; i < values.length; i++) {
                    values[i] = function.compute(forIndex(i));
                }
            }
        };
        ARGUMENT.fillArray(expected, column);
        fracture.fillArray(actual, column);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(Double.doubleToRawLongBits(expected[i]), Double.doubleToRawLongBits(actual[i]), "index " + i);
        }
        assertEquals(ARGUMENT.minValue(), fracture.minValue());
        assertEquals(ARGUMENT.maxValue(), fracture.maxValue());
    }

    /**
     * {@code mapAll} hands the visitor on to the argument: a visitor that wraps the interpolated marker (as
     * {@code RingBlend} puts its gates there) reaches it, and the wrapper stays on top.
     */
    @Test
    void mapAllKeepsTheGatesInsideTheArgument() {
        DensityFunction marker = DensityFunctions.interpolated(ARGUMENT);
        List<DensityFunction> visited = new ArrayList<>();
        DensityFunction mapped = new ZoneFracture(marker).mapAll(function -> {
            visited.add(function);
            return function instanceof DensityFunctions.MarkerOrMarked ? DensityFunctions.flatCache(function) : function;
        });
        ZoneFracture wrapper = assertInstanceOf(ZoneFracture.class, mapped);
        assertTrue(wrapper.argument() instanceof DensityFunctions.MarkerOrMarked);
        assertTrue(visited.stream().anyMatch(f -> f instanceof DensityFunctions.MarkerOrMarked));
    }

    /** The overworld router: {@code final_density} is the zone fracture around the ring blend (W-terrein, 82). */
    @Test
    void overworldFinalDensityIsTheFractureAroundTheRingBlend() {
        var json = WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("noise_router").getAsJsonObject("final_density");
        assertEquals("corruptedadventure:zone_fracture", json.get("type").getAsString());
        assertEquals("corruptedadventure:ring_blend/overworld", json.get("argument").getAsString());
    }
}
