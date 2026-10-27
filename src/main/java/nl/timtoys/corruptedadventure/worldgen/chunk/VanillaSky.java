package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;

/**
 * Where vanilla's noise aquifer of a noise chunk answers air for certain (opt/aquifer): its sky, from the top of the world
 * down to just above the highest fluid level of the cells it compares. {@link RingAquifer} answers there without asking
 * it; the 1.21.1 aquifer runs its full search over twelve cells for every open block, the sky included.
 * <p>
 * Why it is exact ({@code Aquifer.NoiseBasedAquifer.computeSubstance}): a block at y compares the cells of its grid band
 * {@code floorDiv(y + 1, 12)} and the bands beside it, two cells in x and two in z. If y is at least
 * {@link #BARRIER_REACH} above every level of those twelve cells, the nearest cell's fluid is air at y, and for each pair
 * of cells vanilla compares the pressure is negative or zero (equal levels give 0; otherwise its distance term is at most
 * (highest level − y − 0.5) / 2.5 ≤ −2.2, below −2, where vanilla takes no barrier noise and returns twice that), so no
 * barrier forms for any substance ≤ 0: vanilla returns air. Its first check, the global fluid's lava, comes from the same
 * picker ({@link RingFluidPicker.ChunkPicker#lavaFreeFrom}). The fluid update flag vanilla sets there (often true) is
 * not known: the noise chunk reads it only for a block with a fluid, but a carver also reads it, left over from its last
 * call, for the lava it places at its lava level without asking. So the sky answers in the noise fill only.
 * <p>
 * The cells and their statuses are vanilla's own (its caches, filled the way it fills them), so vanilla later finds them
 * there. Only the chunk's own columns are answered; the sky of a column group is found once, on first use, scanning the
 * bands from the top down to the first one where it stops.
 */
final class VanillaSky {
    /** How far above the highest level of its cells a block is air for certain (see the class comment). */
    static final int BARRIER_REACH = 5;
    /** No sky in the column: vanilla answers every open block. */
    static final int NONE = Integer.MAX_VALUE;
    // Vanilla's grid of aquifer cells (its own constants, AT): a cell's spacing, and how far its spot lies in it.
    private static final int X_SPACING = Aquifer.NoiseBasedAquifer.X_SPACING;
    private static final int Y_SPACING = Aquifer.NoiseBasedAquifer.Y_SPACING;
    private static final int Z_SPACING = Aquifer.NoiseBasedAquifer.Z_SPACING;
    private static final int X_RANGE = Aquifer.NoiseBasedAquifer.X_RANGE;
    private static final int Y_RANGE = Aquifer.NoiseBasedAquifer.Y_RANGE;
    private static final int Z_RANGE = Aquifer.NoiseBasedAquifer.Z_RANGE;
    /** The shift of a block's first cell in x and z ({@code floorDiv(x - 5, 16)} in vanilla's search, a literal there). */
    private static final int CELL_SHIFT = 5;

    private final Aquifer.NoiseBasedAquifer aquifer;
    private final LavaFree lavaFree;
    private final int minX;
    private final int minZ;
    private final int minY;
    private final int maxY;
    // Per column group (the cells vanilla compares in x and z, two per axis over a chunk): the sky's bottom.
    private final int[] from = new int[4];
    private final boolean[] known = new boolean[4];

    /**
     * @param lavaFree where the global fluid picker vanilla's aquifer was made with stops giving lava
     *                 ({@link RingFluidPicker.ChunkPicker#lavaFreeFrom})
     * @param minY     the lowest block of the noise chunk ({@code NoiseSettings.minY}, as the aquifer was made with)
     * @param height   its height
     */
    VanillaSky(Aquifer.NoiseBasedAquifer aquifer, LavaFree lavaFree, ChunkPos chunk, int minY, int height) {
        this.aquifer = aquifer;
        this.lavaFree = lavaFree;
        this.minX = chunk.getMinBlockX();
        this.minZ = chunk.getMinBlockZ();
        this.minY = minY;
        this.maxY = minY + height - 1;
    }

    /**
     * The lowest Y from which vanilla's aquifer answers air at every open block of the column, up to the top of the
     * world, if it lies below {@code below} (the cells are not looked at when its global fluid's lava reaches that high);
     * else, outside the chunk or where the sky does not reach, {@link #NONE}.
     */
    int from(int x, int z, int below) {
        if (x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) {
            return NONE;
        }
        int lavaFree = this.lavaFree.from(x, z);
        if (lavaFree >= below) {
            return NONE;
        }
        int gridX = Math.floorDiv(x - CELL_SHIFT, X_SPACING);
        int gridZ = Math.floorDiv(z - CELL_SHIFT, Z_SPACING);
        // Over a chunk the first cell in x is one of two neighbours, so is the one in z: their parity tells them apart.
        int group = (gridX & 1) | (gridZ & 1) << 1;
        if (!known[group]) {
            from[group] = scan(gridX, gridZ);
            known[group] = true;
        }
        return Math.max(from[group], lavaFree);
    }

    /** The lowest Y from which the global fluid picker's status is not lava at any Y up in a column. */
    @FunctionalInterface
    interface LavaFree {
        int from(int x, int z);
    }

    /** The sky's bottom over the bands, from the top down to the first one where some block is not air for certain. */
    private int scan(int gridX, int gridZ) {
        int sky = NONE;
        for (int band = Math.floorDiv(maxY + 1, Y_SPACING); band >= Math.floorDiv(minY + 1, Y_SPACING); band--) {
            // The blocks of the band: floorDiv(y + 1, 12) == band.
            int low = Math.max(band * Y_SPACING - 1, minY);
            int high = Math.min(band * Y_SPACING + Y_SPACING - 2, maxY);
            long clear = (long) highestLevel(gridX, band, gridZ) + BARRIER_REACH;
            if (clear > high) {
                break;
            }
            sky = (int) Math.max(clear, low);
            if (clear > low) {
                break;
            }
        }
        return sky;
    }

    /** The highest fluid level of the twelve cells vanilla compares for a block of the band. */
    private int highestLevel(int gridX, int band, int gridZ) {
        int highest = Integer.MIN_VALUE;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    highest = Math.max(highest, aquifer.getAquiferStatus(location(gridX + dx, band + dy, gridZ + dz)).fluidLevel);
                }
            }
        }
        return highest;
    }

    /** Vanilla's cell location (its cache, or made and stored as vanilla makes it; {@code VanillaSkyTest} compares). */
    private long location(int gridX, int gridY, int gridZ) {
        int index = aquifer.getIndex(gridX, gridY, gridZ);
        long location = aquifer.aquiferLocationCache[index];
        if (location == Long.MAX_VALUE) {
            RandomSource random = aquifer.positionalRandomFactory.at(gridX, gridY, gridZ);
            location = BlockPos.asLong(gridX * X_SPACING + random.nextInt(X_RANGE), gridY * Y_SPACING + random.nextInt(Y_RANGE),
                    gridZ * Z_SPACING + random.nextInt(Z_RANGE));
            aquifer.aquiferLocationCache[index] = location;
        }
        return location;
    }
}
