package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

/**
 * {@code corruptedadventure:island_lift} (lift package before teststop 5b; design ring 4 "Omhoog"): fills the lift
 * columns of package 61 ({@link IslandField#liftsWithin}: beside about one in four middle and giant islands, two blocks
 * out from the edge, clear of every island) with the updraft, from the water's top block (the sea's Y 62 or the
 * island's lake) up to two blocks above the edge.
 * <p>
 * A column is one block wide, so it lies in one chunk: each chunk fills the lifts whose column it holds and writes
 * nothing elsewhere. The column is a pure function of the seed ({@link IslandField}); every block of it becomes the
 * updraft whatever stands there, so a lily pad, a vine or a neighbouring tree's leaves in the column never make the
 * result depend on the order chunks decorate in. Placed last (top layer step), after every feature of its own chunk.
 */
public final class IslandLiftFeature extends Feature<BlockStateConfiguration> {
    public IslandLiftFeature(Codec<BlockStateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        BlockState updraft = context.config().state;
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (IslandField.Lift lift : field.liftsWithin(minX, minZ, minX + 15, minZ + 15)) {
            int from = Math.max(lift.bottom(), level.getMinBuildHeight());
            int to = Math.min(lift.top(), level.getMaxBuildHeight() - 1);
            for (int y = from; y <= to; y++) {
                level.setBlock(pos.set(lift.x(), y, lift.z()), updraft, Block.UPDATE_CLIENTS);
                placed = true;
            }
        }
        return placed;
    }
}
