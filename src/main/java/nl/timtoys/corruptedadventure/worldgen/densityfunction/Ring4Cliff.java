package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import javax.annotation.Nullable;

/**
 * The cliff 3→4 (package 62): ring 3 ends in a wall of {@link #MIN_HEIGHT}–{@link #MAX_HEIGHT} blocks at the outer
 * edge of the zone 3→4 ({@link #LINE_AT} of the zone, ring 4's side), with {@code steam_fens} on the plateau above it at
 * ring 3's height and real ring 4 at its foot. Ring 4's input in the zone follows that plateau up to the line and drops
 * to its own terrain there; the blend of {@code ring_blend} then keeps ring 3's terrain on the plateau (it weighs ring 4
 * at 97 % at the line). Near the line the wall is exact per block ({@code thin}), so it stands vertical and its falls
 * hang free: about every few hundred blocks a ravine or a scree slope breaks it ({@link #GAP_CELL}: a way down), and
 * steaming waterfalls pour from springs at its top into plunge pools at the sea level ({@link #FALL_CELL}).
 * <p>
 * Distances are blocks across the line (outwards positive) and along it ({@code arc}: the angle times the line's
 * radius). Pure: functions of the layout, the salt and x, z.
 */
final class Ring4Cliff {
    /** Where the wall stands in the zone 3→4 (its blend: 0 ring 3's edge, 1 ring 4's). */
    static final double LINE_AT = 0.9;
    static final double MIN_HEIGHT = 40.0;
    static final double MAX_HEIGHT = 80.0;
    /** The wall is exact per block within this many blocks of the line, either side. */
    static final double WALL_BAND = 6.0;
    /** How far the line wanders (blocks) with the detail noise. */
    static final double JITTER = 3.0;
    /** One gap (ravine or scree slope) at most per this many blocks along the line. */
    static final int GAP_CELL = 280;
    private static final double GAP_CHANCE = 0.8;
    /** A ravine reaches this far into the plateau (blocks), its floor ramping down to the foot. */
    static final double RAVINE_LENGTH = 48.0;
    /** A scree slope runs from this far inside the line to {@link #SCREE_OUT} beyond it. */
    static final double SCREE_IN = 30.0;
    static final double SCREE_OUT = 12.0;
    /** One fall at most per this many blocks along the line. */
    static final int FALL_CELL = 160;
    private static final double FALL_CHANCE = 0.6;
    /** A fall's plunge pool at the foot: radius and bed (blocks). */
    static final double POOL_RADIUS = 6.0;
    static final double POOL_BED = 56.0;
    private static final int GAP_SALT = 6201;
    private static final int FALL_SALT = 6202;

    private Ring4Cliff() {
    }

    /** The radius (effective distance) of the line: {@link #LINE_AT} across the zone 3→4. */
    static double lineRadius(RingLayout layout) {
        double r3 = layout.settings().ringOuterRadii().get(Ring.RING_3.ordinal());
        double half = layout.settings().transitionWidth(Ring.RING_3.ordinal()) / 2.0;
        return r3 + half * (2.0 * LINE_AT - 1.0);
    }

    /** The inner edge of the zone 3→4 (effective distance): ring 4's input starts there. */
    static double zoneStart(RingLayout layout) {
        double r3 = layout.settings().ringOuterRadii().get(Ring.RING_3.ordinal());
        return r3 - layout.settings().transitionWidth(Ring.RING_3.ordinal()) / 2.0;
    }

    /** The position along the line (blocks). */
    static double arc(RingLayout layout, double x, double z) {
        return Math.atan2(z, x) * lineRadius(layout);
    }

    /** The plateau's height above the foot: ring 3's height, kept {@link #MIN_HEIGHT}–{@link #MAX_HEIGHT} above it. */
    static double plateau(double ring3, double foot) {
        return Mth.clamp(ring3, foot + MIN_HEIGHT, foot + MAX_HEIGHT);
    }

    /** A gap of the line: centre along it, half width, and whether it is a ravine (else a scree slope). */
    record Gap(double centre, double halfWidth, boolean ravine) {
        /** 0..1: how much of the gap a position along the line has (its edges blend over 4 blocks). */
        double weight(double arc) {
            return 1.0 - Ring4Relief.smooth(Math.abs(arc - centre), halfWidth, halfWidth + 4.0);
        }
    }

    /** The gap of gap cell {@code k}, or null. */
    @Nullable
    static Gap gap(long salt, int k) {
        long h = CellShapes.cell(salt, k, 0, GAP_SALT);
        if (CellShapes.unit(h, 0) >= GAP_CHANCE) {
            return null;
        }
        boolean ravine = CellShapes.unit(h, 1) < 0.5;
        double halfWidth = ravine ? 5.0 + 2.0 * CellShapes.unit(h, 2) : 15.0 + 10.0 * CellShapes.unit(h, 2);
        double centre = (k + 0.2 + 0.6 * CellShapes.unit(h, 3)) * GAP_CELL;
        return new Gap(centre, halfWidth, ravine);
    }

    /** The gap a position along the line lies in (the strongest of its cell and the two beside), or null. */
    @Nullable
    static Gap gapAt(long salt, double arc) {
        int k = Mth.floor(arc / GAP_CELL);
        Gap best = null;
        double weight = 0.0;
        for (int dk = -1; dk <= 1; dk++) {
            Gap gap = gap(salt, k + dk);
            if (gap != null && gap.weight(arc) > weight) {
                weight = gap.weight(arc);
                best = gap;
            }
        }
        return best;
    }

    /**
     * The surface of ring 4's input across the line ({@code distance}: blocks beyond the line): the plateau inside, the
     * foot beyond; in a ravine a floor ramping from the plateau {@link #RAVINE_LENGTH} inside down to the foot at the
     * line, on a scree slope a ramp from {@link #SCREE_IN} inside to {@link #SCREE_OUT} beyond.
     */
    static double surface(double distance, double plateau, double foot, @Nullable Gap gap, double arc) {
        double wall = distance < 0.0 ? plateau : foot;
        if (gap == null) {
            return wall;
        }
        if (gap.ravine()) {
            // A notch cut into the plateau: never above the wall's own surface.
            double ramp = Mth.lerp(Mth.clamp((distance + RAVINE_LENGTH) / RAVINE_LENGTH, 0.0, 1.0), plateau, foot);
            return Mth.lerp(gap.weight(arc), wall, Math.min(wall, ramp));
        }
        // A slope of rubble: below the plateau inside the line, above the foot beyond it.
        double ramp = Mth.lerp(Mth.clamp((distance + SCREE_IN) / (SCREE_IN + SCREE_OUT), 0.0, 1.0), plateau, foot);
        return Mth.lerp(gap.weight(arc), wall, ramp);
    }

    /** The fall of fall cell {@code k}: its position along the line, or NaN without one. */
    static double fallArc(long salt, int k) {
        long h = CellShapes.cell(salt, k, 0, FALL_SALT);
        if (CellShapes.unit(h, 0) >= FALL_CHANCE) {
            return Double.NaN;
        }
        return (k + 0.15 + 0.7 * CellShapes.unit(h, 1)) * FALL_CELL;
    }
}
