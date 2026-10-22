package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The inner band of ring 5 (package 83, V4-A): dead ground around Y 78 with low hills (Y 70–95), and per region the shapes
 * that are "just wrong" (the region axes and thresholds of W-biomes, {@code golf-1-biomes.md} §2):
 * <ul>
 *     <li>{@code corrupted_wastes}: a plain with too regular stairs (treads exactly {@link #STEP} high, straight edges on
 *     the block grid) and perfectly round, shallow pits;</li>
 *     <li>{@code false_meadow}: a smooth meadow (Y 73–79) whose roll is a regular wave on two axes instead of noise;</li>
 *     <li>{@code glitch_fields}: blocky ground ({@link #QUANTUM} × {@link #QUANTUM} × {@link #QUANTUM}, the quantisation
 *     in Java: the inner band's ground is a Java height field, so no {@code quantize} density function type is needed),
 *     shifted layers and floating cubes.</li>
 * </ul>
 * The shapes blend over ±{@link #BLEND} of the region axes. Loose cracks (2–6 wide, 20–120 long, ~1 per 256 × 256) reach
 * down into the void. Pure: a function of the noises, the per-world salt and x, z.
 */
final class Ring5Inner {
    /** The dead ground: Y {@link #BASE} plus the hills' noise times {@link #HILLS}, held to Y 70–95 (V4-A). */
    static final double BASE = 78.0;
    static final double HILLS = 16.0;
    static final double MIN_TOP = 70.0;
    static final double MAX_TOP = 95.0;
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
    /** Pits: one per cell of 64 with this chance, perfectly round, Ø 8–20, flat floor 2–4 down. */
    static final int PIT_CELL = 64;
    static final double PIT_CHANCE = 0.35;
    static final int MIN_PIT_RADIUS = 4;
    static final int MAX_PIT_RADIUS = 10;
    static final int MIN_PIT_DEPTH = 2;
    static final int MAX_PIT_DEPTH = 4;
    /** The false meadow: Y 76 ± 3 as a regular wave of this length on both axes. */
    static final double MEADOW_Y = 76.0;
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

    Ring5Inner(DensityFunction.NoiseHolder hills, DensityFunction.NoiseHolder rot, DensityFunction.NoiseHolder glitch, long salt) {
        this.hills = hills;
        this.rot = rot;
        this.glitch = glitch;
        this.salt = salt;
    }

    /** The weight of {@code glitch_fields} (0..1). */
    double glitchWeight(double x, double z) {
        return Ring4Relief.smooth(glitch.getValue(x, 0.0, z), GLITCH_AT - BLEND, GLITCH_AT + BLEND);
    }

    /** The weight of {@code false_meadow} (0..1), outside the glitch fields. */
    double meadowWeight(double x, double z, double glitchWeight) {
        return (1.0 - glitchWeight) * Ring4Relief.smooth(rot.getValue(x, 0.0, z), MEADOW_AT - BLEND, MEADOW_AT + BLEND);
    }

    /** The dead ground with its hills, before the regions' shapes. */
    double base(double x, double z) {
        return Mth.clamp(BASE + HILLS * hills.getValue(x, 0.0, z), MIN_TOP, MAX_TOP);
    }

    /**
     * The inner band's surface (the Y above the highest block), the regions' shapes blended: without cracks, layers and
     * cubes.
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
        double top = base(x, z);
        double stair = stairAt(x, z);
        if (!Double.isNaN(stair)) {
            return stair;
        }
        double pit = pitAt(x, z);
        return Double.isNaN(pit) ? top : pit;
    }

    /** {@code false_meadow}: a regular wave on both axes. */
    double meadowTop(int x, int z) {
        double k = 2.0 * Math.PI / MEADOW_WAVE;
        return MEADOW_Y + MEADOW_AMPLITUDE * (Math.sin(k * (x + 0.5)) + Math.sin(k * (z + 0.5)));
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
        long h = CellShapes.cell(salt, ci, cj, STAIR_SALT);
        if (CellShapes.unit(h, 0) >= STAIR_CHANCE) {
            return Double.NaN;
        }
        int steps = MIN_STEPS + (int) Math.floor((MAX_STEPS - MIN_STEPS + 1) * CellShapes.unit(h, 1));
        int run = MIN_RUN + (int) Math.floor((MAX_RUN - MIN_RUN + 1) * CellShapes.unit(h, 2));
        int width = MIN_WIDTH + (int) Math.floor((MAX_WIDTH - MIN_WIDTH + 1) * CellShapes.unit(h, 3));
        int direction = (int) Math.floor(4 * CellShapes.unit(h, 4));
        int margin = MAX_STEPS * MAX_RUN + MAX_WIDTH;
        int ox = ci * STAIR_CELL + margin / 2 + (int) Math.floor((STAIR_CELL - margin) * CellShapes.unit(h, 5));
        int oz = cj * STAIR_CELL + margin / 2 + (int) Math.floor((STAIR_CELL - margin) * CellShapes.unit(h, 6));
        int dx = x - ox;
        int dz = z - oz;
        // Along (u ≥ 0 up the stairs) and across (v) in one of the four directions: straight edges on the grid.
        int u = switch (direction) {
            case 0 -> dx;
            case 1 -> -dx;
            case 2 -> dz;
            default -> -dz;
        };
        int v = direction < 2 ? dz : dx;
        if (u < 0 || u >= steps * run || v < -width / 2 || v >= width - width / 2) {
            return Double.NaN;
        }
        int step = u / run;
        return Math.round(base(ox, oz)) + (double) STEP * (step + 1);
    }

    /** A pit's flat floor at the column (perfectly round), or NaN. */
    double pitAt(int x, int z) {
        int ci = Math.floorDiv(x, PIT_CELL);
        int cj = Math.floorDiv(z, PIT_CELL);
        long h = CellShapes.cell(salt, ci, cj, PIT_SALT);
        if (CellShapes.unit(h, 0) >= PIT_CHANCE) {
            return Double.NaN;
        }
        int radius = MIN_PIT_RADIUS + (int) Math.floor((MAX_PIT_RADIUS - MIN_PIT_RADIUS + 1) * CellShapes.unit(h, 1));
        int depth = MIN_PIT_DEPTH + (int) Math.floor((MAX_PIT_DEPTH - MIN_PIT_DEPTH + 1) * CellShapes.unit(h, 2));
        int cx = ci * PIT_CELL + radius + (int) Math.floor((PIT_CELL - 2 * radius) * CellShapes.unit(h, 3));
        int cz = cj * PIT_CELL + radius + (int) Math.floor((PIT_CELL - 2 * radius) * CellShapes.unit(h, 4));
        double d = Math.hypot(x + 0.5 - (cx + 0.5), z + 0.5 - (cz + 0.5));
        return d <= radius ? Math.round(base(cx, cz)) - depth : Double.NaN;
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
}
