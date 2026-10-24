package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code corruptedadventure:inverted_tree} (95, V10-A): a tree that hangs upside down from the underside of an upside-down
 * hill of {@code inverted_hills}: a trunk of {@code corrupted_log} down from the hill, a crown of persistent
 * {@code corrupted_leaves} at its lower end (an oak's shape turned over). Only where a hill hangs at least
 * {@link #MIN_HILL} of its depth under the floe; the hill's underside comes from the pure terrain
 * ({@link Ring5Field.Column#bottom()}), and the tree only goes where the block above its trunk is the hill's rock. A ground
 * object: its own random, its own chunk (reach {@link #REACH}), only into air.
 */
public final class InvertedTreeFeature extends Feature<NoneFeatureConfiguration> {
    static final int REACH = 2;
    /** The share of the deepest hill ({@code inverted} output) from which a tree hangs. */
    static final double MIN_HILL = 0.25;
    private static final long SALT = 0x1_7E37_EE5L;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    public InvertedTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        Ring5World world = Ring5Terrain.world(level);
        BlockPos origin = GroundObjects.fit(context.origin(), REACH);
        if (world == null || !world.core(origin.getX(), origin.getZ())) {
            return false;
        }
        Ring5Field.Column column = world.field().column(origin.getX(), origin.getZ());
        if (column.isVoid() || column.inverted() < MIN_HILL || !Double.isFinite(column.bottom())) {
            return false;
        }
        // The first open block under the hill: the terrain is rock above its underside.
        int hang = Mth.floor(column.bottom());
        BlockPos anchor = new BlockPos(origin.getX(), hang + 1, origin.getZ());
        if (!level.getBlockState(anchor).isSolid() || !level.getBlockState(anchor.below()).isAir()) {
            return false;
        }
        RandomSource random = GroundObjects.random(level.getSeed(), origin, SALT);
        BlockState log = ModBlocks.CORRUPTED_LOG.get().defaultBlockState();
        BlockState leaves = ModBlocks.CORRUPTED_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        List<int[]> oak = MirroredTreesFeature.oak(random);
        for (int[] b : oak) {
            blocks.put(new BlockPos(origin.getX() + b[0], hang - b[1], origin.getZ() + b[2]), b[3] == 1 ? log : leaves);
        }
        for (Map.Entry<BlockPos, BlockState> block : blocks.entrySet()) {
            if (level.getBlockState(block.getKey()).isAir()) {
                level.setBlock(block.getKey(), block.getValue(), FLAGS);
            }
        }
        return true;
    }
}
