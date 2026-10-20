package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The shape of a giant moonpetal ({@link GiantMoonpetalFeature}, decor rollout C): a 2 × 2 trunk, root flares at its
 * foot, 8–10 boughs that arch outwards from its top with a fork each, and a broad, flat crown of blossom blobs on the
 * bough ends with a fringe hanging from its rim, so the crown covers its island. Pure in the random (every draw is made
 * in a fixed order) and relative to the trunk's corner (0, 0, 0), the first block above the ground; the trunk takes x, z
 * in {0, 1}.
 * <p>
 * Every leaf lies within {@link #MAX_LEAF_DISTANCE} steps (through leaves) of the wood, so no leaf of the crown decays.
 *
 * @param trunk  the logs of the trunk (upright)
 * @param wood   roots and boughs (bark on every side); a root block means "this column, from here down to the ground"
 * @param roots  the root blocks of {@code wood} (they reach down to the ground)
 * @param leaves the crown
 */
record GiantMoonpetalShape(Set<BlockPos> trunk, Set<BlockPos> wood, Set<BlockPos> roots, Set<BlockPos> leaves) {
    /** Vanilla's leaves decay at distance 7. */
    static final int MAX_LEAF_DISTANCE = 6;
    static final int MIN_CROWN = 9;
    static final int MAX_CROWN = 13;
    /** The middle of the 2 × 2 trunk. */
    private static final double CENTRE = 1.0;

    /** A crown of {@code crown} blocks around the trunk (clamped to {@link #MIN_CROWN} … {@link #MAX_CROWN}). */
    static GiantMoonpetalShape grow(RandomSource random, int crown) {
        int radius = Mth.clamp(crown, MIN_CROWN, MAX_CROWN);
        int height = 8 + random.nextInt(3) + (radius - MIN_CROWN) / 3;
        Set<BlockPos> trunk = new LinkedHashSet<>();
        Set<BlockPos> wood = new LinkedHashSet<>();
        Set<BlockPos> roots = new LinkedHashSet<>();
        Map<BlockPos, Boolean> leaves = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    trunk.add(new BlockPos(x, y, z));
                }
            }
        }
        // Root flares: a fin stepping down outwards from each side of the trunk, now and then a second one.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int fins = 1 + (random.nextFloat() < 0.4F ? 1 : 0);
            int along = random.nextInt(2);
            int length = 2 + random.nextInt(3);
            for (int f = 0; f < fins; f++) {
                int lane = (along + f) % 2;
                for (int k = 1; k <= length; k++) {
                    int x = side.getStepX() == 0 ? lane : side.getStepX() > 0 ? 1 + k : -k;
                    int z = side.getStepZ() == 0 ? lane : side.getStepZ() > 0 ? 1 + k : -k;
                    BlockPos root = new BlockPos(x, Math.max(0, 2 - k + (f == 0 ? 1 : 0)), z);
                    wood.add(root);
                    roots.add(root);
                }
            }
        }
        // The top: the trunk goes on a little as one log column, under the crown's middle blob.
        int top = height + 1 + random.nextInt(2);
        int topX = random.nextInt(2);
        int topZ = random.nextInt(2);
        for (int y = height; y <= top; y++) {
            trunk.add(new BlockPos(topX, y, topZ));
        }
        blob(leaves, random, CENTRE, top + 1, CENTRE, 5.5, 2.5);
        // Boughs: from the upper trunk outwards and up, each with a fork, a blossom blob at every end and halfway.
        int boughs = 8 + random.nextInt(3);
        double turn = random.nextDouble() * Math.PI * 2.0;
        for (int b = 0; b < boughs; b++) {
            double angle = turn + b * Math.PI * 2.0 / boughs + (random.nextDouble() - 0.5) * 0.5;
            int start = height - 4 + random.nextInt(4);
            double length = radius - 4.0 + random.nextDouble() * 1.5;
            double rise = 2.0 + random.nextInt(3);
            double forkTurn = (random.nextBoolean() ? 1.0 : -1.0) * (0.5 + random.nextDouble() * 0.3);
            double forkLength = 3.0 + random.nextDouble() * 2.0;
            double[] end = bough(wood, CENTRE, start, CENTRE, angle, length, rise);
            double[] mid = point(CENTRE, start, CENTRE, angle, length, rise, 0.6);
            double[] fork = bough(wood, mid[0], mid[1], mid[2], angle + forkTurn, forkLength, 1.5);
            blob(leaves, random, end[0], end[1] + 1, end[2], 4.5 + random.nextDouble(), 2.0);
            blob(leaves, random, fork[0], fork[1] + 1, fork[2], 3.5 + random.nextDouble(), 2.0);
            double[] half = point(CENTRE, start, CENTRE, angle, length, rise, 0.45);
            blob(leaves, random, half[0], half[1] + 1, half[2], 4.0, 2.0);
        }
        // The fringe: from the crown's underside in its outer part, blossom hangs down one to three blocks.
        Set<BlockPos> candidates = new LinkedHashSet<>(leaves.keySet().stream().sorted().toList());
        for (BlockPos leaf : candidates) {
            float roll = random.nextFloat();
            int drop = 1 + random.nextInt(4);
            double out = Math.hypot(leaf.getX() + 0.5 - CENTRE, leaf.getZ() + 0.5 - CENTRE);
            if (roll < 0.3F && out > radius * 0.4 && !leaves.containsKey(leaf.below())) {
                for (int d = 1; d <= drop; d++) {
                    leaves.putIfAbsent(leaf.below(d), Boolean.TRUE);
                }
            }
        }
        Set<BlockPos> solid = new LinkedHashSet<>(trunk);
        solid.addAll(wood);
        Set<BlockPos> kept = reachable(solid, leaves.keySet());
        return new GiantMoonpetalShape(trunk, wood, roots, kept);
    }

    /** A point along a bough at {@code t} (0 … 1): straight out, rising fast and then levelling off. */
    private static double[] point(double x, double y, double z, double angle, double length, double rise, double t) {
        return new double[]{x + Math.cos(angle) * length * t, y + rise * (1.0 - (1.0 - t) * (1.0 - t)), z + Math.sin(angle) * length * t};
    }

    /** The wood of a bough (every block it passes, face-connected); returns its end. */
    private static double[] bough(Set<BlockPos> wood, double x, double y, double z, double angle, double length, double rise) {
        BlockPos last = null;
        int steps = (int) Math.ceil(length * 3.0);
        for (int i = 0; i <= steps; i++) {
            double[] p = point(x, y, z, angle, length, rise, i / (double) steps);
            BlockPos pos = BlockPos.containing(p[0], p[1], p[2]);
            if (last != null && !pos.equals(last)) {
                // Keep the wood face-connected: step through one axis at a time.
                BlockPos step = last;
                while (!step.equals(pos)) {
                    if (step.getX() != pos.getX()) {
                        step = step.offset(Integer.signum(pos.getX() - step.getX()), 0, 0);
                    } else if (step.getZ() != pos.getZ()) {
                        step = step.offset(0, 0, Integer.signum(pos.getZ() - step.getZ()));
                    } else {
                        step = step.offset(0, Integer.signum(pos.getY() - step.getY()), 0);
                    }
                    wood.add(step);
                }
            }
            wood.add(pos);
            last = pos;
        }
        return point(x, y, z, angle, length, rise, 1.0);
    }

    /** A flattened blob of leaves (ragged edge), its centre at (x, y, z). */
    private static void blob(Map<BlockPos, Boolean> leaves, RandomSource random, double x, double y, double z, double across,
                             double up) {
        int r = Mth.ceil(across);
        int h = Mth.ceil(up);
        for (int dy = -h; dy <= h; dy++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    float rag = random.nextFloat();
                    BlockPos pos = BlockPos.containing(x + dx, y + dy, z + dz);
                    double fx = pos.getX() + 0.5 - x;
                    double fz = pos.getZ() + 0.5 - z;
                    double fy = pos.getY() + 0.5 - y;
                    // Flatter below the centre than above it: a dome on a flat underside.
                    double vertical = fy < 0 ? fy / (up * 0.6) : fy / up;
                    double d = (fx * fx + fz * fz) / (across * across) + vertical * vertical;
                    if (d <= 1.0 - 0.3 * rag) {
                        leaves.putIfAbsent(pos, Boolean.TRUE);
                    }
                }
            }
        }
    }

    /** The leaves within {@link #MAX_LEAF_DISTANCE} steps of the wood, through leaves only (vanilla's decay distance). */
    static Set<BlockPos> reachable(Set<BlockPos> wood, Set<BlockPos> leaves) {
        Map<BlockPos, Integer> distance = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos log : wood) {
            distance.put(log, 0);
            queue.add(log);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int next = distance.get(pos) + 1;
            if (next > MAX_LEAF_DISTANCE) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = pos.relative(direction);
                if (leaves.contains(neighbour) && !wood.contains(neighbour) && !distance.containsKey(neighbour)) {
                    distance.put(neighbour, next);
                    queue.add(neighbour);
                }
            }
        }
        Set<BlockPos> kept = new LinkedHashSet<>();
        leaves.stream().sorted().filter(leaf -> !wood.contains(leaf) && distance.containsKey(leaf)).forEach(kept::add);
        return kept;
    }
}
