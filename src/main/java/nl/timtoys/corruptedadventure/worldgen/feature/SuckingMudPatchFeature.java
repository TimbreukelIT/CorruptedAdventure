package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * {@code corruptedadventure:sucking_mud_patch} (package 76, G1 = A): a small patch of deadly sucking mud on a bank, at
 * most {@link #MAX_WIDTH} × {@link #MAX_WIDTH} and 2–3 deep. The patch lies on the ground at the origin's height: every
 * column whose top block ({@code WORLD_SURFACE_WG} − 1) is that height, dry and {@code replaceable} turns to mud down to
 * {@code depth} (stopping at the first block it may not replace); the rest of the rectangle stays. The rectangle is
 * shifted into the origin's chunk, so it writes only there.
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
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int sizeX = config.width().sample(random);
        int sizeZ = config.width().sample(random);
        int depth = config.depth().sample(random);
        int x0 = start(origin.getX(), sizeX);
        int z0 = start(origin.getZ(), sizeZ);
        int ground = origin.getY() - 1;
        BlockState mud = ModBlocks.SUCKING_MUD.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = x0; x < x0 + sizeX; x++) {
            for (int z = z0; z < z0 + sizeZ; z++) {
                if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1 != ground
                        || !level.getFluidState(pos.set(x, ground + 1, z)).isEmpty()) {
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

    /** The first block of a side of {@code size} around {@code centre}, shifted to lie wholly in the centre's chunk. */
    static int start(int centre, int size) {
        int chunk = centre & ~15;
        int start = centre - size / 2;
        return Math.max(chunk, Math.min(start, chunk + 16 - size));
    }
}
