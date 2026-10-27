package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandFieldTest;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring4Tunnels;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicFieldTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.ASK;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.KEEP;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.LAVA_UNLESS_EXPOSED;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.LAVA_UNLESS_OPEN;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.ROCK;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.SPRING;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.TO_AIR;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.TO_FALLING_LAVA;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.TO_FALLING_WATER;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.TO_GAS;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.TO_LAVA;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.TO_POOL;
import static nl.timtoys.corruptedadventure.worldgen.chunk.RingAquifer.TO_WATER;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The column profile of {@link RingAquifer} (opt/aquifer) against the chain of tests per block it replaced, in the
 * chain's order (the first rule that decides a block wins): random combinations of every rule's bounds, with and
 * without the carvers' pass, over the whole height and block by block (the open ends of the rules on both sides of the
 * profile's base), with real tunnel spans of ring 4 and real cave columns of ring 3. The profile was also compared call
 * by call with a copy of the old {@code RingAquifer} (51d3aade): 93,986,946 calls, the fluid update flag after each.
 */
class RingAquiferProfileTest {
    private static final VolcanicField FIELD = VolcanicFieldTest.field();
    private static final IslandField ISLANDS = IslandFieldTest.field();
    private static final int MIN_Y = -64;
    private static final int HEIGHT = 384;
    private static final int TOP = RingFluidPicker.LAVA_LAYER_TOP;

    private static final Aquifer NOTHING = new Aquifer() {
        @Override
        public BlockState computeSubstance(DensityFunction.FunctionContext context, double substance) {
            return null;
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return false;
        }
    };

    @Test
    void theProfileIsTheChainOfRulesPerBlock() {
        Random random = new Random(20261027L);
        List<Ring4Tunnels.Span> spans = spans();
        List<int[]> caveColumns = caveColumns();
        assertTrue(!spans.isEmpty() && !caveColumns.isEmpty(), "real spans and cave columns found");
        RingAquifer aquifer = new RingAquifer(Level.OVERWORLD, NOTHING, FIELD, null, (x, z) -> RingPosition.within(Ring.RING_1),
                (x, z) -> 0.0, null, null, MIN_Y, HEIGHT);
        List<String> faults = new ArrayList<>();
        int combinations = 4000;
        for (int c = 0; c < combinations; c++) {
            int[] at = caveColumns.get(random.nextInt(caveColumns.size()));
            RingAquifer.Rules r = rules(random, spans, at[0], at[1]);
            for (boolean carving : new boolean[]{false, true}) {
                byte[] whole = paint(aquifer, r, at, MIN_Y, HEIGHT, carving);
                for (int k = 0; k < HEIGHT; k++) {
                    check(faults, c, r, at, MIN_Y + k, carving, whole[k] & 0xFF);
                }
                for (int k = 0; k < 6; k++) {
                    // One block on its own (outside the profile's heights in worldgen), above and below the base.
                    int y = MIN_Y - 10 + random.nextInt(HEIGHT + 20);
                    check(faults, c, r, at, y, carving, paint(aquifer, r, at, y, 1, carving)[0] & 0xFF);
                }
            }
        }
        assertTrue(faults.isEmpty(), String.join("\n", faults));
    }

    /**
     * Under the Nether's disabled aquifer the profile holds the picker's status as decided: the same state (instance) and
     * fluid update flag as asking the disabled aquifer, block for block, over every ring and zone.
     */
    @Test
    void theNethersProfileHoldsThePickersAnswers() {
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        Aquifer.FluidStatus sea = new Aquifer.FluidStatus(32, Blocks.LAVA.defaultBlockState());
        RingPosition[] positions = {RingPosition.within(Ring.RING_1), RingPosition.within(Ring.RING_2), RingPosition.within(Ring.RING_3),
                RingPosition.within(Ring.RING_4), RingPosition.within(Ring.RING_5), RingPosition.within(Ring.VOID),
                new RingPosition(Ring.RING_1, Ring.RING_2, 0.4), new RingPosition(Ring.RING_2, Ring.RING_3, 0.6),
                new RingPosition(Ring.RING_4, Ring.RING_5, 0.5), new RingPosition(Ring.RING_5, Ring.VOID, 0.3)};
        RingAquifer.RingLookup rings = (x, z) -> positions[Math.floorMod(x * 31 + z * 17, positions.length)];
        RingFluidPicker.Ring5Bands bands = (x, z) -> 0.0;
        RingFluidPicker picker = new RingFluidPicker(Level.NETHER, (x, y, z) -> y < -54 ? lava : sea,
                Map.of(Ring.RING_2, 22, Ring.RING_3, 22), bands);
        ChunkPos chunk = new ChunkPos(-3, 5);
        RingFluidPicker.ChunkPicker plain = picker.forNoiseChunk(rings, chunk);
        RingAquifer profiled = new RingAquifer(Level.NETHER, Aquifer.createDisabled(plain), null, null, rings, bands, null, plain, 0, 256);
        RingAquifer asking = new RingAquifer(Level.NETHER, Aquifer.createDisabled(picker.forNoiseChunk(rings, chunk)), null, null,
                rings, bands, null, null, 0, 256);
        List<String> faults = new ArrayList<>();
        for (boolean carving : new boolean[]{false, true}) {
            profiled.carving(carving);
            asking.carving(carving);
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
                for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                    for (int y = 0; y < 256; y++) {
                        for (double substance : new double[]{-0.5, 0.0, 0.5}) {
                            DensityFunction.FunctionContext context = new DensityFunction.SinglePointContext(x, y, z);
                            BlockState expected = asking.computeSubstance(context, substance);
                            boolean expectedFlag = asking.shouldScheduleFluidUpdate();
                            BlockState actual = profiled.computeSubstance(context, substance);
                            if ((expected != actual || expectedFlag != profiled.shouldScheduleFluidUpdate()) && faults.size() < 20) {
                                faults.add(x + " " + y + " " + z + " " + rings.at(x, z) + ": " + expected + " vs " + actual);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(faults.isEmpty(), String.join(System.lineSeparator(), faults));
    }

    private static byte[] paint(RingAquifer aquifer, RingAquifer.Rules r, int[] at, int baseY, int n, boolean carving) {
        aquifer.carving(carving);
        byte[] out = new byte[n];
        aquifer.paint(r, at[0], at[1], baseY, out, n);
        return out;
    }

    private static void check(List<String> faults, int c, RingAquifer.Rules r, int[] at, int y, boolean carving, int painted) {
        int expected = chain(r, at[0], y, at[1], carving);
        if (painted != expected && faults.size() < 20) {
            faults.add("combination " + c + " y " + y + " carving " + carving + ": painted " + painted + ", chain " + expected);
        }
    }

    /** The old chain of tests per block (51d3aade's {@code RingAquifer.openSpace}), as a profile entry. */
    private static int chain(RingAquifer.Rules r, int x, int y, int z, boolean carving) {
        if (r.noFluid) {
            return TO_AIR;
        }
        if (y < r.sealBelow) {
            return ROCK;
        }
        if (y >= r.airFrom || y < r.dryBelow) {
            return TO_AIR;
        }
        if (y == r.springY) {
            return TO_LAVA | SPRING;
        }
        if (y <= r.fallTop && y >= r.fallBottom) {
            return TO_FALLING_LAVA | (y <= r.fallFlow ? SPRING : 0);
        }
        if (y <= r.waterFallTop && y >= r.waterFallBottom) {
            return TO_FALLING_WATER | (y <= r.waterFallFlow ? SPRING : 0);
        }
        if (y == r.waterSpringY) {
            return TO_WATER | SPRING;
        }
        if (y < r.skyTop && y > r.skyFloor) {
            return TO_WATER;
        }
        if (carving && y >= r.skyGuardFrom && y <= r.skyGuardTo) {
            return ROCK;
        }
        if (carving && y >= r.lakeGuardBottom && y <= r.lakeGuardTop) {
            return ROCK;
        }
        if (y >= r.openFrom && y <= r.openTo) {
            return TO_AIR;
        }
        if (y >= r.lakeGuardBottom && y <= r.lakeGuardTop && y < r.lakeFloor) {
            return ROCK;
        }
        if (y < r.lakeTop && y >= r.lakeFloor) {
            return TO_WATER;
        }
        if (r.tunnels != null) {
            if (carving && r.tunnels.lifted(y)) {
                return ROCK;
            }
            Ring4Tunnels.Fill fill = r.tunnels.fillAt(y);
            if (fill != Ring4Tunnels.Fill.NONE) {
                return switch (fill) {
                    case WATER -> TO_WATER;
                    case GAS -> TO_GAS;
                    case POOL -> TO_POOL;
                    default -> TO_AIR;
                };
            }
        }
        if (r.flooded) {
            return y == TOP ? ROCK : y <= Ring4Tunnels.SEA_TOP ? TO_WATER : TO_AIR;
        }
        if (carving && y >= r.guardBottom && y <= r.guardTop) {
            return ROCK;
        }
        if (y < r.lavaTop && y >= r.lavaBottom) {
            return LAVA_UNLESS_EXPOSED;
        }
        if (y >= r.caveBottom && y <= r.caveTop) {
            VolcanicField.Column column = FIELD.column(x, z);
            if (column.voidAt(y)) {
                return column.subLavaAt(y) ? LAVA_UNLESS_OPEN : TO_AIR;
            }
            if (column.deepGuardAt(y)) {
                return ROCK;
            }
        }
        int rule = after(r, y, null) == ROCK ? 1 : 0;
        rule |= after(r, y, Blocks.WATER) << 1 | after(r, y, Blocks.LAVA) << 3;
        return ASK + rule;
    }

    /** The old rules on vanilla's answer (air: null fluid), as an outcome. */
    private static int after(RingAquifer.Rules r, int y, Block fluid) {
        if (r.dryFrom != Integer.MAX_VALUE && y == TOP) {
            return ROCK;
        }
        if (y >= r.dryFrom && fluid == Blocks.WATER) {
            return TO_AIR;
        }
        if (r.dryFrom != Integer.MAX_VALUE && y >= TOP && fluid == Blocks.LAVA) {
            return TO_AIR;
        }
        if (y < r.waterOnlyFrom || fluid == null) {
            return KEEP;
        }
        return y == TOP ? ROCK : TO_WATER;
    }

    private static RingAquifer.Rules rules(Random random, List<Ring4Tunnels.Span> spans, int x, int z) {
        RingAquifer.Rules r = new RingAquifer.Rules();
        r.noFluid = random.nextInt(30) == 0;
        r.sealBelow = pick(random, Integer.MIN_VALUE, TOP, RingFluidPicker.SEA_LEVEL);
        r.dryBelow = pick(random, Integer.MIN_VALUE, TOP);
        r.airFrom = pick(random, Integer.MAX_VALUE, TOP, Integer.MIN_VALUE);
        r.springY = pick(random, Integer.MIN_VALUE);
        int[] fall = range(random);
        r.fallBottom = fall[0];
        r.fallTop = fall[1];
        r.fallFlow = fall[2];
        int[] waterFall = range(random);
        r.waterFallBottom = waterFall[0];
        r.waterFallTop = waterFall[1];
        r.waterFallFlow = waterFall[2];
        r.waterSpringY = pick(random, Integer.MIN_VALUE);
        r.skyFloor = pick(random, Integer.MIN_VALUE);
        r.skyTop = r.skyFloor == Integer.MIN_VALUE ? Integer.MIN_VALUE : r.skyFloor + random.nextInt(12);
        r.skyGuardFrom = pick(random, Integer.MAX_VALUE);
        r.skyGuardTo = r.skyGuardFrom == Integer.MAX_VALUE ? Integer.MIN_VALUE : r.skyGuardFrom + random.nextInt(30);
        r.lakeTop = pick(random, Integer.MIN_VALUE);
        r.lakeGuardBottom = pick(random, Integer.MAX_VALUE);
        r.lakeGuardTop = r.lakeGuardBottom == Integer.MAX_VALUE ? Integer.MIN_VALUE : r.lakeGuardBottom + random.nextInt(40);
        r.lakeFloor = r.lakeGuardBottom == Integer.MAX_VALUE ? Integer.MAX_VALUE : r.lakeGuardBottom + random.nextInt(40);
        r.openFrom = pick(random, Integer.MAX_VALUE);
        r.openTo = r.openFrom == Integer.MAX_VALUE ? Integer.MIN_VALUE : r.openFrom + random.nextInt(20);
        r.flooded = random.nextInt(5) == 0;
        r.tunnels = random.nextInt(3) == 0 ? spans.get(random.nextInt(spans.size())) : null;
        r.lavaBottom = pick(random, Integer.MIN_VALUE);
        r.lavaTop = r.lavaBottom == Integer.MIN_VALUE ? Integer.MIN_VALUE : r.lavaBottom + random.nextInt(30);
        r.guardBottom = pick(random, Integer.MAX_VALUE);
        r.guardTop = r.guardBottom == Integer.MAX_VALUE ? Integer.MIN_VALUE : r.guardBottom + random.nextInt(40);
        VolcanicField.Column column = FIELD.column(x, z);
        boolean caves = random.nextInt(3) != 0;
        r.caveBottom = caves ? Math.min(column.voidMin(), column.deepGuardMin()) : Integer.MAX_VALUE;
        r.caveTop = caves ? Math.max(column.voidMax(), column.deepGuardMax()) : Integer.MIN_VALUE;
        r.dryFrom = pick(random, Integer.MAX_VALUE, RingFluidPicker.RING_3_DRY_FROM);
        r.waterOnlyFrom = pick(random, Integer.MAX_VALUE, TOP);
        return r;
    }

    /** One of the given values (a rule's "none" first) or a random height, also just outside the profile. */
    private static int pick(Random random, int... values) {
        int k = random.nextInt(values.length + 2);
        return k < values.length ? values[k] : MIN_Y - 8 + random.nextInt(HEIGHT + 16);
    }

    /** A fall's bottom, top and flow-below (all none or random, also crossed). */
    private static int[] range(Random random) {
        if (random.nextInt(3) == 0) {
            int bottom = pick(random);
            return new int[]{bottom, bottom + random.nextInt(40) - 5, bottom + random.nextInt(10) - 2};
        }
        return new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
    }

    /** Tunnel spans of ring 4's net (rooms, domes, sinkholes). */
    private static List<Ring4Tunnels.Span> spans() {
        List<Ring4Tunnels.Span> spans = new ArrayList<>();
        for (IslandField.TunnelRoom room : ISLANDS.tunnelRoomsWithin(6200, -1400, 7800, 1400)) {
            Ring4Tunnels.Span span = ISLANDS.tunnelSpan((int) Math.floor(room.x()), (int) Math.floor(room.z()));
            if (span != null) {
                spans.add(span);
            }
            if (spans.size() == 8) {
                break;
            }
        }
        return spans;
    }

    /** Ring 3 columns with openings (tubes, chambers, geodes) and their guard bands. */
    private static List<int[]> caveColumns() {
        List<int[]> columns = new ArrayList<>();
        for (int x = -5800; x <= -4000 && columns.size() < 12; x += 16) {
            for (int z = -2000; z <= 2000 && columns.size() < 12; z += 16) {
                VolcanicField.Column c = FIELD.column(x, z);
                if (c.subLavaMax() != Integer.MIN_VALUE || c.voidMax() >= c.voidMin()) {
                    columns.add(new int[]{x, z});
                }
            }
        }
        return columns;
    }
}
