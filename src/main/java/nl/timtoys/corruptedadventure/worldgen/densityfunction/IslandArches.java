package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The island pairs of ring 4 and the rock arches between some of them (74, Q8 = A). A pair is two islands whose facing
 * edges lie {@link #MIN_GAP}–{@link #MAX_GAP} blocks apart along the line between their centres, at most
 * {@link #MAX_STEP} apart in height (W-biomes hangs vines between them, 75); each island pairs with its
 * {@link #PARTNERS} nearest islands after it (by x, then z), so a pair is found once. About one in two pairs of two
 * middle or giant islands {@link #ARCH_MIN_GAP}–{@link #ARCH_MAX_GAP} apart, their facing tops close enough for a
 * walkable deck ({@link #ARCH_STEP}, {@link #ARCH_SLOPE}), gets a natural rock arch: a deck rising a little in the
 * middle, thicker and wider where it grows out of each island, rounded underneath. An arch keeps away from other
 * islands, from every fall, spring and lift, from the sky lakes and the world trees. Pure, decided once per island and
 * world.
 */
final class IslandArches {
    static final double MIN_GAP = 3.0;
    static final double MAX_GAP = 40.0;
    static final int MAX_STEP = 40;
    static final int PARTNERS = 3;
    static final double ARCH_CHANCE = 0.5;
    static final double ARCH_MIN_GAP = 6.0;
    static final double ARCH_MAX_GAP = 32.0;
    static final int ARCH_STEP = 12;
    /** The facing tops differ at most this much per block of gap (the deck stays walkable). */
    static final double ARCH_SLOPE = 0.4;
    /** How far (blocks) an arch reaches into each island, so it grows out of it. */
    static final double ANCHOR = 3.0;
    /** The free space (blocks) an arch keeps from falls, springs, lifts and sky lakes, and over and under other islands. */
    private static final int KEEP_CLEAR = 2;
    private static final long SALT = 7403;

    private IslandArches() {
    }

    /** The column of an island's edge facing the other island: its top and bottom block. */
    record Edge(int x, int z, int top, int bottom) {
    }

    /** Two nearby islands, their facing edges, the gap between them (blocks) and the arch between them (or null). */
    record Pair(IslandShape first, IslandShape second, Edge a, Edge b, double gap, @Nullable Arch arch) {
        IslandField.IslandPair view() {
            return new IslandField.IslandPair(first.view(), second.view(), a.x(), a.z(), a.top(), a.bottom(),
                    b.x(), b.z(), b.top(), b.bottom(), arch != null);
        }

        boolean reaches(int minX, int minZ, int maxX, int maxZ) {
            return Math.max(a.x(), b.x()) >= minX && Math.min(a.x(), b.x()) <= maxX
                    && Math.max(a.z(), b.z()) >= minZ && Math.min(a.z(), b.z()) <= maxZ;
        }
    }

    /**
     * A rock arch from edge point (ax, az) to (bx, bz) (column centres), its deck's top block {@code topA} at the one
     * end and {@code topB} at the other, rising {@code rise} in the middle; half width {@code width} and thickness
     * {@code thickness} in the middle, both growing towards the ends; its middle line bent sideways by {@code bend}.
     */
    record Arch(double ax, double az, double bx, double bz, int topA, int topB, double rise, double width, double thickness,
                double bend, int minX, int minZ, int maxX, int maxZ) {
        static Arch between(Edge a, Edge b, long h) {
            double ax = a.x() + 0.5;
            double az = a.z() + 0.5;
            double bx = b.x() + 0.5;
            double bz = b.z() + 0.5;
            double gap = Math.hypot(bx - ax, bz - az);
            double rise = 1.0 + 0.08 * gap * SeededHash.unit(h, 1);
            double width = 1.6 + 0.6 * SeededHash.unit(h, 2);
            double thickness = 2.6 + 1.0 * SeededHash.unit(h, 3);
            double bend = (SeededHash.unit(h, 4) - 0.5) * 3.0;
            double margin = 1.5 * width + Math.abs(bend) + ANCHOR + 2.0;
            return new Arch(ax, az, bx, bz, a.top(), b.top(), rise, width, thickness, bend,
                    Mth.floor(Math.min(ax, bx) - margin), Mth.floor(Math.min(az, bz) - margin),
                    Mth.ceil(Math.max(ax, bx) + margin), Mth.ceil(Math.max(az, bz) + margin));
        }

        boolean touches(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        /** The arch's blocks in column (x, z): {lowest, highest}, or null. */
        @Nullable
        int[] interval(int x, int z) {
            if (!touches(x, z)) {
                return null;
            }
            double dx = bx - ax;
            double dz = bz - az;
            double length = Math.sqrt(dx * dx + dz * dz);
            double ux = dx / length;
            double uz = dz / length;
            double px = x + 0.5 - ax;
            double pz = z + 0.5 - az;
            double t = (px * ux + pz * uz) / length;
            double ext = ANCHOR / length;
            if (t < -ext || t > 1.0 + ext) {
                return null;
            }
            double tt = Mth.clamp(t, 0.0, 1.0);
            double arc = Math.sin(Math.PI * tt);
            double side = -px * uz + pz * ux - bend * arc;
            double half = width * (1.0 + 0.5 * (1.0 - arc));
            double s = Math.abs(side) / half;
            if (s >= 1.0) {
                return null;
            }
            double deck = topA + (topB - topA) * tt + rise * arc;
            int hi = (int) Math.round(deck - 0.8 * s * s);
            int lo = (int) Math.round(deck - thickness * (1.0 + 0.9 * (1.0 - arc)) * Math.sqrt(1.0 - s * s));
            return new int[]{Math.min(lo, hi), hi};
        }
    }

    /** Whether {@code a} comes before {@code b} (x, then z): the pair is found from its first island. */
    static boolean before(IslandShape a, IslandShape b) {
        return a.x() < b.x() || a.x() == b.x() && a.z() < b.z();
    }

    /** The pairs an island starts (with islands after it), each with its arch if it has one. */
    static List<Pair> pairsOf(IslandField field, IslandShape island) {
        if (island.kind() == IslandShape.TREE) {
            return List.of();
        }
        double reach = island.reach() + IslandField.MAX_REACH + MAX_GAP;
        List<Pair> found = new ArrayList<>();
        for (IslandShape other : field.shapesWithin(Mth.floor(island.x() - reach), Mth.floor(island.z() - reach),
                Mth.floor(island.x() + reach), Mth.floor(island.z() + reach))) {
            if (other.kind() == IslandShape.TREE || !before(island, other) || Math.abs(other.highest() - island.highest()) > MAX_STEP) {
                continue;
            }
            double d = Math.hypot(other.x() - island.x(), other.z() - island.z());
            if (d > island.reach() + other.reach() + MAX_GAP) {
                continue;
            }
            Pair pair = pair(island, other);
            if (pair != null) {
                found.add(pair);
            }
        }
        found.sort(Comparator.comparingDouble(Pair::gap).thenComparingDouble(p -> p.second().x()).thenComparingDouble(p -> p.second().z()));
        List<Pair> nearest = found.subList(0, Math.min(PARTNERS, found.size()));
        List<Pair> result = new ArrayList<>();
        for (Pair pair : nearest) {
            Arch arch = arch(field, pair);
            result.add(arch == null ? pair : new Pair(pair.first(), pair.second(), pair.a(), pair.b(), pair.gap(), arch));
        }
        return List.copyOf(result);
    }

    /** The facing edges of two islands along the line between their centres, or null if they are no pair. */
    @Nullable
    private static Pair pair(IslandShape first, IslandShape second) {
        double dx = second.x() - first.x();
        double dz = second.z() - first.z();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0) {
            return null;
        }
        double ux = dx / d;
        double uz = dz / d;
        Edge a = null;
        double sa = 0.0;
        for (double s = 0.0; s <= Math.min(d, first.reach()); s += 0.5) {
            int x = Mth.floor(first.x() + s * ux);
            int z = Mth.floor(first.z() + s * uz);
            int[] interval = first.interval(x + 0.5, z + 0.5);
            if (interval != null) {
                a = new Edge(x, z, interval[1], interval[0]);
                sa = s;
            }
        }
        Edge b = null;
        double sb = d;
        for (double s = d; s >= Math.max(0.0, d - second.reach()); s -= 0.5) {
            int x = Mth.floor(first.x() + s * ux);
            int z = Mth.floor(first.z() + s * uz);
            int[] interval = second.interval(x + 0.5, z + 0.5);
            if (interval != null) {
                b = new Edge(x, z, interval[1], interval[0]);
                sb = s;
            }
        }
        if (a == null || b == null) {
            return null;
        }
        double gap = sb - sa;
        if (gap < MIN_GAP || gap > MAX_GAP || Math.abs(a.top() - b.top()) > MAX_STEP) {
            return null;
        }
        return new Pair(first, second, a, b, gap, null);
    }

    /** The arch of a pair, if it gets one and it stays clear; else null. */
    @Nullable
    private static Arch arch(IslandField field, Pair pair) {
        if (pair.first().kind() == IslandShape.CLOD || pair.second().kind() == IslandShape.CLOD
                || pair.gap() < ARCH_MIN_GAP || pair.gap() > ARCH_MAX_GAP
                || Math.abs(pair.a().top() - pair.b().top()) > Math.min(ARCH_STEP, Math.max(3.0, ARCH_SLOPE * pair.gap()))) {
            return null;
        }
        long h = SeededHash.hash(field.hashNoise(), KeyedSlots.key(pair.a().x(), pair.a().z()),
                KeyedSlots.key(pair.b().x(), pair.b().z()), SALT);
        if (SeededHash.unit(h, 0) >= ARCH_CHANCE) {
            return null;
        }
        Arch arch = Arch.between(pair.a(), pair.b(), h);
        return clear(field, pair, arch) ? arch : null;
    }

    /**
     * Whether the arch keeps clear: no other island within {@link #KEEP_CLEAR} over or under it, no fall, spring or lift
     * within {@link #KEEP_CLEAR} columns, no sky lake (basin, brook, rim) of its two islands, no world tree.
     */
    private static boolean clear(IslandField field, Pair pair, Arch arch) {
        List<IslandShape> near = field.shapesWithin(arch.minX(), arch.minZ(), arch.maxX(), arch.maxZ());
        List<int[]> wet = new ArrayList<>();
        for (IslandShape shape : near) {
            IslandField.Details details = field.details(shape);
            for (IslandField.Fall fall : details.falls()) {
                wet.add(new int[]{fall.springX(), fall.springZ()});
                wet.add(new int[]{fall.fallX(), fall.fallZ()});
            }
            if (details.lift() != null) {
                wet.add(new int[]{details.lift().x(), details.lift().z()});
            }
        }
        for (int[] w : wet) {
            if (w[0] >= arch.minX() - KEEP_CLEAR && w[0] <= arch.maxX() + KEEP_CLEAR && w[1] >= arch.minZ() - KEEP_CLEAR
                    && w[1] <= arch.maxZ() + KEEP_CLEAR) {
                for (int dx = -KEEP_CLEAR; dx <= KEEP_CLEAR; dx++) {
                    for (int dz = -KEEP_CLEAR; dz <= KEEP_CLEAR; dz++) {
                        if (arch.interval(w[0] + dx, w[1] + dz) != null) {
                            return false;
                        }
                    }
                }
            }
        }
        SkyLakes.Lake[] lakes = {field.landmarks().lake(pair.first()), field.landmarks().lake(pair.second())};
        for (WorldTreeShape tree : field.landmarks().trees()) {
            if (tree.reaches(arch.minX(), arch.minZ(), arch.maxX(), arch.maxZ())) {
                return false;
            }
        }
        for (int x = arch.minX(); x <= arch.maxX(); x++) {
            for (int z = arch.minZ(); z <= arch.maxZ(); z++) {
                int[] interval = arch.interval(x, z);
                if (interval == null) {
                    continue;
                }
                for (SkyLakes.Lake lake : lakes) {
                    if (lake != null && lake.touches(x, z) && (lake.island().distance(x + 0.5, z + 0.5) < lake.share() + 0.2
                            || lake.brook().contains(KeyedSlots.key(x, z)))) {
                        return false;
                    }
                }
                for (IslandShape other : near) {
                    if (other.equals(pair.first()) || other.equals(pair.second())) {
                        continue;
                    }
                    int[] solid = other.interval(x + 0.5, z + 0.5);
                    if (solid != null && solid[0] <= interval[1] + KEEP_CLEAR + 1 && solid[1] >= interval[0] - KEEP_CLEAR) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
