package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code corruptedadventure:rock_bridge} (G2 ring 2): the eye-catcher of {@code canyonlands}, a natural rock bridge
 * over a deep canyon from rim to rim ({@link Ring2RockLandmarks.Bridge}), in the canyon strata of the walls, with a
 * little red sand on its deck. Every chunk writes only its own part of every bridge that reaches it.
 */
public final class RockBridgeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    /** Share of the deck's top blocks that are red sand (the plateau's own surface), where the deck is two or more thick. */
    private static final double SAND = 0.3;

    public RockBridgeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        boolean placed = false;
        for (Ring2RockLandmarks.Bridge bridge : Ring2RockLandmarks.bridgesNear(level, chunk.getMinBlockX(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), chunk.getMaxBlockZ())) {
            placed |= write(level, chunk, bridge);
        }
        return placed;
    }

    private static boolean write(WorldGenLevel level, ChunkPos chunk, Ring2RockLandmarks.Bridge bridge) {
        Ring2PureTerrain terrain = Ring2PureTerrain.of(level);
        int[] box = bridge.box();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = Math.max(box[0], chunk.getMinBlockX()); x <= Math.min(box[2], chunk.getMaxBlockX()); x++) {
            for (int z = Math.max(box[1], chunk.getMinBlockZ()); z <= Math.min(box[3], chunk.getMaxBlockZ()); z++) {
                int[] span = bridge.interval(x, z);
                if (span == null) {
                    continue;
                }
                for (int y = span[0]; y <= span[1]; y++) {
                    boolean sand = y == span[1] && span[1] > span[0]
                            && Ring2RockLandmarks.hash(bridge.seed(), x, y, z, 11) < SAND;
                    level.setBlock(pos.set(x, y, z), sand ? Blocks.RED_SAND.defaultBlockState() : terrain.stratum(x, y, z), FLAGS);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
