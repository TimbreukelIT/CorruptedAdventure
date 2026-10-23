package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Map;

/**
 * {@code corruptedadventure:ring_5_memory} (94, V7-A), once per chunk: this chunk's part of the memories of one kind
 * ({@link Ring5Memories}; the placed features {@code ring_5/memory_<kind>}). The ground of a memory lying on the dead
 * ground goes over what is there (it is a piece of terrain); a floating memory and everything that grows on a memory
 * only fill air and the plants of the ground ({@link Ring5Placements#free}).
 */
public final class MemoryFeature extends Feature<MemoryFeature.Config> {
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public record Config(Ring5Memories.Kind kind) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ring5Memories.Kind.CODEC.fieldOf("kind").forGetter(Config::kind)
        ).apply(instance, Config::new));
    }

    public MemoryFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Ring5World world = Ring5Terrain.world(level);
        if (world == null) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(context.origin());
        boolean placed = false;
        for (Ring5Memories.Memory memory : Ring5Memories.within(world, chunk.getMinBlockX(),
                chunk.getMinBlockZ(), chunk.getMaxBlockX(), chunk.getMaxBlockZ(), 0)) {
            if (memory.kind() != context.config().kind()) {
                continue;
            }
            for (Map.Entry<BlockPos, BlockState> block : Ring5Memories.blocks(memory, world).entrySet()) {
                BlockPos pos = block.getKey();
                if (pos.getX() >> 4 != chunk.x || pos.getZ() >> 4 != chunk.z || level.isOutsideBuildHeight(pos)) {
                    continue;
                }
                boolean terrain = !memory.floating() && block.getValue().isSolid();
                if (terrain || Ring5Placements.free(level.getBlockState(pos))) {
                    level.setBlock(pos, block.getValue(), FLAGS);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
