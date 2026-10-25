package nl.timtoys.corruptedadventure.worldgen.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleBinaryOperator;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

/**
 * The rock eye-catchers of ring 2 (G2 ring 2, decision 1): a natural rock bridge over a main canyon of
 * {@code canyonlands} and a giant hoodoo group in {@code hoodoo_fields}. At most one per landmark cell
 * ({@link Landmarks.Grid}), its place and shape a pure function of the seed, the cell and the pure ring 2 terrain
 * ({@link Ring2PureTerrain}: the 2D surface height, never a block), so every chunk that asks gets the same landmark and
 * writes only its own part. The decided shapes are remembered per world (the canyon scan is the costly part).
 */
public final class Ring2RockLandmarks {
    /** The grids (cell, share of cells that try, tries, margin); the anchor is a point on the crossing or the hoodoo's foot. */
    static final Landmarks.Grid BRIDGE_GRID = new Landmarks.Grid(256, 1.0F, 48, 40);
    static final Landmarks.Grid HOODOO_GRID = new Landmarks.Grid(384, 1.0F, 12, 40);
    private static final long BRIDGE_SALT = 0x2B_41D6_E5L;
    private static final long HOODOO_SALT = 0x2B_400D_00L;

    // ---- the bridge
    /** Half the longest scan across the canyon, and the directions tried (over half a turn). */
    static final int SCAN = 44;
    private static final int DIRECTIONS = 32;
    /** The crossing point lies on a canyon floor or its lower steps (canyon floors lie at Y 66–89). */
    static final int MAX_FLOOR_Y = 104;
    /** Span between the rims at deck height. */
    static final int MIN_SPAN = 18;
    static final int MAX_SPAN = 44;
    /** The canyon under the crossing point is at least this much lower than the deck, the rims at least on this height. */
    static final int MIN_DEPTH = 24;
    static final int MIN_RIM_Y = 112;
    /** Ground beyond each rim: within this many blocks under the deck for {@code LANDING} blocks (a solid abutment). */
    static final int LANDING = 6;
    static final int LANDING_DROP = 3;
    /** How far the deck runs on into the plateau beyond each rim, and how deep its abutment goes there. */
    static final int EXTENSION = 5;
    static final int ABUTMENT = 12;
    /** Biome of the bridge: canyonlands at the crossing point and this far beyond both ends. */
    private static final int BIOME_MARGIN = 12;
    /** Every bridge block lies within this many blocks (per axis) of its anchor. */
    static final int BRIDGE_REACH = SCAN + EXTENSION + 4;

    // ---- the giant hoodoo
    /** The floor of the hoodoo fields (pure height) the group stands on, and its largest unevenness under the group. */
    static final int MIN_FLOOR = 82;
    static final int MAX_FLOOR = 96;
    static final int MAX_UNEVEN = 4;
    /** The group's foot reaches at most this far from its anchor (companion distance + foot + rubble). */
    static final int HOODOO_REACH = 26;
    private static final int HOODOO_BIOME_MARGIN = 14;
    /** The columns go this deep under the pure floor (never floating on a slightly lower real floor). */
    static final int ROOT = 10;

    private static final Map<Object, Map<Long, Optional<Bridge>>> BRIDGES = new WeakHashMap<>();
    private static final Map<Object, Map<Long, Optional<Hoodoo>>> HOODOOS = new WeakHashMap<>();

    private Ring2RockLandmarks() {
    }

    // ------------------------------------------------------------------------------------------------------- bridge

    /**
     * A natural rock bridge in the vertical plane through ({@code x}, {@code z}) along ({@code cos}, {@code sin}): the
     * rims at {@code -back} and {@code ahead} along it, the deck top {@code top} (absolute Y) crowned by {@code crown}
     * over the middle, {@code thickness} thick at the middle and {@code flare} more at the rims, {@code width} across.
     */
    public record Bridge(int x, int z, double cos, double sin, double back, double ahead, int top, double crown, double thickness,
                         double flare, double width, long seed) {
        /** Along and across coordinates of a block centre. */
        double along(double px, double pz) {
            return (px - x) * cos + (pz - z) * sin;
        }

        double across(double px, double pz) {
            return -(px - x) * sin + (pz - z) * cos;
        }

        /** The solid interval (absolute Y, inclusive) of the bridge in the column of block (bx, bz), or null. */
        int[] interval(int bx, int bz) {
            double u = along(bx + 0.5, bz + 0.5);
            double v = across(bx + 0.5, bz + 0.5);
            if (u < -back - EXTENSION || u > ahead + EXTENSION) {
                return null;
            }
            double mid = (ahead - back) / 2.0;
            double half = (ahead + back) / 2.0;
            double t = Mth.clamp((u - mid) / half, -1.0, 1.0);
            // The rim edges eat into the deck a little (hash per block along), narrowest at the middle.
            double w = Math.max(2.0, width * (1.0 - 0.18 * (1.0 - t * t)) - 0.6 * hash(seed, Mth.floor(u), 3));
            if (Math.abs(v) > w / 2.0) {
                return null;
            }
            boolean beyond = u < -back || u > ahead;
            // The outer row of the deck is one lower on top and one higher underneath: a rounded cross-section.
            double edge = Math.abs(v) > w / 2.0 - 1.0 ? 1.0 : 0.0;
            double centre = top + crown * (1.0 - t * t);
            int high = (int) Math.round(centre - edge);
            if (beyond) {
                // The abutment: down into the plateau, so a deck end never floats over a slightly lower real rim.
                return new int[]{top - ABUTMENT, high};
            }
            double depth = thickness + flare * Math.pow(Math.abs(t), 3.0) + 0.8 * hash(seed, Mth.floor(u), 7);
            return new int[]{(int) Math.round(centre - depth + edge) + 1, high};
        }

        /** The box (min x, min z, max x, max z) of every block of the bridge. */
        int[] box() {
            double reachAlong = Math.max(back, ahead) + EXTENSION + 1;
            double reachAcross = width / 2.0 + 1;
            int rx = (int) Math.ceil(Math.abs(cos) * reachAlong + Math.abs(sin) * reachAcross);
            int rz = (int) Math.ceil(Math.abs(sin) * reachAlong + Math.abs(cos) * reachAcross);
            return new int[]{x - rx, z - rz, x + rx, z + rz};
        }
    }

    /** The bridge of the cell, decided once per world. */
    static Optional<Bridge> bridge(WorldGenLevel level, int cellX, int cellZ) {
        Map<Long, Optional<Bridge>> cache = cache(BRIDGES, Landmarks.world(level));
        return cache.computeIfAbsent(ChunkPos.asLong(cellX, cellZ), key -> {
            Ring2PureTerrain terrain = Ring2PureTerrain.of(level);
            Bridge[] found = new Bridge[1];
            Landmarks.inCell(level.getSeed(), BRIDGE_SALT, BRIDGE_GRID, cellX, cellZ, candidate -> {
                if (!Landmarks.biome(level, candidate.x(), candidate.z()).is(ModBiomes.CANYONLANDS)) {
                    return false;
                }
                Bridge bridge = crossing(terrain::height, candidate.x(), candidate.z(), candidate.random());
                if (bridge == null || !bridgeBiome(level, bridge)) {
                    return false;
                }
                found[0] = bridge;
                return true;
            });
            return Optional.ofNullable(found[0]);
        });
    }

    private static boolean bridgeBiome(WorldGenLevel level, Bridge bridge) {
        // Every 8 blocks along the deck: every chunk the bridge reaches then holds canyonlands and runs the feature.
        for (double u = -bridge.back() - BIOME_MARGIN; u <= bridge.ahead() + BIOME_MARGIN; u += 8.0) {
            int px = Mth.floor(bridge.x() + 0.5 + u * bridge.cos());
            int pz = Mth.floor(bridge.z() + 0.5 + u * bridge.sin());
            if (!Landmarks.biome(level, px, pz).is(ModBiomes.CANYONLANDS)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The shortest crossing of a deep canyon through (x, z), or null: over up to 12 of {@value #DIRECTIONS} directions the profile of
     * the pure height {@code height} is scanned {@value #SCAN} blocks each way; the deck lies one block under the lower
     * of the two highest points, the rims are where the ground first reaches it, and both rims need a landing.
     */
    static Bridge crossing(DoubleBinaryOperator height, int x, int z, RandomSource random) {
        double floor = height.applyAsDouble(x + 0.5, z + 0.5);
        if (floor > MAX_FLOOR_Y) {
            return null;
        }
        // Coarse every fourth direction, then the neighbours of the best one: 12 profiles instead of 32.
        double[] pick = null;
        int bestD = -1;
        for (int d = 0; d < DIRECTIONS; d += 4) {
            double[] cross = scan(height, x, z, d, floor);
            if (cross != null && (pick == null || cross[2] + cross[3] < pick[2] + pick[3])) {
                pick = cross;
                bestD = d;
            }
        }
        if (pick == null) {
            return null;
        }
        for (int step : new int[]{-2, -1, 1, 2}) {
            double[] cross = scan(height, x, z, Math.floorMod(bestD + step, DIRECTIONS), floor);
            if (cross != null && cross[2] + cross[3] < pick[2] + pick[3]) {
                pick = cross;
            }
        }
        if (pick == null) {
            return null;
        }
        int top = (int) pick[4];
        double crown = random.nextInt(3);
        double thickness = 3.0 + random.nextInt(3);
        double flare = 3.0 + random.nextDouble() * 4.0;
        double width = 3.0 + random.nextDouble() * 2.2;
        return new Bridge(x, z, pick[0], pick[1], pick[2], pick[3], top, crown, thickness, flare, width, random.nextLong());
    }

    /** The crossing along direction {@code d} (of {@value #DIRECTIONS} over half a turn): {cos, sin, back, ahead, top} or null. */
    private static double[] scan(DoubleBinaryOperator height, int x, int z, int d, double floor) {
        double angle = Math.PI * d / DIRECTIONS;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double[] profile = new double[2 * SCAN + 1];
        for (int i = -SCAN; i <= SCAN; i++) {
            profile[i + SCAN] = height.applyAsDouble(x + 0.5 + i * cos, z + 0.5 + i * sin);
        }
        double[] cross = crossingOf(profile, floor);
        return cross == null ? null : new double[]{cos, sin, cross[0], cross[1], cross[2]};
    }

    /**
     * One direction's profile (index {@code SCAN} = the crossing point): {back, ahead, deck top} or null. The deck top is
     * one under the lower of the highest points on either side; it must lie {@value #MIN_DEPTH} over the floor here.
     */
    static double[] crossingOf(double[] profile, double floor) {
        double maxBack = Double.NEGATIVE_INFINITY;
        double maxAhead = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= SCAN; i++) {
            maxBack = Math.max(maxBack, profile[SCAN - i]);
            maxAhead = Math.max(maxAhead, profile[SCAN + i]);
        }
        int top = (int) Math.floor(Math.min(maxBack, maxAhead)) - 1;
        if (top < MIN_RIM_Y || top - floor < MIN_DEPTH) {
            return null;
        }
        int ahead = rim(profile, top, 1);
        int back = rim(profile, top, -1);
        if (ahead < 0 || back < 0 || ahead + back < MIN_SPAN || ahead + back > MAX_SPAN) {
            return null;
        }
        if (!lands(profile, top, ahead, 1) || !lands(profile, top, back, -1)) {
            return null;
        }
        return new double[]{back, ahead, top};
    }

    /** Steps from the crossing point to where the ground first reaches {@code top} − 1 in direction {@code dir}, or −1. */
    private static int rim(double[] profile, int top, int dir) {
        for (int i = 1; i <= SCAN; i++) {
            if (profile[SCAN + dir * i] >= top - 1) {
                return i;
            }
        }
        return -1;
    }

    /** Whether the ground stays within {@value #LANDING_DROP} under the deck for {@value #LANDING} blocks beyond the rim. */
    private static boolean lands(double[] profile, int top, int rim, int dir) {
        for (int i = rim; i <= rim + LANDING; i++) {
            if (i > SCAN || profile[SCAN + dir * i] < top - LANDING_DROP) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------ hoodoo

    /**
     * One giant pillar: foot centre, its lean per block up, the ground (pure floor) and its top, the radius at the foot,
     * the body and the neck, and its balanced cap (radius and thickness).
     */
    public record Pillar(double x, double z, double leanX, double leanZ, int ground, int top, double foot, double body, double neck,
                         double cap, int capThickness) {
        double centreX(int y) {
            return x + leanX * (y - ground);
        }

        double centreZ(int y) {
            return z + leanZ * (y - ground);
        }

        /** The radius of the shaft at y (the cap not counted), from the flared foot to the neck under the cap. */
        double radius(int y) {
            double t = Mth.clamp((y - ground) / (double) (top - capThickness - ground), 0.0, 1.0);
            double r = t < 0.15 ? Mth.lerp(t / 0.15, foot, body) : Mth.lerp((t - 0.15) / 0.85, body, neck);
            // Softer and harder bands erode unevenly: a slow swelling of the shaft along its height.
            return r + 0.45 * Math.sin((y - ground) * 0.38 + x * 0.7);
        }

        int capBottom() {
            return top - capThickness + 1;
        }
    }

    /** A giant hoodoo group: its anchor (the main pillar's foot), the pillars and the seed of its details. */
    public record Hoodoo(int x, int z, List<Pillar> pillars, long seed) {
        int[] box() {
            int reach = HOODOO_REACH;
            return new int[]{x - reach, z - reach, x + reach, z + reach};
        }
    }

    /**
     * Whether block (x, y, z) belongs to the pillar: the cap (ragged round its edge, one smaller at its bottom and top
     * rows) or the shaft (rough by a hash per block), down to {@value #ROOT} under the floor at the foot's width.
     */
    static boolean inPillar(long seed, Pillar pillar, int x, int y, int z) {
        if (y > pillar.top() || y < pillar.ground() - ROOT) {
            return false;
        }
        if (y >= pillar.capBottom()) {
            double dx = x + 0.5 - pillar.centreX(pillar.capBottom());
            double dz = z + 0.5 - pillar.centreZ(pillar.capBottom());
            int sector = Mth.floor((Math.atan2(dz, dx) + Math.PI) * 3.0);
            double r = pillar.cap() * (0.85 + 0.15 * hash(seed, sector, 13));
            if (y == pillar.capBottom()) {
                r -= 1.0;
            }
            if (y == pillar.top()) {
                r -= 0.8;
            }
            return dx * dx + dz * dz <= r * r;
        }
        double r = (y < pillar.ground() ? pillar.foot() + 0.5 : pillar.radius(y)) + 0.5 * (hash(seed, x, y, z, 5) - 0.5);
        double dx = x + 0.5 - pillar.centreX(y);
        double dz = z + 0.5 - pillar.centreZ(y);
        return dx * dx + dz * dz <= r * r;
    }

    /** The giant hoodoo of the cell, decided once per world. */
    static Optional<Hoodoo> hoodoo(WorldGenLevel level, int cellX, int cellZ) {
        Map<Long, Optional<Hoodoo>> cache = cache(HOODOOS, Landmarks.world(level));
        return cache.computeIfAbsent(ChunkPos.asLong(cellX, cellZ), key -> {
            Ring2PureTerrain terrain = Ring2PureTerrain.of(level);
            Hoodoo[] found = new Hoodoo[1];
            Landmarks.inCell(level.getSeed(), HOODOO_SALT, HOODOO_GRID, cellX, cellZ, candidate -> {
                if (!Landmarks.biome(level, candidate.x(), candidate.z()).is(ModBiomes.HOODOO_FIELDS)
                        || !flatFloor(terrain::height, candidate.x(), candidate.z())
                        || !Landmarks.biomeAround(level, candidate.x(), candidate.z(), HOODOO_REACH + HOODOO_BIOME_MARGIN, ModBiomes.HOODOO_FIELDS)) {
                    return false;
                }
                found[0] = group(terrain::height, candidate.x(), candidate.z(), candidate.random());
                return true;
            });
            return Optional.ofNullable(found[0]);
        });
    }

    /** Whether the pure floor at (x, z) and 8 columns round it lies in the hoodoo floor's band and nearly level. */
    static boolean flatFloor(DoubleBinaryOperator height, int x, int z) {
        double lo = Double.MAX_VALUE;
        double hi = -Double.MAX_VALUE;
        for (int i = -1; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            double r = i < 0 ? 0.0 : HOODOO_REACH;
            double h = height.applyAsDouble(x + 0.5 + Math.cos(angle) * r, z + 0.5 + Math.sin(angle) * r);
            lo = Math.min(lo, h);
            hi = Math.max(hi, h);
        }
        return lo >= MIN_FLOOR && hi <= MAX_FLOOR && hi - lo <= MAX_UNEVEN;
    }

    /** The group: a main giant of 55–75 above the floor and one or two shorter ones leaning out beside it. */
    static Hoodoo group(DoubleBinaryOperator height, int x, int z, RandomSource random) {
        List<Pillar> pillars = new ArrayList<>();
        double heading = random.nextDouble() * Math.PI * 2.0;
        double lean = random.nextDouble() * 0.05;
        double foot = 5.0 + random.nextDouble() * 1.5;
        pillars.add(pillar(height, x + 0.5, z + 0.5, Math.cos(heading) * lean, Math.sin(heading) * lean, 55 + random.nextInt(21), foot,
                3.6 + random.nextDouble() * 0.8, 2.6 + random.nextDouble() * 0.6, 5.0 + random.nextDouble() * 2.0, 3 + random.nextInt(2)));
        int companions = 1 + random.nextInt(2);
        double start = random.nextDouble() * Math.PI * 2.0;
        int mainTop = pillars.getFirst().top() - pillars.getFirst().ground();
        for (int i = 0; i < companions; i++) {
            double around = start + i * Math.PI * 2.0 / companions + (random.nextDouble() - 0.5) * 1.0;
            double scale = 0.7 + random.nextDouble() * 0.12;
            double footC = foot * scale;
            double distance = foot + footC + 1.0 + random.nextDouble() * 3.0;
            double outward = 0.03 + random.nextDouble() * 0.05;
            pillars.add(pillar(height, x + 0.5 + Math.cos(around) * distance, z + 0.5 + Math.sin(around) * distance,
                    Math.cos(around) * outward, Math.sin(around) * outward, (int) Math.round(mainTop * (0.55 + random.nextDouble() * 0.25)),
                    footC, 3.6 * scale + 0.4, 2.6 * scale + 0.4, (4.0 + random.nextDouble() * 1.5) * scale + 0.8, 2 + random.nextInt(2)));
        }
        return new Hoodoo(x, z, List.copyOf(pillars), random.nextLong());
    }

    private static Pillar pillar(DoubleBinaryOperator height, double x, double z, double leanX, double leanZ, int tall, double foot,
                                 double body, double neck, double cap, int capThickness) {
        int ground = (int) Math.floor(height.applyAsDouble(x, z));
        return new Pillar(x, z, leanX, leanZ, ground, ground + tall, foot, body, neck, cap, capThickness);
    }

    // ------------------------------------------------------------------------------------------------------ lookup

    /** The bridges whose blocks may reach into the box (block coordinates, inclusive). */
    static List<Bridge> bridgesNear(WorldGenLevel level, int minX, int minZ, int maxX, int maxZ) {
        List<Bridge> found = new ArrayList<>();
        int cell = BRIDGE_GRID.cellSize();
        for (int cz = Math.floorDiv(minZ - BRIDGE_REACH, cell); cz <= Math.floorDiv(maxZ + BRIDGE_REACH, cell); cz++) {
            for (int cx = Math.floorDiv(minX - BRIDGE_REACH, cell); cx <= Math.floorDiv(maxX + BRIDGE_REACH, cell); cx++) {
                if (!anyCandidateNear(level.getSeed(), BRIDGE_SALT, BRIDGE_GRID, cx, cz, minX - BRIDGE_REACH, minZ - BRIDGE_REACH,
                        maxX + BRIDGE_REACH, maxZ + BRIDGE_REACH)) {
                    continue;
                }
                bridge(level, cx, cz).filter(b -> overlaps(b.box(), minX, minZ, maxX, maxZ)).ifPresent(found::add);
            }
        }
        return found;
    }

    /** The giant hoodoos whose blocks may reach into the box (block coordinates, inclusive). */
    static List<Hoodoo> hoodoosNear(WorldGenLevel level, int minX, int minZ, int maxX, int maxZ) {
        List<Hoodoo> found = new ArrayList<>();
        int cell = HOODOO_GRID.cellSize();
        for (int cz = Math.floorDiv(minZ - HOODOO_REACH, cell); cz <= Math.floorDiv(maxZ + HOODOO_REACH, cell); cz++) {
            for (int cx = Math.floorDiv(minX - HOODOO_REACH, cell); cx <= Math.floorDiv(maxX + HOODOO_REACH, cell); cx++) {
                if (!anyCandidateNear(level.getSeed(), HOODOO_SALT, HOODOO_GRID, cx, cz, minX - HOODOO_REACH, minZ - HOODOO_REACH,
                        maxX + HOODOO_REACH, maxZ + HOODOO_REACH)) {
                    continue;
                }
                hoodoo(level, cx, cz).filter(h -> overlaps(h.box(), minX, minZ, maxX, maxZ)).ifPresent(found::add);
            }
        }
        return found;
    }

    /** One line per landmark whose anchor lies in the box: kind, x, deck/floor y, z (for {@code /ca landmarks}). */
    public static List<String> centredIn(WorldGenLevel level, int minX, int minZ, int maxX, int maxZ) {
        List<String> lines = new ArrayList<>();
        for (Bridge b : bridgesNear(level, minX, minZ, maxX, maxZ)) {
            if (b.x() >= minX && b.x() <= maxX && b.z() >= minZ && b.z() <= maxZ) {
                lines.add(String.format(java.util.Locale.ROOT, "rock_bridge %d %d %d (span %d)", b.x(), b.top(), b.z(),
                        Math.round(b.back() + b.ahead())));
            }
        }
        for (Hoodoo h : hoodoosNear(level, minX, minZ, maxX, maxZ)) {
            if (h.x() >= minX && h.x() <= maxX && h.z() >= minZ && h.z() <= maxZ) {
                Pillar main = h.pillars().getFirst();
                lines.add(String.format(java.util.Locale.ROOT, "giant_hoodoo %d %d %d (height %d, %d pillars)", h.x(), main.ground(), h.z(),
                        main.top() - main.ground(), h.pillars().size()));
            }
        }
        return lines;
    }

    private static boolean anyCandidateNear(long seed, long salt, Landmarks.Grid grid, int cx, int cz, int minX, int minZ, int maxX,
                                            int maxZ) {
        boolean[] near = new boolean[1];
        Landmarks.inCell(seed, salt, grid, cx, cz, c -> {
            near[0] |= c.x() >= minX && c.x() <= maxX && c.z() >= minZ && c.z() <= maxZ;
            return false;
        });
        return near[0];
    }

    private static boolean overlaps(int[] box, int minX, int minZ, int maxX, int maxZ) {
        return box[0] <= maxX && box[2] >= minX && box[1] <= maxZ && box[3] >= minZ;
    }

    private static <T> Map<Long, Optional<T>> cache(Map<Object, Map<Long, Optional<T>>> caches, Object world) {
        synchronized (caches) {
            return caches.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
        }
    }

    /** A hash in [0, 1) of the seed, a position and a salt. */
    static double hash(long seed, int a, int salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (salt * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        return ((h ^ (h >>> 32)) >>> 11) * 0x1.0p-53;
    }

    static double hash(long seed, int x, int y, int z, int salt) {
        return hash(seed ^ (x * 0x632BE59BD9B4E019L) ^ (z * 0x85157AF5L), y, salt);
    }
}
