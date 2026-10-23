package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * {@code corruptedadventure:missing_chunk} (94, V1-A), once per chunk: finishes the clean missing chunks
 * ({@link Ring5MissingChunks}) that the surface pass emptied. In a missing chunk it clears what the surface rules do not
 * touch (the bedrock floor, the deep lava layer under Y −54): the chunk is open down to below {@code min_y}. In a chunk
 * beside one it turns the fluid within {@link #DAM} blocks of the hole into rock, down from Y {@link #DAM_TOP}: the dam the
 * fluid rule keeps round every crack (V9-B), so no lava ever stands against the hole. Only its own chunk, from the pure
 * decision: independent of generation order.
 */
public final class MissingChunkFeature extends Feature<NoneFeatureConfiguration> {
    static final int DAM = 6;
    /** The highest Y the dam reaches: ring 5's lava layer lies under Y −54. */
    static final int DAM_TOP = 0;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public MissingChunkFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        Ring5World world = Ring5Terrain.world(level);
        ChunkPos chunk = new ChunkPos(context.origin());
        if (world == null || !world.core(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ())) {
            return false;
        }
        if (Ring5MissingChunks.missing(world, chunk.x, chunk.z)) {
            clear(level, chunk);
            return true;
        }
        boolean placed = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && Ring5MissingChunks.missing(world, chunk.x + dx, chunk.z + dz)) {
                    dam(level, chunk, new ChunkPos(chunk.x + dx, chunk.z + dz));
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static void clear(WorldGenLevel level, ChunkPos chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    if (!level.getBlockState(pos.set(x, y, z)).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                    }
                }
            }
        }
    }

    /** The fluid of {@code chunk} within {@link #DAM} of {@code hole} becomes rock: the wall beside it corrupted stone. */
    private static void dam(WorldGenLevel level, ChunkPos chunk, ChunkPos hole) {
        BlockState wall = ModBlocks.CORRUPTED_STONE.get().defaultBlockState();
        BlockState rock = Blocks.DEEPSLATE.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                int dx = Math.max(Math.max(hole.getMinBlockX() - x, x - hole.getMaxBlockX()), 0);
                int dz = Math.max(Math.max(hole.getMinBlockZ() - z, z - hole.getMaxBlockZ()), 0);
                int distance = Math.max(dx, dz);
                if (distance > DAM) {
                    continue;
                }
                for (int y = level.getMinBuildHeight(); y <= DAM_TOP; y++) {
                    if (!level.getFluidState(pos.set(x, y, z)).isEmpty()) {
                        level.setBlock(pos, distance <= 1 ? wall : rock, FLAGS);
                    }
                }
            }
        }
    }
}
