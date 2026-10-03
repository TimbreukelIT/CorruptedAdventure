package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The pure core of {@code outcrop}, without world access: a rock of a few tapering knobs (salt chimneys, dune rocks).
 * <ol>
 *     <li>{@code knobs} knobs around the origin: the first in the middle with the full {@code height}, the others up to
 *     {@code spread} away with 0.4–0.8 of it. A knob is a cone that keeps {@code taper} of its base radius at the top:
 *     a column at distance {@code d} from its centre rises {@code height · (1 − d / r) / (1 − taper)} (at most the full
 *     height) above the ground at the knob's centre.</li>
 *     <li>The shape shifts into the origin's chunk until every column lies at least one block from its edge; if it is
 *     too wide it shrinks (radii and spread) and is tried again, else there is no rock.</li>
 *     <li>Every column fills from its own ground ({@code sink} blocks buried) up to its top, so it stands on its own
 *     ground, also on a slope, and never floats. Where the ground lies above the top the column stays buried.</li>
 *     <li>With {@code overhang} one knob gets a collar at its top: 1–2 thick, 1–2 wider than the knob, to one side.
 *     It grows outward block by block from the knob, so each collar block is held by a solid neighbour; it counts only
 *     if at least {@link #MIN_ROOFED} columns get a roof (shade).</li>
 * </ol>
 * The rock needs usable ground in every column of its footprint (all or nothing): no water or pool rim.
 */
final class OutcropShape {
    static final int MIN_ROOFED = 3;
    /** Palette layers counted from a rock's lowest block; higher blocks keep the last band. */
    static final int MAX_LAYERS = 64;
    /** Shape columns lie in [chunk + 1, chunk + 14]. */
    private static final int LOW = 1;
    private static final int HIGH = 14;
    private static final float SHRINK = 0.85F;
    private static final int SHRINK_STEPS = 4;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private OutcropShape() {
    }

    /** What the shape reads of the world. */
    interface Terrain {
        /** Y of the ground block of the column (the rock's footing), or {@link Integer#MIN_VALUE} if unusable. */
        int ground(int x, int z);

        /** Whether a rock block may go here (air or a replaceable plant). */
        boolean free(int x, int y, int z);
    }

    /**
     * @param knobs    number of knobs
     * @param spread   how far the other knobs lie from the first
     * @param radius   per knob its base radius (drawn {@code knobs} times)
     * @param height   height of the highest knob
     * @param taper    the fraction of the base radius a knob keeps at its top
     * @param sink     ground blocks per column that become rock (half buried)
     * @param overhang whether one knob gets a collar
     */
    record Params(int knobs, float spread, float[] radius, int height, float taper, int sink, boolean overhang) {
    }

    record Knob(double x, double z, float radius, int height) {
    }

    /**
     * @param blocks     the rock blocks, per column from the bottom up, then the collar
     * @param tops       above each knob's centre column, if free: the spot for a top decoration
     * @param roofed     columns with a collar block above the rock and the ground below (in the shade)
     */
    record Rock(List<BlockPos> blocks, List<BlockPos> tops, int roofed) {
    }

    /** The rock near {@code originX, originZ} in the chunk at {@code minX, minZ}, if it fits. */
    static Optional<Rock> build(Terrain terrain, int minX, int minZ, int originX, int originZ, Params params, RandomSource random) {
        List<Knob> knobs = knobs(params, random);
        double collarAngle = random.nextDouble() * Mth.TWO_PI;
        int collarWidth = 1 + random.nextInt(2);
        int collarThickness = 1 + random.nextInt(2);
        float scale = 1.0F;
        for (int step = 0; step <= SHRINK_STEPS; step++, scale *= SHRINK) {
            List<Knob> scaled = scale(knobs, scale);
            Map<Long, Integer> heights = heights(scaled, params.taper());
            // The collar may reach collarWidth + 1 beyond the knob's columns.
            int[] box = box(heights.keySet(), params.overhang() ? collarWidth + 1 : 0);
            if (box[1] - box[0] > HIGH - LOW || box[3] - box[2] > HIGH - LOW) {
                continue;
            }
            int cx = minX + Mth.clamp(originX - minX, LOW - box[0], HIGH - box[1]);
            int cz = minZ + Mth.clamp(originZ - minZ, LOW - box[2], HIGH - box[3]);
            return rock(terrain, minX, minZ, cx, cz, scaled, heights, params, collarAngle, collarWidth, collarThickness);
        }
        return Optional.empty();
    }

    static List<Knob> knobs(Params params, RandomSource random) {
        List<Knob> knobs = new ArrayList<>();
        for (int i = 0; i < params.knobs(); i++) {
            if (i == 0) {
                knobs.add(new Knob(0.0, 0.0, params.radius()[0], params.height()));
            } else {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double distance = 1.0 + random.nextDouble() * Math.max(0.0, params.spread() - 1.0);
                int height = Math.max(1, Math.round(params.height() * (0.4F + 0.4F * random.nextFloat())));
                knobs.add(new Knob(Math.cos(angle) * distance, Math.sin(angle) * distance, params.radius()[i], height));
            }
        }
        return knobs;
    }

    private static List<Knob> scale(List<Knob> knobs, float scale) {
        return knobs.stream().map(k -> new Knob(k.x() * scale, k.z() * scale, Math.max(0.5F, k.radius() * scale), k.height())).toList();
    }

    /** Per column offset (packed) its height above the knob base, highest knob wins; only columns with height ≥ 1. */
    static Map<Long, Integer> heights(List<Knob> knobs, float taper) {
        Map<Long, Integer> heights = new HashMap<>();
        for (Knob knob : knobs) {
            int reach = Mth.ceil(knob.radius()) + 1;
            int kx = (int) Math.round(knob.x());
            int kz = (int) Math.round(knob.z());
            for (int dx = kx - reach; dx <= kx + reach; dx++) {
                for (int dz = kz - reach; dz <= kz + reach; dz++) {
                    int h = columnHeight(knob, dx, dz, taper);
                    if (h >= 1) {
                        heights.merge(PoolLayout.pack(dx, dz), h, Math::max);
                    }
                }
            }
        }
        return heights;
    }

    /** The height of a knob over column {@code dx, dz} (0 outside it). */
    static int columnHeight(Knob knob, int dx, int dz, float taper) {
        double d = Math.sqrt(sq(dx - knob.x()) + sq(dz - knob.z()));
        if (d > knob.radius()) {
            return 0;
        }
        double t = Math.min(1.0, (1.0 - d / knob.radius()) / (1.0 - taper));
        return (int) Math.round(knob.height() * t);
    }

    private static Optional<Rock> rock(Terrain terrain, int minX, int minZ, int cx, int cz, List<Knob> knobs, Map<Long, Integer> heights, Params params,
                                       double collarAngle, int collarWidth, int collarThickness) {
        // The base of each knob is the ground at its centre column; a column's top is the highest knob over it.
        int[] base = new int[knobs.size()];
        for (int i = 0; i < knobs.size(); i++) {
            Knob knob = knobs.get(i);
            base[i] = terrain.ground(cx + (int) Math.round(knob.x()), cz + (int) Math.round(knob.z()));
            if (base[i] == Integer.MIN_VALUE) {
                return Optional.empty();
            }
        }
        Map<Long, Integer> tops = new HashMap<>();
        Map<Long, Integer> grounds = new HashMap<>();
        for (long c : sorted(heights.keySet())) {
            int ground = terrain.ground(cx + PoolLayout.x(c), cz + PoolLayout.z(c));
            if (ground == Integer.MIN_VALUE) {
                return Optional.empty();
            }
            grounds.put(c, ground);
            int top = Integer.MIN_VALUE;
            for (int i = 0; i < knobs.size(); i++) {
                int h = columnHeight(knobs.get(i), PoolLayout.x(c), PoolLayout.z(c), params.taper());
                if (h >= 1) {
                    top = Math.max(top, base[i] + h);
                }
            }
            tops.put(c, top);
        }
        List<BlockPos> blocks = new ArrayList<>();
        Set<BlockPos> solid = new HashSet<>();
        Map<Long, Integer> filledTop = new HashMap<>();
        for (long c : sorted(heights.keySet())) {
            int x = cx + PoolLayout.x(c);
            int z = cz + PoolLayout.z(c);
            int ground = grounds.get(c);
            int top = tops.get(c);
            int highest = Integer.MIN_VALUE;
            if (top > ground) {
                for (int y = ground - params.sink() + 1; y <= top; y++) {
                    if (y > ground && !terrain.free(x, y, z)) {
                        break;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    blocks.add(pos);
                    solid.add(pos);
                    highest = y;
                }
            }
            filledTop.put(c, Math.max(highest, ground));
        }
        int roofed = 0;
        if (params.overhang()) {
            roofed = collar(terrain, minX, minZ, cx, cz, knobs, base, filledTop, grounds, collarAngle, collarWidth, collarThickness, blocks, solid);
        }
        List<BlockPos> decorations = new ArrayList<>();
        for (int i = 0; i < knobs.size(); i++) {
            long c = PoolLayout.pack((int) Math.round(knobs.get(i).x()), (int) Math.round(knobs.get(i).z()));
            Integer top = filledTop.get(c);
            if (top != null && top > grounds.get(c)) {
                BlockPos spot = new BlockPos(cx + PoolLayout.x(c), top + 1, cz + PoolLayout.z(c));
                if (!solid.contains(spot) && terrain.free(spot.getX(), spot.getY(), spot.getZ())) {
                    decorations.add(spot);
                }
            }
        }
        return blocks.isEmpty() ? Optional.empty() : Optional.of(new Rock(blocks, decorations, roofed));
    }

    /**
     * A collar at the top of the highest knob: from the knob's solid blocks at that level outward, one ring per step,
     * within the knob radius + {@code width} and within 70° of {@code angle}. Returns the number of roofed columns, or
     * 0 (and no collar) if fewer than {@link #MIN_ROOFED}.
     */
    private static int collar(Terrain terrain, int minX, int minZ, int cx, int cz, List<Knob> knobs, int[] base, Map<Long, Integer> filledTop,
                              Map<Long, Integer> grounds, double angle, int width, int thickness, List<BlockPos> blocks,
                              Set<BlockPos> solid) {
        int k = 0;
        for (int i = 1; i < knobs.size(); i++) {
            if (base[i] + knobs.get(i).height() > base[k] + knobs.get(k).height()) {
                k = i;
            }
        }
        Knob knob = knobs.get(k);
        int top = base[k] + knob.height();
        double kx = cx + knob.x();
        double kz = cz + knob.z();
        double reach = knob.radius() + width;
        List<BlockPos> added = new ArrayList<>();
        for (int y = top; y > top - thickness; y--) {
            // Seeds: the knob's own blocks at this level, in a fixed order.
            List<BlockPos> seeds = new ArrayList<>();
            for (BlockPos pos : solid) {
                if (pos.getY() == y && Math.sqrt(sq(pos.getX() - kx) + sq(pos.getZ() - kz)) <= knob.radius() + 0.5) {
                    seeds.add(pos);
                }
            }
            seeds.sort(BlockPos::compareTo);
            ArrayDeque<BlockPos> queue = new ArrayDeque<>(seeds);
            Set<BlockPos> seen = new HashSet<>(seeds);
            while (!queue.isEmpty()) {
                BlockPos from = queue.poll();
                for (int[] d : SIDES) {
                    BlockPos next = from.offset(d[0], 0, d[1]);
                    if (seen.contains(next) || solid.contains(next)) {
                        continue;
                    }
                    seen.add(next);
                    double dx = next.getX() + 0.5 - (kx + 0.5);
                    double dz = next.getZ() + 0.5 - (kz + 0.5);
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    double off = Math.abs(Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx) - angle)));
                    int localX = next.getX() - minX;
                    int localZ = next.getZ() - minZ;
                    boolean inChunk = localX >= LOW && localX <= HIGH && localZ >= LOW && localZ <= HIGH;
                    Integer ground = grounds.get(PoolLayout.pack(next.getX() - cx, next.getZ() - cz));
                    int groundY = ground != null ? ground : terrain.ground(next.getX(), next.getZ());
                    if (distance > reach || off > 70.0 || !inChunk || groundY == Integer.MIN_VALUE || groundY >= y - 1
                            || !terrain.free(next.getX(), y, next.getZ())) {
                        continue;
                    }
                    added.add(next);
                    solid.add(next);
                    queue.add(next);
                }
            }
        }
        // Roofed: a collar block above the column's rock (or ground) with at least one free block between.
        Set<Long> roofedColumns = new HashSet<>();
        for (BlockPos pos : added) {
            long c = PoolLayout.pack(pos.getX() - cx, pos.getZ() - cz);
            Integer below = filledTop.get(c);
            int under = below != null ? below : terrain.ground(pos.getX(), pos.getZ());
            if (!solid.contains(pos.below()) && pos.getY() - 1 > under) {
                roofedColumns.add(c);
            }
        }
        if (roofedColumns.size() < MIN_ROOFED) {
            solid.removeAll(added);
            return 0;
        }
        blocks.addAll(added);
        return roofedColumns.size();
    }

    /** min x, max x, min z, max z of the offsets, grown by {@code margin}. */
    private static int[] box(Set<Long> columns, int margin) {
        int[] box = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (long c : columns) {
            box[0] = Math.min(box[0], PoolLayout.x(c) - margin);
            box[1] = Math.max(box[1], PoolLayout.x(c) + margin);
            box[2] = Math.min(box[2], PoolLayout.z(c) - margin);
            box[3] = Math.max(box[3], PoolLayout.z(c) + margin);
        }
        return box;
    }

    private static List<Long> sorted(Set<Long> columns) {
        List<Long> list = new ArrayList<>(columns);
        list.sort(Long::compare);
        return list;
    }

    private static double sq(double v) {
        return v * v;
    }
}
