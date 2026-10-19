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

import java.util.Map;
import java.util.TreeMap;

/**
 * {@code corruptedadventure:terrace_dressing} (ring 4 decor proef, 2026-10-19): the edges of the terraces of
 * {@code cascade_vale}, one column at a time. Where a neighbouring column stands {@code min_step} or more higher, a vine
 * may hang down that wall from its top; where one lies that much lower, the column is a rim: moss for its grass and
 * now and then a carpet on it.
 * <p>
 * Order-independent as the ground objects ({@link GroundObjects}): every draw comes from the column's own random and is
 * made whatever stands there; it reads the frozen heightmaps and the terrain of the wall beside it (sturdy whatever
 * grows on it), changes only its own column's grass, and puts vines and carpets only into open air, so a tree's block
 * wins over them in either order. Each vine block holds on to the wall by itself.
 */
public final class TerraceDressingFeature extends Feature<TerraceDressingConfiguration> {
    private static final long SALT = 0x7E77_ACEL;

    public TerraceDressingFeature(Codec<TerraceDressingConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TerraceDressingConfiguration> context) {
        WorldGenLevel level = context.level();
        TerraceDressingConfiguration config = context.config();
        int x = context.origin().getX();
        int z = context.origin().getZ();
        RandomSource random = GroundObjects.random(level.getSeed(), new BlockPos(x, 0, z), SALT);
        int ground = GroundObjects.surface(level, x, z);
        Map<Integer, BlockState> vines = new TreeMap<>();
        boolean rim = false;
        BlockPos.MutableBlockPos wall = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            boolean hangs = random.nextFloat() < config.vineChance();
            int length = 1 + random.nextInt(config.maxVine());
            int step = GroundObjects.surface(level, x + direction.getStepX(), z + direction.getStepZ()) - ground;
            rim |= step <= -config.minStep();
            if (step < config.minStep() || !hangs) {
                continue;
            }
            int top = ground + step - 1;
            for (int y = top; y >= Math.max(ground, top - length + 1); y--) {
                wall.set(x + direction.getStepX(), y, z + direction.getStepZ());
                if (!level.getBlockState(wall).isFaceSturdy(level, wall, direction.getOpposite())) {
                    break;
                }
                vines.merge(y, config.vine().setValue(VineBlock.getPropertyForFace(direction), true),
                        (a, b) -> a.setValue(VineBlock.getPropertyForFace(direction), true));
            }
        }
        boolean moss = random.nextFloat() < config.mossChance();
        boolean carpet = random.nextFloat() < config.carpetChance();
        if (!GroundObjects.dry(level, x, z)) {
            return false;
        }
        boolean placed = false;
        for (Map.Entry<Integer, BlockState> vine : vines.entrySet()) {
            placed |= GroundObjects.placeDecor(level, new BlockPos(x, vine.getKey(), z), vine.getValue());
        }
        if (rim) {
            BlockPos grass = new BlockPos(x, ground - 1, z);
            if (moss && level.getBlockState(grass).is(Blocks.GRASS_BLOCK)) {
                level.setBlock(grass, config.moss(), Block.UPDATE_CLIENTS);
                placed = true;
            }
            if (carpet) {
                placed |= GroundObjects.placeDecor(level, grass.above(), config.carpet());
            }
        }
        return placed;
    }
}
