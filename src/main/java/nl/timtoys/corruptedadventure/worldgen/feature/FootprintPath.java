package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The pure core of {@code corruptedadventure:ash_footprints} (change 49): at most one trail of footprints per grid cell,
 * a function of the world seed, the cell and the pure terrain ({@link Rims}) only, so every chunk computes the same
 * trail and writes its own part of it. A trail starts inside its cell, wanders a little, and stops abruptly after 15–40
 * steps; it may reach up to {@link #MAX_LENGTH} blocks beyond its cell.
 */
final class FootprintPath {
    /** How a trail ends. */
    enum End {
        /** Nothing: the steps simply stop. */
        NOTHING,
        /** At a cast of ash, standing where the next step would be. */
        CAST,
        /** At the rim of a collapse or a crater, walking in; without a rim in the cell, nothing. */
        RIM
    }

    /** One footprint: the column, and whether it is a left foot (the feet alternate either side of the line). */
    record Step(int x, int z, boolean left) {
    }

    /** A trail: its steps, how it ends, and its heading at the end (radians). */
    record Trail(List<Step> steps, End end, double heading, double endX, double endZ) {
    }

    /** Where a trail heading for a rim stops: the last spot before the drop, and the heading into the hole there. */
    record Rim(double x, double z, double heading) {
    }

    /** The rim nearest to a point, from the pure terrain; null where there is none. */
    @FunctionalInterface
    interface Rims {
        Rims NONE = (x, z) -> null;

        @Nullable
        Rim near(double x, double z);
    }

    static final double STRIDE = 1.6;
    static final int MAX_STEPS = 40;
    static final int MIN_STEPS = 15;
    static final int MAX_LENGTH = (int) Math.ceil(MAX_STEPS * STRIDE) + 2;
    private static final long SALT = 0x7A3F1C2E9B5D4A61L;

    private FootprintPath() {
    }

    /** The trail of cell (i, j), or null: a cell has one with {@code chance}. */
    static Trail trail(long seed, int i, int j, int cell, float chance, Rims rims) {
        long h = mix(seed ^ SALT ^ mix(i * 0x9E3779B97F4A7C15L + j * 0xC2B2AE3D27D4EB4FL));
        if (unit(h, 0) >= chance) {
            return null;
        }
        double x = i * cell + unit(h, 1) * cell;
        double z = j * cell + unit(h, 2) * cell;
        double heading = unit(h, 3) * Math.PI * 2.0;
        int count = MIN_STEPS + (int) (unit(h, 4) * (MAX_STEPS - MIN_STEPS + 1));
        double turn = (unit(h, 5) - 0.5) * 0.12;
        End end = End.values()[(int) (unit(h, 6) * End.values().length)];
        if (end == End.RIM) {
            Rim rim = rims.near(x, z);
            if (rim != null && inCell(rim.x(), i, cell) && inCell(rim.z(), j, cell)) {
                // Walked backwards from the rim, away from the hole, then turned round: the trail ends at the rim.
                double[][] line = line(h, rim.x(), rim.z(), rim.heading() + Math.PI, turn, count);
                double[][] forward = new double[count][];
                for (int k = 0; k < count; k++) {
                    double[] back = line[count - 1 - k];
                    forward[k] = new double[]{back[0], back[1], back[2] + Math.PI};
                }
                return new Trail(feet(forward), end, rim.heading(), rim.x(), rim.z());
            }
            end = End.NOTHING;
        }
        double[][] line = line(h, x, z, heading, turn, count);
        double last = line[count - 1][2];
        double lastX = line[count - 1][0];
        double lastZ = line[count - 1][1];
        return new Trail(feet(line), end, last, lastX + Math.cos(last) * STRIDE * 1.5, lastZ + Math.sin(last) * STRIDE * 1.5);
    }

    /** {@code count} points of a wandering line from (x, z), each one stride on: x, z and the heading taken to it. */
    private static double[][] line(long h, double x, double z, double heading, double turn, int count) {
        double[][] line = new double[count][];
        for (int k = 0; k < count; k++) {
            heading += turn + (unit(h, 10 + k) - 0.5) * 0.25;
            x += Math.cos(heading) * STRIDE;
            z += Math.sin(heading) * STRIDE;
            line[k] = new double[]{x, z, heading};
        }
        return line;
    }

    /** The feet along a line, alternating half a block either side of it. */
    private static List<Step> feet(double[][] line) {
        List<Step> steps = new ArrayList<>(line.length);
        for (int k = 0; k < line.length; k++) {
            boolean left = k % 2 == 0;
            double side = left ? 0.5 : -0.5;
            double heading = line[k][2];
            steps.add(new Step(Mth.floor(line[k][0] - Math.sin(heading) * side), Mth.floor(line[k][1] + Math.cos(heading) * side), left));
        }
        return List.copyOf(steps);
    }

    private static boolean inCell(double v, int index, int cell) {
        return v >= (double) index * cell && v < (double) (index + 1) * cell;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A uniform number in [0, 1) from {@code hash} and an index. */
    private static double unit(long hash, int n) {
        return (mix(hash + n * 0x632BE59BD9B4E019L) >>> 11) * 0x1.0p-53;
    }
}
