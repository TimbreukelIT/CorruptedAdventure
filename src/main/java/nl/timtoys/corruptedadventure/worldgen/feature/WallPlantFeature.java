package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code corruptedadventure:wall_plant} (75b, §6b: orchids and wall bromeliads on the gorge walls): in the origin's
 * column, from the origin (the foot of a wall) up {@code height} blocks, each open block beside a sturdy wall takes a
 * wall plant with {@code chance}, facing away from the wall (the first sturdy side in a random turn of the four).
 * Writes only in the origin's column; the walls are terrain. Never in water (a wall plant is not waterlogged).
 */
public final class WallPlantFeature extends Feature<WallPlantConfiguration> {
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public WallPlantFeature(Codec<WallPlantConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<WallPlantConfiguration> context) {
        WorldGenLevel level = context.level();
        WallPlantConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dy = 0; dy < config.height(); dy++) {
            // Always the same draws, before the world is asked, so a block never depends on the ones below it.
            float roll = random.nextFloat();
            int turn = random.nextInt(SIDES.length);
            BlockState plant = config.plants().getState(random, BlockPos.ZERO);
            pos.set(origin.getX(), origin.getY() + dy, origin.getZ());
            if (roll >= config.chance() || !level.getBlockState(pos).isAir()) {
                continue;
            }
            for (int i = 0; i < SIDES.length; i++) {
                Direction wall = SIDES[(turn + i) % SIDES.length];
                BlockState state = plant.trySetValue(HorizontalDirectionalBlock.FACING, wall.getOpposite());
                if (state.canSurvive(level, pos)) {
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                    placed = true;
                    break;
                }
            }
        }
        return placed;
    }
}
