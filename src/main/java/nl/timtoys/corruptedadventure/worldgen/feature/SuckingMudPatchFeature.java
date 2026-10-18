package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

/**
 * {@code corruptedadventure:sucking_mud_patch} (package 76, G1 = A; denser in mud-5b): a small patch of deadly sucking
 * mud on a bank, at most {@link #MAX_WIDTH} × {@link #MAX_WIDTH} and 2–3 deep.
 * <p>
 * The feature looks for the bank itself: of the origin's chunk it takes every bank column (see {@link #bankAt}), picks
 * one ({@link #pick}) and centres the patch on it.
 * A random point per chunk almost never hit the 1–2 wide strip of a bank (mud-5b: 1 patch in ~4.400 chunks), so the
 * chance per chunk with a bank is now the placement's own. Around the chosen column every column whose top block
 * ({@code WORLD_SURFACE_WG} − 1) lies at its height, dry and {@code replaceable} turns to mud down to {@code depth}
 * (stopping at the first block it may not replace) if it lies in one of {@code biomes} (a patch never reaches into the
 * reeds of {@code reed_marsh}); the rest of the rectangle stays. The rectangle is shifted into the origin's chunk.
 * <p>
 * Determinism: every block and heightmap read (the frozen worldgen heightmap) and every write stays in the origin's
 * chunk; biomes are fixed before features; the bank's ground is checked against the pure terrain
 * ({@link Ring4Filter#onGround}: no mud on islands). The pick does not depend on how many banks were found, so a
 * column a neighbour's feature changed before this one ran only matters if it is the winner itself.
 */
public final class SuckingMudPatchFeature extends Feature<SuckingMudPatchConfiguration> {
    /** The largest side of a patch (G1 = A: ≤ 4 × 4). */
    public static final int MAX_WIDTH = 4;

    public SuckingMudPatchFeature(Codec<SuckingMudPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SuckingMudPatchConfiguration> context) {
        WorldGenLevel level = context.level();
        SuckingMudPatchConfiguration config = context.config();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        RandomSource random = context.random();
        int sizeX = config.width().sample(random);
        int sizeZ = config.width().sample(random);
        int depth = config.depth().sample(random);
        long salt = random.nextLong();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        LongArrayList banks = new LongArrayList();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = minZ; z < minZ + 16; z++) {
            for (int x = minX; x < minX + 16; x++) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                // Cheap reads first; the biome and the pure terrain only for a bank.
                if (dry(level, config, pos, x, top, z) && bankAt(level, config, pos, minX, minZ, x, top, z)
                        && inBiomes(level, config, pos, x, top, z)
                        && Ring4Filter.onGround(top + 1, field.column(x, z).surface(), Ring4Filter.DEFAULT_TOLERANCE)) {
                    banks.add(BlockPos.asLong(x, top, z));
                }
            }
        }
        if (banks.isEmpty()) {
            return false;
        }
        BlockPos centre = BlockPos.of(pick(banks, salt));
        int x0 = start(centre.getX(), sizeX);
        int z0 = start(centre.getZ(), sizeZ);
        int ground = centre.getY();
        BlockState mud = ModBlocks.SUCKING_MUD.get().defaultBlockState();
        boolean placed = false;
        for (int x = x0; x < x0 + sizeX; x++) {
            for (int z = z0; z < z0 + sizeZ; z++) {
                if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1 != ground || !dry(level, config, pos, x, ground, z)
                        || !inBiomes(level, config, pos, x, ground, z)) {
                    continue;
                }
                for (int y = ground; y > ground - depth; y--) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (!state.is(config.replaceable()) || !state.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(pos, mud, Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** Whether the top block {@code top} of (x, z) is dry {@code replaceable} ground with nothing wet above. */
    private static boolean dry(WorldGenLevel level, SuckingMudPatchConfiguration config, BlockPos.MutableBlockPos pos,
                               int x, int top, int z) {
        BlockState state = level.getBlockState(pos.set(x, top, z));
        return state.is(config.replaceable()) && state.getFluidState().isEmpty() && level.getFluidState(pos.set(x, top + 1, z)).isEmpty();
    }

    /** Whether the block above the top block {@code top} of (x, z) lies in one of the biomes. */
    private static boolean inBiomes(WorldGenLevel level, SuckingMudPatchConfiguration config, BlockPos.MutableBlockPos pos,
                                    int x, int top, int z) {
        return config.biomes().contains(level.getBiome(pos.set(x, top + 1, z)));
    }

    /**
     * Whether (x, z) with its top block at {@code top} is a bank: within {@code bank_reach} straight along x or z, inside
     * the chunk with minimum corner ({@code minX}, {@code minZ}), a column whose top block is water at most
     * {@code bank_drop} below {@code top} ({@link #waterFits}). Water just over the chunk's edge is not looked at.
     */
    private static boolean bankAt(WorldGenLevel level, SuckingMudPatchConfiguration config, BlockPos.MutableBlockPos pos,
                                  int minX, int minZ, int x, int top, int z) {
        for (int d = 1; d <= config.bankReach(); d++) {
            if (water(level, config, pos, minX, minZ, x + d, top, z) || water(level, config, pos, minX, minZ, x - d, top, z)
                    || water(level, config, pos, minX, minZ, x, top, z + d) || water(level, config, pos, minX, minZ, x, top, z - d)) {
                return true;
            }
        }
        return false;
    }

    private static boolean water(WorldGenLevel level, SuckingMudPatchConfiguration config, BlockPos.MutableBlockPos pos,
                                 int minX, int minZ, int x, int ground, int z) {
        if (!inChunk(minX, minZ, x, z)) {
            return false;
        }
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        return waterFits(ground, surface, config.bankDrop()) && level.getFluidState(pos.set(x, surface, z)).is(FluidTags.WATER);
    }

    /** Whether (x, z) lies in the chunk with minimum corner ({@code minX}, {@code minZ}). */
    static boolean inChunk(int minX, int minZ, int x, int z) {
        return x >= minX && x < minX + 16 && z >= minZ && z < minZ + 16;
    }

    /**
     * The bank with the highest hash of its column and {@code salt}: the same column wins whatever else is in the list
     * or in which order, so one changed candidate moves the patch only if it was the winner.
     */
    static long pick(LongList banks, long salt) {
        long best = banks.getLong(0);
        long bestHash = HashCommon.mix(salt ^ best);
        for (int i = 1; i < banks.size(); i++) {
            long hash = HashCommon.mix(salt ^ banks.getLong(i));
            if (hash > bestHash) {
                best = banks.getLong(i);
                bestHash = hash;
            }
        }
        return best;
    }

    /** Whether water whose top block is {@code water} lies beside ground with its top at {@code ground}: level with it or up to {@code drop} lower. */
    static boolean waterFits(int ground, int water, int drop) {
        return water <= ground && water >= ground - drop;
    }

    /** The first block of a side of {@code size} around {@code centre}, shifted to lie wholly in the centre's chunk. */
    static int start(int centre, int size) {
        return start(centre, size, 0);
    }

    /**
     * The first block of a side of {@code size} around {@code centre}, shifted to lie in the centre's chunk at least
     * {@code margin} from its edges (a shape that reads its neighbours keeps every read in its own chunk).
     */
    static int start(int centre, int size, int margin) {
        int chunk = centre & ~15;
        int start = centre - size / 2;
        return Math.max(chunk + margin, Math.min(start, chunk + 16 - margin - size));
    }
}
