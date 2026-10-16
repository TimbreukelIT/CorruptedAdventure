package nl.timtoys.corruptedadventure.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A ground plant of {@code verdant_falls} (72b): bromeliad and blossom bush. A bush on {@code #dirt} (grass, moss, mud,
 * peat) that pops a copy with bonemeal ({@link PoppingBonemealable}).
 */
public class VerdantPlantBlock extends BushBlock implements PoppingBonemealable {
    public static final MapCodec<VerdantPlantBlock> CODEC = simpleCodec(VerdantPlantBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public VerdantPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VerdantPlantBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Follows the random offset of the model, like vanilla's flowers.
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }
}
