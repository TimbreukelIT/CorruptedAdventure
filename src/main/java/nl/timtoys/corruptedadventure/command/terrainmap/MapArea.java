package nl.timtoys.corruptedadventure.command.terrainmap;

import java.util.Locale;

/**
 * The columns of a top-view map: a square around a center, one sample every {@code scale} blocks (the pixel's
 * north-west corner). Blocks of the map's own dimension.
 */
record MapArea(int centerX, int centerZ, int radius, int scale) {
    /** Pixels per side; also the cap that keeps a run within minutes. */
    static final int MAX_SIZE = 2048;
    static final int MAX_SCALE = 64;
    static final int DEFAULT_SCALE = 4;

    /** {@code null} if the area is valid, else the reason in fixed English text. */
    static String validate(int radius, int scale) {
        if (scale < 1 || scale > MAX_SCALE) {
            return String.format(Locale.ROOT, "Scale must be 1-%d blocks per pixel", MAX_SCALE);
        }
        if (radius < scale) {
            return "Radius must be at least the scale";
        }
        long size = 2L * radius / scale;
        if (size > MAX_SIZE) {
            return String.format(Locale.ROOT, "Map too large: %d x %d pixels (max %d); use a larger scale or a smaller radius",
                    size, size, MAX_SIZE);
        }
        return null;
    }

    int size() {
        return 2 * radius / scale;
    }

    int columns() {
        return size() * size();
    }

    int blockX(int pixel) {
        return centerX - radius + pixel * scale;
    }

    int blockZ(int pixel) {
        return centerZ - radius + pixel * scale;
    }
}
