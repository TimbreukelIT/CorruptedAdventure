package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.List;

/**
 * A straight, level, two-high and one-wide tunnel that runs from a cave wall or a slope into the rock and dead-ends:
 * dug, not grown. The mouth is the first step from two open blocks into two blocks of rock along the tunnel's
 * heading, on the origin's height. The whole tunnel is checked before anything is cut, so it is placed whole or not at
 * all: no water or lava in or beside it, nothing that falls in its roof, no rock under an open sky (no trench, no
 * plug), nothing a feature may not replace, and enough rock to be a tunnel rather than a cave.
 */
public final class UnnaturalTunnelFeature extends Feature<UnnaturalTunnelConfiguration> {
    private static final int HEIGHT = 2;

    public UnnaturalTunnelFeature(Codec<UnnaturalTunnelConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<UnnaturalTunnelConfiguration> context) {
        UnnaturalTunnelConfiguration config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        // Floor and roof are checked too.
        if (origin.getY() - 1 < level.getMinBuildHeight() || origin.getY() + HEIGHT >= level.getMaxBuildHeight()) {
            return false;
        }
        TunnelPath path = TunnelPath.random(context.random(), config.firstLength(), config.secondLength(), config.turnChance());
        BlockPos mouth = findMouth(level, path, origin, config);
        if (mouth == null) {
            return false;
        }
        List<BlockPos> cells = path.cells(mouth);
        if (!isSound(level, cells, config)) {
            return false;
        }
        // Every cell with rock in it has a roof (see isSound), so this never cuts an open trench.
        BlockState caveAir = Blocks.CAVE_AIR.defaultBlockState();
        for (BlockPos cell : cells) {
            for (int dy = 0; dy < HEIGHT; dy++) {
                BlockPos block = cell.above(dy);
                if (isRock(level.getBlockState(block), config)) {
                    level.setBlock(block, caveAir, Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    private static BlockPos findMouth(WorldGenLevel level, TunnelPath path, BlockPos origin, UnnaturalTunnelConfiguration config) {
        Direction back = path.direction().getOpposite();
        for (BlockPos mouth : path.mouthCandidates(origin)) {
            BlockPos front = mouth.relative(back);
            if (level.isEmptyBlock(front) && level.isEmptyBlock(front.above())
                    && isRock(level.getBlockState(mouth), config) && isRock(level.getBlockState(mouth.above()), config)) {
                return mouth;
            }
        }
        return null;
    }

    /**
     * Read-only check of the whole tunnel, its floor, roof and sides. A cell without a roof may only be open already
     * (a cave with an open ceiling): cutting it would leave a trench, not cutting it a plug in the tunnel.
     */
    private static boolean isSound(WorldGenLevel level, List<BlockPos> cells, UnnaturalTunnelConfiguration config) {
        int rock = 0;
        for (BlockPos cell : cells) {
            BlockState roof = level.getBlockState(cell.above(HEIGHT));
            if (roof.getBlock() instanceof FallingBlock) {
                return false;
            }
            boolean roofless = roof.isAir();
            for (int dy = 0; dy < HEIGHT; dy++) {
                BlockPos block = cell.above(dy);
                BlockState state = level.getBlockState(block);
                if (state.is(BlockTags.FEATURES_CANNOT_REPLACE) || !state.getFluidState().isEmpty()) {
                    return false;
                }
                for (Direction side : Direction.values()) {
                    if (!level.getFluidState(block.relative(side)).isEmpty()) {
                        return false;
                    }
                }
                if (isRock(state, config)) {
                    if (roofless) {
                        return false;
                    }
                    rock++;
                }
            }
        }
        return rock >= config.minSolidFraction() * cells.size() * HEIGHT;
    }

    private static boolean isRock(BlockState state, UnnaturalTunnelConfiguration config) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.is(config.replaceable());
    }
}
