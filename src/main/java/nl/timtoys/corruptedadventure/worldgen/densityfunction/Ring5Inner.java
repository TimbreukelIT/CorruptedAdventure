package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.DoubleBinaryOperator;

/**
 * The inner band of ring 5 (package 83, V4-A; relief revised in 83b at teststop 6a): rolling dead ground Y ~65–120 with
 * now and then a ridge or cliff up to ~140, growing over {@link #RELIEF_FADE} blocks after the zone 4→5 out of the old
 * low ground (Y 70–95 round 78) that the fracture zone blends with. On it, per region, the shapes that are "just wrong"
 * (the region axes and thresholds of W-biomes, {@code golf-1-biomes.md} §2; the stairs rise up to {@link #STEP} ×
 * {@link #MAX_STEPS} above their ground, a deliberate exception):
 * <ul>
 *     <li>{@code corrupted_wastes}: too regular stairs up the slope (treads exactly {@link #STEP} high, straight edges on
 *     the block grid) and perfectly round, shallow pits;</li>
 *     <li>{@code false_meadow}: the rolling ground without ridges, a regular wave on two axes over it;</li>
 *     <li>{@code glitch_fields}: blocky ground ({@link #QUANTUM} × {@link #QUANTUM} × {@link #QUANTUM}, the quantisation
 *     in Java: the inner band's ground is a Java height field, so no {@code quantize} density function type is needed),
 *     shifted layers and floating cubes.</li>
 * </ul>
 * The shapes blend over ±{@link #BLEND} of the region axes. Loose cracks (2–6 wide, 20–120 long, ~1 per 256 × 256) reach
 * down into the void; the corrupt spots (83b) are {@link Ring5CorruptSpots}. Pure: a function of the noises, the
 * per-world salt, the ring layout (the relief's weight) and x, z.
 */
final class Ring5Inner {
    /**
     * The rolling ground (83b): Y {@link #ROLL_BASE} plus the hills' noise times {@link #ROLL}, bent softly (over
     * {@link #KNEE} blocks) towards {@link #ROLL_LOW} and {@link #ROLL_HIGH} instead of a hard clamp.
     */
    static final double ROLL_BASE = 90.0;
    static final double ROLL = 48.0;
    static final double ROLL_LOW = 60.0;
    static final double ROLL_HIGH = 124.0;
    static final double KNEE = 8.0;
    /**
     * Smaller bumps on the dead ground (not on the false meadow, which stays too smooth): a fourth sample of the hills at
     * scale {@link #ROUGH_SCALE} (waves of ~130 and ~65 blocks) times {@link #ROUGH}.
     */
    static final double ROUGH = 24.0;
    static final double ROUGH_SCALE = 2.0;
    /**
     * The basins (walk 1, 2026-10-24): the soft bend makes every low stretch converge on {@link #ROLL_LOW}, so the low
     * basins were dead flat over 100–200 blocks (in {@code glitch_fields} exactly Y 60). On the floor the ground gets
     * back swells of up to {@link #FLOOR_SWELL} (a fifth sample of the hills at scale {@link #FLOOR_SCALE}, waves of
     * ~30–65 blocks, the noise's ±0.5 stretched to 0..1; only upwards, so the floor stays above {@link #ROLL_LOW}),
     * faded out over {@link #FLOOR_FADE} blocks above the floor. The same at the top: the summits bent towards {@link #ROLL_HIGH} were flat round Y 123, so
     * there the rolling ground (before the ridges) dips by up to {@link #FLOOR_SWELL} (another sample of the same scale).
     */
    static final double FLOOR_SWELL = 12.0;
    static final double FLOOR_SCALE = 4.0;
    static final double FLOOR_FADE = 20.0;
    /**
     * Ridges and cliffs (83b): along the zero line of a second sample of the hills (scale {@link #RIDGE_SCALE}) the ground
     * rises {@link #RIDGE_HEIGHT}, steeply on one side (over {@link #RIDGE_FACE} of the noise: a few blocks), gently on the
     * other ({@link #RIDGE_BACK}); only where a third, coarse sample (scale {@link #MASK_SCALE}) opens the mask
     * ({@link #MASK_FROM}..{@link #MASK_TO}). With them the ground bends softly towards {@link #CAP}.
     */
    static final double RIDGE_SCALE = 0.6;
    static final double RIDGE_HEIGHT = 26.0;
    static final double RIDGE_FACE = 0.012;
    static final double RIDGE_BACK = 0.12;
    static final double MASK_SCALE = 0.35;
    static final double MASK_FROM = 0.05;
    static final double MASK_TO = 0.25;
    static final double CAP = 146.0;
    /** The relief grows over this many blocks (effective distance) after the zone 4→5 out of the old ground. */
    static final double RELIEF_FADE = 160.0;
    /** The old dead ground (83), kept in the zone 4→5: Y 78 plus the hills' noise times 16, held to Y 70–95. */
    static final double ZONE_BASE = 78.0;
    static final double ZONE_HILLS = 16.0;
    static final double ZONE_MIN = 70.0;
    static final double ZONE_MAX = 95.0;
    /**
     * Towards the band line (over {@link #LINE_FADE} of {@code Ring5Band.signedDistance}, ~90 blocks across the line) the
     * ground bends softly into the outer band's floe tops, Y {@link #LINE_LOW}–{@link #LINE_HIGH} (84), so the outer band's
     * blend from the inner top starts inside its own range and stays unchanged.
     */
    static final double LINE_FADE = 160.0;
    static final double LINE_LOW = 70.0;
    static final double LINE_HIGH = 110.0;
    /** The region thresholds (W-biomes): glitch ≥ 0.125 glitch_fields, else rot ≥ 0.244 false_meadow; blended ±0.04. */
    static final double GLITCH_AT = 0.125;
    static final double MEADOW_AT = 0.244;
    static final double BLEND = 0.04;

    /** Stairs: one per cell of 128 with this chance, 3–6 treads exactly 4 high, 5–8 deep, 12–24 wide. */
    static final int STAIR_CELL = 128;
    static final double STAIR_CHANCE = 0.3;
    static final int STEP = 4;
    static final int MIN_STEPS = 3;
    static final int MAX_STEPS = 6;
    static final int MIN_RUN = 5;
    static final int MAX_RUN = 8;
    static final int MIN_WIDTH = 12;
    static final int MAX_WIDTH = 24;
    /** Pits: one per cell of 64 with this chance, perfectly round, Ø 8–20, flat floor 2–4 below the lowest rim. */
    static final int PIT_CELL = 64;
    static final double PIT_CHANCE = 0.35;
    static final int MIN_PIT_RADIUS = 4;
    static final int MAX_PIT_RADIUS = 10;
    static final int MIN_PIT_DEPTH = 2;
    static final int MAX_PIT_DEPTH = 4;
    /** The false meadow: the rolling ground ± 3, a regular wave of this length on both axes. */
    static final double MEADOW_AMPLITUDE = 1.5;
    static final double MEADOW_WAVE = 64.0;
    /** Glitch fields: blocks of this size; layers 4–6 high (one height per layer cell) shifted 2–6 sideways in the top {@link #LAYER_DEPTH}. */
    static final int QUANTUM = 4;
    static final int MIN_LAYER_HEIGHT = 4;
    static final int MAX_LAYER_HEIGHT = 6;
    static final int LAYER_CELL = 64;
    static final int MIN_SHIFT = 2;
    static final int MAX_SHIFT = 6;
    static final int LAYER_DEPTH = 16;
    /** Floating cubes: one per cell of 32 with this chance (times the glitch weight), 3–8 big, 6–30 above the ground. */
    static final int CUBE_CELL = 32;
    static final double CUBE_CHANCE = 0.25;
    static final int MIN_CUBE = 3;
    static final int MAX_CUBE = 8;
    static final int MIN_CUBE_LIFT = 6;
    static final int MAX_CUBE_LIFT = 30;
    /** Loose cracks: one per cell of 256 with this chance, 20–120 long, 2–6 wide, wavy; kept off the band line and the zone. */
    static final int CRACK_CELL = 256;
    static final double CRACK_CHANCE = 0.85;
    static final double MIN_CRACK_LENGTH = 20.0;
    static final double MAX_CRACK_LENGTH = 120.0;
    static final double MIN_CRACK_HALF = 1.0;
    static final double MAX_CRACK_HALF = 3.0;
    static final double CRACK_WAVE = 2.0;
    /** A crack's ends taper over this many blocks; it keeps this far from the band line and the zone 4→5. */
    static final double CRACK_TAPER = 8.0;
    static final double CRACK_CLEARANCE = 24.0;

    private static final int STAIR_SALT = 8301;
    private static final int PIT_SALT = 8302;
    private static final int LAYER_SALT = 8303;
    private static final int LAYER_HEIGHT_SALT = 8306;
    private static final int CUBE_SALT = 8304;
    private static final int CRACK_SALT = 8305;

    private final DensityFunction.NoiseHolder hills;
    private final DensityFunction.NoiseHolder rot;
    private final DensityFunction.NoiseHolder glitch;
    private final long salt;
    /** The relief's weight at a position: 0 up to the zone 4→5's outer edge, 1 from {@link #RELIEF_FADE} after it. */
    private final DoubleBinaryOperator relief;
    /** The weight of the full relief against the floe range at a position: 0 on the band line, 1 from {@link #LINE_FADE} inside. */
    private final DoubleBinaryOperator line;
    // Pure per-cell results: a staircase's or pit's ground takes a few samples of the relief.
    private final KeyedSlots<Optional<Stair>> stairs = new KeyedSlots<>(64);
    private final KeyedSlots<Optional<Pit>> pits = new KeyedSlots<>(256);

    Ring5Inner(DensityFunction.NoiseHolder hills, DensityFunction.NoiseHolder rot, DensityFunction.NoiseHolder glitch, long salt,
               DoubleBinaryOperator relief, DoubleBinaryOperator line) {
        this.hills = hills;
        this.rot = rot;
        this.glitch = glitch;
        this.salt = salt;
        this.relief = relief;
        this.line = line;
    }

    /** The hills' noise (the corrupt spots' ragged rims sample it too). */
    DensityFunction.NoiseHolder hills() {
        return hills;
    }

    /** The weight of {@code glitch_fields} (0..1). */
    double glitchWeight(double x, double z) {
        return Ring4Relief.smooth(glitch.getValue(x, 0.0, z), GLITCH_AT - BLEND, GLITCH_AT + BLEND);
    }

    /** The weight of {@code false_meadow} (0..1), outside the glitch fields. */
    double meadowWeight(double x, double z, double glitchWeight) {
        return (1.0 - glitchWeight) * Ring4Relief.smooth(rot.getValue(x, 0.0, z), MEADOW_AT - BLEND, MEADOW_AT + BLEND);
    }

    /** The dead ground with its relief (rolling ground, ridges and cliffs), before the regions' shapes. */
    double base(double x, double z) {
        return ground(x, z, true);
    }

    /** The rolling ground without ridges and cliffs (the false meadow's ground). */
    double rolling(double x, double z) {
        return ground(x, z, false);
    }

    private double ground(double x, double z, boolean ridges) {
        double h = hills.getValue(x, 0.0, z);
        double old = Mth.clamp(ZONE_BASE + ZONE_HILLS * h, ZONE_MIN, ZONE_MAX);
        double w = relief.applyAsDouble(x, z);
        if (w <= 0.0) {
            return old;
        }
        double ground;
        if (ridges) {
            double bumps = ROUGH * hills.getValue(x * ROUGH_SCALE + 900.5, 0.0, z * ROUGH_SCALE + 5200.5);
            double rolled = soft(ROLL_BASE + ROLL * h + bumps, ROLL_LOW, ROLL_HIGH);
            double summit = Ring4Relief.smooth(rolled, ROLL_HIGH - FLOOR_FADE, ROLL_HIGH);
            if (summit > 0.0) {
                double dip = Mth.clamp(0.5 + hills.getValue(x * FLOOR_SCALE + 6100.5, 0.0, z * FLOOR_SCALE - 3900.5), 0.0, 1.0);
                rolled -= summit * FLOOR_SWELL * dip;
            }
            ground = soft(rolled + ridge(x, z), ROLL_LOW, CAP);
            double floor = 1.0 - Ring4Relief.smooth(ground, ROLL_LOW, ROLL_LOW + FLOOR_FADE);
            if (floor > 0.0) {
                double swell = Mth.clamp(0.5 + hills.getValue(x * FLOOR_SCALE - 2600.5, 0.0, z * FLOOR_SCALE + 7300.5), 0.0, 1.0);
                ground += floor * FLOOR_SWELL * swell;
            }
        } else {
            ground = soft(ROLL_BASE + ROLL * h, ROLL_LOW, ROLL_HIGH);
        }
        double l = line.applyAsDouble(x, z);
        if (l < 1.0) {
            ground = Mth.lerp(l, soft(ground, LINE_LOW, LINE_HIGH), ground);
        }
        return w >= 1.0 ? ground : Mth.lerp(w, old, ground);
    }

    /** The lift of a ridge or cliff at a position: 0 where the mask is shut or no zero line is near. */
    double ridge(double x, double z) {
        double mask = Ring4Relief.smooth(hills.getValue(x * MASK_SCALE + 4400.5, 0.0, z * MASK_SCALE - 900.5), MASK_FROM, MASK_TO);
        if (mask <= 0.0) {
            return 0.0;
        }
        double n = hills.getValue(x * RIDGE_SCALE + 3100.5, 0.0, z * RIDGE_SCALE - 1700.5);
        double profile = n >= 0.0 ? 1.0 - Ring4Relief.smooth(n, 0.0, RIDGE_BACK) : Ring4Relief.smooth(n, -RIDGE_FACE, 0.0);
        return RIDGE_HEIGHT * mask * profile;
    }

    /** {@code v} bent softly (over {@link #KNEE} blocks) towards {@code low} and {@code high}, which it never reaches. */
    static double soft(double v, double low, double high) {
        double top = high - KNEE;
        if (v > top) {
            v = top + KNEE * Math.tanh((v - top) / KNEE);
        }
        double bottom = low + KNEE;
        if (v < bottom) {
            v = bottom - KNEE * Math.tanh((bottom - v) / KNEE);
        }
        return v;
    }

    /**
     * The inner band's surface (the Y above the highest block), the regions' shapes blended: without cracks, corrupt
     * spots, layers and cubes.
     */
    double top(int x, int z) {
        double g = glitchWeight(x, z);
        double m = meadowWeight(x, z, g);
        double w = 1.0 - g - m;
        double top = 0.0;
        if (w > 0.0) {
            top += w * wastesTop(x, z);
        }
        if (m > 0.0) {
            top += m * meadowTop(x, z);
        }
        if (g > 0.0) {
            top += g * glitchTop(x, z);
        }
        return top;
    }

    /** {@code corrupted_wastes}: the ground with its stairs and pits. */
    double wastesTop(int x, int z) {
        double stair = stairAt(x, z);
        if (!Double.isNaN(stair)) {
            return stair;
        }
        double pit = pitAt(x, z);
        return Double.isNaN(pit) ? base(x, z) : pit;
    }

    /** {@code false_meadow}: the rolling ground with a regular wave on both axes over it. */
    double meadowTop(int x, int z) {
        return rolling(x, z) + meadowWave(x, z);
    }

    /** The false meadow's regular wave (± 2 × {@link #MEADOW_AMPLITUDE}), the same every {@link #MEADOW_WAVE}. */
    static double meadowWave(int x, int z) {
        double k = 2.0 * Math.PI / MEADOW_WAVE;
        return MEADOW_AMPLITUDE * (Math.sin(k * (x + 0.5)) + Math.sin(k * (z + 0.5)));
    }

    /** {@code glitch_fields}: the ground sampled on a coarse grid and rounded to it (blocks of {@link #QUANTUM}). */
    double glitchTop(int x, int z) {
        int qx = Math.floorDiv(x, QUANTUM) * QUANTUM;
        int qz = Math.floorDiv(z, QUANTUM) * QUANTUM;
        return Math.floor(base(qx + QUANTUM / 2.0, qz + QUANTUM / 2.0) / QUANTUM) * QUANTUM;
    }

    /** A stair's tread top at the column (exactly {@link #STEP} apart), or NaN. */
    double stairAt(int x, int z) {
        int ci = Math.floorDiv(x, STAIR_CELL);
        int cj = Math.floorDiv(z, STAIR_CELL);
        Stair stair = stairs.get(KeyedSlots.key(ci, cj), key -> Optional.ofNullable(placeStair(ci, cj))).orElse(null);
        if (stair == null) {
            return Double.NaN;
        }
        int dx = x - stair.ox;
        int dz = z - stair.oz;
        // Along (u ≥ 0 up the stairs) and across (v) in one of the four directions: straight edges on the grid.
        int u = switch (stair.direction) {
            case 0 -> dx;
            case 1 -> -dx;
            case 2 -> dz;
            default -> -dz;
        };
        int v = stair.direction < 2 ? dz : dx;
        if (u < 0 || u >= stair.steps * stair.run || v < -stair.width / 2 || v >= stair.width - stair.width / 2) {
            return Double.NaN;
        }
        return stair.ground + (double) STEP * (u / stair.run + 1);
    }

    /** The staircase of stair cell (i, j): {origin x, origin z, length, direction (0 +x, 1 −x, 2 +z, 3 −z)}, or null. */
    @Nullable
    int[] stairShape(int ci, int cj) {
        Stair stair = stairs.get(KeyedSlots.key(ci, cj), key -> Optional.ofNullable(placeStair(ci, cj))).orElse(null);
        return stair == null ? null : new int[]{stair.ox, stair.oz, stair.steps * stair.run, stair.direction};
    }

    /** The staircase of stair cell (i, j), or null; it climbs the slope (of the four directions the one rising most). */
    @Nullable
    private Stair placeStair(int ci, int cj) {
        long h = CellShapes.cell(salt, ci, cj, STAIR_SALT);
        if (CellShapes.unit(h, 0) >= STAIR_CHANCE) {
            return null;
        }
        int steps = MIN_STEPS + (int) Math.floor((MAX_STEPS - MIN_STEPS + 1) * CellShapes.unit(h, 1));
        int run = MIN_RUN + (int) Math.floor((MAX_RUN - MIN_RUN + 1) * CellShapes.unit(h, 2));
        int width = MIN_WIDTH + (int) Math.floor((MAX_WIDTH - MIN_WIDTH + 1) * CellShapes.unit(h, 3));
        // The origin keeps the whole staircase inside its cell (MAX_STEPS × MAX_RUN along, half of it either side): the
        // column of a neighbouring cell then never needs this staircase, so no edge is cut.
        int reach = MAX_STEPS * MAX_RUN;
        int ox = ci * STAIR_CELL + reach + (int) Math.floor((STAIR_CELL - 2 * reach) * CellShapes.unit(h, 5));
        int oz = cj * STAIR_CELL + reach + (int) Math.floor((STAIR_CELL - 2 * reach) * CellShapes.unit(h, 6));
        int length = steps * run;
        double[] ends = {base(ox + length, oz), base(ox - length, oz), base(ox, oz + length), base(ox, oz - length)};
        int direction = 0;
        for (int d = 1; d < ends.length; d++) {
            if (ends[d] > ends[direction]) {
                direction = d;
            }
        }
        return new Stair(ox, oz, steps, run, width, direction, Math.round(base(ox, oz)));
    }

    /** A pit's flat floor at the column (perfectly round), or NaN. */
    double pitAt(int x, int z) {
        int ci = Math.floorDiv(x, PIT_CELL);
        int cj = Math.floorDiv(z, PIT_CELL);
        Pit pit = pits.get(KeyedSlots.key(ci, cj), key -> Optional.ofNullable(placePit(ci, cj))).orElse(null);
        if (pit == null) {
            return Double.NaN;
        }
        return Math.hypot(x - pit.cx, z - pit.cz) <= pit.radius ? pit.floor : Double.NaN;
    }

    /** The pit of pit cell (i, j), or null: its floor 2–4 below the lowest ground of its middle and four rim points. */
    @Nullable
    private Pit placePit(int ci, int cj) {
        long h = CellShapes.cell(salt, ci, cj, PIT_SALT);
        if (CellShapes.unit(h, 0) >= PIT_CHANCE) {
            return null;
        }
        int radius = MIN_PIT_RADIUS + (int) Math.floor((MAX_PIT_RADIUS - MIN_PIT_RADIUS + 1) * CellShapes.unit(h, 1));
        int depth = MIN_PIT_DEPTH + (int) Math.floor((MAX_PIT_DEPTH - MIN_PIT_DEPTH + 1) * CellShapes.unit(h, 2));
        int cx = ci * PIT_CELL + radius + (int) Math.floor((PIT_CELL - 2 * radius) * CellShapes.unit(h, 3));
        int cz = cj * PIT_CELL + radius + (int) Math.floor((PIT_CELL - 2 * radius) * CellShapes.unit(h, 4));
        double low = Math.min(base(cx, cz), Math.min(Math.min(base(cx + radius, cz), base(cx - radius, cz)),
                Math.min(base(cx, cz + radius), base(cx, cz - radius))));
        return new Pit(cx, cz, radius, Math.round(low) - depth);
    }

    /** The height of the glitch layers at the column: 4–6, one value per layer cell. */
    int layerHeight(int x, int z) {
        long h = CellShapes.cell(salt, Math.floorDiv(x, LAYER_CELL), Math.floorDiv(z, LAYER_CELL), LAYER_HEIGHT_SALT);
        return MIN_LAYER_HEIGHT + (int) Math.floor((MAX_LAYER_HEIGHT - MIN_LAYER_HEIGHT + 1) * CellShapes.unit(h, 0));
    }

    /** The sideways shift {dx, dz} of the glitch layer holding {@code y} at the column. */
    int[] layerShift(int x, int y, int z) {
        int layer = Math.floorDiv(y, layerHeight(x, z));
        long h = CellShapes.cell(salt, Math.floorDiv(x, LAYER_CELL) * 31L + layer, Math.floorDiv(z, LAYER_CELL), LAYER_SALT);
        int amount = MIN_SHIFT + (int) Math.floor((MAX_SHIFT - MIN_SHIFT + 1) * CellShapes.unit(h, 0));
        return switch ((int) Math.floor(4 * CellShapes.unit(h, 1))) {
            case 0 -> new int[]{amount, 0};
            case 1 -> new int[]{-amount, 0};
            case 2 -> new int[]{0, amount};
            default -> new int[]{0, -amount};
        };
    }

    /** The floating cube of cube cell (i, j): {x0, z0, size, bottom}, or null. */
    int[] cube(int i, int j) {
        long h = CellShapes.cell(salt, i, j, CUBE_SALT);
        int size = MIN_CUBE + (int) Math.floor((MAX_CUBE - MIN_CUBE + 1) * CellShapes.unit(h, 1));
        int x0 = i * CUBE_CELL + (int) Math.floor((CUBE_CELL - size) * CellShapes.unit(h, 2));
        int z0 = j * CUBE_CELL + (int) Math.floor((CUBE_CELL - size) * CellShapes.unit(h, 3));
        double weight = glitchWeight(x0 + size / 2.0, z0 + size / 2.0);
        if (CellShapes.unit(h, 0) >= CUBE_CHANCE * weight) {
            return null;
        }
        double ground = Double.NEGATIVE_INFINITY;
        for (int dx = 0; dx <= size; dx += Math.max(1, size / 2)) {
            for (int dz = 0; dz <= size; dz += Math.max(1, size / 2)) {
                ground = Math.max(ground, top(x0 + Math.min(dx, size - 1), z0 + Math.min(dz, size - 1)));
            }
        }
        int lift = MIN_CUBE_LIFT + (int) Math.floor((MAX_CUBE_LIFT - MIN_CUBE_LIFT + 1) * CellShapes.unit(h, 4));
        return new int[]{x0, z0, size, (int) Math.ceil(ground) + lift};
    }

    /** The loose crack of crack cell (i, j): {x, z, ux, uz, length, half width, wave phase}, before its validity check. */
    double[] crack(int i, int j) {
        long h = CellShapes.cell(salt, i, j, CRACK_SALT);
        if (CellShapes.unit(h, 0) >= CRACK_CHANCE) {
            return null;
        }
        double length = MIN_CRACK_LENGTH + (MAX_CRACK_LENGTH - MIN_CRACK_LENGTH) * CellShapes.unit(h, 1);
        double half = MIN_CRACK_HALF + (MAX_CRACK_HALF - MIN_CRACK_HALF) * CellShapes.unit(h, 2);
        double angle = Math.PI * CellShapes.unit(h, 3);
        double margin = MAX_CRACK_LENGTH / 2.0 + MAX_CRACK_HALF + CRACK_WAVE;
        double cx = i * CRACK_CELL + margin + (CRACK_CELL - 2.0 * margin) * CellShapes.unit(h, 4);
        double cz = j * CRACK_CELL + margin + (CRACK_CELL - 2.0 * margin) * CellShapes.unit(h, 5);
        double ux = Math.cos(angle);
        double uz = Math.sin(angle);
        return new double[]{cx - ux * length / 2.0, cz - uz * length / 2.0, ux, uz, length, half, 2.0 * Math.PI * CellShapes.unit(h, 6)};
    }

    /** How far (blocks) a column lies outside a crack's edge (≤ 0 inside it). */
    static double crackDistance(double[] crack, double x, double z) {
        double px = x - crack[0];
        double pz = z - crack[1];
        double along = px * crack[2] + pz * crack[3];
        double length = crack[4];
        double t = Mth.clamp(along, 0.0, length);
        double across = px * crack[3] - pz * crack[2] - CRACK_WAVE * Math.sin(t / 9.0 + crack[6]);
        double half = crack[5] * Math.sqrt(Mth.clamp(Math.min(t, length - t) / CRACK_TAPER, 0.0, 1.0));
        double beyond = along - t;
        return Math.sqrt(across * across + beyond * beyond) - half;
    }

    /** A staircase: origin, 3–6 treads {@code run} deep, {@code width} across, rising in {@code direction} from {@code ground}. */
    private record Stair(int ox, int oz, int steps, int run, int width, int direction, double ground) {
    }

    /** A round pit: its centre (as the distance is measured), radius and flat floor. */
    private record Pit(int cx, int cz, int radius, double floor) {
    }
}
