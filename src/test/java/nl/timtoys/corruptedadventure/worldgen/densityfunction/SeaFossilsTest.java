package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The density function {@code sea_fossils} (78): the terrain it makes round a fossil, its cells, its cost. */
class SeaFossilsTest {
    /** Rolling dunes: the floor height of {@code below(floor)}, as {@code ring_4/nether/floor_height} in the dunes. */
    static double dune(int x, int z) {
        return 43.0 + 3.0 * Math.sin(x / 9.0) * Math.cos(z / 11.0);
    }

    private record Floor() implements DensityFunction.SimpleFunction {
        @Override
        public double compute(FunctionContext context) {
            return dune(context.blockX(), context.blockZ());
        }

        @Override
        public double minValue() {
            return 40.0;
        }

        @Override
        public double maxValue() {
            return 46.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }

    /** The dunes' shape: (floor − y) / 8 − 0.05 (uninterpolated is close enough for the floor tests). */
    private record Dunes() implements DensityFunction.SimpleFunction {
        @Override
        public double compute(FunctionContext context) {
            return (dune(context.blockX(), context.blockZ()) - context.blockY()) / 8.0 - 0.05;
        }

        @Override
        public double minValue() {
            return -20.0;
        }

        @Override
        public double maxValue() {
            return 20.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }

    private static SeaFossils fossils(NormalNoise wired) {
        return new SeaFossils(new Dunes(), new Floor(), DensityFunctions.zero(), DensityFunctions.zero(),
                new DensityFunction.NoiseHolder(Holder.direct(WorldgenData.noiseParameters("nether_ring_4_fossil_cells")), wired),
                SeaFossilShapeTest.SETTINGS);
    }

    private static NormalNoise noise(long seed) {
        return NormalNoise.create(new LegacyRandomSource(seed), WorldgenData.noiseParameters("nether_ring_4_fossil_cells"));
    }

    private static boolean solid(SeaFossils f, int x, int y, int z) {
        return f.compute(new DensityFunction.SinglePointContext(x, y, z)) > 0.0;
    }

    /** First open Y of a column, scanning down from above the dunes (bones above the floor ignored: from the floor up). */
    private static int ground(SeaFossils f, int x, int z, int from) {
        int y = from;
        while (!solid(f, x, y - 1, z)) {
            y--;
        }
        return y;
    }

    @Test
    void floorIsFlatInsideAndRampsWalkablyThroughTheGaps() {
        SeaFossils f = fossils(noise(11L));
        int checked = 0;
        for (int cell = 0; cell < 6; cell++) {
            SeaFossilShape.Fossil fossil = f.fossilOfCell(cell, 3);
            assertNotNull(fossil, "region 0 and edge 0 everywhere: every cell has one");
            // inside: the flat floor at the fossil's floor, open above it
            int x0 = (int) Math.floor(fossil.x());
            int z0 = (int) Math.floor(fossil.z());
            assertTrue(solid(f, x0, fossil.floor() - 1, z0));
            assertTrue(!solid(f, x0, fossil.floor(), z0) && !solid(f, x0, fossil.floor() + 1, z0));
            // through every gap between two ribs, from 18 outside to the middle: steps of at most 1, 2 high open
            for (int k = 1; k < fossil.ribs().size(); k++) {
                double s = (fossil.ribs().get(k - 1).s() + fossil.ribs().get(k).s()) * 0.5;
                for (int side = -1; side <= 1; side += 2) {
                    int last = Integer.MIN_VALUE;
                    for (double l = side * (fossil.halfWidth() + 18.0); Math.abs(l) > 0.5; l -= side * 0.5) {
                        int x = (int) Math.floor(fossil.x() + s * fossil.cos() - l * fossil.sin());
                        int z = (int) Math.floor(fossil.z() + s * fossil.sin() + l * fossil.cos());
                        int y = ground(f, x, z, fossil.floor() + 5);
                        assertTrue(!solid(f, x, y + 1, z), "2 high at " + l);
                        if (last != Integer.MIN_VALUE) {
                            assertTrue(Math.abs(y - last) <= 1, "step " + last + " -> " + y + " at l " + l);
                        }
                        last = y;
                        checked++;
                    }
                    assertEquals(fossil.floor(), last, "flat inside");
                }
            }
        }
        assertTrue(checked > 1000);
    }

    @Test
    void awayFromTheFossilsItIsTheTerrain() {
        SeaFossils f = fossils(noise(11L));
        Dunes dunes = new Dunes();
        SeaFossilShape.Fossil fossil = f.fossilOfCell(0, 0);
        assertNotNull(fossil);
        // the corner of the cell lies outside the margin
        for (int y = 30; y < 60; y++) {
            DensityFunction.SinglePointContext at = new DensityFunction.SinglePointContext(2, y, 2);
            assertEquals(dunes.compute(at), f.compute(at));
        }
    }

    /** The random state wires the noise (a new map of decided cells); the noise chunk's copies keep it. */
    @Test
    void copiesOfTheWiredFunctionShareTheDecidedCells() {
        NormalNoise noise = noise(20260930L);
        SeaFossils unwired = fossils(null);
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
        DensityFunction.Visitor chunk = function -> function;
        SeaFossils wired = (SeaFossils) unwired.mapAll(wiring);
        SeaFossils copy = (SeaFossils) wired.mapAll(chunk);
        assertNotSame(wired, copy);
        assertSame(wired.decided(), copy.decided());
        assertNotSame(unwired.decided(), wired.decided());
        SeaFossils found = SeaFossils.find(DensityFunctions.add(DensityFunctions.zero(), copy));
        assertNotNull(found, "find(root) sees through the tree");
        assertSame(copy.decided(), found.decided(), "and keeps the world's cells");
        assertEquals(wired.fossilOfCell(1, 1), copy.fossilOfCell(1, 1));
    }

    /**
     * Not a pass/fail test of speed: prints the per-block cost of the wrapper next to the bare terrain, away from a
     * fossil (what most columns pay) and inside one, as a guide for the noise budget (perf runs belong to INT).
     */
    @Test
    void costEstimate() {
        SeaFossils f = fossils(noise(11L));
        Dunes dunes = new Dunes();
        SeaFossilShape.Fossil fossil = f.fossilOfCell(0, 0);
        assertNotNull(fossil);
        int fx = (int) fossil.x() & ~15;
        int fz = (int) fossil.z() & ~15;
        double sink = 0.0;
        long[] times = new long[3];
        for (int round = 0; round < 30; round++) {
            long t0 = System.nanoTime();
            sink += sweep(dunes, 0, 0);
            long t1 = System.nanoTime();
            sink += sweep(f, 0, 0);
            long t2 = System.nanoTime();
            sink += sweep(f, fx, fz);
            long t3 = System.nanoTime();
            if (round >= 10) {
                times[0] += t1 - t0;
                times[1] += t2 - t1;
                times[2] += t3 - t2;
            }
        }
        double blocks = 4.0 * 16 * 16 * 128 * 20;
        System.out.printf("sea_fossils cost per block: terrain %.1f ns, wrapper away %.1f ns, inside a fossil %.1f ns (%s)%n",
                times[0] / blocks, times[1] / blocks, times[2] / blocks, sink != 0.0 ? "ok" : "-");
        int touched = 0;
        int columns = 0;
        for (int x = 0; x < 256; x++) {
            for (int z = 0; z < 256; z++) {
                columns++;
                if (fossil.touches(x, z)) {
                    touched++;
                }
            }
        }
        System.out.printf("columns in a fossil's rectangle: %.1f %% of its cell%n", 100.0 * touched / columns);
    }

    /** Four chunks of 16 × 128 × 16 at chunk (x, z), in the noise chunk's order (y, then x, then z). */
    private static double sweep(DensityFunction f, int x0, int z0) {
        double sum = 0.0;
        for (int c = 0; c < 4; c++) {
            int cx = x0 + (c & 1) * 16;
            int cz = z0 + (c >> 1) * 16;
            for (int y = 127; y >= 0; y--) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        sum += f.compute(new DensityFunction.SinglePointContext(cx + x, y, cz + z));
                    }
                }
            }
        }
        return sum;
    }
}
