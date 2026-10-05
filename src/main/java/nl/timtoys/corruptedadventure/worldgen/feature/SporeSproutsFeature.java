package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code corruptedadventure:spore_sprouts_patch} (change 35): a small patch of ground plants on the floor below the
 * origin. The floor of each column is found with {@link FungalTerrainScan} from a few blocks above the origin's floor; any
 * floor that is terrain (not lava, not bedrock) takes a plant, without looking at which block it is (an ore or a dew
 * source that another feature wrote there later would otherwise change the outcome). The plants are the lowest growth.
 */
public final class SporeSproutsFeature extends Feature<SporeSproutsConfiguration> {
    private static final int SCAN = 40;
    /** How far above the centre's floor the other columns of the patch are scanned from. */
    private static final int LIFT = 3;
    private static final int COLUMN_SCAN = 6;

    public SporeSproutsFeature(Codec<SporeSproutsConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SporeSproutsConfiguration> context) {
        FungalWorld world = FungalWorld.of(context.level());
        SporeSproutsConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int floor = FungalTerrainScan.floor(world, origin.getX(), origin.getY(), origin.getZ(), SCAN);
        if (floor == FungalTerrainScan.NONE || !standable(world.get(new BlockPos(origin.getX(), floor, origin.getZ())))) {
            return false;
        }
        int radius = config.radius().sample(random);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                // One draw per column of the patch, used or not.
                boolean chosen = random.nextFloat() < config.density();
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int columnFloor = chosen ? FungalTerrainScan.floor(world, x, floor + LIFT, z, COLUMN_SCAN + LIFT) : FungalTerrainScan.NONE;
                if (columnFloor == FungalTerrainScan.NONE || !standable(world.get(pos.set(x, columnFloor, z)))) {
                    continue;
                }
                GrowthPriority.write(world, pos.set(x, columnFloor + 1, z), config.plant());
                placed = true;
            }
        }
        return placed;
    }

    private static boolean standable(BlockState floor) {
        return !FungalTerrainScan.isLava(floor) && !floor.is(Blocks.BEDROCK);
    }
}
