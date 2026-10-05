package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code corruptedadventure:mold_patch} (change 35): glow mold on the terrain surfaces (floor, ceiling and walls) within a
 * sphere around the origin. Replaces vanilla's {@code multiface_growth}, which searches for a place and so depends on what
 * the neighbouring chunks put there: here every free block next to terrain (air or a growth) decides on its own, in a
 * fixed order, and gets the faces towards terrain; two patches that overlap merge their faces ({@link GrowthPriority}).
 */
public final class MoldPatchFeature extends Feature<MoldPatchConfiguration> {
    public MoldPatchFeature(Codec<MoldPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<MoldPatchConfiguration> context) {
        FungalWorld world = FungalWorld.of(context.level());
        MoldPatchConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int radius = config.radius().sample(random);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    pos.setWithOffset(origin, dx, dy, dz);
                    // One draw per block of the sphere, used or not.
                    boolean chosen = random.nextFloat() < config.chance();
                    if (!chosen || FungalTerrainScan.isTerrain(world.get(pos))) {
                        continue;
                    }
                    BlockState mold = withFaces(config.mold(), world, pos, side);
                    if (mold != null) {
                        GrowthPriority.write(world, pos, mold);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    /** {@code mold} with a face on every side of {@code pos} that is terrain, or null when there is none. */
    static BlockState withFaces(BlockState mold, FungalWorld world, BlockPos pos, BlockPos.MutableBlockPos side) {
        BlockState result = mold;
        boolean any = false;
        for (Direction direction : Direction.values()) {
            BlockState neighbour = world.get(side.setWithOffset(pos, direction));
            if (FungalTerrainScan.isTerrain(neighbour) && !FungalTerrainScan.isLava(neighbour)) {
                result = result.setValue(MultifaceBlock.getFaceProperty(direction), true);
                any = true;
            }
        }
        return any ? result : null;
    }
}
