package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The volcanoes of ring 3 (split out of {@link VolcanicField}, 39d): one per cell of the jittered grid, with its kind,
 * heights and crater, the island clusters and archipelagos above low active craters, and the island clusters above lava
 * rivers. Pure: functions of the field's noises, the ring layout and the cell.
 */
final class VolcanoPlacement {
    private static final long CELL_SALT = 31;
    private static final long ISLAND_CELL_SALT = 37;
    private static final int CELL_MARGIN = 16;
    /** Largest foot radius as a share of the cell: 0.8 leaves room for the warp of the outline (0.08 * radius * noise). */
    private static final double MAX_RADIUS_SHARE = 0.8;
    private static final double MIN_FLANK = 30.0;
    /** Islands above rivers: cell of their grid (a cell carries at most one cluster). */
    static final int ISLAND_CELL = 960;
    private static final int ISLAND_CANDIDATES = 24;
    /** Effective distance behind the zone 2→3 where the horizon band of great volcanoes stands. */
    private static final double BAND_START = 60.0;
    private static final double BAND_END = 480.0;
    /**
     * Archipelagos (39d): above active calderas with a rim up to this height, with this chance. 39f: lower rims only, so
     * the islands (at least {@link #ARCHIPELAGO_LIFT} above the rim) stay below the clouds.
     */
    private static final double ARCHIPELAGO_MAX_RIM = 150.0;
    private static final double ARCHIPELAGO_CHANCE = 0.35;
    private static final double ARCHIPELAGO_LIFT = 32.0;
    /** Salt of the pillars' own hash (the cluster hash's slots belong to the islands). */
    private static final long PILLAR_SALT = 0x5DEECE66DL;
    private static final double MIN_CONE_LIFT = 40.0;

    private VolcanoPlacement() {
    }

    /** The volcano of grid cell (i, j), or null. Pure. */
    static Volcano place(VolcanicField f, int i, int j) {
        int size = f.settings().cellSize();
        long cell = SeededHash.hash(f.hashNoise(), i, j, CELL_SALT);
        double cx = i * size + CELL_MARGIN + SeededHash.unit(cell, 0) * (size - 2.0 * CELL_MARGIN);
        double cz = j * size + CELL_MARGIN + SeededHash.unit(cell, 1) * (size - 2.0 * CELL_MARGIN);
        RingLayout ringLayout = f.ringLayout();
        List<Integer> radii = ringLayout.settings().ringOuterRadii();
        double half = ringLayout.settings().transitionWidth() / 2.0;
        double inner = radii.get(1) + half;
        double outer = radii.get(2) - half;
        double distance = ringLayout.effectiveDistance(cx, cz);
        if (distance < inner || distance > outer) {
            return null;
        }
        VolcanicField.Regions r = f.regions(cx, cz);
        double behindZone = distance - inner;
        boolean band = behindZone >= BAND_START && behindZone <= BAND_END;
        double open = 1.0 - Math.max(r.seabed(), r.titan());
        // 39d: about one volcano per 500–600 blocks in the ash plain, ember woods and sulfur fields too.
        double chance = (0.07 + 0.78 * r.peaks() + 0.2 * r.flows() + 0.23 * (r.ember() + r.ashen()) + 0.28 * r.sulfur()) * open;
        if (band) {
            chance = Math.max(chance, 0.8 * (1.0 - r.seabed()));
        }
        if (SeededHash.unit(cell, 2) >= chance) {
            return null;
        }
        double greatChance = band ? 0.5 : 0.03 + 0.3 * r.peaks();
        double calderaChance = (1.0 - greatChance) * (0.22 + 0.3 * (1.0 - r.hot()));
        double kindRoll = SeededHash.unit(cell, 3);
        Volcano.Kind kind = kindRoll < greatChance ? Volcano.Kind.GREAT
                : kindRoll < greatChance + calderaChance ? Volcano.Kind.CALDERA : Volcano.Kind.CONE;
        double plainHere = f.plain(cx, cz, r);
        double u4 = SeededHash.unit(cell, 4);
        double u5 = SeededHash.unit(cell, 5);
        double u6 = SeededHash.unit(cell, 6);
        double u7 = SeededHash.unit(cell, 7);
        double u8 = SeededHash.unit(cell, 8);
        double u9 = SeededHash.unit(cell, 9);
        double rim;
        double radius;
        double crest;
        double floorRadius;
        double floor;
        double p;
        double q;
        boolean dormant;
        switch (kind) {
            case GREAT -> {
                rim = f.settings().greatMinY() + u4 * (f.settings().greatMaxY() - f.settings().greatMinY());
                radius = (rim - plainHere) * (1.25 + 0.3 * u5);
                crest = 45.0 + 25.0 * u6;
                floor = rim - (22.0 + 18.0 * u7);
                p = 1.6 + 0.3 * u9;
                q = 1.5 + 0.5 * u5;
                floorRadius = wallFoot(crest, rim - floor, q);
                dormant = false;
            }
            case CONE -> {
                // Medium cones rising 80–140 above the plain (higher where it is hottest), always below the great ones.
                double lift = Math.min(1.0, u4 * (0.6 + 0.5 * r.peaks()));
                rim = plainHere + f.settings().coneMinRise() + lift * (f.settings().coneMaxRise() - f.settings().coneMinRise());
                // Below the great volcanoes, but always well above a high plain in the peak range.
                rim = Math.max(Math.min(rim, f.settings().greatMinY() - 10.0), plainHere + MIN_CONE_LIFT);
                radius = (rim - plainHere) * (1.35 + 0.45 * u5);
                crest = 16.0 + 0.13 * radius + 8.0 * u6;
                dormant = u8 < 0.3;
                double depth = 20.0 + 12.0 * u7 + (dormant ? 10.0 : 0.0);
                floor = rim - depth;
                p = 1.5 + 0.5 * u9;
                q = 1.4 + 0.6 * u5;
                floorRadius = wallFoot(crest, depth, q);
            }
            default -> {
                rim = plainHere + 22.0 + 22.0 * u4;
                crest = 65.0 + 55.0 * u5;
                radius = crest + 50.0 + 30.0 * u6;
                dormant = u8 < 0.45;
                floor = dormant ? plainHere - 10.0 + 4.0 * u7 : plainHere - 4.0 + 8.0 * u7;
                floor = Math.min(floor, rim - 20.0);
                p = 1.4 + 0.4 * u9;
                q = 1.3 + 0.5 * u7;
                floorRadius = wallFoot(crest, rim - floor, q);
            }
        }
        // A column only looks at the 3 x 3 cells around it: the foot (plus the warp of the outline) must stay inside.
        // The flank needs some width outside the crest (surface() divides by radius - crest).
        radius = Math.max(Math.min(radius, MAX_RADIUS_SHARE * size), crest + MIN_FLANK);
        double allowance = kind == Volcano.Kind.GREAT ? 0.4 : 1.0;
        if (behindZone < allowance * radius || outer - distance < 1.05 * radius) {
            return null;
        }
        double lift = rim - plainHere;
        double rimA2 = 1.0 + 0.035 * lift;
        double rimA3 = 0.5 + 0.025 * lift;
        double rimLow = rim - rimA2 - rimA3;
        double lake = Double.NaN;
        if (!dormant) {
            lake = Math.min(floor + Math.min(7.0, rim - floor - 13.0), rimLow - 6.0);
        }
        int gullies = 7 + (int) Math.round(radius / 30.0);
        double gullyDepth = kind == Volcano.Kind.CALDERA ? 0.08 : 0.16 + 0.1 * u6;
        long shape = SeededHash.hash(f.hashNoise(), i, j, CELL_SALT + 1);
        List<Volcano.Island> islands = List.of();
        List<Volcano.Pillar> pillars = List.of();
        if (!dormant && kind == Volcano.Kind.CALDERA && rim <= ARCHIPELAGO_MAX_RIM && SeededHash.unit(shape, 5) < ARCHIPELAGO_CHANCE) {
            islands = archipelago(f, shape, cx, cz, rim);
            pillars = pillars(f, SeededHash.mix(shape + PILLAR_SALT), islands.get(0), floor);
        } else if (!dormant && rim <= f.settings().islandMaxY() - 36.0 && SeededHash.unit(shape, 0) < f.settings().islandChance()) {
            islands = cluster(f, shape, cx, cz, floorRadius, rim + 30.0);
        }
        return new Volcano(cx, cz, kind, plainHere, rim, radius, crest, floorRadius, floor, p, q, dormant, lake,
                rimA2, SeededHash.unit(shape, 1) * Math.PI * 2.0, rimA3, SeededHash.unit(shape, 2) * Math.PI * 2.0,
                gullies, SeededHash.unit(shape, 3) * Math.PI * 2.0, gullyDepth, 0.08 * radius, islands, pillars);
    }

    /**
     * Radius of the crater floor: the wall of height {@code wall} gets the width for a slope of about 1.8 under the
     * rim (steep, but never a knife edge), and the floor keeps at least a third of the crest radius.
     */
    private static double wallFoot(double crest, double wall, double q) {
        return Math.max(crest / 3.0, crest - q * wall / 1.8);
    }

    /** A cluster of 1–4 islands around (x, z) within {@code spread}, the main one with its top at or above {@code lowestTop}. */
    private static List<Volcano.Island> cluster(VolcanicField f, long seed, double x, double z, double spread, double lowestTop) {
        double maxTop = f.settings().islandMaxY() - 6.0;
        double minTop = Math.max(f.settings().islandMinY() + 4.0, lowestTop);
        if (minTop > maxTop) {
            return List.of();
        }
        double roll = SeededHash.unit(seed, 10);
        int count = roll < 0.25 ? 1 : roll < 0.6 ? 2 : roll < 0.85 ? 3 : 4;
        double baseTop = minTop + SeededHash.unit(seed, 11) * (maxTop - minTop);
        double angle0 = SeededHash.unit(seed, 12) * Math.PI * 2.0;
        double ox = x + Math.cos(angle0) * spread * 0.3 * SeededHash.unit(seed, 13);
        double oz = z + Math.sin(angle0) * spread * 0.3 * SeededHash.unit(seed, 13);
        List<Volcano.Island> islands = new ArrayList<>(count);
        double mainA = 9.0 + 6.0 * SeededHash.unit(seed, 14);
        for (int k = 0; k < count; k++) {
            int b = 20 + 8 * k;
            double a = k == 0 ? mainA : 5.0 + 5.0 * SeededHash.unit(seed, b);
            double ix = ox;
            double iz = oz;
            double top = baseTop;
            if (k > 0) {
                double angle = SeededHash.unit(seed, b + 1) * Math.PI * 2.0;
                double distance = mainA + 0.6 * a + 4.0 + 10.0 * SeededHash.unit(seed, b + 2);
                ix += Math.cos(angle) * distance;
                iz += Math.sin(angle) * distance;
                top = Mth.clamp(baseTop - 9.0 + 18.0 * SeededHash.unit(seed, b + 3), f.settings().islandMinY() + 2.0, f.settings().islandMaxY());
                top = Math.max(top, lowestTop - 6.0);
            }
            double stretch = 1.0 + 0.5 * SeededHash.unit(seed, b + 4);
            double thickness = Math.min(28.0, a * (1.4 + 0.8 * SeededHash.unit(seed, b + 5)));
            islands.add(island(f, SeededHash.mix(seed + b), ix, iz, a, stretch, SeededHash.unit(seed, b + 6) * Math.PI, top,
                    thickness, SeededHash.unit(seed, b + 7) < 0.3 ? 1 : 0, SeededHash.unit(seed, b + 7) * 20.0));
        }
        return islands;
    }

    /**
     * An archipelago (39d) above a low active caldera: a main island 60–100 wide with lava falls from its edge, and 2–5
     * smaller ones around it. 39f: below the cloud layer (tops up to {@code island_max_y}), so the pillars are shorter.
     */
    private static List<Volcano.Island> archipelago(VolcanicField f, long seed, double x, double z, double rim) {
        int count = 3 + (int) (3.99 * SeededHash.unit(seed, 40));
        List<Volcano.Island> islands = new ArrayList<>(count);
        double maxTop = f.settings().islandMaxY();
        double mainA = 30.0 + 20.0 * SeededHash.unit(seed, 41);
        double mainStretch = 1.0 + 0.3 * SeededHash.unit(seed, 42);
        double mainTop = Math.min(maxTop, Math.max(rim + ARCHIPELAGO_LIFT, 160.0 + 25.0 * SeededHash.unit(seed, 43)));
        islands.add(island(f, SeededHash.mix(seed + 47), x, z, mainA, mainStretch, SeededHash.unit(seed, 44) * Math.PI, mainTop,
                Math.min(34.0, 0.75 * mainA), 2 + (int) (1.99 * SeededHash.unit(seed, 45)), SeededHash.unit(seed, 46) * 6.3));
        for (int k = 1; k < count; k++) {
            int b = 50 + 8 * k;
            double a = 10.0 + 12.0 * SeededHash.unit(seed, b);
            double angle = SeededHash.unit(seed, b + 1) * Math.PI * 2.0;
            double distance = mainA * mainStretch + a + 6.0 + 14.0 * SeededHash.unit(seed, b + 2);
            double top = Mth.clamp(mainTop - 12.0 + 24.0 * SeededHash.unit(seed, b + 3), rim + ARCHIPELAGO_LIFT - 8.0, maxTop);
            islands.add(island(f, SeededHash.mix(seed + b), x + Math.cos(angle) * distance, z + Math.sin(angle) * distance, a,
                    1.0 + 0.4 * SeededHash.unit(seed, b + 4), SeededHash.unit(seed, b + 5) * Math.PI, top,
                    Math.min(24.0, 1.1 * a), SeededHash.unit(seed, b + 6) < 0.4 ? 1 : 0, SeededHash.unit(seed, b + 7) * 6.3));
        }
        return islands;
    }

    /** An island with its underside lobes (from {@code seed}) and its lava falls, traced over its real outline. */
    private static Volcano.Island island(VolcanicField f, long seed, double x, double z, double a, double stretch, double angle,
                                         double top, double thickness, int springs, double springAngle) {
        int lobes = 2 + (int) (1.99 * SeededHash.unit(seed, 0));
        double lobePhase = SeededHash.unit(seed, 1) * Math.PI * 2.0;
        Volcano.Island shape = new Volcano.Island(x, z, a, stretch, angle, top, thickness, lobes, lobePhase, List.of());
        if (springs == 0) {
            return shape;
        }
        List<Volcano.LavaFall> falls = new ArrayList<>(springs);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        for (int k = 0; k < springs; k++) {
            double t = springAngle + k * Math.PI * 2.0 / springs;
            // The outward direction of the ellipse at parameter t, in world axes.
            double ex = a * stretch * Math.cos(t);
            double ez = a * Math.sin(t);
            double dx = ex * cos - ez * sin;
            double dz = ex * sin + ez * cos;
            double length = Math.hypot(dx, dz);
            // The fall column (one past the last island column) must stay inside the island's reach, or no column would
            // ever look at it.
            double to = Math.min(1.6 * length, shape.reach() - 2.0);
            Volcano.LavaFall fall = LavaFallTrace.trace(x, z, dx / length, dz / length, 0.6 * length, to,
                    (bx, bz) -> shape.columnTop(bx + 0.5, bz + 0.5, f.warpAt(bx + 0.5, bz + 0.5)));
            if (fall != null) {
                falls.add(fall);
            }
        }
        return new Volcano.Island(x, z, a, stretch, angle, top, thickness, lobes, lobePhase, List.copyOf(falls));
    }

    /**
     * One or two basalt pillars (39f: columnar, leaning, with ledges) from the crater floor up into the main island of an
     * archipelago.
     */
    private static List<Volcano.Pillar> pillars(VolcanicField f, long seed, Volcano.Island main, double floor) {
        int count = SeededHash.unit(seed, 0) < 0.5 ? 1 : 2;
        List<Volcano.Pillar> pillars = new ArrayList<>(count);
        for (int k = 0; k < count; k++) {
            int b = 40 * (k + 1);
            double angle = SeededHash.unit(seed, b) * Math.PI * 2.0;
            double d = 0.25 + 0.2 * SeededHash.unit(seed, b + 1);
            double px = main.x() + Math.cos(angle) * main.a() * d;
            double pz = main.z() + Math.sin(angle) * main.a() * d;
            double radius = 2.6 + 1.6 * SeededHash.unit(seed, b + 5);
            double lean = 3.0 + 5.0 * SeededHash.unit(seed, b + 2);
            double leanAngle = SeededHash.unit(seed, b + 3) * Math.PI * 2.0;
            double top = pillarTop(f, main, px + Math.cos(leanAngle) * lean, pz + Math.sin(leanAngle) * lean, radius);
            int ledges = 1 + (int) (2.99 * SeededHash.unit(seed, b + 4));
            double[] ledgeY = new double[ledges];
            double[] ledgeAngle = new double[ledges];
            double[] ledgeLength = new double[ledges];
            for (int l = 0; l < ledges; l++) {
                ledgeY[l] = floor + (top - floor) * (0.2 + 0.6 * SeededHash.unit(seed, b + 10 + 3 * l));
                ledgeAngle[l] = SeededHash.unit(seed, b + 11 + 3 * l) * Math.PI * 2.0;
                ledgeLength[l] = 2.0 + 3.0 * SeededHash.unit(seed, b + 12 + 3 * l);
            }
            pillars.add(new Volcano.Pillar(px, pz, radius, floor, top,
                    Math.cos(leanAngle) * lean, Math.sin(leanAngle) * lean, SeededHash.unit(seed, b + 6) * 6.3,
                    SeededHash.unit(seed, b + 7) * 6.3, ledgeY, ledgeAngle, ledgeLength));
        }
        return List.copyOf(pillars);
    }

    /**
     * The top of a pillar whose (leaning) axis ends at (tx, tz): five blocks into the island above the highest point of
     * its underside over the flared top (1.7 × the radius, plus the lobes), so it never ends below the island; kept two
     * blocks under the island's top there.
     */
    private static double pillarTop(VolcanicField f, Volcano.Island main, double tx, double tz, double radius) {
        double highestBottom = Double.NEGATIVE_INFINITY;
        double lowestTop = Double.POSITIVE_INFINITY;
        double spread = radius * 1.7 * 1.24;
        for (int k = 0; k <= 8; k++) {
            double sx = k == 8 ? tx : tx + spread * Math.cos(k * Math.PI / 4.0);
            double sz = k == 8 ? tz : tz + spread * Math.sin(k * Math.PI / 4.0);
            double d = main.distance(sx, sz, 0.18 * f.warpAt(sx, sz));
            if (d >= 1.0) {
                continue;
            }
            highestBottom = Math.max(highestBottom, main.bottomAt(d, main.theta(sx, sz)));
            lowestTop = Math.min(lowestTop, main.topAt(d));
        }
        return Math.max(highestBottom + 1.0, Math.min(highestBottom + 5.0, lowestTop - 2.0));
    }

    /** Island cluster above a lava river in island cell (i, j), or none. Pure. */
    static List<Volcano.Island> channelCluster(VolcanicField f, int i, int j) {
        long cell = SeededHash.hash(f.hashNoise(), i, j, ISLAND_CELL_SALT);
        if (SeededHash.unit(cell, 0) >= f.settings().islandChance() * 0.6) {
            return List.of();
        }
        for (int k = 0; k < ISLAND_CANDIDATES; k++) {
            double x = i * ISLAND_CELL + 64.0 + SeededHash.unit(cell, 1 + 2 * k) * (ISLAND_CELL - 128.0);
            double z = j * ISLAND_CELL + 64.0 + SeededHash.unit(cell, 2 + 2 * k) * (ISLAND_CELL - 128.0);
            VolcanicField.Column probe = f.probeColumn();
            f.fill(probe, Mth.floor(x), Mth.floor(z), VolcanicField.Mode.HEIGHT);
            if (probe.channel >= 0.6) {
                return cluster(f, SeededHash.mix(cell + k), x, z, 10.0, probe.preHeight + 60.0);
            }
        }
        return List.of();
    }
}
