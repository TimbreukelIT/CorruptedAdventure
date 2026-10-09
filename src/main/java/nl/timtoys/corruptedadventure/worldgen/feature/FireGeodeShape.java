package nl.timtoys.corruptedadventure.worldgen.feature;

import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeededHash;

/**
 * The pure geometry of a fire crystal geode of ring 3 (package 42), shared by the terrain (which digs the cavity,
 * {@code ring_3/overworld/fire_geodes}) and the lining feature of 41b, so both agree on where a geode is without reading
 * each other. Like {@link DewGeodeShape}: one geode at most per cell of a world-fixed 2D grid of {@link #CELL} blocks,
 * its centre kept {@link #MARGIN} blocks from the cell's edge so the whole geode, lining included, stays inside its cell
 * (a chunk lies within one cell). Each cell tries {@link #ATTEMPTS} positions in order; the terrain takes the first that
 * lies deep in the ring 3 core, clear of the abyss and the magma chambers ({@code VolcanicField#fireGeode} gives the
 * one it took). Closed: vanilla caves stay out of it; a lava tube may cut through one now and then.
 */
public final class FireGeodeShape {
    /** Cell size (blocks, a multiple of 16): with {@link #CHANCE} about one geode per 12 chunks. */
    public static final int CELL = 48;
    public static final double CHANCE = 0.75;
    public static final int ATTEMPTS = 4;
    public static final int RADIUS_MIN = 4;
    public static final int RADIUS_MAX = 7;
    /** The cavity's centre lies between these Y (deep, above vanilla's lava layer). */
    public static final int Y_MIN = -50;
    public static final int Y_MAX = 20;
    /** How much the cavity differs from a sphere: axis stretch in [1 − this, 1 + this]. */
    private static final double STRETCH = 0.15;
    /** The lining (smooth basalt, blackstone, fire crystal) reaches this far past the cavity. */
    public static final double LINING = 3.0;
    /** Distance from a cell edge to the nearest possible centre. */
    public static final int MARGIN = (int) Math.ceil(RADIUS_MAX * (1.0 + STRETCH) + LINING) + 1;
    private static final int SALT = 4201;

    private FireGeodeShape() {
    }

    /** {@link SeededHash} of a cell, whatever noise it comes from (a density function's holder, a feature's noise). */
    @FunctionalInterface
    public interface CellHash {
        long hash(int cellX, int cellZ, int salt);
    }

    /** A geode: the cavity is the ellipsoid {@link #distance}{@code (x, y, z) <= radius}. */
    public record Geode(int x, int y, int z, int radius, double stretchX, double stretchY, double stretchZ) {
        /** Stretched distance in blocks from the centre to the block {@code (x, y, z)}; the cavity is {@code <= radius}. */
        public double distance(int blockX, int blockY, int blockZ) {
            double dx = (blockX - x) / stretchX;
            double dy = (blockY - y) / stretchY;
            double dz = (blockZ - z) / stretchZ;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public boolean inCavity(int blockX, int blockY, int blockZ) {
            return distance(blockX, blockY, blockZ) <= radius;
        }

        /** Largest distance in blocks, on any axis, from the centre to the lining. */
        public int reach() {
            return (int) Math.ceil((radius + LINING) * Math.max(stretchX, Math.max(stretchY, stretchZ)));
        }
    }

    /**
     * Attempt {@code attempt} of cell ({@code cellX}, {@code cellZ}), or null if the cell has no geode (one draw per cell,
     * {@link #CHANCE}). Each attempt has its own centre, radius, height and shape. Pure.
     */
    public static Geode candidate(CellHash hash, int cellX, int cellZ, int attempt) {
        long cell = hash.hash(cellX, cellZ, SALT);
        if (SeededHash.unit(cell, 0) >= CHANCE) {
            return null;
        }
        long h = hash.hash(cellX, cellZ, SALT + 1 + attempt);
        int span = CELL - 2 * MARGIN;
        int x = cellX * CELL + MARGIN + (int) (SeededHash.unit(h, 1) * span);
        int z = cellZ * CELL + MARGIN + (int) (SeededHash.unit(h, 2) * span);
        int radius = RADIUS_MIN + (int) (SeededHash.unit(h, 3) * (RADIUS_MAX - RADIUS_MIN + 1));
        int yMin = Y_MIN + (int) Math.ceil(radius * (1.0 + STRETCH));
        int y = yMin + (int) (SeededHash.unit(h, 4) * (Y_MAX - yMin + 1));
        return new Geode(x, y, z, radius, stretch(h, 5), stretch(h, 6), stretch(h, 7));
    }

    private static double stretch(long hash, int n) {
        return 1.0 - STRETCH + 2.0 * STRETCH * SeededHash.unit(hash, n);
    }
}
