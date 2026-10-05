package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code corruptedadventure:slime_stalactite} (change 35): a slime stalactite of 1 to a few blocks hanging from the
 * terrain ceiling above the origin. Replaces vanilla's {@code block_column}-style placement, which cuts off and shifts
 * the tip depending on what stands there: every block goes through {@link GrowthPriority} to the same position whatever
 * is already there, so a stalactite that a higher growth overwrites half way simply stays shorter (cosmetic).
 */
public final class SlimeStalactiteFeature extends Feature<SlimeStalactiteConfiguration> {
    private static final int SCAN = 40;

    public SlimeStalactiteFeature(Codec<SlimeStalactiteConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SlimeStalactiteConfiguration> context) {
        FungalWorld world = FungalWorld.of(context.level());
        BlockPos origin = context.origin();
        int ceiling = FungalTerrainScan.ceiling(world, origin.getX(), origin.getY(), origin.getZ(), SCAN);
        if (ceiling == FungalTerrainScan.NONE) {
            return false;
        }
        List<GiantFungusShape.Part> parts = new ArrayList<>();
        GiantFungusShape.hang(parts, origin.getX(), ceiling - 1, origin.getZ(), context.config().length().sample(context.random()));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (GiantFungusShape.Part part : parts) {
            GrowthPriority.write(world, pos.set(part.x(), part.y(), part.z()), context.config().stalactite()
                    .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN)
                    .setValue(BlockStateProperties.DRIPSTONE_THICKNESS, part.thickness()));
        }
        return true;
    }
}
