package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The horseshoe gorges of {@code verdant_falls} (W-terrein 2c part 2, plan §2.3, like Iguazu): one per arc cell of
 * ~{@link #CELL} blocks round the ring. A gorge is cut back into the descent ({@link VerdantDescent}): a rounded
 * amphitheatre (half an ellipse: half width 45–75, as deep as the descent allows under a level rim; its back wall 25–40
 * blocks past the rim) with two arms running out to the foot river, the wall line bent organically (never straight).
 * <ul>
 *   <li><b>Collar:</b> round the amphitheatre the descent is held on one tread ({@link Gorge#top}) for
 *   {@link #FLAT} blocks, so the whole arc stands at one height; along the arms it lets go over {@link #ARM_FADE}
 *   blocks, so the arms get lower where the treads beside them fall. The stair lines bend round the gorge.</li>
 *   <li><b>Rim lake:</b> on the collar tread a curved lake follows the arc, {@link #RIM_LAKE_IN} blocks back from the
 *   edge; a notch through the edge feeds each fall's spring.</li>
 *   <li><b>Falls:</b> along the arc every 10–14 blocks, from the spring at the wall line into the gorge lake; the
 *   falling column stands one block beyond the wall line.</li>
 *   <li><b>Floor:</b> the gorge lake on tread {@link Gorge#floor} (the wall 24 or 32 high); downstream the floor
 *   steps down with the descent's stair lines, a lake per tread behind a threshold with 2–4 falls over each lip, to
 *   the sea level and the foot river. Along the arms a dry shore of 4–6.</li>
 *   <li><b>Walls:</b> exact per block within {@link #WALL_BAND} of the line, rough (strata and knobs, ±3), the top
 *   edge crumbling; smooth within {@link #FALL_CLEAR} of a fall and never hollowed below the gorge's water.</li>
 *   <li><b>Ledge path:</b> 3 deep and 4 high cut into the wall at half its height all round the amphitheatre, behind
 *   the falls; on one arm it ramps down to the shore, on the other up to the rim (1 in 2).</li>
 *   <li><b>Niches:</b> behind one in three arc falls a niche off the ledge path (3 wide, 4 high, 5–8 deep).</li>
 *   <li><b>Climbing ledges</b> (Q3): short overhanging ledges stacked a climbing distance apart on the walls and
 *   beside falls, each with a knob over its end whose strand ({@code ledge_strand}) leads from the ledge below onto
 *   it, the last one into the ledge path.</li>
 * </ul>
 * The basins' streams keep clear of a gorge's reach ({@link #clear}). Pure: hashes of the world salt
 * ({@link CellShapes#cell}), the descent and the relief's noises; gorges, their falls and ledges are decided once per
 * world.
 */
final class VerdantGorges {
    /** One gorge at most per this many blocks along the loop (≈ 54 round the ring at the default radii). */
    static final double CELL = 700.0;
    private static final int GORGE_SALT = 6301;
    private static final int FALL_SALT = 6303;
    private static final int NICHE_SALT = 6304;
    /** The climbing ledges (Q3, not in the plan's list of salts). */
    private static final int LEDGE_SALT = 6307;
    /** The amphitheatre's half width: RADIUS_MIN + 0…RADIUS_SPAN (45–75), for a zone of 256. */
    private static final double RADIUS_MIN = 45.0;
    private static final double RADIUS_SPAN = 30.0;
    /** The back wall lies BACK_MIN–BACK_MIN + BACK_SPAN past the rim, and at least BACK_PAST_TOP past the top terrace. */
    private static final double BACK_MIN = 25.0;
    private static final double BACK_SPAN = 15.0;
    private static final double BACK_PAST_TOP = 22.0;
    /** The rim height counts as the next tread when it lies within this many blocks under it. */
    private static final double RIM_UP = 2.0;
    /** The arc's length table: this many steps from the back to an end. */
    static final int ARC_STEPS = 32;
    /** The half ellipse is at least this deep and this share of its half width (else the gorge steps its rim down a tread). */
    private static final double DEPTH_MIN = 24.0;
    private static final double DEPTH_SHARE = 0.4;
    /** The rim steps down a tread where that makes the half ellipse at least this round (depth / half width). */
    private static final double DEPTH_ROUND = 0.65;
    /** Places tried in a cell before it is left without a gorge. */
    private static final int PLACE_TRIES = 6;
    /** The collar stays this far under the foot river's bank envelope, and the arms have this much room past the arc. */
    private static final double ENVELOPE_MARGIN = 3.0;

    /** The collar: flat for FLAT blocks beyond the wall line, back to the descent over BLEND more. */
    static final double FLAT = 18.0;
    static final double BLEND = 24.0;
    /** Along the arms the collar lets go over this many blocks. */
    static final double ARM_FADE = 48.0;
    /** The shore along the arms turns in over this many blocks past the amphitheatre. */
    private static final double ARM_TURN = 12.0;
    /** The rim lake lies this far back from the edge; it ends this far before the arc's ends. */
    static final double RIM_LAKE_IN = 5.0;
    private static final double RIM_LAKE_END = 4.0;
    /** The rim lake keeps this far (blocks across the zone) past the rim: its neighbours lie on the exact descent. */
    private static final double RIM_LAKE_PAST_RIM = 4.0;
    /** A gorge lake's threshold before its lip (blocks), and the banks' rise per block. */
    static final double THRESHOLD = 3.0;
    private static final double BANK = 2.0;
    /** The wall is exact per block within this many blocks of its line (columns flagged for {@link #block}). */
    static final double WALL_BAND = 6.0;
    /** How far the face moves in and out with the roughness (5a), and how far the top edge crumbles. */
    static final double ROUGHNESS = 3.0;
    /** The roughness noise mostly stays within ±0.5: scaled by this, then held to ±ROUGHNESS. */
    private static final double ROUGH_GAIN = 6.0;
    static final double CRUMBLE = 3.0;
    /** Within this many blocks (along the wall) of a fall the wall is smooth; the top does not crumble within CRUMBLE_CLEAR. */
    static final double FALL_CLEAR = 3.0;
    private static final double CRUMBLE_CLEAR = 4.0;
    /** The ledge path: this deep into the wall, this high (blocks). */
    static final int PATH_DEPTH = 3;
    static final int PATH_HEIGHT = 4;
    /** Falls along the arc every FALL_SPACING + 0…FALL_SPACING_SPAN blocks, none within FALL_END of the arc's ends. */
    private static final double FALL_SPACING = 10.0;
    private static final double FALL_SPACING_SPAN = 4.0;
    private static final double FALL_END = 8.0;
    /** The notch from the rim lake to a spring is at most this long. */
    private static final int MAX_NOTCH = 10;
    private static final double NICHE_CHANCE = 1.0 / 3.0;
    /** A niche: 3 wide (half width), 4 high, 5–8 deep past the ledge path. */
    static final double NICHE_HALF = 1.6;
    /** Climbing ledges: protrude this far, two blocks thick, a gap of 4–5 between them. */
    static final double LEDGE_OUT = 2.0;
    private static final double LEDGE_STAGGER = 1.0;
    /** A ledge's knob: this far over the ledge's underside, this far out from the wall, past this share of its half length. */
    static final int KNOB_RISE = 4;
    static final double KNOB_OUT = 1.6;
    private static final double KNOB_PAST = 0.7;
    /** The path's knob stands this far over the ledge path's floor. */
    static final int PATH_KNOB_RISE = 2;
    /** Climbing routes beside this share of the arc falls, and along the walls every ROUTE_SPACING + 0…12 blocks. */
    private static final double BESIDE_FALLS = 1.0 / 3.0;
    private static final double ROUTE_SPACING = 24.0;
    /** The wall line's own blocks of the per-block zone: from this far inside the wall to this far into the gorge. */
    private static final double BLOCK_IN = PATH_DEPTH + 9.0;
    private static final double BLOCK_OUT = WALL_BAND + 1.0;

    private final IslandField field;
    private final VerdantDescent descent;

    VerdantGorges(IslandField field, VerdantDescent descent) {
        this.field = field;
        this.descent = descent;
    }

    // ---------------------------------------------------------------- gorges

    /**
     * A gorge: its cell, its axis position along the loop {@code s0}, the back wall's point, the amphitheatre's centre
     * and the outward direction (unit), its half width {@code radius} and depth (a half ellipse: the descent is too short
     * for a full half circle under a level rim), the collar's tread {@code top} (level 64 + 8 top), the gorge lake's
     * tread {@code floor}, the collar's ramp value, the ledge path's floor, the arm with the ramp down to the shore
     * (−1/+1 along the side axis), the rim lake's width and depth, the lakes' depths, the shore's width, the wall's
     * three phases, the reach along the loop (blocks of s either side of {@code s0}) and the arc's length table
     * ({@link #ARC_STEPS} + 1 values from the back to an end).
     */
    record Gorge(int index, double s0, double bx, double bz, double cx, double cz, double ox, double oz, double radius,
                 double depth, int top, int floor, double collar, int pathY, int downSide, double rimWidth, double rimDepth,
                 double lakeDepth, double lowerDepth, double shore, double p1, double p2, double p3, double reach,
                 double[] arcs) {
        /** How far the wall line lies outside the half ellipse at arc parameter t (blocks): it never runs straight. */
        double wobble(double t) {
            return 0.07 * radius * Math.sin(1.3 * t / radius + p3) + 2.5 * Math.sin(t / 21.0 + p2 + p3) + 2.0 * Math.sin(t / 13.0 + p1)
                    + 1.0 * Math.sin(t / 6.5 + p2);
        }

        /** The arc parameter at an arc's end (where the arms begin): the half ellipse's length from its back. */
        double arcEnd() {
            return arcs[ARC_STEPS];
        }

        /** The arc length from the back to the ellipse's angle phi (signed). */
        double arcAt(double phi) {
            double f = Math.min(1.0, Math.abs(phi) / (0.5 * Math.PI)) * ARC_STEPS;
            int i = Math.min(ARC_STEPS - 1, (int) f);
            return Math.signum(phi) * Mth.lerp(f - i, arcs[i], arcs[i + 1]);
        }

        /** The ellipse's angle at arc length t (signed, |t| ≤ the arc's end). */
        double phiAt(double t) {
            double target = Math.min(Math.abs(t), arcEnd());
            int i = 0;
            while (i < ARC_STEPS - 1 && arcs[i + 1] < target) {
                i++;
            }
            double f = (target - arcs[i]) / Math.max(1e-9, arcs[i + 1] - arcs[i]);
            return Math.signum(t) * (i + Mth.clamp(f, 0.0, 1.0)) * 0.5 * Math.PI / ARC_STEPS;
        }

        /** The top of the gorge's highest water (exclusive): the gorge lake's. */
        int waterLimit() {
            return VerdantDescent.waterTop(floor);
        }

        /** The rim lake's water top (exclusive) and the spring's Y of the arc falls. */
        int rimWater() {
            return VerdantDescent.waterTop(top);
        }
    }

    /**
     * A fall of a gorge: from the arc (fed by the rim lake through a notch {@code notch} blocks long) or over a lip of
     * the floor (from the lake of tread {@code body}, a notch through its threshold); its place along the wall
     * ({@code t}, arc falls) and whether a niche lies behind it (depth, 0 for none).
     */
    record GorgeFall(VerdantBasins.Fall fall, boolean arc, int body, int notch, double t, double niche) {
        /** Whether (x, z) lies within {@code reach} (Chebyshev) of the spring, the fall's column or a notch column. */
        boolean nearNotch(int x, int z, int reach) {
            IslandField.Fall f = fall.fall();
            for (int k = -1; k <= notch + 1; k++) {
                int cx = f.springX() - k * fall.stepX();
                int cz = f.springZ() - k * fall.stepZ();
                if (Math.abs(x - cx) <= reach && Math.abs(z - cz) <= reach) {
                    return true;
                }
            }
            return false;
        }

        boolean notchAt(int x, int z) {
            IslandField.Fall f = fall.fall();
            for (int k = 1; k <= notch; k++) {
                if (x == f.springX() - k * fall.stepX() && z == f.springZ() - k * fall.stepZ()) {
                    return true;
                }
            }
            return false;
        }
    }

    /** A climbing route: ledges at {@code t} along the wall, from {@code firstY} every {@code spacing}, skipping the path band. */
    record Route(double t, double half, int firstY, int spacing, int count, int skipLo, int skipHi) {
        /** Whether ledge k (its underside at firstY + k · spacing) is left out. */
        boolean skipped(int y) {
            return y + 1 >= skipLo && y <= skipHi;
        }

        /** The topmost ledge under the ledge path's band, or −1. */
        int belowPath() {
            int k = -1;
            for (int i = 0; i < count; i++) {
                if (firstY + i * spacing + 1 < skipLo) {
                    k = i;
                }
            }
            return k;
        }
    }

    /** What a gorge has, decided once: its falls (arc falls sorted by t), its springs by column, its climbing routes. */
    record Details(List<GorgeFall> falls, Map<Long, GorgeFall> springs, List<Route> routes) {
    }

    int gorgeCount() {
        return RingArc.cellCount(VerdantDescent.perimeter(field.ringLayout()), CELL);
    }

    double cellSize() {
        return VerdantDescent.perimeter(field.ringLayout()) / gorgeCount();
    }

    /** Gorge {@code k} (modulo the count round the loop), or null where its cell has none. */
    @Nullable
    Gorge gorge(int k) {
        int kk = Math.floorMod(k, gorgeCount());
        return field.decisions().once(field.decisions().verdantGorges, kk, () -> Optional.ofNullable(placeGorge(kk))).orElse(null);
    }

    @Nullable
    private Gorge placeGorge(int k) {
        RingLayout layout = field.ringLayout();
        long h = CellShapes.cell(field.salt(), k, 0, GORGE_SALT);
        double scale = VerdantDescent.width(layout) / VerdantDescent.REFERENCE_WIDTH;
        double rimAt = VerdantDescent.rimAt(layout);
        double radius = (RADIUS_MIN + RADIUS_SPAN * CellShapes.unit(h, 1)) * scale;
        // A few places in the middle half of the cell, the first where the gorge fits: the boundary noise bends the
        // zone's coordinates by up to ±20 blocks across a gorge, so a level rim does not fit everywhere.
        double s0 = 0.0;
        double[] b = null;
        double ox = 0.0;
        double oz = 0.0;
        int top = 0;
        double depth = 0.0;
        VerdantDescent.Profile profile = null;
        for (int attempt = 0; attempt < PLACE_TRIES && b == null; attempt++) {
            long ha = attempt == 0 ? h : CellShapes.cell(field.salt(), k, attempt, GORGE_SALT);
            s0 = (k + 0.25 + 0.5 * CellShapes.unit(ha, 0)) * cellSize();
            profile = descent.profile(s0);
            double back = Math.max(rimAt + (BACK_MIN + BACK_SPAN * CellShapes.unit(h, 2)) * scale, profile.descentStart(rimAt) + BACK_PAST_TOP);
            double angle = s0 / VerdantDescent.midRadius(layout);
            double[] at = VerdantDescent.onRay(layout, angle, VerdantDescent.zoneStart(layout) + back);
            ox = Math.cos(angle);
            oz = Math.sin(angle);
            if (Double.isNaN(descent.rampAt(at[0], at[1]))) {
                continue;
            }
            // The collar holds the arc on the rim's tread (T rounded to its tread, up within RIM_UP of the next): the
            // gorge is cut into the plateau's edge, its wall H = T − G. The arc must stay under the foot river's bank
            // envelope with the rim level; where the descent is too short for that, the rim steps down a tread.
            top = (int) Math.floor((profile.rim() - VerdantDescent.BANK_TOP + RIM_UP) / VerdantDescent.STEP);
            for (; top >= 4; top--) {
                depth = deepest(top, radius, at, ox, oz);
                // A rounded horseshoe where a tread lower allows it (the collar then lowers the land round it a little).
                boolean round = depth >= DEPTH_ROUND * radius;
                boolean lower = top > 4 && deepest(top - 1, radius, at, ox, oz) >= DEPTH_ROUND * radius;
                if (round || !lower && depth >= Math.max(DEPTH_MIN, DEPTH_SHARE * radius)) {
                    break;
                }
            }
            if (top >= 4 && depth >= Math.max(DEPTH_MIN, DEPTH_SHARE * radius)) {
                b = at;
            }
        }
        // The wall 24 or 32 high (T − G ≥ 20): skipped where the descent is too low for a gorge lake above the sea.
        int drop = CellShapes.unit(h, 3) < 0.5 ? 3 : 4;
        int floor = top - drop;
        if (floor < 1) {
            floor = top - 3;
        }
        if (b == null || floor < 1) {
            return null;
        }
        double collar = VerdantDescent.level(top) + 0.3 * VerdantDescent.STEP;
        int water = VerdantDescent.waterTop(floor);
        // Half way up the wall from the gorge lake's surface to the rim's.
        int pathY = water + (int) Math.round((VerdantDescent.level(top) - water) / 2.0);
        int downSide = CellShapes.unit(h, 4) < 0.5 ? -1 : 1;
        double rimWidth = 5.0 + 2.0 * CellShapes.unit(h, 5);
        double rimDepth = 2.0 + CellShapes.unit(h, 6);
        double lakeDepth = 4.0 + 2.0 * CellShapes.unit(h, 7);
        double lowerDepth = 2.5 + 1.5 * CellShapes.unit(h, 8);
        double shore = 4.0 + 2.0 * CellShapes.unit(h, 9);
        double p1 = 2.0 * Math.PI * CellShapes.unit(h, 10);
        double p2 = 2.0 * Math.PI * CellShapes.unit(h, 11);
        double p3 = 2.0 * Math.PI * CellShapes.unit(h, 12);
        // The reach along the loop: the widest wall line with its collar, at the zone's outer radius.
        double reach = (radius + 0.07 * radius + 3.0 + FLAT + BLEND + 6.0) * 1.06;
        double[] arcs = new double[ARC_STEPS + 1];
        double dphi = 0.5 * Math.PI / ARC_STEPS;
        for (int i = 1; i <= ARC_STEPS; i++) {
            double mid = (i - 0.5) * dphi;
            arcs[i] = arcs[i - 1] + Math.hypot(radius * Math.cos(mid), depth * Math.sin(mid)) * dphi;
        }
        return new Gorge(k, s0, b[0], b[1], b[0] + depth * ox, b[1] + depth * oz, ox, oz, radius, depth, top, floor, collar, pathY,
                downSide, rimWidth, rimDepth, lakeDepth, lowerDepth, shore, p1, p2, p3, reach, arcs);
    }

    /** Where (blocks across the zone) the foot river's inner bank envelope (its bank rising 0.6 per block) reaches y. */
    private static double envelopeU(VerdantDescent.Profile profile, double y) {
        return profile.river() - profile.bedHalf() - (y - VerdantDescent.RIVER_BED) / VerdantDescent.RIVER_SLOPE;
    }

    /**
     * The deepest half ellipse (at most the half width, 0 if none) whose arc, with its collar on tread {@code top}, stays
     * under the foot river's bank envelope with room for the arms, measured at points along the arc (the boundary noise
     * bends the zone's coordinates, so not on the axis alone).
     */
    private double deepest(int top, double radius, double[] back, double ox, double oz) {
        RingLayout layout = field.ringLayout();
        // The rim lake (and the rim) need the envelope a block over the collar's tread out to the rim lake's far edge.
        double y = VerdantDescent.level(top) + 1.0 + ENVELOPE_MARGIN;
        double mid = VerdantDescent.midRadius(layout);
        for (double depth = radius; depth >= DEPTH_MIN; depth -= 2.0) {
            boolean fits = true;
            for (double phi = -0.5 * Math.PI; phi <= 0.5 * Math.PI + 1e-9 && fits; phi += Math.PI / 12.0) {
                // The arc's point (a = R sin φ, w = −D cos φ) about the centre at the back point + D outward, a little outside.
                double a = (radius + RIM_LAKE_IN + 9.0) * Math.sin(phi);
                double w = depth - depth * Math.cos(phi);
                double x = back[0] - a * oz + w * ox;
                double z = back[1] + a * ox + w * oz;
                double u = layout.effectiveDistance(x, z) - VerdantDescent.zoneStart(layout);
                VerdantDescent.Profile profile = descent.profile(RingArc.arc(x, z, mid));
                fits = u <= envelopeU(profile, y);
            }
            if (fits) {
                return depth;
            }
        }
        return 0.0;
    }

    /** The foot river's bank envelope at u (the highest ground the river's bank allows there), less the margin. */
    private static double envelope(VerdantDescent.Point p) {
        return VerdantDescent.RIVER_BED + VerdantDescent.RIVER_SLOPE * Math.max(0.0, p.profile.river() - p.profile.bedHalf() - p.u)
                - ENVELOPE_MARGIN;
    }

    /** The gorge whose reach covers a point of the descent (filling {@code l}), or null. */
    @Nullable
    Gorge at(double x, double z, VerdantDescent.Point p, Local l) {
        if (p.kind != VerdantDescent.Point.DESCENT || p.u > p.profile.river()) {
            return null;
        }
        double period = VerdantDescent.perimeter(field.ringLayout());
        int k = (int) Math.floor(p.s / cellSize());
        for (int dk = -1; dk <= 1; dk++) {
            Gorge g = gorge(k + dk);
            if (g == null || RingArc.loopDistance(p.s, g.s0(), period) > g.reach()) {
                continue;
            }
            local(g, x, z, l);
            if (l.din >= -(FLAT + BLEND + 1.0) && (l.w <= ARM_FADE + 4.0 || l.din > -BLOCK_IN)) {
                return g;
            }
        }
        return null;
    }

    /**
     * Whether a stretch of the loop (positions s) keeps clear of every gorge's reach: the basins' streams whose water
     * would reach into a gorge are left out.
     */
    boolean clear(double low, double high) {
        double period = VerdantDescent.perimeter(field.ringLayout());
        double mid = RingArc.wrap(0.5 * (low + high), period);
        double half = 0.5 * (high - low);
        int k = (int) Math.floor(mid / cellSize());
        for (int dk = -1; dk <= 1; dk++) {
            Gorge g = gorge(k + dk);
            if (g != null && RingArc.loopDistance(mid, g.s0(), period) <= g.reach() + half) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- local coordinates

    /**
     * A column in a gorge's frame: {@code a} across (along the side axis) and {@code w} out from the centre (negative
     * in the amphitheatre), the polar angle {@code phi} (0 at the back) and distance {@code rho} there, the arc
     * parameter {@code t} (blocks along the wall line from the back, negative on the −a side; on the arms past the
     * arc's end) and {@code din}: how far inside the gorge (negative: into the wall and the land beyond).
     */
    static final class Local {
        double a;
        double w;
        double phi;
        double rho;
        double t;
        double din;
        int side;
    }

    static void local(Gorge g, double x, double z, Local l) {
        double dx = x - g.cx();
        double dz = z - g.cz();
        l.a = -dx * g.oz() + dz * g.ox();
        l.w = dx * g.ox() + dz * g.oz();
        l.side = l.a >= 0.0 ? 1 : -1;
        if (l.w < 0.0) {
            // The half ellipse E(φ) = (R sin φ, −D cos φ): the foot point of the column on it (Newton from the scaled polar
            // angle), so t is the wall's own arc length there and din the distance along its normal, plus the wobble.
            double r = g.radius();
            double d = g.depth();
            double phi = Math.atan2(l.a / r, -l.w / d);
            for (int n = 0; n < 4; n++) {
                double sin = Math.sin(phi);
                double cos = Math.cos(phi);
                double ea = l.a - r * sin;
                double ew = l.w + d * cos;
                double f = ea * r * cos + ew * d * sin;
                double df = -(r * r * cos * cos + d * d * sin * sin) - ea * r * sin + ew * d * cos;
                if (df >= -1e-9) {
                    break;
                }
                phi = Mth.clamp(phi - f / df, -0.5 * Math.PI, 0.5 * Math.PI);
            }
            double sin = Math.sin(phi);
            double cos = Math.cos(phi);
            double na = sin / r;
            double nw = -cos / d;
            double len = Math.hypot(na, nw);
            l.phi = phi;
            l.rho = Math.hypot(l.a, l.w);
            l.t = g.arcAt(phi);
            l.din = g.wobble(l.t) - ((l.a - r * sin) * na + (l.w + d * cos) * nw) / len;
        } else {
            l.rho = Math.abs(l.a);
            l.phi = l.side * 0.5 * Math.PI;
            l.t = l.side * (g.arcEnd() + l.w);
            l.din = g.radius() + g.wobble(l.t) - l.rho;
        }
    }

    /**
     * A point {@code into} blocks inside the gorge from the wall line at arc parameter t (arc or arm), and the unit
     * direction into the gorge there: {x, z, nx, nz}.
     */
    static double[] wallAt(Gorge g, double t, double into) {
        double a;
        double w;
        double na;
        double nw;
        if (Math.abs(t) <= g.arcEnd()) {
            double phi = g.phiAt(t);
            double sin = Math.sin(phi);
            double cos = Math.cos(phi);
            // The outward normal of the ellipse (a = R sin φ, w = −D cos φ).
            double ga = sin / g.radius();
            double gw = -cos / g.depth();
            double len = Math.hypot(ga, gw);
            na = -ga / len;
            nw = -gw / len;
            double off = g.wobble(t) - into;
            a = g.radius() * sin - na * off;
            w = -g.depth() * cos - nw * off;
        } else {
            int side = t >= 0.0 ? 1 : -1;
            w = Math.abs(t) - g.arcEnd();
            a = side * (g.radius() + g.wobble(t) - into);
            na = -side;
            nw = 0.0;
        }
        return new double[]{g.cx() - a * g.oz() + w * g.ox(), g.cz() + a * g.ox() + w * g.oz(), -na * g.oz() + nw * g.ox(),
                na * g.ox() + nw * g.oz()};
    }

    // ---------------------------------------------------------------- per column

    /** A column's values for the field ({@link #shape}). */
    static final class Shape {
        /** The gorge whose reach covers the column (set by the field), else null. */
        @Nullable
        Gorge zone;
        /** The gorge's ground outside the wall line (the collar with the rim lake) and inside (the gorge floor). */
        double outside;
        double inside;
        /** The gorge whose wall shapes reach the column per block, else null. */
        @Nullable
        Gorge blocks;
        Local local = new Local();
        /** Blocks along the wall to the nearest arc fall (0 beside a notch or a climbing route: the wall stays smooth). */
        double nearFall;
        /** A spring's side beside the wall line (rock at springY ± 1), else MIN_VALUE. */
        int earY;
        /** The blocks the wall may open under the column's own ground (path, ramp, niche, rough hollows), else lo > hi. */
        int openLo;
        int openHi;
    }

    /**
     * The ground of a column in gorge {@code g} into {@code out}: outside the wall line the collar (with the rim lake
     * and the notches), inside the gorge floor (lakes, thresholds, shores), with the water's top, the travertine rim
     * and the exact wall for the surface; {@code shape} gets the column's values for the per-block wall. With
     * {@code fallCuts} the falls are decided (notches cut, the top crumbles away from them).
     */
    void shape(Gorge g, int x, int z, VerdantDescent.Point p, Local l, boolean fallCuts, VerdantBasins.Water out, Shape shape) {
        out.rim = 0.0;
        out.cliff = 0.0;
        out.lakeTop = Integer.MIN_VALUE;
        double east = descent.rampAt(x + 1, z);
        double west = descent.rampAt(x - 1, z);
        double south = descent.rampAt(x, z + 1);
        double north = descent.rampAt(x, z - 1);
        double grad = Double.isNaN(east) || Double.isNaN(west) || Double.isNaN(south) || Double.isNaN(north) ? 0.3
                : Math.max(0.02, 0.5 * Math.hypot(east - west, south - north));
        Details details = fallCuts ? details(g) : null;
        double nearFall = details == null ? Double.POSITIVE_INFINITY : nearestArcFall(details, l.t);
        // Outside: the collar, its rim lake and the notches to the springs.
        double outside = collarGround(g, p, l, x, z);
        int rimWater = g.rimWater();
        boolean notched = false;
        if (details != null) {
            for (GorgeFall f : details.falls()) {
                if (!f.arc() || Math.abs(f.t() - l.t) > MAX_NOTCH + 4.0) {
                    continue;
                }
                if (f.notchAt(x, z)) {
                    outside = Math.min(outside, rimWater - 2.0);
                    notched = true;
                }
                if (f.nearNotch(x, z, 2)) {
                    // Beside a spring or its notch the wall stays smooth and whole (no roughness, no crumbling).
                    nearFall = 0.0;
                }
            }
        }
        // Inside: the floor of the gorge.
        int b0 = p.body;
        int b = Math.max(0, Math.min(b0, g.floor()));
        double level = VerdantDescent.level(b);
        double dry = b0 >= g.floor() ? VerdantDescent.level(g.floor()) : p.treads;
        double arm = Ring4Relief.smooth(l.w, 0.0, ARM_TURN);
        double across = l.din - g.shore() * arm + 4.0 * (1.0 - arm);
        double toEdge = b >= 1 ? (p.ramp - level) / grad : Double.POSITIVE_INFINITY;
        double inset = Math.min(across, toEdge - THRESHOLD);
        double depth = b == g.floor() ? g.lakeDepth() : g.lowerDepth();
        double inside = inset <= 0.0 ? Math.min(dry, level - BANK * inset) : level - (1.0 + depth) * Ring4Relief.smooth(inset, 0.0, 3.0);
        int water = VerdantDescent.waterTop(b);
        if (inside < water + 1.0) {
            if (b >= 1 && (east < level || west < level || south < level || north < level)) {
                // Next to a lower tread (the lip): no water here, so only a spring meets the lip.
                inside = dry;
            } else if (b == 0 && p.u < p.width / 2.0 + 2.0) {
                inside = Math.max(inside, VerdantDescent.BANK_TOP - 1.0);
            }
        }
        if (details != null) {
            for (GorgeFall f : details.falls()) {
                if (f.arc()) {
                    continue;
                }
                IslandField.Fall ff = f.fall().fall();
                if (Math.abs(ff.springX() - x) > MAX_NOTCH + 2 || Math.abs(ff.springZ() - z) > MAX_NOTCH + 2) {
                    continue;
                }
                if (f.notchAt(x, z)) {
                    inside = Math.min(inside, VerdantDescent.waterTop(f.body()) - 2.0);
                }
                if (f.fall().beside(x, z)) {
                    inside = Math.max(inside, VerdantDescent.level(f.body()));
                }
            }
        }
        // The wall stays smooth and whole (no roughness, no crumbling) near the falls, their notches and the climbing
        // routes (the strands must hang free from ledge to ledge).
        double calm = nearFall;
        if (details != null) {
            for (Route r : details.routes()) {
                if (Math.abs(l.t - r.t()) <= r.half() + LEDGE_STAGGER + 2.5) {
                    calm = 0.0;
                }
            }
        }
        // Crumbling top edge (5a), where the wall stands, away from the falls.
        double wallHeight = outside - inside;
        if (details != null && l.din > -4.0 && calm > CRUMBLE_CLEAR && !notched) {
            double edge = (1.0 - Ring4Relief.smooth(-l.din, 1.0, 4.0)) * Ring4Relief.smooth(wallHeight, 2.0, 8.0);
            outside -= Math.round(CRUMBLE * edge * Math.max(0.0, field.relief().crumble(x, z)));
        }
        shape.outside = outside;
        shape.inside = inside;
        shape.nearFall = calm;
        shape.earY = Integer.MIN_VALUE;
        shape.blocks = l.din >= -BLOCK_IN && l.din <= BLOCK_OUT ? g : null;
        shape.openLo = Integer.MAX_VALUE;
        shape.openHi = Integer.MIN_VALUE;
        if (shape.blocks != null && details != null && l.din <= 0.0) {
            openRange(g, l, inside, outside, calm, shape);
        }
        Local copy = shape.local;
        copy.a = l.a;
        copy.w = l.w;
        copy.phi = l.phi;
        copy.rho = l.rho;
        copy.t = l.t;
        copy.din = l.din;
        copy.side = l.side;
        if (l.din > 0.0) {
            out.height = inside;
            out.body = b;
            if (b >= 1 && inside < water) {
                out.lakeTop = water;
            }
            // Travertine on a lip's threshold and beside its falls.
            if (b >= 1 && toEdge <= 2.5 && inset <= 0.0) {
                out.rim = 1.0;
            }
        } else {
            out.height = outside;
            out.body = g.top();
            if (outside < rimWater && (notched || rimLake(g, p, l) > 0.0)) {
                out.lakeTop = rimWater;
            }
            if (nearFall <= 2.5 && -l.din <= 2.0) {
                out.rim = 1.0;
            }
        }
        if (Math.abs(l.din) <= 2.0 && wallHeight >= 3.0 && out.rim == 0.0) {
            out.cliff = 1.0;
        }
        if (details != null) {
            for (GorgeFall f : details.falls()) {
                if (f.arc() && Math.abs(f.t() - l.t) <= 4.0 && f.fall().beside(x, z) && l.din > -0.5) {
                    shape.earY = f.fall().fall().springY();
                }
            }
        }
    }

    /** The collar's ground at a column (no rim lake, no notches). */
    private double collarGround(Gorge g, VerdantDescent.Point p, Local l, int x, int z) {
        double dOut = -l.din;
        double start = p.profile.descentStart(p.rimAt);
        double level = l.w <= 0.0 ? g.collar() : Mth.lerp(Ring4Relief.smooth(l.w, 0.0, ARM_FADE), g.collar(), p.ramp);
        // Where the collar lowers the ground (behind the back wall) it lets go towards the top terrace, never lowering it.
        double share = (1.0 - Ring4Relief.smooth(dOut, FLAT, FLAT + BLEND))
                * (p.ramp <= level ? 1.0 : Ring4Relief.smooth(p.u, start + 2.0, start + 8.0));
        // Never above the foot river's bank envelope (the arms come down to the foot as the descent does).
        double envelope = envelope(p);
        level = Math.min(level, Math.max(envelope, p.ramp));
        double ground = VerdantDescent.treadHeight(Mth.lerp(share, p.ramp, level), p.detail);
        // The rim lake: a curved lake round the arc on the collar's tread (only where the collar holds it level).
        // On the collar's tread (or higher: its banks then rise); never where the ground lies under it.
        double m = ground >= VerdantDescent.level(g.top()) - 0.01 ? rimLake(g, p, l) : -1.0;
        if (m > 0.0) {
            ground = Math.min(ground, VerdantDescent.level(g.top()) - (1.0 + g.rimDepth()) * Ring4Relief.smooth(m, 0.0, 1.5));
        }
        return ground;
    }

    /**
     * How far inside the rim lake's outline a column lies (blocks; ≤ 0 outside it). Only where the collar holds its
     * tread level all round (under the river's envelope, clear of the top terrace and the steam_fens strip, whose
     * ground the 4-block grid shows).
     */
    private static double rimLake(Gorge g, VerdantDescent.Point p, Local l) {
        double dOut = -l.din;
        if (l.w >= 0.0 || dOut <= RIM_LAKE_IN - 1.0 || envelope(p) < VerdantDescent.level(g.top()) + 1.0
                || p.u < p.rimAt + RIM_LAKE_PAST_RIM) {
            return -1.0;
        }
        double width = g.rimWidth() * (0.75 + 0.35 * Math.cos(l.phi));
        return Math.min(Math.min(dOut - RIM_LAKE_IN, RIM_LAKE_IN + width - dOut), g.arcEnd() - RIM_LAKE_END - Math.abs(l.t));
    }

    private static double nearestArcFall(Details details, double t) {
        double best = Double.POSITIVE_INFINITY;
        for (GorgeFall f : details.falls()) {
            if (f.arc()) {
                best = Math.min(best, Math.abs(f.t() - t));
            }
        }
        return best;
    }

    /** The column's ground in gorge g without the falls (for deciding them). */
    private double groundAt(Gorge g, int x, int z, VerdantBasins.Water w, Shape shape) {
        VerdantDescent.Point p = descent.locate(x, z, new VerdantDescent.Point());
        if (p.kind != VerdantDescent.Point.DESCENT) {
            return Double.NaN;
        }
        Local l = new Local();
        local(g, x, z, l);
        shape(g, x, z, p, l, false, w, shape);
        return w.height;
    }

    // ---------------------------------------------------------------- per block

    /**
     * The per-block wall of a column {@link Shape#blocks} names: +1 rock, −1 open (under the gorge floor rock, from the
     * collar up open; the field asks it only within the column's exact band). The ledge path, its ramps and the niches are cut into the wall; climbing ledges and
     * a spring's sides stand out of it; between the gorge floor and the collar the face moves with the roughness.
     *
     * @param inside  the gorge floor's ground at the column
     * @param outside the collar's ground at the column
     */
    double block(Gorge g, Local l, double inside, double outside, double nearFall, int earY, int x, int y, int z) {
        double din = l.din;
        if (din <= 0.0 && y >= Mth.ceil(inside) && carved(g, l, inside, y)) {
            return -1.0;
        }
        if (earY != Integer.MIN_VALUE && y >= earY - 1 && y <= earY + 1) {
            return 1.0;
        }
        if (din > 0.0 && din <= LEDGE_OUT + 0.5 && y >= inside && ledge(g, l, y)) {
            return 1.0;
        }
        if (y < inside) {
            return 1.0;
        }
        if (y >= outside) {
            return -1.0;
        }
        double face = din;
        if (Math.abs(din) < ROUGHNESS + 0.5 && nearFall > FALL_CLEAR && outside - inside >= 3.0) {
            double rough = Mth.clamp(ROUGH_GAIN * field.relief().rough(x, y, z), -ROUGHNESS, ROUGHNESS);
            if (rough > 0.0 && (y < g.waterLimit() || nearPath(g, l, y))) {
                // Never hollowed beside the gorge's water, nor under or into the ledge path.
                rough = 0.0;
            }
            face += rough;
        }
        return face <= 0.0 ? 1.0 : -1.0;
    }

    /** The floor of the ledge path or its ramp at the column, or MIN_VALUE where none passes (only within its depth). */
    private static int pathFloor(Gorge g, Local l) {
        if (l.din <= -PATH_DEPTH) {
            return Integer.MIN_VALUE;
        }
        if (l.w < 0.0) {
            return g.pathY();
        }
        int run = (int) Math.floor(l.w / 2.0);
        if (run > 2 * (VerdantDescent.level(g.top()) - VerdantDescent.level(g.floor())) + 8) {
            return Integer.MIN_VALUE;
        }
        return l.side == g.downSide() ? g.pathY() - run : g.pathY() + run;
    }

    private static boolean nearPath(Gorge g, Local l, int y) {
        int floor = pathFloor(g, l);
        return floor != Integer.MIN_VALUE && y >= floor - 2 && y <= floor + PATH_HEIGHT + 1;
    }

    /** Whether the ledge path, a ramp or a niche opens the block. */
    private boolean carved(Gorge g, Local l, double inside, int y) {
        int floor = pathFloor(g, l);
        if (floor != Integer.MIN_VALUE && y >= floor && y < floor + PATH_HEIGHT) {
            // The ramp down stops at the shore (never below the floor of the gorge).
            return y >= Mth.ceil(inside);
        }
        if (l.w >= 0.0 || y < g.pathY() || y >= g.pathY() + PATH_HEIGHT) {
            return false;
        }
        double lateral = niche(g, l);
        // A rounded end and an arched roof: lower at the sides.
        return !Double.isNaN(lateral) && (Math.abs(lateral) < 1.0 || y < g.pathY() + PATH_HEIGHT - 1);
    }

    /** The column's offset along the wall from the fall of the niche it lies in, or NaN outside every niche. */
    private double niche(Gorge g, Local l) {
        double past = -l.din - PATH_DEPTH;
        if (l.w >= 0.0 || past < -0.5) {
            return Double.NaN;
        }
        for (GorgeFall f : details(g).falls()) {
            if (f.niche() <= 0.0 || Math.abs(f.t() - l.t) > 4.0) {
                continue;
            }
            double lateral = l.t - f.t();
            double across = lateral / NICHE_HALF;
            if (Math.abs(across) < 1.0 && past <= f.niche() * Math.sqrt(1.0 - across * across)) {
                return lateral;
            }
        }
        return Double.NaN;
    }

    /**
     * The blocks under the ground of a wall column that the per-block wall may open (the aquifer leaves them air): the
     * ledge path or its ramp, a niche, and the hollows of the roughness above the gorge's water.
     */
    private void openRange(Gorge g, Local l, double inside, double outside, double nearFall, Shape shape) {
        int lo = Integer.MAX_VALUE;
        int hi = Integer.MIN_VALUE;
        int floor = pathFloor(g, l);
        if (floor != Integer.MIN_VALUE) {
            lo = floor;
            hi = floor + PATH_HEIGHT - 1;
        }
        if (!Double.isNaN(niche(g, l))) {
            lo = Math.min(lo, g.pathY());
            hi = Math.max(hi, g.pathY() + PATH_HEIGHT - 1);
        }
        if (Math.abs(l.din) < ROUGHNESS + 0.5 && nearFall > FALL_CLEAR && outside - inside >= 3.0) {
            lo = Math.min(lo, g.waterLimit());
            hi = Math.max(hi, Mth.floor(outside) - 1);
        }
        shape.openLo = Math.max(lo, Mth.ceil(inside));
        shape.openHi = Math.min(hi, Mth.floor(outside) - 2);
    }

    /**
     * Whether a climbing ledge or the knob over its end stands at the block (a gorge column within LEDGE_OUT of the
     * wall). Ledge k: two blocks thick from its underside, its middle shifted ±1 along the wall by turns. Its knob: one
     * block, {@link #KNOB_RISE} over the ledge's underside, just past the ledge's end on the side where the ledge below
     * reaches further: the strand the feature hangs under the knob runs down to the ledge below, and from its top a
     * player steps sideways onto the ledge (a strand under the ledge itself ends under its rock).
     */
    private boolean ledge(Gorge g, Local l, int y) {
        for (Route r : details(g).routes()) {
            double lateral = l.t - r.t();
            if (Math.abs(lateral) > r.half() + LEDGE_STAGGER + 2.5) {
                continue;
            }
            int k = Math.floorDiv(y - r.firstY(), r.spacing());
            if (k >= 0 && k < r.count()) {
                int under = r.firstY() + k * r.spacing();
                if (y <= under + 1 && !r.skipped(under)) {
                    double across = (lateral - stagger(k)) / r.half();
                    double out = l.din / LEDGE_OUT;
                    if (across * across + out * out <= 1.0) {
                        return true;
                    }
                }
            }
            int kk = y - KNOB_RISE - r.firstY();
            if (kk >= 0 && kk % r.spacing() == 0 && kk / r.spacing() < r.count() && !r.skipped(y - KNOB_RISE)
                    && Math.abs(lateral - knob(r, kk / r.spacing())) <= 0.75 && l.din <= KNOB_OUT) {
                return true;
            }
            // Into the ledge path: a knob in front of its opening over the topmost ledge under it; from the strand's
            // top a player steps into the path.
            int below = r.belowPath();
            if (y == g.pathY() + PATH_KNOB_RISE && below >= 0 && Math.abs(r.t()) < g.arcEnd() - 3.0 && l.din <= 1.0
                    && Math.abs(lateral - stagger(below)) <= 0.75) {
                return true;
            }
        }
        return false;
    }

    /** Ledge k's middle along the wall from its route's place: shifted ±{@link #LEDGE_STAGGER} by turns. */
    private static double stagger(int k) {
        return (k & 1) == 0 ? -LEDGE_STAGGER : LEDGE_STAGGER;
    }

    /** The knob of ledge k along the wall from its route's place: just past the ledge's end, over the ledge below's end. */
    static double knob(Route r, int k) {
        double side = -Math.signum(stagger(k));
        return stagger(k) + side * (KNOB_PAST * r.half() + 1.0);
    }

    // ---------------------------------------------------------------- falls and ledges

    /** A gorge's falls, springs and climbing routes, decided once per world. */
    Details details(Gorge g) {
        return field.decisions().once(field.decisions().verdantGorgeDetails, g.index(), () -> placeDetails(g));
    }

    private Details placeDetails(Gorge g) {
        List<GorgeFall> falls = new ArrayList<>();
        // Along the arc: every 10–14 blocks, from one end to the other.
        long h = CellShapes.cell(field.salt(), g.index(), 0, FALL_SALT);
        double end = g.arcEnd() - FALL_END;
        int n = 0;
        for (double t = -end + 2.0 + 2.0 * CellShapes.unit(h, 0); t <= end; n++) {
            long hf = CellShapes.cell(field.salt(), g.index(), n + 1, FALL_SALT);
            GorgeFall fall = arcFall(g, t, hf);
            if (fall != null) {
                falls.add(fall);
            }
            t += FALL_SPACING + FALL_SPACING_SPAN * CellShapes.unit(hf, 1);
        }
        // Over the lips of the floor: 2–4 per tread down from the gorge lake.
        for (int b = g.floor(); b >= 1; b--) {
            long hl = CellShapes.cell(field.salt(), g.index(), 100 + b, FALL_SALT);
            int count = 2 + (int) Math.floor(3.0 * CellShapes.unit(hl, 0) * 0.999);
            double span = g.radius() - g.shore() - 8.0;
            for (int j = 0; j < count; j++) {
                double a = -span + (j + 0.5) * (2.0 * span / count) + (2.0 * CellShapes.unit(hl, j + 1) - 1.0) * 3.0;
                GorgeFall fall = lipFall(g, b, a);
                if (fall != null) {
                    falls.add(fall);
                }
            }
        }
        Map<Long, GorgeFall> springs = new HashMap<>();
        for (GorgeFall f : falls) {
            springs.put(KeyedSlots.key(f.fall().fall().springX(), f.fall().fall().springZ()), f);
        }
        List<Route> routes = routes(g, falls);
        return new Details(List.copyOf(falls), Map.copyOf(springs), List.copyOf(routes));
    }

    /** The fall of the arc at t, fed from the rim lake, or null where a rule fails. */
    @Nullable
    private GorgeFall arcFall(Gorge g, double t, long hash) {
        double[] q = wallAt(g, t, 0.0);
        // Into the gorge: the wall's inward normal.
        double nx = q[2];
        double nz = q[3];
        int[] step = Math.abs(nx) >= Math.abs(nz) ? new int[]{nx > 0.0 ? 1 : -1, 0} : new int[]{0, nz > 0.0 ? 1 : -1};
        int[] perp = {-step[1], step[0]};
        int baseX = Mth.floor(q[0]);
        int baseZ = Mth.floor(q[1]);
        Local l = new Local();
        for (int i : new int[]{0, -1, 1, -2, 2}) {
            int sx = baseX + i * step[0];
            int sz = baseZ + i * step[1];
            local(g, sx, sz, l);
            double springIn = l.din;
            local(g, sx + step[0], sz + step[1], l);
            if (springIn <= 0.0 && springIn > -1.6 && l.din > 0.0) {
                GorgeFall fall = checkArc(g, t, sx, sz, step, perp, hash);
                if (fall != null) {
                    return fall;
                }
            }
        }
        return null;
    }

    @Nullable
    private GorgeFall checkArc(Gorge g, double t, int sx, int sz, int[] step, int[] perp, long hash) {
        int springY = g.rimWater() - 1;
        VerdantBasins.Water w = new VerdantBasins.Water();
        Shape s = new Shape();
        Local l = new Local();
        // Rock over the spring, the full collar.
        if (!(groundAt(g, sx, sz, w, s) >= springY + 2.0)) {
            return null;
        }
        // Its sides: rock at its level (the wall, or a knob of rock beside the wall line).
        for (int side = -1; side <= 1; side += 2) {
            local(g, sx + side * perp[0], sz + side * perp[1], l);
            if (l.din <= 0.0 && !(groundAt(g, sx + side * perp[0], sz + side * perp[1], w, s) >= springY + 1.0)) {
                return null;
            }
        }
        // The notch back to the rim lake: each column on the collar, its sides solid wall.
        int notch = 0;
        for (int k = 1; k <= MAX_NOTCH; k++) {
            int x = sx - k * step[0];
            int z = sz - k * step[1];
            double ground = groundAt(g, x, z, w, s);
            local(g, x, z, l);
            if (l.din > -0.5 || Double.isNaN(ground)) {
                return null;
            }
            if (ground <= springY - 1.0 && w.lakeTop == g.rimWater()) {
                notch = k - 1;
                break;
            }
            for (int side = -1; side <= 1; side += 2) {
                int ax = x + side * perp[0];
                int az = z + side * perp[1];
                local(g, ax, az, l);
                // Rock up to the rim's tread, or the rim lake itself (water beside the notch's water).
                double beside = groundAt(g, ax, az, w, s);
                if (l.din > 0.0 || !(beside >= springY + 1.0 || w.lakeTop == g.rimWater())) {
                    return null;
                }
            }
        }
        if (notch == 0 && !(groundAt(g, sx - step[0], sz - step[1], w, s) <= springY - 1.0)) {
            // No rim lake within reach.
            return null;
        }
        // The fall lands in the gorge's water below.
        int fx = sx + step[0];
        int fz = sz + step[1];
        double ground = groundAt(g, fx, fz, w, s);
        if (Double.isNaN(ground) || w.lakeTop == Integer.MIN_VALUE || ground > w.lakeTop - 2.0) {
            return null;
        }
        int bottom = w.lakeTop;
        if (springY - bottom < 5) {
            return null;
        }
        double niche = CellShapes.unit(hash, 2) < NICHE_CHANCE ? 5.0 + 3.0 * CellShapes.unit(hash, 3) : 0.0;
        if (niche > 0.0) {
            // Its far end on the exact descent (not in the steam_fens strip the 4-block grid shows).
            double[] end = wallAt(g, t, -(PATH_DEPTH + niche + 1.0));
            VerdantDescent.Point ep = descent.locate(end[0], end[1], new VerdantDescent.Point());
            if (ep.kind != VerdantDescent.Point.DESCENT || ep.u < ep.rimAt + 2.0) {
                niche = 0.0;
            }
        }
        IslandField.Fall fall = new IslandField.Fall(sx, sz, springY, fx, fz, bottom);
        return new GorgeFall(new VerdantBasins.Fall(fall, step[0], step[1]), true, g.top(), notch, t, niche);
    }

    /** The fall over the lip of tread b's lake at a place across the gorge, or null where a rule fails. */
    @Nullable
    private GorgeFall lipFall(Gorge g, int b, double a) {
        double level = VerdantDescent.level(b);
        // Along the line at a, out from the amphitheatre: where the ramp falls through the level.
        double sx0 = g.cx() - a * g.oz();
        double sz0 = g.cz() + a * g.ox();
        double lo = -Math.sqrt(Math.max(0.0, g.radius() * g.radius() - a * a)) + 3.0;
        double hi = lo + 260.0;
        double rampLo = descent.rampAt(sx0 + lo * g.ox(), sz0 + lo * g.oz());
        double rampHi = descent.rampAt(sx0 + hi * g.ox(), sz0 + hi * g.oz());
        if (Double.isNaN(rampHi)) {
            hi = lo + 120.0;
            rampHi = descent.rampAt(sx0 + hi * g.ox(), sz0 + hi * g.oz());
        }
        if (Double.isNaN(rampLo) || Double.isNaN(rampHi) || rampLo < level || rampHi >= level) {
            return null;
        }
        for (int n = 0; n < 30; n++) {
            double mid = 0.5 * (lo + hi);
            double r = descent.rampAt(sx0 + mid * g.ox(), sz0 + mid * g.oz());
            if (!Double.isNaN(r) && r >= level) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double ex = sx0 + 0.5 * (lo + hi) * g.ox();
        double ez = sz0 + 0.5 * (lo + hi) * g.oz();
        double gx = descent.rampAt(ex + 1.0, ez) - descent.rampAt(ex - 1.0, ez);
        double gz = descent.rampAt(ex, ez + 1.0) - descent.rampAt(ex, ez - 1.0);
        if (Double.isNaN(gx) || Double.isNaN(gz)) {
            return null;
        }
        int[] step = Math.abs(gx) >= Math.abs(gz) ? new int[]{gx < 0.0 ? 1 : -1, 0} : new int[]{0, gz < 0.0 ? 1 : -1};
        int[] perp = {-step[1], step[0]};
        int baseX = Mth.floor(ex);
        int baseZ = Mth.floor(ez);
        for (int i : new int[]{0, -1, 1, -2, 2}) {
            int sx = baseX + i * step[0];
            int sz = baseZ + i * step[1];
            if (inside(sx, sz, level) && !inside(sx + step[0], sz + step[1], level)) {
                return checkLip(g, b, sx, sz, step, perp);
            }
        }
        return null;
    }

    private boolean inside(int x, int z, double level) {
        double r = descent.rampAt(x, z);
        return !Double.isNaN(r) && r >= level;
    }

    @Nullable
    private GorgeFall checkLip(Gorge g, int b, int sx, int sz, int[] step, int[] perp) {
        double level = VerdantDescent.level(b);
        int water = VerdantDescent.waterTop(b);
        int springY = water - 1;
        VerdantBasins.Water w = new VerdantBasins.Water();
        Shape s = new Shape();
        Local l = new Local();
        // The spring, its sides and the notch lie inside the gorge, clear of the shore and the wall.
        local(g, sx, sz, l);
        if (l.din < g.shore() + 4.0) {
            return null;
        }
        if (!(groundAt(g, sx, sz, w, s) > springY + 1.0)) {
            return null;
        }
        for (int side = -1; side <= 1; side += 2) {
            VerdantDescent.Point lp = descent.locate(sx + side * perp[0], sz + side * perp[1], new VerdantDescent.Point());
            if (lp.kind != VerdantDescent.Point.DESCENT || lp.treads <= springY + 1.0) {
                return null;
            }
        }
        // The notch through the threshold to the lake: its columns and all their neighbours on the tread.
        int notch = 0;
        for (int k = 1; k <= 6; k++) {
            int x = sx - k * step[0];
            int z = sz - k * step[1];
            for (int[] c : new int[][]{{x, z}, {x + perp[0], z + perp[1]}, {x - perp[0], z - perp[1]}, {x - step[0], z - step[1]}}) {
                if (!inside(c[0], c[1], level)) {
                    return null;
                }
            }
            double ground = groundAt(g, x, z, w, s);
            if (ground <= springY - 1.0 && w.lakeTop == water) {
                notch = k;
                break;
            }
        }
        if (notch == 0) {
            return null;
        }
        // The fall lands in the next tread's lake (or the sea).
        int fx = sx + step[0];
        int fz = sz + step[1];
        VerdantDescent.Point fp = descent.locate(fx, fz, new VerdantDescent.Point());
        if (fp.kind != VerdantDescent.Point.DESCENT || fp.body != b - 1) {
            return null;
        }
        double ground = groundAt(g, fx, fz, w, s);
        local(g, fx, fz, l);
        if (l.din <= 1.0) {
            return null;
        }
        int bottom = VerdantDescent.waterTop(b - 1);
        if (b - 1 == 0) {
            bottom = (int) VerdantDescent.BANK_TOP - 1;
            if (fp.u < fp.width / 2.0 + 2.0 || ground > bottom - 1) {
                return null;
            }
        } else if (ground > bottom - 1 || w.lakeTop != bottom) {
            return null;
        }
        IslandField.Fall fall = new IslandField.Fall(sx, sz, springY, fx, fz, bottom);
        return new GorgeFall(new VerdantBasins.Fall(fall, step[0], step[1]), false, b, notch - 1, Double.NaN, 0.0);
    }

    /**
     * The climbing routes (Q3): beside a third of the arc falls and along the walls every 24–36 blocks where the wall stands
     * at least 12 high; each a stack of short ledges from just above the water or shore to near the rim.
     */
    private List<Route> routes(Gorge g, List<GorgeFall> falls) {
        List<Double> places = new ArrayList<>();
        long h = CellShapes.cell(field.salt(), g.index(), 0, LEDGE_SALT);
        int n = 0;
        for (GorgeFall f : falls) {
            if (f.arc() && CellShapes.unit(CellShapes.cell(field.salt(), g.index(), ++n, LEDGE_SALT), 0) < BESIDE_FALLS) {
                places.add(f.t() + (CellShapes.unit(h, n % 8) < 0.5 ? -6.0 : 6.0));
            }
        }
        double far = g.arcEnd() + ARM_FADE;
        for (double t = -far + 4.0 * CellShapes.unit(h, 9); t <= far; ) {
            long hr = CellShapes.cell(field.salt(), g.index(), 1000 + places.size() + (int) Math.round(t), LEDGE_SALT);
            places.add(t);
            t += ROUTE_SPACING + 12.0 * CellShapes.unit(hr, 0);
        }
        List<Route> routes = new ArrayList<>();
        Local l = new Local();
        VerdantBasins.Water w = new VerdantBasins.Water();
        Shape s = new Shape();
        for (double t : places) {
            if (nearestArc(falls, t) < 5.5) {
                continue;
            }
            boolean taken = false;
            for (Route r : routes) {
                taken |= Math.abs(r.t() - t) < 8.0;
            }
            if (taken) {
                continue;
            }
            // The wall's foot and top just inside the gorge at t.
            double[] q = wallAt(g, t, 1.0);
            int x = Mth.floor(q[0]);
            int z = Mth.floor(q[1]);
            double ground = groundAt(g, x, z, w, s);
            if (Double.isNaN(ground)) {
                continue;
            }
            local(g, x, z, l);
            if (l.din <= 0.0) {
                continue;
            }
            double foot = w.lakeTop != Integer.MIN_VALUE ? w.lakeTop : Mth.ceil(s.inside);
            double top = s.outside;
            if (top - foot < 12.0) {
                continue;
            }
            long hr = CellShapes.cell(field.salt(), g.index(), 2000 + routes.size(), LEDGE_SALT);
            int spacing = 6 + (CellShapes.unit(hr, 0) < 0.5 ? 0 : 1);
            int firstY = (int) foot + 4;
            int count = (int) Math.floor((top - 4.0 - firstY) / spacing) + 1;
            if (count < 2) {
                continue;
            }
            routes.add(new Route(t, 2.5 + CellShapes.unit(hr, 1), firstY, spacing, count, g.pathY() - 3, g.pathY() + PATH_HEIGHT + 2));
        }
        return routes;
    }

    private static double nearestArc(List<GorgeFall> falls, double t) {
        double best = Double.POSITIVE_INFINITY;
        for (GorgeFall f : falls) {
            if (f.arc()) {
                best = Math.min(best, Math.abs(f.t() - t));
            }
        }
        return best;
    }


    /** The falls of the gorges near a position along the loop (within {@code reach} of a gorge's reach). */
    List<VerdantBasins.Fall> fallsNear(double s, double reach) {
        List<VerdantBasins.Fall> falls = new ArrayList<>();
        double period = VerdantDescent.perimeter(field.ringLayout());
        int k = (int) Math.floor(RingArc.wrap(s, period) / cellSize());
        for (int dk = -1; dk <= 1; dk++) {
            Gorge g = gorge(k + dk);
            if (g == null || RingArc.loopDistance(s, g.s0(), period) > g.reach() + reach) {
                continue;
            }
            for (GorgeFall f : details(g).falls()) {
                falls.add(f.fall());
            }
        }
        return falls;
    }

    /** The fall whose spring is the column, in gorge g, or null. */
    @Nullable
    GorgeFall springAt(Gorge g, int x, int z) {
        return details(g).springs().get(KeyedSlots.key(x, z));
    }

    /** A lower bound of the floors of a gorge's water near a column in its reach (for the preliminary surface). */
    static double floorNear(Gorge g, VerdantDescent.Point p) {
        int lowest = Math.max(0, Math.min(p.body, g.floor()) - 1);
        return VerdantDescent.waterTop(lowest) - 8.0;
    }
}
