package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.List;

/**
 * {@code corruptedadventure:giant_fungus} (change 35): a giant fungus of the dripshroom forest, a pillar from floor to
 * ceiling or a free-standing one ({@link GiantFungusShape}). The origin's Y is the start of the floor and ceiling scan, and
 * must be air; both scans read terrain only ({@link FungalTerrainScan}). Every part is written through
 * {@link GrowthPriority}, so the result does not depend on the order in which chunks are decorated.
 */
public final class GiantFungusFeature extends Feature<GiantFungusConfiguration> {
    public GiantFungusFeature(Codec<GiantFungusConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GiantFungusConfiguration> context) {
        WorldGenLevel level = context.level();
        GiantFungusConfiguration config = context.config();
        FungalWorld world = FungalWorld.of(level);
        BlockPos origin = context.origin();
        List<GiantFungusShape.Part> parts = GiantFungusShape.build(context.random(), origin.getX(), origin.getY(), origin.getZ(),
                ground(world), config.pillars());
        if (parts.isEmpty()) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (GiantFungusShape.Part part : parts) {
            GrowthPriority.write(world, pos.set(part.x(), part.y(), part.z()), state(config, part));
        }
        return true;
    }

    static BlockState state(GiantFungusConfiguration config, GiantFungusShape.Part part) {
        return switch (part.kind()) {
            case STEM -> config.stem().hasProperty(RotatedPillarBlock.AXIS) ? config.stem().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : config.stem();
            case CAP -> config.cap();
            case LAMP -> config.lamp();
            case STALACTITE -> config.stalactite()
                    .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN)
                    .setValue(BlockStateProperties.DRIPSTONE_THICKNESS, part.thickness());
        };
    }

    static GiantFungusShape.Ground ground(FungalWorld world) {
        int scan = GiantFungusShape.scan();
        return new GiantFungusShape.Ground() {
            @Override
            public int floor(int x, int fromY, int z) {
                return FungalTerrainScan.floor(world, x, fromY, z, scan);
            }

            @Override
            public int ceiling(int x, int fromY, int z) {
                return FungalTerrainScan.ceiling(world, x, fromY, z, scan);
            }

            @Override
            public boolean lava(int x, int y, int z) {
                return FungalTerrainScan.isLava(world.get(new BlockPos(x, y, z)));
            }
        };
    }
}
