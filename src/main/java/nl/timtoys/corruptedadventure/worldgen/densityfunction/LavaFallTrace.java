package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;

/**
 * Finds where a lava fall leaves a floating shape (39f): walking outward from its centre along a direction, the last
 * block column that still holds the shape gets the spring (on its top) and the next column, sharing a side with it,
 * gets the falling lava. Pure: only the shape's column tops are read.
 */
final class LavaFallTrace {
    private static final double STEP = 0.4;

    private LavaFallTrace() {
    }

    /** The top of a shape in a block column (first air above it), or NaN if the column holds none of it. */
    @FunctionalInterface
    interface ColumnTop {
        double top(int bx, int bz);
    }

    /**
     * The fall along (dirX, dirZ) (a unit vector) from {@code from} to {@code to} blocks off (x, z), or null if the walk
     * never crosses the shape's edge.
     */
    @Nullable
    static Volcano.LavaFall trace(double x, double z, double dirX, double dirZ, double from, double to, ColumnTop shape) {
        int lastX = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;
        double lastTop = Double.NaN;
        for (double s = from; s <= to; s += STEP) {
            int bx = Mth.floor(x + dirX * s);
            int bz = Mth.floor(z + dirZ * s);
            if (bx == lastX && bz == lastZ) {
                continue;
            }
            double top = shape.top(bx, bz);
            if (!Double.isNaN(top)) {
                lastX = bx;
                lastZ = bz;
                lastTop = top;
                continue;
            }
            if (Double.isNaN(lastTop)) {
                // Not on the shape yet (the walk starts inside a gap of a ragged outline): keep walking.
                continue;
            }
            if (bx != lastX && bz != lastZ) {
                // A diagonal step: the column beside both decides which of them the spring and the fall get.
                double side = shape.top(bx, lastZ);
                if (!Double.isNaN(side)) {
                    return new Volcano.LavaFall(bx, lastZ, Mth.floor(side), bx, bz);
                }
                return new Volcano.LavaFall(lastX, lastZ, Mth.floor(lastTop), bx, lastZ);
            }
            return new Volcano.LavaFall(lastX, lastZ, Mth.floor(lastTop), bx, bz);
        }
        return null;
    }
}
