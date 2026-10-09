package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code corruptedadventure:wall_drape} (change 47d): green over a crater wall, from its foot up: vines on the wall's
 * faces and hanging roots under its overhangs. The column of the origin climbs through air for {@code height} blocks;
 * each block takes a vine on the first sturdy wall face beside it, or hanging roots where it hangs under solid rock.
 * Reads and writes only the origin's column and its four neighbours, inside its own chunk (the placement margin is at
 * least 1).
 */
public final class WallDrapeFeature extends Feature<WallDrapeConfiguration> {
    public WallDrapeFeature(Codec<WallDrapeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<WallDrapeConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        WallDrapeConfiguration config = context.config();
        int height = config.height().sample(random);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = context.origin().mutable();
        for (int i = 0; i < height; i++, pos.move(Direction.UP)) {
            // Two draws per block, used or not.
            float vine = random.nextFloat();
            float root = random.nextFloat();
            if (!level.getBlockState(pos).isAir()) {
                break;
            }
            Direction wall = wall(level, pos);
            if (wall != null && vine < config.vineChance()) {
                level.setBlock(pos, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(wall), true), Block.UPDATE_CLIENTS);
                placed = true;
            } else if (root < config.rootChance() && level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN)) {
                level.setBlock(pos, Blocks.HANGING_ROOTS.defaultBlockState(), Block.UPDATE_CLIENTS);
                placed = true;
            }
        }
        return placed;
    }

    /** The first horizontal side with a sturdy face towards {@code pos}, or null. */
    private static Direction wall(WorldGenLevel level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = pos.relative(side);
            BlockState state = level.getBlockState(neighbour);
            if (state.isFaceSturdy(level, neighbour, side.getOpposite())) {
                return side;
            }
        }
        return null;
    }
}
