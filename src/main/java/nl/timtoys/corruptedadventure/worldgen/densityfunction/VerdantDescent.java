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
 *   v · (1 + c (1 − v)), c about 0.25), its stair lines bent by a slow wave and, polish round "afdaling", by lobes,
 *   small inlets and a drift per level (the column's u displaced, so the lines bend and wander apart or together), stepped
 *   on the levels 64 + 8k ({@link #treadHeight}: 60 % tread / 40 % riser with a rounded edge and foot, Q5; here and there
 *   a rocky knick, the riser's slope gathered in its middle);</li>
 *   <li>the foot river: bed at {@link #RIVER_BED}, banks of {@link #RIVER_SLOPE} like ring 4's rivers, meandering along
 *   s; beyond it a bank of at most {@link #OUTER_SLOPE} up to ring 4's own terrain ({@link Ring4Relief}), so ring 4's
 *   rivers open into the foot river. Where ring 4 stands high at the zone's outer edge the river lies further in, so
 *   that bank is a slope, never a wall (decision 2026-10-15).</li>
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
    /** How far the slow wave bends the stair lines: this share of the drop as rise (≈ ±12 blocks across, ≈ 250 along). */
    private static final double WAVE = 0.07;
    /**
     * Beyond the river's beach (the bank of {@link #RIVER_SLOPE} up to {@link #BANK_TOP}) the outer bank rises at most
     * this much per block up to ring 4's own terrain (decision 2026-10-15, "river inwards": a slope, never a wall).
     */
    static final double OUTER_SLOPE = 0.8;
    /** The river moves inwards (blocks) until its outer bank reaches ring 4's height this far before the zone's edge. */
    private static final double OUTER_MARGIN = 3.0;
    /** But never further in than this (blocks across a zone of 256): it stays well inside ring 4's half. */
    private static final double RIVER_INNERMOST = 150.0;
    /**
     * Ring 4's height at the zone's edge, per profile sample: the highest of a few points across the edge, the highest
     * of those within ±OUTER_REACH samples (±32 blocks), averaged over ±OUTER_SMOOTH (never under the sample's own).
     */
    private static final int OUTER_REACH = 8;
    private static final int OUTER_SMOOTH = 8;
    /** How softly the river's position takes the lower of its own and the edge's limit (blocks). */
    private static final double RIVER_SOFTNESS = 8.0;
    /** The ramp's noise and the profile's noises: scale and offsets of the warp noise (≈ 250 / 430 blocks). */
    private static final double WAVE_SCALE = 0.5;
    private static final double PROFILE_SCALE = 0.3;
    // Polish round "afdaling" (2026-10-15): no rice fields. The stair lines (the ramp's contour lines) move across the
    // zone by a displacement of the column's u, in blocks of the reference zone: lobes and inlets of the treads at a few
    // tens of blocks, and a drift that changes with the level, so neighbouring lines wander apart (a wide, gentle tread)
    // or together (a steep knick, two risers nearly one) instead of running in parallel.
    /** The lobes and spurs: this many blocks (warp noise, ≈ 70 blocks along the ring, ≈ 160 across it). */
    static final double LOBE = 12.0;
    private static final double LOBE_SCALE = 1.8;
    private static final double LOBE_ACROSS = 0.8;
    /** Small inlets and tongues of the edges: this many blocks (≈ 25 blocks along the ring, ≈ 64 across). */
    static final double CRINKLE = 3.0;
    private static final double CRINKLE_SCALE = 5.0;
    private static final double CRINKLE_ACROSS = 2.0;
    /** The drift per level: this many blocks (≈ 64 blocks along the ring, one turn per DRIFT_LEVELS of ramp). */
    static final double DRIFT = 10.0;
    private static final double DRIFT_SCALE = 2.0;
    private static final double DRIFT_LEVELS = 14.0;
    /**
     * The riser's profile varies with the detail noise d (≈ 32 blocks): where d is high its slope gathers in the middle,
     * up to KNICK + 1 times steeper (a rocky knick: ≥ 2 per block on the steeper risers, where the surface's slope rule
     * shows stone), elsewhere the smootherstep of Q5 as it was. The treads stay level and the step's ends exact (no hair
     * above a level).
     */
    static final double KNICK = 0.8;
    private static final double KNICK_FROM = 0.15;
    private static final double KNICK_TO = 0.6;
    /** The ramp's floor in ring 3's half (the edge of tread 72) and how far into ring 4's half it fades (blocks). */
    private static final double GUARD_LEVEL = BANK_TOP + STEP * (1.0 + TREAD);
    private static final double GUARD_FADE = 16.0;
    /** Over this share of the descent (v) the risers' profile fades in from the top terrace. */
    private static final double TERRACE_FADE = 0.1;
    /** How far the foot river wobbles inwards (blocks, 0 … 2 BANK_WOBBLE) and the noise's scale (warp noise, ≈ 24 blocks). */
    static final double BANK_WOBBLE = 1.0;
    private static final double BANK_WOBBLE_SCALE = 5.3;
    /** The top terrace's least width where the lobes eat into it (blocks). */
    private static final double TOP_LEAST = 3.0;

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
        double bed = (BED_HALF + BED_SWING * unit(relief.warpAt(mx + 2903.0, mz + 3301.0))) * scale;
        // The river moves inwards where ring 4 stands high at the zone's edge, so its outer bank rises to it at
        // OUTER_SLOPE: the lower (softly) of its own meander and that limit, never past RIVER_INNERMOST.
        double outer = 0.0;
        double outerWeights = 0.0;
        for (int i = -OUTER_SMOOTH; i <= OUTER_SMOOTH; i++) {
            double w = OUTER_SMOOTH + 1 - Math.abs(i);
            outer += w * outerMax(Math.floorMod(j + i, count));
            outerWeights += w;
        }
        outer /= outerWeights;
        double own = (RIVER_AT + RIVER_SWING * unit(relief.warpAt(mx - 1717.0, mz + 909.0))) * scale;
        double limit = width(layout) - OUTER_MARGIN - bed - (BANK_TOP - RIVER_BED) / RIVER_SLOPE
                - Math.max(0.0, outer - BANK_TOP) / OUTER_SLOPE;
        double river = Math.max(RIVER_INNERMOST * scale, VolcanicMath.smoothMin(own, limit, RIVER_SOFTNESS * scale));
        double rim = Mth.clamp(sum / weights, RIM_MIN, RIM_MAX);
        double curve = CURVE + CURVE_SWING * unit(relief.warpAt(mx - 4111.0, mz - 2707.0));
        // Not steeper at the top than STEEPEST where the drop over the descent allows it (else as even as it can be).
        Profile flat = new Profile(rim, top, river, bed, 0.0);
        double length = flat.bankTop() - flat.descentStart(rimAt(layout));
        double room = STEEPEST * length / Math.max(1.0, rim - BANK_TOP) - 1.0;
        return new Profile(rim, top, river, bed, Mth.clamp(Math.min(curve, room), 0.0, 0.6));
    }

    /** The highest of {@link #rawOuter} within ±{@link #OUTER_REACH} samples, remembered per world. */
    private double outerMax(int j) {
        return field.decisions().once(field.decisions().verdantOuterMax, j, () -> {
            int count = gridCount();
            double highest = Double.NEGATIVE_INFINITY;
            for (int i = -OUTER_REACH; i <= OUTER_REACH; i++) {
                highest = Math.max(highest, rawOuter(Math.floorMod(j + i, count)));
            }
            return highest;
        });
    }

    /** Ring 4's own height across the zone's outer edge on the ray of sample {@code j} (the highest of 4 points). */
    private double rawOuter(int j) {
        return field.decisions().once(field.decisions().verdantOuter, j, () -> {
            RingLayout layout = field.ringLayout();
            double angle = j * (perimeter(layout) / gridCount()) / midRadius(layout);
            double highest = Double.NEGATIVE_INFINITY;
            for (double du = -8.0; du <= 16.0; du += 8.0) {
                double[] p = onRay(layout, angle, zoneStart(layout) + width(layout) + du);
                highest = Math.max(highest, field.baseHeight(p[0], p[1]));
            }
            return highest;
        });
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
        /** The detail noise at the column (the risers' profile, {@link #treadHeight}). */
        double detail;
        /** How far the foot river lies inwards of its profile here (blocks, 0 … −2 {@link #BANK_WOBBLE}; 0 off the descent). */
        double wobble;

        /** The foot river's middle at this column. */
        double river() {
            return profile.river() + wobble;
        }
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
        // The risers' profile: not on the top terrace, which may stand part way up a riser (the rim height), nearly level,
        // where any noise would scatter loose blocks over the integer it hovers about.
        double detail = Mth.lerp(Ring4Relief.smooth(p.v, 0.0, TERRACE_FADE), -1.0, field.relief().detailAt(x, z));
        p.detail = detail;
        double top = treadHeight(p.profile.rim(), -1.0);
        if (p.kind == Point.STRIP) {
            VolcanicField ring3 = field.ring3();
            double own = ring3 == null ? top : ring3.column(Mth.floor(x), Mth.floor(z)).height();
            p.treads = Mth.lerp(Ring4Relief.smooth(u, 0.0, p.rimAt), own, top);
        } else {
            p.treads = treadHeight(p.ramp, detail);
            p.body = (int) Math.floor((p.ramp - BANK_TOP) / STEP);
        }
        // The foot river wobbles a little (both banks with it), so their shore lines do not draw a regular sawtooth
        // across the block grid; inwards only, so the outer bank keeps its room to rise to ring 4 (decision "river
        // inwards").
        p.wobble = 0.0;
        if (p.kind == Point.DESCENT) {
            double noise = Mth.clamp(field.relief().warpAt(BANK_WOBBLE_SCALE * x + 523.5, BANK_WOBBLE_SCALE * z - 1789.5), -1.0, 1.0);
            p.wobble = BANK_WOBBLE * scale(layout) * (noise - 1.0);
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
        double length = Math.max(8.0, profile.bankTop() - start);
        double drop = profile.rim() - BANK_TOP;
        Ring4Relief relief = field.relief();
        // The level the column would have without the displacement: the drift's third axis.
        double plain = profile.rim() - drop * fall(Mth.clamp((u - start) / length, 0.0, 1.0), profile.curve());
        // The noises run on the column's projection onto the zone's middle line: they change along the ring at their own
        // scale and across it only slowly (through u), so the lines bend without the risers being squeezed.
        double r = Math.max(1.0, Math.hypot(x, z));
        double mid = midRadius(field.ringLayout());
        double px = x * mid / r;
        double pz = z * mid / r;
        double across = u / scale(field.ringLayout());
        double shift = LOBE * relief.warpAt(LOBE_SCALE * px - 3713.5, LOBE_ACROSS * across, LOBE_SCALE * pz + 1931.5)
                + CRINKLE * relief.warpAt(CRINKLE_SCALE * px + 811.5, CRINKLE_ACROSS * across + 97.0, CRINKLE_SCALE * pz - 2203.5)
                + DRIFT * relief.warpAt(DRIFT_SCALE * px + 2417.5, plain * (128.0 / DRIFT_LEVELS), DRIFT_SCALE * pz - 4519.5);
        // The top terrace may widen freely, but it narrows to no less than TOP_LEAST: the strip's blend at the rim still
        // meets the top tread. (A fade over the terrace would fold the ramp: its slope times a large shift.)
        shift = VolcanicMath.smoothMin(shift * scale(field.ringLayout()), profile.topWidth() - TOP_LEAST * scale(field.ringLayout()), 2.0);
        double v = Mth.clamp((u + shift - start) / length, 0.0, 1.0);
        double wave = WAVE * drop * relief.warpAt(WAVE_SCALE * x + 1234.5, WAVE_SCALE * z - 987.5);
        if (out != null) {
            out.v = v;
        }
        double ramp = profile.rim() - drop * fall(v, profile.curve()) + wave * 4.0 * v * (1.0 - v);
        // Ring 3's half stays above its seal (≥ 65): there the displacement never takes the ramp below the plain one's
        // or below the edge of tread 72, whichever is lower; fading out over GUARD_FADE blocks into ring 4's half.
        double half = 0.5 * width(field.ringLayout());
        double guard = 1.0 - Ring4Relief.smooth(u, half, half + GUARD_FADE * scale(field.ringLayout()));
        if (guard > 0.0) {
            double v0 = Mth.clamp((u - start) / length, 0.0, 1.0);
            double floor = Math.min(profile.rim() - drop * fall(v0, profile.curve()) + wave * 4.0 * v0 * (1.0 - v0), GUARD_LEVEL);
            ramp = Mth.lerp(guard, ramp, VolcanicMath.smoothMax(ramp, floor, 2.0));
        }
        return ramp;
    }

    /** The share of the drop fallen at v: narrow steps above, wide below. */
    private static double fall(double v, double curve) {
        return v * (1.0 + curve * (1.0 - v));
    }

    /**
     * The treads of a ramp value: level on BANK_TOP + 8k for {@link #TREAD} of each step (Q5), then a riser whose profile
     * follows the detail noise {@code detail}: the smootherstep of {@link Ring4Relief#terrace}, or where the detail is
     * high a rocky knick, its slope gathered in the middle. Never past the step's own levels.
     */
    static double treadHeight(double ramp, double detail) {
        double rise = ramp - BANK_TOP;
        if (rise <= 0.0) {
            return ramp;
        }
        double k = rise / STEP;
        double n = Math.floor(k);
        double t = Mth.clamp((k - n - TREAD) / (1.0 - TREAD), 0.0, 1.0);
        double g = Mth.clamp(0.5 + (t - 0.5) * knick(detail), 0.0, 1.0);
        return BANK_TOP + STEP * (n + g * g * g * (g * (6.0 * g - 15.0) + 10.0));
    }

    /** How much steeper than the smootherstep a riser is in its middle (1: as Q5). */
    static double knick(double detail) {
        return 1.0 + KNICK * Ring4Relief.smooth(detail, KNICK_FROM, KNICK_TO);
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
     * top) up to the river's middle with the river cut into it; beyond it the outer bank (the beach up to
     * {@link #BANK_TOP}, then {@link #OUTER_SLOPE}) up to ring 4's own terrain {@code relief}, wherever that is lower
     * (its rivers and lagoons open into the foot river). The river lies far enough in that the bank meets ring 4's
     * height before the zone's edge; the last blocks blend to ring 4 in case a spot still stands higher.
     */
    static double withRiver(Point p, double inner, double relief) {
        Profile profile = p.profile;
        double river = p.river();
        if (p.u <= river) {
            double across = river - p.u - profile.bedHalf();
            return Math.min(inner, RIVER_BED + Math.max(0.0, across) * RIVER_SLOPE);
        }
        double d = p.u - river - profile.bedHalf();
        double beach = (BANK_TOP - RIVER_BED) / RIVER_SLOPE;
        double bank = d <= 0.0 ? RIVER_BED : d <= beach ? RIVER_BED + d * RIVER_SLOPE : BANK_TOP + (d - beach) * OUTER_SLOPE;
        return Mth.lerp(Ring4Relief.smooth(p.u, p.width - 4.0, p.width), Math.min(bank, relief), relief);
    }

    /** Whether the ground at a point needs ring 4's own terrain (beyond the river's middle). */
    static boolean needsRelief(Point p) {
        return p.kind == Point.DESCENT && p.u > p.river() - 1.0;
    }

    /** 0..1: the foot river's channel (1 on its bed). */
    static double river(Point p) {
        if (p.kind != Point.DESCENT) {
            return 0.0;
        }
        double across = Math.abs(p.u - p.river());
        return 1.0 - Ring4Relief.smooth(across, p.profile.bedHalf(), p.profile.bedHalf() + 4.0);
    }
}
