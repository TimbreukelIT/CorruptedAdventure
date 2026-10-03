package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The pure core of {@code surface_pool}, without world access: a small pool of still water in a closed basin, with a
 * rim around it. Geometry in, blocks out.
 * <ol>
 *     <li>The footprint is a ragged disc (8 notches around the circle, linear between them, as {@code bare_patch}); the
 *     rim is the footprint grown by {@code rim_width} columns (8-neighbourhood). The shape shifts into the origin's
 *     chunk until everything lies at least one block from its edge; if it is too wide it shrinks, down to radius
 *     {@link #MIN_RADIUS}, else there is no layout.</li>
 *     <li>The water level {@code L} is the lowest top terrain block of footprint and rim. Every column must have a
 *     {@code ground} top block at most {@code max_step} above {@code L} and solid ground on {@code L}; higher columns
 *     are dug down to {@code L}.</li>
 *     <li>The footprint is water from {@code L - depth + 1} to {@code L} (depth 2 only in the core, the columns whose
 *     eight neighbours all belong to the footprint).</li>
 *     <li>Closed basin (all or nothing): beside every water block on its level and below it there must be water or a
 *     solid block.</li>
 * </ol>
 * The water is only source blocks in a closed basin: vanilla's settled state, so no fluid tick is needed and a block
 * update changes nothing (see "Water in features", change 23c).
 */
final class PoolLayout {
    static final float MAX_RADIUS = 6.0F;
    static final float MIN_RADIUS = 1.5F;
    static final int EDGE_POINTS = 8;
    /** Tree trunks at least this far from the chunk edge (palm rules of change 23c; crowns may reach the neighbours). */
    static final int TREE_EDGE = 4;
    static final int TREE_SPACING = 5;
    /** Shape columns lie in [chunk + 1, chunk + 14]. */
    private static final int LOW = 1;
    private static final int HIGH = 14;
    private static final float SHRINK_STEP = 0.5F;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private PoolLayout() {
    }

    /** What the layout reads of the world. */
    interface Terrain {
        /** Y of the top terrain block of the column ({@code WORLD_SURFACE_WG} − 1). */
        int surface(int x, int z);

        /** Whether this block is one of the pool's {@code ground} blocks. */
        boolean ground(int x, int y, int z);

        /** Whether water beside or above this block stays put: a solid block without fluid. */
        boolean solid(int x, int y, int z);
    }

    /**
     * @param radius   radius before the notches
     * @param edge     per edge point the fraction of the radius it keeps (1 − jitter · random)
     * @param depth    1 or 2
     * @param rimWidth 1 or 2
     * @param maxStep  how far a column may rise above the water level
     */
    record Params(float radius, float[] edge, int depth, int rimWidth, int maxStep) {
    }

    /**
     * @param level       the water level (Y of the top water blocks)
     * @param carve       blocks above the level to dig away (become air)
     * @param water       the water blocks (all sources)
     * @param floor       the block under each water column (decorations on the bottom stand on it)
     * @param rim         the top blocks of the rim columns (Y = level)
     * @param besideWater rim top blocks next to the water (4-neighbourhood)
     * @param outerRim    rim top blocks not next to the water (empty for rim width 1)
     */
    record Layout(int level, List<BlockPos> carve, List<BlockPos> water, List<BlockPos> floor, List<BlockPos> rim,
                  List<BlockPos> besideWater, List<BlockPos> outerRim) {
    }

    /** The layout of a pool near {@code originX, originZ} in the chunk at {@code minX, minZ}, if it fits. */
    static Optional<Layout> build(Terrain terrain, int minX, int minZ, int originX, int originZ, Params params) {
        for (float radius = Math.min(params.radius(), MAX_RADIUS); radius >= MIN_RADIUS; radius -= SHRINK_STEP) {
            Set<Long> footprint = footprint(radius, params.edge());
            if (footprint.isEmpty()) {
                return Optional.empty();
            }
            Set<Long> rim = grow(footprint, params.rimWidth());
            int[] box = box(rim);
            if (box[1] - box[0] > HIGH - LOW || box[3] - box[2] > HIGH - LOW) {
                continue;
            }
            int cx = minX + Mth.clamp(originX - minX, LOW - box[0], HIGH - box[1]);
            int cz = minZ + Mth.clamp(originZ - minZ, LOW - box[2], HIGH - box[3]);
            return layout(terrain, cx, cz, footprint, rim, params);
        }
        return Optional.empty();
    }

    /** Offsets (packed) of the columns inside the ragged disc. */
    static Set<Long> footprint(float radius, float[] edge) {
        Set<Long> columns = new HashSet<>();
        int reach = Mth.ceil(radius);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                float distance = Mth.sqrt(dx * dx + dz * dz);
                if (distance <= radius * edgeAt(edge, (float) Mth.atan2(dz, dx))) {
                    columns.add(pack(dx, dz));
                }
            }
        }
        return columns;
    }

    /** The edge fraction at an angle, linear between the edge points (as {@code bare_patch}). */
    static float edgeAt(float[] edge, float angle) {
        float index = (angle + Mth.PI) / Mth.TWO_PI * edge.length;
        int below = Mth.floor(index);
        return Mth.lerp(index - below, edge[Math.floorMod(below, edge.length)], edge[Math.floorMod(below + 1, edge.length)]);
    }

    /** The columns within {@code width} steps of the footprint (8-neighbourhood), without the footprint. */
    private static Set<Long> grow(Set<Long> footprint, int width) {
        Set<Long> all = new HashSet<>(footprint);
        for (int step = 0; step < width; step++) {
            Set<Long> next = new HashSet<>(all);
            for (long c : all) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        next.add(pack(x(c) + dx, z(c) + dz));
                    }
                }
            }
            all = next;
        }
        all.removeAll(footprint);
        return all;
    }

    /** min x, max x, min z, max z of the offsets. */
    private static int[] box(Set<Long> columns) {
        int[] box = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (long c : columns) {
            box[0] = Math.min(box[0], x(c));
            box[1] = Math.max(box[1], x(c));
            box[2] = Math.min(box[2], z(c));
            box[3] = Math.max(box[3], z(c));
        }
        return box;
    }

    private static Optional<Layout> layout(Terrain terrain, int cx, int cz, Set<Long> footprint, Set<Long> rim, Params params) {
        Set<Long> all = new HashSet<>(footprint);
        all.addAll(rim);
        int level = Integer.MAX_VALUE;
        for (long c : all) {
            level = Math.min(level, terrain.surface(cx + x(c), cz + z(c)));
        }
        for (long c : all) {
            int x = cx + x(c);
            int z = cz + z(c);
            int top = terrain.surface(x, z);
            // Ground on top, at most max_step above the level, and solid ground on the level after digging.
            if (top > level + params.maxStep() || !terrain.ground(x, top, z) || !terrain.solid(x, level, z)) {
                return Optional.empty();
            }
        }
        List<BlockPos> carve = new ArrayList<>();
        for (long c : sorted(all)) {
            int x = cx + x(c);
            int z = cz + z(c);
            for (int y = terrain.surface(x, z); y > level; y--) {
                carve.add(new BlockPos(x, y, z));
            }
        }
        Set<BlockPos> water = new HashSet<>();
        List<BlockPos> waterList = new ArrayList<>();
        List<BlockPos> floor = new ArrayList<>();
        for (long c : sorted(footprint)) {
            int x = cx + x(c);
            int z = cz + z(c);
            int depth = params.depth() >= 2 && core(footprint, c) ? 2 : 1;
            for (int y = level; y > level - depth; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                water.add(pos);
                waterList.add(pos);
            }
            floor.add(new BlockPos(x, level - depth, z));
        }
        if (!closed(terrain, water, all, cx, cz, level)) {
            return Optional.empty();
        }
        List<BlockPos> rimTops = new ArrayList<>();
        List<BlockPos> beside = new ArrayList<>();
        List<BlockPos> outer = new ArrayList<>();
        for (long c : sorted(rim)) {
            BlockPos top = new BlockPos(cx + x(c), level, cz + z(c));
            rimTops.add(top);
            boolean next = false;
            for (int[] d : SIDES) {
                next |= footprint.contains(pack(x(c) + d[0], z(c) + d[1]));
            }
            (next ? beside : outer).add(top);
        }
        return Optional.of(new Layout(level, carve, waterList, floor, rimTops, beside, outer));
    }

    /**
     * Up to {@code count} trunk positions (the block above the rim) on the outer rim, or the rim if it has no outer
     * ring: at least {@link #TREE_EDGE} from the chunk edge, {@link #TREE_SPACING} apart, where {@code spot} holds.
     * Spots are drawn at random; at most 2 · {@code count} draws.
     */
    static List<BlockPos> trunks(Layout layout, int minX, int minZ, int count, RandomSource random, Predicate<BlockPos> spot) {
        List<BlockPos> spots = new ArrayList<>(layout.outerRim().isEmpty() ? layout.rim() : layout.outerRim());
        spots.removeIf(ground -> Math.min(ground.getX() - minX, minX + 15 - ground.getX()) < TREE_EDGE
                || Math.min(ground.getZ() - minZ, minZ + 15 - ground.getZ()) < TREE_EDGE);
        List<BlockPos> trunks = new ArrayList<>();
        for (int draw = 0; draw < 2 * count && trunks.size() < count && !spots.isEmpty(); draw++) {
            BlockPos trunk = spots.remove(random.nextInt(spots.size())).above();
            if (trunks.stream().allMatch(other -> other.distSqr(trunk) >= TREE_SPACING * TREE_SPACING) && spot.test(trunk)) {
                trunks.add(trunk);
            }
        }
        return trunks;
    }

    /** Whether every water block has water or solid ground beside it (on its level) and below it, after digging. */
    private static boolean closed(Terrain terrain, Set<BlockPos> water, Set<Long> shape, int cx, int cz, int level) {
        for (BlockPos pos : water) {
            BlockPos below = pos.below();
            if (!water.contains(below) && !terrain.solid(below.getX(), below.getY(), below.getZ())) {
                return false;
            }
            for (int[] d : SIDES) {
                BlockPos side = pos.offset(d[0], 0, d[1]);
                if (water.contains(side)) {
                    continue;
                }
                // Dug columns are air above the level; nothing else changes on or below it.
                boolean dug = shape.contains(pack(side.getX() - cx, side.getZ() - cz)) && side.getY() > level;
                if (dug || !terrain.solid(side.getX(), side.getY(), side.getZ())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean core(Set<Long> footprint, long c) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!footprint.contains(pack(x(c) + dx, z(c) + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** In a fixed order, so the feature draws its randoms for the same blocks in every run. */
    private static List<Long> sorted(Set<Long> columns) {
        List<Long> list = new ArrayList<>(columns);
        list.sort(Long::compare);
        return list;
    }

    static long pack(int dx, int dz) {
        return ((long) dx << 32) | (dz & 0xFFFF_FFFFL);
    }

    static int x(long packed) {
        return (int) (packed >> 32);
    }

    static int z(long packed) {
        return (int) packed;
    }
}
