package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The pure shape of a giant fungus (change 35), as a list of parts with a kind; the feature turns kinds into blocks and
 * writes them through {@link GrowthPriority}. Two forms:
 * <ul>
 *   <li>a <b>pillar</b>: a round stem from the floor to the ceiling (diameter 3–7), 1–3 cap discs along it with short
 *       drooping rims, a collar of cap against the ceiling, lamps scattered in the cap, and short stalactites under
 *       the discs;</li>
 *   <li>a <b>free-standing</b> fungus: a stem of 8–20 blocks with a dome-shaped cap (radius 3–6), lamps and short
 *       stalactites under the rim.</li>
 * </ul>
 * Reads the terrain only through {@link Ground}; every part lies within {@link #MAX_REACH} blocks (horizontally) of the
 * origin, so the whole fungus stays within the 3 × 3 chunks around the origin's chunk.
 */
public final class GiantFungusShape {
    /** Largest horizontal distance of any part from the origin. */
    public static final int MAX_REACH = 10;
    /** Pillars need at least this much room between floor and ceiling. */
    public static final int MIN_PILLAR_GAP = 20;
    /** How far down the stem of a pillar reaches below the centre's floor on an uneven floor. */
    public static final int ROOT_DEPTH = 5;
    /** A free-standing fungus needs at least this much room (a dew geode's cavity is lower). */
    public static final int MIN_FREE_GAP = 18;
    public static final double LAMP_CHANCE = 0.08;

    private static final int SCAN = 70;

    private GiantFungusShape() {
    }

    public enum Kind {
        STEM, CAP, LAMP, STALACTITE
    }

    /** One block of the fungus; {@code thickness} only for stalactites. */
    public record Part(int x, int y, int z, Kind kind, @Nullable DripstoneThickness thickness) {
    }

    /** Terrain heights at a column, from a Y inside the hall: the Y of the floor/ceiling block, or {@link FungalTerrainScan#NONE}. */
    public interface Ground {
        int floor(int x, int fromY, int z);

        int ceiling(int x, int fromY, int z);

        /** Whether the floor block at (x, y, z) is lava. */
        boolean lava(int x, int y, int z);
    }

    /**
     * The fungus for the origin column ({@code x}, {@code z}) with its scan start at {@code y}: a pillar if the hall is high
     * enough (and {@code pillars} is set), else a free-standing fungus; empty if there is no floor (or lava) to stand on.
     */
    public static List<Part> build(RandomSource random, int x, int y, int z, Ground ground, boolean pillars) {
        int floor = ground.floor(x, y, z);
        if (floor == FungalTerrainScan.NONE || ground.lava(x, floor, z)) {
            return List.of();
        }
        int ceiling = ground.ceiling(x, y, z);
        // The scan start must lie in air, below the ceiling and above the floor; a hall lower than a pillar still takes a free fungus.
        if (pillars && ceiling != FungalTerrainScan.NONE && ceiling - floor >= MIN_PILLAR_GAP) {
            return pillar(random, x, z, floor, ceiling, ground);
        }
        if (ceiling == FungalTerrainScan.NONE || ceiling - floor >= MIN_FREE_GAP) {
            return freeStanding(random, x, z, floor, ground);
        }
        return List.of();
    }

    static List<Part> pillar(RandomSource random, int x, int z, int floor, int ceiling, Ground ground) {
        List<Part> parts = new ArrayList<>();
        double radius = 1.5 + random.nextDouble() * 2.0;
        int reach = (int) Math.ceil(radius);
        // Stem: every column of the disc from its own floor (at most ROOT_DEPTH below the centre's) up to its own ceiling.
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int columnFloor = ground.floor(x + dx, floor + 1, z + dz);
                int columnCeiling = ground.ceiling(x + dx, ceiling - 1, z + dz);
                // A column whose ceiling is lower than the centre's starts inside terrain: look down for its own ceiling.
                for (int k = 1; k <= 8 && columnCeiling == FungalTerrainScan.NONE; k++) {
                    columnCeiling = ground.ceiling(x + dx, ceiling - 1 - k, z + dz);
                }
                if (columnFloor == FungalTerrainScan.NONE || columnCeiling == FungalTerrainScan.NONE) {
                    continue;
                }
                for (int y = Math.max(columnFloor + 1, floor - ROOT_DEPTH + 1); y < columnCeiling; y++) {
                    parts.add(new Part(x + dx, y, z + dz, Kind.STEM, null));
                }
            }
        }
        int discs = 1 + random.nextInt(3);
        int gap = ceiling - floor;
        for (int i = 0; i < discs; i++) {
            int y = floor + gap * (i + 1) / (discs + 1) + random.nextInt(5) - 2;
            y = Math.max(floor + 8, Math.min(ceiling - 8, y));
            int discRadius = 4 + random.nextInt(5);
            disc(random, parts, x, y, z, discRadius);
            stalactites(random, parts, x, y, z, discRadius);
        }
        collar(random, parts, x, z, ceiling, ground, 5 + random.nextInt(3));
        return parts;
    }

    /** A disc of cap two layers thick whose rim droops one more layer down. */
    private static void disc(RandomSource random, List<Part> parts, int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) {
                    continue;
                }
                parts.add(cap(random, x + dx, y, z + dz));
                if (distance <= radius - 1) {
                    parts.add(cap(random, x + dx, y + 1, z + dz));
                } else {
                    parts.add(cap(random, x + dx, y - 1, z + dz));
                }
            }
        }
    }

    /** The cap against the ceiling: two layers of cap under the terrain ceiling of each column. */
    private static void collar(RandomSource random, List<Part> parts, int x, int z, int ceiling, Ground ground, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int columnCeiling = ground.ceiling(x + dx, ceiling - 1, z + dz);
                if (columnCeiling == FungalTerrainScan.NONE) {
                    continue;
                }
                parts.add(cap(random, x + dx, columnCeiling - 1, z + dz));
                parts.add(cap(random, x + dx, columnCeiling - 2, z + dz));
            }
        }
    }

    /** 2–5 short stalactites hanging from the underside of a disc, towards its rim. */
    private static void stalactites(RandomSource random, List<Part> parts, int x, int y, int z, int radius) {
        int count = 2 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.max(1.0, radius - random.nextDouble() * 3.0);
            int sx = x + (int) Math.round(Math.cos(angle) * distance);
            int sz = z + (int) Math.round(Math.sin(angle) * distance);
            double fromCentre = Math.sqrt((sx - x) * (sx - x) + (sz - z) * (sz - z));
            // The rim droops one block lower; the stalactite hangs from the lowest cap block of its column.
            int hangFrom = fromCentre > radius - 1 ? y - 1 : y;
            hang(parts, sx, hangFrom - 1, sz, 1 + random.nextInt(3));
        }
    }

    /** A stalactite of {@code length} blocks whose top block is at {@code topY}: base/middle/frustum/tip from the top down. */
    static void hang(List<Part> parts, int x, int topY, int z, int length) {
        for (int i = 0; i < length; i++) {
            int fromTip = length - 1 - i;
            DripstoneThickness thickness = fromTip == 0 ? DripstoneThickness.TIP : fromTip == 1 ? DripstoneThickness.FRUSTUM
                    : i == 0 && length >= 4 ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
            parts.add(new Part(x, topY - i, z, Kind.STALACTITE, thickness));
        }
    }

    static List<Part> freeStanding(RandomSource random, int x, int z, int floor, Ground ground) {
        List<Part> parts = new ArrayList<>();
        int height = 8 + random.nextInt(13);
        double radius = random.nextBoolean() ? 0.5 : 1.5;
        int reach = (int) Math.ceil(radius);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int columnFloor = ground.floor(x + dx, floor + 1, z + dz);
                if (columnFloor == FungalTerrainScan.NONE || columnFloor < floor - 3) {
                    continue;
                }
                for (int y = columnFloor + 1; y <= floor + height; y++) {
                    parts.add(new Part(x + dx, y, z + dz, Kind.STEM, null));
                }
            }
        }
        int domeRadius = 3 + random.nextInt(4);
        int top = floor + height;
        // A dome: the upper part of a shell two blocks thick, with its centre one block below the stem's top.
        for (int dx = -domeRadius; dx <= domeRadius; dx++) {
            for (int dz = -domeRadius; dz <= domeRadius; dz++) {
                for (int dy = -1; dy <= domeRadius; dy++) {
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance <= domeRadius && distance > domeRadius - 2) {
                        parts.add(cap(random, x + dx, top + dy, z + dz));
                    }
                }
            }
        }
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int sx = x + (int) Math.round(Math.cos(angle) * (domeRadius - 1));
            int sz = z + (int) Math.round(Math.sin(angle) * (domeRadius - 1));
            hang(parts, sx, top - 2, sz, 1 + random.nextInt(2));
        }
        return parts;
    }

    private static Part cap(RandomSource random, int x, int y, int z) {
        return new Part(x, y, z, random.nextDouble() < LAMP_CHANCE ? Kind.LAMP : Kind.CAP, null);
    }

    /** Scan distance for the feature's {@link Ground}. */
    public static int scan() {
        return SCAN;
    }
}
