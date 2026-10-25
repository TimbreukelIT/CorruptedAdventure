package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The relief of the ring 3 plain (change 39d): highlands and lowlands, and one landform per biome. Part of
 * {@code volcanic_field} (its {@code relief} object); {@link VolcanicField} composes the column from these pieces.
 * <p>
 * Two kinds of pieces:
 * <ul>
 *   <li>smooth ones ({@link #highlandOffset}, {@link #peakRidges}, {@link #reefRidges}): part of the plain the volcanoes
 *   stand on, so ridges run up a cone's flank;</li>
 *   <li>detail ({@link #dunes}, {@link #terraces}, {@link #plateaus}, {@link #lavaTube}, {@link #rift}): cliffs, steps and
 *   cuts, faded out on volcano flanks.</li>
 * </ul>
 * Every landform is weighted by its biome's inner weight ({@link VolcanicField.Regions}), which is 0 on the biome border
 * and 1 a little inside, so neighbouring landforms never meet in a wall.
 * <p>
 * Pure: functions of the noises and x, z. Several pieces read the same noise at their own scale and offset (an
 * independent pattern for the price of one sample).
 *
 * @param highlands broad highlands and lowlands (~1–1.5 km)
 * @param shapes    the mid-scale noise of the landforms (dunes, terraces, plateaus, tubes, ridges)
 * @param detail    small variations (crest segments, tube collapses)
 * @param rifts     the rift valleys (stretched along their axis) and where they occur
 */
public record Ring3Relief(DensityFunction.NoiseHolder highlands, DensityFunction.NoiseHolder shapes,
                          DensityFunction.NoiseHolder detail, DensityFunction.NoiseHolder rifts) {
    public static final MapCodec<Ring3Relief> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.NoiseHolder.CODEC.fieldOf("highlands").forGetter(Ring3Relief::highlands),
            DensityFunction.NoiseHolder.CODEC.fieldOf("shapes").forGetter(Ring3Relief::shapes),
            DensityFunction.NoiseHolder.CODEC.fieldOf("detail").forGetter(Ring3Relief::detail),
            DensityFunction.NoiseHolder.CODEC.fieldOf("rifts").forGetter(Ring3Relief::rifts)
    ).apply(instance, Ring3Relief::new));

    /** Offset of the highlands above {@code plain_y}: −15 in the lowlands to +45 on the highlands. */
    static final double HIGHLAND_LOW = -15.0;
    static final double HIGHLAND_HIGH = 45.0;
    /** The highland offset towards which the relief fades at the ring's edges (the mean of ring 2's plain there). */
    static final double EDGE_OFFSET = 8.0;

    // Micro relief (G2): the wobble's and the height ripple's amplitude (blocks), and the ripple's period in height.
    static final double MICRO_BUMP = 0.9;
    static final double MICRO_RIPPLE = 0.6;
    static final double MICRO_RIPPLE_PERIOD = 4.0;

    // Dunes (ashen_wastes): crests across a fixed wind direction, a gentle windward side and a steep lee.
    private static final double WIND = 0.35;
    private static final double WIND_COS = Math.cos(WIND);
    private static final double WIND_SIN = Math.sin(WIND);
    private static final double DUNE_SPACING = 120.0;
    private static final double DUNE_CREST = 0.78;
    private static final double GULLY_WIDTH = 0.08;

    // Terraces (sulfur_fields): 3-block steps with a raised lip on each pool's lower edge.
    static final double TERRACE_STEP = 3.0;
    private static final int TERRACE_STEPS = 6;

    // Plateaus (basalt_flows): three treads with columnar risers.
    static final double PLATEAU_STEP = 12.0;
    static final int PLATEAU_STEPS = 3;
    static final double PLATEAU_SHARPNESS = 0.18;

    // Lava tubes (basalt_flows): a narrow sinuous band, collapsed into a trench in places, a tunnel elsewhere.
    private static final double TUBE_WIDTH = 0.045;

    // Rift valleys: a stretched ridged band, turned away from the axes.
    private static final double RIFT_ANGLE = 2.1;
    private static final double RIFT_COS = Math.cos(RIFT_ANGLE);
    private static final double RIFT_SIN = Math.sin(RIFT_ANGLE);
    private static final double RIFT_WIDTH = 0.07;
    /** The direction across the rift valleys (they run along {@code (RIFT_COS, RIFT_SIN)}). */
    static final double RIFT_NORMAL_X = -RIFT_SIN;
    static final double RIFT_NORMAL_Z = RIFT_COS;

    Ring3Relief map(DensityFunction.Visitor visitor) {
        return new Ring3Relief(visitor.visitNoise(highlands), visitor.visitNoise(shapes), visitor.visitNoise(detail),
                visitor.visitNoise(rifts));
    }

    private static double sample(DensityFunction.NoiseHolder noise, double x, double z) {
        return noise.getValue(x, 0.0, z);
    }

    // ---------------------------------------------------------------- smooth pieces

    /** Highland offset above the plain, {@link #HIGHLAND_LOW}..{@link #HIGHLAND_HIGH}, through a smooth curve. */
    double highlandOffset(double x, double z) {
        double n = sample(highlands, x, z);
        return HIGHLAND_LOW + (HIGHLAND_HIGH - HIGHLAND_LOW) * VolcanicMath.smoothstep(-0.55, 0.6, n);
    }

    /** 0..1: sharp crests of a ridge network (arêtes between the cones of {@code volcanic_peaks}). */
    double peakRidges(double x, double z) {
        double n = sample(shapes, x * 0.55 + 311.3, z * 0.55 - 127.9);
        double crest = 1.0 - Math.abs(n) * 1.6;
        return crest <= 0.0 ? 0.0 : crest * crest * crest;
    }

    /**
     * The smooth micro relief of the plain (G2), in blocks, within ±({@link #MICRO_BUMP} + {@link #MICRO_RIPPLE}): a
     * fine wobble (~16–32 blocks) that bends the step lines, plus a ripple tied to the height itself ({@code base}, the
     * ground without the micro relief; one period per {@link #MICRO_RIPPLE_PERIOD} blocks of height, its phase drifting
     * across the plain), which bunches neighbouring step lines into a two-block step in one place and spreads them over a
     * wide tread in another, so a slope shows no evenly spaced parallel lines. The wobble stays below a block (the old
     * ±2 wobble drew three or four nested rings round every bump, a contour map); the ripple is too weak to turn a slope
     * around through its height term ({@code MICRO_RIPPLE · 2π / MICRO_RIPPLE_PERIOD < 1}). Its drifting phase is a
     * free 2D term of up to ±{@link #MICRO_RIPPLE} on a flat tread, so tiny hollows of a block can occur there; the
     * total stays within ±1.5, below the old ±2.
     */
    double microRelief(double x, double z, double base) {
        double bump = Math.max(-1.0, Math.min(1.0, 1.6 * sample(detail, x * 1.3 + 71.1, z * 1.3 - 19.7)));
        double phase = Math.max(-1.0, Math.min(1.0, 1.5 * sample(detail, x * 0.9 - 2711.3, z * 0.9 + 1301.7)));
        return MICRO_BUMP * bump + MICRO_RIPPLE * Math.sin(2.0 * Math.PI * base / MICRO_RIPPLE_PERIOD + Math.PI * phase);
    }

    /**
     * The threshold (G2) on a column's height fraction above which {@code ThinShapes#microSteps} raises it by a block,
     * about −0.45..1.45: a clumpy pattern (~16 blocks, with a ragged edge of ~4–8) drawn anew for every block of height
     * ({@code level} shifts the sample), so each step line frays on its own instead of in step with its neighbours. Below
     * 0 a whole tread rises (the line merges with the one below into a two-block step), above 1 none does (the line stays
     * where the smooth relief put it).
     */
    double microSteps(double x, double z, int level) {
        double o = level * 97.31;
        double smooth = Math.max(-1.0, Math.min(1.0, 1.4 * sample(detail, x * 2.0 - 517.3 + o, z * 2.0 + 231.9 - 0.61 * o)));
        double rough = Math.max(-1.0, Math.min(1.0, 1.4 * sample(detail, x * 4.0 + 1517.3 - 0.37 * o, z * 4.0 - 731.9 + o)));
        return 0.5 - 0.65 * smooth - 0.3 * rough;
    }

    /** A slowly varying phase (39f) for the per-block patterns of the ground that follow the steps of the relief. */
    double treadPhase(double x, double z) {
        return sample(detail, x * 0.45 + 1907.7, z * 0.45 - 3301.1);
    }

    /** A fast varying phase (39f) for the winding veins through the obsidian needles. */
    double veinPhase(double x, double z) {
        return sample(detail, x * 5.0 - 77.7, z * 5.0 + 4410.3);
    }

    /** 0..1: low, long ridges of old reefs on the seabed floor. */
    double reefRidges(double x, double z) {
        double n = sample(shapes, x * 0.8 - 911.1, z * 0.8 + 433.3);
        double crest = 1.0 - Math.abs(n) / 0.12;
        return crest <= 0.0 ? 0.0 : VolcanicMath.smoothstep(0.0, 1.0, crest);
    }

    // ---------------------------------------------------------------- detail (faded on volcano flanks)

    /**
     * Ash dunes with eroded gullies (ashen_wastes): writes the height change (blocks) into {@code out.height}, the gully
     * floor share (0..1, 1 = flat floor) into {@code out.floor} and the wall share into {@code out.cliff}.
     */
    void dunes(double x, double z, Detail out) {
        double u = x * WIND_COS + z * WIND_SIN;
        double v = -x * WIND_SIN + z * WIND_COS;
        double bend = sample(shapes, x * 0.6 + 1000.5, z * 0.6);
        // Size varies slowly across the wind and faster along a crest, so crests break up into segments.
        double size = sample(shapes, u * 0.3 + 300.5, v * 0.9 - 90.5);
        double phase = (u + 40.0 * bend) / DUNE_SPACING;
        double d = phase - Math.floor(phase);
        double profile = d < DUNE_CREST ? Math.pow(d / DUNE_CREST, 1.6) : Math.pow((1.0 - d) / (1.0 - DUNE_CREST), 0.8);
        double height = (10.0 + 15.0 * VolcanicMath.smoothstep(-0.4, 0.4, size)) * profile;
        double gully = 1.0 - Math.abs(sample(shapes, x * 0.45 - 2000.5, z * 0.45 + 777.7)) / GULLY_WIDTH;
        out.reset();
        if (gully > 0.0) {
            double t = Math.min(1.0, gully / 0.5);
            double wall = VolcanicMath.smoothstep(0.0, 1.0, t);
            double depth = 8.0 + 5.0 * (0.5 + 0.5 * size);
            height = height * (1.0 - wall) - depth * wall;
            out.floor = VolcanicMath.smoothstep(0.8, 1.0, t);
            out.cliff = 1.0 - Math.abs(2.0 * t - 1.0);
        }
        out.height = height;
    }

    /**
     * Pamukkale-like terraces (sulfur_fields): {@link #TERRACE_STEPS} steps of {@link #TERRACE_STEP} blocks with a lip on
     * the lower edge of each pool, centred on 0.
     */
    double terraces(double x, double z) {
        double t = Math.max(0.0, Math.min(1.0, 0.5 + 0.8 * sample(shapes, x * 1.4 + 512.5, z * 1.4 - 64.5)));
        double s = t * TERRACE_STEPS;
        double k = Math.floor(s);
        double f = s - k;
        double riser = VolcanicMath.smoothstep(0.82, 1.0, f);
        double lip = k > 0.0 ? 1.2 * (1.0 - VolcanicMath.smoothstep(0.0, 0.2, f)) : 0.0;
        return TERRACE_STEP * (k + riser) + lip - TERRACE_STEP * TERRACE_STEPS * 0.5;
    }

    /**
     * Stepped lava plateaus (basalt_flows): the tread level (0..{@link #PLATEAU_STEPS}, fractional on a riser) at a
     * position; times {@link #PLATEAU_STEP} it is the height above the lowest tread.
     */
    double plateauLevel(double x, double z) {
        double t = Math.max(0.0, Math.min(1.0, (sample(shapes, x * 0.5 - 3000.5, z * 0.5 + 1200.5) + 0.25) / 0.7));
        return VolcanicMath.stair(t, PLATEAU_STEPS, PLATEAU_SHARPNESS) * PLATEAU_STEPS;
    }

    /** The signed value whose zero line is the axis of the lava tubes (42: to find a trench by its sign change). */
    double lavaTubeAxis(double x, double z) {
        return sample(shapes, x * 0.7 + 4100.5, z * 0.7 - 2500.5);
    }

    /**
     * A lava tube (basalt_flows): 0 outside, else the share across it (1 on its axis); {@code out.floor} = 1 where it has
     * collapsed into a trench, 0 where it is a tunnel.
     */
    double lavaTube(double x, double z, Detail out) {
        double where = sample(shapes, x * 0.2 + 77.7, z * 0.2 - 1500.5);
        if (where < 0.05) {
            return 0.0;
        }
        double across = 1.0 - Math.abs(lavaTubeAxis(x, z)) / TUBE_WIDTH;
        if (across <= 0.0) {
            return 0.0;
        }
        out.floor = VolcanicMath.smoothstep(0.1, 0.25, sample(detail, x * 0.5 + 33.3, z * 0.5 - 21.1));
        return across * VolcanicMath.smoothstep(0.05, 0.2, where);
    }

    /**
     * A rift valley: the depth of the cut (blocks, 0 outside), with stepped flanks and a flat floor; {@code out.cliff} =
     * the flank share, {@code out.floor} = how strongly rifts occur here (0..1; their full depth is
     * {@code (20 + 20 · floor) · floor}). {@code warp} bends the axis (blocks of offset across it).
     */
    double rift(double x, double z, double warp, Detail out) {
        out.reset();
        double where = VolcanicMath.smoothstep(0.1, 0.3, sample(rifts, x * 0.35 + 2222.2, z * 0.35 - 3333.3));
        if (where <= 0.0) {
            return 0.0;
        }
        out.floor = where;
        double along = x * RIFT_COS + z * RIFT_SIN;
        double across = -x * RIFT_SIN + z * RIFT_COS + warp;
        double band = 1.0 - Math.abs(sample(rifts, across * 2.0, along * 0.4)) / RIFT_WIDTH;
        if (band <= 0.0) {
            return 0.0;
        }
        double t = Math.min(1.0, band / 0.6);
        double s = t * 3.0;
        double f = s - Math.floor(s);
        out.cliff = t < 1.0 ? Math.max(0.0, 1.0 - Math.abs(f - 0.5) / 0.2) : 0.0;
        return (20.0 + 20.0 * where) * where * VolcanicMath.stair(t, 3, 0.3);
    }

    /** Scratch result of a detail piece (one per thread, through the column being filled). */
    static final class Detail {
        double height;
        double floor;
        double cliff;

        void reset() {
            height = 0.0;
            floor = 0.0;
            cliff = 0.0;
        }
    }
}
