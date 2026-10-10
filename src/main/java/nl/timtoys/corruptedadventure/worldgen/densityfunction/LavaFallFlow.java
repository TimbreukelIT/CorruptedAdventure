package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.worldgen.chunk.RingFluidPicker;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Where the lava of a fall (39f) may flow once it lands (47e): a pure trace over the terrain, so the ash drifts stay
 * out of it and a fall that may reach water is not made at all. Flowing lava destroys the loose blocks it reaches, and
 * lava meeting water turns to obsidian, at the moment it flows: the world would depend on when its chunks were loaded.
 * <p>
 * Vanilla's rule, made generous: the bottom of a fall is a falling block (amount 8); lava spreads to the four sides with
 * 2 less per block (the overworld) while it lies on something, and falls (amount 8 again) where open space lies below.
 * The trace spreads to every side (vanilla prefers the way down within 2 blocks, so it reaches less) and takes the
 * ground one block lower than the terrain's interpolated surface, so it rather reaches too far than too short.
 * <p>
 * The columns are computed into the trace's own cache, not the thread's column window: a caller may hold a column of
 * that window (the aquifer asks while it loads one).
 */
public final class LavaFallFlow {
    /** How far (blocks, per axis) a flow is followed from its fall; a flow is cut off there. */
    static final int REACH = 64;
    private static final int MAX_NODES = 8_000;
    /** The amount of a falling block, and how much less each block of spreading leaves (lava in the overworld). */
    private static final int FALLING = 8;
    private static final int DROP_OFF = 2;
    private static final int CELL = 4;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    static final Flow EMPTY = new Flow(new long[0], false, false, 0, 0, 0, -1, -1);

    private LavaFallFlow() {
    }

    /**
     * The columns a fall's lava may reach (sorted keys {@code x << 32 | z}), whether it may reach water ({@code wet}:
     * below {@link RingFluidPicker#RING_3_DRY_FROM}, or out of ring 3 proper into a transition zone), whether the trace
     * was cut off ({@link #REACH}, {@link #MAX_NODES}), how many states it visited, and the bounds of its columns.
     */
    public record Flow(long[] columns, boolean wet, boolean cut, int nodes, int minX, int minZ, int maxX, int maxZ) {
        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ && Arrays.binarySearch(columns, key(x, z)) >= 0;
        }

        public boolean isEmpty() {
            return columns.length == 0;
        }

        /** Whether the fall is made: its lava surely stays dry (a cut-off trace may reach water past its end). */
        public boolean made() {
            return !wet && !cut;
        }
    }

    static long key(int x, int z) {
        return (long) x << 32 | z & 0xFFFFFFFFL;
    }

    /** The flow of {@code fall}: empty if its column holds no fall of its own (blocked, or its island is not made). */
    static Flow trace(VolcanicField field, Volcano.LavaFall fall) {
        Ground ground = new Ground(field);
        int fx = fall.fallX();
        int fz = fall.fallZ();
        VolcanicField.Column column = ground.column(fx, fz);
        if (column.fallTop() == Integer.MIN_VALUE || column.fall() == null) {
            return EMPTY;
        }
        int top = column.fallTop();
        // The lowest falling block lies in the first open block from where the fall lands.
        int y = column.fallBottom();
        while (y <= top && !ground.open(fx, y, fz)) {
            y++;
        }
        if (y > top) {
            return EMPTY;
        }
        y = ground.landing(fx, y, fz);
        Map<Long, Integer> best = new HashMap<>();
        TreeSet<Long> reached = new TreeSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{fx, y, fz, FALLING});
        reached.add(key(fx, fz));
        boolean wet = false;
        boolean cut = false;
        int nodes = 0;
        while (!queue.isEmpty()) {
            int[] n = queue.poll();
            wet |= n[1] < RingFluidPicker.RING_3_DRY_FROM || !field.ring3Proper(n[0] + 0.5, n[2] + 0.5);
            if (++nodes > MAX_NODES) {
                cut = true;
                break;
            }
            int amount = n[3] - DROP_OFF;
            if (amount <= 0) {
                continue;
            }
            for (int[] side : SIDES) {
                int x = n[0] + side[0];
                int z = n[2] + side[1];
                if (Math.abs(x - fx) > REACH || Math.abs(z - fz) > REACH) {
                    cut = true;
                    continue;
                }
                if (!ground.open(x, n[1], z)) {
                    continue;
                }
                int land = ground.landing(x, n[1], z);
                int next = land < n[1] ? FALLING : amount;
                long state = (long) (x - fx + 2 * REACH) << 32 | (long) (z - fz + 2 * REACH) << 16 | (land + 64);
                Integer known = best.get(state);
                if (known != null && known >= next) {
                    continue;
                }
                best.put(state, next);
                reached.add(key(x, z));
                queue.add(new int[]{x, land, z, next});
            }
        }
        long[] columns = new long[reached.size()];
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int k = 0;
        for (long c : reached) {
            columns[k++] = c;
            int x = (int) (c >> 32);
            int z = (int) c;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        return new Flow(columns, wet, cut, nodes, minX, minZ, maxX, maxZ);
    }

    /** Open space as the terrain makes it, generously: the ground one block lower than its interpolated surface. */
    private static final class Ground {
        private final VolcanicField field;
        private final Map<Long, VolcanicField.Column> columns = new HashMap<>();
        private final Map<Long, Double> surfaces = new HashMap<>();

        Ground(VolcanicField field) {
            this.field = field;
        }

        VolcanicField.Column column(int x, int z) {
            return columns.computeIfAbsent(key(x, z), k -> {
                VolcanicField.Column c = new VolcanicField.Column();
                field.fill(c, x, z, VolcanicField.Mode.FULL);
                return c;
            });
        }

        /** Whether lava may stand in block (x, y, z): no island or thin solid, no terrain lava, an opening or above ground. */
        boolean open(int x, int y, int z) {
            VolcanicField.Column c = column(x, z);
            if (c.islandAt(y) || c.thinSolidAt(y)) {
                return false;
            }
            if (c.voidAt(y)) {
                return !c.subLavaAt(y);
            }
            // Above ground; the terrain's own lava (a lake, a river) carries the flow like rock.
            return y >= surface(x, z) - 1.0 && !(y < c.lavaTop() && y >= c.lavaBottom());
        }

        /** Where lava at (x, y, z) comes to rest falling: the lowest open block straight down from y. */
        int landing(int x, int y, int z) {
            while (y > -64 && open(x, y - 1, z)) {
                y--;
            }
            return y;
        }

        /** The terrain's surface: the blend of its cell corners (the noise chunk interpolates every 4 blocks). */
        private double surface(int x, int z) {
            return surfaces.computeIfAbsent(key(x, z), k -> blend(x, z));
        }

        private double blend(int x, int z) {
            int x0 = Math.floorDiv(x, CELL) * CELL;
            int z0 = Math.floorDiv(z, CELL) * CELL;
            double fx = (x - x0) / (double) CELL;
            double fz = (z - z0) / (double) CELL;
            double h00 = column(x0, z0).height();
            double h10 = column(x0 + CELL, z0).height();
            double h01 = column(x0, z0 + CELL).height();
            double h11 = column(x0 + CELL, z0 + CELL).height();
            return Mth.lerp(fz, Mth.lerp(fx, h00, h10), Mth.lerp(fx, h01, h11));
        }
    }
}
