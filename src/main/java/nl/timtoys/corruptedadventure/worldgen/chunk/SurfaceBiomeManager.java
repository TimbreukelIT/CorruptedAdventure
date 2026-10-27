package nl.timtoys.corruptedadventure.worldgen.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;

/**
 * The biome lookup of one chunk's surface pass: vanilla's zoomed lookup with the same result, only cheaper. Vanilla
 * returns the biome of whichever of the eight quart corners around a block is nearest by a hashed ("fiddled") distance;
 * where all eight corners hold the same biome the choice cannot matter, so that biome comes back without the hashes.
 * The surface rules ask for the biome of nearly every stone block (opt/surface: 35-42 % of the surface step in ring 2
 * went into the hashes), and almost every block lies inside one biome. The corners come from a table per chunk, filled
 * on first use from the vanilla manager's own noise biome source; any other block goes to the vanilla manager.
 * <p>
 * The super constructor gets a dummy zoom seed: every method that would use it is delegated to {@code vanilla}.
 */
final class SurfaceBiomeManager extends BiomeManager {
    // Quart columns from one west of the chunk's first through one east of its last (vanilla's corner l + 1).
    private static final int SIZE = 6;

    private final BiomeManager vanilla;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final int height;
    private final Holder<?>[] corners;

    /**
     * The table covers the chunk's blocks from {@code minY} up to (not including) {@code maxY}; every other block (the
     * pass also asks for the block above the column's top, which can lie at or above the build height) goes to vanilla.
     *
     * @param minBlockX the chunk's first block x
     * @param minBlockZ the chunk's first block z
     * @param minY      the lowest block y covered (the chunk's min build height)
     * @param maxY      the first block y above the covered ones (exclusive: the chunk's max build height)
     */
    SurfaceBiomeManager(BiomeManager vanilla, int minBlockX, int minBlockZ, int minY, int maxY) {
        super(vanilla::getNoiseBiomeAtQuart, 0L);
        this.vanilla = vanilla;
        this.originX = ((minBlockX - 2) >> 2);
        this.originZ = ((minBlockZ - 2) >> 2);
        this.originY = ((minY - 2) >> 2);
        this.height = ((maxY - 1 - 2) >> 2) + 2 - originY;
        this.corners = new Holder<?>[SIZE * SIZE * height];
    }

    @Override
    public Holder<Biome> getBiome(BlockPos pos) {
        // Vanilla's corner indices: the quart of (pos - 2), and the next one on each axis.
        int l = ((pos.getX() - 2) >> 2) - originX;
        int m = ((pos.getY() - 2) >> 2) - originY;
        int n = ((pos.getZ() - 2) >> 2) - originZ;
        if (l >= 0 && l < SIZE - 1 && n >= 0 && n < SIZE - 1 && m >= 0 && m < height - 1) {
            Holder<Biome> biome = corner(l, m, n);
            if (corner(l, m, n + 1) == biome && corner(l, m + 1, n) == biome && corner(l, m + 1, n + 1) == biome
                    && corner(l + 1, m, n) == biome && corner(l + 1, m, n + 1) == biome
                    && corner(l + 1, m + 1, n) == biome && corner(l + 1, m + 1, n + 1) == biome) {
                return biome;
            }
        }
        return vanilla.getBiome(pos);
    }

    @SuppressWarnings("unchecked")
    private Holder<Biome> corner(int l, int m, int n) {
        int index = (m * SIZE + l) * SIZE + n;
        Holder<Biome> biome = (Holder<Biome>) corners[index];
        if (biome == null) {
            biome = vanilla.getNoiseBiomeAtQuart(originX + l, originY + m, originZ + n);
            corners[index] = biome;
        }
        return biome;
    }

    @Override
    public BiomeManager withDifferentSource(NoiseBiomeSource newSource) {
        return vanilla.withDifferentSource(newSource);
    }
}
