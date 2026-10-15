package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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
    /** A plunge pool's centre lies this far below the lip; the spring pool of a stream's top tread this far from the riser's foot. */
    private static final double POOL_OUT = 2.5;
    private static final double SPRING_POOL_IN = 4.0;
    /** The water begins this far past the top terrace (the steam_fens rim stays dry). */
    private static final double TOP_MARGIN = 4.0;
    private static final int STREAM_SALT = 6306;
    private static final int SEGMENT_SALT = 6302;

    private final IslandField field;
    private final VerdantDescent descent;

    VerdantBasins(IslandField field, VerdantDescent descent) {
        this.field = field;
        this.descent = descent;
    }

    // ---------------------------------------------------------------- streams and segments

    /**
     * A stream: its index (in [0, count)), position along the loop, the range of the loop it owns (half way to the
     * next cells' nominal positions; its lakes stay inside, 8 blocks apart at least), its top body (the highest tread
     * with a full riser above it everywhere near) and its stretch's chance of a lake per tread.
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
        if (lake && half >= 22.0 && CellShapes.unit(h, 11) < SPUR_CHANCE) {
            double side = CellShapes.unit(h, 12) < 0.5 ? -1.0 : 1.0;
            double at = centre + side * (half - 8.0);
            double period = VerdantDescent.perimeter(field.ringLayout());
            if (RingArc.loopDistance(at, exit, period) >= 12.0) {
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
            double pool = Math.hypot(along, top ? fromFoot - SPRING_POOL_IN : below - POOL_OUT) - seg.poolRadius();
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
                cut = Math.min(cut, lake(x, z, p.s, ramp, g, level, seg, period));
                if (!Double.isNaN(seg.spur()) && toEdge < lakeMiddle(seg, g)) {
                    double spurAcross = Math.abs(RingArc.loopDelta(p.s, seg.spur(), period)) - 0.6;
                    cut = Math.min(cut, water - seg.gullyDepth() + BANK * Math.max(0.0, spurAcross));
                }
            }
            // Travertine on the threshold of a lake and the lip of each fall (rim faces down 7 blocks, the surface).
            if (b >= 1 && toEdge <= 2.5) {
                boolean threshold = seg.lake() && lakeFits(seg, g)
                        && Math.abs(RingArc.loopDelta(p.s, seg.lakeCentre(), period)) <= seg.lakeHalf() - 2.0;
                boolean lip = RingArc.loopDistance(p.s, seg.exit(), period) <= 4.0
                        || !Double.isNaN(seg.spur()) && RingArc.loopDistance(p.s, seg.spur(), period) <= 4.0;
                if (threshold || lip) {
                    out.rim = 1.0;
                }
            }
        }
        double height = Math.min(p.treads, cut);
        if (height < water + 1.0) {
            if (b >= 1 && (east < level || west < level || south < level || north < level)) {
                // Next to a lower body (the edge of the tread): no water here and the rock of the edge stays whole, so
                // only a spring meets the edge, with rock over it.
                height = p.treads;
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
                        height = p.treads;
                    }
                }
            }
        }
        out.height = height;
        if (b >= 1 && height < water) {
            out.lakeTop = water;
        }
        if (wall && height >= water && p.treads - water >= 3.0) {
            out.cliff = 1.0;
        }
    }

    /** The lake's bowl at a column (its height, +infinity outside the lake). */
    private double lake(int x, int z, double s, double ramp, double g, double level, Segment seg, double period) {
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
        if (inside <= 0.0) {
            // Outside the outline a bank rising BANK per block: where the lake reaches into the riser above, its back
            // shore is that bank, not a wall (on the tread it stays at the tread's level).
            return inside < -8.0 ? Double.POSITIVE_INFINITY : level - BANK * inside;
        }
        return level - (1.0 + seg.lakeDepth()) * Ring4Relief.smooth(inside, 0.0, 4.0);
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
