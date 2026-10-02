package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.List;

/**
 * {@code corruptedadventure:rubble}: a boulder of 1–3 blocks across on the terrain at the origin ({@link RubbleShape}),
 * in one material. Reads the {@code WORLD_SURFACE_WG} heightmap and blocks of its own chunk only, and writes only
 * there: on the ground it replaces solid terrain (half buried), above it only air and replaceable plants.
 */
public final class RubbleFeature extends Feature<RubbleConfiguration> {
    public RubbleFeature(Codec<RubbleConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RubbleConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        RubbleConfiguration config = context.config();
        int size = config.size().sample(random);
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        List<BlockPos> blocks = RubbleShape.blocks(context.origin(), size, random, new RubbleShape.Terrain() {
            @Override
            public int groundTop(int x, int z) {
                return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            }

            @Override
            public boolean solid(int x, int y, int z) {
                probe.set(x, y, z);
                BlockState state = level.getBlockState(probe);
                return state.getFluidState().isEmpty() && !state.is(BlockTags.FEATURES_CANNOT_REPLACE)
                        && state.isFaceSturdy(level, probe, Direction.UP);
            }

            @Override
            public boolean free(int x, int y, int z) {
                BlockState state = level.getBlockState(probe.set(x, y, z));
                return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
            }
        });
        if (blocks.isEmpty()) {
            return false;
        }
        BlockState material = config.stateProvider().getState(random, context.origin());
        for (BlockPos pos : blocks) {
            level.setBlock(pos, material, Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
