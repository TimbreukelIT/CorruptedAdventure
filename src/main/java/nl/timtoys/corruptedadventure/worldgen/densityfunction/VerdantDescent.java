package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * The descent of {@code verdant_falls} (W-terrein 2c): ring 4's terrain over the zone 3→4, from ring 3's height at the
 * {@code steam_fens} rim down to a foot river at the sea level, in treads like the cascades'.
 * <p>
 * Coordinates: {@code u} across the zone (blocks from its inner edge, 0…Z, Z = the zone's width; the boundary noise is
 * already in the effective distance, so the whole descent wanders with it) and {@code s} along it (the arc on the
 * zone's middle line, periodic, {@link RingArc}). Over u:
 * <ul>
 *   <li>0 … {@link IslandField#VERDANT_EDGE_SHARE} Z (60 of 256): {@code steam_fens}, ring 3's terrain; ring 4's input
 *   blends from ring 3's height of the same column to the top tread, so the mix of the two rings meets;</li>
 *   <li>then a flat top terrace ({@link Profile#topWidth}) at the rim height {@code T} (ring 3's height on the rim,
 *   sampled every {@link #GRID} blocks along s, smoothed, clamped to {@link #RIM_MIN}–{@link #RIM_MAX}, remembered per
 *   world);</li>
 *   <li>the descent: a ramp {@code R} from {@code T} down to {@link #BANK_TOP} (narrow steps above, wide below:
 *   v · (1 + c (1 − v)), c about 0.25), its stair lines bent by a slow wave, stepped by {@link Ring4Relief#terrace} (treads of 8
 *   on the levels 64 + 8k, 60 % tread / 40 % riser with a rounded edge and foot, Q5);</li>
 *   <li>the foot river: bed at {@link #RIVER_BED}, banks of {@link #RIVER_SLOPE} like ring 4's rivers, meandering along
 *   s; beyond it the ground blends to ring 4's own terrain ({@link Ring4Relief}) at the zone's outer edge, so ring 4's
 *   rivers open into the foot river.</li>
 * </ul>
 * Distances along u are shares of the zone (for a zone of 256, the numbers in the constants), so an older world with a
 * zone of 128 gets the same descent, steeper. The basins on the treads are {@link VerdantBasins}.
 * <p>
 * Pure: functions of the layout, the noises, ring 3's terrain and x, z; the rim profile is remembered per world.
 */
final class VerdantDescent {
    /** The lowest tread and the top of the foot river's banks; the treads lie on BANK_TOP + 8k (the cascades' levels). */
    static final double BANK_TOP = 64.0;
    static final double STEP = Ring4Relief.TERRACE_STEP;
    /** The share of a step that is tread (Q5: 60 %); the rest is the riser. */
    static final double TREAD = Ring4Relief.TERRACE_TREAD;
    /** The rim height's bounds: at least 24, at most 56 above the lowest tread. */
    static final double RIM_MIN = BANK_TOP + 24.0;
    static final double RIM_MAX = BANK_TOP + 56.0;
    /** The rim height without ring 3's terrain (other dimensions, tests on other seeds). */
    static final double NO_RING_3 = 100.0;
    /** Blocks of arc between two samples of the rim profile. */
    static final double GRID = 4.0;
    /**
     * The rim height is smoothed over ±SMOOTH samples (triangular, ±128 blocks): ring 3's crags must not swirl the
     * stair lines, which are the ramp's contour lines and should run along the ring (the steam_fens strip of 60 blocks
     * takes up the difference to ring 3's own height).
     */
    private static final int SMOOTH = 32;
    /** The zone width the block numbers below are given for. */
    static final double REFERENCE_WIDTH = 256.0;
    /** The flat top terrace after the rim (blocks): TOP_WIDTH ± TOP_SWING along s. */
    private static final double TOP_WIDTH = 10.0;
    private static final double TOP_SWING = 4.0;
    /** The foot river's middle (blocks from the zone's inner edge), meandering RIVER_SWING along s. */
    static final double RIVER_AT = 226.0;
    private static final double RIVER_SWING = 7.0;
    /** Half the width of its bed: BED_HALF ± BED_SWING (12–18 wide). */
    private static final double BED_HALF = 7.0;
    private static final double BED_SWING = 2.0;
    static final double RIVER_BED = Ring4Relief.RIVER_BED;
    static final double RIVER_SLOPE = Ring4Relief.RIVER_SLOPE;
    /**
     * The descent's curve {@code c} (narrow treads above, wide below): the ramp falls as v · (1 + c (1 − v)), so it is
     * (1 + c) / (1 − c) as steep at the top as at the foot. CURVE ± CURVE_SWING along s, but never steeper at the top
     * than {@link #STEEPEST} where the drop allows (the risers stay walkable and at least ~8 wide).
     */
    private static final double CURVE = 0.25;
    private static final double CURVE_SWING = 0.15;
    private static final double STEEPEST = 0.3;
    /** How far the wave bends the stair lines: this share of the drop as rise (≈ ±12 blocks across). */
    private static final double WAVE = 0.07;
    /** Beyond the river the outer bank steepens towards the zone's edge, so the ground there is ring 4's own. */
    private static final double OUTER_LIFT = 200.0;
    /** The ramp's noise and the profile's noises: scale and offsets of the warp noise (≈ 250 / 430 blocks). */
    private static final double WAVE_SCALE = 0.5;
    private static final double PROFILE_SCALE = 0.3;

    private final IslandField field;

    VerdantDescent(IslandField field) {
        this.field = field;
    }

    // ---------------------------------------------------------------- the zone

    /** The zone 3→4's width (blocks). */
    static double width(RingLayout layout) {
        return layout.settings().transitionWidth(Ring.RING_3.ordinal());
    }

    /** The effective distance of the zone's inner edge. */
    static double zoneStart(RingLayout layout) {
        return layout.settings().ringOuterRadii().get(Ring.RING_3.ordinal()) - width(layout) / 2.0;
    }

    /** The radius of the zone's middle line: s is measured on it. */
    static double midRadius(RingLayout layout) {
        return layout.settings().ringOuterRadii().get(Ring.RING_3.ordinal());
    }

    static double perimeter(RingLayout layout) {
        return RingArc.perimeter(midRadius(layout));
    }

    /** Where {@code steam_fens} ends and the descent begins (blocks across the zone). */
    static double rimAt(RingLayout layout) {
        return IslandField.VERDANT_EDGE_SHARE * width(layout);
    }

    /** Blocks of the reference zone in this zone. */
    private static double scale(RingLayout layout) {
        return width(layout) / REFERENCE_WIDTH;
    }

    // ---------------------------------------------------------------- the rim profile

    /**
     * The profile along s at one sample: the rim height, the top terrace's width, the river's middle and bed half width
     * (blocks across the zone) and the descent's curve.
     */
    record Profile(double rim, double topWidth, double river, double bedHalf, double curve) {
        /** Where the descent leaves the top terrace (blocks across the zone). */
        double descentStart(double rimAt) {
            return rimAt + topWidth;
        }

        /** Where the river's inner bank reaches {@link #BANK_TOP}: the foot of the descent. */
        double bankTop() {
            return river - bedHalf - (BANK_TOP - RIVER_BED) / RIVER_SLOPE;
        }

        static Profile lerp(double t, Profile a, Profile b) {
            return new Profile(Mth.lerp(t, a.rim, b.rim), Mth.lerp(t, a.topWidth, b.topWidth), Mth.lerp(t, a.river, b.river),
                    Mth.lerp(t, a.bedHalf, b.bedHalf), Mth.lerp(t, a.curve, b.curve));
        }
    }

    /** The number of profile samples round the loop. */
    int gridCount() {
        return RingArc.cellCount(perimeter(field.ringLayout()), GRID);
    }

    /** The profile at a position along the loop: linear between the two samples around it. */
    Profile profile(double s) {
        RingLayout layout = field.ringLayout();
        int count = gridCount();
        double spacing = perimeter(layout) / count;
        double k = RingArc.wrap(s, perimeter(layout)) / spacing;
        int j = (int) Math.floor(k);
        return Profile.lerp(k - j, sample(Math.floorMod(j, count)), sample(Math.floorMod(j + 1, count)));
    }

    /** Profile sample {@code j} (in [0, count)), decided once per world. */
    Profile sample(int j) {
        return field.decisions().once(field.decisions().verdantProfiles, j, () -> placeSample(j));
    }

    private Profile placeSample(int j) {
        int count = gridCount();
        double sum = 0.0;
        double weights = 0.0;
        for (int i = -SMOOTH; i <= SMOOTH; i++) {
            double w = SMOOTH + 1 - Math.abs(i);
            // A volcano or crag on the rim weighs no more than a little beyond the bounds.
            sum += w * Mth.clamp(rawRim(Math.floorMod(j + i, count)), RIM_MIN - 8.0, RIM_MAX + 8.0);
            weights += w;
        }
        RingLayout layout = field.ringLayout();
        double angle = j * (perimeter(layout) / count) / midRadius(layout);
        double mx = midRadius(layout) * Math.cos(angle) * PROFILE_SCALE;
        double mz = midRadius(layout) * Math.sin(angle) * PROFILE_SCALE;
        Ring4Relief relief = field.relief();
        double scale = scale(layout);
        double top = (TOP_WIDTH + TOP_SWING * unit(relief.warpAt(mx + 811.0, mz - 211.0))) * scale;
        double river = (RIVER_AT + RIVER_SWING * unit(relief.warpAt(mx - 1717.0, mz + 909.0))) * scale;
        double bed = (BED_HALF + BED_SWING * unit(relief.warpAt(mx + 2903.0, mz + 3301.0))) * scale;
        double rim = Mth.clamp(sum / weights, RIM_MIN, RIM_MAX);
        double curve = CURVE + CURVE_SWING * unit(relief.warpAt(mx - 4111.0, mz - 2707.0));
        // Not steeper at the top than STEEPEST where the drop over the descent allows it (else as even as it can be).
        Profile flat = new Profile(rim, top, river, bed, 0.0);
        double length = flat.bankTop() - flat.descentStart(rimAt(layout));
        double room = STEEPEST * length / Math.max(1.0, rim - BANK_TOP) - 1.0;
        return new Profile(rim, top, river, bed, Mth.clamp(Math.min(curve, room), 0.0, 0.6));
    }

    /** A noise value held to −1..1. */
    private static double unit(double noise) {
        return Mth.clamp(noise, -1.0, 1.0);
    }

    /** Ring 3's height on the rim (u = the rim) on the ray of sample {@code j}, remembered per world. */
    private double rawRim(int j) {
        return field.decisions().once(field.decisions().verdantRims, j, () -> {
            VolcanicField ring3 = field.ring3();
            if (ring3 == null) {
                return NO_RING_3;
            }
            RingLayout layout = field.ringLayout();
            double angle = j * (perimeter(layout) / gridCount()) / midRadius(layout);
            double[] p = onRay(layout, angle, zoneStart(layout) + rimAt(layout));
            return ring3.column(Mth.floor(p[0]), Mth.floor(p[1])).height();
        });
    }

    /**
     * The point on the ray at {@code angle} whose effective distance is {@code target} (the boundary noise moves it by up
     * to its amplitude; where it changes fast along the ray a fixed-point step would not settle, so: bisection).
     */
    static double[] onRay(RingLayout layout, double angle, double target) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double reach = layout.settings().boundaryNoise() + 16.0;
        double lo = Math.max(0.0, target - reach);
        double hi = target + reach;
        for (int n = 0; n < 40; n++) {
            double mid = 0.5 * (lo + hi);
            if (layout.effectiveDistance(mid * cos, mid * sin) < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double r = 0.5 * (lo + hi);
        return new double[]{r * cos, r * sin};
    }

    // ---------------------------------------------------------------- per column

    /** Where a column lies in the zone and the descent's values there ({@link #locate}). */
    static final class Point {
        static final int OUTSIDE = 0;
        /** In {@code steam_fens}: ring 4's input blends to the top terrace. */
        static final int STRIP = 1;
        /** On the descent, the foot river or the ground beyond it, up to the zone's outer edge. */
        static final int DESCENT = 2;

        int kind;
        double u;
        double s;
        /** The zone's width and the rim's u. */
        double width;
        double rimAt;
        Profile profile;
        /** The ramp (continuous height before the steps) and its share v of the descent (0 on the top terrace). */
        double ramp;
        double v;
        /** The treads' height (with the steam_fens blend, without the river and the basins). */
        double treads;
        /** The body (tread) index: floor((ramp − BANK_TOP) / STEP), −1 off the descent. */
        int body;
    }

    /** Fills {@code p} for the column at (x, z); {@link Point#OUTSIDE} outside the zone 3→4. */
    Point locate(double x, double z, Point p) {
        RingLayout layout = field.ringLayout();
        double u = layout.effectiveDistance(x, z) - zoneStart(layout);
        double width = width(layout);
        p.kind = Point.OUTSIDE;
        p.body = -1;
        if (u < 0.0 || u > width || width <= 0.0) {
            return p;
        }
        p.u = u;
        p.width = width;
        p.rimAt = rimAt(layout);
        p.s = RingArc.arc(x, z, midRadius(layout));
        p.profile = profile(p.s);
        p.kind = u < p.rimAt ? Point.STRIP : Point.DESCENT;
        p.ramp = ramp(x, z, u, p.rimAt, p.profile, p);
        double detail = field.relief().detailAt(x, z);
        double top = treadHeight(p.profile.rim(), detail);
        if (p.kind == Point.STRIP) {
            VolcanicField ring3 = field.ring3();
            double own = ring3 == null ? top : ring3.column(Mth.floor(x), Mth.floor(z)).height();
            p.treads = Mth.lerp(Ring4Relief.smooth(u, 0.0, p.rimAt), own, top);
        } else {
            p.treads = treadHeight(p.ramp, detail);
            p.body = (int) Math.floor((p.ramp - BANK_TOP) / STEP);
        }
        return p;
    }

    /** The ramp at (x, z) with the profile there, or NaN off the descent (for the neighbours of a column). */
    double rampAt(double x, double z) {
        RingLayout layout = field.ringLayout();
        double u = layout.effectiveDistance(x, z) - zoneStart(layout);
        double rim = rimAt(layout);
        if (u < rim || u > width(layout)) {
            return Double.NaN;
        }
        return ramp(x, z, u, rim, profile(RingArc.arc(x, z, midRadius(layout))), null);
    }

    /**
     * The ramp: the rim height on the top terrace, then down to {@link #BANK_TOP} at the foot along the curve, plus the
     * wave (zero at both ends) that bends the stair lines.
     */
    private double ramp(double x, double z, double u, double rimAt, Profile profile, Point out) {
        double start = profile.descentStart(rimAt);
        double v = Mth.clamp((u - start) / Math.max(8.0, profile.bankTop() - start), 0.0, 1.0);
        double drop = profile.rim() - BANK_TOP;
        double wave = field.relief().warpAt(WAVE_SCALE * x + 1234.5, WAVE_SCALE * z - 987.5);
        if (out != null) {
            out.v = v;
        }
        double fall = v * (1.0 + profile.curve() * (1.0 - v));
        return profile.rim() - drop * fall + WAVE * drop * wave * 4.0 * v * (1.0 - v);
    }

    /** The treads of a ramp value: {@link Ring4Relief#terrace} on BANK_TOP + 8k, detail on the risers only. */
    static double treadHeight(double ramp, double detail) {
        return BANK_TOP + Ring4Relief.steps(ramp - BANK_TOP, detail);
    }

    /** The level of body {@code b}'s tread. */
    static double level(int body) {
        return BANK_TOP + STEP * body;
    }

    /** The top (exclusive) of body {@code b}'s still water: one block under its tread's top block. */
    static int waterTop(int body) {
        return (int) level(body) - 1;
    }

    // ---------------------------------------------------------------- the foot river

    /**
     * The ground with the foot river: {@code inner} (the treads with their basins; the lowest tread beyond the bank's
     * top) up to the river's middle, beyond it a blend from the lowest tread to ring 4's own terrain {@code relief} at
     * the zone's edge; the river cut into it (its outer bank steepening near the edge, so ring 4's terrain stands alone
     * there).
     */
    static double withRiver(Point p, double inner, double relief) {
        Profile profile = p.profile;
        double land = p.u <= profile.river() ? inner
                : Mth.lerp(Ring4Relief.smooth(p.u, profile.river(), p.width), BANK_TOP, relief);
        double across = Math.abs(p.u - profile.river()) - profile.bedHalf();
        double cut = RIVER_BED + Math.max(0.0, across) * RIVER_SLOPE;
        if (p.u > profile.river()) {
            double from = Math.min(profile.river() + profile.bedHalf() + 6.0, p.width - 2.0);
            cut += OUTER_LIFT * Ring4Relief.smooth(p.u, from, p.width);
        }
        return Math.min(land, cut);
    }

    /** Whether the ground at a point needs ring 4's own terrain (beyond the river's middle). */
    static boolean needsRelief(Point p) {
        return p.kind == Point.DESCENT && p.u > p.profile.river() - 1.0;
    }

    /** 0..1: the foot river's channel (1 on its bed). */
    static double river(Point p) {
        if (p.kind != Point.DESCENT) {
            return 0.0;
        }
        double across = Math.abs(p.u - p.profile.river());
        return 1.0 - Ring4Relief.smooth(across, p.profile.bedHalf(), p.profile.bedHalf() + 4.0);
    }
}
