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
 * The water of {@code verdant_falls} (W-terrein 2c, plan §2.2): stepped basins on the treads of {@link VerdantDescent},
 * like Plitvice. Water runs down the descent in <em>streams</em>, one per stream cell along s (~{@link #STREAM_CELL}
 * blocks). On each tread ("body" b, the tread on level 64 + 8b with the riser above it) a stream is still water at one
 * level, {@link VerdantDescent#waterTop}: a plunge pool in a cove cut into the riser under the fall from the tread above,
 * a brook in a gully (2–3 wide, 1–2 deep) meandering across the tread, now and then a lake (2–5 deep, on the tread
 * behind a travertine threshold of 6–10 blocks), and at the tread's outer edge a spring (walled in on three sides, the
 * lip of rock above it) and a recorded fall in the column beyond, which lands in the next tread's pool. The lowest tread
 * (64) holds sea water: its gullies run into the foot river. Long lakes have a second outlet, a spur, whose fall lands in
 * a pool of its own.
 * <p>
 * Water never stands against air: all water of a body lies at one level and is cut per block (the terrain is exact over
 * the descent, {@link IslandField#thin}); a column may hold water only if its four neighbours lie on its body or higher
 * (so only a spring meets the edge), and the riser and tread around it stand above the water. The only water against
 * air is the falls' (contract §4).
 * <p>
 * Pure: hashes of the world salt ({@link CellShapes#cell}) and the descent; streams and falls are decided once per world.
 */
final class VerdantBasins {
    /** One stream at most per this many blocks along the loop. */
    static final double STREAM_CELL = 48.0;
    private static final double STREAM_CHANCE = 0.85;
    /** Bodies (treads) 0 … MAX_BODY − 1 may hold water (levels 64 … 120). */
    static final int MAX_BODY = 8;
    /**
     * A segment's chance of a lake: LAKE_CHANCE ± LAKE_SWING with a slow noise along the ring (~1000 blocks), so some
     * stretches are lake terraces like Plitvice and others brooks and pools.
     */
    private static final double LAKE_CHANCE = 0.55;
    private static final double LAKE_SWING = 0.3;
    private static final double REGION_SCALE = 0.12;
    private static final double SPUR_CHANCE = 0.5;
    /**
     * A lake this long (half length, blocks) or longer may have a spur. A lake stays in its stream's own range (~±24),
     * so its half length is at most ~20.
     */
    private static final double SPUR_HALF = 16.0;
    /** How far (blocks along s) a column looks for streams: the widest lake reaches 35 from its stream. */
    private static final double REACH = 46.0;
    /** The banks of pools and gullies rise this many blocks per block (rounded, steeper than the risers). */
    private static final double BANK = 2.0;
    /**
     * A lake reaches this far (blocks) into the foot of the riser above its tread (negative: past the riser's foot), its
     * back shore a bank cut into the riser: the treads are only ~15 wide, so between the threshold (6–10) and the riser
     * there is no room otherwise.
     */
    private static final double LAKE_BACK = -4.0;
    /**
     * Polish: the back shore waves by this much more, mostly inwards (so it meets the riser above only at its points,
     * not as a straight wall along the contour).
     */
    private static final double BACK_WAVE = 2.0;
    /** A plunge pool's centre lies this far below the lip; the spring pool of a stream's top tread this far from the riser's foot. */
    private static final double POOL_OUT = 2.5;
    private static final double SPRING_POOL_IN = 4.0;
    /** Polish: how far (blocks) a plunge pool's shore wobbles with the detail noise. */
    private static final double POOL_WOBBLE = 0.7;
    /**
     * Polish (Plitvice): a lake's threshold is a travertine dam that bulges its tread's edge out over the riser below by
     * DAM_MIN…DAM_MAX blocks, waving along the edge; the lake's front shore follows DAM_SHARE of it, its shore waves by
     * SHORE_WAVE, and stays MIN_THRESHOLD from the tread's own edge.
     */
    private static final double DAM_MIN = 0.5;
    private static final double DAM_MAX = 3.5;
    private static final double DAM_SHARE = 0.7;
    private static final double SHORE_WAVE = 1.6;
    private static final double MIN_THRESHOLD = 3.5;
    /**
     * The rim output: travertine on the dry surface (the surface rule's calcite), and on the wet side of a threshold
     * too (≥ 0.75: a thin crust under the water).
     */
    static final double DRY_RIM = 0.6;
    static final double WET_RIM = 0.9;
    /** The water begins this far past the top terrace (the steam_fens rim stays dry). */
    private static final double TOP_MARGIN = 4.0;
    private static final int STREAM_SALT = 6306;
    private static final int SEGMENT_SALT = 6302;
    /** Part 2: niches behind falls (the plan's salt for niches) and rock bridges over brooks (a salt of their own). */
    private static final int NICHE_SALT = 6304;
    private static final int BRIDGE_SALT = 6308;
    /**
     * One in four falls with a lip of at least NICHE_LIP gets a niche, one in three of those a tunnel up (plan §2.4):
     * about 45 % of the tries fit (rock over it, no water near), so a fall tries with NICHE_CHANCE; every niche tries a
     * tunnel first (about 30 % fit).
     */
    private static final double NICHE_CHANCE = 0.55;
    private static final double TUNNEL_CHANCE = 1.0;
    private static final int NICHE_LIP = 6;
    /** A niche: 3 wide, 3 high, NICHE_DEPTH + 0…2 deep; with a tunnel TUNNEL_NICHE deep (the tunnel runs 6 from the edge). */
    private static final int NICHE_DEPTH = 4;
    private static final int TUNNEL_NICHE = 7;
    private static final int NICHE_HEIGHT = 3;
    /** The longest tunnel (blocks along the tread); it rises 1 in 2. */
    private static final int TUNNEL_MAX = 30;
    /** The tunnel's exit lies at least this far (blocks) from the tread's edge; the tunnel moves a row away every TUNNEL_DRIFT. */
    private static final double TUNNEL_EXIT = 6.0;
    private static final int TUNNEL_DRIFT = 3;
    /** One in five brooks gets a rock bridge (span 4–8, 2–3 thick); about 47 % of the tries fit, so a brook tries with this. */
    private static final double BRIDGE_CHANCE = 0.43;
    /** Past each end of a rock bridge the tread is levelled this far (blocks along the loop, review 2c-2). */
    private static final double LANDING = 1.5;
    /** Polish: the height of a bridge's vault over the tread's top block in its middle. */
    private static final double ARCH_RISE = 3.0;

    private final IslandField field;
    private final VerdantDescent descent;
    private final VerdantGorges gorges;

    VerdantBasins(IslandField field, VerdantDescent descent, VerdantGorges gorges) {
        this.field = field;
        this.descent = descent;
        this.gorges = gorges;
    }

    // ---------------------------------------------------------------- streams and segments

    /**
     * A stream: its index (in [0, count)), position along the loop, the range of the loop it owns (half way to the
     * next cells' nominal positions; its lakes stay inside, 8 blocks apart less their shores' wobble), its top body (the
     * highest tread with a full riser above it everywhere near) and its stretch's chance of a lake per tread.
     */
    record Stream(int index, double at, double low, double high, int top, double lakeChance) {
    }

    /**
     * What a stream does on one body: where it leaves the tread (its fall, along s), the gully's meander, half width and
     * depth, the plunge pool's radius and depth, and an optional lake (centre, half length, threshold to the edge,
     * depth) with an optional spur outlet (along s, NaN for none).
     */
    record Segment(double exit, double meander, double gullyHalf, double gullyDepth, double poolRadius, double poolDepth,
                   boolean lake, double lakeCentre, double lakeHalf, double threshold, double lakeDepth, double spur) {
    }

    int streamCount() {
        return RingArc.cellCount(VerdantDescent.perimeter(field.ringLayout()), STREAM_CELL);
    }

    double cellSize() {
        return VerdantDescent.perimeter(field.ringLayout()) / streamCount();
    }

    /** Stream {@code k} (taken modulo the count round the loop), or null where the cell has none. */
    @Nullable
    Stream stream(int k) {
        int kk = Math.floorMod(k, streamCount());
        return field.decisions().once(field.decisions().verdantStreams, kk, () -> Optional.ofNullable(placeStream(kk))).orElse(null);
    }

    private double nominal(int kk) {
        long h = CellShapes.cell(field.salt(), kk, 0, STREAM_SALT);
        return (kk + 0.3 + 0.4 * CellShapes.unit(h, 1)) * cellSize();
    }

    @Nullable
    private Stream placeStream(int kk) {
        long h = CellShapes.cell(field.salt(), kk, 0, STREAM_SALT);
        long before = CellShapes.cell(field.salt(), Math.floorMod(kk - 1, streamCount()), 0, STREAM_SALT);
        // Never two empty cells in a row: a gap is at most about two cells long.
        if (CellShapes.unit(h, 0) >= STREAM_CHANCE && CellShapes.unit(before, 0) < STREAM_CHANCE) {
            return null;
        }
        double at = nominal(kk);
        // Half way to the nominal positions of the cells beside (whether they hold a stream or not), across the seam too.
        double period = VerdantDescent.perimeter(field.ringLayout());
        int count = streamCount();
        double previous = nominal(Math.floorMod(kk - 1, count)) - (kk == 0 ? period : 0.0);
        double next = nominal(Math.floorMod(kk + 1, count)) + (kk == count - 1 ? period : 0.0);
        double low = 0.5 * (at + previous);
        double high = 0.5 * (at + next);
        // Part 2: a stream whose water would reach into a gorge's reach is left out (the gorge has its own water).
        if (!gorges.clear(Math.min(low, at - 22.0) - 4.0, Math.max(high, at + 22.0) + 4.0)) {
            return null;
        }
        double rim = Double.POSITIVE_INFINITY;
        for (double ds = -30.0; ds <= 30.0; ds += 15.0) {
            rim = Math.min(rim, descent.profile(at + ds).rim());
        }
        // The highest tread with a riser of its own above it and a few blocks to spare.
        int top = Math.min(MAX_BODY - 1, (int) Math.floor((rim - VerdantDescent.BANK_TOP - VerdantDescent.STEP * VerdantDescent.TREAD - 3.0)
                / VerdantDescent.STEP));
        RingLayout layout = field.ringLayout();
        double angle = at / VerdantDescent.midRadius(layout);
        double region = field.relief().warpAt(VerdantDescent.midRadius(layout) * Math.cos(angle) * REGION_SCALE + 5101.0,
                VerdantDescent.midRadius(layout) * Math.sin(angle) * REGION_SCALE - 3203.0);
        return new Stream(kk, at, low, high, top, LAKE_CHANCE + LAKE_SWING * Mth.clamp(region, -1.0, 1.0));
    }

    /** The segment of a stream on body {@code b}. */
    Segment segment(Stream stream, int b) {
        long h = CellShapes.cell(field.salt(), stream.index(), b, SEGMENT_SALT);
        double exit = stream.at() + (2.0 * CellShapes.unit(h, 0) - 1.0) * 6.0;
        double meander = (2.0 * CellShapes.unit(h, 1) - 1.0) * 8.0;
        double gullyHalf = 0.5 + 0.5 * CellShapes.unit(h, 2);
        double gullyDepth = 1.4 + 1.0 * CellShapes.unit(h, 3);
        double poolRadius = 3.0 + 2.0 * CellShapes.unit(h, 4);
        double poolDepth = 2.5 + 1.5 * CellShapes.unit(h, 5);
        boolean lake = b >= 1 && CellShapes.unit(h, 6) < stream.lakeChance();
        double centre = stream.at() + (2.0 * CellShapes.unit(h, 7) - 1.0) * 5.0;
        double half = Math.min(18.0 + 14.0 * CellShapes.unit(h, 8), Math.min(centre - stream.low(), stream.high() - centre) - 4.0);
        double threshold = 6.0 + 4.0 * CellShapes.unit(h, 9);
        double lakeDepth = 2.0 + 3.0 * CellShapes.unit(h, 10);
        double spur = Double.NaN;
        if (lake && half >= SPUR_HALF && CellShapes.unit(h, 11) < SPUR_CHANCE) {
            // At the lake's far end from the main outlet (a coin where the outlet is at the middle).
            double period = VerdantDescent.perimeter(field.ringLayout());
            double offset = RingArc.loopDelta(centre, exit, period);
            double side = Math.abs(offset) > 1.0 ? Math.signum(offset) : CellShapes.unit(h, 12) < 0.5 ? -1.0 : 1.0;
            double at = centre + side * (half - 5.0);
            if (RingArc.loopDistance(at, exit, period) >= 10.0) {
                spur = at;
            }
        }
        return new Segment(exit, meander, gullyHalf, gullyDepth, poolRadius, poolDepth, lake && half >= 8.0, centre, half,
                threshold, lakeDepth, spur);
    }

    /** Where the stream enters body {@code b}: the fall of the body above, or (its top body) where it leaves. */
    double entry(Stream stream, int b, Segment segment) {
        return b < stream.top() ? segment(stream, b + 1).exit() : segment.exit();
    }

    // ---------------------------------------------------------------- per column

    /** The basins' effect on one column ({@link #shape}). */
    static final class Water {
        double height;
        /** The tread (body) the column's water belongs to. */
        int body;
        /** The top (exclusive) of the still water above the sea in the column, {@link Integer#MIN_VALUE} for none. */
        int lakeTop;
        /** 0..1: a travertine rim (threshold edge, fall lip) and an exact steep wall (a cove's side). */
        double rim;
        double cliff;
    }

    /**
     * The ground of a column on the descent with the basins cut into it, into {@code out}: {@code p} is the column's
     * point ({@link VerdantDescent#locate}). With {@code fallCuts} the two blocks of gully behind each fall's spring are
     * cut too (they need the fall decided; without, for deciding falls).
     */
    void shape(int x, int z, VerdantDescent.Point p, boolean fallCuts, Water out) {
        out.height = p.treads;
        out.lakeTop = Integer.MIN_VALUE;
        out.rim = 0.0;
        out.cliff = 0.0;
        out.body = p.body;
        int b = p.body;
        if (p.kind != VerdantDescent.Point.DESCENT || b < 0 || b >= MAX_BODY
                || p.u < p.profile.descentStart(p.rimAt) + TOP_MARGIN || p.u > p.profile.river()) {
            return;
        }
        double period = VerdantDescent.perimeter(field.ringLayout());
        int k = (int) Math.floor(p.s / cellSize());
        Stream[] near = new Stream[3];
        int found = 0;
        for (int dk = -1; dk <= 1; dk++) {
            Stream stream = stream(k + dk);
            if (stream != null && b <= stream.top() && RingArc.loopDistance(p.s, stream.at(), period) <= REACH) {
                near[found++] = stream;
            }
        }
        if (found == 0) {
            return;
        }
        // The ramp's gradient (blocks of rise per block) and the neighbours' bodies, from the four neighbours.
        double east = descent.rampAt(x + 1, z);
        double west = descent.rampAt(x - 1, z);
        double south = descent.rampAt(x, z + 1);
        double north = descent.rampAt(x, z - 1);
        if (Double.isNaN(east) || Double.isNaN(west) || Double.isNaN(south) || Double.isNaN(north)) {
            return;
        }
        double g = Math.max(0.02, 0.5 * Math.hypot(east - west, south - north));
        double level = VerdantDescent.level(b);
        int water = VerdantDescent.waterTop(b);
        double ramp = p.ramp;
        double below = (VerdantDescent.level(b + 1) - ramp) / g;
        double fromFoot = (level + VerdantDescent.STEP * VerdantDescent.TREAD - ramp) / g;
        double toEdge = (ramp - level) / g;
        double t = Mth.clamp((VerdantDescent.level(b + 1) - ramp) / VerdantDescent.STEP, 0.0, 1.0);
        double cut = Double.POSITIVE_INFINITY;
        boolean wall = false;
        // Polish: the travertine dam of a lake on the tread above bulges its edge out over this tread's riser (the
        // riser keeps its rounded shape, moved out), so the threshold's line waves instead of following the contour.
        double base = p.treads;
        boolean dammed = false;
        for (int n = 0; n < found; n++) {
            if (b + 1 <= near[n].top()) {
                double e = dam(p.s, b + 1, segment(near[n], b + 1), g, period);
                if (e > 0.0) {
                    double raised = VerdantDescent.treadHeight(Math.min(ramp + e * g, VerdantDescent.level(b + 1)), p.detail);
                    if (raised > base + 0.5) {
                        base = raised;
                        dammed |= VerdantDescent.level(b + 1) - raised <= 2.0;
                    }
                }
            }
        }
        double rim = dammed ? DRY_RIM : 0.0;
        boolean wetRim = false;
        for (int n = 0; n < found; n++) {
            Stream stream = near[n];
            Segment seg = segment(stream, b);
            double entry = entry(stream, b, seg);
            boolean top = b == stream.top();
            // The gully: from the pool under the fall above (or the top tread's spring pool) to the outer edge.
            double run = RingArc.loopDelta(seg.exit(), entry, period);
            double centre = entry + run * Ring4Relief.smooth(t, 0.0, 1.0) + seg.meander() * Math.sin(Math.PI * t);
            if (b == 0) {
                // On the lowest tread (flat to the river: the ramp stays at its foot) the brook keeps meandering.
                double phase = 2.0 * Math.PI * (seg.exit() / 13.0 - Math.floor(seg.exit() / 13.0));
                centre += 0.6 * seg.meander() * Math.sin((p.profile.river() - p.u) / 7.0 + phase)
                        * (1.0 - Ring4Relief.smooth(ramp, VerdantDescent.BANK_TOP, VerdantDescent.BANK_TOP + 2.0));
            }
            double across = Math.abs(RingArc.loopDelta(p.s, centre, period)) - seg.gullyHalf();
            if (!top || fromFoot >= SPRING_POOL_IN) {
                cut = Math.min(cut, water - seg.gullyDepth() + BANK * Math.max(0.0, across));
                wall |= across <= 2.0 && across > 0.0;
            }
            // The pool: in a cove under the fall from the tread above, or the top tread's spring pool.
            double along = RingArc.loopDelta(p.s, entry, period);
            // Polish: the plunge pool's centre lies further out the larger it is (it still reaches the lip by a block or
            // more, so the fall lands in it), and its shore wobbles: the cove's lip is short and the pool round, not a
            // straight wall along the contour.
            double poolOut = Math.max(POOL_OUT, seg.poolRadius() - 1.6);
            double pool = Math.hypot(along, top ? fromFoot - SPRING_POOL_IN : below - poolOut) - seg.poolRadius()
                    - POOL_WOBBLE * norm(field.relief().detailAt(3.0 * x - 611.0, 3.0 * z + 157.0));
            cut = Math.min(cut, water - seg.poolDepth() + BANK * Math.max(0.0, pool));
            wall |= pool <= 2.0 && pool > 0.0;
            if (!top) {
                // A spur's pool under the spur fall of the lake above.
                Segment above = segment(stream, b + 1);
                if (!Double.isNaN(above.spur())) {
                    double spur = Math.hypot(RingArc.loopDelta(p.s, above.spur(), period), below - POOL_OUT) - 3.0;
                    cut = Math.min(cut, water - seg.poolDepth() + BANK * Math.max(0.0, spur));
                    wall |= spur <= 2.0 && spur > 0.0;
                }
            }
            if (seg.lake()) {
                double e = dam(p.s, b, seg, g, period);
                double bowl = lake(x, z, p.s, ramp, g, level, seg, period, e);
                cut = Math.min(cut, bowl);
                if (!Double.isNaN(seg.spur()) && toEdge < lakeMiddle(seg, g)) {
                    double spurAcross = Math.abs(RingArc.loopDelta(p.s, seg.spur(), period)) - 0.6;
                    cut = Math.min(cut, water - seg.gullyDepth() + BANK * Math.max(0.0, spurAcross));
                }
                // A thin crust on the wet side of the threshold, in patches.
                wetRim |= b >= 1 && bowl < water && toEdge < seg.threshold() + 2.0
                        && norm(field.relief().detailAt(3.0 * x - 503.0, 3.0 * z + 71.0)) > 0.3;
            }
            // Travertine on the threshold of a lake and the lip of each fall (the rim's face down to the water below
            // is the surface rule's).
            if (b >= 1) {
                rim = Math.max(rim, travertine(x, z, p.s, toEdge, b, seg, g, period));
            }
        }
        double height = Math.min(base, cut);
        if (height < water + 1.0) {
            if (b >= 1 && (east < level || west < level || south < level || north < level)) {
                // Next to a lower body (the edge of the tread): no water here and the rock of the edge stays whole, so
                // only a spring meets the edge, with rock over it.
                height = base;
            } else if (b == 0 && p.u < p.width / 2.0 + 2.0) {
                // Ring 3's half is sealed below the sea: the lowest tread stays dry there.
                height = Math.max(height, VerdantDescent.BANK_TOP - 1.0);
            }
        }
        if (fallCuts) {
            for (int n = 0; n < found; n++) {
                Segment seg = segment(near[n], b);
                for (boolean spur : new boolean[]{false, true}) {
                    Fall fall = fall(near[n], b, spur);
                    if (fall != null && fall.behind(x, z)) {
                        // The gully reaches the spring.
                        height = Math.min(height, water - seg.gullyDepth());
                    }
                    Fall above = fall(near[n], b + 1, spur);
                    if (fall != null && fall.beside(x, z) || above != null && above.beside(x, z)) {
                        // A spring's sides are rock at its level, on the tread (not the gully) and just over a slanting
                        // edge (not the pool below).
                        height = base;
                    }
                }
            }
        }
        out.height = height;
        if (b >= 1 && height < water) {
            out.lakeTop = water;
            out.rim = wetRim ? WET_RIM : 0.0;
        } else {
            out.rim = rim;
        }
        if (wall && height >= water && p.treads - water >= 3.0) {
            out.cliff = 1.0;
        }
    }

    /** The lake's bowl at a column (its height, +infinity outside the lake); {@code dam} the threshold's bulge there. */
    private double lake(int x, int z, double s, double ramp, double g, double level, Segment seg, double period, double dam) {
        double lo = level + seg.threshold() * g;
        double hi = level + VerdantDescent.STEP * VerdantDescent.TREAD - LAKE_BACK * g;
        double halfWidth = (hi - lo) / (2.0 * g);
        if (!lakeFits(seg, g)) {
            return Double.POSITIVE_INFINITY;
        }
        double across = (ramp - 0.5 * (lo + hi)) / g;
        double half = Math.max(seg.lakeHalf(), halfWidth);
        double along = Math.abs(RingArc.loopDelta(s, seg.lakeCentre(), period)) - (half - halfWidth);
        // A stadium (a rectangle with round ends), its outline wobbling with the detail noise.
        double outside = along <= 0.0 ? Math.abs(across) - halfWidth : Math.hypot(along, across) - halfWidth;
        double inside = -outside + 1.2 * field.relief().detailAt(2.0 * x + 77.0, 2.0 * z - 55.0);
        // Polish: a slower wave along the shore (bays and points of a few blocks, least on the threshold's side), and
        // the front shore following the dam out where it bulges, so the lake is wider there.
        double front = Ring4Relief.smooth(-across, 0.0, halfWidth);
        inside += SHORE_WAVE * (1.0 - 0.6 * front) * norm(field.relief().detailAt(1.7 * s + 401.0, 31.0 * level - 7.0))
                + BACK_WAVE * Ring4Relief.smooth(across, 0.0, halfWidth) * (norm(field.relief().detailAt(2.9 * s - 263.0, 17.0 * level + 5.0)) - 0.6);
        inside += DAM_SHARE * dam * front;
        // Never nearer the edge than MIN_THRESHOLD: the threshold stays a dry rim of travertine.
        inside = Math.min(inside, (ramp - level) / g - MIN_THRESHOLD);
        if (inside <= 0.0) {
            // Outside the outline a bank rising BANK per block: where the lake reaches into the riser above, its back
            // shore is that bank, not a wall (on the tread it stays at the tread's level).
            return inside < -8.0 ? Double.POSITIVE_INFINITY : level - BANK * inside;
        }
        return level - (1.0 + seg.lakeDepth()) * Ring4Relief.smooth(inside, 0.0, 4.0);
    }

    /**
     * How far (blocks) the travertine dam of the lake of {@code seg} on body {@code b} bulges its tread's edge out over
     * the riser below at loop position {@code s}: 0 off a lake and near its falls, else a wave of DAM_MIN…DAM_MAX
     * along the edge that tapers out at the lake's ends.
     */
    private double dam(double s, int b, Segment seg, double g, double period) {
        if (!seg.lake() || !lakeFits(seg, g)) {
            return 0.0;
        }
        double fromCentre = Math.abs(RingArc.loopDelta(s, seg.lakeCentre(), period));
        double taper = 1.0 - Ring4Relief.smooth(fromCentre, seg.lakeHalf() - 6.0, seg.lakeHalf() + 1.0);
        if (taper <= 0.0) {
            return 0.0;
        }
        double clear = Ring4Relief.smooth(RingArc.loopDistance(s, seg.exit(), period), 4.0, 8.0);
        if (!Double.isNaN(seg.spur())) {
            clear *= Ring4Relief.smooth(RingArc.loopDistance(s, seg.spur(), period), 4.0, 8.0);
        }
        double wave = unit(0.8 * field.relief().detailAt(2.2 * s + 811.0, 57.0 * b + 3.0)
                + 0.4 * field.relief().detailAt(5.5 * s - 97.0, 57.0 * b + 41.0));
        return taper * clear * (DAM_MIN + (DAM_MAX - DAM_MIN) * wave);
    }

    /**
     * The travertine (calcite) of a column on tread {@code b}, {@code toEdge} blocks in from its edge: a crust along a
     * lake's threshold 1–4 wide with a ragged inner line, patches reaching back over the threshold, and a ragged blob
     * round each fall's lip. 0 or {@link #DRY_RIM}.
     */
    private double travertine(int x, int z, double s, double toEdge, int b, Segment seg, double g, double period) {
        // The lips: an ellipse along the edge round each fall, its size and outline ragged.
        double ragged = norm(field.relief().detailAt(6.0 * x - 733.0, 6.0 * z + 291.0));
        for (double at : new double[]{seg.exit(), seg.spur()}) {
            if (Double.isNaN(at)) {
                continue;
            }
            double ds = RingArc.loopDelta(s, at, period);
            if (Math.abs(ds) > 8.0 || toEdge > 6.0) {
                continue;
            }
            double length = 3.0 + 2.0 * unit(field.relief().detailAt(1.3 * x + 59.0, 1.3 * z - 421.0));
            double depth = 1.5 + 2.0 * unit(field.relief().detailAt(1.1 * x - 177.0, 1.1 * z + 383.0));
            double e = (ds / length) * (ds / length) + (toEdge / depth) * (toEdge / depth);
            if (e < 1.0 + 0.5 * ragged) {
                return DRY_RIM;
            }
        }
        if (!seg.lake() || !lakeFits(seg, g)) {
            return 0.0;
        }
        double fromCentre = Math.abs(RingArc.loopDelta(s, seg.lakeCentre(), period));
        double taper = 1.0 - Ring4Relief.smooth(fromCentre, seg.lakeHalf() - 5.0, seg.lakeHalf() + 1.0);
        if (taper <= 0.05) {
            return 0.0;
        }
        // The crust along the edge: 1–4 wide, waving along the edge, its inner line ragged.
        double width = taper * (1.0 + 3.0 * unit(field.relief().detailAt(1.8 * s + 1517.0, 97.0 * b)));
        if (toEdge < width + 0.9 * ragged) {
            return DRY_RIM;
        }
        // Patches running back over the threshold, fewer further in.
        double patch = norm(field.relief().detailAt(2.6 * x + 233.0, 2.6 * z - 919.0));
        if (toEdge < seg.threshold() && taper > 0.3 && patch > 0.5 + 0.6 * toEdge / seg.threshold()) {
            return DRY_RIM;
        }
        return 0.0;
    }

    /**
     * The detail noise stretched to about −1..1 (its RMS is ~0.3: most of it lies within ±0.4), clamped to ±1.5.
     */
    private static double norm(double noise) {
        return Mth.clamp(2.8 * noise, -1.5, 1.5);
    }

    /** The detail noise as 0..1 ({@link #norm}). */
    private static double unit(double noise) {
        return Mth.clamp(0.5 + 0.5 * norm(noise), 0.0, 1.0);
    }

    /** Whether a lake has room on its tread where the ramp rises {@code g} per block (at least 5 blocks across). */
    private static boolean lakeFits(Segment seg, double g) {
        return VerdantDescent.STEP * VerdantDescent.TREAD / g - LAKE_BACK - seg.threshold() >= 5.0;
    }

    /** Blocks from the outer edge to the lake's middle (across). */
    private static double lakeMiddle(Segment seg, double g) {
        return 0.5 * (seg.threshold() + VerdantDescent.STEP * VerdantDescent.TREAD / g - LAKE_BACK);
    }

    // ---------------------------------------------------------------- falls

    /**
     * A fall of the descent: the spring at the tread's outer edge, walled in on three sides with rock over it, the fall
     * in the column beyond, from the spring's level down to the water of the body below.
     */
    record Fall(IslandField.Fall fall, int stepX, int stepZ) {
        /** Whether (x, z) is one of the two gully columns behind the spring (cut down to the gully's floor). */
        boolean behind(int x, int z) {
            int bx = fall.springX() - stepX;
            int bz = fall.springZ() - stepZ;
            return x == bx && z == bz || x == bx - stepX && z == bz - stepZ;
        }

        /** Whether (x, z) is one of the spring's two sides (its walls at the spring's level). */
        boolean beside(int x, int z) {
            int dx = x - fall.springX();
            int dz = z - fall.springZ();
            return dx == -stepZ && dz == stepX || dx == stepZ && dz == -stepX;
        }
    }

    /** The fall where stream {@code stream} leaves body {@code b} (or its lake's spur), decided once per world. */
    @Nullable
    Fall fall(Stream stream, int b, boolean spur) {
        if (b < 1 || b > stream.top()) {
            return null;
        }
        long key = ((long) stream.index() * MAX_BODY + b) * 2 + (spur ? 1 : 0);
        return field.decisions().once(field.decisions().verdantFalls, key, () -> Optional.ofNullable(placeFall(stream, b, spur)))
                .orElse(null);
    }

    @Nullable
    private Fall placeFall(Stream stream, int b, boolean spur) {
        Segment seg = segment(stream, b);
        double exit = spur ? seg.spur() : seg.exit();
        if (Double.isNaN(exit)) {
            return null;
        }
        RingLayout layout = field.ringLayout();
        double level = VerdantDescent.level(b);
        double angle = exit / VerdantDescent.midRadius(layout);
        // The tread's outer edge on the ray: where the ramp falls through the level (bisection between the top terrace
        // and the foot).
        VerdantDescent.Profile profile = descent.profile(exit);
        double start = VerdantDescent.zoneStart(layout) + profile.descentStart(VerdantDescent.rimAt(layout));
        double[] inner = VerdantDescent.onRay(layout, angle, start);
        double[] outer = VerdantDescent.onRay(layout, angle, VerdantDescent.zoneStart(layout) + profile.bankTop());
        double lo = Math.hypot(inner[0], inner[1]);
        double hi = Math.hypot(outer[0], outer[1]);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double rampLo = descent.rampAt(lo * cos, lo * sin);
        double rampHi = descent.rampAt(hi * cos, hi * sin);
        if (Double.isNaN(rampLo) || Double.isNaN(rampHi) || rampLo < level || rampHi >= level) {
            return null;
        }
        for (int n = 0; n < 30; n++) {
            double mid = 0.5 * (lo + hi);
            double r = descent.rampAt(mid * cos, mid * sin);
            if (r >= level) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double ex = 0.5 * (lo + hi) * cos;
        double ez = 0.5 * (lo + hi) * sin;
        // Downhill: the cardinal step closest to the ramp's falling gradient.
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
            int fx = sx + step[0];
            int fz = sz + step[1];
            if (inside(sx, sz, level) && !inside(fx, fz, level)) {
                return check(stream, b, spur, seg, sx, sz, fx, fz, step, perp);
            }
        }
        return null;
    }

    /** Whether a column lies on body {@code level}'s tread or higher (its ramp at or above the level). */
    private boolean inside(int x, int z, double level) {
        double r = descent.rampAt(x, z);
        return !Double.isNaN(r) && r >= level;
    }

    /** The fall from spring (sx, sz) into column (fx, fz) if every rule holds, else null. */
    @Nullable
    private Fall check(Stream stream, int b, boolean spur, Segment seg, int sx, int sz, int fx, int fz, int[] step, int[] perp) {
        double level = VerdantDescent.level(b);
        int water = VerdantDescent.waterTop(b);
        int springY = water - 1;
        int bx = sx - step[0];
        int bz = sz - step[1];
        int b2x = bx - step[0];
        int b2z = bz - step[1];
        // The two gully columns behind the spring and all their neighbours lie on the tread.
        int[][] onTread = {{bx, bz}, {bx + perp[0], bz + perp[1]}, {bx - perp[0], bz - perp[1]}, {b2x, b2z},
                {b2x + perp[0], b2z + perp[1]}, {b2x - perp[0], b2z - perp[1]}, {b2x - step[0], b2z - step[1]}};
        for (int[] c : onTread) {
            if (!inside(c[0], c[1], level)) {
                return null;
            }
        }
        double treadTop = level + VerdantDescent.STEP * VerdantDescent.TREAD;
        if (descent.rampAt(b2x, b2z) >= treadTop) {
            return null;
        }
        // Rock over the spring and on its two sides at its level: the sides are kept whole ({@link Fall#beside}), so
        // their ground is the tread's or, just over a slanting edge, the riser's top.
        Water w = new Water();
        if (shapeAt(sx, sz, w) <= springY + 1.0) {
            return null;
        }
        for (int side = -1; side <= 1; side += 2) {
            VerdantDescent.Point lp = descent.locate(sx + side * perp[0], sz + side * perp[1], new VerdantDescent.Point());
            if (lp.kind != VerdantDescent.Point.DESCENT || lp.treads <= springY + 1.0) {
                return null;
            }
        }
        // The fall lands in the water of the body below: the top block of its pool is open in the fall's column.
        int lower = b - 1;
        VerdantDescent.Point fp = descent.locate(fx, fz, new VerdantDescent.Point());
        if (fp.kind != VerdantDescent.Point.DESCENT || fp.body != lower) {
            return null;
        }
        double ground = shapeAt(fx, fz, w);
        int bottom = VerdantDescent.waterTop(lower);
        if (lower == 0) {
            bottom = (int) VerdantDescent.BANK_TOP - 1;
            if (fp.u < fp.width / 2.0 + 2.0 || ground > bottom - 1) {
                return null;
            }
        } else if (ground > bottom - 1 || w.lakeTop != bottom) {
            return null;
        }
        return new Fall(new IslandField.Fall(sx, sz, springY, fx, fz, bottom), step[0], step[1]);
    }

    /** The ground of a column with the basins but without the falls' cuts (for deciding the falls). */
    private double shapeAt(int x, int z, Water w) {
        VerdantDescent.Point p = descent.locate(x, z, new VerdantDescent.Point());
        shape(x, z, p, false, w);
        return w.height;
    }

    /** The falls of the streams near a position along the loop (within {@code reach} blocks), all bodies. */
    List<Fall> fallsNear(double s, double reach) {
        List<Fall> falls = new ArrayList<>();
        double period = VerdantDescent.perimeter(field.ringLayout());
        int k = (int) Math.floor(RingArc.wrap(s, period) / cellSize());
        int cells = (int) Math.ceil(reach / cellSize()) + 1;
        for (int dk = -cells; dk <= cells; dk++) {
            Stream stream = stream(k + dk);
            if (stream == null || RingArc.loopDistance(s, stream.at(), period) > reach + 20.0) {
                continue;
            }
            for (int b = 1; b <= stream.top(); b++) {
                for (boolean spur : new boolean[]{false, true}) {
                    Fall fall = fall(stream, b, spur);
                    if (fall != null) {
                        falls.add(fall);
                    }
                }
            }
        }
        return falls;
    }

    /** The streams near a position along the loop with water on body {@code b} (for a column's spring). */
    List<Fall> fallsOfBody(double s, int b) {
        List<Fall> falls = new ArrayList<>(2);
        double period = VerdantDescent.perimeter(field.ringLayout());
        int k = (int) Math.floor(RingArc.wrap(s, period) / cellSize());
        for (int dk = -1; dk <= 1; dk++) {
            Stream stream = stream(k + dk);
            if (stream == null || b > stream.top() || RingArc.loopDistance(s, stream.at(), period) > REACH) {
                continue;
            }
            for (boolean spur : new boolean[]{false, true}) {
                Fall fall = fall(stream, b, spur);
                if (fall != null) {
                    falls.add(fall);
                }
            }
        }
        return falls;
    }

    // ---------------------------------------------------------------- niches, tunnels, bridges (part 2, plan §2.4)

    /**
     * A niche behind a fall: rows 0…depth − 1 behind the spring (away from the fall), 3 wide, from {@code bottom} (the
     * first block above the water the fall lands in) 3 high; with a tunnel ({@code side} ±1 along the spring's sides,
     * 0 for none) from its back row {@code length} blocks along the tread, rising 1 in 2 to the tread's surface.
     */
    record Niche(int depth, int bottom, int side, int length) {
        /** The open blocks [lo, hi] of the niche or its tunnel in column (x, z), or null. */
        @Nullable
        int[] range(Fall fall, int x, int z) {
            IslandField.Fall f = fall.fall();
            int dx = x - f.springX();
            int dz = z - f.springZ();
            int k = -(dx * fall.stepX() + dz * fall.stepZ());
            int j = -dx * fall.stepZ() + dz * fall.stepX();
            if (k >= 0 && k < depth && Math.abs(j) <= 1) {
                return new int[]{bottom, bottom + NICHE_HEIGHT - 1};
            }
            int run = side * j;
            if (side != 0 && run >= 0 && run <= length && Math.abs(k - tunnelRow(depth, run)) <= 1) {
                int floor = bottom + run / 2;
                return new int[]{floor, floor + NICHE_HEIGHT - 1};
            }
            return null;
        }

        /** The middle row (blocks behind the spring) of a tunnel {@code run} blocks along: it drifts away from the edge. */
        static int tunnelRow(int depth, int run) {
            return depth - 1 + run / TUNNEL_DRIFT;
        }
    }

    /**
     * A rock bridge over a brook: at the ramp value {@code ramp} (blocks across the tread at gradient {@code grad}),
     * centred on the gully at {@code centre} along the loop, {@code span} long, up to {@code thickness} thick in the
     * middle; its underside from the tread's top block at the ends up to two blocks over the tread's top in the middle.
     * The deck thins towards its ends (review 2c-2: walkable): one block over the tread at an end, then a block higher per
     * block inwards up to its full height, so it is walked onto and over with steps of one block; where a short span
     * leaves no room for that the underside comes down to one block over the tread (at least 2 thick in the middle,
     * always a block of air over the brook). Just past each end a dry dip of the tread is filled to the tread's top
     * ({@link #landing}).
     */
    record Bridge(double centre, double ramp, double grad, double span, int thickness, int level) {
        /** The rock [lo, hi] of the arch in a column at ramp value r and loop position s, or null. */
        @Nullable
        int[] range(double r, double s, double period) {
            double along = (r - ramp) / grad;
            double d = RingArc.loopDelta(s, centre, period);
            if (Math.abs(along) > 1.5 || Math.abs(d) > 0.5 * span) {
                return null;
            }
            double q = 2.0 * d / span;
            // Polish: a real arch. The deck rises a block per block from one over the tread at its ends up to
            // thickness + 1 over it; the underside is an elliptic vault ARCH_RISE over the tread's top block in the middle,
            // coming down to the tread at the ends, so there is air under it over the brook and beside it.
            int top = level + Math.min(thickness + 1, (int) Math.floor(0.5 * span - Math.abs(d)));
            int under = Mth.ceil(level - 1.0 + ARCH_RISE * Math.sqrt(Math.max(0.0, 1.0 - q * q)));
            // At least two thick in its middle half, one towards its ends; the vault wins over the thickness.
            return new int[]{Math.min(under, top - (Math.abs(q) < 0.5 ? 1 : 0)), top};
        }

        /** Whether a column at ramp value r and loop position s lies just past an end, within the deck's width. */
        boolean landing(double r, double s, double period) {
            double d = Math.abs(RingArc.loopDelta(s, centre, period));
            return Math.abs((r - ramp) / grad) <= 1.5 && d > 0.5 * span && d <= 0.5 * span + LANDING;
        }
    }

    /**
     * The niches, tunnels and rock bridges reaching a column of tread {@code p.body} into the column: open
     * ({@code carveLo…carveHi}) and rock ({@code solidLo…solidHi}). A niche counts only behind a kept fall.
     */
    void features(int x, int z, VerdantDescent.Point p, IslandField.Column c) {
        int b = p.body;
        if (p.kind != VerdantDescent.Point.DESCENT || b < 0 || b >= MAX_BODY || p.u > p.profile.river()) {
            return;
        }
        double period = VerdantDescent.perimeter(field.ringLayout());
        int k = (int) Math.floor(p.s / cellSize());
        for (int dk = -1; dk <= 1; dk++) {
            Stream stream = stream(k + dk);
            if (stream == null || RingArc.loopDistance(p.s, stream.at(), period) > REACH + TUNNEL_MAX) {
                continue;
            }
            // The niches of the falls leaving this tread and the one above (a niche reaches over a slanting edge).
            for (int fb = b; fb <= b + 1; fb++) {
                for (boolean spur : new boolean[]{false, true}) {
                    Fall fall = fall(stream, fb, spur);
                    Niche niche = fall == null ? null : niche(stream, fb, spur, fall);
                    int[] range = niche == null ? null : niche.range(fall, x, z);
                    if (range != null && field.fallsOfChunk(fall.fall().fallX() >> 4, fall.fall().fallZ() >> 4).contains(fall.fall())) {
                        c.carveLo = Math.min(c.carveLo, range[0]);
                        c.carveHi = Math.max(c.carveHi, range[1]);
                    }
                }
            }
            if (b >= 1 && b <= stream.top()) {
                Bridge bridge = bridge(stream, b);
                int[] range = bridge == null ? null : bridge.range(p.ramp, p.s, period);
                if (range != null) {
                    // Polish: a rock bridge, its deck bare stone (the surface rule's stone with moss patches).
                    c.cliff = 1.0;
                }
                if (range == null && bridge != null && bridge.landing(p.ramp, p.s, period) && c.height < bridge.level()) {
                    // Past an end: a dip of the tread (its rounded edge, a gully's bank) is filled up to the tread's top
                    // block where no water stands in it, so the step onto the deck stays one block.
                    Water w = new Water();
                    shape(x, z, p, true, w);
                    if (w.lakeTop == Integer.MIN_VALUE || Mth.ceil(c.height) >= w.lakeTop) {
                        range = new int[]{Mth.floor(c.height), bridge.level() - 1};
                    }
                }
                if (range != null) {
                    c.solidLo = Math.min(c.solidLo, range[0]);
                    c.solidHi = Math.max(c.solidHi, range[1]);
                }
            }
        }
    }

    /** The niche behind the fall of stream {@code stream} off tread b (or its spur), decided once per world, or null. */
    @Nullable
    Niche niche(Stream stream, int b, boolean spur, Fall fall) {
        long key = ((long) stream.index() * MAX_BODY + b) * 2 + (spur ? 1 : 0);
        return field.decisions().once(field.decisions().verdantNiches, key, () -> Optional.ofNullable(placeNiche(key, b, fall)))
                .orElse(null);
    }

    @Nullable
    private Niche placeNiche(long key, int b, Fall fall) {
        long h = CellShapes.cell(field.salt(), key, 0, NICHE_SALT);
        if (CellShapes.unit(h, 0) >= NICHE_CHANCE) {
            return null;
        }
        IslandField.Fall f = fall.fall();
        int bottom = f.bottom();
        if (VerdantDescent.level(b) - bottom < NICHE_LIP) {
            return null;
        }
        Map<Long, Water> memo = new HashMap<>();
        boolean tunnel = CellShapes.unit(h, 1) < TUNNEL_CHANCE;
        if (tunnel && nicheFits(fall, bottom, TUNNEL_NICHE, memo)) {
            boolean first = CellShapes.unit(h, 2) < 0.5;
            for (int side : first ? new int[]{1, -1} : new int[]{-1, 1}) {
                int length = tunnelFits(fall, b, bottom, side, memo);
                if (length > 0) {
                    return new Niche(TUNNEL_NICHE, bottom, side, length);
                }
            }
        }
        int depth = NICHE_DEPTH + (int) Math.floor(3.0 * CellShapes.unit(h, 3) * 0.999);
        return nicheFits(fall, bottom, depth, memo) ? new Niche(depth, bottom, 0, 0) : null;
    }

    /** The column's water and ground as the field makes it (with the falls' cuts), remembered for one decision. */
    private Water waterAt(int x, int z, Map<Long, Water> memo) {
        return memo.computeIfAbsent(KeyedSlots.key(x, z), key -> {
            Water w = new Water();
            VerdantDescent.Point p = descent.locate(x, z, new VerdantDescent.Point());
            if (p.kind != VerdantDescent.Point.DESCENT) {
                w.height = Double.NaN;
                w.body = -1;
                w.lakeTop = Integer.MIN_VALUE;
                return w;
            }
            shape(x, z, p, true, w);
            return w;
        });
    }

    /**
     * Whether the niche fits: at least two blocks of rock over every cell up to the ground or the water, and no water
     * within two columns of it unless two blocks of rock lie between (at the mouth the water the fall lands in lies
     * below the niche's floor, which is rock up to that water's top).
     */
    private boolean nicheFits(Fall fall, int bottom, int depth, Map<Long, Water> memo) {
        IslandField.Fall f = fall.fall();
        int top = bottom + NICHE_HEIGHT - 1;
        for (int k = 0; k < depth + 2; k++) {
            for (int j = -3; j <= 3; j++) {
                int x = f.springX() - k * fall.stepX() - j * fall.stepZ();
                int z = f.springZ() - k * fall.stepZ() + j * fall.stepX();
                Water w = waterAt(x, z, memo);
                if (Double.isNaN(w.height)) {
                    return false;
                }
                boolean cell = k < depth && Math.abs(j) <= 1;
                if ((cell || w.lakeTop != Integer.MIN_VALUE) && Mth.ceil(w.height) < top + 3) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The length of a tunnel from the niche's back row along {@code side}, rising 1 in 2 to the tread's surface, or 0
     * where it does not fit: every cell on tread b, two blocks of rock between it and any water, the exit at least 6
     * blocks from the tread's edge.
     */
    private int tunnelFits(Fall fall, int b, int bottom, int side, Map<Long, Water> memo) {
        IslandField.Fall f = fall.fall();
        for (int run = 2; run <= TUNNEL_MAX; run++) {
            int floor = bottom + run / 2;
            int back = Niche.tunnelRow(TUNNEL_NICHE, run);
            boolean open = false;
            for (int k = back - 1; k <= back + 1; k++) {
                int j = side * run;
                int x = f.springX() - k * fall.stepX() - j * fall.stepZ();
                int z = f.springZ() - k * fall.stepZ() + j * fall.stepX();
                double r = descent.rampAt(x, z);
                if (Double.isNaN(r) || r < VerdantDescent.level(b) || r >= VerdantDescent.level(b + 1)) {
                    return 0;
                }
                Water w = waterAt(x, z, memo);
                if (w.lakeTop != Integer.MIN_VALUE) {
                    return 0;
                }
                if (k == back && floor >= w.height - 1.0) {
                    // The exit: at least 6 blocks from the tread's edge (one less for the cell's corner).
                    double gx = descent.rampAt(x + 1, z) - descent.rampAt(x - 1, z);
                    double gz = descent.rampAt(x, z + 1) - descent.rampAt(x, z - 1);
                    if ((r - VerdantDescent.level(b)) / Math.max(0.02, 0.5 * Math.hypot(gx, gz)) < TUNNEL_EXIT) {
                        return 0;
                    }
                    open = true;
                }
                // No water within two columns unless two blocks of rock lie between it and the tunnel's roof.
                for (int dk = -2; dk <= 2; dk++) {
                    for (int dj = -2; dj <= 2; dj++) {
                        int nx = x - dk * fall.stepX() - dj * fall.stepZ();
                        int nz = z - dk * fall.stepZ() + dj * fall.stepX();
                        Water n = waterAt(nx, nz, memo);
                        if (n.lakeTop != Integer.MIN_VALUE && Mth.ceil(n.height) < floor + NICHE_HEIGHT + 2) {
                            return 0;
                        }
                    }
                }
            }
            if (open) {
                // The exit: on the tread, at least 6 from its edge (the back row lies TUNNEL_NICHE − 1 behind the spring).
                return run;
            }
        }
        return 0;
    }

    /** The rock bridge over stream {@code stream}'s brook on tread b, decided once per world, or null. */
    @Nullable
    Bridge bridge(Stream stream, int b) {
        long key = (long) stream.index() * MAX_BODY + b;
        return field.decisions().once(field.decisions().verdantBridges, key, () -> Optional.ofNullable(placeBridge(stream, b)))
                .orElse(null);
    }

    @Nullable
    private Bridge placeBridge(Stream stream, int b) {
        long h = CellShapes.cell(field.salt(), stream.index(), b, BRIDGE_SALT);
        if (CellShapes.unit(h, 0) >= BRIDGE_CHANCE || b < 1 || b > stream.top()) {
            return null;
        }
        Segment seg = segment(stream, b);
        double entry = entry(stream, b, seg);
        double period = VerdantDescent.perimeter(field.ringLayout());
        RingLayout layout = field.ringLayout();
        double r = VerdantDescent.midRadius(layout);
        double level = VerdantDescent.level(b);
        // The gradient half way across the tread, on the ray of the stream.
        double[] mid = rampPoint(stream.at() / r, level + 0.5 * VerdantDescent.STEP * VerdantDescent.TREAD);
        if (mid == null) {
            return null;
        }
        double gx = descent.rampAt(mid[0] + 1.0, mid[1]) - descent.rampAt(mid[0] - 1.0, mid[1]);
        double gz = descent.rampAt(mid[0], mid[1] + 1.0) - descent.rampAt(mid[0], mid[1] - 1.0);
        double grad = 0.5 * Math.hypot(gx, gz);
        if (Double.isNaN(grad) || grad < 0.05) {
            return null;
        }
        double flat = VerdantDescent.STEP * VerdantDescent.TREAD / grad;
        if (flat < 9.0) {
            return null;
        }
        double toEdge = 4.0 + (flat - 8.0) * CellShapes.unit(h, 1);
        double ramp = level + toEdge * grad;
        double t = Mth.clamp((VerdantDescent.level(b + 1) - ramp) / VerdantDescent.STEP, 0.0, 1.0);
        double run = RingArc.loopDelta(seg.exit(), entry, period);
        double centre = RingArc.wrap(entry + run * Ring4Relief.smooth(t, 0.0, 1.0) + seg.meander() * Math.sin(Math.PI * t), period);
        // Polish: at least 6 long, so the vault shows over the gully (2c: 4–8).
        double span = 6.0 + 2.0 * CellShapes.unit(h, 2);
        int thickness = CellShapes.unit(h, 3) < 0.5 ? 2 : 3;
        // The brook under it, dry tread at both ends (not a lake, a pool or a spring).
        Water w = new Water();
        double[] c = rampPoint(centre / r, ramp);
        if (c == null || !(shapeAt(Mth.floor(c[0]), Mth.floor(c[1]), w) < VerdantDescent.waterTop(b) - 1.0) || w.lakeTop == Integer.MIN_VALUE) {
            return null;
        }
        for (int end = -1; end <= 1; end += 2) {
            double[] e = rampPoint((centre + end * (0.5 * span + 0.5)) / r, ramp);
            if (e == null || shapeAt(Mth.floor(e[0]), Mth.floor(e[1]), w) < level - 0.5 || w.lakeTop != Integer.MIN_VALUE) {
                return null;
            }
        }
        return new Bridge(centre, ramp, grad, span, thickness, (int) level);
    }

    /** The point on the ray at {@code angle} where the ramp falls through {@code value} (bisection), or null. */
    @Nullable
    double[] rampPoint(double angle, double value) {
        RingLayout layout = field.ringLayout();
        double start = VerdantDescent.zoneStart(layout);
        VerdantDescent.Profile profile = descent.profile(angle * VerdantDescent.midRadius(layout));
        double[] inner = VerdantDescent.onRay(layout, angle, start + profile.descentStart(VerdantDescent.rimAt(layout)));
        double[] outer = VerdantDescent.onRay(layout, angle, start + profile.bankTop());
        double lo = Math.hypot(inner[0], inner[1]);
        double hi = Math.hypot(outer[0], outer[1]);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double rampLo = descent.rampAt(lo * cos, lo * sin);
        double rampHi = descent.rampAt(hi * cos, hi * sin);
        if (Double.isNaN(rampLo) || Double.isNaN(rampHi) || rampLo < value || rampHi >= value) {
            return null;
        }
        for (int n = 0; n < 30; n++) {
            double m = 0.5 * (lo + hi);
            if (descent.rampAt(m * cos, m * sin) >= value) {
                lo = m;
            } else {
                hi = m;
            }
        }
        return new double[]{0.5 * (lo + hi) * cos, 0.5 * (lo + hi) * sin};
    }

    /** A lower bound of the floors of the water near a column (for the preliminary surface), or +infinity. */
    double floorNear(VerdantDescent.Point p, double reach) {
        if (p.kind != VerdantDescent.Point.DESCENT || p.body < 0) {
            return Double.POSITIVE_INFINITY;
        }
        double floor = Double.POSITIVE_INFINITY;
        double period = VerdantDescent.perimeter(field.ringLayout());
        int k = (int) Math.floor(p.s / cellSize());
        for (int dk = -2; dk <= 2; dk++) {
            Stream stream = stream(k + dk);
            if (stream != null && RingArc.loopDistance(p.s, stream.at(), period) <= REACH + reach) {
                int lowest = Math.max(0, Math.min(p.body, stream.top()) - 1);
                floor = Math.min(floor, VerdantDescent.waterTop(lowest) - 7.0);
            }
        }
        return floor;
    }
}
