package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring4Band;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The sky lakes of ring 4 (74, Q8 = A): a lake in a basin in the top of every large high island ({@code sky_gardens};
 * the height biomes' rule: top at {@link #SKY_TOP} or higher), with a short brook to one of the island's kept falls
 * where one fits, so the fall springs from the lake; over the blighted band ({@code withered_gardens}) a dried-up bowl
 * without water. Terrain, not a feature: the basin lowers the island's top per column ({@link #apply}), the aquifer
 * fills it with still water ({@link IslandField.Column#skyLakeTop}).
 * <p>
 * Walled in per block: the water level lies a block under the lowest top of every island column beside the basin (and
 * beside the brook), so every water block has water or the island's rock beside and under it (the waterscan's rule);
 * carvers may not cut the basin, its rim or the brook ({@link IslandField.Column#skyGuardFrom}). Pure, decided once per
 * island and world.
 */
final class SkyLakes {
    /** The height biomes' threshold (the preset's {@code island_biomes.min_top}): an island this high is a sky garden. */
    static final int SKY_TOP = 140;
    /** Islands this wide (blocks) and more get a lake. */
    static final double MIN_DIAMETER = 36.0;
    /** A brook is at most this many columns long. */
    static final int MAX_BROOK = 40;
    /** A brook's water lies at most this far under the basin's own level (the fall's spring sets it). */
    private static final int BROOK_DROP = 4;
    /** Carvers stay out of this many blocks under the basin's deepest floor. */
    private static final int GUARD_BELOW = 6;
    /** The rim band (blocks past the basin) kept free of carvers. */
    private static final double GUARD_RIM = 3.0;
    private static final long SALT = 7402;

    private SkyLakes() {
    }

    /**
     * One island's basin: normalised basin distance {@code d / share} below 1 (d: the island's own ragged distance),
     * floor {@code water − 1 − ⌊depth · (1 − b²)⌋}; {@code water} the top water block (a dry bowl's brim), the brook's
     * columns (keys {@link KeyedSlots#key}), its box.
     */
    record Lake(IslandShape island, double share, double depth, int water, boolean dry, Set<Long> brook,
                int minX, int minZ, int maxX, int maxZ) {
        double basin(double px, double pz) {
            return island.distance(px, pz) / share;
        }

        /** The floor's top block (the last rock under the water) of a basin column at basin distance {@code b} &lt; 1. */
        int floor(double b) {
            return water - 1 - (int) Math.floor(depth * (1.0 - b * b));
        }

        boolean touches(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        /** The public view ({@link IslandField.SkyLake}). */
        IslandField.SkyLake view() {
            double radius = share * island.a() * Math.sqrt(island.stretch());
            return new IslandField.SkyLake(Mth.floor(island.x()), Mth.floor(island.z()), radius, water, dry, brook.size());
        }
    }

    /** The lake of an island, or null if it has none. */
    @Nullable
    static Lake decide(IslandField field, IslandShape island) {
        if (island.kind() == IslandShape.TREE || island.kind() == IslandShape.CLOD) {
            return null;
        }
        double diameter = 2.0 * island.a() * Math.sqrt(island.stretch());
        if (diameter < MIN_DIAMETER || island.highest() < SKY_TOP) {
            return null;
        }
        long h = SeededHash.hash(field.hashNoise(), Mth.floor(island.x()), Mth.floor(island.z()), SALT);
        double share = island.kind() == IslandShape.GIANT ? 0.28 + 0.12 * SeededHash.unit(h, 0) : 0.38 + 0.1 * SeededHash.unit(h, 0);
        double depth = Math.min(2.0 + 3.0 * SeededHash.unit(h, 1), 0.2 * island.thickness());
        boolean dry = Ring4Band.of(field.ringLayout()).blighted(island.x(), island.z());
        double extent = share * island.reach() + 2.0;
        int minX = Mth.floor(island.x() - extent);
        int minZ = Mth.floor(island.z() - extent);
        int maxX = Mth.ceil(island.x() + extent);
        int maxZ = Mth.ceil(island.z() + extent);
        Set<Long> basin = new HashSet<>();
        List<IslandShape> others = new ArrayList<>();
        for (IslandShape other : field.shapesWithin(minX - 1, minZ - 1, maxX + 1, maxZ + 1)) {
            if (!other.equals(island) && Math.abs(other.x() - island.x()) < other.reach() + extent + 1.0
                    && Math.abs(other.z() - island.z()) < other.reach() + extent + 1.0) {
                others.add(other);
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (inBasin(island, share, x + 0.5, z + 0.5)) {
                    basin.add(KeyedSlots.key(x, z));
                }
            }
        }
        if (basin.isEmpty() || stacked(others, basin)) {
            return null;
        }
        // The level: one block under the lowest top of the island's columns beside the basin.
        int level = rimLevel(island, basin, Set.of(), Long.MIN_VALUE);
        if (level == Integer.MIN_VALUE) {
            return null;
        }
        Set<Long> brook = Set.of();
        if (!dry) {
            for (IslandField.Fall fall : keptFalls(field, island)) {
                if (fall.springY() > level || fall.springY() < level - BROOK_DROP) {
                    continue;
                }
                Set<Long> path = brook(island, basin, fall);
                if (path != null && rimLevel(island, basin, path, KeyedSlots.key(fall.springX(), fall.springZ())) >= fall.springY()
                        && sound(island, path, fall.springY())) {
                    brook = path;
                    level = fall.springY();
                    break;
                }
            }
        }
        // Keep at least two blocks of island under the deepest floor.
        int[] centre = island.interval(island.x(), island.z());
        if (centre == null) {
            return null;
        }
        depth = Math.max(0.0, Math.min(depth, level - 1 - (centre[0] + 2)));
        Lake lake = new Lake(island, share, depth, level, dry, Set.copyOf(brook), minX, minZ, maxX, maxZ);
        if (!brook.isEmpty()) {
            for (long key : brook) {
                int bx = (int) (key >> 32);
                int bz = (int) key;
                lake = new Lake(island, share, depth, level, dry, lake.brook(), Math.min(lake.minX(), bx - 1),
                        Math.min(lake.minZ(), bz - 1), Math.max(lake.maxX(), bx + 1), Math.max(lake.maxZ(), bz + 1));
            }
        }
        return lake;
    }

    /**
     * One block under the lowest top of the island's columns beside the basin and the brook (4-neighbours that are
     * neither, nor the spring {@code spring}); MIN_VALUE if one of them holds no island (the basin would open its
     * side).
     */
    private static int rimLevel(IslandShape island, Set<Long> basin, Set<Long> brook, long spring) {
        int lowest = Integer.MAX_VALUE;
        Set<Long> wet = new HashSet<>(basin);
        wet.addAll(brook);
        for (long key : wet) {
            int x = (int) (key >> 32);
            int z = (int) key;
            for (int[] n : NEIGHBOURS) {
                long nk = KeyedSlots.key(x + n[0], z + n[1]);
                if (wet.contains(nk) || nk == spring) {
                    continue;
                }
                int[] interval = island.interval(x + n[0] + 0.5, z + n[1] + 0.5);
                if (interval == null) {
                    return Integer.MIN_VALUE;
                }
                lowest = Math.min(lowest, interval[1]);
            }
        }
        return lowest - 1;
    }

    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** The one basin test: the column's centre lies within {@code share} of the island's ragged outline. */
    static boolean inBasin(IslandShape island, double share, double px, double pz) {
        return island.distance(px, pz) < share;
    }

    /**
     * Whether another island holds a block in a basin column or one beside it: a column keeps one lake's water only, and
     * an island stacked over the basin would shade it anyway.
     */
    private static boolean stacked(List<IslandShape> others, Set<Long> basin) {
        if (others.isEmpty()) {
            return false;
        }
        for (long key : basin) {
            int x = (int) (key >> 32);
            int z = (int) key;
            for (IslandShape other : others) {
                for (int[] n : NEIGHBOURS) {
                    if (other.interval(x + n[0] + 0.5, z + n[1] + 0.5) != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** The island's falls the terrain keeps (the per-chunk cap), from the highest spring down. */
    private static List<IslandField.Fall> keptFalls(IslandField field, IslandShape island) {
        List<IslandField.Fall> kept = new ArrayList<>();
        for (IslandField.Fall fall : field.details(island).falls()) {
            if (field.fallsOfChunk(fall.fallX() >> 4, fall.fallZ() >> 4).contains(fall)) {
                kept.add(fall);
            }
        }
        kept.sort(Comparator.comparingInt(IslandField.Fall::springY).reversed().thenComparingInt(IslandField.Fall::springX)
                .thenComparingInt(IslandField.Fall::springZ));
        return kept;
    }

    /**
     * The brook from a fall's spring inwards to the basin: first one column straight in (the spring's inner wall), then
     * step by step towards the island's centre, face to face, until the next column lies in the basin. Null if longer
     * than {@link #MAX_BROOK}.
     */
    @Nullable
    private static Set<Long> brook(IslandShape island, Set<Long> basin, IslandField.Fall fall) {
        int dx = Integer.signum(fall.fallX() - fall.springX());
        int dz = Integer.signum(fall.fallZ() - fall.springZ());
        int x = fall.springX() - dx;
        int z = fall.springZ() - dz;
        Set<Long> path = new java.util.LinkedHashSet<>();
        for (int n = 0; n < MAX_BROOK; n++) {
            long key = KeyedSlots.key(x, z);
            if (basin.contains(key)) {
                return path;
            }
            path.add(key);
            double rx = island.x() - (x + 0.5);
            double rz = island.z() - (z + 0.5);
            if (Math.abs(rx) >= Math.abs(rz)) {
                x += rx > 0 ? 1 : -1;
            } else {
                z += rz > 0 ? 1 : -1;
            }
        }
        return null;
    }

    /** Whether the island holds a brook at {@code level}: every column of it is rock at least two blocks under it. */
    private static boolean sound(IslandShape island, Set<Long> path, int level) {
        for (long key : path) {
            int[] interval = island.interval((int) (key >> 32) + 0.5, (int) key + 0.5);
            if (interval == null || interval[1] < level - 1 || interval[0] > level - 3) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies a lake to a column: lowers the island's top in the basin and the brook to the floor (the column's matching
     * island range) and sets the water and the carvers' guard. Columns of the rim band only get the guard.
     */
    static void apply(Lake lake, IslandField.Column c) {
        double px = c.x + 0.5;
        double pz = c.z + 0.5;
        double b = lake.basin(px, pz);
        boolean basin = inBasin(lake.island(), lake.share(), px, pz);
        boolean brook = !lake.brook().isEmpty() && lake.brook().contains(KeyedSlots.key(c.x, c.z));
        int guardLo = lake.water() - 1 - (int) Math.floor(lake.depth()) - GUARD_BELOW;
        if (!basin && !brook) {
            if (!lake.dry() && b < 1.0 + GUARD_RIM / Math.max(1.0, lake.share() * lake.island().a())) {
                c.guardSky(guardLo, lake.water() + 1);
            }
            return;
        }
        int[] interval = lake.island().interval(px, pz);
        if (interval == null) {
            return;
        }
        int floor = Math.max(interval[0] + 1, Math.min(interval[1], basin ? lake.floor(Math.min(b, 1.0)) : lake.water() - 1));
        if (!c.lowerIsland(interval[0], interval[1], floor)) {
            return;
        }
        c.skyLakeFloor = floor;
        if (!lake.dry()) {
            c.skyLakeTop = lake.water() + 1;
            c.guardSky(guardLo, lake.water() + 1);
        }
    }
}
