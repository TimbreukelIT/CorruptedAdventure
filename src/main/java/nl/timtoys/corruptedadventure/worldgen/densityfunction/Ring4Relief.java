package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The base terrain of overworld ring 4 (package 60): low around the sea level, about half water, with one landform per
 * kind of region. Part of {@code island_field} (its {@code relief} object); {@link IslandField} adds the islands' holes
 * on top.
 * <p>
 * The kinds of region come from the router's climate axes, read here at exact positions (the same noises as
 * {@code ring_4/overworld/region_water} and {@code region_growth}, never through their flat caches), and the band
 * ({@code Ring4Band}). The biomes (W-biomes, owner of the thresholds) are placed on the same axes with the same
 * thresholds, so each landform lies under its biome; the landforms blend over {@link #BLEND} around each threshold.
 * Water axis high = open water, growth axis high = grown. Mythical band:
 * <ul>
 *   <li>bare ({@code growth < }{@link #MYTHIC_GROWTH}): {@code crystal_lagoon} where the water axis is ≥ 0 (sand banks
 *   and deep troughs), {@code mist_fjords} below (a plateau cut by fjords);</li>
 *   <li>grown: {@code lotus_shallows} from {@link #WATER_SPLIT} (shallow flats), {@code glowroot_swamp} between
 *   ±{@link #WATER_SPLIT} (around the sea level), {@code cascade_vale} below (terraces).</li>
 * </ul>
 * The mythical half of the band's transition is always {@code mist_fjords} (a narrow misty rim): there the terrain
 * turns into a low fjord plateau whatever the climate, never a lagoon. Blighted band: {@code drowned_forest} from
 * {@link #WATER_SPLIT} (deep dark water); below it {@code reed_marsh} where it grows, {@code peat_moors} (flat, with
 * sinkholes) where it is bare. Rivers run at the sea level through the land, their beds below it, so they hold sea
 * water and connect the lakes, the lagoon and the fjords.
 * <p>
 * Pure: functions of the noises and x, z. The landform pieces only sample their noise where they have weight.
 *
 * @param water  the water axis (open water ↔ land), the noise of {@code region_water}
 * @param growth the growth axis (grown ↔ bare rock), the noise of {@code region_growth}
 * @param hills  rolling hills and the height of the land (~300 blocks)
 * @param detail small bumps (~40 blocks)
 * @param shoals the lagoon's sand banks and troughs, the lotus flats
 * @param rivers the river network (a ridged line: rivers where it is near 0)
 * @param fjords the fjords (likewise, wider)
 * @param warp   bends the rivers and fjords
 */
public record Ring4Relief(DensityFunction.NoiseHolder water, DensityFunction.NoiseHolder growth,
                          DensityFunction.NoiseHolder hills, DensityFunction.NoiseHolder detail,
                          DensityFunction.NoiseHolder shoals, DensityFunction.NoiseHolder rivers,
                          DensityFunction.NoiseHolder fjords, DensityFunction.NoiseHolder warp) {
    public static final MapCodec<Ring4Relief> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.NoiseHolder.CODEC.fieldOf("water").forGetter(Ring4Relief::water),
            DensityFunction.NoiseHolder.CODEC.fieldOf("growth").forGetter(Ring4Relief::growth),
            DensityFunction.NoiseHolder.CODEC.fieldOf("hills").forGetter(Ring4Relief::hills),
            DensityFunction.NoiseHolder.CODEC.fieldOf("detail").forGetter(Ring4Relief::detail),
            DensityFunction.NoiseHolder.CODEC.fieldOf("shoals").forGetter(Ring4Relief::shoals),
            DensityFunction.NoiseHolder.CODEC.fieldOf("rivers").forGetter(Ring4Relief::rivers),
            DensityFunction.NoiseHolder.CODEC.fieldOf("fjords").forGetter(Ring4Relief::fjords),
            DensityFunction.NoiseHolder.CODEC.fieldOf("warp").forGetter(Ring4Relief::warp)
    ).apply(instance, Ring4Relief::new));

    /** The overworld's sea level. */
    static final double SEA = 63.0;

    // The biomes' thresholds (W-biomes, preset rings.ring_4): water axis ±WATER_SPLIT and 0, growth MYTHIC_GROWTH
    // (mythical) and 0 (blighted).
    static final double WATER_SPLIT = 0.125;
    static final double MYTHIC_GROWTH = -0.075;
    static final double BLIGHTED_GROWTH = 0.0;
    /** Half width (axis units, ~25 blocks) over which two landforms blend around a threshold. */
    static final double BLEND = 0.045;

    // Lagoon: troughs at the bottom, sand banks breaking the surface.
    static final double TROUGH_Y = 42.0;
    private static final double LAGOON_RISE = 21.5;
    private static final double LOTUS_Y = 59.5;
    // Cascades: treads of TERRACE_STEP blocks, the riser in the last fifth of each step (wide enough for the 4-block grid).
    static final double TERRACE_STEP = 8.0;
    static final double TERRACE_TREAD = 0.8;
    private static final double CASCADE_RISE = 45.0;
    // Fjords: a plateau, the channels' floor far below the sea.
    private static final double FJORD_PLATEAU = 8.0;
    private static final double FJORD_RISE = 32.0;
    static final double FJORD_FLOOR = 40.0;
    /** Half width of a fjord's floor and how steep its walls rise (blocks up per block across). */
    static final double FJORD_HALF = 12.0;
    static final double FJORD_SLOPE = 2.0;
    // Rivers: a bed below the sea level, valley sides rising to the land.
    static final double RIVER_BED = 57.0;
    static final double RIVER_HALF = 5.5;
    static final double RIVER_SLOPE = 0.6;
    /**
     * How fast the river and fjord lines grow per block across them (the median, measured on the test seed): turns
     * their noise into a distance in blocks.
     */
    static final double RIVER_GRADIENT = 0.0018;
    static final double FJORD_GRADIENT = 0.0018;
    private static final double WARP_BLOCKS = 40.0;
    private static final double WARP_OFFSET = 3131.5;
    // Blighted band.
    private static final double DROWNED_Y = 55.0;
    private static final double PEAT_Y = 65.5;
    /** Peat sinkholes: one cell of this size holds at most one. */
    static final int SINKHOLE_CELL = 40;
    private static final double SINKHOLE_CHANCE = 0.5;
    static final double SINKHOLE_MAX_RADIUS = 7.0;
    static final double SINKHOLE_MAX_DEPTH = 16.0;
    private static final long SINKHOLE_SALT = 4401;

    Ring4Relief map(DensityFunction.Visitor visitor) {
        return new Ring4Relief(visitor.visitNoise(water), visitor.visitNoise(growth), visitor.visitNoise(hills),
                visitor.visitNoise(detail), visitor.visitNoise(shoals), visitor.visitNoise(rivers), visitor.visitNoise(fjords),
                visitor.visitNoise(warp));
    }

    private static double sample(DensityFunction.NoiseHolder noise, double x, double z) {
        return noise.getValue(x, 0.0, z);
    }

    /** 0 below {@code from}, 1 above {@code to}, smoothstep between. */
    static double smooth(double v, double from, double to) {
        double t = Mth.clamp((v - from) / (to - from), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * 3D roughness of an exact wall (−1..1 about, 5a): strata (stretched flat, ~3 blocks high, ~9 across) for ledges and
     * overhangs, and knobs (~5 blocks) for bulges and clefts. From the detail noise at other scales.
     */
    double rough(double x, double y, double z) {
        return 0.6 * detail.getValue(x * 3.5, y * 10.0, z * 3.5) + 0.4 * detail.getValue(x * 6.0 + 917.0, y * 6.0, z * 6.0 - 431.0);
    }

    /** The warp noise at a position (−1..1 about, ~128 blocks at scale 1), for bending grids. */
    double warpAt(double x, double z) {
        return sample(warp, x, z);
    }

    /** 2D noise (−1..1 about, ~6 blocks) for where a wall's top edge crumbles (5a). */
    double crumble(double x, double z) {
        return detail.getValue(x * 5.0 - 211.0, 0.0, z * 5.0 + 377.0);
    }

    /** The detail noise at a position (−1..1 about), for small wanderings of lines. */
    double detailAt(double x, double z) {
        return sample(detail, x, z);
    }

    /** The water axis at a position (as the router reads it, without its cache). */
    double waterAxis(double x, double z) {
        return sample(water, x, z);
    }

    /** The growth axis at a position. */
    double growthAxis(double x, double z) {
        return sample(growth, x, z);
    }

    /** Fills {@code s} with the base terrain of the column at (x, z) ({@code band}: the band axis there, −1..1). */
    void sample(double x, double z, double band, Sample s, long salt) {
        double w = sample(water, x, z);
        double g = sample(growth, x, z);
        // Mythical weights (sum 1), the rim of the band's transition pulling them towards the fjords.
        double grown = split(g, MYTHIC_GROWTH);
        double rim = smooth(band, -1.0, -0.5);
        double lagoonW = (1.0 - grown) * split(w, 0.0) * (1.0 - rim);
        double lotusW = grown * split(w, WATER_SPLIT) * (1.0 - rim);
        double cascadeW = grown * (1.0 - split(w, -WATER_SPLIT)) * (1.0 - rim);
        double swampW = grown * (split(w, -WATER_SPLIT) - split(w, WATER_SPLIT)) * (1.0 - rim);
        double fjordW = 1.0 - lagoonW - lotusW - cascadeW - swampW;
        // Blighted weights.
        double drownedW = split(w, WATER_SPLIT);
        double reedW = (1.0 - drownedW) * split(g, BLIGHTED_GROWTH);
        double peatW = (1.0 - drownedW) * (1.0 - split(g, BLIGHTED_GROWTH));
        double blighted = split(band, 0.0, 0.3);

        double d = sample(detail, x, z);
        double hillsV = sample(hills, x, z);
        double hill01 = smooth(hillsV, -0.45, 0.55);
        double open = Mth.lerp(blighted, lagoonW + lotusW, drownedW);
        double shoal = open > 0.0 ? smooth(sample(shoals, x, z), -0.5, 0.5) : 0.0;

        // The warped position of the rivers and fjords, once per column (review 60 #6).
        double wx = Double.NaN;
        double wz = Double.NaN;
        if (open < 1.0 || fjordW > 0.0 && blighted < 1.0) {
            wx = warpX(x, z);
            wz = warpZ(x, z);
        }
        double mythicH = 0.0;
        double fjord = 0.0;
        if (blighted < 1.0) {
            if (lagoonW > 0.0) {
                mythicH += lagoonW * (TROUGH_Y + LAGOON_RISE * Math.sqrt(shoal) + 1.2 * d);
            }
            if (lotusW > 0.0) {
                mythicH += lotusW * (LOTUS_Y + 2.0 * shoal + 1.0 * d);
            }
            if (swampW > 0.0) {
                mythicH += swampW * swamp(hillsV, d);
            }
            if (cascadeW > 0.0) {
                double inland = smooth(-w, WATER_SPLIT, WATER_SPLIT + 0.33);
                mythicH += cascadeW * (SEA + 1.0 + terrace((5.0 + CASCADE_RISE * hill01) * inland + 1.5 * d));
            }
            if (fjordW > 0.0) {
                double inland = smooth(-w, 0.0, 0.3) * (1.0 - rim);
                double plateau = SEA + FJORD_PLATEAU + FJORD_RISE * inland * (0.55 + 0.45 * hill01) + 2.0 * d;
                double distance = fjordLine(wx, wz) / FJORD_GRADIENT;
                // A floor FJORD_HALF wide each side, then steep walls up to the plateau (a fjord, not a slot).
                double cut = FJORD_FLOOR + Math.max(0.0, distance - FJORD_HALF) * FJORD_SLOPE;
                fjord = fjordW * (1.0 - smooth(distance, FJORD_HALF, FJORD_HALF + 4.0)) * (1.0 - blighted);
                mythicH += fjordW * Math.min(plateau, cut);
            }
        }
        double blightedH = 0.0;
        if (blighted > 0.0) {
            double drowned = DROWNED_Y + 2.5 * d + 2.0 * hillsV;
            double reed = SEA + 1.2 * hillsV + 0.8 * d;
            double peatY = PEAT_Y + 1.5 * hill01 + 0.6 * d;
            blightedH = drownedW * drowned + reedW * reed + peatW * peatY;
        }
        double h = Mth.lerp(blighted, mythicH, blightedH);
        double river = 0.0;
        if (open < 1.0) {
            // A valley (review 60 #1): a bed RIVER_HALF wide each side, then slopes of RIVER_SLOPE up to the land, so
            // the valley widens with the height of the land around it.
            double distance = riverLine(wx, wz) / RIVER_GRADIENT;
            double cut = RIVER_BED + Math.max(0.0, distance - RIVER_HALF) * RIVER_SLOPE;
            h = Mth.lerp(1.0 - open, h, Math.min(h, cut));
            river = (1.0 - smooth(distance, RIVER_HALF, RIVER_HALF + 4.0)) * (1.0 - open);
        }
        double peat = peatW * blighted;
        s.height = h;
        s.water = w;
        s.growth = g;
        s.river = river;
        s.fjord = fjord;
        s.peat = peat;
        s.sinkholeFloor = peat > 0.0 ? sinkholeFloor(x, z, h, peat, salt) : Double.NaN;
    }

    /** 0 below the threshold, 1 above it, blended over ±{@link #BLEND}. */
    static double split(double v, double threshold) {
        return split(v, threshold, BLEND);
    }

    static double split(double v, double threshold, double blend) {
        return smooth(v, threshold - blend, threshold + blend);
    }

    private static double swamp(double hillsV, double d) {
        return SEA + 0.3 + 2.5 * hillsV + 1.5 * d;
    }

    /** Treads of {@link #TERRACE_STEP} blocks: flat for most of each step, a steep riser in its last part. */
    static double terrace(double rise) {
        double k = Math.max(0.0, rise) / TERRACE_STEP;
        double n = Math.floor(k);
        double f = k - n;
        double riser = Mth.clamp((f - TERRACE_TREAD) / (1.0 - TERRACE_TREAD), 0.0, 1.0);
        return TERRACE_STEP * (n + riser * riser * (3.0 - 2.0 * riser));
    }

    private double warpX(double x, double z) {
        return x + WARP_BLOCKS * sample(warp, x, z);
    }

    private double warpZ(double x, double z) {
        return z + WARP_BLOCKS * sample(warp, z + WARP_OFFSET, x - WARP_OFFSET);
    }

    /** The river line's value at a warped position: 0 on the line, rising about {@link #RIVER_GRADIENT} per block. */
    double riverLine(double wx, double wz) {
        return Math.abs(sample(rivers, wx, wz));
    }

    /** The fjord line's value at a warped position (axes swapped: another pattern from the same warp). */
    double fjordLine(double wx, double wz) {
        return Math.abs(sample(fjords, wz, wx));
    }

    /** The river line at a column (for tests). */
    double riverLineAt(double x, double z) {
        return riverLine(warpX(x, z), warpZ(x, z));
    }

    /** The fjord line at a column (for tests). */
    double fjordLineAt(double x, double z) {
        return fjordLine(warpX(x, z), warpZ(x, z));
    }

    /**
     * The floor of a peat sinkhole in this column (a bowl in its cell, cut {@code peat} deep of its full depth below
     * {@code height}), or NaN outside one. One cell of {@link #SINKHOLE_CELL} blocks holds at most one, kept inside the
     * cell. The terrain cuts it exactly per block (review 60 #4: the 4-block grid would smooth a small bowl away).
     */
    double sinkholeFloor(double x, double z, double height, double peat, long salt) {
        int i = Math.floorDiv(Mth.floor(x), SINKHOLE_CELL);
        int j = Math.floorDiv(Mth.floor(z), SINKHOLE_CELL);
        double[] hole = sinkhole(i, j, salt);
        if (hole == null) {
            return Double.NaN;
        }
        double dx = x - hole[0];
        double dz = z - hole[1];
        double q = (dx * dx + dz * dz) / (hole[2] * hole[2]);
        double depth = q >= 1.0 ? 0.0 : peat * hole[3] * Math.pow(1.0 - q, 0.6);
        return depth < 1.0 ? Double.NaN : height - depth;
    }

    /** The sinkhole of a cell as {x, z, radius, depth}, or null. Pure. */
    static double[] sinkhole(int i, int j, long salt) {
        long h = CellShapes.cell(salt, i, j, (int) SINKHOLE_SALT);
        if (CellShapes.unit(h, 0) >= SINKHOLE_CHANCE) {
            return null;
        }
        double radius = 3.0 + (SINKHOLE_MAX_RADIUS - 3.0) * CellShapes.unit(h, 1);
        double margin = radius + 1.0;
        double x = i * SINKHOLE_CELL + margin + CellShapes.unit(h, 2) * (SINKHOLE_CELL - 2.0 * margin);
        double z = j * SINKHOLE_CELL + margin + CellShapes.unit(h, 3) * (SINKHOLE_CELL - 2.0 * margin);
        double depth = 7.0 + (SINKHOLE_MAX_DEPTH - 7.0) * CellShapes.unit(h, 4);
        return new double[]{x, z, radius, depth};
    }

    /** The base terrain of one column. */
    static final class Sample {
        /** Surface height (blocks), without the islands' holes. */
        double height;
        double water;
        double growth;
        /** 0..1: a river's channel. */
        double river;
        /** 0..1: how much of the peat landform the column has (where its sinkholes cut). */
        double peat;
        /** The floor of a peat sinkhole in the column, NaN outside one (cut per block, not part of {@link #height}). */
        double sinkholeFloor;
        /** 0..1: a fjord's channel (weighted by the fjord landform). */
        double fjord;
    }
}
