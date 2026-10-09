package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code corruptedadventure:hanging_cluster} (change 47c, G2-WB13 = A): crystals hanging from the underside of a floating
 * island, glowing at night and seen from below. From the origin (the island's top) each column of a small square goes
 * down through the island to the first open block; with open air below that, a cluster hangs there facing down.
 * Reads and writes only its own chunk (the placement margin keeps the square inside).
 */
public final class HangingClusterFeature extends Feature<HangingClusterConfiguration> {
    public static final int MAX_RADIUS = 3;

    public HangingClusterFeature(Codec<HangingClusterConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<HangingClusterConfiguration> context) {
        HangingClusterConfiguration config = context.config();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int minX = origin.getX() & ~15;
        int minZ = origin.getZ() & ~15;
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = -config.radius(); dz <= config.radius(); dz++) {
            for (int dx = -config.radius(); dx <= config.radius(); dx++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (random.nextFloat() >= config.chance() || x < minX || x >= minX + 16 || z < minZ || z >= minZ + 16) {
                    continue;
                }
                int underside = underside(level, pos, x, origin.getY() - 1, z, config);
                if (underside == Integer.MIN_VALUE) {
                    continue;
                }
                BlockState state = config.state().getState(random, pos.set(x, underside, z));
                if (state.hasProperty(AmethystClusterBlock.FACING)) {
                    state = state.setValue(AmethystClusterBlock.FACING, Direction.DOWN);
                }
                if (state.canSurvive(level, pos)) {
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /**
     * The first open block below the solid run starting at {@code top}, if open air goes on at least
     * {@code min_drop} below it; otherwise {@link Integer#MIN_VALUE}.
     */
    private static int underside(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int top, int z, HangingClusterConfiguration config) {
        if (level.getBlockState(pos.set(x, top, z)).isAir()) {
            return Integer.MIN_VALUE;
        }
        for (int y = top - 1; y >= top - config.maxDepth() && y > level.getMinBuildHeight(); y--) {
            if (level.getBlockState(pos.set(x, y, z)).isAir()) {
                for (int below = 1; below <= config.minDrop(); below++) {
                    if (!level.getBlockState(pos.set(x, y - below, z)).isAir()) {
                        return Integer.MIN_VALUE;
                    }
                }
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
