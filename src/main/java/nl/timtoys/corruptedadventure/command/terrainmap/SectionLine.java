package nl.timtoys.corruptedadventure.command.terrainmap;

import java.util.Locale;

/**
 * The columns of a vertical section from {@code (x1, z1)} to {@code (x2, z2)}, both ends included: one column per
 * block along the line, or evenly spread over at most {@link #MAX_SAMPLES} columns on a longer line.
 */
record SectionLine(int x1, int z1, int x2, int z2) {
    static final int MAX_SAMPLES = 2048;
    static final int MAX_LENGTH = 16_384;

    /** {@code null} if the line is valid, else the reason in fixed English text. */
    static String validate(int x1, int z1, int x2, int z2) {
        double length = length(x1, z1, x2, z2);
        if (length < 1.0) {
            return "The two ends of a section must differ";
        }
        if (length > MAX_LENGTH) {
            return String.format(Locale.ROOT, "Section too long: %.0f blocks (max %d)", length, MAX_LENGTH);
        }
        return null;
    }

    double length() {
        return length(x1, z1, x2, z2);
    }

    int samples() {
        return (int) Math.min(MAX_SAMPLES, Math.floor(length()) + 1);
    }

    /** Blocks along the line per sample. */
    double step() {
        return length() / (samples() - 1);
    }

    int blockX(int sample) {
        return x1 + (int) Math.round((double) (x2 - x1) * sample / (samples() - 1));
    }

    int blockZ(int sample) {
        return z1 + (int) Math.round((double) (z2 - z1) * sample / (samples() - 1));
    }

    private static double length(int x1, int z1, int x2, int z2) {
        return Math.hypot(x2 - x1, z2 - z1);
    }
}
