package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongArrayList;
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
 * The feature looks for the bank itself: of the origin's chunk it takes every bank column (see {@link #bankAt}) at
 * least {@code bank_reach} from the chunk's edges, picks one with the placement random and centres the patch on it.
 * A random point per chunk almost never hit the 1–2 wide strip of a bank (mud-5b: 1 patch in ~4.400 chunks), so the
 * chance per chunk with a bank is now the placement's own. Around the chosen column every column whose top block
 * ({@code WORLD_SURFACE_WG} − 1) lies at its height, dry and {@code replaceable} turns to mud down to {@code depth}
 * (stopping at the first block it may not replace) if it lies in one of {@code biomes} (a patch never reaches into the
 * reeds of {@code reed_marsh}); the rest of the rectangle stays. The rectangle is shifted into the origin's chunk.
 * <p>
 * Determinism: every read (the frozen worldgen heightmap, blocks, biomes) and every write stays in the origin's chunk,
 * and the bank's ground is checked against the pure terrain ({@link Ring4Filter#onGround}: no mud on islands).
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
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        LongArrayList banks = new LongArrayList();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int reach = config.bankReach();
        for (int z = minZ + reach; z < minZ + 16 - reach; z++) {
            for (int x = minX + reach; x < minX + 16 - reach; x++) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (dryGround(level, config, pos, x, top, z) && bankAt(level, config, pos, x, top, z)
                        && Ring4Filter.onGround(top + 1, field.column(x, z).surface(), Ring4Filter.DEFAULT_TOLERANCE)) {
                    banks.add(BlockPos.asLong(x, top, z));
                }
            }
        }
        if (banks.isEmpty()) {
            return false;
        }
        BlockPos centre = BlockPos.of(banks.getLong(random.nextInt(banks.size())));
        int x0 = start(centre.getX(), sizeX);
        int z0 = start(centre.getZ(), sizeZ);
        int ground = centre.getY();
        BlockState mud = ModBlocks.SUCKING_MUD.get().defaultBlockState();
        boolean placed = false;
        for (int x = x0; x < x0 + sizeX; x++) {
            for (int z = z0; z < z0 + sizeZ; z++) {
                if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1 != ground || !dryGround(level, config, pos, x, ground, z)) {
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

    /** Whether the top block {@code top} of (x, z) is dry {@code replaceable} ground with nothing wet above, in one of the biomes. */
    private static boolean dryGround(WorldGenLevel level, SuckingMudPatchConfiguration config, BlockPos.MutableBlockPos pos,
                                     int x, int top, int z) {
        BlockState state = level.getBlockState(pos.set(x, top, z));
        return state.is(config.replaceable()) && state.getFluidState().isEmpty()
                && level.getFluidState(pos.set(x, top + 1, z)).isEmpty() && config.biomes().contains(level.getBiome(pos));
    }

    /**
     * Whether (x, z) with its top block at {@code top} is a bank: within {@code bank_reach} straight along x or z a
     * column whose top block is water, at most {@code bank_drop} below {@code top} ({@link #waterFits}).
     */
    private static boolean bankAt(WorldGenLevel level, SuckingMudPatchConfiguration config, BlockPos.MutableBlockPos pos,
                                  int x, int top, int z) {
        for (int d = 1; d <= config.bankReach(); d++) {
            if (water(level, config, pos, x + d, top, z) || water(level, config, pos, x - d, top, z)
                    || water(level, config, pos, x, top, z + d) || water(level, config, pos, x, top, z - d)) {
                return true;
            }
        }
        return false;
    }

    private static boolean water(WorldGenLevel level, SuckingMudPatchConfiguration config, BlockPos.MutableBlockPos pos,
                                 int x, int ground, int z) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        return waterFits(ground, surface, config.bankDrop()) && level.getFluidState(pos.set(x, surface, z)).is(FluidTags.WATER);
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
